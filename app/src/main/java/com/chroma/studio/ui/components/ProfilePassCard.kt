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
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
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
    GradientPreset("Cosmic Indigo", listOf(Color(0xFF1E1B4B), Color(0xFF312E81), Color(0xFF4338CA))),
    GradientPreset("Pacific Titanium", listOf(Color(0xFF0A2540), Color(0xFF0284C7), Color(0xFF0EA5E9))),
    GradientPreset("Emerald Luxury", listOf(Color(0xFF064E3B), Color(0xFF047857), Color(0xFF10B981))),
    GradientPreset("Midnight Royal", listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155))),
    GradientPreset("Sunset Radiance", listOf(Color(0xFF4C1D95), Color(0xFF9333EA), Color(0xFFF43F5E))),
    GradientPreset("Golden Amber", listOf(Color(0xFF451A03), Color(0xFF92400E), Color(0xFFD97706))),
    GradientPreset("Studio Minimal", listOf(Color(0xFFF8FAFC), Color(0xFFEEF2F6), Color(0xFFE2E8F0)))
)

val QR_COLOR_PRESETS = listOf(
    Color(0xFF1D1D1F), // Jet Black
    Color(0xFFFFFFFF), // Pure White
    Color(0xFF007AFF), // System Blue
    Color(0xFF34C759), // Mint Green
    Color(0xFFFF9500), // Sunset Amber
    Color(0xFFFF2D55), // Crimson
    Color(0xFF5856D6), // Deep Indigo
    Color(0xFF32ADE6)  // Cyan
)

val ACCENT_COLOR_PRESETS = listOf(
    Color(0xFF007AFF), // System Blue
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
    val gradientColors: List<Color> = listOf(Color(0xFF007AFF), Color(0xFF0051D5), Color(0xFF32ADE6)),
    val gradientAngle: Float = 45f,
    val customImageUri: Uri? = null,
    val customImageBitmap: Bitmap? = null,
    val imageBlur: Float = 0f,
    val imageScrimAlpha: Float = 0.25f,
    val qrColor: Color = Color(0xFF1D1D1F),
    val qrFrameStyle: QrFrameStyle = QrFrameStyle.SOLID_WHITE,
    val isDarkText: Boolean = true,
    val accentColor: Color = Color(0xFF007AFF)
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
        accentColor = Color(0xFF007AFF),
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

/**
 * Profile Pass Card View with Customization and Role display
 */
@Composable
fun ProfilePassCardView(
    profile: UserProfile,
    userId: String,
    customization: PassCustomization = PassCustomization(),
    onQrClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profileShareUrl = "https://oscellion.vercel.app/u/${profile.username.ifBlank { userId }}"

    var isCopied by remember { mutableStateOf(false) }

    val qrBitmap = remember(profileShareUrl, customization.qrColor) {
        generateQrBitmap(
            profileShareUrl,
            sizePx = 360,
            foregroundColor = customization.qrColor.toArgb()
        )
    }

    val primaryTextColor = if (!customization.isDarkText) Color.White else Color(0xFF1D1D1F)
    val secondaryTextColor = if (!customization.isDarkText) Color.White.copy(alpha = 0.75f) else Color(0xFF86868B)
    val buttonBgColor = if (!customization.isDarkText) Color.White.copy(alpha = 0.15f) else Color(0xFFF2F2F7)

    val cardShape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .border(
                width = 0.5.dp,
                color = if (!customization.isDarkText) Color.White.copy(alpha = 0.28f) else Color.Black.copy(alpha = 0.10f),
                shape = cardShape
            )
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
                            .background(Brush.linearGradient(listOf(Color(0xFFEEF2FF), Color(0xFFDBEAFE))))
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
                        .background(Brush.linearGradient(listOf(Color(0xFFEEF2FF), Color(0xFFDBEAFE))))
                        .drawBehind {
                            val dotSpacing = 20.dp.toPx()
                            val dotRadius = 1.dp.toPx()
                            val dotColor = Color(0xFF1D1D1F).copy(alpha = 0.04f)
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
                            Color.White.copy(alpha = if (!customization.isDarkText) 0.55f else 0.25f),
                            Color.Transparent
                        )
                    )
                )
        )

        // ── Card Foreground Content ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── 1. Top Section: Brand Identity & Official Badge ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Lucide.Sparkles,
                        contentDescription = null,
                        tint = if (!customization.isDarkText) Color.White.copy(alpha = 0.9f) else customization.accentColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "CHROMA STUDIO",
                        color = primaryTextColor.copy(alpha = 0.95f),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "•",
                        color = secondaryTextColor.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )
                    Text(
                        text = "CREATOR PASS",
                        color = secondaryTextColor,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp
                    )
                }

                // Official Pass Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(if (!customization.isDarkText) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.05f))
                        .border(
                            0.5.dp,
                            if (!customization.isDarkText) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.08f),
                            RoundedCornerShape(100.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Lucide.ShieldCheck,
                        contentDescription = null,
                        tint = if (!customization.isDarkText) Color(0xFF34C759) else Color(0xFF059669),
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "OFFICIAL PASS",
                        color = primaryTextColor,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                }
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = if (!customization.isDarkText) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.06f)
            )

            // ── 2. Middle Section: Identity (Left) + Scannable QR Badge (Right) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left Column: Avatar + Profile Name + Handle + Organized Metadata Chips
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Circular Avatar with Dual Rim
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(if (!customization.isDarkText) Color(0xFF2C2C2E) else Color(0xFFF5F5F7))
                                .border(
                                    width = 1.5.dp,
                                    color = if (!customization.isDarkText) Color.White.copy(alpha = 0.85f) else Color.White,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            IosAvatarView(
                                name = profile.displayName.ifBlank { profile.username },
                                avatarUrl = profile.avatarUrl,
                                size = 62.dp,
                                gender = profile.gender,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Display Name & Username
                        Column(verticalArrangement = Arrangement.spacedBy((-2).dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = profile.displayName.ifBlank { "Creator" },
                                    color = primaryTextColor,
                                    fontSize = 17.sp,
                                    lineHeight = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (profile.verified) {
                                    VerifiedBadge(size = 15.dp, showGlow = true, animated = true)
                                }
                            }

                            Text(
                                text = "@${profile.username.ifBlank { "creator" }}",
                                color = secondaryTextColor,
                                fontSize = 12.5.sp,
                                lineHeight = 14.5.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Organized Info Badges with Icons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Role Chip
                        val roleText = profile.role
                        if (!roleText.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(customization.accentColor.copy(alpha = if (!customization.isDarkText) 0.28f else 0.12f))
                                    .border(
                                        width = 0.5.dp,
                                        color = customization.accentColor.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Lucide.Briefcase,
                                    contentDescription = null,
                                    tint = if (!customization.isDarkText) Color.White else customization.accentColor,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = roleText,
                                    color = if (!customization.isDarkText) Color.White else customization.accentColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Followers Chip
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (!customization.isDarkText) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.05f))
                                .border(
                                    width = 0.5.dp,
                                    color = if (!customization.isDarkText) Color.White.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.Users,
                                contentDescription = null,
                                tint = secondaryTextColor,
                                modifier = Modifier.size(11.dp)
                            )
                            val formattedFollowers = if (profile.followersCount >= 1000) {
                                String.format("%.1fk", profile.followersCount / 1000f)
                            } else {
                                profile.followersCount.toString()
                            }
                            Text(
                                text = "$formattedFollowers Followers",
                                color = primaryTextColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Location Chip (if available)
                        if (profile.location.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (!customization.isDarkText) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.05f))
                                    .border(
                                        width = 0.5.dp,
                                        color = if (!customization.isDarkText) Color.White.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Lucide.MapPin,
                                    contentDescription = null,
                                    tint = secondaryTextColor,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = profile.location,
                                    color = secondaryTextColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Right Column: Scannable QR Code Frame
                val qrContainerModifier = when (customization.qrFrameStyle) {
                    QrFrameStyle.SOLID_WHITE -> Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(0.5.dp, Color(0xFFE5E5EA), RoundedCornerShape(14.dp))
                    QrFrameStyle.FROSTED_GLASS -> Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.35f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    QrFrameStyle.SOLID_DARK -> Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1C1C1E))
                        .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                    QrFrameStyle.MINIMAL -> Modifier
                        .clip(RoundedCornerShape(14.dp))
                }

                Box(
                    modifier = qrContainerModifier
                        .clickable { onQrClick() }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "Scan to connect QR",
                                modifier = Modifier.size(80.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Lucide.QrCode,
                                contentDescription = "QR Code",
                                tint = customization.qrColor,
                                modifier = Modifier.size(80.dp)
                            )
                        }

                        val scanLabelColor = when (customization.qrFrameStyle) {
                            QrFrameStyle.SOLID_DARK -> Color.White.copy(alpha = 0.7f)
                            QrFrameStyle.FROSTED_GLASS -> if (!customization.isDarkText) Color.White else Color(0xFF1D1D1F)
                            else -> Color(0xFF86868B)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.QrCode,
                                contentDescription = null,
                                tint = scanLabelColor,
                                modifier = Modifier.size(9.dp)
                            )
                            Text(
                                text = "CONNECT",
                                color = scanLabelColor,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }
            }

            // ── 3. Bottom Section: Action Buttons ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Copy Link Button
                val copyBgColor by animateColorAsState(
                    targetValue = if (isCopied) Color(0xFF34C759) else buttonBgColor,
                    label = "copy_btn_bg"
                )
                val copyTextColor by animateColorAsState(
                    targetValue = if (isCopied || !customization.isDarkText) Color.White else primaryTextColor,
                    label = "copy_btn_text"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(copyBgColor)
                        .border(
                            0.5.dp,
                            if (!customization.isDarkText) Color.White.copy(alpha = 0.20f) else Color.Black.copy(alpha = 0.08f),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Profile Link", profileShareUrl))
                            isCopied = true
                            ToastManager.showToast("Profile link copied!")
                            scope.launch {
                                delay(2000)
                                isCopied = false
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isCopied) Lucide.Check else Lucide.Copy,
                            contentDescription = null,
                            tint = copyTextColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isCopied) "Copied" else "Copy Link",
                            color = copyTextColor,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }

                // Share Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(customization.accentColor)
                        .clickable {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Connect with ${profile.displayName} (@${profile.username}) on Oscellion: $profileShareUrl"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Profile"))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.Share2,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Share Pass",
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
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
                val customBmp = customization.customImageBitmap ?: customization.customImageUri?.let { uri ->
                    try {
                        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                    } catch (_: Exception) { null }
                }

                if (customBmp != null) {
                    val srcRect = android.graphics.Rect(0, 0, customBmp.width, customBmp.height)
                    val dstRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
                    canvas.drawBitmap(customBmp, srcRect, dstRect, null)

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

/**
 * Modern Profile Pass Sheet Modal with interactive Customization Studio
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profileShareUrl = "https://oscellion.vercel.app/u/${profile.username.ifBlank { userId }}"

    var customization by remember { mutableStateOf(PassCustomization()) }
    var activeTab by remember { mutableStateOf(0) } // 0: Background, 1: QR Code, 2: Text & Accents
    var showEnlargedQr by remember { mutableStateOf(false) }
    var isSavingImage by remember { mutableStateOf(false) }

    // Photo picker launcher for custom background image
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            customization = customization.copy(
                bgMode = PassBackgroundMode.CUSTOM_IMAGE,
                customImageUri = uri,
                isDarkText = false // Default to light text for custom photos
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.bg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.glassBorder.copy(alpha = 0.6f))
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Header ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Profile Pass Card",
                        color = colors.textMain,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Customize and export your digital creator identity",
                        color = Color(0xFF8E8E93),
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.glassBgHover)
                ) {
                    Icon(
                        imageVector = Lucide.X,
                        contentDescription = "Close",
                        tint = colors.textMain,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // ── The Customized Profile Pass Card ──
            ProfilePassCardView(
                profile = profile,
                userId = userId,
                customization = customization,
                onQrClick = { showEnlargedQr = true }
            )

            // ── Identity Unchangeable Notice ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.glassBgHover.copy(alpha = 0.3f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Lucide.Lock,
                    contentDescription = "Locked",
                    tint = Color(0xFF8E8E93),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "Name, Username, Verified status, and Avatar are locked to your authentic profile.",
                    color = Color(0xFF8E8E93),
                    fontSize = 11.sp
                )
            }

            // ── Customization Category Tabs ──
            val tabs = listOf("Background", "QR Code", "Text & Accents")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.glassBgHover.copy(alpha = 0.5f))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = activeTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) colors.bg else Color.Transparent)
                            .clickable { activeTab = index }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) Color(0xFF007AFF) else colors.textMain,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // ── Tab 0: Background Customization ──
            if (activeTab == 0) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Mode Selector: Default vs Gradient Maker vs Custom Image
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Default
                        OutlinedButton(
                            onClick = {
                                customization = customization.copy(
                                    bgMode = PassBackgroundMode.DEFAULT,
                                    isDarkText = true
                                )
                            },
                            modifier = Modifier.weight(1f).height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                if (customization.bgMode == PassBackgroundMode.DEFAULT) 1.5.dp else 0.5.dp,
                                if (customization.bgMode == PassBackgroundMode.DEFAULT) Color(0xFF007AFF) else colors.glassBorder
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (customization.bgMode == PassBackgroundMode.DEFAULT) Color(0xFF007AFF).copy(alpha = 0.12f) else Color.Transparent
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                "Default",
                                color = if (customization.bgMode == PassBackgroundMode.DEFAULT) Color(0xFF007AFF) else colors.textMain,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Gradient Maker
                        OutlinedButton(
                            onClick = {
                                customization = customization.copy(
                                    bgMode = PassBackgroundMode.GRADIENT_MAKER,
                                    isDarkText = false
                                )
                            },
                            modifier = Modifier.weight(1f).height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                if (customization.bgMode == PassBackgroundMode.GRADIENT_MAKER) 1.5.dp else 0.5.dp,
                                if (customization.bgMode == PassBackgroundMode.GRADIENT_MAKER) Color(0xFF007AFF) else colors.glassBorder
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (customization.bgMode == PassBackgroundMode.GRADIENT_MAKER) Color(0xFF007AFF).copy(alpha = 0.12f) else Color.Transparent
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                "Gradient",
                                color = if (customization.bgMode == PassBackgroundMode.GRADIENT_MAKER) Color(0xFF007AFF) else colors.textMain,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Custom Image
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch("image/*")
                            },
                            modifier = Modifier.weight(1f).height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                if (customization.bgMode == PassBackgroundMode.CUSTOM_IMAGE) 1.5.dp else 0.5.dp,
                                if (customization.bgMode == PassBackgroundMode.CUSTOM_IMAGE) Color(0xFF007AFF) else colors.glassBorder
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (customization.bgMode == PassBackgroundMode.CUSTOM_IMAGE) Color(0xFF007AFF).copy(alpha = 0.12f) else Color.Transparent
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Lucide.Image, contentDescription = null, modifier = Modifier.size(13.dp))
                                Text(
                                    "Photo",
                                    color = if (customization.bgMode == PassBackgroundMode.CUSTOM_IMAGE) Color(0xFF007AFF) else colors.textMain,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // If Gradient Maker Mode: Workspace Button + Chroma Studio Gradient Presets
                    if (customization.bgMode == PassBackgroundMode.GRADIENT_MAKER) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF007AFF).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .clickable {
                                    val intent = Intent(context, com.chroma.studio.MainActivity::class.java).apply {
                                        putExtra(com.chroma.studio.MainActivity.EXTRA_IS_PASS_CARD_MODE, true)
                                        putExtra(com.chroma.studio.MainActivity.EXTRA_PASS_USER_ID, userId)
                                        putExtra(com.chroma.studio.MainActivity.EXTRA_PASS_DISPLAY_NAME, profile.displayName)
                                        putExtra(com.chroma.studio.MainActivity.EXTRA_PASS_USERNAME, profile.username)
                                        putExtra(com.chroma.studio.MainActivity.EXTRA_PASS_AVATAR, profile.avatarUrl)
                                        putExtra(com.chroma.studio.MainActivity.EXTRA_PASS_ROLE, profile.role)
                                        putExtra(com.chroma.studio.MainActivity.EXTRA_PASS_VERIFIED, profile.verified)
                                        putExtra(com.chroma.studio.MainActivity.EXTRA_PASS_GENDER, profile.gender)
                                    }
                                    context.startActivity(intent)
                                },
                            color = Color(0xFF007AFF).copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(Color(0xFF007AFF), Color(0xFF00C6FF)))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AiWaveSphereIcon(
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Open in Gradient Maker Workspace",
                                        color = colors.textMain,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Live edit multi-layer gradients with real-time Pass Card preview",
                                        color = Color(0xFF8E8E93),
                                        fontSize = 11.5.sp
                                    )
                                }
                                Icon(
                                    imageVector = Lucide.ChevronRight,
                                    contentDescription = null,
                                    tint = Color(0xFF007AFF),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Text("Quick Presets", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(CHROMA_GRADIENT_PRESETS) { preset ->
                                val isSelected = customization.gradientColors == preset.colors
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Brush.linearGradient(preset.colors))
                                        .border(
                                            width = if (isSelected) 2.dp else 0.5.dp,
                                            color = if (isSelected) Color.White else Color.Black.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            customization = customization.copy(
                                                gradientColors = preset.colors,
                                                isDarkText = false
                                            )
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = preset.name,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // If Custom Image Mode: Scrim and Blur controls
                    if (customization.bgMode == PassBackgroundMode.CUSTOM_IMAGE && customization.customImageUri != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Dark Scrim Overlay", color = colors.textMain, fontSize = 12.sp)
                                Text("${(customization.imageScrimAlpha * 100).toInt()}%", color = Color(0xFF8E8E93), fontSize = 12.sp)
                            }
                            AppleSlider(
                                value = customization.imageScrimAlpha,
                                onValueChange = { customization = customization.copy(imageScrimAlpha = it) },
                                valueRange = 0f..0.8f
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Blur Effect", color = colors.textMain, fontSize = 12.sp)
                                Text("${customization.imageBlur.toInt()} dp", color = Color(0xFF8E8E93), fontSize = 12.sp)
                            }
                            AppleSlider(
                                value = customization.imageBlur,
                                onValueChange = { customization = customization.copy(imageBlur = it) },
                                valueRange = 0f..20f
                            )
                        }
                    }
                }
            }

            // ── Tab 1: QR Code Customization ──
            if (activeTab == 1) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("QR Code Foreground Color", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(QR_COLOR_PRESETS) { color ->
                            val isSelected = customization.qrColor == color
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF007AFF) else colors.glassBorder,
                                        shape = CircleShape
                                    )
                                    .clickable { customization = customization.copy(qrColor = color) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Lucide.Check,
                                        contentDescription = null,
                                        tint = if (color == Color.White) Color.Black else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Text("QR Code Frame Container", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QrFrameStyle.values().forEach { frameStyle ->
                            val isSelected = customization.qrFrameStyle == frameStyle
                            val label = when (frameStyle) {
                                QrFrameStyle.SOLID_WHITE -> "White"
                                QrFrameStyle.FROSTED_GLASS -> "Glass"
                                QrFrameStyle.SOLID_DARK -> "Dark"
                                QrFrameStyle.MINIMAL -> "Minimal"
                            }
                            OutlinedButton(
                                onClick = { customization = customization.copy(qrFrameStyle = frameStyle) },
                                modifier = Modifier.weight(1f).height(34.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isSelected) 1.5.dp else 0.5.dp,
                                    if (isSelected) Color(0xFF007AFF) else colors.glassBorder
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) Color(0xFF007AFF).copy(alpha = 0.12f) else Color.Transparent
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    label,
                                    color = if (isSelected) Color(0xFF007AFF) else colors.textMain,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // ── Tab 2: Text & Accent Customization ──
            if (activeTab == 2) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Text Contrast Tone", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { customization = customization.copy(isDarkText = false) },
                            modifier = Modifier.weight(1f).height(34.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                if (!customization.isDarkText) 1.5.dp else 0.5.dp,
                                if (!customization.isDarkText) Color(0xFF007AFF) else colors.glassBorder
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (!customization.isDarkText) Color(0xFF007AFF).copy(alpha = 0.12f) else Color.Transparent
                            )
                        ) {
                            Text("Light Text (Dark Bg)", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = { customization = customization.copy(isDarkText = true) },
                            modifier = Modifier.weight(1f).height(34.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                if (customization.isDarkText) 1.5.dp else 0.5.dp,
                                if (customization.isDarkText) Color(0xFF007AFF) else colors.glassBorder
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (customization.isDarkText) Color(0xFF007AFF).copy(alpha = 0.12f) else Color.Transparent
                            )
                        ) {
                            Text("Dark Text (Light Bg)", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Text("Accent Tint & Badges Color", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(ACCENT_COLOR_PRESETS) { color ->
                            val isSelected = customization.accentColor == color
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) Color.White else colors.glassBorder,
                                        shape = CircleShape
                                    )
                                    .clickable { customization = customization.copy(accentColor = color) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Lucide.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Modal Bottom Action (Save to Photos) ──
            ChromaPillButton(
                onClick = {
                    if (!isSavingImage) {
                        isSavingImage = true
                        scope.launch {
                            val success = savePassCardImageToGallery(context, profile, userId, customization)
                            isSavingImage = false
                            if (success) {
                                ToastManager.showToast("Custom Pass saved to Photos!")
                            } else {
                                ToastManager.showToast("Failed to save image")
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(),
                variant = ChromaPillVariant.Emerald,
                height = 46.dp
            ) {
                if (isSavingImage) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 1.8.dp, color = Color.White)
                } else {
                    Icon(Lucide.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text("Save to Photos", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            }
        }
    }

    // ── Fullscreen Enlarged QR Dialog ──
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
                        color = Color(0xFF1D1D1F),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    val largeQr = remember(profileShareUrl, customization.qrColor) {
                        generateQrBitmap(
                            profileShareUrl,
                            sizePx = 600,
                            foregroundColor = customization.qrColor.toArgb()
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFF5F5F7))
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
                        text = "@${profile.username.ifBlank { profile.displayName }}",
                        color = customization.accentColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Scan with any camera or QR scanner to view profile directly.",
                        color = Color(0xFF86868B),
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
