package com.chroma.studio.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.Button(
                            onClick = { vm.addLayer() },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add Layer",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.padding(start = 4.dp))
                            Text("Add Layer", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        androidx.compose.material3.IconButton(
                            onClick = { vm.randomize() },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                .background(colors.glassBg)
                                .border(1.dp, colors.glassBorder, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Filled.Shuffle,
                                contentDescription = "Randomize",
                                modifier = Modifier.size(16.dp),
                                tint = colors.textMain
                            )
                        }
                    }
                }
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
                    onTypeChange = { t -> vm.updateLayer(layer.id, recordHistory = true) { it.copy(type = t) } },
                    onBlendChange = { b -> vm.updateLayer(layer.id, recordHistory = true) { it.copy(blendMode = b) } },
                    onOpacityChange = { o -> vm.updateLayer(layer.id) { it.copy(opacity = o) } },
                    onAngleChange = { a -> vm.updateLayer(layer.id) { it.copy(angle = a) } },
                    onStopsChange = { s -> vm.updateLayer(layer.id, recordHistory = true) { it.copy(stops = s) } },
                    onToggleVisibility = { vm.updateLayer(layer.id) { it.copy(visible = !it.visible) } },
                    onCenterXChange = { cx -> vm.updateLayer(layer.id) { it.copy(centerX = cx) } },
                    onCenterYChange = { cy -> vm.updateLayer(layer.id) { it.copy(centerY = cy) } },
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
