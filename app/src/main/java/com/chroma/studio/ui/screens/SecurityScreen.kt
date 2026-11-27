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
    var hasPinConfigured by remember {
        mutableStateOf(com.chroma.studio.security.SecurityPinManager.hasPin(context))
    }
    var isBiometricUnlockEnabled by remember {
        mutableStateOf(com.chroma.studio.security.SecurityPinManager.isBiometricEnabled(context))
    }
    var autoLockDelayMs by remember {
        mutableLongStateOf(com.chroma.studio.security.SecurityPinManager.getAutoLockDelayMs(context))
    }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var isSendingReset by remember { mutableStateOf(false) }
    var unrecognizedLogins by remember { mutableStateOf(profile.unrecognizedLoginsEnabled) }
    var hasChanges by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isDeletingAccount by remember { mutableStateOf(false) }

    var showAppLockerScreen by remember { mutableStateOf(false) }
    var appLockerCount by remember { mutableIntStateOf(com.chroma.studio.security.applock.AppLockerManager.getLockedCount(context)) }
    var appLockerMasterEnabled by remember { mutableStateOf(com.chroma.studio.security.applock.AppLockerManager.isEnabled(context)) }

    if (showAppLockerScreen) {
        AppLockerScreen(
            onBack = {
                showAppLockerScreen = false
                appLockerCount = com.chroma.studio.security.applock.AppLockerManager.getLockedCount(context)
                appLockerMasterEnabled = com.chroma.studio.security.applock.AppLockerManager.isEnabled(context)
            }
        )
        return
    }

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
                    header = "Login Security & App Lock",
                    footer = "Protect your app with a 4-digit PIN and optional Biometrics for fast, secure access."
                ) {
                    AppleSettingToggleRow(
                        title = "App Lock",
                        subtitle = if (biometricPasskeyEnabled) {
                            if (isBiometricUnlockEnabled) "Active • PIN or Biometrics required" else "Active • 4-digit PIN required"
                        } else "Disabled • No lock required",
                        icon = Lucide.Lock,
                        iconGradient = listOf(Color(0xFF0381FE), Color(0xFF0072DE)),
                        checked = biometricPasskeyEnabled,
                        onCheckedChange = { enabled ->
                            val activity = context.findActivity()
                            if (enabled && !hasPinConfigured) {
                                // Prompt user to set up PIN first as the alternative backup
                                showPinSetupDialog = true
                            } else if (activity == null) {
                                ToastManager.showToast("Authentication unavailable on this screen")
                            } else {
                                BiometricHelper.authenticate(
                                    activity = activity,
                                    title = if (enabled) "Enable App Lock" else "Disable App Lock",
                                    subtitle = if (enabled) {
                                        "Verify your identity to lock Oscellion"
                                    } else {
                                        "Verify your identity to turn off App Lock"
                                    },
                                    onSuccess = {
                                        biometricPasskeyEnabled = enabled
                                        com.chroma.studio.security.SecurityPinManager.setAppLockEnabled(context, enabled)
                                        ToastManager.showToast(if (enabled) "App lock enabled" else "App lock disabled")
                                    },
                                    onCancel = {},
                                    onError = { err -> ToastManager.showToast(err) }
                                )
                            }
                        }
                    )

                    AppleSettingDivider()

                    AppleSettingToggleRow(
                        title = "Biometric Unlock",
                        subtitle = if (isBiometricUnlockEnabled) "Face & Fingerprint unlock active" else "Disabled • Unlock using 4-digit PIN only",
                        icon = Lucide.Fingerprint,
                        iconGradient = listOf(Color(0xFF34C759), Color(0xFF30D158)),
                        checked = isBiometricUnlockEnabled,
                        onCheckedChange = { enabled ->
                            if (!biometricPasskeyEnabled) {
                                ToastManager.showToast("Enable App Lock first")
                                return@AppleSettingToggleRow
                            }
                            if (!enabled && !hasPinConfigured) {
                                ToastManager.showToast("Set up a 4-digit PIN before disabling Biometrics")
                                showPinSetupDialog = true
                            } else {
                                isBiometricUnlockEnabled = enabled
                                com.chroma.studio.security.SecurityPinManager.setBiometricEnabled(context, enabled)
                                ToastManager.showToast(if (enabled) "Biometric unlock enabled" else "Biometrics disabled (PIN only)")
                            }
                        }
                    )

                    AppleSettingDivider()

                    AppleSettingNavRow(
                        title = "App Lock PIN",
                        subtitle = if (hasPinConfigured) "Change your 4-digit backup PIN" else "Set up a 4-digit PIN as a backup to Biometrics",
                        icon = Lucide.Lock,
                        iconGradient = listOf(Color(0xFF0381FE), Color(0xFF0072DE)),
                        valueText = if (hasPinConfigured) "Configured" else "Set Up",
                        onClick = {
                            showPinSetupDialog = true
                        }
                    )

                    AppleSettingDivider()

                    AppleSettingNavRow(
                        title = "Auto-Lock Delay",
                        subtitle = "How soon Oscellion locks after leaving the app",
                        icon = Lucide.Clock,
                        iconGradient = listOf(Color(0xFF5856D6), Color(0xFF3B82F6)),
                        valueText = com.chroma.studio.security.SecurityPinManager.getAutoLockDelayLabel(autoLockDelayMs),
                        showChevron = false,
                        onClick = {
                            SoundManager.playReaction()
                            val options = listOf(
                                com.chroma.studio.security.SecurityPinManager.DELAY_IMMEDIATE_MS,
                                com.chroma.studio.security.SecurityPinManager.DELAY_15S_MS,
                                com.chroma.studio.security.SecurityPinManager.DELAY_1M_MS,
                                com.chroma.studio.security.SecurityPinManager.DELAY_5M_MS
                            )
                            val idx = options.indexOf(autoLockDelayMs)
                            val next = options[(idx + 1) % options.size]
                            autoLockDelayMs = next
                            com.chroma.studio.security.SecurityPinManager.setAutoLockDelayMs(context, next)
                        }
                    )

                    if (biometricPasskeyEnabled) {
                        AppleSettingDivider()

                        AppleSettingNavRow(
                            title = "Lock App Now",
                            subtitle = "Immediately require Biometric or PIN unlock",
                            icon = Lucide.ShieldAlert,
                            iconGradient = listOf(Color(0xFFEF4444), Color(0xFFDC2626)),
                            valueText = "Lock",
                            showChevron = false,
                            onClick = {
                                SoundManager.playReaction()
                                com.chroma.studio.security.SecurityPinManager.lockSession()
                            }
                        )
                    }

                    AppleSettingDivider()

                    AppleSettingNavRow(
                        title = "Device App Locker",
                        subtitle = if (appLockerMasterEnabled && appLockerCount > 0) {
                            "$appLockerCount apps locked with Oscellion shield"
                        } else {
                            "Lock other apps on your device with PIN & Biometrics"
                        },
                        icon = Lucide.ShieldCheck,
                        iconGradient = listOf(Color(0xFF10B981), Color(0xFF059669)),
                        valueText = if (appLockerMasterEnabled) "$appLockerCount Locked" else "Configure",
                        onClick = {
                            SoundManager.playReaction()
                            showAppLockerScreen = true
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
        IosAlertDialog(
            title = "Delete Account Permanently?",
            message = "Your Oscellion profile (@${profile.username}), username reservation, and sign-in credentials will be permanently deleted. This action cannot be undone.",
            confirmText = if (isDeletingAccount) "Deleting..." else "Delete Permanently",
            confirmEnabled = !isDeletingAccount,
            isDestructive = true,
            onConfirm = {
                val currentUser = auth.currentUser ?: return@IosAlertDialog
                val executeDelete: () -> Unit = {
                    isDeletingAccount = true
                    scope.launch {
                        try {
                            val db = FirebaseFirestore.getInstance()
                            if (profile.username.isNotBlank()) {
                                try {
                                    db.collection("usernames").document(profile.username.lowercase()).delete().await()
                                } catch (_: Exception) {}
                            }
                            try {
                                db.collection("users").document(currentUser.uid).delete().await()
                            } catch (_: Exception) {}
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
            dismissText = if (isDeletingAccount) null else "Cancel",
            onDismiss = { if (!isDeletingAccount) showDeleteConfirm = false }
        )
    }

    if (showPinSetupDialog) {
        PinSetupDialog(
            isChangingExisting = hasPinConfigured,
            onDismiss = { showPinSetupDialog = false },
            onSuccess = {
                showPinSetupDialog = false
                hasPinConfigured = true
                isBiometricUnlockEnabled = com.chroma.studio.security.SecurityPinManager.isBiometricEnabled(context)
                if (!biometricPasskeyEnabled) {
                    biometricPasskeyEnabled = true
                    com.chroma.studio.security.SecurityPinManager.setAppLockEnabled(context, true)
                }
            }
        )
    }
}
