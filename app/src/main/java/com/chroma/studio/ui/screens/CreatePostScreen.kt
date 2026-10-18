package com.chroma.studio.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.ui.components.ChromaHeaderActionButton
import com.chroma.studio.ui.components.ChromaScreenHeader
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*

private data class TemplateStyle(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color
)

private data class RatioOption(
    val id: String,
    val label: String,
    val subtitle: String,
    val widthRatio: Float,
    val heightRatio: Float
)

/**
 * Flagship Apple HIG New Post Creation Screen.
 * 100% capsule-free squircle geometry, full dark/light theme integration,
 * interactive aspect ratio selector, stylized template cards, and haptic sound effects.
 */
@Composable
fun CreatePostScreen(
    onNavigateNext: (Uri, String, String) -> Unit, // passes image uri, selected template, and aspect ratio
    onBack: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val surfaceColor = if (colors.isDark) Color(0xFF1C1C1E) else Color.White

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var rawOriginalUri by remember { mutableStateOf<Uri?>(null) }
    var selectedTemplate by remember { mutableStateOf("None") }
    var selectedAspectRatio by remember { mutableStateOf("4:5") }

    val templateStyles = remember {
        listOf(
            TemplateStyle("None", "Original", "Pure visual", Lucide.Sparkle, Color(0xFF007AFF)),
            TemplateStyle("Cartoon", "Cartoon", "Vibrant 2D", Lucide.Palette, Color(0xFFFF9500)),
            TemplateStyle("Anime", "Anime", "Cel-shaded", Lucide.Flame, Color(0xFFFF2D55)),
            TemplateStyle("Monochrome", "Monochrome", "High contrast B&W", Lucide.Contrast, Color(0xFF8E8E93)),
            TemplateStyle("Cyberpunk", "Cyberpunk", "Neon glow", Lucide.Zap, Color(0xFFAF52DE)),
            TemplateStyle("Vintage", "Vintage", "Classic film grain", Lucide.Film, Color(0xFF5856D6)),
            TemplateStyle("Watercolor", "Watercolor", "Aquarelle wash", Lucide.Paintbrush, Color(0xFF34C759))
        )
    }

    val aspectRatios = remember {
        listOf(
            RatioOption("1:1", "1:1", "Square", 1f, 1f),
            RatioOption("4:5", "4:5", "Portrait", 4f, 5f),
            RatioOption("16:9", "16:9", "Landscape", 16f, 9f),
            RatioOption("3:4", "3:4", "Standard", 3f, 4f)
        )
    }

    val cropImage = rememberLauncherForActivityResult(com.canhub.cropper.CropImageContract()) { result ->
        if (result.isSuccessful) {
            val uriContent = result.uriContent
            selectedImageUri = uriContent
            SoundManager.playPop()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            rawOriginalUri = uri
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

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            ChromaScreenHeader(
                title = "Oscellion",
                badgeText = "New Post",
                showBackButton = true,
                onBack = onBack,
                actions = {
                    ChromaHeaderActionButton(
                        text = "Next",
                        icon = Lucide.ChevronRight,
                        enabled = selectedImageUri != null,
                        accentColor = Color(0xFF007AFF),
                        onClick = {
                            selectedImageUri?.let { uri ->
                                SoundManager.playReaction()
                                onNavigateNext(uri, selectedTemplate, selectedAspectRatio)
                            }
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
            Spacer(modifier = Modifier.height(14.dp))

            // ── 1. Hero Image / Media Upload Box ─────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(310.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(surfaceColor)
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                colors.glassBorder,
                                colors.primary.copy(alpha = if (selectedImageUri != null) 0.4f else 0.15f),
                                colors.glassBorder
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable {
                        SoundManager.playPop()
                        galleryLauncher.launch("image/*")
                    },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Selected Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    // Top Action Overlay Floating Bar
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (colors.isDark) Color(0xCC000000) else Color(0xCCFFFFFF))
                            .border(0.5.dp, colors.glassBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Re-crop button
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    SoundManager.playPop()
                                    val targetUri = rawOriginalUri ?: selectedImageUri
                                    if (targetUri != null) {
                                        cropImage.launch(
                                            com.canhub.cropper.CropImageContractOptions(
                                                uri = targetUri,
                                                cropImageOptions = com.canhub.cropper.CropImageOptions(
                                                    guidelines = com.canhub.cropper.CropImageView.Guidelines.ON,
                                                    allowFlipping = true,
                                                    allowRotation = true,
                                                    fixAspectRatio = false
                                                )
                                            )
                                        )
                                    }
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.Crop,
                                contentDescription = "Crop",
                                modifier = Modifier.size(14.dp),
                                tint = colors.textMain
                            )
                            Text(
                                text = "Crop",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textMain
                            )
                        }

                        // Replace image button
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    SoundManager.playPop()
                                    galleryLauncher.launch("image/*")
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.Image,
                                contentDescription = "Change",
                                modifier = Modifier.size(14.dp),
                                tint = colors.textMain
                            )
                            Text(
                                text = "Change",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textMain
                            )
                        }

                        // Remove image
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    SoundManager.playPop()
                                    selectedImageUri = null
                                    rawOriginalUri = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.X,
                                contentDescription = "Remove",
                                modifier = Modifier.size(15.dp),
                                tint = Color(0xFFFF3B30)
                            )
                        }
                    }

                    // Ratio indicator badge on bottom-left
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xCC000000))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Ratio: $selectedAspectRatio",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                } else {
                    // Empty state prompt
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.primary.copy(alpha = 0.12f))
                                .border(1.dp, colors.primary.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.ImagePlus,
                                contentDescription = "Upload Photo",
                                modifier = Modifier.size(30.dp),
                                tint = colors.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Select Photo to Publish",
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Tap to open gallery, crop & align with high resolution",
                            fontSize = 12.5.sp,
                            color = colors.textMuted
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (colors.isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = colors.textMain
                            )
                            Text(
                                text = "Browse Photos",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textMain
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── 2. Aspect Ratio Selector (Apple Segmented Style) ──────────────
            Text(
                text = "ASPECT RATIO",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMuted,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(surfaceColor)
                    .border(0.5.dp, colors.glassBorder, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                aspectRatios.forEach { option ->
                    val isSelected = selectedAspectRatio == option.id
                    val animatedBg by animateColorAsState(
                        targetValue = if (isSelected) (if (colors.isDark) Color(0xFF3A3A3C) else Color.White) else Color.Transparent,
                        animationSpec = tween(180),
                        label = "ratioBg"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(animatedBg)
                            .let {
                                if (isSelected) {
                                    it.border(
                                        0.5.dp,
                                        if (colors.isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                                        RoundedCornerShape(9.dp)
                                    )
                                } else it
                            }
                            .clickable {
                                SoundManager.playPop()
                                selectedAspectRatio = option.id
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = option.label,
                                fontSize = 13.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) colors.primary else colors.textMain
                            )
                            Text(
                                text = option.subtitle,
                                fontSize = 9.5.sp,
                                color = colors.textMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── 3. Template & Art Styles Selection ─────────────────────────────
            Text(
                text = "STUDIO TEMPLATE STYLES",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMuted,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
            Text(
                text = "Apply AI rendering shaders or aesthetic filters to your visual",
                fontSize = 12.sp,
                color = colors.textMuted,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
            )

            // Grid layout with 2 columns
            val rows = templateStyles.chunked(2)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { style ->
                            val isSelected = selectedTemplate == style.id
                            val cardBg by animateColorAsState(
                                targetValue = if (isSelected) style.accent.copy(alpha = 0.12f) else surfaceColor,
                                animationSpec = tween(180),
                                label = "templateBg"
                            )
                            val borderColor by animateColorAsState(
                                targetValue = if (isSelected) style.accent else colors.glassBorder,
                                animationSpec = tween(180),
                                label = "templateBorder"
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(cardBg)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                        color = borderColor,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        SoundManager.playPop()
                                        selectedTemplate = style.id
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(style.accent.copy(alpha = if (isSelected) 0.22f else 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = style.icon,
                                            contentDescription = null,
                                            tint = style.accent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = style.title,
                                            fontSize = 13.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = colors.textMain
                                        )
                                        Text(
                                            text = style.subtitle,
                                            fontSize = 11.sp,
                                            color = colors.textMuted,
                                            maxLines = 1
                                        )
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Lucide.Check,
                                            contentDescription = "Selected",
                                            tint = style.accent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Fill gap if odd number of items in row
                        if (rowItems.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // ── 4. Primary Continue Action ────────────────────────────────────
            CupertinoPrimaryButton(
                onClick = {
                    selectedImageUri?.let { uri ->
                        SoundManager.playReaction()
                        onNavigateNext(uri, selectedTemplate, selectedAspectRatio)
                    }
                },
                enabled = selectedImageUri != null,
                containerColor = Color(0xFF007AFF),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (selectedImageUri != null) "Continue to Post Details" else "Select a Photo to Continue",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (selectedImageUri != null) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Lucide.ArrowRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
