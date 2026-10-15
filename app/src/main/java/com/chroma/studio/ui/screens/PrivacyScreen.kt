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
fun PrivacyScreen(
    profile: UserProfile,
    onSave: (UserProfile) -> Unit,
    onBack: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    var isPrivate by remember { mutableStateOf(profile.isPrivateAccount) }
    var allowTagging by remember { mutableStateOf(profile.allowTagging) }
    var showOnline by remember { mutableStateOf(profile.showOnlineStatus) }
    var hasChanges by remember { mutableStateOf(false) }

    LaunchedEffect(isPrivate, allowTagging, showOnline) {
        hasChanges = (isPrivate != profile.isPrivateAccount) ||
                     (allowTagging != profile.allowTagging) ||
                     (showOnline != profile.showOnlineStatus)
    }

    val screenBg = if (isDark) Color(0xFF000000) else Color(0xFFF2F2F7)

    Scaffold(
        topBar = {
            AppleSettingsHeader(
                title = "Privacy",
                icon = Lucide.Lock,
                iconGradient = listOf(Color(0xFFFF9500), Color(0xFFFF5E00)),
                doneText = if (hasChanges) "Save" else "Done",
                onDoneClick = {
                    if (hasChanges) {
                        onSave(profile.copy(
                            isPrivateAccount = isPrivate,
                            allowTagging = allowTagging,
                            showOnlineStatus = showOnline
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
                    header = "Account Visibility",
                    footer = "When your account is private, only people you approve can see your creations, flashes, and followers."
                ) {
                    AppleSettingToggleRow(
                        title = "Private Account",
                        subtitle = "Only approved followers can view your portfolio",
                        icon = Lucide.Lock,
                        checked = isPrivate,
                        onCheckedChange = { isPrivate = it }
                    )
                }
            }

            item {
                AppleSettingsGroup(
                    header = "Interactions & Mentions",
                    footer = "Allow other community members to tag and mention your handle in creations and comments."
                ) {
                    AppleSettingToggleRow(
                        title = "Allow Mentions & Tags",
                        subtitle = "Let other creators mention @${profile.username}",
                        icon = Lucide.AtSign,
                        checked = allowTagging,
                        onCheckedChange = { allowTagging = it }
                    )

                    AppleSettingDivider()

                    AppleSettingToggleRow(
                        title = "Show Online Activity",
                        subtitle = "Show green presence indicator when active",
                        icon = Lucide.Activity,
                        checked = showOnline,
                        onCheckedChange = { showOnline = it }
                    )
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
