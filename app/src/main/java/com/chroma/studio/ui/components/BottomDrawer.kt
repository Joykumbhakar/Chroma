package com.chroma.studio.ui.components

import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.draw.shadow
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
    val animatedOffsetY by animateDpAsState(
        targetValue = 620.dp - targetHeight, 
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium), 
        label = "drawerOffsetY"
    )
    var dragAccum by remember { mutableStateOf(0f) }

    GlassPanel(
        modifier = modifier
            .offset(y = animatedOffsetY)
            .fillMaxWidth()
            .height(620.dp),
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

@Composable
private fun UiverseTabs(
    activeTab: MobileTab,
    onTabSelected: (MobileTab) -> Unit
) {
    val colors = LocalChromaColors.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    
    // 1:1 interactive Animatable for the indicator
    val indicatorOffsetAnim = remember { androidx.compose.animation.core.Animatable(if (activeTab == MobileTab.LAYERS) 2f else 132f) }
    
    // Animate to target whenever activeTab changes (e.g., from tap, or completed swipe)
    androidx.compose.runtime.LaunchedEffect(activeTab) {
        indicatorOffsetAnim.animateTo(
            targetValue = if (activeTab == MobileTab.LAYERS) 2f else 132f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
        )
    }

    Box(
        modifier = Modifier
            .padding(bottom = 12.dp, top = 4.dp)
            .size(width = 264.dp, height = 32.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.06f))
            .pointerInput(activeTab) {
                var dragAmountTotal = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dragAmountTotal = 0f },
                    onDragEnd = {
                        if (dragAmountTotal > 30f && activeTab == MobileTab.LAYERS) {
                            onTabSelected(MobileTab.GLOBAL_FX)
                        } else if (dragAmountTotal < -30f && activeTab == MobileTab.GLOBAL_FX) {
                            onTabSelected(MobileTab.LAYERS)
                        } else {
                            // Snap back if threshold not met
                            coroutineScope.launch {
                                indicatorOffsetAnim.animateTo(
                                    targetValue = if (activeTab == MobileTab.LAYERS) 2f else 132f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            indicatorOffsetAnim.animateTo(
                                targetValue = if (activeTab == MobileTab.LAYERS) 2f else 132f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
                            )
                        }
                    }
                ) { change, dragAmount ->
                    change.consume()
                    dragAmountTotal += dragAmount
                    coroutineScope.launch {
                        val base = if (activeTab == MobileTab.LAYERS) 2f else 132f
                        indicatorOffsetAnim.snapTo((base + dragAmountTotal).coerceIn(2f, 132f))
                    }
                }
            }
    ) {
        // Indicator
        Box(
            modifier = Modifier
                .offset(x = indicatorOffsetAnim.value.dp, y = 2.dp)
                .size(width = 130.dp, height = 28.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(colors.glassBg)
                .border(
                    width = 2.dp,
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            androidx.compose.ui.graphics.Color(0xFF60A5FA),
                            androidx.compose.ui.graphics.Color(0xFF3B82F6),
                            androidx.compose.ui.graphics.Color(0xFF2563EB)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
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
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            color = colors.textMain.copy(alpha = if (active) 1f else 0.6f)
        )
    }
}
