package com.chroma.studio.chat.ui.components


import android.graphics.BlurMaskFilter
import android.graphics.LinearGradient
import android.graphics.Paint as NativePaint
import android.graphics.Path as NativePath
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// ============================================================================
// APPLE BLUE GLASS DESIGN TOKENS (visionOS / iOS Translucent Blue Glass)
// ============================================================================

// Ambient stage gradient colors
private val StageCenterColor = Color(0xFFDBEAFE)
private val StageMidColor = Color(0xFFEFF6FF)
private val StageEdgeColor = Color(0xFFE2E8F0)

// Vibrant Apple Liquid Glass Blue Gradient Stops
private val GlassBlueTop = Color(0xFF3E91FF)      // Brilliant vibrant azure crown
private val GlassBlueUpperMid = Color(0xFF0381FE) // Core rich vibrant blue
private val GlassBlueLowerMid = Color(0xFF0072DE) // Saturated electric blue body
private val GlassBlueBottom = Color(0xFF0056D6)   // Dense vibrant sapphire base
private val GlassCyanCrown = Color(0x80B3E5FC)    // Top sky-blue radial light bloom
private val GlassBottomCaustic = Color(0x6040C4FF)// Internal bottom lens caustic glow

// Neutral Elevation Shadow (Zero outer blue glow)
private val GlassAmbientGlow = Color.Transparent  // Outer glow removed
private val GlassDepthShadow = Color(0x28000000)  // Crisp neutral contact shadow

// Specular Rim & Inner Refraction Highlights
private val SpecularTopHighlight = Color(0xF2FFFFFF)    // Razor-sharp top specular edge
private val UpperGlassSheen = Color(0x54FFFFFF)         // Upper volume gloss
private val LowerCyanRefraction = Color(0x8CAEE4FF)     // Bottom rim sky-blue light
private val InnerBottomDeepTint = Color(0x5E002266)     // Deep blue lower meniscus

// Interactive Action Item & Etched Divider Tokens
private val GlassActionPressedBg = Color(0x3DFFFFFF)    // Frosted inner glass pill highlight
private val GlassActionPressedBorder = Color(0x80FFFFFF)// Inner pill specular border
private val GlassContentShadow = Color(0x66002670)      // Crisp icon/label legibility shadow

// Dimensions
private val CollapsedWidthDp = 52.dp
private val ExpandedWidthDp = 210.dp
private val CapsuleHeightDp = 52.dp

// Spring Physics Constants
private const val STIFFNESS = 240f
private const val DAMPING = 16f
private const val PRESS_STIFFNESS = 320f
private const val PRESS_DAMPING = 18f

/**
 * Full-screen scrollable container hosting the centered Apple-style Blue Glass Capsule Button.
 * Scroll down to morph into "(icon) Group | (icon) Chat", scroll up to collapse into "+".
 */
@Composable
fun AppleGlassButtonScreen(
    onGroupClick: () -> Unit = {},
    onChatClick: () -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -6f && !isExpanded) {
                    isExpanded = true
                } else if (available.y > 6f && isExpanded) {
                    isExpanded = false
                }
                return Offset.Zero
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(StageCenterColor, StageMidColor, StageEdgeColor),
                    radius = 1200f
                )
            )
            .nestedScroll(nestedScrollConnection)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var lastY = down.position.y
                    do {
                        val event = awaitPointerEvent()
                        val currentY = event.changes.firstOrNull()?.position?.y ?: lastY
                        val deltaY = lastY - currentY
                        if (abs(deltaY) > 15f) {
                            isExpanded = deltaY > 0f
                            lastY = currentY
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Invisible scrollable area to enable native scroll gestures
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(1800.dp))
        }

        // Centered Apple Blue Glass Morphing Capsule Button
        AppleGlassMorphButton(
            isExpanded = isExpanded,
            onExpandChange = { isExpanded = it },
            onGroupClick = onGroupClick,
            onChatClick = onChatClick
        )
    }
}

/**
 * Apple-style Blue Glass Morphing Capsule Button with specular rim lighting,
 * internal light refraction, top gloss overlay, and ODE spring physics.
 */
@Composable
fun AppleGlassMorphButton(
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onGroupClick: () -> Unit,
    onChatClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current

    // 1. Morph Spring State (0f = Circle, 1f = Expanded Capsule)
    var position by remember { mutableFloatStateOf(if (isExpanded) 1f else 0f) }
    var velocity by remember { mutableFloatStateOf(0f) }
    val target = if (isExpanded) 1f else 0f

    // 2. Tactile Press Impulse Spring State
    var pressScale by remember { mutableFloatStateOf(1f) }
    var pressVelocity by remember { mutableFloatStateOf(0f) }
    val pressTarget = 1f

    var impulseTrigger by remember { mutableIntStateOf(0) }

    fun triggerPressBounce(impulse: Float) {
        pressVelocity += impulse * 18f
        impulseTrigger++
    }

    // Trigger subtle haptic tick when expansion state changes
    LaunchedEffect(isExpanded) {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    // 1:1 Frame-by-frame Spring Physics Loop
    LaunchedEffect(target, impulseTrigger) {
        var lastTimeNanos = withFrameNanos { it }
        var isSettled = false

        while (!isSettled) {
            withFrameNanos { nowNanos ->
                val dt = ((nowNanos - lastTimeNanos) / 1_000_000_000f).coerceIn(0f, 0.032f)
                lastTimeNanos = nowNanos

                // Morph Spring ODE
                val force = -STIFFNESS * (position - target)
                val dampingForce = -DAMPING * velocity
                val acceleration = force + dampingForce
                velocity += acceleration * dt
                position += velocity * dt

                // Tactile Press Spring ODE
                val pForce = -PRESS_STIFFNESS * (pressScale - pressTarget) - PRESS_DAMPING * pressVelocity
                pressVelocity += pForce * dt
                pressScale += pressVelocity * dt

                val isMorphSettled = abs(position - target) < 0.001f && abs(velocity) < 0.005f
                val isPressSettled = abs(pressScale - pressTarget) < 0.001f && abs(pressVelocity) < 0.005f

                if (isMorphSettled && isPressSettled) {
                    position = target
                    velocity = 0f
                    pressScale = pressTarget
                    pressVelocity = 0f
                    isSettled = true
                }
            }
        }
    }

    val currentWidthDp: Dp = remember(position) {
        val rawWidth = CollapsedWidthDp.value + (ExpandedWidthDp.value - CollapsedWidthDp.value) * position
        rawWidth.coerceAtLeast(CollapsedWidthDp.value).dp
    }

    // Fluid glass velocity stretch & squash
    val stretchX = 1f + (velocity * 0.018f) * pressScale
    val squashY = (1f - (velocity * 0.014f)) * pressScale

    // "+" Icon morph animations
    val plusProgress = (1f - position * 2.1f).coerceIn(0f, 1f)
    val plusRotate = position * 135f
    val plusScale = 0.25f + 0.75f * plusProgress

    // "(icon) Group | (icon) Chat" bloom animations
    val contentProgress = max(0f, (position - 0.3f) / 0.7f)
    val clampedOpacity = contentProgress.coerceIn(0f, 1f)
    val contentScale = 0.65f + 0.35f * contentProgress
    val slideOffsetPx = with(density) { ((1f - contentProgress) * 18f).dp.toPx() }
    val dividerScaleY = max(0.1f, contentProgress)

    Box(
        modifier = modifier
            .width(currentWidthDp)
            .height(CapsuleHeightDp)
            .graphicsLayer {
                scaleX = stretchX
                scaleY = squashY
            }
            // Custom Apple Liquid Glass Shader Surface with dynamic morph shimmer
            .appleBlueGlassSurface(morphProgress = position)
            .clip(CircleShape)
            .pointerInput(isExpanded) {
                if (!isExpanded) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        triggerPressBounce(-0.06f)
                        val up = waitForUpOrCancellation()
                        if (up != null) {
                            triggerPressBounce(-0.10f)
                            onExpandChange(true)
                        }
                    }
                }
            }
            .semantics {
                role = Role.Button
                contentDescription = if (isExpanded) "Group and Chat Actions" else "Expand Actions"
            },
        contentAlignment = Alignment.Center
    ) {
        // ====================================================================
        // COLLAPSED STATE: Circle "+" Icon
        // ====================================================================
        if (plusProgress > 0.001f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = plusProgress
                        rotationZ = plusRotate
                        scaleX = plusScale
                        scaleY = plusScale
                    },
                contentAlignment = Alignment.Center
            ) {
                PlusGlassIcon(modifier = Modifier.size(24.dp))
            }
        }

        // ====================================================================
        // EXPANDED STATE: "(icon) Group | (icon) Chat"
        // ====================================================================
        if (clampedOpacity > 0.001f) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .graphicsLayer {
                        alpha = clampedOpacity
                        scaleX = contentScale
                        scaleY = contentScale
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // (icon) Group
                GlassActionItem(
                    label = "Group",
                    modifier = Modifier.graphicsLayer {
                        translationX = slideOffsetPx
                    },
                    enabled = isExpanded,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        triggerPressBounce(-0.14f)
                        onGroupClick()
                    },
                    icon = { GroupGlassIcon(modifier = Modifier.size(18.dp)) }
                )

                // Etched Luminous Glass Divider "|"
                EtchedGlassDivider(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .graphicsLayer {
                            scaleY = dividerScaleY
                        }
                )

                // (icon) Chat
                GlassActionItem(
                    label = "Chat",
                    modifier = Modifier.graphicsLayer {
                        translationX = -slideOffsetPx
                    },
                    enabled = isExpanded,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        triggerPressBounce(-0.14f)
                        onChatClick()
                    },
                    icon = { ChatGlassIcon(modifier = Modifier.size(18.dp)) }
                )
            }
        }
    }
}

/**
 * Interactive glass pill item ("Group" or "Chat") inside the expanded capsule
 * with its own frosted glass highlight and specular rim when pressed.
 */
@Composable
private fun GlassActionItem(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val itemScale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 550f),
        label = "GlassActionItemScale"
    )
    val highlightAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 600f),
        label = "GlassActionHighlightAlpha"
    )

    val textShadowOffset = with(LocalDensity.current) { Offset(0f, 1.5.dp.toPx()) }
    val textShadowBlur = with(LocalDensity.current) { 4.dp.toPx() }

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = itemScale
                scaleY = itemScale
            }
            .clip(CircleShape)
            .drawWithCache {
                val radius = min(size.width, size.height) / 2f
                val cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
                val strokeWidth = 1.dp.toPx()
                val borderBrush = Brush.verticalGradient(
                    colors = listOf(
                        GlassActionPressedBorder,
                        Color.White.copy(alpha = 0.15f)
                    )
                )
                onDrawBehind {
                    if (highlightAlpha > 0.01f) {
                        drawRoundRect(
                            color = GlassActionPressedBg.copy(alpha = GlassActionPressedBg.alpha * highlightAlpha),
                            cornerRadius = cornerRadius
                        )
                        drawRoundRect(
                            brush = borderBrush,
                            cornerRadius = cornerRadius,
                            style = Stroke(width = strokeWidth),
                            alpha = highlightAlpha
                        )
                    }
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    isPressed = true
                    val up = waitForUpOrCancellation()
                    isPressed = false
                    if (up != null) {
                        up.consume()
                        onClick()
                    }
                }
            }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        icon()
        Text(
            text = label,
            color = Color.White,
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                letterSpacing = 0.3.sp,
                shadow = Shadow(
                    color = GlassContentShadow,
                    offset = textShadowOffset,
                    blurRadius = textShadowBlur
                )
            ),
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Apple-style Etched Frosted Glass Divider (`1.5.dp` x `28.dp`) with luminous center fade and soft glow.
 */
@Composable
private fun EtchedGlassDivider(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .width(1.5.dp)
            .height(20.dp)
    ) {
        val cornerRadius = size.width / 2f
        // Soft ambient glow behind the etched line
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    GlassCyanCrown.copy(alpha = 0.45f),
                    Color.Transparent
                )
            ),
            topLeft = Offset(-1.5.dp.toPx(), 0f),
            size = androidx.compose.ui.geometry.Size(size.width + 3.dp.toPx(), size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius * 2f, cornerRadius * 2f)
        )
        // Crisp etched core
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.10f),
                    Color.White.copy(alpha = 0.78f),
                    Color.White.copy(alpha = 0.10f)
                )
            ),
            size = size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
        )
    }
}

// ============================================================================
// CUSTOM APPLE BLUE GLASS RENDERER (Zero-Allocation Cached Shaders & Paths)
// ============================================================================

/**
 * Renders the multi-layered Apple Liquid Glass surface with zero allocations inside `onDrawWithContent`:
 * 1. Dual outer ambient blue glow & depth shadows
 * 2. Rich 4-stop Apple Blue vertical linear gradient + top sky-blue crown + bottom cyan caustic glow
 * 3. Dynamic specular light shimmer that sweeps across the glass during morphing
 * 4. Inner volume sheen, deep blue lower refraction, and curved top meniscus glass reflection
 * 5. Crisp 1.2dp gradient specular rim border
 */
private fun Modifier.appleBlueGlassSurface(
    morphProgress: Float
): Modifier = this.drawWithCache {
    val w = size.width
    val h = size.height
    // Dynamically clamp radius so spring undershoot (w < h) never distorts pill geometry
    val radiusPx = min(w, h) / 2f

    // Outer depth shadow: subtle clean contact shadow (no outer glow)
    val s2OffsetY = 6.dp.toPx()
    val s2Blur = 10.dp.toPx()
    val s2Spread = (-2).dp.toPx()

    val outerDepthPaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
        style = NativePaint.Style.FILL
        color = GlassDepthShadow.toArgb()
        maskFilter = BlurMaskFilter(s2Blur, BlurMaskFilter.Blur.NORMAL)
    }

    val pillRect = RectF(0f, 0f, w, h)
    val pillPath = NativePath().apply {
        addRoundRect(pillRect, radiusPx, radiusPx, NativePath.Direction.CW)
    }

    // Translucent Apple Blue 4-Stop Linear Gradient Fill
    val linearBluePaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
        style = NativePaint.Style.FILL
        shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(
                GlassBlueTop.toArgb(),
                GlassBlueUpperMid.toArgb(),
                GlassBlueLowerMid.toArgb(),
                GlassBlueBottom.toArgb()
            ),
            floatArrayOf(0f, 0.36f, 0.72f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    // Top Radial Sky-Blue Light Crown
    val radialCrownPaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
        style = NativePaint.Style.FILL
        shader = RadialGradient(
            w * 0.5f,
            0f,
            max(w * 0.68f, h),
            intArrayOf(
                GlassCyanCrown.toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.78f),
            Shader.TileMode.CLAMP
        )
    }

    // Bottom Caustic Lens Glow (simulates light focusing at the base of the curved glass)
    val bottomCausticPaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
        style = NativePaint.Style.FILL
        shader = RadialGradient(
            w * 0.5f,
            h * 1.05f,
            max(w * 0.55f, h * 0.75f),
            intArrayOf(
                GlassBottomCaustic.toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.72f),
            Shader.TileMode.CLAMP
        )
    }

    // Dynamic Specular Shimmer Sweep driven by morphProgress
    val shimmerCenterX = w * (0.18f + 0.64f * morphProgress.coerceIn(0f, 1.2f))
    val shimmerWidth = max(w * 0.42f, 48.dp.toPx())
    val shimmerPaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
        style = NativePaint.Style.FILL
        shader = LinearGradient(
            shimmerCenterX - shimmerWidth, 0f,
            shimmerCenterX + shimmerWidth, h,
            intArrayOf(
                Color.Transparent.toArgb(),
                Color(0x2EFFFFFF).toArgb(),
                Color(0x52C8ECFF).toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.45f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    // Pre-computed Boolean Difference Paths for True Inset Refractions
    val pad = 60.dp.toPx()
    val outerBoundsPath = NativePath().apply {
        addRect(-pad, -pad, w + pad, h + pad, NativePath.Direction.CW)
    }

    val bottomDepthInset = NativePath().apply {
        val hole = NativePath().apply {
            addRoundRect(
                RectF(0f, (-10).dp.toPx(), w, h - 10.dp.toPx()),
                radiusPx,
                radiusPx,
                NativePath.Direction.CW
            )
        }
        op(outerBoundsPath, hole, NativePath.Op.DIFFERENCE)
    }

    val upperSheenInset = NativePath().apply {
        val hole = NativePath().apply {
            addRoundRect(
                RectF(0f, 14.dp.toPx(), w, h + 14.dp.toPx()),
                radiusPx,
                radiusPx,
                NativePath.Direction.CW
            )
        }
        op(outerBoundsPath, hole, NativePath.Op.DIFFERENCE)
    }

    val topRimInset = NativePath().apply {
        val hole = NativePath().apply {
            addRoundRect(
                RectF(0f, 1.5.dp.toPx(), w, h + 1.5.dp.toPx()),
                radiusPx,
                radiusPx,
                NativePath.Direction.CW
            )
        }
        op(outerBoundsPath, hole, NativePath.Op.DIFFERENCE)
    }

    val bottomRimInset = NativePath().apply {
        val hole = NativePath().apply {
            addRoundRect(
                RectF(0f, (-1.8).dp.toPx(), w, h - 1.8.dp.toPx()),
                radiusPx,
                radiusPx,
                NativePath.Direction.CW
            )
        }
        op(outerBoundsPath, hole, NativePath.Op.DIFFERENCE)
    }

    val innerUpperSheenPaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
        style = NativePaint.Style.FILL
        color = UpperGlassSheen.toArgb()
        maskFilter = BlurMaskFilter(14.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
    }

    val innerBottomTintPaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
        style = NativePaint.Style.FILL
        color = InnerBottomDeepTint.toArgb()
        maskFilter = BlurMaskFilter(12.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
    }

    val topSpecularPaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
        style = NativePaint.Style.FILL
        color = SpecularTopHighlight.toArgb()
        maskFilter = BlurMaskFilter(1.2.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
    }

    val bottomRimPaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
        style = NativePaint.Style.FILL
        color = LowerCyanRefraction.toArgb()
        maskFilter = BlurMaskFilter(1.5.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
    }

    // Top-half curved meniscus glass glaze
    val glossInset = 1.2.dp.toPx()
    val glossPaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
        style = NativePaint.Style.FILL
        shader = LinearGradient(
            0f, glossInset, 0f, h * 0.50f,
            intArrayOf(
                Color(0x66FFFFFF).toArgb(),
                Color(0x18FFFFFF).toArgb(),
                Color.Transparent.toArgb()
            ),
            floatArrayOf(0f, 0.78f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    // Outer Specular Glass Border (1.2dp gradient stroke)
    val strokeWidthPx = 1.2.dp.toPx()
    val halfStroke = strokeWidthPx / 2f
    val borderPaint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
        style = NativePaint.Style.STROKE
        strokeWidth = strokeWidthPx
        shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(
                Color(0xD9FFFFFF).toArgb(),
                Color(0x52FFFFFF).toArgb(),
                Color(0x94AEE4FF).toArgb()
            ),
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    onDrawWithContent {
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas

            // 1. Outer Depth Shadow (clean neutral grounding, zero blue glow)
            nativeCanvas.drawRoundRect(
                -s2Spread,
                s2OffsetY - s2Spread,
                w + s2Spread,
                h + s2OffsetY + s2Spread,
                radiusPx,
                radiusPx,
                outerDepthPaint
            )

            // 3. Translucent Apple Blue Linear Gradient Fill
            nativeCanvas.drawPath(pillPath, linearBluePaint)

            // 4. Top Radial Sky-Blue Light Crown & Bottom Caustic Lens Glow
            nativeCanvas.drawPath(pillPath, radialCrownPaint)
            nativeCanvas.drawPath(pillPath, bottomCausticPaint)

            // 5. Clipped Internal Glass Refractions, Shimmer & Top Meniscus Gloss
            nativeCanvas.save()
            nativeCanvas.clipPath(pillPath)

            // 5a. Dynamic diagonal specular shimmer sweep
            nativeCanvas.drawPath(pillPath, shimmerPaint)

            // 5b. Lower dark-blue internal depth refraction
            nativeCanvas.drawPath(bottomDepthInset, innerBottomTintPaint)

            // 5c. Upper soft white glass volume sheen
            nativeCanvas.drawPath(upperSheenInset, innerUpperSheenPaint)

            // 5d. Top-half curved glass reflection glaze
            nativeCanvas.drawRoundRect(
                glossInset,
                glossInset,
                w - glossInset,
                h - glossInset,
                max(0f, radiusPx - glossInset),
                max(0f, radiusPx - glossInset),
                glossPaint
            )

            // 5e. Crisp top specular rim & bottom cyan refraction rim
            nativeCanvas.drawPath(topRimInset, topSpecularPaint)
            nativeCanvas.drawPath(bottomRimInset, bottomRimPaint)

            nativeCanvas.restore()

            // 6. Outer Specular Glass Border
            nativeCanvas.drawRoundRect(
                halfStroke,
                halfStroke,
                w - halfStroke,
                h - halfStroke,
                max(0f, radiusPx - halfStroke),
                max(0f, radiusPx - halfStroke),
                borderPaint
            )
        }

        // 7. Draw Foreground Icons & Labels
        drawContent()
    }
}

// ============================================================================
// APPLE GLASS VECTOR ICONS
// ============================================================================

@Composable
private fun PlusGlassIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidthPx = 2.8.dp.toPx() * (size.width / 24.dp.toPx())
        val shadowOffsetY = 1.5.dp.toPx()
        val scale = size.width / 24f

        // Subtle glass shadow pass
        drawLine(
            color = GlassContentShadow,
            start = Offset(12f * scale, 5f * scale + shadowOffsetY),
            end = Offset(12f * scale, 19f * scale + shadowOffsetY),
            strokeWidth = strokeWidthPx,
            cap = StrokeCap.Round
        )
        drawLine(
            color = GlassContentShadow,
            start = Offset(5f * scale, 12f * scale + shadowOffsetY),
            end = Offset(19f * scale, 12f * scale + shadowOffsetY),
            strokeWidth = strokeWidthPx,
            cap = StrokeCap.Round
        )

        // Foreground crisp white pass
        drawLine(
            color = Color.White,
            start = Offset(12f * scale, 5f * scale),
            end = Offset(12f * scale, 19f * scale),
            strokeWidth = strokeWidthPx,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White,
            start = Offset(5f * scale, 12f * scale),
            end = Offset(19f * scale, 12f * scale),
            strokeWidth = strokeWidthPx,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun GroupGlassIcon(modifier: Modifier = Modifier) {
    val svgPathData = remember {
        PathParser().parsePathString(
            "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2 " +
            "M9 3a4 4 0 1 0 0 8 4 4 0 1 0 0-8 " +
            "M22 21v-2a4 4 0 0 0-3-3.87 " +
            "M16 3.13a4 4 0 0 1 0 7.75"
        ).toPath()
    }

    GlassVectorIconCanvas(
        rawPath = svgPathData,
        modifier = modifier
    )
}

@Composable
private fun ChatGlassIcon(modifier: Modifier = Modifier) {
    val svgPathData = remember {
        PathParser().parsePathString(
            "M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 " +
            "8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 " +
            "8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"
        ).toPath()
    }

    GlassVectorIconCanvas(
        rawPath = svgPathData,
        modifier = modifier
    )
}

@Composable
private fun GlassVectorIconCanvas(
    rawPath: androidx.compose.ui.graphics.Path,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val scale = min(size.width, size.height) / 24f
        val strokeWidthPx = 2.3f * scale
        val shadowDy = 1.5.dp.toPx()

        val scaledMatrix = android.graphics.Matrix().apply {
            setScale(scale, scale)
        }
        val scaledPath = androidx.compose.ui.graphics.Path().apply {
            asAndroidPath().set(rawPath.asAndroidPath())
            asAndroidPath().transform(scaledMatrix)
        }

        val shadowPath = androidx.compose.ui.graphics.Path().apply {
            asAndroidPath().set(scaledPath.asAndroidPath())
            asAndroidPath().offset(0f, shadowDy)
        }

        drawPath(
            path = shadowPath,
            color = GlassContentShadow,
            style = Stroke(
                width = strokeWidthPx,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        drawPath(
            path = scaledPath,
            color = Color.White,
            style = Stroke(
                width = strokeWidthPx,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 732)
@Composable
private fun AppleGlassButtonScreenPreview() {
    AppleGlassButtonScreen()
}