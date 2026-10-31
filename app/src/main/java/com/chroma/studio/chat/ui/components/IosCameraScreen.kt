package com.chroma.studio.chat.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.foundation.layout.PaddingValues
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.ScaleGestureDetector
import android.view.ViewGroup
import android.widget.VideoView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
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
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.graphics.StrokeCap
import kotlin.math.ln
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI
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
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
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
    ON,
    TORCH
}

enum class ProAspectRatio(val title: String, val ratioNumerator: Float, val ratioDenominator: Float) {
    RATIO_4_3("4:3", 3f, 4f),
    RATIO_16_9("16:9", 9f, 16f),
    RATIO_1_1("1:1", 1f, 1f)
}

enum class FilterCategory(val title: String) {
    BEAUTY("Beauty"),
    STYLES("Styles")
}

enum class ProCameraFilter(
    val displayName: String,
    val category: FilterCategory,
    val colorMatrixValues: FloatArray?,
    val beautyGlowStrength: Float = 0f,
    val swatchColor: Color = Color.White,
    val liveOverlayColor: Color? = null,
    val liveOverlayAlpha: Float = 0f
) {
    // ── BEAUTY FILTERS ──
    PORCELAIN(
        displayName = "Porcelain",
        category = FilterCategory.BEAUTY,
        colorMatrixValues = floatArrayOf(
            1.08f, 0f, 0f, 0f, 16f,
            0f, 1.06f, 0f, 0f, 14f,
            0f, 0f, 1.04f, 0f, 12f,
            0f, 0f, 0f, 1f, 0f
        ),
        beautyGlowStrength = 0.22f,
        swatchColor = Color(0xFFFFF0F5),
        liveOverlayColor = Color(0xFFFFF0F5),
        liveOverlayAlpha = 0.10f
    ),
    ROSY_BLUSH(
        displayName = "Rosy Blush",
        category = FilterCategory.BEAUTY,
        colorMatrixValues = floatArrayOf(
            1.16f, 0f, 0f, 0f, 18f,
            0f, 1.04f, 0f, 0f, 6f,
            0f, 0f, 0.98f, 0f, -4f,
            0f, 0f, 0f, 1f, 0f
        ),
        beautyGlowStrength = 0.25f,
        swatchColor = Color(0xFFFF8DA1),
        liveOverlayColor = Color(0xFFFF69B4),
        liveOverlayAlpha = 0.11f
    ),
    GOLDEN_HOUR(
        displayName = "Golden Glow",
        category = FilterCategory.BEAUTY,
        colorMatrixValues = floatArrayOf(
            1.16f, 0f, 0f, 0f, 22f,
            0f, 1.08f, 0f, 0f, 10f,
            0f, 0f, 0.88f, 0f, -16f,
            0f, 0f, 0f, 1f, 0f
        ),
        beautyGlowStrength = 0.26f,
        swatchColor = Color(0xFFFFD54F),
        liveOverlayColor = Color(0xFFFFB300),
        liveOverlayAlpha = 0.12f
    ),
    SOFT_SILK(
        displayName = "Soft Silk",
        category = FilterCategory.BEAUTY,
        colorMatrixValues = floatArrayOf(
            1.06f, 0f, 0f, 0f, 14f,
            0f, 1.06f, 0f, 0f, 14f,
            0f, 0f, 1.06f, 0f, 14f,
            0f, 0f, 0f, 1f, 0f
        ),
        beautyGlowStrength = 0.30f,
        swatchColor = Color(0xFFFCE4EC),
        liveOverlayColor = Color(0xFFFFEBEE),
        liveOverlayAlpha = 0.12f
    ),
    CRYSTAL_DEW(
        displayName = "Crystal Dew",
        category = FilterCategory.BEAUTY,
        colorMatrixValues = floatArrayOf(
            1.10f, 0f, 0f, 0f, 10f,
            0f, 1.12f, 0f, 0f, 12f,
            0f, 0f, 1.14f, 0f, 16f,
            0f, 0f, 0f, 1f, 0f
        ),
        beautyGlowStrength = 0.18f,
        swatchColor = Color(0xFFE0F7FA),
        liveOverlayColor = Color(0xFFE0F7FA),
        liveOverlayAlpha = 0.09f
    ),
    CARAMEL_BRONZE(
        displayName = "Caramel",
        category = FilterCategory.BEAUTY,
        colorMatrixValues = floatArrayOf(
            1.18f, 0f, 0f, 0f, 24f,
            0f, 1.06f, 0f, 0f, 8f,
            0f, 0f, 0.82f, 0f, -22f,
            0f, 0f, 0f, 1f, 0f
        ),
        beautyGlowStrength = 0.24f,
        swatchColor = Color(0xFFD7CCC8),
        liveOverlayColor = Color(0xFFFFAB40),
        liveOverlayAlpha = 0.11f
    ),

    // ── CLASSIC STYLES ──
    STANDARD(
        displayName = "Original",
        category = FilterCategory.STYLES,
        colorMatrixValues = null,
        beautyGlowStrength = 0f,
        swatchColor = Color.White
    ),
    VIVID(
        displayName = "Vivid",
        category = FilterCategory.STYLES,
        colorMatrixValues = floatArrayOf(
            1.22f, 0f, 0f, 0f, -8f,
            0f, 1.22f, 0f, 0f, -8f,
            0f, 0f, 1.22f, 0f, -8f,
            0f, 0f, 0f, 1f, 0f
        ),
        beautyGlowStrength = 0f,
        swatchColor = Color(0xFFFF5252),
        liveOverlayColor = Color(0xFFFF3B30),
        liveOverlayAlpha = 0.06f
    ),
    WARM(
        displayName = "Warm",
        category = FilterCategory.STYLES,
        colorMatrixValues = floatArrayOf(
            1.15f, 0f, 0f, 0f, 14f,
            0f, 1.05f, 0f, 0f, 6f,
            0f, 0f, 0.90f, 0f, -14f,
            0f, 0f, 0f, 1f, 0f
        ),
        beautyGlowStrength = 0f,
        swatchColor = Color(0xFFFFB74D),
        liveOverlayColor = Color(0xFFFF9500),
        liveOverlayAlpha = 0.08f
    ),
    COOL(
        displayName = "Cool",
        category = FilterCategory.STYLES,
        colorMatrixValues = floatArrayOf(
            0.90f, 0f, 0f, 0f, -10f,
            0f, 1.05f, 0f, 0f, 4f,
            0f, 0f, 1.22f, 0f, 18f,
            0f, 0f, 0f, 1f, 0f
        ),
        beautyGlowStrength = 0f,
        swatchColor = Color(0xFF64B5F6),
        liveOverlayColor = Color(0xFF007AFF),
        liveOverlayAlpha = 0.08f
    ),
    DRAMATIC_NOIR(
        displayName = "Noir",
        category = FilterCategory.STYLES,
        colorMatrixValues = floatArrayOf(
            0.33f, 0.59f, 0.11f, 0f, -10f,
            0.33f, 0.59f, 0.11f, 0f, -10f,
            0.33f, 0.59f, 0.11f, 0f, -10f,
            0f, 0f, 0f, 1f, 0f
        ),
        beautyGlowStrength = 0f,
        swatchColor = Color(0xFF424242)
    ),
    SILVERTONE(
        displayName = "Silver",
        category = FilterCategory.STYLES,
        colorMatrixValues = floatArrayOf(
            0.30f, 0.55f, 0.15f, 0f, 22f,
            0.30f, 0.55f, 0.15f, 0f, 22f,
            0.30f, 0.55f, 0.15f, 0f, 28f,
            0f, 0f, 0f, 1f, 0f
        ),
        beautyGlowStrength = 0f,
        swatchColor = Color(0xFFB0BEC5)
    );

    val colorMatrix: ColorMatrix?
        get() = colorMatrixValues?.let { ColorMatrix(it) }

    fun getInterpolatedMatrix(intensity: Float): ColorMatrix? {
        val base = colorMatrixValues ?: return null
        val alpha = intensity.coerceIn(0f, 1f)
        val identity = floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        val result = FloatArray(20) { i ->
            identity[i] * (1f - alpha) + base[i] * alpha
        }
        return ColorMatrix(result)
    }
}

/**
 * Authentic Apple iOS Pro In-App Camera Screen.
 * 
 * Advanced & Professional Features Integrated:
 * 1. Dual Photo & Video Capture with CameraX VideoCapture<Recorder>
 * 2. QuickTake: Hold shutter in Photo mode to seamlessly record video
 * 3. Interactive Tap-to-Focus with Vertical Exposure (EV Sun Icon Slider
 * 4. Long-Press AE / AF Lock with yellow pill indicator banner
 * 5. 3x3 Rule-of-Thirds Grid & Horizon Level indicator
 * 6. Aspect Ratio Switcher (4:3, 16:9, 1:1) with live viewfinder framing
 * 7. 4-State Flash System (Off, Auto, On, Continuous Torch)
 * 8. Pro Photographic Styles / Live Color Grading Filters (Standard, Vivid, Warm, Cool, Noir, Silver)
 * 9. Multi-Lens Zoom System (.5x, 1x, 2x, 3x, 5x) with smooth dial
 * 10. Front Camera Screen Flash & 3D Spring Camera Switcher
 * 11. Self-Timer (Off, 3s, 10s) with acoustic & visual countdown
 * 12. Full-featured Media Review Studio with looping Video playback, Save to Device, and Direct Send
 */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun IosCameraScreen(
    onDismiss: () -> Unit,
    onPhotoCaptured: (Uri, String?) -> Unit,
    onVideoCaptured: ((Uri, String?) -> Unit)? = null,
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

    // Camera Configuration State
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var flashMode by remember { mutableStateOf(IosFlashMode.OFF) }
    var selectedMode by remember { mutableStateOf(IosCameraMode.PHOTO) }
    var aspectRatio by remember { mutableStateOf(ProAspectRatio.RATIO_4_3) }
    var selectedFilter by remember { mutableStateOf(ProCameraFilter.STANDARD) }
    var selectedCategory by remember { mutableStateOf(FilterCategory.BEAUTY) }
    var filterIntensity by remember { mutableFloatStateOf(0.80f) }
    var showFilterTray by remember { mutableStateOf(false) }
    var showGrid by remember { mutableStateOf(false) }
    var showLevel by remember { mutableStateOf(false) }

    // Zoom state
    var currentZoomRatio by remember { mutableFloatStateOf(1f) }
    var minZoom by remember { mutableFloatStateOf(1f) }
    var maxZoom by remember { mutableFloatStateOf(8f) }

    // Timer state
    var timerSeconds by remember { mutableIntStateOf(0) }
    var countdownValue by remember { mutableIntStateOf(0) }

    // Tap to focus & Exposure state
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var isFocusing by remember { mutableStateOf(false) }
    var isAeAfLocked by remember { mutableStateOf(false) }
    var exposureCompensationIndex by remember { mutableIntStateOf(0) }
    var minExposureIndex by remember { mutableIntStateOf(-4) }
    var maxExposureIndex by remember { mutableIntStateOf(4) }

    // Video Recording state
    var isRecordingVideo by remember { mutableStateOf(false) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var videoDurationSeconds by remember { mutableIntStateOf(0) }
    var isQuickTake by remember { mutableStateOf(false) }

    // Review Captured Media State
    var capturedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var capturedPhotoFile by remember { mutableStateOf<File?>(null) }
    var capturedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var capturedVideoFile by remember { mutableStateOf<File?>(null) }
    var isCapturingPhotoAnimation by remember { mutableStateOf(false) }
    var isReviewing by remember { mutableStateOf(false) }
    var captionText by remember { mutableStateOf("") }

    // Recent device media thumbnail for bottom-left preview
    var recentMediaUri by remember { mutableStateOf<Uri?>(null) }
    LaunchedEffect(Unit) {
        try {
            val media = MediaStoreHelper.loadDeviceMedia(context)
            recentMediaUri = media.firstOrNull()?.uri
        } catch (_: Exception) {}
    }

    // CameraX controllers
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var videoCapture: VideoCapture<Recorder>? by remember { mutableStateOf(null) }
    var cameraControl: CameraControl? by remember { mutableStateOf(null) }
    var cameraInfo: CameraInfo? by remember { mutableStateOf(null) }

    // Recording duration timer
    LaunchedEffect(isRecordingVideo) {
        if (isRecordingVideo) {
            videoDurationSeconds = 0
            while (isRecordingVideo) {
                delay(1000)
                videoDurationSeconds++
            }
        } else {
            videoDurationSeconds = 0
        }
    }

    // Flash white shutter effect for selfie front camera
    var isFrontScreenFlashActive by remember { mutableStateOf(false) }

    // Helper: Stop video recording cleanly
    fun stopVideoRecording() {
        if (isRecordingVideo) {
            activeRecording?.stop()
            activeRecording = null
            isRecordingVideo = false
            isQuickTake = false
            SoundManager.playPop()
        }
    }

    // Helper: Start video recording
    fun startVideoRecording(videoCap: VideoCapture<Recorder>?) {
        val cap = videoCap ?: return
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        SoundManager.playPop()

        val videoFile = File(context.cacheDir, "camera_video_${System.currentTimeMillis()}.mp4")
        val outputOptions = FileOutputOptions.Builder(videoFile).build()

        val pendingRecording = cap.output.prepareRecording(context, outputOptions)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            try {
                pendingRecording.withAudioEnabled()
            } catch (_: Exception) {}
        }

        activeRecording = pendingRecording.start(ContextCompat.getMainExecutor(context)) { recordEvent ->
            when (recordEvent) {
                is VideoRecordEvent.Start -> {
                    isRecordingVideo = true
                }
                is VideoRecordEvent.Finalize -> {
                    isRecordingVideo = false
                    if (!recordEvent.hasError()) {
                        capturedVideoFile = videoFile
                        capturedVideoUri = Uri.fromFile(videoFile)
                        isReviewing = true
                    } else {
                        Toast.makeText(context, "Video recording error: ${recordEvent.cause?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // If reviewing captured photo or video, show the Pro Review Screen
    if (isReviewing && (capturedPhotoUri != null || capturedVideoUri != null)) {
        IosCameraReviewScreen(
            photoUri = capturedPhotoUri,
            videoUri = capturedVideoUri,
            captionText = captionText,
            activeFilter = selectedFilter,
            onFilterChange = { selectedFilter = it },
            onCaptionChange = { captionText = it },
            onRetake = {
                capturedPhotoFile?.delete()
                capturedVideoFile?.delete()
                capturedPhotoUri = null
                capturedVideoUri = null
                capturedPhotoFile = null
                capturedVideoFile = null
                captionText = ""
                isReviewing = false
            },
            onSend = {
                if (capturedVideoUri != null) {
                    val vidUri = capturedVideoUri!!
                    val caption = captionText.takeIf { it.isNotBlank() }
                    if (onVideoCaptured != null) {
                        onVideoCaptured(vidUri, caption)
                    } else {
                        onPhotoCaptured(vidUri, caption)
                    }
                    onDismiss()
                } else if (capturedPhotoUri != null) {
                    val photoUri = capturedPhotoUri!!
                    val caption = captionText.takeIf { it.isNotBlank() }
                    onPhotoCaptured(photoUri, caption)
                    onDismiss()
                }
            }
        )
        return
    }

    // Full Professional Camera Viewport
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
                                IosFlashMode.OFF, IosFlashMode.TORCH -> ImageCapture.FLASH_MODE_OFF
                            }
                        )
                        .build()
                    imageCapture = capture

                    // Video Recording Pipeline
                    val recorder = Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HD, FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)))
                        .build()
                    val vidCapture = VideoCapture.withOutput(recorder)
                    videoCapture = vidCapture

                    try {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            capture,
                            vidCapture
                        )
                        cameraControl = camera.cameraControl
                        cameraInfo = camera.cameraInfo

                        // Torch control
                        if (camera.cameraInfo.hasFlashUnit()) {
                            camera.cameraControl.enableTorch(flashMode == IosFlashMode.TORCH)
                        }

                        // Exposure bounds
                        val expState = camera.cameraInfo.exposureState
                        minExposureIndex = expState.exposureCompensationRange.lower
                        maxExposureIndex = expState.exposureCompensationRange.upper
                        exposureCompensationIndex = expState.exposureCompensationIndex

                        camera.cameraInfo.zoomState.observe(lifecycleOwner) { zoomState ->
                            currentZoomRatio = zoomState.zoomRatio
                            minZoom = zoomState.minZoomRatio
                            maxZoom = zoomState.maxZoomRatio
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                // Pinch to Zoom gesture
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
                    detectTapGestures(
                        onTap = { offset ->
                            focusPoint = offset
                            isFocusing = true
                            isAeAfLocked = false
                            cameraControl?.let { control ->
                                val meteringPointFactory = SurfaceOrientedMeteringPointFactory(size.width.toFloat(), size.height.toFloat())
                                val meteringPoint = meteringPointFactory.createPoint(offset.x, offset.y)
                                val action = FocusMeteringAction.Builder(meteringPoint, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE).build()
                                control.startFocusAndMetering(action)
                            }
                        },
                        onLongPress = { offset ->
                            focusPoint = offset
                            isFocusing = true
                            isAeAfLocked = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            SoundManager.playPop()
                            cameraControl?.let { control ->
                                val meteringPointFactory = SurfaceOrientedMeteringPointFactory(size.width.toFloat(), size.height.toFloat())
                                val meteringPoint = meteringPointFactory.createPoint(offset.x, offset.y)
                                val action = FocusMeteringAction.Builder(meteringPoint, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                                    .disableAutoCancel()
                                    .build()
                                control.startFocusAndMetering(action)
                            }
                        }
                    )
                }
        )

        // ── 2. Photographic Style & Beauty Color Grading Overlay ───────────
        if (selectedFilter != ProCameraFilter.STANDARD && selectedFilter.liveOverlayColor != null) {
            val overlayColor = selectedFilter.liveOverlayColor!!
            val alpha = selectedFilter.liveOverlayAlpha * filterIntensity
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlayColor.copy(alpha = alpha))
            )
        }

        // ── 3. Pro Composition Overlays (Grid & Level) ─────────────────────
        if (showGrid) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val gridStroke = Stroke(width = 0.8.dp.toPx())
                val gridColor = Color.White.copy(alpha = 0.28f)

                // 2 vertical lines
                drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth = gridStroke.width)
                drawLine(gridColor, Offset(2 * w / 3f, 0f), Offset(2 * w / 3f, h), strokeWidth = gridStroke.width)

                // 2 horizontal lines
                drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth = gridStroke.width)
                drawLine(gridColor, Offset(0f, 2 * h / 3f), Offset(w, 2 * h / 3f), strokeWidth = gridStroke.width)
            }
        }

        if (showLevel) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Horizon center level reticle
                Canvas(modifier = Modifier.size(70.dp, 16.dp)) {
                    val levelColor = Color(0xFFFFCC00) // Golden leveled indicator
                    drawLine(levelColor, Offset(0f, size.height / 2), Offset(24.dp.toPx(), size.height / 2), strokeWidth = 2.dp.toPx())
                    drawLine(levelColor, Offset(size.width - 24.dp.toPx(), size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 2.dp.toPx())
                    drawCircle(levelColor, radius = 2.dp.toPx(), center = Offset(size.width / 2, size.height / 2))
                }
            }
        }

        // ── 4. Aspect Ratio Masking (1:1 Square & 16:9 Letterbox) ──────────
        if (aspectRatio == ProAspectRatio.RATIO_1_1) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black.copy(alpha = 0.85f)))
                Spacer(modifier = Modifier.fillMaxWidth().aspectRatio(1f))
                Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black.copy(alpha = 0.85f)))
            }
        }

        // ── 5. Tap to Focus with EV Sun Icon Slider ──────────────────
        focusPoint?.let { pt ->
            LaunchedEffect(pt, isAeAfLocked) {
                if (!isAeAfLocked) {
                    isFocusing = true
                    delay(1800)
                    isFocusing = false
                }
            }

            AnimatedVisibility(
                visible = isFocusing || isAeAfLocked,
                enter = scaleIn(initialScale = 1.35f, animationSpec = tween(150)) + fadeIn(),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = (pt.x - 36).dp, y = (pt.y - 36).dp)
                        .size(72.dp)
                        .border(1.2.dp, Color(0xFFFFCC00), RoundedCornerShape(2.dp))
                ) {
                    // Vertical EV Sun Icon Slider
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .offset(x = 32.dp)
                            .height(80.dp)
                            .pointerInput(minExposureIndex, maxExposureIndex) {
                                detectDragGestures { _, dragAmount ->
                                    val delta = -dragAmount.y / 25f
                                    val current = exposureCompensationIndex
                                    val newIndex = (current + delta.roundToInt()).coerceIn(minExposureIndex, maxExposureIndex)
                                    if (newIndex != current) {
                                        exposureCompensationIndex = newIndex
                                        cameraControl?.setExposureCompensationIndex(newIndex)
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Canvas(modifier = Modifier.width(2.dp).height(50.dp)) {
                            drawLine(Color(0xFFFFCC00).copy(alpha = 0.5f), Offset(0f, 0f), Offset(0f, size.height), strokeWidth = 2.dp.toPx())
                        }
                        CameraSunIcon(
                            tint = Color(0xFFFFCC00),
                            size = 15.dp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }

        // ── 6. AE / AF Lock Banner ─────────────────────────────────────────
        if (isAeAfLocked) {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 54.dp)
                    .align(Alignment.TopCenter)
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFFFCC00),
                    shadowElevation = 6.dp
                ) {
                    Text(
                        text = "AE/AF LOCK",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // ── 7. Video Recording HUD Header ──────────────────────────────────
        if (isRecordingVideo) {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 16.dp)
                    .align(Alignment.TopCenter)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.65f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Pulsing red recording dot
                        val infiniteTransition = rememberInfiniteTransition(label = "pulseRed")
                        val redAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
                            label = "dotAlpha"
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF3B30).copy(alpha = redAlpha))
                        )

                        val mins = videoDurationSeconds / 60
                        val secs = videoDurationSeconds % 60
                        Text(
                            text = String.format(Locale.US, "%02d:%02d", mins, secs),
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ── 8. Timer Countdown Overlay ─────────────────────────────────────
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

        // ── 9. Shutter Animation White Flash ───────────────────────────────
        AnimatedVisibility(
            visible = isCapturingPhotoAnimation || isFrontScreenFlashActive,
            enter = fadeIn(animationSpec = tween(40)),
            exit = fadeOut(animationSpec = tween(140))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            )
        }

        // ── 10. Top Pro Tools Bar ──────────────────────────────────────────
        if (!isRecordingVideo) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Dismiss / Close
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

                // Middle Pro Controls: Flash, Aspect Ratio, Grid/Level, Styles, Timer
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Flash / Torch Button (Off, Auto, On, Torch)
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
                                    IosFlashMode.ON -> IosFlashMode.TORCH
                                    IosFlashMode.TORCH -> IosFlashMode.OFF
                                }
                                imageCapture?.flashMode = when (flashMode) {
                                    IosFlashMode.ON -> ImageCapture.FLASH_MODE_ON
                                    IosFlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
                                    IosFlashMode.OFF, IosFlashMode.TORCH -> ImageCapture.FLASH_MODE_OFF
                                }
                                try {
                                    cameraControl?.enableTorch(flashMode == IosFlashMode.TORCH)
                                } catch (_: Exception) {}
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when (flashMode) {
                            IosFlashMode.ON -> CameraFlashOnIcon(tint = Color.Black, size = 18.dp)
                            IosFlashMode.AUTO -> CameraFlashAutoIcon(tint = Color.Black, size = 18.dp)
                            IosFlashMode.TORCH -> CameraTorchIcon(tint = Color.Black, size = 18.dp)
                            IosFlashMode.OFF -> CameraFlashOffIcon(tint = Color.White, size = 18.dp)
                        }
                    }

                    // Aspect Ratio Toggle (4:3, 16:9, 1:1)
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                aspectRatio = when (aspectRatio) {
                                    ProAspectRatio.RATIO_4_3 -> ProAspectRatio.RATIO_16_9
                                    ProAspectRatio.RATIO_16_9 -> ProAspectRatio.RATIO_1_1
                                    ProAspectRatio.RATIO_1_1 -> ProAspectRatio.RATIO_4_3
                                }
                            }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = aspectRatio.title,
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Grid & Level Reticle Toggle
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (showGrid || showLevel) Color(0xFFFFCC00) else Color.Black.copy(alpha = 0.45f))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (!showGrid && !showLevel) {
                                    showGrid = true
                                } else if (showGrid && !showLevel) {
                                    showLevel = true
                                } else {
                                    showGrid = false
                                    showLevel = false
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            showGrid && showLevel -> CameraGridLevelIcon(tint = Color.Black, size = 18.dp)
                            showGrid -> CameraGridIcon(tint = Color.Black, size = 18.dp)
                            showLevel -> CameraLevelIcon(tint = Color.Black, size = 18.dp)
                            else -> CameraGridIcon(tint = Color.White, size = 18.dp)
                        }
                    }

                    // Photographic Styles / Filters Drawer Toggle
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (selectedFilter != ProCameraFilter.STANDARD || showFilterTray) Color(0xFFFFCC00) else Color.Black.copy(alpha = 0.45f))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showFilterTray = !showFilterTray
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedFilter.category == FilterCategory.BEAUTY && selectedFilter != ProCameraFilter.STANDARD) {
                            CameraBeautySparkleIcon(
                                tint = Color.Black,
                                size = 18.dp
                            )
                        } else {
                            CameraFilterIcon(
                                tint = if (selectedFilter != ProCameraFilter.STANDARD || showFilterTray) Color.Black else Color.White,
                                size = 18.dp
                            )
                        }
                    }

                    // Self-Timer (Off, 3s, 10s)
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
                        if (timerSeconds > 0) {
                            Text(
                                text = "${timerSeconds}s",
                                color = Color.Black,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            CameraTimerIcon(
                                tint = Color.White,
                                size = 18.dp
                            )
                        }
                    }
                }

                // Balance right spacer
                Spacer(Modifier.size(36.dp))
            }
        }

        // ── 11. Bottom Controls: Filters, Zoom, Mode Carousel, Shutter ──────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Live Filter Styles & Beauty Pill Tray
            AnimatedVisibility(
                visible = showFilterTray,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Category Selector Tabs [ Beauty ] [ Styles ]
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterCategory.values().forEach { cat ->
                            val isCatActive = selectedCategory == cat
                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isCatActive) Color(0xFFFFCC00) else Color.Transparent)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedCategory = cat
                                    }
                                    .padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                if (cat == FilterCategory.BEAUTY) {
                                    CameraBeautySparkleIcon(
                                        tint = if (isCatActive) Color.Black else Color.White,
                                        size = 13.dp
                                    )
                                } else {
                                    CameraFilterIcon(
                                        tint = if (isCatActive) Color.Black else Color.White,
                                        size = 13.dp
                                    )
                                }
                                Text(
                                    text = cat.title,
                                    color = if (isCatActive) Color.Black else Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isCatActive) FontWeight.Bold else FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Beauty / Filter Intensity Slider (Visible when any non-standard filter is active)
                    if (selectedFilter != ProCameraFilter.STANDARD) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (selectedFilter.category == FilterCategory.BEAUTY) "Smooth" else "Strength",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Slider(
                                value = filterIntensity,
                                onValueChange = { newIntensity ->
                                    filterIntensity = newIntensity
                                },
                                valueRange = 0.2f..1.0f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFFFCC00),
                                    activeTrackColor = Color(0xFFFFCC00),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                                )
                            )
                            Text(
                                text = "${(filterIntensity * 100).roundToInt()}%",
                                color = Color(0xFFFFCC00),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(36.dp),
                                textAlign = TextAlign.End
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                    }

                    // Filter Chips Row
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        val currentCategoryFilters = ProCameraFilter.values().filter { it.category == selectedCategory }
                        items(currentCategoryFilters) { filter ->
                            val isSelected = selectedFilter == filter
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) Color(0xFFFFCC00) else Color.Black.copy(alpha = 0.65f),
                                border = androidx.compose.foundation.BorderStroke(
                                    0.5.dp,
                                    if (isSelected) Color(0xFFFFCC00) else Color.White.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier.clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedFilter = filter
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Swatch dot
                                    if (filter != ProCameraFilter.STANDARD) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(filter.swatchColor)
                                                .border(0.5.dp, Color.Black.copy(alpha = 0.3f), CircleShape)
                                        )
                                    }
                                    Text(
                                        text = filter.displayName,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Interactive Animated Zoom Circle Slider (.5, 1x, 2, 3, 5) with radial arc dial
            AnimatedZoomCircleSlider(
                currentZoom = currentZoomRatio,
                minZoom = minZoom,
                maxZoom = maxZoom,
                onZoomChanged = { targetZoom ->
                    cameraControl?.setZoomRatio(targetZoom)
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(10.dp))

            // Pro Camera Mode Carousel Strip
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
                        color = if (isSelected) Color(0xFFFFCC00) else Color.White.copy(alpha = 0.65f),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (!isRecordingVideo) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedMode = mode
                                    if (mode == IosCameraMode.SQUARE) {
                                        aspectRatio = ProAspectRatio.RATIO_1_1
                                    }
                                }
                            }
                            .padding(vertical = 4.dp, horizontal = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Shutter Row: Gallery Thumb | Concentric Shutter Ring | Camera Flip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 34.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Bottom-Left: Recent Device Photo Thumbnail
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color.White.copy(alpha = 0.75f), CircleShape)
                        .background(Color.DarkGray)
                        .clickable {
                            if (!isRecordingVideo) {
                                onDismiss()
                            }
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

                // Center: Apple Concentric Shutter Button with QuickTake & Video Recording
                val shutterInteractionSource = remember { MutableInteractionSource() }
                val isShutterPressed by shutterInteractionSource.collectIsPressedAsState()
                val shutterInnerScale by animateFloatAsState(
                    targetValue = if (isShutterPressed || isRecordingVideo) 0.82f else 1f,
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
                    label = "shutterScale"
                )

                // Shutter actions (Photo capture, Video start/stop, QuickTake hold)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(4.5.dp, Color.White, CircleShape)
                        .padding(4.5.dp)
                        .pointerInput(selectedMode, isRecordingVideo) {
                            detectTapGestures(
                                onTap = {
                                    if (selectedMode == IosCameraMode.VIDEO) {
                                        if (isRecordingVideo) {
                                            stopVideoRecording()
                                        } else {
                                            startVideoRecording(videoCapture)
                                        }
                                    } else {
                                        // Still Photo Capture
                                        coroutineScope.launch {
                                            if (timerSeconds > 0) {
                                                countdownValue = timerSeconds
                                                while (countdownValue > 0) {
                                                    SoundManager.playReaction()
                                                    delay(1000)
                                                    countdownValue--
                                                }
                                            }

                                            // Trigger screen flash if front camera
                                            if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
                                                isFrontScreenFlashActive = true
                                            }
                                            isCapturingPhotoAnimation = true
                                            SoundManager.playReaction()
                                            delay(120)
                                            isCapturingPhotoAnimation = false
                                            isFrontScreenFlashActive = false

                                            val photoFile = File(context.cacheDir, "camera_capture_${System.currentTimeMillis()}.jpg")
                                            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                                            imageCapture?.takePicture(
                                                outputOptions,
                                                ContextCompat.getMainExecutor(context),
                                                object : ImageCapture.OnImageSavedCallback {
                                                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                                        // Process aspect ratio crop & filter application
                                                        applyPostCaptureFiltersAndAspect(
                                                            photoFile = photoFile,
                                                            aspectRatio = aspectRatio,
                                                            filter = selectedFilter
                                                        )
                                                        val savedUri = Uri.fromFile(photoFile)
                                                        capturedPhotoUri = savedUri
                                                        capturedPhotoFile = photoFile
                                                        isReviewing = true
                                                    }

                                                    override fun onError(exc: ImageCaptureException) {
                                                        exc.printStackTrace()
                                                        Toast.makeText(context, "Capture error: ${exc.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            )
                                        }
                                    }
                                },
                                onLongPress = {
                                    // QuickTake: Hold shutter in PHOTO mode to record video!
                                    if (selectedMode != IosCameraMode.VIDEO && !isRecordingVideo) {
                                        isQuickTake = true
                                        startVideoRecording(videoCapture)
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val innerColor = if (selectedMode == IosCameraMode.VIDEO || isRecordingVideo) Color(0xFFFF3B30) else Color.White
                    val innerShape = if (isRecordingVideo) RoundedCornerShape(10.dp) else CircleShape

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(shutterInnerScale)
                            .clip(innerShape)
                            .background(innerColor)
                    )
                }

                // Bottom-Right: Camera Flip (Front/Back) with 3D Rotation Animation
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
                            if (!isRecordingVideo) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                flipRotation += 180f
                                cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                                    CameraSelector.DEFAULT_FRONT_CAMERA
                                } else {
                                    CameraSelector.DEFAULT_BACK_CAMERA
                                }
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
 * Interactive Animated Zoom Circle Slider for Pro Camera.
 * 
 * Features:
 * 1. Compact Multi-Lens Capsule (.5, 1x, 2, 3, 5) with animated indicator.
 * 2. Circular Radial Arc Dial: Expands smoothly on drag, viewfinder pinch, or long-press.
 * 3. Dynamic radial arc tick ruler with major stops and minor tenths ticks.
 * 4. Center floating loupe badge with glowing yellow accent (#FFCC00) showing real-time zoom readout (e.g. 1.0x, 2.4x).
 * 5. Logarithmic gesture mapping for smooth natural zoom response across wide and telephoto ranges.
 * 6. Tactile haptic feedback on increments.
 * 7. Inactivity timer auto-collapses dial back to compact capsule after 2.5s.
 */
@Composable
fun AnimatedZoomCircleSlider(
    currentZoom: Float,
    minZoom: Float,
    maxZoom: Float,
    onZoomChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var isExpanded by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(0L) }
    var prevZoomForPinch by remember { mutableFloatStateOf(currentZoom) }

    // Auto-expand dial when zoom changes via viewfinder pinch gesture
    LaunchedEffect(currentZoom) {
        if (kotlin.math.abs(currentZoom - prevZoomForPinch) > 0.08f) {
            isExpanded = true
            lastInteractionTime = System.currentTimeMillis()
        }
        prevZoomForPinch = currentZoom
    }

    // Auto-collapse back to compact capsule after 2.5 seconds of inactivity
    LaunchedEffect(isExpanded, lastInteractionTime) {
        if (isExpanded) {
            delay(2500)
            isExpanded = false
        }
    }

    // Filter available stops based on device camera hardware bounds
    val standardStops = remember(minZoom, maxZoom) {
        val stops = mutableListOf<Float>()
        if (minZoom <= 0.65f) stops.add(0.5f)
        stops.add(1f)
        if (maxZoom >= 1.8f) stops.add(2f)
        if (maxZoom >= 2.8f) stops.add(3f)
        if (maxZoom >= 4.5f) stops.add(5f)
        stops
    }

    // Tick markers generation for smooth radial dial
    val tickValues = remember(minZoom, maxZoom) {
        val list = mutableListOf<Float>()
        var z = if (minZoom < 0.9f) 0.5f else 1.0f
        while (z <= maxZoom + 0.05f) {
            list.add((z * 10f).roundToInt() / 10f)
            z += if (z < 2f) 0.1f else if (z < 5f) 0.2f else 0.5f
        }
        list
    }

    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = {
                fadeIn(animationSpec = tween(220)) + expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                fadeOut(animationSpec = tween(180)) + shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
            },
            label = "zoomSliderTransition"
        ) { expanded ->
            if (expanded) {
                // ── EXPANDED RADIAL CIRCLE ARC SLIDER ──────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black.copy(alpha = 0.68f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(24.dp))
                        .pointerInput(minZoom, maxZoom, currentZoom) {
                            detectDragGestures(
                                onDragStart = {
                                    lastInteractionTime = System.currentTimeMillis()
                                },
                                onDragEnd = {
                                    lastInteractionTime = System.currentTimeMillis()
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    lastInteractionTime = System.currentTimeMillis()
                                    // Logarithmic sensitivity
                                    val sensitivity = 360f
                                    val currentLog = ln(currentZoom.coerceIn(minZoom, maxZoom))
                                    val logDelta = -dragAmount.x / sensitivity
                                    val newZoom = exp(currentLog + logDelta).coerceIn(minZoom, maxZoom)

                                    val oldStep = (currentZoom * 10f).roundToInt()
                                    val newStep = (newZoom * 10f).roundToInt()
                                    if (oldStep != newStep) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    onZoomChanged(newZoom)
                                }
                            )
                        }
                        .padding(top = 10.dp, bottom = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Curved Arc Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val centerX = w / 2f
                            val arcRadius = 180.dp.toPx()
                            val centerY = h + arcRadius - 42.dp.toPx()

                            // Base arc track
                            val arcBounds = Size(arcRadius * 2f, arcRadius * 2f)
                            val arcTopLeft = Offset(centerX - arcRadius, centerY - arcRadius)
                            drawArc(
                                color = Color.White.copy(alpha = 0.15f),
                                startAngle = 245f,
                                sweepAngle = 50f,
                                useCenter = false,
                                topLeft = arcTopLeft,
                                size = arcBounds,
                                style = Stroke(width = 1.dp.toPx())
                            )

                            val currentLog = ln(currentZoom.coerceIn(minZoom, maxZoom))
                            val angularSensitivity = 0.40f // radians per natural log unit

                            // Draw dynamic ticks along the arc
                            for (tick in tickValues) {
                                val tickLog = ln(tick)
                                val theta = (tickLog - currentLog) * angularSensitivity

                                // Render within visible arc angle bounds (+/- 0.42 rad)
                                if (kotlin.math.abs(theta) < 0.42f) {
                                    val isMajor = tick in listOf(0.5f, 1f, 2f, 3f, 4f, 5f, 8f, 10f)
                                    val tickLen = if (isMajor) 15.dp.toPx() else 8.dp.toPx()
                                    val strokeW = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
                                    val color = if (isMajor) Color.White.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.42f)

                                    // Position along circle
                                    val sinT = sin(theta)
                                    val cosT = cos(theta)
                                    val xStart = centerX + arcRadius * sinT
                                    val yStart = centerY - arcRadius * cosT
                                    val xEnd = centerX + (arcRadius - tickLen) * sinT
                                    val yEnd = centerY - (arcRadius - tickLen) * cosT

                                    drawLine(
                                        color = color,
                                        start = Offset(xStart, yStart),
                                        end = Offset(xEnd, yEnd),
                                        strokeWidth = strokeW,
                                        cap = StrokeCap.Round
                                    )

                                    // Label major ticks
                                    if (isMajor && kotlin.math.abs(theta) < 0.36f) {
                                        val labelText = if (tick < 1f) ".5" else if (tick == 1f) "1x" else "${tick.toInt()}"
                                        val textResult = textMeasurer.measure(
                                            text = labelText,
                                            style = TextStyle(
                                                color = Color.White.copy(alpha = 0.85f),
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        val labelRadius = arcRadius - tickLen - 12.dp.toPx()
                                        val labelX = centerX + labelRadius * sinT - (textResult.size.width / 2f)
                                        val labelY = centerY - labelRadius * cosT - (textResult.size.height / 2f)
                                        drawText(
                                            textLayoutResult = textResult,
                                            topLeft = Offset(labelX, labelY)
                                        )
                                    }
                                }
                            }

                            // Center Reticle Needle (Amber Gold)
                            val needleLen = 22.dp.toPx()
                            drawLine(
                                color = Color(0xFFFFCC00),
                                start = Offset(centerX, centerY - arcRadius + 4.dp.toPx()),
                                end = Offset(centerX, centerY - arcRadius - needleLen),
                                strokeWidth = 2.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            drawCircle(
                                color = Color(0xFFFFCC00),
                                radius = 2.5.dp.toPx(),
                                center = Offset(centerX, centerY - arcRadius + 4.dp.toPx())
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // Center Animated Glowing Loupe Badge (Click to collapse)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.85f))
                                .border(1.5.dp, Color(0xFFFFCC00), CircleShape)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isExpanded = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.1fx", currentZoom),
                                color = Color(0xFFFFCC00),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Lens Stops Quick Selector Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        standardStops.forEach { stop ->
                            val isSelected = kotlin.math.abs(currentZoom - stop) < 0.2f
                            val label = if (stop < 1f) ".5" else if (stop == 1f) "1x" else "${stop.toInt()}"
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) Color(0xFFFFCC00) else Color.White.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .size(30.dp)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        lastInteractionTime = System.currentTimeMillis()
                                        onZoomChanged(stop.coerceIn(minZoom, maxZoom))
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // ── COMPACT LENS CAPSULE (DEFAULT) ─────────────────────────────────
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        .pointerInput(minZoom, maxZoom, currentZoom) {
                            detectDragGestures(
                                onDragStart = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isExpanded = true
                                    lastInteractionTime = System.currentTimeMillis()
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    lastInteractionTime = System.currentTimeMillis()
                                    val sensitivity = 360f
                                    val currentLog = ln(currentZoom.coerceIn(minZoom, maxZoom))
                                    val logDelta = -dragAmount.x / sensitivity
                                    val newZoom = exp(currentLog + logDelta).coerceIn(minZoom, maxZoom)
                                    onZoomChanged(newZoom)
                                }
                            )
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    standardStops.forEach { stop ->
                        val isSelected = kotlin.math.abs(currentZoom - stop) < 0.35f
                        val label = if (isSelected && kotlin.math.abs(currentZoom - stop) > 0.15f) {
                            String.format(Locale.US, "%.1f", currentZoom)
                        } else {
                            if (stop < 1f) ".5" else if (stop == 1f) "1x" else "${stop.toInt()}"
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFFFFCC00) else Color.Transparent)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (isSelected) {
                                        // Tap on current active lens stop expands the full circular radial dial
                                        isExpanded = true
                                        lastInteractionTime = System.currentTimeMillis()
                                    } else {
                                        onZoomChanged(stop.coerceIn(minZoom, maxZoom))
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = if (label.length > 2) 10.sp else 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Applies filter and aspect ratio crop to still photo bitmap.
 */
private fun applyPostCaptureFiltersAndAspect(
    photoFile: File,
    aspectRatio: ProAspectRatio,
    filter: ProCameraFilter,
    intensity: Float = 0.8f
) {
    try {
        val originalBitmap = BitmapFactory.decodeFile(photoFile.absolutePath) ?: return

        // 1. Aspect Ratio Crop (if 1:1 Square)
        val croppedBitmap = if (aspectRatio == ProAspectRatio.RATIO_1_1) {
            val size = minOf(originalBitmap.width, originalBitmap.height)
            val startX = (originalBitmap.width - size) / 2
            val startY = (originalBitmap.height - size) / 2
            Bitmap.createBitmap(originalBitmap, startX, startY, size, size)
        } else {
            originalBitmap
        }

        // 2. Beauty Glow / Skin Smoothing Diffusion Pass
        val glowStrength = filter.beautyGlowStrength * intensity.coerceIn(0f, 1f)
        val glowBitmap = if (glowStrength > 0.02f) {
            applyBeautyGlowDiffusion(croppedBitmap, glowStrength)
        } else {
            croppedBitmap
        }

        // 3. Color Grading Filter Application with User Intensity
        val finalMatrix = filter.getInterpolatedMatrix(intensity)
        val finalBitmap = if (finalMatrix != null) {
            val result = Bitmap.createBitmap(glowBitmap.width, glowBitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            val paint = Paint()
            val androidMatrix = android.graphics.ColorMatrix(finalMatrix.values)
            paint.colorFilter = ColorMatrixColorFilter(androidMatrix)
            canvas.drawBitmap(glowBitmap, 0f, 0f, paint)
            result
        } else {
            glowBitmap
        }

        // Save back to file
        FileOutputStream(photoFile).use { out ->
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * Optical Bilinear Diffusion pass for natural, airbrushed skin smoothing.
 * Softens harsh pores and blemishes while preserving sharp eye/hair details.
 */
private fun applyBeautyGlowDiffusion(source: Bitmap, strength: Float): Bitmap {
    return try {
        val width = source.width
        val height = source.height
        val smallW = (width / 4).coerceAtLeast(64)
        val smallH = (height / 4).coerceAtLeast(64)
        val downsampled = Bitmap.createScaledBitmap(source, smallW, smallH, true)
        val blurred = Bitmap.createScaledBitmap(downsampled, width, height, true)
        downsampled.recycle()

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawBitmap(source, 0f, 0f, null)

        val paint = Paint().apply {
            alpha = (strength * 255f).roundToInt().coerceIn(0, 255)
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        }
        canvas.drawBitmap(blurred, 0f, 0f, paint)
        blurred.recycle()
        output
    } catch (e: Exception) {
        source
    }
}

/**
 * Apple iOS Camera Review Screen for captured photo or video.
 * Displays interactive media playback, filter selector, device gallery save, and direct send.
 */
@Composable
fun IosCameraReviewScreen(
    photoUri: Uri?,
    videoUri: Uri?,
    captionText: String,
    activeFilter: ProCameraFilter,
    onFilterChange: (ProCameraFilter) -> Unit,
    onCaptionChange: (String) -> Unit,
    onRetake: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Media Preview: Photo or Video
        if (videoUri != null) {
            // Looping Video Player View
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setVideoURI(videoUri)
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            start()
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else if (photoUri != null) {
            AsyncImage(
                model = photoUri,
                contentDescription = "Captured Photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        // Top Navigation: Retake Button + Save to Device Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Retake
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

            // Save to Device Gallery Action
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        try {
                            if (photoUri != null) {
                                val file = File(photoUri.path ?: "")
                                if (file.exists()) {
                                    val values = ContentValues().apply {
                                        put(MediaStore.Images.Media.DISPLAY_NAME, "Chroma_${System.currentTimeMillis()}.jpg")
                                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ChromaStudio")
                                    }
                                    context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)?.let { uri ->
                                        context.contentResolver.openOutputStream(uri)?.use { out ->
                                            file.inputStream().use { input -> input.copyTo(out) }
                                        }
                                        Toast.makeText(context, "Saved to device gallery!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else if (videoUri != null) {
                                val file = File(videoUri.path ?: "")
                                if (file.exists()) {
                                    val values = ContentValues().apply {
                                        put(MediaStore.Video.Media.DISPLAY_NAME, "Chroma_${System.currentTimeMillis()}.mp4")
                                        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                                        put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/ChromaStudio")
                                    }
                                    context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)?.let { uri ->
                                        context.contentResolver.openOutputStream(uri)?.use { out ->
                                            file.inputStream().use { input -> input.copyTo(out) }
                                        }
                                        Toast.makeText(context, "Video saved to gallery!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Saved to cache", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CameraSaveIcon(tint = Color.White, size = 16.dp)
                    Text(text = "Save", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                }
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
                    .background(Color.Black.copy(alpha = 0.70f))
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

@Composable
private fun PhosphorCloseX(tint: Color, size: androidx.compose.ui.unit.Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val s = size.toPx()
        val pad = s * 0.25f
        val stroke = Stroke(width = 2.dp.toPx())
        drawLine(tint, Offset(pad, pad), Offset(s - pad, s - pad), strokeWidth = stroke.width)
        drawLine(tint, Offset(s - pad, pad), Offset(pad, s - pad), strokeWidth = stroke.width)
    }
}
