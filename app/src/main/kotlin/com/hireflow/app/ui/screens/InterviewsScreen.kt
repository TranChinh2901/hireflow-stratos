package com.hireflow.app.ui.screens

import com.hireflow.app.ui.components.HeaderAction
import com.hireflow.app.ui.components.HeaderOverflow
import com.hireflow.app.ui.components.LocalHeaderNavigation
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
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
import androidx.compose.material3.HorizontalDivider
import com.hireflow.app.ui.components.SectionTitle
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
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
    var selectedInterviewId by rememberSaveable { mutableStateOf<Long?>(null) }
    val navigate = LocalHeaderNavigation.current
    val context = LocalContext.current
    val now = System.currentTimeMillis()
    val filtered = interviews.filter { interview ->
        when (InterviewTab.valueOf(selectedTab)) {
            InterviewTab.TODAY -> isSameDay(interview.scheduledAt, now)
            InterviewTab.UPCOMING -> interview.scheduledAt > endOfToday()
            InterviewTab.PAST -> interview.scheduledAt < startOfToday()
        }
    }

    val selectedInterview = filtered.firstOrNull { it.id == selectedInterviewId } ?: filtered.minByOrNull { it.scheduledAt }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScreenHeader("Lịch phỏng vấn", onBack = { navigate("dashboard") }, action = {
                HeaderAction(Icons.Rounded.CalendarMonth, "Mở lịch phỏng vấn") {
                    if (canSchedule) showDialog = true else selectedTab = InterviewTab.TODAY.name
                }
            })
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InterviewTab.entries.forEach { tab ->
                    FilterChip(selected = selectedTab == tab.name, onClick = { selectedTab = tab.name }, label = { Text(tab.label) }, modifier = Modifier.weight(1f),
                        shape = CircleShape,
                        border = null,
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary))
                }
            }
            Text(
                SimpleDateFormat("EEEE, dd 'tháng' MM, yyyy", Locale.forLanguageTag("vi-VN")).format(Date()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filtered.isEmpty()) {
                    item { EmptyState(Icons.Rounded.CalendarMonth, "Chưa có lịch phỏng vấn", "Nhấn + để tạo lịch mới cho ứng viên.") }
                } else {
                    items(filtered.sortedBy { it.scheduledAt }, key = { it.id }) { interview ->
                        InterviewCard(interview, interview.id == selectedInterview?.id) { selectedInterviewId = interview.id }
                    }
                }
                if (canSchedule) item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        FilledTonalButton(shape = RoundedCornerShape(9.dp), onClick = { showDialog = true }) {
                            Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("Thêm lịch phỏng vấn")
                        }
                    }
                }
                selectedInterview?.let { selected ->
                    item {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionTitle("Chi tiết phỏng vấn")
                            InterviewDetailLine("Ứng viên", selected.candidateName)
                            InterviewDetailLine("Vị trí", selected.position)
                            InterviewDetailLine("Ngày", SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(selected.scheduledAt)))
                            InterviewDetailLine("Thời gian", SimpleDateFormat("HH:mm", Locale.US).format(Date(selected.scheduledAt)) + " · ${selected.durationMinutes} phút")
                            InterviewDetailLine("Hình thức", selected.format)
                            InterviewDetailLine("Interviewer", selected.interviewer)
                            InterviewDetailLine("Nhắc trước", if (notificationsEnabled) "15 phút" else "Đang tắt")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(shape = RoundedCornerShape(9.dp), onClick = { onOpenCandidate(selected.candidateId) }, modifier = Modifier.weight(1f)) { Text("Xem hồ sơ") }
                                Button(shape = RoundedCornerShape(9.dp), onClick = { onReview(selected.candidateId) }, modifier = Modifier.weight(1f)) { Text("Đánh giá") }
                            }
                        }
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
private fun InterviewCard(interview: InterviewEntity, selected: Boolean, onSelect: () -> Unit) {
    val time = SimpleDateFormat("HH:mm", Locale.forLanguageTag("vi-VN")).format(Date(interview.scheduledAt))
    val end = SimpleDateFormat("HH:mm", Locale.forLanguageTag("vi-VN")).format(Date(interview.scheduledAt + interview.durationMinutes * 60_000L))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 10.dp)) {
            Text(time, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(end, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(Modifier.padding(top = 7.dp).size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            Box(Modifier.size(1.dp, 50.dp).background(MaterialTheme.colorScheme.primaryContainer))
        }
        Spacer(Modifier.size(12.dp))
        androidx.compose.material3.Card(
            shape = RoundedCornerShape(9.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .55f) else MaterialTheme.colorScheme.outline),
            elevation = androidx.compose.material3.CardDefaults.cardElevation(0.dp),
            modifier = Modifier.weight(1f).clickable(onClick = onSelect)
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    InitialAvatar(interview.candidateName, Modifier.size(32.dp))
                    Spacer(Modifier.size(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(interview.candidateName, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(interview.position, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(interview.format, color = if (interview.format == "Online") Azure else MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelMedium, modifier = Modifier.clip(CircleShape).background((if (interview.format == "Online") Azure else MaterialTheme.colorScheme.tertiary).copy(alpha = .1f)).padding(horizontal = 8.dp, vertical = 4.dp))
                }
                Text(SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN")).format(Date(interview.scheduledAt)) + " · " + interview.round, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Interviewer: ${interview.interviewer}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun InterviewDetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, modifier = Modifier.weight(.8f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.bodyMedium)
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
                    OutlinedButton(shape = RoundedCornerShape(9.dp), onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
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
            Button(shape = RoundedCornerShape(9.dp),
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
