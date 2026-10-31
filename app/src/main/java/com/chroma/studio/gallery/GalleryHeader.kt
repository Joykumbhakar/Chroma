package com.chroma.studio.gallery

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowDown
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowUp
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Samsung One UIâ€“style coordinator: the large centred title collapses as the grid scrolls up
 * and re-expands when you overscroll back at the top of the list.
 */
@Stable
class CollapsingHeaderState(val maxCollapsePx: Float) {
    var offsetPx by mutableFloatStateOf(0f)
        private set

    val progress: Float
        get() = if (maxCollapsePx <= 0f) 1f else (-offsetPx / maxCollapsePx).coerceIn(0f, 1f)

    val isCollapsed: Boolean get() = progress > 0.5f

    val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (available.y >= 0f) return Offset.Zero
            val new = (offsetPx + available.y).coerceIn(-maxCollapsePx, 0f)
            val consumed = new - offsetPx
            offsetPx = new
            return Offset(0f, consumed)
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (available.y <= 0f) return Offset.Zero
            val new = (offsetPx + available.y).coerceIn(-maxCollapsePx, 0f)
            val used = new - offsetPx
            offsetPx = new
            return Offset(0f, used)
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            settle()
            return Velocity.Zero
        }
    }

    /** Snap to the nearest end so the header never rests half-collapsed. */
    suspend fun settle() = animateTo(if (progress > 0.5f) -maxCollapsePx else 0f)

    suspend fun toggle() = animateTo(if (isCollapsed) 0f else -maxCollapsePx)

    private suspend fun animateTo(target: Float) {
        animate(offsetPx, target, animationSpec = tween(220)) { v, _ -> offsetPx = v }
    }
}

@Composable
fun GalleryHeader(
    state: CollapsingHeaderState,
    expandedHeight: Dp,
    title: String,
    subtitle: String,
    countBadge: Int,
    isSheet: Boolean,
    onBack: () -> Unit,
    onToggleCollapse: suspend () -> Unit,
    onPicker: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Gallery.colors
    val density = LocalDensity.current
    val progress = state.progress
    val expandedPx = with(density) { expandedHeight.toPx() }
    val scope = rememberCoroutineScope()

    Column(modifier.fillMaxWidth().background(c.surface)) {
        // Expanded title area (shrinks with scroll)
        Box(
            Modifier
                .fillMaxWidth()
                .height(with(density) { (expandedPx + state.offsetPx).coerceAtLeast(0f).toDp() })
                .graphicsLayer { alpha = (1f - progress * 1.4f).coerceIn(0f, 1f) },
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(title, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = c.text, maxLines = 1)
                Text(
                    subtitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = c.muted,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        // Compact sticky bar
        Row(
            Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!isSheet) HeaderIcon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)

            Row(
                Modifier
                    .padding(start = if (isSheet) 8.dp else 4.dp)
                    .graphicsLayer { alpha = ((progress - 0.55f) / 0.45f).coerceIn(0f, 1f) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = c.text, maxLines = 1)
                Box(
                    Modifier.clip(CircleShape).background(c.elevated).padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text("$countBadge", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = c.muted)
                }
            }

            Box(Modifier.weight(1f))

            HeaderIcon(
                if (state.isCollapsed) Icons.Rounded.KeyboardDoubleArrowDown else Icons.Rounded.KeyboardDoubleArrowUp,
                if (state.isCollapsed) "Expand header" else "Collapse header",
                onClick = { scope.launch { onToggleCollapse() } },
                tint = c.muted,
            )
            HeaderIcon(Icons.Rounded.FolderOpen, "System photo picker", onPicker)
            HeaderIcon(Icons.Rounded.Search, "Search", onSearch)
            if (isSheet) HeaderIcon(Icons.Rounded.Close, "Close", onBack, tint = c.muted)
        }
        HorizontalDivider(color = c.hairline, thickness = 0.5.dp)
    }
}

@Composable
private fun HeaderIcon(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = Gallery.colors.text,
) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(20.dp))
    }
}
