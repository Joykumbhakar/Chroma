package com.chroma.studio.ui.components

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.chat.util.BiometricHelper
import com.chroma.studio.chat.util.findActivity
import com.chroma.studio.security.SecurityPinManager
import com.chroma.studio.ui.theme.InterFontFamily
import com.composables.icons.lucide.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class LockScreenView {
    BIOMETRIC_CHOICE,
    PIN_ENTRY,
    UNLOCKED_CONFIRMATION
}

/**
 * Flagship Oscellion Biometric & PIN Lock Screen Component.
 * Pixel-perfect implementation derived directly from e:\Design & 3D Assets\biometric_pin_lock_screen.html:
 * - Shared wallpaper canvas with real-time dimming (0% to 85%) and iOS long-press preview zoom.
 * - Screen 1: Minimal Biometric unlock with luminous blue gradient "Biometric" glow title,
 *             primary gradient pill button and secondary frosted glass pill button.
 * - Screen 2: Frosted glass PIN unlock with 4 rounded-square PIN boxes (46dp x 50dp, active pulsing caret, 12dp white filled circle),
 *             and 3x4 uniform 78dp circular glass keypad buttons with glossy specular bevels.
 * - Screen 3: Unlocked confirmation stage with emerald checkmark before navigating.
 * - Wallpaper Studio Bottom Sheet: Live dimming slider + horizontal wallpaper preset carousel.
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

    // Wallpaper and dimming customization state
    var wallpaperUri by remember { mutableStateOf(SecurityPinManager.getWallpaperUri(context)) }
    var wallpaperDimming by remember { mutableFloatStateOf(SecurityPinManager.getWallpaperDimming(context)) }

    var isWallpaperEditing by remember { mutableStateOf(false) }
    var pendingWallpaperUri by remember { mutableStateOf(wallpaperUri) }
    var pendingDimming by remember { mutableFloatStateOf(wallpaperDimming) }

    // Wallpaper presets (Downloaded into app assets)
    val wallpaperPresets = remember {
        listOf(
            WallpaperPreset("Style 1", "file:///android_asset/wallpaper/wallpaper_1.jpg"),
            WallpaperPreset("Default", "file:///android_asset/wallpaper/wallpaper_2.jpg"),
            WallpaperPreset("Style 3", "file:///android_asset/wallpaper/wallpaper_3.jpg"),
            WallpaperPreset("Style 4", "file:///android_asset/wallpaper/wallpaper_4.jpg"),
            WallpaperPreset("Style 5", "file:///android_asset/wallpaper/wallpaper_5.jpg"),
            WallpaperPreset("Style 6", "file:///android_asset/wallpaper/wallpaper_6.jpg"),
            WallpaperPreset("Style 7", "file:///android_asset/wallpaper/wallpaper_7.jpg"),
            WallpaperPreset("Style 8", "file:///android_asset/wallpaper/wallpaper_8.jpg")
        )
    }

    // Intercept hardware back button when locked
    BackHandler(enabled = true) {
        if (isWallpaperEditing) {
            isWallpaperEditing = false
            pendingWallpaperUri = wallpaperUri
            pendingDimming = wallpaperDimming
        } else if (currentView == LockScreenView.PIN_ENTRY) {
            currentView = LockScreenView.BIOMETRIC_CHOICE
            enteredPin = ""
        } else {
            (activity ?: (context as? Activity))?.finish()
        }
    }

    // Biometric authentication trigger
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
                currentView = LockScreenView.UNLOCKED_CONFIRMATION
            },
            onCancel = {},
            onError = { err ->
                ToastManager.showToast(err)
            }
        )
    }

    // Auto-prompt biometrics once on initial presentation
    LaunchedEffect(Unit) {
        delay(300)
        triggerBiometricPrompt()
    }

    // Trigger confirmation screen delay
    LaunchedEffect(currentView) {
        if (currentView == LockScreenView.UNLOCKED_CONFIRMATION) {
            delay(750)
            onUnlockSuccess()
        }
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
                val isValid = SecurityPinManager.verifyPin(context, newPin)
                if (isValid) {
                    SecurityPinManager.markUnlocked()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    SoundManager.playReaction()
                    currentView = LockScreenView.UNLOCKED_CONFIRMATION
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

    // Active screen zoom animation for iOS-style wallpaper customization
    val screenScale by animateFloatAsState(
        targetValue = if (isWallpaperEditing) 0.84f else 1f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "screenScale"
    )
    val screenOffsetY by animateDpAsState(
        targetValue = if (isWallpaperEditing) (-32).dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "screenOffsetY"
    )
    val screenCornerRadius by animateDpAsState(
        targetValue = if (isWallpaperEditing) 34.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "screenCornerRadius"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030407))
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        pendingWallpaperUri = wallpaperUri
                        pendingDimming = wallpaperDimming
                        isWallpaperEditing = true
                    }
                )
            }
    ) {
        // =========================================================================
        // 1. SHARED WALLPAPER & DIMMING SERVICE LAYER
        // =========================================================================
        AsyncImage(
            model = if (isWallpaperEditing) pendingWallpaperUri else wallpaperUri,
            contentDescription = "Lock Screen Wallpaper",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Real-Time Dimming Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color.Black.copy(
                        alpha = if (isWallpaperEditing) pendingDimming else wallpaperDimming
                    )
                )
        )

        // =========================================================================
        // 2. MAIN ACTIVE SCREEN CONTENT CONTAINER (ZOOMABLE)
        // =========================================================================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = screenScale
                    scaleY = screenScale
                    translationY = screenOffsetY.toPx()
                }
                .clip(RoundedCornerShape(screenCornerRadius))
                .border(
                    width = if (isWallpaperEditing) 1.5.dp else 0.dp,
                    color = Color.White.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(screenCornerRadius)
                )
        ) {
            AnimatedContent(
                targetState = currentView,
                transitionSpec = {
                    if (targetState == LockScreenView.PIN_ENTRY) {
                        (fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 10 })
                            .togetherWith(fadeOut(tween(180)))
                    } else if (targetState == LockScreenView.UNLOCKED_CONFIRMATION) {
                        fadeIn(tween(240)).togetherWith(fadeOut(tween(180)))
                    } else {
                        fadeIn(tween(260))
                            .togetherWith(fadeOut(tween(180)) + slideOutVertically(tween(200)) { it / 10 })
                    }
                },
                label = "lockScreenTransition"
            ) { viewState ->
                when (viewState) {
                    LockScreenView.BIOMETRIC_CHOICE -> {
                        // ── SCREEN 1: MINIMAL BIOMETRIC UNLOCK SCREEN ───────────
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .navigationBarsPadding()
                                .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Subtle Top Long-Press Discovery Hint
                            Box(
                                modifier = Modifier
                                    .padding(top = 16.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.35f))
                                    .border(0.75.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                                    .padding(horizontal = 14.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "Hold wallpaper to customize",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontFamily = InterFontFamily,
                                    letterSpacing = 0.4.sp
                                )
                            }

                            // Centered Main Content Stack
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Main Title with drop shadow
                                Text(
                                    text = "Unlock the app",
                                    fontSize = 35.sp,
                                    lineHeight = 41.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFF4F6F8),
                                    textAlign = TextAlign.Center,
                                    letterSpacing = (-0.025).sp,
                                    fontFamily = InterFontFamily,
                                    style = TextStyle(
                                        shadow = Shadow(
                                            color = Color.Black.copy(alpha = 0.85f),
                                            offset = Offset(0f, 2f),
                                            blurRadius = 6f
                                        )
                                    )
                                )

                                Text(
                                    text = buildAnnotatedString {
                                        append("with ")
                                        withStyle(
                                            SpanStyle(
                                                brush = Brush.linearGradient(
                                                    listOf(
                                                        Color(0xFF60A5FA),
                                                        Color(0xFF2563EB),
                                                        Color(0xFF0066FF)
                                                    )
                                                ),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        ) {
                                            append("Biometric")
                                        }
                                    },
                                    fontSize = 35.sp,
                                    lineHeight = 41.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFF4F6F8),
                                    textAlign = TextAlign.Center,
                                    letterSpacing = (-0.025).sp,
                                    fontFamily = InterFontFamily,
                                    style = TextStyle(
                                        shadow = Shadow(
                                            color = Color(0xFF167EFF).copy(alpha = 0.35f),
                                            offset = Offset(0f, 2f),
                                            blurRadius = 12f
                                        )
                                    )
                                )

                                Spacer(Modifier.height(44.dp))

                                // Action Buttons Stack (Max width 334dp)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .widthIn(max = 334.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Primary Blue Gradient Pill Button: "Unlock with Biometric"
                                    val bioInteraction = remember { MutableInteractionSource() }
                                    val bioPressed by bioInteraction.collectIsPressedAsState()
                                    val bioScale by animateFloatAsState(
                                        targetValue = if (bioPressed) 0.975f else 1f,
                                        label = "bioScale"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(58.dp)
                                            .graphicsLayer {
                                                scaleX = bioScale
                                                scaleY = bioScale
                                            }
                                            .clip(CircleShape)
                                            .background(
                                                brush = Brush.verticalGradient(
                                                    listOf(Color(0xFF167EFF), Color(0xFF0062E6))
                                                )
                                            )
                                            .border(0.75.dp, Color.White.copy(alpha = 0.26f), CircleShape)
                                            .clickable(
                                                interactionSource = bioInteraction,
                                                indication = null
                                            ) {
                                                SoundManager.playReaction()
                                                triggerBiometricPrompt()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(horizontal = 24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Lucide.ScanFace,
                                                contentDescription = "Biometric Icon",
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(Modifier.width(16.dp))
                                            Box(
                                                modifier = Modifier
                                                    .width(1.dp)
                                                    .height(22.dp)
                                                    .background(Color.White.copy(alpha = 0.35f))
                                            )
                                            Spacer(Modifier.width(16.dp))
                                            Text(
                                                text = "Unlock with Biometric",
                                                fontSize = 15.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                letterSpacing = (-0.01).sp,
                                                color = Color.White,
                                                fontFamily = InterFontFamily
                                            )
                                        }
                                    }

                                    // Secondary Frosted Glass Pill Button: "Unlock with PIN"
                                    val pinInteraction = remember { MutableInteractionSource() }
                                    val pinPressed by pinInteraction.collectIsPressedAsState()
                                    val pinScale by animateFloatAsState(
                                        targetValue = if (pinPressed) 0.975f else 1f,
                                        label = "pinScale"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(58.dp)
                                            .graphicsLayer {
                                                scaleX = pinScale
                                                scaleY = pinScale
                                            }
                                            .clip(CircleShape)
                                            .background(
                                                if (pinPressed) Color(0xFF222834).copy(alpha = 0.76f)
                                                else Color(0xFF12161E).copy(alpha = 0.62f)
                                            )
                                            .border(0.75.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                                            .clickable(
                                                interactionSource = pinInteraction,
                                                indication = null
                                            ) {
                                                SoundManager.playReaction()
                                                if (SecurityPinManager.hasPin(context)) {
                                                    currentView = LockScreenView.PIN_ENTRY
                                                    enteredPin = ""
                                                } else {
                                                    showPinSetupDialog = true
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(horizontal = 24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Lucide.Lock,
                                                contentDescription = "PIN Icon",
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(Modifier.width(16.dp))
                                            Box(
                                                modifier = Modifier
                                                    .width(1.dp)
                                                    .height(22.dp)
                                                    .background(Color.White.copy(alpha = 0.25f))
                                            )
                                            Spacer(Modifier.width(16.dp))
                                            Text(
                                                text = "Unlock with PIN",
                                                fontSize = 15.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                letterSpacing = (-0.01).sp,
                                                color = Color.White,
                                                fontFamily = InterFontFamily
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                        }
                    }

                    LockScreenView.PIN_ENTRY -> {
                        // ── SCREEN 2: FROSTED GLASS PIN UNLOCK SCREEN ───────────
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .navigationBarsPadding()
                                .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset(x = shakeAnim.value.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Header Section
                                Text(
                                    text = "Enter PIN",
                                    fontSize = 29.sp,
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

                                Text(
                                    text = if (isPinError) "Incorrect PIN. Try again." else "Enter your 4-digit PIN to unlock",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = if (isPinError) Color(0xFFEF4444) else Color(0xFFE5E7EB),
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

                                Spacer(Modifier.height(28.dp))

                                // 4-Digit Rounded-Square PIN Indicator Boxes (46dp x 50dp, radius 14dp)
                                val infiniteCaret = rememberInfiniteTransition(label = "pinCaret")
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
                                    for (index in 0 until 4) {
                                        val isFilled = index < enteredPin.length
                                        val isActive = index == enteredPin.length && !isPinError

                                        Box(
                                            modifier = Modifier
                                                .size(width = 46.dp, height = 50.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(
                                                    when {
                                                        isPinError -> Color(0xFFEF4444).copy(alpha = 0.18f)
                                                        isActive -> Color(0xFF3B82F6).copy(alpha = 0.14f)
                                                        isFilled -> Color.White.copy(alpha = 0.18f)
                                                        else -> Color.White.copy(alpha = 0.08f)
                                                    }
                                                )
                                                .border(
                                                    width = 1.25.dp,
                                                    color = when {
                                                        isPinError -> Color(0xFFEF4444)
                                                        isActive -> Color(0xFF3B82F6)
                                                        isFilled -> Color.White.copy(alpha = 0.55f)
                                                        else -> Color.White.copy(alpha = 0.24f)
                                                    },
                                                    shape = RoundedCornerShape(14.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isFilled) {
                                                // Center 12dp circular white dot
                                                Box(
                                                    modifier = Modifier
                                                        .size(12.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.White)
                                                )
                                            } else if (isActive) {
                                                // Vertical pulsing caret bar
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

                                Spacer(Modifier.height(36.dp))

                                // 3x4 Uniform Circular Frosted Keypad Grid (78dp circular buttons)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .widthIn(max = 282.dp),
                                    verticalArrangement = Arrangement.spacedBy(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    val keypadMatrix = listOf(
                                        listOf(KeypadData("1", ""), KeypadData("2", "ABC"), KeypadData("3", "DEF")),
                                        listOf(KeypadData("4", "GHI"), KeypadData("5", "JKL"), KeypadData("6", "MNO")),
                                        listOf(KeypadData("7", "PQRS"), KeypadData("8", "TUV"), KeypadData("9", "WXYZ")),
                                        listOf(KeypadData("BIO", ""), KeypadData("0", ""), KeypadData("DEL", ""))
                                    )

                                    for (row in keypadMatrix) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            for (key in row) {
                                                when (key.digit) {
                                                    "BIO" -> {
                                                        // Circular Biometric Shortcut Button (78dp)
                                                        GlassKeypadButton(
                                                            onClick = {
                                                                SoundManager.playReaction()
                                                                triggerBiometricPrompt()
                                                            }
                                                        ) {
                                                            Column(
                                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                                verticalArrangement = Arrangement.Center
                                                            ) {
                                                                Icon(
                                                                    imageVector = Lucide.Fingerprint,
                                                                    contentDescription = "Switch to Biometric",
                                                                    tint = Color.White,
                                                                    modifier = Modifier.size(23.dp)
                                                                )
                                                                Spacer(Modifier.height(2.dp))
                                                                Text(
                                                                    text = "BIO",
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.Medium,
                                                                    letterSpacing = 0.06.sp,
                                                                    color = Color(0xFFE5E7EB),
                                                                    fontFamily = InterFontFamily
                                                                )
                                                            }
                                                        }
                                                    }
                                                    "DEL" -> {
                                                        // Circular Backspace Button (78dp)
                                                        GlassKeypadButton(
                                                            onClick = { onPinDelete() }
                                                        ) {
                                                            Icon(
                                                                imageVector = Lucide.Delete,
                                                                contentDescription = "Delete digit",
                                                                tint = Color.White,
                                                                modifier = Modifier.size(25.dp)
                                                            )
                                                        }
                                                    }
                                                    else -> {
                                                        // Standard Digit Keypad Button (78dp)
                                                        GlassKeypadButton(
                                                            onClick = { onDigitEntered(key.digit) }
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
                                                                if (key.sublabel.isNotBlank()) {
                                                                    Spacer(Modifier.height(2.dp))
                                                                    Text(
                                                                        text = key.sublabel,
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

                                Spacer(Modifier.height(28.dp))

                                Text(
                                    text = "Enter any 4 digits to unlock",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color.White.copy(alpha = 0.65f),
                                    letterSpacing = 0.5.sp,
                                    fontFamily = InterFontFamily,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    LockScreenView.UNLOCKED_CONFIRMATION -> {
                        // ── SCREEN 3: UNLOCKED CONFIRMATION / OSCELLION MORPH STAGE ───
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF040609).copy(alpha = 0.92f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Emerald Glowing Ring + Checkmark
                                val pulseAnim = rememberInfiniteTransition(label = "pulse")
                                val ringScale by pulseAnim.animateFloat(
                                    initialValue = 0.96f,
                                    targetValue = 1.04f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(700, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "ringScale"
                                )

                                Box(
                                    modifier = Modifier
                                        .size(114.dp)
                                        .graphicsLayer {
                                            scaleX = ringScale
                                            scaleY = ringScale
                                        }
                                        .clip(CircleShape)
                                        .background(
                                            brush = Brush.radialGradient(
                                                listOf(
                                                    Color(0xFF10B981).copy(alpha = 0.28f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                        .border(
                                            width = 2.5.dp,
                                            brush = Brush.sweepGradient(
                                                listOf(
                                                    Color(0xFF10B981),
                                                    Color(0xFF34D399),
                                                    Color(0xFF10B981)
                                                )
                                            ),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Lucide.Check,
                                        contentDescription = "Unlocked",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(54.dp)
                                    )
                                }

                                Spacer(Modifier.height(24.dp))

                                Text(
                                    text = "Unlocked",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    fontFamily = InterFontFamily,
                                    letterSpacing = (-0.02).sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 3. IOS-STYLE WALLPAPER GALLERY & DIMMING STUDIO BOTTOM SHEET
        // =========================================================================
        AnimatedVisibility(
            visible = isWallpaperEditing,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(320, easing = FastOutSlowInEasing)
            ) + fadeIn(tween(240)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(280, easing = FastOutSlowInEasing)
            ) + fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(Color(0xFF0B0D13).copy(alpha = 0.94f))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                // Drag Handle
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f))
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(Modifier.height(14.dp))

                // Compact Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Wallpaper Gallery",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            fontFamily = InterFontFamily
                        )
                        Text(
                            text = "Pick a wallpaper or adjust dimming",
                            fontSize = 11.5.sp,
                            color = Color.White.copy(alpha = 0.55f),
                            fontFamily = InterFontFamily
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Cancel Button
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.10f))
                                .clickable {
                                    pendingWallpaperUri = wallpaperUri
                                    pendingDimming = wallpaperDimming
                                    isWallpaperEditing = false
                                }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.85f),
                                fontFamily = InterFontFamily
                            )
                        }

                        // Set Button
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFF167EFF))
                                .clickable {
                                    wallpaperUri = pendingWallpaperUri
                                    wallpaperDimming = pendingDimming
                                    SecurityPinManager.setWallpaperUri(context, wallpaperUri)
                                    SecurityPinManager.setWallpaperDimming(context, wallpaperDimming)
                                    isWallpaperEditing = false
                                }
                                .padding(horizontal = 18.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "Set",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                fontFamily = InterFontFamily
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Real-Time Wallpaper Dimming Slider
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Lucide.Sun,
                        contentDescription = "Dimming Icon",
                        tint = Color.White.copy(alpha = 0.70f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Dimming",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.80f),
                        fontFamily = InterFontFamily
                    )
                    Spacer(Modifier.width(10.dp))
                    Slider(
                        value = pendingDimming,
                        onValueChange = { pendingDimming = it },
                        valueRange = 0f..0.85f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color(0xFF167EFF),
                            inactiveTrackColor = Color.White.copy(alpha = 0.16f)
                        )
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "${(pendingDimming * 100).toInt()}%",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontFamily = InterFontFamily,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.End
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Horizontal Wallpaper Gallery Presets Carousel
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    wallpaperPresets.forEach { preset ->
                        val isSelected = pendingWallpaperUri == preset.uri
                        Box(
                            modifier = Modifier
                                .size(width = 74.dp, height = 112.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF167EFF) else Color.White.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    pendingWallpaperUri = preset.uri
                                }
                        ) {
                            AsyncImage(
                                model = preset.uri,
                                contentDescription = preset.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Bottom Vignette Label
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.80f))
                                        )
                                    ),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Text(
                                    text = preset.name,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.95f),
                                    fontFamily = InterFontFamily,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
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

/**
 * Specular Frosted Glass Circular Keypad Button (78dp x 78dp)
 * Matches `.keypad-btn` from e:\Design & 3D Assets\biometric_pin_lock_screen.html:
 * - 78px uniform circle with specular vertical bevel highlight
 * - 0.92 scale feedback on press
 */
@Composable
private fun GlassKeypadButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.6f),
        label = "glassKeypadScale"
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

private data class KeypadData(val digit: String, val sublabel: String)
private data class WallpaperPreset(val name: String, val uri: String)
