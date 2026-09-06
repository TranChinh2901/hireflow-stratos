package com.hireflow.app.ui.screens

import com.hireflow.app.ui.components.HeaderAction
import com.hireflow.app.ui.components.HeaderOverflow
import com.hireflow.app.ui.components.LocalHeaderNavigation
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hireflow.app.data.CandidateEntity
import com.hireflow.app.data.InterviewEntity
import com.hireflow.app.data.RecruitmentStage
import com.hireflow.app.data.ScorecardEntity
import com.hireflow.app.domain.WorkQueue
import com.hireflow.app.ui.components.ScreenHeader
import com.hireflow.app.ui.components.CandidateRow
import com.hireflow.app.ui.components.EmptyState

@Composable
fun CandidatesScreen(
    candidates: List<CandidateEntity>,
    interviews: List<InterviewEntity> = emptyList(),
    scorecards: List<ScorecardEntity> = emptyList(),
    onAddCandidate: (CandidateEntity, (Long) -> Unit, (String) -> Unit) -> Unit,
    canManage: Boolean,
    onOpenCandidate: (Long) -> Unit,
    onDeleteRejected: (List<CandidateEntity>) -> Unit = {},
    onDeleteCandidate: (CandidateEntity) -> Unit = {},
    initialStage: String? = null,
    initialNeed: String? = null
) {
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var newestFirst by rememberSaveable { mutableStateOf(true) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedStage by rememberSaveable { mutableStateOf<String?>(null) }
    var activeNeed by rememberSaveable { mutableStateOf<String?>(null) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteRejectedDialog by rememberSaveable { mutableStateOf(false) }
    var candidateToDelete by remember { mutableStateOf<CandidateEntity?>(null) }
    LaunchedEffect(initialStage, initialNeed) {
        // Chỉ áp dụng args khác null để không xóa bộ lọc user đang giữ khi quay lại.
        initialStage?.let { stage ->
            if (RecruitmentStage.entries.any { it.name == stage }) selectedStage = stage
        }
        initialNeed?.let { need ->
            if (need in setOf("review", "screening", "offer", "decision", "unscheduled")) {
                activeNeed = need
                showFilters = true
            }
        }
    }
    val needIds: Set<Long>? = when (activeNeed) {
        "review" -> WorkQueue.missingReviews(candidates, interviews, scorecards).map { it.id }.toSet()
        "screening" -> WorkQueue.screeningQueue(candidates).map { it.id }.toSet()
        "offer" -> WorkQueue.offerQueue(candidates).map { it.id }.toSet()
        "decision" -> WorkQueue.decisionQueue(candidates).map { it.id }.toSet()
        "unscheduled" -> WorkQueue.unscheduled(candidates, interviews).map { it.id }.toSet()
        else -> null
    }
    val needLabel = when (activeNeed) {
        "review" -> "Chờ đánh giá"
        "screening" -> "Đang sàng lọc"
        "offer" -> "Chờ phản hồi offer"
        "decision" -> "Chờ quyết định"
        "unscheduled" -> "Chưa đặt lịch"
        else -> null
    }
    val rejectedCount = candidates.count { it.recruitmentStage == RecruitmentStage.REJECTED }
    val filtered = candidates.filter { candidate ->
        (query.isBlank() || candidate.name.contains(query, true) || candidate.position.contains(query, true) || candidate.skills.contains(query, true)) &&
            (selectedStage == null || candidate.stage == selectedStage) &&
            (needIds == null || candidate.id in needIds)
    }.let { list -> if (newestFirst) list.sortedByDescending { it.createdAt } else list.sortedBy { it.name } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (canManage) FloatingActionButton(onClick = { showAddDialog = true }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Rounded.Add, "Thêm ứng viên", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScreenHeader("Ứng viên", action = {
                HeaderAction(Icons.Rounded.FilterList, "Lọc ứng viên") { showFilters = !showFilters }
                if (canManage) HeaderAction(Icons.Rounded.PersonAdd, "Thêm hồ sơ") { showAddDialog = true }
            })
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Surface(shape = RoundedCornerShape(9.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                        Row(Modifier.fillMaxWidth().height(46.dp).padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            BasicTextField(
                                value = query,
                                onValueChange = { query = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier.weight(1f).semantics { contentDescription = "Tìm kiếm ứng viên" },
                                decorationBox = { inner ->
                                    Box { if (query.isEmpty()) Text("Tìm kiếm ứng viên...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); inner() }
                                }
                            )
                            IconButton(onClick = { showFilters = !showFilters }) {
                                Icon(Icons.Rounded.FilterList, "Bộ lọc ứng viên", tint = if (selectedStage != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
                if (needLabel != null) item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = true,
                            onClick = { activeNeed = null },
                            label = { Text("Lọc: $needLabel") },
                            trailingIcon = { Icon(Icons.Rounded.Close, "Bỏ lọc", modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
                if (showFilters) item {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = selectedStage == null, onClick = { selectedStage = null }, label = { Text("Tất cả") })
                        RecruitmentStage.entries.forEach { stage ->
                            FilterChip(selected = selectedStage == stage.name, onClick = { selectedStage = stage.name }, label = { Text(stage.label) })
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("${filtered.size} ứng viên", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        if (canManage && rejectedCount > 0) {
                            TextButton(
                                onClick = { showDeleteRejectedDialog = true },
                                modifier = Modifier.height(36.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(4.dp))
                                Text("Xóa từ chối ($rejectedCount)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        TextButton(onClick = { newestFirst = !newestFirst }, modifier = Modifier.height(36.dp), contentPadding = PaddingValues(horizontal = 0.dp)) {
                            Text(if (newestFirst) "Sắp xếp: Mới nhất ↓" else "Sắp xếp: Tên A–Z ↓", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                if (filtered.isEmpty()) {
                    item { EmptyState(Icons.Rounded.PersonSearch, "Không tìm thấy ứng viên", "Thử từ khóa hoặc bộ lọc khác.") }
                } else {
                    items(filtered.size, key = { filtered[it].id }) { index ->
                        val item = filtered[index]
                        CandidateRow(
                            candidate = item,
                            onClick = { onOpenCandidate(item.id) },
                            trailing = if (canManage && item.recruitmentStage == RecruitmentStage.REJECTED) {
                                {
                                    IconButton(
                                        onClick = { candidateToDelete = item },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Rounded.Delete, "Xóa ${item.name}", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(19.dp))
                                    }
                                }
                            } else null
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) AddCandidateDialog(
        onDismiss = { showAddDialog = false },
        onSave = { candidate, onError ->
            onAddCandidate(
                candidate,
                { id ->
                    showAddDialog = false
                    onOpenCandidate(id)
                },
                { message -> onError(message) }
            )
        }
    )

    if (showDeleteRejectedDialog) AlertDialog(
        onDismissRequest = { showDeleteRejectedDialog = false },
        title = { Text("Xóa ứng viên bị từ chối?") },
        text = { Text("Sẽ xóa vĩnh viễn $rejectedCount hồ sơ ở vòng Từ chối cùng lịch phỏng vấn, đánh giá và CV đã lưu. Không thể hoàn tác.") },
        confirmButton = {
            Button(
                onClick = {
                    onDeleteRejected(candidates)
                    showDeleteRejectedDialog = false
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Xóa $rejectedCount hồ sơ") }
        },
        dismissButton = { TextButton(onClick = { showDeleteRejectedDialog = false }) { Text("Hủy") } }
    )

    candidateToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { candidateToDelete = null },
            title = { Text("Xóa ${target.name}?") },
            text = { Text("Hồ sơ đang ở vòng Từ chối sẽ bị xóa vĩnh viễn cùng lịch, đánh giá và CV. Ứng viên giữ lại để xem xét đợt sau thì bấm Hủy.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCandidate(target)
                        candidateToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Xóa") }
            },
            dismissButton = { TextButton(onClick = { candidateToDelete = null }) { Text("Giữ lại") } }
        )
    }
}

private data class ExperienceOption(val key: String, val label: String, val years: Int)

private val experienceOptions = listOf(
    ExperienceOption("intern", "Intern · 0 năm", 0),
    ExperienceOption("fresher", "Fresher · <1 năm", 0),
    ExperienceOption("junior", "Junior · 1–2 năm", 1),
    ExperienceOption("middle", "Middle · 3–4 năm", 3),
    ExperienceOption("senior", "Senior · 5+ năm", 5)
)

private val skillOptions = listOf(
   "Html/Css", "Javascript", "Typescript", "NodeJs", "Java", "C", "C++", "SpringBoot", "Reactjs", "NextJs", "NestJs", 
    "Git", "SQL", "Figma", "Testing", "Docker", "AWS"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddCandidateDialog(onDismiss: () -> Unit, onSave: (CandidateEntity, (String) -> Unit) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var position by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var selectedExperience by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSkills by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var saving by rememberSaveable { mutableStateOf(false) }
    var saveError by rememberSaveable { mutableStateOf<String?>(null) }
    val experience = experienceOptions.firstOrNull { it.key == selectedExperience }
    // Chỉ bắt buộc tên và vị trí; kinh nghiệm/kỹ năng bổ sung sau.
    val valid = name.isNotBlank() && position.isNotBlank()

    Dialog(
        onDismissRequest = { if (!saving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(.94f)
                .fillMaxHeight(.9f)
                .widthIn(max = 520.dp)
                .heightIn(max = 700.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 10.dp, top = 12.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(38.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.PersonAdd, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
                    }
                    Column(Modifier.weight(1f).padding(start = 11.dp)) {
                        Text("Thêm ứng viên", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text("Tạo hồ sơ mới cho pipeline", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "Đóng") }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .65f))

                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FormSectionTitle("Thông tin cơ bản")
                    CompactField(name, { name = it }, "Họ và tên *", Icons.Rounded.Person, Modifier.fillMaxWidth())
                    CompactField(position, { position = it }, "Vị trí ứng tuyển *", Icons.Rounded.Badge, Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CompactField(email, { email = it }, "Email", Icons.Rounded.Email, Modifier.weight(1f), KeyboardType.Email)
                        CompactField(phone, { phone = it }, "Điện thoại", Icons.Rounded.Phone, Modifier.weight(1f), KeyboardType.Phone)
                    }

                    FormSectionTitle("Kinh nghiệm", "Không bắt buộc, bổ sung sau")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        experienceOptions.forEach { option ->
                            FilterChip(
                                selected = selectedExperience == option.key,
                                onClick = { selectedExperience = option.key },
                                label = { Text(option.label, fontSize = 12.sp) },
                                leadingIcon = if (selectedExperience == option.key) {
                                    { Icon(Icons.Rounded.Check, null, modifier = Modifier.size(15.dp)) }
                                } else null,
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    FormSectionTitle("Kỹ năng", "Không bắt buộc, chọn sau cũng được")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        skillOptions.forEach { skill ->
                            val selected = skill in selectedSkills
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    selectedSkills = if (selected) selectedSkills - skill else selectedSkills + skill
                                },
                                label = { Text(skill, fontSize = 12.sp) },
                                leadingIcon = if (selected) {
                                    { Icon(Icons.Rounded.Check, null, modifier = Modifier.size(15.dp)) }
                                } else null,
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .65f))
                if (saveError != null) {
                    Text(
                        saveError!!,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { if (!saving) onDismiss() }, modifier = Modifier.weight(1f).height(44.dp)) { Text("Hủy") }
                    Button(
                        onClick = {
                            saving = true
                            saveError = null
                            onSave(
                                CandidateEntity(
                                    name = name.trim(),
                                    position = position.trim(),
                                    email = email.trim(),
                                    phone = phone.trim(),
                                    experienceYears = experience?.years ?: 0,
                                    skills = selectedSkills.joinToString(", ")
                                )
                            ) { message ->
                                saving = false
                                saveError = message
                            }
                        },
                        enabled = valid && !saving,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.6f).height(44.dp)
                    ) {
                        Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(if (saving) "Đang lưu..." else "Thêm ứng viên", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)
    val borderColor = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        interactionSource = interactionSource,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .3f), shape)
                    .border(if (focused) 1.5.dp else 1.dp, borderColor, shape)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    null,
                    tint = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(9.dp))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(label, fontSize = 12.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    innerTextField()
                }
            }
        }
    )
}

@Composable
private fun FormSectionTitle(title: String, subtitle: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        if (subtitle != null) {
            Spacer(Modifier.size(8.dp))
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
