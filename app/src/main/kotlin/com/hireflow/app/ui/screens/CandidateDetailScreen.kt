package com.hireflow.app.ui.screens

import com.hireflow.app.ui.components.HeaderAction
import com.hireflow.app.ui.components.HeaderOverflow
import com.hireflow.app.ui.components.LocalHeaderNavigation
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hireflow.app.data.CandidateEntity
import com.hireflow.app.data.InterviewEntity
import com.hireflow.app.data.RecruitmentStage
import com.hireflow.app.data.ScorecardEntity
import com.hireflow.app.data.StageHistoryEntity
import com.hireflow.app.domain.PrimaryAction
import com.hireflow.app.domain.RecruitmentRules
import com.hireflow.app.domain.WorkQueue
import com.hireflow.app.ui.components.EmptyState
import com.hireflow.app.ui.components.InfoCard
import com.hireflow.app.ui.components.InitialAvatar
import com.hireflow.app.ui.components.ScreenHeader
import com.hireflow.app.ui.components.SectionTitle
import com.hireflow.app.ui.components.StagePill
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun CandidateDetailScreen(
    candidate: CandidateEntity?,
    scorecard: ScorecardEntity?,
    candidateScorecards: List<ScorecardEntity> = emptyList(),
    interviews: List<InterviewEntity>,
    histories: List<StageHistoryEntity>,
    onBack: () -> Unit,
    onAttachCv: (Long, String) -> Unit,
    onOpenCv: (CandidateEntity, (String) -> Unit) -> Unit,
    onUpdate: (CandidateEntity, (Boolean) -> Unit) -> Unit,
    canManage: Boolean,
    onMoveNext: (CandidateEntity, (Boolean) -> Unit) -> Unit,
    onReject: (CandidateEntity, String, (Boolean) -> Unit) -> Unit,
    onReview: (Long, Long?) -> Unit,
    onRecordResponse: (CandidateEntity, Boolean, String?, (Boolean) -> Unit) -> Unit = { _, _, _, _ -> },
    onSchedule: (Long) -> Unit = {},
    onOpenInterview: (Long) -> Unit = {},
    onSetCompleted: (InterviewEntity, Boolean, (Boolean) -> Unit) -> Unit = { _, _, _ -> },
    onDelete: (CandidateEntity) -> Unit = {}
) {
    if (candidate == null) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("Chi tiết ứng viên", onBack = onBack)
            EmptyState(Icons.Rounded.Badge, "Không tìm thấy ứng viên", "Hồ sơ có thể đã được cập nhật.")
        }
        return
    }
    val context = LocalContext.current
    var showEdit by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var showDeclineDialog by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val isRejected = candidate.recruitmentStage == RecruitmentStage.REJECTED
    val myInterviews = WorkQueue.interviewsOf(candidate.id, interviews)
    val gateScorecards = candidateScorecards.ifEmpty { listOfNotNull(scorecard) }
    val advanceBlockReason = RecruitmentRules.advanceBlockReason(candidate, interviews, gateScorecards)
    val reviewBlockReason = RecruitmentRules.reviewBlockReason(candidate, interviews)
    val nextAction = WorkQueue.nextActionFor(candidate, interviews, gateScorecards)
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        onAttachCv(candidate.id, uri.toString())
    }
    fun handlePrimary() {
        if (pendingAction) return
        when (nextAction.action) {
            PrimaryAction.ATTACH_CV -> pdfLauncher.launch(arrayOf("application/pdf"))
            PrimaryAction.SCHEDULE -> onSchedule(candidate.id)
            PrimaryAction.VIEW_INTERVIEW -> nextAction.interviewId?.let(onOpenInterview)
            PrimaryAction.REVIEW -> onReview(candidate.id, WorkQueue.latestCompleted(candidate.id, interviews)?.id)
            PrimaryAction.COMPLETE_AND_REVIEW -> {
                val target = myInterviews.firstOrNull { it.id == nextAction.interviewId } ?: return
                pendingAction = true
                onSetCompleted(target, true) { ok ->
                    pendingAction = false
                    if (ok) onReview(candidate.id, target.id)
                }
            }
            PrimaryAction.ADVANCE -> {
                pendingAction = true
                onMoveNext(candidate) { pendingAction = false }
            }
            PrimaryAction.RECORD_RESPONSE -> {
                pendingAction = true
                onRecordResponse(candidate, true, null) { pendingAction = false }
            }
            // DECIDE xuất hiện khi thiếu kết luận Hire/Strong Hire nên không thể gọi
            // chuyển sang Offer (bị chặn bởi chính điều kiện đó): dẫn tới xử lý đánh giá.
            PrimaryAction.DECIDE -> onReview(candidate.id, WorkQueue.latestCompleted(candidate.id, interviews)?.id)
            PrimaryAction.NONE -> Unit
        }
    }
    val primaryLabel = when (nextAction.action) {
        PrimaryAction.ATTACH_CV -> "Bổ sung CV"
        PrimaryAction.ADVANCE -> "Chuyển sang ${candidate.recruitmentStage.next().label}"
        PrimaryAction.SCHEDULE -> "Đặt lịch phỏng vấn"
        PrimaryAction.VIEW_INTERVIEW -> "Xem lịch phỏng vấn"
        PrimaryAction.COMPLETE_AND_REVIEW -> "Hoàn thành & đánh giá"
        PrimaryAction.REVIEW -> "Viết đánh giá"
        PrimaryAction.DECIDE -> "Xem lại đánh giá"
        PrimaryAction.RECORD_RESPONSE -> "Ghi nhận đồng ý offer"
        PrimaryAction.NONE -> null
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("", onBack = onBack, action = {
            if (canManage) HeaderAction(Icons.Rounded.Edit, "Sửa hồ sơ") { showEdit = true }
            HeaderOverflow(buildList {
                if (scorecard != null || reviewBlockReason == null) add("Đánh giá ứng viên" to { onReview(candidate.id, null) })
                if (canManage) add("Đính kèm CV" to { pdfLauncher.launch(arrayOf("application/pdf")) })
                if (canManage && isRejected) add("Xóa ứng viên" to { showDeleteDialog = true })
            })
        })
        Box(Modifier.weight(1f)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        InitialAvatar(candidate.name, Modifier.size(64.dp))
                        Spacer(Modifier.size(14.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(candidate.name, style = MaterialTheme.typography.titleLarge)
                            Text(candidate.position, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            StagePill(candidate.recruitmentStage)
                        }
                    }

                    InfoCard(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(nextAction.headline, style = MaterialTheme.typography.titleMedium)
                            if (nextAction.detail != null) {
                                Text(nextAction.detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (primaryLabel != null && canManage) {
                                Button(
                                    shape = RoundedCornerShape(9.dp),
                                    onClick = ::handlePrimary,
                                    enabled = !pendingAction,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (pendingAction) "Đang xử lý..." else primaryLabel, modifier = Modifier.weight(1f))
                                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null)
                                }
                            }
                            if (advanceBlockReason != null && nextAction.action != PrimaryAction.NONE) {
                                Text(advanceBlockReason, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Column(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                            ContactLine(Icons.Rounded.Email, candidate.email.ifBlank { "Chưa cập nhật email" })
                            ContactLine(Icons.Rounded.Phone, candidate.phone.ifBlank { "Chưa cập nhật số điện thoại" })
                            ContactLine(Icons.Rounded.Work, "${candidate.experienceYears} năm kinh nghiệm")

                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionTitle("Kỹ năng")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                            candidate.skillList.forEach { skill ->
                                Text(skill, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 10.dp, vertical = 6.dp))
                            }
                        }

                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionTitle("Kinh nghiệm làm việc")
                        InfoCard(Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(candidate.position, style = MaterialTheme.typography.titleMedium)
                                Text("${candidate.experienceYears} năm kinh nghiệm", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Xem quá trình làm việc chi tiết trong CV đính kèm.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionTitle("Ghi chú nội bộ")
                        InfoCard(Modifier.fillMaxWidth()) {
                            Text(candidate.note.ifBlank { "Chưa có ghi chú nội bộ." }, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionTitle("CV đính kèm")
                        InfoCard(Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFFFE7E7)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Description, null, tint = Color(0xFFC92F3F))
                                }
                                Spacer(Modifier.size(10.dp))
                                Column(Modifier.weight(1f)) {
                                    val hasCv = candidate.cvUri != null || candidate.remoteCvPath != null
                                    val displayName = candidate.cvFileName?.takeIf { it.isNotBlank() }
                                        ?: "${candidate.name.replace(" ", "_")}_CV.pdf"
                                    Text(if (!hasCv) "Chưa có CV" else displayName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(if (!hasCv) "Chọn file PDF từ điện thoại" else if (candidate.cvUri != null) "Đã lưu trên thiết bị" else "Đã lưu trên cloud", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (candidate.cvUri != null || candidate.remoteCvPath != null) {
                                    Icon(
                                        Icons.Rounded.Download,
                                        "Mở CV",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(38.dp).clip(CircleShape).clickable {
                                            onOpenCv(candidate) { uri ->
                                                val parsed = uri.toUri()
                                                val intent = Intent(Intent.ACTION_VIEW, parsed).apply {
                                                    setDataAndType(parsed, "application/pdf")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                try { context.startActivity(intent) } catch (_: ActivityNotFoundException) {
                                                    scope.launch { snackbar.showSnackbar("Thiết bị chưa có ứng dụng đọc PDF") }
                                                }
                                            }
                                        }.padding(8.dp)
                                    )
                                }
                            }
                        }
                        if (canManage) OutlinedButton(shape = RoundedCornerShape(9.dp), onClick = { pdfLauncher.launch(arrayOf("application/pdf")) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Rounded.AttachFile, null)
                            Spacer(Modifier.size(7.dp))
                            Text(if (candidate.cvUri == null && candidate.remoteCvPath == null) "Đính kèm CV PDF" else "Thay CV")
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionTitle(
                            "Lịch phỏng vấn",
                            actionText = if (canManage && candidate.recruitmentStage == RecruitmentStage.INTERVIEW) "Đặt lịch" else null,
                            onAction = { onSchedule(candidate.id) }
                        )
                        if (myInterviews.isEmpty()) {
                            InfoCard(Modifier.fillMaxWidth()) {
                                Text(
                                    if (candidate.recruitmentStage == RecruitmentStage.INTERVIEW) "Chưa có buổi nào. Đặt lịch ngay trên hồ sơ."
                                    else "Chưa có lịch phỏng vấn.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            myInterviews.forEach { interview ->
                                InfoCard(Modifier.fillMaxWidth().clickable { onOpenInterview(interview.id) }) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Text(interview.round, style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.forLanguageTag("vi-VN")).format(Date(interview.scheduledAt)) +
                                                    " · ${interview.interviewer}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                interview.interviewStatus.label,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = when (interview.interviewStatus) {
                                                    com.hireflow.app.data.InterviewStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                                                    com.hireflow.app.data.InterviewStatus.SCHEDULED -> MaterialTheme.colorScheme.error
                                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        }
                                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    if (scorecard != null) {
                        InfoCard(Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.RateReview, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.size(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Đã có phiếu đánh giá", style = MaterialTheme.typography.titleMedium)
                                    Text("${scorecard.conclusion} · ${"%.1f".format(scorecard.average)}/5", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    if (histories.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionTitle("Lịch sử chuyển vòng")
                            histories.take(5).forEach { history ->
                                val from = runCatching { RecruitmentStage.valueOf(history.fromStage).label }.getOrDefault(history.fromStage)
                                val to = runCatching { RecruitmentStage.valueOf(history.toStage).label }.getOrDefault(history.toStage)
                                InfoCard(Modifier.fillMaxWidth()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("$from → $to", style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("vi-VN")).format(Date(history.changedAt)),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    FilledTonalButton(
                        shape = RoundedCornerShape(9.dp),
                        onClick = { onReview(candidate.id, null) },
                        enabled = scorecard != null || reviewBlockReason == null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.RateReview, null)
                        Spacer(Modifier.size(7.dp))
                        Text(if (scorecard == null) "Mở Blind Review & chấm điểm" else "Xem lại đánh giá")
                    }
                    if (scorecard == null && reviewBlockReason != null) {
                        Text(reviewBlockReason, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                    }
                    if (canManage && candidate.recruitmentStage == RecruitmentStage.OFFER) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionTitle("Phản hồi offer")
                            InfoCard(Modifier.fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    if (candidate.offerSentAt != null) {
                                        Text(
                                            "Đã gửi offer: ${
                                                SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN")).format(Date(candidate.offerSentAt))
                                            }",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    Text(
                                        when (candidate.offerResponse) {
                                            com.hireflow.app.data.OfferResponse.ACCEPTED.name -> "Ứng viên đã đồng ý. Chuyển sang Đã tuyển để kết thúc."
                                            else -> "Ghi nhận kết quả HR đã trao đổi bên ngoài. App không giả lập gửi email."
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    shape = RoundedCornerShape(9.dp),
                                    onClick = {
                                        pendingAction = true
                                        onRecordResponse(candidate, true, null) { pendingAction = false }
                                    },
                                    enabled = !pendingAction && candidate.offerResponse != com.hireflow.app.data.OfferResponse.ACCEPTED.name,
                                    modifier = Modifier.weight(1f)
                                ) { Text(if (pendingAction) "Đang lưu..." else "Ứng viên đồng ý") }
                                OutlinedButton(
                                    shape = RoundedCornerShape(9.dp),
                                    onClick = { showDeclineDialog = true },
                                    enabled = !pendingAction,
                                    modifier = Modifier.weight(1f)
                                ) { Text("Ứng viên từ chối", color = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                    if (canManage && isRejected && candidate.closeReason != null) {
                        InfoCard(Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Lý do đóng hồ sơ", style = MaterialTheme.typography.titleMedium)
                                Text(candidate.closeReason, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    if (canManage && candidate.recruitmentStage != RecruitmentStage.HIRED && candidate.recruitmentStage != RecruitmentStage.REJECTED) {
                        OutlinedButton(
                            shape = RoundedCornerShape(9.dp),
                            onClick = { showRejectDialog = true },
                            enabled = !pendingAction,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Từ chối ứng viên", color = MaterialTheme.colorScheme.error) }
                    }
                    if (canManage && isRejected) {
                        OutlinedButton(
                            shape = RoundedCornerShape(9.dp),
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Xóa ứng viên này", color = MaterialTheme.colorScheme.error) }
                        Text(
                            "Ứng viên tiềm năng thì giữ lại để xem xét đợt sau, ứng viên bị loại hẳn thì xóa.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.size(28.dp))
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
        if (primaryLabel != null && canManage) {
            androidx.compose.material3.Surface(shadowElevation = 4.dp) {
                Box(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Button(
                        shape = RoundedCornerShape(9.dp),
                        onClick = ::handlePrimary,
                        enabled = !pendingAction,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (pendingAction) "Đang xử lý..." else primaryLabel, modifier = Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null)
                    }
                }
            }
        }

    }

    if (showEdit) EditCandidateDialog(
        candidate = candidate,
        onDismiss = { showEdit = false },
        onSave = { updated, onError ->
            onUpdate(updated) { ok ->
                if (ok) showEdit = false else onError("Không thể cập nhật hồ sơ.")
            }
        }
    )

    if (showRejectDialog) CloseReasonDialog(
        title = "Từ chối ${candidate.name}?",
        confirmLabel = "Từ chối",
        onDismiss = { showRejectDialog = false },
        onConfirm = { reason ->
            pendingAction = true
            onReject(candidate, reason) { ok ->
                pendingAction = false
                if (ok) showRejectDialog = false
            }
        }
    )

    if (showDeclineDialog) CloseReasonDialog(
        title = "Ứng viên từ chối offer?",
        confirmLabel = "Ghi nhận từ chối",
        onDismiss = { showDeclineDialog = false },
        onConfirm = { reason ->
            pendingAction = true
            onRecordResponse(candidate, false, reason) { ok ->
                pendingAction = false
                if (ok) showDeclineDialog = false
            }
        }
    )

    if (showDeleteDialog) AlertDialog(
        onDismissRequest = { showDeleteDialog = false },
        title = { Text("Xóa ${candidate.name}?") },
        text = { Text("Hồ sơ bị từ chối sẽ bị xóa vĩnh viễn cùng lịch, đánh giá và CV. Nếu còn muốn xem xét đợt sau thì bấm Giữ lại.") },
        confirmButton = {
            Button(
                shape = RoundedCornerShape(9.dp),
                onClick = {
                    onDelete(candidate)
                    showDeleteDialog = false
                    onBack()
                },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Xóa") }
        },
        dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Giữ lại") } }
    )
}

@Composable
private fun CloseReasonDialog(title: String, confirmLabel: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var reason by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Ghi rõ lý do để phân biệt HR loại, ứng viên rút và từ chối offer. Lịch còn hiệu lực sẽ được hủy, lịch sử giữ lại.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    reason,
                    { reason = it },
                    label = { Text("Lý do *") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                shape = RoundedCornerShape(9.dp),
                enabled = reason.isNotBlank(),
                onClick = { onConfirm(reason.trim()) },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
private fun ContactLine(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(11.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EditCandidateDialog(candidate: CandidateEntity, onDismiss: () -> Unit, onSave: (CandidateEntity, (String) -> Unit) -> Unit) {
    var name by rememberSaveable(candidate.id) { mutableStateOf(candidate.name) }
    var position by rememberSaveable(candidate.id) { mutableStateOf(candidate.position) }
    var email by rememberSaveable(candidate.id) { mutableStateOf(candidate.email) }
    var phone by rememberSaveable(candidate.id) { mutableStateOf(candidate.phone) }
    var experience by rememberSaveable(candidate.id) { mutableStateOf(candidate.experienceYears.toString()) }
    var skills by rememberSaveable(candidate.id) { mutableStateOf(candidate.skills) }
    var note by rememberSaveable(candidate.id) { mutableStateOf(candidate.note) }
    var saving by rememberSaveable(candidate.id) { mutableStateOf(false) }
    var saveError by rememberSaveable(candidate.id) { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Chỉnh sửa hồ sơ") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EditField(name, { name = it }, "Họ và tên")
                EditField(position, { position = it }, "Vị trí ứng tuyển")
                EditField(email, { email = it }, "Email")
                EditField(phone, { phone = it }, "Số điện thoại")
                EditField(experience, { experience = it.filter(Char::isDigit) }, "Số năm kinh nghiệm")
                EditField(skills, { skills = it }, "Kỹ năng")
                OutlinedTextField(note, { note = it }, label = { Text("Ghi chú nội bộ") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                if (saveError != null) {
                    Text(saveError!!, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(shape = RoundedCornerShape(9.dp),
                enabled = !saving && name.isNotBlank() && position.isNotBlank() && skills.isNotBlank() && experience.toIntOrNull() != null,
                onClick = {
                    saving = true
                    saveError = null
                    onSave(candidate.copy(name = name.trim(), position = position.trim(), email = email.trim(), phone = phone.trim(), experienceYears = experience.toIntOrNull() ?: 0, skills = skills.trim(), note = note.trim())) { message ->
                        saving = false
                        saveError = message
                    }
                }
            ) { Text(if (saving) "Đang lưu..." else "Lưu thay đổi") }
        },
        dismissButton = { TextButton(onClick = { if (!saving) onDismiss() }) { Text("Hủy") } }
    )
}

@Composable
private fun EditField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(value, onValueChange, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth())
}
