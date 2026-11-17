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
import com.composables.icons.lucide.*
import kotlin.math.*

// ============================================================================
// STUDIO DESIGN SYSTEM: PROFESSIONAL SOLID & GRADIENT PALETTES WITH BEVEL EDGES
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
    val BevelBorderActive = Color(0xFF00E5FF).copy(alpha = 0.55f)

    // Professional Multi-stop Gradients
    val CobaltGradient = listOf(Color(0xFF0284C7), Color(0xFF06B6D4))
    val AmberGoldGradient = listOf(Color(0xFFD97706), Color(0xFFF59E0B))
    val EmeraldMintGradient = listOf(Color(0xFF059669), Color(0xFF10B981))
    val CrimsonFlareGradient = listOf(Color(0xFFE11D48), Color(0xFFFB7185))
    val ElectricVioletGradient = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
    val CyberNeonGradient = listOf(Color(0xFF00F5FF), Color(0xFF0077FF))
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
            .studioBevel(cornerRadius = 10.dp)
            .background(StudioDesignTokens.TrackBg, RoundedCornerShape(10.dp))
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
                    !isInsideTrim -> SolidColor(Color(0xFF333846).copy(alpha = 0.45f))
                    isPlayed -> Brush.verticalGradient(
                        listOf(Color(0xFF00F5FF), Color(0xFF0284C7)),
                        startY = centerY - barH / 2f,
                        endY = centerY + barH / 2f
                    )
                    else -> Brush.verticalGradient(
                        listOf(Color(0xFF64748B), Color(0xFF334155)),
                        startY = centerY - barH / 2f,
                        endY = centerY + barH / 2f
                    )
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
                brush = Brush.verticalGradient(listOf(Color.White, Color(0xFF00E5FF))),
                start = Offset(playheadX, 0f),
                end = Offset(playheadX, totalHeight),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(
                color = Color.White,
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
    val infiniteTransition = rememberInfiniteTransition(label = "VuMeterAnim")
    val decayOscillator by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "VuDecay"
    )

    val activeSegmentsLeft = remember(isPlaying, decayOscillator, volumeNormalized) {
        if (!isPlaying) 2
        else {
            val base = (volumeNormalized.coerceIn(0f, 1.5f) / 1.5f * 9).toInt()
            val jitter = ((sin(decayOscillator * 8f) + 1f) * 1.5f).toInt()
            (base + jitter).coerceIn(1, 12)
        }
    }

    val activeSegmentsRight = remember(isPlaying, decayOscillator, volumeNormalized) {
        if (!isPlaying) 2
        else {
            val base = (volumeNormalized.coerceIn(0f, 1.5f) / 1.5f * 8).toInt()
            val jitter = ((cos(decayOscillator * 7f) + 1f) * 1.5f).toInt()
            (base + jitter).coerceIn(1, 12)
        }
    }

    Column(
        modifier = modifier
            .studioBevel(cornerRadius = 8.dp)
            .background(StudioDesignTokens.TrackBg, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        VuChannelBar("L", activeSegmentsLeft)
        VuChannelBar("R", activeSegmentsRight)
    }
}

@Composable
private fun VuChannelBar(channelLabel: String, activeSegments: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = channelLabel,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(10.dp)
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            for (seg in 1..12) {
                val isLit = seg <= activeSegments
                val segColor = when {
                    seg <= 7 -> if (isLit) Color(0xFF10B981) else Color(0xFF064E3B).copy(alpha = 0.35f)
                    seg <= 10 -> if (isLit) Color(0xFFF59E0B) else Color(0xFF78350F).copy(alpha = 0.35f)
                    else -> if (isLit) Color(0xFFEF4444) else Color(0xFF7F1D1D).copy(alpha = 0.35f)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(1.dp))
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
    val haptic = LocalHapticFeedback.current
    var playingSfxId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .studioBevel(cornerRadius = 12.dp)
            .background(StudioDesignTokens.CardBg, RoundedCornerShape(12.dp))
            .padding(12.dp),
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
                Icon(Lucide.Zap, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(15.dp))
                Text("STUDIO SOUND EFFECTS (SFX)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Text("6 Presets", fontSize = 10.sp, color = Color(0xFF94A3B8))
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(BuiltInStudioSfxPresets) { sfx ->
                val isPlayingThis = playingSfxId == sfx.id

                Box(
                    modifier = Modifier
                        .width(135.dp)
                        .studioBevel(
                            cornerRadius = 10.dp,
                            isPressed = isPlayingThis,
                            accentBorder = if (isPlayingThis) Brush.verticalGradient(StudioDesignTokens.CobaltGradient) else null
                        )
                        .background(StudioDesignTokens.ElevatedBg, RoundedCornerShape(10.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            playingSfxId = sfx.id
                            onPreviewSfx(sfx)
                        }
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isPlayingThis) Color(0xFF00E5FF).copy(alpha = 0.25f)
                                        else Color.White.copy(alpha = 0.08f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = sfx.icon,
                                    contentDescription = sfx.title,
                                    tint = if (isPlayingThis) Color(0xFF00E5FF) else Color(0xFFCBD5E1),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Text(sfx.durationText, fontSize = 9.sp, color = Color(0xFF94A3B8), fontFamily = FontFamily.Monospace)
                        }

                        Text(
                            text = sfx.title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = sfx.description,
                            fontSize = 8.5.sp,
                            color = Color(0xFF94A3B8),
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Quick Add Action
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onInsertSfx(sfx)
                                }
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Lucide.Plus, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(11.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Use SFX", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
                        }
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
    val haptic = LocalHapticFeedback.current
    val scrubbingState = LocalSliderScrubbingState.current
    val speedSliderKey = remember { java.util.UUID.randomUUID().toString() }
    val cardBg by androidx.compose.animation.animateColorAsState(
        targetValue = if (scrubbingState.isScrubbing) Color.Transparent else StudioDesignTokens.CardBg,
        animationSpec = tween(180),
        label = "speedCardBg"
    )
    val cardBorderAlpha by animateFloatAsState(
        targetValue = if (scrubbingState.isScrubbing) 0f else 1f,
        animationSpec = tween(180),
        label = "speedCardBorder"
    )
    val primaryPresets = listOf(
        Pair(0.5f, "0.5x Slow"),
        Pair(1.0f, "1.0x Normal"),
        Pair(1.5f, "1.5x Fast"),
        Pair(2.0f, "2.0x Hyper")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .studioBevel(
                cornerRadius = 12.dp,
                accentBorder = Brush.verticalGradient(
                    listOf(
                        StudioDesignTokens.BevelHighlightTop.copy(alpha = StudioDesignTokens.BevelHighlightTop.alpha * cardBorderAlpha),
                        StudioDesignTokens.BevelBorderDefault.copy(alpha = StudioDesignTokens.BevelBorderDefault.alpha * cardBorderAlpha)
                    )
                )
            )
            .background(cardBg, RoundedCornerShape(12.dp))
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
                Icon(Lucide.Activity, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(15.dp))
                Text("PLAYBACK SPEED & TEMPO", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            // Real-time badge
            Box(
                modifier = Modifier
                    .studioBevel(cornerRadius = 6.dp)
                    .background(Color(0xFF00E5FF).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${String.format("%.2f", currentSpeed)}x",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF00E5FF)
                )
            }
        }

        // Primary 4 Presets (Roomy, perfectly proportioned, never cramped)
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
                        .studioBevel(
                            cornerRadius = 8.dp,
                            isPressed = isSelected,
                            accentBorder = if (isSelected) Brush.verticalGradient(StudioDesignTokens.CobaltGradient) else null
                        )
                        .background(
                            if (isSelected) Brush.verticalGradient(StudioDesignTokens.CobaltGradient)
                            else Brush.verticalGradient(StudioDesignTokens.TitaniumGradient),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSpeedChange(spd)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                    )
                }
            }
        }

        // Continuous Slider for Fine Tuning
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .focusSliderContainer(
                    sliderId = speedSliderKey,
                    isDark = true,
                    cornerRadius = 14.dp,
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
                Text("Precision Smooth Ramping", fontSize = 10.sp, color = Color(0xFF94A3B8))
                Text("${String.format("%.2f", currentSpeed)}x", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color(0xFF00E5FF))
            }

            AppleSlider(
                value = currentSpeed,
                sliderId = speedSliderKey,
                onValueChange = { newVal ->
                    // Snap with haptic detent near common multiples
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
                gradientColors = listOf(Color(0xFF00E5FF), Color(0xFF38BDF8)),
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
    FcpGraphicStickerPreset("cinema", "CINEMA", "2.39:1 SCOPE", Color(0xFF8B5CF6), Lucide.Film)
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
    val haptic = LocalHapticFeedback.current
    val scrubbingState = LocalSliderScrubbingState.current
    val rangeSliderKey = remember { java.util.UUID.randomUUID().toString() }
    val cardBg by androidx.compose.animation.animateColorAsState(
        targetValue = if (scrubbingState.isScrubbing) Color.Transparent else StudioDesignTokens.CardBg,
        animationSpec = tween(180),
        label = "rangeCardBg"
    )
    val cardBorderAlpha by animateFloatAsState(
        targetValue = if (scrubbingState.isScrubbing) 0f else 1f,
        animationSpec = tween(180),
        label = "rangeCardBorder"
    )
    val effectiveTotal = totalDurationSec.coerceAtLeast(1.0f)
    val effectiveEnd = if (timelineEndSec > timelineStartSec) timelineEndSec else effectiveTotal

    Column(
        modifier = modifier
            .fillMaxWidth()
            .studioBevel(
                cornerRadius = 12.dp,
                accentBorder = Brush.verticalGradient(
                    listOf(
                        StudioDesignTokens.BevelHighlightTop.copy(alpha = StudioDesignTokens.BevelHighlightTop.alpha * cardBorderAlpha),
                        StudioDesignTokens.BevelBorderDefault.copy(alpha = StudioDesignTokens.BevelBorderDefault.alpha * cardBorderAlpha)
                    )
                )
            )
            .background(cardBg, RoundedCornerShape(12.dp))
            .padding(12.dp),
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
                Icon(Lucide.Clock, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(15.dp))
                Text("TITLE TIMELINE DURATION", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Box(
                modifier = Modifier
                    .studioBevel(cornerRadius = 6.dp)
                    .background(Color(0xFFF59E0B).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${String.format("%.1fs", timelineStartSec)} - ${String.format("%.1fs", effectiveEnd)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFF59E0B)
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
                Pair("Entire Video", 0f to 0f),
                Pair("Intro (0-3s)", 0f to 3f.coerceAtMost(effectiveTotal)),
                Pair("Outro (Last 3s)", (effectiveTotal - 3f).coerceAtLeast(0f) to effectiveTotal)
            )

            presets.forEach { (label, range) ->
                val isActive = if (range.second == 0f) timelineEndSec == 0f
                else abs(timelineStartSec - range.first) < 0.2f && abs(timelineEndSec - range.second) < 0.2f

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp)
                        .studioBevel(
                            cornerRadius = 6.dp,
                            isPressed = isActive,
                            accentBorder = if (isActive) Brush.verticalGradient(StudioDesignTokens.AmberGoldGradient) else null
                        )
                        .background(
                            if (isActive) Brush.verticalGradient(StudioDesignTokens.AmberGoldGradient)
                            else Brush.verticalGradient(StudioDesignTokens.TitaniumGradient),
                            RoundedCornerShape(6.dp)
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
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        color = if (isActive) Color.White else Color(0xFFCBD5E1)
                    )
                }
            }
        }

        // In & Out Dual Range Sliders
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .focusSliderContainer(
                    sliderId = rangeSliderKey,
                    isDark = true,
                    cornerRadius = 14.dp,
                    horizontalPadding = 8.dp,
                    verticalPadding = 4.dp
                ),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("In-Point: ${String.format("%.1fs", timelineStartSec)}", fontSize = 10.sp, color = Color(0xFF94A3B8))
                Text("Out-Point: ${String.format("%.1fs", effectiveEnd)}", fontSize = 10.sp, color = Color(0xFF94A3B8))
            }

            AppleSlider(
                value = timelineStartSec,
                sliderId = rangeSliderKey,
                onValueChange = { newSt ->
                    val clampedSt = newSt.coerceIn(0f, (effectiveEnd - 0.5f).coerceAtLeast(0f))
                    onRangeChange(clampedSt, effectiveEnd)
                },
                valueRange = 0f..effectiveTotal,
                gradientColors = listOf(Color(0xFFF59E0B), Color(0xFFFBBF24)),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Quick Sticker & Graphic Badge Strip for instant insertion onto Timeline
 */
@Composable
fun FcpQuickStickerStrip(
    onAddSticker: (FcpGraphicStickerPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .editorChromeFadeOnScrub()
            .studioBevel(cornerRadius = 12.dp)
            .background(StudioDesignTokens.CardBg, RoundedCornerShape(12.dp))
            .padding(12.dp),
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
                Icon(Lucide.Sparkles, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(15.dp))
                Text("GRAPHIC BADGES & TIMELINE STICKERS", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Text("Tap to Add", fontSize = 10.sp, color = Color(0xFF94A3B8))
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(BuiltInGraphicStickers) { sticker ->
                Box(
                    modifier = Modifier
                        .studioBevel(cornerRadius = 8.dp)
                        .background(StudioDesignTokens.ElevatedBg, RoundedCornerShape(8.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onAddSticker(sticker)
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(sticker.badgeColor.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = sticker.icon,
                                contentDescription = sticker.label,
                                tint = sticker.badgeColor,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                        Column {
                            Text(sticker.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(sticker.subtitle, fontSize = 8.sp, color = sticker.badgeColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
