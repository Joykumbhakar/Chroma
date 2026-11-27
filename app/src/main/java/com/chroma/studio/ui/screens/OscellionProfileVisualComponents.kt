@file:OptIn(
    ExperimentalLayoutApi::class,
    ExperimentalFoundationApi::class
)

package com.chroma.studio.ui.screens

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chroma.studio.data.MusicRepository
import com.chroma.studio.data.normalizeMusicStreamUrl
import com.chroma.studio.model.DiscoverPost
import com.chroma.studio.model.FlashVideo
import com.chroma.studio.model.UserProfile
import com.chroma.studio.ui.components.VerifiedBadge
import com.chroma.studio.ui.components.getAppleStatusRingBrush
import com.chroma.studio.ui.components.getAvatarPresetDrawableRes
import com.chroma.studio.ui.components.getBannerPresetDrawableRes
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Activity
import com.composables.icons.lucide.BookOpen
import com.composables.icons.lucide.Briefcase
import com.composables.icons.lucide.LayoutTemplate
import com.composables.icons.lucide.Monitor
import com.composables.icons.lucide.Palette
import com.composables.icons.lucide.Smartphone
import com.composables.icons.lucide.Terminal
import com.composables.icons.lucide.Video
import com.composables.icons.lucide.Volume2
import com.composables.icons.lucide.VolumeX
import com.composables.icons.lucide.Wrench
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Color palette and surface tokens for the redesigned Oscellion Profile experience.
 * Light mode preserves exact hex values from the Oscellion design spec while adapting cleanly for Dark Mode.
 */
internal object OscellionProfilePalette {
    val BlueAccent = Color(0xFF0064FA)
    val BlueBright = Color(0xFF1A7EFF)
    val BlueDeep = Color(0xFF0047B3)
    val BlueGradEnd = Color(0xFF0052D6)
    val BlueSky = Color(0xFF38BDF8)
    val Indigo = Color(0xFF818CF8)
    val OnlineGreen = Color(0xFF10B981)
    val GoldTop = Color(0xFFFFD700)
    val GoldMid = Color(0xFFF59E0B)
    val GoldShimmer = Color(0xFFFFF5B8)
    val GoldDeep = Color(0xFFD97706)
    val CrimsonTrack = Color(0xFFDC2626)

    fun sheetBg(isDark: Boolean): Color = if (isDark) Color(0xFF0D1017) else Color(0xFFFFFFFF)
    fun elevatedCard(isDark: Boolean): Color = if (isDark) Color(0xFF151B27) else Color(0xFFFFFFFF)
    fun inkPrimary(isDark: Boolean): Color = if (isDark) Color(0xFFF5F8FF) else Color(0xFF070B18)
    fun inkSecondary(isDark: Boolean): Color = if (isDark) Color(0xFFD0D9EA) else Color(0xFF2F3A4D)
    fun subInk(isDark: Boolean): Color = if (isDark) Color(0xFF9AA8BF) else Color(0xFF525E75)
    fun metaInk(isDark: Boolean): Color = if (isDark) Color(0xFFB3C0D4) else Color(0xFF5F6B82)
    fun inkMuted(isDark: Boolean): Color = if (isDark) Color(0xFF8C9AB0) else Color(0xFF8B95AA)
    fun softPill(isDark: Boolean): Color = if (isDark) Color(0xFF1B2231) else Color(0xFFEFF1F6)
    fun pillSurface(isDark: Boolean): Color = if (isDark) Color(0xFF182030) else Color(0xFFF4F7FD)
    fun badgeGradTop(isDark: Boolean): Color = if (isDark) Color(0xFF172642) else Color(0xFFF2F7FF)
    fun badgeGradBottom(isDark: Boolean): Color = if (isDark) Color(0xFF111C33) else Color(0xFFE6F0FF)
    fun badgeBorder(isDark: Boolean): Color = if (isDark) Color(0xFF253C66) else Color(0xFFCFE2FF)
    fun statsDivider(isDark: Boolean): Color = if (isDark) Color(0xFF232C3E) else Color(0xFFEAEFF7)
    fun hairline(isDark: Boolean): Color = if (isDark) Color(0xFF222A3A) else Color(0xFFEEF2F9)
    fun borderLight(isDark: Boolean): Color = if (isDark) Color(0xFF283247) else Color(0xFFE5ECF7)
    fun ringInactive(isDark: Boolean): Color = if (isDark) Color(0xFF2A3448) else Color(0xFFDCE5F4)
    fun topIconBg(isDark: Boolean): Color = if (isDark) Color(0xD9171E2C) else Color(0xD6FFFFFF)
    fun topIconBorder(isDark: Boolean): Color = if (isDark) Color(0x663B4B6B) else Color(0xE6FFFFFF)
}

@Suppress("DEPRECATION")
internal val OscellionTightTextStyle = TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both
    )
)

internal enum class ProfileSocialType {
    GITHUB,
    INSTAGRAM,
    WEBSITE
}

/**
 * Lightweight playable track representation for the floating draggable vinyl player on UserProfileScreen.
 */
internal data class ProfilePlayableTrack(
    val id: String,
    val title: String,
    val artist: String,
    val audioUrl: String,
    val coverUrl: String = "",
    val fallbackDurationSec: Int = 30
)

/**
 * Smoothstep easing function matching the reference CSS cubic-bezier choreography.
 */
internal fun smoothStepFraction(t: Float): Float {
    val clamped = t.coerceIn(0f, 1f)
    return clamped * clamped * (3f - 2f * clamped)
}

/**
 * Draws the signature Oscellion layered liquid-wave banner backdrop with solar glow,
 * gentle morphing wave motion, highlight ribbon, and atmospheric micro-dot matrix.
 */
@Composable
internal fun LiquidWaveBannerCanvas(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val waveTransition = rememberInfiniteTransition(label = "bannerWave")
    val wavePhaseA by waveTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bannerWaveA"
    )
    val wavePhaseB by waveTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bannerWaveB"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val baseColors = if (isDark) {
            listOf(Color(0xFF0A162D), Color(0xFF102447), Color(0xFF132D58))
        } else {
            listOf(Color(0xFFE4F0FF), Color(0xFFC9E0FF), Color(0xFFDDE8FF))
        }

        drawRect(
            brush = Brush.linearGradient(
                colors = baseColors,
                start = Offset.Zero,
                end = Offset(w, h)
            )
        )

        // Top-right soft radial sun bloom
        drawCircle(
            brush = Brush.radialGradient(
                colors = if (isDark) {
                    listOf(Color(0x4060A5FA), Color(0x183B82F6), Color.Transparent)
                } else {
                    listOf(Color.White.copy(alpha = 0.92f), Color.White.copy(alpha = 0.24f), Color.Transparent)
                },
                center = Offset(w * 0.78f, h * 0.16f),
                radius = w * 0.34f
            ),
            radius = w * 0.34f,
            center = Offset(w * 0.78f, h * 0.16f)
        )

        // Lower liquid wave A
        val y0 = h * (0.73f - 0.05f * wavePhaseA)
        val waveA = Path().apply {
            moveTo(-w * 0.05f, y0)
            cubicTo(
                w * (0.18f + 0.03f * wavePhaseA), h * (0.43f + 0.09f * wavePhaseA),
                w * (0.40f + 0.03f * wavePhaseA), h * (0.90f - 0.10f * wavePhaseA),
                w * (0.64f + 0.03f * wavePhaseA), h * (0.52f - 0.06f * wavePhaseA)
            )
            cubicTo(
                w * 0.79f, h * (0.29f - 0.03f * wavePhaseA),
                w * 0.91f, h * (0.31f + 0.05f * wavePhaseA),
                w * 1.05f, h * (0.42f + 0.06f * wavePhaseA)
            )
            lineTo(w * 1.05f, h * 1.08f)
            lineTo(-w * 0.05f, h * 1.08f)
            close()
        }
        drawPath(
            path = waveA,
            brush = Brush.linearGradient(
                colors = if (isDark) {
                    listOf(Color(0x521D4ED8), Color(0x3838BDF8), Color(0x286366F1))
                } else {
                    listOf(
                        OscellionProfilePalette.BlueAccent.copy(alpha = 0.28f),
                        OscellionProfilePalette.BlueSky.copy(alpha = 0.22f),
                        OscellionProfilePalette.Indigo.copy(alpha = 0.18f)
                    )
                },
                start = Offset.Zero,
                end = Offset(w, h * 0.9f)
            )
        )

        // Specular ribbon along wave A crest
        val ribbonPath = Path().apply {
            moveTo(-w * 0.05f, y0)
            cubicTo(
                w * (0.18f + 0.03f * wavePhaseA), h * (0.43f + 0.09f * wavePhaseA),
                w * (0.40f + 0.03f * wavePhaseA), h * (0.90f - 0.10f * wavePhaseA),
                w * (0.64f + 0.03f * wavePhaseA), h * (0.52f - 0.06f * wavePhaseA)
            )
            cubicTo(
                w * 0.79f, h * (0.29f - 0.03f * wavePhaseA),
                w * 0.91f, h * (0.31f + 0.05f * wavePhaseA),
                w * 1.05f, h * (0.42f + 0.06f * wavePhaseA)
            )
        }
        drawPath(
            path = ribbonPath,
            brush = Brush.horizontalGradient(
                colors = if (isDark) {
                    listOf(Color.Transparent, Color(0x6693C5FD), Color.Transparent)
                } else {
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.72f), Color.Transparent)
                }
            ),
            style = Stroke(width = 3f)
        )

        // Upper translucent wave B
        val waveB = Path().apply {
            moveTo(-w * 0.05f, h * (0.23f + 0.05f * wavePhaseB))
            cubicTo(
                w * (0.21f + 0.02f * wavePhaseB), h * (0.54f - 0.08f * wavePhaseB),
                w * (0.42f + 0.02f * wavePhaseB), h * (0.09f + 0.06f * wavePhaseB),
                w * (0.70f + 0.02f * wavePhaseB), h * (0.41f + 0.05f * wavePhaseB)
            )
            cubicTo(
                w * 0.84f, h * (0.57f + 0.03f * wavePhaseB),
                w * 0.95f, h * (0.49f - 0.05f * wavePhaseB),
                w * 1.05f, h * (0.38f - 0.05f * wavePhaseB)
            )
            lineTo(w * 1.05f, -h * 0.08f)
            lineTo(-w * 0.05f, -h * 0.08f)
            close()
        }
        drawPath(
            path = waveB,
            brush = Brush.linearGradient(
                colors = if (isDark) {
                    listOf(Color(0x4D2563EB), Color(0x2460A5FA), Color(0x101E3A8A))
                } else {
                    listOf(
                        Color(0xFF0E76FF).copy(alpha = 0.28f),
                        Color(0xFF60A5FA).copy(alpha = 0.14f),
                        Color(0xFFBFDBFE).copy(alpha = 0.06f)
                    )
                },
                start = Offset(w, 0f),
                end = Offset(0f, h)
            )
        )

        // Micro-dot atmospheric matrix
        val dotColor = if (isDark) Color(0x3860A5FA) else OscellionProfilePalette.BlueAccent.copy(alpha = 0.14f)
        val dotR = 1.5.dp.toPx()
        for (row in 0..1) {
            for (col in 0..2) {
                drawCircle(
                    color = dotColor,
                    radius = dotR,
                    center = Offset(w * (0.08f + col * 0.044f), h * (0.22f + row * 0.11f))
                )
            }
        }
        for (col in 0..2) {
            drawCircle(
                color = dotColor,
                radius = dotR,
                center = Offset(w * (0.83f + col * 0.044f), h * 0.64f)
            )
        }
    }
}

/**
 * Royal Blue macOS silhouette default avatar matching the Oscellion reference design.
 */
@Composable
internal fun MacOsDefaultAvatarCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val s = minOf(w, h) / 512f

        drawCircle(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF2563EB), Color(0xFF1E3A8A))
            ),
            radius = minOf(w, h) / 2f
        )

        val clipCircle = Path().apply {
            addOval(Rect(Offset.Zero, size))
        }
        clipPath(clipCircle) {
            val silBrush = Brush.verticalGradient(
                colors = listOf(Color.White, Color(0xFFD5D9E0)),
                startY = 0f,
                endY = 512f * s
            )
            // Head
            drawCircle(
                brush = silBrush,
                radius = 95f * s,
                center = Offset(256f * s, 165f * s)
            )
            // Shoulders silhouette
            drawCircle(
                brush = silBrush,
                radius = 250f * s,
                center = Offset(256f * s, 530f * s)
            )
        }

        drawCircle(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xB3FFFFFF), Color(0x33000000))
            ),
            radius = minOf(w, h) / 2f - 1.5f * s,
            style = Stroke(width = 3f * s)
        )
    }
}

/**
 * Profile avatar with multi-tier Story Ring (Golden circle for Verified creators, Green for Only Friends,
 * Electric Blue for active unseen stories, Gray for seen/inactive, plus upload progress/error states),
 * and a single animated Online green pulse dot.
 */
@Composable
internal fun OscellionProfileAvatar(
    profile: UserProfile?,
    avatarUrl: String,
    isDark: Boolean,
    isOnline: Boolean,
    hasActiveStories: Boolean,
    isStorySeen: Boolean,
    isOnlyFriendStory: Boolean,
    isVerified: Boolean,
    isUploading: Boolean,
    isUploadFailed: Boolean,
    size: Dp = 96.dp,
    onTap: () -> Unit,
    onLongTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetColor = OscellionProfilePalette.sheetBg(isDark)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = tween(120),
        label = "avatarPress"
    )

    val ringTransition = rememberInfiniteTransition(label = "profileRing")
    val ringRotation by ringTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isUploading) 1100 else 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringSpin"
    )

    val pulseTransition = rememberInfiniteTransition(label = "onlinePulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "onlinePulseScale"
    )
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "onlinePulseAlpha"
    )

    val ringBrush = remember(isUploading, isUploadFailed, hasActiveStories, isStorySeen, isOnlyFriendStory, isVerified, isDark) {
        when {
            isUploadFailed -> Brush.linearGradient(listOf(Color(0xFFEF4444), Color(0xFFB91C1C)))
            isUploading -> Brush.sweepGradient(
                listOf(OscellionProfilePalette.BlueBright, Color(0xFF60A5FA), Color(0xFFEFF6FF), OscellionProfilePalette.BlueBright)
            )
            hasActiveStories && !isStorySeen && isOnlyFriendStory -> Brush.sweepGradient(
                listOf(Color(0xFF10B981), Color(0xFF34D399), Color(0xFF059669), Color(0xFF10B981))
            )
            isVerified && (hasActiveStories || !isStorySeen) -> Brush.sweepGradient(
                listOf(
                    OscellionProfilePalette.GoldTop,
                    OscellionProfilePalette.GoldMid,
                    OscellionProfilePalette.GoldShimmer,
                    OscellionProfilePalette.GoldDeep,
                    OscellionProfilePalette.GoldTop
                )
            )
            isVerified -> Brush.linearGradient(
                listOf(
                    OscellionProfilePalette.GoldMid.copy(alpha = 0.85f),
                    OscellionProfilePalette.GoldTop.copy(alpha = 0.90f),
                    OscellionProfilePalette.GoldDeep.copy(alpha = 0.85f)
                )
            )
            hasActiveStories && !isStorySeen -> getAppleStatusRingBrush(
                hasStatus = true,
                isSeen = false,
                isDark = isDark,
                isOnlyFriend = false,
                isVerified = false
            )
            hasActiveStories && isStorySeen -> Brush.linearGradient(
                listOf(OscellionProfilePalette.ringInactive(isDark), OscellionProfilePalette.ringInactive(isDark))
            )
            else -> Brush.linearGradient(
                listOf(OscellionProfilePalette.ringInactive(isDark), OscellionProfilePalette.ringInactive(isDark))
            )
        }
    }

    val shouldAnimateRotation = isUploading || (hasActiveStories && !isStorySeen) || isVerified
    val ringStrokeWidth = when {
        isUploadFailed || isUploading -> 3.5.dp
        hasActiveStories && !isStorySeen -> 3.2.dp
        isVerified -> 3.dp
        else -> 2.8.dp
    }

    Box(
        modifier = modifier
            .size(size)
            .scale(pressScale)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onTap,
                onLongClick = onLongTap
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = ringStrokeWidth.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            if (shouldAnimateRotation) {
                rotate(degrees = ringRotation) {
                    drawArc(
                        brush = ringBrush,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                }
            } else {
                drawArc(
                    brush = ringBrush,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        val innerSize = size - 6.dp
        Box(
            modifier = Modifier
                .size(innerSize)
                .clip(CircleShape)
                .background(sheetColor)
                .padding(3.5.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val cleanUrl = avatarUrl.trim()
            val presetRes = remember(cleanUrl) { getAvatarPresetDrawableRes(cleanUrl) }
            val isCustomPhoto = cleanUrl.isNotBlank() && !cleanUrl.contains("default_avatar") && presetRes == null

            when {
                presetRes != null -> {
                    Image(
                        painter = painterResource(id = presetRes),
                        contentDescription = profile?.displayName ?: "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                }
                isCustomPhoto -> {
                    AsyncImage(
                        model = cleanUrl,
                        contentDescription = profile?.displayName ?: "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                }
                else -> {
                    MacOsDefaultAvatarCanvas(modifier = Modifier.fillMaxSize())
                }
            }
        }

        if (isOnline) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(3.dp)
                    .size(18.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(17.dp)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                            alpha = pulseAlpha
                        }
                        .border(2.dp, OscellionProfilePalette.OnlineGreen.copy(alpha = 0.65f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(17.dp)
                        .clip(CircleShape)
                        .background(OscellionProfilePalette.OnlineGreen)
                        .border(3.dp, sheetColor, CircleShape)
                )
            }
        }
    }
}

/**
 * Crown vector icon rendered in custom vector Canvas for Admin/Founder/Owner role capsules.
 */
@Composable
internal fun CrownVectorIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val path = Path().apply {
            moveTo(2f * sx, 4f * sy)
            lineTo(5f * sx, 16f * sy)
            lineTo(19f * sx, 16f * sy)
            lineTo(22f * sx, 4f * sy)
            lineTo(16f * sx, 10f * sy)
            lineTo(12f * sx, 4f * sy)
            lineTo(8f * sx, 10f * sy)
            close()
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = 2.4f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawLine(
            color = tint,
            start = Offset(5f * sx, 20f * sy),
            end = Offset(19f * sx, 20f * sy),
            strokeWidth = 2.4f * sx,
            cap = StrokeCap.Round
        )
    }
}

@Composable
internal fun CodeAngleVectorIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val left = Path().apply {
            moveTo(w * 0.35f, h * 0.24f)
            lineTo(w * 0.12f, h * 0.50f)
            lineTo(w * 0.35f, h * 0.76f)
        }
        val right = Path().apply {
            moveTo(w * 0.65f, h * 0.24f)
            lineTo(w * 0.88f, h * 0.50f)
            lineTo(w * 0.65f, h * 0.76f)
        }
        drawPath(left, tint, style = stroke)
        drawPath(right, tint, style = stroke)
    }
}

private fun resolveRoleIcon(role: String?): ImageVector? {
    return when (role?.trim()?.lowercase()) {
        "designer" -> Lucide.Palette
        "web designer" -> Lucide.Monitor
        "frontend developer" -> Lucide.LayoutTemplate
        "android developer", "ios developer" -> Lucide.Smartphone
        "content creator" -> Lucide.Video
        "doctor" -> Lucide.Activity
        "engineer" -> Lucide.Wrench
        "advocate" -> Lucide.BookOpen
        "ca" -> Lucide.Briefcase
        else -> null
    }
}

/**
 * Soft ice-blue gradient Role capsule matching the exact Oscellion design spec.
 */
@Composable
internal fun OscellionRoleCapsuleBadge(
    role: String,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val cleanRole = role.trim()
    val roleLower = cleanRole.lowercase()
    val useDeveloperChevrons = roleLower == "developer"
    val useCrown = roleLower in setOf("admin", "founder", "owner", "ceo", "creator", "founder & ceo")
    val customVectorIcon = if (!useCrown && !useDeveloperChevrons) resolveRoleIcon(cleanRole) else null
    val blueInk = if (isDark) Color(0xFF60A5FA) else Color(0xFF0058E6)

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        OscellionProfilePalette.badgeGradTop(isDark),
                        OscellionProfilePalette.badgeGradBottom(isDark)
                    )
                )
            )
            .border(1.dp, OscellionProfilePalette.badgeBorder(isDark), CircleShape)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        when {
            useDeveloperChevrons -> CodeAngleVectorIcon(tint = OscellionProfilePalette.BlueAccent, modifier = Modifier.size(12.dp))
            useCrown -> CrownVectorIcon(tint = OscellionProfilePalette.BlueAccent, modifier = Modifier.size(12.dp))
            customVectorIcon != null -> Icon(
                imageVector = customVectorIcon,
                contentDescription = null,
                tint = OscellionProfilePalette.BlueAccent,
                modifier = Modifier.size(12.dp)
            )
            else -> CrownVectorIcon(tint = OscellionProfilePalette.BlueAccent, modifier = Modifier.size(12.dp))
        }
        Text(
            text = cleanRole,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = blueInk,
            maxLines = 1,
            style = OscellionTightTextStyle
        )
    }
}

/**
 * Borderless metadata icons (Location pin, Calendar icon, Male/Female/Non-binary symbol).
 */
@Composable
internal fun LocationPinVectorIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val stroke = Stroke(width = 2.1f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val path = Path().apply {
            moveTo(20f * sx, 10f * sy)
            cubicTo(20f * sx, 15.5f * sy, 12f * sx, 22f * sy, 12f * sx, 22f * sy)
            cubicTo(12f * sx, 22f * sy, 4f * sx, 15.5f * sy, 4f * sx, 10f * sy)
            cubicTo(4f * sx, 5.58f * sy, 7.58f * sx, 2f * sy, 12f * sx, 2f * sy)
            cubicTo(16.42f * sx, 2f * sy, 20f * sx, 5.58f * sy, 20f * sx, 10f * sy)
            close()
        }
        drawPath(path, tint, style = stroke)
        drawCircle(tint, radius = 3f * sx, center = Offset(12f * sx, 10f * sy), style = stroke)
    }
}

@Composable
internal fun CalendarYearVectorIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val stroke = Stroke(width = 2.1f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawRoundRect(
            color = tint,
            topLeft = Offset(3f * sx, 4f * sy),
            size = Size(18f * sx, 18f * sy),
            cornerRadius = CornerRadius(3f * sx, 3f * sy),
            style = stroke
        )
        drawLine(tint, Offset(16f * sx, 2f * sy), Offset(16f * sx, 6f * sy), 2.1f * sx, StrokeCap.Round)
        drawLine(tint, Offset(8f * sx, 2f * sy), Offset(8f * sx, 6f * sy), 2.1f * sx, StrokeCap.Round)
        drawLine(tint, Offset(3f * sx, 10f * sy), Offset(21f * sx, 10f * sy), 2.1f * sx)
    }
}

@Composable
internal fun GenderSymbolVectorIcon(
    gender: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val g = gender.trim().lowercase()
    Canvas(modifier = modifier) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val stroke = Stroke(width = 2.2f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when {
            g == "female" || g == "woman" || g == "she/her" -> {
                drawCircle(tint, radius = 5.5f * sx, center = Offset(12f * sx, 9f * sy), style = stroke)
                drawLine(tint, Offset(12f * sx, 14.5f * sy), Offset(12f * sx, 21.5f * sy), 2.2f * sx, StrokeCap.Round)
                drawLine(tint, Offset(8.5f * sx, 18.5f * sy), Offset(15.5f * sx, 18.5f * sy), 2.2f * sx, StrokeCap.Round)
            }
            g == "male" || g == "man" || g == "he/him" -> {
                drawCircle(tint, radius = 5.5f * sx, center = Offset(10f * sx, 14f * sy), style = stroke)
                drawLine(tint, Offset(14.2f * sx, 9.8f * sy), Offset(19.8f * sx, 4.2f * sy), 2.2f * sx, StrokeCap.Round)
                drawLine(tint, Offset(14.5f * sx, 4.2f * sy), Offset(19.8f * sx, 4.2f * sy), 2.2f * sx, StrokeCap.Round)
                drawLine(tint, Offset(19.8f * sx, 4.2f * sy), Offset(19.8f * sx, 9.5f * sy), 2.2f * sx, StrokeCap.Round)
            }
            else -> {
                drawCircle(tint, radius = 5f * sx, center = Offset(12f * sx, 12.5f * sy), style = stroke)
                drawLine(tint, Offset(12f * sx, 17.5f * sy), Offset(12f * sx, 22f * sy), 2.2f * sx, StrokeCap.Round)
                drawLine(tint, Offset(9.5f * sx, 20f * sy), Offset(14.5f * sx, 20f * sy), 2.2f * sx, StrokeCap.Round)
                drawLine(tint, Offset(15.5f * sx, 9f * sy), Offset(20f * sx, 4.5f * sy), 2.2f * sx, StrokeCap.Round)
                drawLine(tint, Offset(16f * sx, 4.5f * sy), Offset(20f * sx, 4.5f * sy), 2.2f * sx, StrokeCap.Round)
                drawLine(tint, Offset(20f * sx, 4.5f * sy), Offset(20f * sx, 8.5f * sy), 2.2f * sx, StrokeCap.Round)
            }
        }
    }
}

/**
 * 3D floating + diagonal sheen borderless social link icon row (GitHub, Instagram, Website).
 */
@Composable
internal fun Social3DLinksRow(
    showGithub: Boolean,
    showInstagram: Boolean,
    showWebsite: Boolean,
    onGithubClick: () -> Unit,
    onInstagramClick: () -> Unit,
    onWebsiteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showGithub) {
            Social3DIconButton(
                type = ProfileSocialType.GITHUB,
                phaseOffset = 0f,
                onClick = onGithubClick
            )
        }
        if (showInstagram) {
            Social3DIconButton(
                type = ProfileSocialType.INSTAGRAM,
                phaseOffset = 0.25f,
                onClick = onInstagramClick
            )
        }
        if (showWebsite) {
            Social3DIconButton(
                type = ProfileSocialType.WEBSITE,
                phaseOffset = 0.5f,
                onClick = onWebsiteClick
            )
        }
    }
}

@Composable
internal fun Social3DIconButton(
    type: ProfileSocialType,
    phaseOffset: Float,
    onClick: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "social3d_$type")
    val floatProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = androidx.compose.animation.core.StartOffset((phaseOffset * 3800).roundToInt())
        ),
        label = "socialFloat"
    )
    val sheenProgress by transition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = androidx.compose.animation.core.StartOffset((phaseOffset * 2000).roundToInt())
        ),
        label = "socialSheen"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = tween(120),
        label = "socialPress"
    )

    val floatOffsetDp = -2.8.dp * floatProgress
    val contentDesc = when (type) {
        ProfileSocialType.GITHUB -> "GitHub"
        ProfileSocialType.INSTAGRAM -> "Instagram"
        ProfileSocialType.WEBSITE -> "Website"
    }

    Box(
        modifier = Modifier
            .size(38.dp)
            .offset(y = floatOffsetDp)
            .scale(pressScale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClickLabel = contentDesc,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val s = size.width / 240f
            drawSocial3DFullIcon(type = type, s = s, sheenProgress = sheenProgress)
        }
    }
}

private fun DrawScope.drawSocial3DFullIcon(
    type: ProfileSocialType,
    s: Float,
    sheenProgress: Float
) {
    val bodyRect = RoundRect(30f * s, 26f * s, 210f * s, 206f * s, CornerRadius(56f * s, 56f * s))
    val clipBody = Path().apply { addRoundRect(bodyRect) }

    when (type) {
        ProfileSocialType.GITHUB -> {
            drawOval(Color(0x52090D16), Offset(40f * s, 204f * s), Size(160f * s, 26f * s))
            drawRoundRect(
                color = Color(0xFF0A0D14),
                topLeft = Offset(30f * s, 38f * s),
                size = Size(180f * s, 180f * s),
                cornerRadius = CornerRadius(56f * s, 56f * s)
            )
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF3E4759), Color(0xFF212836), Color(0xFF0E121B)),
                    start = Offset(48f * s, 24f * s),
                    end = Offset(192f * s, 212f * s)
                ),
                topLeft = Offset(30f * s, 26f * s),
                size = Size(180f * s, 180f * s),
                cornerRadius = CornerRadius(56f * s, 56f * s)
            )
            drawRoundRect(
                color = Color(0x3DFFFFFF),
                topLeft = Offset(31f * s, 27f * s),
                size = Size(178f * s, 178f * s),
                cornerRadius = CornerRadius(55f * s, 55f * s),
                style = Stroke(width = 2.2f * s)
            )

            clipPath(clipBody) {
                val diagOffset = sheenProgress * 280f * s
                val sheenPath = Path().apply {
                    moveTo(diagOffset - 50f * s, 0f)
                    lineTo(diagOffset + 15f * s, 0f)
                    lineTo(diagOffset - 75f * s, 240f * s)
                    lineTo(diagOffset - 140f * s, 240f * s)
                    close()
                }
                drawPath(sheenPath, Color(0x28FFFFFF))

                val octocat = Path().apply {
                    moveTo(120f * s, 60f * s)
                    cubicTo(89.1f * s, 60f * s, 64f * s, 85.1f * s, 64f * s, 116f * s)
                    cubicTo(64f * s, 140.7f * s, 80.1f * s, 161.7f * s, 102.3f * s, 169.1f * s)
                    cubicTo(105.1f * s, 169.6f * s, 106.1f * s, 167.9f * s, 106.1f * s, 166.4f * s)
                    lineTo(106.1f * s, 157f * s)
                    cubicTo(90.5f * s, 160.4f * s, 87.2f * s, 150.4f * s, 87.2f * s, 150.4f * s)
                    cubicTo(84.7f * s, 143.9f * s, 81f * s, 142.2f * s, 81f * s, 142.2f * s)
                    cubicTo(75.9f * s, 138.7f * s, 81.4f * s, 138.8f * s, 81.4f * s, 138.8f * s)
                    cubicTo(87f * s, 139.2f * s, 90f * s, 144.6f * s, 90f * s, 144.6f * s)
                    cubicTo(95f * s, 153.2f * s, 103.1f * s, 150.7f * s, 106.3f * s, 149.3f * s)
                    cubicTo(106.8f * s, 145.7f * s, 108.3f * s, 143.2f * s, 109.9f * s, 141.8f * s)
                    cubicTo(97.5f * s, 140.4f * s, 84.4f * s, 135.6f * s, 84.4f * s, 114.2f * s)
                    cubicTo(84.4f * s, 108.1f * s, 86.6f * s, 103.1f * s, 90.2f * s, 99.2f * s)
                    cubicTo(89.6f * s, 97.8f * s, 87.7f * s, 92.1f * s, 90.8f * s, 84.4f * s)
                    cubicTo(90.8f * s, 84.4f * s, 95.5f * s, 82.9f * s, 106.2f * s, 90.1f * s)
                    cubicTo(110.7f * s, 88.9f * s, 115.4f * s, 88.3f * s, 120.2f * s, 88.3f * s)
                    cubicTo(125f * s, 88.3f * s, 129.7f * s, 88.9f * s, 134.2f * s, 90.1f * s)
                    cubicTo(144.9f * s, 82.9f * s, 149.6f * s, 84.4f * s, 149.6f * s, 84.4f * s)
                    cubicTo(152.7f * s, 92.1f * s, 150.8f * s, 97.8f * s, 150.2f * s, 99.2f * s)
                    cubicTo(153.8f * s, 103.1f * s, 156f * s, 108.1f * s, 156f * s, 114.2f * s)
                    cubicTo(156f * s, 135.7f * s, 142.9f * s, 140.3f * s, 130.4f * s, 141.7f * s)
                    cubicTo(132.4f * s, 143.4f * s, 134.2f * s, 146.9f * s, 134.2f * s, 152.1f * s)
                    lineTo(134.2f * s, 166.5f * s)
                    cubicTo(134.2f * s, 168f * s, 135.2f * s, 169.7f * s, 138f * s, 169.2f * s)
                    cubicTo(160.2f * s, 161.8f * s, 176.2f * s, 140.8f * s, 176.2f * s, 116.1f * s)
                    cubicTo(176f * s, 85.1f * s, 150.9f * s, 60f * s, 120f * s, 60f * s)
                    close()
                }
                drawPath(octocat, Color(0x8C000000))
                drawPath(octocat, Color(0xFFFDFEFF))
            }
        }
        ProfileSocialType.INSTAGRAM -> {
            drawOval(Color(0x47E1306C), Offset(40f * s, 204f * s), Size(160f * s, 26f * s))
            drawRoundRect(
                color = Color(0xFF833AB4),
                topLeft = Offset(30f * s, 38f * s),
                size = Size(180f * s, 180f * s),
                cornerRadius = CornerRadius(56f * s, 56f * s)
            )
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFED373), Color(0xFFF15245), Color(0xFFD92E7F), Color(0xFF9B36B7), Color(0xFF515ECF)),
                    center = Offset(72f * s, 198f * s),
                    radius = 220f * s
                ),
                topLeft = Offset(30f * s, 26f * s),
                size = Size(180f * s, 180f * s),
                cornerRadius = CornerRadius(56f * s, 56f * s)
            )
            drawRoundRect(
                color = Color(0x4DFFFFFF),
                topLeft = Offset(31f * s, 27f * s),
                size = Size(178f * s, 178f * s),
                cornerRadius = CornerRadius(55f * s, 55f * s),
                style = Stroke(width = 2.2f * s)
            )

            clipPath(clipBody) {
                val diagOffset = sheenProgress * 280f * s
                val sheenPath = Path().apply {
                    moveTo(diagOffset - 50f * s, 0f)
                    lineTo(diagOffset + 15f * s, 0f)
                    lineTo(diagOffset - 75f * s, 240f * s)
                    lineTo(diagOffset - 140f * s, 240f * s)
                    close()
                }
                drawPath(sheenPath, Color(0x2BFFFFFF))

                drawRoundRect(
                    color = Color(0x4D000000),
                    topLeft = Offset(66f * s, 68f * s),
                    size = Size(108f * s, 108f * s),
                    cornerRadius = CornerRadius(32f * s, 32f * s),
                    style = Stroke(width = 11f * s)
                )
                drawRoundRect(
                    brush = Brush.linearGradient(listOf(Color.White, Color(0xFFFFF4FA)), Offset(60f * s, 60f * s), Offset(180f * s, 180f * s)),
                    topLeft = Offset(66f * s, 62f * s),
                    size = Size(108f * s, 108f * s),
                    cornerRadius = CornerRadius(32f * s, 32f * s),
                    style = Stroke(width = 11f * s)
                )
                drawCircle(
                    color = Color.White,
                    radius = 25f * s,
                    center = Offset(120f * s, 116f * s),
                    style = Stroke(width = 11f * s)
                )
                drawCircle(color = Color.White, radius = 7.5f * s, center = Offset(152f * s, 84f * s))
            }
        }
        ProfileSocialType.WEBSITE -> {
            drawOval(Color(0x470064FA), Offset(40f * s, 204f * s), Size(160f * s, 26f * s))
            drawRoundRect(
                color = Color(0xFF003B99),
                topLeft = Offset(30f * s, 38f * s),
                size = Size(180f * s, 180f * s),
                cornerRadius = CornerRadius(56f * s, 56f * s)
            )
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF3DA2FF), Color(0xFF0064FA), Color(0xFF0040AD)),
                    start = Offset(48f * s, 24f * s),
                    end = Offset(192f * s, 212f * s)
                ),
                topLeft = Offset(30f * s, 26f * s),
                size = Size(180f * s, 180f * s),
                cornerRadius = CornerRadius(56f * s, 56f * s)
            )
            drawRoundRect(
                color = Color(0x4DFFFFFF),
                topLeft = Offset(31f * s, 27f * s),
                size = Size(178f * s, 178f * s),
                cornerRadius = CornerRadius(55f * s, 55f * s),
                style = Stroke(width = 2.2f * s)
            )

            clipPath(clipBody) {
                val diagOffset = sheenProgress * 280f * s
                val sheenPath = Path().apply {
                    moveTo(diagOffset - 50f * s, 0f)
                    lineTo(diagOffset + 15f * s, 0f)
                    lineTo(diagOffset - 75f * s, 240f * s)
                    lineTo(diagOffset - 140f * s, 240f * s)
                    close()
                }
                drawPath(sheenPath, Color(0x2EFFFFFF))

                val stroke = Stroke(width = 9.2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
                drawCircle(color = Color.White, radius = 46f * s, center = Offset(120f * s, 113f * s), style = stroke)
                drawLine(Color.White, Offset(76f * s, 113f * s), Offset(164f * s, 113f * s), 9.2f * s, cap = StrokeCap.Round)
                val meridian = Path().apply {
                    moveTo(120f * s, 67f * s)
                    cubicTo(135f * s, 85f * s, 135f * s, 141f * s, 120f * s, 159f * s)
                    cubicTo(105f * s, 141f * s, 105f * s, 85f * s, 120f * s, 67f * s)
                    close()
                }
                drawPath(meridian, Color.White, style = stroke)
                val topLat = Path().apply {
                    moveTo(84f * s, 88f * s)
                    cubicTo(104f * s, 95f * s, 136f * s, 95f * s, 156f * s, 88f * s)
                }
                val botLat = Path().apply {
                    moveTo(84f * s, 138f * s)
                    cubicTo(104f * s, 131f * s, 136f * s, 131f * s, 156f * s, 138f * s)
                }
                drawPath(topLat, Color.White, style = stroke)
                drawPath(botLat, Color.White, style = stroke)
            }
        }
    }
}

/**
 * Primary Oscellion electric-blue gradient pill button.
 */
@Composable
internal fun OscellionPrimaryGradientButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 43.dp,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1f,
        animationSpec = tween(110),
        label = "primaryGradScale"
    )

    Box(
        modifier = modifier
            .height(height)
            .scale(scale)
            .shadow(
                elevation = if (enabled) 10.dp else 0.dp,
                shape = CircleShape,
                ambientColor = OscellionProfilePalette.BlueAccent.copy(alpha = 0.44f),
                spotColor = OscellionProfilePalette.BlueAccent.copy(alpha = 0.44f)
            )
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    colors = if (enabled) {
                        listOf(
                            OscellionProfilePalette.BlueBright,
                            OscellionProfilePalette.BlueAccent,
                            OscellionProfilePalette.BlueGradEnd
                        )
                    } else {
                        listOf(Color(0xFF94A3B8), Color(0xFF64748B))
                    }
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            content = content
        )
    }
}

// ── Profile Content Tab Vector Icons (matching the exact reference strokes + Custom Heart-Shape Handshake) ──

@Composable
internal fun GridTabVectorIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val s = Stroke(width = 1.9f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawRoundRect(tint, Offset(3f * sx, 3f * sy), Size(7f * sx, 7f * sy), CornerRadius(1.2f * sx), style = s)
        drawRoundRect(tint, Offset(14f * sx, 3f * sy), Size(7f * sx, 7f * sy), CornerRadius(1.2f * sx), style = s)
        drawRoundRect(tint, Offset(14f * sx, 14f * sy), Size(7f * sx, 7f * sy), CornerRadius(1.2f * sx), style = s)
        drawRoundRect(tint, Offset(3f * sx, 14f * sy), Size(7f * sx, 7f * sy), CornerRadius(1.2f * sx), style = s)
    }
}

@Composable
internal fun ImageTabVectorIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val s = Stroke(width = 1.9f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawRoundRect(tint, Offset(3f * sx, 3f * sy), Size(18f * sx, 18f * sy), CornerRadius(2.5f * sx), style = s)
        drawCircle(tint, radius = 1.7f * sx, center = Offset(8.5f * sx, 8.5f * sy))
        val path = Path().apply {
            moveTo(21f * sx, 15f * sy)
            lineTo(16f * sx, 10f * sy)
            lineTo(5f * sx, 21f * sy)
        }
        drawPath(path, tint, style = s)
    }
}

@Composable
internal fun FlashBoltTabVectorIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val path = Path().apply {
            moveTo(13f * sx, 2f * sy)
            lineTo(3f * sx, 14f * sy)
            lineTo(12f * sx, 14f * sy)
            lineTo(11f * sx, 22f * sy)
            lineTo(21f * sx, 10f * sy)
            lineTo(12f * sx, 10f * sy)
            close()
        }
        drawPath(path, tint, style = Stroke(width = 1.9f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/**
 * Custom heart-shaped handshake icon crafted for Oscellion Remixes.
 */
@Composable
internal fun RemixHeartHandshakeVectorIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val s = Stroke(width = 1.9f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val p = Path().apply {
            moveTo(19f * sx, 14f * sy)
            cubicTo(20.49f * sx, 12.54f * sy, 22f * sx, 10.79f * sy, 22f * sx, 8.5f * sy)
            cubicTo(22f * sx, 5.42f * sx, 19.5f * sx, 3f * sy, 16.5f * sx, 3f * sy)
            cubicTo(14.74f * sx, 3f * sy, 13.5f * sx, 3.5f * sy, 12f * sx, 5f * sy)
            cubicTo(10.5f * sx, 3.5f * sy, 9.26f * sx, 3f * sy, 7.5f * sx, 3f * sy)
            cubicTo(4.42f * sx, 3f * sy, 2f * sx, 5.5f * sy, 2f * sx, 8.5f * sy)
            cubicTo(2f * sx, 10.8f * sy, 3.5f * sx, 12.55f * sy, 5f * sx, 14f * sy)
            lineTo(12f * sx, 21f * sy)
            lineTo(19f * sx, 14f * sy)
            close()
            moveTo(12f * sx, 5f * sy)
            lineTo(9.04f * sx, 7.96f * sy)
            cubicTo(8.16f * sx, 8.84f * sy, 8.16f * sx, 10.26f * sy, 9.04f * sx, 11.14f * sy)
            cubicTo(9.92f * sx, 12.02f * sy, 11.34f * sx, 12.02f * sy, 12.22f * sx, 11.14f * sy)
            lineTo(14.22f * sx, 9.14f * sy)
            cubicTo(15.1f * sx, 8.26f * sy, 16.52f * sx, 8.26f * sy, 17.4f * sx, 9.14f * sy)
            lineTo(20.36f * sx, 12.1f * sy)
        }
        drawPath(p, tint, style = s)
        drawLine(tint, Offset(18f * sx, 15f * sy), Offset(16f * sx, 13f * sy), 1.9f * sx, StrokeCap.Round)
        drawLine(tint, Offset(15f * sx, 18f * sy), Offset(13f * sx, 16f * sy), 1.9f * sx, StrokeCap.Round)
    }
}

@Composable
internal fun BookmarkTabVectorIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val p = Path().apply {
            moveTo(19f * sx, 21f * sy)
            lineTo(12f * sx, 16f * sy)
            lineTo(5f * sx, 21f * sy)
            lineTo(5f * sx, 5f * sy)
            cubicTo(5f * sx, 3.895f * sy, 5.895f * sx, 3f * sy, 7f * sx, 3f * sy)
            lineTo(17f * sx, 3f * sy)
            cubicTo(18.105f * sx, 3f * sy, 19f * sx, 3.895f * sy, 19f * sx, 5f * sy)
            close()
        }
        drawPath(p, tint, style = Stroke(width = 1.9f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/**
 * Clean 5-tab bar (All, Posts, Flash, Remixes, Saved) with custom vector icons, labels, and a smoothly
 * sliding electric-blue indicator pill.
 */
@Composable
internal fun OscellionProfileTabsBar(
    tabs: List<String>,
    selectedIndex: Int,
    isDark: Boolean,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (tabs.isEmpty()) return
    val safeIndex = selectedIndex.coerceIn(0, tabs.lastIndex)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(OscellionProfilePalette.sheetBg(isDark))
    ) {
        val tabWidth = maxWidth / tabs.size
        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * safeIndex + (tabWidth - 40.dp) / 2f,
            animationSpec = spring(
                dampingRatio = 0.85f,
                stiffness = Spring.StiffnessMedium
            ),
            label = "tabIndicatorSlide"
        )

        Column {
            HorizontalDivider(thickness = 1.dp, color = OscellionProfilePalette.hairline(isDark))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                tabs.forEachIndexed { idx, label ->
                    val isSelected = idx == safeIndex
                    val tint = if (isSelected) OscellionProfilePalette.BlueAccent else OscellionProfilePalette.inkMuted(isDark)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSelectTab(idx) }
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        when (label) {
                            "All" -> GridTabVectorIcon(tint = tint, modifier = Modifier.size(18.dp))
                            "Posts" -> ImageTabVectorIcon(tint = tint, modifier = Modifier.size(18.dp))
                            "Flash" -> FlashBoltTabVectorIcon(tint = tint, modifier = Modifier.size(18.dp))
                            "Remixes" -> RemixHeartHandshakeVectorIcon(tint = tint, modifier = Modifier.size(18.dp))
                            else -> BookmarkTabVectorIcon(tint = tint, modifier = Modifier.size(18.dp))
                        }
                        Text(
                            text = label,
                            fontSize = 10.5.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = tint,
                            maxLines = 1,
                            style = OscellionTightTextStyle
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .align(Alignment.BottomCenter)
                        .background(OscellionProfilePalette.hairline(isDark))
                )
                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(40.dp)
                        .height(2.5.dp)
                        .clip(CircleShape)
                        .background(OscellionProfilePalette.BlueAccent)
                )
            }
        }
    }
}

/**
 * Swipe-down-to-dismiss native Oscellion Bottom Slider used for Website links & QR Card.
 */
@Composable
internal fun OscellionBottomSlider(
    visible: Boolean,
    isDark: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val scope = rememberCoroutineScope()
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    val animatedDrag by animateFloatAsState(
        targetValue = dragOffsetPx,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 420f),
        label = "sliderDrag"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)),
        exit = fadeOut(tween(200)),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x66070B18))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(320, easing = FastOutSlowInEasing)
        ) + fadeIn(tween(220)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(240, easing = FastOutSlowInEasing)
        ) + fadeOut(tween(180)),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, animatedDrag.roundToInt()) }
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    )
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(OscellionProfilePalette.elevatedCard(isDark))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (dragOffsetPx > 120f) {
                                    onDismiss()
                                }
                                scope.launch { dragOffsetPx = 0f }
                            },
                            onDragCancel = {
                                scope.launch { dragOffsetPx = 0f }
                            },
                            onVerticalDrag = { _, dragAmount ->
                                dragOffsetPx = (dragOffsetPx + dragAmount).coerceAtLeast(0f)
                            }
                        )
                    }
                    .navigationBarsPadding()
                    .padding(bottom = 22.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(38.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF3A4458) else Color(0xFFCBD5E1))
                    )
                }

                content()
            }
        }
    }
}

/**
 * Vinyl Disc Canvas artwork (Starry Mountain gradient sleeve or track artwork + grooves + crimson center ring).
 */
@Composable
internal fun VinylDiscCanvas(
    isPlaying: Boolean,
    coverUrl: String = "",
    modifier: Modifier = Modifier
) {
    val spinTransition = rememberInfiniteTransition(label = "vinylSpin")
    val rotation by spinTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinylDeg"
    )
    var frozenRotation by remember { mutableFloatStateOf(0f) }
    if (isPlaying) {
        frozenRotation = rotation
    }

    Box(
        modifier = modifier
            .graphicsLayer { rotationZ = frozenRotation }
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (coverUrl.isNotBlank()) {
            AsyncImage(
                model = coverUrl,
                contentDescription = "Track Cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val r = minOf(w, h) / 2f

            if (coverUrl.isBlank()) {
                // Starry Night Purple-Mountain Vinyl Artwork (matching oscellion_updated_profile_screen.html)
                drawCircle(color = Color.Black, radius = r)
                drawCircle(Color.White, 2.5f * r / 64f, Offset(20f * w / 128f, 20f * h / 128f))
                drawCircle(Color.White, 2.5f * r / 64f, Offset(40f * w / 128f, 30f * h / 128f))
                drawCircle(Color.White, 2.5f * r / 64f, Offset(60f * w / 128f, 10f * h / 128f))
                drawCircle(Color.White, 2.5f * r / 64f, Offset(80f * w / 128f, 40f * h / 128f))
                drawCircle(Color.White, 2.5f * r / 64f, Offset(100f * w / 128f, 20f * h / 128f))
                drawCircle(Color.White, 2.5f * r / 64f, Offset(120f * w / 128f, 50f * h / 128f))
                drawCircle(Color(0x80FFFFFF), 10f * w / 128f, Offset(90f * w / 128f, 30f * h / 128f))
                drawCircle(Color.White, 8f * w / 128f, Offset(90f * w / 128f, 30f * h / 128f))

                val clipC = Path().apply { addOval(Rect(Offset.Zero, size)) }
                clipPath(clipC) {
                    val sx = w / 128f
                    val sy = h / 128f
                    val purpleDark = Color(0xFF800080)
                    val purpleMid = Color(0xFF9370DB)
                    val purpleDeep = Color(0xFF663399)

                    fun qPeak(baseY: Float, peakY: Float, fill: Color) {
                        val p = Path().apply {
                            moveTo(0f, baseY * sy)
                            quadraticBezierTo(32f * sx, peakY * sy, 64f * sx, baseY * sy)
                            quadraticBezierTo(96f * sx, peakY * sy, 128f * sx, baseY * sy)
                            lineTo(128f * sx, 128f * sy)
                            lineTo(0f, 128f * sy)
                            close()
                        }
                        drawPath(p, fill)
                        drawPath(p, Color.Black, style = Stroke(width = 1f * sx))
                    }
                    qPeak(128f, 32f, purpleDeep)
                    qPeak(128f, 48f, purpleMid)
                    qPeak(128f, 64f, purpleDark)

                    val leftSmall = Path().apply {
                        moveTo(0f, 128f * sy)
                        quadraticBezierTo(16f * sx, 64f * sy, 32f * sx, 128f * sy)
                        close()
                    }
                    drawPath(leftSmall, purpleDark)
                    val rightSmall = Path().apply {
                        moveTo(64f * sx, 128f * sy)
                        quadraticBezierTo(80f * sx, 64f * sy, 96f * sx, 128f * sy)
                        close()
                    }
                    drawPath(rightSmall, purpleMid)
                }
            } else {
                drawCircle(Color(0x28000000), radius = r)
            }

            drawCircle(
                color = Color(0xFFA1A1AA),
                radius = r - 1f,
                center = Offset(cx, cy),
                style = Stroke(width = maxOf(1.5f, r * 0.045f))
            )
            drawCircle(
                color = Color(0xFFA1A1AA),
                radius = r * 0.24f,
                center = Offset(cx, cy),
                style = Stroke(width = maxOf(1.2f, r * 0.04f))
            )
            drawCircle(
                color = Color.White,
                radius = r * 0.20f,
                center = Offset(cx, cy)
            )
        }
    }
}

/**
 * Floating draggable 50x50 vinyl disc widget wired cleanly to real user tracks / MusicRepository tracks
 * and Android MediaPlayer audio playback.
 */
@Composable
internal fun FloatingDraggableProfileMusicPlayer(
    profile: UserProfile?,
    works: List<DiscoverPost>,
    flashes: List<FlashVideo>,
    context: Context,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    var catalogTracks by remember { mutableStateOf<List<ProfilePlayableTrack>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            val res = MusicRepository.getInstance().fetchActiveCatalog(context)
            catalogTracks = res.mapNotNull { t ->
                val url = normalizeMusicStreamUrl(t.audioUrl)
                if (url.isBlank()) null
                else ProfilePlayableTrack(
                    id = t.id,
                    title = t.title.ifBlank { "Untitled Track" },
                    artist = t.artist.ifBlank { "Oscellion Audio" },
                    audioUrl = url,
                    coverUrl = t.coverUrl,
                    fallbackDurationSec = t.durationSec.coerceAtLeast(15)
                )
            }
        } catch (_: Exception) {
            // Catalog unavailable offline; profile-embedded tracks or fallback display will still work safely
        }
    }

    val tracks = remember(profile?.uid, works, flashes, catalogTracks) {
        val merged = LinkedHashMap<String, ProfilePlayableTrack>()
        works.forEach { w ->
            val url = normalizeMusicStreamUrl(w.musicAudioUrl)
            val title = w.musicTitle.orEmpty().trim()
            if (url.isNotBlank() && title.isNotBlank()) {
                val id = w.musicTrackId?.takeIf { it.isNotBlank() } ?: "work_${w.id}"
                merged.putIfAbsent(
                    id,
                    ProfilePlayableTrack(
                        id = id,
                        title = title,
                        artist = w.musicArtist?.takeIf { it.isNotBlank() } ?: (profile?.displayName ?: "Creator"),
                        audioUrl = url,
                        coverUrl = w.musicCoverUrl.orEmpty(),
                        fallbackDurationSec = w.musicDurationSec.takeIf { it > 0 } ?: 30
                    )
                )
            }
        }
        flashes.forEach { f ->
            val url = normalizeMusicStreamUrl(f.musicAudioUrl)
            val title = f.musicTitle.orEmpty().trim()
            if (url.isNotBlank() && title.isNotBlank()) {
                val id = f.musicTrackId?.takeIf { it.isNotBlank() } ?: "flash_${f.id}"
                merged.putIfAbsent(
                    id,
                    ProfilePlayableTrack(
                        id = id,
                        title = title,
                        artist = f.musicArtist?.takeIf { it.isNotBlank() } ?: (profile?.displayName ?: "Creator"),
                        audioUrl = url,
                        coverUrl = f.musicCoverUrl.orEmpty(),
                        fallbackDurationSec = f.musicDurationSec.takeIf { it > 0 } ?: 30
                    )
                )
            }
        }
        if (merged.isEmpty()) {
            catalogTracks.take(4).forEach { merged[it.id] = it }
        }
        merged.values.toList()
    }

    if (tracks.isEmpty()) return

    var trackIndex by remember { mutableIntStateOf(0) }
    val safeTrack = tracks[trackIndex.coerceIn(0, tracks.lastIndex)]
    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var isShuffle by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var panelExpanded by remember { mutableStateOf(false) }
    var currentPosMs by remember { mutableIntStateOf(0) }
    var totalDurMs by remember(safeTrack.id) { mutableIntStateOf(safeTrack.fallbackDurationSec * 1000) }
    var isPrepared by remember(safeTrack.id) { mutableStateOf(false) }

    fun pickNextIndex(step: Int): Int {
        if (tracks.size <= 1) return 0
        if (isShuffle) {
            val candidates = tracks.indices.filter { it != trackIndex }
            return candidates.randomOrNull() ?: ((trackIndex + step + tracks.size) % tracks.size)
        }
        return (trackIndex + step + tracks.size) % tracks.size
    }

    val mediaPlayer = remember { MediaPlayer() }

    DisposableEffect(Unit) {
        onDispose {
            try {
                if (mediaPlayer.isPlaying) mediaPlayer.stop()
                mediaPlayer.release()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(safeTrack.audioUrl, isPlaying) {
        if (!isPlaying) {
            try {
                if (mediaPlayer.isPlaying) mediaPlayer.pause()
            } catch (_: Exception) {}
            return@LaunchedEffect
        }
        if (safeTrack.audioUrl.isBlank()) {
            isPlaying = false
            return@LaunchedEffect
        }
        if (!isPrepared) {
            try {
                mediaPlayer.reset()
                mediaPlayer.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                mediaPlayer.setDataSource(safeTrack.audioUrl)
                mediaPlayer.setOnPreparedListener { mp ->
                    isPrepared = true
                    totalDurMs = mp.duration.takeIf { it > 0 } ?: (safeTrack.fallbackDurationSec * 1000)
                    val vol = if (isMuted) 0f else 1f
                    mp.setVolume(vol, vol)
                    if (isPlaying) mp.start()
                }
                mediaPlayer.setOnCompletionListener {
                    if (tracks.size > 1) {
                        isPrepared = false
                        trackIndex = pickNextIndex(1)
                    } else {
                        isPlaying = false
                        currentPosMs = 0
                    }
                }
                mediaPlayer.setOnErrorListener { _, _, _ ->
                    isPlaying = false
                    true
                }
                mediaPlayer.prepareAsync()
            } catch (_: Exception) {
                isPlaying = false
            }
        } else {
            try {
                val vol = if (isMuted) 0f else 1f
                mediaPlayer.setVolume(vol, vol)
                if (!mediaPlayer.isPlaying) mediaPlayer.start()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(isMuted) {
        try {
            val vol = if (isMuted) 0f else 1f
            mediaPlayer.setVolume(vol, vol)
        } catch (_: Exception) {}
    }

    LaunchedEffect(isPlaying, safeTrack.id) {
        while (isActive && isPlaying) {
            try {
                if (isPrepared && mediaPlayer.isPlaying) {
                    currentPosMs = mediaPlayer.currentPosition
                    val dur = mediaPlayer.duration
                    if (dur > 0) totalDurMs = dur
                }
            } catch (_: Exception) {}
            delay(350L)
        }
    }

    val density = LocalDensity.current
    val navBarBottomDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var offsetX by remember { mutableFloatStateOf(Float.NaN) }
    var offsetY by remember { mutableFloatStateOf(Float.NaN) }
    var isDragging by remember { mutableStateOf(false) }

    val discPx = with(density) { 50.dp.toPx() }
    val sidePadPx = with(density) { 14.dp.toPx() }
    val bottomPadPx = with(density) { (104.dp + navBarBottomDp).toPx() }
    val topLimitPx = with(density) { 86.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                val newSize = coords.size
                if (containerSize != newSize && newSize.width > 0 && newSize.height > 0) {
                    val wasUninit = offsetX.isNaN()
                    containerSize = newSize
                    if (wasUninit) {
                        offsetX = newSize.width - discPx - sidePadPx
                        offsetY = newSize.height - discPx - bottomPadPx
                    } else {
                        offsetX = offsetX.coerceIn(sidePadPx, newSize.width - discPx - sidePadPx)
                        offsetY = offsetY.coerceIn(topLimitPx, newSize.height - discPx - bottomPadPx)
                    }
                }
            }
    ) {
        if (!offsetX.isNaN() && containerSize.width > 0) {
            val animX by animateFloatAsState(
                targetValue = offsetX,
                animationSpec = if (isDragging) tween(0) else spring(dampingRatio = 0.78f, stiffness = 360f),
                label = "musicDragX"
            )
            val animY by animateFloatAsState(
                targetValue = offsetY,
                animationSpec = if (isDragging) tween(0) else spring(dampingRatio = 0.78f, stiffness = 360f),
                label = "musicDragY"
            )
            val isOnRightHalf = animX > containerSize.width / 2f

            AnimatedVisibility(
                visible = panelExpanded,
                enter = fadeIn(tween(180)) + slideInVertically { 18 },
                exit = fadeOut(tween(150)),
                modifier = Modifier
                    .offset {
                        val panelW = with(density) { 268.dp.toPx() }
                        val panelH = with(density) { 156.dp.toPx() }
                        val px = if (isOnRightHalf) {
                            (animX + discPx - panelW).coerceAtLeast(sidePadPx)
                        } else {
                            animX.coerceAtMost(containerSize.width - panelW - sidePadPx)
                        }
                        val py = if (animY - panelH - 12 > topLimitPx) {
                            animY - panelH - 12
                        } else {
                            animY + discPx + 12
                        }
                        IntOffset(px.roundToInt(), py.roundToInt())
                    }
            ) {
                ProfileMusicControlPanel(
                    tracks = tracks,
                    trackIndex = trackIndex,
                    track = safeTrack,
                    isPlaying = isPlaying,
                    isMuted = isMuted,
                    isShuffle = isShuffle,
                    showQueue = showQueue,
                    currentMs = currentPosMs,
                    totalMs = totalDurMs,
                    isDark = isDark,
                    onTogglePlay = { isPlaying = !isPlaying },
                    onPrev = {
                        isPrepared = false
                        currentPosMs = 0
                        trackIndex = pickNextIndex(-1)
                    },
                    onNext = {
                        isPrepared = false
                        currentPosMs = 0
                        trackIndex = pickNextIndex(1)
                    },
                    onSelectTrack = { idx ->
                        if (idx != trackIndex) {
                            isPrepared = false
                            currentPosMs = 0
                            trackIndex = idx
                        }
                        isPlaying = true
                        showQueue = false
                    },
                    onSeek = { ratio ->
                        val targetMs = (ratio * totalDurMs).roundToInt().coerceIn(0, totalDurMs)
                        currentPosMs = targetMs
                        try {
                            if (isPrepared) mediaPlayer.seekTo(targetMs)
                        } catch (_: Exception) {}
                    },
                    onToggleMute = { isMuted = !isMuted },
                    onToggleShuffle = { isShuffle = !isShuffle },
                    onToggleQueue = { showQueue = !showQueue },
                    onClose = { panelExpanded = false; showQueue = false }
                )
            }

            Box(
                modifier = Modifier
                    .offset { IntOffset(animX.roundToInt(), animY.roundToInt()) }
                    .size(50.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(
                            elevation = 12.dp,
                            shape = CircleShape,
                            ambientColor = Color(0x55070B18),
                            spotColor = Color(0x55070B18)
                        )
                        .clip(CircleShape)
                        .background(Color.Black)
                        .border(2.dp, if (isDark) Color(0xFF283247) else Color.White, CircleShape)
                        .pointerInput(containerSize) {
                            detectDragGestures(
                                onDragStart = { isDragging = true },
                                onDragEnd = {
                                    isDragging = false
                                    val mid = containerSize.width / 2f
                                    offsetX = if (offsetX + discPx / 2f < mid) {
                                        sidePadPx
                                    } else {
                                        containerSize.width - discPx - sidePadPx
                                    }
                                },
                                onDragCancel = { isDragging = false },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    offsetX = (offsetX + dragAmount.x).coerceIn(sidePadPx, containerSize.width - discPx - sidePadPx)
                                    offsetY = (offsetY + dragAmount.y).coerceIn(topLimitPx, containerSize.height - discPx - bottomPadPx)
                                }
                            )
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { panelExpanded = !panelExpanded },
                    contentAlignment = Alignment.Center
                ) {
                    VinylDiscCanvas(
                        isPlaying = isPlaying,
                        coverUrl = safeTrack.coverUrl,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(2.dp, Color(0xFFA1A1AA), CircleShape)
                    )
                }

                // Tiny Live Music Note Indicator Badge (Top-End) matching reference HTML
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .size(15.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) OscellionProfilePalette.BlueAccent else Color(0xFFA1A1AA))
                        .border(2.dp, if (isDark) Color(0xFF0D1017) else Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(7.5.dp)) {
                        val sx = size.width / 24f
                        val sy = size.height / 24f
                        val p = Path().apply {
                            moveTo(9f * sx, 18f * sy)
                            lineTo(9f * sx, 5f * sy)
                            lineTo(21f * sx, 3f * sy)
                            lineTo(21f * sx, 16f * sy)
                        }
                        drawPath(p, Color.White, style = Stroke(width = 2.8f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        drawCircle(Color.White, 3.2f * sx, Offset(6.5f * sx, 18f * sy))
                        drawCircle(Color.White, 3.2f * sx, Offset(18.5f * sx, 16f * sy))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileMusicControlPanel(
    tracks: List<ProfilePlayableTrack>,
    trackIndex: Int,
    track: ProfilePlayableTrack,
    isPlaying: Boolean,
    isMuted: Boolean,
    isShuffle: Boolean,
    showQueue: Boolean,
    currentMs: Int,
    totalMs: Int,
    isDark: Boolean,
    onTogglePlay: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSelectTrack: (Int) -> Unit,
    onSeek: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleQueue: () -> Unit,
    onClose: () -> Unit
) {
    val safeTotal = totalMs.coerceAtLeast(1000)
    val progress = (currentMs.toFloat() / safeTotal.toFloat()).coerceIn(0f, 1f)
    fun fmt(ms: Int): String {
        val s = (ms / 1000).coerceAtLeast(0)
        return "%d:%02d".format(s / 60, s % 60)
    }

    Column(
        modifier = Modifier
            .width(268.dp)
            .shadow(18.dp, RoundedCornerShape(18.dp), ambientColor = Color(0x33070B18), spotColor = Color(0x33070B18))
            .clip(RoundedCornerShape(18.dp))
            .background(OscellionProfilePalette.elevatedCard(isDark))
            .border(1.dp, OscellionProfilePalette.borderLight(isDark), RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .border(1.dp, OscellionProfilePalette.hairline(isDark), CircleShape)
            ) {
                VinylDiscCanvas(isPlaying = isPlaying, coverUrl = track.coverUrl, modifier = Modifier.fillMaxSize())
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, Color(0xFFA1A1AA), CircleShape)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = track.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = OscellionProfilePalette.inkPrimary(isDark),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = OscellionTightTextStyle,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    MusicEqBars(isPlaying = isPlaying)
                }
                Text(
                    text = track.artist,
                    fontSize = 11.5.sp,
                    color = OscellionProfilePalette.inkMuted(isDark),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = OscellionTightTextStyle
                )
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(OscellionProfilePalette.pillSurface(isDark))
                    .clickable(onClick = onToggleMute),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isMuted) Lucide.VolumeX else Lucide.Volume2,
                    contentDescription = if (isMuted) "Unmute" else "Mute",
                    tint = if (isMuted) OscellionProfilePalette.CrimsonTrack else OscellionProfilePalette.inkMuted(isDark),
                    modifier = Modifier.size(13.5.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(OscellionProfilePalette.pillSurface(isDark))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(11.dp)) {
                    drawLine(OscellionProfilePalette.inkMuted(isDark), Offset.Zero, Offset(size.width, size.height), 2f, StrokeCap.Round)
                    drawLine(OscellionProfilePalette.inkMuted(isDark), Offset(size.width, 0f), Offset(0f, size.height), 2f, StrokeCap.Round)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE0E7FF))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = fmt(currentMs),
                fontSize = 10.5.sp,
                fontFamily = FontFamily.Monospace,
                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF52525B)
            )
            Slider(
                value = progress,
                onValueChange = onSeek,
                modifier = Modifier
                    .weight(1f)
                    .height(20.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = OscellionProfilePalette.BlueAccent,
                    inactiveTrackColor = if (isDark) Color(0xFF334155) else Color(0xFFD1D5DB)
                )
            )
            Text(
                text = fmt(safeTotal),
                fontSize = 10.5.sp,
                fontFamily = FontFamily.Monospace,
                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF52525B)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onToggleShuffle),
                contentAlignment = Alignment.Center
            ) {
                val shuffleTint = if (isShuffle) OscellionProfilePalette.BlueAccent else Color(0xFF777777)
                Canvas(modifier = Modifier.size(17.dp)) {
                    val w = size.width
                    val h = size.height
                    val s = Stroke(width = 1.7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    if (isShuffle) {
                        drawLine(shuffleTint, Offset(w * 0.16f, h * 0.80f), Offset(w * 0.84f, h * 0.20f), strokeWidth = 1.7.dp.toPx(), cap = StrokeCap.Round)
                        drawLine(shuffleTint, Offset(w * 0.16f, h * 0.20f), Offset(w * 0.40f, h * 0.42f), strokeWidth = 1.7.dp.toPx(), cap = StrokeCap.Round)
                        drawLine(shuffleTint, Offset(w * 0.60f, h * 0.58f), Offset(w * 0.84f, h * 0.80f), strokeWidth = 1.7.dp.toPx(), cap = StrokeCap.Round)
                    } else {
                        val top = Path().apply {
                            moveTo(w * 0.14f, h * 0.46f)
                            lineTo(w * 0.14f, h * 0.28f)
                            lineTo(w * 0.86f, h * 0.28f)
                        }
                        val bot = Path().apply {
                            moveTo(w * 0.86f, h * 0.54f)
                            lineTo(w * 0.86f, h * 0.72f)
                            lineTo(w * 0.14f, h * 0.72f)
                        }
                        drawPath(top, shuffleTint, style = s)
                        drawPath(bot, shuffleTint, style = s)
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = onPrev)
                ) {
                    val sx = size.width / 24f
                    val sy = size.height / 24f
                    val tri = Path().apply {
                        moveTo(19f * sx, 20f * sy)
                        lineTo(9f * sx, 12f * sy)
                        lineTo(19f * sx, 4f * sy)
                        close()
                    }
                    drawPath(tri, OscellionProfilePalette.inkPrimary(isDark))
                    drawLine(OscellionProfilePalette.inkPrimary(isDark), Offset(5f * sx, 5f * sy), Offset(5f * sx, 19f * sy), 2.5f * sx, StrokeCap.Round)
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(OscellionProfilePalette.pillSurface(isDark))
                        .clickable(onClick = onTogglePlay),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(17.dp)) {
                        val sx = size.width / 24f
                        val sy = size.height / 24f
                        val playTint = OscellionProfilePalette.inkPrimary(isDark)
                        val stroke = Stroke(width = 2.4f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        if (isPlaying) {
                            drawRoundRect(playTint, Offset(6f * sx, 4.5f * sy), Size(4.2f * sx, 15f * sy), CornerRadius(1.2f * sx), style = stroke)
                            drawRoundRect(playTint, Offset(13.8f * sx, 4.5f * sy), Size(4.2f * sx, 15f * sy), CornerRadius(1.2f * sx), style = stroke)
                        } else {
                            val play = Path().apply {
                                moveTo(6.5f * sx, 4f * sy)
                                lineTo(19.5f * sx, 12f * sy)
                                lineTo(6.5f * sx, 20f * sy)
                                close()
                            }
                            drawPath(play, playTint, style = stroke)
                        }
                    }
                }

                Canvas(
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = onNext)
                ) {
                    val sx = size.width / 24f
                    val sy = size.height / 24f
                    val tri = Path().apply {
                        moveTo(5f * sx, 4f * sy)
                        lineTo(15f * sx, 12f * sy)
                        lineTo(5f * sx, 20f * sy)
                        close()
                    }
                    drawPath(tri, OscellionProfilePalette.inkPrimary(isDark))
                    drawLine(OscellionProfilePalette.inkPrimary(isDark), Offset(19f * sx, 5f * sy), Offset(19f * sx, 19f * sy), 2.5f * sx, StrokeCap.Round)
                }
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onToggleQueue),
                contentAlignment = Alignment.Center
            ) {
                val queueTint = if (showQueue) OscellionProfilePalette.BlueAccent else Color(0xFF777777)
                Canvas(modifier = Modifier.size(17.dp)) {
                    val w = size.width
                    val h = size.height
                    val sw = 1.7.dp.toPx()
                    listOf(0.26f, 0.50f, 0.74f).forEach { y ->
                        drawCircle(queueTint, radius = 1.2.dp.toPx(), center = Offset(w * 0.16f, h * y))
                        drawLine(queueTint, Offset(w * 0.34f, h * y), Offset(w * 0.88f, h * y), strokeWidth = sw, cap = StrokeCap.Round)
                    }
                }
            }
        }

        if (showQueue && tracks.size > 1) {
            HorizontalDivider(color = OscellionProfilePalette.hairline(isDark))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 128.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                tracks.forEachIndexed { idx, item ->
                    val active = idx == trackIndex
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (active) OscellionProfilePalette.pillSurface(isDark) else Color.Transparent)
                            .clickable { onSelectTrack(idx) }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                fontSize = 11.5.sp,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (active) OscellionProfilePalette.BlueAccent else OscellionProfilePalette.inkPrimary(isDark),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = OscellionTightTextStyle
                            )
                            Text(
                                text = item.artist,
                                fontSize = 10.sp,
                                color = OscellionProfilePalette.inkMuted(isDark),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = OscellionTightTextStyle
                            )
                        }
                        if (active) {
                            MusicEqBars(isPlaying = isPlaying)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicEqBars(isPlaying: Boolean) {
    val eqTransition = rememberInfiniteTransition(label = "eqBars")
    val b1 by eqTransition.animateFloat(
        initialValue = 0.32f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(420, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "eq1"
    )
    val b2 by eqTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(340, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "eq2"
    )
    val b3 by eqTransition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(tween(480, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "eq3"
    )
    Row(
        modifier = Modifier.height(11.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val h1 = if (isPlaying) b1 else 0.35f
        val h2 = if (isPlaying) b2 else 0.35f
        val h3 = if (isPlaying) b3 else 0.35f
        Box(Modifier.width(2.5.dp).height((11f * h1).dp).clip(CircleShape).background(OscellionProfilePalette.BlueAccent))
        Box(Modifier.width(2.5.dp).height((11f * h2).dp).clip(CircleShape).background(Color(0xFF38BDF8)))
        Box(Modifier.width(2.5.dp).height((11f * h3).dp).clip(CircleShape).background(OscellionProfilePalette.BlueAccent))
    }
}
