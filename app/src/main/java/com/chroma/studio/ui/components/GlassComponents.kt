package com.chroma.studio.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.chroma.studio.ui.theme.LocalOscellionColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import com.chroma.studio.ui.theme.OscellionPalette

fun Modifier.glossyBorder(
    shape: Shape,
    colors: OscellionPalette,
    baseColor: Color = colors.glassBorder,
    width: androidx.compose.ui.unit.Dp = 0.5.dp
): Modifier = this.border(
    width = width,
    color = baseColor,
    shape = shape
)

val LocalHazeState = androidx.compose.runtime.compositionLocalOf { HazeState() }

@Composable
fun HazeSourceRoot(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val hazeState = LocalHazeState.current
    Box(modifier = modifier.haze(hazeState)) { content() }
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 16, // --radius-md
    backgroundColor: androidx.compose.ui.graphics.Color? = null,
    content: @Composable () -> Unit
) {
    val colors = LocalOscellionColors.current
    val hazeState = LocalHazeState.current
    val shape = RoundedCornerShape(cornerRadius.dp)

    Box(
        modifier = modifier
            .shadow(elevation = 8.dp, shape = shape, ambientColor = Color(0x0D1F2687), spotColor = Color(0x0D1F2687))
            .clip(shape)
            .hazeChild(state = hazeState, shape = shape) // backdrop-filter: blur(16px)
            .background(backgroundColor ?: colors.glassBg, shape) // fallback tint if blur unsupported below API 31
            .glossyBorder(shape, colors)
    ) {
        content()
    }
}

@Composable
fun glassCircleShape() = RoundedCornerShape(50)

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(50),
    colors: androidx.compose.material3.ButtonColors = androidx.compose.material3.ButtonDefaults.buttonColors(
        containerColor = LocalOscellionColors.current.primary.copy(alpha = 0.8f)
    ),
    borderColor: Color = LocalOscellionColors.current.primary.copy(alpha = 0.5f),
    elevation: androidx.compose.material3.ButtonElevation? = androidx.compose.material3.ButtonDefaults.buttonElevation(),
    border: androidx.compose.foundation.BorderStroke? = null,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.material3.ButtonDefaults.ContentPadding,
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    val hazeState = LocalHazeState.current
    
    androidx.compose.material3.Button(
        onClick = onClick,
        modifier = modifier.hazeChild(state = hazeState, shape = shape),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border ?: androidx.compose.foundation.BorderStroke(0.5.dp, borderColor),
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun GlassDialog(
    onDismissRequest: () -> Unit,
    properties: androidx.compose.ui.window.DialogProperties = androidx.compose.ui.window.DialogProperties(
        usePlatformDefaultWidth = false,
        decorFitsSystemWindows = false
    ),
    cornerRadius: Int = 28,
    content: @Composable () -> Unit
) {
    var isVisible by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        isVisible = true
    }
    val animatedScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.90f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ),
        label = "dialog_scale"
    )
    val animatedAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 180),
        label = "dialog_alpha"
    )

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        val colors = LocalOscellionColors.current
        val hazeState = LocalHazeState.current
        val shape = RoundedCornerShape(cornerRadius.dp)

        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.52f * animatedAlpha))
                .clickable(
                    interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                ),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .graphicsLayer {
                        scaleX = animatedScale
                        scaleY = animatedScale
                        alpha = animatedAlpha
                    }
                    .clickable(
                        interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .shadow(
                        elevation = 24.dp,
                        shape = shape,
                        ambientColor = Color.Black.copy(alpha = 0.15f),
                        spotColor = Color.Black.copy(alpha = 0.25f)
                    )
                    .clip(shape)
                    .hazeChild(state = hazeState, shape = shape)
                    .background(colors.glassBg.copy(alpha = 0.98f), shape)
                    .border(
                        width = 0.5.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = if (colors.isDark) 0.22f else 0.75f),
                                Color.White.copy(alpha = if (colors.isDark) 0.05f else 0.25f)
                            )
                        ),
                        shape = shape
                    )
            ) {
                content()
            }
        }
    }
}

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun GlassBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: androidx.compose.material3.SheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        containerColor = Color.White,
        contentColor = Color(0xFF1C1C1E),
        tonalElevation = 0.dp,
        dragHandle = {
            androidx.compose.material3.BottomSheetDefaults.DragHandle(
                color = Color(0xFFD1D5DB)
            )
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White),
            content = content
        )
    }
}

@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: androidx.compose.material3.IconButtonColors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(
        containerColor = LocalOscellionColors.current.glassBg
    ),
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    val chromaColors = LocalOscellionColors.current
    val hazeState = LocalHazeState.current
    val shape = RoundedCornerShape(50)
    
    androidx.compose.material3.IconButton(
        onClick = onClick,
        modifier = modifier
            .hazeChild(state = hazeState, shape = shape)
            .glossyBorder(shape, chromaColors),
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun GlassTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(50),
    colors: androidx.compose.material3.ButtonColors = androidx.compose.material3.ButtonDefaults.textButtonColors(),
    elevation: androidx.compose.material3.ButtonElevation? = null,
    border: androidx.compose.foundation.BorderStroke? = null,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.material3.ButtonDefaults.TextButtonContentPadding,
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    val chromaColors = LocalOscellionColors.current
    val hazeState = LocalHazeState.current
    
    androidx.compose.material3.TextButton(
        onClick = onClick,
        modifier = modifier
            .hazeChild(state = hazeState, shape = shape)
            .glossyBorder(shape, chromaColors),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun GlassOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(50),
    colors: androidx.compose.material3.ButtonColors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(),
    elevation: androidx.compose.material3.ButtonElevation? = null,
    border: androidx.compose.foundation.BorderStroke? = androidx.compose.material3.ButtonDefaults.outlinedButtonBorder,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.material3.ButtonDefaults.ContentPadding,
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    val chromaColors = LocalOscellionColors.current
    val hazeState = LocalHazeState.current
    
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .hazeChild(state = hazeState, shape = shape)
            .glossyBorder(shape, chromaColors),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun GlassFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = androidx.compose.foundation.shape.CircleShape,
    containerColor: androidx.compose.ui.graphics.Color = LocalOscellionColors.current.primary.copy(alpha = 0.85f),
    contentColor: androidx.compose.ui.graphics.Color = androidx.compose.material3.contentColorFor(containerColor),
    elevation: androidx.compose.material3.FloatingActionButtonElevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(),
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    val chromaColors = LocalOscellionColors.current
    val hazeState = LocalHazeState.current
    
    androidx.compose.material3.FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .hazeChild(state = hazeState, shape = shape)
            .glossyBorder(shape, chromaColors),
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
        interactionSource = interactionSource,
        content = content
    )
}

val PrimaryButtonGradient = Brush.verticalGradient(
    listOf(
        Color(0xFF4DA6FF), // 0%
        Color(0xFF0A84FF), // 52%
        Color(0xFF0051D5)  // 100%
    )
)

/**
 * Primary action button with vertical electric blue gradient (#4DA6FF -> #0A84FF -> #0051D5)
 * and subtle upper ambient highlight shadow.
 */
@Composable
fun PrimaryGradientButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(8.dp),
    height: androidx.compose.ui.unit.Dp = 38.dp,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 8.dp),
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 2.dp,
                shape = shape,
                ambientColor = Color(0x66EFEFEF),
                spotColor = Color(0x4D0550ED)
            )
            .clip(shape)
            .background(
                if (enabled) PrimaryButtonGradient
                else Brush.verticalGradient(listOf(Color(0xFF8E8E93), Color(0xFF636366)))
            )
            .border(
                0.5.dp,
                Color.White.copy(alpha = if (enabled) 0.4f else 0.1f),
                shape
            )
            .clickable(enabled = enabled, onClick = onClick)
            .then(if (height > 0.dp) Modifier.height(height) else Modifier)
            .padding(contentPadding),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            content()
        }
    }
}

enum class ChromaPillVariant {
    Blue,      // Liquid Glass Primary Blue (#4DA6FF -> #0A84FF -> #0051D5)
    Secondary, // Liquid Glass Secondary White / Specular (#FFFFFF -> #F4F7FB -> #DFE8F3)
    Purple,    // Creative Neon Violet (#A855F7 -> #7C3AED -> #581C87)
    Emerald,   // Success Mint Green (#34D399 -> #059669 -> #065F46)
    Amber,     // Radiant Orange/Gold (#FB923C -> #EA580C -> #9A3412)
    Rose,      // Vivid Crimson/Red (#FB7185 -> #E11D48 -> #881337)
    Cyan,      // Arctic Teal/Cyan (#38BDF8 -> #0284C7 -> #0C4A6E)
    DarkGlass  // Frosted Glass
}

object ChromaPillColors {
    val BlueBorder = Brush.linearGradient(
        listOf(
            Color(0xFFFFFFFF).copy(alpha = 0.95f),
            Color(0xFF7CC4FF).copy(alpha = 0.65f),
            Color(0xFFE0F2FE).copy(alpha = 0.85f)
        ),
        start = androidx.compose.ui.geometry.Offset.Zero,
        end = androidx.compose.ui.geometry.Offset.Infinite
    )
    val BlueBody = Brush.verticalGradient(
        listOf(
            Color(0xFF4DA6FF),
            Color(0xFF0A84FF),
            Color(0xFF0051D5)
        )
    )

    val SecondaryBorder = Brush.linearGradient(
        listOf(
            Color.White,
            Color(0xFF0A84FF).copy(alpha = 0.28f),
            Color(0xFF93C5FD).copy(alpha = 0.55f)
        ),
        start = androidx.compose.ui.geometry.Offset.Zero,
        end = androidx.compose.ui.geometry.Offset.Infinite
    )
    val SecondaryBody = Brush.verticalGradient(
        listOf(
            Color(0xFFFFFFFF),
            Color(0xFFF8F9FA),
            Color(0xFFF1F3F5)
        )
    )

    val PurpleBorder = Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = 0.92f),
            Color(0xFFD8B4FE).copy(alpha = 0.65f),
            Color(0xFFF3E8FF).copy(alpha = 0.85f)
        ),
        start = androidx.compose.ui.geometry.Offset.Zero,
        end = androidx.compose.ui.geometry.Offset.Infinite
    )
    val PurpleBody = Brush.verticalGradient(listOf(Color(0xFFA855F7), Color(0xFF7C3AED), Color(0xFF581C87)))

    val EmeraldBorder = Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = 0.92f),
            Color(0xFF6EE7B7).copy(alpha = 0.65f),
            Color(0xFFD1FAE5).copy(alpha = 0.85f)
        ),
        start = androidx.compose.ui.geometry.Offset.Zero,
        end = androidx.compose.ui.geometry.Offset.Infinite
    )
    val EmeraldBody = Brush.verticalGradient(listOf(Color(0xFF34D399), Color(0xFF059669), Color(0xFF065F46)))

    val AmberBorder = Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = 0.92f),
            Color(0xFFFED7AA).copy(alpha = 0.65f),
            Color(0xFFFEF3C7).copy(alpha = 0.85f)
        ),
        start = androidx.compose.ui.geometry.Offset.Zero,
        end = androidx.compose.ui.geometry.Offset.Infinite
    )
    val AmberBody = Brush.verticalGradient(listOf(Color(0xFFFB923C), Color(0xFFEA580C), Color(0xFF9A3412)))

    val RoseBorder = Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = 0.92f),
            Color(0xFFFDA4AF).copy(alpha = 0.65f),
            Color(0xFFFFE4E6).copy(alpha = 0.85f)
        ),
        start = androidx.compose.ui.geometry.Offset.Zero,
        end = androidx.compose.ui.geometry.Offset.Infinite
    )
    val RoseBody = Brush.verticalGradient(listOf(Color(0xFFFB7185), Color(0xFFE11D48), Color(0xFF881337)))

    val CyanBorder = Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = 0.92f),
            Color(0xFF7DD3FC).copy(alpha = 0.65f),
            Color(0xFFE0F2FE).copy(alpha = 0.85f)
        ),
        start = androidx.compose.ui.geometry.Offset.Zero,
        end = androidx.compose.ui.geometry.Offset.Infinite
    )
    val CyanBody = Brush.verticalGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7), Color(0xFF0C4A6E)))

    val DarkGlassBorder = Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = 0.70f),
            Color(0xFF94A3B8).copy(alpha = 0.25f),
            Color.White.copy(alpha = 0.50f)
        ),
        start = androidx.compose.ui.geometry.Offset.Zero,
        end = androidx.compose.ui.geometry.Offset.Infinite
    )
    val DarkGlassBody = Brush.verticalGradient(
        listOf(
            Color(0xFFFFFFFF).copy(alpha = 0.12f),
            Color(0xFFFFFFFF).copy(alpha = 0.05f)
        )
    )

    fun getBorder(variant: ChromaPillVariant): Brush = when (variant) {
        ChromaPillVariant.Blue -> BlueBorder
        ChromaPillVariant.Secondary -> SecondaryBorder
        ChromaPillVariant.Purple -> PurpleBorder
        ChromaPillVariant.Emerald -> EmeraldBorder
        ChromaPillVariant.Amber -> AmberBorder
        ChromaPillVariant.Rose -> RoseBorder
        ChromaPillVariant.Cyan -> CyanBorder
        ChromaPillVariant.DarkGlass -> DarkGlassBorder
    }

    fun getBody(variant: ChromaPillVariant): Brush = when (variant) {
        ChromaPillVariant.Blue -> BlueBody
        ChromaPillVariant.Secondary -> SecondaryBody
        ChromaPillVariant.Purple -> PurpleBody
        ChromaPillVariant.Emerald -> EmeraldBody
        ChromaPillVariant.Amber -> AmberBody
        ChromaPillVariant.Rose -> RoseBody
        ChromaPillVariant.Cyan -> CyanBody
        ChromaPillVariant.DarkGlass -> DarkGlassBody
    }
}

val PrimaryPillBorderGradient = ChromaPillColors.BlueBorder
val PrimaryPillBodyGradient = ChromaPillColors.BlueBody

/**
 * Chroma Gaming Pill Button:
 * - Outer border: 1.5dp padding with vertical gradient highlight
 * - Inner body: 3-stop rich vertical gradient fill
 * - Shape: 100% pill rounded-full (CircleShape)
 * - Supported color variants: Blue, Purple, Emerald, Amber, Rose, Cyan, DarkGlass
 */
@Composable
fun ChromaPillButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ChromaPillVariant = ChromaPillVariant.Blue,
    borderBrush: Brush? = null,
    bodyBrush: Brush? = null,
    enabled: Boolean = true,
    height: androidx.compose.ui.unit.Dp = 38.dp,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp),
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    val activeBorder = borderBrush ?: ChromaPillColors.getBorder(variant)
    val activeBody = bodyBrush ?: ChromaPillColors.getBody(variant)
    val disabledBorder = Brush.verticalGradient(listOf(Color(0xFF8E8E93), Color(0xFF636366)))
    val disabledBody = Brush.verticalGradient(listOf(Color(0xFF636366), Color(0xFF48484A)))
    val strokeWidth = if (variant == ChromaPillVariant.Secondary) 1.dp else 1.5.dp
    val defaultContentColor = if (variant == ChromaPillVariant.Secondary) Color.Black else Color.White

    Box(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(if (enabled) activeBorder else disabledBorder)
            .padding(strokeWidth)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(if (enabled) activeBody else disabledBody)
            .clickable(enabled = enabled, onClick = onClick)
            .then(if (height > 0.dp) Modifier.height(height) else Modifier)
            .padding(contentPadding),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides defaultContentColor
        ) {
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                content()
            }
        }
    }
}
