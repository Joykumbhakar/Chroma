package com.chroma.studio.ui.screens
import com.chroma.studio.ui.components.ChromaScreenHeader
import com.chroma.studio.ui.components.ChromaHeaderActionButton
import com.chroma.studio.ui.components.OscellionLoadingSpinner
import com.chroma.studio.ui.components.GlassIconButton
import com.chroma.studio.ui.components.GlassButton
import com.chroma.studio.ui.components.LiquidGlassProgressBar

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chroma.studio.data.DiscoverRepository
import com.chroma.studio.model.UserProfile
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.chroma.studio.viewmodel.FlashViewModel
import com.composables.icons.lucide.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun FlashPostScreen(
    flashVm: FlashViewModel,
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
    val scope = rememberCoroutineScope()
    val colors = LocalOscellionColors.current

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
        // Extract the current @mention being typed (last @word)
        val lastAt = caption.lastIndexOf('@')
        if (lastAt >= 0) {
            val partial = caption.substring(lastAt + 1).takeWhile { it.isLetterOrDigit() || it == '_' }
            if (partial.length >= 2) {
                mentionSearchQuery = partial
                delay(300)
                // Search users by username prefix
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

    // Upload state
    val isUploading by flashVm.isUploading.collectAsState()
    val uploadProgress by flashVm.uploadProgress.collectAsState()
    val uploadError by flashVm.uploadError.collectAsState()

    // Show error
    LaunchedEffect(uploadError) {
        if (uploadError != null) {
            delay(3000)
            flashVm.clearUploadError()
        }
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
                ChromaHeaderActionButton(
                    text = "Post",
                    icon = Lucide.Send,
                    enabled = !isUploading,
                    isLoading = isUploading,
                    onClick = {
                        if (!isUploading) {
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
                                onSuccess = { onSuccess() },
                                onError = { /* shown via uploadError state */ }
                            )
                        }
                    }
                )
            }
        )

        // Upload progress bar
        if (isUploading) {
            LiquidGlassProgressBar(
                progress = uploadProgress,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                height = 6.dp
            )
            Text(
                "Uploading… ${(uploadProgress * 100).toInt()}%",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp)
            )
        }

        // Error banner
        if (uploadError != null) {
            Surface(
                color = Color(0xFFFF3B30).copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Lucide.Globe, null, tint = Color(0xFFFF3B30), modifier = Modifier.size(16.dp))
                    Text(uploadError ?: "", color = Color(0xFFFF3B30), fontSize = 13.sp)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))

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
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.08f))
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
                            Icon(Lucide.Image, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(32.dp))
                        }
                    }

                    // Edit overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(24.dp)
                            .clip(CircleShape)
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
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF007AFF),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            cursorColor = Color(0xFF007AFF)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 6,
                        visualTransformation = { text ->
                            // Highlight hashtags and mentions
                            val annotated = buildAnnotatedString {
                                val raw = text.text
                                var cursor = 0
                                val regex = Regex("""([#@]\w+)""")
                                regex.findAll(raw).forEach { match ->
                                    if (match.range.first > cursor) {
                                        append(raw.substring(cursor, match.range.first))
                                    }
                                    withStyle(SpanStyle(color = Color(0xFF4DA3FF), fontWeight = FontWeight.SemiBold)) {
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
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                    )
                }
            }

            // ── Mention suggestions ──────────────────────────────────────────
            if (mentionSuggestions.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(mentionSuggestions) { user ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                                .clickable {
                                    // Replace partial mention with full username
                                    val lastAt = caption.lastIndexOf('@')
                                    if (lastAt >= 0) {
                                        val before = caption.substring(0, lastAt)
                                        caption = "$before@${user.username} "
                                    }
                                    mentionSuggestions = emptyList()
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF007AFF))) {
                                if (user.avatarUrl.isNotBlank()) {
                                    AsyncImage(model = user.avatarUrl, contentDescription = null,
                                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                }
                            }
                            Text("@${user.username}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            if (user.verified) {
                                Icon(Lucide.BadgeCheck, null, tint = Color(0xFF007AFF), modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Parsed tags preview ──────────────────────────────────────────
            if (hashtags.isNotEmpty() || mentions.isNotEmpty()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    if (hashtags.isNotEmpty()) {
                        Text("Hashtags", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            hashtags.forEach { tag ->
                                Text(
                                    "#$tag",
                                    color = Color(0xFF4DA3FF),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFF007AFF).copy(alpha = 0.12f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    if (mentions.isNotEmpty()) {
                        Text("Tagged People", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            mentions.forEach { username ->
                                Text(
                                    "@$username",
                                    color = Color(0xFF4DA3FF),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFF007AFF).copy(alpha = 0.12f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Info cards ───────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoCard(icon = Lucide.Film, title = "Flash Video", subtitle = "Visible on the Flash Feed")
                InfoCard(icon = Lucide.Clock, title = "Duration", subtitle = "${(trimEndMs - trimStartMs) / 1000}s")
                InfoCard(icon = Lucide.Globe, title = "Audience", subtitle = "Everyone")
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun InfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = Color(0xFF007AFF), modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
        }
    }
}
