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
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Surface
import androidx.compose.material3.IconButton
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hireflow.app.data.CandidateEntity
import com.hireflow.app.data.InterviewEntity
import com.hireflow.app.data.ScorecardEntity
import com.hireflow.app.domain.ScoreCalculator
import com.hireflow.app.domain.RecruitmentRules
import com.hireflow.app.ui.components.EmptyState
import com.hireflow.app.ui.components.InfoCard
import com.hireflow.app.ui.components.InitialAvatar
import com.hireflow.app.ui.components.ScreenHeader
import com.hireflow.app.ui.components.SectionTitle
import com.hireflow.app.ui.theme.Azure
import com.hireflow.app.ui.theme.Danger
import com.hireflow.app.ui.theme.Purple
import com.hireflow.app.ui.theme.Success
import com.hireflow.app.ui.theme.Warning
import kotlinx.coroutines.launch

@Composable
fun ScorecardScreen(
    candidates: List<CandidateEntity>,
    interviews: List<InterviewEntity>,
    scorecards: List<ScorecardEntity>,
    evaluatorId: String?,
    initialCandidateId: Long?,
    onSave: (ScorecardEntity) -> Unit,
    onBack: (() -> Unit)?
) {
    val evaluatorScorecards = scorecards.filter { it.evaluatorId == evaluatorId }
    val availableCandidates = candidates.filter { candidate ->
        RecruitmentRules.canReview(candidate, interviews) || evaluatorScorecards.any { it.candidateId == candidate.id }
    }
    var selectedId by rememberSaveable(initialCandidateId, availableCandidates.firstOrNull()?.id) {
        mutableStateOf(
            initialCandidateId?.takeIf { id -> availableCandidates.any { it.id == id } }
                ?: availableCandidates.firstOrNull()?.id
        )
    }
    val candidate = availableCandidates.firstOrNull { it.id == selectedId }
    val existing = evaluatorScorecards.firstOrNull { it.candidateId == selectedId }
    var blindMode by rememberSaveable { mutableStateOf(true) }
    var expanded by remember { mutableStateOf(false) }
    var technical by rememberSaveable(selectedId) { mutableIntStateOf(existing?.technical ?: 0) }
    var communication by rememberSaveable(selectedId) { mutableIntStateOf(existing?.communication ?: 0) }
    var problemSolving by rememberSaveable(selectedId) { mutableIntStateOf(existing?.problemSolving ?: 0) }
    var cultureFit by rememberSaveable(selectedId) { mutableIntStateOf(existing?.cultureFit ?: 0) }
    var strengths by rememberSaveable(selectedId) { mutableStateOf(existing?.strengths.orEmpty()) }
    var improvements by rememberSaveable(selectedId) { mutableStateOf(existing?.improvements.orEmpty()) }
    var notes by rememberSaveable(selectedId) { mutableStateOf(existing?.notes.orEmpty()) }
    var conclusion by rememberSaveable(selectedId) { mutableStateOf(existing?.conclusion.orEmpty()) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val navigate = LocalHeaderNavigation.current

    Column(Modifier.fillMaxSize().imePadding()) {
        ScreenHeader("Đánh giá phỏng vấn", onBack = onBack ?: { navigate("dashboard") }, action = {
            HeaderOverflow(listOf((if (blindMode) "Hiện danh tính" else "Ẩn danh tính") to { blindMode = !blindMode }))
        })
        Box(Modifier.weight(1f)) {
            Column(Modifier.fillMaxSize().padding(bottom = if (candidate != null) 64.dp else 0.dp).verticalScroll(rememberScrollState())) {
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Purple.copy(alpha = .1f)),
                        shape = RoundedCornerShape(10.dp),
                        elevation = CardDefaults.cardElevation(0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Blind Review Mode", style = MaterialTheme.typography.titleMedium, color = Purple)
                                Text(if (blindMode) "Thông tin định danh đã được ẩn" else "Đang hiển thị danh tính", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { blindMode = !blindMode }, modifier = Modifier.semantics { stateDescription = if (blindMode) "Đang bật" else "Đang tắt" }) {
                                Icon(Icons.Rounded.VisibilityOff, "Ẩn hoặc hiện danh tính", tint = Purple)
                            }
                        }
                    }

                    if (candidate == null) {
                        EmptyState(Icons.Rounded.Badge, "Chưa có ứng viên cần đánh giá", "Hoàn thành một lịch phỏng vấn trước khi tạo scorecard.")
                    } else {
                        Box {
                            InfoCard(Modifier.fillMaxWidth().clickable { expanded = true }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    InitialAvatar(candidate.name, Modifier.size(36.dp), blind = blindMode)
                                    Spacer(Modifier.size(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(if (blindMode) "Ứng viên #${candidate.id.toString().padStart(3, '0')}" else candidate.name, style = MaterialTheme.typography.titleMedium)
                                        Text(candidate.position, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${candidate.experienceYears} năm · ${candidate.skillList.take(3).joinToString(" · ")}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Icon(Icons.Rounded.KeyboardArrowDown, null)
                                }
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                availableCandidates.forEach { item ->
                                    DropdownMenuItem(
                                        text = { Text(if (blindMode) "Ứng viên #${item.id.toString().padStart(3, '0')} · ${item.position}" else "${item.name} · ${item.position}") },
                                        onClick = { selectedId = item.id; expanded = false }
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            SectionTitle("Đánh giá năng lực")
                            RatingRow("Technical skills", technical) { technical = it }
                            RatingRow("Communication", communication) { communication = it }
                            RatingRow("Problem solving", problemSolving) { problemSolving = it }
                            RatingRow("Culture fit", cultureFit) { cultureFit = it }
                            val ratingsComplete = listOf(technical, communication, problemSolving, cultureFit).all { it in 1..5 }
                            val average = if (ratingsComplete) ScoreCalculator.average(technical, communication, problemSolving, cultureFit) else null
                            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("Điểm trung bình", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                                Text(average?.let { "${"%.2f".format(it)} / 5" } ?: "Chưa chấm đủ", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        SectionTitle("Nhận xét")
                        ReviewField("Điểm mạnh", strengths, { strengths = it }, Success)
                        ReviewField("Cần cải thiện", improvements, { improvements = it }, Warning)
                        ReviewField("Ghi chú", notes, { notes = it }, Azure)

                        SectionTitle("Kết luận")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            listOf("Strong Hire" to Success, "Hire" to Azure, "Consider" to Warning, "Reject" to Danger).forEach { (label, color) ->
                                ConclusionChip(label, color, conclusion == label, Modifier.weight(1f)) { conclusion = label }
                            }
                        }
                        Spacer(Modifier.size(32.dp))
                    }
                }
            }
            if (candidate != null) Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Box(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        Button(shape = RoundedCornerShape(9.dp),
                            onClick = {
                                val newScorecard = ScorecardEntity(candidateId = candidate.id, technical = technical, communication = communication, problemSolving = problemSolving, cultureFit = cultureFit, strengths = strengths, improvements = improvements, notes = notes, conclusion = conclusion)
                                onSave(if (existing == null) newScorecard else newScorecard.copy(
                                    id = existing.id,
                                    remoteId = existing.remoteId,
                                    remoteCandidateId = existing.remoteCandidateId,
                                    organizationId = existing.organizationId,
                                    evaluatorId = existing.evaluatorId
                                ))
                                scope.launch { snackbar.showSnackbar("Đã lưu phiếu đánh giá cho ${if (blindMode) "ứng viên #${candidate.id}" else candidate.name}") }
                            },
                            enabled = listOf(technical, communication, problemSolving, cultureFit).all { it in 1..5 } &&
                                conclusion.isNotBlank() && RecruitmentRules.canReview(candidate, interviews),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Check, null)
                            Spacer(Modifier.size(7.dp))
                            Text("Lưu đánh giá")
                        }
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp))
        }
    }
}

@Composable
private fun RatingRow(label: String, rating: Int, onRatingChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        repeat(5) { index ->
            val value = index + 1
            Icon(
                Icons.Rounded.Star,
                contentDescription = "$value điểm",
                tint = if (value <= rating) Azure else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(28.dp).clip(CircleShape).clickable { onRatingChange(value) }.padding(3.dp)
            )
        }
        Text("$rating/5", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 5.dp))
    }
}

@Composable
private fun ReviewField(label: String, value: String, onValueChange: (String) -> Unit, color: Color) {
    Column(
        Modifier.fillMaxWidth().background(color.copy(alpha = .08f), RoundedCornerShape(8.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, color = color, style = MaterialTheme.typography.labelLarge)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp)
                .semantics { contentDescription = label },
            minLines = 2,
            maxLines = 5
        )
    }
}

@Composable
private fun ConclusionChip(label: String, color: Color, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(10.dp)).background(if (selected) color else color.copy(alpha = .08f)).clickable(onClick = onClick).padding(horizontal = 3.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) Color.White else color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}
