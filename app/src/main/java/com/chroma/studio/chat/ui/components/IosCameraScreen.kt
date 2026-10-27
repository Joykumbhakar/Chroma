package com.chroma.studio.chat.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.view.ScaleGestureDetector
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.chroma.studio.chat.media.MediaStoreHelper
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.chat.util.PermissionHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executor
import kotlin.math.roundToInt

enum class IosCameraMode(val title: String) {
    PHOTO("PHOTO"),
    VIDEO("VIDEO"),
    PORTRAIT("PORTRAIT"),
    SQUARE("SQUARE")
}

enum class IosFlashMode {
    OFF,
    AUTO,
    ON
}

/**
 * Authentic Apple iOS In-App Camera Screen.
 * Implements full-screen CameraX viewfinder, iOS zoom capsule (.5, 1x, 2, 3),
 * swipeable camera mode strip with yellow active indicator (#FFCC00),
 * iOS concentric ring shutter button, front/back flip with 3D rotation,
 * tap-to-focus yellow box, and an instant photo review screen with caption input.
 */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun IosCameraScreen(
    onDismiss: () -> Unit,
    onPhotoCaptured: (Uri, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(PermissionHelper.hasCameraPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Camera permission required",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
        return
    }

    // Camera State
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var flashMode by remember { mutableStateOf(IosFlashMode.OFF) }
    var selectedMode by remember { mutableStateOf(IosCameraMode.PHOTO) }
    var currentZoomRatio by remember { mutableFloatStateOf(1f) }
    var minZoom by remember { mutableFloatStateOf(1f) }
    var maxZoom by remember { mutableFloatStateOf(8f) }
    var isTimerActive by remember { mutableStateOf(false) }
    var timerSeconds by remember { mutableIntStateOf(0) }
    var countdownValue by remember { mutableIntStateOf(0) }

    // Tap to focus state
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var isFocusing by remember { mutableStateOf(false) }

    // Review Captured Media State
    var capturedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var capturedPhotoFile by remember { mutableStateOf<File?>(null) }
    var captionText by remember { mutableStateOf("") }
    var isReviewing by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }

    // Recent device media thumbnail for bottom-left gallery preview
    var recentMediaUri by remember { mutableStateOf<Uri?>(null) }
    LaunchedEffect(Unit) {
        try {
            val media = MediaStoreHelper.loadDeviceMedia(context)
            recentMediaUri = media.firstOrNull()?.uri
        } catch (e: Exception) {}
    }

    // CameraX Controllers
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var cameraControl: CameraControl? by remember { mutableStateOf(null) }
    var cameraInfo: CameraInfo? by remember { mutableStateOf(null) }

    // If reviewing captured photo, show the iOS Review Screen
    if (isReviewing && capturedPhotoUri != null) {
        IosCameraReviewScreen(
            photoUri = capturedPhotoUri!!,
            captionText = captionText,
            onCaptionChange = { captionText = it },
            onRetake = {
                capturedPhotoFile?.delete()
                capturedPhotoUri = null
                capturedPhotoFile = null
                captionText = ""
                isReviewing = false
            },
            onSend = {
                val uriToSend = capturedPhotoUri!!
                val captionToSend = captionText.takeIf { it.isNotBlank() }
                onPhotoCaptured(uriToSend, captionToSend)
                onDismiss()
            }
        )
        return
    }

    // Full Camera UI
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ── 1. CameraX Viewfinder ──────────────────────────────────────────
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setFlashMode(
                            when (flashMode) {
                                IosFlashMode.ON -> ImageCapture.FLASH_MODE_ON
                                IosFlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
                                IosFlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
                            }
                        )
                        .build()

                    imageCapture = capture

                    try {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            capture
                        )
                        cameraControl = camera.cameraControl
                        cameraInfo = camera.cameraInfo

                        camera.cameraInfo.zoomState.observe(lifecycleOwner) { zoomState ->
                            currentZoomRatio = zoomState.zoomRatio
                            minZoom = zoomState.minZoomRatio
                            maxZoom = zoomState.maxZoomRatio
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                // Pinch to Zoom listener
                val scaleGestureDetector = ScaleGestureDetector(ctx, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    override fun onScale(detector: ScaleGestureDetector): Boolean {
                        val currentZoom = currentZoomRatio
                        val delta = detector.scaleFactor
                        val newZoom = (currentZoom * delta).coerceIn(minZoom, maxZoom)
                        cameraControl?.setZoomRatio(newZoom)
                        return true
                    }
                })

                previewView.setOnTouchListener { _, event ->
                    scaleGestureDetector.onTouchEvent(event)
                    false
                }

                previewView
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        focusPoint = offset
                        isFocusing = true
                        cameraControl?.let { control ->
                            val meteringPointFactory = SurfaceOrientedMeteringPointFactory(size.width.toFloat(), size.height.toFloat())
                            val meteringPoint = meteringPointFactory.createPoint(offset.x, offset.y)
                            val action = FocusMeteringAction.Builder(meteringPoint).build()
                            control.startFocusAndMetering(action)
                        }
                    }
                }
        )

        // ── 2. Tap to Focus Yellow Square Indicator ────────────────────────
        focusPoint?.let { pt ->
            LaunchedEffect(pt) {
                isFocusing = true
                delay(1200)
                isFocusing = false
            }

            AnimatedVisibility(
                visible = isFocusing,
                enter = scaleIn(initialScale = 1.35f, animationSpec = tween(180)) + fadeIn(),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = (pt.x - 36).dp, y = (pt.y - 36).dp)
                        .size(72.dp)
                        .border(1.2.dp, Color(0xFFFFCC00), RoundedCornerShape(2.dp))
                )
            }
        }

        // ── 3. Timer Countdown Overlay ─────────────────────────────────────
        if (countdownValue > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$countdownValue",
                    color = Color.White,
                    fontSize = 92.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ── 4. Flash White Shutter Animation ───────────────────────────────
        AnimatedVisibility(
            visible = isCapturing,
            enter = fadeIn(animationSpec = tween(50)),
            exit = fadeOut(animationSpec = tween(150))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            )
        }

        // ── 5. Top Bar: Flash, Timer, Close ────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Dismiss / Close Camera
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDismiss()
                    },
                contentAlignment = Alignment.Center
            ) {
                PhosphorCloseX(tint = Color.White, size = 16.dp)
            }

            // Center Actions: Flash & Timer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Flash Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (flashMode != IosFlashMode.OFF) Color(0xFFFFCC00) else Color.Black.copy(alpha = 0.45f))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            flashMode = when (flashMode) {
                                IosFlashMode.OFF -> IosFlashMode.AUTO
                                IosFlashMode.AUTO -> IosFlashMode.ON
                                IosFlashMode.ON -> IosFlashMode.OFF
                            }
                            imageCapture?.flashMode = when (flashMode) {
                                IosFlashMode.ON -> ImageCapture.FLASH_MODE_ON
                                IosFlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
                                IosFlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (flashMode) {
                            IosFlashMode.ON -> "⚡"
                            IosFlashMode.AUTO -> "⚡A"
                            IosFlashMode.OFF -> "⚡"
                        },
                        color = if (flashMode != IosFlashMode.OFF) Color.Black else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Timer Button (Off, 3s, 10s)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (timerSeconds > 0) Color(0xFFFFCC00) else Color.Black.copy(alpha = 0.45f))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            timerSeconds = when (timerSeconds) {
                                0 -> 3
                                3 -> 10
                                else -> 0
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (timerSeconds > 0) "${timerSeconds}s" else "⏱",
                        color = if (timerSeconds > 0) Color.Black else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Placeholder to balance header
            Spacer(Modifier.size(36.dp))
        }

        // ── 6. Bottom Controls: Zoom, Mode Carousel, Shutter Bar ────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Zoom Selector Capsule (.5, 1x, 2, 3)
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.50f))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val zoomLevels = listOf(1f, 2f, 3f)
                zoomLevels.forEach { zoom ->
                    val isSelected = (currentZoomRatio >= zoom - 0.2f && currentZoomRatio < zoom + 0.8f) || (zoom == 3f && currentZoomRatio >= 2.8f)
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color(0xFFFFCC00) else Color.Transparent)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                cameraControl?.setZoomRatio(zoom)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (zoom == 1f) "1x" else "${zoom.toInt()}",
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // iOS Camera Mode Carousel Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IosCameraMode.values().forEach { mode ->
                    val isSelected = selectedMode == mode
                    Text(
                        text = mode.title,
                        color = if (isSelected) Color(0xFFFFCC00) else Color.White.copy(alpha = 0.70f),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedMode = mode
                            }
                            .padding(vertical = 4.dp, horizontal = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Shutter Row: Gallery Thumb | Concentric Shutter Ring | Camera Flip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 36.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Bottom-Left: Recent Gallery Photo Thumbnail
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color.White.copy(alpha = 0.75f), CircleShape)
                        .background(Color.DarkGray)
                        .clickable {
                            // Opens gallery
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (recentMediaUri != null) {
                        AsyncImage(
                            model = recentMediaUri,
                            contentDescription = "Recent Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        PhosphorPhotoPlaceholder(tint = Color.White, size = 20.dp)
                    }
                }

                // Center: Concentric Apple Shutter Button
                val shutterInteractionSource = remember { MutableInteractionSource() }
                val isShutterPressed by shutterInteractionSource.collectIsPressedAsState()
                val shutterInnerScale by animateFloatAsState(
                    targetValue = if (isShutterPressed) 0.86f else 1f,
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
                    label = "shutterScale"
                )

                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(4.dp)
                        .clickable(
                            interactionSource = shutterInteractionSource,
                            indication = null
                        ) {
                            coroutineScope.launch {
                                if (timerSeconds > 0) {
                                    countdownValue = timerSeconds
                                    while (countdownValue > 0) {
                                        SoundManager.playReaction()
                                        delay(1000)
                                        countdownValue--
                                    }
                                }

                                isCapturing = true
                                SoundManager.playReaction()
                                delay(120)
                                isCapturing = false

                                // Capture real photo
                                val photoFile = File(context.cacheDir, "camera_capture_${System.currentTimeMillis()}.jpg")
                                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                                imageCapture?.takePicture(
                                    outputOptions,
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                            val savedUri = Uri.fromFile(photoFile)
                                            capturedPhotoUri = savedUri
                                            capturedPhotoFile = photoFile
                                            isReviewing = true
                                        }

                                        override fun onError(exc: ImageCaptureException) {
                                            exc.printStackTrace()
                                        }
                                    }
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(shutterInnerScale)
                            .clip(CircleShape)
                            .background(if (selectedMode == IosCameraMode.VIDEO) Color(0xFFFF3B30) else Color.White)
                    )
                }

                // Bottom-Right: Camera Flip (Front/Back)
                var flipRotation by remember { mutableFloatStateOf(0f) }
                val animatedRotation by animateFloatAsState(
                    targetValue = flipRotation,
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
                    label = "flipCameraRotation"
                )

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            flipRotation += 180f
                            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            } else {
                                CameraSelector.DEFAULT_BACK_CAMERA
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    PhosphorCameraSwitchIcon(
                        tint = Color.White,
                        size = 24.dp,
                        modifier = Modifier.rotate(animatedRotation)
                    )
                }
            }
        }
    }
}

/**
 * Apple iOS Camera Review Screen for captured photo.
 * Allows entering an optional caption and tapping "Send" directly into the chat.
 */
@Composable
fun IosCameraReviewScreen(
    photoUri: Uri,
    captionText: String,
    onCaptionChange: (String) -> Unit,
    onRetake: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Captured Photo
        AsyncImage(
            model = photoUri,
            contentDescription = "Captured Photo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Top Controls: Retake Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onRetake()
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Retake",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Bottom Action Bar: Caption Field + Blue Send Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(26.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BasicTextField(
                    value = captionText,
                    onValueChange = onCaptionChange,
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(fontSize = 14.5.sp, color = Color.White),
                    cursorBrush = SolidColor(Color(0xFF007AFF)),
                    decorationBox = { innerTextField ->
                        if (captionText.isEmpty()) {
                            Text(
                                text = "Add a caption...",
                                fontSize = 14.5.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                        innerTextField()
                    }
                )

                // Send Button
                PhosphorSendArrowIcon(
                    tint = Color(0xFF007AFF),
                    size = 28.dp,
                    modifier = Modifier
                        .clickable {
                            SoundManager.playSend()
                            onSend()
                        }
                        .padding(4.dp)
                )
            }
        }
    }
}

@Composable
private fun PhosphorPhotoPlaceholder(tint: Color, size: androidx.compose.ui.unit.Dp) {
    Canvas(modifier = Modifier.size(size)) {
        drawCircle(color = tint.copy(alpha = 0.5f), radius = size.toPx() / 2.5f)
    }
}

@Composable
private fun PhosphorCameraSwitchIcon(tint: Color, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val stroke = Stroke(width = 2.dp.toPx())
        val radius = size.toPx() * 0.38f
        val centerPt = Offset(size.toPx() / 2, size.toPx() / 2)
        drawArc(
            color = tint,
            startAngle = 45f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = Offset(centerPt.x - radius, centerPt.y - radius),
            size = Size(radius * 2, radius * 2),
            style = stroke
        )
        drawArc(
            color = tint,
            startAngle = 225f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = Offset(centerPt.x - radius, centerPt.y - radius),
            size = Size(radius * 2, radius * 2),
            style = stroke
        )
    }
}
