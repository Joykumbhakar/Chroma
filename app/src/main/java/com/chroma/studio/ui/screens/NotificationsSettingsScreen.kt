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

@Composable
fun NotificationsSettingsScreen(
    profile: UserProfile,
    onSave: (UserProfile) -> Unit,
    onBack: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    var pushEnabled by remember { mutableStateOf(profile.pushNotificationsEnabled) }
    var emailEnabled by remember { mutableStateOf(profile.emailNotificationsEnabled) }
    var quietMode by remember { mutableStateOf(profile.quietModeEnabled) }
    var hasChanges by remember { mutableStateOf(false) }

    LaunchedEffect(pushEnabled, emailEnabled, quietMode) {
        hasChanges = (pushEnabled != profile.pushNotificationsEnabled) ||
                     (emailEnabled != profile.emailNotificationsEnabled) ||
                     (quietMode != profile.quietModeEnabled)
    }

    val screenBg = if (isDark) Color(0xFF000000) else Color(0xFFF2F2F7)

    Scaffold(
        topBar = {
            AppleSettingsHeader(
                title = "Notifications",
                icon = Lucide.Bell,
                iconGradient = listOf(Color(0xFFFF2D55), Color(0xFFFF375F)),
                doneText = if (hasChanges) "Save" else "Done",
                onDoneClick = {
                    if (hasChanges) {
                        onSave(profile.copy(
                            pushNotificationsEnabled = pushEnabled,
                            emailNotificationsEnabled = emailEnabled,
                            quietModeEnabled = quietMode
                        ))
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
                    header = "Push & Device Delivery",
                    footer = "Receive instant alerts on this device for likes, comments, remixes, and direct messages."
                ) {
                    AppleSettingToggleRow(
                        title = "Allow Push Notifications",
                        subtitle = "Banners, sounds, and badges",
                        icon = Lucide.BellRing,
                        checked = pushEnabled,
                        onCheckedChange = { pushEnabled = it }
                    )

                    AppleSettingDivider()

                    AppleSettingToggleRow(
                        title = "Quiet Hours Mode",
                        subtitle = "Silence all alerts and vibrations during night hours",
                        icon = Lucide.Moon,
                        checked = quietMode,
                        onCheckedChange = { quietMode = it }
                    )
                }
            }

            item {
                AppleSettingsGroup(
                    header = "Email & Newsletters",
                    footer = "We only send critical account security updates and weekly community digest emails."
                ) {
                    AppleSettingToggleRow(
                        title = "Email Notifications",
                        subtitle = "Product updates and weekly highlights",
                        icon = Lucide.Mail,
                        checked = emailEnabled,
                        onCheckedChange = { emailEnabled = it }
                    )
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
