package com.chroma.studio.ui.screens



import com.chroma.studio.ui.components.VerifiedBadge



import android.graphics.Bitmap

import android.net.Uri

import android.widget.Toast

import androidx.compose.animation.*

import androidx.compose.foundation.*

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.LazyRow

import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.asImageBitmap

import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.text.SpanStyle

import androidx.compose.ui.text.buildAnnotatedString

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.withStyle

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import coil.compose.AsyncImage

import com.chroma.studio.data.DiscoverRepository

import com.chroma.studio.data.DraftPostRepository

import com.chroma.studio.data.FlashUploadManager

import com.chroma.studio.model.CreatorPostDraft

import com.chroma.studio.model.UserProfile

import com.chroma.studio.ui.components.ChromaHeaderActionButton
import com.chroma.studio.ui.components.LiquidGlassPrimaryButton
import com.chroma.studio.ui.components.LiquidGlassSecondaryButton

import com.chroma.studio.ui.components.ChromaScreenHeader

import com.chroma.studio.ui.theme.LocalOscellionColors

import com.chroma.studio.viewmodel.FlashViewModel

import com.composables.icons.lucide.*

import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.delay

import kotlinx.coroutines.launch

import kotlinx.coroutines.withContext

import java.util.UUID

import com.chroma.studio.model.DiscoverPost

import com.chroma.studio.model.FlashVideo

import com.chroma.studio.viewmodel.DiscoverViewModel



@OptIn(ExperimentalMaterial3Api::class)

@Composable

fun FlashPostScreen(

    flashVm: FlashViewModel,

    discoverViewModel: DiscoverViewModel? = null,

    videoUri: Uri,

    thumbnailBitmap: Bitmap?,

    trimStartMs: Long,

    trimEndMs: Long,

    filterName: String,

    brightness: Float = 0f,

    contrast: Float = 1f,

    saturation: Float = 1f,

    hue: Float = 0f,

    mediaType: com.chroma.studio.model.FlashMediaType,

    photoUris: List<Uri> = emptyList(),

    durationPerSlideMs: Long = 3000L,

    remixedFromPostId: String? = null,

    remixedFromAuthorUsername: String? = null,

    onSuccess: () -> Unit,

    onBack: () -> Unit

) {

    val context = LocalContext.current

    val colors = LocalOscellionColors.current

    val draftRepo = remember { DraftPostRepository(context) }

    val effectiveDiscoverVm: DiscoverViewModel = discoverViewModel

        ?: androidx.lifecycle.viewmodel.compose.viewModel()



    // Caption state

    var caption by remember { mutableStateOf(if (remixedFromAuthorUsername != null) "Remix of @$remixedFromAuthorUsername " else "") }



    // Parsed hashtags and mentions

    val hashtags by remember(caption) {

        derivedStateOf {

            Regex("""#(\w+)""").findAll(caption).map { it.groupValues[1] }.toList()

        }

    }

    val mentions by remember(caption) {

        derivedStateOf {

            Regex("""@(\w+)""").findAll(caption).map { it.groupValues[1] }.toList()

        }

    }



    // User search for mentions

    var mentionSearchQuery by remember { mutableStateOf("") }

    var mentionSuggestions by remember { mutableStateOf<List<UserProfile>>(emptyList()) }

    val discoverRepo = remember { DiscoverRepository() }



    LaunchedEffect(caption) {

        val lastAt = caption.lastIndexOf('@')

        if (lastAt >= 0) {

            val partial = caption.substring(lastAt + 1).takeWhile { it.isLetterOrDigit() || it == '_' }

            if (partial.length >= 2) {

                mentionSearchQuery = partial

                delay(300)

                val results = withContext(Dispatchers.IO) {

                    try {

                        discoverRepo.searchUsersByUsername(partial)

                    } catch (e: Exception) { emptyList() }

                }

                mentionSuggestions = results

            } else {

                mentionSuggestions = emptyList()

            }

        } else {

            mentionSuggestions = emptyList()

        }

    }



    // Current thumbnail

    var currentThumbnail by remember { mutableStateOf(thumbnailBitmap) }

    var showThumbnailPicker by remember { mutableStateOf(false) }

    LaunchedEffect(videoUri, mediaType) {
        if (currentThumbnail == null && mediaType == com.chroma.studio.model.FlashMediaType.VIDEO) {
            withContext(Dispatchers.IO) {
                try {
                    val retriever = android.media.MediaMetadataRetriever()
                    if (videoUri.scheme == "file") {
                        retriever.setDataSource(videoUri.path)
                    } else {
                        retriever.setDataSource(context, videoUri)
                    }
                    val frame = retriever.getFrameAtTime(trimStartMs * 1000L, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?: retriever.getFrameAtTime()
                    retriever.release()
                    if (frame != null) {
                        withContext(Dispatchers.Main) {
                            currentThumbnail = frame
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("FlashPostScreen", "Could not extract video thumbnail", e)
                }
            }
        }
    }



    // Helper to persist draft locally

    fun saveDraftLocally(showToast: Boolean = false): String {

        val draftId = UUID.randomUUID().toString()

        val draft = CreatorPostDraft(

            id = draftId,

            title = if (caption.isNotBlank()) caption.take(40) else "New Flash Video",

            description = caption,

            mode = "flash",

            aspectRatio = "9:16",

            videoUri = videoUri.toString(),

            flashTrimStartMs = trimStartMs,

            flashTrimEndMs = trimEndMs,

            hashtags = hashtags,

            mentions = mentions

        )

        draftRepo.saveDraft(draft)

        if (showToast) {

            Toast.makeText(context, "Draft saved successfully", Toast.LENGTH_SHORT).show()

        }

        return draftId

    }



    // Executes background post and immediately redirects to Community tab (Instagram UX)

    fun executePostToCommunity() {

        val savedDraftId = saveDraftLocally(showToast = false)



        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser

        val uid = currentUser?.uid ?: "local_user"

        val rawName = currentUser?.displayName?.ifBlank { null } ?: "Creator"

        val authorUsername = rawName.lowercase().replace(" ", ".")

        val avatarUrl = currentUser?.photoUrl?.toString() ?: ""

        val tempPostId = "temp_post_${System.currentTimeMillis()}"

        val tempFlashId = "temp_flash_${System.currentTimeMillis()}"



        val optimisticPost = DiscoverPost(

            id = tempPostId,

            creatorId = uid,

            authorUsername = authorUsername,

            authorDisplayName = rawName,

            authorAvatarUrl = avatarUrl,

            authorVerified = false,

            title = if (caption.isNotBlank()) caption.take(40) else "New Flash Video",

            description = caption,

            aspectRatio = "9:16",

            postType = "video",

            videoUrl = videoUri.toString(),

            thumbnailUrl = "",

            tags = hashtags,

            mentions = mentions,

            timestamp = System.currentTimeMillis()

        )

        effectiveDiscoverVm.addOptimisticPost(optimisticPost)



        val optimisticFlash = FlashVideo(

            id = tempFlashId,

            creatorId = uid,

            authorUsername = authorUsername,

            authorDisplayName = rawName,

            authorAvatarUrl = avatarUrl,

            caption = caption,

            videoUrl = videoUri.toString(),

            thumbnailUrl = "",

            hashtags = hashtags,

            mentions = mentions,

            timestamp = System.currentTimeMillis()

        )

        flashVm.addOptimisticFlash(optimisticFlash)



        fun startPublish() {

            FlashUploadManager.startUpload(

                thumbnailBitmap = currentThumbnail,

                title = if (caption.isNotBlank()) caption else "New Flash Video",

                statusText = "Posting video to Community…",

                draftId = savedDraftId,

                retryAction = { startPublish() }

            )



            flashVm.publishFlash(

                context = context,

                mediaUri = videoUri,

                mediaType = mediaType,

                trimStartMs = trimStartMs,

                trimEndMs = trimEndMs,

                thumbnailBitmap = currentThumbnail,

                caption = caption,

                hashtags = hashtags,

                mentions = mentions,

                filterName = filterName,

                brightness = brightness,

                contrast = contrast,

                saturation = saturation,

                hue = hue,

                imageUris = photoUris,

                durationPerSlideMs = durationPerSlideMs,

                remixedFromPostId = remixedFromPostId,

                remixedFromAuthorUsername = remixedFromAuthorUsername,

                onSuccess = {

                    draftRepo.deleteDraft(savedDraftId)

                    FlashUploadManager.setSuccess("Posted! 🎉")

                    effectiveDiscoverVm.removeOptimisticPost(tempPostId)

                    flashVm.removeOptimisticFlash(tempFlashId)

                    effectiveDiscoverVm.loadWorks(isRefresh = true)

                    flashVm.loadFeed(isRefresh = true)

                },

                onError = { err ->

                    effectiveDiscoverVm.removeOptimisticPost(tempPostId)

                    flashVm.removeOptimisticFlash(tempFlashId)

                    FlashUploadManager.setFailed(err) { startPublish() }

                }

            )

        }



        // Trigger upload

        startPublish()



        // Immediate Instagram-style redirection to Community tab

        onSuccess()

    }



    Column(

        modifier = Modifier

            .fillMaxSize()

            .background(colors.bg)

    ) {

        // ── Top bar ──────────────────────────────────────────────────────────

        ChromaScreenHeader(

            title = "Oscellion",

            badgeText = "New Flash",

            showBackButton = true,

            onBack = onBack,

            actions = {

                // Draft Button

                OutlinedButton(

                    onClick = { saveDraftLocally(showToast = true) },

                    shape = RoundedCornerShape(8.dp),

                    border = BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.2f) else Color(0xFFCBD5E1)),

                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),

                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textMain),

                    modifier = Modifier.padding(end = 6.dp)

                ) {

                    Text("Draft", fontSize = 12.sp, fontWeight = FontWeight.Medium)

                }



                ChromaHeaderActionButton(

                    text = "Post",

                    icon = Lucide.Send,

                    enabled = true,

                    isLoading = false,

                    onClick = { executePostToCommunity() }

                )

            }

        )



        HorizontalDivider(color = colors.glassBorder, thickness = 0.5.dp)



        Column(

            modifier = Modifier

                .fillMaxSize()

                .verticalScroll(rememberScrollState())

        ) {

            Spacer(Modifier.height(14.dp))



            // ── Thumbnail + Caption ──────────────────────────────────────────

            Row(

                modifier = Modifier

                    .fillMaxWidth()

                    .padding(horizontal = 16.dp),

                horizontalArrangement = Arrangement.spacedBy(12.dp)

            ) {

                // Thumbnail

                Box(

                    modifier = Modifier

                        .size(100.dp, 140.dp)

                        .clip(RoundedCornerShape(10.dp))

                        .background(Color.White.copy(alpha = 0.08f))

                        .border(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFE2E8F0), RoundedCornerShape(10.dp))

                        .clickable { showThumbnailPicker = !showThumbnailPicker }

                ) {

                    if (currentThumbnail != null) {

                        androidx.compose.foundation.Image(

                            bitmap = currentThumbnail!!.asImageBitmap(),

                            contentDescription = "Thumbnail",

                            contentScale = ContentScale.Crop,

                            modifier = Modifier.fillMaxSize()

                        )

                    } else {

                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

                            Icon(Lucide.Image, null, tint = colors.textMuted, modifier = Modifier.size(32.dp))

                        }

                    }



                    // Edit overlay

                    Box(

                        modifier = Modifier

                            .align(Alignment.TopEnd)

                            .padding(6.dp)

                            .size(24.dp)

                            .clip(RoundedCornerShape(6.dp))

                            .background(Color.Black.copy(alpha = 0.7f)),

                        contentAlignment = Alignment.Center

                    ) {

                        Icon(Lucide.Pencil, null, tint = Color.White, modifier = Modifier.size(12.dp))

                    }

                }



                // Caption field

                Column(modifier = Modifier.weight(1f)) {

                    OutlinedTextField(

                        value = caption,

                        onValueChange = { if (it.length <= 2200) caption = it },

                        placeholder = {

                            Text(

                                "Write a caption, add #hashtags or @mention people...",

                                color = colors.textMuted,

                                fontSize = 13.sp

                            )

                        },

                        modifier = Modifier

                            .fillMaxWidth()

                            .height(140.dp),

                        colors = OutlinedTextFieldDefaults.colors(

                            focusedTextColor = colors.textMain,

                            unfocusedTextColor = colors.textMain,

                            focusedBorderColor = Color(0xFF2563EB),

                            unfocusedBorderColor = if (colors.isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFCBD5E1),

                            cursorColor = Color(0xFF2563EB)

                        ),

                        shape = RoundedCornerShape(10.dp),

                        maxLines = 6,

                        visualTransformation = { text ->

                            val annotated = buildAnnotatedString {

                                val raw = text.text

                                var cursor = 0

                                val regex = Regex("""([#@]\w+)""")

                                regex.findAll(raw).forEach { match ->

                                    if (match.range.first > cursor) {

                                        append(raw.substring(cursor, match.range.first))

                                    }

                                    withStyle(SpanStyle(color = Color(0xFF2563EB), fontWeight = FontWeight.SemiBold)) {

                                        append(match.value)

                                    }

                                    cursor = match.range.last + 1

                                }

                                if (cursor < raw.length) append(raw.substring(cursor))

                            }

                            androidx.compose.ui.text.input.TransformedText(

                                annotated,

                                androidx.compose.ui.text.input.OffsetMapping.Identity

                            )

                        }

                    )

                    Text(

                        "${caption.length}/2200",

                        color = colors.textMuted,

                        fontSize = 11.sp,

                        modifier = Modifier.align(Alignment.End).padding(top = 4.dp)

                    )

                }

            }



            // ── Mention suggestions ──────────────────────────────────────────

            if (mentionSuggestions.isNotEmpty()) {

                Spacer(Modifier.height(10.dp))

                LazyRow(

                    contentPadding = PaddingValues(horizontal = 16.dp),

                    horizontalArrangement = Arrangement.spacedBy(8.dp)

                ) {

                    items(mentionSuggestions) { user ->

                        Row(

                            modifier = Modifier

                                .clip(RoundedCornerShape(8.dp))

                                .background(if (colors.isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF))

                                .border(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFBFDBFE), RoundedCornerShape(8.dp))

                                .clickable {

                                    val lastAt = caption.lastIndexOf('@')

                                    if (lastAt >= 0) {

                                        val before = caption.substring(0, lastAt)

                                        caption = "$before@${user.username} "

                                    }

                                    mentionSuggestions = emptyList()

                                }

                                .padding(horizontal = 10.dp, vertical = 6.dp),

                            verticalAlignment = Alignment.CenterVertically,

                            horizontalArrangement = Arrangement.spacedBy(6.dp)

                        ) {

                            Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(Color(0xFF2563EB))) {

                                if (user.avatarUrl.isNotBlank()) {

                                    AsyncImage(

                                        model = user.avatarUrl,

                                        contentDescription = null,

                                        contentScale = ContentScale.Crop,

                                        modifier = Modifier.fillMaxSize()

                                    )

                                }

                            }

                            Text("@${user.username}", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                            if (user.verified) {

                                VerifiedBadge(size = 12.dp)

                            }

                        }

                    }

                }

            }



            Spacer(Modifier.height(16.dp))

            HorizontalDivider(color = colors.glassBorder, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

            Spacer(Modifier.height(14.dp))



            // ── Parsed tags preview ──────────────────────────────────────────

            if (hashtags.isNotEmpty() || mentions.isNotEmpty()) {

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                    if (hashtags.isNotEmpty()) {

                        Text("Hashtags", color = colors.textMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                        Spacer(Modifier.height(6.dp))

                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {

                            hashtags.forEach { tag ->

                                Text(

                                    "#$tag",

                                    color = Color(0xFF2563EB),

                                    fontSize = 12.sp,

                                    fontWeight = FontWeight.SemiBold,

                                    modifier = Modifier

                                        .clip(RoundedCornerShape(6.dp))

                                        .background(Color(0xFF2563EB).copy(alpha = 0.12f))

                                        .padding(horizontal = 8.dp, vertical = 4.dp)

                                )

                            }

                        }

                        Spacer(Modifier.height(12.dp))

                    }



                    if (mentions.isNotEmpty()) {

                        Text("Tagged People", color = colors.textMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                        Spacer(Modifier.height(6.dp))

                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {

                            mentions.forEach { username ->

                                Text(

                                    "@$username",

                                    color = Color(0xFF2563EB),

                                    fontSize = 12.sp,

                                    fontWeight = FontWeight.SemiBold,

                                    modifier = Modifier

                                        .clip(RoundedCornerShape(6.dp))

                                        .background(Color(0xFF2563EB).copy(alpha = 0.12f))

                                        .padding(horizontal = 8.dp, vertical = 4.dp)

                                )

                            }

                        }

                    }

                }

                Spacer(Modifier.height(14.dp))

                HorizontalDivider(color = colors.glassBorder, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

                Spacer(Modifier.height(14.dp))

            }



            // ── Clean Info rows (No double cards, separated by hr lines) ─────

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                InfoRow(icon = Lucide.Film, title = "Flash Video", subtitle = "Visible on the Community & Flash Feed")

                HorizontalDivider(color = colors.glassBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 10.dp))

                InfoRow(icon = Lucide.Clock, title = "Duration", subtitle = "${((trimEndMs - trimStartMs) / 1000L).coerceAtLeast(1L)}s")

                HorizontalDivider(color = colors.glassBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 10.dp))

                InfoRow(icon = Lucide.Globe, title = "Audience", subtitle = "Public (Everyone)")

            }



            Spacer(Modifier.height(28.dp))



            // Bottom Post Button

            Box(

                modifier = Modifier

                    .fillMaxWidth()

                    .padding(horizontal = 16.dp)

            ) {

                LiquidGlassPrimaryButton(
                    text = "Post Flash Video",
                    leadingIcon = Lucide.Send,
                    onClick = { executePostToCommunity() },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )

            }



            Spacer(Modifier.height(40.dp))

        }

    }

}



@Composable

private fun InfoRow(

    icon: androidx.compose.ui.graphics.vector.ImageVector,

    title: String,

    subtitle: String

) {

    val colors = LocalOscellionColors.current

    Row(

        modifier = Modifier.fillMaxWidth(),

        verticalAlignment = Alignment.CenterVertically,

        horizontalArrangement = Arrangement.spacedBy(12.dp)

    ) {

        Box(

            modifier = Modifier

                .size(36.dp)

                .clip(RoundedCornerShape(8.dp))

                .background(Color(0xFF2563EB).copy(alpha = 0.12f)),

            contentAlignment = Alignment.Center

        ) {

            Icon(icon, null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))

        }

        Column(modifier = Modifier.weight(1f)) {

            Text(title, color = colors.textMain, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)

            Text(subtitle, color = colors.textMuted, fontSize = 11.5.sp)

        }

    }

}

