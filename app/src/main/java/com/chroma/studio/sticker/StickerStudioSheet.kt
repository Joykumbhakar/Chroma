package com.chroma.studio.sticker

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chroma.studio.ui.components.AppleSlider
import com.chroma.studio.ui.components.GlassButton
import com.chroma.studio.ui.components.LocalSliderScrubbingState
import com.chroma.studio.ui.components.editorChromeFadeOnScrub
import com.chroma.studio.ui.components.focusSliderContainer
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class StickerStudioMode {
    PREVIEW,
    MANUAL_ERASE
}

@Composable
fun StickerStudioModal(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onStickerCreated: (StickerItem) -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = LocalOscellionColors.current

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var cutoutBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewWithBorderBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var isProcessing by remember { mutableStateOf(false) }
    var activeMode by remember { mutableStateOf(StickerStudioMode.PREVIEW) }
    var applyWhiteBorder by remember { mutableStateOf(true) }
    var eraserBrushRadius by remember { mutableFloatStateOf(24f) }
    val eraserHistory = remember { mutableStateListOf<Bitmap>() }

    // Gallery Picker Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            isProcessing = true
            scope.launch {
                val bmp = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }
                if (bmp != null) {
                    val fixedBmp = bmp.copy(Bitmap.Config.ARGB_8888, true)
                    originalBitmap = fixedBmp
                    // Automatically execute background removal on selection
                    val autoCut = withContext(Dispatchers.Default) {
                        StickerStudioEngine.autoCutoutSubject(fixedBmp, tolerance = 32)
                    }
                    cutoutBitmap = autoCut
                    eraserHistory.clear()
                    eraserHistory.add(autoCut.copy(Bitmap.Config.ARGB_8888, true))

                    // Automatically generate 5px white border preview
                    val bordered = withContext(Dispatchers.Default) {
                        StickerStudioEngine.apply5pxWhiteBorder(autoCut, borderPx = 5)
                    }
                    previewWithBorderBitmap = bordered
                }
                isProcessing = false
            }
        }
    }

    // Function to re-render 5px border preview
    fun updateBorderPreview() {
        val currCutout = cutoutBitmap ?: return
        scope.launch {
            val bordered = withContext(Dispatchers.Default) {
                if (applyWhiteBorder) {
                    StickerStudioEngine.apply5pxWhiteBorder(currCutout, borderPx = 5)
                } else {
                    currCutout
                }
            }
            previewWithBorderBitmap = bordered
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.bg)
                .systemBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Action Bar (clean One UI — action buttons only, no branding/title clutter)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .editorChromeFadeOnScrub(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val iconBtnBg = if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF0F2F5)
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(iconBtnBg)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Lucide.X, contentDescription = "Close", tint = colors.textMain, modifier = Modifier.size(19.dp))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (eraserHistory.size > 1) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(iconBtnBg)
                                    .clickable {
                                        if (eraserHistory.size > 1) {
                                            eraserHistory.removeAt(eraserHistory.lastIndex)
                                            val prev = eraserHistory.last().copy(Bitmap.Config.ARGB_8888, true)
                                            cutoutBitmap = prev
                                            updateBorderPreview()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Lucide.Undo2, contentDescription = "Undo", tint = colors.textMain, modifier = Modifier.size(17.dp))
                            }
                        }

                        if (cutoutBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(iconBtnBg)
                                    .clickable { galleryLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Lucide.ImagePlus, contentDescription = "Replace Image", tint = colors.textMain, modifier = Modifier.size(17.dp))
                            }
                        }

                        Button(
                            onClick = {
                                val targetBitmap = previewWithBorderBitmap ?: cutoutBitmap
                                if (targetBitmap != null) {
                                    isProcessing = true
                                    scope.launch {
                                        val finalBmp = if (applyWhiteBorder && previewWithBorderBitmap == null) {
                                            withContext(Dispatchers.Default) {
                                                StickerStudioEngine.apply5pxWhiteBorder(targetBitmap, borderPx = 5)
                                            }
                                        } else targetBitmap

                                        val stickerItem = StickerRepository.saveCustomSticker(
                                            context = context,
                                            bitmap = finalBmp,
                                            name = "Custom Sticker"
                                        )
                                        isProcessing = false
                                        onStickerCreated(stickerItem)
                                        onDismiss()
                                    }
                                }
                            },
                            enabled = cutoutBitmap != null && !isProcessing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0381FE),
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFF0381FE).copy(alpha = 0.35f),
                                disabledContentColor = Color.White.copy(alpha = 0.7f)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 7.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Done", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Main Interactive Canvas / Preview Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF161922))
                        .border(1.dp, colors.glassBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (cutoutBitmap == null && !isProcessing) {
                        // Empty State: Prompt to select photo
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(colors.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Lucide.Sticker,
                                    contentDescription = "Sticker",
                                    tint = colors.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Create a Custom Sticker",
                                color = colors.textMain,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Select a photo to auto-remove background and generate an authentic 5px white die-cut sticker for Stories & Chats.",
                                color = colors.textMuted,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { galleryLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Lucide.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Choose from Gallery", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    } else if (isProcessing) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = colors.primary, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Processing Subject & 5px Border...",
                                color = colors.textMain,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        // Display Cutout with Checkerboard & Border
                        val displayBmp = if (activeMode == StickerStudioMode.MANUAL_ERASE) {
                            cutoutBitmap
                        } else {
                            if (applyWhiteBorder) previewWithBorderBitmap ?: cutoutBitmap else cutoutBitmap
                        }

                        if (displayBmp != null) {
                            // Touch Drawing for Manual Eraser (maps Compose ContentScale.Fit coords -> bitmap pixels)
                            val currentPoints = remember { mutableStateListOf<Offset>() }
                            val padPx = with(androidx.compose.ui.platform.LocalDensity.current) { 16.dp.toPx() }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(activeMode) {
                                        if (activeMode == StickerStudioMode.MANUAL_ERASE) {
                                            detectDragGestures(
                                                onDragStart = { offset ->
                                                    currentPoints.clear()
                                                    currentPoints.add(offset)
                                                },
                                                onDrag = { change, _ ->
                                                    change.consume()
                                                    currentPoints.add(change.position)
                                                },
                                                onDragEnd = {
                                                    val src = cutoutBitmap ?: return@detectDragGestures
                                                    val availW = (size.width - padPx * 2f).coerceAtLeast(1f)
                                                    val availH = (size.height - padPx * 2f).coerceAtLeast(1f)
                                                    val fitScale = minOf(availW / src.width.toFloat(), availH / src.height.toFloat()).coerceAtLeast(0.01f)
                                                    val drawnW = src.width * fitScale
                                                    val drawnH = src.height * fitScale
                                                    val originX = padPx + (availW - drawnW) / 2f
                                                    val originY = padPx + (availH - drawnH) / 2f

                                                    val mappedPoints = currentPoints.map { pt ->
                                                        Offset(
                                                            x = ((pt.x - originX) / fitScale).coerceIn(0f, src.width.toFloat()),
                                                            y = ((pt.y - originY) / fitScale).coerceIn(0f, src.height.toFloat())
                                                        )
                                                    }
                                                    val mappedRadius = (eraserBrushRadius / fitScale).coerceIn(4f, 200f)
                                                    val erased = StickerStudioEngine.manualErase(
                                                        source = src,
                                                        pathPoints = mappedPoints,
                                                        brushRadiusPx = mappedRadius
                                                    )
                                                    cutoutBitmap = erased
                                                    eraserHistory.add(erased.copy(Bitmap.Config.ARGB_8888, true))
                                                    while (eraserHistory.size > 15) {
                                                        val evicted = eraserHistory.removeAt(0)
                                                        if (evicted !== erased && evicted !== originalBitmap) {
                                                            try { evicted.recycle() } catch (_: Throwable) {}
                                                        }
                                                    }
                                                    currentPoints.clear()
                                                    updateBorderPreview()
                                                }
                                            )
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = displayBmp.asImageBitmap(),
                                    contentDescription = "Cutout Sticker Preview",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Editing Toolbar
                if (cutoutBitmap != null) {
                    val stickerScrub = LocalSliderScrubbingState.current
                    val toolbarBg by animateColorAsState(
                        targetValue = if (stickerScrub.isScrubbing) Color.Transparent else colors.glassBg,
                        animationSpec = androidx.compose.animation.core.tween(180),
                        label = "stickerToolbarBg"
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(toolbarBg)
                            .padding(12.dp)
                    ) {
                        // Tool Switcher Tabs
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .editorChromeFadeOnScrub(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Auto AI Cutout
                            FilterChip(
                                selected = activeMode == StickerStudioMode.PREVIEW,
                                onClick = {
                                    activeMode = StickerStudioMode.PREVIEW
                                    val orig = originalBitmap
                                    if (orig != null) {
                                        scope.launch {
                                            isProcessing = true
                                            val auto = withContext(Dispatchers.Default) {
                                                StickerStudioEngine.autoCutoutSubject(orig, tolerance = 32)
                                            }
                                            cutoutBitmap = auto
                                            updateBorderPreview()
                                            isProcessing = false
                                        }
                                    }
                                },
                                label = { Text("Auto Cutout", fontSize = 12.sp) },
                                leadingIcon = { Icon(Lucide.Sparkles, null, modifier = Modifier.size(14.dp)) }
                            )

                            // 2. Manual Eraser
                            FilterChip(
                                selected = activeMode == StickerStudioMode.MANUAL_ERASE,
                                onClick = { activeMode = StickerStudioMode.MANUAL_ERASE },
                                label = { Text("Manual Eraser", fontSize = 12.sp) },
                                leadingIcon = { Icon(Lucide.Eraser, null, modifier = Modifier.size(14.dp)) }
                            )

                            // 3. 5px White Border Toggle
                            FilterChip(
                                selected = applyWhiteBorder,
                                onClick = {
                                    applyWhiteBorder = !applyWhiteBorder
                                    updateBorderPreview()
                                },
                                label = { Text("5px Border", fontSize = 12.sp) },
                                leadingIcon = { Icon(Lucide.CircleDot, null, modifier = Modifier.size(14.dp)) }
                            )
                        }

                        // Eraser brush controls when in MANUAL_ERASE mode
                        if (activeMode == StickerStudioMode.MANUAL_ERASE) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusSliderContainer(
                                        sliderId = "StickerEraserSize",
                                        isDark = colors.isDark,
                                        cornerRadius = 14.dp,
                                        horizontalPadding = 10.dp,
                                        verticalPadding = 4.dp
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Brush Size", color = colors.textMuted, fontSize = 11.sp)
                                AppleSlider(
                                    value = eraserBrushRadius,
                                    sliderId = "StickerEraserSize",
                                    onValueChange = { eraserBrushRadius = it },
                                    valueRange = 8f..64f,
                                    modifier = Modifier.weight(1f)
                                )
                                if (eraserHistory.size > 1) {
                                    IconButton(
                                        onClick = {
                                            if (eraserHistory.size > 1) {
                                                eraserHistory.removeAt(eraserHistory.lastIndex)
                                                val prev = eraserHistory.last().copy(Bitmap.Config.ARGB_8888, true)
                                                cutoutBitmap = prev
                                                updateBorderPreview()
                                            }
                                        },
                                        modifier = Modifier
                                            .editorChromeFadeOnScrub()
                                            .size(32.dp)
                                    ) {
                                        Icon(Lucide.Undo2, contentDescription = "Undo", tint = colors.textMain, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        // Change Photo Button
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .editorChromeFadeOnScrub(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            TextButton(
                                onClick = { galleryLauncher.launch("image/*") }
                            ) {
                                Icon(Lucide.Image, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Choose Another Image", color = colors.textMuted, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
