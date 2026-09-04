package com.hireflow.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hireflow.app.AccountUiState
import com.hireflow.app.HireFlowUiState
import com.hireflow.app.ui.components.InitialAvatar
import com.hireflow.app.ui.theme.Azure
import com.hireflow.app.ui.theme.Danger
import com.hireflow.app.ui.theme.IceBlue
import com.hireflow.app.ui.theme.Purple
import com.hireflow.app.ui.theme.Success
import com.hireflow.app.ui.theme.Teal

private data class ProfileMetric(val value: Int, val label: String, val icon: ImageVector, val color: Color)

@Composable
fun ProfileScreen(
    account: AccountUiState,
    state: HireFlowUiState,
    onBack: () -> Unit,
    onToggleTheme: (Boolean) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onUpdateProfile: (String, String, String, String) -> Unit,
    onSignOut: () -> Unit
) {
    val profile = account.profile
    val displayName = profile?.fullName ?: "Linh Nguyễn"
    val email = profile?.email ?: account.email ?: "demo@hireflow.local"
    val phone = profile?.phone?.ifBlank { "Chưa cập nhật" } ?: "Chưa cập nhật"
    val department = profile?.department?.ifBlank { "Human Resources" } ?: "Human Resources"
    val jobTitle = profile?.jobTitle?.ifBlank { "HR Specialist" } ?: "HR Specialist"
    val displayedRole = if (account.offlineMode) "Demo Admin" else roleLabel(account.role)
    var showEdit by rememberSaveable { mutableStateOf(false) }
    var showLogoutConfirm by rememberSaveable { mutableStateOf(false) }

    val metrics = remember(state.interviews.size, state.candidates.size, state.scorecards.size) {
        listOf(
            ProfileMetric(state.interviews.size, "lịch phỏng vấn", Icons.Rounded.CalendarMonth, Azure),
            ProfileMetric(state.candidates.size, "ứng viên", Icons.Rounded.Groups, Success),
            ProfileMetric(state.scorecards.size, "scorecard", Icons.Rounded.RateReview, Purple)
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(38.dp)) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Quay lại") }
                Text("Hồ sơ", color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    InitialAvatar(displayName, Modifier.size(64.dp))
                    Box(
                        Modifier.size(23.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface)
                            .align(Alignment.BottomEnd),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Rounded.CameraAlt, null, tint = Teal, modifier = Modifier.size(14.dp)) }
                }
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(displayName, color = MaterialTheme.colorScheme.onBackground, fontSize = 19.sp, lineHeight = 21.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(jobTitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(displayedRole, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Teal, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                metrics.forEach { metric -> ProfileMetricCard(metric, Modifier.weight(1f)) }
            }
        }
        item {
            ProfileCard {
                ProfileInfoRow(Icons.Rounded.Email, "Email", email)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .6f))
                ProfileInfoRow(Icons.Rounded.Phone, "Số điện thoại", phone)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .6f))
                ProfileInfoRow(Icons.Rounded.Business, "Phòng ban", department)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .6f))
                ProfileInfoRow(Icons.Rounded.Badge, "Vị trí", jobTitle)
            }
        }
        item { Text("Cài đặt & Tùy chọn", color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        item {
            ProfileCard {
                SettingRow(Icons.Rounded.NotificationsNone, "Nhắc lịch phỏng vấn") {
                    Switch(checked = state.notificationsEnabled, onCheckedChange = onToggleNotifications, modifier = Modifier.scale(.82f))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .6f))
                SettingRow(Icons.Rounded.DarkMode, "Dark mode") {
                    Switch(checked = state.darkMode, onCheckedChange = onToggleTheme, modifier = Modifier.scale(.82f))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .6f))
                SettingRow(Icons.Rounded.Language, "Ngôn ngữ") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Tiếng Việt", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Icon(Icons.AutoMirrored.Rounded.NavigateNext, null, modifier = Modifier.size(19.dp))
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .6f))
                SettingRow(Icons.Rounded.Security, "Bảo mật") {
                    Icon(Icons.AutoMirrored.Rounded.NavigateNext, null, modifier = Modifier.size(19.dp))
                }
            }
        }
        item { Text("Tài liệu", color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        item {
            ProfileCard {
                SettingRow(Icons.Rounded.Description, "CV mẫu / File nội bộ") {
                    Icon(Icons.AutoMirrored.Rounded.NavigateNext, null, modifier = Modifier.size(19.dp))
                }
            }
        }
        if (account.authenticated) {
            item {
                Button(
                    onClick = { showEdit = true },
                    shape = RoundedCornerShape(13.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Rounded.Edit, null)
                    Spacer(Modifier.size(9.dp))
                    Text("Chỉnh sửa hồ sơ", fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            OutlinedButton(
                onClick = { showLogoutConfirm = true },
                border = BorderStroke(1.dp, Danger),
                shape = RoundedCornerShape(13.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, null, tint = Danger)
                Spacer(Modifier.size(9.dp))
                Text(if (account.offlineMode) "Thoát demo" else "Đăng xuất", color = Danger, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showEdit && profile != null) {
        EditProfileDialog(
            initialName = profile.fullName,
            initialPhone = profile.phone,
            initialDepartment = profile.department,
            initialJobTitle = profile.jobTitle,
            saving = account.checking,
            onDismiss = { if (!account.checking) showEdit = false },
            onSave = { name, updatedPhone, updatedDepartment, updatedJobTitle ->
                onUpdateProfile(name, updatedPhone, updatedDepartment, updatedJobTitle)
                showEdit = false
            }
        )
    }
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text(if (account.offlineMode) "Thoát dữ liệu demo?" else "Đăng xuất khỏi HireFlow?") },
            text = { Text("Dữ liệu đã lưu trên thiết bị vẫn được giữ lại cho lần sử dụng tiếp theo.") },
            confirmButton = {
                TextButton(onClick = onSignOut) { Text(if (account.offlineMode) "Thoát demo" else "Đăng xuất", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { showLogoutConfirm = false }) { Text("Hủy") } }
        )
    }
}

@Composable
private fun ProfileMetricCard(metric: ProfileMetric, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            Modifier.background(Brush.linearGradient(listOf(metric.color.copy(alpha = .14f), metric.color.copy(alpha = .05f))))
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(24.dp).background(metric.color.copy(alpha = .12f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(metric.icon, null, tint = metric.color, modifier = Modifier.size(14.dp))
                }
                Spacer(Modifier.size(5.dp))
                Text(metric.value.toString(), color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text(metric.label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 4.dp), maxLines = 1)
        }
    }
}

@Composable
private fun ProfileCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .7f)),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) { Column(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 1.dp), content = content) }
}

@Composable
private fun ProfileInfoRow(icon: ImageVector, label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(start = 10.dp).width(82.dp))
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SettingRow(icon: ImageVector, label: String, action: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(19.dp))
        Text(label, fontSize = 13.sp, modifier = Modifier.padding(start = 10.dp).weight(1f), color = MaterialTheme.colorScheme.onSurface)
        action()
    }
}

@Composable
private fun EditProfileDialog(
    initialName: String,
    initialPhone: String,
    initialDepartment: String,
    initialJobTitle: String,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var phone by rememberSaveable { mutableStateOf(initialPhone) }
    var department by rememberSaveable { mutableStateOf(initialDepartment) }
    var jobTitle by rememberSaveable { mutableStateOf(initialJobTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chỉnh sửa hồ sơ") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Họ và tên") }, singleLine = true)
                OutlinedTextField(phone, { phone = it }, label = { Text("Số điện thoại") }, singleLine = true)
                OutlinedTextField(department, { department = it }, label = { Text("Phòng ban") }, singleLine = true)
                OutlinedTextField(jobTitle, { jobTitle = it }, label = { Text("Vị trí") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving && name.isNotBlank() && department.isNotBlank() && jobTitle.isNotBlank(),
                onClick = { onSave(name, phone, department, jobTitle) }
            ) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Hủy") } }
    )
}

private fun roleLabel(role: String): String = when (role.lowercase()) {
    "admin" -> "Admin"
    "hr" -> "HR Specialist"
    "interviewer" -> "Interviewer"
    else -> "HR Specialist"
}
