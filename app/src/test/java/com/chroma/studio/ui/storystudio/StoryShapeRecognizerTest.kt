package com.chroma.studio.ui.storystudio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class StoryShapeRecognizerTest {

    private val portraitW = 1080f
    private val portraitH = 1920f

    private fun toNorm(pxPoints: List<Pair<Float, Float>>): List<NormalizedPoint> =
        pxPoints.map { (x, y) -> NormalizedPoint(x / portraitW, y / portraitH) }

    @Test
    fun recognizesCircle_onPortraitCanvas() {
        val cx = 540f
        val cy = 960f
        val r = 220f
        val pts = (0..48).map { i ->
            val a = (2.0 * PI * i) / 48.0
            val jitter = if (i % 2 == 0) 4f else -3f
            (cx + (r + jitter) * cos(a)).toFloat() to (cy + (r - jitter) * sin(a)).toFloat()
        }

        val res = StoryShapeRecognizer.recognize(toNorm(pts), portraitW, portraitH)
        assertNotNull(res)
        assertEquals(RecognizedShapeType.CIRCLE, res!!.type)
        assertTrue(res.refinedPoints.size >= 32)
    }

    @Test
    fun recognizesEllipse_whenRadiiDifferClearly() {
        val cx = 540f
        val cy = 960f
        val rx = 310f
        val ry = 140f
        val pts = (0..48).map { i ->
            val a = (2.0 * PI * i) / 48.0
            (cx + rx * cos(a)).toFloat() to (cy + ry * sin(a)).toFloat()
        }

        val res = StoryShapeRecognizer.recognize(toNorm(pts), portraitW, portraitH)
        assertNotNull(res)
        assertEquals(RecognizedShapeType.ELLIPSE, res!!.type)
        assertTrue(res.refinedPoints.size >= 32)
    }

    @Test
    fun recognizesArc_forPartialCurvedSweep() {
        val cx = 540f
        val cy = 960f
        val r = 240f
        val pts = (0..24).map { i ->
            val a = -0.75 * PI + (1.05 * PI * i / 24.0)
            (cx + r * cos(a)).toFloat() to (cy + r * sin(a)).toFloat()
        }

        val res = StoryShapeRecognizer.recognize(toNorm(pts), portraitW, portraitH)
        assertNotNull(res)
        assertEquals(RecognizedShapeType.ARC, res!!.type)
        assertTrue(res.refinedPoints.size >= 12)
    }

    @Test
    fun recognizesSquare_withSharpCorners() {
        val left = 340f
        val top = 760f
        val size = 400f
        val pts = buildPolygonSamples(
            listOf(
                left to top,
                (left + size) to top,
                (left + size) to (top + size),
                left to (top + size),
                left to top
            ),
            stepsPerEdge = 12
        )

        val res = StoryShapeRecognizer.recognize(toNorm(pts), portraitW, portraitH)
        assertNotNull(res)
        assertEquals(RecognizedShapeType.SQUARE, res!!.type)
        assertEquals(5, res.refinedPoints.size)
    }

    @Test
    fun recognizesRectangle_whenAspectDiffers() {
        val left = 220f
        val top = 780f
        val w = 620f
        val h = 280f
        val pts = buildPolygonSamples(
            listOf(
                left to top,
                (left + w) to top,
                (left + w) to (top + h),
                left to (top + h),
                left to top
            ),
            stepsPerEdge = 12
        )

        val res = StoryShapeRecognizer.recognize(toNorm(pts), portraitW, portraitH)
        assertNotNull(res)
        assertEquals(RecognizedShapeType.RECTANGLE, res!!.type)
        assertEquals(5, res.refinedPoints.size)
    }

    @Test
    fun recognizesTriangle_fromThreeSides() {
        val pts = buildPolygonSamples(
            listOf(
                540f to 660f,
                780f to 1120f,
                300f to 1120f,
                540f to 660f
            ),
            stepsPerEdge = 14
        )

        val res = StoryShapeRecognizer.recognize(toNorm(pts), portraitW, portraitH)
        assertNotNull(res)
        assertEquals(RecognizedShapeType.TRIANGLE, res!!.type)
        assertEquals(4, res.refinedPoints.size)
    }

    @Test
    fun recognizesLine_andSnapsNearHorizontal() {
        val pts = (0..18).map { i ->
            val t = i / 18f
            val x = 220f + 620f * t
            val y = 960f + 12f * t // ~1.1 deg tilt -> should snap flat horizontally
            x to y
        }

        val res = StoryShapeRecognizer.recognize(toNorm(pts), portraitW, portraitH)
        assertNotNull(res)
        assertEquals(RecognizedShapeType.LINE, res!!.type)
        assertEquals(2, res.refinedPoints.size)
        assertEquals(res.refinedPoints[0].y, res.refinedPoints[1].y, 0.0001f)
    }

    @Test
    fun recognizesArrow_fromShaftAndHook() {
        val shaft = (0..22).map { i ->
            val t = i / 22f
            (240f + 540f * t) to 960f
        }
        val tipX = 780f
        val tipY = 960f
        val hook = (1..8).map { i ->
            val t = i / 8f
            (tipX - 95f * t) to (tipY - 70f * t)
        }

        val res = StoryShapeRecognizer.recognize(toNorm(shaft + hook), portraitW, portraitH)
        assertNotNull(res)
        assertEquals(RecognizedShapeType.ARROW, res!!.type)
        assertEquals(5, res.refinedPoints.size)
    }

    @Test
    fun returnsNull_forTinyStrokeOrInsufficientPoints() {
        val tiny = listOf(0.5f to 0.5f, 0.501f to 0.501f, 0.502f to 0.502f)
        assertNull(StoryShapeRecognizer.recognize(toNorm(tiny), portraitW, portraitH))
    }

    private fun buildPolygonSamples(
        vertices: List<Pair<Float, Float>>,
        stepsPerEdge: Int
    ): List<Pair<Float, Float>> {
        val out = mutableListOf<Pair<Float, Float>>()
        for (i in 0 until vertices.lastIndex) {
            val (x0, y0) = vertices[i]
            val (x1, y1) = vertices[i + 1]
            for (s in 0 until stepsPerEdge) {
                val t = s / stepsPerEdge.toFloat()
                out += (x0 + (x1 - x0) * t) to (y0 + (y1 - y0) * t)
            }
        }
        out += vertices.last()
        return out
    }
}
