package com.hireflow.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hireflow.app.data.CandidateEntity
import com.hireflow.app.data.InterviewEntity
import com.hireflow.app.data.InterviewStatus
import com.hireflow.app.ui.components.EmptyState
import com.hireflow.app.ui.components.ScreenHeader
import com.hireflow.app.ui.components.SectionTitle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterviewDetailScreen(
    interview: InterviewEntity?,
    candidate: CandidateEntity?,
    canSchedule: Boolean,
    notificationsEnabled: Boolean,
    onBack: () -> Unit,
    onOpenCandidate: (Long) -> Unit,
    onReview: (candidateId: Long, interviewId: Long?) -> Unit,
    onSetCompleted: (InterviewEntity, Boolean, (Boolean) -> Unit) -> Unit,
    onReschedule: (Long, Long, (Boolean) -> Unit) -> Unit = { _, _, _ -> },
    onCancel: (InterviewEntity, (Boolean) -> Unit) -> Unit = { _, _ -> },
    onNoShow: (InterviewEntity, (Boolean) -> Unit) -> Unit = { _, _ -> }
) {
    var saving by rememberSaveable { mutableStateOf(false) }
    var showReschedule by rememberSaveable { mutableStateOf(false) }
    var showCancelConfirm by rememberSaveable { mutableStateOf(false) }
    val now = System.currentTimeMillis()
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Chi tiết phỏng vấn", onBack = onBack)
        if (interview == null) {
            EmptyState(Icons.Rounded.CalendarMonth, "Không tìm thấy lịch phỏng vấn", "Lịch có thể đã được cập nhật. Quay lại danh sách.")
            return@Column
        }
        val isScheduled = interview.interviewStatus == InterviewStatus.SCHEDULED
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionTitle("Buổi phỏng vấn")
            InterviewDetailLine("Ứng viên", interview.candidateName)
            InterviewDetailLine("Vị trí", interview.position)
            InterviewDetailLine("Ngày", SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(interview.scheduledAt)))
            InterviewDetailLine(
                "Thời gian",
                SimpleDateFormat("HH:mm", Locale.US).format(Date(interview.scheduledAt)) + " · ${interview.durationMinutes} phút"
            )
            InterviewDetailLine("Hình thức", interview.format)
            InterviewDetailLine("Vòng", interview.round)
            InterviewDetailLine("Interviewer", interview.interviewer)
            InterviewDetailLine("Checklist", interview.checklist.ifBlank { "Chưa có checklist" })
            InterviewDetailLine("Trạng thái", interview.interviewStatus.label)
            InterviewDetailLine("Nhắc trước", if (notificationsEnabled) "15 phút" else "Đang tắt")
            if (candidate == null) {
                Text(
                    "Hồ sơ ứng viên không còn trong phạm vi hiển thị.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    shape = RoundedCornerShape(9.dp),
                    onClick = { onOpenCandidate(interview.candidateId) },
                    modifier = Modifier.weight(1f)
                ) { Text("Xem hồ sơ") }
                Button(
                    shape = RoundedCornerShape(9.dp),
                    onClick = { onReview(interview.candidateId, interview.id) },
                    enabled = interview.interviewStatus == InterviewStatus.COMPLETED,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.CalendarMonth, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Đánh giá")
                }
            }
            if (interview.interviewStatus == InterviewStatus.COMPLETED) {
                Text(
                    "Phiếu của buổi này không thay phiếu buổi khác.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (canSchedule && isScheduled) {
                FilledTonalButton(
                    shape = RoundedCornerShape(9.dp),
                    onClick = {
                        saving = true
                        onSetCompleted(interview, true) { saving = false }
                    },
                    enabled = !saving && interview.scheduledAt <= now,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (saving) "Đang lưu..." else "Hoàn thành phỏng vấn")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        shape = RoundedCornerShape(9.dp),
                        onClick = { showReschedule = true },
                        enabled = !saving,
                        modifier = Modifier.weight(1f)
                    ) { Text("Đổi lịch") }
                    OutlinedButton(
                        shape = RoundedCornerShape(9.dp),
                        onClick = { showCancelConfirm = true },
                        enabled = !saving,
                        modifier = Modifier.weight(1f)
                    ) { Text("Hủy lịch", color = MaterialTheme.colorScheme.error) }
                }
                if (interview.scheduledAt <= now) {
                    OutlinedButton(
                        shape = RoundedCornerShape(9.dp),
                        onClick = {
                            saving = true
                            onNoShow(interview) { saving = false }
                        },
                        enabled = !saving,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Ghi vắng mặt") }
                }
            }
            Spacer(Modifier.size(16.dp))
        }
    }

    if (showReschedule && interview != null) {
        RescheduleDialog(
            interview = interview,
            onDismiss = { showReschedule = false },
            onSave = { newTime ->
                saving = true
                onReschedule(interview.id, newTime) { ok ->
                    saving = false
                    if (ok) showReschedule = false
                }
            }
        )
    }

    if (showCancelConfirm && interview != null) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            title = { Text("Hủy buổi phỏng vấn?") },
            text = { Text("Buổi ${interview.round} sẽ chuyển sang Đã hủy và không giữ chỗ nữa. Lịch sử vẫn được lưu.") },
            confirmButton = {
                Button(
                    shape = RoundedCornerShape(9.dp),
                    onClick = {
                        saving = true
                        onCancel(interview) { ok ->
                            saving = false
                            if (ok) showCancelConfirm = false
                        }
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(if (saving) "Đang hủy..." else "Hủy lịch") }
            },
            dismissButton = { TextButton(onClick = { showCancelConfirm = false }) { Text("Giữ lại") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RescheduleDialog(interview: InterviewEntity, onDismiss: () -> Unit, onSave: (Long) -> Unit) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var dateMillis by rememberSaveable { mutableStateOf(interview.scheduledAt) }
    var hour by rememberSaveable {
        mutableStateOf(Calendar.getInstance().apply { timeInMillis = interview.scheduledAt }.get(Calendar.HOUR_OF_DAY))
    }
    var minute by rememberSaveable {
        mutableStateOf(Calendar.getInstance().apply { timeInMillis = interview.scheduledAt }.get(Calendar.MINUTE))
    }
    val newTime = Calendar.getInstance().apply {
        timeInMillis = dateMillis
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Đổi lịch phỏng vấn") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(shape = RoundedCornerShape(9.dp), onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                        Text(SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(dateMillis)), modifier = Modifier.weight(1f))
                    }
                    OutlinedButton(shape = RoundedCornerShape(9.dp), onClick = { showTimePicker = true }, modifier = Modifier.weight(.7f)) {
                        Text(String.format(Locale.US, "%02d:%02d", hour, minute), modifier = Modifier.weight(1f))
                    }
                }
                Text(
                    "Giờ mới: ${SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.forLanguageTag("vi-VN")).format(Date(newTime))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(shape = RoundedCornerShape(9.dp), onClick = { onSave(newTime) }) { Text("Lưu lịch mới") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { dateMillis = it }
                    showDatePicker = false
                }) { Text("Chọn") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Hủy") } }
        ) { DatePicker(state = pickerState) }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Chọn giờ phỏng vấn") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    hour = timeState.hour
                    minute = timeState.minute
                    showTimePicker = false
                }) { Text("Chọn") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Hủy") } }
        )
    }
}

@Composable
private fun InterviewDetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, modifier = Modifier.weight(.8f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.bodyMedium)
    }
}
