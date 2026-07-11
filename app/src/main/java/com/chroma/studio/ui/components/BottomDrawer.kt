package com.chroma.studio.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.ui.theme.LocalChromaColors
import com.chroma.studio.viewmodel.ChromaViewModel
import com.chroma.studio.viewmodel.DrawerLevel
import com.chroma.studio.viewmodel.MobileTab

/**
 * Mobile bottom drawer (#mobile-drawer): draggable handle (.drawer-handle) that snaps
 * between COLLAPSED / MID / FULL heights, with a Layers / Global FX tab switcher
 * (#m-tab-layers / #m-tab-globalfx) at the top — matching the mobile layout in index.html.
 */
@Composable
fun BottomDrawer(vm: ChromaViewModel, modifier: Modifier = Modifier) {
    val colors = LocalChromaColors.current
    val targetHeight = when (vm.drawerLevel) {
        DrawerLevel.COLLAPSED -> 64.dp
        DrawerLevel.MID -> 340.dp
        DrawerLevel.FULL -> 620.dp
    }
    val animatedHeight by animateDpAsState(targetHeight, tween(300), label = "drawerHeight")
    var dragAccum by remember { mutableStateOf(0f) }

    GlassPanel(
        modifier = modifier
            .fillMaxWidth()
            .height(animatedHeight),
        cornerRadius = 24
    ) {
        Column(Modifier.fillMaxWidth()) {
            // .drawer-handle — 36x4 grab bar, drag up/down cycles COLLAPSED/MID/FULL
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (dragAccum < -40f) {
                                    vm.updateDrawerLevel(
                                        when (vm.drawerLevel) {
                                            DrawerLevel.COLLAPSED -> DrawerLevel.MID
                                            DrawerLevel.MID -> DrawerLevel.FULL
                                            DrawerLevel.FULL -> DrawerLevel.FULL
                                        }
                                    )
                                } else if (dragAccum > 40f) {
                                    vm.updateDrawerLevel(
                                        when (vm.drawerLevel) {
                                            DrawerLevel.FULL -> DrawerLevel.MID
                                            DrawerLevel.MID -> DrawerLevel.COLLAPSED
                                            DrawerLevel.COLLAPSED -> DrawerLevel.COLLAPSED
                                        }
                                    )
                                }
                                dragAccum = 0f
                            }
                        ) { change, dragY ->
                            change.consume()
                            dragAccum += dragY
                        }
                    }
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                        vm.cycleDrawerLevel()
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colors.textMuted.copy(alpha = 0.4f))
                )
            }

            if (vm.drawerLevel != DrawerLevel.COLLAPSED) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    UiverseTabs(
                        activeTab = vm.mobileTab,
                        onTabSelected = { vm.switchMobileTab(it) }
                    )
                }

                when (vm.mobileTab) {
                    MobileTab.LAYERS -> LayersPanel(
                        layers = vm.layers,
                        activeLayerId = vm.activeLayerId,
                        vm = vm,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                    MobileTab.GLOBAL_FX -> GlobalFxPanel(vm = vm, modifier = Modifier.fillMaxWidth().weight(1f))
                }
            }
        }
    }
}

@Composable
private fun UiverseTabs(
    activeTab: MobileTab,
    onTabSelected: (MobileTab) -> Unit
) {
    val colors = LocalChromaColors.current
    val indicatorOffset by animateDpAsState(
        targetValue = if (activeTab == MobileTab.LAYERS) 2.dp else 132.dp,
        animationSpec = tween(200),
        label = "tabIndicator"
    )

    Box(
        modifier = Modifier
            .padding(bottom = 12.dp, top = 4.dp)
            .size(width = 264.dp, height = 32.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.06f))
    ) {
        // Indicator
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset, y = 2.dp)
                .size(width = 130.dp, height = 28.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(colors.glassBg)
                .border(0.5.dp, androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.04f), RoundedCornerShape(24.dp))
        )
        
        // Labels
        Row(Modifier.fillMaxSize()) {
            TabLabel(
                text = "Layers Stack",
                active = activeTab == MobileTab.LAYERS,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onTabSelected(MobileTab.LAYERS) }
            )
            TabLabel(
                text = "Global FX",
                active = activeTab == MobileTab.GLOBAL_FX,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onTabSelected(MobileTab.GLOBAL_FX) }
            )
        }
    }
}

@Composable
private fun TabLabel(text: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = LocalChromaColors.current
    Box(
        modifier = modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textMain.copy(alpha = if (active) 1f else 0.6f)
        )
    }
}
