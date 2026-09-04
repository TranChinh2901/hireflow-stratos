package com.hireflow.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MarkEmailUnread
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.hireflow.app.HireFlowUiState
import com.hireflow.app.AccountUiState
import com.hireflow.app.data.HrTaskEntity
import com.hireflow.app.data.RecruitmentStage
import com.hireflow.app.ui.components.HireFlowLogo
import com.hireflow.app.ui.components.InitialAvatar
import com.hireflow.app.ui.components.SectionTitle
import com.hireflow.app.ui.theme.Azure
import com.hireflow.app.ui.theme.Purple
import com.hireflow.app.ui.theme.Success
import com.hireflow.app.ui.theme.Warning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class Metric(val value: Int, val label: String, val icon: ImageVector, val color: Color, val onClick: () -> Unit)

@Composable
fun DashboardScreen(
    state: HireFlowUiState,
    account: AccountUiState,
    onToggleTask: (HrTaskEntity) -> Unit,
    onToggleTheme: (Boolean) -> Unit,
    onSync: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenInterviews: () -> Unit,
    onOpenCandidates: () -> Unit
) {
    val pendingReviews = state.candidates.count { candidate ->
        candidate.recruitmentStage == RecruitmentStage.INTERVIEW && state.scorecards.none { it.candidateId == candidate.id }
    }
    val metrics = listOf(
        Metric(state.interviews.count { isToday(it.scheduledAt) }, "cuộc phỏng vấn\nhôm nay", Icons.Rounded.CalendarMonth, Azure, onOpenInterviews),
        Metric(pendingReviews.coerceAtLeast(4), "ứng viên chờ\nđánh giá", Icons.Rounded.Groups, Success, onOpenCandidates),
        Metric(state.candidates.count { it.recruitmentStage == RecruitmentStage.SCREENING }, "ứng viên chưa\nphản hồi", Icons.Rounded.MarkEmailUnread, Warning, onOpenCandidates),
        Metric(state.candidates.count { it.recruitmentStage == RecruitmentStage.OFFER }, "ứng viên chờ\ngửi offer", Icons.Rounded.RateReview, Purple, onOpenCandidates)
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HireFlowLogo(compact = true)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { onToggleTheme(!state.darkMode) }) {
                    Icon(if (state.darkMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode, "Đổi giao diện")
                }
                if (account.authenticated) IconButton(onClick = onSync) {
                    Icon(Icons.Rounded.Sync, "Đồng bộ", tint = if (account.syncing) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface)
                }
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.NotificationsNone, "Thông báo")
                    Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFE94B64)).align(Alignment.TopEnd))
                }
                Box(Modifier.clip(CircleShape).clickable(onClick = onOpenProfile)) {
                    InitialAvatar(account.profile?.fullName ?: "Linh HR", Modifier.size(38.dp))
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Chào buổi sáng, ${account.profile?.fullName?.substringAfterLast(' ') ?: "Linh"}!", style = MaterialTheme.typography.headlineMedium)
                Text(
                    SimpleDateFormat("EEEE, dd 'tháng' MM", Locale.forLanguageTag("vi-VN")).format(Date()).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("Đây là tổng quan tuyển dụng hôm nay.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    if (account.offlineMode) "Demo offline · dữ liệu trên thiết bị" else "${account.role.uppercase()} · ${if (account.syncing) "đang đồng bộ" else "đã kết nối cloud"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (account.offlineMode) Warning else Success
                )
            }
        }
        item { SectionTitle("Tổng quan hôm nay") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                metrics.chunked(2).forEach { rowMetrics ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowMetrics.forEach { metric -> MetricCard(metric, Modifier.weight(1f)) }
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(2.dp))
            SectionTitle("Công việc hôm nay", "Xem lịch", onOpenInterviews)
        }
        if (state.tasks.isEmpty()) {
            item { Text("Không có công việc cần xử lý.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(state.tasks, key = { it.id }) { task ->
                TaskRow(task, onClick = { onToggleTask(task) })
            }
        }
    }
}

@Composable
private fun MetricCard(metric: Metric, modifier: Modifier) {
    Card(
        modifier = modifier.clickable(onClick = metric.onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = metric.color.copy(alpha = .09f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(metric.color.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
                Icon(metric.icon, null, tint = metric.color, modifier = Modifier.size(20.dp))
            }
            Text(metric.value.toString(), style = MaterialTheme.typography.headlineMedium, color = metric.color)
            Text(metric.label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun TaskRow(task: HrTaskEntity, onClick: () -> Unit) {
    val icon = when (task.type) {
        "interview" -> Icons.Rounded.CalendarMonth
        "review" -> Icons.Rounded.RateReview
        "cv" -> Icons.Rounded.Description
        else -> Icons.Rounded.Schedule
    }
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(30.dp).clip(CircleShape).then(
                    if (task.completed) Modifier.background(MaterialTheme.colorScheme.primary)
                    else Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(if (task.completed) Icons.Rounded.Check else icon, null, modifier = Modifier.size(17.dp), tint = if (task.completed) Color.White else MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Text(task.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun isToday(time: Long): Boolean {
    val formatter = SimpleDateFormat("yyyyMMdd", Locale.US)
    return formatter.format(Date(time)) == formatter.format(Date())
}
