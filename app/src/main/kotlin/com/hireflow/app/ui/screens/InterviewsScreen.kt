package com.hireflow.app.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.VideoCall
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hireflow.app.data.CandidateEntity
import com.hireflow.app.data.InterviewEntity
import com.hireflow.app.reminder.scheduleInterviewReminder
import com.hireflow.app.ui.components.EmptyState
import com.hireflow.app.ui.components.InitialAvatar
import com.hireflow.app.ui.components.ScreenHeader
import com.hireflow.app.ui.theme.Azure
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class InterviewTab(val label: String) { TODAY("Hôm nay"), UPCOMING("Sắp tới"), PAST("Đã qua") }

@Composable
fun InterviewsScreen(
    candidates: List<CandidateEntity>,
    interviews: List<InterviewEntity>,
    onAddInterview: (InterviewEntity, (Long) -> Unit) -> Unit,
    notificationsEnabled: Boolean,
    canSchedule: Boolean,
    onOpenCandidate: (Long) -> Unit,
    onReview: (Long) -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(InterviewTab.TODAY.name) }
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val now = System.currentTimeMillis()
    val filtered = interviews.filter { interview ->
        when (InterviewTab.valueOf(selectedTab)) {
            InterviewTab.TODAY -> isSameDay(interview.scheduledAt, now)
            InterviewTab.UPCOMING -> interview.scheduledAt > endOfToday()
            InterviewTab.PAST -> interview.scheduledAt < startOfToday()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (canSchedule) FloatingActionButton(onClick = { showDialog = true }) { Icon(Icons.Rounded.Add, "Thêm lịch phỏng vấn") }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScreenHeader("Lịch phỏng vấn", "Quản lý lịch và nhắc hẹn")
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InterviewTab.entries.forEach { tab ->
                    FilterChip(selected = selectedTab == tab.name, onClick = { selectedTab = tab.name }, label = { Text(tab.label) }, modifier = Modifier.weight(1f))
                }
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filtered.isEmpty()) {
                    item { EmptyState(Icons.Rounded.CalendarMonth, "Chưa có lịch phỏng vấn", "Nhấn + để tạo lịch mới cho ứng viên.") }
                } else {
                    items(filtered, key = { it.id }) { interview ->
                        InterviewCard(interview, onOpenCandidate, onReview)
                    }
                }
            }
        }
    }

    if (showDialog) ScheduleInterviewDialog(
        candidates = candidates,
        onDismiss = { showDialog = false },
        onSave = { interview ->
            onAddInterview(interview) { id ->
                if (notificationsEnabled) {
                    scheduleInterviewReminder(context, id, interview.candidateName, interview.position, interview.scheduledAt)
                }
            }
            showDialog = false
        }
    )
}

@Composable
private fun InterviewCard(interview: InterviewEntity, onOpenCandidate: (Long) -> Unit, onReview: (Long) -> Unit) {
    val time = SimpleDateFormat("HH:mm", Locale.forLanguageTag("vi-VN")).format(Date(interview.scheduledAt))
    val end = SimpleDateFormat("HH:mm", Locale.forLanguageTag("vi-VN")).format(Date(interview.scheduledAt + interview.durationMinutes * 60_000L))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 10.dp)) {
            Text(time, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(end, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(Modifier.padding(top = 7.dp).size(9.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        }
        Spacer(Modifier.size(12.dp))
        androidx.compose.material3.Card(
            shape = RoundedCornerShape(16.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .24f)),
            elevation = androidx.compose.material3.CardDefaults.cardElevation(0.dp),
            modifier = Modifier.weight(1f).clickable { onOpenCandidate(interview.candidateId) }
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    InitialAvatar(interview.candidateName, Modifier.size(42.dp))
                    Spacer(Modifier.size(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(interview.candidateName, style = MaterialTheme.typography.titleMedium)
                        Text(interview.position, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(interview.format, color = if (interview.format == "Online") Azure else MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelMedium, modifier = Modifier.clip(CircleShape).background((if (interview.format == "Online") Azure else MaterialTheme.colorScheme.tertiary).copy(alpha = .1f)).padding(horizontal = 8.dp, vertical = 4.dp))
                }
                Text("${interview.round} · ${interview.interviewer}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FilledTonalButton(onClick = { onReview(interview.candidateId) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Mở phiếu đánh giá")
                }
            }
        }
    }
}

@Composable
private fun ScheduleInterviewDialog(candidates: List<CandidateEntity>, onDismiss: () -> Unit, onSave: (InterviewEntity) -> Unit) {
    var selectedId by rememberSaveable { mutableStateOf(candidates.firstOrNull()?.id) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var date by rememberSaveable { mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(System.currentTimeMillis() + 86_400_000))) }
    var time by rememberSaveable { mutableStateOf("09:30") }
    var format by rememberSaveable { mutableStateOf("Online") }
    var interviewer by rememberSaveable { mutableStateOf("Trần Hoàng Nam") }
    var round by rememberSaveable { mutableStateOf("Vòng 1: HR") }
    var checklist by rememberSaveable { mutableStateOf("Giới thiệu bản thân, Kinh nghiệm liên quan, Tình huống thực tế") }
    val candidate = candidates.firstOrNull { it.id == selectedId }
    val scheduledAt = parseDateTime(date, time)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tạo lịch phỏng vấn") },
        text = {
            Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(candidate?.let { "${it.name} · ${it.position}" } ?: "Chọn ứng viên", modifier = Modifier.weight(1f))
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        candidates.forEach { item -> DropdownMenuItem(text = { Text("${item.name} · ${item.position}") }, onClick = { selectedId = item.id; expanded = false }) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(date, { date = it }, label = { Text("Ngày") }, supportingText = { Text("dd/MM/yyyy") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(time, { time = it }, label = { Text("Giờ") }, supportingText = { Text("HH:mm") }, singleLine = true, modifier = Modifier.weight(.7f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Online", "Onsite").forEach { item -> FilterChip(selected = format == item, onClick = { format = item }, label = { Text(item) }) }
                }
                OutlinedTextField(interviewer, { interviewer = it }, label = { Text("Interviewer") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(round, { round = it }, label = { Text("Vòng phỏng vấn") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(checklist, { checklist = it }, label = { Text("Checklist câu hỏi") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                Text("Ứng dụng sẽ nhắc trước 15 phút.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(
                enabled = candidate != null && scheduledAt != null && interviewer.isNotBlank(),
                onClick = {
                    if (candidate != null && scheduledAt != null) onSave(InterviewEntity(candidateId = candidate.id, candidateName = candidate.name, position = candidate.position, scheduledAt = scheduledAt, format = format, interviewer = interviewer, round = round, checklist = checklist))
                }
            ) { Text("Lưu lịch") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

private fun parseDateTime(date: String, time: String): Long? = runCatching {
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).apply { isLenient = false }.parse("$date $time")?.time
}.getOrNull()

private fun startOfToday(): Long = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
private fun endOfToday(): Long = startOfToday() + 86_400_000 - 1
private fun isSameDay(a: Long, b: Long): Boolean = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(a)) == SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(b))
