package com.chroma.studio.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Universal Verification Tiers for legacy enum callers.
 */
enum class VerifiedBadgeTier {
    BLUE,       // Official Creator / Verified (Standard Blue)
    GOLD,       // Official Organization / Gold Premium
    CHROMA,     // Chroma Original / Prism Holographic
    SILVER      // Certified Partner / Silver Exec
}

/**
 * Designed styled verified badge definition matching Admin Panel & Web `BADGE_PALETTES`.
 */
@Immutable
data class VerifiedBadgeStyle(
    val key: String,
    val label: String,
    val gradient: List<Color>,
    val glowColor: Color,
    val metallicRim: Boolean = true,
    val autoShimmer: Boolean = false
)

val VerifiedBadgeBlueGradient = listOf(
    Color(0xFF3B82F6),
    Color(0xFF0381FE),
    Color(0xFF1D4ED8)
)

val VerifiedBadgeGoldGradient = listOf(
    Color(0xFFFBBF24),
    Color(0xFFF59E0B),
    Color(0xFFB45309)
)

val VerifiedBadgeChromaGradient = listOf(
    Color(0xFF00F5FF),
    Color(0xFF0381FE),
    Color(0xFF0052B4)
)

val VerifiedBadgeSilverGradient = listOf(
    Color(0xFFE2E8F0),
    Color(0xFF9CA3AF),
    Color(0xFF4B5563)
)

/**
 * Complete catalog of all 20 Admin-configurable styled verified badges (+ Chroma Prism).
 */
val AllVerifiedBadgeStyles: List<VerifiedBadgeStyle> = listOf(
    VerifiedBadgeStyle(
        key = "blue",
        label = "Standard Blue",
        gradient = VerifiedBadgeBlueGradient,
        glowColor = Color(0xFF3B82F6)
    ),
    VerifiedBadgeStyle(
        key = "gold",
        label = "Gold Premium",
        gradient = VerifiedBadgeGoldGradient,
        glowColor = Color(0xFFFBBF24),
        autoShimmer = true
    ),
    VerifiedBadgeStyle(
        key = "silver",
        label = "Silver Exec",
        gradient = VerifiedBadgeSilverGradient,
        glowColor = Color(0xFF9CA3AF),
        autoShimmer = true
    ),
    VerifiedBadgeStyle(
        key = "bronze",
        label = "Bronze Pioneer",
        gradient = listOf(Color(0xFFF59E0B), Color(0xFFD97706), Color(0xFF78350F)),
        glowColor = Color(0xFFD97706)
    ),
    VerifiedBadgeStyle(
        key = "emerald",
        label = "Emerald Official",
        gradient = listOf(Color(0xFF34D399), Color(0xFF10B981), Color(0xFF047857)),
        glowColor = Color(0xFF10B981)
    ),
    VerifiedBadgeStyle(
        key = "ruby",
        label = "Ruby Elite",
        gradient = listOf(Color(0xFFFB7185), Color(0xFFF43F5E), Color(0xFF9F1239)),
        glowColor = Color(0xFFF43F5E)
    ),
    VerifiedBadgeStyle(
        key = "amethyst",
        label = "Amethyst Trust",
        gradient = listOf(Color(0xFFC084FC), Color(0xFFA855F7), Color(0xFF6B21A8)),
        glowColor = Color(0xFFA855F7)
    ),
    VerifiedBadgeStyle(
        key = "midnight",
        label = "Midnight Core",
        gradient = listOf(Color(0xFF475569), Color(0xFF334155), Color(0xFF0F172A)),
        glowColor = Color(0xFF334155)
    ),
    VerifiedBadgeStyle(
        key = "spaceGray",
        label = "Space Gray Auth",
        gradient = listOf(Color(0xFF94A3B8), Color(0xFF64748B), Color(0xFF334155)),
        glowColor = Color(0xFF64748B)
    ),
    VerifiedBadgeStyle(
        key = "navy",
        label = "Navy Seal",
        gradient = listOf(Color(0xFF2563EB), Color(0xFF1E3A8A), Color(0xFF172554)),
        glowColor = Color(0xFF1E3A8A)
    ),
    VerifiedBadgeStyle(
        key = "teal",
        label = "Teal Authentic",
        gradient = listOf(Color(0xFF2DD4BF), Color(0xFF14B8A6), Color(0xFF0F766E)),
        glowColor = Color(0xFF14B8A6)
    ),
    VerifiedBadgeStyle(
        key = "crimson",
        label = "Crimson Crown",
        gradient = listOf(Color(0xFFFB7185), Color(0xFFE11D48), Color(0xFF881337)),
        glowColor = Color(0xFFE11D48)
    ),
    VerifiedBadgeStyle(
        key = "slate",
        label = "Slate Authority",
        gradient = listOf(Color(0xFF64748B), Color(0xFF475569), Color(0xFF1E293B)),
        glowColor = Color(0xFF475569)
    ),
    VerifiedBadgeStyle(
        key = "deepPurple",
        label = "Deep Purple VIP",
        gradient = listOf(Color(0xFFA855F7), Color(0xFF7E22CE), Color(0xFF4C1D95)),
        glowColor = Color(0xFF7E22CE)
    ),
    VerifiedBadgeStyle(
        key = "ocean",
        label = "Ocean Sentinel",
        gradient = listOf(Color(0xFF38BDF8), Color(0xFF0EA5E9), Color(0xFF0369A1)),
        glowColor = Color(0xFF0EA5E9)
    ),
    VerifiedBadgeStyle(
        key = "forest",
        label = "Forest Secure",
        gradient = listOf(Color(0xFF4ADE80), Color(0xFF22C55E), Color(0xFF15803D)),
        glowColor = Color(0xFF22C55E)
    ),
    VerifiedBadgeStyle(
        key = "cyan",
        label = "Corporate Cyan",
        gradient = listOf(Color(0xFF22D3EE), Color(0xFF06B6D4), Color(0xFF0E7490)),
        glowColor = Color(0xFF06B6D4)
    ),
    VerifiedBadgeStyle(
        key = "charcoal",
        label = "Charcoal Stealth",
        gradient = listOf(Color(0xFF52525B), Color(0xFF3F3F46), Color(0xFF18181B)),
        glowColor = Color(0xFF3F3F46)
    ),
    VerifiedBadgeStyle(
        key = "roseGold",
        label = "Rose Gold",
        gradient = listOf(Color(0xFFFECDD3), Color(0xFFFDA4AF), Color(0xFFBE123C)),
        glowColor = Color(0xFFFDA4AF),
        autoShimmer = true
    ),
    VerifiedBadgeStyle(
        key = "sapphire",
        label = "Sapphire Auth",
        gradient = listOf(Color(0xFF6366F1), Color(0xFF4F46E5), Color(0xFF312E81)),
        glowColor = Color(0xFF4F46E5)
    ),
    VerifiedBadgeStyle(
        key = "chroma",
        label = "Chroma Prism",
        gradient = VerifiedBadgeChromaGradient,
        glowColor = Color(0xFF0381FE),
        autoShimmer = true
    )
)

private val VERIFIED_BADGE_STYLE_BY_NORMALIZED_KEY: Map<String, VerifiedBadgeStyle> by lazy {
    buildMap {
        for (style in AllVerifiedBadgeStyles) {
            put(style.key.lowercase(), style)
        }
        put("space_gray", AllVerifiedBadgeStyles.first { it.key == "spaceGray" })
        put("space-gray", AllVerifiedBadgeStyles.first { it.key == "spaceGray" })
        put("deep_purple", AllVerifiedBadgeStyles.first { it.key == "deepPurple" })
        put("deep-purple", AllVerifiedBadgeStyles.first { it.key == "deepPurple" })
        put("rose_gold", AllVerifiedBadgeStyles.first { it.key == "roseGold" })
        put("rose-gold", AllVerifiedBadgeStyles.first { it.key == "roseGold" })
        put("obsidian", AllVerifiedBadgeStyles.first { it.key == "midnight" })
    }
}

fun resolveVerifiedBadgeStyle(
    tone: String? = null,
    tier: VerifiedBadgeTier = VerifiedBadgeTier.BLUE
): VerifiedBadgeStyle {
    val cleanTone = tone?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
    if (cleanTone != null) {
        VERIFIED_BADGE_STYLE_BY_NORMALIZED_KEY[cleanTone]?.let { return it }
    }
    val tierKey = when (tier) {
        VerifiedBadgeTier.BLUE -> "blue"
        VerifiedBadgeTier.GOLD -> "gold"
        VerifiedBadgeTier.CHROMA -> "chroma"
        VerifiedBadgeTier.SILVER -> "silver"
    }
    return VERIFIED_BADGE_STYLE_BY_NORMALIZED_KEY[tierKey] ?: AllVerifiedBadgeStyles.first()
}

val IosBadgeGradients = listOf(
    VerifiedBadgeBlueGradient
)

// 12-lobed scalloped rosette seal in 24x24 canonical coordinate space (matches Web `VerifiedSealBadge`)
private const val SEAL_PATH_STRING =
    "M22.5 12.5c0-1.58-.875-2.95-2.148-3.6.154-.435.238-.905.238-1.4 0-2.21-1.71-3.998-3.918-3.998-.47 0-.92.084-1.336.25C14.818 2.415 13.51 1.5 12 1.5s-2.816.917-3.337 2.25c-.416-.165-.866-.25-1.336-.25-2.21 0-3.918 1.79-3.918 4 0 .495.084.965.238 1.4-1.273.65-2.148 2.02-2.148 3.6 0 1.46.74 2.746 1.865 3.45-.06.31-.09.63-.09.95 0 2.21 1.71 4 3.918 4 .47 0 .92-.085 1.336-.25.52 1.335 1.826 2.25 3.337 2.25s2.816-.915 3.337-2.25c.416.165.866.25 1.336.25 2.21 0 3.918-1.79 3.918-4 0-.32-.03-.64-.09-.95 1.125-.705 1.865-1.99 1.865-3.45z"

/**
 * Universal Designed Verified Badge used across all screens in Oscellion Android.
 * Supports all 20 Admin-controlled styled badge palettes with vertical specular gradients,
 * metallic diagonal rim lighting, soft depth shadow, optional ambient glow, and gleam shimmer.
 */
@Composable
fun VerifiedBadge(
    modifier: Modifier = Modifier,
    size: Dp = 14.dp,
    tier: VerifiedBadgeTier = VerifiedBadgeTier.BLUE,
    tone: String? = null,
    badgeColor: Color? = null,
    userId: String? = null,
    animated: Boolean = false,
    showGlow: Boolean = false
) {
    val resolvedStyle = remember(tone, tier) {
        resolveVerifiedBadgeStyle(tone = tone, tier = tier)
    }
    val shouldAnimate = animated || resolvedStyle.autoShimmer

    val sealPath = remember { PathParser().parsePathString(SEAL_PATH_STRING).toPath() }
    val checkPath = remember {
        Path().apply {
            moveTo(7.4f, 12.1f)
            lineTo(10.5f, 15.2f)
            lineTo(17.1f, 8.5f)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "badgeGleam")
    val gleamProgress by if (shouldAnimate) {
        infiniteTransition.animateFloat(
            initialValue = -1f,
            targetValue = 2f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "gleamShift"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    val primaryBrush = remember(resolvedStyle, badgeColor) {
        if (badgeColor != null) {
            Brush.verticalGradient(listOf(badgeColor, badgeColor))
        } else {
            Brush.verticalGradient(
                colors = resolvedStyle.gradient,
                startY = 1.5f,
                endY = 22.5f
            )
        }
    }

    val effectiveGlowColor = remember(resolvedStyle, badgeColor) {
        (badgeColor ?: resolvedStyle.glowColor).copy(alpha = 0.36f)
    }

    Canvas(modifier = modifier.size(size)) {
        val scale = this.size.width / 24f

        // 0. Optional Ambient Glow matching badge palette
        if (showGlow) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(effectiveGlowColor, Color.Transparent),
                    center = center,
                    radius = this.size.width * 0.78f
                ),
                radius = this.size.width * 0.78f
            )
        }

        withTransform({
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            drawIntoCanvas { canvas ->
                // 1. Soft Seal Drop Shadow for realistic sculpted depth
                val sealShadowPaint = androidx.compose.ui.graphics.Paint().apply {
                    color = Color.Black.copy(alpha = 0.24f)
                }
                sealShadowPaint.asFrameworkPaint().maskFilter =
                    android.graphics.BlurMaskFilter(1f, android.graphics.BlurMaskFilter.Blur.NORMAL)

                canvas.nativeCanvas.save()
                canvas.nativeCanvas.translate(0f, 0.45f)
                canvas.nativeCanvas.drawPath(sealPath.asAndroidPath(), sealShadowPaint.asFrameworkPaint())
                canvas.nativeCanvas.restore()

                // 2. Sculpted Rosette Seal Body
                drawPath(
                    path = sealPath,
                    brush = primaryBrush,
                    style = Fill
                )

                // 3. Specular Gleam Sweep (animated / metallic tiers)
                if (shouldAnimate) {
                    val gleamWidth = 8f
                    val gleamStart = gleamProgress * 24f
                    drawPath(
                        path = sealPath,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.36f),
                                Color.Transparent
                            ),
                            start = Offset(gleamStart, 0f),
                            end = Offset(gleamStart + gleamWidth, 24f)
                        ),
                        style = Fill
                    )
                }

                // 4. Metallic Bevel Rim Lighting matching Web `bd-rim`
                drawPath(
                    path = sealPath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.70f),
                            Color.White.copy(alpha = 0.12f),
                            Color.Black.copy(alpha = 0.36f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(24f, 24f)
                    ),
                    style = Stroke(width = 0.65f, join = StrokeJoin.Round)
                )

                // 5. Checkmark Drop Shadow
                val checkShadowPaint = androidx.compose.ui.graphics.Paint().apply {
                    color = Color.Black.copy(alpha = 0.22f)
                }
                checkShadowPaint.asFrameworkPaint().strokeWidth = 2.4f
                checkShadowPaint.asFrameworkPaint().style = android.graphics.Paint.Style.STROKE
                checkShadowPaint.asFrameworkPaint().strokeCap = android.graphics.Paint.Cap.ROUND
                checkShadowPaint.asFrameworkPaint().strokeJoin = android.graphics.Paint.Join.ROUND
                checkShadowPaint.asFrameworkPaint().maskFilter =
                    android.graphics.BlurMaskFilter(0.8f, android.graphics.BlurMaskFilter.Blur.NORMAL)

                canvas.nativeCanvas.save()
                canvas.nativeCanvas.translate(0f, 0.35f)
                canvas.nativeCanvas.drawPath(checkPath.asAndroidPath(), checkShadowPaint.asFrameworkPaint())
                canvas.nativeCanvas.restore()

                // 6. Crisp White Checkmark with Rounded Caps & Join
                drawPath(
                    path = checkPath,
                    color = Color.White,
                    style = Stroke(
                        width = 2.4f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}

/**
 * Backward compatibility alias for IosVerificationBadge.
 */
@Composable
fun IosVerificationBadge(
    modifier: Modifier = Modifier,
    userId: String? = null,
    size: Dp = 14.dp,
    tier: VerifiedBadgeTier = VerifiedBadgeTier.BLUE,
    tone: String? = null,
    animated: Boolean = false
) {
    VerifiedBadge(
        modifier = modifier,
        size = size,
        tier = tier,
        tone = tone,
        userId = userId,
        animated = animated
    )
}

/**
 * Backward compatibility alias for ScallopedVerifiedBadge.
 */
@Composable
fun ScallopedVerifiedBadge(
    modifier: Modifier = Modifier,
    size: Dp = 14.dp,
    badgeColor: Color? = null,
    tier: VerifiedBadgeTier = VerifiedBadgeTier.BLUE,
    tone: String? = null,
    animated: Boolean = false
) {
    VerifiedBadge(
        modifier = modifier,
        size = size,
        badgeColor = badgeColor,
        tier = tier,
        tone = tone,
        animated = animated
    )
}
