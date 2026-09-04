package com.hireflow.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hireflow.app.data.CandidateEntity
import com.hireflow.app.data.RecruitmentStage
import com.hireflow.app.ui.theme.Azure
import com.hireflow.app.ui.theme.Danger
import com.hireflow.app.ui.theme.Muted
import com.hireflow.app.ui.theme.Navy
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

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Quay lại")
            }
            Spacer(Modifier.size(4.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        action?.invoke()
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
        modifier = Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = .11f)).padding(horizontal = 9.dp, vertical = 5.dp)
    )
}

@Composable
fun CandidateRow(candidate: CandidateEntity, onClick: () -> Unit, compact: Boolean = false) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .8f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(if (compact) 10.dp else 14.dp), verticalAlignment = Alignment.CenterVertically) {
            InitialAvatar(candidate.name, Modifier.size(if (compact) 36.dp else 48.dp))
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(candidate.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(candidate.position, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                if (!compact) Text("${candidate.experienceYears} năm kinh nghiệm", style = MaterialTheme.typography.labelMedium, color = Muted)
            }
            if (!compact) StagePill(candidate.recruitmentStage)
        }
    }
}

@Composable
fun InfoCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .75f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) { Box(Modifier.padding(16.dp)) { content() } }
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
