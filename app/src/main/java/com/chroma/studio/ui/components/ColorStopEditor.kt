package com.chroma.studio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.WandSparkles
import com.composables.icons.lucide.GripVertical
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.Copy
import kotlinx.coroutines.launch
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.model.ColorStop
import com.chroma.studio.ui.components.glossyBorder
import com.chroma.studio.ui.theme.LocalChromaColors
import kotlin.math.roundToInt

@Composable
fun ColorStopEditor(
    stops: List<ColorStop>,
    onStopsChange: (List<ColorStop>) -> Unit,
    trailingContent: @Composable (() -> Unit)? = null
) {
    val colors = LocalChromaColors.current
    val shape = RoundedCornerShape(8.dp)

    // derivedStateOf: only re-sorts when stops actually change identity
    val sorted by remember(stops) { derivedStateOf { stops.sortedBy { it.position } } }

    var editingStopId by remember { mutableStateOf(sorted.firstOrNull()?.id) }

    // Auto-select first stop if current is deleted or null
    if (editingStopId == null && sorted.isNotEmpty()) {
        editingStopId = sorted.first().id
    } else if (editingStopId != null && sorted.none { it.id == editingStopId }) {
        editingStopId = sorted.firstOrNull()?.id
    }

    val currentOnStopsChange by androidx.compose.runtime.rememberUpdatedState(onStopsChange)
    val currentStops by androidx.compose.runtime.rememberUpdatedState(stops)

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("COLOR STOPS", color = colors.textMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            if (trailingContent != null) trailingContent()
        }
        Spacer(Modifier.padding(top = 8.dp))

        var trackWidthPx by remember { mutableFloatStateOf(1f) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clip(shape)
                .background(checkerboardBrush())
                .border(1.dp, colors.glassBorder, shape)
                .onSizeChanged { trackWidthPx = it.width.toFloat() }
        ) {

            // Gradient preview strip — key on sorted identities so it only recomposes when stop list changes
            val gradientBrush by remember(sorted) {
                derivedStateOf {
                    Brush.linearGradient(
                        colorStops = sorted.map { (it.position / 100f) to it.color }.toTypedArray()
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .background(gradientBrush)
                    // tap empty space to insert a new stop at that position
                    .pointerInput(Unit) {
                        detectTapGestures { tap ->
                            val pos = (tap.x / size.width * 100f).coerceIn(0f, 100f)
                            val nearestColor = currentStops
                                .minByOrNull { kotlin.math.abs(it.position - pos) }?.color ?: Color.White
                            val newStop = ColorStop(color = nearestColor, position = pos)
                            currentOnStopsChange(currentStops + newStop)
                            editingStopId = newStop.id
                        }
                    }
            )

            // Stop thumbs — each keyed on stop.id for stable identity
            sorted.forEach { stop ->
                val isSelected = stop.id == editingStopId

                // graphicsLayer translation: skips layout & draw passes, only triggers transform
                Box(
                    modifier = Modifier
                        .size(width = 14.dp, height = 42.dp)
                        .graphicsLayer {
                            val pct = stop.position / 100f
                            translationX = (trackWidthPx * pct) - 7.dp.toPx()
                            translationY = -6.dp.toPx()
                        }
                ) {
                    // Vertical pin line
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .graphicsLayer { translationY = 14.dp.toPx() }
                            .size(width = 2.dp, height = 28.dp)
                            .background(Color.Black)
                    )
                    // Thumb circle — drag + tap combined in one pointerInput block
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .size(14.dp)
                            .pointerInput(stop.id) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val deltaPct = (dragAmount.x / trackWidthPx) * 100f
                                    val newPos = (stop.position + deltaPct).coerceIn(0f, 100f)
                                    currentOnStopsChange(
                                        currentStops.map { if (it.id == stop.id) it.copy(position = newPos) else it }
                                    )
                                }
                            }
                            .pointerInput(stop.id) {
                                detectTapGestures(
                                    onTap = { editingStopId = stop.id },
                                    onLongPress = {
                                        if (currentStops.size > 2) {
                                            currentOnStopsChange(currentStops.filterNot { it.id == stop.id })
                                            if (editingStopId == stop.id) {
                                                editingStopId = currentStops.firstOrNull { it.id != stop.id }?.id
                                            }
                                        }
                                    }
                                )
                            }
                            .clip(CircleShape)
                            .background(stop.color)
                            .border(
                                2.dp,
                                if (isSelected) colors.primary else Color.White,
                                CircleShape
                            )
                    )
                }
            }
        }

        Spacer(Modifier.padding(top = 8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("TAP TRACK TO ADD", color = colors.textMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Text("LONG PRESS TO REMOVE", color = colors.textMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        }

        Spacer(Modifier.padding(top = 16.dp))
        Text("COLOR STOPS LIST", color = colors.textMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.padding(top = 8.dp))
        
        var draggingIndex by remember { androidx.compose.runtime.mutableIntStateOf(-1) }
        val dragOffsetAnim = remember { androidx.compose.animation.core.Animatable(0f) }
        var itemHeightPx by remember { androidx.compose.runtime.mutableIntStateOf(1) }
        val liftProgress = remember { androidx.compose.animation.core.Animatable(0f) }
        val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

        androidx.compose.runtime.LaunchedEffect(draggingIndex) {
            if (draggingIndex >= 0) {
                liftProgress.animateTo(1f, androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow))
            } else {
                liftProgress.animateTo(0f, androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessHigh))
            }
        }

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            sorted.forEachIndexed { index, stop ->
                val isSelected = stop.id == editingStopId
                val itemBg = if (isSelected) colors.primary.copy(alpha = 0.12f) else colors.glassBg
                val itemBorder = if (isSelected) colors.primary.copy(alpha = 0.4f) else colors.glassBorder
                
                val isDragging = index == draggingIndex
                val flipOffset = remember { androidx.compose.animation.core.Animatable(0f) }
                val density = androidx.compose.ui.platform.LocalDensity.current
                val spacingPx = remember { with(density) { 6.dp.toPx() } }
                val fullItemHeight = itemHeightPx + spacingPx

                val targetFlipOffset = when {
                    draggingIndex < 0 -> 0f
                    isDragging -> 0f
                    else -> {
                        val steps = Math.round(dragOffsetAnim.value / fullItemHeight)
                        val draggedToIndex = (draggingIndex + steps).coerceIn(0, sorted.size - 1)
                        when {
                            draggingIndex < draggedToIndex && index in (draggingIndex + 1)..draggedToIndex -> -fullItemHeight
                            draggingIndex > draggedToIndex && index in draggedToIndex until draggingIndex -> fullItemHeight
                            else -> 0f
                        }
                    }
                }

                androidx.compose.runtime.LaunchedEffect(targetFlipOffset) {
                    if (!isDragging) {
                        flipOffset.animateTo(
                            targetValue = targetFlipOffset,
                            animationSpec = androidx.compose.animation.core.spring(
                                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                            )
                        )
                    }
                }
                
                androidx.compose.runtime.LaunchedEffect(draggingIndex) {
                    if (draggingIndex < 0) flipOffset.snapTo(0f)
                }

                Row(
                    modifier = Modifier
                        .onGloballyPositioned { if (itemHeightPx <= 1) itemHeightPx = it.size.height }
                        .zIndex(if (isDragging) 10f else 1f)
                        .graphicsLayer {
                            if (isDragging) {
                                translationY = dragOffsetAnim.value
                                scaleX = 1f + liftProgress.value * 0.02f
                                scaleY = 1f + liftProgress.value * 0.02f
                                shadowElevation = liftProgress.value * 12.dp.toPx()
                                alpha = 0.96f
                            } else {
                                translationY = flipOffset.value
                                scaleX = 1f - liftProgress.value * 0.01f
                                scaleY = 1f - liftProgress.value * 0.01f
                                alpha = 1f - liftProgress.value * 0.05f
                            }
                        }
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(itemBg)
                        .border(1.dp, itemBorder, RoundedCornerShape(8.dp))
                        .clickable { editingStopId = stop.id }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Lucide.GripVertical,
                            contentDescription = "Reorder",
                            tint = colors.textMuted,
                            modifier = Modifier
                                .size(16.dp)
                                .pointerInput(stop.id) {
                                    detectDragGestures(
                                        onDragStart = {
                                            draggingIndex = index
                                            coroutineScope.launch {
                                                dragOffsetAnim.snapTo(0f)
                                            }
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            coroutineScope.launch {
                                                dragOffsetAnim.snapTo(dragOffsetAnim.value + dragAmount.y)
                                            }
                                        },
                                        onDragEnd = {
                                            val steps = Math.round(dragOffsetAnim.value / fullItemHeight)
                                            val to = (draggingIndex + steps).coerceIn(0, sorted.size - 1)
                                            
                                            coroutineScope.launch {
                                                val targetOffset = (to - draggingIndex) * fullItemHeight
                                                dragOffsetAnim.animateTo(targetOffset, androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMedium))
                                                
                                                if (to != draggingIndex) {
                                                    val sortedMutable = sorted.toMutableList()
                                                    val moved = sortedMutable.removeAt(draggingIndex)
                                                    sortedMutable.add(to, moved)
                                                    
                                                    val oldPositions = sorted.map { it.position }
                                                    val updatedStops = sortedMutable.mapIndexed { i, s ->
                                                        s.copy(position = oldPositions[i])
                                                    }
                                                    
                                                    val finalStops = currentStops.map { c ->
                                                        updatedStops.find { it.id == c.id } ?: c
                                                    }
                                                    currentOnStopsChange(finalStops)
                                                }
                                                draggingIndex = -1
                                                dragOffsetAnim.snapTo(0f)
                                            }
                                        },
                                        onDragCancel = {
                                            coroutineScope.launch {
                                                dragOffsetAnim.animateTo(0f, androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMedium))
                                                draggingIndex = -1
                                            }
                                        }
                                    )
                                }
                        )
                        Spacer(Modifier.padding(start = 12.dp))
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(stop.color)
                                .border(1.dp, colors.glassBorder, CircleShape)
                        )
                        Spacer(Modifier.padding(start = 10.dp))
                        Text(
                            text = String.format("#%06X", 0xFFFFFF and stop.color.toArgb()),
                            color = colors.textMain,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${stop.position.toInt()}%",
                            color = colors.textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.padding(start = 12.dp))
                        
                        // Copy Button
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .clickable { 
                                    val newPos = (stop.position + 5f).coerceAtMost(100f)
                                    val newStop = ColorStop(color = stop.color, position = newPos)
                                    currentOnStopsChange(currentStops + newStop)
                                    editingStopId = newStop.id
                                }
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Lucide.Copy,
                                contentDescription = "Copy",
                                modifier = Modifier.size(12.dp),
                                tint = colors.textMuted
                            )
                        }
                        
                        Spacer(Modifier.padding(start = 4.dp))
                        
                        // Delete Button
                        val canDelete = currentStops.size > 2
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .clickable(enabled = canDelete) { 
                                    if (canDelete) {
                                        currentOnStopsChange(currentStops.filterNot { it.id == stop.id })
                                        if (editingStopId == stop.id) {
                                            editingStopId = currentStops.firstOrNull { it.id != stop.id }?.id
                                        }
                                    }
                                }
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Lucide.Trash2,
                                contentDescription = "Remove",
                                modifier = Modifier.size(12.dp),
                                tint = if (canDelete) colors.textMuted else colors.textMuted.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }

        val editing = sorted.find { it.id == editingStopId }
        if (editing != null) {
            Spacer(Modifier.padding(top = 12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(editing.color)
                            .border(1.5.dp, colors.primary, CircleShape)
                    )
                    Spacer(Modifier.padding(start = 8.dp))
                    Text("SELECTED STOP", color = colors.textMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "POS ${editing.position.toInt()}%",
                    color = colors.primary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.padding(top = 6.dp))
            ChromaSlider(
                value = editing.position,
                onValueChange = { newPos ->
                    currentOnStopsChange(currentStops.map { if (it.id == editing.id) it.copy(position = newPos) else it })
                },
                valueRange = 0f..100f
            )

            Spacer(Modifier.padding(top = 12.dp))
            Text("COLOR", color = colors.textMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.padding(top = 8.dp))
            InlineColorPicker(
                initialColor = editing.color,
                onColorChange = { newColor ->
                    currentOnStopsChange(currentStops.map { if (it.id == editing.id) it.copy(color = newColor) else it })
                }
            )
        }
    }
}

@Composable
fun InlineColorPicker(
    initialColor: Color,
    onColorChange: (Color) -> Unit
) {
    val colors = LocalChromaColors.current
    val hsv = remember { FloatArray(3) }

    // mutableFloatStateOf: specialized primitive state, avoids boxing overhead on every drag
    var hue by remember { mutableFloatStateOf(0f) }
    var sat by remember { mutableFloatStateOf(0f) }
    var value by remember { mutableFloatStateOf(0f) }
    var alpha by remember { mutableFloatStateOf(1f) }
    var hexText by remember { mutableStateOf("") }

    val currentOnColorChange by androidx.compose.runtime.rememberUpdatedState(onColorChange)

    // Sync from external color ONLY when it changes (e.g., switching stops or undo)
    androidx.compose.runtime.LaunchedEffect(initialColor) {
        val currentLocalColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))).copy(alpha = alpha)
        if (currentLocalColor != initialColor) {
            android.graphics.Color.colorToHSV(initialColor.toArgb(), hsv)
            if (hsv[1] > 0.001f && hsv[2] > 0.001f || hexText.isEmpty()) {
                hue = hsv[0]
            }
            sat = hsv[1]
            value = hsv[2]
            alpha = initialColor.alpha
            hexText = colorToHex(initialColor)
        }
    }

    fun currentColor(): Color {
        val rgb = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))
        return Color(rgb).copy(alpha = alpha)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, colors.glassBorder, RoundedCornerShape(12.dp))
            .background(colors.glassBg)
            .padding(16.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            // SV Picker area
            var svWidthPx by remember { mutableFloatStateOf(1f) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .onSizeChanged { svWidthPx = it.width.toFloat() }
            ) {
                val pureHueColor by remember(hue) {
                    derivedStateOf { Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))) }
                }

                // Single combined pointerInput for tap + drag to reduce gesture recognizer overhead
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                sat = (change.position.x / size.width).coerceIn(0f, 1f)
                                value = (1f - change.position.y / size.height).coerceIn(0f, 1f)
                                val nc = currentColor()
                                hexText = colorToHex(nc)
                                currentOnColorChange(nc)
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures { tap ->
                                sat = (tap.x / size.width).coerceIn(0f, 1f)
                                value = (1f - tap.y / size.height).coerceIn(0f, 1f)
                                val nc = currentColor()
                                hexText = colorToHex(nc)
                                currentOnColorChange(nc)
                            }
                        }
                ) {
                    drawRect(Brush.horizontalGradient(listOf(Color.White, pureHueColor)))
                    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                }

                // Thumb — graphicsLayer avoids layout pass on sat/value changes
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            translationX = svWidthPx * sat - 10.dp.toPx()
                            translationY = (140.dp.toPx() * (1f - value)) - 10.dp.toPx()
                        }
                        .shadow(4.dp, CircleShape)
                        .background(Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))))
                        .border(3.dp, Color.White, CircleShape)
                )
            }

            Spacer(Modifier.padding(top = 16.dp))

            // Hue Slider
            var hueWidthPx by remember { mutableFloatStateOf(1f) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .onSizeChanged { hueWidthPx = it.width.toFloat() }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            hue = ((change.position.x / size.width) * 360f).coerceIn(0f, 360f)
                            val nc = currentColor()
                            hexText = colorToHex(nc)
                            currentOnColorChange(nc)
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { tap ->
                            hue = ((tap.x / size.width) * 360f).coerceIn(0f, 360f)
                            val nc = currentColor()
                            hexText = colorToHex(nc)
                            currentOnColorChange(nc)
                        }
                    }
            ) {
                val hueColors = remember {
                    listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                }
                Box(modifier = Modifier.fillMaxWidth().height(12.dp).align(Alignment.Center).clip(RoundedCornerShape(6.dp))) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawRect(Brush.horizontalGradient(hueColors))
                    }
                }
                // Thumb via graphicsLayer — no layout recomposition on hue drag
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            translationX = hueWidthPx * (hue / 360f) - 10.dp.toPx()
                            translationY = 2.dp.toPx()
                        }
                        .shadow(4.dp, CircleShape)
                        .background(Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))))
                        .border(3.dp, Color.White, CircleShape)
                )
            }

            Spacer(Modifier.padding(top = 16.dp))

            // Alpha Slider
            var alphaWidthPx by remember { mutableFloatStateOf(1f) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .onSizeChanged { alphaWidthPx = it.width.toFloat() }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            alpha = (change.position.x / size.width).coerceIn(0f, 1f)
                            val nc = currentColor()
                            hexText = colorToHex(nc)
                            currentOnColorChange(nc)
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { tap ->
                            alpha = (tap.x / size.width).coerceIn(0f, 1f)
                            val nc = currentColor()
                            hexText = colorToHex(nc)
                            currentOnColorChange(nc)
                        }
                    }
            ) {
                val baseColor by remember(hue, sat, value) {
                    derivedStateOf { Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))) }
                }
                Box(modifier = Modifier.fillMaxWidth().height(12.dp).align(Alignment.Center).clip(RoundedCornerShape(6.dp)).background(checkerboardBrush())) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawRect(Brush.horizontalGradient(listOf(Color.Transparent, baseColor)))
                    }
                }
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            translationX = alphaWidthPx * alpha - 10.dp.toPx()
                            translationY = 2.dp.toPx()
                        }
                        .shadow(4.dp, CircleShape)
                        .background(Color.White)
                        .drawWithContent {
                            drawCircle(checkerboardBrush())
                            drawCircle(currentColor())
                        }
                        .border(3.dp, Color.White, CircleShape)
                )
            }

            Spacer(Modifier.padding(top = 16.dp))

            // Hex + Alpha inputs
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = hexText,
                    onValueChange = { text ->
                        hexText = text
                        hexToColor(text)?.let { c ->
                            android.graphics.Color.colorToHSV(c.toArgb(), hsv)
                            hue = hsv[0]; sat = hsv[1]; value = hsv[2]; alpha = c.alpha
                            currentOnColorChange(c)
                        }
                    },
                    modifier = Modifier.weight(2f).height(44.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 12.sp,
                        color = colors.textMain,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = colors.glassBorder,
                        focusedBorderColor = colors.primary,
                        unfocusedContainerColor = colors.glassBg,
                        focusedContainerColor = colors.glassBg
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = "${(alpha * 100).roundToInt()} %",
                    onValueChange = { /* read-only display */ },
                    modifier = Modifier.weight(1f).height(44.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 12.sp,
                        color = colors.textMain,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = colors.glassBorder,
                        focusedBorderColor = colors.primary,
                        unfocusedContainerColor = colors.glassBg,
                        focusedContainerColor = colors.glassBg
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            Spacer(Modifier.padding(top = 16.dp))
            // Auto-Harmony Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .glossyBorder(RoundedCornerShape(8.dp), colors)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                        hue = (hue + 30f) % 360f
                        val nc = currentColor()
                        hexText = colorToHex(nc)
                        currentOnColorChange(nc)
                    }
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Lucide.WandSparkles, contentDescription = null, tint = colors.textMain, modifier = Modifier.size(14.dp))
                Spacer(Modifier.padding(start = 6.dp))
                Text("Auto-Harmony", color = colors.textMain, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun checkerboardBrush(): Brush {
    return Brush.linearGradient(
        listOf(Color(0xFFE2E8F0), Color(0xFFF8FAFC))
    )
}

fun colorToHex(color: Color): String {
    val a = (color.alpha * 255).toInt()
    val r = (color.red * 255).toInt()
    val g = (color.green * 255).toInt()
    val b = (color.blue * 255).toInt()
    return if (a == 255) {
        String.format("#%02X%02X%02X", r, g, b)
    } else {
        String.format("#%02X%02X%02X%02X", a, r, g, b)
    }
}

fun hexToColor(hex: String): Color? {
    if (hex.isBlank()) return null
    var cleanHex = hex.trim().removePrefix("#")
    if (cleanHex.length == 3) {
        cleanHex = cleanHex.map { "$it$it" }.joinToString("")
    }
    return try {
        when (cleanHex.length) {
            6 -> Color(android.graphics.Color.parseColor("#$cleanHex"))
            8 -> {
                val a = cleanHex.substring(0, 2).toInt(16) / 255f
                val rgb = cleanHex.substring(2)
                Color(android.graphics.Color.parseColor("#$rgb")).copy(alpha = a)
            }
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}
