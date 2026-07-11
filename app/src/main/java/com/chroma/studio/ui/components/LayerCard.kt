package com.chroma.studio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
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
    isActive: Boolean,
    dragHandleModifier: Modifier = Modifier,
    onToggleExpand: () -> Unit,
    onTypeChange: (LayerType) -> Unit,
    onBlendChange: (ChromaBlendMode) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onAngleChange: (Float) -> Unit,
    onStopsChange: (List<com.chroma.studio.model.ColorStop>) -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalChromaColors.current
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(tween(400))
            .clip(shape)
            .background(colors.glassBg, shape)
            .border(1.dp, if (layer.expanded) colors.primary else colors.glassBorder, shape)
            .padding(12.dp)
    ) {
        // ---- Header row (drag handle, name, type icon, expand chevron, delete) ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(indication = null, interactionSource = MutableInteractionSource()) { onToggleExpand() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.DragHandle,
                contentDescription = "Reorder",
                tint = colors.textMuted,
                modifier = dragHandleModifier.size(18.dp)
            )
            Spacer(Modifier.padding(start = 8.dp))
            Column(Modifier.weight(1f)) {
                Text(layer.name, color = colors.textMain, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(layer.type.label, color = colors.textMuted, fontSize = 11.sp)
            }
            // little swatch preview of the layer's own gradient
            val swatchColors = layer.stops.sortedBy { it.position }.map { it.color }
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(if (swatchColors.size >= 2) swatchColors else listOf(colors.primary, colors.primary)))
                    .border(1.dp, colors.glassBorder, CircleShape)
            )
            Spacer(Modifier.padding(start = 8.dp))
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = "Expand",
                tint = colors.textMuted,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(if (layer.expanded) 180f else 0f)
            )
        }

        AnimatedVisibility(
            visible = layer.expanded,
            enter = expandVertically(tween(400)) + fadeIn(tween(400)),
            exit = shrinkVertically(tween(300)) + fadeOut(tween(200))
        ) {
            Column(Modifier.padding(top = 12.dp)) {
                // .type-grid: repeat(3, 1fr)
                FieldLabel("Type")
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(88.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(LayerType.entries.toList()) { type ->
                        TypeButton(label = type.label, active = layer.type == type) { onTypeChange(type) }
                    }
                }

                Spacer(Modifier.padding(top = 12.dp))
                ColorStopEditor(stops = layer.stops, onStopsChange = onStopsChange)

                if (layer.type == LayerType.LINEAR) {
                    Spacer(Modifier.padding(top = 8.dp))
                    FieldLabel("Angle", value = "${layer.angle.toInt()}°")
                    Slider(
                        value = layer.angle,
                        onValueChange = onAngleChange,
                        valueRange = 0f..360f,
                        colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
                    )
                }

                Spacer(Modifier.padding(top = 4.dp))
                FieldLabel("Opacity", value = "${(layer.opacity * 100).toInt()}%")
                Slider(
                    value = layer.opacity,
                    onValueChange = onOpacityChange,
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
                )

                Spacer(Modifier.padding(top = 4.dp))
                FieldLabel("Blend Mode")
                BlendModeSelector(current = layer.blendMode, onChange = onBlendChange)

                Spacer(Modifier.padding(top = 10.dp))
                Text(
                    text = "Delete Layer",
                    color = colors.error,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(indication = null, interactionSource = MutableInteractionSource()) { onDelete() }
                )
            }
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
private fun TypeButton(label: String, active: Boolean, onClick: () -> Unit) {
    val colors = LocalChromaColors.current
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier = Modifier
            .clip(shape)
            .background(if (active) colors.primary else colors.glassBg, shape)
            .border(1.dp, if (active) colors.primary else colors.glassBorder, shape)
            .clickable(indication = null, interactionSource = MutableInteractionSource()) { onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = if (active) colors.onPrimary else colors.textMuted)
    }
}

@Composable
private fun BlendModeSelector(current: ChromaBlendMode, onChange: (ChromaBlendMode) -> Unit) {
    val colors = LocalChromaColors.current
    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        lazyRowItems(ChromaBlendMode.entries.toList()) { mode ->
            val active = mode == current
            val shape = RoundedCornerShape(50)
            Text(
                text = mode.label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (active) colors.onPrimary else colors.textMuted,
                modifier = Modifier
                    .clip(shape)
                    .background(if (active) colors.primary else colors.glassBg, shape)
                    .border(1.dp, if (active) colors.primary else colors.glassBorder, shape)
                    .clickable(indication = null, interactionSource = MutableInteractionSource()) { onChange(mode) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}
