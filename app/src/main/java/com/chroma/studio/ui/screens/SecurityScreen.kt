package com.chroma.studio.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.chat.util.BiometricHelper
import com.chroma.studio.chat.util.findActivity
import com.chroma.studio.model.UserProfile
import com.chroma.studio.ui.components.*
import com.composables.icons.lucide.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun SecurityScreen(
    profile: UserProfile,
    onSave: (UserProfile) -> Unit,
    onBack: () -> Unit,
    onAccountDeleted: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser

    val secPrefs = remember { context.getSharedPreferences("chroma_chat_security_prefs", Context.MODE_PRIVATE) }
    var biometricPasskeyEnabled by remember {
        mutableStateOf(secPrefs.getBoolean("biometric_passkey_lock", false))
    }
    var isSendingReset by remember { mutableStateOf(false) }
    var unrecognizedLogins by remember { mutableStateOf(profile.unrecognizedLoginsEnabled) }
    var hasChanges by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isDeletingAccount by remember { mutableStateOf(false) }

    LaunchedEffect(unrecognizedLogins) {
        hasChanges = (unrecognizedLogins != profile.unrecognizedLoginsEnabled)
    }

    OneUiCollapsingHeaderLayout(
        title = "Security",
        subtitle = "Authentication & login alerts",
        onBack = onBack,
        headerActions = { _ ->
            Text(
                text = if (hasChanges) "Save" else "Done",
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0381FE),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        SoundManager.playReaction()
                        if (hasChanges) {
                            // onSave pops the back stack in HomeActivity — do not call onBack() twice
                            onSave(profile.copy(unrecognizedLoginsEnabled = unrecognizedLogins))
                        } else {
                            onBack()
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
    ) { _ ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            item {
                AppleSettingsGroup(
                    header = "Authentication & Credentials",
                    footer = "A password reset email will be sent to your verified account address."
                ) {
                    AppleSettingNavRow(
                        title = "Change Password",
                        subtitle = user?.email?.let { "Send reset link to $it" } ?: "Reset your password",
                        icon = Lucide.Key,
                        iconGradient = listOf(Color(0xFF0381FE), Color(0xFF0072DE)),
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
                            } else if (user?.email == null) {
                                ToastManager.showToast("No email address linked to this account")
                            }
                        }
                    )
                }
            }

            item {
                AppleSettingsGroup(
                    header = "Login Security & Alerts",
                    footer = "Biometric protection and login alerts help keep your account secure."
                ) {
                    AppleSettingToggleRow(
                        title = "Biometric Passkey Lock",
                        subtitle = "Require fingerprint, face, or screen lock to open Oscellion and for sensitive actions",
                        icon = Lucide.Fingerprint,
                        iconGradient = listOf(Color(0xFF0381FE), Color(0xFF0072DE)),
                        checked = biometricPasskeyEnabled,
                        onCheckedChange = { enabled ->
                            val activity = context.findActivity()
                            if (activity == null) {
                                ToastManager.showToast("Biometric authentication unavailable on this screen")
                            } else {
                                BiometricHelper.authenticate(
                                    activity = activity,
                                    title = if (enabled) "Enable Biometric Lock" else "Disable Biometric Lock",
                                    subtitle = if (enabled) {
                                        "Verify your identity to lock Oscellion with Biometrics"
                                    } else {
                                        "Verify your identity to turn off Biometric Lock"
                                    },
                                    onSuccess = {
                                        biometricPasskeyEnabled = enabled
                                        secPrefs.edit().putBoolean("biometric_passkey_lock", enabled).apply()
                                        ToastManager.showToast(if (enabled) "Biometric lock enabled" else "Biometric lock disabled")
                                    },
                                    onCancel = {},
                                    onError = { err -> ToastManager.showToast(err) }
                                )
                            }
                        }
                    )

                    AppleSettingDivider()

                    AppleSettingToggleRow(
                        title = "Unrecognized Login Alerts",
                        subtitle = "Notify when a new device signs in",
                        icon = Lucide.ShieldAlert,
                        iconGradient = listOf(Color(0xFF0381FE), Color(0xFF0072DE)),
                        checked = unrecognizedLogins,
                        onCheckedChange = { unrecognizedLogins = it }
                    )
                }
            }

            item {
                AppleSettingsGroup(
                    header = "Danger Zone",
                    footer = "Permanently deletes your profile, username reservation, and authentication account. This cannot be undone."
                ) {
                    AppleSettingNavRow(
                        title = "Delete Account",
                        subtitle = if (isDeletingAccount) "Deleting account..." else "Permanently remove your account and profile",
                        icon = Lucide.Trash2,
                        iconGradient = listOf(Color(0xFFEF4444), Color(0xFFDC2626)),
                        valueText = "",
                        showChevron = true,
                        onClick = {
                            if (!isDeletingAccount) {
                                showDeleteConfirm = true
                            }
                        }
                    )
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { if (!isDeletingAccount) showDeleteConfirm = false },
            title = { Text("Delete Account Permanently?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Your Oscellion profile (@${profile.username}), username reservation, and sign-in credentials will be permanently deleted. This action cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isDeletingAccount,
                    onClick = {
                        val currentUser = auth.currentUser ?: return@TextButton
                        val executeDelete: () -> Unit = {
                            isDeletingAccount = true
                            scope.launch {
                                try {
                                    val db = FirebaseFirestore.getInstance()
                                    // 1. Release username reservation if present
                                    if (profile.username.isNotBlank()) {
                                        try {
                                            db.collection("usernames").document(profile.username.lowercase()).delete().await()
                                        } catch (_: Exception) {}
                                    }
                                    // 2. Delete Firestore user profile document
                                    try {
                                        db.collection("users").document(currentUser.uid).delete().await()
                                    } catch (_: Exception) {}
                                    // 3. Delete Firebase Auth account
                                    currentUser.delete().await()
                                    showDeleteConfirm = false
                                    isDeletingAccount = false
                                    ToastManager.showToast("Account deleted")
                                    if (onAccountDeleted != null) {
                                        onAccountDeleted()
                                    } else {
                                        auth.signOut()
                                        onBack()
                                    }
                                } catch (e: FirebaseAuthRecentLoginRequiredException) {
                                    isDeletingAccount = false
                                    showDeleteConfirm = false
                                    ToastManager.showToast("For security, please sign out and sign back in before deleting your account.")
                                } catch (e: Exception) {
                                    isDeletingAccount = false
                                    showDeleteConfirm = false
                                    ToastManager.showToast("Could not delete account: ${e.localizedMessage ?: "Try again"}")
                                }
                            }
                        }
                        if (biometricPasskeyEnabled) {
                            val activity = context.findActivity()
                            if (activity == null) {
                                ToastManager.showToast("Biometric authentication unavailable on this screen")
                            } else {
                                BiometricHelper.authenticate(
                                    activity = activity,
                                    title = "Confirm Account Deletion",
                                    subtitle = "Verify your identity before permanently deleting your account",
                                    onSuccess = executeDelete,
                                    onCancel = {},
                                    onError = { err -> ToastManager.showToast(err) }
                                )
                            }
                        } else {
                            executeDelete()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                ) {
                    Text(if (isDeletingAccount) "Deleting..." else "Delete Permanently", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isDeletingAccount,
                    onClick = { showDeleteConfirm = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
