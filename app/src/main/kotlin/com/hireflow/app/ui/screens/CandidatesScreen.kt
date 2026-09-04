package com.hireflow.app.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.hireflow.app.data.CandidateEntity
import com.hireflow.app.data.RecruitmentStage
import com.hireflow.app.ui.components.CandidateRow
import com.hireflow.app.ui.components.EmptyState
import com.hireflow.app.ui.components.ScreenHeader

@Composable
fun CandidatesScreen(
    candidates: List<CandidateEntity>,
    onAddCandidate: (CandidateEntity, (Long) -> Unit) -> Unit,
    canManage: Boolean,
    onOpenCandidate: (Long) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedStage by rememberSaveable { mutableStateOf<String?>(null) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    val filtered = candidates.filter { candidate ->
        (query.isBlank() || candidate.name.contains(query, true) || candidate.position.contains(query, true) || candidate.skills.contains(query, true)) &&
            (selectedStage == null || candidate.stage == selectedStage)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (canManage) FloatingActionButton(onClick = { showAddDialog = true }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Rounded.Add, "Thêm ứng viên", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { padding ->
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { ScreenHeader("Ứng viên", "${candidates.size} hồ sơ trong hệ thống") }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Tìm theo tên, vị trí, kỹ năng...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    trailingIcon = { IconButton(onClick = { selectedStage = null }) { Icon(Icons.Rounded.FilterList, "Xóa bộ lọc") } },
                    singleLine = true,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = selectedStage == null, onClick = { selectedStage = null }, label = { Text("Tất cả") })
                    RecruitmentStage.entries.forEach { stage ->
                        FilterChip(selected = selectedStage == stage.name, onClick = { selectedStage = stage.name }, label = { Text(stage.label) })
                    }
                }
            }
            if (filtered.isEmpty()) {
                item { EmptyState(Icons.Rounded.PersonSearch, "Không tìm thấy ứng viên", "Thử từ khóa hoặc bộ lọc khác.") }
            } else {
                items(filtered.size, key = { filtered[it].id }) { index ->
                    CandidateRow(filtered[index], onClick = { onOpenCandidate(filtered[index].id) })
                }
            }
        }
    }

    if (showAddDialog) AddCandidateDialog(
        onDismiss = { showAddDialog = false },
        onSave = { candidate ->
            onAddCandidate(candidate) { id -> onOpenCandidate(id) }
            showAddDialog = false
        }
    )
}

@Composable
private fun AddCandidateDialog(onDismiss: () -> Unit, onSave: (CandidateEntity) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var position by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var experience by rememberSaveable { mutableStateOf("") }
    var skills by rememberSaveable { mutableStateOf("") }
    val valid = name.isNotBlank() && position.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm ứng viên") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                CompactField(name, { name = it }, "Họ và tên *")
                CompactField(position, { position = it }, "Vị trí ứng tuyển *")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompactField(email, { email = it }, "Email", Modifier.weight(1f))
                    CompactField(phone, { phone = it }, "Điện thoại", Modifier.weight(1f), KeyboardType.Phone)
                }
                CompactField(experience, { experience = it.filter(Char::isDigit) }, "Số năm kinh nghiệm", keyboardType = KeyboardType.Number)
                CompactField(skills, { skills = it }, "Kỹ năng (phân cách bằng dấu phẩy)")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(CandidateEntity(name = name.trim(), position = position.trim(), email = email.trim(), phone = phone.trim(), experienceYears = experience.toIntOrNull() ?: 0, skills = skills.trim()))
                },
                enabled = valid
            ) { Text("Thêm ứng viên") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
private fun CompactField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier
    )
}
