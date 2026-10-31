package com.chroma.studio.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.composables.icons.lucide.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min

/**
 * Target aspect ratio and styling for profile photo and banner editing.
 */
enum class ProfileCropTarget(
    val title: String,
    val subtitle: String,
    val tagText: String,
    val aspectRatio: Float, // width / height
    val isCircular: Boolean,
    val outputWidth: Int,
    val outputHeight: Int
) {
    AVATAR(
        title = "Crop Avatar",
        subtitle = "Drag & pinch to fit circular portrait",
        tagText = "1:1 Circle",
        aspectRatio = 1.0f,
        isCircular = true,
        outputWidth = 512,
        outputHeight = 512
    ),
    BANNER(
        title = "Crop Cover Banner",
        subtitle = "Drag & pinch to fit panoramic banner",
        tagText = "3:1 Banner",
        aspectRatio = 3.0f,
        isCircular = false,
        outputWidth = 1200,
        outputHeight = 400
    )
}

/**
 * High-performance, Apple-styled interactive profile media crop & transform editor.
 *
 * Supports:
 * - Interactive pinch-to-zoom (1.0x to 4.5x) and drag panning with boundary constraint.
 * - Double tap to toggle 1x / 2x zoom or reset.
 * - 90° clockwise rotation steps (0°, 90°, 180°, 270°).
 * - Horizontal flip (mirror) and vertical flip.
 * - Precision zoom slider.
 * - Seamless cutout overlays: 1:1 circular mask for Avatars, 3:1 rounded rectangle mask for Banners.
 * - Rule-of-thirds alignment grid and Apple crop corner brackets.
 * - Full EXIF orientation auto-correction.
 * - Direct background rendering & compression to cached JPEG ready for Supabase / Firestore upload.
 */
@Composable
fun ProfileMediaEditorModal(
    imageUri: Uri,
    target: ProfileCropTarget,
    onDismiss: () -> Unit,
    onCropSuccess: (croppedUri: Uri) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var loadedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingImage by remember { mutableStateOf(true) }
    var isProcessingOutput by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Editor transformation states
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var rotationDegrees by remember { mutableIntStateOf(0) }
    var isFlippedH by remember { mutableStateOf(false) }
    var isFlippedV by remember { mutableStateOf(false) }

    // Load and normalize bitmap on background thread
    LaunchedEffect(imageUri) {
        isLoadingImage = true
        errorMessage = null
        try {
            val bmp = withContext(Dispatchers.IO) {
                loadSafeNormalizedBitmap(context, imageUri)
            }
            if (bmp != null) {
                loadedBitmap = bmp
            } else {
                errorMessage = "Unable to load selected photo"
            }
        } catch (e: Exception) {
            errorMessage = e.message ?: "Failed to decode photo"
        } finally {
            isLoadingImage = false
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isProcessingOutput) onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0C0C0E)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val availableW = maxWidth
                val availableH = maxHeight
                val density = LocalDensity.current

                val maxWPx = with(density) { availableW.toPx() }
                val maxHPx = with(density) { availableH.toPx() }

                // Top bar reserved ~72.dp, bottom controls reserved ~190.dp
                val topBarHeightPx = with(density) { 72.dp.toPx() }
                val bottomBarHeightPx = with(density) { 190.dp.toPx() }
                val viewportHeightPx = (maxHPx - topBarHeightPx - bottomBarHeightPx).coerceAtLeast(200f)

                // Calculate crop box frame
                val (cropBoxWPx, cropBoxHPx) = remember(target, maxWPx, viewportHeightPx) {
                    if (target.isCircular) {
                        val side = min(maxWPx - with(density) { 56.dp.toPx() }, viewportHeightPx - with(density) { 24.dp.toPx() })
                        side to side
                    } else {
                        val width = (maxWPx - with(density) { 36.dp.toPx() })
                        val height = (width / target.aspectRatio).coerceAtMost(viewportHeightPx - with(density) { 20.dp.toPx() })
                        val adjustedW = height * target.aspectRatio
                        adjustedW to height
                    }
                }

                val cropCenterX = maxWPx / 2f
                val cropCenterY = topBarHeightPx + (viewportHeightPx / 2f)

                val cropLeft = cropCenterX - (cropBoxWPx / 2f)
                val cropTop = cropCenterY - (cropBoxHPx / 2f)
                val cropRight = cropCenterX + (cropBoxWPx / 2f)
                val cropBottom = cropCenterY + (cropBoxHPx / 2f)

                // Clamping pan bounds based on rotated & scaled image dimensions
                val currentBitmap = loadedBitmap
                val clampedOffset = remember(
                    currentBitmap,
                    rotationDegrees,
                    zoomScale,
                    panOffset,
                    cropBoxWPx,
                    cropBoxHPx
                ) {
                    if (currentBitmap == null) return@remember Offset.Zero
                    val origW = currentBitmap.width.toFloat()
                    val origH = currentBitmap.height.toFloat()
                    val effW = if (rotationDegrees % 180 == 0) origW else origH
                    val effH = if (rotationDegrees % 180 == 0) origH else origW

                    val baseScale = max(cropBoxWPx / effW, cropBoxHPx / effH)
                    val totalScale = baseScale * zoomScale

                    val scaledW = effW * totalScale
                    val scaledH = effH * totalScale

                    val maxPanX = max(0f, (scaledW - cropBoxWPx) / 2f)
                    val maxPanY = max(0f, (scaledH - cropBoxHPx) / 2f)

                    Offset(
                        x = panOffset.x.coerceIn(-maxPanX, maxPanX),
                        y = panOffset.y.coerceIn(-maxPanY, maxPanY)
                    )
                }

                // Center Image & Mask Area
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(target, rotationDegrees) {
                            detectTapGestures(
                                onDoubleTap = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (zoomScale > 1.25f) {
                                        zoomScale = 1.0f
                                        panOffset = Offset.Zero
                                    } else {
                                        zoomScale = 2.0f
                                    }
                                }
                            )
                        }
                        .pointerInput(target, rotationDegrees) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                zoomScale = (zoomScale * zoom).coerceIn(1.0f, 4.5f)
                                panOffset = Offset(panOffset.x + pan.x, panOffset.y + pan.y)
                            }
                        }
                ) {
                    if (currentBitmap != null) {
                        // 1. Draw Image with Matrix
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val origW = currentBitmap.width.toFloat()
                            val origH = currentBitmap.height.toFloat()
                            val effW = if (rotationDegrees % 180 == 0) origW else origH
                            val effH = if (rotationDegrees % 180 == 0) origH else origW

                            val baseScale = max(cropBoxWPx / effW, cropBoxHPx / effH)
                            val totalScale = baseScale * zoomScale

                            val displayMatrix = Matrix().apply {
                                postTranslate(-origW / 2f, -origH / 2f)
                                postScale(
                                    if (isFlippedH) -1f else 1f,
                                    if (isFlippedV) -1f else 1f
                                )
                                if (rotationDegrees != 0) {
                                    postRotate(rotationDegrees.toFloat())
                                }
                                postScale(totalScale, totalScale)
                                postTranslate(cropCenterX + clampedOffset.x, cropCenterY + clampedOffset.y)
                            }

                            drawIntoCanvas { canvas ->
                                val nativeCanvas = canvas.nativeCanvas
                                val paint = android.graphics.Paint(
                                    android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG
                                )
                                nativeCanvas.drawBitmap(currentBitmap, displayMatrix, paint)
                            }
                        }

                        // 2. Apple Cutout Mask with Grid & Brackets
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        ) {
                            // Dark vignette overlay
                            drawRect(Color(0xD908080A))

                            val cornerRadiusPx = 16.dp.toPx()
                            val cropSize = Size(cropBoxWPx, cropBoxHPx)
                            val cropTopLeft = Offset(cropLeft, cropTop)

                            if (target.isCircular) {
                                val radius = cropBoxWPx / 2f
                                val center = Offset(cropCenterX, cropCenterY)
                                // Cut circular hole
                                drawCircle(
                                    color = Color.Transparent,
                                    radius = radius,
                                    center = center,
                                    blendMode = BlendMode.Clear
                                )
                                // Circular border
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.88f),
                                    radius = radius,
                                    center = center,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                                // Rule of thirds grid
                                val thirdX = cropBoxWPx / 3f
                                val thirdY = cropBoxHPx / 3f
                                val gridColor = Color.White.copy(alpha = 0.24f)
                                drawLine(gridColor, Offset(cropLeft + thirdX, cropTop + thirdY * 0.45f), Offset(cropLeft + thirdX, cropBottom - thirdY * 0.45f), strokeWidth = 1.dp.toPx())
                                drawLine(gridColor, Offset(cropLeft + 2 * thirdX, cropTop + thirdY * 0.45f), Offset(cropLeft + 2 * thirdX, cropBottom - thirdY * 0.45f), strokeWidth = 1.dp.toPx())
                                drawLine(gridColor, Offset(cropLeft + thirdX * 0.45f, cropTop + thirdY), Offset(cropRight - thirdX * 0.45f, cropTop + thirdY), strokeWidth = 1.dp.toPx())
                                drawLine(gridColor, Offset(cropLeft + thirdX * 0.45f, cropTop + 2 * thirdY), Offset(cropRight - thirdX * 0.45f, cropTop + 2 * thirdY), strokeWidth = 1.dp.toPx())
                            } else {
                                // Cut panoramic rounded rectangle hole
                                drawRoundRect(
                                    color = Color.Transparent,
                                    topLeft = cropTopLeft,
                                    size = cropSize,
                                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                                    blendMode = BlendMode.Clear
                                )
                                // Rect border
                                drawRoundRect(
                                    color = Color.White.copy(alpha = 0.88f),
                                    topLeft = cropTopLeft,
                                    size = cropSize,
                                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                                // Rule of thirds grid
                                val thirdX = cropBoxWPx / 3f
                                val thirdY = cropBoxHPx / 3f
                                val gridColor = Color.White.copy(alpha = 0.24f)
                                drawLine(gridColor, Offset(cropLeft + thirdX, cropTop), Offset(cropLeft + thirdX, cropBottom), strokeWidth = 1.dp.toPx())
                                drawLine(gridColor, Offset(cropLeft + 2 * thirdX, cropTop), Offset(cropLeft + 2 * thirdX, cropBottom), strokeWidth = 1.dp.toPx())
                                drawLine(gridColor, Offset(cropLeft, cropTop + thirdY), Offset(cropRight, cropTop + thirdY), strokeWidth = 1.dp.toPx())
                                drawLine(gridColor, Offset(cropLeft, cropTop + 2 * thirdY), Offset(cropRight, cropTop + 2 * thirdY), strokeWidth = 1.dp.toPx())
                            }

                            // Apple Photo crop corner brackets
                            val bracketLen = 20.dp.toPx()
                            val bracketStroke = 3.5.dp.toPx()
                            val bracketColor = Color.White

                            // Top-Left
                            drawLine(bracketColor, Offset(cropLeft - 1.dp.toPx(), cropTop), Offset(cropLeft + bracketLen, cropTop), strokeWidth = bracketStroke)
                            drawLine(bracketColor, Offset(cropLeft, cropTop - 1.dp.toPx()), Offset(cropLeft, cropTop + bracketLen), strokeWidth = bracketStroke)
                            // Top-Right
                            drawLine(bracketColor, Offset(cropRight + 1.dp.toPx(), cropTop), Offset(cropRight - bracketLen, cropTop), strokeWidth = bracketStroke)
                            drawLine(bracketColor, Offset(cropRight, cropTop - 1.dp.toPx()), Offset(cropRight, cropTop + bracketLen), strokeWidth = bracketStroke)
                            // Bottom-Left
                            drawLine(bracketColor, Offset(cropLeft - 1.dp.toPx(), cropBottom), Offset(cropLeft + bracketLen, cropBottom), strokeWidth = bracketStroke)
                            drawLine(bracketColor, Offset(cropLeft, cropBottom + 1.dp.toPx()), Offset(cropLeft, cropBottom - bracketLen), strokeWidth = bracketStroke)
                            // Bottom-Right
                            drawLine(bracketColor, Offset(cropRight + 1.dp.toPx(), cropBottom), Offset(cropRight - bracketLen, cropBottom), strokeWidth = bracketStroke)
                            drawLine(bracketColor, Offset(cropRight, cropBottom + 1.dp.toPx()), Offset(cropRight, cropBottom - bracketLen), strokeWidth = bracketStroke)
                        }
                    } else if (isLoadingImage) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 3.dp,
                                    color = Color(0xFF007AFF)
                                )
                                Text(
                                    text = "Loading Photo...",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else if (errorMessage != null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = errorMessage!!,
                                    color = Color(0xFFFF453A),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Tap Cancel to choose another photo",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // ── TOP NAVIGATION BAR ───────────────────────────────────────
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(64.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cancel Button
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable(enabled = !isProcessingOutput) { onDismiss() }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Title & Badge
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = target.title,
                            color = Color.White,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFF007AFF).copy(alpha = 0.22f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = target.tagText,
                                    color = Color(0xFF64D2FF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Done / Save Button
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (loadedBitmap != null && !isProcessingOutput) Color(0xFF007AFF)
                                else Color(0xFF007AFF).copy(alpha = 0.35f)
                            )
                            .clickable(enabled = loadedBitmap != null && !isProcessingOutput) {
                                val bmp = loadedBitmap ?: return@clickable
                                isProcessingOutput = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                                scope.launch {
                                    try {
                                        val croppedUri = withContext(Dispatchers.IO) {
                                            renderCroppedMediaBitmap(
                                                context = context,
                                                bitmap = bmp,
                                                target = target,
                                                cropBoxWPx = cropBoxWPx,
                                                cropBoxHPx = cropBoxHPx,
                                                panOffset = clampedOffset,
                                                zoomScale = zoomScale,
                                                rotationDegrees = rotationDegrees,
                                                isFlippedH = isFlippedH,
                                                isFlippedV = isFlippedV
                                            )
                                        }
                                        if (croppedUri != null) {
                                            withContext(Dispatchers.Main) {
                                                onCropSuccess(croppedUri)
                                            }
                                        } else {
                                            withContext(Dispatchers.Main) {
                                                isProcessingOutput = false
                                            }
                                        }
                                    } catch (_: Exception) {
                                        withContext(Dispatchers.Main) {
                                            isProcessingOutput = false
                                        }
                                    }
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                    ) {
                        if (isProcessingOutput) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(13.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Saving...",
                                    color = Color.White,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Text(
                                text = "Done",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // ── BOTTOM CONTROL TOOLBAR ───────────────────────────────────
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Zoom Slider Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .clip(CircleShape)
                            .background(Color(0xFF1C1C1E).copy(alpha = 0.85f))
                            .padding(horizontal = 14.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.ZoomOut,
                            contentDescription = "Zoom Out",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(16.dp)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    zoomScale = (zoomScale - 0.25f).coerceAtLeast(1.0f)
                                }
                        )

                        Slider(
                            value = zoomScale,
                            onValueChange = { zoomScale = it },
                            valueRange = 1.0f..4.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color(0xFF007AFF),
                                inactiveTrackColor = Color.White.copy(alpha = 0.18f)
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Icon(
                            imageVector = Lucide.ZoomIn,
                            contentDescription = "Zoom In",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(16.dp)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    zoomScale = (zoomScale + 0.25f).coerceAtMost(4.0f)
                                }
                        )
                    }

                    // Action Buttons Row: Rotate, Flip Horizontal, Flip Vertical, Reset
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF1C1C1E).copy(alpha = 0.90f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Rotate 90° Clockwise
                        EditorActionButton(
                            icon = Lucide.RotateCw,
                            label = if (rotationDegrees == 0) "Rotate" else "$rotationDegrees°",
                            isActive = rotationDegrees != 0,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                rotationDegrees = (rotationDegrees + 90) % 360
                                panOffset = Offset.Zero
                            }
                        )

                        // 2. Flip Horizontal (Mirror)
                        EditorActionButton(
                            icon = Lucide.FlipHorizontal,
                            label = "Flip H",
                            isActive = isFlippedH,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isFlippedH = !isFlippedH
                            }
                        )

                        // 3. Flip Vertical
                        EditorActionButton(
                            icon = Lucide.FlipVertical,
                            label = "Flip V",
                            isActive = isFlippedV,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isFlippedV = !isFlippedV
                            }
                        )

                        // 4. Reset All Transforms
                        val hasTransforms = zoomScale != 1.0f || panOffset != Offset.Zero || rotationDegrees != 0 || isFlippedH || isFlippedV
                        EditorActionButton(
                            icon = Lucide.RotateCcw,
                            label = "Reset",
                            isActive = false,
                            isEnabled = hasTransforms,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                zoomScale = 1.0f
                                panOffset = Offset.Zero
                                rotationDegrees = 0
                                isFlippedH = false
                                isFlippedV = false
                            }
                        )
                    }

                    // Friendly Gesture Hint
                    Text(
                        text = "Pinch to zoom • Drag to align • Double-tap to reset",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    isEnabled: Boolean = true,
    onClick: () -> Unit
) {
    val activeBg = if (isActive) Color(0xFF007AFF) else Color.Transparent
    val activeTint = if (isActive) Color.White else if (isEnabled) Color.White else Color.White.copy(alpha = 0.3f)
    val textTint = if (isActive) Color(0xFF64D2FF) else if (isEnabled) Color.White.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.3f)

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = isEnabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(activeBg)
                .border(
                    width = if (isActive) 0.dp else 0.5.dp,
                    color = Color.White.copy(alpha = if (isEnabled) 0.15f else 0.05f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = activeTint,
                modifier = Modifier.size(17.dp)
            )
        }

        Text(
            text = label,
            color = textTint,
            fontSize = 10.5.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
        )
    }
}

/**
 * Renders the transformed and cropped bitmap to a high-resolution JPEG file in cache
 * and returns its content Uri.
 */
private fun renderCroppedMediaBitmap(
    context: Context,
    bitmap: Bitmap,
    target: ProfileCropTarget,
    cropBoxWPx: Float,
    cropBoxHPx: Float,
    panOffset: Offset,
    zoomScale: Float,
    rotationDegrees: Int,
    isFlippedH: Boolean,
    isFlippedV: Boolean
): Uri? {
    return try {
        val origW = bitmap.width.toFloat()
        val origH = bitmap.height.toFloat()
        val effW = if (rotationDegrees % 180 == 0) origW else origH
        val effH = if (rotationDegrees % 180 == 0) origH else origW

        val baseScale = max(cropBoxWPx / effW, cropBoxHPx / effH)
        val uiTotalScale = baseScale * zoomScale

        val outW = target.outputWidth
        val outH = target.outputHeight

        val outBitmap = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val outCanvas = android.graphics.Canvas(outBitmap)

        // Neutral solid background so transparent pixels don't render black
        outCanvas.drawColor(android.graphics.Color.WHITE)

        val outScaleFactor = outW.toFloat() / cropBoxWPx
        val finalScale = uiTotalScale * outScaleFactor
        val finalCenterX = (outW / 2f) + (panOffset.x * outScaleFactor)
        val finalCenterY = (outH / 2f) + (panOffset.y * outScaleFactor)

        val matrix = Matrix().apply {
            postTranslate(-origW / 2f, -origH / 2f)
            postScale(
                if (isFlippedH) -1f else 1f,
                if (isFlippedV) -1f else 1f
            )
            if (rotationDegrees != 0) {
                postRotate(rotationDegrees.toFloat())
            }
            postScale(finalScale, finalScale)
            postTranslate(finalCenterX, finalCenterY)
        }

        val paint = android.graphics.Paint(
            android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG
        )
        outCanvas.drawBitmap(bitmap, matrix, paint)

        val targetFilename = "cropped_${target.name.lowercase()}_${System.currentTimeMillis()}.jpg"
        val cacheFile = File(context.cacheDir, targetFilename)
        cacheFile.outputStream().use { out ->
            outBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        outBitmap.recycle()

        FileProvider.getUriForFile(context, "${context.packageName}.provider", cacheFile)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Loads a bitmap from Uri with automatic inSampleSize calculation and EXIF rotation correction.
 */
private fun loadSafeNormalizedBitmap(context: Context, uri: Uri, maxDimension: Int = 2048): Bitmap? {
    return try {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        val outW = boundsOpts.outWidth
        val outH = boundsOpts.outHeight
        if (outW <= 0 || outH <= 0) return null

        var sampleSize = 1
        while ((outW / sampleSize) > maxDimension || (outH / sampleSize) > maxDimension) {
            sampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        } ?: return null

        val exifRotation = try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (_: Exception) {
            0
        }

        if (exifRotation != 0) {
            val matrix = Matrix().apply { postRotate(exifRotation.toFloat()) }
            val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            if (rotated != decoded) decoded.recycle()
            rotated
        } else {
            decoded
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
