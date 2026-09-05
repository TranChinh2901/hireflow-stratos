package com.hireflow.app.ui.screens

import com.hireflow.app.ui.components.HeaderAction
import com.hireflow.app.ui.components.HeaderOverflow
import com.hireflow.app.ui.components.LocalHeaderNavigation
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hireflow.app.data.CandidateEntity
import com.hireflow.app.data.InterviewEntity
import com.hireflow.app.data.RecruitmentStage
import com.hireflow.app.data.ScorecardEntity
import com.hireflow.app.domain.RecruitmentRules
import com.hireflow.app.ui.components.ScreenHeader
import com.hireflow.app.ui.components.stageColor

@Composable
fun PipelineScreen(
    candidates: List<CandidateEntity>,
    interviews: List<InterviewEntity>,
    scorecards: List<ScorecardEntity>,
    onOpenCandidate: (Long) -> Unit,
    onMoveNext: (CandidateEntity) -> Unit,
    canManage: Boolean
) {
    var query by rememberSaveable { mutableStateOf("") }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    val filtered = candidates.filter { query.isBlank() || it.name.contains(query, true) || it.position.contains(query, true) }
    val selected = filtered.firstOrNull { it.id == selectedId }
    val advanceBlockReason = selected?.let { RecruitmentRules.advanceBlockReason(it, interviews, scorecards) }
    val canMove = selected != null && advanceBlockReason == null

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Pipeline", action = {
            HeaderAction(Icons.Rounded.FilterList, "Lọc pipeline") { showSearch = !showSearch }
            HeaderOverflow(listOf("Xóa tìm kiếm" to { query = ""; showSearch = false }, "Bỏ chọn ứng viên" to { selectedId = null }))
        })
        if (showSearch) OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Tên hoặc vị trí ứng tuyển", style = MaterialTheme.typography.bodyMedium) },
            singleLine = true,
            shape = RoundedCornerShape(9.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(RecruitmentStage.entries, key = { it.name }) { stage ->
                PipelineColumn(
                    stage = stage,
                    candidates = filtered.filter { it.recruitmentStage == stage },
                    selectedId = selectedId,
                    onSelect = { if (canManage) selectedId = it else onOpenCandidate(it) }
                )
            }
        }
        if (canManage) Surface(shadowElevation = 3.dp) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                if (selected != null) Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(selected.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    TextButton(onClick = { onOpenCandidate(selected.id) }) { Text("Xem hồ sơ") }
                }
                Button(shape = RoundedCornerShape(9.dp),
                    onClick = { selected?.let(onMoveNext) },
                    enabled = canMove,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (selected == null) "Chọn ứng viên để chuyển vòng"
                        else if (canMove) "Chuyển sang ${selected.recruitmentStage.next().label}"
                        else "Chưa đủ điều kiện chuyển vòng",
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, modifier = Modifier.size(18.dp))
                }
                if (selected != null && advanceBlockReason != null) {
                    Text(
                        advanceBlockReason,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PipelineColumn(
    stage: RecruitmentStage,
    candidates: List<CandidateEntity>,
    selectedId: Long?,
    onSelect: (Long) -> Unit
) {
    val color = stageColor(stage)
    var expanded by rememberSaveable(stage.name) { mutableStateOf(false) }
    Card(
        shape = RoundedCornerShape(9.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = .07f)),
        border = BorderStroke(1.dp, color.copy(alpha = .1f))
    ) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${stage.label} (${candidates.size})", style = MaterialTheme.typography.labelLarge, color = color)
            if (candidates.isEmpty()) Text("Chưa có ứng viên", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp))
            (if (expanded) candidates else candidates.take(2)).forEach { candidate ->
                Card(
                    onClick = { onSelect(candidate.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, if (candidate.id == selectedId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                ) {
                    Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(candidate.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(candidate.position, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            if (candidates.size > 2) TextButton(
                onClick = { expanded = !expanded },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.fillMaxWidth().height(32.dp)
            ) { Text(if (expanded) "Thu gọn" else "+ ${candidates.size - 2} ứng viên", style = MaterialTheme.typography.labelMedium, color = color) }
        }
    }
}
