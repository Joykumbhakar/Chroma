package com.chroma.studio.ui.components

import com.chroma.studio.ui.components.VerifiedBadge

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.chroma.studio.R
import com.chroma.studio.model.UserProfile
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Background style mode for the Pass Card
 */
enum class PassBackgroundMode {
    DEFAULT,
    GRADIENT_MAKER,
    CUSTOM_IMAGE
}

/**
 * Frame/container styling for QR Code
 */
enum class QrFrameStyle {
    SOLID_WHITE,
    FROSTED_GLASS,
    SOLID_DARK,
    MINIMAL
}

/**
 * Gradient preset for Chroma Studio Gradient Maker
 */
data class GradientPreset(
    val name: String,
    val colors: List<Color>
)

val CHROMA_GRADIENT_PRESETS = listOf(
    GradientPreset("Obsidian Stealth", listOf(Color(0xFF141416), Color(0xFF222228), Color(0xFF0C0C0E))),
    GradientPreset("Cosmic Sapphire", listOf(Color(0xFF0F172A), Color(0xFF1E40AF), Color(0xFF3B82F6))),
    GradientPreset("Pacific Titanium", listOf(Color(0xFF0A2540), Color(0xFF0284C7), Color(0xFF0EA5E9))),
    GradientPreset("Emerald Luxury", listOf(Color(0xFF064E3B), Color(0xFF047857), Color(0xFF10B981))),
    GradientPreset("Midnight Royal", listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155))),
    GradientPreset("Ocean Radiance", listOf(Color(0xFF0A2540), Color(0xFF0381FE), Color(0xFF38BDF8))),
    GradientPreset("Golden Amber", listOf(Color(0xFF451A03), Color(0xFF92400E), Color(0xFFD97706))),
    GradientPreset("Studio Minimal", listOf(Color(0xFFF8FAFC), Color(0xFFEEF2F6), Color(0xFFE2E8F0)))
)

val QR_COLOR_PRESETS = listOf(
    Color(0xFF1D1D1F), // Jet Black
    Color(0xFFFFFFFF), // Pure White
    Color(0xFF0381FE), // System Blue
    Color(0xFF34C759), // Mint Green
    Color(0xFFFF9500), // Sunset Amber
    Color(0xFFFF2D55), // Crimson
    Color(0xFF0072DE), // Brand Cobalt
    Color(0xFF32ADE6)  // Cyan
)

val ACCENT_COLOR_PRESETS = listOf(
    Color(0xFF0381FE), // System Blue
    Color(0xFF34C759), // Mint Green
    Color(0xFFFF9500), // Amber
    Color(0xFFFF2D55), // Crimson
    Color(0xFF32ADE6), // Cyan
    Color(0xFF5856D6)  // Indigo
)

/**
 * Customizable state of the Profile Pass Card
 */
data class PassCustomization(
    val bgMode: PassBackgroundMode = PassBackgroundMode.DEFAULT,
    val gradientColors: List<Color> = listOf(Color(0xFF0381FE), Color(0xFF0051D5), Color(0xFF32ADE6)),
    val gradientAngle: Float = 45f,
    val customImageUri: Uri? = null,
    val customImageBitmap: Bitmap? = null,
    val imageBlur: Float = 0f,
    val imageScrimAlpha: Float = 0.25f,
    val qrColor: Color = Color(0xFF1D1D1F),
    val qrFrameStyle: QrFrameStyle = QrFrameStyle.SOLID_WHITE,
    val isDarkText: Boolean = true,
    val accentColor: Color = Color(0xFF0381FE)
)

/**
 * Base Pass Theme enum (simplified to 'Default' as requested)
 */
enum class PassTheme(
    val title: String,
    val borderGradient: List<Color>,
    val bgGradient: List<Color>,
    val isDarkCard: Boolean,
    val accentColor: Color,
    val buttonBg: Color,
    val textPrimary: Color,
    val textSecondary: Color
) {
    DEFAULT(
        title = "Default",
        borderGradient = listOf(Color.White.copy(alpha = 0.9f), Color(0xFFDBEAFE)),
        bgGradient = listOf(Color(0xFFEEF2FF), Color(0xFFDBEAFE)),
        isDarkCard = false,
        accentColor = Color(0xFF0381FE),
        buttonBg = Color(0xFFF2F2F7),
        textPrimary = Color(0xFF1D1D1F),
        textSecondary = Color(0xFF86868B)
    )
}

/**
 * Generates an Android Bitmap for a real scannable QR Code using ZXing.
 */
fun generateQrBitmap(
    content: String,
    sizePx: Int = 512,
    foregroundColor: Int = android.graphics.Color.BLACK,
    backgroundColor: Int = android.graphics.Color.TRANSPARENT
): Bitmap? {
    return try {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.MARGIN to 0
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val width = matrix.width
        val height = matrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (matrix.get(x, y)) foregroundColor else backgroundColor)
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000f)
        count >= 1_000 -> String.format("%.1fK", count / 1_000f)
        else -> count.toString()
    }
}

/**
 * Profile Pass Card View with Customization and Role display
 */
@Composable
fun ProfilePassCardView(
    profile: UserProfile,
    userId: String,
    customization: PassCustomization = PassCustomization(),
    worksCount: Int = 0,
    isFlipped: Boolean = false,
    onFlipCard: (() -> Unit)? = null,
    onQrClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val scope = rememberCoroutineScope()
    val profileShareUrl = "https://oscellion.vercel.app/u/${profile.username.ifBlank { userId }}"

    var internalFlipped by remember { mutableStateOf(false) }
    val effectiveFlipped = isFlipped || internalFlipped
    val rotation by animateFloatAsState(
        targetValue = if (effectiveFlipped) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
        label = "passCardFlip"
    )

    var isCopied by remember { mutableStateOf(false) }

    val qrBitmap = remember(profileShareUrl, customization.qrColor) {
        generateQrBitmap(
            profileShareUrl,
            sizePx = 480,
            foregroundColor = customization.qrColor.toArgb()
        )
    }

    val primaryTextColor = if (!customization.isDarkText) Color.White else Color(0xFF1D1D1F)
    val secondaryTextColor = if (!customization.isDarkText) Color.White.copy(alpha = 0.70f) else Color(0xFF86868B)

    val cardShape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 14f * density
            }
            .clip(cardShape)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = if (!customization.isDarkText) 0.35f else 0.15f),
                        Color.White.copy(alpha = if (!customization.isDarkText) 0.08f else 0.04f),
                        Color.White.copy(alpha = if (!customization.isDarkText) 0.20f else 0.10f)
                    )
                ),
                shape = cardShape
            )
            .clickable {
                if (onFlipCard != null) onFlipCard()
                else internalFlipped = !internalFlipped
            }
    ) {
        // ── Card Background Layer ──
        when (customization.bgMode) {
            PassBackgroundMode.CUSTOM_IMAGE -> {
                if (customization.customImageUri != null || customization.customImageBitmap != null) {
                    Box(modifier = Modifier.matchParentSize()) {
                        if (customization.customImageBitmap != null) {
                            Image(
                                bitmap = customization.customImageBitmap.asImageBitmap(),
                                contentDescription = "Custom Background",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(if (customization.imageBlur > 0f) Modifier.blur(customization.imageBlur.dp) else Modifier),
                                contentScale = ContentScale.Crop
                            )
                        } else if (customization.customImageUri != null) {
                            AsyncImage(
                                model = customization.customImageUri,
                                contentDescription = "Custom Background",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(if (customization.imageBlur > 0f) Modifier.blur(customization.imageBlur.dp) else Modifier),
                                contentScale = ContentScale.Crop
                            )
                        }
                        // Scrim overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = customization.imageScrimAlpha))
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Brush.linearGradient(customization.gradientColors))
                    )
                }
            }
            PassBackgroundMode.GRADIENT_MAKER -> {
                val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
                val savedPassLayersJson = prefs.getString("custom_pass_card_layers", null)
                val customLayers = remember(savedPassLayersJson, customization.gradientColors) {
                    if (!savedPassLayersJson.isNullOrBlank()) {
                        try {
                            com.chroma.studio.data.WorkRepository(context).deserializeLayers(savedPassLayersJson)
                        } catch (e: Exception) {
                            null
                        }
                    } else null
                }

                if (!customLayers.isNullOrEmpty()) {
                    Box(modifier = Modifier.matchParentSize()) {
                        CanvasPreview(
                            layers = customLayers,
                            shape = "rounded",
                            borderColor = Color.Transparent,
                            isStatic = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Brush.linearGradient(customization.gradientColors))
                    )
                }
            }
            PassBackgroundMode.DEFAULT -> {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0F1117), Color(0xFF191D26), Color(0xFF0C0E12))
                            )
                        )
                        .drawBehind {
                            // Specular radial glow from top-right corner
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(Color(0xFF0381FE).copy(alpha = 0.20f), Color.Transparent),
                                    center = Offset(size.width * 0.85f, size.height * 0.15f),
                                    radius = size.width * 0.75f
                                )
                            )
                            // Subtle geometric dot matrix
                            val dotSpacing = 20.dp.toPx()
                            val dotRadius = 0.8.dp.toPx()
                            val dotColor = Color.White.copy(alpha = 0.04f)
                            var x = dotSpacing
                            while (x < size.width) {
                                var y = dotSpacing
                                while (y < size.height) {
                                    drawCircle(color = dotColor, radius = dotRadius, center = Offset(x, y))
                                    y += dotSpacing
                                }
                                x += dotSpacing
                            }
                        }
                )
            }
        }

        // Specular top highlight hairline
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.65f),
                            Color.White.copy(alpha = 0.20f),
                            Color.Transparent
                        )
                    )
                )
        )

        // ── Card Foreground Content (Flippable Front / Back) ──
        if (rotation <= 90f) {
            // FRONT FACE: Authentic Creator Pass Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        AiWaveSphereIcon(modifier = Modifier.size(20.dp))
                        Text(
                            text = "OSCELLION",
                            color = primaryTextColor,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color.White.copy(alpha = 0.10f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(100.dp))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (profile.verified) Color(0xFFFFD700) else Color(0xFF34C759))
                        )
                        Text(
                            text = if (profile.verified) "VERIFIED PASS" else "CREATOR PASS",
                            color = primaryTextColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = Color.White.copy(alpha = 0.10f)
                )

                // Identity Hero
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val statusRingBrush = getAppleStatusRingBrush(
                        hasStatus = true,
                        isSeen = false,
                        isDark = true,
                        isVerified = profile.verified
                    )
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .border(2.dp, statusRingBrush, CircleShape)
                            .padding(3.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        IosAvatarView(
                            name = profile.displayName.ifBlank { profile.username },
                            avatarUrl = profile.avatarUrl,
                            size = 56.dp,
                            gender = profile.gender,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = profile.displayName.ifBlank { "Creator" },
                                color = primaryTextColor,
                                fontSize = 18.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (profile.verified) {
                                VerifiedBadge(
                                    size = 16.dp,
                                    tone = profile.verifiedTone,
                                    showGlow = true
                                )
                            }
                        }

                        Text(
                            text = "@${profile.username.ifBlank { "creator" }}",
                            color = secondaryTextColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )

                        // Status pill
                        val followerStr = formatCount(profile.followersCount)
                        val roleStr = profile.role?.takeIf { it.isNotBlank() } ?: "Creator"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.10f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "$followerStr Followers  •  $roleStr",
                                color = primaryTextColor.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Smart Chip & Serial
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(26.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFFFD700), Color(0xFFB8860B), Color(0xFFD4AF37))
                                )
                            )
                            .border(0.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(5.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxSize(0.6f)
                                .border(0.5.dp, Color(0xFF4A3510), RoundedCornerShape(2.dp))
                        )
                    }

                    Text(
                        text = "OSC • ${userId.take(8).uppercase()} • VERIFIED",
                        color = secondaryTextColor.copy(alpha = 0.70f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.2.sp
                    )
                }

                // Tap Hint
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Lucide.QrCode,
                        contentDescription = null,
                        tint = secondaryTextColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Tap card to reveal QR Code  ↺",
                        color = secondaryTextColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            // BACK FACE: High-Contrast Scannable QR Matrix
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { rotationY = 180f }
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        AiWaveSphereIcon(modifier = Modifier.size(20.dp))
                        Text(
                            text = "OSCELLION",
                            color = primaryTextColor,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }

                    Text(
                        text = "SCAN TO CONNECT",
                        color = Color(0xFF38BDF8),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = Color.White.copy(alpha = 0.10f)
                )

                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Scan to connect QR",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Text(
                    text = "oscellion.vercel.app/u/${profile.username.ifBlank { "creator" }}",
                    color = primaryTextColor.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.4.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tap card to flip back  ↺",
                        color = secondaryTextColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private suspend fun savePassCardImageToGallery(
    context: Context,
    profile: UserProfile,
    userId: String,
    customization: PassCustomization
): Boolean = withContext(Dispatchers.IO) {
    try {
        val width = 1200
        val height = 750
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cardRect = RectF(0f, 0f, width.toFloat(), height.toFloat())

        // 1. Background Rendering
        when (customization.bgMode) {
            PassBackgroundMode.CUSTOM_IMAGE -> {
                val decodedUriBmp = if (customization.customImageBitmap == null) {
                    customization.customImageUri?.let { uri ->
                        com.chroma.studio.utils.DevicePerformanceManager.decodeSampledBitmap(
                            context = context,
                            uri = uri,
                            reqWidth = width,
                            reqHeight = height
                        )
                    }
                } else null
                val customBmp = customization.customImageBitmap ?: decodedUriBmp

                if (customBmp != null) {
                    val srcRect = android.graphics.Rect(0, 0, customBmp.width, customBmp.height)
                    val dstRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
                    canvas.drawBitmap(customBmp, srcRect, dstRect, null)
                    if (decodedUriBmp != null && !decodedUriBmp.isRecycled) {
                        decodedUriBmp.recycle()
                    }

                    if (customization.imageScrimAlpha > 0f) {
                        val scrimPaint = Paint().apply {
                            color = Color.Black.copy(alpha = customization.imageScrimAlpha).toArgb()
                        }
                        canvas.drawRect(dstRect, scrimPaint)
                    }
                } else {
                    val defaultPaint = Paint().apply {
                        isAntiAlias = true
                        shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), 0xFFEEF2FF.toInt(), 0xFFDBEAFE.toInt(), Shader.TileMode.CLAMP)
                    }
                    canvas.drawRoundRect(cardRect, 72f, 72f, defaultPaint)
                }
            }
            PassBackgroundMode.GRADIENT_MAKER -> {
                val startColor = customization.gradientColors.firstOrNull()?.toArgb() ?: 0xFF4F46E5.toInt()
                val endColor = customization.gradientColors.lastOrNull()?.toArgb() ?: 0xFF818CF8.toInt()
                val bgPaint = Paint().apply {
                    isAntiAlias = true
                    shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), startColor, endColor, Shader.TileMode.CLAMP)
                }
                canvas.drawRoundRect(cardRect, 72f, 72f, bgPaint)
            }
            PassBackgroundMode.DEFAULT -> {
                val bgPaint = Paint().apply {
                    isAntiAlias = true
                    shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), 0xFFEEF2FF.toInt(), 0xFFDBEAFE.toInt(), Shader.TileMode.CLAMP)
                }
                canvas.drawRoundRect(cardRect, 72f, 72f, bgPaint)
            }
        }

        // 2. Border
        val borderPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = if (!customization.isDarkText) android.graphics.Color.argb(70, 255, 255, 255) else android.graphics.Color.argb(200, 219, 234, 254)
        }
        canvas.drawRoundRect(cardRect, 72f, 72f, borderPaint)

        val primaryTextColor = if (!customization.isDarkText) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#1D1D1F")
        val secondaryTextColor = if (!customization.isDarkText) android.graphics.Color.argb(190, 255, 255, 255) else android.graphics.Color.parseColor("#86868B")

        // 3. Display Name
        val namePaint = Paint().apply {
            isAntiAlias = true
            color = primaryTextColor
            textSize = 58f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        canvas.drawText(profile.displayName.ifBlank { "Creator" }, 80f, 300f, namePaint)

        // 4. Role (if available)
        var nextY = 360f
        val roleText = profile.role
        if (!roleText.isNullOrBlank()) {
            val rolePaint = Paint().apply {
                isAntiAlias = true
                color = customization.accentColor.toArgb()
                textSize = 34f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            canvas.drawText("✦ $roleText", 80f, nextY, rolePaint)
            nextY += 50f
        }

        // 5. Username
        val userPaint = Paint().apply {
            isAntiAlias = true
            color = secondaryTextColor
            textSize = 38f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        canvas.drawText("@${profile.username.ifBlank { "creator" }}", 80f, nextY, userPaint)

        // 6. Brand Subtitle
        val subPaint = Paint().apply {
            isAntiAlias = true
            color = customization.accentColor.toArgb()
            textSize = 28f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            letterSpacing = 0.1f
        }
        canvas.drawText("CHROMA STUDIO • CREATOR PASS", 80f, 670f, subPaint)

        // 7. QR Code Card on Right
        val profileShareUrl = "https://oscellion.vercel.app/u/${profile.username.ifBlank { userId }}"
        val qrBmp = generateQrBitmap(
            profileShareUrl,
            sizePx = 300,
            foregroundColor = customization.qrColor.toArgb()
        )
        if (qrBmp != null) {
            val qrCardWidth = 380f
            val qrCardHeight = 440f
            val qrLeft = width - qrCardWidth - 80f
            val qrTop = (height - qrCardHeight) / 2f

            val qrBgPaint = Paint().apply {
                isAntiAlias = true
                color = when (customization.qrFrameStyle) {
                    QrFrameStyle.SOLID_DARK -> android.graphics.Color.parseColor("#1C1C1E")
                    QrFrameStyle.FROSTED_GLASS -> android.graphics.Color.argb(120, 255, 255, 255)
                    QrFrameStyle.MINIMAL -> android.graphics.Color.TRANSPARENT
                    else -> android.graphics.Color.WHITE
                }
            }
            canvas.drawRoundRect(RectF(qrLeft, qrTop, qrLeft + qrCardWidth, qrTop + qrCardHeight), 56f, 56f, qrBgPaint)
            canvas.drawBitmap(qrBmp, qrLeft + 40f, qrTop + 40f, null)

            val scanTextPaint = Paint().apply {
                isAntiAlias = true
                color = when (customization.qrFrameStyle) {
                    QrFrameStyle.SOLID_DARK -> android.graphics.Color.argb(180, 255, 255, 255)
                    else -> android.graphics.Color.parseColor("#86868B")
                }
                textSize = 22f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                letterSpacing = 0.15f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("SCAN TO CONNECT", qrLeft + (qrCardWidth / 2f), qrTop + qrCardHeight - 40f, scanTextPaint)
        }

        // Save to MediaStore
        val filename = "ChromaPass_${profile.username}_${System.currentTimeMillis()}.png"
        var fos: OutputStream? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ChromaStudio")
            }
            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (imageUri != null) {
                fos = resolver.openOutputStream(imageUri)
            }
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/ChromaStudio"
            val file = File(imagesDir)
            if (!file.exists()) file.mkdirs()
            val image = File(file, filename)
            fos = FileOutputStream(image)
        }

        fos?.use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

private suspend fun saveModernBlueCardToGallery(
    context: Context,
    profile: UserProfile,
    userId: String,
    profileShareUrl: String
): Boolean = withContext(Dispatchers.IO) {
    try {
        val width = 1280
        val height = 720
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Base electric royal navy gradient
        val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(0xFF0A32A3.toInt(), 0xFF0064FA.toInt(), 0xFF3E95FF.toInt()),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), 64f, 64f, basePaint)

        // Sculpted deep sapphire waves matching ModernBlueProfileCard
        val sx = width / 400f
        val sy = height / 225f
        val wave1 = android.graphics.Path().apply {
            moveTo(-20f * sx, 90f * sy)
            cubicTo(90f * sx, 30f * sy, 180f * sx, 160f * sy, 290f * sx, 95f * sy)
            cubicTo(360f * sx, 55f * sy, 410f * sx, 100f * sy, 430f * sx, 85f * sy)
            lineTo(430f * sx, 240f * sy)
            lineTo(-20f * sx, 240f * sy)
            close()
        }
        canvas.drawPath(
            wave1,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, 30f * sy, width.toFloat(), height.toFloat(), 0x99002073.toInt(), 0x8C004AE0.toInt(), Shader.TileMode.CLAMP)
            }
        )
        val wave2 = android.graphics.Path().apply {
            moveTo(-20f * sx, 150f * sy)
            cubicTo(85f * sx, 195f * sy, 165f * sx, 105f * sy, 265f * sx, 155f * sy)
            cubicTo(335f * sx, 190f * sy, 395f * sx, 140f * sy, 430f * sx, 155f * sy)
            lineTo(430f * sx, 240f * sy)
            lineTo(-20f * sx, 240f * sy)
            close()
        }
        canvas.drawPath(
            wave2,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, 110f * sy, width.toFloat(), height.toFloat(), 0x59FFFFFF, 0xCC001657.toInt(), Shader.TileMode.CLAMP)
            }
        )

        // Left identity text & OSCELLION brand
        val badgeBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFC.toInt()
        }
        val brandX = 112f
        val brandY = 230f
        canvas.drawCircle(brandX, brandY, 24f, badgeBg)
        val brandStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = 0xFF0057E3.toInt()
            strokeWidth = 4.5f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawCircle(brandX, brandY, 13f, brandStroke)

        val brandTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFEBF3FF.toInt()
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.22f
        }
        canvas.drawText("OSCELLION", brandX + 38f, brandY + 9f, brandTextPaint)

        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 54f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val displayName = profile.displayName.ifBlank { profile.username.ifBlank { "Oscellion Creator" } }
        canvas.drawText(displayName.take(24), 88f, 340f, namePaint)

        val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFDCEBFF.toInt()
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val handle = "@${profile.username.ifBlank { userId.take(10) }}"
        canvas.drawText(handle, 88f, 396f, handlePaint)

        val pillRect = RectF(88f, 456f, 430f, 520f)
        canvas.drawRoundRect(pillRect, 32f, 32f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x26FFFFFF })
        canvas.drawRoundRect(
            pillRect,
            32f,
            32f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2f
                color = 0x45FFFFFF
            }
        )
        val scanPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Scan to view profile", 140f, 496f, scanPaint)

        // White QR Card on Right
        val qrCardRect = RectF(780f, 120f, 1188f, 600f)
        canvas.drawRoundRect(qrCardRect, 48f, 48f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xF7FFFFFF.toInt() })
        val qrBmp = generateQrBitmap(profileShareUrl, sizePx = 340, foregroundColor = 0xFF0A1628.toInt())
        if (qrBmp != null) {
            canvas.drawBitmap(qrBmp, 814f, 190f, null)
            qrBmp.recycle()
        }

        val filename = "Oscellion_Card_${profile.username.ifBlank { userId }}_${System.currentTimeMillis()}.png"
        val saved: Boolean
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Oscellion")
            }
            val resolver = context.contentResolver
            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            saved = imageUri?.let { uri ->
                resolver.openOutputStream(uri)?.use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
                true
            } ?: false
        } else {
            @Suppress("DEPRECATION")
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Oscellion")
            if (!dir.exists()) dir.mkdirs()
            val outFile = File(dir, filename)
            FileOutputStream(outFile).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
            saved = true
        }
        bitmap.recycle()
        saved
    } catch (_: Exception) {
        false
    }
}

@Composable
private fun ModernBlueCardBrandCircle(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val s = minOf(w, h) / 48f
        drawCircle(color = Color(0xFAFFFFFF), radius = minOf(w, h) / 2f)
        drawCircle(
            color = Color(0xFF0057E3),
            radius = 12.2f * s,
            center = Offset(24f * s, 24f * s),
            style = Stroke(width = 4.6f * s, cap = StrokeCap.Round)
        )
        drawArc(
            color = Color(0xFF00A3FF),
            startAngle = -55f,
            sweepAngle = 155f,
            useCenter = false,
            topLeft = Offset(14.5f * s, 14.5f * s),
            size = Size(19f * s, 19f * s),
            style = Stroke(width = 3.6f * s, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun ModernBlueCardWavesCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Base rich blue gradient
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF002CA0), Color(0xFF034DD8), Color(0xFF013BC2), Color(0xFF002B9E)),
                start = Offset.Zero,
                end = Offset(w, h)
            )
        )

        // Top sky bloom
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF8DD4FF).copy(alpha = 0.96f),
                    Color(0xFF2D96FF).copy(alpha = 0.80f),
                    Color(0xFF095FF0).copy(alpha = 0.40f),
                    Color.Transparent
                ),
                center = Offset(w * 0.67f, h * 0.04f),
                radius = w * 0.54f
            )
        )

        // Bottom-left glow
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF45A9FF).copy(alpha = 0.85f),
                    Color(0xFF1372FF).copy(alpha = 0.50f),
                    Color.Transparent
                ),
                center = Offset(w * 0.11f, h * 0.95f),
                radius = w * 0.48f
            )
        )

        // Right wedge highlight
        val rightWedge = Path().apply {
            moveTo(w, h * 0.18f)
            cubicTo(w * 0.88f, h * 0.31f, w * 0.79f, h * 0.48f, w * 0.74f, h * 0.73f)
            cubicTo(w * 0.715f, h * 0.83f, w * 0.70f, h * 0.92f, w * 0.695f, h)
            lineTo(w, h)
            close()
        }
        drawPath(
            path = rightWedge,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF2994FF).copy(alpha = 0.70f),
                    Color(0xFF0D69F4).copy(alpha = 0.46f),
                    Color(0xFF0341BE).copy(alpha = 0.14f)
                ),
                start = Offset(w * 0.95f, h * 0.18f),
                end = Offset(w * 0.68f, h * 0.95f)
            )
        )

        // Top-Left Outer Wave
        val tlOuter = Path().apply {
            moveTo(0f, h * 0.385f)
            cubicTo(w * 0.145f, h * 0.32f, w * 0.29f, h * 0.225f, w * 0.40f, 0f)
            lineTo(0f, 0f)
            close()
        }
        drawPath(
            path = tlOuter,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF002EA8), Color(0xFF0A5CE8), Color(0xFF2D8EFF), Color(0xFF69C1FF)),
                start = Offset(w * 0.05f, h * 0.05f),
                end = Offset(w * 0.40f, h * 0.38f)
            )
        )

        // Top-Left Inner Wave + Crisp Rim
        val tlInner = Path().apply {
            moveTo(0f, h * 0.27f)
            cubicTo(w * 0.108f, h * 0.225f, w * 0.22f, h * 0.15f, w * 0.29f, 0f)
            lineTo(0f, 0f)
            close()
        }
        drawPath(
            path = tlInner,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF00299E), Color(0xFF0A5BE6), Color(0xFF359AFF), Color(0xFF87D2FF)),
                start = Offset.Zero,
                end = Offset(w * 0.29f, h * 0.27f)
            )
        )
        val tlRim = Path().apply {
            moveTo(0f, h * 0.27f)
            cubicTo(w * 0.108f, h * 0.225f, w * 0.22f, h * 0.15f, w * 0.29f, 0f)
        }
        drawPath(
            path = tlRim,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF67BCFF).copy(alpha = 0.4f), Color.White.copy(alpha = 0.9f)),
                start = Offset(0f, h * 0.27f),
                end = Offset(w * 0.29f, 0f)
            ),
            style = Stroke(width = 2.4f, cap = StrokeCap.Round)
        )

        // Bottom-Right Outer Wave
        val brOuter = Path().apply {
            moveTo(w * 0.50f, h)
            cubicTo(w * 0.62f, h * 0.83f, w * 0.78f, h * 0.75f, w, h * 0.76f)
            lineTo(w, h)
            close()
        }
        drawPath(
            path = brOuter,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF5DB9FF), Color(0xFF1978FC), Color(0xFF044AD0), Color(0xFF002DA3)),
                start = Offset(w * 0.50f, h * 0.74f),
                end = Offset(w, h)
            )
        )

        // Bottom-Right Front Wave + Rim
        val brFront = Path().apply {
            moveTo(w * 0.685f, h)
            cubicTo(w * 0.775f, h * 0.83f, w * 0.89f, h * 0.725f, w, h * 0.68f)
            lineTo(w, h)
            close()
        }
        drawPath(
            path = brFront,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF88D4FF), Color(0xFF2D93FF), Color(0xFF085AE6), Color(0xFF0035B8)),
                start = Offset(w * 0.68f, h * 0.68f),
                end = Offset(w, h)
            )
        )
        val brRim = Path().apply {
            moveTo(w * 0.685f, h)
            cubicTo(w * 0.775f, h * 0.83f, w * 0.89f, h * 0.725f, w, h * 0.68f)
        }
        drawPath(
            path = brRim,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF9CD7FF).copy(alpha = 0.72f), Color.White.copy(alpha = 0.92f)),
                start = Offset(w * 0.685f, h),
                end = Offset(w, h * 0.68f)
            ),
            style = Stroke(width = 2.6f, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun ModernBlueCardView(
    profile: UserProfile,
    userId: String,
    profileShareUrl: String,
    qrBitmap: Bitmap?,
    onQrClick: () -> Unit
) {
    val cleanAvatar = profile.avatarUrl.trim()
    val presetRes = remember(cleanAvatar) { getAvatarPresetDrawableRes(cleanAvatar) }
    val isCustomPhoto = cleanAvatar.isNotBlank() && !cleanAvatar.contains("default_avatar") && presetRes == null
    val handle = profile.username.ifBlank { userId.take(10) }

    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }
    val animTiltX by animateFloatAsState(
        targetValue = tiltX,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 280f),
        label = "cardTiltX"
    )
    val animTiltY by animateFloatAsState(
        targetValue = tiltY,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 280f),
        label = "cardTiltY"
    )

    val sheenInfinite = rememberInfiniteTransition(label = "cardSheen")
    val autoSheen by sheenInfinite.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Restart
        ),
        label = "autoSheen"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationX = animTiltX
                rotationY = animTiltY
                cameraDistance = 18f * density
            }
            .shadow(
                elevation = 22.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color(0x660064FA),
                spotColor = Color(0x700039AD)
            )
            .clip(RoundedCornerShape(24.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { tiltX = 0f; tiltY = 0f },
                    onDragCancel = { tiltX = 0f; tiltY = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        tiltY = (tiltY + dragAmount.x * 0.14f).coerceIn(-12f, 12f)
                        tiltX = (tiltX - dragAmount.y * 0.14f).coerceIn(-9f, 9f)
                    }
                )
            }
    ) {
        val cqw = (maxWidth.value / 100f).coerceAtLeast(2.4f)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            ModernBlueCardWavesCanvas(modifier = Modifier.fillMaxSize())

            // Dynamic specular sheen across the card face
            Canvas(modifier = Modifier.fillMaxSize()) {
                val offset = autoSheen * size.width * 1.4f
                val band = Path().apply {
                    moveTo(offset - size.width * 0.18f, 0f)
                    lineTo(offset + size.width * 0.06f, 0f)
                    lineTo(offset - size.width * 0.34f, size.height)
                    lineTo(offset - size.width * 0.58f, size.height)
                    close()
                }
                drawPath(band, Color(0x16FFFFFF))
            }

            // Glass inner stroke
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.2.dp, Color(0x52FFFFFF), RoundedCornerShape(24.dp))
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = (4.2f * cqw).dp, end = (4.1f * cqw).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Left: Large 22.6cqw Circular Avatar with Ice-White Ring
                Box(
                    modifier = Modifier
                        .size((22.6f * cqw).dp)
                        .offset(y = (-1.8f * cqw).dp)
                        .shadow(14.dp, CircleShape, ambientColor = Color(0x80000E44), spotColor = Color(0x80000E44))
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color.White, Color(0xFFDFEEFF), Color(0xFFCFE5FF))
                            )
                        )
                        .padding((0.48f * cqw).dp)
                        .clip(CircleShape)
                        .background(Color(0xFF173E8F)),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        presetRes != null -> {
                            Image(
                                painter = painterResource(id = presetRes),
                                contentDescription = profile.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        }
                        isCustomPhoto -> {
                            AsyncImage(
                                model = cleanAvatar,
                                contentDescription = profile.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        }
                        else -> {
                            com.chroma.studio.ui.screens.MacOsDefaultAvatarCanvas(modifier = Modifier.fillMaxSize())
                        }
                    }
                }

                // 2. Center: Display Name + VerifiedBadge + @username + Frosted Scan Pill
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = (3.15f * cqw).dp, end = (2.2f * cqw).dp)
                        .offset(y = (0.5f * cqw).dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy((1.05f * cqw).dp)
                    ) {
                        Text(
                            text = profile.displayName.ifBlank { handle },
                            fontSize = (4.15f * cqw).sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (profile.verified) {
                            VerifiedBadge(
                                size = (3.4f * cqw).dp,
                                tone = profile.verifiedTone,
                                userId = profile.uid
                            )
                        }
                    }

                    Text(
                        text = "@$handle",
                        fontSize = (2.45f * cqw).sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xE0CEE3FF),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = (0.65f * cqw).dp)
                    )

                    Row(
                        modifier = Modifier
                            .padding(top = (2.55f * cqw).dp)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0x29FFFFFF), Color(0x17FFFFFF))
                                )
                            )
                            .border(0.8.dp, Color(0x57FFFFFF), CircleShape)
                            .padding(
                                start = (1.85f * cqw).dp,
                                end = (2.15f * cqw).dp,
                                top = (1.05f * cqw).dp,
                                bottom = (1.05f * cqw).dp
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy((1.25f * cqw).dp)
                    ) {
                        Icon(
                            imageVector = Lucide.ScanLine,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size((2.3f * cqw).dp)
                        )
                        Text(
                            text = "Scan to view profile",
                            fontSize = (1.82f * cqw).sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }

                // 3. Fading Vertical Hairline Divider
                Box(
                    modifier = Modifier
                        .padding(end = (2.75f * cqw).dp)
                        .width(1.dp)
                        .height((18.2f * cqw).dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0x14FFFFFF), Color(0x80FFFFFF), Color(0x14FFFFFF))
                            )
                        )
                )

                // 4. Right: 20.1cqw Elevated White Square QR Code Tile
                Box(
                    modifier = Modifier
                        .size((20.1f * cqw).dp)
                        .shadow(16.dp, RoundedCornerShape((2.25f * cqw).dp), ambientColor = Color(0x70001048), spotColor = Color(0x70001048))
                        .clip(RoundedCornerShape((2.25f * cqw).dp))
                        .background(Color.White)
                        .clickable(onClick = onQrClick)
                        .padding((1.65f * cqw).dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Profile QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Lucide.QrCode,
                            contentDescription = "QR",
                            tint = Color(0xFF0A1628),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Modern Blue Profile Card QR Bottom Sheet (16:9 Liquid-Blue card + Copy Link + Save + Done).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleProfileCardSheet(
    profile: UserProfile,
    userId: String,
    worksCount: Int = 0,
    totalLikes: Int = 0,
    onDismiss: () -> Unit
) {
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val handle = profile.username.ifBlank { userId }
    val profileShareUrl = "https://oscellion.vercel.app/u/$handle"

    var showEnlargedQr by remember { mutableStateOf(false) }
    var isSavingImage by remember { mutableStateOf(false) }
    var copiedFeedback by remember { mutableStateOf(false) }

    val qrBitmap = remember(profileShareUrl) {
        generateQrBitmap(
            content = profileShareUrl,
            sizePx = 560,
            foregroundColor = 0xFF0A1628.toInt(),
            backgroundColor = 0xFFFFFFFF.toInt()
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF151B27) else Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .width(38.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF3A4458) else Color(0xFFCBD5E1))
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        windowInsets = WindowInsets(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header row: Profile Card | Ready to share
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Profile Card",
                    color = if (isDark) Color(0xFFF5F8FF) else Color(0xFF070B18),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Ready to share",
                    color = if (isDark) Color(0xFF8C9AB0) else Color(0xFF8B95AA),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Modern 16:9 Blue Profile Card
            ModernBlueCardView(
                profile = profile,
                userId = userId,
                profileShareUrl = profileShareUrl,
                qrBitmap = qrBitmap,
                onQrClick = { showEnlargedQr = true }
            )

            // Action Deck: Primary Gradient Copy Link + Save + Done
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .shadow(10.dp, CircleShape, ambientColor = Color(0x520064FA), spotColor = Color(0x520064FA))
                        .clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(Color(0xFF1A7EFF), Color(0xFF0064FA), Color(0xFF0052D6))))
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Profile Link", profileShareUrl))
                            copiedFeedback = true
                            ToastManager.showToast("Profile link copied!")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (copiedFeedback) Lucide.Check else Lucide.Copy,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (copiedFeedback) "Copied" else "Copy Link",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF151B27) else Color.White)
                        .border(1.dp, if (isDark) Color(0xFF283247) else Color(0xFFDCE5F4), CircleShape)
                        .clickable {
                            if (!isSavingImage) {
                                isSavingImage = true
                                scope.launch {
                                    val saved = saveModernBlueCardToGallery(context, profile, userId, profileShareUrl)
                                    isSavingImage = false
                                    ToastManager.showToast(if (saved) "Saved Profile Card PNG to Photos!" else "Unable to save image")
                                }
                            }
                        }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSavingImage) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF0064FA)
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.Download,
                                contentDescription = null,
                                tint = Color(0xFF0064FA),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Save",
                                color = if (isDark) Color(0xFFF5F8FF) else Color(0xFF070B18),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF1B2231) else Color(0xFFF4F7FD))
                        .border(1.dp, if (isDark) Color(0xFF283247) else Color(0xFFE6EDF8), CircleShape)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Done",
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF525E75),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    // Fullscreen Enlarged QR Dialog
    if (showEnlargedQr) {
        Dialog(
            onDismissRequest = { showEnlargedQr = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.88f))
                    .clickable { showEnlargedQr = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.White)
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Scan to Connect",
                        color = Color(0xFF070B18),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    val largeQr = remember(profileShareUrl) {
                        generateQrBitmap(
                            profileShareUrl,
                            sizePx = 600,
                            foregroundColor = 0xFF0A1628.toInt()
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFF4F7FD))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (largeQr != null) {
                            Image(
                                bitmap = largeQr.asImageBitmap(),
                                contentDescription = "Large QR Code",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Text(
                        text = "@$handle",
                        color = Color(0xFF0064FA),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Scan with any camera or QR scanner to view profile directly.",
                        color = Color(0xFF5C6B80),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    ChromaPillButton(
                        onClick = { showEnlargedQr = false },
                        modifier = Modifier.fillMaxWidth(),
                        variant = ChromaPillVariant.Blue,
                        height = 44.dp
                    ) {
                        Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
