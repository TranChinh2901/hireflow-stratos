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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hireflow.app.data.CandidateEntity
import com.hireflow.app.data.ScorecardEntity
import com.hireflow.app.domain.ScoreCalculator
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
    scorecards: List<ScorecardEntity>,
    initialCandidateId: Long?,
    onSave: (ScorecardEntity) -> Unit,
    onMoveNext: (CandidateEntity) -> Unit,
    onBack: (() -> Unit)?
) {
    var selectedId by rememberSaveable(initialCandidateId) { mutableStateOf(initialCandidateId ?: candidates.firstOrNull()?.id) }
    val candidate = candidates.firstOrNull { it.id == selectedId }
    val existing = scorecards.firstOrNull { it.candidateId == selectedId }
    var blindMode by rememberSaveable { mutableStateOf(true) }
    var expanded by remember { mutableStateOf(false) }
    var technical by rememberSaveable(selectedId) { mutableIntStateOf(existing?.technical ?: 4) }
    var communication by rememberSaveable(selectedId) { mutableIntStateOf(existing?.communication ?: 3) }
    var problemSolving by rememberSaveable(selectedId) { mutableIntStateOf(existing?.problemSolving ?: 4) }
    var cultureFit by rememberSaveable(selectedId) { mutableIntStateOf(existing?.cultureFit ?: 4) }
    var strengths by rememberSaveable(selectedId) { mutableStateOf(existing?.strengths ?: "Kiến thức chuyên môn tốt, tư duy logic rõ ràng.") }
    var improvements by rememberSaveable(selectedId) { mutableStateOf(existing?.improvements ?: "Cần trình bày câu trả lời mạch lạc và cụ thể hơn.") }
    var notes by rememberSaveable(selectedId) { mutableStateOf(existing?.notes ?: "Ứng viên tiềm năng, phù hợp với yêu cầu vị trí.") }
    var conclusion by rememberSaveable(selectedId) { mutableStateOf(existing?.conclusion ?: "Hire") }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            ScreenHeader("Đánh giá phỏng vấn", if (blindMode) "Danh tính đang được ẩn" else "Chế độ đánh giá đầy đủ", onBack)
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Purple.copy(alpha = .1f)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(38.dp).clip(CircleShape).background(Purple.copy(alpha = .15f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.VisibilityOff, null, tint = Purple)
                        }
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Blind Review Mode", style = MaterialTheme.typography.titleMedium, color = Purple)
                            Text("Ẩn tên, ảnh, giới tính và tuổi", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = blindMode, onCheckedChange = { blindMode = it })
                    }
                }

                if (candidate == null) {
                    EmptyState(Icons.Rounded.Badge, "Chưa có ứng viên", "Thêm ứng viên trước khi tạo scorecard.")
                } else {
                    Box {
                        InfoCard(Modifier.fillMaxWidth().clickable { expanded = true }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                InitialAvatar(candidate.name, Modifier.size(48.dp), blind = blindMode)
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
                            candidates.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(if (blindMode) "Ứng viên #${item.id.toString().padStart(3, '0')} · ${item.position}" else "${item.name} · ${item.position}") },
                                    onClick = { selectedId = item.id; expanded = false }
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionTitle("Đánh giá năng lực")
                        RatingRow("Technical skills", technical) { technical = it }
                        RatingRow("Communication", communication) { communication = it }
                        RatingRow("Problem solving", problemSolving) { problemSolving = it }
                        RatingRow("Culture fit", cultureFit) { cultureFit = it }
                        val average = ScoreCalculator.average(technical, communication, problemSolving, cultureFit)
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Điểm trung bình", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Text("${"%.2f".format(average)} / 5", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
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
                    Button(
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
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Check, null)
                        Spacer(Modifier.size(7.dp))
                        Text("Lưu đánh giá")
                    }
                    Spacer(Modifier.size(28.dp))
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun RatingRow(label: String, rating: Int, onRatingChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
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
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = color) },
        minLines = 2,
        maxLines = 4,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    )
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
