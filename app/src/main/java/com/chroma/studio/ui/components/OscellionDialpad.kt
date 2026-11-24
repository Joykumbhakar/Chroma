package com.chroma.studio.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.ui.theme.InterFontFamily
import com.composables.icons.lucide.Delete
import com.composables.icons.lucide.Fingerprint
import com.composables.icons.lucide.Lucide
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild

/**
 * Job types defining the context in which the Oscellion Dialpad operates.
 */
enum class PinJobType {
    /** Full-screen system/app lock unlock */
    APP_LOCK_UNLOCK,

    /** Setting up or changing application PIN */
    PIN_SETUP,

    /** Unlocking or configuring Hidden Chats Private Vault */
    HIDDEN_CHATS,

    /** Custom / embedded PIN verification */
    CUSTOM
}

data class KeypadDigitInfo(
    val digit: String,
    val sublabel: String
)

val DEFAULT_KEYPAD_DIGITS: List<List<KeypadDigitInfo>> = listOf(
    listOf(KeypadDigitInfo("1", ""), KeypadDigitInfo("2", "ABC"), KeypadDigitInfo("3", "DEF")),
    listOf(KeypadDigitInfo("4", "GHI"), KeypadDigitInfo("5", "JKL"), KeypadDigitInfo("6", "MNO")),
    listOf(KeypadDigitInfo("7", "PQRS"), KeypadDigitInfo("8", "TUV"), KeypadDigitInfo("9", "WXYZ"))
)

/**
 * Universal Specular Frosted Glass Keypad Button (Samsung One UI / Apple Glass style).
 * Derived directly from flagship biometric_pin_lock_screen specifications:
 * - Real-time Haze backdrop blur with graceful translucent fallback
 * - Specular vertical gradient rim border
 * - Spring scale feedback on tactile press
 * - Integrated mechanical tap sound and tactile haptics
 */
@Composable
fun OscellionKeypadButton(
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
    hazeState: HazeState? = null,
    enabled: Boolean = true,
    playSound: Boolean = true,
    playHaptic: Boolean = true,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val useBlur = rememberUseRealtimeBlur()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.92f else 1f,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.6f),
        label = "oscellionKeypadScale"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.4f
            }
            .clip(CircleShape)
            .then(
                if (useBlur && hazeState != null) {
                    Modifier.hazeChild(
                        state = hazeState,
                        shape = CircleShape,
                        style = HazeDefaults.style(
                            tint = if (isPressed && enabled) Color.White.copy(alpha = 0.18f)
                            else Color.White.copy(alpha = 0.08f),
                            blurRadius = 24.dp,
                            noiseFactor = 0f
                        )
                    )
                } else Modifier
            )
            .background(
                if (isPressed && enabled) Color.White.copy(alpha = 0.22f)
                else Color.White.copy(alpha = 0.08f)
            )
            .border(
                width = 0.75.dp,
                brush = Brush.verticalGradient(
                    colors = if (isPressed && enabled) listOf(
                        Color.White.copy(alpha = 0.75f),
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.45f)
                    ) else listOf(
                        Color.White.copy(alpha = 0.58f),
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.04f),
                        Color.White.copy(alpha = 0.24f)
                    )
                ),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = {
                    if (playSound) SoundManager.playLockKeyTap(context)
                    if (playHaptic) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Standard Circular Biometric Trigger Keypad Button
 */
@Composable
fun OscellionBiometricKeypadButton(
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
    hazeState: HazeState? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OscellionKeypadButton(
        modifier = modifier,
        size = size,
        hazeState = hazeState,
        enabled = enabled,
        onClick = onClick
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Lucide.Fingerprint,
                contentDescription = "Use Biometric",
                tint = Color.White,
                modifier = Modifier.size(if (size < 70.dp) 20.dp else 24.dp)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "BIO",
                fontSize = if (size < 70.dp) 8.5.sp else 9.5.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.6.sp,
                color = Color(0xFFE5E7EB),
                fontFamily = InterFontFamily,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.85f),
                        offset = Offset(0f, 1f),
                        blurRadius = 3f
                    )
                )
            )
        }
    }
}

/**
 * Standard Circular Backspace / Delete Keypad Button
 */
@Composable
fun OscellionDeleteKeypadButton(
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
    hazeState: HazeState? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OscellionKeypadButton(
        modifier = modifier,
        size = size,
        hazeState = hazeState,
        enabled = enabled,
        onClick = onClick
    ) {
        Icon(
            imageVector = Lucide.Delete,
            contentDescription = "Delete digit",
            tint = Color.White,
            modifier = Modifier.size(if (size < 70.dp) 21.dp else 24.dp)
        )
    }
}

/**
 * Universal 3x4 Frosted Glass Dialpad Grid Component.
 * Guarantees mathematical centering across all screens and device form factors.
 * - Rows 1-3: Digits 1-9 with standard alphanumeric letters
 * - Row 4: Configurable Action Slot Left (e.g. Biometric), Digit 0, Configurable Action Slot Right (e.g. Backspace)
 */
@Composable
fun OscellionDialpadGrid(
    modifier: Modifier = Modifier,
    buttonSize: Dp = 76.dp,
    horizontalSpacing: Dp = 20.dp,
    verticalSpacing: Dp = 16.dp,
    hazeState: HazeState? = null,
    enabled: Boolean = true,
    onDigitClick: (String) -> Unit,
    actionSlotLeft: (@Composable () -> Unit)? = null,
    actionSlotRight: (@Composable () -> Unit)? = {
        OscellionDeleteKeypadButton(
            size = buttonSize,
            hazeState = hazeState,
            enabled = enabled,
            onClick = {}
        )
    }
) {
    Column(
        modifier = modifier.wrapContentWidth(),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Rows 1 - 3 (1-9)
        for (row in DEFAULT_KEYPAD_DIGITS) {
            Row(
                modifier = Modifier.wrapContentWidth(),
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (key in row) {
                    OscellionKeypadButton(
                        size = buttonSize,
                        hazeState = hazeState,
                        enabled = enabled,
                        onClick = { onDigitClick(key.digit) }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = key.digit,
                                fontSize = if (buttonSize < 70.dp) 26.sp else 30.sp,
                                fontWeight = FontWeight.Normal,
                                lineHeight = if (buttonSize < 70.dp) 26.sp else 30.sp,
                                color = Color.White,
                                fontFamily = InterFontFamily,
                                style = TextStyle(
                                    shadow = Shadow(
                                        color = Color.Black.copy(alpha = 0.85f),
                                        offset = Offset(0f, 1f),
                                        blurRadius = 3f
                                    )
                                )
                            )
                            if (key.sublabel.isNotBlank()) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = key.sublabel,
                                    fontSize = if (buttonSize < 70.dp) 9.sp else 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 1.2.sp,
                                    color = Color(0xFFE5E7EB),
                                    fontFamily = InterFontFamily,
                                    style = TextStyle(
                                        shadow = Shadow(
                                            color = Color.Black.copy(alpha = 0.85f),
                                            offset = Offset(0f, 1f),
                                            blurRadius = 3f
                                        )
                                    )
                                )
                            } else {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = " ",
                                    fontSize = if (buttonSize < 70.dp) 9.sp else 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Transparent
                                )
                            }
                        }
                    }
                }
            }
        }

        // Row 4: [Slot Left] | Digit 0 | [Slot Right]
        Row(
            modifier = Modifier.wrapContentWidth(),
            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Action Slot (Biometric or spacer)
            if (actionSlotLeft != null) {
                actionSlotLeft()
            } else {
                Box(modifier = Modifier.size(buttonSize))
            }

            // Center Digit: 0
            OscellionKeypadButton(
                size = buttonSize,
                hazeState = hazeState,
                enabled = enabled,
                onClick = { onDigitClick("0") }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "0",
                        fontSize = if (buttonSize < 70.dp) 26.sp else 30.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = if (buttonSize < 70.dp) 26.sp else 30.sp,
                        color = Color.White,
                        fontFamily = InterFontFamily,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.85f),
                                offset = Offset(0f, 1f),
                                blurRadius = 3f
                            )
                        )
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = " ",
                        fontSize = if (buttonSize < 70.dp) 9.sp else 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Transparent
                    )
                }
            }

            // Right Action Slot (Delete / Backspace by default or spacer)
            if (actionSlotRight != null) {
                actionSlotRight()
            } else {
                Box(modifier = Modifier.size(buttonSize))
            }
        }
    }
}

/**
 * Flagship 4-Digit Rounded-Square PIN Indicator Component.
 * - Width: 46dp, Height: 50dp, Radius: 14dp (scalable)
 * - Active focus slot: Glowing blue border with pulsing caret bar (|)
 * - Filled slot: Frosted white fill with 12dp solid white dot
 * - Error slot: Vivid red error highlight
 */
@Composable
fun OscellionPinIndicatorBoxes(
    enteredPinLength: Int,
    pinLength: Int = 4,
    isError: Boolean = false,
    boxWidth: Dp = 46.dp,
    boxHeight: Dp = 50.dp,
    boxRadius: Dp = 14.dp,
    spacing: Dp = 14.dp,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier
) {
    val useBlur = rememberUseRealtimeBlur()
    val infiniteCaret = rememberInfiniteTransition(label = "oscellionPinCaret")
    val caretAlpha by infiniteCaret.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "caretAlpha"
    )

    Row(
        modifier = modifier
            .wrapContentWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (index in 0 until pinLength) {
            val isFilled = index < enteredPinLength
            val isActive = index == enteredPinLength && !isError

            Box(
                modifier = Modifier
                    .size(width = boxWidth, height = boxHeight)
                    .clip(RoundedCornerShape(boxRadius))
                    .then(
                        if (useBlur && hazeState != null) {
                            Modifier.hazeChild(
                                state = hazeState,
                                shape = RoundedCornerShape(boxRadius),
                                style = HazeDefaults.style(
                                    tint = when {
                                        isError -> Color(0xFFEF4444).copy(alpha = 0.18f)
                                        isActive -> Color(0xFF3B82F6).copy(alpha = 0.14f)
                                        isFilled -> Color.White.copy(alpha = 0.18f)
                                        else -> Color.White.copy(alpha = 0.08f)
                                    },
                                    blurRadius = 18.dp,
                                    noiseFactor = 0f
                                )
                            )
                        } else Modifier
                    )
                    .background(
                        when {
                            isError -> Color(0xFFEF4444).copy(alpha = 0.18f)
                            isActive -> Color(0xFF3B82F6).copy(alpha = 0.14f)
                            isFilled -> Color.White.copy(alpha = 0.18f)
                            else -> Color.White.copy(alpha = 0.08f)
                        }
                    )
                    .border(
                        width = 1.25.dp,
                        color = when {
                            isError -> Color(0xFFEF4444)
                            isActive -> Color(0xFF3B82F6)
                            isFilled -> Color.White.copy(alpha = 0.55f)
                            else -> Color.White.copy(alpha = 0.24f)
                        },
                        shape = RoundedCornerShape(boxRadius)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isFilled) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (isError) Color(0xFFEF4444) else Color.White)
                    )
                } else if (isActive) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF60A5FA).copy(alpha = caretAlpha))
                    )
                }
            }
        }
    }
}

/**
 * High-Level, Self-Contained PIN Entry & Dialpad Section.
 * Coordinates Header, Subtitle, Indicator Boxes, and 3x4 Dialpad Grid.
 */
@Composable
fun OscellionPinEntrySection(
    title: String,
    subtitle: String,
    enteredPin: String,
    modifier: Modifier = Modifier,
    pinLength: Int = 4,
    isError: Boolean = false,
    lockoutSecondsRemaining: Int = 0,
    hintText: String? = "Enter any 4 digits to unlock",
    buttonSize: Dp = 76.dp,
    horizontalSpacing: Dp = 20.dp,
    verticalSpacing: Dp = 16.dp,
    boxWidth: Dp = 46.dp,
    boxHeight: Dp = 50.dp,
    boxRadius: Dp = 14.dp,
    hazeState: HazeState? = null,
    onDigitClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    actionSlotLeft: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier.wrapContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Text(
            text = title,
            fontSize = if (buttonSize < 70.dp) 24.sp else 29.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.02).sp,
            color = Color.White,
            fontFamily = InterFontFamily,
            textAlign = TextAlign.Center,
            style = TextStyle(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.85f),
                    offset = Offset(0f, 2f),
                    blurRadius = 6f
                )
            )
        )

        Spacer(Modifier.height(6.dp))

        // Subtitle
        Text(
            text = subtitle,
            fontSize = if (buttonSize < 70.dp) 13.5.sp else 15.sp,
            fontWeight = FontWeight.Normal,
            color = if (isError || lockoutSecondsRemaining > 0) Color(0xFFEF4444) else Color(0xFFE5E7EB),
            fontFamily = InterFontFamily,
            textAlign = TextAlign.Center,
            style = TextStyle(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.85f),
                    offset = Offset(0f, 1f),
                    blurRadius = 4f
                )
            )
        )

        Spacer(Modifier.height(if (buttonSize < 70.dp) 20.dp else 30.dp))

        // Indicator Boxes
        OscellionPinIndicatorBoxes(
            enteredPinLength = enteredPin.length,
            pinLength = pinLength,
            isError = isError,
            boxWidth = boxWidth,
            boxHeight = boxHeight,
            boxRadius = boxRadius,
            spacing = 14.dp,
            hazeState = hazeState
        )

        Spacer(Modifier.height(if (buttonSize < 70.dp) 22.dp else 34.dp))

        // Dialpad Grid
        OscellionDialpadGrid(
            buttonSize = buttonSize,
            horizontalSpacing = horizontalSpacing,
            verticalSpacing = verticalSpacing,
            hazeState = hazeState,
            enabled = lockoutSecondsRemaining == 0,
            onDigitClick = onDigitClick,
            actionSlotLeft = actionSlotLeft,
            actionSlotRight = {
                OscellionDeleteKeypadButton(
                    size = buttonSize,
                    hazeState = hazeState,
                    enabled = lockoutSecondsRemaining == 0,
                    onClick = onDeleteClick
                )
            }
        )

        if (!hintText.isNullOrBlank()) {
            Spacer(Modifier.height(if (buttonSize < 70.dp) 18.dp else 28.dp))
            Text(
                text = if (lockoutSecondsRemaining > 0) "Keypad locked due to security" else hintText,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Normal,
                color = if (lockoutSecondsRemaining > 0) Color(0xFFEF4444).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.65f),
                letterSpacing = 0.5.sp,
                fontFamily = InterFontFamily,
                textAlign = TextAlign.Center
            )
        }
    }
}
