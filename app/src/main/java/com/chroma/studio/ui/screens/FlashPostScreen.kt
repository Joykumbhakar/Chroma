package com.chroma.studio.ui.screens

import com.chroma.studio.chat.sound.SoundManager

import com.chroma.studio.ui.theme.InterFontFamily

import androidx.compose.ui.graphics.Brush

import androidx.compose.ui.draw.shadow

import androidx.compose.ui.text.TextStyle

import androidx.compose.foundation.text.BasicTextField



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



data class FlashPostParams(
    val flashVm: FlashViewModel,
    val discoverViewModel: DiscoverViewModel? = null,
    val videoUri: Uri,
    val thumbnailBitmap: Bitmap?,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    val filterName: String = "",
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val hue: Float = 0f,
    val mediaType: com.chroma.studio.model.FlashMediaType = com.chroma.studio.model.FlashMediaType.VIDEO,
    val photoUris: List<Uri> = emptyList(),
    val durationPerSlideMs: Long = 3000L,
    val remixedFromPostId: String? = null,
    val remixedFromAuthorUsername: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashPostScreen(
    params: FlashPostParams,
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val flashVm = params.flashVm
    val discoverViewModel = params.discoverViewModel
    val videoUri = params.videoUri
    val thumbnailBitmap = params.thumbnailBitmap
    val trimStartMs = params.trimStartMs
    val trimEndMs = params.trimEndMs
    val filterName = params.filterName
    val brightness = params.brightness
    val contrast = params.contrast
    val saturation = params.saturation
    val hue = params.hue
    val mediaType = params.mediaType
    val photoUris = params.photoUris
    val durationPerSlideMs = params.durationPerSlideMs
    val remixedFromPostId = params.remixedFromPostId
    val remixedFromAuthorUsername = params.remixedFromAuthorUsername

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
        // ── Samsung One UI Top Navigation Bar ─────────────────────────────────
        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bg)
                .padding(top = statusBarTop + 6.dp, bottom = 10.dp, start = 12.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        SoundManager.playTap(context)
                        onBack()
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Lucide.ArrowLeft,
                        contentDescription = "Back",
                        tint = colors.textMain,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "New Flash",
                        color = colors.textMain,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    )
                    Text(
                        text = "Video & Reels",
                        color = colors.textMuted,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Draft Capsule
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (colors.isDark) Color(0xFF1E2028) else Color(0xFFF1F3F7))
                        .clickable {
                            SoundManager.playTap(context)
                            saveDraftLocally(showToast = true)
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "Draft",
                        color = colors.textMain,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Post Capsule
                Box(
                    modifier = Modifier
                        .shadow(
                            elevation = 2.dp,
                            shape = RoundedCornerShape(999.dp),
                            spotColor = Color(0xFF0381FE).copy(alpha = 0.35f)
                        )
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0072DE), Color(0xFF0381FE))
                            )
                        )
                        .clickable {
                            SoundManager.playTap(context)
                            executePostToCommunity()
                        }
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.Send,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Post",
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = colors.glassBorder.copy(alpha = 0.35f), thickness = 0.5.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(14.dp))

            // ── One UI 22.dp Media Preview + Caption Input Card ─────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (colors.isDark) Color(0xFF1A1C23) else Color(0xFFF8FAFD))
                    .border(
                        width = 0.5.dp,
                        color = if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Thumbnail
                    Box(
                        modifier = Modifier
                            .size(96.dp, 136.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (colors.isDark) Color(0xFF12141A) else Color(0xFFE8EDF5))
                            .clickable { showThumbnailPicker = !showThumbnailPicker }
                    ) {
                        if (currentThumbnail != null) {
                            Image(
                                bitmap = currentThumbnail!!.asImageBitmap(),
                                contentDescription = "Thumbnail",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Lucide.Film, null, tint = colors.textMuted, modifier = Modifier.size(28.dp))
                            }
                        }

                        // Duration overlay badge
                        val durSec = ((trimEndMs - trimStartMs) / 1000L).coerceAtLeast(1L)
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(6.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.70f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text("${durSec}s", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        // Edit pencil overlay
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.70f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Lucide.Pencil, null, tint = Color.White, modifier = Modifier.size(11.dp))
                        }
                    }

                    // Caption Input Area
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(136.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        BasicTextField(
                            value = caption,
                            onValueChange = { if (it.length <= 2200) caption = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            textStyle = TextStyle(
                                color = colors.textMain,
                                fontSize = 14.sp,
                                fontFamily = InterFontFamily
                            ),
                            decorationBox = { innerTextField ->
                                if (caption.isEmpty()) {
                                    Text(
                                        text = "Write a caption, add #hashtags or @mention creators...",
                                        color = colors.textMuted,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                                innerTextField()
                            }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(Color(0xFF0381FE).copy(alpha = 0.12f))
                                        .clickable {
                                            caption = if (caption.endsWith(" ") || caption.isEmpty()) "${caption}#" else "$caption #"
                                        }
                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Text("#tag", color = Color(0xFF0381FE), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(Color(0xFF0381FE).copy(alpha = 0.12f))
                                        .clickable {
                                            caption = if (caption.endsWith(" ") || caption.isEmpty()) "${caption}@" else "$caption @"
                                        }
                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Text("@user", color = Color(0xFF0381FE), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Text(
                                text = "${caption.length}/2200",
                                color = colors.textMuted,
                                fontSize = 10.5.sp
                            )
                        }
                    }
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
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (colors.isDark) Color(0xFF1E2028) else Color(0xFFF1F3F7))
                                .border(
                                    width = 0.5.dp,
                                    color = if (colors.isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(999.dp)
                                )
                                .clickable {
                                    val lastAt = caption.lastIndexOf('@')
                                    if (lastAt >= 0) {
                                        val before = caption.substring(0, lastAt)
                                        caption = "$before@${user.username} "
                                    }
                                    mentionSuggestions = emptyList()
                                }
                                .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF0381FE))) {
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
                                    color = Color(0xFF0381FE),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(Color(0xFF0381FE).copy(alpha = 0.12f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }

                    if (mentions.isNotEmpty()) {
                        Text("Tagged People", color = colors.textMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            mentions.forEach { username ->
                                Text(
                                    "@$username",
                                    color = Color(0xFF0381FE),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(Color(0xFF0381FE).copy(alpha = 0.12f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            // ── One UI 22.dp Settings Group Card ────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (colors.isDark) Color(0xFF1A1C23) else Color(0xFFF8FAFD))
                    .border(
                        width = 0.5.dp,
                        color = if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    InfoRow(icon = Lucide.Film, title = "Flash Video", subtitle = "Visible on Community & Flash Feed", badge = "Shorts")
                    HorizontalDivider(color = colors.glassBorder.copy(alpha = 0.35f), thickness = 0.5.dp)
                    InfoRow(icon = Lucide.Clock, title = "Duration", subtitle = "Video playtime", badge = "${((trimEndMs - trimStartMs) / 1000L).coerceAtLeast(1L)}s")
                    HorizontalDivider(color = colors.glassBorder.copy(alpha = 0.35f), thickness = 0.5.dp)
                    InfoRow(icon = Lucide.Globe, title = "Audience", subtitle = "Visible to everyone", badge = "Public")
                }
            }

            Spacer(Modifier.height(28.dp))

            // One UI Signature Action Capsule
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Button(
                    onClick = {
                        SoundManager.playTap(context)
                        executePostToCommunity()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0381FE),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(
                            elevation = 3.dp,
                            shape = RoundedCornerShape(999.dp),
                            spotColor = Color(0xFF0381FE).copy(alpha = 0.35f)
                        )
                ) {
                    Icon(
                        imageVector = Lucide.Send,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Post Flash Video",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    badge: String? = null
) {
    val colors = LocalOscellionColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0381FE).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color(0xFF0381FE), modifier = Modifier.size(18.dp))
            }
            Column {
                Text(title, color = colors.textMain, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(subtitle, color = colors.textMuted, fontSize = 12.sp)
            }
        }

        if (badge != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (colors.isDark) Color(0xFF22242C) else Color(0xFFE8EDF5))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badge,
                    color = colors.textMain,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
