package com.chroma.studio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.unit.dp
import kotlin.math.abs

val IosBadgeGradients = listOf(
    listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)), // Standard Blue
    listOf(Color(0xFFFBBF24), Color(0xFFB45309)), // Gold Premium
    listOf(Color(0xFF9CA3AF), Color(0xFF4B5563)), // Silver Executive
    listOf(Color(0xFFD97706), Color(0xFF78350F)), // Bronze Pioneer
    listOf(Color(0xFF10B981), Color(0xFF047857)), // Emerald Official
    listOf(Color(0xFFF43F5E), Color(0xFF9F1239)), // Ruby Elite
    listOf(Color(0xFFA855F7), Color(0xFF6B21A8)), // Amethyst Trust
    listOf(Color(0xFF334155), Color(0xFF0F172A)), // Midnight Core
    listOf(Color(0xFF64748B), Color(0xFF334155)), // Space Gray Auth
    listOf(Color(0xFF1E3A8A), Color(0xFF172554)), // Navy Seal
    listOf(Color(0xFF14B8A6), Color(0xFF0F766E)), // Teal Authentic
    listOf(Color(0xFFE11D48), Color(0xFF881337)), // Crimson Crown
    listOf(Color(0xFF475569), Color(0xFF1E293B)), // Slate Authority
    listOf(Color(0xFF7E22CE), Color(0xFF4C1D95)), // Deep Purple VIP
    listOf(Color(0xFF0EA5E9), Color(0xFF0369A1)), // Ocean Sentinel
    listOf(Color(0xFF22C55E), Color(0xFF15803D)), // Forest Secure
    listOf(Color(0xFF06B6D4), Color(0xFF0E7490)), // Corporate Cyan
    listOf(Color(0xFF3F3F46), Color(0xFF18181B)), // Charcoal Stealth
    listOf(Color(0xFFFDA4AF), Color(0xFFBE123C)), // Rose Gold
    listOf(Color(0xFF4F46E5), Color(0xFF312E81))  // Sapphire Verified
)

@Composable
fun IosVerificationBadge(
    modifier: Modifier = Modifier,
    userId: String? = null
) {
    val gradient = remember(userId) {
        if (userId.isNullOrBlank()) IosBadgeGradients[0]
        else IosBadgeGradients[abs(userId.hashCode()) % IosBadgeGradients.size]
    }

    val sealPathStr = "M22.5 12.5c0-1.58-.875-2.95-2.148-3.6.154-.435.238-.905.238-1.4 0-2.21-1.71-3.998-3.918-3.998-.47 0-.92.084-1.336.25C14.818 2.415 13.51 1.5 12 1.5s-2.816.917-3.337 2.25c-.416-.165-.866-.25-1.336-.25-2.21 0-3.918 1.79-3.918 4 0 .495.084.965.238 1.4-1.273.65-2.148 2.02-2.148 3.6 0 1.46.74 2.746 1.865 3.45-.06.31-.09.63-.09.95 0 2.21 1.71 4 3.918 4 .47 0 .92-.085 1.336-.25.52 1.335 1.826 2.25 3.337 2.25s2.816-.915 3.337-2.25c.416.165.866.25 1.336.25 2.21 0 3.918-1.79 3.918-4 0-.32-.03-.64-.09-.95 1.125-.705 1.865-1.99 1.865-3.45z"
    val checkPathStr = "M 9.25 17.0 l -4.25 -4.25 l 1.75 -1.75 l 2.5 2.5 l 7.5 -7.5 l 1.75 1.75 z"

    val sealPath = remember { PathParser().parsePathString(sealPathStr).toPath() }
    val checkPath = remember { PathParser().parsePathString(checkPathStr).toPath() }

    Canvas(modifier = modifier.size(16.dp)) {
        val scale = size.minDimension / 24f
        translate(left = 0f, top = 0f) {
            withTransform({
                scale(scale, scale, pivot = androidx.compose.ui.geometry.Offset.Zero)
            }) {
                drawIntoCanvas { canvas ->
                    // Seal drop shadow
                    val sealShadowPaint = androidx.compose.ui.graphics.Paint().apply {
                        color = Color.Black.copy(alpha = 0.3f)
                    }
                    sealShadowPaint.asFrameworkPaint().maskFilter = android.graphics.BlurMaskFilter(1f, android.graphics.BlurMaskFilter.Blur.NORMAL)
                    
                    canvas.nativeCanvas.save()
                    canvas.nativeCanvas.translate(0f, 0.5f)
                    canvas.nativeCanvas.drawPath(sealPath.asAndroidPath(), sealShadowPaint.asFrameworkPaint())
                    canvas.nativeCanvas.restore()

                    // Seal body
                    drawPath(
                        path = sealPath,
                        brush = Brush.verticalGradient(gradient),
                        style = Fill
                    )
                    
                    // Bevel border stroke
                    drawPath(
                        path = sealPath,
                        brush = Brush.linearGradient(
                            colors = listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.1f), Color.Black.copy(alpha = 0.3f))
                        ),
                        style = Stroke(width = 0.5f)
                    )

                    // Checkmark shadow
                    val checkShadowPaint = androidx.compose.ui.graphics.Paint().apply {
                        color = Color.Black.copy(alpha = 0.2f)
                    }
                    checkShadowPaint.asFrameworkPaint().maskFilter = android.graphics.BlurMaskFilter(0.4f.coerceAtLeast(1f), android.graphics.BlurMaskFilter.Blur.NORMAL) // Android BlurMaskFilter needs radius > 0

                    canvas.nativeCanvas.save()
                    canvas.nativeCanvas.translate(0f, 0.3f)
                    canvas.nativeCanvas.drawPath(checkPath.asAndroidPath(), checkShadowPaint.asFrameworkPaint())
                    canvas.nativeCanvas.restore()

                    // Checkmark
                    drawPath(
                        path = checkPath,
                        color = Color.White,
                        style = Fill
                    )
                }
            }
        }
    }
}
