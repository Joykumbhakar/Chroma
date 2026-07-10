package com.chroma.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.ui.components.ChromaAppBar
import com.chroma.studio.ui.components.CanvasPreview
import com.chroma.studio.ui.components.HazeSourceRoot
import com.chroma.studio.ui.components.LayersPanel
import com.chroma.studio.ui.theme.ChromaTheme
import com.chroma.studio.ui.theme.LocalChromaColors
import com.chroma.studio.viewmodel.ChromaViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: ChromaViewModel by viewModels()
            ChromaTheme(darkTheme = vm.isDarkMode) {
                ChromaStudioApp(vm)
            }
        }
    }
}

@Composable
fun ChromaStudioApp(vm: ChromaViewModel) {
    val colors = LocalChromaColors.current

    // .grid-background: full-bleed dotted grid behind everything, radial-masked at the top,
    // acting as the Haze blur *source* for every glass panel above it.
    HazeSourceRoot(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        Column(Modifier.fillMaxSize()) {
            ChromaAppBar(
                isDarkMode = vm.isDarkMode,
                onToggleDarkMode = vm::toggleDarkMode,
                onAddLayer = vm::addLayer,
                onRandomize = vm::randomize
            )

            Row(Modifier.fillMaxWidth().padding(12.dp)) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // mobile shape pills: Card / Circle / Full / Text
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        listOf("rounded" to "Card", "circle" to "Circle", "full" to "Full", "text" to "Text")
                            .forEach { (key, label) -> ShapePill(label, key == vm.canvasShape) { vm.setShape(key) } }
                    }

                    CanvasPreview(
                        layers = vm.layers,
                        shape = vm.canvasShape,
                        borderColor = colors.glassBorder,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            LayersPanel(
                layers = vm.layers,
                activeLayerId = vm.activeLayerId,
                vm = vm,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(12.dp)
            )
        }
    }
}

@Composable
private fun ShapePill(label: String, active: Boolean, onClick: () -> Unit) {
    val colors = LocalChromaColors.current
    val shape = RoundedCornerShape(50)
    Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (active) colors.onPrimary else colors.textMuted,
        modifier = Modifier
            .clip(shape)
            .background(if (active) colors.primary else colors.glassBg, shape)
            .clickable(indication = null, interactionSource = MutableInteractionSource()) { onClick() }
            .padding(horizontal = 16.dp, vertical = 6.dp)
    )
}
