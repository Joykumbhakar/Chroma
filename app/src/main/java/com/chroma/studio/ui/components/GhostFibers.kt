package com.chroma.studio.ui.components

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/*
 * ─────────────────────────────────────────────────────────────────────────────
 *  GhostFibers — native Jetpack Compose port of the React Bits <GhostFibers />.
 *
 *  The web version is a WebGL2 instanced renderer with a bloom chain. This port keeps
 *  the same ribbon maths (waves, twist, volume, per-fiber depth spread, fold glow,
 *  circle-of-confusion depth of field, twinkle, intro sweep, drifting dust) but projects
 *  the dots on the CPU and paints them as additive glow sprites. Only the dots that are
 *  actually on screen are drawn, so it stays cheap.
 *
 *  Defaults match the usage example in the brief:
 *    glowColor #001fff · lineColor #0070ff · secondaryColor #0038ff · layers 3 ·
 *    twist 1.2 · threads 72 · fiberStyle "dots" · depthOfField 0.85 · intro on.
 * ─────────────────────────────────────────────────────────────────────────────
 */

/** Tuning knobs, named after the React props. */
data class GhostFibersConfig(
    val glowColor: Color = Color(0xFF001FFF),
    val lineColor: Color = Color(0xFF0070FF),
    val secondaryColor: Color = Color(0xFF0038FF),
    val layers: Int = 3,
    val threads: Int = 72,
    val density: Float = 1f,
    val twist: Float = 1.2f,
    val waves: Float = 1f,
    val volume: Float = 0.7f,
    val spread: Float = 1f,
    val speed: Float = 0.5f,
    val flow: Float = 0.4f,
    val rollSpeed: Float = 0.15f,
    val scale: Float = 1f,
    /** null = automatic: 18° on landscape, 68° on portrait so the ribbons run along the tall axis. */
    val rotationDegrees: Float? = null,
    val perspective: Float = 0.65f,
    val thickness: Float = 1f,
    val depthOfField: Float = 0.85f,
    val foldGlow: Float = 1f,
    val particles: Float = 0.5f,
    val sparkle: Float = 0.5f,
    /** Soft halo around the brightest dots (stands in for the web version's bloom pass). */
    val glow: Float = 0.8f,
    val brightness: Float = 1f,
    val intro: Boolean = true
)

/**
 * Animated ghost-fiber background. Fills its parent; draw your UI on top.
 *
 * @param animate false renders one settled frame (use for "reduce motion").
 * @param paused  freezes the animation on the current frame.
 */
@Composable
fun GhostFibers(
    modifier: Modifier = Modifier,
    config: GhostFibersConfig = GhostFibersConfig(),
    animate: Boolean = true,
    paused: Boolean = false
) {
    val engine = remember(config) { GhostFibersEngine(config) }
    var tick by remember { mutableStateOf(0) }

    LaunchedEffect(engine, animate, paused) {
        if (!animate) {
            engine.settle()
            tick++
            return@LaunchedEffect
        }
        if (paused) return@LaunchedEffect
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    engine.advance(((now - last) / 1_000_000_000f).coerceAtMost(0.05f))
                }
                last = now
                tick++
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val frame = tick // subscribe this draw block to every animation frame
        if (frame < 0) return@Canvas
        drawIntoCanvas { canvas ->
            engine.draw(canvas.nativeCanvas, size.width, size.height, density)
        }
    }
}

// ── Engine ───────────────────────────────────────────────────────────────────

private const val GF_CAMERA = 7.5f
private const val GF_FOV = 0.56f
private const val GF_LENGTH = 24f
private const val GF_SPINE = 256
private const val GF_STRIDE = 21 // centre(3) + 3 twist variants x (across(3) + sheet(3))
private const val GF_INTRO = 2.6f
private const val GF_DEG = 0.017453292f
private const val GF_TINTS = 6
private const val GF_HIGHS = 4
private const val SPRITE = 64

private val GF_PLACEMENTS = arrayOf(
    floatArrayOf(0f, 0f, 0f, 0f),
    floatArrayOf(1.9f, -4.6f, -0.08f, 2.4f),
    floatArrayOf(-1.05f, 2.0f, 0.12f, 4.1f)
)
private val GF_GAIN = floatArrayOf(1f, 0.5f, 0.55f)
private val GF_TINT = floatArrayOf(0f, 0.75f, 0.35f)
private val GF_FOCAL = (1.0 / (2.0 * tan(GF_FOV / 2.0))).toFloat()

private fun gfHash(n: Float): Float {
    val v = sin(n.toDouble() * 91.3458) * 47453.5453
    return (v - floor(v)).toFloat()
}

private fun gfSmooth(e0: Float, e1: Float, x: Float): Float {
    val t = ((x - e0) / (e1 - e0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun gfMul3(a: FloatArray, b: FloatArray): FloatArray {
    val out = FloatArray(9)
    for (col in 0..2) {
        for (row in 0..2) {
            out[col * 3 + row] =
                a[row] * b[col * 3] + a[3 + row] * b[col * 3 + 1] + a[6 + row] * b[col * 3 + 2]
        }
    }
    return out
}

private fun gfOrientation(yaw: Float, pitch: Float, roll: Float): FloatArray {
    val cy = cos(yaw); val sy = sin(yaw)
    val cp = cos(pitch); val sp = sin(pitch)
    val cr = cos(roll); val sr = sin(roll)
    val ry = floatArrayOf(cy, 0f, -sy, 0f, 1f, 0f, sy, 0f, cy)
    val rx = floatArrayOf(1f, 0f, 0f, 0f, cp, sp, 0f, -sp, cp)
    val rz = floatArrayOf(cr, sr, 0f, -sr, cr, 0f, 0f, 0f, 1f)
    return gfMul3(rz, gfMul3(rx, ry))
}

private fun gfSprite(bokeh: Boolean): Bitmap {
    val px = IntArray(SPRITE * SPRITE)
    val half = SPRITE / 2f
    for (y in 0 until SPRITE) {
        for (x in 0 until SPRITE) {
            val dx = (x + 0.5f - half) / half
            val dy = (y + 0.5f - half) / half
            val rho = sqrt(dx * dx + dy * dy)
            var a = 0f
            if (rho < 1f) {
                a = if (bokeh) {
                    val disk = 1f - gfSmooth(0.82f, 1f, rho)
                    disk * (0.94f + 0.08f * gfSmooth(0.55f, 0.95f, rho))
                } else {
                    exp(-rho * rho * 3.2f) * (1f - gfSmooth(0.8f, 1f, rho))
                }
            }
            val alpha = (a.coerceIn(0f, 1f) * 255f).roundToInt()
            px[y * SPRITE + x] = (alpha shl 24) or 0x00FFFFFF
        }
    }
    return Bitmap.createBitmap(px, SPRITE, SPRITE, Bitmap.Config.ARGB_8888)
}

internal class GhostFibersEngine(private val cfg: GhostFibersConfig) {

    private val layers = cfg.layers.coerceIn(1, 3)
    private val threads = cfg.threads.coerceIn(6, 180)
    private val dots = max(8, (260f * cfg.density.coerceIn(0.2f, 3f)).roundToInt())
    private val dustCount = (420f * cfg.particles.coerceIn(0f, 2f)).roundToInt()

    var time = 0f
        private set
    private var roll = 0f
    private var flow = 0f
    private var introT = if (cfg.intro) 0f else GF_INTRO

    // Static per-fiber data
    private val fSeed = FloatArray(layers * threads)
    private val fV = FloatArray(threads)
    private val fLayer = IntArray(threads)
    private val fStrength = FloatArray(threads)
    private val fDepth = FloatArray(threads)
    private val tPhase = FloatArray(layers * threads * dots)

    // Static dust data
    private val dSeed = FloatArray(dustCount * 3)
    private val dSize = FloatArray(dustCount)

    // Per-frame spine of each ribbon
    private val spine = Array(layers) { FloatArray((GF_SPINE + 1) * GF_STRIDE) }
    private val onScreen = Array(layers) { BooleanArray(GF_SPINE + 2) }
    private val centers = FloatArray((GF_SPINE + 2) * 3)
    private val tmp = FloatArray(3)

    // Drawing resources
    private val coreSprite = gfSprite(false)
    private val bokehSprite = gfSprite(true)
    private val paints = arrayOfNulls<Paint>(GF_TINTS * GF_HIGHS)
    private val rect = RectF()
    private var canvas: android.graphics.Canvas? = null

    init {
        val perLayer = max(floor(threads / 3f), 1f)
        for (fi in 0 until threads) {
            val layer = (fi % 3) - 1
            val slot = floor(fi / 3f)
            fLayer[fi] = layer
            fV[fi] = ((slot + 0.5f) / perLayer) * 2f - 1f +
                (gfHash(fi * 3.113f + 4f) - 0.5f) * (1.6f / perLayer) +
                layer * 0.12f / perLayer
            fStrength[fi] = (0.45f + 0.55f * gfHash(fi * 5.37f + 1f)) *
                (1f - 0.28f * abs(layer) * cfg.volume)
            fDepth[fi] = gfHash(fi * 7.919f + 2f)
            for (r in 0 until layers) {
                fSeed[r * threads + fi] = gfHash(fi * 1.731f + r * 17f)
                for (b in 0 until dots) {
                    tPhase[(r * threads + fi) * dots + b] = gfHash(fi * 13.7f + b * 0.917f + r * 3f)
                }
            }
        }
        for (i in 0 until dustCount) {
            val id = i.toFloat()
            dSeed[i * 3] = gfHash(id * 1.37f + 0.3f)
            dSeed[i * 3 + 1] = gfHash(id * 2.91f + 1.7f)
            dSeed[i * 3 + 2] = gfHash(id * 4.17f + 2.9f)
            dSize[i] = 0.5f + 0.9f * gfHash(id * 7.7f)
        }
    }

    fun advance(dt: Float) {
        time += dt * cfg.speed
        roll += dt * cfg.speed * cfg.rollSpeed
        flow += dt * cfg.flow * 12f / 260f
        introT = min(GF_INTRO, introT + dt)
    }

    /** Jump to a fully-introduced still frame (reduce-motion / paused-at-rest). */
    fun settle() {
        introT = GF_INTRO
        if (time == 0f) time = 6f
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun centerLocal(s: Float, place: FloatArray, out: FloatArray) {
        val ph = place[3]
        val x = s * GF_LENGTH
        val w = cfg.waves
        out[0] = x
        out[1] = w * (0.52f * sin(x * 0.43f + time * 0.31f + ph) +
            0.26f * sin(x * 0.79f - time * 0.23f + 1.3f + ph * 0.7f)) + place[0] + x * place[2]
        out[2] = w * 0.65f * sin(x * 0.33f + time * 0.19f + 0.7f + ph * 1.3f) + place[1]
    }

    private fun buildSpine(
        r: Int, sf: Float, orient: FloatArray, w: Float, h: Float,
        fx: Float, fy: Float, dpr: Float
    ) {
        val place = GF_PLACEMENTS[r]
        val ph = place[3]
        for (i in 0..GF_SPINE + 1) {
            centerLocal(i.toFloat() / GF_SPINE - 0.5f, place, tmp)
            centers[i * 3] = tmp[0]; centers[i * 3 + 1] = tmp[1]; centers[i * 3 + 2] = tmp[2]
        }
        val sp = spine[r]
        val on = onScreen[r]
        val halfWidthWorld = 0.62f * cfg.spread * sf * 1.35f
        for (i in 0..GF_SPINE) {
            val c0x = centers[i * 3]; val c0y = centers[i * 3 + 1]; val c0z = centers[i * 3 + 2]
            var tx = centers[(i + 1) * 3] - c0x
            var ty = centers[(i + 1) * 3 + 1] - c0y
            var tz = centers[(i + 1) * 3 + 2] - c0z
            val tl = sqrt(tx * tx + ty * ty + tz * tz).coerceAtLeast(1e-6f)
            tx /= tl; ty /= tl; tz /= tl
            // n0 = normalize(up - t * dot(up, t)), b0 = cross(t, n0)
            var nx = -tx * ty; var ny = 1f - ty * ty; var nz = -tz * ty
            val nl = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-6f)
            nx /= nl; ny /= nl; nz /= nl
            val bx = ty * nz - tz * ny
            val by = tz * nx - tx * nz
            val bz = tx * ny - ty * nx

            val x = (i.toFloat() / GF_SPINE - 0.5f) * GF_LENGTH
            val baseTh = cfg.twist * 0.483f * x + roll + ph * 0.6f + 0.5f * sin(x * 0.26f + time * 0.27f + ph)

            val base = i * GF_STRIDE
            sp[base] = c0x; sp[base + 1] = c0y; sp[base + 2] = c0z
            for (v in 0..2) {
                val th = baseTh + (v - 1) * cfg.volume * 0.42f
                val ct = cos(th); val st = sin(th)
                val o = base + 3 + v * 6
                sp[o] = ct * nx + st * bx
                sp[o + 1] = ct * ny + st * by
                sp[o + 2] = ct * nz + st * bz
                sp[o + 3] = -st * nx + ct * bx
                sp[o + 4] = -st * ny + ct * by
                sp[o + 5] = -st * nz + ct * bz
            }

            // Is this part of the ribbon anywhere near the screen?
            val lx = c0x * sf; val ly = c0y * sf; val lz = c0z * sf
            val wx = orient[0] * lx + orient[3] * ly + orient[6] * lz
            val wy = orient[1] * lx + orient[4] * ly + orient[7] * lz
            val wz = orient[2] * lx + orient[5] * ly + orient[8] * lz
            val depth = GF_CAMERA - wz
            if (depth > 0.3f) {
                val px = (fx * wx / depth * 0.5f + 0.5f) * w
                val py = (0.5f - fy * wy / depth * 0.5f) * h
                val focalPx = h * GF_FOCAL / depth
                val m = halfWidthWorld * focalPx + 48f * dpr
                on[i] = px > -m && px < w + m && py > -m && py < h + m
            } else {
                on[i] = false
            }
        }
        on[GF_SPINE + 1] = false
    }

    private fun paintFor(tb: Int, hb: Int): Paint {
        val idx = tb * GF_HIGHS + hb
        paints[idx]?.let { return it }
        val tint = tb / (GF_TINTS - 1f)
        val hi = hb / (GF_HIGHS - 1f)
        val g = cfg.glowColor; val s = cfg.secondaryColor; val l = cfg.lineColor
        fun mix(a: Float, b: Float, t: Float) = a + (b - a) * t
        val r = mix(mix(g.red, s.red, tint), l.red, hi)
        val gr = mix(mix(g.green, s.green, tint), l.green, hi)
        val b = mix(mix(g.blue, s.blue, tint), l.blue, hi)
        val argb = android.graphics.Color.argb(
            255, (r * 255f).roundToInt(), (gr * 255f).roundToInt(), (b * 255f).roundToInt()
        )
        val p = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
            colorFilter = PorterDuffColorFilter(argb, PorterDuff.Mode.SRC_IN)
        }
        paints[idx] = p
        return p
    }

    private fun drawDot(
        px: Float, py: Float, radius: Float, alpha: Float,
        tint: Float, hi: Float, bokeh: Boolean
    ) {
        val c = canvas ?: return
        val tb = (tint.coerceIn(0f, 1f) * (GF_TINTS - 1)).roundToInt()
        val hb = (hi.coerceIn(0f, 1f) * (GF_HIGHS - 1)).roundToInt()
        val p = paintFor(tb, hb)
        p.alpha = (alpha.coerceIn(0f, 1f) * 255f).toInt()
        rect.set(px - radius, py - radius, px + radius, py + radius)
        c.drawBitmap(if (bokeh) bokehSprite else coreSprite, null, rect, p)
        // Soft halo on the brightest dots: stands in for the web version's bloom.
        if (cfg.glow > 0.001f && (alpha > 0.55f || hi > 0.3f)) {
            p.alpha = (alpha.coerceIn(0f, 1f) * 255f * 0.2f * cfg.glow).toInt()
            val hr = radius * 3.4f
            rect.set(px - hr, py - hr, px + hr, py + hr)
            c.drawBitmap(coreSprite, null, rect, p)
        }
    }

    // ── frame ────────────────────────────────────────────────────────────────

    fun draw(nc: android.graphics.Canvas, w: Float, h: Float, dpr: Float) {
        if (w < 2f || h < 2f) return
        canvas = nc

        val portrait = h > w
        val rotation = (cfg.rotationDegrees ?: if (portrait) 68f else 18f) * GF_DEG
        val fit = if (portrait) 1f else (w / h / 1.5f).coerceIn(0.6f, 1f)
        val sf = cfg.scale.coerceIn(0.3f, 3f) * fit
        val f = (1.0 / tan(GF_FOV / 2.0)).toFloat()
        val fx = f / (w / h)
        val fy = f
        val orient = gfOrientation(cfg.perspective * 0.85f + 0.05f * sin(time * 0.35f), 0.12f, rotation)

        val intro = (introT / GF_INTRO).coerceIn(0f, 1f)
        val introEase = 1f - (1f - intro) * (1f - intro) * (1f - intro)

        val width = 0.62f * cfg.spread
        val baseSize = 1.6f * cfg.thickness
        val focus = GF_CAMERA
        val blurCap = 22f * sqrt(56000f / (threads * dots * layers)).coerceIn(0.45f, 1f)
        val area = max(0.8f * cfg.thickness, 0.45f).let { it * it } / 0.64f
        val load = ((threads / 72f) * cfg.density * area).pow(-0.75f)
        val dof = cfg.depthOfField
        val volume = cfg.volume
        val edgeCull = 60f * dpr

        for (r in 0 until layers) {
            buildSpine(r, sf, orient, w, h, fx, fy, dpr)
            val sp = spine[r]
            val on = onScreen[r]
            val gain = GF_GAIN[r] * load * cfg.brightness
            val tintBase = GF_TINT[r]

            for (fi in 0 until threads) {
                val idx = r * threads + fi
                val seed = fSeed[idx]
                val v = fV[fi]
                val layer = fLayer[fi]
                val strength = fStrength[fi]
                val dRand = fDepth[fi]
                val variantOffset = 3 + (layer + 1) * 6
                val layerShift = layer * volume * 0.16f
                val phaseBase = idx * dots

                for (bead in 0 until dots) {
                    val sRaw = bead.toFloat() / dots + flow + seed
                    val s = (sRaw - floor(sRaw)) - 0.5f

                    val u = (s + 0.5f) * GF_SPINE
                    val i0 = min(u.toInt(), GF_SPINE - 1)
                    if (!on[i0] && !on[i0 + 1]) continue

                    val fr = u - i0
                    val b0 = i0 * GF_STRIDE
                    val b1 = b0 + GF_STRIDE

                    val cx = sp[b0] + (sp[b1] - sp[b0]) * fr
                    val cy = sp[b0 + 1] + (sp[b1 + 1] - sp[b0 + 1]) * fr
                    val cz = sp[b0 + 2] + (sp[b1 + 2] - sp[b0 + 2]) * fr
                    val ax = sp[b0 + variantOffset] + (sp[b1 + variantOffset] - sp[b0 + variantOffset]) * fr
                    val ay = sp[b0 + variantOffset + 1] + (sp[b1 + variantOffset + 1] - sp[b0 + variantOffset + 1]) * fr
                    val az = sp[b0 + variantOffset + 2] + (sp[b1 + variantOffset + 2] - sp[b0 + variantOffset + 2]) * fr
                    val hx = sp[b0 + variantOffset + 3] + (sp[b1 + variantOffset + 3] - sp[b0 + variantOffset + 3]) * fr
                    val hy = sp[b0 + variantOffset + 4] + (sp[b1 + variantOffset + 4] - sp[b0 + variantOffset + 4]) * fr
                    val hz = sp[b0 + variantOffset + 5] + (sp[b1 + variantOffset + 5] - sp[b0 + variantOffset + 5]) * fr

                    val halfWidth = width * (0.78f + 0.22f * cos(s * 3.14159f))
                    val depthOffset = (dRand - 0.5f) * 0.12f * (0.35f + 6f * s * s)
                    val across = v * halfWidth
                    val sheetOff = (depthOffset + layerShift) * halfWidth

                    val lx = (cx + ax * across + hx * sheetOff) * sf
                    val ly = (cy + ay * across + hy * sheetOff) * sf
                    val lz = (cz + az * across + hz * sheetOff) * sf

                    val wx = orient[0] * lx + orient[3] * ly + orient[6] * lz
                    val wy = orient[1] * lx + orient[4] * ly + orient[7] * lz
                    val wz = orient[2] * lx + orient[5] * ly + orient[8] * lz

                    val depth = GF_CAMERA - wz
                    if (depth < 0.3f) continue
                    val px = (fx * wx / depth * 0.5f + 0.5f) * w
                    val py = (0.5f - fy * wy / depth * 0.5f) * h
                    if (px < -edgeCull || px > w + edgeCull || py < -edgeCull || py > h + edgeCull) continue

                    val baseRadius = max(0.5f * baseSize * dpr * GF_CAMERA / max(depth, 0.1f), 0.45f)
                    val coc = min(dof * abs(1f - focus / max(depth, 0.1f)) * h * 0.035f, blurCap * dpr)
                    val radius = baseRadius + coc
                    val spill = max(10f * dpr, baseRadius)
                    val energy = (baseRadius / min(radius, spill)).pow(1.25f) *
                        min(1f, spill / radius).pow(2f)

                    // Facing / crowding (dots bunch up where the ribbon is seen edge-on)
                    val sxn = hx * orient[0] + hy * orient[3] + hz * orient[6]
                    val syn = hx * orient[1] + hy * orient[4] + hz * orient[7]
                    val szn = hx * orient[2] + hy * orient[5] + hz * orient[8]
                    val nLen = sqrt(sxn * sxn + syn * syn + szn * szn).coerceAtLeast(1e-6f)
                    val ex = -wx; val ey = -wy; val ez = GF_CAMERA - wz
                    val eLen = sqrt(ex * ex + ey * ey + ez * ez).coerceAtLeast(1e-6f)
                    val facing = abs((sxn * ex + syn * ey + szn * ez) / (nLen * eLen))

                    val focalPx = h * GF_FOCAL / depth
                    val widthPx = 2f * width * sf * focalPx
                    val stack = (widthPx * facing + 2f * baseRadius) / (widthPx + 2f * baseRadius)

                    // approximate neighbouring dot along the ribbon for the "trail" term
                    val tlx = (sp[b1] - sp[b0]) * sf
                    val tly = (sp[b1 + 1] - sp[b0 + 1]) * sf
                    val tlz = (sp[b1 + 2] - sp[b0 + 2]) * sf
                    val step = GF_SPINE.toFloat() / dots
                    val twx = (orient[0] * tlx + orient[3] * tly + orient[6] * tlz) * step
                    val twy = (orient[1] * tlx + orient[4] * tly + orient[7] * tlz) * step
                    val twz = (orient[2] * tlx + orient[5] * tly + orient[8] * tlz) * step
                    val nDepth = GF_CAMERA - (wz + twz)
                    val spacingFace = sqrt(twx * twx + twy * twy + twz * twz) * focalPx
                    val trailLen = if (nDepth > 0.3f) {
                        val nxp = (fx * (wx + twx) / nDepth * 0.5f + 0.5f) * w
                        val nyp = (0.5f - fy * (wy + twy) / nDepth * 0.5f) * h
                        val ddx = nxp - px; val ddy = nyp - py
                        sqrt(ddx * ddx + ddy * ddy)
                    } else 0f
                    val trail = (trailLen + 2f * baseRadius) / (spacingFace + 2f * baseRadius)
                    val crowd = sqrt((stack * trail).coerceIn(0.002f, 1f))

                    val fold = (1f - facing).pow(6f) * cfg.foldGlow
                    val endFade = gfSmooth(0.5f, 0.4f, abs(s)) * gfSmooth(1.6f, 4.2f, depth)
                    val appear = gfSmooth(0f, 0.12f, introEase * 1.3f - (s + 0.5f) - seed * 0.12f)

                    val tp = tPhase[phaseBase + bead]
                    val sn = sin(time * (1.2f + 2.6f * tp) * 3f + tp * 61f)
                    val twinkle = if (sn > 0.7f) sn.pow(36f) * cfg.sparkle else 0f

                    val intensity = energy * crowd * strength * endFade * appear * 0.8f * gain *
                        (1f + fold * 7f + twinkle * 5f)
                    if (intensity < 0.0005f) continue

                    val highlight = (fold * 0.55f + twinkle).coerceIn(0f, 1f)
                    val tint = (tintBase + gfSmooth(focus, focus + 5f, depth) * 0.5f).coerceIn(0f, 1f)
                    val bokeh = gfSmooth(0.25f, 0.7f, coc / max(radius, 1e-3f)) > 0.5f

                    drawDot(px, py, radius, intensity, tint, highlight, bokeh)
                }
            }
        }

        // Drifting dust
        val dustScale = 1f + (sf - 1f) * 0.5f
        val dustIntro = gfSmooth(0.3f, 1f, introEase)
        for (i in 0 until dustCount) {
            val sx = dSeed[i * 3]; val sy = dSeed[i * 3 + 1]; val sz = dSeed[i * 3 + 2]
            var x = (sx - 0.5f) * 9f
            var y = (sy - 0.5f) * 5.4f
            var z = (sz - 0.5f) * 6f - 0.6f
            x += 0.18f * sin(time * 0.21f + sx * 40f)
            y += 0.18f * sin(time * 0.17f + sy * 40f)
            z += 0.18f * sin(time * 0.13f + sz * 40f)
            x *= dustScale; y *= dustScale; z *= dustScale
            val depth = GF_CAMERA - z
            if (depth < 0.3f) continue
            val px = (fx * x / depth * 0.5f + 0.5f) * w
            val py = (0.5f - fy * y / depth * 0.5f) * h
            if (px < -edgeCull || px > w + edgeCull || py < -edgeCull || py > h + edgeCull) continue

            val size = 2.2f * cfg.thickness * dSize[i]
            val baseRadius = max(0.5f * size * dpr * GF_CAMERA / max(depth, 0.1f), 0.45f)
            val coc = min(dof * abs(1f - focus / max(depth, 0.1f)) * h * 0.035f, 22f * dpr * 1.6f)
            val radius = baseRadius + coc
            val spill = max(10f * dpr, baseRadius)
            val energy = (baseRadius / min(radius, spill)).pow(0.9f) * min(1f, spill / radius).pow(2f)
            val sn = max(sin(time * (0.8f + 1.6f * sy) * 2f + sz * 50f), 0f)
            val twinkle = sn.pow(8f)
            val intensity = energy * (0.35f + 0.65f * twinkle) * (0.4f + 0.6f * sx) *
                dustIntro * 0.8f * (1f / cfg.thickness) * cfg.brightness
            if (intensity < 0.0005f) continue
            val bokeh = gfSmooth(0.25f, 0.7f, coc / max(radius, 1e-3f)) > 0.5f
            drawDot(px, py, radius, intensity, 0.45f + 0.55f * sy, 0.3f + 0.6f * twinkle * cfg.sparkle, bokeh)
        }

        canvas = null
    }
}
