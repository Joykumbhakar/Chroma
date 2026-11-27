package com.chroma.studio.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*
import kotlin.math.*

// ============================================================================
// STUDIO DESIGN SYSTEM: APPLE HIG LIGHT & DARK ADAPTIVE SURFACES
// ============================================================================

object StudioDesignTokens {
    // Deep Studio Solid Foundations (Lightweight, Dark Room Grade)
    val Obsidian = Color(0xFF0B0D13)
    val SurfaceDark = Color(0xFF12151F)
    val CardBg = Color(0xFF171A27)
    val ElevatedBg = Color(0xFF1E2232)
    val TrackBg = Color(0xFF0E1018)

    // Bevel Edge Specular Highlights & Inset Shades
    val BevelHighlightTop = Color.White.copy(alpha = 0.22f)
    val BevelShadeBottom = Color.Black.copy(alpha = 0.40f)
    val BevelBorderDefault = Color.White.copy(alpha = 0.08f)
    val BevelBorderActive = Color(0xFF0381FE).copy(alpha = 0.55f)

    // Unified One UI Studio Accents
    val CobaltGradient = listOf(Color(0xFF0381FE), Color(0xFF0072DE))
    val AmberGoldGradient = listOf(Color(0xFFD97706), Color(0xFFF59E0B))
    val EmeraldMintGradient = listOf(Color(0xFF0381FE), Color(0xFF3E91FF))
    val CrimsonFlareGradient = listOf(Color(0xFFEF4444), Color(0xFFDC2626))
    val ElectricVioletGradient = listOf(Color(0xFF0381FE), Color(0xFF0072DE))
    val CyberNeonGradient = listOf(Color(0xFF0381FE), Color(0xFF0072DE))
    val TitaniumGradient = listOf(Color(0xFF333848), Color(0xFF222634))
}

/**
 * Reusable Compose modifier that adds a crisp, micro-machined physical bevel edge.
 * Uses a vertical linear gradient highlight on the border and subtle depth shadow.
 */
fun Modifier.studioBevel(
    cornerRadius: Dp = 12.dp,
    isPressed: Boolean = false,
    borderWidth: Dp = 1.dp,
    accentBorder: Brush? = null
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .border(
        width = borderWidth,
        brush = accentBorder ?: Brush.verticalGradient(
            listOf(
                if (isPressed) StudioDesignTokens.BevelHighlightTop.copy(alpha = 0.08f) else StudioDesignTokens.BevelHighlightTop,
                if (isPressed) StudioDesignTokens.BevelShadeBottom.copy(alpha = 0.60f) else StudioDesignTokens.BevelBorderDefault
            )
        ),
        shape = RoundedCornerShape(cornerRadius)
    )

/**
 * Lightweight Tactile Machined Studio Button with Beveled Edge
 */
@Composable
fun StudioBeveledButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    text: String? = null,
    accentColors: List<Color>? = null,
    isActive: Boolean = false,
    isDanger: Boolean = false,
    cornerRadius: Dp = 10.dp,
    height: Dp = 34.dp
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val bgBrush = remember(isPressed, isActive, accentColors, isDanger) {
        when {
            isDanger -> Brush.verticalGradient(listOf(Color(0xFFDC2626), Color(0xFF991B1B)))
            accentColors != null && (isActive || isPressed) -> Brush.verticalGradient(accentColors)
            isActive -> Brush.verticalGradient(StudioDesignTokens.CobaltGradient)
            isPressed -> Brush.verticalGradient(listOf(Color(0xFF181B26), Color(0xFF10121A)))
            else -> Brush.verticalGradient(StudioDesignTokens.TitaniumGradient)
        }
    }

    Box(
        modifier = modifier
            .height(height)
            .studioBevel(
                cornerRadius = cornerRadius,
                isPressed = isPressed,
                borderWidth = if (isActive) 1.5.dp else 1.dp,
                accentBorder = if (isActive && accentColors != null) Brush.verticalGradient(accentColors) else null
            )
            .background(bgBrush, RoundedCornerShape(cornerRadius))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = if (isActive || isDanger) Color.White else Color(0xFFE2E8F0),
                    modifier = Modifier.size(15.dp)
                )
            }
            if (!text.isNullOrBlank()) {
                Text(
                    text = text,
                    fontSize = 11.5.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isActive || isDanger) Color.White else Color(0xFFE2E8F0)
                )
            }
        }
    }
}

// ============================================================================
// PILLAR 1: LIVE AUDIO WAVEFORM VISUALIZER & DUAL-CHANNEL STEREO VU METER
// ============================================================================

/**
 * Live Interactive Audio Waveform Visualizer.
 * Displays 44 dynamic amplitude bars with gradient lighting, interactive seek playhead,
 * and realistic audio pulse dynamics during playback.
 */
@Composable
fun FcpAudioWaveformVisualizer(
    audioProgressFrac: Float,
    isPlaying: Boolean,
    durationSec: Float,
    trimStartFrac: Float = 0f,
    trimEndFrac: Float = 1f,
    onSeekProgress: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalOscellionColors.current
    val haptic = LocalHapticFeedback.current
    val barCount = 44

    // Infinite wave pulse for realistic live animation during playback
    val infiniteTransition = rememberInfiniteTransition(label = "WaveformPulse")
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulsePhase"
    )

    // Deterministic procedural baseline heights for clean studio waveform look
    val baselineBars = remember {
        FloatArray(barCount) { i ->
            val x = i.toFloat() / barCount
            val wave1 = sin(x * 12.0f) * 0.35f
            val wave2 = cos(x * 24.0f) * 0.25f
            val envelope = sin(x * Math.PI.toFloat()).pow(0.7f)
            ((0.25f + abs(wave1 + wave2)) * envelope).coerceIn(0.12f, 0.95f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.inputBg, RoundedCornerShape(10.dp))
            .border(0.5.dp, colors.cardBorder, RoundedCornerShape(10.dp))
            .pointerInput(barCount) {
                detectTapGestures { offset ->
                    val frac = (offset.x / size.width).coerceIn(0f, 1f)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSeekProgress(frac)
                }
            }
            .pointerInput(barCount) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val frac = (change.position.x / size.width).coerceIn(0f, 1f)
                    onSeekProgress(frac)
                }
            }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val totalWidth = size.width
            val totalHeight = size.height
            val barWidth = (totalWidth / barCount) * 0.65f
            val gap = (totalWidth / barCount) * 0.35f
            val centerY = totalHeight / 2f

            for (i in 0 until barCount) {
                val barFrac = i.toFloat() / barCount
                val isPlayed = barFrac <= audioProgressFrac
                val isInsideTrim = barFrac >= trimStartFrac && barFrac <= trimEndFrac

                // Dynamic height: if playing, add animated micro-fluctuations
                val dynamicFactor = if (isPlaying && isInsideTrim) {
                    val flutter = sin(pulsePhase + (i * 0.45f)) * 0.15f
                    (1f + flutter).coerceIn(0.6f, 1.35f)
                } else {
                    1f
                }

                val barH = (baselineBars[i] * dynamicFactor * totalHeight * 0.85f).coerceIn(4f, totalHeight)
                val x = i * (barWidth + gap) + gap / 2f

                val barBrush = when {
                    !isInsideTrim -> SolidColor(colors.textMuted.copy(alpha = 0.18f))
                    isPlayed -> Brush.verticalGradient(
                        listOf(colors.primary, colors.primaryDark),
                        startY = centerY - barH / 2f,
                        endY = centerY + barH / 2f
                    )
                    else -> SolidColor(colors.textMuted.copy(alpha = 0.42f))
                }

                drawRoundRect(
                    brush = barBrush,
                    topLeft = Offset(x, centerY - barH / 2f),
                    size = androidx.compose.ui.geometry.Size(barWidth, barH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }

            // Playhead Cursor Needle
            val playheadX = audioProgressFrac.coerceIn(0f, 1f) * totalWidth
            drawLine(
                color = colors.primary,
                start = Offset(playheadX, 0f),
                end = Offset(playheadX, totalHeight),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(
                color = colors.textMain,
                radius = 3.5.dp.toPx(),
                center = Offset(playheadX, 3.5.dp.toPx())
            )
        }
    }
}

/**
 * Dual Stereo VU Peak Level Meter (Left and Right channels)
 * 12-step calibrated LED ladder with green, amber, and red clipping tiers.
 */
@Composable
fun FcpStereoVuMeter(
    isPlaying: Boolean,
    volumeNormalized: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    val colors = LocalOscellionColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "VuMeterAnim")
    val decayOscillator by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(140, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "VuDecay"
    )

    val segments = 20
    val activeSegmentsLeft = remember(isPlaying, decayOscillator, volumeNormalized) {
        if (!isPlaying) 0
        else {
            val base = (volumeNormalized.coerceIn(0f, 1.5f) / 1.5f * 15).toInt()
            val jitter = ((sin(decayOscillator * 8f) + 1f) * 2.2f).toInt()
            (base + jitter).coerceIn(2, segments)
        }
    }

    val activeSegmentsRight = remember(isPlaying, decayOscillator, volumeNormalized) {
        if (!isPlaying) 0
        else {
            val base = (volumeNormalized.coerceIn(0f, 1.5f) / 1.5f * 14).toInt()
            val jitter = ((cos(decayOscillator * 7f) + 1f) * 2.2f).toInt()
            (base + jitter).coerceIn(2, segments)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.inputBg, RoundedCornerShape(10.dp))
            .border(0.5.dp, colors.cardBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        VuChannelBar("L", activeSegmentsLeft, segments)
        VuChannelBar("R", activeSegmentsRight, segments)
    }
}

@Composable
private fun VuChannelBar(channelLabel: String, activeSegments: Int, totalSegments: Int) {
    val colors = LocalOscellionColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = channelLabel,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textMuted,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(10.dp)
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            for (seg in 1..totalSegments) {
                val isLit = seg <= activeSegments
                val segColor = when {
                    !isLit -> colors.cardBorder.copy(alpha = 0.65f)
                    seg <= (totalSegments * 0.65f).toInt() -> colors.primary
                    seg <= (totalSegments * 0.85f).toInt() -> colors.warning
                    else -> colors.error
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(segColor)
                )
            }
        }
    }
}

// ============================================================================
// PILLAR 2: PRO MULTI-TRACK AUDIO MIXER RACK
// ============================================================================

data class FcpStudioSfxItem(
    val id: String,
    val title: String,
    val durationText: String,
    val icon: ImageVector,
    val description: String,
    val toneHz: Float
)

val BuiltInStudioSfxPresets = listOf(
    FcpStudioSfxItem("whoosh", "Cinematic Whoosh", "0.8s", Lucide.Wind, "Dynamic scene transition whoosh", 440f),
    FcpStudioSfxItem("camera", "Camera Shutter", "0.4s", Lucide.Camera, "Crisp mechanical shutter click", 880f),
    FcpStudioSfxItem("sub_hit", "Deep Sub Hit", "1.2s", Lucide.Zap, "Low-end cinematic bass impact", 65f),
    FcpStudioSfxItem("chime", "Glass Chime", "1.0s", Lucide.Bell, "Crystalline bell notification", 1200f),
    FcpStudioSfxItem("vinyl", "Vinyl Crackle", "2.0s", Lucide.Radio, "Retro warm analog lo-fi texture", 220f),
    FcpStudioSfxItem("pop", "Modern Pop", "0.3s", Lucide.Sparkles, "Snappy punchy accent pop", 600f)
)

/**
 * Built-in Quick Sound Effects Soundboard Carousel
 */
@Composable
fun FcpStudioSfxSoundboard(
    onPreviewSfx: (FcpStudioSfxItem) -> Unit,
    onInsertSfx: (FcpStudioSfxItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalOscellionColors.current
    val haptic = LocalHapticFeedback.current
    var playingSfxId by remember { mutableStateOf<String?>(null) }
    val cardBg = colors.cardSurface
    val padBg = colors.inputBg
    val borderColor = colors.cardBorder

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg, RoundedCornerShape(16.dp))
            .border(0.75.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Lucide.Zap, contentDescription = "Sound Effects", tint = colors.primary, modifier = Modifier.size(16.dp))
                Text("Sound Effects", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textMain)
            }
            Text("Tap to audition", fontSize = 10.5.sp, color = colors.textMuted)
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(BuiltInStudioSfxPresets) { sfx ->
                val isPlayingThis = playingSfxId == sfx.id

                Column(
                    modifier = Modifier
                        .width(114.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isPlayingThis) colors.primary.copy(alpha = 0.08f) else padBg)
                        .border(
                            width = if (isPlayingThis) 1.dp else 0.5.dp,
                            color = if (isPlayingThis) colors.primary else borderColor,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            playingSfxId = sfx.id
                            onPreviewSfx(sfx)
                        }
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.primary.copy(alpha = if (isPlayingThis) 0.20f else 0.10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = sfx.icon,
                                contentDescription = sfx.title,
                                tint = colors.primary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(colors.primary.copy(alpha = 0.14f))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onInsertSfx(sfx)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.Plus,
                                contentDescription = "Add ${sfx.title}",
                                tint = colors.primary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = sfx.title,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textMain,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            text = sfx.durationText,
                            fontSize = 10.sp,
                            color = colors.textMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// PILLAR 3: PRO COLOR GRADING & "APPLY TO ALL" STUDIO CONTROLS
// ============================================================================

/**
 * Dedicated Header Action for Syncing Color Grade across all timeline clips
 */
@Composable
fun FcpColorSyncAllHeader(
    isGraded: Boolean,
    onResetCurrent: () -> Unit,
    onApplyToAllClips: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Lucide.Palette, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(15.dp))
            Text("COLOR CORRECTION & GRADING", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (isGraded) {
                StudioBeveledButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onResetCurrent()
                    },
                    text = "Reset",
                    height = 28.dp,
                    cornerRadius = 6.dp
                )
            }

            StudioBeveledButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onApplyToAllClips()
                },
                icon = Lucide.CheckCheck,
                text = "Apply to All",
                accentColors = StudioDesignTokens.AmberGoldGradient,
                isActive = true,
                height = 28.dp,
                cornerRadius = 6.dp
            )
        }
    }
}

// ============================================================================
// PILLAR 4: ERGONOMIC SPEED RAMPING (REPLACES CRAMPED 8-BUTTON ROW)
// ============================================================================

/**
 * Ergonomic, Featherlight Speed Selector.
 * Combines 4 clean primary preset chips (0.5x, 1.0x, 1.5x, 2.0x) with a continuous
 * precision speed slider (0.25x to 3.0x) and haptic detents.
 */
@Composable
fun FcpErgonomicSpeedSelector(
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalOscellionColors.current
    val haptic = LocalHapticFeedback.current
    val scrubbingState = LocalSliderScrubbingState.current
    val speedSliderKey = remember { java.util.UUID.randomUUID().toString() }
    val baseCardBg = colors.cardSurface
    val chipNeutralBg = colors.cardSurfaceElevated
    val borderColor = colors.cardBorder
    val cardBg by androidx.compose.animation.animateColorAsState(
        targetValue = if (scrubbingState.isScrubbing) Color.Transparent else baseCardBg,
        animationSpec = tween(180),
        label = "speedCardBg"
    )
    val cardBorderAlpha by animateFloatAsState(
        targetValue = if (scrubbingState.isScrubbing) 0f else 1f,
        animationSpec = tween(180),
        label = "speedCardBorder"
    )
    val primaryPresets = listOf(
        Pair(0.5f, "0.5x"),
        Pair(1.0f, "1.0x"),
        Pair(1.5f, "1.5x"),
        Pair(2.0f, "2.0x")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg, RoundedCornerShape(14.dp))
            .border(0.75.dp, borderColor.copy(alpha = borderColor.alpha * cardBorderAlpha), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .editorChromeFadeOnScrub(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Lucide.Activity, contentDescription = "Playback Speed", tint = colors.primary, modifier = Modifier.size(15.dp))
                Text("Playback Speed", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textMain)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.primary.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${String.format("%.2f", currentSpeed)}x",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = colors.primary
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .editorChromeFadeOnScrub(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            primaryPresets.forEach { (spd, label) ->
                val isSelected = abs(currentSpeed - spd) < 0.04f
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isSelected) colors.primary else chipNeutralBg)
                        .border(0.75.dp, if (isSelected) colors.primary else borderColor, RoundedCornerShape(9.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSpeedChange(spd)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) colors.onPrimary else colors.textMain
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .focusSliderContainer(
                    sliderId = speedSliderKey,
                    isDark = colors.isDark,
                    cornerRadius = 12.dp,
                    horizontalPadding = 8.dp,
                    verticalPadding = 4.dp
                ),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Fine Speed", fontSize = 10.5.sp, color = colors.textMuted)
                Text("${String.format("%.2f", currentSpeed)}x", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = colors.primary)
            }

            AppleSlider(
                value = currentSpeed,
                sliderId = speedSliderKey,
                onValueChange = { newVal ->
                    val snapped = when {
                        abs(newVal - 1.0f) < 0.04f -> 1.0f
                        abs(newVal - 0.5f) < 0.04f -> 0.5f
                        abs(newVal - 1.5f) < 0.04f -> 1.5f
                        abs(newVal - 2.0f) < 0.04f -> 2.0f
                        else -> newVal
                    }
                    if (snapped != currentSpeed && (snapped == 1.0f || snapped == 0.5f || snapped == 2.0f)) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onSpeedChange(snapped)
                },
                valueRange = 0.25f..3.0f,
                gradientColors = listOf(colors.primary, colors.primary),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ============================================================================
// PILLAR 5: TEXT TIMELINE DISPLAY RANGE & STICKER PRESETS
// ============================================================================

data class FcpGraphicStickerPreset(
    val id: String,
    val label: String,
    val subtitle: String,
    val badgeColor: Color,
    val icon: ImageVector
)

val BuiltInGraphicStickers = listOf(
    FcpGraphicStickerPreset("live", "LIVE", "BROADCAST", Color(0xFFEF4444), Lucide.Radio),
    FcpGraphicStickerPreset("rec", "REC ●", "RECORDING", Color(0xFFEF4444), Lucide.Camera),
    FcpGraphicStickerPreset("4k", "4K HDR", "ULTRA HD", Color(0xFF00E5FF), Lucide.Sparkles),
    FcpGraphicStickerPreset("new", "NEW", "FEATURED", Color(0xFF10B981), Lucide.Zap),
    FcpGraphicStickerPreset("pro", "PRO", "STUDIO CUT", Color(0xFFF59E0B), Lucide.Film),
    FcpGraphicStickerPreset("cinema", "CINEMA", "2.39:1 SCOPE", Color(0xFF0381FE), Lucide.Film)
)

/**
 * Text Layer Timeline In/Out Timing Card.
 * Allows user to control exactly when a title layer starts and finishes on the video.
 */
@Composable
fun FcpTextTimelineRangeCard(
    layerName: String,
    timelineStartSec: Float,
    timelineEndSec: Float,
    totalDurationSec: Float,
    onRangeChange: (startSec: Float, endSec: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = com.chroma.studio.ui.theme.LocalOscellionColors.current
    val haptic = LocalHapticFeedback.current
    val scrubbingState = LocalSliderScrubbingState.current
    val startSliderKey = remember { java.util.UUID.randomUUID().toString() }
    val endSliderKey = remember { java.util.UUID.randomUUID().toString() }
    val cardBg by androidx.compose.animation.animateColorAsState(
        targetValue = if (scrubbingState.isScrubbing) Color.Transparent else colors.cardSurface.copy(alpha = if (colors.isDark) 0.72f else 0.88f),
        animationSpec = tween(180),
        label = "rangeCardBg"
    )
    val effectiveTotal = totalDurationSec.coerceAtLeast(1.0f)
    val effectiveEnd = if (timelineEndSec > timelineStartSec) timelineEndSec.coerceAtMost(effectiveTotal) else effectiveTotal

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .border(0.5.dp, if (scrubbingState.isScrubbing) Color.Transparent else colors.glassBorder, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .editorChromeFadeOnScrub(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Lucide.Clock, contentDescription = null, tint = colors.primary, modifier = Modifier.size(14.dp))
                Text("Title Timing", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textMain)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.primary.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${String.format("%.1fs", timelineStartSec)} – ${String.format("%.1fs", effectiveEnd)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = colors.primary
                )
            }
        }

        // Quick Presets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .editorChromeFadeOnScrub(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val presets = listOf(
                Pair("Full Clip", 0f to 0f),
                Pair("Intro 3s", 0f to 3f.coerceAtMost(effectiveTotal)),
                Pair("Outro 3s", (effectiveTotal - 3f).coerceAtLeast(0f) to effectiveTotal)
            )

            presets.forEach { (label, range) ->
                val isActive = if (range.second == 0f) timelineEndSec == 0f
                else abs(timelineStartSec - range.first) < 0.2f && abs(timelineEndSec - range.second) < 0.2f

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isActive) colors.primary
                            else colors.glassBg.copy(alpha = if (colors.isDark) 0.55f else 0.9f)
                        )
                        .border(
                            0.5.dp,
                            if (isActive) colors.primary else colors.glassBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onRangeChange(range.first, range.second)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 10.5.sp,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isActive) Color.White else colors.textMuted
                    )
                }
            }
        }

        // In & Out Dual Range Sliders
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .focusSliderContainer(
                    sliderId = startSliderKey,
                    isDark = colors.isDark,
                    cornerRadius = 10.dp,
                    horizontalPadding = 8.dp,
                    verticalPadding = 4.dp
                ),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("In-Point", fontSize = 10.sp, color = colors.textMuted)
                Text(String.format("%.1fs", timelineStartSec), fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = colors.textMain)
            }

            AppleSlider(
                value = timelineStartSec.coerceIn(0f, (effectiveEnd - 0.5f).coerceAtLeast(0f)),
                sliderId = startSliderKey,
                onValueChange = { newSt ->
                    val clampedSt = newSt.coerceIn(0f, (effectiveEnd - 0.5f).coerceAtLeast(0f))
                    onRangeChange(clampedSt, effectiveEnd)
                },
                valueRange = 0f..effectiveTotal,
                gradientColors = listOf(colors.primary, colors.primary),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .focusSliderContainer(
                    sliderId = endSliderKey,
                    isDark = colors.isDark,
                    cornerRadius = 10.dp,
                    horizontalPadding = 8.dp,
                    verticalPadding = 4.dp
                ),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Out-Point", fontSize = 10.sp, color = colors.textMuted)
                Text(String.format("%.1fs", effectiveEnd), fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = colors.textMain)
            }

            AppleSlider(
                value = effectiveEnd.coerceIn((timelineStartSec + 0.5f).coerceAtMost(effectiveTotal), effectiveTotal),
                sliderId = endSliderKey,
                onValueChange = { newEnd ->
                    val minEnd = (timelineStartSec + 0.5f).coerceAtMost(effectiveTotal)
                    val clampedEnd = newEnd.coerceIn(minEnd, effectiveTotal)
                    val outVal = if (timelineStartSec <= 0.05f && abs(clampedEnd - effectiveTotal) < 0.15f) 0f else clampedEnd
                    onRangeChange(timelineStartSec, outVal)
                },
                valueRange = 0f..effectiveTotal,
                gradientColors = listOf(colors.primary, colors.primary),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Clean Quick Badges Strip (borderless flat Apple list strip, zero nested card boxes or pills)
 */
@Composable
fun FcpQuickStickerStrip(
    onAddSticker: (FcpGraphicStickerPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = com.chroma.studio.ui.theme.LocalOscellionColors.current
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .editorChromeFadeOnScrub()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Lucide.Sparkles, contentDescription = null, tint = colors.primary, modifier = Modifier.size(13.dp))
                Text("Quick Badges", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textMain)
            }
            Text("Tap to add", fontSize = 10.5.sp, color = colors.textMuted)
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(BuiltInGraphicStickers) { sticker ->
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.inputBg)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onAddSticker(sticker)
                        }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(
                        imageVector = sticker.icon,
                        contentDescription = sticker.label,
                        tint = sticker.badgeColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Column {
                        Text(sticker.label, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = colors.textMain)
                        Text(sticker.subtitle, fontSize = 9.5.sp, color = colors.textMuted, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
