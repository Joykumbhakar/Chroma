package com.chroma.studio.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.R
import com.composables.icons.lucide.*
import kotlin.math.roundToInt

@Immutable
data class CreatorTestimonial(
    val name: String,
    val username: String,
    val body: String,
    val regionTag: String,
    @param:DrawableRes val avatarRes: Int,
    val verified: Boolean = true,
    val remixCount: String = "48"
)

private val DefaultCreatorTestimonials = listOf(
    CreatorTestimonial(
        name = "Ava Green",
        username = "@ava",
        body = "16-bit LUT export made my whole color workflow 10x faster.",
        regionTag = "AU",
        avatarRes = R.drawable.onboard_art_botanical,
        remixCount = "124"
    ),
    CreatorTestimonial(
        name = "Ana Miller",
        username = "@ana",
        body = "Live shader layers on mobile are an absolute game changer.",
        regionTag = "DE",
        avatarRes = R.drawable.onboard_art_prism,
        remixCount = "89"
    ),
    CreatorTestimonial(
        name = "Mateo Rossi",
        username = "@mat",
        body = "Flashes & optical blur curves feel buttery smooth.",
        regionTag = "IT",
        avatarRes = R.drawable.onboard_art_architecture,
        remixCount = "210"
    ),
    CreatorTestimonial(
        name = "Maya Patel",
        username = "@maya",
        body = "Published my first shader preset and hit Trending in minutes.",
        regionTag = "IN",
        avatarRes = R.drawable.wallpaper_hd_3,
        remixCount = "342"
    ),
    CreatorTestimonial(
        name = "Noah Smith",
        username = "@noah",
        body = "End-to-end encrypted Close Friends circles feel effortless.",
        regionTag = "US",
        avatarRes = R.drawable.wallpaper_macbook,
        remixCount = "67"
    ),
    CreatorTestimonial(
        name = "Lucas Stone",
        username = "@luc",
        body = "Studio Dark OLED contrast is spot on for late grading.",
        regionTag = "FR",
        avatarRes = R.drawable.wallpaper_stock,
        remixCount = "118"
    ),
    CreatorTestimonial(
        name = "Haruto Sato",
        username = "@haru",
        body = "Impressive 60fps canvas fluidity right on my phone.",
        regionTag = "JP",
        avatarRes = R.drawable.wallpaper_hd_4,
        remixCount = "195"
    ),
    CreatorTestimonial(
        name = "Emma Lee",
        username = "@emma",
        body = "Remixing pipelines between web Studio & Android syncs instantly.",
        regionTag = "CA",
        avatarRes = R.drawable.wallpaper_macbook_alt,
        remixCount = "153"
    ),
    CreatorTestimonial(
        name = "Carlos Ray",
        username = "@carl",
        body = "Voice notes, HD drops, and zero clutter. Pure craft.",
        regionTag = "ES",
        avatarRes = R.drawable.wallpapers,
        remixCount = "74"
    ),
    CreatorTestimonial(
        name = "Chloe Park",
        username = "@chloe",
        body = "Split-tone curves & grain match my cinema camera LUTs.",
        regionTag = "KR",
        avatarRes = R.drawable.wallpaper,
        remixCount = "138"
    ),
    CreatorTestimonial(
        name = "Liam Brooks",
        username = "@liam",
        body = "Cross-syncing palettes from web Studio saves hours every week.",
        regionTag = "UK",
        avatarRes = R.drawable.bg_lock_screen,
        remixCount = "96"
    ),
    CreatorTestimonial(
        name = "Sofia Silva",
        username = "@sofia",
        body = "Clean optical presets that export crisp straight to Flashes.",
        regionTag = "BR",
        avatarRes = R.drawable.wallpaper_windows,
        remixCount = "264"
    )
)

/**
 * Jetpack Compose port of the shadcn/21st 3D Testimonials Vertical Marquee:
 * 4 alternating vertical columns tilted in 3D perspective (rotateX=20°, rotateY=-10°, rotateZ=20°),
 * smooth endless scrolling, touch-hold pause, and 4-edge gradient vignette fade.
 */
@Composable
fun Marquee3DTestimonials(
    modifier: Modifier = Modifier,
    fadeColor: Color = Color(0xFF09090B),
    testimonials: List<CreatorTestimonial> = DefaultCreatorTestimonials
) {
    val density = LocalDensity.current
    var isPaused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPaused = true
                        tryAwaitRelease()
                        isPaused = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Partition into disjoint pools for adjacent columns so cards never duplicate side-by-side.
        val evenPool = remember(testimonials) {
            testimonials.filterIndexed { idx, _ -> idx % 2 == 0 }.ifEmpty { testimonials }
        }
        val oddPool = remember(testimonials) {
            testimonials.filterIndexed { idx, _ -> idx % 2 == 1 }.ifEmpty { testimonials.reversed() }
        }
        val col1Items = evenPool
        val col2Items = oddPool
        val col3Items = remember(evenPool) { evenPool.reversed() }
        val col4Items = remember(oddPool) { oddPool.reversed() }

        // Oversized 3D Isometric Tilted Stage so rotated edges bleed cleanly beyond all viewport bounds.
        Row(
            modifier = Modifier
                .requiredWidth(880.dp)
                .requiredHeight(960.dp)
                .graphicsLayer {
                    cameraDistance = 24f * density.density
                    translationX = with(density) { (-14).dp.toPx() }
                    translationY = with(density) { (-36).dp.toPx() }
                    rotationX = 20f
                    rotationY = -10f
                    rotationZ = 20f
                },
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VerticalMarqueeColumn(
                items = col1Items,
                reverse = false,
                speedDpPerSec = 34f,
                paused = isPaused,
                startOffsetFraction = 0.0f
            )
            VerticalMarqueeColumn(
                items = col2Items,
                reverse = true,
                speedDpPerSec = 38f,
                paused = isPaused,
                startOffsetFraction = 0.18f
            )
            VerticalMarqueeColumn(
                items = col3Items,
                reverse = false,
                speedDpPerSec = 32f,
                paused = isPaused,
                startOffsetFraction = 0.50f
            )
            VerticalMarqueeColumn(
                items = col4Items,
                reverse = true,
                speedDpPerSec = 36f,
                paused = isPaused,
                startOffsetFraction = 0.68f
            )
        }

        // 4-Side Edge Gradient Vignettes
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.24f)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        0.0f to fadeColor,
                        0.42f to fadeColor.copy(alpha = 0.72f),
                        1.0f to Color.Transparent
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.42f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.45f to fadeColor.copy(alpha = 0.86f),
                        1.0f to fadeColor
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.12f)
                .align(Alignment.CenterStart)
                .background(Brush.horizontalGradient(listOf(fadeColor, Color.Transparent)))
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.12f)
                .align(Alignment.CenterEnd)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, fadeColor)))
        )
    }
}

@Composable
private fun VerticalMarqueeColumn(
    items: List<CreatorTestimonial>,
    reverse: Boolean,
    speedDpPerSec: Float,
    paused: Boolean,
    startOffsetFraction: Float,
    columnWidth: Dp = 176.dp,
    gap: Dp = 12.dp
) {
    val density = LocalDensity.current
    val speedPx = with(density) { speedDpPerSec.dp.toPx() }
    var blockHeightPx by remember { mutableIntStateOf(0) }
    val scrollOffset = remember { mutableFloatStateOf(0f) }
    var initializedOffset by remember { mutableStateOf(false) }

    LaunchedEffect(blockHeightPx, reverse, paused, speedPx) {
        if (blockHeightPx <= 0) return@LaunchedEffect
        val period = blockHeightPx.toFloat()
        if (!initializedOffset) {
            scrollOffset.floatValue = -(startOffsetFraction * period) % period
            initializedOffset = true
        }
        var lastNs = 0L
        while (true) {
            withFrameNanos { frameNs ->
                if (lastNs == 0L) lastNs = frameNs
                val dt = ((frameNs - lastNs) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastNs = frameNs
                if (!paused) {
                    val dir = if (reverse) 1f else -1f
                    var next = scrollOffset.floatValue + dir * speedPx * dt
                    while (next <= -period) next += period
                    while (next > 0f) next -= period
                    scrollOffset.floatValue = next
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .width(columnWidth)
            .fillMaxHeight()
            .clipToBounds()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Allow all 3 loop copies to measure unconstrained vertically so cards never squash/clip.
                .wrapContentHeight(align = Alignment.Top, unbounded = true)
                .offset { IntOffset(0, scrollOffset.floatValue.roundToInt()) }
        ) {
            // 3 repeated blocks guarantee seamless wrap at both reverse=false and reverse=true
            for (copy in 0 until 3) {
                Column(
                    modifier = (if (copy == 0) {
                        Modifier
                            .fillMaxWidth()
                            .onSizeChanged { blockHeightPx = it.height }
                    } else {
                        Modifier.fillMaxWidth()
                    }).padding(bottom = gap),
                    verticalArrangement = Arrangement.spacedBy(gap)
                ) {
                    items.forEach { review ->
                        CreatorTestimonialCard(item = review)
                    }
                }
            }
        }
    }
}

@Composable
private fun CreatorTestimonialCard(item: CreatorTestimonial) {
    val cardShape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(Color(0xF5151722))
            .border(1.dp, Color.White.copy(alpha = 0.11f), cardShape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Image(
                painter = painterResource(id = item.avatarRes),
                contentDescription = item.name,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.name,
                        color = Color(0xFFFAFAFA),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (item.verified) {
                        Icon(
                            imageVector = Lucide.BadgeCheck,
                            contentDescription = null,
                            tint = Color(0xFF0381FE),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Text(
                    text = "${item.username} · ${item.regionTag}",
                    color = Color(0xFFA1A1AA),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Text(
            text = item.body,
            color = Color(0xFFE4E4E7),
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Lucide.HeartHandshake,
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = "${item.remixCount} remixes",
                color = Color(0xFF94A3B8),
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
