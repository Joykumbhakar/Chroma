package com.chroma.studio.ui.screens

import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.data.DiscoverRepository
import com.chroma.studio.model.UserProfile
import com.chroma.studio.ui.components.AiWaveSphereIcon
import com.chroma.studio.ui.components.ChromaHeaderActionButton
import com.chroma.studio.ui.components.ChromaScreenHeader
import com.chroma.studio.ui.components.ToastManager
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.chroma.studio.utils.compressImageToUnder50KB
import com.composables.icons.lucide.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Flagship Apple HIG Post Details & Publishing Screen.
 * 100% capsule-free squircle geometry, full dark/light theme integration,
 * interactive tag & collaborator management, quick-tag suggestions, and live user mentions.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PostDetailsScreen(
    imageUri: Uri,
    selectedTemplate: String,
    aspectRatio: String = "4:5",
    work: com.chroma.studio.model.ChromaWork? = null,
    onBack: () -> Unit,
    onPostSuccess: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val surfaceColor = if (colors.isDark) Color(0xFF1C1C1E) else Color.White
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val discoverRepo = remember { DiscoverRepository() }

    var caption by remember { mutableStateOf(work?.name ?: "") }
    var description by remember { mutableStateOf(work?.description ?: "") }
    var tagInput by remember { mutableStateOf("") }
    var mentionInput by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf(listOf<String>()) }
    var mentions by remember { mutableStateOf(listOf<String>()) }
    var isPosting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Live mention suggestions
    var suggestedUsers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }

    LaunchedEffect(mentionInput) {
        val q = mentionInput.trim().removePrefix("@").lowercase()
        if (q.length >= 2) {
            try {
                val users = discoverRepo.getSuggestedCreators(10)
                suggestedUsers = users.filter { u ->
                    u.username.lowercase().contains(q) || u.displayName.lowercase().contains(q)
                }
            } catch (_: Exception) {
                suggestedUsers = emptyList()
            }
        } else {
            suggestedUsers = emptyList()
        }
    }

    val popularTags = listOf("art", "design", "3d", "vector", "minimal", "creative", "studio")

    fun addTag(customTag: String? = null) {
        val raw = customTag ?: tagInput
        val t = raw.trim().removePrefix("#").lowercase()
        if (t.isNotEmpty() && !tags.contains(t) && tags.size < 20) {
            tags = tags + t
            SoundManager.playPop()
        }
        tagInput = ""
    }

    fun addMention(customUser: String? = null) {
        val raw = customUser ?: mentionInput
        val m = raw.trim().removePrefix("@").lowercase()
        if (m.isNotEmpty() && !mentions.contains(m) && mentions.size < 20) {
            mentions = mentions + m
            SoundManager.playPop()
        }
        mentionInput = ""
        suggestedUsers = emptyList()
    }

    suspend fun compressAndUpload() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            errorMessage = "You must be logged in to post"
            return
        }
        if (caption.isBlank()) {
            errorMessage = "Please enter a caption for your post"
            return
        }
        isPosting = true
        errorMessage = null
        try {
            if (work != null) {
                // Publish JSON work
                val modifiedWork = work.copy(name = caption.trim(), description = description.trim())
                withContext(Dispatchers.IO) {
                    discoverRepo.publishWork(
                        work = modifiedWork,
                        aspectRatio = work.canvasShape,
                        tags = tags,
                        mentions = mentions
                    )
                }
                SoundManager.playSend()
                ToastManager.showToast("Work published successfully")
                onPostSuccess()
            } else {
                // Publish Image Post
                val imageBytes = withContext(Dispatchers.IO) {
                    compressImageToUnder50KB(context, imageUri)
                } ?: throw Exception("Could not read image bytes")

                val success = withContext(Dispatchers.IO) {
                    discoverRepo.uploadImagePost(
                        imageBytes = imageBytes,
                        caption = caption.trim(),
                        description = if (selectedTemplate == "None") "" else "Template: $selectedTemplate",
                        aspectRatio = aspectRatio,
                        tags = tags,
                        mentions = mentions
                    )
                }
                if (success) {
                    SoundManager.playSend()
                    ToastManager.showToast("Post published successfully")
                    onPostSuccess()
                } else {
                    errorMessage = "Failed to publish post. Please check connection and try again."
                }
            }
        } catch (e: Exception) {
            Log.e("PostDetailsScreen", "Upload error", e)
            errorMessage = "Error publishing: ${e.message}"
        } finally {
            isPosting = false
        }
    }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            ChromaScreenHeader(
                title = "Oscellion",
                badgeText = "Post Details",
                showBackButton = true,
                onBack = onBack,
                actions = {
                    ChromaHeaderActionButton(
                        text = "Post",
                        icon = Lucide.Send,
                        enabled = !isPosting && caption.isNotBlank(),
                        isLoading = isPosting,
                        accentColor = Color(0xFF007AFF),
                        onClick = {
                            SoundManager.playReaction()
                            scope.launch { compressAndUpload() }
                        }
                    )
                }
            )
        },
        containerColor = colors.bg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(14.dp))

            // ── 1. Hero Preview & Caption Glass Card ──────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(surfaceColor)
                    .border(1.dp, colors.glassBorder, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Visual Thumbnail
                        if (work != null) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.glassBg)
                                    .border(1.dp, colors.glassBorder, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Lucide.Layers,
                                        contentDescription = "Canvas",
                                        modifier = Modifier.size(36.dp),
                                        tint = colors.primary
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = "Canvas",
                                        color = colors.textMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.glassBg)
                                    .border(1.dp, colors.glassBorder, RoundedCornerShape(14.dp))
                            ) {
                                AsyncImage(
                                    model = imageUri,
                                    contentDescription = "Post preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        // Caption Text Field
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = caption,
                                onValueChange = { if (it.length <= 2200) caption = it },
                                placeholder = {
                                    Text(
                                        text = "Write a caption...",
                                        color = colors.textMuted.copy(alpha = 0.6f),
                                        fontSize = 14.5.sp
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.primary.copy(alpha = 0.7f),
                                    unfocusedBorderColor = colors.glassBorder,
                                    focusedContainerColor = colors.glassBg,
                                    unfocusedContainerColor = colors.glassBg,
                                    focusedTextColor = colors.textMain,
                                    unfocusedTextColor = colors.textMain,
                                    cursorColor = colors.primary
                                ),
                                minLines = 4,
                                maxLines = 7,
                                supportingText = {
                                    Text(
                                        text = "${caption.length}/2200",
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                        fontSize = 11.sp,
                                        color = colors.textMuted
                                    )
                                },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
                            )
                        }
                    }

                    // Metadata badges below
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Ratio Badge
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.glassBg)
                                .border(0.5.dp, colors.glassBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Lucide.Maximize2, null, tint = colors.textMuted, modifier = Modifier.size(12.dp))
                            Text("Ratio: $aspectRatio", fontSize = 11.5.sp, color = colors.textMuted, fontWeight = FontWeight.Medium)
                        }

                        // Template Badge
                        if (selectedTemplate != "None" && work == null) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF007AFF).copy(alpha = 0.12f))
                                    .border(0.5.dp, Color(0xFF007AFF).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                AiWaveSphereIcon(modifier = Modifier.size(12.dp))
                                Text(selectedTemplate, fontSize = 11.5.sp, color = Color(0xFF007AFF), fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Encryption Badge
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF34C759).copy(alpha = 0.1f))
                                .border(0.5.dp, Color(0xFF34C759).copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Lucide.ShieldCheck, null, tint = Color(0xFF34C759), modifier = Modifier.size(12.dp))
                            Text("Encrypted", fontSize = 11.5.sp, color = Color(0xFF34C759), fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Error Message Banner
            if (errorMessage != null) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFF3B30).copy(alpha = 0.1f))
                        .border(1.dp, Color(0xFFFF3B30).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Lucide.Info, null, tint = Color(0xFFFF3B30), modifier = Modifier.size(16.dp))
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFFF3B30),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── 2. Tags Section ───────────────────────────────────────────────
            Text(
                text = "TAGS & CATEGORIES",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMuted,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
            Text(
                text = "Help creators discover your work through relevant search keywords",
                fontSize = 12.sp,
                color = colors.textMuted,
                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { if (it.length <= 30) tagInput = it },
                    placeholder = { Text("Add tag (e.g. 3d, design)", color = colors.textMuted.copy(alpha = 0.5f), fontSize = 13.5.sp) },
                    leadingIcon = { Icon(Lucide.Hash, null, tint = colors.textMuted, modifier = Modifier.size(17.dp)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.glassBorder,
                        focusedContainerColor = colors.glassBg,
                        unfocusedContainerColor = colors.glassBg,
                        focusedTextColor = colors.textMain,
                        unfocusedTextColor = colors.textMain,
                        cursorColor = colors.primary
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { addTag() })
                )

                // Apple squircle add button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.primary)
                        .clickable { addTag() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Lucide.Plus, contentDescription = "Add Tag", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            // Quick suggestion tags
            if (tags.size < 5) {
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(start = 2.dp)
                ) {
                    popularTags.filter { !tags.contains(it) }.take(5).forEach { tag ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(surfaceColor)
                                .border(0.5.dp, colors.glassBorder, RoundedCornerShape(8.dp))
                                .clickable { addTag(tag) }
                                .padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text("+ #$tag", fontSize = 11.5.sp, color = colors.textMuted, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Active tags chips
            if (tags.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { tag ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(colors.primary.copy(alpha = 0.12f))
                                .border(1.dp, colors.primary.copy(alpha = 0.28f), RoundedCornerShape(9.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#$tag",
                                color = colors.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = Lucide.X,
                                contentDescription = "Remove tag",
                                tint = colors.primary,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable {
                                        SoundManager.playPop()
                                        tags = tags.filter { it != tag }
                                    }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── 3. Mentions Section ───────────────────────────────────────────
            Text(
                text = "COLLABORATORS & MENTIONS",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMuted,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
            Text(
                text = "Mention team members or fellow artists to notify them",
                fontSize = 12.sp,
                color = colors.textMuted,
                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = mentionInput,
                    onValueChange = { if (it.length <= 30) mentionInput = it },
                    placeholder = { Text("Tag user (e.g. username)", color = colors.textMuted.copy(alpha = 0.5f), fontSize = 13.5.sp) },
                    leadingIcon = { Icon(Lucide.AtSign, null, tint = colors.textMuted, modifier = Modifier.size(17.dp)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF34C759),
                        unfocusedBorderColor = colors.glassBorder,
                        focusedContainerColor = colors.glassBg,
                        unfocusedContainerColor = colors.glassBg,
                        focusedTextColor = colors.textMain,
                        unfocusedTextColor = colors.textMain,
                        cursorColor = Color(0xFF34C759)
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { addMention() })
                )

                // Apple squircle add button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF34C759))
                        .clickable { addMention() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Lucide.Plus, contentDescription = "Add Mention", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            // Live autocomplete suggestions
            AnimatedVisibility(visible = suggestedUsers.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(surfaceColor)
                        .border(1.dp, colors.glassBorder, RoundedCornerShape(14.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    suggestedUsers.take(4).forEach { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    addMention(user.username.ifBlank { user.displayName })
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AsyncImage(
                                model = user.avatarUrl,
                                contentDescription = user.displayName,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.glassBg),
                                contentScale = ContentScale.Crop
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.displayName.ifBlank { user.username },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textMain
                                )
                                Text(
                                    text = "@${user.username}",
                                    fontSize = 11.5.sp,
                                    color = colors.textMuted
                                )
                            }
                            Text(
                                text = "Tag",
                                fontSize = 12.sp,
                                color = Color(0xFF34C759),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Active mentions chips
            if (mentions.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    mentions.forEach { mention ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(Color(0xFF34C759).copy(alpha = 0.12f))
                                .border(1.dp, Color(0xFF34C759).copy(alpha = 0.3f), RoundedCornerShape(9.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "@$mention",
                                color = Color(0xFF34C759),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = Lucide.X,
                                contentDescription = "Remove mention",
                                tint = Color(0xFF34C759),
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable {
                                        SoundManager.playPop()
                                        mentions = mentions.filter { it != mention }
                                    }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── 4. Publish Primary Action Button ──────────────────────────────
            CupertinoPrimaryButton(
                onClick = {
                    SoundManager.playReaction()
                    scope.launch { compressAndUpload() }
                },
                enabled = !isPosting && caption.isNotBlank(),
                containerColor = Color(0xFF007AFF),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isPosting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Publishing to Feed...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "Publish Post",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Lucide.Send,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(36.dp))
        }
    }
}
