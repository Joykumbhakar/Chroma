package com.chroma.studio.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.model.GradientLayer
import com.chroma.studio.ui.theme.LocalChromaColors
import com.chroma.studio.viewmodel.ChromaViewModel

/**
 * .right-panel + #layers-stack-list { padding: 16px; display:flex; flex-direction:column; gap:16px; }
 * Drag handle (.layer-drag-handle, cursor:grab) reorders via long-press-drag, matching the
 * JS drag-and-drop list (.layer-card.is-dragging).
 */
@Composable
fun LayersPanel(
    layers: List<GradientLayer>,
    activeLayerId: String,
    vm: ChromaViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalChromaColors.current
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val rowHeightPx = remember { mutableStateOf(1) }

    GlassPanel(modifier = modifier.fillMaxSize(), cornerRadius = 16) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "LAYERS STACK",
                    color = colors.textMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            items(layers, key = { it.id }) { layer ->

                LayerCard(
                    layer = layer,
                    isActive = layer.id == activeLayerId,
                    dragHandleModifier = Modifier
                        .onSizeChanged { rowHeightPx.value = it.height.coerceAtLeast(1) }
                        .pointerInput(layer.id, layers.size) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { draggingId = layer.id; dragOffsetY = 0f },
                                onDrag = { change, drag ->
                                    change.consume()
                                    dragOffsetY += drag.y
                                    val steps = (dragOffsetY / rowHeightPx.value.toFloat()).toInt()
                                    if (steps != 0) {
                                        val from = layers.indexOfFirst { it.id == layer.id }
                                        val to = (from + steps).coerceIn(0, layers.size - 1)
                                        if (to != from) {
                                            vm.moveLayer(from, to)
                                            dragOffsetY -= steps * rowHeightPx.value.toFloat()
                                        }
                                    }
                                },
                                onDragEnd = { draggingId = null; dragOffsetY = 0f },
                                onDragCancel = { draggingId = null; dragOffsetY = 0f }
                            )
                        },
                    onToggleExpand = { vm.selectLayer(layer.id); vm.toggleExpanded(layer.id) },
                    onTypeChange = { type -> vm.updateLayer(layer.id, recordHistory = true) { it.copy(type = type) } },
                    onBlendChange = { mode -> vm.updateLayer(layer.id, recordHistory = true) { it.copy(blendMode = mode) } },
                    onOpacityChange = { v -> vm.updateLayer(layer.id) { it.copy(opacity = v) } },
                    onAngleChange = { v -> vm.updateLayer(layer.id) { it.copy(angle = v) } },
                    onStopsChange = { newStops -> vm.updateLayer(layer.id, recordHistory = true) { it.copy(stops = newStops) } },
                    onDelete = { vm.requestDeleteLayer(layer.id) }
                )
            }
        }
    }

    val pendingId = vm.pendingDeleteLayerId
    if (pendingId != null) {
        val target = layers.find { it.id == pendingId }
        DeleteLayerModal(
            layerName = target?.name ?: "Layer",
            onCancel = vm::cancelDeleteLayer,
            onConfirm = vm::confirmDeleteLayer
        )
    }
}
