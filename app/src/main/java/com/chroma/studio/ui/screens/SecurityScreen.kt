package com.chroma.studio.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.chroma.studio.model.UserProfile
import com.chroma.studio.ui.components.*
import com.composables.icons.lucide.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun SecurityScreen(
    profile: UserProfile,
    onSave: (UserProfile) -> Unit,
    onBack: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser

    var isSendingReset by remember { mutableStateOf(false) }
    var unrecognizedLogins by remember { mutableStateOf(profile.unrecognizedLoginsEnabled) }
    var hasChanges by remember { mutableStateOf(false) }

    LaunchedEffect(unrecognizedLogins) {
        hasChanges = (unrecognizedLogins != profile.unrecognizedLoginsEnabled)
    }

    val screenBg = if (isDark) Color(0xFF000000) else Color(0xFFF2F2F7)

    Scaffold(
        topBar = {
            AppleSettingsHeader(
                title = "Security",
                icon = Lucide.Shield,
                iconGradient = listOf(Color(0xFF34C759), Color(0xFF248A3D)),
                doneText = if (hasChanges) "Save" else "Done",
                onDoneClick = {
                    if (hasChanges) {
                        onSave(profile.copy(unrecognizedLoginsEnabled = unrecognizedLogins))
                    }
                    onBack()
                }
            )
        },
        containerColor = screenBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            item {
                AppleSettingsGroup(
                    header = "Authentication & Credentials",
                    footer = "A cryptographic password reset or passkey will be securely authenticated with your verified credentials."
                ) {
                    AppleSettingNavRow(
                        title = "Change Password",
                        subtitle = user?.email?.let { "Send reset link to $it" } ?: "Reset your password",
                        icon = Lucide.Key,
                        iconGradient = listOf(Color(0xFF007AFF), Color(0xFF0051D5)),
                        valueText = if (isSendingReset) "Sending..." else "Send",
                        showChevron = false,
                        onClick = {
                            if (user?.email != null && !isSendingReset) {
                                isSendingReset = true
                                scope.launch {
                                    try {
                                        auth.sendPasswordResetEmail(user.email!!).addOnCompleteListener { task ->
                                            isSendingReset = false
                                            if (task.isSuccessful) {
                                                ToastManager.showToast("Password reset email sent to ${user.email}")
                                            } else {
                                                ToastManager.showToast("Failed to send reset email: ${task.exception?.message}")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        isSendingReset = false
                                        ToastManager.showToast("Error: ${e.message}")
                                    }
                                }
                            }
                        }
                    )
                }
            }

            item {
                AppleSettingsGroup(
                    header = "Login Security & Alerts",
                    footer = "Biometric protection and instant alerts keep your studio node secure against unauthorized access."
                ) {
                    AppleSettingToggleRow(
                        title = "Biometric Passkey Lock",
                        subtitle = "Require Face/Fingerprint for sensitive operations",
                        icon = Lucide.Fingerprint,
                        iconGradient = listOf(Color(0xFF34C759), Color(0xFF248A3D)),
                        checked = true,
                        onCheckedChange = { }
                    )

                    AppleSettingDivider()

                    AppleSettingToggleRow(
                        title = "Unrecognized Login Alerts",
                        subtitle = "Notify when a new device signs in",
                        icon = Lucide.ShieldAlert,
                        iconGradient = listOf(Color(0xFFFF9500), Color(0xFFCC7A00)),
                        checked = unrecognizedLogins,
                        onCheckedChange = { unrecognizedLogins = it }
                    )
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
