package com.hireflow.app.ui.screens

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
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocationOn
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hireflow.app.data.CandidateEntity
import com.hireflow.app.data.RecruitmentStage
import com.hireflow.app.data.ScorecardEntity
import com.hireflow.app.ui.components.EmptyState
import com.hireflow.app.ui.components.InfoCard
import com.hireflow.app.ui.components.InitialAvatar
import com.hireflow.app.ui.components.ScreenHeader
import com.hireflow.app.ui.components.SectionTitle
import com.hireflow.app.ui.components.StagePill
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun CandidateDetailScreen(
    candidate: CandidateEntity?,
    scorecard: ScorecardEntity?,
    onBack: () -> Unit,
    onAttachCv: (Long, String) -> Unit,
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
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        onAttachCv(candidate.id, uri.toString())
        scope.launch { snackbar.showSnackbar("Đã đính kèm CV PDF") }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            ScreenHeader(
                "Chi tiết ứng viên",
                onBack = onBack,
                action = {
                    if (canManage) androidx.compose.material3.IconButton(onClick = { showEdit = true }) {
                        Icon(Icons.Rounded.Edit, "Sửa hồ sơ")
                    }
                }
            )
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    InitialAvatar(candidate.name, Modifier.size(72.dp))
                    Spacer(Modifier.size(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(candidate.name, style = MaterialTheme.typography.headlineMedium)
                        Text(candidate.position, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        StagePill(candidate.recruitmentStage)
                    }
                }

                InfoCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                        ContactLine(Icons.Rounded.Email, candidate.email.ifBlank { "Chưa cập nhật email" })
                        ContactLine(Icons.Rounded.Phone, candidate.phone.ifBlank { "Chưa cập nhật số điện thoại" })
                        ContactLine(Icons.Rounded.Work, "${candidate.experienceYears} năm kinh nghiệm")
                        ContactLine(Icons.Rounded.LocationOn, "Hà Nội")
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle("Kỹ năng")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                        candidate.skillList.take(4).forEach { skill ->
                            Text(skill, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                    }
                    if (candidate.skillList.size > 4) Text("+ ${candidate.skillList.size - 4} kỹ năng khác", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Kinh nghiệm làm việc")
                    InfoCard(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("${candidate.position} — TechSoft JSC", style = MaterialTheme.typography.titleMedium)
                            Text("06/2021 – Hiện tại (${candidate.experienceYears} năm)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("• Phát triển sản phẩm và phối hợp cùng team\n• Áp dụng best practices trong công việc\n• Tối ưu hiệu năng và xử lý vấn đề", style = MaterialTheme.typography.bodyMedium)
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
                                Text(if (candidate.cvUri == null) "Chưa có CV" else "${candidate.name.replace(" ", "_")}_CV.pdf", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(if (candidate.cvUri == null) "Chọn file PDF từ điện thoại" else "Đã lưu quyền truy cập", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (candidate.cvUri != null) {
                                Icon(
                                    Icons.Rounded.Download,
                                    "Mở CV",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(38.dp).clip(CircleShape).clickable {
                                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(candidate.cvUri)).apply {
                                            setDataAndType(android.net.Uri.parse(candidate.cvUri), "application/pdf")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        try { context.startActivity(intent) } catch (_: ActivityNotFoundException) { scope.launch { snackbar.showSnackbar("Thiết bị chưa có ứng dụng đọc PDF") } }
                                    }.padding(8.dp)
                                )
                            }
                        }
                    }
                    if (canManage) OutlinedButton(onClick = { pdfLauncher.launch(arrayOf("application/pdf")) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.AttachFile, null)
                        Spacer(Modifier.size(7.dp))
                        Text(if (candidate.cvUri == null) "Đính kèm CV PDF" else "Thay CV")
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

                FilledTonalButton(onClick = { onReview(candidate.id) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.RateReview, null)
                    Spacer(Modifier.size(7.dp))
                    Text(if (scorecard == null) "Mở Blind Review & chấm điểm" else "Xem lại đánh giá")
                }
                if (canManage && candidate.recruitmentStage != RecruitmentStage.HIRED && candidate.recruitmentStage != RecruitmentStage.REJECTED) {
                    Button(onClick = { onMoveNext(candidate) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Chuyển sang ${candidate.recruitmentStage.next().label}", modifier = Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null)
                    }
                    OutlinedButton(onClick = { onReject(candidate) }, modifier = Modifier.fillMaxWidth()) { Text("Từ chối ứng viên", color = MaterialTheme.colorScheme.error) }
                }
                Spacer(Modifier.size(28.dp))
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
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
            Button(
                enabled = name.isNotBlank() && position.isNotBlank(),
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
