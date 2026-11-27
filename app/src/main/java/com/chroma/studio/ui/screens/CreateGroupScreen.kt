package com.chroma.studio.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.chroma.studio.chat.data.User
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.chat.ui.components.GroupContact
import com.chroma.studio.chat.ui.components.GroupCreationContent
import com.chroma.studio.chat.ui.theme.ChatAppTheme
import com.chroma.studio.data.OnlineStatusManager
import com.chroma.studio.ui.components.ToastManager
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.chroma.studio.viewmodel.ChatViewModel
import com.google.firebase.auth.FirebaseAuth

/**
 * Standalone navigation screen for Group Creation Onboarding (`create_group` route).
 * Uses the unified Apple HIG `GroupCreationContent` wrapped in `ChatAppTheme` so Light/Dark
 * modes, Gallery/Icon Studio avatars, Co-Admin toggles, Disappearing Messages, Privacy
 * Access, and Invite Links integrate directly with `ChatViewModel` & Firestore.
 */
@Composable
fun CreateGroupScreen(
    chatViewModel: ChatViewModel,
    onBack: () -> Unit,
    onGroupCreated: (String) -> Unit
) {
    val oscellionColors = LocalOscellionColors.current
    val context = LocalContext.current
    val userProfiles by chatViewModel.userProfiles.collectAsState()
    val firebaseUser = FirebaseAuth.getInstance().currentUser
    val currentUid = firebaseUser?.uid.orEmpty()
    val myProfile = userProfiles[currentUid]

    val currentUser = remember(currentUid, myProfile, firebaseUser) {
        User(
            id = currentUid,
            name = myProfile?.displayName?.takeIf { it.isNotBlank() }
                ?: firebaseUser?.displayName?.takeIf { it.isNotBlank() }
                ?: "You",
            username = myProfile?.username?.takeIf { it.isNotBlank() } ?: "you",
            avatar = myProfile?.avatarUrl.orEmpty(),
            status = if (myProfile?.online == true || OnlineStatusManager.isUserOnline(currentUid)) "Online" else "Active on Oscellion",
            isVerified = myProfile?.verified == true,
            verifiedTone = myProfile?.verifiedTone
        )
    }

    val contactCandidates = remember(userProfiles, currentUid) {
        userProfiles.values
            .filter { it.uid.isNotBlank() && it.uid != currentUid }
            .map { prof ->
                val isOnline = prof.online || OnlineStatusManager.isUserOnline(prof.uid)
                GroupContact(
                    id = prof.uid,
                    name = prof.displayName.ifBlank { prof.username.ifBlank { "User" } },
                    role = if (prof.bio.isNotBlank()) prof.bio else "@${prof.username}",
                    avatar = prof.avatarUrl,
                    status = if (isOnline) "Online" else "Active on Oscellion",
                    isVerified = prof.verified,
                    verifiedTone = prof.verifiedTone
                )
            }
    }

    ChatAppTheme(darkTheme = oscellionColors.isDark) {
        GroupCreationContent(
            currentUser = currentUser,
            availableContacts = contactCandidates,
            onDismiss = onBack,
            onCreateGroup = { config ->
                chatViewModel.createGroupFromConfig(
                    context = context,
                    config = config
                ) { roomId ->
                    SoundManager.playSend()
                    ToastManager.showToast("Group \"${config.groupName}\" created")
                    onGroupCreated(roomId)
                }
            }
        )
    }
}
