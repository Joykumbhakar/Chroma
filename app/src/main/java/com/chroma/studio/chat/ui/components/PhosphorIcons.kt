package com.chroma.studio.chat.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ==========================================
// 📸 INPUT & ATTACHMENT ICONS (from Interface.html)
// ==========================================

@Composable
fun PhosphorCameraIcon(tint: Color = Color(0xFF8E8E93), size: Dp = 24.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        
        // Central camera lens circle
        drawCircle(
            color = tint,
            radius = 3.2f * scale,
            center = Offset(12f * scale, 13f * scale),
            style = stroke
        )
        
        // Camera body contour matching Interface.html SVG exactly
        val path = Path().apply {
            moveTo(9.778f * scale, 21f * scale)
            lineTo(14.222f * scale, 21f * scale)
            cubicTo(17.343f * scale, 21f * scale, 18.904f * scale, 21f * scale, 20.025f * scale, 20.265f * scale)
            cubicTo(20.573f * scale, 19.907f * scale, 20.985f * scale, 19.414f * scale, 21.251f * scale, 18.829f * scale)
            cubicTo(22f * scale, 17.729f * scale, 22f * scale, 16.196f * scale, 22f * scale, 13.132f * scale)
            cubicTo(22f * scale, 10.068f * scale, 22f * scale, 8.535f * scale, 21.251f * scale, 7.435f * scale)
            cubicTo(20.985f * scale, 6.85f * scale, 20.573f * scale, 6.357f * scale, 20.025f * scale, 5.999f * scale)
            cubicTo(19.305f * scale, 5.526f * scale, 18.403f * scale, 5.357f * scale, 17.022f * scale, 5.297f * scale)
            cubicTo(16.363f * scale, 5.297f * scale, 15.796f * scale, 4.807f * scale, 15.667f * scale, 4.172f * scale)
            cubicTo(15.42f * scale, 3.42f * scale, 14.59f * scale, 3f * scale, 13.634f * scale, 3f * scale)
            lineTo(10.366f * scale, 3f * scale)
            cubicTo(9.378f * scale, 3f * scale, 8.527f * scale, 3.685f * scale, 8.333f * scale, 4.636f * scale)
            cubicTo(8.204f * scale, 5.271f * scale, 7.637f * scale, 5.761f * scale, 6.978f * scale, 5.761f * scale)
            cubicTo(5.598f * scale, 5.821f * scale, 4.696f * scale, 5.991f * scale, 3.975f * scale, 6.463f * scale)
            cubicTo(3.427f * scale, 6.821f * scale, 3.015f * scale, 7.314f * scale, 2.749f * scale, 7.899f * scale)
            cubicTo(2f * scale, 8.999f * scale, 2f * scale, 10.531f * scale, 2f * scale, 13.596f * scale)
            cubicTo(2f * scale, 16.66f * scale, 2f * scale, 18.193f * scale, 2.749f * scale, 19.293f * scale)
            cubicTo(3.073f * scale, 19.769f * scale, 3.489f * scale, 20.178f * scale, 3.975f * scale, 20.497f * scale)
            cubicTo(5.096f * scale, 21f * scale, 6.657f * scale, 21f * scale, 9.778f * scale, 21f * scale)
            close()
        }
        drawPath(path, color = tint, style = stroke)
    }
}

@Composable
fun PhosphorPhotoIcon(tint: Color = Color(0xFF8E8E93), size: Dp = 24.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        
        // Outer rounded rectangle
        drawRoundRect(
            color = tint,
            topLeft = Offset(3f * scale, 3f * scale),
            size = Size(18f * scale, 18f * scale),
            cornerRadius = CornerRadius(4f * scale, 4f * scale),
            style = stroke
        )
        // Small sun / circle
        drawCircle(
            color = tint,
            radius = 2f * scale,
            center = Offset(8.5f * scale, 8.5f * scale),
            style = stroke
        )
        // Mountain path: d="m21 15l-4.5-4.5a2 2 0 0 0-2.828 0L6 21"
        val path = Path().apply {
            moveTo(21f * scale, 15f * scale)
            lineTo(16.5f * scale, 10.5f * scale)
            cubicTo(15.719f * scale, 9.719f * scale, 14.453f * scale, 9.719f * scale, 13.672f * scale, 10.5f * scale)
            lineTo(6f * scale, 21f * scale)
        }
        drawPath(path, color = tint, style = stroke)
    }
}

@Composable
fun PhosphorFileIcon(tint: Color = Color(0xFF8E8E93), size: Dp = 24.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Cross / Plus icon in Interface.html: d="M12 5v14M5 12h14"
        drawLine(color = tint, start = Offset(12f * scale, 5f * scale), end = Offset(12f * scale, 19f * scale), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(5f * scale, 12f * scale), end = Offset(19f * scale, 12f * scale), strokeWidth = stroke.width, cap = StrokeCap.Round)
    }
}

@Composable
fun PhosphorSmileyIcon(tint: Color = Color(0xFF8E8E93), size: Dp = 24.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Outer Circle
        drawCircle(color = tint, radius = 9f * scale, center = Offset(12f * scale, 12f * scale), style = stroke)
        // Eyes
        drawCircle(color = tint, radius = 1.2f * scale, center = Offset(9f * scale, 10f * scale))
        drawCircle(color = tint, radius = 1.2f * scale, center = Offset(15f * scale, 10f * scale))
        // Smile arc
        val smile = Path().apply {
            moveTo(8.5f * scale, 14.5f * scale)
            cubicTo(9.8f * scale, 17f * scale, 14.2f * scale, 17f * scale, 15.5f * scale, 14.5f * scale)
        }
        drawPath(smile, color = tint, style = stroke)
    }
}

@Composable
fun PhosphorVoiceMicIcon(tint: Color = Color(0xFF8E8E93), size: Dp = 24.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Audio equalizer waveform lines: d="M12 4v16m4-13v10M8 7v10m12-6v2M4 11v2"
        drawLine(color = tint, start = Offset(12f * scale, 4f * scale), end = Offset(12f * scale, 20f * scale), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(16f * scale, 7f * scale), end = Offset(16f * scale, 17f * scale), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(8f * scale, 7f * scale), end = Offset(8f * scale, 17f * scale), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(20f * scale, 11f * scale), end = Offset(20f * scale, 13f * scale), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(4f * scale, 11f * scale), end = Offset(4f * scale, 13f * scale), strokeWidth = stroke.width, cap = StrokeCap.Round)
    }
}

@Composable
fun PhosphorSendArrowIcon(tint: Color = Color.White, size: Dp = 16.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // d="m5 12l7-7l7 7m-7 7V5"
        val path = Path().apply {
            moveTo(5.5f * scale, 12f * scale)
            lineTo(12f * scale, 5.5f * scale)
            lineTo(18.5f * scale, 12f * scale)
        }
        drawPath(path, color = tint, style = stroke)
        drawLine(color = tint, start = Offset(12f * scale, 18.5f * scale), end = Offset(12f * scale, 5.5f * scale), strokeWidth = stroke.width, cap = StrokeCap.Round)
    }
}

@Composable
fun PhosphorPaperPlaneTilt(tint: Color = Color.White, size: Dp = 16.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(22f * scale, 2f * scale)
            lineTo(11f * scale, 13f * scale)
            moveTo(22f * scale, 2f * scale)
            lineTo(15f * scale, 22f * scale)
            lineTo(11f * scale, 13f * scale)
            lineTo(2f * scale, 9f * scale)
            close()
        }
        drawPath(path, color = tint, style = stroke)
    }
}

// ==========================================
// 🔍 NAVIGATION & HEADER ICONS
// ==========================================

@Composable
fun PhosphorMagnifyingGlass(tint: Color = Color(0xFF0078FE), size: Dp = 22.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawCircle(color = tint, radius = 6.5f * scale, center = Offset(10.5f * scale, 10.5f * scale), style = stroke)
        drawLine(color = tint, start = Offset(15.5f * scale, 15.5f * scale), end = Offset(20.5f * scale, 20.5f * scale), strokeWidth = stroke.width, cap = StrokeCap.Round)
    }
}

@Composable
fun PhosphorPhone(tint: Color = Color(0xFF0078FE), size: Dp = 22.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(20.5f * scale, 16.5f * scale)
            cubicTo(20.5f * scale, 18.5f * scale, 18f * scale, 21f * scale, 15.5f * scale, 21f * scale)
            cubicTo(9f * scale, 21f * scale, 3f * scale, 15f * scale, 3f * scale, 8.5f * scale)
            cubicTo(3f * scale, 6f * scale, 5.5f * scale, 3.5f * scale, 7.5f * scale, 3.5f * scale)
            cubicTo(8.5f * scale, 3.5f * scale, 9.5f * scale, 4.5f * scale, 10f * scale, 6f * scale)
            lineTo(11f * scale, 8.5f * scale)
            cubicTo(11.5f * scale, 9.5f * scale, 11f * scale, 10.5f * scale, 10f * scale, 11f * scale)
            cubicTo(11f * scale, 13f * scale, 13f * scale, 15f * scale, 15f * scale, 16f * scale)
            cubicTo(15.5f * scale, 15f * scale, 16.5f * scale, 14.5f * scale, 17.5f * scale, 15f * scale)
            lineTo(20f * scale, 16f * scale)
            close()
        }
        drawPath(path, color = tint, style = stroke)
    }
}

@Composable
fun PhosphorVideoCamera(tint: Color = Color(0xFF0078FE), size: Dp = 22.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawRoundRect(
            color = tint,
            topLeft = Offset(3f * scale, 6f * scale),
            size = Size(13f * scale, 12f * scale),
            cornerRadius = CornerRadius(3f * scale, 3f * scale),
            style = stroke
        )
        val path = Path().apply {
            moveTo(16f * scale, 10f * scale)
            lineTo(21f * scale, 7f * scale)
            lineTo(21f * scale, 17f * scale)
            lineTo(16f * scale, 14f * scale)
        }
        drawPath(path, color = tint, style = stroke)
    }
}

@Composable
fun PhosphorDotsThreeVertical(tint: Color = Color(0xFF0078FE), size: Dp = 22.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        drawCircle(color = tint, radius = 1.8f * scale, center = Offset(12f * scale, 6f * scale))
        drawCircle(color = tint, radius = 1.8f * scale, center = Offset(12f * scale, 12f * scale))
        drawCircle(color = tint, radius = 1.8f * scale, center = Offset(12f * scale, 18f * scale))
    }
}

@Composable
fun PhosphorChevronLeft(tint: Color = Color(0xFF0078FE), size: Dp = 24.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(15f * scale, 18f * scale)
            lineTo(9f * scale, 12f * scale)
            lineTo(15f * scale, 6f * scale)
        }
        drawPath(path, color = tint, style = stroke)
    }
}

@Composable
fun PhosphorSingleCheck(tint: Color = Color(0xFF34C759), size: Dp = 13.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.35f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(4.5f * scale, 12f * scale)
            lineTo(9.5f * scale, 17f * scale)
            lineTo(19.5f * scale, 7f * scale)
        }
        drawPath(path, color = tint, style = stroke)
    }
}

@Composable
fun PhosphorDoubleCheck(tint: Color = Color(0xFF34C759), size: Dp = 15.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.35f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // First / Left check
        val path1 = Path().apply {
            moveTo(2.5f * scale, 12f * scale)
            lineTo(7.5f * scale, 17f * scale)
            lineTo(15.5f * scale, 9f * scale)
        }
        drawPath(path1, color = tint, style = stroke)
        // Second / Right check (staggered by 6px)
        val path2 = Path().apply {
            moveTo(8.5f * scale, 12f * scale)
            lineTo(13.5f * scale, 17f * scale)
            lineTo(21.5f * scale, 7f * scale)
        }
        drawPath(path2, color = tint, style = stroke)
    }
}

@Composable
fun PhosphorGreenCheck(size: Dp = 13.dp, modifier: Modifier = Modifier) {
    PhosphorSingleCheck(tint = Color(0xFF34C759), size = size, modifier = modifier)
}

@Composable
fun PhosphorGreenDoubleCheck(size: Dp = 15.dp, modifier: Modifier = Modifier) {
    PhosphorDoubleCheck(tint = Color(0xFF34C759), size = size, modifier = modifier)
}

@Composable
fun PhosphorPendingClock(tint: Color = Color(0xFF8E8E93), size: Dp = 12.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawCircle(
            color = tint,
            radius = 8f * scale,
            center = Offset(12f * scale, 12f * scale),
            style = stroke
        )
        val path = Path().apply {
            moveTo(12f * scale, 7f * scale)
            lineTo(12f * scale, 12f * scale)
            lineTo(15.5f * scale, 14f * scale)
        }
        drawPath(path, color = tint, style = stroke)
    }
}

/**
 * Modern iOS 18 / WhatsApp / Telegram pixel-perfect status tick for messages.
 * Displays:
 *  - "pending": small clock icon
 *  - "sent": single tick (normal tint)
 *  - "delivered": double tick (normal tint)
 *  - "read" / "seen": double tick with readTint (sky blue / cyan #38BDF8)
 */
@Composable
fun MessageStatusTick(
    status: String,
    isOutgoing: Boolean = true,
    normalTint: Color = Color(0xD8FFFFFF),
    readTint: Color = Color(0xFF38BDF8),
    pendingTint: Color = Color(0xB3FFFFFF),
    size: Dp = 13.dp,
    modifier: Modifier = Modifier
) {
    if (!isOutgoing) return
    when (status.lowercase()) {
        "pending" -> {
            PhosphorPendingClock(
                tint = pendingTint,
                size = (size.value - 1.5f).coerceAtLeast(10f).dp,
                modifier = modifier
            )
        }
        "sent" -> {
            PhosphorSingleCheck(
                tint = normalTint,
                size = size,
                modifier = modifier
            )
        }
        "delivered" -> {
            PhosphorDoubleCheck(
                tint = normalTint,
                size = (size.value + 1.5f).dp,
                modifier = modifier
            )
        }
        "read", "seen" -> {
            PhosphorDoubleCheck(
                tint = readTint,
                size = (size.value + 1.5f).dp,
                modifier = modifier
            )
        }
        else -> {
            PhosphorSingleCheck(
                tint = normalTint,
                size = size,
                modifier = modifier
            )
        }
    }
}

@Composable
fun PhosphorPlay(tint: Color = Color(0xFF0078FE), size: Dp = 16.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val path = Path().apply {
            moveTo(7.5f * scale, 5.5f * scale)
            lineTo(18.5f * scale, 12f * scale)
            lineTo(7.5f * scale, 18.5f * scale)
            close()
        }
        drawPath(path, color = tint)
    }
}

@Composable
fun LucidePlay(tint: Color = Color(0xFF0078FE), size: Dp = 16.dp, modifier: Modifier = Modifier) {
    PhosphorPlay(tint = tint, size = size, modifier = modifier)
}

@Composable
fun PhosphorPause(tint: Color = Color(0xFF0078FE), size: Dp = 16.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.4f * density, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(9f * scale, 6f * scale), end = Offset(9f * scale, 18f * scale), strokeWidth = stroke.width, cap = stroke.cap)
        drawLine(color = tint, start = Offset(15f * scale, 6f * scale), end = Offset(15f * scale, 18f * scale), strokeWidth = stroke.width, cap = stroke.cap)
    }
}

@Composable
fun LucidePause(tint: Color = Color(0xFF0078FE), size: Dp = 16.dp, modifier: Modifier = Modifier) {
    PhosphorPause(tint = tint, size = size, modifier = modifier)
}

@Composable
fun PhosphorPushPin(tint: Color = Color(0xFF0078FE), size: Dp = 16.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(16f * scale, 3f * scale)
            lineTo(21f * scale, 8f * scale)
            lineTo(18f * scale, 11f * scale)
            lineTo(19f * scale, 15f * scale)
            lineTo(14f * scale, 15f * scale)
            lineTo(10f * scale, 19f * scale)
            lineTo(5f * scale, 14f * scale)
            lineTo(9f * scale, 10f * scale)
            lineTo(9f * scale, 5f * scale)
            lineTo(13f * scale, 6f * scale)
            close()
        }
        drawPath(path, color = tint, style = stroke)
        drawLine(color = tint, start = Offset(7.5f * scale, 16.5f * scale), end = Offset(3f * scale, 21f * scale), strokeWidth = stroke.width, cap = StrokeCap.Round)
    }
}

@Composable
fun PhosphorCloseX(tint: Color = Color(0xFF64748B), size: Dp = 14.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawLine(color = tint, start = Offset(5.5f * scale, 5.5f * scale), end = Offset(18.5f * scale, 18.5f * scale), strokeWidth = stroke.width, cap = stroke.cap)
        drawLine(color = tint, start = Offset(18.5f * scale, 5.5f * scale), end = Offset(5.5f * scale, 18.5f * scale), strokeWidth = stroke.width, cap = stroke.cap)
    }
}

@Composable
fun LucideCloseX(tint: Color = Color(0xFF64748B), size: Dp = 14.dp, modifier: Modifier = Modifier) {
    PhosphorCloseX(tint = tint, size = size, modifier = modifier)
}

@Composable
fun PhosphorPlusIcon(tint: Color = Color.White, size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawLine(color = tint, start = Offset(12f * scale, 5f * scale), end = Offset(12f * scale, 19f * scale), strokeWidth = stroke.width, cap = stroke.cap)
        drawLine(color = tint, start = Offset(5f * scale, 12f * scale), end = Offset(19f * scale, 12f * scale), strokeWidth = stroke.width, cap = stroke.cap)
    }
}

// ==========================================
// 🗑️ LUCIDE ACTION & SYSTEM ICONS
// ==========================================

// Lucide Thin Trash Icon (Delete for me)
@Composable
fun LucideTrash(tint: Color = Color(0xFFEF4444), size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Top line
        drawLine(color = tint, start = Offset(3f * scale, 6f * scale), end = Offset(21f * scale, 6f * scale), strokeWidth = stroke.width, cap = stroke.cap)
        // Lid
        val lid = Path().apply {
            moveTo(8f * scale, 6f * scale)
            lineTo(8f * scale, 4f * scale)
            cubicTo(8f * scale, 2.9f * scale, 8.9f * scale, 2f * scale, 10f * scale, 2f * scale)
            lineTo(14f * scale, 2f * scale)
            cubicTo(15.1f * scale, 2f * scale, 16f * scale, 2.9f * scale, 16f * scale, 4f * scale)
            lineTo(16f * scale, 6f * scale)
        }
        drawPath(lid, color = tint, style = stroke)
        // Body
        val body = Path().apply {
            moveTo(19f * scale, 6f * scale)
            lineTo(18.1f * scale, 19.3f * scale)
            cubicTo(18f * scale, 20.3f * scale, 17.2f * scale, 21.1f * scale, 16.2f * scale, 21.1f * scale)
            lineTo(7.8f * scale, 21.1f * scale)
            cubicTo(6.8f * scale, 21.1f * scale, 6f * scale, 20.3f * scale, 5.9f * scale, 19.3f * scale)
            lineTo(5f * scale, 6f * scale)
        }
        drawPath(body, color = tint, style = stroke)
        // Lines
        drawLine(color = tint, start = Offset(10f * scale, 11f * scale), end = Offset(10f * scale, 17f * scale), strokeWidth = stroke.width, cap = stroke.cap)
        drawLine(color = tint, start = Offset(14f * scale, 11f * scale), end = Offset(14f * scale, 17f * scale), strokeWidth = stroke.width, cap = stroke.cap)
    }
}

// Lucide Thick / Fill Danger Trash Icon (For "Delete for All")
@Composable
fun LucideTrashFill(tint: Color = Color(0xFFEF4444), size: Dp = 20.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Top handle bar
        drawLine(color = tint, start = Offset(2f * scale, 6f * scale), end = Offset(22f * scale, 6f * scale), strokeWidth = stroke.width, cap = stroke.cap)
        // Lid
        val lid = Path().apply {
            moveTo(8f * scale, 6f * scale)
            lineTo(8f * scale, 3.5f * scale)
            cubicTo(8f * scale, 2.5f * scale, 9f * scale, 1.5f * scale, 10.5f * scale, 1.5f * scale)
            lineTo(13.5f * scale, 1.5f * scale)
            cubicTo(15f * scale, 1.5f * scale, 16f * scale, 2.5f * scale, 16f * scale, 3.5f * scale)
            lineTo(16f * scale, 6f * scale)
            close()
        }
        drawPath(lid, color = tint)
        // Filled Body
        val body = Path().apply {
            moveTo(19f * scale, 6f * scale)
            lineTo(18.2f * scale, 19.5f * scale)
            cubicTo(18.1f * scale, 20.9f * scale, 17f * scale, 22f * scale, 15.6f * scale, 22f * scale)
            lineTo(8.4f * scale, 22f * scale)
            cubicTo(7f * scale, 22f * scale, 5.9f * scale, 20.9f * scale, 5.8f * scale, 19.5f * scale)
            lineTo(5f * scale, 6f * scale)
            close()
        }
        drawPath(body, color = tint.copy(alpha = 0.2f))
        drawPath(body, color = tint, style = stroke)
        // White inner lines
        drawLine(color = Color.White, start = Offset(10f * scale, 10.5f * scale), end = Offset(10f * scale, 17.5f * scale), strokeWidth = 1.25f * density, cap = StrokeCap.Round)
        drawLine(color = Color.White, start = Offset(14f * scale, 10.5f * scale), end = Offset(14f * scale, 17.5f * scale), strokeWidth = 1.25f * density, cap = StrokeCap.Round)
    }
}

// Lucide Thin Paperclip
@Composable
fun LucidePaperclip(tint: Color = Color(0xFF8E8E93), size: Dp = 20.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(21.44f * scale, 11.05f * scale)
            lineTo(12.25f * scale, 20.24f * scale)
            cubicTo(9.91f * scale, 22.58f * scale, 6.11f * scale, 22.58f * scale, 3.77f * scale, 20.24f * scale)
            cubicTo(1.43f * scale, 17.9f * scale, 1.43f * scale, 14.1f * scale, 3.77f * scale, 11.76f * scale)
            lineTo(12.96f * scale, 2.57f * scale)
            cubicTo(14.52f * scale, 1.01f * scale, 17.05f * scale, 1.01f * scale, 18.61f * scale, 2.57f * scale)
            cubicTo(20.17f * scale, 4.13f * scale, 20.17f * scale, 6.66f * scale, 18.61f * scale, 8.22f * scale)
            lineTo(9.41f * scale, 17.41f * scale)
            cubicTo(8.63f * scale, 18.19f * scale, 7.37f * scale, 18.19f * scale, 6.59f * scale, 17.41f * scale)
            cubicTo(5.81f * scale, 16.63f * scale, 5.81f * scale, 15.37f * scale, 6.59f * scale, 14.59f * scale)
            lineTo(15.07f * scale, 6.1f * scale)
        }
        drawPath(path, color = tint, style = stroke)
    }
}

// Lucide Thin Reply
@Composable
fun LucideReply(tint: Color = Color(0xFF0078FE), size: Dp = 16.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(9f * scale, 17f * scale)
            lineTo(4f * scale, 12f * scale)
            lineTo(9f * scale, 7f * scale)
        }
        drawPath(path, color = tint, style = stroke)
        val linePath = Path().apply {
            moveTo(20f * scale, 18f * scale)
            lineTo(20f * scale, 16f * scale)
            cubicTo(20f * scale, 13.8f * scale, 18.2f * scale, 12f * scale, 16f * scale, 12f * scale)
            lineTo(4f * scale, 12f * scale)
        }
        drawPath(linePath, color = tint, style = stroke)
    }
}

// Lucide Thin Edit / Pencil
@Composable
fun LucideEdit(tint: Color = Color(0xFF8E8E93), size: Dp = 15.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(12f * scale, 20f * scale)
            lineTo(20f * scale, 20f * scale)
        }
        drawPath(path, color = tint, style = stroke)
        val pencil = Path().apply {
            moveTo(16.5f * scale, 3.5f * scale)
            cubicTo(17.33f * scale, 2.67f * scale, 18.67f * scale, 2.67f * scale, 19.5f * scale, 3.5f * scale)
            cubicTo(20.33f * scale, 4.33f * scale, 20.33f * scale, 5.67f * scale, 19.5f * scale, 6.5f * scale)
            lineTo(7f * scale, 19f * scale)
            lineTo(3f * scale, 20f * scale)
            lineTo(4f * scale, 16f * scale)
            close()
        }
        drawPath(pencil, color = tint, style = stroke)
    }
}

// Lucide Thin Copy
@Composable
fun LucideCopy(tint: Color = Color(0xFF8E8E93), size: Dp = 15.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val backPath = Path().apply {
            moveTo(5f * scale, 15f * scale)
            lineTo(4f * scale, 15f * scale)
            cubicTo(2.9f * scale, 15f * scale, 2f * scale, 14.1f * scale, 2f * scale, 13f * scale)
            lineTo(2f * scale, 4f * scale)
            cubicTo(2f * scale, 2.9f * scale, 2.9f * scale, 2f * scale, 4f * scale, 2f * scale)
            lineTo(13f * scale, 2f * scale)
            cubicTo(14.1f * scale, 2f * scale, 15f * scale, 2.9f * scale, 15f * scale, 4f * scale)
            lineTo(15f * scale, 5f * scale)
        }
        drawPath(backPath, color = tint, style = stroke)
        drawRoundRect(
            color = tint,
            topLeft = Offset(8f * scale, 8f * scale),
            size = Size(13f * scale, 13f * scale),
            cornerRadius = CornerRadius(2f * scale, 2f * scale),
            style = stroke
        )
    }
}

// Lucide Lock (Padlock)
@Composable
fun LucideLock(tint: Color = Color(0xFF8E8E93), size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Lock body
        drawRoundRect(
            color = tint,
            topLeft = Offset(5f * scale, 11f * scale),
            size = Size(14f * scale, 10f * scale),
            cornerRadius = CornerRadius(2.5f * scale, 2.5f * scale),
            style = stroke
        )
        // Shackle
        val shackle = Path().apply {
            moveTo(8f * scale, 11f * scale)
            lineTo(8f * scale, 7f * scale)
            cubicTo(8f * scale, 4.8f * scale, 9.8f * scale, 3f * scale, 12f * scale, 3f * scale)
            cubicTo(14.2f * scale, 3f * scale, 16f * scale, 4.8f * scale, 16f * scale, 7f * scale)
            lineTo(16f * scale, 11f * scale)
        }
        drawPath(shackle, color = tint, style = stroke)
    }
}

// Lucide Bell (Notification on)
@Composable
fun LucideBell(tint: Color = Color(0xFF8E8E93), size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(6f * scale, 8f * scale)
            cubicTo(6f * scale, 4.7f * scale, 8.7f * scale, 2f * scale, 12f * scale, 2f * scale)
            cubicTo(15.3f * scale, 2f * scale, 18f * scale, 4.7f * scale, 18f * scale, 8f * scale)
            cubicTo(18f * scale, 15f * scale, 21f * scale, 17f * scale, 21f * scale, 17f * scale)
            lineTo(3f * scale, 17f * scale)
            cubicTo(3f * scale, 17f * scale, 6f * scale, 15f * scale, 6f * scale, 8f * scale)
        }
        drawPath(path, color = tint, style = stroke)
        val clapper = Path().apply {
            moveTo(10.3f * scale, 20f * scale)
            cubicTo(10.7f * scale, 20.6f * scale, 11.3f * scale, 21f * scale, 12f * scale, 21f * scale)
            cubicTo(12.7f * scale, 21f * scale, 13.3f * scale, 20.6f * scale, 13.7f * scale, 20f * scale)
        }
        drawPath(clapper, color = tint, style = stroke)
    }
}

// Lucide Bell Off (Muted notifications)
@Composable
fun LucideBellOff(tint: Color = Color(0xFF8E8E93), size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(8.7f * scale, 3f * scale)
            cubicTo(9.7f * scale, 2.4f * scale, 10.8f * scale, 2f * scale, 12f * scale, 2f * scale)
            cubicTo(15.3f * scale, 2f * scale, 18f * scale, 4.7f * scale, 18f * scale, 8f * scale)
            cubicTo(18f * scale, 11f * scale, 18.7f * scale, 13.5f * scale, 19.5f * scale, 15.5f * scale)
            moveTo(17f * scale, 17f * scale)
            lineTo(3f * scale, 17f * scale)
            cubicTo(3f * scale, 17f * scale, 6f * scale, 15f * scale, 6f * scale, 8f * scale)
            cubicTo(6f * scale, 7.3f * scale, 6.1f * scale, 6.7f * scale, 6.3f * scale, 6.1f * scale)
        }
        drawPath(path, color = tint, style = stroke)
        drawLine(color = tint, start = Offset(2f * scale, 2f * scale), end = Offset(22f * scale, 22f * scale), strokeWidth = stroke.width, cap = stroke.cap)
    }
}

// Lucide Info
@Composable
fun LucideInfo(tint: Color = Color(0xFF8E8E93), size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawCircle(color = tint, radius = 9f * scale, center = Offset(12f * scale, 12f * scale), style = stroke)
        drawLine(color = tint, start = Offset(12f * scale, 16f * scale), end = Offset(12f * scale, 12f * scale), strokeWidth = stroke.width, cap = stroke.cap)
        drawCircle(color = tint, radius = 1f * scale, center = Offset(12f * scale, 8f * scale))
    }
}

// Lucide Moon (Dark Mode)
@Composable
fun LucideMoon(tint: Color = Color(0xFF8E8E93), size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(21f * scale, 12.79f * scale)
            cubicTo(20.35f * scale, 17.41f * scale, 16.41f * scale, 21f * scale, 11.5f * scale, 21f * scale)
            cubicTo(6.25f * scale, 21f * scale, 2f * scale, 16.75f * scale, 2f * scale, 11.5f * scale)
            cubicTo(2f * scale, 6.59f * scale, 5.59f * scale, 2.65f * scale, 10.21f * scale, 2f * scale)
            cubicTo(9.06f * scale, 3.52f * scale, 8.4f * scale, 5.43f * scale, 8.4f * scale, 7.5f * scale)
            cubicTo(8.4f * scale, 12.19f * scale, 12.21f * scale, 16f * scale, 16.9f * scale, 16f * scale)
            cubicTo(18.97f * scale, 16f * scale, 20.88f * scale, 15.34f * scale, 22.4f * scale, 14.19f * scale)
        }
        drawPath(path, color = tint, style = stroke)
    }
}

// Lucide Palette
@Composable
fun LucidePalette(tint: Color = Color(0xFF8E8E93), size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(12f * scale, 2f * scale)
            cubicTo(6.5f * scale, 2f * scale, 2f * scale, 6.5f * scale, 2f * scale, 12f * scale)
            cubicTo(2f * scale, 17.5f * scale, 6.5f * scale, 22f * scale, 12f * scale, 22f * scale)
            cubicTo(13.1f * scale, 22f * scale, 14f * scale, 21.1f * scale, 14f * scale, 20f * scale)
            cubicTo(14f * scale, 19.5f * scale, 13.8f * scale, 19f * scale, 13.4f * scale, 18.6f * scale)
            cubicTo(13f * scale, 18.2f * scale, 12.8f * scale, 17.6f * scale, 12.8f * scale, 17f * scale)
            cubicTo(12.8f * scale, 15.3f * scale, 14.1f * scale, 14f * scale, 15.8f * scale, 14f * scale)
            lineTo(18f * scale, 14f * scale)
            cubicTo(20.2f * scale, 14f * scale, 22f * scale, 12.2f * scale, 22f * scale, 10f * scale)
            cubicTo(22f * scale, 5.6f * scale, 17.5f * scale, 2f * scale, 12f * scale, 2f * scale)
        }
        drawPath(path, color = tint, style = stroke)
        drawCircle(color = tint, radius = 1.3f * scale, center = Offset(7.5f * scale, 11f * scale))
        drawCircle(color = tint, radius = 1.3f * scale, center = Offset(10.5f * scale, 7.5f * scale))
        drawCircle(color = tint, radius = 1.3f * scale, center = Offset(14.5f * scale, 7.5f * scale))
        drawCircle(color = tint, radius = 1.3f * scale, center = Offset(17.5f * scale, 11f * scale))
    }
}

// Lucide Wallpaper / Image
@Composable
fun LucideWallpaper(tint: Color = Color(0xFF8E8E93), size: Dp = 18.dp, modifier: Modifier = Modifier) {
    PhosphorPhotoIcon(tint = tint, size = size, modifier = modifier)
}

// Lucide Poll / Bar Chart
@Composable
fun LucidePoll(tint: Color = Color(0xFF8E8E93), size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawLine(color = tint, start = Offset(18f * scale, 20f * scale), end = Offset(18f * scale, 10f * scale), strokeWidth = stroke.width, cap = stroke.cap)
        drawLine(color = tint, start = Offset(12f * scale, 20f * scale), end = Offset(12f * scale, 4f * scale), strokeWidth = stroke.width, cap = stroke.cap)
        drawLine(color = tint, start = Offset(6f * scale, 20f * scale), end = Offset(6f * scale, 14f * scale), strokeWidth = stroke.width, cap = stroke.cap)
    }
}

// Lucide Download
@Composable
fun LucideDownload(tint: Color = Color(0xFF8E8E93), size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val arrow = Path().apply {
            moveTo(8f * scale, 12f * scale)
            lineTo(12f * scale, 16f * scale)
            lineTo(16f * scale, 12f * scale)
        }
        drawPath(arrow, color = tint, style = stroke)
        drawLine(color = tint, start = Offset(12f * scale, 3f * scale), end = Offset(12f * scale, 16f * scale), strokeWidth = stroke.width, cap = stroke.cap)
        val tray = Path().apply {
            moveTo(4f * scale, 17f * scale)
            lineTo(4f * scale, 20f * scale)
            lineTo(20f * scale, 20f * scale)
            lineTo(20f * scale, 17f * scale)
        }
        drawPath(tray, color = tint, style = stroke)
    }
}

// Lucide Ban / Block
@Composable
fun LucideBan(tint: Color = Color(0xFFEF4444), size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawCircle(color = tint, radius = 9f * scale, center = Offset(12f * scale, 12f * scale), style = stroke)
        drawLine(color = tint, start = Offset(5.6f * scale, 5.6f * scale), end = Offset(18.4f * scale, 18.4f * scale), strokeWidth = stroke.width, cap = stroke.cap)
    }
}

// Lucide MapPin (Location)
@Composable
fun LucideMapPin(tint: Color = Color(0xFFF43F5E), size: Dp = 22.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(20f * scale, 10f * scale)
            cubicTo(20f * scale, 16f * scale, 12f * scale, 22f * scale, 12f * scale, 22f * scale)
            cubicTo(12f * scale, 22f * scale, 4f * scale, 16f * scale, 4f * scale, 10f * scale)
            cubicTo(4f * scale, 5.58f * scale, 7.58f * scale, 2f * scale, 12f * scale, 2f * scale)
            cubicTo(16.42f * scale, 2f * scale, 20f * scale, 5.58f * scale, 20f * scale, 10f * scale)
            close()
        }
        drawPath(path, color = tint, style = stroke)
        drawCircle(color = tint, radius = 3f * scale, center = Offset(12f * scale, 10f * scale), style = stroke)
    }
}

// Lucide FileText (Document file)
@Composable
fun LucideFileText(tint: Color = Color(0xFF0078FE), size: Dp = 22.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(14f * scale, 2f * scale)
            lineTo(6f * scale, 2f * scale)
            cubicTo(4.9f * scale, 2f * scale, 4f * scale, 2.9f * scale, 4f * scale, 4f * scale)
            lineTo(4f * scale, 20f * scale)
            cubicTo(4f * scale, 21.1f * scale, 4.9f * scale, 22f * scale, 6f * scale, 22f * scale)
            lineTo(18f * scale, 22f * scale)
            cubicTo(19.1f * scale, 22f * scale, 20f * scale, 21.1f * scale, 20f * scale, 20f * scale)
            lineTo(20f * scale, 8f * scale)
            lineTo(14f * scale, 2f * scale)
            close()
        }
        drawPath(path, color = tint, style = stroke)
        // Fold
        val fold = Path().apply {
            moveTo(14f * scale, 2f * scale)
            lineTo(14f * scale, 8f * scale)
            lineTo(20f * scale, 8f * scale)
        }
        drawPath(fold, color = tint, style = stroke)
        // Text lines
        drawLine(color = tint, start = Offset(8f * scale, 13f * scale), end = Offset(16f * scale, 13f * scale), strokeWidth = stroke.width, cap = stroke.cap)
        drawLine(color = tint, start = Offset(8f * scale, 17f * scale), end = Offset(14f * scale, 17f * scale), strokeWidth = stroke.width, cap = stroke.cap)
    }
}

// Lucide Heart (Fill & Outline)
@Composable
fun LucideHeart(tint: Color = Color(0xFFFF453A), isFilled: Boolean = true, size: Dp = 20.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(12f * scale, 21.35f * scale)
            lineTo(10.55f * scale, 20.03f * scale)
            cubicTo(5.4f * scale, 15.36f * scale, 2f * scale, 12.28f * scale, 2f * scale, 8.5f * scale)
            cubicTo(2f * scale, 5.42f * scale, 4.42f * scale, 3f * scale, 7.5f * scale, 3f * scale)
            cubicTo(9.24f * scale, 3f * scale, 10.91f * scale, 3.81f * scale, 12f * scale, 5.09f * scale)
            cubicTo(13.09f * scale, 3.81f * scale, 14.76f * scale, 3f * scale, 16.5f * scale, 3f * scale)
            cubicTo(19.58f * scale, 3f * scale, 22f * scale, 5.42f * scale, 22f * scale, 8.5f * scale)
            cubicTo(22f * scale, 12.28f * scale, 18.6f * scale, 15.36f * scale, 13.45f * scale, 20.04f * scale)
            lineTo(12f * scale, 21.35f * scale)
            close()
        }
        if (isFilled) {
            drawPath(path, color = tint)
        } else {
            drawPath(path, color = tint, style = stroke)
        }
    }
}

// Lucide Eye
@Composable
fun LucideEye(tint: Color = Color.White, size: Dp = 20.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        
        val path = Path().apply {
            moveTo(2f * scale, 12f * scale)
            cubicTo(5f * scale, 19f * scale, 19f * scale, 19f * scale, 22f * scale, 12f * scale)
            cubicTo(19f * scale, 5f * scale, 5f * scale, 5f * scale, 2f * scale, 12f * scale)
            close()
        }
        drawPath(path, color = tint, style = stroke)
        drawCircle(color = tint, radius = 3f * scale, center = Offset(12f * scale, 12f * scale), style = stroke)
    }
}


// Lucide Arrow Down (Scroll to bottom)
@Composable
fun LucideArrowDown(tint: Color = Color.White, size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val arrow = Path().apply {
            moveTo(6f * scale, 13f * scale)
            lineTo(12f * scale, 19f * scale)
            lineTo(18f * scale, 13f * scale)
        }
        drawPath(arrow, color = tint, style = stroke)
        drawLine(color = tint, start = Offset(12f * scale, 5f * scale), end = Offset(12f * scale, 19f * scale), strokeWidth = stroke.width, cap = stroke.cap)
    }
}

// Lucide Microphone
@Composable
fun LucideMic(tint: Color = Color.White, size: Dp = 22.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawRoundRect(
            color = tint,
            topLeft = Offset(9f * scale, 2f * scale),
            size = Size(6f * scale, 12f * scale),
            cornerRadius = CornerRadius(3f * scale, 3f * scale),
            style = stroke
        )
        val arc = Path().apply {
            moveTo(5f * scale, 10f * scale)
            cubicTo(5f * scale, 14f * scale, 8f * scale, 17f * scale, 12f * scale, 17f * scale)
            cubicTo(16f * scale, 17f * scale, 19f * scale, 14f * scale, 19f * scale, 10f * scale)
        }
        drawPath(arc, color = tint, style = stroke)
        drawLine(color = tint, start = Offset(12f * scale, 17f * scale), end = Offset(12f * scale, 21f * scale), strokeWidth = stroke.width, cap = stroke.cap)
        drawLine(color = tint, start = Offset(8f * scale, 21f * scale), end = Offset(16f * scale, 21f * scale), strokeWidth = stroke.width, cap = stroke.cap)
    }
}

// Lucide Microphone Off (Muted)
@Composable
fun LucideMicOff(tint: Color = Color.White, size: Dp = 22.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(9f * scale, 9f * scale)
            lineTo(9f * scale, 5f * scale)
            cubicTo(9f * scale, 3.3f * scale, 10.3f * scale, 2f * scale, 12f * scale, 2f * scale)
            cubicTo(13.7f * scale, 2f * scale, 15f * scale, 3.3f * scale, 15f * scale, 5f * scale)
            lineTo(15f * scale, 10f * scale)
        }
        drawPath(path, color = tint, style = stroke)
        val arc = Path().apply {
            moveTo(5f * scale, 10f * scale)
            cubicTo(5f * scale, 13.5f * scale, 7.5f * scale, 16.3f * scale, 11f * scale, 16.9f * scale)
            moveTo(19f * scale, 10f * scale)
            cubicTo(19f * scale, 11.2f * scale, 18.7f * scale, 12.3f * scale, 18.1f * scale, 13.3f * scale)
        }
        drawPath(arc, color = tint, style = stroke)
        drawLine(color = tint, start = Offset(12f * scale, 17f * scale), end = Offset(12f * scale, 21f * scale), strokeWidth = stroke.width, cap = stroke.cap)
        drawLine(color = tint, start = Offset(8f * scale, 21f * scale), end = Offset(16f * scale, 21f * scale), strokeWidth = stroke.width, cap = stroke.cap)
        drawLine(color = tint, start = Offset(2f * scale, 2f * scale), end = Offset(22f * scale, 22f * scale), strokeWidth = stroke.width, cap = stroke.cap)
    }
}

// Lucide Video Camera
@Composable
fun LucideVideo(tint: Color = Color.White, size: Dp = 22.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawRoundRect(
            color = tint,
            topLeft = Offset(3f * scale, 6f * scale),
            size = Size(13f * scale, 12f * scale),
            cornerRadius = CornerRadius(3f * scale, 3f * scale),
            style = stroke
        )
        val path = Path().apply {
            moveTo(16f * scale, 10f * scale)
            lineTo(21f * scale, 7f * scale)
            lineTo(21f * scale, 17f * scale)
            lineTo(16f * scale, 14f * scale)
        }
        drawPath(path, color = tint, style = stroke)
    }
}

// Lucide Video Camera Off
@Composable
fun LucideVideoOff(tint: Color = Color.White, size: Dp = 22.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val cam = Path().apply {
            moveTo(10.66f * scale, 6f * scale)
            lineTo(14f * scale, 6f * scale)
            cubicTo(15.1f * scale, 6f * scale, 16f * scale, 6.9f * scale, 16f * scale, 8f * scale)
            lineTo(16f * scale, 11.34f * scale)
            moveTo(16f * scale, 16f * scale)
            cubicTo(16f * scale, 17.1f * scale, 15.1f * scale, 18f * scale, 14f * scale, 18f * scale)
            lineTo(5f * scale, 18f * scale)
            cubicTo(3.9f * scale, 18f * scale, 3f * scale, 17.1f * scale, 3f * scale, 16f * scale)
            lineTo(3f * scale, 8f * scale)
            cubicTo(3f * scale, 7.37f * scale, 3.28f * scale, 6.82f * scale, 3.73f * scale, 6.45f * scale)
        }
        drawPath(cam, color = tint, style = stroke)
        val lens = Path().apply {
            moveTo(16f * scale, 10f * scale)
            lineTo(21f * scale, 7f * scale)
            lineTo(21f * scale, 17f * scale)
            lineTo(16f * scale, 14f * scale)
        }
        drawPath(lens, color = tint, style = stroke)
        drawLine(color = tint, start = Offset(2f * scale, 2f * scale), end = Offset(22f * scale, 22f * scale), strokeWidth = stroke.width, cap = stroke.cap)
    }
}

// Lucide Phone Hangup / Call End
@Composable
fun LucideCallEnd(tint: Color = Color.White, size: Dp = 24.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(4f * scale, 15f * scale)
            cubicTo(6.5f * scale, 12f * scale, 17.5f * scale, 12f * scale, 20f * scale, 15f * scale)
            lineTo(18.5f * scale, 18f * scale)
            cubicTo(17.5f * scale, 18.5f * scale, 16f * scale, 18f * scale, 15f * scale, 17f * scale)
            lineTo(14f * scale, 15.5f * scale)
            cubicTo(13f * scale, 15.2f * scale, 11f * scale, 15.2f * scale, 10f * scale, 15.5f * scale)
            lineTo(9f * scale, 17f * scale)
            cubicTo(8f * scale, 18f * scale, 6.5f * scale, 18.5f * scale, 5.5f * scale, 18f * scale)
            close()
        }
        drawPath(path, color = tint, style = stroke)
    }
}

// Lucide Volume / Speaker
@Composable
fun LucideVolume2(tint: Color = Color.White, size: Dp = 22.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val spk = Path().apply {
            moveTo(11f * scale, 5f * scale)
            lineTo(6f * scale, 9f * scale)
            lineTo(2f * scale, 9f * scale)
            lineTo(2f * scale, 15f * scale)
            lineTo(6f * scale, 15f * scale)
            lineTo(11f * scale, 19f * scale)
            close()
        }
        drawPath(spk, color = tint, style = stroke)
        val wave1 = Path().apply {
            moveTo(15.54f * scale, 8.46f * scale)
            cubicTo(17.49f * scale, 10.41f * scale, 17.49f * scale, 13.59f * scale, 15.54f * scale, 15.54f * scale)
        }
        drawPath(wave1, color = tint, style = stroke)
        val wave2 = Path().apply {
            moveTo(19.07f * scale, 4.93f * scale)
            cubicTo(22.98f * scale, 8.83f * scale, 22.98f * scale, 15.17f * scale, 19.07f * scale, 19.07f * scale)
        }
        drawPath(wave2, color = tint, style = stroke)
    }
}

// Lucide Check (Single Tick)
@Composable
fun LucideCheck(tint: Color = Color(0xFF34C759), size: Dp = 15.dp, modifier: Modifier = Modifier) {
    PhosphorSingleCheck(tint = tint, size = size, modifier = modifier)
}

// Lucide Check Check (Double Tick)
@Composable
fun LucideCheckCheck(tint: Color = Color(0xFF34C759), size: Dp = 16.dp, modifier: Modifier = Modifier) {
    PhosphorDoubleCheck(tint = tint, size = size, modifier = modifier)
}

// Lucide Chevron Left
@Composable
fun LucideChevronLeft(tint: Color = Color(0xFF007AFF), size: Dp = 24.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(15f * scale, 18f * scale)
            lineTo(9f * scale, 12f * scale)
            lineTo(15f * scale, 6f * scale)
        }
        drawPath(path, color = tint, style = stroke)
    }
}

// Lucide X (Close)
@Composable
fun LucideX(tint: Color = Color(0xFF8E8E93), size: Dp = 16.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawLine(color = tint, start = Offset(18f * scale, 6f * scale), end = Offset(6f * scale, 18f * scale), strokeWidth = stroke.width, cap = stroke.cap)
        drawLine(color = tint, start = Offset(6f * scale, 6f * scale), end = Offset(18f * scale, 18f * scale), strokeWidth = stroke.width, cap = stroke.cap)
    }
}

// Lucide Phone
@Composable
fun LucidePhone(tint: Color = Color(0xFF007AFF), size: Dp = 24.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(22f * scale, 16.92f * scale)
            lineTo(22f * scale, 19.92f * scale)
            cubicTo(22f * scale, 21.03f * scale, 21.1f * scale, 22f * scale, 20f * scale, 22f * scale)
            cubicTo(10.06f * scale, 22f * scale, 2f * scale, 13.94f * scale, 2f * scale, 4f * scale)
            cubicTo(2f * scale, 2.9f * scale, 2.9f * scale, 2f * scale, 4.08f * scale, 2f * scale)
            lineTo(7.08f * scale, 2f * scale)
            cubicTo(7.63f * scale, 2f * scale, 8.12f * scale, 2.37f * scale, 8.27f * scale, 2.9f * scale)
            lineTo(9.51f * scale, 7.15f * scale)
            cubicTo(9.63f * scale, 7.58f * scale, 9.5f * scale, 8.04f * scale, 9.17f * scale, 8.35f * scale)
            lineTo(7.61f * scale, 9.91f * scale)
            cubicTo(9.34f * scale, 13.06f * scale, 10.94f * scale, 14.66f * scale, 14.09f * scale, 16.39f * scale)
            lineTo(15.65f * scale, 14.83f * scale)
            cubicTo(15.96f * scale, 14.5f * scale, 16.42f * scale, 14.37f * scale, 16.85f * scale, 14.49f * scale)
            lineTo(21.1f * scale, 15.73f * scale)
            cubicTo(21.63f * scale, 15.88f * scale, 22f * scale, 16.37f * scale, 22f * scale, 16.92f * scale)
            close()
        }
        drawPath(path, color = tint, style = stroke)
    }
}

// Lucide Search
@Composable
fun LucideSearch(tint: Color = Color(0xFF007AFF), size: Dp = 24.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawCircle(
            color = tint,
            radius = 7.5f * scale,
            center = Offset(11f * scale, 11f * scale),
            style = stroke
        )
        drawLine(
            color = tint,
            start = Offset(16.65f * scale, 16.65f * scale),
            end = Offset(21.5f * scale, 21.5f * scale),
            strokeWidth = stroke.width,
            cap = stroke.cap
        )
    }
}

// Lucide Mail
@Composable
fun LucideMail(tint: Color = Color(0xFF007AFF), size: Dp = 24.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawRoundRect(
            color = tint,
            topLeft = Offset(3f * scale, 4f * scale),
            size = Size(18f * scale, 16f * scale),
            cornerRadius = CornerRadius(2.5f * scale, 2.5f * scale),
            style = stroke
        )
        val envelope = Path().apply {
            moveTo(3.5f * scale, 6.5f * scale)
            lineTo(12f * scale, 13f * scale)
            lineTo(20.5f * scale, 6.5f * scale)
        }
        drawPath(envelope, color = tint, style = stroke)
    }
}

// Lucide Share
@Composable
fun LucideShare(tint: Color = Color(0xFF007AFF), size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val tray = Path().apply {
            moveTo(4f * scale, 12f * scale)
            lineTo(4f * scale, 20f * scale)
            lineTo(20f * scale, 20f * scale)
            lineTo(20f * scale, 12f * scale)
        }
        drawPath(tray, color = tint, style = stroke)
        val arrow = Path().apply {
            moveTo(16f * scale, 6f * scale)
            lineTo(12f * scale, 2f * scale)
            lineTo(8f * scale, 6f * scale)
        }
        drawPath(arrow, color = tint, style = stroke)
        drawLine(color = tint, start = Offset(12f * scale, 2f * scale), end = Offset(12f * scale, 15f * scale), strokeWidth = stroke.width, cap = stroke.cap)
    }
}

// Lucide Star
@Composable
fun LucideStar(tint: Color = Color(0xFF007AFF), size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(12f * scale, 2f * scale)
            lineTo(15.09f * scale, 8.26f * scale)
            lineTo(22f * scale, 9.27f * scale)
            lineTo(17f * scale, 14.14f * scale)
            lineTo(18.18f * scale, 21.02f * scale)
            lineTo(12f * scale, 17.77f * scale)
            lineTo(5.82f * scale, 21.02f * scale)
            lineTo(7f * scale, 14.14f * scale)
            lineTo(2f * scale, 9.27f * scale)
            lineTo(8.91f * scale, 8.26f * scale)
            close()
        }
        drawPath(path, color = tint, style = stroke)
    }
}

// Lucide Flag
@Composable
fun LucideFlag(tint: Color = Color(0xFFFF3B30), size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(4f * scale, 15f * scale)
            cubicTo(6f * scale, 13.5f * scale, 8f * scale, 13.5f * scale, 10f * scale, 15f * scale)
            cubicTo(12f * scale, 16.5f * scale, 14f * scale, 16.5f * scale, 16f * scale, 15f * scale)
            cubicTo(18f * scale, 13.5f * scale, 19f * scale, 14f * scale, 20f * scale, 15f * scale)
            lineTo(20f * scale, 4f * scale)
            cubicTo(19f * scale, 3f * scale, 18f * scale, 2.5f * scale, 16f * scale, 4f * scale)
            cubicTo(14f * scale, 5.5f * scale, 12f * scale, 5.5f * scale, 10f * scale, 4f * scale)
            cubicTo(8f * scale, 2.5f * scale, 6f * scale, 2.5f * scale, 4f * scale, 4f * scale)
            close()
        }
        drawPath(path, color = tint, style = stroke)
        drawLine(color = tint, start = Offset(4f * scale, 22f * scale), end = Offset(4f * scale, 15f * scale), strokeWidth = stroke.width, cap = stroke.cap)
    }
}

// Uiverse AI Voice Waveform Icon (from Uiverse snippet)
@Composable
fun UiverseVoiceWaveformIcon(tint: Color = Color(0xFF959595), size: Dp = 20.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // d="M12 4v16m4-13v10M8 7v10m12-6v2M4 11v2"
        drawLine(color = tint, start = Offset(12f * scale, 4f * scale), end = Offset(12f * scale, 20f * scale), strokeWidth = stroke.width, cap = stroke.cap)
        drawLine(color = tint, start = Offset(16f * scale, 7f * scale), end = Offset(16f * scale, 17f * scale), strokeWidth = stroke.width, cap = stroke.cap)
        drawLine(color = tint, start = Offset(8f * scale, 7f * scale), end = Offset(8f * scale, 17f * scale), strokeWidth = stroke.width, cap = stroke.cap)
        drawLine(color = tint, start = Offset(20f * scale, 11f * scale), end = Offset(20f * scale, 13f * scale), strokeWidth = stroke.width, cap = stroke.cap)
        drawLine(color = tint, start = Offset(4f * scale, 11f * scale), end = Offset(4f * scale, 13f * scale), strokeWidth = stroke.width, cap = stroke.cap)
    }
}

// Uiverse Send Arrow (from Uiverse snippet)
@Composable
fun UiverseSendUpArrowIcon(tint: Color = Color.White, size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // d="m5 12l7-7l7 7m-7 7V5"
        val path = Path().apply {
            moveTo(5f * scale, 12f * scale)
            lineTo(12f * scale, 5f * scale)
            lineTo(19f * scale, 12f * scale)
        }
        drawPath(path, color = tint, style = stroke)
        drawLine(color = tint, start = Offset(12f * scale, 19f * scale), end = Offset(12f * scale, 5f * scale), strokeWidth = stroke.width, cap = stroke.cap)
    }
}

// Uiverse Folder Upload Icon (from Uiverse snippet)
@Composable
fun UiverseFolderUploadIcon(tint: Color = Color(0xFFAAAAAA), size: Dp = 20.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(6f * scale, 14f * scale)
            lineTo(7.5f * scale, 11.1f * scale)
            cubicTo(7.9f * scale, 10.4f * scale, 8.5f * scale, 10f * scale, 9.24f * scale, 10f * scale)
            lineTo(20f * scale, 10f * scale)
            cubicTo(21.1f * scale, 10f * scale, 22f * scale, 10.9f * scale, 21.94f * scale, 12.5f * scale)
            lineTo(20.4f * scale, 18.5f * scale)
            cubicTo(20.1f * scale, 19.4f * scale, 19.3f * scale, 20f * scale, 18.45f * scale, 20f * scale)
            lineTo(4f * scale, 20f * scale)
            cubicTo(2.9f * scale, 20f * scale, 2f * scale, 19.1f * scale, 2f * scale, 18f * scale)
            lineTo(2f * scale, 5f * scale)
            cubicTo(2f * scale, 3.9f * scale, 2.9f * scale, 3f * scale, 4f * scale, 3f * scale)
            lineTo(7.9f * scale, 3f * scale)
            cubicTo(8.6f * scale, 3f * scale, 9.3f * scale, 3.4f * scale, 9.59f * scale, 3.9f * scale)
            lineTo(10.4f * scale, 5.1f * scale)
            cubicTo(10.8f * scale, 5.7f * scale, 11.4f * scale, 6f * scale, 12.07f * scale, 6f * scale)
            lineTo(18f * scale, 6f * scale)
            cubicTo(19.1f * scale, 6f * scale, 20f * scale, 6.9f * scale, 20f * scale, 8f * scale)
            lineTo(20f * scale, 10f * scale)
        }
        drawPath(path, color = tint, style = stroke)
    }
}

// Lucide Sparkles -> Replaced with new Purple Wave Sphere AI Icon
@Composable
fun LucideSparkles(tint: Color = Color(0xFF007AFF), size: Dp = 18.dp, modifier: Modifier = Modifier) {
    com.chroma.studio.ui.components.AiWaveSphereIcon(modifier = modifier.size(size))
}

// Lucide Sliders
@Composable
fun LucideSliders(tint: Color = Color(0xFF8E8E93), size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawLine(color = tint, start = Offset(4f * scale, 21f * scale), end = Offset(4f * scale, 14f * scale), strokeWidth = stroke.width, cap = stroke.cap)
        drawLine(color = tint, start = Offset(4f * scale, 10f * scale), end = Offset(4f * scale, 3f * scale), strokeWidth = stroke.width, cap = stroke.cap)
        drawLine(color = tint, start = Offset(12f * scale, 21f * scale), end = Offset(12f * scale, 12f * scale), strokeWidth = stroke.width, cap = stroke.cap)
        drawLine(color = tint, start = Offset(12f * scale, 8f * scale), end = Offset(12f * scale, 3f * scale), strokeWidth = stroke.width, cap = stroke.cap)
        drawLine(color = tint, start = Offset(20f * scale, 21f * scale), end = Offset(20f * scale, 16f * scale), strokeWidth = stroke.width, cap = stroke.cap)
        drawLine(color = tint, start = Offset(20f * scale, 12f * scale), end = Offset(20f * scale, 3f * scale), strokeWidth = stroke.width, cap = stroke.cap)
        drawCircle(color = tint, radius = 2f * scale, center = Offset(4f * scale, 12f * scale), style = stroke)
        drawCircle(color = tint, radius = 2f * scale, center = Offset(12f * scale, 10f * scale), style = stroke)
        drawCircle(color = tint, radius = 2f * scale, center = Offset(20f * scale, 14f * scale), style = stroke)
    }
}

// Lucide Chevron Right
@Composable
fun LucideChevronRight(tint: Color = Color(0xFF8E8E93), size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(9f * scale, 18f * scale)
            lineTo(15f * scale, 12f * scale)
            lineTo(9f * scale, 6f * scale)
        }
        drawPath(path, color = tint, style = stroke)
    }
}

// Phosphor Sun
@Composable
fun PhosphorSun(tint: Color = Color(0xFF007AFF), size: Dp = 22.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawCircle(color = tint, radius = 4.5f * scale, center = Offset(12f * scale, 12f * scale), style = stroke)
        val rayLen = 2.5f * scale
        val rays = listOf(
            Offset(12f * scale, 2f * scale) to Offset(12f * scale, 2f * scale + rayLen),
            Offset(12f * scale, 22f * scale) to Offset(12f * scale, 22f * scale - rayLen),
            Offset(2f * scale, 12f * scale) to Offset(2f * scale + rayLen, 12f * scale),
            Offset(22f * scale, 12f * scale) to Offset(22f * scale - rayLen, 12f * scale),
            Offset(4.93f * scale, 4.93f * scale) to Offset(4.93f * scale + 1.8f * scale, 4.93f * scale + 1.8f * scale),
            Offset(19.07f * scale, 19.07f * scale) to Offset(19.07f * scale - 1.8f * scale, 19.07f * scale - 1.8f * scale),
            Offset(4.93f * scale, 19.07f * scale) to Offset(4.93f * scale + 1.8f * scale, 19.07f * scale - 1.8f * scale),
            Offset(19.07f * scale, 4.93f * scale) to Offset(19.07f * scale - 1.8f * scale, 4.93f * scale + 1.8f * scale)
        )
        rays.forEach { (start, end) ->
            drawLine(color = tint, start = start, end = end, strokeWidth = stroke.width, cap = stroke.cap)
        }
    }
}

// Phosphor Moon
@Composable
fun PhosphorMoon(tint: Color = Color(0xFF007AFF), size: Dp = 22.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(21f * scale, 12.79f * scale)
            cubicTo(20.35f * scale, 17.4f * scale, 16.5f * scale, 21f * scale, 11.8f * scale, 21f * scale)
            cubicTo(6.39f * scale, 21f * scale, 2f * scale, 16.61f * scale, 2f * scale, 11.2f * scale)
            cubicTo(2f * scale, 6.5f * scale, 5.6f * scale, 2.65f * scale, 10.21f * scale, 2f * scale)
            cubicTo(9.44f * scale, 3.42f * scale, 9f * scale, 5.04f * scale, 9f * scale, 6.75f * scale)
            cubicTo(9f * scale, 12.16f * scale, 13.39f * scale, 16.55f * scale, 18.8f * scale, 16.55f * scale)
            cubicTo(19.58f * scale, 16.55f * scale, 20.32f * scale, 16.45f * scale, 21f * scale, 12.79f * scale)
            close()
        }
        drawPath(path, color = tint, style = stroke)
    }
}

// Phosphor Copy Icon
@Composable
fun PhosphorCopyIcon(tint: Color = Color(0xFF8E8E93), size: Dp = 16.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.35f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val bgPath = Path().apply {
            moveTo(8f * scale, 4f * scale)
            lineTo(19f * scale, 4f * scale)
            lineTo(19f * scale, 15f * scale)
        }
        drawPath(bgPath, color = tint, style = stroke)
        val fgPath = Path().apply {
            moveTo(5f * scale, 8f * scale)
            lineTo(15f * scale, 8f * scale)
            lineTo(15f * scale, 19f * scale)
            lineTo(5f * scale, 19f * scale)
            close()
        }
        drawPath(fgPath, color = tint, style = stroke)
    }
}

// Phosphor Waveform
@Composable
fun PhosphorWaveform(tint: Color = Color.White, size: Dp = 20.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val xCoords = listOf(4f, 8f, 12f, 16f, 20f)
        val heights = listOf(6f, 14f, 18f, 10f, 6f)
        xCoords.forEachIndexed { i, x ->
            val h = heights[i]
            val y1 = 12f - h / 2f
            val y2 = 12f + h / 2f
            drawLine(color = tint, start = Offset(x * scale, y1 * scale), end = Offset(x * scale, y2 * scale), strokeWidth = stroke.width, cap = stroke.cap)
        }
    }
}

// Phosphor Keypad Icon
@Composable
fun PhosphorKeypadIcon(tint: Color = Color.White, size: Dp = 24.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val r = 1.8f * scale
        for (row in 0..2) {
            for (col in 0..2) {
                drawCircle(
                    color = tint,
                    radius = r,
                    center = Offset((6f + col * 6f) * scale, (6f + row * 6f) * scale)
                )
            }
        }
    }
}

// Phosphor User Icon
@Composable
fun PhosphorUserIcon(tint: Color = Color.White, size: Dp = 24.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawCircle(color = tint, radius = 4.5f * scale, center = Offset(12f * scale, 8f * scale), style = stroke)
        val path = Path().apply {
            moveTo(4f * scale, 20f * scale)
            cubicTo(4f * scale, 16.5f * scale, 7.5f * scale, 15f * scale, 12f * scale, 15f * scale)
            cubicTo(16.5f * scale, 15f * scale, 20f * scale, 16.5f * scale, 20f * scale, 20f * scale)
        }
        drawPath(path, color = tint, style = stroke)
    }
}

// Phosphor Poll Icon
@Composable
fun PhosphorPollIcon(tint: Color = Color.White, size: Dp = 20.dp, modifier: Modifier = Modifier) {
    LucidePoll(tint = tint, size = size, modifier = modifier)
}

// Phosphor Sparkle / AI Icon -> Replaced with new Purple Wave Sphere AI Icon
@Composable
fun PhosphorSparkleIcon(tint: Color = Color.White, size: Dp = 16.dp, modifier: Modifier = Modifier) {
    com.chroma.studio.ui.components.AiWaveSphereIcon(modifier = modifier.size(size))
}

@Composable
fun LucidePhoneIncoming(tint: Color = Color.White, size: Dp = 16.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Incoming Arrow: (16,2) -> (22,8), (22,2) -> (22,8) -> (16,8)
        val arrowPath = Path().apply {
            moveTo(16f * scale, 2f * scale)
            lineTo(22f * scale, 8f * scale)
            moveTo(16f * scale, 8f * scale)
            lineTo(22f * scale, 8f * scale)
            lineTo(22f * scale, 2f * scale)
        }
        drawPath(arrowPath, color = tint, style = stroke)
        // Phone receiver
        val phonePath = Path().apply {
            moveTo(22f * scale, 16.92f * scale)
            cubicTo(22f * scale, 17.5f * scale, 21.6f * scale, 18.06f * scale, 21.05f * scale, 18.33f * scale)
            lineTo(17.89f * scale, 19.91f * scale)
            cubicTo(17.29f * scale, 20.21f * scale, 16.57f * scale, 20.06f * scale, 16.14f * scale, 19.55f * scale)
            lineTo(14.5f * scale, 17.55f * scale)
            cubicTo(12.7f * scale, 18.5f * scale, 11.5f * scale, 17.3f * scale, 10.5f * scale, 15.5f * scale)
            lineTo(8.5f * scale, 13.86f * scale)
            cubicTo(8f * scale, 13.43f * scale, 7.8f * scale, 12.71f * scale, 8.1f * scale, 12.11f * scale)
            lineTo(9.67f * scale, 8.95f * scale)
            cubicTo(9.94f * scale, 8.4f * scale, 10.5f * scale, 8f * scale, 11.08f * scale, 8f * scale)
            lineTo(13f * scale, 8f * scale)
        }
        drawPath(phonePath, color = tint, style = stroke)
    }
}

@Composable
fun LucideImage(tint: Color = Color.White, size: Dp = 18.dp, modifier: Modifier = Modifier) {
    PhosphorPhotoIcon(tint = tint, size = size, modifier = modifier)
}

@Composable
fun LucideMessageSquare(tint: Color = Color.White, size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(21f * scale, 15f * scale)
            cubicTo(21f * scale, 16.1f * scale, 20.1f * scale, 17f * scale, 19f * scale, 17f * scale)
            lineTo(7f * scale, 17f * scale)
            lineTo(3f * scale, 21f * scale)
            lineTo(3f * scale, 5f * scale)
            cubicTo(3f * scale, 3.9f * scale, 3.9f * scale, 3f * scale, 5f * scale, 3f * scale)
            lineTo(19f * scale, 3f * scale)
            cubicTo(20.1f * scale, 3.9f * scale, 21f * scale, 3.9f * scale, 21f * scale, 5f * scale)
            close()
        }
        drawPath(path, color = tint, style = stroke)
    }
}

@Composable
fun LucideSmartphone(tint: Color = Color.White, size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawRoundRect(
            color = tint,
            topLeft = Offset(5f * scale, 2f * scale),
            size = Size(14f * scale, 20f * scale),
            cornerRadius = CornerRadius(3f * scale, 3f * scale),
            style = stroke
        )
        drawLine(
            color = tint,
            start = Offset(11f * scale, 18f * scale),
            end = Offset(13f * scale, 18f * scale),
            strokeWidth = 1.25f * density,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun LucideUserX(tint: Color = Color.White, size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Head circle
        drawCircle(color = tint, radius = 4f * scale, center = Offset(9f * scale, 7f * scale), style = stroke)
        // Body path
        val bodyPath = Path().apply {
            moveTo(16f * scale, 21f * scale)
            cubicTo(16f * scale, 17.5f * scale, 13f * scale, 15f * scale, 9f * scale, 15f * scale)
            cubicTo(5f * scale, 15f * scale, 2f * scale, 17.5f * scale, 2f * scale, 21f * scale)
        }
        drawPath(bodyPath, color = tint, style = stroke)
        // Cross lines: (17,8)->(22,13), (22,8)->(17,13)
        drawLine(tint, Offset(17f * scale, 8f * scale), Offset(22f * scale, 13f * scale), strokeWidth = 1.25f * density, cap = StrokeCap.Round)
        drawLine(tint, Offset(22f * scale, 8f * scale), Offset(17f * scale, 13f * scale), strokeWidth = 1.25f * density, cap = StrokeCap.Round)
    }
}

@Composable
fun LucideShieldCheck(tint: Color = Color.White, size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Shield path: d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"
        val shieldPath = Path().apply {
            moveTo(12f * scale, 22f * scale)
            cubicTo(12f * scale, 22f * scale, 20f * scale, 18f * scale, 20f * scale, 12f * scale)
            lineTo(20f * scale, 5f * scale)
            lineTo(12f * scale, 2f * scale)
            lineTo(4f * scale, 5f * scale)
            lineTo(4f * scale, 12f * scale)
            cubicTo(4f * scale, 18f * scale, 12f * scale, 22f * scale, 12f * scale, 22f * scale)
            close()
        }
        drawPath(shieldPath, color = tint, style = stroke)
        // Checkmark inside shield: (9,12) -> (11,14) -> (15,10)
        val checkPath = Path().apply {
            moveTo(9f * scale, 12f * scale)
            lineTo(11f * scale, 14f * scale)
            lineTo(15f * scale, 10f * scale)
        }
        drawPath(checkPath, color = tint, style = stroke)
    }
}

@Composable
fun LucideArchive(tint: Color = Color(0xFF8E8E93), size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawRoundRect(
            color = tint,
            topLeft = Offset(2f * scale, 3f * scale),
            size = Size(20f * scale, 4f * scale),
            cornerRadius = CornerRadius(1.5f * scale, 1.5f * scale),
            style = stroke
        )
        val path = Path().apply {
            moveTo(4f * scale, 7f * scale)
            lineTo(4f * scale, 20f * scale)
            cubicTo(4f * scale, 21f * scale, 5f * scale, 21f * scale, 6f * scale, 21f * scale)
            lineTo(18f * scale, 21f * scale)
            cubicTo(19f * scale, 21f * scale, 20f * scale, 21f * scale, 20f * scale, 20f * scale)
            lineTo(20f * scale, 7f * scale)
        }
        drawPath(path, color = tint, style = stroke)
        drawLine(tint, Offset(10f * scale, 12f * scale), Offset(14f * scale, 12f * scale), strokeWidth = 1.25f * density, cap = StrokeCap.Round)
    }
}

@Composable
fun PhosphorArchiveIcon(tint: Color = Color(0xFF8E8E93), size: Dp = 18.dp, modifier: Modifier = Modifier) {
    LucideArchive(tint = tint, size = size, modifier = modifier)
}

@Composable
fun LucidePin(tint: Color = Color(0xFF8E8E93), size: Dp = 18.dp, modifier: Modifier = Modifier) {
    PhosphorPushPin(tint = tint, size = size, modifier = modifier)
}

@Composable
fun LucideForward(tint: Color = Color(0xFF8E8E93), size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(15f * scale, 17f * scale)
            lineTo(20f * scale, 12f * scale)
            lineTo(15f * scale, 7f * scale)
        }
        drawPath(path, color = tint, style = stroke)
        val curvePath = Path().apply {
            moveTo(4f * scale, 18f * scale)
            cubicTo(4f * scale, 12f * scale, 9f * scale, 12f * scale, 20f * scale, 12f * scale)
        }
        drawPath(curvePath, color = tint, style = stroke)
    }
}

@Composable
fun LucideBarChart2(tint: Color = Color(0xFF8E8E93), size: Dp = 18.dp, modifier: Modifier = Modifier) {
    LucidePoll(tint = tint, size = size, modifier = modifier)
}

@Composable
fun LucideShare2(tint: Color = Color(0xFF007AFF), size: Dp = 18.dp, modifier: Modifier = Modifier) {
    LucideShare(tint = tint, size = size, modifier = modifier)
}

@Composable
fun LucideLink(tint: Color = Color(0xFF007AFF), size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 24f
        val stroke = Stroke(width = 1.25f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Left loop
        val p1 = Path().apply {
            moveTo(10f * scale, 13f * scale)
            cubicTo(10f * scale, 15.2f * scale, 8.2f * scale, 17f * scale, 6f * scale, 17f * scale)
            cubicTo(3.8f * scale, 17f * scale, 2f * scale, 15.2f * scale, 2f * scale, 13f * scale)
            cubicTo(2f * scale, 10.8f * scale, 3.8f * scale, 9f * scale, 6f * scale, 9f * scale)
            lineTo(8f * scale, 9f * scale)
        }
        drawPath(p1, color = tint, style = stroke)
        // Right loop
        val p2 = Path().apply {
            moveTo(14f * scale, 11f * scale)
            cubicTo(14f * scale, 8.8f * scale, 15.8f * scale, 7f * scale, 18f * scale, 7f * scale)
            cubicTo(20.2f * scale, 7f * scale, 22f * scale, 8.8f * scale, 22f * scale, 11f * scale)
            cubicTo(22f * scale, 13.2f * scale, 20.2f * scale, 15f * scale, 18f * scale, 15f * scale)
            lineTo(16f * scale, 15f * scale)
        }
        drawPath(p2, color = tint, style = stroke)
        // Connecting line
        drawLine(color = tint, start = androidx.compose.ui.geometry.Offset(8f * scale, 12f * scale), end = androidx.compose.ui.geometry.Offset(16f * scale, 12f * scale), strokeWidth = 1.25f * density, cap = StrokeCap.Round)
    }
}




