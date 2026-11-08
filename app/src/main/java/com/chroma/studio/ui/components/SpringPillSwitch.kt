package com.chroma.studio.ui.components

import android.content.Context
import android.graphics.BlurMaskFilter
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Lucide
import kotlinx.coroutines.delay
import kotlin.math.*

private val isAppInDark: Boolean
    @Composable
    get() = LocalOscellionColors.current.isDark

// ============================================================================
// 1. SPRING PHYSICS CONFIGURATION & PRESETS
// ============================================================================

/**
 * Global/shared configuration for the second-order damped harmonic oscillator:
 * F = -k * (x - target) - c * v
 * a = F / m
 */
@Stable
class SpringPhysicsConfig(
    stiffness: Float = 520f,
    damping: Float = 26f,
    mass: Float = 1.0f,
    squishFactor: Float = 1.0f,
    timeScale: Float = 1.0f,
    hapticsEnabled: Boolean = true
) {
    var stiffness by mutableFloatStateOf(stiffness)
    var damping by mutableFloatStateOf(damping)
    var mass by mutableFloatStateOf(mass)
    var squishFactor by mutableFloatStateOf(squishFactor)
    var timeScale by mutableFloatStateOf(timeScale)
    var hapticsEnabled by mutableStateOf(hapticsEnabled)

    /** Damping ratio: zeta = c / (2 * sqrt(k * m)) */
    val dampingRatio: Float
        get() = damping / (2f * sqrt(max(1f, stiffness * mass)))

    fun applyPreset(preset: SpringPreset) {
        stiffness = preset.stiffness
        damping = preset.damping
        mass = preset.mass
        squishFactor = preset.squishFactor
    }
}

enum class SpringPreset(
    val title: String,
    val stiffness: Float,
    val damping: Float,
    val mass: Float,
    val squishFactor: Float
) {
    Snappy("iOS Snappy", stiffness = 520f, damping = 26f, mass = 1.0f, squishFactor = 1.0f),
    Bouncy("Underdamped Bouncy", stiffness = 620f, damping = 14f, mass = 1.0f, squishFactor = 1.35f),
    Liquid("Liquid Glass", stiffness = 340f, damping = 19f, mass = 1.4f, squishFactor = 1.65f),
    Critical("Critically Damped", stiffness = 500f, damping = 45f, mass = 1.0f, squishFactor = 0.75f)
}

// ============================================================================
// 2. GLOBAL APP SPRING PHYSICS MANAGER
// ============================================================================

/**
 * App-wide singleton that manages and persists customized spring physics settings.
 * All toggle buttons throughout ChromaStudio reference this configuration.
 */
object AppSpringPhysics {
    val globalConfig = SpringPhysicsConfig(
        stiffness = 520f,
        damping = 26f,
        mass = 1.0f,
        squishFactor = 1.0f,
        timeScale = 1.0f,
        hapticsEnabled = true
    )

    private const val PREFS_NAME = "spring_physics_preferences"

    fun init(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            globalConfig.stiffness = prefs.getFloat("stiffness", 520f)
            globalConfig.damping = prefs.getFloat("damping", 26f)
            globalConfig.mass = prefs.getFloat("mass", 1.0f)
            globalConfig.squishFactor = prefs.getFloat("squish", 1.0f)
            globalConfig.timeScale = prefs.getFloat("timeScale", 1.0f)
            globalConfig.hapticsEnabled = prefs.getBoolean("haptics", true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun save(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putFloat("stiffness", globalConfig.stiffness)
                .putFloat("damping", globalConfig.damping)
                .putFloat("mass", globalConfig.mass)
                .putFloat("squish", globalConfig.squishFactor)
                .putFloat("timeScale", globalConfig.timeScale)
                .putBoolean("haptics", globalConfig.hapticsEnabled)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun reset(context: Context) {
        globalConfig.applyPreset(SpringPreset.Snappy)
        globalConfig.mass = 1.0f
        globalConfig.timeScale = 1.0f
        globalConfig.hapticsEnabled = true
        save(context)
    }
}

// ============================================================================
// 3. COLOR PALETTES MATCHING REFERENCE SPECS
// ============================================================================

@Immutable
data class PillSwitchColors(
    val offTrackColor: Color,
    val onTrackColor: Color,
    val thumbColor: Color = Color.White,
    val ioOnIndicatorColor: Color = Color(0xFFFFF7F5),
    val ioOffIndicatorColor: Color = Color(0xFF962226)
) {
    companion object {
        /** ChromaStudio Brand Blue Theme (#0A84FF) */
        val Brand: PillSwitchColors
            @Composable
            get() = PillSwitchColors(
                offTrackColor = if (isAppInDark) Color(0xFF39393D) else Color(0xFFE9E9EA),
                onTrackColor = Color(0xFF0381FE),
                thumbColor = Color.White
            )

        /** Card 01: Indigo / Purple Accent (#E3E3E6 -> #6155F5) */
        val Indigo = PillSwitchColors(
            offTrackColor = Color(0xFFE3E3E6),
            onTrackColor = Color(0xFF6155F5),
            thumbColor = Color.White
        )

        /** Card 02: System Green Accent (#E3E3E6 -> #2AD158) */
        val SystemGreen = PillSwitchColors(
            offTrackColor = Color(0xFFE3E3E6),
            onTrackColor = Color(0xFF2AD158),
            thumbColor = Color.White
        )

        /** Card 03: HIG Anatomy & I/O Labels Spec (#F6565C -> #8C2522) */
        val HigSpecCoral = PillSwitchColors(
            offTrackColor = Color(0xFFF6565C),
            onTrackColor = Color(0xFF8C2522),
            thumbColor = Color(0xFFFFF9F8),
            ioOnIndicatorColor = Color(0xFFFFF7F5),
            ioOffIndicatorColor = Color(0xFF962226)
        )
    }
}

// ============================================================================
// 4. REAL-TIME SECOND-ORDER SPRING SOLVER STATE
// ============================================================================

/**
 * Holds the real-time physical state of an individual Pill Toggle Switch.
 * Runs a sub-stepped Semi-Implicit Euler integrator for unconditional stability
 * at high stiffness, supporting velocity squish, wall-overshoot compression,
 * tap/hold clay plumping, and hyperbolic tangent drag rubber-banding.
 */
@Stable
class SpringToggleState(
    initialChecked: Boolean,
    val label: String = "Switch"
) {
    val initialCheckedState: Boolean = initialChecked
    var checked by mutableStateOf(initialChecked)
        private set

    // Primary horizontal displacement spring state (0f = OFF, 1f = ON)
    var x by mutableFloatStateOf(if (initialChecked) 1f else 0f)
    var targetX by mutableFloatStateOf(if (initialChecked) 1f else 0f)
    var v by mutableFloatStateOf(0f)

    // Secondary press/hold zoom & plump spring state (0f = rest, 1f = pressed)
    var press by mutableFloatStateOf(0f)
    var targetPress by mutableFloatStateOf(0f)
    var pressV by mutableFloatStateOf(0f)

    // Pointer gesture tracking
    var isDragging by mutableStateOf(false)
    var dragStartX by mutableFloatStateOf(0f)

    /** Returns true while either the horizontal or press spring has remaining kinetic/potential energy */
    val isAnimating: Boolean
        get() = isDragging ||
                abs(x - targetX) > 0.0004f ||
                abs(v) > 0.004f ||
                abs(press - targetPress) > 0.001f ||
                abs(pressV) > 0.01f

    /** Trigger a tap/hold press down impulse (used by both the switch and its parent row) */
    fun onPressStart(impulse: Float = 10.5f) {
        targetPress = 1f
        pressV = max(pressV, impulse)
    }

    /** Release press hold back to rest */
    fun onPressRelease() {
        targetPress = 0f
    }

    /** Flip switch state with an optional kinetic kick impulse */
    fun toggle(
        withImpulse: Boolean = true,
        onStateChanged: ((Boolean) -> Unit)? = null
    ) {
        val next = !checked
        if (withImpulse) {
            pressV = max(pressV, 10.5f)
        }
        setCheckedState(
            newState = next,
            extraVelocity = if (withImpulse) (if (next) 3.2f else -3.2f) else 0f,
            onStateChanged = onStateChanged
        )
    }

    /** Set target checked state and inject velocity into the solver */
    fun setCheckedState(
        newState: Boolean,
        extraVelocity: Float = 0f,
        onStateChanged: ((Boolean) -> Unit)? = null
    ) {
        val changed = checked != newState
        checked = newState
        targetX = if (newState) 1f else 0f
        v += extraVelocity
        if (changed) {
            onStateChanged?.invoke(newState)
        }
    }

    /** Reset back to the reference card's initial state with a crisp bounce */
    fun resetToInitial(onStateChanged: ((Boolean) -> Unit)? = null) {
        pressV = 8.5f
        setCheckedState(
            newState = initialCheckedState,
            extraVelocity = if (initialCheckedState) 2.8f else -2.8f,
            onStateChanged = onStateChanged
        )
    }

    /**
     * Sub-stepped Semi-Implicit Euler integration step.
     * Called every frame via [withFrameNanos].
     */
    fun step(dtSeconds: Float, config: SpringPhysicsConfig) {
        val scaledDt = min(dtSeconds, 0.032f) * config.timeScale
        val subSteps = 4
        val h = scaledDt / subSteps

        // Tighten spring while dragging so thumb tracks finger closely with liquid lag
        val k = if (isDragging) 1100f else config.stiffness
        val c = if (isDragging) 58f else config.damping
        val m = max(0.2f, config.mass)

        for (i in 0 until subSteps) {
            // 1. Main horizontal displacement spring
            val forceX = -k * (x - targetX) - c * v
            val accX = forceX / m
            v += accX * h
            x += v * h

            // 2. Press/hold zoom spring (underdamped for lush tactile zoom pop)
            val forceP = -560f * (press - targetPress) - 25f * pressV
            pressV += forceP * h
            press += pressV * h
        }

        // Snap to exact rest when energy becomes negligible
        if (!isDragging && abs(x - targetX) < 0.0004f && abs(v) < 0.004f) {
            x = targetX
            v = 0f
        }
        if (abs(press - targetPress) < 0.001f && abs(pressV) < 0.01f) {
            press = targetPress
            pressV = 0f
        }
    }
}

@Composable
fun rememberSpringToggleState(
    initialChecked: Boolean,
    label: String = "Switch"
): SpringToggleState {
    return remember { SpringToggleState(initialChecked = initialChecked, label = label) }
}

// ============================================================================
// 5. CLAYMORPHIC INSET-ONLY KNOB RENDERER (CUSTOM COMPOSE CANVAS)
// ============================================================================

/**
 * Renders the exact porcelain surface gradient and 4-layer CSS `inset` shadows
 * inside the pill thumb without any outer drop shadow:
 *  - Base porcelain surface gradient (`170deg`, white 95% -> #F1F3F9 55%)
 *  - Layer 1: `inset 0 3.5dp 5dp rgba(255, 255, 255, 1.0)` (top specular highlight)
 *  - Layer 2: `inset 0 -4dp 7.5dp rgba(15, 23, 42, 0.16)` (bottom clay bevel, deepens with press)
 *  - Layer 3: `inset -2dp -2dp 5dp rgba(15, 23, 42, 0.09)` (bottom-right contour)
 *  - Layer 4: `inset 0 0 0 1dp rgba(15, 23, 42, 0.04)` (crisp inner rim)
 */
fun Modifier.clayPillThumb(
    baseColor: Color,
    clayDepth: Float = 1f,
    clayDepthProvider: (() -> Float)? = null
): Modifier = this.drawWithCache {
    val radius = size.height / 2f
    val cornerRadius = CornerRadius(radius, radius)

    // Porcelain surface gradient
    val surfaceGradient = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.95f),
            Color(0xFFF1F3F9).copy(alpha = 0.55f)
        ),
        start = Offset(size.width * 0.2f, 0f),
        end = Offset(size.width * 0.8f, size.height)
    )

    // Specular top highlight gradient (100% GPU shader)
    val topSpecularGradient = Brush.verticalGradient(
        0.0f to Color.White.copy(alpha = 0.90f),
        0.4f to Color.White.copy(alpha = 0.15f),
        1.0f to Color.Transparent
    )

    val strokeWidth = 1.dp.toPx()

    onDrawBehind {
        val currentDepth = clayDepthProvider?.invoke() ?: clayDepth
        val bottomBevelGradient = Brush.verticalGradient(
            0.0f to Color.Transparent,
            0.6f to Color(0xFF0F172A).copy(alpha = 0.04f * currentDepth.coerceIn(0.5f, 2.5f)),
            1.0f to Color(0xFF0F172A).copy(alpha = 0.16f * currentDepth.coerceIn(0.5f, 2.5f))
        )

        // 1. Draw base solid color + porcelain gradient
        drawRoundRect(color = baseColor, cornerRadius = cornerRadius)
        drawRoundRect(brush = surfaceGradient, cornerRadius = cornerRadius)

        // 2. Specular top highlight & bottom clay bevel
        drawRoundRect(brush = topSpecularGradient, cornerRadius = cornerRadius)
        drawRoundRect(brush = bottomBevelGradient, cornerRadius = cornerRadius)

        // 3. Crisp inner rim contour
        drawRoundRect(
            color = Color(0xFF0F172A).copy(alpha = 0.04f),
            topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
            size = Size(size.width - strokeWidth, size.height - strokeWidth),
            cornerRadius = cornerRadius,
            style = Stroke(width = strokeWidth)
        )
    }
}

// ============================================================================
// 6. PIXEL-PERFECT SPRING PILL SWITCH COMPOSABLE
// ============================================================================

/**
 * Universal Wide-Pill Toggle Switch with real-time second-order spring physics,
 * claymorphic porcelain thumb, knob zoom plumping on tap/hold, drag rubber-banding,
 * velocity squish, and boundary overshoot.
 *
 * Fully scalable across standard settings rows (default 52dp x 31dp) and showcase sizes (98dp x 45dp).
 * 120 FPS pure hardware GPU accelerated with zero recompositions during motion.
 */
@Composable
fun SpringPillSwitch(
    state: SpringToggleState,
    modifier: Modifier = Modifier,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    colors: PillSwitchColors = PillSwitchColors.Brand,
    config: SpringPhysicsConfig = AppSpringPhysics.globalConfig,
    trackWidth: Dp = 52.dp,
    trackHeight: Dp = 31.dp,
    showIoLabels: Boolean = false,
    onActivate: ((SpringToggleState) -> Unit)? = null
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val isDark = isAppInDark

    // Geometry calculations adapt dynamically to trackWidth and trackHeight
    val paddingDp = (trackHeight * 0.07f).coerceIn(1.5.dp, 3.dp)
    val thumbHeightDp = trackHeight - paddingDp * 2
    val innerTrackWidthDp = trackWidth - paddingDp * 2
    val baseThumbWidthDp = if (trackWidth <= 64.dp) thumbHeightDp else innerTrackWidthDp * (61f / 92f)
    val maxTravelDp = innerTrackWidthDp - baseThumbWidthDp
    val maxTravelPx = with(density) { maxTravelDp.toPx() }

    val scaleFactor = (trackHeight.value / 45f).coerceIn(0.4f, 2.5f)

    // Haptic trigger helper with SDK safety
    fun triggerHaptic(isTurnedOn: Boolean) {
        if (!config.hapticsEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val feedbackConstant = if (isTurnedOn) {
                    HapticFeedbackConstants.CONFIRM
                } else {
                    HapticFeedbackConstants.CLOCK_TICK
                }
                if (!view.performHapticFeedback(feedbackConstant)) {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                }
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            }
        } catch (_: Exception) {}
    }

    // High efficiency frame-synced physics loop (runs strictly when spring is active)
    LaunchedEffect(state, config) {
        snapshotFlow { state.isAnimating }
            .collect { animating ->
                if (animating) {
                    var lastFrameNanos = withFrameNanos { it }
                    while (state.isAnimating) {
                        withFrameNanos { nowNanos ->
                            val dt = ((nowNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0.001f, 0.064f)
                            lastFrameNanos = nowNanos
                            state.step(dt, config)
                        }
                    }
                }
            }
    }

    Box(
        modifier = modifier
            .zIndex(1f)
            .graphicsLayer {
                val squish = config.squishFactor
                val clampedX = state.x.coerceIn(0f, 1f)
                val overX = state.x - clampedX
                val trackNudgeXDp = (overX * (3.4f * scaleFactor) * squish).dp
                val pressAmount = max(-0.12f, state.press)
                val trackScale = 1f + max(0f, pressAmount) * 0.025f * squish

                this.translationX = trackNudgeXDp.toPx()
                this.scaleX = trackScale
                this.scaleY = trackScale
            }
            .size(width = trackWidth, height = trackHeight)
            .drawBehind {
                val clampedX = state.x.coerceIn(0f, 1f)
                val trackColor = lerp(colors.offTrackColor, colors.onTrackColor, clampedX)
                val cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)

                // 1. Base track pill
                drawRoundRect(
                    color = trackColor,
                    cornerRadius = cornerRadius
                )


            }
            .semantics {
                role = Role.Switch
                toggleableState = ToggleableState(state.checked)
                contentDescription = state.label
            }
            .pointerInput(state, maxTravelPx) {
                val touchSlopPx = viewConfiguration.touchSlop.coerceIn(
                    with(density) { 6.dp.toPx() },
                    with(density) { 14.dp.toPx() }
                )
                val dragSlopPx = with(density) { 3.dp.toPx() }

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downTime = down.uptimeMillis
                    onActivate?.invoke(state)
                    state.onPressStart(impulse = 8.5f)
                    state.isDragging = false
                    state.dragStartX = state.x

                    val velocityTracker = VelocityTracker()
                    velocityTracker.addPosition(down.uptimeMillis, down.position)

                    var totalDxPx = 0f
                    var totalDyPx = 0f
                    var pointerId = down.id
                    var lastChange: PointerInputChange? = null
                    var isCancelled = false

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull { it.id == pointerId }
                            ?: event.changes.firstOrNull()
                            ?: break

                        pointerId = change.id
                        lastChange = change

                        if (!change.pressed) {
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            break
                        }

                        val stepDx = change.positionChange().x
                        val stepDy = change.positionChange().y
                        totalDxPx += stepDx
                        totalDyPx += stepDy
                        velocityTracker.addPosition(change.uptimeMillis, change.position)

                        // Avoid initiating horizontal drag if user is scrolling a vertical parent list
                        if (!state.isDragging && !isCancelled) {
                            if (abs(totalDyPx) > touchSlopPx && abs(totalDyPx) > abs(totalDxPx) * 1.3f) {
                                isCancelled = true
                                state.onPressRelease()
                            } else if (abs(totalDxPx) > dragSlopPx && abs(totalDxPx) > abs(totalDyPx)) {
                                state.isDragging = true
                            }
                        }

                        if (state.isDragging) {
                            change.consume()
                            var rawNorm = state.dragStartX + (totalDxPx / maxTravelPx.coerceAtLeast(1f))

                            // Smooth hyperbolic tangent rubber-banding past [0, 1] bounds
                            if (rawNorm < 0f) {
                                rawNorm = -tanh(abs(rawNorm) * 1.4f) * 0.22f
                            } else if (rawNorm > 1f) {
                                rawNorm = 1f + tanh((rawNorm - 1f) * 1.4f) * 0.22f
                            }
                            state.targetX = rawNorm
                        }
                    }

                    // Pointer Released
                    state.onPressRelease()
                    val gestureDuration = (lastChange?.uptimeMillis ?: downTime) - downTime
                    val releaseVelPxPerSec = velocityTracker.calculateVelocity().x
                    val releaseVelNorm = releaseVelPxPerSec / maxTravelPx.coerceAtLeast(1f)

                    if (!isCancelled) {
                        lastChange?.consume()

                        // Determine whether this interaction is a tap or a drag/swipe:
                        val isTap = !state.isDragging || (gestureDuration < 280L && abs(totalDxPx) < maxTravelPx * 0.4f)

                        if (isTap) {
                            // Instant 1-TAP toggle!
                            val nextState = !state.checked
                            val impulse = if (nextState) 3.2f else -3.2f
                            triggerHaptic(nextState)
                            try {
                                SoundManager.playReaction()
                            } catch (_: Exception) {}
                            state.setCheckedState(nextState, extraVelocity = impulse, onStateChanged = onCheckedChange)
                        } else {
                            // Deliberate drag / flick:
                            val nextState = when {
                                releaseVelPxPerSec > 200f -> true // flicked right (ON)
                                releaseVelPxPerSec < -200f -> false // flicked left (OFF)
                                else -> {
                                    val projectedEnd = state.x + releaseVelNorm * 0.12f
                                    projectedEnd >= 0.5f
                                }
                            }
                            val extraVel = if (nextState == state.checked) 0f else (if (nextState) 3.2f else -3.2f)
                            triggerHaptic(nextState)
                            try {
                                SoundManager.playReaction()
                            } catch (_: Exception) {}
                            state.setCheckedState(nextState, extraVelocity = extraVel, onStateChanged = onCheckedChange)
                        }
                    }

                    state.isDragging = false
                }
            }
            .padding(paddingDp)
    ) {
        // Optional HIG Spec I / O Accessibility Indicators
        if (showIoLabels) {
            // "I" On Indicator
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (trackWidth * 0.16f))
                    .graphicsLayer {
                        val onOpacity = ((state.x - 0.25f) / 0.65f).coerceIn(0f, 1f)
                        alpha = onOpacity
                        val s = 0.75f + 0.25f * onOpacity
                        this.scaleX = s
                        this.scaleY = s
                    }
                    .size(width = 2.dp, height = trackHeight * 0.35f)
                    .background(color = colors.ioOnIndicatorColor, shape = RoundedCornerShape(2.dp))
            )

            // "O" Off Indicator
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = -(trackWidth * 0.12f))
                    .graphicsLayer {
                        val offOpacity = ((0.75f - state.x) / 0.65f).coerceIn(0f, 1f)
                        alpha = offOpacity
                        val s = 0.75f + 0.25f * offOpacity
                        this.scaleX = s
                        this.scaleY = s
                    }
                    .size(trackHeight * 0.35f)
                    .border(
                        width = 2.dp,
                        color = colors.ioOffIndicatorColor,
                        shape = CircleShape
                    )
            )
        }

        // Claymorphic Pill Thumb (GPU hardware rendered with zero per-frame recomposition)
        Box(
            modifier = Modifier
                .offset {
                    val squish = config.squishFactor
                    val intensity = max(0.35f, squish)
                    val pressAmount = max(-0.12f, state.press)
                    val pressWidenDp = max(0f, pressAmount) * (11f * scaleFactor) * intensity
                    val speedStretchDp = min(9f * scaleFactor, abs(state.v) * 0.9f * scaleFactor) * squish
                    val overshootCompressDp = when {
                        state.x < 0f -> min(8f * scaleFactor, abs(state.x) * 18f * scaleFactor) * squish
                        state.x > 1f -> min(8f * scaleFactor, (state.x - 1f) * 18f * scaleFactor) * squish
                        else -> 0f
                    }
                    val currentWidthDp = (baseThumbWidthDp.value + pressWidenDp + speedStretchDp - overshootCompressDp).dp
                    val availableTravelDp = innerTrackWidthDp - currentWidthDp
                    val clampedX = state.x.coerceIn(0f, 1f)
                    val overX = state.x - clampedX
                    val translateXDp = availableTravelDp * clampedX + (overX * (10f * scaleFactor)).dp

                    IntOffset(x = translateXDp.roundToPx(), y = 0)
                }
                .size(width = baseThumbWidthDp, height = thumbHeightDp)
                .graphicsLayer {
                    val squish = config.squishFactor
                    val intensity = max(0.35f, squish)
                    val pressAmount = max(-0.12f, state.press)
                    val pressWidenDp = max(0f, pressAmount) * (11f * scaleFactor) * intensity
                    val knobZoomScale = 1f + pressAmount * 0.28f * intensity
                    val speedStretchDp = min(9f * scaleFactor, abs(state.v) * 0.9f * scaleFactor) * squish
                    val overshootCompressDp = when {
                        state.x < 0f -> min(8f * scaleFactor, abs(state.x) * 18f * scaleFactor) * squish
                        state.x > 1f -> min(8f * scaleFactor, (state.x - 1f) * 18f * scaleFactor) * squish
                        else -> 0f
                    }
                    val motionDeformRatio = (speedStretchDp - overshootCompressDp) / baseThumbWidthDp.value
                    val widthRatio = (baseThumbWidthDp.value + pressWidenDp + speedStretchDp - overshootCompressDp) / baseThumbWidthDp.value

                    this.scaleX = knobZoomScale * widthRatio
                    this.scaleY = knobZoomScale / sqrt(max(0.25f, 1f + motionDeformRatio * 0.32f))
                    this.transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                }
                .shadow(
                    elevation = (2.dp * scaleFactor).coerceAtLeast(1.dp),
                    shape = CircleShape,
                    spotColor = Color.Black.copy(alpha = 0.20f),
                    ambientColor = Color.Black.copy(alpha = 0.10f)
                )
                .clayPillThumb(
                    baseColor = colors.thumbColor,
                    clayDepthProvider = { (1f + max(0f, state.press) * 0.45f) * scaleFactor }
                )
        )
    }
}

/**
 * Value-driven overload for standard Compose [checked] and [onCheckedChange] usage.
 * Powered by Apple Blue Liquid Glass design with authentic iOS liquid thumb stretch.
 */
@Composable
fun SpringPillSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    colors: PillSwitchColors = PillSwitchColors.Brand,
    config: SpringPhysicsConfig = AppSpringPhysics.globalConfig,
    trackWidth: Dp = 52.dp,
    trackHeight: Dp = 31.dp,
    showIoLabels: Boolean = false
) {
    AppleSwitch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        trackWidth = trackWidth,
        trackHeight = trackHeight
    )
}

// ============================================================================
// 7. FULL SPRING PHYSICS LAB & SHOWCASE SCREEN (MATCHING EXACT HTML LAB)
// ============================================================================

/**
 * Exact implementation of the Spring Physics Lab from spring_physics_pill_toggle_switch.html:
 *  - Real-time live oscilloscope telemetry curve (x(t), v(t), zeta)
 *  - 4 Spring presets: iOS Snappy, Underdamped Bouncy, Liquid Glass, Critically Damped
 *  - 4 Dynamic sliders: Stiffness (k), Damping (c), Mass (m), Zoom & Squish
 *  - Quick action controls: Bounce All, Slow-Mo 0.25x, Sound/Haptics toggle, Reset
 *  - 3 Exact reference spec cards (Card 01 Indigo, Card 02 System Green, Card 03 HIG Coral)
 *  - Card 05: Live In-App ChromaStudio Brand Blue (#0A84FF) interactive preview
 *  - Real-time persistent state auto-saved to SharedPreferences for all switches in the app
 */
@Composable
fun SpringToggleShowcaseScreen(
    onBack: (() -> Unit)? = null
) {
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark
    val context = LocalContext.current
    val config = AppSpringPhysics.globalConfig
    val view = LocalView.current

    val swIndigo1 = rememberSpringToggleState(initialChecked = false, label = "Indigo Top")
    val swIndigo2 = rememberSpringToggleState(initialChecked = true, label = "Indigo Bottom")
    val swGreen1 = rememberSpringToggleState(initialChecked = false, label = "Green Top")
    val swGreen2 = rememberSpringToggleState(initialChecked = true, label = "Green Bottom")
    val swSpec1 = rememberSpringToggleState(initialChecked = true, label = "Spec Top (ON)")
    val swSpec2 = rememberSpringToggleState(initialChecked = false, label = "Spec Bottom (OFF)")
    val swBrand = rememberSpringToggleState(initialChecked = true, label = "Brand Blue")

    val allToggles = remember {
        listOf(swIndigo1, swIndigo2, swGreen1, swGreen2, swSpec1, swSpec2, swBrand)
    }

    var activeToggle by remember { mutableStateOf(swIndigo2) }
    var selectedPreset by remember { mutableStateOf<SpringPreset?>(SpringPreset.Snappy) }

    // Live Oscilloscope History (180 samples matching HTML)
    val history = remember { mutableStateListOf<Float>().apply { repeat(180) { add(1f) } } }

    LaunchedEffect(activeToggle) {
        while (true) {
            withFrameNanos {
                history.add(activeToggle.x)
                if (history.size > 180) {
                    history.removeAt(0)
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = if (isDark) Color(0xFF0B0C10) else Color(0xFFF4F4F6)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Sticky Navigation Bar if onBack is provided
            if (onBack != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                SoundManager.playReaction()
                                onBack()
                            }
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.ArrowLeft,
                            contentDescription = "Back",
                            tint = if (isDark) Color.White else Color(0xFF0F172A),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Settings",
                            color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B),
                            fontSize = 14.sp
                        )
                    }

                    Text(
                        text = "Toggle Switch Lab",
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "Apply",
                        color = Color(0xFF818CF8),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                AppSpringPhysics.save(context)
                                SoundManager.playReaction()
                                onBack()
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f), thickness = 0.5.dp)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header & Quick Action Bar
                ShowcaseHeader(
                    config = config,
                    onBounceAll = {
                        allToggles.forEachIndexed { index, t ->
                            t.toggle(withImpulse = true)
                        }
                        if (config.hapticsEnabled) {
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                } else {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                }
                            } catch (_: Exception) {}
                        }
                        try {
                            SoundManager.playReaction()
                        } catch (_: Exception) {}
                    },
                    onResetAll = {
                        allToggles.forEach { it.resetToInitial() }
                        AppSpringPhysics.reset(context)
                        selectedPreset = SpringPreset.Snappy
                    },
                    onConfigChanged = {
                        AppSpringPhysics.save(context)
                    }
                )

                // CARD 04: Spring Physics Lab & Live Telemetry (Placed First for Fast Tweaking)
                ReferenceCardSection(
                    title = "04 — Spring Physics Lab & Live Telemetry",
                    hexSubtitle = "Active: ${activeToggle.label}",
                    subtitleColor = Color(0xFF818CF8)
                ) {
                    SpringPhysicsLabCard(
                        config = config,
                        activeToggle = activeToggle,
                        selectedPreset = selectedPreset,
                        history = history,
                        onSelectPreset = { preset ->
                            selectedPreset = preset
                            config.applyPreset(preset)
                            AppSpringPhysics.save(context)
                            activeToggle.toggle(withImpulse = true)
                        },
                        onValuesChanged = {
                            selectedPreset = null // custom values
                            AppSpringPhysics.save(context)
                        }
                    )
                }

                // CARD 05: In-App Brand Live Test Card (Brand Blue #0A84FF)
                ReferenceCardSection(
                    title = "05 — In-App Brand Settings Switch",
                    hexSubtitle = "Unified Blue: #0A84FF",
                    subtitleColor = Color(0xFF38BDF8)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(color = Color(0xFF18181B), shape = RoundedCornerShape(28.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), shape = RoundedCornerShape(28.dp))
                            .padding(horizontal = 20.dp, vertical = 18.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    activeToggle = swBrand
                                    swBrand.toggle(withImpulse = true)
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Allow High-Res Studio Remix",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Previewing live spring motion with unified #0A84FF",
                                    color = Color(0xFFA1A1AA),
                                    fontSize = 12.sp
                                )
                            }

                            SpringPillSwitch(
                                state = swBrand,
                                colors = PillSwitchColors.Brand,
                                config = config,
                                trackWidth = 56.dp,
                                trackHeight = 28.dp,
                                onActivate = { activeToggle = it }
                            )
                        }
                    }
                }

                // CARD 01: Indigo Accent Variant
                ReferenceCardSection(
                    title = "01 — Indigo Accent Variant",
                    hexSubtitle = "#E3E3E6 → #6155F5"
                ) {
                    LightGroupedToggleCard(
                        topState = swIndigo1,
                        bottomState = swIndigo2,
                        colors = PillSwitchColors.Indigo,
                        config = config,
                        onActivate = { activeToggle = it }
                    )
                }

                // CARD 02: System Green Variant
                ReferenceCardSection(
                    title = "02 — System Green Variant",
                    hexSubtitle = "#E3E3E6 → #2AD158"
                ) {
                    LightGroupedToggleCard(
                        topState = swGreen1,
                        bottomState = swGreen2,
                        colors = PillSwitchColors.SystemGreen,
                        config = config,
                        onActivate = { activeToggle = it }
                    )
                }

                // CARD 03: HIG Anatomy & I/O Labels Spec Card
                ReferenceCardSection(
                    title = "03 — HIG Anatomy & I/O Labels Spec",
                    hexSubtitle = "#F6565C → #8C2522"
                ) {
                    HigCoralSpecCard(
                        topState = swSpec1,
                        bottomState = swSpec2,
                        config = config,
                        onActivate = { activeToggle = it }
                    )
                }

                Spacer(Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun ShowcaseHeader(
    config: SpringPhysicsConfig,
    onBounceAll: () -> Unit,
    onResetAll: () -> Unit,
    onConfigChanged: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Status Badge
        Row(
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.06f), CircleShape)
                .padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(Color(0xFF818CF8), CircleShape)
            )
            Text(
                text = "Real-Time Second-Order Damped Spring Solver",
                color = Color(0xFFA5B4FC),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Text(
            text = "Wide-Pill Toggle Switch & Spring Physics",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.5).sp
        )

        Text(
            text = "Click, press & hold, or drag & flick any switch below. Features elastic overshoot, press plumping, and velocity squish.",
            color = Color(0xFFA1A1AA),
            fontSize = 13.sp,
            lineHeight = 18.sp
        )

        // Action Buttons Row (Matching exact HTML header actions)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onBounceAll,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 13.dp, vertical = 8.dp)
            ) {
                Text("Bounce All", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            val isSlowMo = config.timeScale < 1f
            OutlinedButton(
                onClick = {
                    config.timeScale = if (isSlowMo) 1.0f else 0.25f
                    onConfigChanged()
                },
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White.copy(alpha = if (isSlowMo) 0.14f else 0.06f),
                    contentColor = Color(0xFFE4E4E7)
                ),
                contentPadding = PaddingValues(horizontal = 11.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(
                            if (isSlowMo) Color(0xFFFBBF24) else Color(0xFF71717A),
                            CircleShape
                        )
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isSlowMo) "Slow-Mo: 0.25x" else "Slow-Mo: OFF",
                    fontSize = 12.sp
                )
            }

            OutlinedButton(
                onClick = {
                    config.hapticsEnabled = !config.hapticsEnabled
                    onConfigChanged()
                },
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White.copy(alpha = if (config.hapticsEnabled) 0.10f else 0.04f),
                    contentColor = Color(0xFFE4E4E7)
                ),
                contentPadding = PaddingValues(horizontal = 11.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(
                            if (config.hapticsEnabled) Color(0xFF34D399) else Color(0xFF71717A),
                            CircleShape
                        )
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (config.hapticsEnabled) "Haptic: ON" else "Haptic: OFF",
                    fontSize = 12.sp
                )
            }

            OutlinedButton(
                onClick = onResetAll,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White.copy(alpha = 0.06f),
                    contentColor = Color(0xFFE4E4E7)
                ),
                contentPadding = PaddingValues(horizontal = 11.dp, vertical = 8.dp)
            ) {
                Text("Reset", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ReferenceCardSection(
    title: String,
    hexSubtitle: String,
    subtitleColor: Color = Color(0xFFA1A1AA),
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Color(0xFFD4D4D8),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = hexSubtitle,
                color = subtitleColor,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        content()
    }
}

@Composable
fun LightGroupedToggleCard(
    topState: SpringToggleState,
    bottomState: SpringToggleState,
    colors: PillSwitchColors,
    config: SpringPhysicsConfig,
    onActivate: (SpringToggleState) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color(0xFFF2F2F7), shape = RoundedCornerShape(42.dp))
            .padding(horizontal = 26.dp, vertical = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Color.White, shape = RoundedCornerShape(30.dp))
                .padding(horizontal = 26.dp, vertical = 4.dp)
        ) {
            GroupedToggleRow(
                title = "Title",
                state = topState,
                colors = colors,
                config = config,
                onActivate = onActivate
            )

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = Color(0xFFCECED2)
            )

            GroupedToggleRow(
                title = "Title",
                state = bottomState,
                colors = colors,
                config = config,
                onActivate = onActivate
            )
        }
    }
}

@Composable
private fun GroupedToggleRow(
    title: String,
    state: SpringToggleState,
    colors: PillSwitchColors,
    config: SpringPhysicsConfig,
    onActivate: (SpringToggleState) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onActivate(state)
                state.toggle(withImpulse = true)
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color.Black,
            fontSize = 25.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = (-0.5).sp
        )

        SpringPillSwitch(
            state = state,
            colors = colors,
            config = config,
            trackWidth = 98.dp,
            trackHeight = 45.dp,
            onActivate = onActivate
        )
    }
}

@Composable
fun HigCoralSpecCard(
    topState: SpringToggleState,
    bottomState: SpringToggleState,
    config: SpringPhysicsConfig,
    onActivate: (SpringToggleState) -> Unit
) {
    val coralGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFF7A45),
            Color(0xFFFA665E),
            Color(0xFFF25A7B)
        ),
        start = Offset(0f, 0f),
        end = Offset(1000f, 650f)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(brush = coralGradient, shape = RoundedCornerShape(42.dp))
            .padding(horizontal = 30.dp, vertical = 34.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SpecToggleRow(
                label = "Label",
                state = topState,
                config = config,
                showDimensionCallouts = true,
                onActivate = onActivate
            )

            SpecToggleRow(
                label = "Label",
                state = bottomState,
                config = config,
                showDimensionCallouts = false,
                onActivate = onActivate
            )
        }
    }
}

@Composable
private fun SpecToggleRow(
    label: String,
    state: SpringToggleState,
    config: SpringPhysicsConfig,
    showDimensionCallouts: Boolean,
    onActivate: (SpringToggleState) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onActivate(state)
                state.toggle(withImpulse = true)
            }
            .padding(end = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(155.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Text(
                text = label,
                color = Color(0xFF8D2323),
                fontSize = 26.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.padding(start = 18.dp, bottom = 2.dp)
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
            ) {
                drawLine(
                    color = Color(0xFFA52222).copy(alpha = 0.48f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                )
            }
        }

        Box(contentAlignment = Alignment.Center) {
            if (showDimensionCallouts) {
                Canvas(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-16).dp)
                        .width(98.dp)
                        .height(11.dp)
                ) {
                    val stroke = 1.5.dp.toPx()
                    val color = Color(0xFFD83A3A)
                    val midY = size.height / 2f
                    drawLine(color, Offset(0f, 0f), Offset(0f, size.height), stroke)
                    drawLine(color, Offset(0f, midY), Offset(size.width, midY), stroke)
                    drawLine(color, Offset(size.width, 0f), Offset(size.width, size.height), stroke)
                }

                Canvas(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 18.dp)
                        .width(11.dp)
                        .height(45.dp)
                ) {
                    val stroke = 1.5.dp.toPx()
                    val color = Color(0xFFD83A3A)
                    val midX = size.width / 2f
                    drawLine(color, Offset(0f, 0f), Offset(size.width, 0f), stroke)
                    drawLine(color, Offset(midX, 0f), Offset(midX, size.height), stroke)
                    drawLine(color, Offset(0f, size.height), Offset(size.width, size.height), stroke)
                }
            }

            SpringPillSwitch(
                state = state,
                colors = PillSwitchColors.HigSpecCoral,
                config = config,
                trackWidth = 98.dp,
                trackHeight = 45.dp,
                showIoLabels = true,
                onActivate = onActivate
            )
        }
    }
}

@Composable
private fun SpringPhysicsLabCard(
    config: SpringPhysicsConfig,
    activeToggle: SpringToggleState,
    selectedPreset: SpringPreset?,
    history: List<Float>,
    onSelectPreset: (SpringPreset) -> Unit,
    onValuesChanged: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF18181B), RoundedCornerShape(36.dp))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(36.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Presets Selector (Matching HTML)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SpringPreset.entries.forEach { preset ->
                val isSelected = preset == selectedPreset
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (isSelected) Color(0xFF6366F1).copy(alpha = 0.25f)
                            else Color.White.copy(alpha = 0.05f)
                        )
                        .clickable { onSelectPreset(preset) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = preset.title,
                        color = if (isSelected) Color(0xFFA5B4FC) else Color(0xFFD4D4D8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 4 Sliders matching HTML exact ranges
        SpringSliderControl(
            label = "Stiffness (k)",
            valueText = "${config.stiffness.roundToInt()} N/m",
            value = config.stiffness,
            valueRange = 150f..1000f,
            onValueChange = {
                config.stiffness = it
                onValuesChanged()
            }
        )

        SpringSliderControl(
            label = "Damping (c)",
            valueText = "${config.damping.roundToInt()} Ns/m",
            value = config.damping,
            valueRange = 8f..65f,
            onValueChange = {
                config.damping = it
                onValuesChanged()
            }
        )

        SpringSliderControl(
            label = "Mass (m)",
            valueText = "${"%.1f".format(config.mass)} kg",
            value = config.mass,
            valueRange = 0.4f..3.0f,
            onValueChange = {
                config.mass = it
                onValuesChanged()
            }
        )

        SpringSliderControl(
            label = "Zoom & Squish Intensity",
            valueText = "${(config.squishFactor * 100).roundToInt()}%",
            value = config.squishFactor * 100f,
            valueRange = 0f..200f,
            onValueChange = {
                config.squishFactor = it / 100f
                onValuesChanged()
            }
        )

        // Real-Time Oscilloscope Telemetry Panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DISPLACEMENT x(t)",
                    color = Color(0xFFA1A1AA),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "x: ${"%.3f".format(activeToggle.x)}",
                        color = Color(0xFF34D399),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "v: ${"%.2f".format(activeToggle.v)}/s",
                        color = Color(0xFF818CF8),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "ζ: ${"%.2f".format(config.dampingRatio)}",
                        color = Color(0xFFFCD34D),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
            ) {
                fun mapY(v: Float): Float {
                    val norm = (v - (-0.35f)) / 1.7f
                    return size.height - norm * size.height
                }

                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                listOf(0f, 1f).forEach { refVal ->
                    val y = mapY(refVal)
                    drawLine(
                        color = Color.White.copy(alpha = 0.12f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect
                    )
                }

                if (history.isNotEmpty()) {
                    val wavePath = Path()
                    val count = history.size
                    for (i in 0 until count) {
                        val px = (i.toFloat() / (count - 1).coerceAtLeast(1)) * size.width
                        val py = mapY(history[i])
                        if (i == 0) wavePath.moveTo(px, py) else wavePath.lineTo(px, py)
                    }
                    drawPath(
                        path = wavePath,
                        color = Color(0xFF818CF8),
                        style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    val lastY = mapY(history.last())
                    drawCircle(
                        color = Color(0xFF34D399),
                        radius = 4.dp.toPx(),
                        center = Offset(size.width - 4.dp.toPx(), lastY)
                    )
                }
            }
        }
    }
}

@Composable
private fun SpringSliderControl(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = Color(0xFFA1A1AA), fontSize = 12.sp)
            Text(
                text = valueText,
                color = Color(0xFFA5B4FC),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color(0xFF6366F1),
                inactiveTrackColor = Color.White.copy(alpha = 0.14f)
            )
        )
    }
}
