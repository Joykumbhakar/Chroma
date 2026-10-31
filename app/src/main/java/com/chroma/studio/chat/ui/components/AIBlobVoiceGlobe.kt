package com.chroma.studio.chat.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.*

// ============================================================================
// CHROMA BLOB — Native Compose port of React Bits AIBlob
// A glowing glass orb that morphs and breathes with voice amplitude.
// Colors: #ff50a1 (hot pink), #ff6700 (orange), #ff00a7 (magenta), #004dff (deep blue)
// ============================================================================

data class AIBlobParams(
    val colors: List<Color> = DEFAULT_AIBLOB_COLORS,
    val noiseScale: Float = 2.4f,     // how much the blob outline is distorted
    val innerScale: Float = 1.68f,    // inner glow orb scale relative to base radius
    val rimWidth: Float = 1.75f,      // rim stroke width multiplier
    val rimIntensity: Float = 1.12f,  // rim brightness
    val halo: Float = 1.4f            // outer halo radius multiplier
)

val DEFAULT_AIBLOB_COLORS = listOf(
    Color(0xFFFF50A1),  // hot pink
    Color(0xFFFF6700),  // orange
    Color(0xFFFF00A7),  // magenta
    Color(0xFF004DFF)   // deep blue
)

@Composable
fun AIBlobVoiceGlobe(
    level: Float? = null,
    listening: Boolean = false,
    modifier: Modifier = Modifier,
    params: AIBlobParams = AIBlobParams(),
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aiBlob")

    // Primary rotation — drives the blob warp animation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rot"
    )

    // Secondary slow rotation for inner orb
    val innerRot by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(7500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "innerRot"
    )

    // Breath / pulse scale
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    // Halo pulse
    val haloPulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo"
    )

    // Voice-reactive energy: goes up when listening/level is high
    val targetEnergy = when {
        level != null -> 0.4f + level * 0.8f
        listening -> 0.7f
        else -> 0.0f
    }
    val energy by animateFloatAsState(
        targetValue = targetEnergy,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "energy"
    )

    // Noise warp phase driven by time + energy
    val warpPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "warp"
    )

    Canvas(modifier = modifier.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null
    ) { onClick() }) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val baseR = minOf(cx, cy) * 0.44f

        val effectiveScale = breathScale * (1f + energy * 0.12f)
        val blobR = baseR * effectiveScale

        // --- 1. Outer HALO glow ---
        val haloR = blobR * params.halo * (1f + energy * 0.15f)
        drawHalo(cx, cy, haloR, haloPulse, params.colors, energy)

        // --- 2. Blob shape (noise-warped polygon) ---
        val blobPath = buildBlobPath(
            cx = cx, cy = cy,
            baseRadius = blobR,
            noiseScale = params.noiseScale,
            warpPhase = warpPhase,
            energy = energy,
            points = 64
        )

        // Blob gradient fill — radial from center, using colors
        val blobBrush = Brush.radialGradient(
            colorStops = arrayOf(
                0.0f to params.colors[2].copy(alpha = 0.95f),
                0.4f to params.colors[0].copy(alpha = 0.85f),
                0.75f to params.colors[3].copy(alpha = 0.9f),
                1.0f to Color.Black
            ),
            center = Offset(cx, cy),
            radius = blobR
        )
        drawPath(blobPath, blobBrush, style = Fill)

        // --- 3. Inner glow orb ---
        val innerR = blobR * (params.innerScale / 3f)
        val innerOffsetX = cos(Math.toRadians(innerRot.toDouble())).toFloat() * baseR * 0.05f
        val innerOffsetY = sin(Math.toRadians(innerRot.toDouble())).toFloat() * baseR * 0.05f
        val innerBrush = Brush.radialGradient(
            colorStops = arrayOf(
                0.0f to Color.White.copy(alpha = 0.92f),
                0.3f to params.colors[0].copy(alpha = 0.8f),
                0.65f to params.colors[1].copy(alpha = 0.6f),
                1.0f to Color.Transparent
            ),
            center = Offset(cx + innerOffsetX, cy + innerOffsetY),
            radius = innerR
        )
        drawPath(blobPath, innerBrush, style = Fill)

        // --- 4. Rim lighting ---
        val rimStrokeWidth = baseR * 0.045f * params.rimWidth
        val rimAlpha = (params.rimIntensity * 0.55f).coerceIn(0f, 1f)
        val rimBrush = Brush.sweepGradient(
            colorStops = arrayOf(
                0.0f to params.colors[0].copy(alpha = rimAlpha),
                0.25f to params.colors[1].copy(alpha = rimAlpha * 0.7f),
                0.5f to params.colors[2].copy(alpha = rimAlpha),
                0.75f to params.colors[3].copy(alpha = rimAlpha * 0.8f),
                1.0f to params.colors[0].copy(alpha = rimAlpha)
            ),
            center = Offset(cx, cy)
        )
        drawPath(blobPath, rimBrush, style = Stroke(width = rimStrokeWidth, cap = StrokeCap.Round))

        // --- 5. Specular highlight (top-left glass shine) ---
        val shineR = blobR * 0.28f
        val shineOx = -blobR * 0.28f
        val shineOy = -blobR * 0.32f
        val shineBrush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.55f),
                Color.White.copy(alpha = 0.0f)
            ),
            center = Offset(cx + shineOx, cy + shineOy),
            radius = shineR
        )
        drawPath(blobPath, shineBrush, style = Fill)

        // --- 6. Voice energy ring bursts ---
        if (energy > 0.15f) {
            drawVoiceRings(cx, cy, blobR, energy, warpPhase, params.colors)
        }
    }
}

// ============================================================================
// HELPERS
// ============================================================================

/** Smooth pseudorandom noise: two sin waves combined, deterministic per (angle, seed). */
private fun smoothNoise(angle: Float, phase: Float, seed: Float): Float {
    return (sin(angle * 3.1f + phase + seed) * 0.5f +
            sin(angle * 5.7f - phase * 1.3f + seed * 2.1f) * 0.3f +
            sin(angle * 1.9f + phase * 0.7f) * 0.2f)
}

/** Build the warped blob path around (cx,cy) with baseRadius distorted by noise. */
private fun buildBlobPath(
    cx: Float, cy: Float,
    baseRadius: Float,
    noiseScale: Float,
    warpPhase: Float,
    energy: Float,
    points: Int = 64
): Path {
    val path = Path()
    val angleStep = 2f * PI.toFloat() / points
    val warpAmount = baseRadius * 0.14f * noiseScale * (1f + energy * 0.6f)

    val pts = Array(points) { i ->
        val angle = i * angleStep
        val noise = smoothNoise(angle, warpPhase, 1.23f)
        val r = baseRadius + noise * warpAmount
        Offset(cx + r * cos(angle), cy + r * sin(angle))
    }

    // Catmull-Rom smooth path through points
    path.moveTo(pts[0].x, pts[0].y)
    for (i in pts.indices) {
        val p0 = pts[(i - 1 + points) % points]
        val p1 = pts[i]
        val p2 = pts[(i + 1) % points]
        val p3 = pts[(i + 2) % points]

        val cp1x = p1.x + (p2.x - p0.x) / 6f
        val cp1y = p1.y + (p2.y - p0.y) / 6f
        val cp2x = p2.x - (p3.x - p1.x) / 6f
        val cp2y = p2.y - (p3.y - p1.y) / 6f

        path.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
    }
    path.close()
    return path
}

/** Multi-layer radial halo glow. */
private fun DrawScope.drawHalo(
    cx: Float, cy: Float,
    haloR: Float,
    haloPulse: Float,
    colors: List<Color>,
    energy: Float
) {
    // Three layered halo rings
    listOf(
        Triple(haloR, 0.22f * haloPulse, colors[3]),
        Triple(haloR * 0.78f, 0.30f * haloPulse, colors[2]),
        Triple(haloR * 0.55f, 0.18f * haloPulse, colors[0])
    ).forEach { (r, alpha, color) ->
        val haloBrush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = alpha * (1f + energy * 0.4f)),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = r
        )
        drawCircle(brush = haloBrush, radius = r, center = Offset(cx, cy))
    }
}

/** Expanding pulse rings triggered by voice energy. */
private fun DrawScope.drawVoiceRings(
    cx: Float, cy: Float,
    blobR: Float,
    energy: Float,
    phase: Float,
    colors: List<Color>
) {
    repeat(2) { i ->
        val ringPhase = (phase + i * PI.toFloat()) % (2f * PI.toFloat())
        val ringProgress = (sin(ringPhase) * 0.5f + 0.5f)
        val ringR = blobR * (1.05f + ringProgress * 0.35f * energy)
        val ringAlpha = energy * (1f - ringProgress) * 0.4f
        val ringColor = colors[(i + 2) % colors.size]
        drawCircle(
            color = ringColor.copy(alpha = ringAlpha),
            radius = ringR,
            center = Offset(cx, cy),
            style = Stroke(width = blobR * 0.02f)
        )
    }
}
