package com.chroma.studio.ui.components

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.R
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.chat.util.BiometricHelper
import com.chroma.studio.chat.util.findActivity
import com.chroma.studio.security.SecurityPinManager
import com.composables.icons.lucide.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class LockScreenView {
    BIOMETRIC_CHOICE,
    PIN_ENTRY
}

/**
 * Flagship Oscellion Lock Screen Component.
 * Faithfully matches the exact design:
 * - View 1: Dark titanium silk waves background with "Unlock the app with Biometric",
 *           pill-shaped "Unlock with Biometric" and frosted "Unlock with PIN" buttons.
 * - View 2: Deep-black PIN entry screen with 4 indicator dots, 3x4 circular glass keypad,
 *           "Use Biometric" pill shortcut, and backspace.
 */
@Composable
fun OscellionLockScreen(
    titleText: String = "Unlock the app",
    onUnlockSuccess: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val activity = remember(context) { context.findActivity() }

    var currentView by remember { mutableStateOf(LockScreenView.BIOMETRIC_CHOICE) }
    var enteredPin by remember { mutableStateOf("") }
    var isPinError by remember { mutableStateOf(false) }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    val shakeAnim = remember { Animatable(0f) }

    // Intercept hardware back button when locked to prevent bypassing
    BackHandler(enabled = true) {
        if (currentView == LockScreenView.PIN_ENTRY) {
            currentView = LockScreenView.BIOMETRIC_CHOICE
            enteredPin = ""
        } else {
            (activity ?: (context as? Activity))?.finish()
        }
    }

    // Launch biometric prompt
    fun triggerBiometricPrompt() {
        val act = activity ?: return
        if (BiometricHelper.isPromptShowing) return

        BiometricHelper.authenticate(
            activity = act,
            title = "Unlock Oscellion",
            subtitle = "Verify your identity to access the app",
            onSuccess = {
                SecurityPinManager.markUnlocked()
                SoundManager.playReaction()
                onUnlockSuccess()
            },
            onCancel = {},
            onError = { err ->
                ToastManager.showToast(err)
            }
        )
    }

    // Initial biometric auto-prompt on display
    LaunchedEffect(Unit) {
        delay(250)
        triggerBiometricPrompt()
    }

    fun triggerPinError() {
        scope.launch {
            isPinError = true
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
            isPinError = false
            enteredPin = ""
        }
    }

    fun onDigitEntered(digit: String) {
        if (isPinError) return
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

        if (enteredPin.length < 4) {
            val newPin = enteredPin + digit
            enteredPin = newPin
            if (newPin.length == 4) {
                // Verify PIN
                val isValid = SecurityPinManager.verifyPin(context, newPin)
                if (isValid) {
                    SecurityPinManager.markUnlocked()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    SoundManager.playReaction()
                    onUnlockSuccess()
                } else {
                    triggerPinError()
                }
            }
        }
    }

    fun onPinDelete() {
        if (isPinError) return
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Consume touches */ }
    ) {
        AnimatedContent(
            targetState = currentView,
            transitionSpec = {
                if (targetState == LockScreenView.PIN_ENTRY) {
                    (fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 8 })
                        .togetherWith(fadeOut(tween(200)))
                } else {
                    fadeIn(tween(260))
                        .togetherWith(fadeOut(tween(200)) + slideOutVertically(tween(200)) { it / 8 })
                }
            },
            label = "lockScreenTransition"
        ) { viewState ->
            when (viewState) {
                LockScreenView.BIOMETRIC_CHOICE -> {
                    // ── SCREEN 1: DARK SILK WAVES BIOMETRIC CHOICE SCREEN ──────────────
                    Box(modifier = Modifier.fillMaxSize()) {
                        // High-fidelity dark waves background
                        Image(
                            painter = painterResource(id = R.drawable.bg_lock_screen),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Subtle dark tint to guarantee readability
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.25f))
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .navigationBarsPadding()
                                .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // "Unlock the app with Biometric" Header
                            Text(
                                text = titleText,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                letterSpacing = (-0.5).sp
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = buildAnnotatedString {
                                    append("with ")
                                    withStyle(
                                        SpanStyle(
                                            color = Color(0xFF0381FE),
                                            fontWeight = FontWeight.Bold
                                        )
                                    ) {
                                        append("Biometric")
                                    }
                                },
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                letterSpacing = (-0.5).sp
                            )

                            Spacer(Modifier.height(58.dp))

                            // 1. Top Vibrant Blue Pill Button: "Unlock with Biometric"
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(58.dp)
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            listOf(Color(0xFF006CFF), Color(0xFF0381FE))
                                        )
                                    )
                                    .clickable {
                                        SoundManager.playReaction()
                                        triggerBiometricPrompt()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                ) {
                                    Icon(
                                        imageVector = Lucide.ScanFace,
                                        contentDescription = "Biometric",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(Modifier.width(14.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(24.dp)
                                            .background(Color.White.copy(alpha = 0.35f))
                                    )
                                    Spacer(Modifier.width(14.dp))
                                    Text(
                                        text = "Unlock with Biometric",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // 2. Bottom Frosted Dark Glass Pill Button: "Unlock with PIN"
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(58.dp)
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(Color(0xFF1E2127).copy(alpha = 0.85f))
                                    .border(
                                        width = 1.dp,
                                        color = Color.White.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(30.dp)
                                    )
                                    .clickable {
                                        SoundManager.playReaction()
                                        if (SecurityPinManager.hasPin(context)) {
                                            currentView = LockScreenView.PIN_ENTRY
                                            enteredPin = ""
                                        } else {
                                            // User hasn't configured a PIN yet -> prompt setup dialog
                                            showPinSetupDialog = true
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                ) {
                                    Icon(
                                        imageVector = Lucide.Lock,
                                        contentDescription = "PIN Lock",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(14.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(24.dp)
                                            .background(Color.White.copy(alpha = 0.25f))
                                    )
                                    Spacer(Modifier.width(14.dp))
                                    Text(
                                        text = "Unlock with PIN",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                LockScreenView.PIN_ENTRY -> {
                    // ── SCREEN 2: DEEP BLACK PIN ENTRY SCREEN ─────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF000000))
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
                            Spacer(Modifier.height(16.dp))

                            // Header + 4 Dots
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.offset(x = shakeAnim.value.dp)
                            ) {
                                Text(
                                    text = "Enter PIN",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = if (isPinError) "Incorrect PIN. Try again." else "Enter your 4-digit PIN to unlock",
                                    fontSize = 14.5.sp,
                                    color = if (isPinError) Color(0xFFFF3B30) else Color(0xFF8E8E93),
                                    textAlign = TextAlign.Center
                                )

                                Spacer(Modifier.height(34.dp))

                                // 4 Dots Indicator
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for (i in 0 until 4) {
                                        val isFilled = i < enteredPin.length
                                        val dotScale by animateFloatAsState(
                                            targetValue = if (isFilled) 1.15f else 1f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            ),
                                            label = "pinDot$i"
                                        )

                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .border(
                                                    width = 1.5.dp,
                                                    color = when {
                                                        isPinError -> Color(0xFFFF3B30)
                                                        isFilled -> Color.White
                                                        else -> Color(0xFF636366)
                                                    },
                                                    shape = CircleShape
                                                )
                                                .clip(CircleShape)
                                                .background(
                                                    when {
                                                        isPinError -> Color(0xFFFF3B30)
                                                        isFilled -> Color.White
                                                        else -> Color.Transparent
                                                    }
                                                )
                                        )
                                    }
                                }
                            }

                            // 3x4 Circular Keypad
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                val keyRows = listOf(
                                    listOf(PinKeyItem("1", ""), PinKeyItem("2", "ABC"), PinKeyItem("3", "DEF")),
                                    listOf(PinKeyItem("4", "GHI"), PinKeyItem("5", "JKL"), PinKeyItem("6", "MNO")),
                                    listOf(PinKeyItem("7", "PQRS"), PinKeyItem("8", "TUV"), PinKeyItem("9", "WXYZ")),
                                    listOf(PinKeyItem("BIOMETRIC", ""), PinKeyItem("0", ""), PinKeyItem("DEL", ""))
                                )

                                for (row in keyRows) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(0.85f),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        for (key in row) {
                                            when (key.digit) {
                                                "BIOMETRIC" -> {
                                                    // "Use Biometric" Pill Shortcut
                                                    Box(
                                                        modifier = Modifier
                                                            .width(112.dp)
                                                            .height(46.dp)
                                                            .clip(RoundedCornerShape(23.dp))
                                                            .background(Color(0xFF1C1C1E))
                                                            .border(
                                                                1.dp,
                                                                Color.White.copy(alpha = 0.12f),
                                                                RoundedCornerShape(23.dp)
                                                            )
                                                            .clickable(
                                                                interactionSource = remember { MutableInteractionSource() },
                                                                indication = null
                                                            ) {
                                                                triggerBiometricPrompt()
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.Center,
                                                            modifier = Modifier.padding(horizontal = 8.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Lucide.Fingerprint,
                                                                contentDescription = "Biometric",
                                                                tint = Color.White,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                            Spacer(Modifier.width(6.dp))
                                                            Box(
                                                                modifier = Modifier
                                                                    .width(1.dp)
                                                                    .height(14.dp)
                                                                    .background(Color.White.copy(alpha = 0.25f))
                                                            )
                                                            Spacer(Modifier.width(6.dp))
                                                            Text(
                                                                text = "Use Biometric",
                                                                color = Color.White,
                                                                fontSize = 11.5.sp,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                        }
                                                    }
                                                }
                                                "DEL" -> {
                                                    // Backspace circular button
                                                    Box(
                                                        modifier = Modifier
                                                            .size(76.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF1C1C1E))
                                                            .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                                                            .clickable(
                                                                interactionSource = remember { MutableInteractionSource() },
                                                                indication = null
                                                            ) { onPinDelete() },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Lucide.Delete,
                                                            contentDescription = "Delete",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }
                                                }
                                                else -> {
                                                    // Standard number key [1-9, 0]
                                                    Box(
                                                        modifier = Modifier
                                                            .size(76.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF1C1C1E))
                                                            .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                                                            .clickable(
                                                                interactionSource = remember { MutableInteractionSource() },
                                                                indication = null
                                                            ) { onDigitEntered(key.digit) },
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
            }
        }
    }

    // Modal PIN Setup Dialog if user needs to configure PIN
    if (showPinSetupDialog) {
        PinSetupDialog(
            isChangingExisting = false,
            onDismiss = { showPinSetupDialog = false },
            onSuccess = {
                showPinSetupDialog = false
                currentView = LockScreenView.PIN_ENTRY
                enteredPin = ""
            }
        )
    }
}

private data class PinKeyItem(val digit: String, val letters: String)
