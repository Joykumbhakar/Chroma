package com.chroma.studio.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.model.GradientLayer
import com.chroma.studio.ui.theme.LocalChromaColors
import com.chroma.studio.viewmodel.ChromaViewModel

/**
 * .right-panel + #layers-stack-list { padding: 16px; display:flex; flex-direction:column; gap:16px; }
 */
@Composable
fun LayersPanel(
    layers: List<GradientLayer>,
    activeLayerId: String,
    vm: ChromaViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalChromaColors.current

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
                    onToggleExpand = { vm.selectLayer(layer.id); vm.toggleExpanded(layer.id) },
                    onTypeChange = { type -> vm.updateLayer(layer.id) { it.type = type } },
                    onBlendChange = { mode -> vm.updateLayer(layer.id) { it.blendMode = mode } },
                    onOpacityChange = { v -> vm.updateLayer(layer.id) { it.opacity = v } },
                    onAngleChange = { v -> vm.updateLayer(layer.id) { it.angle = v } },
                    onDelete = { vm.deleteLayer(layer.id) }
                )
            }
        }
    }
}
