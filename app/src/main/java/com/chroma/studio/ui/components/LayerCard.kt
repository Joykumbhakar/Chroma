package com.chroma.studio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import com.chroma.studio.ui.components.glossyBorder
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Circle
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.GripVertical
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.Grid3x3
import com.composables.icons.lucide.CircleDashed
import com.composables.icons.lucide.ArrowUpRight
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.Droplet
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.model.ChromaBlendMode
import com.chroma.studio.model.GradientLayer
import com.chroma.studio.model.LayerType
import com.chroma.studio.ui.theme.LocalChromaColors

/**
 * .layer-card { background: var(--glass-bg); animation: slideInLayer .4s; }
 * .layer-card.expanded { border-color: var(--primary); box-shadow: 0 4px 20px rgba(...); transform: scale(1.01); }
 */
@Composable
fun LayerCard(
    layer: GradientLayer,
    dragHandleModifier: Modifier = Modifier,
    isDragging: Boolean = false,
    onToggleExpand: () -> Unit,
    onTypeChange: (LayerType) -> Unit,
    onBlendChange: (ChromaBlendMode) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onAngleChange: (Float) -> Unit,
    onStopsChange: (List<com.chroma.studio.model.ColorStop>) -> Unit,
    onToggleVisibility: () -> Unit,
    onCenterXChange: (Float) -> Unit,
    onCenterYChange: (Float) -> Unit,
    onDelete: () -> Unit,
    onRepeatPatternChange: (Boolean) -> Unit = {},
    onWidthChange: (Float) -> Unit = {},
    onHeightChange: (Float) -> Unit = {},
    onFeatherChange: (Float) -> Unit = {},
    onWaveSpeedChange: (Float) -> Unit = {},
    onComplexityChange: (Float) -> Unit = {},
    onBrightnessChange: (Float) -> Unit = {},
    onColumnsChange: (Int) -> Unit = {},
    onRowsChange: (Int) -> Unit = {},
    onBaseBackgroundChange: (Boolean) -> Unit = {},
    onBlobBgColorChange: (Color) -> Unit = {},
    onRegenerateMesh: () -> Unit = {},
    onAddBlob: () -> Unit = {},
    onRemoveBlob: (Int) -> Unit = {},
    onSetActiveBlob: (Int) -> Unit = {},
    onUpdateBlobParam: (Int, String, Float) -> Unit = { _, _, _ -> }
) {
    val colors = LocalChromaColors.current
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = dragHandleModifier
            .fillMaxWidth()
            .animateContentSize(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
            .clip(shape)
            .background(colors.glassBg, shape)
            .border(
                width = if (isDragging) 2.dp else 1.dp,
                brush = if (isDragging) {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF60A5FA),
                            Color(0xFF3B82F6),
                            Color(0xFF2563EB)
                        )
                    )
                } else if (layer.expanded) {
                    SolidColor(colors.primary)
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            colors.glassBorder.copy(alpha = (colors.glassBorder.alpha * 3f).coerceAtMost(1f)),
                            colors.glassBorder.copy(alpha = (colors.glassBorder.alpha * 0.2f).coerceAtMost(1f))
                        )
                    )
                },
                shape = shape
            )
            .padding(12.dp)
    ) {
        // ---- Header row (drag handle, name, type icon, expand chevron, delete) ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onToggleExpand() }
                    )
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Lucide.GripVertical,
                contentDescription = "Reorder",
                tint = colors.textMuted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.padding(start = 8.dp))
            Icon(
                imageVector = if (layer.visible) Lucide.Eye else Lucide.EyeOff,
                contentDescription = "Toggle Visibility",
                tint = colors.textMuted,
                modifier = Modifier
                    .size(18.dp)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onToggleVisibility() }
            )
            Spacer(Modifier.padding(start = 8.dp))
            val swatchColors = layer.stops.sortedBy { it.position }.map { it.color }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(if (swatchColors.size >= 2) swatchColors else listOf(colors.primary, colors.primary)))
            )
            Spacer(Modifier.padding(start = 8.dp))
            Column(Modifier.weight(1f)) {
                Text(layer.name, color = colors.textMain, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
            Icon(
                imageVector = Lucide.ChevronDown,
                contentDescription = "Expand",
                tint = colors.textMuted,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(indication = null, interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() }) { onToggleExpand() }
                    .padding(3.dp)
                    .rotate(if (layer.expanded) 180f else 0f)
            )
            Spacer(Modifier.padding(start = 8.dp))
            Icon(
                imageVector = Lucide.Trash2,
                contentDescription = "Delete",
                tint = colors.error,
                modifier = Modifier
                    .size(16.dp)
                    .clickable(indication = null, interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() }) { onDelete() }
            )
        }

        AnimatedVisibility(
            visible = layer.expanded,
            enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(spring(stiffness = Spring.StiffnessMediumLow)),
            exit = shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(spring(stiffness = Spring.StiffnessHigh))
        ) {
            Column(Modifier.padding(top = 12.dp)) {
                // .type-grid: repeat(3, 1fr)
                FieldLabel("Engine Selection")
                Spacer(Modifier.padding(top = 8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(180.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(LayerType.entries.toList()) { type ->
                        TypeButton(
                            label = type.label,
                            icon = when (type) {
                                LayerType.LINEAR -> Lucide.ArrowUpRight
                                LayerType.RADIAL -> Lucide.Circle
                                LayerType.CONIC -> Lucide.CircleDashed
                                LayerType.MESH -> Lucide.Grid3x3
                                LayerType.BLOB -> Lucide.Droplet
                                LayerType.AURORA -> Lucide.Sparkles
                                LayerType.LIQUID -> Lucide.Droplet
                            },
                            active = layer.type == type
                        ) { onTypeChange(type) }
                    }
                }


                Spacer(Modifier.padding(top = 12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        FieldLabel("Blend Mode")
                        BlendModeSelector(current = layer.blendMode, onChange = onBlendChange)
                    }
                    Column(Modifier.weight(1f)) {
                        FieldLabel("Opacity", value = "${(layer.opacity * 100).toInt()}%")
                        ChromaSlider(
                            value = layer.opacity,
                            onValueChange = onOpacityChange,
                            valueRange = 0f..1f
                        )
                    }
                }

                when (layer.type) {
                    LayerType.LINEAR -> {
                        Spacer(Modifier.padding(top = 8.dp))
                        FieldLabel("Angle", value = "${layer.angle.toInt()}°")
                        ChromaSlider(
                            value = layer.angle,
                            onValueChange = onAngleChange,
                            valueRange = 0f..360f
                        )
                        Spacer(Modifier.padding(top = 8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("REPEAT PATTERN", color = colors.textMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            SmallSwitch(
                                checked = layer.repeatPattern,
                                onCheckedChange = onRepeatPatternChange
                            )
                        }
                    }
                    LayerType.CONIC -> {
                        Spacer(Modifier.padding(top = 8.dp))
                        FieldLabel("Angle", value = "${layer.angle.toInt()}°")
                        ChromaSlider(
                            value = layer.angle,
                            onValueChange = onAngleChange,
                            valueRange = 0f..360f
                        )
                        Spacer(Modifier.padding(top = 8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f)) {
                                FieldLabel("Center X (%)")
                                CompactNumberField(value = layer.centerX.toInt(), onValueChange = onCenterXChange)
                            }
                            Column(Modifier.weight(1f)) {
                                FieldLabel("Center Y (%)")
                                CompactNumberField(value = layer.centerY.toInt(), onValueChange = onCenterYChange)
                            }
                        }
                    }
                    LayerType.RADIAL -> {
                        Spacer(Modifier.padding(top = 8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f)) {
                                FieldLabel("Center X (%)")
                                CompactNumberField(value = layer.centerX.toInt(), onValueChange = onCenterXChange)
                            }
                            Column(Modifier.weight(1f)) {
                                FieldLabel("Center Y (%)")
                                CompactNumberField(value = layer.centerY.toInt(), onValueChange = onCenterYChange)
                            }
                        }
                        Spacer(Modifier.padding(top = 8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f)) {
                                FieldLabel("Width X", value = "${layer.width.toInt()}%")
                                ChromaSlider(value = layer.width, onValueChange = onWidthChange, valueRange = 10f..150f)
                            }
                            Column(Modifier.weight(1f)) {
                                FieldLabel("Height Y", value = "${layer.height.toInt()}%")
                                ChromaSlider(value = layer.height, onValueChange = onHeightChange, valueRange = 10f..150f)
                            }
                        }
                        Spacer(Modifier.padding(top = 8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("REPEAT PATTERN", color = colors.textMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            SmallSwitch(
                                checked = layer.repeatPattern,
                                onCheckedChange = onRepeatPatternChange
                            )
                        }
                    }
                    LayerType.MESH -> {
                        Spacer(Modifier.padding(top = 8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f)) {
                                FieldLabel("Columns")
                                CompactNumberField(value = layer.columns, onValueChange = { onColumnsChange(it.toInt()) })
                            }
                            Column(Modifier.weight(1f)) {
                                FieldLabel("Rows")
                                CompactNumberField(value = layer.rows, onValueChange = { onRowsChange(it.toInt()) })
                            }
                        }
                    }
                    LayerType.AURORA -> {
                        Spacer(Modifier.padding(top = 8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f)) {
                                FieldLabel("Wave Speed", value = "${layer.waveSpeed.toInt()}%")
                                ChromaSlider(value = layer.waveSpeed, onValueChange = onWaveSpeedChange, valueRange = 0f..100f)
                            }
                            Column(Modifier.weight(1f)) {
                                FieldLabel("Complexity", value = "${layer.complexity.toInt()}%")
                                ChromaSlider(value = layer.complexity, onValueChange = onComplexityChange, valueRange = 10f..100f)
                            }
                        }
                        Spacer(Modifier.padding(top = 8.dp))
                        FieldLabel("Brightness", value = "${layer.brightness.toInt()}%")
                        ChromaSlider(
                            value = layer.brightness,
                            onValueChange = onBrightnessChange,
                            valueRange = 0f..100f
                        )
                    }
                    LayerType.BLOB, LayerType.LIQUID -> {
                        Spacer(Modifier.padding(top = 8.dp))
                        // Info banner matching HTML "Preview Canvas active: Drag handles..."
                        Row(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.primary.copy(alpha = 0.1f))
                                .border(1.dp, colors.primary.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Lucide.ArrowUpRight, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.primary)
                            Spacer(Modifier.padding(start = 8.dp))
                            Text("Preview Canvas active: Drag handles to move elements.", color = colors.primary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }

                        // Per-active-blob sliders (matching HTML width/height/feather)
                        val activeIdx = layer.activeBlobIdx.coerceIn(0, layer.blobs.size - 1)
                        val activeBlob = layer.blobs.getOrNull(activeIdx)
                        if (activeBlob != null) {
                            Spacer(Modifier.padding(top = 8.dp))
                            // Width + Height
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(Modifier.weight(1f)) {
                                    FieldLabel("Width", value = "${activeBlob.width.toInt()}%")
                                    ChromaSlider(
                                        value = activeBlob.width,
                                        onValueChange = { onUpdateBlobParam(activeIdx, "width", it) },
                                        valueRange = 10f..200f
                                    )
                                }
                                Column(Modifier.weight(1f)) {
                                    FieldLabel("Height", value = "${activeBlob.height.toInt()}%")
                                    ChromaSlider(
                                        value = activeBlob.height,
                                        onValueChange = { onUpdateBlobParam(activeIdx, "height", it) },
                                        valueRange = 10f..200f
                                    )
                                }
                            }
                            Spacer(Modifier.padding(top = 8.dp))
                            // Feather
                            FieldLabel("Feather", value = "${activeBlob.feather.toInt()}%")
                            ChromaSlider(
                                value = activeBlob.feather,
                                onValueChange = { onUpdateBlobParam(activeIdx, "feather", it) },
                                valueRange = 0f..100f
                            )
                            Spacer(Modifier.padding(top = 8.dp))
                            // Rotation + Opacity
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(Modifier.weight(1f)) {
                                    FieldLabel("Rotation", value = "${activeBlob.rotation.toInt()}°")
                                    ChromaSlider(
                                        value = activeBlob.rotation,
                                        onValueChange = { onUpdateBlobParam(activeIdx, "rotation", it) },
                                        valueRange = 0f..360f
                                    )
                                }
                                Column(Modifier.weight(1f)) {
                                    FieldLabel("Opacity", value = "${(activeBlob.opacity * 100).toInt()}%")
                                    ChromaSlider(
                                        value = activeBlob.opacity,
                                        onValueChange = { onUpdateBlobParam(activeIdx, "opacity", it) },
                                        valueRange = 0f..1f
                                    )
                                }
                            }
                        }
                    }

                }
                
                if (layer.type == LayerType.BLOB || layer.type == LayerType.LIQUID) {
                    Spacer(Modifier.padding(top = 12.dp))
                    ElementsAndColorsEditor(
                        layer = layer,
                        onStopsChange = onStopsChange,
                        onBaseBackgroundChange = onBaseBackgroundChange,
                        onBlobBgColorChange = onBlobBgColorChange,
                        onAddBlob = onAddBlob,
                        onRemoveBlob = onRemoveBlob,
                        onSetActiveBlob = onSetActiveBlob
                    )
                } else {
                    Spacer(Modifier.padding(top = 12.dp))
                    ColorStopEditor(
                        stops = layer.stops,
                        onStopsChange = onStopsChange,
                        trailingContent = if (layer.type == LayerType.MESH) {
                            {
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.Transparent)
                                        .border(1.dp, colors.glassBorder, RoundedCornerShape(4.dp))
                                        .clickable { onRegenerateMesh() }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Regenerate Mesh", color = colors.textMain, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun ElementsAndColorsEditor(
    layer: GradientLayer,
    onStopsChange: (List<com.chroma.studio.model.ColorStop>) -> Unit,
    onBaseBackgroundChange: (Boolean) -> Unit,
    onBlobBgColorChange: (Color) -> Unit,
    onAddBlob: () -> Unit,
    onRemoveBlob: (Int) -> Unit,
    onSetActiveBlob: (Int) -> Unit
) {
    val colors = LocalChromaColors.current
    val activeIdx = layer.activeBlobIdx.coerceIn(0, layer.blobs.size - 1)
    val activeStop = layer.stops.getOrNull(activeIdx)
    Column(Modifier.fillMaxWidth()) {
        FieldLabel("Elements & Colors")
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            layer.blobs.forEachIndexed { idx, blob ->
                val stop = layer.stops.getOrNull(idx)
                val isActive = idx == layer.activeBlobIdx
                val itemBg = if (isActive) colors.primary.copy(alpha = 0.12f) else colors.glassBg
                val itemBorder = if (isActive) colors.primary.copy(alpha = 0.4f) else colors.glassBorder
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(itemBg)
                        .border(1.dp, itemBorder, RoundedCornerShape(8.dp))
                        .clickable { onSetActiveBlob(idx) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(stop?.color ?: Color.White)
                                .border(1.dp, colors.glassBorder, CircleShape)
                        )
                        Spacer(Modifier.padding(start = 10.dp))
                        Text(
                            text = stop?.color?.let { c -> String.format("#%06X", 0xFFFFFF and c.toArgb()) } ?: "—",
                            color = colors.textMain,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                    // Remove button (only if more than 1 blob)
                    if (layer.blobs.size > 1) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .clickable { onRemoveBlob(idx) }
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Lucide.Trash2,
                                contentDescription = "Remove",
                                modifier = Modifier.size(12.dp),
                                tint = colors.textMuted
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.padding(top = 8.dp))
        // "+ Add Element" button matching HTML
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, colors.glassBorder, RoundedCornerShape(8.dp))
                .clickable { onAddBlob() }
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Lucide.Plus, contentDescription = null, modifier = Modifier.size(14.dp), tint = colors.textMain)
            Spacer(Modifier.padding(start = 6.dp))
            Text("Add Element", color = colors.textMain, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.padding(top = 12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, colors.glassBorder, RoundedCornerShape(12.dp))
                .background(colors.glassBg)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("BASE BACKGROUND", color = colors.textMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                SmallSwitch(
                    checked = layer.hasBaseBackground,
                    onCheckedChange = onBaseBackgroundChange
                )
            }

            var showBgColorPicker by remember { mutableStateOf(false) }
            Spacer(Modifier.padding(top = 12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(layer.blobBgColor.copy(alpha = 1f))
                        .border(1.dp, colors.glassBorder, RoundedCornerShape(6.dp))
                        .clickable { showBgColorPicker = !showBgColorPicker }
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    ChromaSlider(
                        value = layer.blobBgColor.alpha,
                        onValueChange = { a -> onBlobBgColorChange(layer.blobBgColor.copy(alpha = a)) },
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.padding(start = 8.dp))
                    Text(
                        "${(layer.blobBgColor.alpha * 100).toInt()}%",
                        color = colors.textMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.defaultMinSize(minWidth = 28.dp)
                    )
                }
            }

            if (showBgColorPicker) {
                Spacer(Modifier.padding(top = 12.dp))
                InlineColorPicker(
                    initialColor = layer.blobBgColor,
                    onColorChange = onBlobBgColorChange
                )
            }
        }

        // ACTIVE ELEMENT COLOR — color picker for the selected blob (matches HTML's colorPicker.renderPicker)
        if (activeStop != null) {
            Spacer(Modifier.padding(top = 16.dp))
            Text(
                "ACTIVE ELEMENT COLOR",
                color = colors.textMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.padding(top = 8.dp))
            InlineColorPicker(
                initialColor = activeStop.color,
                onColorChange = { newColor ->
                    onStopsChange(layer.stops.mapIndexed { i, s ->
                        if (i == activeIdx) s.copy(color = newColor) else s
                    })
                }
            )
        }
    }
}

@Composable
private fun FieldLabel(text: String, value: String? = null) {
    val colors = LocalChromaColors.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text.uppercase(), color = colors.textMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        if (value != null) Text(value, color = colors.textMain, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TypeButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, active: Boolean, onClick: () -> Unit) {
    val colors = LocalChromaColors.current
    val shape = RoundedCornerShape(12.dp)
    
    val bgColor = colors.glassBg
    val contentColor = if (active) colors.primary else colors.textMain

    Column(
        modifier = Modifier
            .clip(shape)
            .background(bgColor, shape)
            .border(
                width = if (active) 2.dp else 1.dp,
                brush = if (active) {
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            androidx.compose.ui.graphics.Color(0xFF60A5FA),
                            androidx.compose.ui.graphics.Color(0xFF3B82F6),
                            androidx.compose.ui.graphics.Color(0xFF2563EB)
                        )
                    )
                } else {
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            colors.glassBorder.copy(alpha = (colors.glassBorder.alpha * 3f).coerceAtMost(1f)),
                            colors.glassBorder.copy(alpha = (colors.glassBorder.alpha * 0.2f).coerceAtMost(1f))
                        )
                    )
                },
                shape = shape
            )
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClick() }
            .padding(vertical = 14.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.padding(top = 8.dp))
        Text(label, fontSize = 11.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium, color = contentColor)
    }
}

@Composable
private fun BlendModeSelector(current: ChromaBlendMode, onChange: (ChromaBlendMode) -> Unit) {
    val colors = LocalChromaColors.current
    var expanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    androidx.compose.foundation.layout.Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.glassBg)
                .border(1.dp, colors.glassBorder, RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(current.label, fontSize = 12.sp, color = colors.textMain)
            Icon(Lucide.ChevronDown, null, tint = colors.textMuted, modifier = Modifier.size(16.dp))
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(colors.glassBg)
        ) {
            ChromaBlendMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.label, color = colors.textMain, fontSize = 12.sp) },
                    onClick = {
                        onChange(mode)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ChromaSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    val colors = LocalChromaColors.current
    val trackColor = if (colors.isDark) androidx.compose.ui.graphics.Color(0x33FFFFFF) else androidx.compose.ui.graphics.Color(0x26000000)

    // mutableFloatStateOf: primitive-specialized state, avoids boxing on every drag frame
    var dragValue by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Float?>(null) }
    val displayValue = dragValue ?: value
    val currentOnValueChange by androidx.compose.runtime.rememberUpdatedState(onValueChange)

    // Compute fraction once, only when displayValue changes
    val pct by remember(displayValue, valueRange) {
        derivedStateOf {
            if (valueRange.endInclusive > valueRange.start)
                ((displayValue - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
            else 0f
        }
    }

    var sliderWidthPx by androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .onSizeChanged { sliderWidthPx = it.width.toFloat() }
            .pointerInput(Unit) {
                detectTapGestures { tap ->
                    val p = (tap.x / size.width).coerceIn(0f, 1f)
                    currentOnValueChange(valueRange.start + p * (valueRange.endInclusive - valueRange.start))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragValue = displayValue },
                    onDragEnd = { dragValue = null },
                    onDragCancel = { dragValue = null }
                ) { change, _ ->
                    change.consume()
                    val p = (change.position.x / size.width).coerceIn(0f, 1f)
                    val v = valueRange.start + p * (valueRange.endInclusive - valueRange.start)
                    dragValue = v
                    currentOnValueChange(v)
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(trackColor)
        )

        // Thumb — graphicsLayer only triggers transform (no layout/draw recomposition)
        Box(
            modifier = Modifier
                .size(14.dp)
                .graphicsLayer { translationX = (sliderWidthPx - 14.dp.toPx()) * pct }
                .clip(CircleShape)
                .background(colors.primary)
                .border(2.dp, androidx.compose.ui.graphics.Color.White, CircleShape)
        )
    }
}

@Composable
fun CompactNumberField(
    value: Int,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalChromaColors.current
    BasicTextField(
        value = value.toString(),
        onValueChange = { it.toFloatOrNull()?.let { v -> onValueChange(v) } },
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.glassBg, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 12.sp,
            color = colors.textMain,
            fontWeight = FontWeight.Medium
        ),
        singleLine = true,
        cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.primary)
    )
}
