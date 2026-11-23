package com.chroma.studio.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.security.SecurityPinManager
import com.chroma.studio.ui.theme.InterFontFamily
import com.composables.icons.lucide.Delete
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class SetupStep {
    ENTER_PIN,
    CONFIRM_PIN
}

/**
 * Flagship PIN Setup and Change Dialog.
 * Uses identical design language and styling from e:\Design & 3D Assets\biometric_pin_lock_screen.html:
 * - 4 rounded-square PIN boxes (46dp x 50dp, radius 14dp, active slot pulsing caret, 12dp white filled circle)
 * - 3x4 uniform 78dp circular glass keypad buttons with specular glossy bevel
 * - Smooth step animation and error shake feedback
 */
@Composable
fun PinSetupDialog(
    isChangingExisting: Boolean = false,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var step by remember { mutableStateOf(SetupStep.ENTER_PIN) }
    var firstPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val shakeAnim = remember { Animatable(0f) }
    val currentEnteredPin = if (step == SetupStep.ENTER_PIN) firstPin else confirmPin
    val wallpaperUri = remember { SecurityPinManager.getWallpaperUri(context) }
    val wallpaperDimming = remember { SecurityPinManager.getWallpaperDimming(context) }

    fun triggerError(msg: String) {
        scope.launch {
            isError = true
            errorMessage = msg
            SoundManager.playLockError()
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            shakeAnim.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    0f at 0
                    (-18f) at 50
                    18f at 100
                    (-14f) at 150
                    14f at 200
                    (-8f) at 250
                    8f at 300
                    0f at 400
                }
            )
            delay(400)
            isError = false
            errorMessage = null
            if (step == SetupStep.CONFIRM_PIN) {
                confirmPin = ""
                step = SetupStep.ENTER_PIN
                firstPin = ""
            } else {
                firstPin = ""
            }
        }
    }

    fun onDigitPress(digit: String) {
        if (isError) return
        SoundManager.playLockKeyTap(context)
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

        if (step == SetupStep.ENTER_PIN) {
            if (firstPin.length < 4) {
                val next = firstPin + digit
                firstPin = next
                if (next.length == 4) {
                    scope.launch {
                        delay(120)
                        step = SetupStep.CONFIRM_PIN
                    }
                }
            }
        } else {
            if (confirmPin.length < 4) {
                val next = confirmPin + digit
                confirmPin = next
                if (next.length == 4) {
                    if (next == firstPin) {
                        SecurityPinManager.setPin(context, next)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        SoundManager.playLockSuccess(context)
                        ToastManager.showToast(if (isChangingExisting) "PIN updated successfully" else "PIN set up successfully")
                        onSuccess()
                    } else {
                        triggerError("PINs do not match. Try again.")
                    }
                }
            }
        }
    }

    fun onDeletePress() {
        if (isError) return
        SoundManager.playLockKeyTap(context)
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (step == SetupStep.ENTER_PIN) {
            if (firstPin.isNotEmpty()) firstPin = firstPin.dropLast(1)
        } else {
            if (confirmPin.isNotEmpty()) confirmPin = confirmPin.dropLast(1)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF030407))
        ) {
            // Background Wallpaper + Dimming
            AsyncImage(
                model = wallpaperUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = (wallpaperDimming + 0.15f).coerceAtMost(0.88f)))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Row with Close Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .border(0.75.dp, Color.White.copy(alpha = 0.20f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Lucide.X,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Center: Title, Subtitle, and 4 Rounded-Square Boxes
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(x = shakeAnim.value.dp)
                ) {
                    AnimatedContent(
                        targetState = step,
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                        label = "pinSetupTitle"
                    ) { currentStep ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (currentStep == SetupStep.ENTER_PIN) {
                                    if (isChangingExisting) "Enter New PIN" else "Set a PIN"
                                } else {
                                    "Confirm PIN"
                                },
                                fontSize = 29.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                letterSpacing = (-0.02).sp,
                                fontFamily = InterFontFamily,
                                style = TextStyle(
                                    shadow = Shadow(
                                        color = Color.Black.copy(alpha = 0.85f),
                                        offset = Offset(0f, 2f),
                                        blurRadius = 6f
                                    )
                                )
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = errorMessage ?: if (currentStep == SetupStep.ENTER_PIN) {
                                    "Enter a 4-digit PIN for backup unlock"
                                } else {
                                    "Re-enter your 4-digit PIN to confirm"
                                },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                color = if (errorMessage != null) Color(0xFFEF4444) else Color(0xFFE5E7EB),
                                textAlign = TextAlign.Center,
                                fontFamily = InterFontFamily,
                                style = TextStyle(
                                    shadow = Shadow(
                                        color = Color.Black.copy(alpha = 0.85f),
                                        offset = Offset(0f, 1f),
                                        blurRadius = 4f
                                    )
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(28.dp))

                    // 4 Rounded-Square Indicator Boxes (46dp x 50dp, radius 14dp)
                    val infiniteCaret = rememberInfiniteTransition(label = "setupPinCaret")
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
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until 4) {
                            val isFilled = i < currentEnteredPin.length
                            val isActive = i == currentEnteredPin.length && !isError

                            Box(
                                modifier = Modifier
                                    .size(width = 46.dp, height = 50.dp)
                                    .clip(RoundedCornerShape(14.dp))
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
                                        shape = RoundedCornerShape(14.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isFilled) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
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

                // Bottom: 3x4 Uniform 78dp Circular Keypad
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 282.dp)
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    val keyRows = listOf(
                        listOf(PinKey("1", ""), PinKey("2", "ABC"), PinKey("3", "DEF")),
                        listOf(PinKey("4", "GHI"), PinKey("5", "JKL"), PinKey("6", "MNO")),
                        listOf(PinKey("7", "PQRS"), PinKey("8", "TUV"), PinKey("9", "WXYZ")),
                        listOf(null, PinKey("0", ""), PinKey("DEL", ""))
                    )

                    for (row in keyRows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (key in row) {
                                if (key == null) {
                                    Spacer(Modifier.size(78.dp))
                                } else if (key.digit == "DEL") {
                                    SetupGlassKeypadButton(
                                        onClick = { onDeletePress() }
                                    ) {
                                        Icon(
                                            imageVector = Lucide.Delete,
                                            contentDescription = "Delete",
                                            tint = Color.White,
                                            modifier = Modifier.size(25.dp)
                                        )
                                    }
                                } else {
                                    SetupGlassKeypadButton(
                                        onClick = { onDigitPress(key.digit) }
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = key.digit,
                                                fontSize = 28.sp,
                                                fontWeight = FontWeight.Normal,
                                                lineHeight = 28.sp,
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
                                            if (key.letters.isNotBlank()) {
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    text = key.letters,
                                                    fontSize = 10.sp,
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
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color.Transparent
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupGlassKeypadButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.6f),
        label = "setupGlassScale"
    )

    Box(
        modifier = modifier
            .size(78.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(
                if (isPressed) Color.White.copy(alpha = 0.22f)
                else Color.White.copy(alpha = 0.08f)
            )
            .border(
                width = 0.75.dp,
                brush = Brush.verticalGradient(
                    colors = if (isPressed) listOf(
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
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

private data class PinKey(val digit: String, val letters: String)
