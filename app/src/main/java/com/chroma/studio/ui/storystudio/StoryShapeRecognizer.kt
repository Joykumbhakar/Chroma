package com.chroma.studio.ui.storystudio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

enum class RecognizedShapeType(val label: String) {
    CIRCLE("Circle"),
    ELLIPSE("Ellipse"),
    ARC("Arc"),
    SQUARE("Square"),
    RECTANGLE("Rectangle"),
    TRIANGLE("Triangle"),
    LINE("Line"),
    ARROW("Arrow")
}

data class RecognizedShapeResult(
    val type: RecognizedShapeType,
    val refinedPoints: List<NormalizedPoint>,
    val confidence: Float
)

private data class Vec2(val x: Float, val y: Float)

/**
 * Apple iPad PencilKit-style geometric shape recognizer & refiner.
 * Operates in isotropic pixel coordinates `(x * canvasWidth, y * canvasHeight)` so
 * circles, squares, arcs, ellipses, triangles, lines, and arrows remain aspect-accurate
 * on any portrait/square canvas ratio.
 */
object StoryShapeRecognizer {

    fun recognize(
        rawPoints: List<NormalizedPoint>,
        canvasWidth: Float,
        canvasHeight: Float
    ): RecognizedShapeResult? {
        if (rawPoints.size < 4 || canvasWidth <= 1f || canvasHeight <= 1f) return null

        val pts = rawPoints.map { Vec2(it.x * canvasWidth, it.y * canvasHeight) }
        val minX = pts.minOf { it.x }
        val maxX = pts.maxOf { it.x }
        val minY = pts.minOf { it.y }
        val maxY = pts.maxOf { it.y }
        val bw = maxX - minX
        val bh = maxY - minY
        val maxBox = max(bw, bh)
        val totalLen = polylineLength(pts)
        if (maxBox < 18f || totalLen < 24f) return null

        val resampled = resamplePolyline(pts, 72)
        val first = resampled.first()
        val last = resampled.last()
        val endDist = dist(first, last)
        val closedThreshold = max(maxBox * 0.34f, 22f)
        val isClosed = endDist <= closedThreshold && totalLen >= maxBox * 1.9f

        fun toNormalized(poly: List<Vec2>): List<NormalizedPoint> =
            poly.map { p ->
                NormalizedPoint(
                    x = (p.x / canvasWidth).coerceIn(0.01f, 0.99f),
                    y = (p.y / canvasHeight).coerceIn(0.01f, 0.99f)
                )
            }

        if (!isClosed) {
            // 1. Check Arrow (long straight shaft + quick angled tip hook at the end)
            detectArrow(resampled)?.let { arrowPts ->
                return RecognizedShapeResult(RecognizedShapeType.ARROW, toNormalized(arrowPts), 0.90f)
            }

            // 2. Check Straight Line (axis-snapped within 6 degrees)
            val chord = dist(first, last)
            if (chord >= 20f) {
                val maxPerp = resampled.maxOf { pointToSegmentDist(it, first, last) }
                val straightness = totalLen / chord
                if (maxPerp / chord <= 0.085f && straightness <= 1.18f) {
                    val snapped = snapLineEndpoints(first, last)
                    return RecognizedShapeResult(RecognizedShapeType.LINE, toNormalized(snapped), 0.96f)
                }
            }

            // 3. Check Circular Arc (open curved sweep 42°..325° with consistent radius)
            detectArc(resampled)?.let { (arcPts, conf) ->
                return RecognizedShapeResult(RecognizedShapeType.ARC, toNormalized(arcPts), conf)
            }

            return null
        }

        // Closed shape candidates: Circle, Ellipse, Square, Rectangle, Triangle
        val closedPts = resamplePolyline(pts + first, 72)

        val circleFit = fitCircleLeastSquares(closedPts)
        val ellipseFit = fitEllipsePca(closedPts)
        val boxFit = fitOrientedRect(closedPts)
        val triFit = fitTriangle(closedPts)

        val candidates = mutableListOf<RecognizedShapeResult>()

        if (circleFit != null) {
            val (cx, cy, r, err) = circleFit
            val aspectOk = (ellipseFit?.axisRatio ?: 1f) >= 0.79f
            if (r >= 10f && err <= 0.145f && aspectOk) {
                val generated = generateEllipsePoints(cx, cy, r, r, 0f, 72)
                candidates += RecognizedShapeResult(
                    type = RecognizedShapeType.CIRCLE,
                    refinedPoints = toNormalized(generated),
                    confidence = (1f - err * 2.8f).coerceIn(0.4f, 0.99f)
                )
            }
        }

        if (ellipseFit != null && ellipseFit.err <= 0.155f && ellipseFit.axisRatio < 0.84f) {
            val generated = generateEllipsePoints(
                cx = ellipseFit.cx,
                cy = ellipseFit.cy,
                a = ellipseFit.majorA,
                b = ellipseFit.minorB,
                angleRad = ellipseFit.angleRad,
                count = 72
            )
            candidates += RecognizedShapeResult(
                type = RecognizedShapeType.ELLIPSE,
                refinedPoints = toNormalized(generated),
                confidence = (1f - ellipseFit.err * 2.6f).coerceIn(0.4f, 0.97f)
            )
        }

        if (boxFit != null && boxFit.err <= 0.105f) {
            // Bias slightly toward polygons when edges are visibly straight (corners present)
            val cornerCoverageOk = boxFit.cornersHit >= 3
            if (cornerCoverageOk) {
                val type = if (boxFit.aspectRatio >= 0.84f) {
                    RecognizedShapeType.SQUARE
                } else {
                    RecognizedShapeType.RECTANGLE
                }
                candidates += RecognizedShapeResult(
                    type = type,
                    refinedPoints = toNormalized(boxFit.cornersClosed),
                    confidence = (1f - boxFit.err * 2.2f).coerceIn(0.45f, 0.99f)
                )
            }
        }

        if (triFit != null && triFit.err <= 0.105f) {
            candidates += RecognizedShapeResult(
                type = RecognizedShapeType.TRIANGLE,
                refinedPoints = toNormalized(triFit.cornersClosed),
                confidence = (1f - triFit.err * 2.2f).coerceIn(0.45f, 0.98f)
            )
        }

        if (candidates.isEmpty()) return null

        // Compare polygon vs curve cleanly: compare mean point-to-shape RMS error!
        val bestBoxErr = boxFit?.err ?: Float.MAX_VALUE
        val bestTriErr = triFit?.err ?: Float.MAX_VALUE
        val bestCurveErr = min(circleFit?.err ?: Float.MAX_VALUE, ellipseFit?.err ?: Float.MAX_VALUE)

        // If triangle error is decisively lower than rectangle & curve, pick triangle
        val triCand = candidates.firstOrNull { it.type == RecognizedShapeType.TRIANGLE }
        if (triCand != null && bestTriErr < bestBoxErr * 0.84f && bestTriErr < bestCurveErr * 0.88f) {
            return triCand
        }

        // If box error is decisively lower than curve (e.g. square has ~0.03-0.06 rectErr vs ~0.11+ ellipseErr)
        val boxCand = candidates.firstOrNull {
            it.type == RecognizedShapeType.SQUARE || it.type == RecognizedShapeType.RECTANGLE
        }
        if (boxCand != null && bestBoxErr < bestCurveErr * 0.88f) {
            return boxCand
        }

        // Prefer Circle over Ellipse when nearly circular
        val circleCand = candidates.firstOrNull { it.type == RecognizedShapeType.CIRCLE }
        if (circleCand != null && (circleFit?.err ?: 1f) <= (ellipseFit?.err ?: 1f) * 1.15f) {
            return circleCand
        }

        return candidates.maxByOrNull { it.confidence }
    }

    private fun snapLineEndpoints(a: Vec2, b: Vec2): List<Vec2> {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val deg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))
        val normDeg = ((deg % 180.0) + 180.0) % 180.0
        val endB = when {
            normDeg < 6.5 || normDeg > 173.5 -> Vec2(b.x, a.y)
            abs(normDeg - 90.0) < 6.5 -> Vec2(a.x, b.y)
            else -> b
        }
        return listOf(a, endB)
    }

    private fun detectArrow(resampled: List<Vec2>): List<Vec2>? {
        val n = resampled.size
        if (n < 16) return null
        val start = resampled.first()
        // Try splitting around 68%..88% of arc length where shaft ends at arrow tip
        var bestTipIdx = -1
        var bestScore = Float.MAX_VALUE
        for (idx in (n * 0.65f).toInt()..(n * 0.90f).toInt()) {
            val tip = resampled[idx]
            val shaftLen = dist(start, tip)
            val tailLen = polylineLength(resampled.subList(idx, n))
            if (shaftLen < 24f || tailLen < 8f || tailLen > shaftLen * 0.65f) continue
            val shaftErr = (0..idx).maxOf { pointToSegmentDist(resampled[it], start, tip) } / shaftLen
            if (shaftErr < 0.08f && shaftErr < bestScore) {
                bestScore = shaftErr
                bestTipIdx = idx
            }
        }
        if (bestTipIdx == -1) return null

        val tip = resampled[bestTipIdx]
        val tailEnd = resampled.last()
        val shaftAngle = atan2((tip.y - start.y).toDouble(), (tip.x - start.x).toDouble())
        val tailAngle = atan2((tailEnd.y - tip.y).toDouble(), (tailEnd.x - tip.x).toDouble())
        var deltaDeg = Math.toDegrees(abs(tailAngle - shaftAngle))
        if (deltaDeg > 180.0) deltaDeg = 360.0 - deltaDeg
        // Arrowhead stroke reverses back sharply (~110°..165° relative to forward shaft direction)
        if (deltaDeg !in 105.0..172.0) return null

        val shaftLen = dist(start, tip)
        val headLen = (shaftLen * 0.22f).coerceIn(14f, 44f)
        val wingSpread = Math.toRadians(32.0)
        val backAngle = shaftAngle + PI
        val leftWing = Vec2(
            tip.x + (headLen * cos(backAngle + wingSpread)).toFloat(),
            tip.y + (headLen * sin(backAngle + wingSpread)).toFloat()
        )
        val rightWing = Vec2(
            tip.x + (headLen * cos(backAngle - wingSpread)).toFloat(),
            tip.y + (headLen * sin(backAngle - wingSpread)).toFloat()
        )
        return listOf(start, tip, leftWing, tip, rightWing)
    }

    private fun detectArc(resampled: List<Vec2>): Pair<List<Vec2>, Float>? {
        val fit = fitCircleLeastSquares(resampled) ?: return null
        val (cx, cy, r, err) = fit
        if (r < 14f || err > 0.14f) return null

        var unwrappedSweep = 0f
        var prevA = atan2(resampled[0].y - cy, resampled[0].x - cx)
        for (i in 1 until resampled.size) {
            val a = atan2(resampled[i].y - cy, resampled[i].x - cx)
            var d = a - prevA
            while (d > PI) d -= (2f * PI).toFloat()
            while (d < -PI) d += (2f * PI).toFloat()
            unwrappedSweep += d
            prevA = a
        }

        val absSweep = abs(unwrappedSweep)
        val minSweep = (PI * 0.23f).toFloat() // ~41°
        val maxSweep = (PI * 1.82f).toFloat() // ~328°
        if (absSweep < minSweep || absSweep > maxSweep) return null

        // Make sure points actually progress consistently along the arc (not zigzagging back and forth)
        val expectedArcLen = r * absSweep
        val actualLen = polylineLength(resampled)
        if (actualLen > expectedArcLen * 1.32f || actualLen < expectedArcLen * 0.72f) return null

        val startAngle = atan2(resampled.first().y - cy, resampled.first().x - cx)
        val arcCount = 48
        val out = List(arcCount) { i ->
            val t = i.toFloat() / (arcCount - 1)
            val ang = startAngle + unwrappedSweep * t
            Vec2(cx + r * cos(ang), cy + r * sin(ang))
        }
        val conf = (1f - err * 2.5f).coerceIn(0.45f, 0.96f)
        return out to conf
    }

    private data class CircleFitResult(val cx: Float, val cy: Float, val r: Float, val err: Float)

    private fun fitCircleLeastSquares(pts: List<Vec2>): CircleFitResult? {
        if (pts.size < 4) return null
        val mx = pts.map { it.x }.average()
        val my = pts.map { it.y }.average()

        var suu = 0.0
        var suv = 0.0
        var svv = 0.0
        var suuu = 0.0
        var svvv = 0.0
        var suvv = 0.0
        var svuu = 0.0

        pts.forEach { p ->
            val u = p.x - mx
            val v = p.y - my
            val uu = u * u
            val vv = v * v
            suu += uu
            svv += vv
            suv += u * v
            suuu += u * uu
            svvv += v * vv
            suvv += u * vv
            svuu += v * uu
        }

        val det = suu * svv - suv * suv
        if (abs(det) < 1e-6) return null

        val rhs1 = 0.5 * (suuu + suvv)
        val rhs2 = 0.5 * (svvv + svuu)
        val uc = (rhs1 * svv - rhs2 * suv) / det
        val vc = (suu * rhs2 - suv * rhs1) / det

        val cx = (mx + uc).toFloat()
        val cy = (my + vc).toFloat()
        val radii = pts.map { hypot((it.x - cx).toDouble(), (it.y - cy).toDouble()) }
        val r = radii.average().toFloat()
        if (r <= 1e-3f) return null

        val rms = sqrt(radii.map { (it - r) * (it - r) }.average()).toFloat()
        return CircleFitResult(cx, cy, r, rms / r)
    }

    private data class EllipseFitResult(
        val cx: Float,
        val cy: Float,
        val majorA: Float,
        val minorB: Float,
        val angleRad: Float,
        val axisRatio: Float,
        val err: Float
    )

    private fun fitEllipsePca(pts: List<Vec2>): EllipseFitResult? {
        if (pts.size < 6) return null
        val cx = pts.map { it.x }.average().toFloat()
        val cy = pts.map { it.y }.average().toFloat()

        var sxx = 0.0
        var syy = 0.0
        var sxy = 0.0
        pts.forEach { p ->
            val dx = (p.x - cx).toDouble()
            val dy = (p.y - cy).toDouble()
            sxx += dx * dx
            syy += dy * dy
            sxy += dx * dy
        }
        sxx /= pts.size
        syy /= pts.size
        sxy /= pts.size

        // Snap principal axis to 0 or 90 deg if close
        var angleRad = (0.5 * atan2(2.0 * sxy, sxx - syy)).toFloat()
        val deg = Math.toDegrees(angleRad.toDouble())
        if (abs(deg) < 8.0) angleRad = 0f
        else if (abs(abs(deg) - 90.0) < 8.0) angleRad = (PI / 2.0).toFloat()

        val ca = cos(angleRad)
        val sa = sin(angleRad)
        var maxAbsU = 0f
        var maxAbsV = 0f
        val projected = pts.map { p ->
            val dx = p.x - cx
            val dy = p.y - cy
            val u = dx * ca + dy * sa
            val v = -dx * sa + dy * ca
            maxAbsU = max(maxAbsU, abs(u))
            maxAbsV = max(maxAbsV, abs(v))
            Vec2(u, v)
        }
        val a = maxAbsU.coerceAtLeast(1f)
        val b = maxAbsV.coerceAtLeast(1f)
        val radialDiffs = projected.map { p -> abs(hypot(p.x / a, p.y / b) - 1f) }
        val err = radialDiffs.average().toFloat()
        val ratio = min(a, b) / max(a, b)
        return EllipseFitResult(cx, cy, a, b, angleRad, ratio, err)
    }

    private data class RectFitResult(
        val cornersClosed: List<Vec2>,
        val aspectRatio: Float,
        val cornersHit: Int,
        val err: Float
    )

    private fun fitOrientedRect(pts: List<Vec2>): RectFitResult? {
        if (pts.size < 8) return null
        val cx = pts.map { it.x }.average().toFloat()
        val cy = pts.map { it.y }.average().toFloat()

        // Find dominant axis via PCA and try clean 0° snap + 45° diamond candidate
        var sxx = 0.0
        var syy = 0.0
        var sxy = 0.0
        pts.forEach { p ->
            val dx = (p.x - cx).toDouble()
            val dy = (p.y - cy).toDouble()
            sxx += dx * dx; syy += dy * dy; sxy += dx * dy
        }
        val pcaAngle = (0.5 * atan2(2.0 * sxy, sxx - syy)).toFloat()
        val candidateAngles = listOf(0f, pcaAngle, (PI / 4.0).toFloat()).distinct()

        var best: RectFitResult? = null
        for (rawAngle in candidateAngles) {
            val angle = if (abs(Math.toDegrees(rawAngle.toDouble())) < 9.0) 0f else rawAngle
            val ca = cos(angle)
            val sa = sin(angle)

            var minU = Float.MAX_VALUE; var maxU = -Float.MAX_VALUE
            var minV = Float.MAX_VALUE; var maxV = -Float.MAX_VALUE
            val proj = pts.map { p ->
                val dx = p.x - cx; val dy = p.y - cy
                val u = dx * ca + dy * sa
                val v = -dx * sa + dy * ca
                minU = min(minU, u); maxU = max(maxU, u)
                minV = min(minV, v); maxV = max(maxV, v)
                Vec2(u, v)
            }
            var w = maxU - minU
            var h = maxV - minV
            val minSide = min(w, h)
            if (minSide < 12f) continue

            val ratio = minSide / max(w, h)
            // Square snap: equalize side length centered at box midpoint
            if (ratio >= 0.84f) {
                val side = (w + h) * 0.5f
                val midU = (minU + maxU) * 0.5f
                val midV = (minV + maxV) * 0.5f
                minU = midU - side * 0.5f; maxU = midU + side * 0.5f
                minV = midV - side * 0.5f; maxV = midV + side * 0.5f
                w = side; h = side
            }

            val err = proj.map { p ->
                val dLeft = abs(p.x - minU)
                val dRight = abs(maxU - p.x)
                val dTop = abs(p.y - minV)
                val dBottom = abs(maxV - p.y)
                min(min(dLeft, dRight), min(dTop, dBottom)) / minSide
            }.average().toFloat()

            val cornerThresh = minSide * 0.28f
            val localCorners = listOf(
                Vec2(minU, minV), Vec2(maxU, minV),
                Vec2(maxU, maxV), Vec2(minU, maxV)
            )
            val hits = localCorners.count { c -> proj.any { dist(it, c) <= cornerThresh } }

            fun toWorld(u: Float, v: Float) = Vec2(
                cx + u * ca - v * sa,
                cy + u * sa + v * ca
            )
            val worldCorners = (localCorners + localCorners.first()).map { toWorld(it.x, it.y) }
            val result = RectFitResult(worldCorners, ratio, hits, err)
            if (best == null || result.err < best.err) {
                best = result
            }
        }
        return best
    }

    private data class TriFitResult(val cornersClosed: List<Vec2>, val err: Float)

    private fun fitTriangle(pts: List<Vec2>): TriFitResult? {
        if (pts.size < 6) return null
        val maxBox = max(
            pts.maxOf { it.x } - pts.minOf { it.x },
            pts.maxOf { it.y } - pts.minOf { it.y }
        ).coerceAtLeast(1f)

        // Find the furthest triangle inscribed in the contour (greedy farthest-3-vertex search)
        val p0 = pts.maxByOrNull { p -> dist(p, pts.first()) } ?: return null
        val p1 = pts.maxByOrNull { p -> dist(p, p0) } ?: return null
        val p2 = pts.maxByOrNull { p -> pointToLineDist(p, p0, p1) } ?: return null

        val v0 = Vec2(p0.x, p0.y)
        val v1 = Vec2(p1.x, p1.y)
        val v2 = Vec2(p2.x, p2.y)
        // Check non-degenerate triangle
        if (pointToLineDist(v2, v0, v1) < maxBox * 0.25f) return null

        val err = pts.map { p ->
            min(
                pointToSegmentDist(p, v0, v1),
                min(pointToSegmentDist(p, v1, v2), pointToSegmentDist(p, v2, v0))
            ) / maxBox
        }.average().toFloat()

        return TriFitResult(listOf(v0, v1, v2, v0), err)
    }

    private fun generateEllipsePoints(
        cx: Float,
        cy: Float,
        a: Float,
        b: Float,
        angleRad: Float,
        count: Int
    ): List<Vec2> {
        val ca = cos(angleRad)
        val sa = sin(angleRad)
        return List(count + 1) { idx ->
            val t = (idx % count).toFloat() / count.toFloat() * (2f * PI).toFloat()
            val lx = a * cos(t)
            val ly = b * sin(t)
            Vec2(cx + lx * ca - ly * sa, cy + lx * sa + ly * ca)
        }
    }

    private fun resamplePolyline(pts: List<Vec2>, targetCount: Int): List<Vec2> {
        if (pts.size <= 2) return pts
        val total = polylineLength(pts)
        if (total < 1e-3f) return pts
        val step = total / (targetCount - 1)
        val result = ArrayList<Vec2>(targetCount)
        result.add(pts.first())

        var accum = 0f
        var i = 1
        var prev = pts.first()
        while (i < pts.size) {
            val cur = pts[i]
            val seg = dist(prev, cur)
            if (accum + seg >= step) {
                val t = ((step - accum) / seg).coerceIn(0f, 1f)
                val interp = Vec2(prev.x + t * (cur.x - prev.x), prev.y + t * (cur.y - prev.y))
                result.add(interp)
                prev = interp
                accum = 0f
                if (result.size >= targetCount) break
            } else {
                accum += seg
                prev = cur
                i++
            }
        }
        while (result.size < targetCount) {
            result.add(pts.last())
        }
        return result
    }

    private fun polylineLength(pts: List<Vec2>): Float {
        var sum = 0f
        for (i in 1 until pts.size) sum += dist(pts[i - 1], pts[i])
        return sum
    }

    private fun dist(a: Vec2, b: Vec2): Float = hypot((b.x - a.x).toDouble(), (b.y - a.y).toDouble()).toFloat()

    private fun pointToSegmentDist(p: Vec2, a: Vec2, b: Vec2): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val lenSq = dx * dx + dy * dy
        if (lenSq < 1e-6f) return dist(p, a)
        val t = (((p.x - a.x) * dx + (p.y - a.y) * dy) / lenSq).coerceIn(0f, 1f)
        return hypot((p.x - (a.x + t * dx)).toDouble(), (p.y - (a.y + t * dy)).toDouble()).toFloat()
    }

    private fun pointToLineDist(p: Vec2, a: Vec2, b: Vec2): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val len = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        if (len < 1e-6f) return dist(p, a)
        return abs(dy * p.x - dx * p.y + b.x * a.y - b.y * a.x) / len
    }
}
