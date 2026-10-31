package com.chroma.studio.chat.ui.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.chat.ui.theme.LocalChatColors
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/*
 * ─────────────────────────────────────────────────────────────────────────────
 *  GlassIcons — native Jetpack Compose port of the React Bits <GlassIcons />.
 *
 *  Look (matches the web component defaults used in the brief):
 *    plate = "tilt" (30°), roundness 0.6, refraction 0.8, bevel 0.6, dispersion 0,
 *    frost 0.2, shine 0.85, tint #dfdfdf @ 0.2, hover = "press", labels visible.
 *
 *  Rendering:
 *    • Android 13+ (API 33): the original WebGL fragment shader, ported to AGSL
 *      (RuntimeShader) — real edge refraction, bevel, shade and specular rim.
 *    • Older devices / any shader failure: a pure-Compose approximation.
 * ─────────────────────────────────────────────────────────────────────────────
 */

// ── Gradient presets (the web component's HSL presets, converted to ARGB) ────

class GlassGradient(val from: Color, val to: Color) {
    companion object {
        val Blue = GlassGradient(Color(0xFF569DFB), Color(0xFF1949E6))
        val Purple = GlassGradient(Color(0xFFBF72F3), Color(0xFF6F34D5))
        val Red = GlassGradient(Color(0xFFFB606F), Color(0xFFE7235D))
        val Indigo = GlassGradient(Color(0xFF7780F8), Color(0xFF4E3FD5))
        val Orange = GlassGradient(Color(0xFFFFB83D), Color(0xFFF8651B))
        val Green = GlassGradient(Color(0xFF3CD77F), Color(0xFF109E73))
        val Teal = GlassGradient(Color(0xFF35DDE9), Color(0xFF108EBC))
        val Pink = GlassGradient(Color(0xFFFA80C5), Color(0xFFDC38B5))
    }
}

/** One glass tile. [icon] draws a glyph into a square DrawScope using the given ink color. */
class GlassIconItem(
    val label: String,
    val contentDescription: String,
    val gradient: GlassGradient,
    val icon: DrawScope.(ink: Color) -> Unit,
    val onClick: () -> Unit
)

/** Visual tuning, mirroring the React props. */
class GlassStyle(
    val roundness: Float = 0.6f,
    val refraction: Float = 0.8f,
    val bevel: Float = 0.6f,
    val frost: Float = 0.2f,
    val shine: Float = 0.85f,
    val tint: Color = Color(0xFFDFDFDF),
    val tintOpacity: Float = 0.2f,
    val tilt: Float = 30f,
    val shade: Float = 0.35f // 0.35 dark theme, 0.22 light theme
)

// ── Public composables ───────────────────────────────────────────────────────

/**
 * Generic glass icon grid. [visible] drives the staggered entrance / exit.
 * The layout reserves room above the first row for the tilted plate.
 */
@Composable
fun GlassIcons(
    items: List<GlassIconItem>,
    visible: Boolean,
    modifier: Modifier = Modifier,
    tile: Dp = 52.dp,
    columns: Int = items.size,
    gapRatio: Float = 0.5f,
    style: GlassStyle = GlassStyle(),
    labelColor: Color = Color(0xDBF4F4F5)
) {
    val agsl = remember { createGlassAgsl() }
    val perRow = max(1, columns)
    val rows = items.chunked(perRow)

    Column(
        modifier = modifier.padding(top = tile * 0.42f, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(tile * 0.5f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rows.forEachIndexed { rowIndex, rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(tile * gapRatio)) {
                rowItems.forEachIndexed { col, item ->
                    GlassIconTile(
                        item = item,
                        index = rowIndex * perRow + col,
                        total = items.size,
                        visible = visible,
                        tile = tile,
                        style = style,
                        agsl = agsl,
                        labelColor = labelColor
                    )
                }
            }
        }
    }
}

/**
 * Drop-in replacement for [SynchronizedRotatingActionNodes] in the chat "+" menu.
 * Uses the same [ActionMenuOption] enum, so callbacks are unchanged.
 */
@Composable
fun GlassIconsActionMenu(
    isOpen: Boolean,
    onOptionSelected: (ActionMenuOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val chatColors = LocalChatColors.current
    val options = remember { ActionMenuOption.entries.toList() }
    val gapRatio = 0.5f
    val pokeRatio = 0.5f // the tilted plate sticks out to the right of the last tile

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val n = options.size
        val fitted = maxWidth / (n + (n - 1) * gapRatio + pokeRatio)
        val tile = maxOf(36.dp, minOf(fitted, 58.dp))

        val items = remember(options) {
            options.map { option ->
                GlassIconItem(
                    label = option.glassLabel(),
                    contentDescription = option.label,
                    gradient = option.glassGradient(),
                    icon = option.glassGlyph(),
                    onClick = { onOptionSelected(option) }
                )
            }
        }

        GlassIcons(
            items = items,
            visible = isOpen,
            tile = tile,
            columns = n,
            gapRatio = gapRatio,
            style = GlassStyle(shade = if (chatColors.isLight) 0.22f else 0.35f),
            labelColor = if (chatColors.isLight) Color(0xC7181B1B) else Color(0xDBF4F4F5)
        )
    }
}

private fun ActionMenuOption.glassLabel(): String = when (this) {
    ActionMenuOption.CAMERA -> "Camera"
    ActionMenuOption.PHOTOS -> "Photos"
    ActionMenuOption.FILES -> "Files"
    ActionMenuOption.POLL -> "Poll"
    ActionMenuOption.STICKER -> "Sticker"
}

private fun ActionMenuOption.glassGradient(): GlassGradient = when (this) {
    ActionMenuOption.CAMERA -> GlassGradient.Pink
    ActionMenuOption.PHOTOS -> GlassGradient.Blue
    ActionMenuOption.FILES -> GlassGradient.Green
    ActionMenuOption.POLL -> GlassGradient.Purple
    ActionMenuOption.STICKER -> GlassGradient.Orange
}

private fun ActionMenuOption.glassGlyph(): DrawScope.(Color) -> Unit = when (this) {
    ActionMenuOption.CAMERA -> { ink -> glyphCamera(ink) }
    ActionMenuOption.PHOTOS -> { ink -> glyphPhotos(ink) }
    ActionMenuOption.FILES -> { ink -> glyphFiles(ink) }
    ActionMenuOption.POLL -> { ink -> glyphPoll(ink) }
    ActionMenuOption.STICKER -> { ink -> glyphSticker(ink) }
}

// ── One tile ─────────────────────────────────────────────────────────────────

@Composable
private fun GlassIconTile(
    item: GlassIconItem,
    index: Int,
    total: Int,
    visible: Boolean,
    tile: Dp,
    style: GlassStyle,
    agsl: Any?,
    labelColor: Color
) {
    val haptic = LocalHapticFeedback.current
    val enter = remember { Animatable(0f) }

    // Staggered entrance (glass settles, plate swings out) and quick staggered exit.
    LaunchedEffect(visible) {
        if (visible) {
            delay(index * 50L)
            enter.animateTo(1f, spring(dampingRatio = 1f, stiffness = 90f))
        } else {
            delay((total - 1 - index) * 20L)
            enter.animateTo(0f, tween(durationMillis = 180))
        }
    }

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 420f),
        label = "GlassPress"
    )

    Column(
        modifier = Modifier
            .clickable(
                enabled = visible,
                interactionSource = interaction,
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                item.onClick()
            }
            .semantics {
                contentDescription = item.contentDescription
                role = Role.Button
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(tile), contentAlignment = Alignment.Center) {
            // Plate + glass. The canvas is larger than the tile so the tilted plate can peek out.
            Canvas(modifier = Modifier.requiredSize(tile * 2.2f)) {
                drawGlassTile(agsl, tile, item.gradient, style, enter.value, press)
            }

            // Glyph rides above the glass and rises/fades in slightly after it.
            Canvas(
                modifier = Modifier
                    .size(tile * 0.44f)
                    .graphicsLayer {
                        val e = enter.value.coerceIn(0f, 1f)
                        val rise = (1f - glassEase((e - 0.25f) / 0.75f)) * 0.1f * tile.toPx()
                        translationY = rise
                        alpha = glassFade(0.3f, 0.85f, e)
                        val s = 1f - 0.08f * press
                        scaleX = s
                        scaleY = s
                    }
            ) {
                item.icon(this, Color.White)
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = item.label,
            color = labelColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.graphicsLayer {
                alpha = glassFade(0.3f, 0.85f, enter.value)
            }
        )
    }
}

// ── Motion helpers (same curves as the web component) ───────────────────────

private fun glassEase(t: Float): Float {
    val x = 1f - t.coerceIn(0f, 1f)
    return 1f - x * x * x
}

private fun glassFade(from: Float, to: Float, t: Float): Float {
    val x = ((t - from) / (to - from)).coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}

private class GlassPose(
    val glassScale: Float,
    val glassAlpha: Float,
    val plateAlpha: Float,
    val plateScale: Float,
    val angle: Float,
    val bend: Float
)

private fun glassPose(enterRaw: Float, press: Float, tilt: Float): GlassPose {
    val enter = enterRaw.coerceIn(0f, 1f)
    val swing = glassEase((enter - 0.18f) / 0.82f)
    val settle = 0.92f + 0.08f * glassEase(enter)
    return GlassPose(
        glassScale = (1f - 0.08f * press) * settle,
        glassAlpha = glassFade(0f, 0.55f, enter),
        plateAlpha = glassFade(0f, 0.35f, enter),
        plateScale = 1f - 0.08f * press,
        angle = tilt * swing,
        bend = 1f + 1.2f * (1f - glassEase(enter))
    )
}

// ── Drawing entry point ──────────────────────────────────────────────────────

private fun DrawScope.drawGlassTile(
    agsl: Any?,
    tile: Dp,
    gradient: GlassGradient,
    style: GlassStyle,
    enter: Float,
    press: Float
) {
    val tilePx = tile.toPx()
    val pose = glassPose(enter, press, style.tilt)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && agsl is GlassAgsl) {
        if (agsl.draw(this, tilePx, density, gradient, style, pose)) return
    }
    drawGlassFallback(tilePx, gradient, style, pose)
}

private fun createGlassAgsl(): Any? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
    return try {
        GlassAgsl()
    } catch (t: Throwable) {
        null
    }
}

// ── AGSL renderer (API 33+) — port of the web component's fragment shader ───

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class GlassAgsl {
    private val shader = RuntimeShader(GLASS_AGSL)
    private val brush = ShaderBrush(shader)
    private var failed = false

    fun draw(
        scope: DrawScope,
        tilePx: Float,
        density: Float,
        gradient: GlassGradient,
        style: GlassStyle,
        pose: GlassPose
    ): Boolean {
        if (failed) return false
        return try {
            val radius = min(tilePx * 0.5f * style.roundness, tilePx * 0.5f)
            val bevel = max(2f * density, tilePx * 0.26f * style.bevel.coerceIn(0f, 1f))
            val turn = (-pose.angle * Math.PI / 180.0)
            val lx = -0.55f
            val ly = -0.83f
            val ll = sqrt(lx * lx + ly * ly)

            shader.setFloatUniform("uCenter", scope.size.width / 2f, scope.size.height / 2f)
            shader.setFloatUniform("uSize", tilePx)
            shader.setFloatUniform("uRadius", radius)
            shader.setFloatUniform("uBevel", bevel)
            shader.setFloatUniform("uRim", 1.2f * density)
            shader.setFloatUniform("uStrength", tilePx * 0.3f * style.refraction * pose.bend)
            shader.setFloatUniform("uSoft", density + style.frost.coerceIn(0f, 1f) * tilePx * 0.15f)
            shader.setFloatUniform("uShine", style.shine.coerceIn(0f, 1f))
            shader.setFloatUniform("uShade", style.shade)
            shader.setFloatUniform(
                "uTint",
                style.tint.red, style.tint.green, style.tint.blue, style.tintOpacity.coerceIn(0f, 1f)
            )
            shader.setFloatUniform("uLight", lx / ll, ly / ll)
            shader.setFloatUniform("uTurn", Math.cos(turn).toFloat(), Math.sin(turn).toFloat())
            shader.setFloatUniform("uScale", pose.plateScale)
            shader.setFloatUniform("uGlassScale", pose.glassScale)
            shader.setFloatUniform("uGlassAlpha", pose.glassAlpha)
            shader.setFloatUniform("uPlateAlpha", pose.plateAlpha)
            shader.setFloatUniform("uLiftY", 0f)
            shader.setFloatUniform("uColorA", gradient.from.red, gradient.from.green, gradient.from.blue)
            shader.setFloatUniform("uColorB", gradient.to.red, gradient.to.green, gradient.to.blue)

            scope.drawRect(brush = brush)
            true
        } catch (t: Throwable) {
            failed = true
            false
        }
    }
}

private const val GLASS_AGSL = """
uniform float2 uCenter;
uniform float uSize;
uniform float uRadius;
uniform float uBevel;
uniform float uRim;
uniform float uStrength;
uniform float uSoft;
uniform float uShine;
uniform float uShade;
uniform float4 uTint;
uniform float2 uLight;
uniform float2 uTurn;
uniform float uScale;
uniform float uGlassScale;
uniform float uGlassAlpha;
uniform float uPlateAlpha;
uniform float uLiftY;
uniform float3 uColorA;
uniform float3 uColorB;

float roundBox(float2 p, float reach, float radius) {
  float2 q = abs(p) - float2(reach - radius);
  return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - radius;
}

float2 roundNormal(float2 p, float reach, float radius) {
  float2 q = abs(p) - float2(reach - radius);
  float2 s = float2(p.x < 0.0 ? -1.0 : 1.0, p.y < 0.0 ? -1.0 : 1.0);
  if (q.x > 0.0 && q.y > 0.0) {
    return normalize(q) * s;
  }
  return q.x > q.y ? float2(s.x, 0.0) : float2(0.0, s.y);
}

float4 plate(float2 p, float soft) {
  float2 o = float2(uSize * 0.5);
  float2 v = (p - o) / uScale;
  float2 q = float2(uTurn.x * v.x - uTurn.y * v.y, uTurn.y * v.x + uTurn.x * v.y) + o;
  float d = roundBox(q, uSize * 0.5, uRadius);
  float a = clamp(0.5 - d / soft, 0.0, 1.0);
  if (a <= 0.0) {
    return float4(0.0);
  }
  float t = clamp(dot(q, float2(0.70710678)) / (uSize * 1.41421356) + 0.5, 0.0, 1.0);
  return float4(mix(uColorA, uColorB, t) * a, a);
}

half4 main(float2 fragCoord) {
  float2 p = fragCoord - uCenter;
  float reach = uSize * 0.5;
  float2 g = (p - float2(0.0, uLiftY)) / uGlassScale;
  float dg = roundBox(g, reach, uRadius);
  float glass = clamp(0.5 - dg * uGlassScale, 0.0, 1.0) * uGlassAlpha;

  float4 color = plate(p, 1.0) * uPlateAlpha;
  if (color.a > 0.0) {
    float drop = roundBox(g - float2(0.0, uSize * 0.1), reach, uRadius);
    color.rgb *= 1.0 - (1.0 - smoothstep(-uSize * 0.12, uSize * 0.14, drop)) * 0.28 * uGlassAlpha;
  }

  if (glass > 0.0) {
    float inside = max(0.0, -dg);
    float t = 1.0 - clamp(inside / uBevel, 0.0, 1.0);
    float2 n = roundNormal(g, reach, uRadius);
    float2 bend = n * pow(t, 2.2);

    float4 seen = plate(p - bend * uStrength, uSoft) * uPlateAlpha;
    float luma = dot(seen.rgb, float3(0.2126, 0.7152, 0.0722));
    seen.rgb = clamp(mix(float3(luma), seen.rgb, 1.25) * 1.04, float3(0.0), float3(1.0));
    seen.rgb = min(mix(seen.rgb, uTint.rgb * seen.a, uTint.a), float3(seen.a));

    float facing = dot(n, uLight);
    float rim = clamp(inside / uRim, 0.0, 1.0);
    float shade = (max(-facing, 0.0) * 0.75 + 0.25) * t * t * rim * uShade;
    float light = clamp((max(facing, 0.0) * 0.95 + max(-facing, 0.0) * 0.35) * t * t * t * rim * uShine, 0.0, 1.0);
    seen = seen * (1.0 - shade) + float4(0.0, 0.0, 0.0, shade);
    seen = seen * (1.0 - light) + float4(light);
    color = mix(color, seen, glass);
  }
  return half4(color);
}
"""

// ── Pure-Compose fallback (API < 33, or if the shader fails) ────────────────

private fun DrawScope.drawGlassFallback(
    tilePx: Float,
    gradient: GlassGradient,
    style: GlassStyle,
    pose: GlassPose
) {
    val s = tilePx
    val cx = size.width / 2f
    val cy = size.height / 2f
    val left = cx - s / 2f
    val top = cy - s / 2f
    val corner = CornerRadius(min(s * 0.5f * style.roundness, s * 0.5f))
    val bottomRight = Offset(left + s, top + s)
    val center = Offset(cx, cy)

    // Tilted colored plate (pivot = bottom-right corner, like the web version)
    withTransform({
        scale(pose.plateScale, pose.plateScale, bottomRight)
        rotate(pose.angle, bottomRight)
    }) {
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(gradient.from, gradient.to),
                start = Offset(left, top),
                end = bottomRight
            ),
            topLeft = Offset(left, top),
            size = Size(s, s),
            cornerRadius = corner,
            alpha = pose.plateAlpha
        )
    }

    // Frosted glass tile
    withTransform({
        scale(pose.glassScale, pose.glassScale, center)
    }) {
        val a = pose.glassAlpha
        // soft drop shadow
        drawRoundRect(
            color = Color(0x33000000),
            topLeft = Offset(left, top + s * 0.08f),
            size = Size(s, s),
            cornerRadius = corner,
            alpha = a
        )
        // tint + frost
        drawRoundRect(
            color = style.tint.copy(alpha = style.tintOpacity),
            topLeft = Offset(left, top),
            size = Size(s, s),
            cornerRadius = corner,
            alpha = a
        )
        // light sweep from the top-left
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0x66FFFFFF), Color(0x0AFFFFFF), Color(0x1F000000)),
                start = Offset(left, top),
                end = bottomRight
            ),
            topLeft = Offset(left, top),
            size = Size(s, s),
            cornerRadius = corner,
            alpha = a
        )
        // bevel rim: bright top-left edge, dim bottom-right edge
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xE6FFFFFF), Color(0x14FFFFFF), Color(0x66FFFFFF)),
                start = Offset(left, top),
                end = bottomRight
            ),
            topLeft = Offset(left, top),
            size = Size(s, s),
            cornerRadius = corner,
            style = Stroke(width = max(1f, s * 0.035f)),
            alpha = a * style.shine
        )
    }
}

// ── Glyphs (white line icons on the glass, 24-unit design grid) ─────────────

private fun DrawScope.glyphStroke(k: Float) =
    Stroke(width = 1.9f * k, cap = StrokeCap.Round, join = StrokeJoin.Round)

/** Draws [block] twice: a faint offset shadow first, then the real ink. */
private inline fun DrawScope.withGlyphShadow(ink: Color, crossinline block: DrawScope.(Color) -> Unit) {
    val k = size.width / 24f
    withTransform({ translate(0f, 0.9f * k) }) { block(Color(0x40000000)) }
    block(ink)
}

private fun DrawScope.glyphCamera(ink: Color) = withGlyphShadow(ink) { c ->
    val k = size.width / 24f
    val st = glyphStroke(k)
    drawRoundRect(
        color = c,
        topLeft = Offset(3f * k, 7f * k),
        size = Size(18f * k, 12.5f * k),
        cornerRadius = CornerRadius(3.2f * k),
        style = st
    )
    val bump = Path().apply {
        moveTo(8.4f * k, 7f * k)
        lineTo(9.7f * k, 4.8f * k)
        lineTo(14.3f * k, 4.8f * k)
        lineTo(15.6f * k, 7f * k)
    }
    drawPath(bump, color = c, style = st)
    drawCircle(color = c, radius = 3.3f * k, center = Offset(12f * k, 13.3f * k), style = st)
}

private fun DrawScope.glyphPhotos(ink: Color) = withGlyphShadow(ink) { c ->
    val k = size.width / 24f
    val st = glyphStroke(k)
    drawRoundRect(
        color = c,
        topLeft = Offset(3.5f * k, 4f * k),
        size = Size(17f * k, 16f * k),
        cornerRadius = CornerRadius(3.6f * k),
        style = st
    )
    drawCircle(color = c, radius = 1.55f * k, center = Offset(9f * k, 9.3f * k))
    val hills = Path().apply {
        moveTo(4.6f * k, 17.4f * k)
        lineTo(9.4f * k, 12.8f * k)
        lineTo(13f * k, 16.2f * k)
        lineTo(15.4f * k, 13.9f * k)
        lineTo(19.4f * k, 17.6f * k)
    }
    drawPath(hills, color = c, style = st)
}

private fun DrawScope.glyphFiles(ink: Color) = withGlyphShadow(ink) { c ->
    val k = size.width / 24f
    val st = glyphStroke(k)
    val page = Path().apply {
        moveTo(7f * k, 3.5f * k)
        lineTo(13.5f * k, 3.5f * k)
        lineTo(18.5f * k, 8.5f * k)
        lineTo(18.5f * k, 19f * k)
        quadraticBezierTo(18.5f * k, 20.5f * k, 17f * k, 20.5f * k)
        lineTo(7f * k, 20.5f * k)
        quadraticBezierTo(5.5f * k, 20.5f * k, 5.5f * k, 19f * k)
        lineTo(5.5f * k, 5f * k)
        quadraticBezierTo(5.5f * k, 3.5f * k, 7f * k, 3.5f * k)
        close()
    }
    drawPath(page, color = c, style = st)
    val fold = Path().apply {
        moveTo(13.5f * k, 3.5f * k)
        lineTo(13.5f * k, 8.5f * k)
        lineTo(18.5f * k, 8.5f * k)
    }
    drawPath(fold, color = c, style = st)
    drawLine(c, Offset(8.6f * k, 13f * k), Offset(15.4f * k, 13f * k), 1.9f * k, StrokeCap.Round)
    drawLine(c, Offset(8.6f * k, 16.6f * k), Offset(13.2f * k, 16.6f * k), 1.9f * k, StrokeCap.Round)
}

private fun DrawScope.glyphPoll(ink: Color) = withGlyphShadow(ink) { c ->
    val k = size.width / 24f
    val w = 3.6f * k
    drawLine(c, Offset(6.4f * k, 19f * k), Offset(6.4f * k, 12.2f * k), w, StrokeCap.Round)
    drawLine(c, Offset(12f * k, 19f * k), Offset(12f * k, 5.4f * k), w, StrokeCap.Round)
    drawLine(c, Offset(17.6f * k, 19f * k), Offset(17.6f * k, 9.4f * k), w, StrokeCap.Round)
}

private fun DrawScope.glyphSticker(ink: Color) = withGlyphShadow(ink) { c ->
    val k = size.width / 24f
    val st = glyphStroke(k)
    val body = Path().apply {
        moveTo(7f * k, 3.5f * k)
        lineTo(17f * k, 3.5f * k)
        quadraticBezierTo(20.5f * k, 3.5f * k, 20.5f * k, 7f * k)
        lineTo(20.5f * k, 13.5f * k)
        lineTo(13.5f * k, 20.5f * k)
        lineTo(7f * k, 20.5f * k)
        quadraticBezierTo(3.5f * k, 20.5f * k, 3.5f * k, 17f * k)
        lineTo(3.5f * k, 7f * k)
        quadraticBezierTo(3.5f * k, 3.5f * k, 7f * k, 3.5f * k)
        close()
    }
    drawPath(body, color = c, style = st)
    val peel = Path().apply {
        moveTo(20.5f * k, 13.5f * k)
        lineTo(16.8f * k, 13.5f * k)
        quadraticBezierTo(13.5f * k, 13.5f * k, 13.5f * k, 16.8f * k)
        lineTo(13.5f * k, 20.5f * k)
    }
    drawPath(peel, color = c, style = st)
    val smile = Path().apply {
        moveTo(8.2f * k, 13.2f * k)
        quadraticBezierTo(10.2f * k, 15.6f * k, 12.2f * k, 13.9f * k)
    }
    drawPath(smile, color = c, style = st)
    drawCircle(color = c, radius = 1.05f * k, center = Offset(8.8f * k, 9.3f * k))
    drawCircle(color = c, radius = 1.05f * k, center = Offset(14.6f * k, 9.3f * k))
}
