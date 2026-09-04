package com.hireflow.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hireflow.app.AccountUiState
import com.hireflow.app.ui.components.HireFlowLogo
import com.hireflow.app.ui.theme.Azure
import com.hireflow.app.ui.theme.IceBlue
import com.hireflow.app.ui.theme.Teal

@Composable
fun LoginScreen(
    account: AccountUiState,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String, String, String, String) -> Unit,
    onOfflineDemo: () -> Unit
) {
    var register by rememberSaveable { mutableStateOf(false) }
    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var selectedRole by rememberSaveable { mutableStateOf("admin") }
    var roleMenuOpen by rememberSaveable { mutableStateOf(false) }
    var acceptedTerms by rememberSaveable { mutableStateOf(false) }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val emailValid = email.trim().matches(Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
    val loginValid = emailValid && password.length >= 6
    val registerValid = loginValid && fullName.trim().length >= 2 &&
        phone.filter(Char::isDigit).length >= 9 && acceptedTerms

    LaunchedEffect(register) { listState.scrollToItem(0) }

    AuthBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            item {
                HireFlowLogo()
                Spacer(Modifier.height(if (register) 22.dp else 28.dp))
                Text(
                    if (register) "Tạo tài khoản" else "Đăng nhập",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 26.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    if (register) "Bắt đầu quản lý tuyển dụng hiệu quả" else "Chào mừng bạn quay lại",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp, bottom = 16.dp)
                )

                if (!account.configured) {
                    AuthMessage("Chưa có cấu hình Supabase trong local.properties.", true)
                    Spacer(Modifier.height(10.dp))
                }

                if (register) {
                    RegisterForm(
                        fullName, { fullName = it },
                        email, { email = it },
                        phone, { phone = it },
                        password, { password = it },
                        selectedRole,
                        roleMenuOpen, { roleMenuOpen = it },
                        onRoleSelected = { selectedRole = it; roleMenuOpen = false },
                        acceptedTerms, { acceptedTerms = it },
                        showPassword, { showPassword = !showPassword },
                        account.configured && !account.checking
                    )
                } else {
                    LoginForm(
                        email, { email = it },
                        password, { password = it },
                        showPassword, { showPassword = !showPassword },
                        account.configured && !account.checking,
                        canSubmit = account.configured && !account.checking && loginValid,
                        loading = account.checking,
                        onSubmit = { onSignIn(email, password) },
                        onCreateAccount = { register = true }
                    )
                }

                account.message?.let {
                    Spacer(Modifier.height(10.dp))
                    AuthMessage(it, account.messageIsError)
                }

                if (register) {
                    Spacer(Modifier.height(12.dp))
                    Button(
                        enabled = account.configured && !account.checking && registerValid,
                        onClick = { onSignUp(fullName, email, phone, password, selectedRole) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Azure),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        if (account.checking) CircularProgressIndicator(Modifier.size(19.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("Đăng ký", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Đã có tài khoản?", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { register = false }) { Text("Đăng nhập", fontWeight = FontWeight.Bold) }
                    }
                } else {
                    OrDivider()
                    FilledTonalButton(
                        onClick = onOfflineDemo,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Box(Modifier.size(30.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = .10f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.CloudOff, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.size(10.dp))
                        Text("Dùng dữ liệu demo", modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, modifier = Modifier.size(20.dp))
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(top = 22.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = Teal, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(7.dp))
                        Text("Quản lý quy trình tuyển dụng nội bộ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginForm(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    showPassword: Boolean,
    onShowPasswordChange: () -> Unit,
    enabled: Boolean,
    canSubmit: Boolean,
    loading: Boolean,
    onSubmit: () -> Unit,
    onCreateAccount: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .97f)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AuthField(email, onEmailChange, "Email công việc", Icons.Rounded.Email, enabled)
            AuthField(password, onPasswordChange, "Mật khẩu", Icons.Rounded.Lock, enabled, true, showPassword, onShowPasswordChange)
            Text(
                "Quên mật khẩu?",
                color = Azure,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.End).padding(top = 2.dp, bottom = 1.dp)
            )
            Button(
                enabled = canSubmit,
                onClick = onSubmit,
                shape = RoundedCornerShape(11.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Azure),
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                if (loading) CircularProgressIndicator(Modifier.size(19.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Đăng nhập", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onCreateAccount,
                enabled = !loading,
                shape = RoundedCornerShape(11.dp),
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) { Text("Tạo tài khoản", fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun RegisterForm(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    selectedRole: String,
    roleMenuOpen: Boolean,
    onRoleMenuChange: (Boolean) -> Unit,
    onRoleSelected: (String) -> Unit,
    acceptedTerms: Boolean,
    onAcceptedTermsChange: (Boolean) -> Unit,
    showPassword: Boolean,
    onShowPasswordChange: () -> Unit,
    enabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        AuthField(fullName, onFullNameChange, "Họ và tên", Icons.Rounded.Person, enabled)
        AuthField(email, onEmailChange, "Email công việc", Icons.Rounded.Email, enabled)
        AuthField(phone, onPhoneChange, "Số điện thoại", Icons.Rounded.Phone, enabled)
        AuthField(password, onPasswordChange, "Mật khẩu", Icons.Rounded.Lock, enabled, true, showPassword, onShowPasswordChange)

        Box {
            Surface(
                shape = RoundedCornerShape(11.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                color = MaterialTheme.colorScheme.surface.copy(alpha = .92f),
                modifier = Modifier.fillMaxWidth().height(44.dp).clickable(enabled = enabled) { onRoleMenuChange(true) }
            ) {
                Row(Modifier.padding(horizontal = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(27.dp).background(IceBlue, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Badge, null, tint = Azure, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.size(10.dp))
                    Text("Vai trò: ${if (selectedRole == "admin") "Admin" else "HR"}", fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Rounded.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }
            DropdownMenu(expanded = roleMenuOpen, onDismissRequest = { onRoleMenuChange(false) }) {
                DropdownMenuItem(text = { Text("Admin") }, onClick = { onRoleSelected("admin") })
                DropdownMenuItem(text = { Text("HR") }, onClick = { onRoleSelected("hr") })
            }
        }

        Row(
            Modifier.fillMaxWidth().clickable(enabled = enabled) { onAcceptedTermsChange(!acceptedTerms) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(acceptedTerms, onCheckedChange = onAcceptedTermsChange, enabled = enabled, modifier = Modifier.size(34.dp))
            Spacer(Modifier.size(4.dp))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) { append("Tôi đồng ý với ") }
                    withStyle(SpanStyle(color = Azure, fontWeight = FontWeight.Medium)) { append("điều khoản sử dụng") }
                },
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    enabled: Boolean,
    password: Boolean = false,
    visible: Boolean = false,
    onToggleVisibility: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(11.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        visualTransformation = if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
        singleLine = true,
        enabled = enabled,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(Azure),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { innerTextField ->
            Row(
                Modifier.fillMaxWidth().height(44.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = .94f), shape)
                    .border(if (focused) 1.5.dp else 1.dp, if (focused) Azure else MaterialTheme.colorScheme.outline, shape)
                    .padding(start = 12.dp, end = if (password) 4.dp else 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = if (focused) Azure else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(17.dp))
                Spacer(Modifier.size(11.dp))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    innerTextField()
                }
                if (password) {
                    IconButton(onClick = { onToggleVisibility?.invoke() }, modifier = Modifier.size(34.dp)) {
                        Icon(
                            if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            if (visible) "Ẩn mật khẩu" else "Hiện mật khẩu",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun AuthMessage(message: String, isError: Boolean) {
    val color = if (isError) MaterialTheme.colorScheme.error else Teal
    Surface(color = color.copy(alpha = .09f), shape = RoundedCornerShape(11.dp), modifier = Modifier.fillMaxWidth()) {
        Text(message, color = color, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
    }
}

@Composable
private fun OrDivider() {
    Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
        Text("hoặc", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 12.dp))
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun AuthBackground(content: @Composable () -> Unit) {
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < .5f
    val topOuter = if (darkTheme) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .38f) else IceBlue.copy(alpha = .72f)
    val topInner = if (darkTheme) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .62f) else Color.White.copy(alpha = .72f)
    val bottomStart = if (darkTheme) Teal.copy(alpha = .14f) else Color(0xFFD8F7FA).copy(alpha = .78f)
    val bottomEnd = if (darkTheme) Azure.copy(alpha = .13f) else Color(0xFFDDEAFF).copy(alpha = .76f)
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(topOuter, size.minDimension * .42f, Offset(size.width * .94f, size.height * .02f))
            drawCircle(topInner, size.minDimension * .31f, Offset(size.width * .77f, size.height * .1f))
            drawCircle(bottomStart, size.width * .55f, Offset(size.width * .05f, size.height * 1.08f))
            drawCircle(bottomEnd, size.width * .48f, Offset(size.width * .92f, size.height * 1.07f))
            repeat(4) { row -> repeat(4) { column ->
                drawCircle(Azure.copy(alpha = .07f), 2.5.dp.toPx(), Offset(size.width * .78f + column * 17.dp.toPx(), size.height * .2f + row * 17.dp.toPx()))
            } }
            drawLine(SolidColor(Teal.copy(alpha = .12f)), Offset(0f, size.height * .975f), Offset(size.width, size.height * .94f), 1.5.dp.toPx())
        }
        content()
    }
}
