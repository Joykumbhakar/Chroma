package com.chroma.studio.ui.screens
import com.chroma.studio.ui.components.AppleSlider
import com.chroma.studio.ui.components.ChromaScreenHeader

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.chroma.studio.utils.VideoProcessor
import com.composables.icons.lucide.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToLong

// Flash video filters
data class FlashFilter(
    val name: String,
    val colorMatrix: ColorMatrix
)

val FlashFilters = listOf(
    FlashFilter("None", ColorMatrix()),
    FlashFilter("B&W", ColorMatrix().apply { setToSaturation(0f) }),
    FlashFilter("Cinematic", ColorMatrix(floatArrayOf(
        1.1f, 0f,   0f,   0f, -10f,
        0f,   1.0f, 0f,   0f, -5f,
        0f,   0f,   0.9f, 0f,  10f,
        0f,   0f,   0f,   1f,   0f
    ))),
    FlashFilter("Warm", ColorMatrix(floatArrayOf(
        1.2f, 0f,   0f,   0f, 20f,
        0f,   1.0f, 0f,   0f, 5f,
        0f,   0f,   0.8f, 0f, -20f,
        0f,   0f,   0f,   1f, 0f
    ))),
    FlashFilter("Cool", ColorMatrix(floatArrayOf(
        0.9f, 0f,   0f,   0f, -10f,
        0f,   1.0f, 0f,   0f, 5f,
        0f,   0f,   1.2f, 0f, 20f,
        0f,   0f,   0f,   1f, 0f
    ))),
    FlashFilter("Fade", ColorMatrix(floatArrayOf(
        0.9f, 0f,   0f,   0f, 30f,
        0f,   0.9f, 0f,   0f, 25f,
        0f,   0f,   0.9f, 0f, 25f,
        0f,   0f,   0f,   1f, 0f
    ))),
    FlashFilter("Vivid", ColorMatrix().apply { setToSaturation(1.8f) }),
    FlashFilter("Matte", ColorMatrix(floatArrayOf(
        0.85f, 0f,    0f,    0f, 20f,
        0f,    0.85f, 0f,    0f, 15f,
        0f,    0f,    0.85f, 0f, 20f,
        0f,    0f,    0f,    1f, 0f
    ))),
    FlashFilter("Sepia", ColorMatrix(floatArrayOf(
        0.393f, 0.769f, 0.189f, 0f, 0f,
        0.349f, 0.686f, 0.168f, 0f, 0f,
        0.272f, 0.534f, 0.131f, 0f, 0f,
        0f,     0f,     0f,     1f, 0f
    ))),
    FlashFilter("Neon", ColorMatrix(floatArrayOf(
        0.8f,  0.2f,  0f,   0f, 0f,
        0f,    0.9f,  0.1f, 0f, 0f,
        0.2f,  0f,    1.0f, 0f, 50f,
        0f,    0f,    0f,   1f, 0f
    ))),
    FlashFilter("Sunset", ColorMatrix(floatArrayOf(
        1.3f, 0f,   0f,   0f, 30f,
        0f,   0.9f, 0f,   0f, -10f,
        0f,   0f,   0.7f, 0f, -30f,
        0f,   0f,   0f,   1f, 0f
    ))),
    FlashFilter("Noir", ColorMatrix(floatArrayOf(
        0.33f, 0.33f, 0.33f, 0f, -10f,
        0.33f, 0.33f, 0.33f, 0f, -10f,
        0.33f, 0.33f, 0.33f, 0f, -10f,
        0f,    0f,    0f,    1f, 0f
    )))
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FlashVideoEditorScreen(
    videoUri: Uri,
    onNext: (trimStart: Long, trimEnd: Long, filterName: String, brightness: Float, contrast: Float, saturation: Float, hue: Float, thumbnail: Bitmap?) -> Unit,
    onSaveDraft: (trimStart: Long, trimEnd: Long, filterName: String, brightness: Float, contrast: Float, saturation: Float, hue: Float) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = LocalOscellionColors.current

    // Video duration
    var durationMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(videoUri) {
        durationMs = withContext(Dispatchers.IO) { VideoProcessor.getVideoDurationMs(context, videoUri) }
    }

    // Trim state
    var trimStartMs by remember { mutableLongStateOf(0L) }
    var trimEndMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(durationMs) {
        trimEndMs = durationMs.coerceAtMost(30_000L)
    }

    // ExoPlayer
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }
    LaunchedEffect(videoUri) {
        player.setMediaItem(MediaItem.fromUri(videoUri))
        player.prepare()
        player.play()
    }
    DisposableEffect(Unit) { onDispose { player.release() } }

    // Sync player to trim start
    LaunchedEffect(trimStartMs) {
        player.seekTo(trimStartMs)
    }

    // Filter
    var selectedFilter by remember { mutableStateOf(FlashFilters[0]) }

    // Adjustments
    var brightness by remember { mutableFloatStateOf(0f) }    // -100..+100 mapped to matrix
    var contrast by remember { mutableFloatStateOf(1f) }      // 0.5..2.0
    var saturation by remember { mutableFloatStateOf(1f) }    // 0..3
    var hue by remember { mutableFloatStateOf(0f) }           // -180..+180

    // Tab: Filters or Adjust
    var activeTab by remember { mutableStateOf(0) } // 0=Filters 1=Adjust

    // Thumbnail
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(videoUri, trimStartMs) {
        thumbnail = withContext(Dispatchers.IO) { VideoProcessor.extractThumbnail(context, videoUri, trimStartMs) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ── Top bar ──────────────────────────────────────────────────────────
        ChromaScreenHeader(
            title = "Oscellion",
            badgeText = "Flash Editor",
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
                            .clickable {
                                onSaveDraft(trimStartMs, trimEndMs, selectedFilter.name, brightness, contrast, saturation, hue)
                            }
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
                            .clickable {
                                onNext(trimStartMs, trimEndMs, selectedFilter.name, brightness, contrast, saturation, hue, thumbnail)
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Next", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        )

        // ── Video preview ─────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF111111)),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    androidx.media3.ui.PlayerView(ctx).apply {
                        this.player = player
                        useController = false
                        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                update = { view -> view.player = player },
                modifier = Modifier.fillMaxSize()
            )

            // Fake overlay for brightness/contrast and tint
            val overlayAlpha = (brightness / 200f).coerceIn(-0.5f, 0.5f)
            val baseTint = if (selectedFilter.name != "None") Color.Black.copy(alpha = 0.2f) else Color.Transparent
            val brightnessTint = if (overlayAlpha > 0) Color.White.copy(alpha = overlayAlpha) 
                                 else if (overlayAlpha < 0) Color.Black.copy(alpha = -overlayAlpha) 
                                 else Color.Transparent

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(baseTint)
                    .background(brightnessTint)
            )
        }

        // ── Trim timeline ─────────────────────────────────────────────────────
        if (durationMs > 0L) {
            Spacer(Modifier.height(12.dp))
            TrimTimeline(
                durationMs = durationMs,
                trimStartMs = trimStartMs,
                trimEndMs = trimEndMs,
                onTrimChange = { start, end ->
                    trimStartMs = start
                    trimEndMs = end
                    player.seekTo(start)
                }
            )
        }

        Spacer(Modifier.height(8.dp))

        // Duration badge
        if (durationMs > 0L) {
            val trimDuration = trimEndMs - trimStartMs
            val isOver = trimDuration > 30_000L
            Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(
                    "${trimDuration / 1000}s / 30s",
                    color = if (isOver) Color(0xFFFF3B30) else Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Tab bar (Filters / Adjust) ────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            listOf("Filters", "Adjust").forEachIndexed { index, label ->
                val selected = activeTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) Color.White.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { activeTab = index }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = if (selected) Color.White else Color.White.copy(alpha = 0.5f),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Filters carousel ──────────────────────────────────────────────────
        if (activeTab == 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FlashFilters.forEach { filter ->
                    FilterChip(filter, thumbnail, selected = filter.name == selectedFilter.name) {
                        selectedFilter = filter
                    }
                }
            }
        }

        // ── Adjustment sliders ────────────────────────────────────────────────
        if (activeTab == 1) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdjustSlider("Brightness", brightness, -100f, 100f, "%+.0f") { brightness = it }
                AdjustSlider("Contrast", contrast, 0.5f, 2f, "%.1fx") { contrast = it }
                AdjustSlider("Saturation", saturation, 0f, 3f, "%.1fx") { saturation = it }
                AdjustSlider("Hue", hue, -180f, 180f, "%+.0f°") { hue = it }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun TrimTimeline(
    durationMs: Long,
    trimStartMs: Long,
    trimEndMs: Long,
    onTrimChange: (Long, Long) -> Unit
) {
    val density = LocalDensity.current
    var barWidthPx by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .onSizeChanged { barWidthPx = it.width.toFloat() }
    ) {
        // Yellow selected range
        if (barWidthPx > 0f && durationMs > 0L) {
            val startFrac = trimStartMs.toFloat() / durationMs
            val endFrac = trimEndMs.toFloat() / durationMs
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(endFrac - startFrac)
                    .offset(x = with(density) { (startFrac * barWidthPx).toDp() })
                    .background(Color(0xFFFFD60A).copy(alpha = 0.35f))
                    .border(2.dp, Color(0xFFFFD60A))
            )

            // Start handle
            DragHandle(
                fraction = startFrac,
                barWidthPx = barWidthPx,
                onDrag = { newFrac ->
                    val newStart = (newFrac * durationMs).roundToLong().coerceIn(0L, trimEndMs - 1000L)
                    onTrimChange(newStart, trimEndMs)
                }
            )

            // End handle
            DragHandle(
                fraction = endFrac,
                barWidthPx = barWidthPx,
                onDrag = { newFrac ->
                    val newEnd = (newFrac * durationMs).roundToLong()
                        .coerceIn(trimStartMs + 1000L, durationMs.coerceAtMost(trimStartMs + 30_000L))
                    onTrimChange(trimStartMs, newEnd)
                }
            )
        }
    }
}

@Composable
private fun DragHandle(fraction: Float, barWidthPx: Float, onDrag: (Float) -> Unit) {
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(with(density) { 16.dp })
            .offset(x = with(density) { (fraction * barWidthPx - 8.dp.toPx()).toDp() })
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFFFD60A))
            .pointerInput(barWidthPx) {
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    val newFrac = ((fraction * barWidthPx + dragAmount) / barWidthPx).coerceIn(0f, 1f)
                    onDrag(newFrac)
                }
            }
    )
}

@Composable
fun FilterChip(
    filter: FlashFilter,
    thumbnail: Bitmap?,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    2.dp,
                    if (selected) Color(0xFF007AFF) else Color.White.copy(alpha = 0.2f),
                    RoundedCornerShape(12.dp)
                )
                .background(Color.DarkGray)
        ) {
            if (thumbnail != null) {
                androidx.compose.foundation.Image(
                    bitmap = thumbnail.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    colorFilter = ColorFilter.colorMatrix(filter.colorMatrix),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize().background(
                    if (filter.name == "B&W") Color.Gray else Color(0xFF2C2C2E)
                ))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            filter.name,
            color = if (selected) Color(0xFF007AFF) else Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun AdjustSlider(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    format: String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(format.format(value), color = Color(0xFF007AFF), fontSize = 13.sp)
        }
        Spacer(Modifier.height(4.dp))
        AppleSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = min..max
        )
    }
}

