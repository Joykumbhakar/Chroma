package com.chroma.studio.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
                // .mobile-tabs — segmented Layers / Global FX switcher
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.glassBgHover, RoundedCornerShape(10.dp)),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TabButton("Layers", vm.mobileTab == MobileTab.LAYERS, Modifier.weight(1f)) {
                        vm.switchMobileTab(MobileTab.LAYERS)
                    }
                    TabButton("Global FX", vm.mobileTab == MobileTab.GLOBAL_FX, Modifier.weight(1f)) {
                        vm.switchMobileTab(MobileTab.GLOBAL_FX)
                    }
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
private fun TabButton(label: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = LocalChromaColors.current
    val bg by animateColorAsState(if (active) colors.primary else androidx.compose.ui.graphics.Color.Transparent, label = "tabBg")
    Text(
        text = label,
        color = if (active) colors.onPrimary else colors.textMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = modifier
            .padding(3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg, RoundedCornerShape(8.dp))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClick() }
            .padding(vertical = 8.dp)
    )
}
