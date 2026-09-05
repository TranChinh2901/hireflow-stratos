package com.hireflow.app

import android.app.Application
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hireflow.app.data.CandidateEntity
import com.hireflow.app.data.CvStorage
import com.hireflow.app.data.HrTaskEntity
import com.hireflow.app.data.InterviewEntity
import com.hireflow.app.data.LocalDataScope
import com.hireflow.app.data.RecruitmentStage
import com.hireflow.app.data.ScorecardEntity
import com.hireflow.app.data.StageHistoryEntity
import com.hireflow.app.domain.RecruitmentRules
import com.hireflow.app.preferences.SettingsStore
import com.hireflow.app.reminder.syncInterviewReminders
import com.hireflow.app.cloud.UserProfileDto
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

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
    val histories: List<StageHistoryEntity> = emptyList(),
    val tasks: List<HrTaskEntity> = emptyList(),
    val darkMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val loading: Boolean = true
)

private data class LocalContent(
    val candidates: List<CandidateEntity>,
    val interviews: List<InterviewEntity>,
    val scorecards: List<ScorecardEntity>,
    val histories: List<StageHistoryEntity>,
    val tasks: List<HrTaskEntity>
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
    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice
    private var realtimeJob: Job? = null

    private val appearance = combine(settings.darkMode, settings.notificationsEnabled) { darkMode, notifications ->
        darkMode to notifications
    }

    private val localContent = combine(
        repository.candidates,
        repository.interviews,
        repository.scorecards,
        repository.histories,
        repository.tasks
    ) { candidates, interviews, scorecards, histories, tasks ->
        LocalContent(candidates, interviews, scorecards, histories, tasks)
    }

    val uiState: StateFlow<HireFlowUiState> = combine(
        localContent,
        appearance,
        accountState
    ) { content, appearance, account ->
        val organizationId = account.profile?.organizationId
        fun inActiveScope(rowOrganizationId: String?): Boolean = LocalDataScope.matches(
            rowOrganizationId = rowOrganizationId,
            activeOrganizationId = organizationId,
            authenticated = account.authenticated,
            offlineMode = account.offlineMode
        )
        val candidates = content.candidates.filter { inActiveScope(it.organizationId) }
        val candidateIds = candidates.mapTo(hashSetOf()) { it.id }
        val candidatesById = candidates.associateBy { it.id }
        HireFlowUiState(
            candidates = candidates,
            interviews = content.interviews.filter { interview ->
                val candidate = candidatesById[interview.candidateId]
                inActiveScope(interview.organizationId) && candidate != null &&
                    RecruitmentRules.shouldShowInterview(candidate, interview)
            },
            scorecards = content.scorecards.filter { inActiveScope(it.organizationId) && it.candidateId in candidateIds },
            histories = content.histories.filter { inActiveScope(it.organizationId) && it.candidateId in candidateIds },
            tasks = content.tasks.filter { inActiveScope(it.organizationId) },
            darkMode = appearance.first,
            notificationsEnabled = appearance.second,
            loading = false
        )
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

    fun signUp(fullName: String, email: String, phone: String, password: String) {
        viewModelScope.launch {
            _accountState.value = _accountState.value.copy(checking = true, message = null)
            runCatching { backend.signUp(fullName.trim(), email.trim(), phone.trim(), password) }
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
                .onSuccess {
                    syncInterviewReminders(app, emptyList(), false)
                    _accountState.value = AccountUiState(configured = backend.isConfigured, checking = false)
                }
                .onFailure {
                    startRealtime()
                    _notice.value = "Không thể đăng xuất. Hãy kiểm tra kết nối và thử lại."
                }
        }
    }

    fun syncNow() {
        val profile = _accountState.value.profile ?: return
        viewModelScope.launch {
            _accountState.value = _accountState.value.copy(syncing = true, message = null)
            runCatching { syncManager.syncAll(profile) }
                .onSuccess {
                    _accountState.value = _accountState.value.copy(syncing = false)
                    _notice.value = "Đồng bộ hoàn tất."
                }
                .onFailure {
                    _accountState.value = _accountState.value.copy(syncing = false)
                    _notice.value = "Đang offline, thay đổi sẽ tự đồng bộ sau."
                }
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
            merge(
                syncManager.realtimeCandidates()
                    .onEach { syncManager.mergeRealtime(it) }
                    .map { Unit },
                syncManager.realtimeInterviews()
                    .onEach { syncManager.mergeRealtimeInterviews(it) }
                    .map { Unit },
                syncManager.realtimeScorecards()
                    .onEach { syncManager.mergeRealtimeScorecards(it) }
                    .map { Unit }
            )
                .catch { _notice.value = "Realtime tạm mất kết nối." }
                .collectLatest { }
        }
    }

    private fun requestSync() {
        if (_accountState.value.authenticated) app.requestCloudSync()
    }

    fun selectCandidate(id: Long) { selectedCandidateId.value = id }

    fun addCandidate(candidate: CandidateEntity, onAdded: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val organizationId = _accountState.value.profile?.organizationId
            if (!_accountState.value.offlineMode && organizationId == null) return@launch
            runCatching { repository.addCandidate(candidate.copy(organizationId = organizationId)) }
                .onSuccess { onAdded(it); requestSync() }
                .onFailure { _notice.value = it.message ?: "Không thể thêm ứng viên." }
        }
    }

    fun updateCandidate(candidate: CandidateEntity) {
        viewModelScope.launch {
            runCatching { repository.updateCandidate(candidate) }
                .onSuccess { requestSync() }
                .onFailure { _notice.value = it.message ?: "Không thể cập nhật ứng viên." }
        }
    }

    fun attachCv(id: Long, uri: String) {
        viewModelScope.launch {
            runCatching {
                val candidate = requireNotNull(repository.candidateByLocalId(id)) { "Không tìm thấy ứng viên." }
                val sourceUri = uri.toUri()
                // Lấy tên file gốc từ Drive/Downloads trước khi copy, để hiển thị đúng tên user chọn.
                val originalName = CvStorage.queryDisplayName(app, sourceUri)
                    ?: candidate.cvFileName?.takeIf { it.isNotBlank() }
                // Copy file thật vào bộ nhớ riêng của app để lưu luôn, không phụ thuộc file gốc.
                val file = withContext(Dispatchers.IO) {
                    CvStorage.saveFromUri(app, candidate.remoteId, sourceUri)
                }
                val internalUri = CvStorage.providerUri(app, file).toString()
                // remoteCvPath = null để buộc upload lại bản mới khi sync.
                repository.attachCv(id, internalUri, null, originalName)
                _notice.value = "Đã lưu CV trong app."
                val profile = _accountState.value.profile
                val updated = repository.candidateByLocalId(id)
                if (profile != null && updated != null) {
                    runCatching { syncManager.uploadCandidateCvFromFile(updated, file, profile) }
                        .onSuccess { requestSync() }
                        .onFailure { _notice.value = "CV đã lưu trong app, sẽ upload khi có mạng." }
                }
            }.onFailure {
                _notice.value = it.message ?: "Không thể lưu CV."
            }
        }
    }

    fun openCv(candidate: CandidateEntity, onReady: (String) -> Unit) {
        // 1. Ưu tiên file thật đã lưu trong app (filesDir), sống sót sau reboot/xóa file gốc.
        CvStorage.findLocalFile(app, candidate.remoteId)?.let {
            onReady(CvStorage.providerUri(app, it).toString())
            return
        }
        // 2. Migrate CV cũ đang là SAF Uri ngoài: copy vào app rồi mở.
        candidate.cvUri?.let { oldUri ->
            runCatching {
                val parsed = oldUri.toUri()
                val name = CvStorage.queryDisplayName(app, parsed) ?: candidate.cvFileName
                val file = CvStorage.saveFromUri(app, candidate.remoteId, parsed)
                name to CvStorage.providerUri(app, file).toString()
            }.onSuccess { (name, internalUri) ->
                viewModelScope.launch { repository.attachCv(candidate.id, internalUri, candidate.remoteCvPath, name) }
                onReady(internalUri)
                return
            }
        }
        val remotePath = candidate.remoteCvPath ?: run {
            _notice.value = "Ứng viên chưa có CV."
            return
        }
        // 3. Chưa có local nhưng có trên cloud: tải về filesDir và lưu luôn.
        viewModelScope.launch {
            runCatching {
                val bytes = withContext(Dispatchers.IO) { backend.downloadCv(remotePath) }
                val file = withContext(Dispatchers.IO) { CvStorage.saveBytes(app, candidate.remoteId, bytes) }
                val internalUri = CvStorage.providerUri(app, file).toString()
                repository.attachCv(candidate.id, internalUri, remotePath, candidate.cvFileName)
                internalUri
            }.onSuccess(onReady)
                .onFailure { _notice.value = "Không thể tải CV từ cloud. Hãy kiểm tra kết nối." }
        }
    }

    fun moveNext(candidate: CandidateEntity) {
        viewModelScope.launch {
            runCatching { repository.moveNext(candidate, _accountState.value.profile?.id) }
                .onSuccess { requestSync() }
                .onFailure { _notice.value = it.message ?: "Không thể chuyển vòng ứng viên." }
        }
    }

    fun reject(candidate: CandidateEntity) {
        viewModelScope.launch {
            runCatching { repository.reject(candidate, _accountState.value.profile?.id) }
                .onSuccess { requestSync() }
                .onFailure { _notice.value = it.message ?: "Không thể từ chối ứng viên." }
        }
    }

    fun deleteRejectedCandidates() {
        viewModelScope.launch {
            val targets = runCatching { repository.rejectedCandidates() }
                .getOrElse {
                    _notice.value = it.message ?: "Không thể xóa ứng viên."
                    return@launch
                }
            if (targets.isEmpty()) {
                _notice.value = "Không có ứng viên bị từ chối để xóa."
                return@launch
            }
            // Xóa trên cloud trước (best-effort, cascade xóa interview/scorecard/history),
            // RLS chỉ cho admin xóa; nếu thất bại vẫn xóa local.
            if (_accountState.value.authenticated) {
                runCatching { backend.deleteCandidates(targets.map { it.remoteId }) }
            }
            runCatching { repository.deleteRejectedCandidates() }
                .onSuccess { deleted ->
                    targets.forEach { CvStorage.deleteLocalFile(app, it.remoteId) }
                    _notice.value = "Đã xóa ${deleted.size} ứng viên bị từ chối."
                }
                .onFailure { _notice.value = it.message ?: "Không thể xóa ứng viên." }
        }
    }

    fun deleteCandidate(candidate: CandidateEntity) {
        viewModelScope.launch {
            runCatching {
                require(candidate.recruitmentStage == RecruitmentStage.REJECTED) { "Chỉ được xóa ứng viên đã bị từ chối." }
                repository.deleteCandidate(candidate.id)
                withContext(Dispatchers.IO) { CvStorage.deleteLocalFile(app, candidate.remoteId) }
                if (_accountState.value.authenticated) {
                    runCatching { backend.deleteCandidate(candidate.remoteId) }
                }
            }.onSuccess { _notice.value = "Đã xóa ${candidate.name}." }
                .onFailure { _notice.value = it.message ?: "Không thể xóa ứng viên." }
        }
    }

    fun deleteRejectedCandidates(candidates: List<CandidateEntity>) {
        val rejected = candidates.filter { it.recruitmentStage == RecruitmentStage.REJECTED }
        if (rejected.isEmpty()) {
            _notice.value = "Không có ứng viên bị từ chối để xóa."
            return
        }
        viewModelScope.launch {
            var deleted = 0
            rejected.forEach { candidate ->
                runCatching {
                    repository.deleteCandidate(candidate.id)
                    withContext(Dispatchers.IO) { CvStorage.deleteLocalFile(app, candidate.remoteId) }
                    if (_accountState.value.authenticated) {
                        runCatching { backend.deleteCandidate(candidate.remoteId) }
                    }
                    deleted++
                }
            }
            _notice.value = if (deleted > 0) "Đã xóa $deleted ứng viên bị từ chối." else "Không thể xóa ứng viên."
        }
    }

    fun addInterview(interview: InterviewEntity, onAdded: (Long) -> Unit) {
        viewModelScope.launch {
            runCatching {
                repository.addInterview(interview.copy(interviewerUserId = _accountState.value.profile?.id))
            }
                .onSuccess { onAdded(it); requestSync() }
                .onFailure { _notice.value = it.message ?: "Không thể tạo lịch phỏng vấn." }
        }
    }

    fun saveScorecard(scorecard: ScorecardEntity) {
        viewModelScope.launch {
            val evaluatorId = _accountState.value.profile?.id
            runCatching { repository.saveScorecard(scorecard, evaluatorId) }
                .onSuccess { requestSync() }
                .onFailure { _notice.value = it.message ?: "Không thể lưu phiếu đánh giá." }
        }
    }

    fun setInterviewCompleted(interview: InterviewEntity, completed: Boolean) {
        viewModelScope.launch {
            runCatching { repository.setInterviewCompleted(interview, completed) }
                .onSuccess { requestSync() }
                .onFailure { _notice.value = it.message ?: "Không thể cập nhật lịch phỏng vấn." }
        }
    }

    fun toggleTask(task: HrTaskEntity) {
        viewModelScope.launch {
            repository.toggleTask(task)
            requestSync()
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { settings.setDarkMode(enabled) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setNotificationsEnabled(enabled) }
    }

    fun clearNotice() {
        _notice.value = null
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
                    message = null,
                    messageIsError = false
                )
                _notice.value = "Đã cập nhật hồ sơ."
            }.onFailure {
                _accountState.value = _accountState.value.copy(checking = false)
                _notice.value = "Không thể cập nhật hồ sơ."
            }
        }
    }
}
