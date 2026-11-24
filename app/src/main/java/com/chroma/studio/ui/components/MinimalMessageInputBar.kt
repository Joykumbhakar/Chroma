package com.chroma.studio.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.ui.theme.LocalOscellionColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ============================================================================
// Design Tokens (Exact match to Tailwind Neutral & Blue palette)
// ============================================================================
private val Neutral50 = Color(0xFFFAFAFA)
private val Neutral300 = Color(0xFFD4D4D4)
private val Neutral400 = Color(0xFFA3A3A3)
private val Neutral900 = Color(0xFF171717)
private val Blue600 = Color(0xFF2563EB)
private val Blue700 = Color(0xFF1D4ED8)

// Custom cubic-bezier curves matching the CSS transitions
private val SmoothEaseOut = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
private val LaunchEaseIn = CubicBezierEasing(0.32f, 0f, 0.67f, 0f)

/**
 * Deterministic multi-stage animation states for the custom filled send icon.
 */
private enum class SendAnimStage {
    IDLE,
    PULLBACK,
    LAUNCH,
    CONFIRM,
    RETURN
}

/**
 * Clean two-component message bar featuring:
 * 1. Profile avatar + animated underline input field
 * 2. Background-free custom filled origami paper-plane animated send button
 *
 * @param onSendMessage Invoked with the trimmed message text when the user sends.
 * @param modifier Optional modifier for outer layout sizing/positioning.
 * @param placeholder Placeholder text shown when the input is empty.
 * @param text Optional hoisted text string. If null, manages its own internal state.
 * @param onTextChange Optional callback invoked when the text changes.
 * @param avatarContent Optional custom composable slot for the user's avatar. If null, uses the Classic Mac avatar.
 */
@Composable
fun MinimalMessageInputBar(
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Write a message...",
    text: String? = null,
    onTextChange: ((String) -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    avatarContent: (@Composable (ringColor: Color) -> Unit)? = null
) {
    var internalText by rememberSaveable { mutableStateOf("") }
    val effectiveText = text ?: internalText
    fun updateText(newText: String) {
        if (text != null && onTextChange != null) {
            onTextChange(newText)
        } else {
            internalText = newText
        }
    }

    var isFocused by remember { mutableStateOf(false) }
    var animStage by remember { mutableStateOf(SendAnimStage.IDLE) }

    val isAnimating = animStage != SendAnimStage.IDLE
    val hasText = effectiveText.trim().isNotEmpty()
    val isInputActive = isFocused || hasText

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val isDark = LocalOscellionColors.current.isDark
    val textColor = if (isDark) Color(0xFFF1F5F9) else Neutral900
    val placeholderColor = if (isDark) Color(0xFF94A3B8) else Neutral400
    val inactiveUnderlineColor = if (isDark) Color(0x33FFFFFF) else Neutral300
    val disabledIconColor = if (isDark) Color(0x4DFFFFFF) else Neutral300

    // --- Animated states for Component 1 (Avatar ring & active underline) ---
    val avatarRingColor by animateColorAsState(
        targetValue = if (isInputActive) Blue600.copy(alpha = 0.25f) else Color.Transparent,
        animationSpec = tween(durationMillis = 300),
        label = "avatarRingColor"
    )

    val underlineScaleX by animateFloatAsState(
        targetValue = if (isInputActive) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = SmoothEaseOut),
        label = "underlineScaleX"
    )

    // --- Animatables for Component 2 (Multi-stage send icon choreography) ---
    val dartOffsetX = remember { Animatable(0f) }
    val dartOffsetY = remember { Animatable(0f) }
    val dartScale = remember { Animatable(1f) }
    val dartAlpha = remember { Animatable(1f) }

    val streakProgress = remember { Animatable(0f) }

    val checkScale = remember { Animatable(0.75f) }
    val checkAlpha = remember { Animatable(0f) }
    val checkDrawProgress = remember { Animatable(0f) }

    // Trigger the 4-stage send animation sequence
    fun triggerSend() {
        val messageToSend = effectiveText.trim()
        if (isAnimating || messageToSend.isEmpty()) return

        // Clear input immediately and deliver callback
        updateText("")
        onSendMessage(messageToSend)

        scope.launch {
            try {
                // --- STAGE 1A: Subtle Pullback Anticipation (0ms - 120ms) ---
                animStage = SendAnimStage.PULLBACK
                val pullbackX = with(density) { (-4).dp.toPx() }
                val pullbackY = with(density) { 4.dp.toPx() }

                val p1 = launch {
                    dartOffsetX.animateTo(pullbackX, tween(120, easing = SmoothEaseOut))
                }
                val p2 = launch {
                    dartOffsetY.animateTo(pullbackY, tween(120, easing = SmoothEaseOut))
                }
                val p3 = launch {
                    dartScale.animateTo(0.9f, tween(120, easing = SmoothEaseOut))
                }
                p1.join(); p2.join(); p3.join()

                // --- STAGE 1B: Diagonal Launch + Thrust Streaks (120ms - 480ms) ---
                animStage = SendAnimStage.LAUNCH
                val launchX = with(density) { 36.dp.toPx() }
                val launchY = with(density) { (-36).dp.toPx() }

                streakProgress.snapTo(0f)
                launch {
                    streakProgress.animateTo(1f, tween(420, easing = SmoothEaseOut))
                }
                launch {
                    dartOffsetX.animateTo(launchX, tween(380, easing = LaunchEaseIn))
                }
                launch {
                    dartOffsetY.animateTo(launchY, tween(380, easing = LaunchEaseIn))
                }
                launch {
                    dartScale.animateTo(0.75f, tween(380, easing = LaunchEaseIn))
                }
                launch {
                    dartAlpha.animateTo(0f, tween(300, easing = FastOutLinearInEasing))
                }

                delay(360)

                // --- STAGE 2: Blue Checkmark Confirmation (480ms - 1220ms) ---
                animStage = SendAnimStage.CONFIRM
                streakProgress.snapTo(0f)

                // Reset dart off-axis invisibly while checkmark draws
                dartOffsetX.snapTo(with(density) { (-24).dp.toPx() })
                dartOffsetY.snapTo(with(density) { 24.dp.toPx() })
                dartScale.snapTo(0.75f)
                dartAlpha.snapTo(0f)

                checkDrawProgress.snapTo(0f)
                launch {
                    checkScale.animateTo(1f, tween(300, easing = SmoothEaseOut))
                }
                launch {
                    checkAlpha.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
                }
                launch {
                    checkDrawProgress.animateTo(1f, tween(360, easing = SmoothEaseOut))
                }

                delay(740)

                // --- STAGE 3: Smooth Glide Return (1220ms - 1700ms) ---
                animStage = SendAnimStage.RETURN
                launch {
                    checkScale.animateTo(0.75f, tween(300, easing = SmoothEaseOut))
                }
                launch {
                    checkAlpha.animateTo(0f, tween(260, easing = FastOutSlowInEasing))
                }

                val r1 = launch {
                    dartOffsetX.animateTo(0f, tween(460, easing = SmoothEaseOut))
                }
                val r2 = launch {
                    dartOffsetY.animateTo(0f, tween(460, easing = SmoothEaseOut))
                }
                val r3 = launch {
                    dartScale.animateTo(1f, tween(460, easing = SmoothEaseOut))
                }
                val r4 = launch {
                    dartAlpha.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
                }
                r1.join(); r2.join(); r3.join(); r4.join()
            } finally {
                // --- STAGE 4: Complete & Unlock Cleanly ---
                checkDrawProgress.snapTo(0f)
                checkAlpha.snapTo(0f)
                checkScale.snapTo(0.75f)
                streakProgress.snapTo(0f)
                dartOffsetX.snapTo(0f)
                dartOffsetY.snapTo(0f)
                dartScale.snapTo(1f)
                dartAlpha.snapTo(1f)
                animStage = SendAnimStage.IDLE
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 512.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp) // gap-1
    ) {
        // ====================================================================
        // COMPONENT 1: Profile Avatar + Underline Input Bar
        // ====================================================================
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp) // gap-3
        ) {
            // Profile Avatar slot (Custom Avatar or Classic Mac Geometric Vector)
            if (avatarContent != null) {
                avatarContent(avatarRingColor)
            } else {
                ClassicMacProfileAvatar(
                    ringColor = avatarRingColor,
                    modifier = Modifier.size(36.dp) // w-9 h-9
                )
            }
            // Use existing profile avatars

            // Bottom-Bordered Input Field
            Box(
                modifier = Modifier.weight(1f)
            ) {
                BasicTextField(
                    value = effectiveText,
                    onValueChange = { updateText(it) },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = textColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    cursorBrush = SolidColor(Blue600),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = { triggerSend() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                        .onFocusChanged { focusState ->
                            isFocused = focusState.isFocused
                        }
                        .padding(vertical = 8.dp), // py-2
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (effectiveText.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    color = placeholderColor,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                // Static bottom hairline (1.dp)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(inactiveUnderlineColor)
                )

                // Animated Active Blue Underline (2.dp, scales from left origin)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(2.dp)
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0f, 0.5f)
                            scaleX = underlineScaleX
                        }
                        .background(Blue600)
                )
            }
        }

        // ====================================================================
        // COMPONENT 2: Background-Free Custom Filled Animated Send Icon Button
        // ====================================================================
        AnimatedFilledSendButton(
            enabled = hasText && !isAnimating,
            hasText = hasText,
            animStage = animStage,
            dartOffsetX = dartOffsetX.value,
            dartOffsetY = dartOffsetY.value,
            dartScale = dartScale.value,
            dartAlpha = dartAlpha.value,
            streakProgress = streakProgress.value,
            checkScale = checkScale.value,
            checkAlpha = checkAlpha.value,
            checkDrawProgress = checkDrawProgress.value,
            disabledColor = disabledIconColor,
            onClick = { triggerSend() }
        )
    }
}

/**
 * Exact Jetpack Compose vector reproduction of the Classic Mac Geometric Profile Avatar
 * (#E2E5E9 -> #9BA2AE background, specular ring border, and geometric silhouette).
 */
@Composable
fun ClassicMacProfileAvatar(
    ringColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(elevation = 1.dp, shape = CircleShape, clip = false)
            .border(width = 2.dp, color = ringColor, shape = CircleShape)
            .clip(CircleShape)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val center = Offset(w / 2f, h / 2f)
            val radius = minOf(w, h) / 2f
            val scale = minOf(w, h) / 512f

            // Background vertical gradient (#E2E5E9 -> #9BA2AE)
            drawCircle(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFE2E5E9), Color(0xFF9BA2AE))
                ),
                radius = radius,
                center = center
            )

            // Silhouette vertical gradient (#FFFFFF at 10% -> #D5D9E0 at 90%)
            val silhouetteBrush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0.10f to Color(0xFFFFFFFF),
                    0.90f to Color(0xFFD5D9E0)
                ),
                startY = 0f,
                endY = h
            )

            // Clipped geometric silhouette (Head circle + Body circle)
            val clipCircle = Path().apply {
                addOval(Rect(center = center, radius = radius))
            }
            clipPath(clipCircle) {
                // Perfect geometric circle for head: cx=256, cy=165, r=95
                drawCircle(
                    brush = silhouetteBrush,
                    radius = 95f * scale,
                    center = Offset(256f * scale, 165f * scale)
                )
                // Perfect geometric circle for body: cx=256, cy=530, r=250
                drawCircle(
                    brush = silhouetteBrush,
                    radius = 250f * scale,
                    center = Offset(256f * scale, 530f * scale)
                )
            }

            // Specular edge border: r=254.5, stroke-width=3
            drawCircle(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.7f),
                        Color.Black.copy(alpha = 0.2f)
                    )
                ),
                radius = 254.5f * scale,
                center = center,
                style = Stroke(width = 3f * scale)
            )
        }
    }
}
// Use existing profile avatars

/**
 * Custom-made filled origami paper-plane icon button with no text and no circle background.
 * Gray when disabled, vibrant blue when active, with multi-stage flight & checkmark confirmation.
 */
@Composable
private fun AnimatedFilledSendButton(
    enabled: Boolean,
    hasText: Boolean,
    animStage: SendAnimStage,
    dartOffsetX: Float,
    dartOffsetY: Float,
    dartScale: Float,
    dartAlpha: Float,
    streakProgress: Float,
    checkScale: Float,
    checkAlpha: Float,
    checkDrawProgress: Float,
    disabledColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val isIdle = animStage == SendAnimStage.IDLE
    val density = LocalDensity.current

    // Micro-interactions when enabled and idle (hover & active press)
    val interactiveOffsetX by animateFloatAsState(
        targetValue = when {
            enabled && isIdle && isPressed -> with(density) { 1.dp.toPx() }
            enabled && isIdle && isHovered -> with(density) { 2.dp.toPx() }
            else -> 0f
        },
        animationSpec = tween(durationMillis = 280, easing = SmoothEaseOut),
        label = "interactiveOffsetX"
    )
    val interactiveOffsetY by animateFloatAsState(
        targetValue = when {
            enabled && isIdle && isPressed -> with(density) { (-1).dp.toPx() }
            enabled && isIdle && isHovered -> with(density) { (-2).dp.toPx() }
            else -> 0f
        },
        animationSpec = tween(durationMillis = 280, easing = SmoothEaseOut),
        label = "interactiveOffsetY"
    )
    val interactiveScale by animateFloatAsState(
        targetValue = when {
            enabled && isIdle && isPressed -> 0.95f
            enabled && isIdle && isHovered -> 1.1f
            else -> 1f
        },
        animationSpec = tween(durationMillis = 280, easing = SmoothEaseOut),
        label = "interactiveScale"
    )

    // Color state: Blue600 when ready/sending (Blue700 on hover/press), disabledColor when inactive
    val targetIconColor = when (animStage) {
        SendAnimStage.PULLBACK, SendAnimStage.LAUNCH, SendAnimStage.CONFIRM -> Blue600
        SendAnimStage.RETURN -> if (hasText) Blue600 else disabledColor
        SendAnimStage.IDLE -> when {
            !hasText -> disabledColor
            isHovered || isPressed -> Blue700
            else -> Blue600
        }
    }

    val iconColor by animateColorAsState(
        targetValue = targetIconColor,
        animationSpec = tween(durationMillis = 300),
        label = "sendIconColor"
    )

    // Reusable path objects to avoid per-frame allocations during animation
    val upperWingPath = remember { Path() }
    val leftWingPath = remember { Path() }
    val shadowWedgePath = remember { Path() }
    val fullCheckPath = remember { Path() }
    val animatedCheckPath = remember { Path() }
    val checkPathMeasure = remember { PathMeasure() }

    Box(
        modifier = modifier
            .size(40.dp) // w-10 h-10 touch target
            .semantics { contentDescription = "Send message" }
            .clickable(
                interactionSource = interactionSource,
                indication = null, // No circular ripple/background
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // 1. Custom-Made Filled Geometric Paper-Plane Icon + Kinetic Thrust Streaks
        Canvas(
            modifier = Modifier
                .size(28.dp) // w-7 h-7
                .graphicsLayer {
                    clip = false // Allow dart to fly smoothly out of bounds
                    translationX = dartOffsetX + interactiveOffsetX
                    translationY = dartOffsetY + interactiveOffsetY
                    scaleX = dartScale * interactiveScale
                    scaleY = dartScale * interactiveScale
                    alpha = dartAlpha
                }
        ) {
            val sx = size.width / 28f
            val sy = size.height / 28f

            // --- Kinetic Thrust / Speed Streaks (ignite during launch) ---
            if (streakProgress > 0f && streakProgress < 1f) {
                val streakAlpha = if (streakProgress <= 0.35f) {
                    streakProgress / 0.35f
                } else {
                    1f - ((streakProgress - 0.35f) / 0.65f)
                }.coerceIn(0f, 1f)

                val startFraction = ((streakProgress - 0.5f) * 2f).coerceIn(0f, 1f)
                val endFraction = (streakProgress * 2f).coerceIn(0f, 1f)

                fun drawAnimatedStreak(
                    x1: Float, y1: Float,
                    x2: Float, y2: Float,
                    strokeWidthUnits: Float
                ) {
                    val pStart = Offset(
                        x = (x1 + (x2 - x1) * startFraction) * sx,
                        y = (y1 + (y2 - y1) * startFraction) * sy
                    )
                    val pEnd = Offset(
                        x = (x1 + (x2 - x1) * endFraction) * sx,
                        y = (y1 + (y2 - y1) * endFraction) * sy
                    )
                    drawLine(
                        color = iconColor.copy(alpha = streakAlpha),
                        start = pStart,
                        end = pEnd,
                        strokeWidth = strokeWidthUnits * sx,
                        cap = StrokeCap.Round
                    )
                }

                // Line 1: (2.5, 21.5) -> (7.5, 16.5), width 2
                drawAnimatedStreak(2.5f, 21.5f, 7.5f, 16.5f, 2f)
                // Line 2: (5.5, 24.5) -> (10.0, 20.0), width 1.75
                drawAnimatedStreak(5.5f, 24.5f, 10f, 20f, 1.75f)
            }

            // --- Primary Upper Wing (100% solid fill) ---
            upperWingPath.reset()
            upperWingPath.moveTo(24.6f * sx, 3.4f * sy)
            upperWingPath.cubicTo(
                25.1f * sx, 3.9f * sy,
                25.2f * sx, 4.6f * sy,
                24.9f * sx, 5.3f * sy
            )
            upperWingPath.lineTo(17.4f * sx, 23.6f * sy)
            upperWingPath.cubicTo(
                17.1f * sx, 24.4f * sy,
                16.3f * sx, 24.9f * sy,
                15.4f * sx, 24.8f * sy
            )
            upperWingPath.cubicTo(
                14.6f * sx, 24.8f * sy,
                13.8f * sx, 24.2f * sy,
                13.6f * sx, 23.4f * sy
            )
            upperWingPath.lineTo(11.5f * sx, 16.5f * sy)
            upperWingPath.lineTo(24.6f * sx, 3.4f * sy)
            upperWingPath.close()

            drawPath(
                path = upperWingPath,
                color = iconColor
            )

            // --- Secondary Left Wing (82% fill for 3D folded depth) ---
            leftWingPath.reset()
            leftWingPath.moveTo(24.6f * sx, 3.4f * sy)
            leftWingPath.lineTo(11.5f * sx, 16.5f * sy)
            leftWingPath.lineTo(4.6f * sx, 14.4f * sy)
            leftWingPath.cubicTo(
                3.8f * sx, 14.2f * sy,
                3.2f * sx, 13.4f * sy,
                3.2f * sx, 12.6f * sy
            )
            leftWingPath.cubicTo(
                3.1f * sx, 11.7f * sy,
                3.6f * sx, 10.9f * sy,
                4.4f * sx, 10.6f * sy
            )
            leftWingPath.lineTo(22.7f * sx, 3.1f * sy)
            leftWingPath.cubicTo(
                23.4f * sx, 2.8f * sy,
                24.1f * sx, 2.9f * sy,
                24.6f * sx, 3.4f * sy
            )
            leftWingPath.close()

            drawPath(
                path = leftWingPath,
                color = iconColor.copy(alpha = 0.82f)
            )

            // --- Inner Fold Shadow Wedge (55% fill) ---
            shadowWedgePath.reset()
            shadowWedgePath.moveTo(11.5f * sx, 16.5f * sy)
            shadowWedgePath.lineTo(15.8f * sx, 18.8f * sy)
            shadowWedgePath.lineTo(13.6f * sx, 23.4f * sy)
            shadowWedgePath.close()

            drawPath(
                path = shadowWedgePath,
                color = iconColor.copy(alpha = 0.55f)
            )
        }

        // 2. Minimal Blue Checkmark Confirmation (no circle background)
        if (checkAlpha > 0f && checkDrawProgress > 0f) {
            Canvas(
                modifier = Modifier
                    .size(28.dp) // w-7 h-7
                    .graphicsLayer {
                        scaleX = checkScale
                        scaleY = checkScale
                        alpha = checkAlpha
                    }
            ) {
                val sx = size.width / 28f
                val sy = size.height / 28f

                fullCheckPath.reset()
                fullCheckPath.moveTo(6.5f * sx, 14.8f * sy)
                fullCheckPath.lineTo(11.6f * sx, 19.8f * sy)
                fullCheckPath.lineTo(21.8f * sx, 8.8f * sy)

                checkPathMeasure.setPath(fullCheckPath, forceClosed = false)
                animatedCheckPath.reset()
                checkPathMeasure.getSegment(
                    startDistance = 0f,
                    stopDistance = checkPathMeasure.length * checkDrawProgress.coerceIn(0f, 1f),
                    destination = animatedCheckPath,
                    startWithMoveTo = true
                )

                drawPath(
                    path = animatedCheckPath,
                    color = Blue600,
                    style = Stroke(
                        width = 2.8f * sx,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}

/**
 * Full-screen preview wrapper matching the centered minimalist canvas.
 */
@Preview(showBackground = true, widthDp = 412, heightDp = 240)
@Composable
fun MinimalMessageInputBarPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Neutral50)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            MinimalMessageInputBar(
                onSendMessage = { /* Handle sent message */ }
            )
        }
    }
}
