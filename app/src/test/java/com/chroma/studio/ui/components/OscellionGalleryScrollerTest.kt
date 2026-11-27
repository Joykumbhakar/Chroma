package com.chroma.studio.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OscellionGalleryScrollerTest {

    @Test
    fun `resolveTargetForFraction produces monotonic sub-pixel offsets across grid rows`() {
        val layout = buildGalleryVirtualLayout(
            groupSizes = listOf(10, 8),
            totalMediaCount = 18,
            useDateGrouping = true,
            showCameraTile = true,
            gridColumns = 3,
            mediaRowHeightPx = 300f,
            headerRowHeightPx = 120f,
            viewportHeightPx = 1200f
        )

        var previousVirtualPos = -1
        val samples = 40
        for (i in 0..samples) {
            val frac = i.toFloat() / samples.toFloat()
            val target = layout.resolveTargetForFraction(frac)
            val rowIdx = layout.rows.indexOfLast { it.startItemIndex <= target.itemIndex }
            val absY = layout.rows[rowIdx].cumulativeTopPx.toInt() + target.scrollOffsetPx
            assertTrue("Target moved backwards at sample $i ($absY < $previousVirtualPos)", absY >= previousVirtualPos)
            previousVirtualPos = absY
        }
        assertEquals(0, layout.resolveTargetForFraction(0f).itemIndex)
    }

    @Test
    fun `computeScrollFraction includes intra-row pixel offset smoothly`() {
        val layout = buildGalleryVirtualLayout(
            groupSizes = emptyList(),
            totalMediaCount = 30,
            useDateGrouping = false,
            showCameraTile = false,
            gridColumns = 3,
            mediaRowHeightPx = 200f,
            headerRowHeightPx = 100f,
            viewportHeightPx = 1000f
        )
        // 10 rows * 200 = 2000px total height; maxScroll = 1000px
        val f0 = layout.computeScrollFraction(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
        val fHalfRow = layout.computeScrollFraction(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 100)
        val fRow1 = layout.computeScrollFraction(firstVisibleItemIndex = 3, firstVisibleItemScrollOffset = 0)

        assertEquals(0f, f0, 0.0001f)
        assertEquals(0.1f, fHalfRow, 0.0001f)
        assertEquals(0.2f, fRow1, 0.0001f)
    }

    @Test
    fun `thumb grab offset avoids jump when touching inside active thumb`() {
        val thumbTop = 200f
        val thumbH = 80f
        val grabInside = computeGalleryThumbGrabOffsetPx(
            pointerY = 260f,
            currentThumbTopPx = thumbTop,
            thumbHeightPx = thumbH
        )
        assertEquals(60f, grabInside, 0.001f)
        assertEquals(0.4f, computeGalleryTrackFraction(260f, grabInside, travelPx = 500f), 0.001f)

        val tapOutside = computeGalleryThumbGrabOffsetPx(
            pointerY = 40f,
            currentThumbTopPx = thumbTop,
            thumbHeightPx = thumbH
        )
        assertEquals(40f, tapOutside, 0.001f)
    }
}
