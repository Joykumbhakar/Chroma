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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.security.SecurityPinManager
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
 * Allows users to set up or change their 4-digit backup PIN with instant dot feedback,
 * confirmation validation, error shake, and haptic feedback.
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

    // Shake animation on error
    val shakeAnim = remember { Animatable(0f) }

    val currentEnteredPin = if (step == SetupStep.ENTER_PIN) firstPin else confirmPin

    fun triggerError(msg: String) {
        scope.launch {
            isError = true
            errorMessage = msg
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
                        // Success!
                        SecurityPinManager.setPin(context, next)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        SoundManager.playReaction()
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
                .background(Color(0xFF000000).copy(alpha = 0.95f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Row with Cancel Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1C1C1E))
                    ) {
                        Icon(
                            imageVector = Lucide.X,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Center: Title, Subtitle, and 4 Dots Indicator
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(x = shakeAnim.value.dp)
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
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = errorMessage ?: if (currentStep == SetupStep.ENTER_PIN) {
                                    "Enter a 4-digit PIN for backup unlock"
                                } else {
                                    "Re-enter your 4-digit PIN to confirm"
                                },
                                fontSize = 14.sp,
                                color = if (errorMessage != null) Color(0xFFFF3B30) else Color(0xFF8E8E93),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(Modifier.height(36.dp))

                    // 4 Indicator Dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(22.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until 4) {
                            val isFilled = i < currentEnteredPin.length
                            val dotScale by animateFloatAsState(
                                targetValue = if (isFilled) 1.15f else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "dotScale$i"
                            )

                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .border(
                                        width = 1.5.dp,
                                        color = when {
                                            isError -> Color(0xFFFF3B30)
                                            isFilled -> Color.White
                                            else -> Color(0xFF636366)
                                        },
                                        shape = CircleShape
                                    )
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isError -> Color(0xFFFF3B30)
                                            isFilled -> Color.White
                                            else -> Color.Transparent
                                        }
                                    )
                            )
                        }
                    }
                }

                // Bottom: Clean Keypad
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val keyRows = listOf(
                        listOf(PinKey("1", ""), PinKey("2", "ABC"), PinKey("3", "DEF")),
                        listOf(PinKey("4", "GHI"), PinKey("5", "JKL"), PinKey("6", "MNO")),
                        listOf(PinKey("7", "PQRS"), PinKey("8", "TUV"), PinKey("9", "WXYZ")),
                        listOf(null, PinKey("0", ""), PinKey("DEL", ""))
                    )

                    for (row in keyRows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(0.85f),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (key in row) {
                                if (key == null) {
                                    Spacer(Modifier.size(76.dp))
                                } else if (key.digit == "DEL") {
                                    Box(
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1C1C1E))
                                            .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) { onDeletePress() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Lucide.Delete,
                                            contentDescription = "Delete",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1C1C1E))
                                            .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) { onDigitPress(key.digit) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = key.digit,
                                                fontSize = 30.sp,
                                                fontWeight = FontWeight.Normal,
                                                color = Color.White,
                                                lineHeight = 32.sp
                                            )
                                            if (key.letters.isNotBlank()) {
                                                Text(
                                                    text = key.letters,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF8E8E93),
                                                    letterSpacing = 1.sp
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

private data class PinKey(val digit: String, val letters: String)
