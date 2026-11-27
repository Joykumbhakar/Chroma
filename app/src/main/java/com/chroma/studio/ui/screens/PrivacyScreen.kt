package com.chroma.studio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.model.UserProfile
import com.chroma.studio.ui.components.*
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private data class BlockedAccountInfo(
    val uid: String,
    val displayName: String,
    val username: String,
    val avatarUrl: String
)

@Composable
fun PrivacyScreen(
    profile: UserProfile,
    onSave: (UserProfile) -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val scope = rememberCoroutineScope()
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid

    var isPrivate by remember { mutableStateOf(profile.isPrivateAccount) }
    var allowTagging by remember { mutableStateOf(profile.allowTagging) }
    var showOnline by remember { mutableStateOf(profile.showOnlineStatus) }
    var hasChanges by remember { mutableStateOf(false) }

    var blockedAccounts by remember { mutableStateOf<List<BlockedAccountInfo>>(emptyList()) }
    var isLoadingBlocked by remember { mutableStateOf(true) }

    LaunchedEffect(isPrivate, allowTagging, showOnline) {
        hasChanges = (isPrivate != profile.isPrivateAccount) ||
            (allowTagging != profile.allowTagging) ||
            (showOnline != profile.showOnlineStatus)
    }

    LaunchedEffect(currentUid) {
        if (currentUid.isNullOrBlank()) {
            isLoadingBlocked = false
            return@LaunchedEffect
        }
        isLoadingBlocked = true
        try {
            val db = FirebaseFirestore.getInstance()
            val doc = db.collection("users").document(currentUid).get().await()
            val rawIds = (doc.get("blockedUserIds") as? List<*>)
                ?.mapNotNull { it as? String }
                ?.filter { it.isNotBlank() }
                ?.distinct()
                ?: emptyList()

            val loaded = mutableListOf<BlockedAccountInfo>()
            for (bUid in rawIds) {
                try {
                    val uDoc = db.collection("users").document(bUid).get().await()
                    val name = uDoc.getString("displayName")?.takeIf { it.isNotBlank() }
                        ?: uDoc.getString("username")?.takeIf { it.isNotBlank() }
                        ?: "Blocked User"
                    val handle = uDoc.getString("username") ?: ""
                    val avatar = uDoc.getString("avatarUrl") ?: ""
                    loaded.add(BlockedAccountInfo(bUid, name, handle, avatar))
                } catch (_: Exception) {
                    loaded.add(BlockedAccountInfo(bUid, "Blocked User", "", ""))
                }
            }
            blockedAccounts = loaded
        } catch (_: Exception) {
            blockedAccounts = emptyList()
        } finally {
            isLoadingBlocked = false
        }
    }

    OneUiCollapsingHeaderLayout(
        title = "Privacy",
        subtitle = "Account visibility, permissions & blocking",
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
                            com.chroma.studio.data.OnlineStatusManager.updateSelfShowOnlineStatus(showOnline)
                            // onSave pops the back stack in HomeActivity — do not call onBack() twice
                            onSave(
                                profile.copy(
                                    isPrivateAccount = isPrivate,
                                    allowTagging = allowTagging,
                                    showOnlineStatus = showOnline
                                )
                            )
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
                    header = "Account Visibility",
                    footer = "When your account is private, only people you approve can see your creations, flashes, and followers."
                ) {
                    AppleSettingToggleRow(
                        title = "Private Account",
                        subtitle = "Only approved followers can view your portfolio",
                        icon = Lucide.Lock,
                        iconGradient = listOf(Color(0xFF0381FE), Color(0xFF0072DE)),
                        checked = isPrivate,
                        onCheckedChange = { isPrivate = it }
                    )
                }
            }

            item {
                AppleSettingsGroup(
                    header = "Interactions & Mentions",
                    footer = "Control who can mention your handle and see your online activity."
                ) {
                    AppleSettingToggleRow(
                        title = "Allow Mentions & Tags",
                        subtitle = "Let other creators mention @${profile.username}",
                        icon = Lucide.AtSign,
                        iconGradient = listOf(Color(0xFF0381FE), Color(0xFF0072DE)),
                        checked = allowTagging,
                        onCheckedChange = { allowTagging = it }
                    )

                    AppleSettingDivider()

                    AppleSettingToggleRow(
                        title = "Show Online Activity",
                        subtitle = "Show when you are active",
                        icon = Lucide.Activity,
                        iconGradient = listOf(Color(0xFF0381FE), Color(0xFF0072DE)),
                        checked = showOnline,
                        onCheckedChange = { showOnline = it }
                    )
                }
            }

            item {
                AppleSettingsGroup(
                    header = "Blocked Accounts (${blockedAccounts.size})",
                    footer = "Blocked accounts cannot message you or appear in your Discover feed. Tap Unblock to restore access."
                ) {
                    if (isLoadingBlocked) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF0381FE)
                            )
                            Text("Loading blocked accounts...", fontSize = 13.sp, color = colors.textMuted)
                        }
                    } else if (blockedAccounts.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Lucide.ShieldCheck, null, tint = colors.textMuted, modifier = Modifier.size(18.dp))
                            Text("You haven't blocked any accounts", fontSize = 13.5.sp, color = colors.textMuted)
                        }
                    } else {
                        blockedAccounts.forEachIndexed { index, blocked ->
                            if (index > 0) AppleSettingDivider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (blocked.avatarUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = blocked.avatarUrl,
                                        contentDescription = blocked.displayName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(colors.glassBg)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF0381FE).copy(alpha = 0.14f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = blocked.displayName.take(1).uppercase(),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0381FE)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = blocked.displayName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textMain,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (blocked.username.isNotBlank()) {
                                        Text(
                                            text = "@${blocked.username}",
                                            fontSize = 12.sp,
                                            color = colors.textMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF0381FE).copy(alpha = 0.12f),
                                    modifier = Modifier.clickable {
                                        val uid = currentUid ?: return@clickable
                                        val previous = blockedAccounts
                                        blockedAccounts = blockedAccounts.filterNot { it.uid == blocked.uid }
                                        scope.launch {
                                            try {
                                                FirebaseFirestore.getInstance()
                                                    .collection("users")
                                                    .document(uid)
                                                    .update("blockedUserIds", FieldValue.arrayRemove(blocked.uid))
                                                    .await()
                                                ToastManager.showToast("Unblocked ${blocked.displayName}")
                                            } catch (e: Exception) {
                                                blockedAccounts = previous
                                                ToastManager.showToast("Failed to unblock: ${e.localizedMessage}")
                                            }
                                        }
                                    }
                                ) {
                                    Text(
                                        text = "Unblock",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0381FE),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
