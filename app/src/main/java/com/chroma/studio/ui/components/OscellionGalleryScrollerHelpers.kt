package com.chroma.studio.ui.components

import kotlin.math.roundToInt

/**
 * Lightweight descriptor of a single visual row inside [androidx.compose.foundation.lazy.grid.LazyVerticalGrid].
 */
data class GalleryRowEntry(
    val startItemIndex: Int,
    val isHeader: Boolean,
    val cumulativeTopPx: Float,
    val heightPx: Float
)

/**
 * Sub-pixel target passed to [androidx.compose.foundation.lazy.grid.LazyGridState.scrollToItem].
 */
data class GalleryScrollTarget(
    val itemIndex: Int,
    val scrollOffsetPx: Int
)

/**
 * Virtual coordinate layout mapping every visual row to cumulative pixels for buttery 120Hz fast-scrolling.
 */
data class GalleryVirtualLayout(
    val rows: List<GalleryRowEntry>,
    val totalContentHeightPx: Float,
    val maxScrollPx: Float
) {
    /**
     * Maps live grid scroll state `(firstVisibleItemIndex, firstVisibleItemScrollOffset)` into `[0f, 1f]`.
     */
    fun computeScrollFraction(
        firstVisibleItemIndex: Int,
        firstVisibleItemScrollOffset: Int
    ): Float {
        if (rows.isEmpty() || maxScrollPx <= 0f) return 0f
        val rowIdx = findRowIndexByItem(firstVisibleItemIndex)
        val rowTopPx = rows[rowIdx].cumulativeTopPx
        val currentY = (rowTopPx + firstVisibleItemScrollOffset.coerceAtLeast(0)).coerceIn(0f, maxScrollPx)
        return (currentY / maxScrollPx).coerceIn(0f, 1f)
    }

    /**
     * Maps normalized slider fraction `[0f, 1f]` into an exact `(itemIndex, scrollOffsetPx)` pair.
     */
    fun resolveTargetForFraction(fraction: Float): GalleryScrollTarget {
        if (rows.isEmpty()) return GalleryScrollTarget(0, 0)
        val clamped = fraction.coerceIn(0f, 1f)
        if (maxScrollPx <= 0f) return GalleryScrollTarget(rows.first().startItemIndex, 0)

        val targetY = clamped * maxScrollPx
        val rowIdx = findRowIndexByY(targetY)
        val row = rows[rowIdx]
        val offsetPx = (targetY - row.cumulativeTopPx)
            .roundToInt()
            .coerceIn(0, (row.heightPx.roundToInt() - 1).coerceAtLeast(0))
        return GalleryScrollTarget(
            itemIndex = row.startItemIndex,
            scrollOffsetPx = offsetPx
        )
    }

    private fun findRowIndexByItem(itemIndex: Int): Int {
        var low = 0
        var high = rows.lastIndex
        var best = 0
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (rows[mid].startItemIndex <= itemIndex) {
                best = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return best
    }

    private fun findRowIndexByY(yPx: Float): Int {
        var low = 0
        var high = rows.lastIndex
        var best = 0
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (rows[mid].cumulativeTopPx <= yPx) {
                best = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return best
    }
}

/**
 * Builds the exact virtual row table mirroring `LazyVerticalGrid` item emission in `OscellionGallery.kt`.
 */
fun buildGalleryVirtualLayout(
    groupSizes: List<Int>,
    totalMediaCount: Int,
    useDateGrouping: Boolean,
    showCameraTile: Boolean,
    gridColumns: Int,
    mediaRowHeightPx: Float,
    headerRowHeightPx: Float,
    viewportHeightPx: Float,
    topPaddingPx: Float = 0f,
    bottomPaddingPx: Float = 0f
): GalleryVirtualLayout {
    val safeCols = gridColumns.coerceAtLeast(1)
    val safeMediaH = mediaRowHeightPx.coerceAtLeast(1f)
    val safeHeaderH = headerRowHeightPx.coerceAtLeast(1f)
    val rows = ArrayList<GalleryRowEntry>()
    var yCursor = topPaddingPx.coerceAtLeast(0f)
    var itemCursor = 0

    if (useDateGrouping) {
        if (showCameraTile) {
            rows.add(GalleryRowEntry(startItemIndex = 0, isHeader = false, cumulativeTopPx = yCursor, heightPx = safeMediaH))
            yCursor += safeMediaH
            itemCursor = 1
        }
        for (groupCount in groupSizes) {
            rows.add(GalleryRowEntry(startItemIndex = itemCursor, isHeader = true, cumulativeTopPx = yCursor, heightPx = safeHeaderH))
            yCursor += safeHeaderH
            itemCursor += 1

            val mediaRows = (groupCount + safeCols - 1) / safeCols
            for (r in 0 until mediaRows) {
                val startIdx = itemCursor + r * safeCols
                rows.add(GalleryRowEntry(startItemIndex = startIdx, isHeader = false, cumulativeTopPx = yCursor, heightPx = safeMediaH))
                yCursor += safeMediaH
            }
            itemCursor += groupCount
        }
    } else {
        val totalItems = totalMediaCount + if (showCameraTile) 1 else 0
        val totalRows = (totalItems + safeCols - 1) / safeCols
        for (r in 0 until totalRows) {
            rows.add(GalleryRowEntry(startItemIndex = r * safeCols, isHeader = false, cumulativeTopPx = yCursor, heightPx = safeMediaH))
            yCursor += safeMediaH
        }
    }

    val totalHeight = yCursor + bottomPaddingPx.coerceAtLeast(0f)
    val maxScrollPx = (totalHeight - viewportHeightPx.coerceAtLeast(0f)).coerceAtLeast(0f)
    return GalleryVirtualLayout(
        rows = rows,
        totalContentHeightPx = totalHeight,
        maxScrollPx = maxScrollPx
    )
}

/**
 * Computes pointer grab offset inside the slider thumb so touching an active thumb never jumps.
 */
fun computeGalleryThumbGrabOffsetPx(
    pointerY: Float,
    currentThumbTopPx: Float,
    thumbHeightPx: Float,
    hitSlopPx: Float = 18f
): Float {
    val inThumbBounds = pointerY >= (currentThumbTopPx - hitSlopPx) &&
        pointerY <= (currentThumbTopPx + thumbHeightPx + hitSlopPx)
    return if (inThumbBounds) {
        (pointerY - currentThumbTopPx).coerceIn(0f, thumbHeightPx)
    } else {
        thumbHeightPx / 2f
    }
}

/**
 * Converts pointer Y and grab offset into clamped normalized `[0f, 1f]` track fraction.
 */
fun computeGalleryTrackFraction(
    pointerY: Float,
    grabOffsetPx: Float,
    travelPx: Float
): Float {
    if (travelPx <= 0f) return 0f
    return ((pointerY - grabOffsetPx) / travelPx).coerceIn(0f, 1f)
}
