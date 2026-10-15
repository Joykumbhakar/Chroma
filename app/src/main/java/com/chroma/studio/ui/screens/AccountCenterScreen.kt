package com.chroma.studio.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.model.UserProfile
import com.chroma.studio.ui.components.*
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.GsonBuilder
import kotlinx.coroutines.tasks.await

@Composable
fun AccountCenterScreen(
    onNavigateToProfileIdentity: (UserProfile) -> Unit,
    onNavigateToSecurity: (UserProfile) -> Unit,
    onNavigateToPrivacy: (UserProfile) -> Unit,
    onNavigateToNotifications: (UserProfile) -> Unit,
    onNavigateToArchives: (UserProfile) -> Unit,
    onNavigateToActivityHistory: (UserProfile) -> Unit,
    onNavigateToPrivacyTerms: () -> Unit = {},
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            try {
                val db = FirebaseFirestore.getInstance()
                val snap = db.collection("users").document(currentUserId).get().await()
                profile = snap.toObject(UserProfile::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        isLoading = false
    }

    val screenBg = if (isDark) Color(0xFF000000) else Color(0xFFF2F2F7)

    Scaffold(
        topBar = {
            AppleSettingsHeader(
                title = "Settings",
                useMacSettingsIcon = true,
                doneText = "Done",
                onDoneClick = onBack
            )
        },
        containerColor = screenBg
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                OscellionLoadingSpinner(color = Color(0xFF007AFF))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Search Bar
                item {
                    AppleSettingsSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "Search Settings"
                    )
                }

                // Apple Account Profile Header Card
                item {
                    val userEmail = FirebaseAuth.getInstance().currentUser?.email
                    AppleSettingsProfileHeaderCard(
                        displayName = profile?.displayName?.ifBlank { "Oscellion Creator" } ?: "Oscellion Creator",
                        handleOrEmail = if (!profile?.username.isNullOrBlank()) "@${profile?.username}" else (userEmail ?: "Oscellion ID, Pass & iCloud"),
                        avatarUrl = profile?.avatarUrl,
                        gender = profile?.gender,
                        onClick = { profile?.let { onNavigateToProfileIdentity(it) } }
                    )
                }

                // Section 1: Core System & Preferences
                item {
                    AppleSettingsGroup(header = "Core System & Preferences") {
                        AppleSettingNavRow(
                            title = "Profile & Identity",
                            subtitle = "Name, avatar, bio, and social handles",
                            icon = Lucide.User,
                            valueText = profile?.displayName?.takeIf { it.isNotBlank() },
                            onClick = { profile?.let { onNavigateToProfileIdentity(it) } }
                        )

                        AppleSettingDivider()

                        AppleSettingNavRow(
                            title = "Security & Access",
                            subtitle = "Password, logins, and recovery alerts",
                            icon = Lucide.Shield,
                            onClick = { profile?.let { onNavigateToSecurity(it) } }
                        )

                        AppleSettingDivider()

                        AppleSettingNavRow(
                            title = "Privacy & Interaction",
                            subtitle = "Account visibility, mentions, and blocking",
                            icon = Lucide.Lock,
                            valueText = if (profile?.isPrivateAccount == true) "Private" else "Public",
                            onClick = { profile?.let { onNavigateToPrivacy(it) } }
                        )

                        AppleSettingDivider()

                        AppleSettingNavRow(
                            title = "Notifications & Alerts",
                            subtitle = "Push notifications, tones, and vibration",
                            icon = Lucide.Bell,
                            valueText = if (profile?.pushNotificationsEnabled == true) "On" else "Off",
                            onClick = { profile?.let { onNavigateToNotifications(it) } }
                        )

                        AppleSettingDivider()

                        AppleSettingNavRow(
                            title = "Archived Creatives",
                            subtitle = "View and restore your archived creations",
                            icon = Lucide.Archive,
                            onClick = { profile?.let { onNavigateToArchives(it) } }
                        )

                        AppleSettingDivider()

                        AppleSettingNavRow(
                            title = "Your Activity",
                            subtitle = "Liked, saved, and shared posts history",
                            icon = Lucide.History,
                            onClick = { profile?.let { onNavigateToActivityHistory(it) } }
                        )
                    }
                }

                // Section 2: Account Data & Privacy Controls
                item {
                    AppleSettingsGroup(header = "Data & Account") {
                        AppleSettingNavRow(
                            title = "Download My Information",
                            subtitle = "Export personal JSON archive",
                            icon = Lucide.Download,
                            onClick = {
                                profile?.let {
                                    val json = GsonBuilder().setPrettyPrinting().create().toJson(it)
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "My Oscellion Profile Data")
                                        putExtra(Intent.EXTRA_TEXT, json)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Download Data"))
                                }
                            }
                        )

                        AppleSettingDivider()

                        AppleSettingNavRow(
                            title = "Privacy Policy and Terms & Conditions",
                            subtitle = "Legal agreements, terms, and data safety",
                            icon = Lucide.FileText,
                            onClick = onNavigateToPrivacyTerms
                        )

                        AppleSettingDivider()

                        AppleSettingNavRow(
                            title = "Sign Out",
                            subtitle = "Log out from this device",
                            icon = Lucide.LogOut,
                            isDestructive = true,
                            showChevron = false,
                            onClick = onLogout
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
