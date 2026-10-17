package com.chroma.studio.ui.screens
import com.chroma.studio.ui.components.ChromaScreenHeader
import com.chroma.studio.ui.components.ChromaHeaderActionButton
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.chroma.studio.ui.components.OscellionLoadingSpinner
import com.chroma.studio.ui.components.GlassIconButton
import com.chroma.studio.ui.components.AiWaveSphereIcon

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.composables.icons.lucide.*
import com.chroma.studio.data.DiscoverRepository
import com.chroma.studio.ui.components.ToastManager
import com.chroma.studio.utils.compressImageToUnder50KB
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val PostDetailsGlossyBrush = Brush.linearGradient(
    colors = listOf(
        Color(0xFF007AFF).copy(alpha = 0.6f),
        Color.Transparent,
        Color(0xFF00C6FF).copy(alpha = 0.3f),
        Color.Transparent,
        Color(0xFF007AFF).copy(alpha = 0.6f)
    ),
    start = androidx.compose.ui.geometry.Offset(0f, 0f),
    end = androidx.compose.ui.geometry.Offset(1000f, 1000f)
)

@OptIn(ExperimentalMaterial3Api::class)
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

    fun addTag() {
        val t = tagInput.trim().removePrefix("#").lowercase()
        if (t.isNotEmpty() && !tags.contains(t) && tags.size < 20) {
            tags = tags + t
        }
        tagInput = ""
    }

    fun addMention() {
        val m = mentionInput.trim().removePrefix("@").lowercase()
        if (m.isNotEmpty() && !mentions.contains(m) && mentions.size < 20) {
            mentions = mentions + m
        }
        mentionInput = ""
    }

    suspend fun compressAndUpload() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            errorMessage = "You must be logged in to post"
            return
        }
        if (caption.isBlank()) {
            errorMessage = "Please add a caption"
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
                ToastManager.showToast("Work published successfully")
                onPostSuccess()
            } else {
                // Publish Image Post
                val imageBytes = withContext(Dispatchers.IO) {
                    compressImageToUnder50KB(context, imageUri)
                } ?: throw Exception("Could not read image")
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
                    ToastManager.showToast("Post published successfully")
                    onPostSuccess()
                } else {
                    errorMessage = "Failed to publish. Please try again."
                }
            }
        } catch (e: Exception) {
            Log.e("PostDetailsScreen", "Upload error", e)
            errorMessage = "Error: ${e.message}"
        } finally {
            isPosting = false
        }
    }

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
                        onClick = { scope.launch { compressAndUpload() } }
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            // Preview Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                if (work != null) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.05f))
                            .border(1.5.dp, PostDetailsGlossyBrush, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Lucide.Image, contentDescription = "JSON Canvas", modifier = Modifier.size(64.dp), tint = Color.Black.copy(alpha = 0.4f))
                        Spacer(Modifier.height(16.dp))
                            Text("Canvas", color = Color.Black.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                } else {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Post preview",
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.5.dp, PostDetailsGlossyBrush, RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    if (selectedTemplate != "None" && work == null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFF007AFF).copy(alpha = 0.1f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            AiWaveSphereIcon(modifier = Modifier.size(13.dp))
                            Text(selectedTemplate, fontSize = 12.sp, color = Color(0xFF007AFF), fontWeight = FontWeight.Medium)
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { if (it.length <= 2200) caption = it },
                        placeholder = { Text("Write a caption...", color = Color.Black.copy(alpha = 0.35f)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF007AFF).copy(alpha = 0.5f),
                            unfocusedBorderColor = Color.Black.copy(alpha = 0.12f),
                            focusedContainerColor = Color(0xFFF5F9FF),
                            unfocusedContainerColor = Color(0xFFF8FAFF),
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        ),
                        minLines = 4,
                        maxLines = 8,
                        supportingText = {
                            Text(
                                text = "${caption.length}/2200",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                fontSize = 11.sp,
                                color = Color.Black.copy(alpha = 0.45f)
                            )
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
                    )
                }
            }


            // Error above submit
            if (errorMessage != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    errorMessage!!,
                    color = Color(0xFFFF3B30),
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFF3B30).copy(alpha = 0.08f))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            // Tags Section
            Text("Tags", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { if (it.length <= 30) tagInput = it },
                    placeholder = { Text("Add a tag (max 20)", color = Color.Black.copy(alpha = 0.35f)) },
                    leadingIcon = { Icon(Lucide.Hash, null, tint = Color.Black.copy(alpha = 0.4f), modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.weight(1f),
                    shape = CircleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF007AFF).copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.Black.copy(alpha = 0.12f),
                        focusedContainerColor = Color(0xFFF5F9FF),
                        unfocusedContainerColor = Color(0xFFF8FAFF),
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF007AFF))
                        .clickable { addTag() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Light)
                }
            }
            if (tags.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                ChipRow(items = tags, color = Color(0xFF007AFF)) { removed ->
                    tags = tags.filter { it != removed }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Mentions Section
            Text("Mentions", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = mentionInput,
                    onValueChange = { if (it.length <= 30) mentionInput = it },
                    placeholder = { Text("Tag a user (max 20)", color = Color.Black.copy(alpha = 0.35f)) },
                    leadingIcon = { Icon(Lucide.AtSign, null, tint = Color.Black.copy(alpha = 0.4f), modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.weight(1f),
                    shape = CircleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF007AFF).copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.Black.copy(alpha = 0.12f),
                        focusedContainerColor = Color(0xFFF5F9FF),
                        unfocusedContainerColor = Color(0xFFF8FAFF),
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF007AFF))
                        .clickable { addMention() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Light)
                }
            }
            if (mentions.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                ChipRow(items = mentions, color = Color(0xFF34C759), prefix = "@") { removed ->
                    mentions = mentions.filter { it != removed }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(
    items: List<String>,
    color: Color,
    prefix: String = "#",
    onRemove: (String) -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f))
                    .border(1.dp, color.copy(alpha = 0.3f), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$prefix$item",
                    color = color,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Lucide.X,
                    contentDescription = "Remove",
                    tint = color,
                    modifier = Modifier
                        .size(14.dp)
                        .clickable { onRemove(item) }
                )
            }
        }
    }
}
