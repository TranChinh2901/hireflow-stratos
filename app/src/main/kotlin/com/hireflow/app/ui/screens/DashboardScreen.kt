package com.hireflow.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.MarkEmailUnread
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material.icons.rounded.Schedule
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.hireflow.app.ui.components.ScreenHeader
import com.hireflow.app.ui.components.HeaderAction
import com.hireflow.app.HireFlowUiState
import com.hireflow.app.AccountUiState
import com.hireflow.app.data.RecruitmentStage
import com.hireflow.app.domain.WorkKind
import com.hireflow.app.domain.WorkQueue
import com.hireflow.app.ui.components.InitialAvatar
import com.hireflow.app.ui.components.SectionTitle
import com.hireflow.app.ui.theme.Azure
import com.hireflow.app.ui.theme.Purple
import com.hireflow.app.ui.theme.Success
import com.hireflow.app.ui.theme.Warning
import java.util.Calendar

private data class Metric(val value: Int, val label: String, val detail: String, val icon: ImageVector, val color: Color, val onClick: () -> Unit)

@Composable
fun DashboardScreen(
    state: HireFlowUiState,
    account: AccountUiState,
    onToggleTheme: (Boolean) -> Unit,
    onSync: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenInterviews: () -> Unit,
    onOpenCandidate: (Long) -> Unit,
    onOpenInterview: (Long) -> Unit,
    onOpenReview: (candidateId: Long, interviewId: Long?) -> Unit,
    onOpenCandidatesFiltered: (stage: String?, need: String?) -> Unit
) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..10 -> "Chào buổi sáng"
        in 11..13 -> "Chào buổi trưa"
        in 14..17 -> "Chào buổi chiều"
        else -> "Chào buổi tối"
    }
    val now = System.currentTimeMillis()
    val workItems = WorkQueue.todayWorkItems(state.interviews, now)
    val pendingReviews = WorkQueue.missingReviews(state.candidates, state.interviews, state.scorecards).size
    val metrics = listOf(
        Metric(state.interviews.count { WorkQueue.isSameDay(it.scheduledAt, now) }, "Phỏng vấn", "Lịch hẹn hôm nay", Icons.Rounded.CalendarMonth, Azure) {
            onOpenInterviews()
        },
        Metric(pendingReviews, "Chờ đánh giá", "Ứng viên cần đánh giá", Icons.Rounded.Groups, Success) {
            onOpenCandidatesFiltered(null, "review")
        },
        Metric(state.candidates.count { it.recruitmentStage == RecruitmentStage.SCREENING }, "Đang sàng lọc", "Ứng viên cần review CV", Icons.Rounded.MarkEmailUnread, Warning) {
            onOpenCandidatesFiltered(RecruitmentStage.SCREENING.name, null)
        },
        Metric(state.candidates.count { it.recruitmentStage == RecruitmentStage.OFFER }, "Chờ phản hồi offer", "Ứng viên đã nhận offer", Icons.Rounded.RateReview, Purple) {
            onOpenCandidatesFiltered(RecruitmentStage.OFFER.name, null)
        }
    )

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "HireFlow",
            menuItems = buildList {
                add((if (state.darkMode) "Giao diện sáng" else "Giao diện tối") to { onToggleTheme(!state.darkMode) })
                if (account.authenticated) add("Đồng bộ dữ liệu" to onSync)
            },
            action = {
                HeaderAction(Icons.Rounded.NotificationsNone, "Lịch và nhắc hẹn", onOpenInterviews)
                IconButton(onClick = onOpenProfile, modifier = Modifier.size(48.dp)) {
                    InitialAvatar(account.profile?.fullName ?: "Linh HR", Modifier.size(28.dp).semantics { contentDescription = "Tài khoản" })
                }
            }
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("$greeting, ${account.profile?.fullName?.substringAfterLast(' ') ?: "Linh"}!", style = MaterialTheme.typography.titleLarge)
                    Text("Đây là tổng quan tuyển dụng hôm nay.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                }
            }
            item { SectionTitle("Tổng quan hôm nay") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    metrics.chunked(2).forEach { rowMetrics ->
                        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            rowMetrics.forEach { metric -> MetricCard(metric, Modifier.weight(1f).fillMaxHeight()) }
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(2.dp))
                SectionTitle("Công việc hôm nay", "Xem lịch", onOpenInterviews)
            }
            if (workItems.isEmpty()) {
                item { Text("Không có công việc cần xử lý.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(workItems, key = { it.key }) { item ->
                    WorkRow(
                        title = item.title,
                        subtitle = item.subtitle,
                        onClick = {
                            when {
                                item.kind == WorkKind.REVIEW ->
                                    item.candidateId?.let { onOpenReview(it, item.interviewId) }
                                item.interviewId != null ->
                                    onOpenInterview(item.interviewId)
                                item.candidateId != null ->
                                    onOpenCandidate(item.candidateId)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCard(metric: Metric, modifier: Modifier) {
    Card(
        onClick = metric.onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = metric.color.copy(alpha = .045f)),
        border = BorderStroke(1.dp, metric.color.copy(alpha = .13f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(metric.value.toString(), fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Box(Modifier.size(32.dp).background(metric.color.copy(alpha = .1f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                    Icon(metric.icon, null, tint = metric.color, modifier = Modifier.size(18.dp))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(metric.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(metric.detail, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun WorkRow(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(9.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Schedule, null, modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
