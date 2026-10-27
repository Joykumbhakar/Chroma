package com.chroma.studio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Official Signature Verified Blue (Apple Cupertino / Instagram)
val VerifiedBadgeBlueGradient = listOf(
    Color(0xFF007AFF),
    Color(0xFF0062E0)
)

val IosBadgeGradients = listOf(
    VerifiedBadgeBlueGradient
)

private const val SEAL_PATH_STRING =
    "M22.5 12.5c0-1.58-.875-2.95-2.148-3.6.154-.435.238-.905.238-1.4 0-2.21-1.71-3.998-3.918-3.998-.47 0-.92.084-1.336.25C14.818 2.415 13.51 1.5 12 1.5s-2.816.917-3.337 2.25c-.416-.165-.866-.25-1.336-.25-2.21 0-3.918 1.79-3.918 4 0 .495.084.965.238 1.4-1.273.65-2.148 2.02-2.148 3.6 0 1.46.74 2.746 1.865 3.45-.06.31-.09.63-.09.95 0 2.21 1.71 4 3.918 4 .47 0 .92-.085 1.336-.25.52 1.335 1.826 2.25 3.337 2.25s2.816-.915 3.337-2.25c.416.165.866.25 1.336.25 2.21 0 3.918-1.79 3.918-4 0-.32-.03-.64-.09-.95 1.125-.705 1.865-1.99 1.865-3.45z"

private const val CHECK_PATH_STRING =
    "M 9.25 17.0 l -4.25 -4.25 l 1.75 -1.75 l 2.5 2.5 l 7.5 -7.5 l 1.75 1.75 z"

/**
 * Common Canonical Verified Badge used across all screens in ChromaStudio.
 * Renders the iconic scalloped rosette seal in signature verified blue with a crisp white checkmark.
 */
@Composable
fun VerifiedBadge(
    modifier: Modifier = Modifier,
    size: Dp = 14.dp,
    badgeColor: Color? = null,
    userId: String? = null
) {
    val sealPath = remember { PathParser().parsePathString(SEAL_PATH_STRING).toPath() }
    val checkPath = remember { PathParser().parsePathString(CHECK_PATH_STRING).toPath() }

    Canvas(modifier = modifier.size(size)) {
        val scale = this.size.width / 24f
        translate(left = 0f, top = 0f) {
            withTransform({
                scale(scale, scale, pivot = androidx.compose.ui.geometry.Offset.Zero)
            }) {
                drawIntoCanvas { canvas ->
                    // Subtle seal drop shadow for depth
                    val sealShadowPaint = androidx.compose.ui.graphics.Paint().apply {
                        color = Color.Black.copy(alpha = 0.25f)
                    }
                    sealShadowPaint.asFrameworkPaint().maskFilter =
                        android.graphics.BlurMaskFilter(1f, android.graphics.BlurMaskFilter.Blur.NORMAL)

                    canvas.nativeCanvas.save()
                    canvas.nativeCanvas.translate(0f, 0.5f)
                    canvas.nativeCanvas.drawPath(sealPath.asAndroidPath(), sealShadowPaint.asFrameworkPaint())
                    canvas.nativeCanvas.restore()

                    // Seal body: signature verified gradient or custom color
                    if (badgeColor != null) {
                        drawPath(
                            path = sealPath,
                            color = badgeColor,
                            style = Fill
                        )
                    } else {
                        drawPath(
                            path = sealPath,
                            brush = Brush.verticalGradient(VerifiedBadgeBlueGradient),
                            style = Fill
                        )
                    }

                    // Subtle Apple-style top highlight / bevel
                    drawPath(
                        path = sealPath,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.45f),
                                Color.White.copy(alpha = 0.05f),
                                Color.Black.copy(alpha = 0.15f)
                            )
                        ),
                        style = Stroke(width = 0.5f)
                    )

                    // Checkmark shadow
                    val checkShadowPaint = androidx.compose.ui.graphics.Paint().apply {
                        color = Color.Black.copy(alpha = 0.15f)
                    }
                    checkShadowPaint.asFrameworkPaint().maskFilter =
                        android.graphics.BlurMaskFilter(1f, android.graphics.BlurMaskFilter.Blur.NORMAL)

                    canvas.nativeCanvas.save()
                    canvas.nativeCanvas.translate(0f, 0.3f)
                    canvas.nativeCanvas.drawPath(checkPath.asAndroidPath(), checkShadowPaint.asFrameworkPaint())
                    canvas.nativeCanvas.restore()

                    // Crisp white checkmark
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

/**
 * Backward compatibility alias for IosVerificationBadge.
 */
@Composable
fun IosVerificationBadge(
    modifier: Modifier = Modifier,
    userId: String? = null,
    size: Dp = 14.dp
) {
    VerifiedBadge(
        modifier = modifier,
        size = size,
        userId = userId
    )
}

/**
 * Backward compatibility alias for ScallopedVerifiedBadge.
 */
@Composable
fun ScallopedVerifiedBadge(
    modifier: Modifier = Modifier,
    size: Dp = 14.dp,
    badgeColor: Color = Color(0xFF007AFF)
) {
    VerifiedBadge(
        modifier = modifier,
        size = size,
        badgeColor = badgeColor
    )
}
