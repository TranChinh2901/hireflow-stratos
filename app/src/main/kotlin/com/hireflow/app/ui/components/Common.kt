package com.hireflow.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hireflow.app.data.CandidateEntity
import com.hireflow.app.data.RecruitmentStage
import com.hireflow.app.ui.theme.Azure
import com.hireflow.app.ui.theme.Danger
import com.hireflow.app.ui.theme.Purple
import com.hireflow.app.ui.theme.Success
import com.hireflow.app.ui.theme.Teal
import com.hireflow.app.ui.theme.Warning

@Composable
fun HireFlowLogo(compact: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(if (compact) 26.dp else 34.dp)) {
            Box(Modifier.size(11.dp, 30.dp).clip(RoundedCornerShape(3.dp)).background(Teal).align(Alignment.CenterStart))
            Box(Modifier.size(11.dp, 30.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF47ACB6)).align(Alignment.CenterEnd))
            Box(Modifier.size(24.dp, 9.dp).clip(RoundedCornerShape(3.dp)).background(Teal).align(Alignment.Center))
        }
        Text("HireFlow", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = if (compact) 18.sp else 24.sp)
    }
}

val LocalHeaderNavigation = staticCompositionLocalOf<(String) -> Unit> { {} }

@Composable
fun HeaderAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(icon, description, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun HeaderOverflow(items: List<Pair<String, () -> Unit>>) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        HeaderAction(Icons.Rounded.MoreVert, "Thao tác khác") { expanded = true }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { (label, action) ->
                DropdownMenuItem(text = { Text(label) }, onClick = { expanded = false; action() })
            }
        }
    }
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    menuItems: List<Pair<String, () -> Unit>> = emptyList(),
    action: (@Composable () -> Unit)? = null
) {
    val navigate = LocalHeaderNavigation.current
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
            .heightIn(min = 56.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            HeaderAction(Icons.AutoMirrored.Rounded.ArrowBack, "Quay lại", onBack)
        } else {
            Box {
                HeaderAction(Icons.Rounded.Menu, "Mở menu") { menuOpen = true }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    listOf("Tổng quan" to "dashboard", "Ứng viên" to "candidates", "Pipeline" to "pipeline", "Lịch phỏng vấn" to "interviews", "Đánh giá" to "scorecards", "Tài khoản" to "profile").forEach { (label, route) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = { menuOpen = false; navigate(route) })
                    }
                    menuItems.forEach { (label, action) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = { menuOpen = false; action() })
                    }
                }
            }
        }
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically) { action?.invoke() }
    }
}

@Composable
fun SectionTitle(title: String, actionText: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (actionText != null) Text(
            actionText,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onAction?.invoke() }.padding(6.dp)
        )
    }
}

@Composable
fun InitialAvatar(name: String, modifier: Modifier = Modifier, blind: Boolean = false) {
    val palette = listOf(Azure, Teal, Purple, Warning)
    val color = palette[(name.hashCode() and Int.MAX_VALUE) % palette.size]
    Box(
        modifier.clip(CircleShape).background(if (blind) MaterialTheme.colorScheme.surfaceVariant else color.copy(alpha = .14f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (blind) "?" else name.trim().split(" ").takeLast(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString(""),
            color = if (blind) MaterialTheme.colorScheme.onSurfaceVariant else color,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

fun stageColor(stage: RecruitmentStage): Color = when (stage) {
    RecruitmentStage.APPLIED -> Azure
    RecruitmentStage.SCREENING -> Warning
    RecruitmentStage.INTERVIEW -> Purple
    RecruitmentStage.WAITING_DECISION -> Color(0xFFB06E2A)
    RecruitmentStage.OFFER -> Teal
    RecruitmentStage.HIRED -> Success
    RecruitmentStage.REJECTED -> Danger
}

@Composable
fun StagePill(stage: RecruitmentStage) {
    val color = stageColor(stage)
    Text(
        stage.label,
        color = color,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = .11f)).padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun CandidateRow(
    candidate: CandidateEntity,
    onClick: () -> Unit,
    compact: Boolean = false,
    trailing: (@Composable () -> Unit)? = null
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(10.dp).fillMaxWidth(), verticalAlignment = Alignment.Top) {
                InitialAvatar(candidate.name, Modifier.size(if (compact) 36.dp else 40.dp))
                Spacer(Modifier.size(10.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            candidate.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        StagePill(candidate.recruitmentStage)
                    }
                    Text(candidate.position, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (!compact) {
                        Text(
                            "${candidate.experienceYears} năm kinh nghiệm",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(end = if (trailing != null) 32.dp else 0.dp)
                        ) {
                            candidate.skillList.take(3).forEach { skill ->
                                Text(skill, style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(5.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                }
            }
            if (trailing != null) {
                Box(Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 8.dp)) { trailing() }
            }
        }
    }
}

@Composable
fun InfoCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .75f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { content() } }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, message: String) {
    Column(
        Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
