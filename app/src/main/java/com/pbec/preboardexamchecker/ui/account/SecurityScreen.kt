package com.pbec.preboardexamchecker.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(
    navController: NavController,
    viewModel: SecurityViewModel = hiltViewModel()
) {
    var showChangePasswordSheet by remember { mutableStateOf(false) }
    var showLoginActivitySheet by remember { mutableStateOf(false) }
    
    val sheetState = rememberModalBottomSheetState()
    val snackbarHostState = remember { SnackbarHostState() }
    val isUpdatingPassword by viewModel.isUpdatingPassword.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is SecurityViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Security", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8F9FF))
            )
        },
        containerColor = Color(0xFFF8F9FF)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            SectionHeader("Password")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    SecurityMenuItem(
                        icon = Icons.Default.Lock,
                        title = "Change Password",
                        subtitle = "Update your account password",
                        onClick = { showChangePasswordSheet = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color(0xFFEEEEEE))
                    SecurityMenuItem(
                        icon = Icons.Default.Key,
                        title = "Login Activity",
                        subtitle = "View recent login sessions",
                        onClick = { showLoginActivitySheet = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Security Tips Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F3FF))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Security Tips",
                        color = Color(0xFF415A91),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SecurityTipItem("Use a strong, unique password")
                    SecurityTipItem("Enable two-factor authentication")
                    SecurityTipItem("Never share your login credentials")
                    SecurityTipItem("Log out from shared devices")
                }
            }
        }

        if (showChangePasswordSheet) {
            ModalBottomSheet(
                onDismissRequest = { showChangePasswordSheet = false },
                sheetState = sheetState,
                containerColor = Color.White
            ) {
                ChangePasswordSheetContent(
                    onDismiss = { showChangePasswordSheet = false },
                    isUpdating = isUpdatingPassword,
                    onSubmit = { currentPassword, newPassword, confirmPassword ->
                        viewModel.changePassword(
                            currentPassword = currentPassword,
                            newPassword = newPassword,
                            confirmPassword = confirmPassword,
                            onSuccess = { showChangePasswordSheet = false }
                        )
                    }
                )
            }
        }

        if (showLoginActivitySheet) {
            ModalBottomSheet(
                onDismissRequest = { showLoginActivitySheet = false },
                sheetState = sheetState,
                containerColor = Color.White
            ) {
                LoginActivitySheetContent()
            }
        }
    }
}

@Composable
fun SecurityMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color(0xFF415A91),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1C1E)
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = Color(0xFF74777F)
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFF74777F)
        )
    }
}

@Composable
fun SecurityTipItem(tip: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text("• ", color = Color(0xFF74777F))
        Text(text = tip, fontSize = 14.sp, color = Color(0xFF44474E))
    }
}

@Composable
fun ChangePasswordSheetContent(
    onDismiss: () -> Unit,
    isUpdating: Boolean,
    onSubmit: (String, String, String) -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .navigationBarsPadding()
    ) {
        Text(
            text = "Change Password",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1C1E)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Enter your current password and choose a new one",
            fontSize = 14.sp,
            color = Color(0xFF74777F)
        )
        Spacer(modifier = Modifier.height(24.dp))

        PasswordField(
            value = currentPassword,
            onValueChange = { currentPassword = it },
            label = "Current Password",
            isVisible = currentPasswordVisible,
            onToggleVisibility = { currentPasswordVisible = !currentPasswordVisible }
        )
        Spacer(modifier = Modifier.height(16.dp))
        PasswordField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = "New Password",
            isVisible = newPasswordVisible,
            onToggleVisibility = { newPasswordVisible = !newPasswordVisible }
        )
        Spacer(modifier = Modifier.height(16.dp))
        PasswordField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = "Confirm New Password",
            isVisible = confirmPasswordVisible,
            onToggleVisibility = { confirmPasswordVisible = !confirmPasswordVisible }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                onSubmit(currentPassword, newPassword, confirmPassword)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF415A91)),
            enabled = !isUpdating
        ) {
            if (isUpdating) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                    color = Color.White
                )
            } else {
                Text("Update Password", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isVisible: Boolean,
    onToggleVisibility: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = null
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF415A91),
            unfocusedBorderColor = Color(0xFFC0C8CD)
        )
    )
}

@Composable
fun LoginActivitySheetContent() {
    val activities = listOf(
        LoginActivityItemData("Android Phone", "Manila, Philippines", "Now", true, Icons.Default.Smartphone),
        LoginActivityItemData("Windows PC", "Quezon City, Philippines", "Yesterday, 3:45 PM", false, Icons.Default.Laptop),
        LoginActivityItemData("Android Tablet", "Makati, Philippines", "Jan 28, 2026, 10:30 AM", false, Icons.Default.TabletAndroid),
        LoginActivityItemData("MacBook Pro", "Cebu City, Philippines", "Jan 25, 2026, 2:15 PM", false, Icons.Default.Laptop)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .navigationBarsPadding()
    ) {
        Text(
            text = "Login Activity",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1C1E)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Recent devices that have accessed your account",
            fontSize = 14.sp,
            color = Color(0xFF74777F)
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(activities) { activity ->
                LoginActivityItem(activity)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

data class LoginActivityItemData(
    val device: String,
    val location: String,
    val time: String,
    val isCurrent: Boolean,
    val icon: ImageVector
)

@Composable
fun LoginActivityItem(activity: LoginActivityItemData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (activity.isCurrent) Color(0xFFEFF3FF) else Color(0xFFF1F3F9)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = activity.icon,
                    contentDescription = null,
                    tint = if (activity.isCurrent) Color(0xFF415A91) else Color(0xFF74777F)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = activity.device,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1C1E)
                    )
                    if (activity.isCurrent) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "This device",
                            color = Color(0xFF415A91),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Text(
                    text = "${activity.location} • ${activity.time}",
                    fontSize = 13.sp,
                    color = Color(0xFF74777F)
                )
            }
        }
    }
}