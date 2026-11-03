package com.chroma.studio.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private data class Topic(val label: String, val icon: ImageVector)

private val topics = listOf(
    Topic("Color Grading", Icons.Rounded.ColorLens),
    Topic("Motion Graphics", Icons.Rounded.Animation),
    Topic("Sound Design", Icons.Rounded.GraphicEq),
    Topic("Cinematic Reels", Icons.Rounded.MovieFilter),
    Topic("3D & VFX", Icons.Rounded.ViewInAr),
    Topic("Typography", Icons.Rounded.TextFields),
    Topic("Film Emulation", Icons.Rounded.CameraRoll),
    Topic("Generative Art", Icons.Rounded.AutoAwesome),
    Topic("Studio Lighting", Icons.Rounded.WbIncandescent),
    Topic("Virtual Sets", Icons.Rounded.Landscape)
)

private const val MIN_PICKS = 2
private const val MAX_PICKS = 5

@Composable
fun InterestsScreen(state: OnboardingState, onBack: () -> Unit, onNext: () -> Unit) {
    val count = state.interests.size
    var maxHit by remember { mutableIntStateOf(0) }
    var showMax by remember { mutableStateOf(false) }
    LaunchedEffect(maxHit) {
        if (maxHit > 0) {
            showMax = true
            delay(1200)
            showMax = false
        }
    }

    ObScreen {
        ObNavBar(onBack = onBack, step = 1, totalSteps = 4) {
            ObBadge("Step 1/4", ObColors.NeonGreen)
        }
        ObHeader(
            firstLine = "What are you",
            highlightLine = "creating?",
            subtitle = "Pick $MIN_PICKS–$MAX_PICKS studio domains. These tailor your feed, shaders, and community discover."
        )
        Spacer(Modifier.height(18.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(topics, key = { it.label }) { t ->
                val selected = t.label in state.interests
                ObTopicChip(t.label, t.icon, selected, {
                    when {
                        selected -> state.interests.remove(t.label)
                        count >= MAX_PICKS -> maxHit++
                        else -> state.interests.add(t.label)
                    }
                })
            }
        }
        ObPrimaryButton(
            text = if (count < MIN_PICKS) "Pick ${MIN_PICKS - count} more" else "Next: Studio Profile",
            onClick = onNext,
            enabled = count >= MIN_PICKS,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )
    }
}
