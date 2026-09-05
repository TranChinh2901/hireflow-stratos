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
import com.hireflow.app.domain.RecruitmentRules
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
    interviews: List<InterviewEntity>,
    histories: List<StageHistoryEntity>,
    onBack: () -> Unit,
    onAttachCv: (Long, String) -> Unit,
    onOpenCv: (CandidateEntity, (String) -> Unit) -> Unit,
    onUpdate: (CandidateEntity) -> Unit,
    canManage: Boolean,
    onMoveNext: (CandidateEntity) -> Unit,
    onReject: (CandidateEntity) -> Unit,
    onReview: (Long) -> Unit
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
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val advanceBlockReason = RecruitmentRules.advanceBlockReason(candidate, interviews, listOfNotNull(scorecard))
    val reviewBlockReason = RecruitmentRules.reviewBlockReason(candidate, interviews)
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        onAttachCv(candidate.id, uri.toString())
        scope.launch { snackbar.showSnackbar("Đã đính kèm CV PDF") }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("", onBack = onBack, action = {
            if (canManage) HeaderAction(Icons.Rounded.Edit, "Sửa hồ sơ") { showEdit = true }
            HeaderOverflow(buildList {
                if (scorecard != null || reviewBlockReason == null) add("Đánh giá ứng viên" to { onReview(candidate.id) })
                if (canManage) add("Đính kèm CV" to { pdfLauncher.launch(arrayOf("application/pdf")) })
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
                                    Text(if (!hasCv) "Chưa có CV" else "${candidate.name.replace(" ", "_")}_CV.pdf", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
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

                    FilledTonalButton(
                        shape = RoundedCornerShape(9.dp),
                        onClick = { onReview(candidate.id) },
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
                    if (canManage && candidate.recruitmentStage != RecruitmentStage.HIRED && candidate.recruitmentStage != RecruitmentStage.REJECTED) {
                        Button(shape = RoundedCornerShape(9.dp), onClick = { onMoveNext(candidate) }, enabled = advanceBlockReason == null, modifier = Modifier.fillMaxWidth()) {
                            Text("Chuyển sang ${candidate.recruitmentStage.next().label}", modifier = Modifier.weight(1f))
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null)
                        }
                        if (advanceBlockReason != null) {
                            Text(advanceBlockReason, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                        }
                        OutlinedButton(shape = RoundedCornerShape(9.dp), onClick = { onReject(candidate) }, modifier = Modifier.fillMaxWidth()) { Text("Từ chối ứng viên", color = MaterialTheme.colorScheme.error) }
                    }
                    Spacer(Modifier.size(28.dp))
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }

    }

    if (showEdit) EditCandidateDialog(
        candidate = candidate,
        onDismiss = { showEdit = false },
        onSave = {
            onUpdate(it)
            showEdit = false
            scope.launch { snackbar.showSnackbar("Đã cập nhật hồ sơ ứng viên") }
        }
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
private fun EditCandidateDialog(candidate: CandidateEntity, onDismiss: () -> Unit, onSave: (CandidateEntity) -> Unit) {
    var name by rememberSaveable(candidate.id) { mutableStateOf(candidate.name) }
    var position by rememberSaveable(candidate.id) { mutableStateOf(candidate.position) }
    var email by rememberSaveable(candidate.id) { mutableStateOf(candidate.email) }
    var phone by rememberSaveable(candidate.id) { mutableStateOf(candidate.phone) }
    var experience by rememberSaveable(candidate.id) { mutableStateOf(candidate.experienceYears.toString()) }
    var skills by rememberSaveable(candidate.id) { mutableStateOf(candidate.skills) }
    var note by rememberSaveable(candidate.id) { mutableStateOf(candidate.note) }
    AlertDialog(
        onDismissRequest = onDismiss,
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
            }
        },
        confirmButton = {
            Button(shape = RoundedCornerShape(9.dp),
                enabled = name.isNotBlank() && position.isNotBlank() && skills.isNotBlank() && experience.toIntOrNull() != null,
                onClick = { onSave(candidate.copy(name = name.trim(), position = position.trim(), email = email.trim(), phone = phone.trim(), experienceYears = experience.toIntOrNull() ?: 0, skills = skills.trim(), note = note.trim())) }
            ) { Text("Lưu thay đổi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
private fun EditField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(value, onValueChange, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth())
}
