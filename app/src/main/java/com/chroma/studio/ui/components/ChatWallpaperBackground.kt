package com.chroma.studio.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.chroma.studio.data.ChatWallpaperPresets
import com.chroma.studio.data.WallpaperConfig
import com.chroma.studio.data.WallpaperType
import com.chroma.studio.ui.theme.LocalOscellionColors

@Composable
fun ChatWallpaperBackground(
    config: WallpaperConfig,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark

    Box(modifier = modifier.fillMaxSize()) {
        when (config.type) {
            WallpaperType.AURORA -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isDark) Color(0xFF000000) else Color(0xFFF2F2F7))
                )
            }

            WallpaperType.SOLID -> {
                val color = remember(config.value) {
                    try {
                        Color(android.graphics.Color.parseColor(config.value))
                    } catch (e: Exception) {
                        if (isDark) Color(0xFF000000) else Color(0xFFF2F2F7)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color)
                )
            }

            WallpaperType.GRADIENT -> {
                val gradientColors = remember(config.value) {
                    ChatWallpaperPresets.gradientPresets.find { it.first == config.value }?.second
                        ?: listOf(Color(0xFF00C6FF), Color(0xFF0072FF))
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(gradientColors))
                )
            }

            WallpaperType.DOODLE -> {
                val baseColor = remember(config.value) {
                    try {
                        Color(android.graphics.Color.parseColor(config.value))
                    } catch (e: Exception) {
                        if (isDark) Color(0xFF000000) else Color(0xFFF2F2F7)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(baseColor)
                )
                WhatsAppDoodlePattern(opacity = config.doodleOpacity)
            }

            WallpaperType.GALLERY -> {
                if (config.value.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(Uri.parse(config.value))
                            .crossfade(true)
                            .build(),
                        contentDescription = "Chat Wallpaper",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (isDark) Color(0xFF000000) else Color(0xFFF2F2F7))
                    )
                }
            }
        }

        // Overlay doodle if enabled in solid/gradient mode
        if (config.doodleEnabled && config.type != WallpaperType.DOODLE) {
            WhatsAppDoodlePattern(opacity = config.doodleOpacity)
        }

        // Dimming overlay for readable message contrast
        if (config.dimAmount > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = config.dimAmount))
            )
        }
    }
}

/**
 * Procedural WhatsApp-style doodle pattern drawn onto Compose canvas.
 * Draws subtle chat bubbles, hearts, stars, sparkles, notes, and geometric symbols.
 */
@Composable
fun WhatsAppDoodlePattern(
    opacity: Float = 0.12f,
    color: Color = Color.Black
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val tint = color.copy(alpha = opacity)
        val stepX = 90.dp.toPx()
        val stepY = 90.dp.toPx()

        var row = 0
        var y = 20.dp.toPx()
        while (y < size.height) {
            val offsetX = if (row % 2 == 1) stepX / 2 else 0f
            var x = 20.dp.toPx() + offsetX
            var col = 0
            while (x < size.width) {
                val symbolIndex = (row * 7 + col * 3) % 6
                drawDoodleSymbol(symbolIndex, Offset(x, y), tint)
                x += stepX
                col++
            }
            y += stepY
            row++
        }
    }
}

private fun DrawScope.drawDoodleSymbol(index: Int, center: Offset, color: Color) {
    when (index) {
        0 -> { // Speech bubble
            val rect = androidx.compose.ui.geometry.Rect(
                center.x - 12.dp.toPx(),
                center.y - 8.dp.toPx(),
                center.x + 12.dp.toPx(),
                center.y + 8.dp.toPx()
            )
            drawRoundRect(
                color = color,
                topLeft = rect.topLeft,
                size = rect.size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx())
            )
        }
        1 -> { // Star / Sparkle
            val r = 7.dp.toPx()
            drawLine(color, Offset(center.x - r, center.y), Offset(center.x + r, center.y), 1.5.dp.toPx())
            drawLine(color, Offset(center.x, center.y - r), Offset(center.x, center.y + r), 1.5.dp.toPx())
        }
        2 -> { // Heart
            val path = Path().apply {
                val w = 14.dp.toPx()
                val h = 12.dp.toPx()
                moveTo(center.x, center.y + h / 2)
                cubicTo(
                    center.x - w, center.y - h / 3,
                    center.x - w / 2, center.y - h,
                    center.x, center.y - h / 3
                )
                cubicTo(
                    center.x + w / 2, center.y - h,
                    center.x + w, center.y - h / 3,
                    center.x, center.y + h / 2
                )
                close()
            }
            drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
        }
        3 -> { // Concentric rings / Geometric
            drawCircle(color, radius = 6.dp.toPx(), center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
            drawCircle(color, radius = 2.dp.toPx(), center = center)
        }
        4 -> { // Musical note / Smile
            val path = Path().apply {
                moveTo(center.x - 6.dp.toPx(), center.y - 4.dp.toPx())
                quadraticBezierTo(center.x, center.y + 6.dp.toPx(), center.x + 6.dp.toPx(), center.y - 4.dp.toPx())
            }
            drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
        }
        else -> { // Cross / Plus sparkle
            val r = 4.dp.toPx()
            drawCircle(color, radius = r, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
        }
    }
}
