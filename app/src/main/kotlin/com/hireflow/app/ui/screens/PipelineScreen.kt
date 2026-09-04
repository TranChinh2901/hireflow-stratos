package com.hireflow.app.ui.screens

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hireflow.app.data.CandidateEntity
import com.hireflow.app.data.RecruitmentStage
import com.hireflow.app.ui.components.InitialAvatar
import com.hireflow.app.ui.components.ScreenHeader
import com.hireflow.app.ui.components.stageColor

@Composable
fun PipelineScreen(
    candidates: List<CandidateEntity>,
    onOpenCandidate: (Long) -> Unit,
    onMoveNext: (CandidateEntity) -> Unit,
    canManage: Boolean
) {
    var query by rememberSaveable { mutableStateOf("") }
    val stages = RecruitmentStage.entries
    val filtered = candidates.filter { query.isBlank() || it.name.contains(query, true) || it.position.contains(query, true) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Pipeline", "Theo dõi ứng viên qua từng vòng")
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            placeholder = { Text("Tìm trong pipeline...") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(stages, key = { it.name }) { stage ->
                PipelineColumn(
                    stage = stage,
                    candidates = filtered.filter { it.recruitmentStage == stage },
                    onOpenCandidate = onOpenCandidate,
                    onMoveNext = onMoveNext,
                    canManage = canManage
                )
            }
        }
    }
}

@Composable
private fun PipelineColumn(
    stage: RecruitmentStage,
    candidates: List<CandidateEntity>,
    onOpenCandidate: (Long) -> Unit,
    onMoveNext: (CandidateEntity) -> Unit,
    canManage: Boolean
) {
    val color = stageColor(stage)
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = .065f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = .18f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                Spacer(Modifier.size(7.dp))
                Text(stage.label, style = MaterialTheme.typography.labelLarge, color = color, modifier = Modifier.weight(1f))
                Text(candidates.size.toString(), style = MaterialTheme.typography.labelMedium, color = color, modifier = Modifier.clip(CircleShape).background(color.copy(alpha = .12f)).padding(horizontal = 7.dp, vertical = 3.dp))
            }
            if (candidates.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 17.dp), contentAlignment = Alignment.Center) {
                    Text("Chưa có ứng viên", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            candidates.take(4).forEach { candidate ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenCandidate(candidate.id) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .7f)),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            InitialAvatar(candidate.name, Modifier.size(28.dp))
                            Spacer(Modifier.size(7.dp))
                            Text(candidate.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        }
                        Text(candidate.position, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (canManage && stage != RecruitmentStage.HIRED && stage != RecruitmentStage.REJECTED) {
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onMoveNext(candidate) }.padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("Vòng tiếp", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
            if (candidates.size > 4) Text("+ ${candidates.size - 4} ứng viên", style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
}
