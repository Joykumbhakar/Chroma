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
import androidx.compose.ui.graphics.drawscope.withTransform
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
import coil.request.ImageRequest
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.chat.util.BiometricHelper
import com.chroma.studio.chat.util.findActivity
import com.chroma.studio.security.SecurityPinManager
import com.chroma.studio.ui.theme.InterFontFamily
import com.composables.icons.lucide.*
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
    onBack: (() -> Unit)? = null,
    onUnlockSuccess: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val activity = remember(context) { context.findActivity() }

    var hasPinConfigured by remember { mutableStateOf(SecurityPinManager.hasPin(context)) }
    val isBiometricEnabled by remember(context) {
        mutableStateOf(SecurityPinManager.isBiometricEnabled(context))
    }
    val initialView = remember(isBiometricEnabled) {
        if (isBiometricEnabled) LockScreenView.BIOMETRIC_CHOICE else LockScreenView.PIN_ENTRY
    }
    var currentView by remember(initialView) { mutableStateOf(initialView) }
    var enteredPin by remember { mutableStateOf("") }
    var isPinError by remember { mutableStateOf(false) }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var lockBannerMessage by remember { mutableStateOf<String?>(null) }
    val shakeAnim = remember { Animatable(0f) }

    // Failed attempts counter and lockout timer synced with persistent SecurityPinManager storage
    var failedAttempts by remember { mutableIntStateOf(SecurityPinManager.getFailedAttempts(context)) }
    var lockoutSecondsRemaining by remember {
        mutableIntStateOf(SecurityPinManager.getRemainingLockoutSeconds(context))
    }

    // Lockout countdown timer synced against wall clock so closing the activity cannot reset cooldown
    LaunchedEffect(lockoutSecondsRemaining) {
        if (lockoutSecondsRemaining > 0) {
            delay(1000)
            val remaining = SecurityPinManager.getRemainingLockoutSeconds(context)
            lockoutSecondsRemaining = remaining
            if (remaining == 0) {
                failedAttempts = SecurityPinManager.getFailedAttempts(context)
            }
        }
    }

    // Wallpaper and dimming customization state
    var wallpaperUri by remember { mutableStateOf(SecurityPinManager.getWallpaperUri(context)) }
    var wallpaperDimming by remember { mutableFloatStateOf(SecurityPinManager.getWallpaperDimming(context)) }

    var isWallpaperEditing by remember { mutableStateOf(false) }
    var isSettingWallpaper by remember { mutableStateOf(false) }
    var isWallpaperSetCompleted by remember { mutableStateOf(false) }
    var pendingWallpaperUri by remember { mutableStateOf(wallpaperUri) }
    var pendingDimming by remember { mutableFloatStateOf(wallpaperDimming) }
    var customWallpapers by remember { mutableStateOf(SecurityPinManager.getCustomWallpapers(context)) }
    var pendingDeleteWallpaperUri by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isWallpaperEditing) {
        if (isWallpaperEditing) {
            customWallpapers = SecurityPinManager.getCustomWallpapers(context)
        }
    }

    // Gallery Custom Photo Picker
    val galleryLauncher = com.chroma.studio.ui.components.rememberOscellionMediaPicker(
        title = "Choose Lock Wallpaper",
        mediaFilter = com.chroma.studio.ui.components.GalleryMediaTypeFilter.PHOTOS,
        showAlbumsTab = true
    ) { uri ->
        scope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val targetFile = File(context.filesDir, "custom_wallpaper_${System.currentTimeMillis()}.jpg")
                inputStream?.use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                val localUri = Uri.fromFile(targetFile).toString()
                SecurityPinManager.addCustomWallpaper(context, localUri)
                val updatedList = SecurityPinManager.getCustomWallpapers(context)
                withContext(Dispatchers.Main) {
                    customWallpapers = updatedList
                    pendingWallpaperUri = localUri
                    lockBannerMessage = "Custom wallpaper added"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    lockBannerMessage = "Failed to load photo: ${e.message}"
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
        } else if (currentView == LockScreenView.PIN_ENTRY && isBiometricEnabled) {
            currentView = LockScreenView.BIOMETRIC_CHOICE
            enteredPin = ""
        } else {
            if (onBack != null) {
                onBack()
            } else {
                (activity ?: (context as? Activity))?.finish()
            }
        }
    }

    // Biometric authentication trigger
    fun triggerBiometricPrompt() {
        if (lockoutSecondsRemaining > 0) {
            lockBannerMessage = "Too many attempts. Try again in ${lockoutSecondsRemaining}s."
            return
        }
        val act = activity ?: return
        if (BiometricHelper.isPromptShowing) return
        lockBannerMessage = null

        BiometricHelper.authenticate(
            activity = act,
            title = "Unlock Oscellion",
            subtitle = "Verify your identity to access the app",
            onSuccess = {
                SecurityPinManager.markUnlocked()
                SecurityPinManager.resetFailedPinAttempts(context)
                SoundManager.playLockSuccess(context)
                failedAttempts = 0
                lockoutSecondsRemaining = 0
                lockBannerMessage = null
                currentView = LockScreenView.UNLOCKED_CONFIRMATION
            },
            onCancel = {},
            onError = { err ->
                lockBannerMessage = err
            }
        )
    }

    // Auto-prompt biometrics once on initial presentation when Biometric is enabled
    LaunchedEffect(isBiometricEnabled) {
        if (isBiometricEnabled) {
            delay(280)
            triggerBiometricPrompt()
        } else if (!hasPinConfigured) {
            showPinSetupDialog = true
        }
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
            val nextState = SecurityPinManager.recordFailedPinAttempt(context)
            failedAttempts = nextState.failedAttempts
            isPinError = true
            SoundManager.playLockError()
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

            shakeAnim.snapTo(0f)
            try {
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
            } finally {
                shakeAnim.snapTo(0f)
            }

            if (nextState.lockoutSeconds > 0) {
                lockoutSecondsRemaining = nextState.lockoutSeconds
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
                if (!SecurityPinManager.hasPin(context)) {
                    lockBannerMessage = "No PIN configured yet. Set a 4-digit PIN first."
                    enteredPin = ""
                    showPinSetupDialog = true
                    return
                }
                val isValid = SecurityPinManager.verifyPin(context, newPin)
                if (isValid) {
                    SecurityPinManager.markUnlocked()
                    SecurityPinManager.resetFailedPinAttempts(context)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    SoundManager.playLockSuccess(context)
                    failedAttempts = 0
                    lockoutSecondsRemaining = 0
                    lockBannerMessage = null
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
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "screenScale"
    )
    val screenOffsetY by animateDpAsState(
        targetValue = if (isWallpaperEditing) (-32).dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "screenOffsetY"
    )
    val screenCornerRadius by animateDpAsState(
        targetValue = if (isWallpaperEditing) 34.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
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
            modifier = Modifier.fillMaxSize()
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(if (isWallpaperEditing) pendingWallpaperUri else wallpaperUri)
                    .crossfade(true)
                    .fallback(com.chroma.studio.R.drawable.wallpaper_stock)
                    .error(com.chroma.studio.R.drawable.wallpaper_stock)
                    .build(),
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
        val safeCornerRadius = screenCornerRadius.coerceAtLeast(0.dp)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = screenScale.coerceIn(0.5f, 1.5f)
                    scaleY = screenScale.coerceIn(0.5f, 1.5f)
                    translationY = screenOffsetY.toPx()
                }
                .clip(RoundedCornerShape(safeCornerRadius))
                .then(
                    if (isWallpaperEditing && safeCornerRadius > 0.dp) {
                        Modifier.border(
                            width = 1.5.dp,
                            color = Color.White.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(safeCornerRadius)
                        )
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = currentView,
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
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
                            // Top row: Borderless Back/Close button + Wallpaper hint
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 14.dp)
                            ) {
                                if (onBack != null) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .size(40.dp)
                                            .clickable { onBack() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Lucide.X,
                                            contentDescription = "Exit to Home",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Hold wallpaper to customize",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.65f),
                                    fontFamily = InterFontFamily,
                                    letterSpacing = 0.3.sp,
                                    modifier = Modifier.align(Alignment.Center)
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
                                    text = titleText,
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

                                Spacer(Modifier.height(22.dp))

                                if (!lockBannerMessage.isNullOrBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .widthIn(max = 334.dp)
                                            .padding(bottom = 16.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0x38FF3B30))
                                            .border(0.8.dp, Color(0xFFFF453A).copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                            .padding(horizontal = 14.dp, vertical = 9.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = lockBannerMessage ?: "",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White,
                                            textAlign = TextAlign.Center,
                                            fontFamily = InterFontFamily
                                        )
                                    }
                                } else {
                                    Spacer(Modifier.height(22.dp))
                                }

                                // Action Buttons Stack (Max width 334dp)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .widthIn(max = 334.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
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
                                            .height(54.dp)
                                            .graphicsLayer {
                                                scaleX = bioScale
                                                scaleY = bioScale
                                            }
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                brush = Brush.horizontalGradient(
                                                    listOf(Color(0xFF0381FE), Color(0xFF0072DE))
                                                )
                                            )
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
                                            .height(54.dp)
                                            .graphicsLayer {
                                                scaleX = pinScale
                                                scaleY = pinScale
                                            }
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                if (pinPressed) Color(0xFF222834).copy(alpha = 0.85f)
                                                else Color(0xFF141822).copy(alpha = 0.72f)
                                            )
                                            .border(0.5.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
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
                                .padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Column(
                                modifier = Modifier
                                    .wrapContentSize()
                                    .offset(x = shakeAnim.value.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                OscellionPinEntrySection(
                                    title = "Enter PIN",
                                    subtitle = when {
                                        lockoutSecondsRemaining > 0 -> "Too many attempts. Try again in ${lockoutSecondsRemaining}s"
                                        !lockBannerMessage.isNullOrBlank() -> lockBannerMessage!!
                                        isPinError -> if (failedAttempts >= 2) "Incorrect PIN (attempt $failedAttempts of 5). Try again." else "Incorrect PIN. Try again."
                                        failedAttempts > 0 -> "Incorrect PIN (attempt $failedAttempts of 5). Try again."
                                        !hasPinConfigured -> "Tap Keypad after setup to set a 4-digit PIN"
                                        else -> "Enter your 4-digit PIN to unlock"
                                    },
                                    enteredPin = enteredPin,
                                    isError = isPinError,
                                    lockoutSecondsRemaining = lockoutSecondsRemaining,
                                    hintText = if (hasPinConfigured) "Use Biometric or your 4-digit PIN" else "No PIN set yet",
                                    buttonSize = 76.dp,
                                    horizontalSpacing = 20.dp,
                                    onDigitClick = { onDigitEntered(it) },
                                    onDeleteClick = { onPinDelete() },
                                    actionSlotLeft = if (isBiometricEnabled) {
                                        {
                                            OscellionBiometricKeypadButton(
                                                size = 76.dp,
                                                enabled = lockoutSecondsRemaining == 0,
                                                onClick = { triggerBiometricPrompt() }
                                            )
                                        }
                                    } else null
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
                    .background(Color(0xFF0B0D13).copy(alpha = 0.95f))
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
                        .clip(RoundedCornerShape(2.dp))
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

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Borderless Cancel Action
                        Text(
                            text = "Cancel",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.80f),
                            fontFamily = InterFontFamily,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    SoundManager.playTap(context)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    pendingWallpaperUri = wallpaperUri
                                    pendingDimming = wallpaperDimming
                                    isWallpaperEditing = false
                                }
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                        )

                        // Set Action Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0381FE))
                                .clickable {
                                    try {
                                        SoundManager.playReaction()
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        isSettingWallpaper = true
                                        isWallpaperSetCompleted = false
                                        scope.launch {
                                            wallpaperUri = pendingWallpaperUri
                                            wallpaperDimming = pendingDimming
                                            SecurityPinManager.setWallpaperUri(context, wallpaperUri)
                                            SecurityPinManager.setWallpaperDimming(context, wallpaperDimming)
                                            if (wallpaperUri.startsWith("file://") && !wallpaperUri.contains("android_asset")) {
                                                SecurityPinManager.addCustomWallpaper(context, wallpaperUri)
                                                customWallpapers = SecurityPinManager.getCustomWallpapers(context)
                                            }
                                            kotlinx.coroutines.delay(350)
                                            isWallpaperSetCompleted = true
                                            kotlinx.coroutines.delay(700)
                                            isSettingWallpaper = false
                                            isWallpaperEditing = false
                                            lockBannerMessage = null
                                        }
                                    } catch (e: Exception) {
                                        isSettingWallpaper = false
                                        lockBannerMessage = "Failed to save wallpaper: ${e.message}"
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "Set",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                fontFamily = InterFontFamily
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Real-Time Wallpaper Dimming Row (borderless hairline section)
                HorizontalDivider(color = Color.White.copy(alpha = 0.10f), thickness = 0.5.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
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
                                SecurityPinManager.markInternalNavigation(30_000L)
                                galleryLauncher.launch("image/*")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Lucide.Plus,
                                contentDescription = "Add Photo",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
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

                    // =============================================================
                    // 2. ALL PREVIOUSLY IMPORTED CUSTOM WALLPAPERS
                    // =============================================================
                    customWallpapers.forEachIndexed { index, customUri ->
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
                                    SoundManager.playTap(context)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    pendingWallpaperUri = customUri
                                }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(customUri)
                                    .crossfade(true)
                                    .fallback(com.chroma.studio.R.drawable.wallpaper_stock)
                                    .error(com.chroma.studio.R.drawable.wallpaper_stock)
                                    .build(),
                                contentDescription = "Custom Photo ${index + 1}",
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
                                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                        )
                                    ),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Text(
                                    text = if (customWallpapers.size == 1) "Custom" else "Custom ${index + 1}",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                    fontFamily = InterFontFamily,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }

                            // Quick delete action (Top Right) - borderless icon, asks confirmation before deleting
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(24.dp)
                                    .clickable {
                                        SoundManager.playTap(context)
                                        pendingDeleteWallpaperUri = customUri
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Lucide.Trash2,
                                    contentDescription = "Delete wallpaper",
                                    tint = Color.White.copy(alpha = 0.92f),
                                    modifier = Modifier.size(13.dp)
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
                                    SoundManager.playTap(context)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                hasPinConfigured = true
                lockBannerMessage = null
                currentView = LockScreenView.PIN_ENTRY
                enteredPin = ""
            }
        )
    }

    // Confirmation dialog before deleting a custom wallpaper
    pendingDeleteWallpaperUri?.let { targetUri ->
        IosAlertDialog(
            title = "Delete Custom Wallpaper?",
            message = "This removes the selected custom wallpaper from your lock screen gallery.",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = {
                pendingDeleteWallpaperUri = null
                SecurityPinManager.deleteCustomWallpaper(context, targetUri)
                val updated = SecurityPinManager.getCustomWallpapers(context)
                customWallpapers = updated
                if (pendingWallpaperUri == targetUri) {
                    pendingWallpaperUri = updated.firstOrNull() ?: SecurityPinManager.DEFAULT_WALLPAPER
                }
                if (wallpaperUri == targetUri) {
                    wallpaperUri = SecurityPinManager.DEFAULT_WALLPAPER
                }
                ToastManager.showToast("Wallpaper removed")
            },
            onDismiss = {
                pendingDeleteWallpaperUri = null
            }
        )
    }

    // Official Oscellion Morph Processing Dialog for Wallpaper Update
    OscellionMorphProcessingDialog(
        visible = isSettingWallpaper,
        isCompleted = isWallpaperSetCompleted,
        loadingText = "Applying Wallpaper...",
        completedText = "Wallpaper Applied"
    )
}

/**
 * Official Oscellion 3D Loader & Overflowed Green Checkmark Morph Screen.
 * Uses official OscellionMorphLoader to seamlessly morph the 3D rotating emblem
 * into the emerald checkmark upon successful unlock.
 */
@Composable
private fun OscellionExactMorphScreen() {
    var isDone by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(220)
        isDone = true
    }

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
                modifier = Modifier.size(134.dp),
                contentAlignment = Alignment.Center
            ) {
                OscellionMorphLoader(
                    modifier = Modifier.size(134.dp),
                    isCompleted = isDone,
                    color = Color.White,
                    checkColor = OscellionGreenCheck
                )
            }

            Spacer(Modifier.height(26.dp))

            Text(
                text = if (isDone) "App Unlocked" else "Verifying...",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                fontFamily = InterFontFamily,
                letterSpacing = (-0.02).sp
            )
        }
    }
}

private data class WallpaperPreset(val name: String, val uri: String)
