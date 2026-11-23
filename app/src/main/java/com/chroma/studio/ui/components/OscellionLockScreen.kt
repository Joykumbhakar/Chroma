package com.chroma.studio.ui.components

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
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
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.PI
import kotlin.math.sin

enum class LockScreenView {
    BIOMETRIC_CHOICE,
    PIN_ENTRY,
    UNLOCKED_CONFIRMATION
}

/**
 * Flagship Oscellion Biometric & PIN Lock Screen Component.
 * Pixel-perfect implementation derived directly from e:\Design & 3D Assets\biometric_pin_lock_screen.html:
 * - Shared wallpaper canvas with real-time dimming (0% to 85%), gallery custom photo picker, and iOS long-press zoom.
 * - Screen 1: Minimal Biometric unlock with luminous blue gradient "Biometric" title glow.
 * - Screen 2: Frosted glass PIN unlock with 4 rounded-square boxes (46dp x 50dp), pulsing caret, and 78dp circular glass dial pad.
 * - Failed Attempts UX: Tracks failed attempts (warnings on 1-4, 30s lockout countdown on 5+ attempts), error shake, and sound effects.
 * - Sound Effects: Tactile keyboard clicks, dual warning error buzz, and harmonious crystal unlock chime.
 * - Screen 3: Exact Oscellion 3D Loader -> Emerald Overflowed Checkmark Morph Engine.
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

    // Failed attempts counter and lockout timer
    var failedAttempts by remember { mutableIntStateOf(0) }
    var lockoutSecondsRemaining by remember { mutableIntStateOf(0) }

    // Lockout countdown timer
    LaunchedEffect(lockoutSecondsRemaining) {
        if (lockoutSecondsRemaining > 0) {
            delay(1000)
            lockoutSecondsRemaining -= 1
            if (lockoutSecondsRemaining == 0) {
                failedAttempts = 0
            }
        }
    }

    val hazeState = remember { HazeState() }
    val useBlur = rememberUseRealtimeBlur()

    // Wallpaper and dimming customization state
    var wallpaperUri by remember { mutableStateOf(SecurityPinManager.getWallpaperUri(context)) }
    var wallpaperDimming by remember { mutableFloatStateOf(SecurityPinManager.getWallpaperDimming(context)) }

    var isWallpaperEditing by remember { mutableStateOf(false) }
    var pendingWallpaperUri by remember { mutableStateOf(wallpaperUri) }
    var pendingDimming by remember { mutableFloatStateOf(wallpaperDimming) }
    var customGalleryWallpaperUri by remember { mutableStateOf<String?>(null) }

    // Gallery Custom Photo Picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val targetFile = File(context.filesDir, "custom_wallpaper_${System.currentTimeMillis()}.jpg")
                    inputStream?.use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    val localUri = "file://${targetFile.absolutePath}"
                    withContext(Dispatchers.Main) {
                        customGalleryWallpaperUri = localUri
                        pendingWallpaperUri = localUri
                        ToastManager.showToast("Custom wallpaper selected")
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        ToastManager.showToast("Failed to load photo: ${e.message}")
                    }
                }
            }
        }
    }

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
        if (lockoutSecondsRemaining > 0) {
            ToastManager.showToast("Too many attempts. Please wait.")
            return
        }
        val act = activity ?: return
        if (BiometricHelper.isPromptShowing) return

        BiometricHelper.authenticate(
            activity = act,
            title = "Unlock Oscellion",
            subtitle = "Verify your identity to access the app",
            onSuccess = {
                SecurityPinManager.markUnlocked()
                SoundManager.playLockSuccess(context)
                failedAttempts = 0
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

    // Delay during unlocked morph stage before navigating
    LaunchedEffect(currentView) {
        if (currentView == LockScreenView.UNLOCKED_CONFIRMATION) {
            delay(1250)
            onUnlockSuccess()
        }
    }

    fun triggerPinError() {
        scope.launch {
            failedAttempts += 1
            isPinError = true
            SoundManager.playLockError()
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

            shakeAnim.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 420
                    0f at 0
                    (-20f) at 50
                    20f at 100
                    (-16f) at 150
                    16f at 200
                    (-10f) at 250
                    10f at 300
                    0f at 420
                }
            )

            if (failedAttempts >= 5) {
                lockoutSecondsRemaining = 30
            }

            delay(350)
            isPinError = false
            enteredPin = ""
        }
    }

    fun onDigitEntered(digit: String) {
        if (isPinError || lockoutSecondsRemaining > 0) return
        SoundManager.playLockKeyTap(context)
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

        if (enteredPin.length < 4) {
            val newPin = enteredPin + digit
            enteredPin = newPin
            if (newPin.length == 4) {
                val isValid = SecurityPinManager.verifyPin(context, newPin)
                if (isValid) {
                    SecurityPinManager.markUnlocked()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    SoundManager.playLockSuccess(context)
                    failedAttempts = 0
                    currentView = LockScreenView.UNLOCKED_CONFIRMATION
                } else {
                    triggerPinError()
                }
            }
        }
    }

    fun onPinDelete() {
        if (isPinError || lockoutSecondsRemaining > 0) return
        SoundManager.playLockKeyTap(context)
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
        // 1. SHARED WALLPAPER & DIMMING SERVICE LAYER (REAL-TIME HAZE BLUR SOURCE)
        // =========================================================================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (useBlur) Modifier.haze(hazeState) else Modifier)
        ) {
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
        }

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
                                            .then(
                                                if (useBlur) {
                                                    Modifier.hazeChild(
                                                        state = hazeState,
                                                        shape = CircleShape,
                                                        style = HazeDefaults.style(
                                                            tint = Color(0xFF0062E6).copy(alpha = 0.35f),
                                                            blurRadius = 20.dp,
                                                            noiseFactor = 0f
                                                        )
                                                    )
                                                } else Modifier
                                            )
                                            .background(
                                                brush = Brush.verticalGradient(
                                                    listOf(Color(0xFF167EFF).copy(alpha = 0.94f), Color(0xFF0062E6).copy(alpha = 0.94f))
                                                )
                                            )
                                            .border(0.75.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                                            .clickable(
                                                interactionSource = bioInteraction,
                                                indication = null
                                            ) {
                                                SoundManager.playLockKeyTap(context)
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
                                            .then(
                                                if (useBlur) {
                                                    Modifier.hazeChild(
                                                        state = hazeState,
                                                        shape = CircleShape,
                                                        style = HazeDefaults.style(
                                                            tint = if (pinPressed) Color(0xFF222834).copy(alpha = 0.70f)
                                                                   else Color(0xFF12161E).copy(alpha = 0.52f),
                                                            blurRadius = 24.dp,
                                                            noiseFactor = 0f
                                                        )
                                                    )
                                                } else Modifier
                                            )
                                            .background(
                                                if (pinPressed) Color(0xFF222834).copy(alpha = 0.76f)
                                                else Color(0xFF12161E).copy(alpha = 0.62f)
                                            )
                                            .border(0.75.dp, Color.White.copy(alpha = 0.24f), CircleShape)
                                            .clickable(
                                                interactionSource = pinInteraction,
                                                indication = null
                                            ) {
                                                SoundManager.playLockKeyTap(context)
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

                                // Subtitle with Failed Attempts feedback
                                Text(
                                    text = when {
                                        lockoutSecondsRemaining > 0 -> "Too many attempts. Try again in ${lockoutSecondsRemaining}s"
                                        isPinError -> if (failedAttempts >= 2) "Incorrect PIN (attempt $failedAttempts of 5). Try again." else "Incorrect PIN. Try again."
                                        failedAttempts > 0 -> "Incorrect PIN (attempt $failedAttempts of 5). Try again."
                                        else -> "Enter your 4-digit PIN to unlock"
                                    },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = if (isPinError || lockoutSecondsRemaining > 0 || failedAttempts > 0) Color(0xFFEF4444) else Color(0xFFE5E7EB),
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

                                Spacer(Modifier.height(32.dp))

                                // 4-Digit Circular PIN Indicator Dots (○ ○ ○ ○ per screenshot specification)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    for (index in 0 until 4) {
                                        val isFilled = index < enteredPin.length
                                        val isActive = index == enteredPin.length && !isPinError && lockoutSecondsRemaining == 0

                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when {
                                                        isPinError -> Color(0xFFEF4444)
                                                        isFilled -> Color.White
                                                        else -> Color.Transparent
                                                    }
                                                )
                                                .border(
                                                    width = 1.5.dp,
                                                    color = when {
                                                        isPinError -> Color(0xFFEF4444)
                                                        isFilled -> Color.White
                                                        isActive -> Color.White.copy(alpha = 0.90f)
                                                        else -> Color.White.copy(alpha = 0.40f)
                                                    },
                                                    shape = CircleShape
                                                )
                                        )
                                    }
                                }

                                Spacer(Modifier.height(44.dp))

                                // 3x4 Uniform Frosted Keypad Grid (78dp circular buttons + Capsule Biometric Shortcut)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .widthIn(max = 296.dp),
                                    verticalArrangement = Arrangement.spacedBy(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    val keypadRows = listOf(
                                        listOf(KeypadData("1", ""), KeypadData("2", "ABC"), KeypadData("3", "DEF")),
                                        listOf(KeypadData("4", "GHI"), KeypadData("5", "JKL"), KeypadData("6", "MNO")),
                                        listOf(KeypadData("7", "PQRS"), KeypadData("8", "TUV"), KeypadData("9", "WXYZ"))
                                    )

                                    for (row in keypadRows) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            for (key in row) {
                                                GlassKeypadButton(
                                                    hazeState = hazeState,
                                                    enabled = lockoutSecondsRemaining == 0,
                                                    onClick = { onDigitEntered(key.digit) }
                                                ) {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        Text(
                                                            text = key.digit,
                                                            fontSize = 30.sp,
                                                            fontWeight = FontWeight.Normal,
                                                            lineHeight = 30.sp,
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

                                    // Row 4: [Use Biometric] Capsule Pill | 0 Digit | [Delete] Backspace
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Left: Capsule Pill Button for Biometrics
                                        Box(
                                            modifier = Modifier.size(width = 78.dp, height = 78.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            GlassCapsuleButton(
                                                hazeState = hazeState,
                                                enabled = lockoutSecondsRemaining == 0,
                                                onClick = { triggerBiometricPrompt() },
                                                modifier = Modifier
                                                    .width(108.dp)
                                                    .height(48.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center,
                                                    modifier = Modifier.padding(horizontal = 8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Lucide.Fingerprint,
                                                        contentDescription = "Use Biometric",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(19.dp)
                                                    )
                                                    Spacer(Modifier.width(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .width(1.dp)
                                                            .height(18.dp)
                                                            .background(Color.White.copy(alpha = 0.28f))
                                                    )
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(
                                                        text = "Use Biometric",
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color.White,
                                                        fontFamily = InterFontFamily,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }

                                        // Center: 0 Digit
                                        GlassKeypadButton(
                                            hazeState = hazeState,
                                            enabled = lockoutSecondsRemaining == 0,
                                            onClick = { onDigitEntered("0") }
                                        ) {
                                            Text(
                                                text = "0",
                                                fontSize = 30.sp,
                                                fontWeight = FontWeight.Normal,
                                                lineHeight = 30.sp,
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
                                        }

                                        // Right: Backspace Button
                                        GlassKeypadButton(
                                            hazeState = hazeState,
                                            enabled = lockoutSecondsRemaining == 0,
                                            onClick = { onPinDelete() }
                                        ) {
                                            Icon(
                                                imageVector = Lucide.Delete,
                                                contentDescription = "Delete digit",
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(28.dp))

                                Text(
                                    text = if (lockoutSecondsRemaining > 0) "Keypad locked due to security" else "Enter any 4 digits to unlock",
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

                    LockScreenView.UNLOCKED_CONFIRMATION -> {
                        // ── SCREEN 3: EXACT OSCELLION 3D LOADER & OVERFLOWED CHECKMARK MORPH ───
                        OscellionExactMorphScreen()
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
                    .then(
                        if (useBlur) {
                            Modifier.hazeChild(
                                state = hazeState,
                                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                                style = HazeDefaults.style(
                                    tint = Color(0xFF0B0D13).copy(alpha = 0.88f),
                                    blurRadius = 24.dp,
                                    noiseFactor = 0f
                                )
                            )
                        } else Modifier
                    )
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
                                    ToastManager.showToast("Wallpaper updated")
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
                    // =============================================================
                    // 1. ADD PHOTO FROM GALLERY CARD (Matches HTML interface)
                    // =============================================================
                    Box(
                        modifier = Modifier
                            .size(width = 74.dp, height = 112.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.30f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                galleryLauncher.launch("image/*")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.10f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Lucide.Plus,
                                    contentDescription = "Add Photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Add Photo",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.85f),
                                fontFamily = InterFontFamily,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Custom Gallery Wallpaper (if picked)
                    val customUri = customGalleryWallpaperUri
                    if (customUri != null) {
                        val isCustomSelected = pendingWallpaperUri == customUri
                        Box(
                            modifier = Modifier
                                .size(width = 74.dp, height = 112.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    width = if (isCustomSelected) 2.dp else 1.dp,
                                    color = if (isCustomSelected) Color(0xFF167EFF) else Color.White.copy(alpha = 0.20f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    pendingWallpaperUri = customUri
                                }
                        ) {
                            AsyncImage(
                                model = customUri,
                                contentDescription = "Custom Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                        )
                                    ),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Text(
                                    text = "Custom",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                    fontFamily = InterFontFamily,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                        }
                    }

                    // Built-in Wallpaper Presets
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
 * Exact Oscellion 3D Loader & Overflowed Checkmark Morph Screen.
 * Recreates the exact mathematical engine from e:\Design & 3D Assets\biometric_pin_lock_screen.html:
 * - RADIUS = 420 on 1024x1024 coordinate system.
 * - Portal opens at 339.87° with 332° sweep.
 * - Oscellion logo morphs out while emerald overflowed checkmark sweeps through portal.
 * - Dynamic Title morphs from "Verifying..." to "App Unlocked".
 */
@Composable
private fun OscellionExactMorphScreen() {
    val animTime = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animTime.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1150, easing = LinearEasing)
        )
    }

    val progress = animTime.value

    // Animation phases matching HTML
    // Phase 1: 0.0 - 0.32: Loading spin & 3D logo
    // Phase 2: 0.32 - 1.0: Morph to checkmark & sweep tick through portal
    val morphProgress = ((progress - 0.32f) / 0.68f).coerceIn(0f, 1f)
    val isMorphing = progress >= 0.32f

    val emeraldColor = Color(0xFF10B981)
    val ringColor = if (isMorphing) {
        val tColor = ((morphProgress - 0.12f) / 0.55f).coerceIn(0f, 1f)
        lerpColor(Color.White, emeraldColor, tColor)
    } else {
        Color.White
    }

    val tickDrawProgress = if (isMorphing) {
        val tTickRaw = ((morphProgress - 0.28f) / 0.58f).coerceIn(0f, 1f)
        // easeOutExpo approximation
        if (tTickRaw == 1f) 1f else 1f - Math.pow(2.0, -10.0 * tTickRaw).toFloat()
    } else 0f

    val titleText = if (morphProgress >= 0.65f) "App Unlocked" else "Verifying..."

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040609).copy(alpha = 0.94f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(134.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasSize = size.minDimension
                    val scaleFactor = canvasSize / 1024f

                    scale(scaleFactor) {
                        val centerOffset = Offset(512f, 512f)
                        val radius = 420f

                        // 1. Background Track Ring
                        val trackSweep = if (isMorphing) {
                            lerpFloat(360f, 332f, morphProgress)
                        } else 360f
                        val trackAlpha = if (isMorphing) {
                            (1f - morphProgress * 1.5f).coerceIn(0f, 1f)
                        } else 1f

                        if (trackAlpha > 0f) {
                            drawArc(
                                color = Color(0xFF1F1F23).copy(alpha = trackAlpha),
                                startAngle = 339.87f,
                                sweepAngle = trackSweep,
                                useCenter = false,
                                topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                                size = Size(radius * 2, radius * 2),
                                style = Stroke(width = 44f, cap = StrokeCap.Round)
                            )
                        }

                        // 2. Active Animated Ring
                        val activeStartAngle: Float
                        val activeSweepAngle: Float
                        if (!isMorphing) {
                            val spinAngle = (progress * 360f * 2.5f) % 360f
                            activeStartAngle = spinAngle
                            activeSweepAngle = 140f
                        } else {
                            val tRing = (morphProgress / 0.62f).coerceIn(0f, 1f)
                            activeStartAngle = lerpFloat(300f, 339.87f, tRing)
                            activeSweepAngle = lerpFloat(140f, 332f, tRing)
                        }

                        drawArc(
                            color = ringColor,
                            startAngle = activeStartAngle,
                            sweepAngle = activeSweepAngle,
                            useCenter = false,
                            topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = 44f, cap = StrokeCap.Round)
                        )

                        // 3. 3D Oscellion Logo in Center (Morph-out on checkmark)
                        val logoAlpha = if (isMorphing) {
                            (1f - (morphProgress / 0.42f)).coerceIn(0f, 1f)
                        } else 1f

                        if (logoAlpha > 0f) {
                            val logoScale = if (isMorphing) {
                                lerpFloat(1f, 0.28f, morphProgress)
                            } else 1f

                            scale(logoScale, pivot = centerOffset) {
                                // Draw centered Oscellion logo geometry
                                drawCircle(
                                    color = Color.White.copy(alpha = logoAlpha),
                                    radius = 160f,
                                    center = centerOffset,
                                    style = Stroke(width = 44f)
                                )
                                drawCircle(
                                    color = Color(0xFFD4D4D8).copy(alpha = logoAlpha * 0.8f),
                                    radius = 80f,
                                    center = centerOffset
                                )
                            }
                        }

                        // 4. Overflowed Checkmark Path: M 308 508 L 468 668 L 952 184
                        if (tickDrawProgress > 0f) {
                            val tickPath = Path().apply {
                                moveTo(308f, 508f)
                                lineTo(468f, 668f)
                                lineTo(952f, 184f)
                            }

                            val measure = android.graphics.PathMeasure()
                            measure.setPath(tickPath.asAndroidPath(), false)
                            val totalLength = measure.length
                            val dst = android.graphics.Path()
                            measure.getSegment(0f, totalLength * tickDrawProgress, dst, true)

                            drawPath(
                                path = dst.asComposePath(),
                                color = emeraldColor,
                                style = Stroke(
                                    width = 54f,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }

                        // 5. Echo Pulse Ring (t > 0.55)
                        if (morphProgress > 0.55f) {
                            val tEcho = ((morphProgress - 0.55f) / 0.45f).coerceIn(0f, 1f)
                            val echoScale = lerpFloat(1.0f, 1.14f, tEcho)
                            val echoAlpha = lerpFloat(0.32f, 0f, tEcho)

                            scale(echoScale, pivot = centerOffset) {
                                drawArc(
                                    color = emeraldColor.copy(alpha = echoAlpha),
                                    startAngle = 339.87f,
                                    sweepAngle = 332f,
                                    useCenter = false,
                                    topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                                    size = Size(radius * 2, radius * 2),
                                    style = Stroke(width = 24f, cap = StrokeCap.Round)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(26.dp))

            Text(
                text = titleText,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                fontFamily = InterFontFamily,
                letterSpacing = (-0.02).sp
            )
        }
    }
}

/**
 * Specular Frosted Glass Capsule Keypad Button (Capsule pill for Biometric Switch)
 * Matches the capsule button from the lock screen specification:
 * - Rounded pill shape with vertical divider between fingerprint and text
 * - Real-time backdrop blur, frosted glass bevel highlight, and tactile press feedback
 */
@Composable
private fun GlassCapsuleButton(
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val useBlur = rememberUseRealtimeBlur()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.94f else 1f,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.6f),
        label = "glassCapsuleScale"
    )

    Box(
        modifier = modifier
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
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Specular Frosted Glass Circular Keypad Button (78dp x 78dp)
 * Matches `.keypad-btn` from e:\Design & 3D Assets\biometric_pin_lock_screen.html:
 * - 78px uniform circle with real-time backdrop blur and specular vertical bevel highlight
 * - 0.92 scale feedback on press
 */
@Composable
private fun GlassKeypadButton(
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val useBlur = rememberUseRealtimeBlur()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.92f else 1f,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.6f),
        label = "glassKeypadScale"
    )

    Box(
        modifier = modifier
            .size(78.dp)
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
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction.coerceIn(0f, 1f)
}

private fun lerpColor(start: Color, stop: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (stop.red - start.red) * f,
        green = start.green + (stop.green - start.green) * f,
        blue = start.blue + (stop.blue - start.blue) * f,
        alpha = start.alpha + (stop.alpha - start.alpha) * f
    )
}

private data class KeypadData(val digit: String, val sublabel: String)
private data class WallpaperPreset(val name: String, val uri: String)
