package com.chroma.studio.ui.screens
import com.chroma.studio.ui.components.GlassTextButton
import com.chroma.studio.ui.components.GlassIconButton

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.composables.icons.lucide.Image
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import com.composables.icons.lucide.ChevronRight

private val GlossyBorderBrush = Brush.linearGradient(
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
fun CreatePostScreen(
    onNavigateNext: (Uri, String, String) -> Unit, // passes image uri, selected template, and aspect ratio
    onBack: () -> Unit
) {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedTemplate by remember { mutableStateOf<String>("None") }
    var selectedAspectRatio by remember { mutableStateOf<String>("4:5") }

    val templates = listOf("None", "Cartoon", "Anime", "Monochrome", "Cyberpunk", "Vintage", "Watercolor")
    val aspectRatios = listOf("1:1", "4:5", "16:9", "3:4")

    val cropImage = rememberLauncherForActivityResult(com.canhub.cropper.CropImageContract()) { result ->
        if (result.isSuccessful) {
            val uriContent = result.uriContent
            selectedImageUri = uriContent
            selectedAspectRatio = "Cropped" // The cropped image dictates the final ratio
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            cropImage.launch(
                com.canhub.cropper.CropImageContractOptions(
                    uri = uri,
                    cropImageOptions = com.canhub.cropper.CropImageOptions(
                        guidelines = com.canhub.cropper.CropImageView.Guidelines.ON,
                        allowFlipping = true,
                        allowRotation = true,
                        fixAspectRatio = false,
                        showIntentChooser = true
                    )
                )
            )
        }
    }

    Scaffold(
        topBar = {
            com.chroma.studio.ui.components.ChromaScreenHeader(
                title = "Oscellion",
                badgeText = "New Post",
                showBackButton = true,
                onBack = onBack,
                actions = {
                    if (selectedImageUri != null) {
                        com.chroma.studio.ui.components.ChromaHeaderActionButton(
                            icon = Lucide.ChevronRight,
                            contentDescription = "Next",
                            tint = Color(0xFF007AFF),
                            onClick = {
                                selectedImageUri?.let { uri ->
                                    onNavigateNext(uri, selectedTemplate, selectedAspectRatio)
                                }
                            }
                        )
                    }
                }
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Image Picker Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFF8FAFF))
                    .border(1.5.dp, GlossyBorderBrush, RoundedCornerShape(24.dp))
                    .clickable { galleryLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Selected Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Lucide.Image,
                            contentDescription = "Pick Image",
                            modifier = Modifier.size(48.dp),
                            tint = Color.Black.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tap to upload & crop photo",
                            color = Color.Black.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Apply Template",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = "Choose a style for your photo (AI processing will be applied later)",
                fontSize = 14.sp,
                color = Color.Black.copy(alpha = 0.5f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            val chunked = templates.chunked(3)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                chunked.forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        rowItems.forEach { template ->
                            val isSelected = selectedTemplate == template
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color(0xFF007AFF) else Color.White)
                                    .let { m -> if (!isSelected) m.border(1.5.dp, GlossyBorderBrush, CircleShape) else m }
                                    .clickable { selectedTemplate = template },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = template,
                                    color = if (isSelected) Color.White else Color.Black,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                        // Fill empty slots in the last row
                        repeat(3 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
