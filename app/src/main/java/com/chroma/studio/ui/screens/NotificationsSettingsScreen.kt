package com.chroma.studio.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.model.UserProfile
import com.chroma.studio.ui.components.AppleSwitch
import com.chroma.studio.ui.components.ToastManager
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.*

/**
 * Flagship Chroma Studio Notification Preferences Screen.
 * Provides granular notification channels, quiet hours management,
 * and delivery methods with 100% theme-accurate glassmorphism styling.
 */
@Composable
fun NotificationsSettingsScreen(
    profile: UserProfile,
    onSave: (UserProfile) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalOscellionColors.current
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }

    // Master toggles from profile
    var pushEnabled by remember { mutableStateOf(profile.pushNotificationsEnabled) }
    var emailEnabled by remember { mutableStateOf(profile.emailNotificationsEnabled) }
    var quietMode by remember { mutableStateOf(profile.quietModeEnabled) }

    // Video Studio & Project Notifications from prefs
    var notifProjectExports by remember { mutableStateOf(prefs.getBoolean("notif_project_exports", true)) }
    var notifProjectCollab by remember { mutableStateOf(prefs.getBoolean("notif_project_collab", true)) }
    var notifTimelineRender by remember { mutableStateOf(prefs.getBoolean("notif_timeline_render", true)) }

    // Granular activity toggles from prefs
    var notifLikes by remember { mutableStateOf(prefs.getBoolean("notif_likes", true)) }
    var notifComments by remember { mutableStateOf(prefs.getBoolean("notif_comments", true)) }
    var notifFollows by remember { mutableStateOf(prefs.getBoolean("notif_follows", true)) }
    var notifMentions by remember { mutableStateOf(prefs.getBoolean("notif_mentions", true)) }
    var notifRemixes by remember { mutableStateOf(prefs.getBoolean("notif_remixes", true)) }
    var notifDrops by remember { mutableStateOf(prefs.getBoolean("notif_drops", true)) }
    var notifSounds by remember { mutableStateOf(prefs.getBoolean("notif_sounds", true)) }
    var notifHaptics by remember { mutableStateOf(prefs.getBoolean("notif_haptics", true)) }

    var hasChanges by remember { mutableStateOf(false) }

    LaunchedEffect(
        pushEnabled, emailEnabled, quietMode,
        notifProjectExports, notifProjectCollab, notifTimelineRender,
        notifLikes, notifComments, notifFollows, notifMentions, notifRemixes,
        notifDrops, notifSounds, notifHaptics
    ) {
        hasChanges = (pushEnabled != profile.pushNotificationsEnabled) ||
                     (emailEnabled != profile.emailNotificationsEnabled) ||
                     (quietMode != profile.quietModeEnabled) ||
                     (notifProjectExports != prefs.getBoolean("notif_project_exports", true)) ||
                     (notifProjectCollab != prefs.getBoolean("notif_project_collab", true)) ||
                     (notifTimelineRender != prefs.getBoolean("notif_timeline_render", true)) ||
                     (notifLikes != prefs.getBoolean("notif_likes", true)) ||
                     (notifComments != prefs.getBoolean("notif_comments", true)) ||
                     (notifFollows != prefs.getBoolean("notif_follows", true)) ||
                     (notifMentions != prefs.getBoolean("notif_mentions", true)) ||
                     (notifRemixes != prefs.getBoolean("notif_remixes", true)) ||
                     (notifDrops != prefs.getBoolean("notif_drops", true)) ||
                     (notifSounds != prefs.getBoolean("notif_sounds", true)) ||
                     (notifHaptics != prefs.getBoolean("notif_haptics", true))
    }

    fun saveAll() {
        SoundManager.playReaction()
        prefs.edit().apply {
            putBoolean("push_notifications_dm", pushEnabled)
            putBoolean("notif_project_exports", notifProjectExports)
            putBoolean("notif_project_collab", notifProjectCollab)
            putBoolean("notif_timeline_render", notifTimelineRender)
            putBoolean("notif_likes", notifLikes)
            putBoolean("notif_comments", notifComments)
            putBoolean("notif_follows", notifFollows)
            putBoolean("notif_mentions", notifMentions)
            putBoolean("notif_remixes", notifRemixes)
            putBoolean("notif_drops", notifDrops)
            putBoolean("notif_sounds", notifSounds)
            putBoolean("notif_haptics", notifHaptics)
            apply()
        }
        ToastManager.showToast("Notification preferences updated")
        onSave(
            profile.copy(
                pushNotificationsEnabled = pushEnabled,
                emailNotificationsEnabled = emailEnabled,
                quietModeEnabled = quietMode
            )
        )
    }

    Scaffold(
        containerColor = colors.bg,
        topBar = {
            Surface(
                color = colors.bg.copy(alpha = 0.96f),
                border = BorderStroke(0.5.dp, colors.glassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            onClick = onBack,
                            shape = RoundedCornerShape(12.dp),
                            color = colors.glassBg,
                            border = BorderStroke(1.dp, colors.glassBorder),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Lucide.ChevronLeft,
                                    contentDescription = "Back",
                                    tint = colors.textMain,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Text(
                            text = "Notification Settings",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain,
                            letterSpacing = (-0.4).sp
                        )
                    }

                    // Done / Save Button
                    Surface(
                        onClick = { saveAll() },
                        shape = RoundedCornerShape(10.dp),
                        color = if (hasChanges) colors.primary else colors.glassBg,
                        border = BorderStroke(1.dp, if (hasChanges) colors.primary else colors.glassBorder),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (hasChanges) "Save" else "Done",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasChanges) Color.White else colors.textMain
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // ── Section 1: Push Delivery Master ──
            item {
                NotificationSectionGroup(
                    header = "DELIVERY & SYSTEM",
                    footer = "When disabled, you will not receive real-time push alerts or timeline render notifications on this device."
                ) {
                    NotificationSettingRow(
                        title = "Allow Push Notifications",
                        subtitle = "Deliver real-time banners, sounds, and active badges",
                        icon = Lucide.Bell,
                        iconColor = Color(0xFF0A84FF),
                        checked = pushEnabled,
                        onCheckedChange = {
                            SoundManager.playReaction()
                            pushEnabled = it
                        }
                    )

                    NotificationDivider()

                    NotificationSettingRow(
                        title = "Quiet Hours Mode",
                        subtitle = "Silence banners and alerts between 10:00 PM and 8:00 AM",
                        icon = Lucide.Moon,
                        iconColor = Color(0xFF6366F1),
                        checked = quietMode,
                        onCheckedChange = {
                            SoundManager.playReaction()
                            quietMode = it
                        }
                    )
                }
            }

            // ── Section 2: Projects & Video Studio Alerts ──
            item {
                AnimatedVisibility(
                    visible = pushEnabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    NotificationSectionGroup(
                        header = "PROJECTS & STUDIO NOTIFICATIONS",
                        footer = "Configure notifications for background video rendering, ProRes exports, and multi-creator collaboration."
                    ) {
                        NotificationSettingRow(
                            title = "Video Render & Export Alerts",
                            subtitle = "Alerts when 4K ProRes exports or background timeline renders complete",
                            icon = Lucide.Video,
                            iconColor = Color(0xFF10B981),
                            checked = notifProjectExports,
                            onCheckedChange = {
                                SoundManager.playReaction()
                                notifProjectExports = it
                            }
                        )

                        NotificationDivider()

                        NotificationSettingRow(
                            title = "Project Collaboration & Invites",
                            subtitle = "Notify when invited to collaborate on or edit shared video timelines",
                            icon = Lucide.Users,
                            iconColor = Color(0xFF0A84FF),
                            checked = notifProjectCollab,
                            onCheckedChange = {
                                SoundManager.playReaction()
                                notifProjectCollab = it
                            }
                        )

                        NotificationDivider()

                        NotificationSettingRow(
                            title = "Timeline Background Processing",
                            subtitle = "Progress notifications for proxy generation and optical flow stabilization",
                            icon = Lucide.Layers,
                            iconColor = Color(0xFFF59E0B),
                            checked = notifTimelineRender,
                            onCheckedChange = {
                                SoundManager.playReaction()
                                notifTimelineRender = it
                            }
                        )

                        NotificationDivider()

                        NotificationSettingRow(
                            title = "Remixes & Asset Re-use",
                            subtitle = "Alerts when creators remix your audio tracks or video transitions",
                            icon = Lucide.Sparkles,
                            iconColor = Color(0xFF8B5CF6),
                            checked = notifRemixes,
                            onCheckedChange = {
                                SoundManager.playReaction()
                                notifRemixes = it
                            }
                        )
                    }
                }
            }

            // ── Section 3: Creator & Community Interactions ──
            item {
                AnimatedVisibility(
                    visible = pushEnabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    NotificationSectionGroup(
                        header = "ACTIVITY & SOCIAL INTERACTIONS",
                        footer = "Customize which viewer and community interactions trigger alerts."
                    ) {
                        NotificationSettingRow(
                            title = "Likes & Reactions",
                            subtitle = "When creators like your video or timeline exports",
                            icon = Lucide.Heart,
                            iconColor = Color(0xFFF43F5E),
                            checked = notifLikes,
                            onCheckedChange = {
                                SoundManager.playReaction()
                                notifLikes = it
                            }
                        )

                        NotificationDivider()

                        NotificationSettingRow(
                            title = "Comments & Discussions",
                            subtitle = "Replies and feedback on your published projects",
                            icon = Lucide.MessageCircle,
                            iconColor = Color(0xFF10B981),
                            checked = notifComments,
                            onCheckedChange = {
                                SoundManager.playReaction()
                                notifComments = it
                            }
                        )

                        NotificationDivider()

                        NotificationSettingRow(
                            title = "New Followers",
                            subtitle = "When a creator follows your creative profile",
                            icon = Lucide.UserPlus,
                            iconColor = Color(0xFF0A84FF),
                            checked = notifFollows,
                            onCheckedChange = {
                                SoundManager.playReaction()
                                notifFollows = it
                            }
                        )

                        NotificationDivider()

                        NotificationSettingRow(
                            title = "Mentions & Sequence Tags",
                            subtitle = "When another creator tags you in a sequence or caption",
                            icon = Lucide.AtSign,
                            iconColor = Color(0xFF06B6D4),
                            checked = notifMentions,
                            onCheckedChange = {
                                SoundManager.playReaction()
                                notifMentions = it
                            }
                        )
                    }
                }
            }

            // ── Section 4: Studio Drops & Highlights ──
            item {
                NotificationSectionGroup(
                    header = "STUDIO DROPS & HIGHLIGHTS",
                    footer = "Stay informed about official LUT presets, cinematic packs, and system updates."
                ) {
                    NotificationSettingRow(
                        title = "LUT & Preset Releases",
                        subtitle = "Alerts for new cinematic grading packs and video tools",
                        icon = Lucide.Sparkles,
                        iconColor = Color(0xFFF59E0B),
                        checked = notifDrops,
                        onCheckedChange = {
                            SoundManager.playReaction()
                            notifDrops = it
                        }
                    )

                    NotificationDivider()

                    NotificationSettingRow(
                        title = "Weekly Creator Digest",
                        subtitle = "Email roundup of trending projects and community masterclasses",
                        icon = Lucide.Mail,
                        iconColor = Color(0xFF0A84FF),
                        checked = emailEnabled,
                        onCheckedChange = {
                            SoundManager.playReaction()
                            emailEnabled = it
                        }
                    )
                }
            }

            // ── Section 5: Feedback & Signals ──
            item {
                NotificationSectionGroup(
                    header = "ALERT FEEDBACK & SIGNALS",
                    footer = "Adjust audio and tactile haptics for in-app alert notifications."
                ) {
                    NotificationSettingRow(
                        title = "In-App Sound Effects",
                        subtitle = "Play subtle audio cues on incoming notifications",
                        icon = Lucide.Volume2,
                        iconColor = Color(0xFF10B981),
                        checked = notifSounds,
                        onCheckedChange = {
                            SoundManager.playReaction()
                            notifSounds = it
                        }
                    )

                    NotificationDivider()

                    NotificationSettingRow(
                        title = "Haptic Vibration",
                        subtitle = "Tactile tap sensation on activity alerts",
                        icon = Lucide.Smartphone,
                        iconColor = Color(0xFF6366F1),
                        checked = notifHaptics,
                        onCheckedChange = {
                            SoundManager.playReaction()
                            notifHaptics = it
                        }
                    )
                }
            }

            item {
                Spacer(Modifier.height(30.dp))
            }
        }
    }
}

// ============================================================================
// SUPPORTING SETTINGS GROUP & ROW COMPONENTS
// ============================================================================

@Composable
private fun NotificationSectionGroup(
    header: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalOscellionColors.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (!header.isNullOrBlank()) {
            Text(
                text = header,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textMuted,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(start = 6.dp)
            )
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = colors.glassBg,
            border = BorderStroke(1.dp, colors.glassBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }

        if (!footer.isNullOrBlank()) {
            Text(
                text = footer,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = colors.textMuted,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun NotificationSettingRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconColor: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalOscellionColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.16f))
                    .border(1.dp, iconColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textMain
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.5.sp,
                        lineHeight = 16.sp,
                        color = colors.textMuted
                    )
                }
            }
        }

        AppleSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            activeTrackColor = iconColor
        )
    }
}

@Composable
private fun NotificationDivider() {
    val colors = LocalOscellionColors.current
    Divider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 65.dp),
        thickness = 0.5.dp,
        color = colors.glassBorder
    )
}
