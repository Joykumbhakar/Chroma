package com.chroma.studio.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chroma.studio.ui.components.AppleSlider
import com.chroma.studio.ui.components.ChromaScreenHeader
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PhotoAspectRatio(val label: String, val ratio: Float?) {
    FIT("Fit", null),
    SQUARE("1:1", 1f),
    PORTRAIT("4:5", 4f / 5f),
    LANDSCAPE("16:9", 16f / 9f),
    STORY("9:16", 9f / 16f)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashPhotoEditorScreen(
    photoUris: List<Uri>,
    onNext: (filterName: String, thumbnailBitmap: Bitmap?, durationPerSlideMs: Long) -> Unit,
    onSaveDraft: (filterName: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = LocalOscellionColors.current

    val isSlideshow = photoUris.size > 1

    // Current preview image index
    var previewIndex by remember { mutableIntStateOf(0) }

    // Filter
    var selectedFilter by remember { mutableStateOf(FlashFilters[0]) }

    // Adjustments
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var warmth by remember { mutableFloatStateOf(0f) }

    // Transform
    var selectedAspectRatio by remember { mutableStateOf(PhotoAspectRatio.FIT) }
    var rotationDegrees by remember { mutableFloatStateOf(0f) }
    var flipHorizontal by remember { mutableStateOf(false) }

    // Slideshow duration per slide (1–10 s, total ≤ 30 s)
    var durationPerSlideMs by remember { mutableLongStateOf(3000L) }
    val maxDurationPerSlide = if (isSlideshow) (30_000L / photoUris.size).coerceIn(1000L, 10_000L) else 30_000L
    val totalSlideshowSec = if (isSlideshow) (durationPerSlideMs * photoUris.size) / 1000 else 0L

    // Active tab (0=Filters, 1=Adjust, 2=Crop/Transform, 3=Slideshow)
    var activeTab by remember { mutableIntStateOf(0) }
    var isExporting by remember { mutableStateOf(false) }

    // Combined color matrix
    val combinedMatrix = remember(selectedFilter, brightness, contrast, saturation, warmth) {
        val baseArray = if (selectedFilter.name != "None") selectedFilter.colorMatrix.values.copyOf() else floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        val baseAndroidMatrix = android.graphics.ColorMatrix(baseArray)

        val rOffset = brightness + (warmth * 0.5f)
        val gOffset = brightness
        val bOffset = brightness - (warmth * 0.5f)

        val adjAndroidMatrix = android.graphics.ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, rOffset,
            0f, contrast, 0f, 0f, gOffset,
            0f, 0f, contrast, 0f, bOffset,
            0f, 0f, 0f, 1f, 0f
        ))

        val satMatrix = android.graphics.ColorMatrix().apply {
            setSaturation(saturation)
        }

        baseAndroidMatrix.postConcat(adjAndroidMatrix)
        baseAndroidMatrix.postConcat(satMatrix)

        ColorMatrix(baseAndroidMatrix.array)
    }

    val previewUri = photoUris.getOrNull(previewIndex) ?: photoUris.firstOrNull()

    fun handleExportAndNext() {
        if (isExporting || previewUri == null) return
        isExporting = true
        scope.launch {
            val renderedBitmap = renderFilteredBitmap(
                context = context,
                uri = previewUri,
                colorMatrix = combinedMatrix,
                rotation = rotationDegrees,
                flipX = flipHorizontal
            )
            isExporting = false
            onNext(selectedFilter.name, renderedBitmap, durationPerSlideMs)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ── Top Navigation Header ──────────────────────────────────────────────
        ChromaScreenHeader(
            title = "Oscellion",
            badgeText = if (isSlideshow) "Slideshow Studio" else "Photo Studio",
            badgeColor = Color(0xFF007AFF),
            showBackButton = true,
            onBack = onBack,
            actions = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.glassBgHover)
                            .border(0.5.dp, colors.glassBorder, RoundedCornerShape(20.dp))
                            .clickable { onSaveDraft(selectedFilter.name) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Draft", color = colors.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF007AFF), Color(0xFF5856D6))
                                )
                            )
                            .clickable(enabled = !isExporting) { handleExportAndNext() }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Next", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        )

        // ── Live Image Preview Canvas ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF111111)),
            contentAlignment = Alignment.Center
        ) {
            val ratio = selectedAspectRatio.ratio
            val canvasModifier = if (ratio != null) {
                Modifier
                    .fillMaxSize()
                    .aspectRatio(ratio, matchHeightConstraintsFirst = false)
            } else {
                Modifier.fillMaxSize()
            }

            Box(
                modifier = canvasModifier
                    .clip(RoundedCornerShape(12.dp))
                    .graphicsLayer {
                        rotationZ = rotationDegrees
                        scaleX = if (flipHorizontal) -1f else 1f
                    },
                contentAlignment = Alignment.Center
            ) {
                if (previewUri != null) {
                    AsyncImage(
                        model = previewUri,
                        contentDescription = "Photo Preview",
                        contentScale = if (ratio != null) ContentScale.Crop else ContentScale.Fit,
                        colorFilter = ColorFilter.colorMatrix(combinedMatrix),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Slideshow image counter & floating navigation
            if (isSlideshow) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        "${previewIndex + 1} / ${photoUris.size}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Left/Right sleek floating navigation chevrons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Lucide.ChevronLeft,
                        contentDescription = "Previous",
                        tint = Color.White.copy(alpha = if (previewIndex > 0) 0.9f else 0.2f),
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable(enabled = previewIndex > 0) { previewIndex-- }
                            .padding(6.dp)
                    )

                    Icon(
                        imageVector = Lucide.ChevronRight,
                        contentDescription = "Next",
                        tint = Color.White.copy(alpha = if (previewIndex < photoUris.size - 1) 0.9f else 0.2f),
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable(enabled = previewIndex < photoUris.size - 1) { previewIndex++ }
                            .padding(6.dp)
                    )
                }
            }
        }

        // ── Tab Bar (Filters / Adjust / Transform / Slideshow) ─────────────────
        val tabList = remember(isSlideshow) {
            if (isSlideshow) listOf("Filters", "Adjust", "Transform", "Slideshow")
            else listOf("Filters", "Adjust", "Transform")
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            tabList.forEachIndexed { index, label ->
                val isSelected = activeTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.04f))
                        .border(
                            0.5.dp,
                            if (isSelected) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.08f),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { activeTab = index },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // ── Tool Panels ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            when (activeTab) {
                0 -> {
                    // Filters Carousel
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FlashFilters.forEach { filter ->
                            LiveFilterChip(
                                filter = filter,
                                previewUri = previewUri,
                                selected = filter.name == selectedFilter.name,
                                onClick = { selectedFilter = filter }
                            )
                        }
                    }
                }
                1 -> {
                    // Adjustments Sliders
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Color Adjustments", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        brightness = 0f
                                        contrast = 1f
                                        saturation = 1f
                                        warmth = 0f
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Lucide.RotateCcw, contentDescription = "Reset", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(12.dp))
                                Text("Reset", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                            }
                        }

                        PhotoAdjustSlider("Brightness", brightness, -100f, 100f, "%+.0f") { brightness = it }
                        PhotoAdjustSlider("Contrast", contrast, 0.5f, 2f, "%.1fx") { contrast = it }
                        PhotoAdjustSlider("Saturation", saturation, 0f, 3f, "%.1fx") { saturation = it }
                        PhotoAdjustSlider("Warmth", warmth, -50f, 50f, "%+.0f") { warmth = it }
                    }
                }
                2 -> {
                    // Crop & Transform Panel
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Aspect Ratio", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PhotoAspectRatio.values().forEach { aspect ->
                                val sel = selectedAspectRatio == aspect
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (sel) Color(0xFF007AFF) else Color.White.copy(alpha = 0.08f))
                                        .clickable { selectedAspectRatio = aspect },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = aspect.label,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(2.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Rotate 90° Clockwise
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { rotationDegrees = (rotationDegrees + 90f) % 360f }
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Lucide.RotateCw,
                                    contentDescription = "Rotate 90°",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text("Rotate", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }

                            // Flip Horizontal
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { flipHorizontal = !flipHorizontal }
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Lucide.FlipHorizontal,
                                    contentDescription = "Flip",
                                    tint = if (flipHorizontal) Color(0xFF007AFF) else Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Flip",
                                    color = if (flipHorizontal) Color(0xFF007AFF) else Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Reset Transform
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedAspectRatio = PhotoAspectRatio.FIT
                                        rotationDegrees = 0f
                                        flipHorizontal = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Lucide.RefreshCw,
                                    contentDescription = "Reset Transform",
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text("Reset", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
                3 -> {
                    // Slideshow Settings
                    if (isSlideshow) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 18.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Duration per slide", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("${durationPerSlideMs / 1000}s", color = Color(0xFF007AFF), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            AppleSlider(
                                value = durationPerSlideMs.toFloat(),
                                onValueChange = { durationPerSlideMs = it.toLong() },
                                valueRange = 1000f..maxDurationPerSlide.toFloat(),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${photoUris.size} photos", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                Text(
                                    "Total: ${totalSlideshowSec}s / 30s",
                                    color = if (totalSlideshowSec > 30L) Color(0xFFFF3B30) else Color.White.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                            }

                            Text("Photo Sequence", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                photoUris.forEachIndexed { idx, uri ->
                                    val isSelected = idx == previewIndex
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { previewIndex = idx }
                                            .border(
                                                2.dp,
                                                if (isSelected) Color(0xFF007AFF) else Color.White.copy(alpha = 0.15f),
                                                RoundedCornerShape(8.dp)
                                            )
                                    ) {
                                        AsyncImage(
                                            model = uri,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(2.dp)
                                                .size(16.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color.Black.copy(alpha = 0.75f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("${idx + 1}", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun LiveFilterChip(
    filter: FlashFilter,
    previewUri: Uri?,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(width = 64.dp, height = 80.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(
                    2.dp,
                    if (selected) Color(0xFF007AFF) else Color.White.copy(alpha = 0.18f),
                    RoundedCornerShape(10.dp)
                )
                .background(Color(0xFF2C2C2E)),
            contentAlignment = Alignment.Center
        ) {
            if (previewUri != null) {
                AsyncImage(
                    model = previewUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    colorFilter = ColorFilter.colorMatrix(filter.colorMatrix),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(if (filter.name == "B&W" || filter.name == "Mono") Color.Gray else Color(0xFF2C2C2E))
                )
            }

            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF007AFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = filter.name,
            color = if (selected) Color(0xFF007AFF) else Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun PhotoAdjustSlider(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    format: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(format.format(value), color = Color(0xFF007AFF), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(2.dp))
        AppleSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = min..max,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Asynchronously renders the high-res bitmap with active color matrix, rotation, and flip transforms.
 */
private suspend fun renderFilteredBitmap(
    context: Context,
    uri: Uri,
    colorMatrix: ColorMatrix,
    rotation: Float = 0f,
    flipX: Boolean = false,
    maxDimension: Int = 1280
): Bitmap? = withContext(Dispatchers.IO) {
    try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
        val original = android.graphics.BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        if (original == null) return@withContext null

        val maxOriginal = maxOf(original.width, original.height).toFloat()
        val scale = if (maxOriginal > maxDimension) maxDimension / maxOriginal else 1.0f

        val targetWidth = (original.width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (original.height * scale).toInt().coerceAtLeast(1)

        val scaled = Bitmap.createScaledBitmap(original, targetWidth, targetHeight, true)
        if (scaled != original && !original.isRecycled) {
            original.recycle()
        }

        val isRotated90or270 = ((rotation.toInt() % 180) != 0)
        val finalWidth = if (isRotated90or270) targetHeight else targetWidth
        val finalHeight = if (isRotated90or270) targetWidth else targetHeight

        val resultBitmap = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(resultBitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG)
        paint.colorFilter = android.graphics.ColorMatrixColorFilter(android.graphics.ColorMatrix(colorMatrix.values))

        val matrix = android.graphics.Matrix()
        matrix.postTranslate(-targetWidth / 2f, -targetHeight / 2f)
        if (flipX) {
            matrix.postScale(-1f, 1f)
        }
        if (rotation != 0f) {
            matrix.postRotate(rotation)
        }
        matrix.postTranslate(finalWidth / 2f, finalHeight / 2f)

        canvas.drawBitmap(scaled, matrix, paint)
        if (!scaled.isRecycled && scaled != resultBitmap) {
            scaled.recycle()
        }
        resultBitmap
    } catch (e: Exception) {
        null
    }
}
