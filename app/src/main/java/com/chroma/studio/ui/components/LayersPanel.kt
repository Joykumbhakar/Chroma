package com.chroma.studio.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Shuffle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.chroma.studio.model.GradientLayer
import com.chroma.studio.ui.theme.LocalChromaColors
import com.chroma.studio.viewmodel.ChromaViewModel

/**
 * LayersPanel with FLIP-style drag-and-drop reordering.
 * - Long press lifts the card (scale up + elevation shadow).
 * - Drag continuously re-orders the list in the ViewModel.
 * - Displaced cards animate to their new position with a spring.
 * - Dragged card floats above all others via zIndex + graphicsLayer elevation.
 */
@Composable
fun LayersPanel(
    layers: List<GradientLayer>,
    activeLayerId: String,
    vm: ChromaViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalChromaColors.current

    // Drag state
    var draggingIndex by remember { mutableIntStateOf(-1) }
    val dragOffsetAnim = remember { Animatable(0f) }
    var itemHeightPx by remember { mutableIntStateOf(1) }

    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    // Lift animation for the dragged card (0f = normal, 1f = fully lifted)
    val liftProgress = remember { Animatable(0f) }

    val listState = rememberLazyListState()

    GlassPanel(modifier = modifier.fillMaxSize(), cornerRadius = 16) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTIVE LAYERS",
                        color = colors.textMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.glassBg)
                                .glossyBorder(RoundedCornerShape(6.dp), colors)
                                .clickable { vm.addLayer() }
                                .padding(horizontal = 10.dp),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                androidx.compose.material3.Icon(
                                    imageVector = Lucide.Plus,
                                    contentDescription = "Add Layer",
                                    tint = colors.textMain,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.padding(start = 6.dp))
                                Text("Add Layer", color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable { vm.randomize() },
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Lucide.Shuffle,
                                contentDescription = "Randomize",
                                tint = colors.textMain,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            itemsIndexed(layers, key = { _, layer -> layer.id }) { index, layer ->

                val isDragging = index == draggingIndex

                // FLIP: each non-dragged card gets a spring-animated Y offset
                // whenever the drag position changes the logical order
                val flipOffset = remember { Animatable(0f) }

                val density = androidx.compose.ui.platform.LocalDensity.current
                val spacingPx = remember { with(density) { 16.dp.toPx() } }
                val fullItemHeight = itemHeightPx + spacingPx

                // Compute where this item should be relative to its natural position.
                // If dragging index has crossed over this item, shift it by one item height.
                val targetFlipOffset = when {
                    draggingIndex < 0 -> 0f          // no drag active
                    isDragging -> 0f                 // handled directly in graphicsLayer
                    else -> {
                        // Compute how many slots the drag has shifted
                        val steps = Math.round(dragOffsetAnim.value / fullItemHeight)
                        val draggedToIndex = (draggingIndex + steps).coerceIn(0, layers.size - 1)
                        when {
                            // Drag going DOWN: items between original and target shift UP
                            draggingIndex < draggedToIndex && index in (draggingIndex + 1)..draggedToIndex ->
                                -fullItemHeight
                            // Drag going UP: items between target and original shift DOWN
                            draggingIndex > draggedToIndex && index in draggedToIndex until draggingIndex ->
                                fullItemHeight
                            else -> 0f
                        }
                    }
                }

                // Animate FLIP offset with a bouncy spring
                LaunchedEffect(targetFlipOffset) {
                    if (!isDragging) {
                        flipOffset.animateTo(
                            targetValue = targetFlipOffset,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                }

                // When drag ends, snap back to 0
                LaunchedEffect(draggingIndex) {
                    if (draggingIndex < 0) {
                        flipOffset.snapTo(0f)
                    }
                }

                LayerCard(
                    layer = layer,
                    isDragging = isDragging,
                    dragHandleModifier = Modifier
                        .onGloballyPositioned { coords ->
                            if (!layer.expanded || itemHeightPx <= 1) {
                                itemHeightPx = coords.size.height
                            }
                        }
                        .zIndex(if (isDragging) 10f else 1f)
                        .graphicsLayer {
                            // Dragged card floats up with lift
                            if (isDragging) {
                                translationY = dragOffsetAnim.value
                                scaleX = 1f + liftProgress.value * 0.04f
                                scaleY = 1f + liftProgress.value * 0.04f
                                alpha = 0.96f
                            } else {
                                translationY = flipOffset.value
                                scaleX = 1f - liftProgress.value * 0.01f
                                scaleY = 1f - liftProgress.value * 0.01f
                                alpha = 1f - liftProgress.value * 0.08f
                            }
                        }
                        .pointerInput(layer.id, index, layer.expanded) {
                            if (!layer.expanded) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                    draggingIndex = index
                                    coroutineScope.launch {
                                        dragOffsetAnim.snapTo(0f)
                                        liftProgress.animateTo(1f, spring(stiffness = Spring.StiffnessMediumLow))
                                    }
                                },
                                onDrag = { change, drag ->
                                    change.consume()
                                    coroutineScope.launch {
                                        dragOffsetAnim.snapTo(dragOffsetAnim.value + drag.y)
                                    }
                                },
                                onDragEnd = {
                                    val spacingPx = with(density) { 16.dp.toPx() }
                                    val currentFullItemHeight = itemHeightPx + spacingPx
                                    val steps = Math.round(dragOffsetAnim.value / currentFullItemHeight)
                                    val to = (draggingIndex + steps).coerceIn(0, layers.size - 1)
                                    
                                    coroutineScope.launch {
                                        // Animate drop into slot
                                        val targetOffset = (to - draggingIndex) * currentFullItemHeight
                                        dragOffsetAnim.animateTo(targetOffset, spring(stiffness = Spring.StiffnessMedium))
                                        
                                        if (to != draggingIndex) {
                                            vm.moveLayer(draggingIndex, to)
                                        }
                                        draggingIndex = -1
                                        dragOffsetAnim.snapTo(0f)
                                        liftProgress.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                                    }
                                },
                                onDragCancel = {
                                    coroutineScope.launch {
                                        dragOffsetAnim.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                                        draggingIndex = -1
                                        liftProgress.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                                    }
                                }
                            )
                            }
                        },
                    onToggleExpand = { vm.selectLayer(layer.id); vm.toggleExpanded(layer.id) },
                    onTypeChange = { t -> vm.updateLayer(layer.id, recordHistory = true) { it.copy(type = t) } },
                    onBlendChange = { b -> vm.updateLayer(layer.id, recordHistory = true) { it.copy(blendMode = b) } },
                    onOpacityChange = { o -> vm.updateLayer(layer.id) { it.copy(opacity = o) } },
                    onAngleChange = { a -> vm.updateLayer(layer.id) { it.copy(angle = a) } },
                    onStopsChange = { s -> vm.updateLayer(layer.id, recordHistory = true) { it.copy(stops = s) } },
                    onToggleVisibility = { vm.updateLayer(layer.id) { it.copy(visible = !it.visible) } },
                    onCenterXChange = { cx -> vm.updateLayer(layer.id) { it.copy(centerX = cx) } },
                    onCenterYChange = { cy -> vm.updateLayer(layer.id) { it.copy(centerY = cy) } },
                    onDelete = { vm.showDeleteLayerConfirmation(layer.id) },
                    onDuplicate = { vm.duplicateLayer(layer.id) },
                    onRepeatPatternChange = { rp -> vm.updateLayer(layer.id) { it.copy(repeatPattern = rp) } },
                    onWidthChange = { w -> vm.updateLayer(layer.id) { it.copy(width = w) } },
                    onHeightChange = { h -> vm.updateLayer(layer.id) { it.copy(height = h) } },
                    onFeatherChange = { f -> vm.updateLayer(layer.id) { it.copy(feather = f) } },
                    onWaveSpeedChange = { ws -> vm.updateLayer(layer.id) { it.copy(waveSpeed = ws) } },
                    onComplexityChange = { c -> vm.updateLayer(layer.id) { it.copy(complexity = c) } },
                    onBrightnessChange = { br -> vm.updateLayer(layer.id) { it.copy(brightness = br) } },
                    onColumnsChange = { col -> 
                        vm.updateLayer(layer.id) { it.copy(columns = col) }
                        vm.regenerateMesh(layer.id)
                    },
                    onRowsChange = { r -> 
                        vm.updateLayer(layer.id) { it.copy(rows = r) }
                        vm.regenerateMesh(layer.id)
                    },
                    onBaseBackgroundChange = { bb -> vm.updateLayer(layer.id) { it.copy(hasBaseBackground = bb) } },
                    onBlobBgColorChange = { color -> vm.updateLayer(layer.id) { it.copy(blobBgColor = color) } },
                    onRegenerateMesh = { vm.regenerateMesh(layer.id) },
                    onAddBlob = { vm.addBlob(layer.id) },
                    onRemoveBlob = { idx -> vm.removeBlob(layer.id, idx) },
                    onSetActiveBlob = { idx -> vm.setActiveBlob(layer.id, idx) },
                    onUpdateBlobParam = { idx, prop, value -> vm.updateBlobParam(layer.id, idx, prop, value) }
                )
            }
        }
    }

    // Animate the lift progress when drag starts/ends
    LaunchedEffect(draggingIndex) {
        if (draggingIndex >= 0) {
            liftProgress.animateTo(1f, spring(stiffness = Spring.StiffnessMediumLow))
        } else {
            liftProgress.animateTo(0f, spring(stiffness = Spring.StiffnessHigh))
        }
    }

    vm.pendingDeleteLayerId?.let {
        DeleteLayerModal(
            dontAskAgain = vm.dontAskDeleteAgain,
            onDontAskAgainChange = { vm.updateDontAskDeleteAgain(it) },
            onCancel = vm::cancelDeleteLayer,
            onConfirm = vm::confirmDeleteLayer
        )
    }
}
