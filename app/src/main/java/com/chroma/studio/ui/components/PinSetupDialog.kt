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
                // Top Header Row with borderless Close Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.X,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Center: Title, Subtitle, and 4 Rounded-Square Boxes
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .wrapContentSize()
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

                    OscellionPinIndicatorBoxes(
                        enteredPinLength = currentEnteredPin.length,
                        pinLength = 4,
                        isError = isError
                    )
                }

                // Bottom: Uniform 76dp Circular Keypad
                Box(
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(bottom = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    OscellionDialpadGrid(
                        buttonSize = 76.dp,
                        horizontalSpacing = 20.dp,
                        verticalSpacing = 16.dp,
                        onDigitClick = { onDigitPress(it) },
                        actionSlotLeft = null,
                        actionSlotRight = {
                            OscellionDeleteKeypadButton(
                                size = 76.dp,
                                onClick = { onDeletePress() }
                            )
                        }
                    )
                }
            }
        }
    }
}
