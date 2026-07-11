package com.chroma.studio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.chroma.studio.ui.theme.LocalChromaColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import com.chroma.studio.ui.theme.ChromaPalette

fun Modifier.glossyBorder(
    shape: Shape,
    colors: ChromaPalette,
    width: androidx.compose.ui.unit.Dp = 1.dp
): Modifier = this.border(
    width = width,
    brush = Brush.linearGradient(
        colors = listOf(
            colors.glassBorder.copy(alpha = (colors.glassBorder.alpha * 3f).coerceAtMost(1f)),
            colors.glassBorder.copy(alpha = (colors.glassBorder.alpha * 0.2f).coerceAtMost(1f)),
            colors.glassBorder.copy(alpha = (colors.glassBorder.alpha * 1.5f).coerceAtMost(1f))
        )
    ),
    shape = shape
)

/**
 * Direct port of:
 *   .glass-panel {
 *     background: var(--glass-bg); backdrop-filter: var(--glass-blur) /16px/;
 *     border: 1px solid var(--glass-border); box-shadow: var(--glass-shadow);
 *   }
 *
 * `hazeSource` should wrap whatever sits BEHIND the glass (e.g. the grid background /
 * canvas) with Modifier.haze(hazeState); every GlassPanel below it then uses
 * Modifier.hazeChild(hazeState) to get the real-time blur-behind effect, matching
 * backdrop-filter: blur(16px) from the CSS.
 */
val LocalHazeState = androidx.compose.runtime.compositionLocalOf { HazeState() }

@Composable
fun HazeSourceRoot(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val hazeState = LocalHazeState.current
    Box(modifier = modifier.haze(hazeState)) { content() }
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 16, // --radius-md
    content: @Composable () -> Unit
) {
    val colors = LocalChromaColors.current
    val hazeState = LocalHazeState.current
    val shape = RoundedCornerShape(cornerRadius.dp)

    Box(
        modifier = modifier
            .shadow(elevation = 8.dp, shape = shape, ambientColor = Color(0x0D1F2687), spotColor = Color(0x0D1F2687))
            .clip(shape)
            .hazeChild(state = hazeState, shape = shape) // backdrop-filter: blur(16px)
            .background(colors.glassBg, shape) // fallback tint if blur unsupported below API 31
            .glossyBorder(shape, colors)
    ) {
        content()
    }
}

/** .btn-icon — 36x36 circular icon button background, hover state handled by caller */
@Composable
fun glassCircleShape() = RoundedCornerShape(50)
