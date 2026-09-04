package com.hireflow.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hireflow.app.data.CandidateEntity
import com.hireflow.app.data.HrTaskEntity
import com.hireflow.app.data.InterviewEntity
import com.hireflow.app.data.ScorecardEntity
import com.hireflow.app.data.StageHistoryEntity
import com.hireflow.app.preferences.SettingsStore
import com.hireflow.app.cloud.UserProfileDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest

data class AccountUiState(
    val configured: Boolean = false,
    val checking: Boolean = true,
    val authenticated: Boolean = false,
    val offlineMode: Boolean = false,
    val profile: UserProfileDto? = null,
    val email: String? = null,
    val syncing: Boolean = false,
    val message: String? = null,
    val messageIsError: Boolean = false
) {
    val role: String get() = profile?.role ?: if (offlineMode) "admin" else ""
    val canManageRecruitment: Boolean get() = offlineMode || role == "admin" || role == "hr"
}

data class HireFlowUiState(
    val candidates: List<CandidateEntity> = emptyList(),
    val interviews: List<InterviewEntity> = emptyList(),
    val scorecards: List<ScorecardEntity> = emptyList(),
    val tasks: List<HrTaskEntity> = emptyList(),
    val darkMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val loading: Boolean = true
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HireFlowViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as HireFlowApplication
    private val repository = app.repository
    private val backend = app.backend
    private val syncManager = app.syncManager
    private val settings = SettingsStore(application)
    private val selectedCandidateId = MutableStateFlow<Long?>(null)
    private val _accountState = MutableStateFlow(AccountUiState(configured = backend.isConfigured))
    val accountState: StateFlow<AccountUiState> = _accountState
    private var realtimeJob: Job? = null

    private val appearance = combine(settings.darkMode, settings.notificationsEnabled) { darkMode, notifications ->
        darkMode to notifications
    }

    val uiState: StateFlow<HireFlowUiState> = combine(
        repository.candidates,
        repository.interviews,
        repository.scorecards,
        repository.tasks,
        appearance
    ) { candidates, interviews, scorecards, tasks, appearance ->
        HireFlowUiState(candidates, interviews, scorecards, tasks, appearance.first, appearance.second, loading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HireFlowUiState())

    val selectedCandidate: StateFlow<CandidateEntity?> = selectedCandidateId
        .flatMapLatest { id -> if (id == null) flowOf(null) else repository.candidate(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val selectedHistory: StateFlow<List<StageHistoryEntity>> = selectedCandidateId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repository.history(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            if (!backend.isConfigured) repository.seedDemoData()
            restoreSession()
        }
    }

    private suspend fun restoreSession() {
        if (!backend.isConfigured || backend.currentUserId() == null) {
            _accountState.value = _accountState.value.copy(checking = false)
            return
        }
        loadProfileAndSync()
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _accountState.value = _accountState.value.copy(checking = true, message = null)
            runCatching { backend.signIn(email.trim(), password) }
                .onSuccess { loadProfileAndSync() }
                .onFailure { showAuthError(it, "Đăng nhập thất bại. Vui lòng thử lại.") }
        }
    }

    fun signUp(fullName: String, email: String, phone: String, password: String, role: String) {
        viewModelScope.launch {
            _accountState.value = _accountState.value.copy(checking = true, message = null)
            runCatching { backend.signUp(fullName.trim(), email.trim(), phone.trim(), password, role) }
                .onSuccess {
                    if (backend.currentUserId() != null) loadProfileAndSync()
                    else _accountState.value = _accountState.value.copy(
                        checking = false,
                        message = "Đã gửi email xác nhận. Hãy kiểm tra hộp thư rồi đăng nhập.",
                        messageIsError = false
                    )
                }
                .onFailure { showAuthError(it, "Không thể tạo tài khoản. Vui lòng thử lại.") }
        }
    }

    private fun showAuthError(error: Throwable, fallback: String) {
        val raw = error.message.orEmpty().lowercase()
        val friendly = when {
            "over_email_send_rate_limit" in raw || "email rate limit" in raw ->
                "Đã gửi quá nhiều email xác nhận. Vui lòng đợi một lúc rồi thử lại."
            "invalid login credentials" in raw -> "Email hoặc mật khẩu chưa chính xác."
            "email not confirmed" in raw -> "Email chưa được xác nhận. Hãy kiểm tra hộp thư của bạn."
            "user already registered" in raw || "already been registered" in raw -> "Email này đã được đăng ký."
            "pgrst205" in raw || "profiles" in raw && "schema cache" in raw -> "Cơ sở dữ liệu chưa được khởi tạo đầy đủ."
            "unable to resolve host" in raw || "failed to connect" in raw || "timeout" in raw -> "Không thể kết nối máy chủ. Hãy kiểm tra mạng."
            else -> fallback
        }
        _accountState.value = _accountState.value.copy(checking = false, message = friendly, messageIsError = true)
    }

    fun useOfflineDemo() {
        viewModelScope.launch {
            repository.seedDemoData()
            _accountState.value = _accountState.value.copy(checking = false, offlineMode = true, message = null)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            realtimeJob?.cancel()
            runCatching { backend.signOut() }
            _accountState.value = AccountUiState(configured = backend.isConfigured, checking = false)
        }
    }

    fun syncNow() {
        val profile = _accountState.value.profile ?: return
        viewModelScope.launch {
            _accountState.value = _accountState.value.copy(syncing = true, message = null)
            runCatching { syncManager.syncAll(profile) }
                .onSuccess { _accountState.value = _accountState.value.copy(syncing = false, message = "Đồng bộ hoàn tất") }
                .onFailure { _accountState.value = _accountState.value.copy(syncing = false, message = "Đang offline, thay đổi sẽ tự đồng bộ sau") }
        }
    }

    private suspend fun loadProfileAndSync() {
        runCatching { backend.profile() }
            .onSuccess { profile ->
                _accountState.value = AccountUiState(
                    configured = true, checking = false, authenticated = true,
                    profile = profile, email = backend.currentEmail(), syncing = true
                )
                startRealtime()
                runCatching { syncManager.syncAll(profile) }
                _accountState.value = _accountState.value.copy(syncing = false)
            }
            .onFailure { _accountState.value = _accountState.value.copy(checking = false, message = "Không tải được hồ sơ người dùng.", messageIsError = true) }
    }

    private fun startRealtime() {
        realtimeJob?.cancel()
        realtimeJob = viewModelScope.launch {
            syncManager.realtimeCandidates()
                .catch { _accountState.value = _accountState.value.copy(message = "Realtime tạm mất kết nối") }
                .collectLatest { syncManager.mergeRealtime(it) }
        }
    }

    private fun requestSync() {
        if (_accountState.value.authenticated) app.requestCloudSync()
    }

    fun selectCandidate(id: Long) { selectedCandidateId.value = id }

    fun addCandidate(candidate: CandidateEntity, onAdded: (Long) -> Unit = {}) {
        viewModelScope.launch { onAdded(repository.addCandidate(candidate)); requestSync() }
    }

    fun updateCandidate(candidate: CandidateEntity) {
        viewModelScope.launch { repository.updateCandidate(candidate); requestSync() }
    }

    fun attachCv(id: Long, uri: String) {
        viewModelScope.launch {
            repository.attachCv(id, uri)
            val profile = _accountState.value.profile
            val candidate = repository.candidateByLocalId(id)
            if (profile != null && candidate != null) {
                runCatching { syncManager.uploadCandidateCv(candidate, Uri.parse(uri), profile) }
                    .onSuccess { requestSync() }
                    .onFailure { _accountState.value = _accountState.value.copy(message = "CV đã lưu local và sẽ upload khi có mạng") }
            }
        }
    }

    fun moveNext(candidate: CandidateEntity) {
        viewModelScope.launch { repository.moveNext(candidate); requestSync() }
    }

    fun reject(candidate: CandidateEntity) {
        viewModelScope.launch { repository.reject(candidate); requestSync() }
    }

    fun addInterview(interview: InterviewEntity, onAdded: (Long) -> Unit) {
        viewModelScope.launch { onAdded(repository.addInterview(interview)); requestSync() }
    }

    fun saveScorecard(scorecard: ScorecardEntity) {
        viewModelScope.launch { repository.saveScorecard(scorecard); requestSync() }
    }

    fun toggleTask(task: HrTaskEntity) {
        viewModelScope.launch { repository.toggleTask(task) }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { settings.setDarkMode(enabled) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setNotificationsEnabled(enabled) }
    }

    fun updateProfile(fullName: String, phone: String, department: String, jobTitle: String) {
        if (!_accountState.value.authenticated) return
        viewModelScope.launch {
            _accountState.value = _accountState.value.copy(checking = true, message = null)
            runCatching {
                backend.updateProfile(fullName.trim(), phone.trim(), department.trim(), jobTitle.trim())
                backend.profile()
            }.onSuccess { profile ->
                _accountState.value = _accountState.value.copy(
                    checking = false,
                    profile = profile,
                    message = "Đã cập nhật hồ sơ.",
                    messageIsError = false
                )
            }.onFailure { showAuthError(it, "Không thể cập nhật hồ sơ.") }
        }
    }
}
