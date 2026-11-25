package com.chroma.studio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser

private object FluentDiamondGemGeometry {
    val facetTopRight: Path by lazy {
        PathParser().parsePathString(
            "M17.999 3C18.2398 3 18.4638 3.11546 18.6038 3.30649L18.6585 3.39288L21.922 9.42119L21.9599 9.51408L21.972 9.5539L21.9921 9.65608L21.9979 9.75C21.9979 9.81904 21.9886 9.88589 21.9711 9.94938L21.9474 10.0124H15.5L13.5 3H17.999Z"
        ).toPath()
    }
    val facetTopLeft: Path by lazy {
        PathParser().parsePathString(
            "M2.00569 9.84286L2 9.75L2.00285 9.68414L2.01606 9.59539C2.02225 9.56547 2.03015 9.53671 2.03966 9.5087L2.06248 9.45021L2.09048 9.39288L5.33943 3.39288C5.45413 3.18107 5.66229 3.03913 5.89695 3.00695L5.99895 3L10.5 3.00695L8.25 10L2.03966 9.99128C2.02352 9.94377 2.01202 9.89412 2.00569 9.84286Z"
        ).toPath()
    }
    val facetBottomRight: Path by lazy {
        PathParser().parsePathString(
            "M21.9599 9.51408L21.9221 9.42119L21.694 9H15.7657L14.9971 10.5L11.999 18.187L11.9994 21.0003L12.1236 20.9904L12.2008 20.973L12.299 20.94C12.4129 20.8917 12.5162 20.8144 12.5963 20.7081L21.8301 10.2208L21.8706 10.1683C21.876 10.1602 21.8811 10.1523 21.8862 10.144L21.9238 10.0754L21.9712 9.94938C21.9886 9.88589 21.998 9.81904 21.998 9.75L21.9921 9.65608L21.9721 9.5539L21.9599 9.51408Z"
        ).toPath()
    }
    val facetBottomLeft: Path by lazy {
        PathParser().parsePathString(
            "M11.9993 21.0003L11.899 20.9937L11.7811 20.9682C11.7209 20.9509 11.6623 20.9254 11.6075 20.8922L11.5983 20.8847C11.5512 20.8564 11.5072 20.8218 11.4672 20.7815L2.17809 10.2352L2.15185 10.2025L2.11178 10.144C2.0563 10.0544 2.01921 9.9523 2.00569 9.84286L2 9.75L2.00285 9.68414L2.01606 9.59539C2.02225 9.56547 2.03015 9.53671 2.03966 9.5087L2.06248 9.45021L2.09048 9.39288L2.30322 9H8.34375L8.998 10.5L11.9989 18.187L11.9993 21.0003Z"
        ).toPath()
    }
    val facetTopCenter: Path by lazy {
        PathParser().parsePathString(
            "M9.2857 3.52142C9.38511 3.21078 9.67387 3 10 3H14C14.3262 3 14.6149 3.21078 14.7143 3.52142L16.6275 9.5H7.37256L9.2857 3.52142Z"
        ).toPath()
    }
    val facetBottomCenter: Path by lazy {
        PathParser().parsePathString(
            "M7.53254 9L7.28568 9.77142C7.23323 9.93534 7.23875 10.1123 7.30132 10.2727L11.3013 20.5227C11.4136 20.8105 11.691 21 12 21C12.309 21 12.5864 20.8105 12.6987 20.5227L16.6987 10.2727C16.7613 10.1123 16.7668 9.93534 16.7143 9.77142L16.4675 9H7.53254Z"
        ).toPath()
    }
    val specularOverlay: Path by lazy {
        PathParser().parsePathString(
            "M17.9989 3C18.2398 3 18.4638 3.11546 18.6038 3.30649L18.6585 3.39288L21.922 9.42119L21.9599 9.51408L21.972 9.5539L21.9921 9.65608L21.9979 9.75C21.9979 9.81904 21.9886 9.88589 21.9711 9.94938L21.9237 10.0754L21.8862 10.144C21.8641 10.1796 21.839 10.2136 21.8107 10.2457L21.8705 10.1683L21.846 10.2025L12.5962 20.7081C12.5161 20.8144 12.4128 20.8917 12.2989 20.94L12.2007 20.973L12.1235 20.9904L11.9993 21.0003L11.899 20.9937L11.7811 20.9682C11.7209 20.9509 11.6623 20.9254 11.6075 20.8922L11.5983 20.8847C11.5512 20.8564 11.5072 20.8218 11.4672 20.7815L2.17809 10.2352L2.15185 10.2025L2.11178 10.144C2.0563 10.0544 2.01921 9.9523 2.00569 9.84286L2 9.75L2.00285 9.68414L2.01606 9.59539C2.02225 9.56547 2.03015 9.53671 2.03966 9.5087L2.06248 9.45021L2.09048 9.39288L5.33943 3.39288C5.45413 3.18107 5.66229 3.03913 5.89695 3.00695L5.99895 3H17.9989Z"
        ).toPath()
    }

    val brushTopRight: Brush by lazy {
        Brush.linearGradient(
            colors = listOf(Color(0xFF0FAFFF), Color(0xFF102784)),
            start = Offset(16.535f, 3f),
            end = Offset(21.0906f, 13.6471f)
        )
    }
    val brushTopLeft: Brush by lazy {
        Brush.linearGradient(
            colors = listOf(Color(0xFF9FF0F9), Color(0xFF29C3FF)),
            start = Offset(9.28571f, 0.0833332f),
            end = Offset(5.30781f, 8.95533f)
        )
    }
    val brushBottomRight: Brush by lazy {
        Brush.linearGradient(
            colors = listOf(Color(0xFF1B44B1), Color(0xFF2052CB)),
            start = Offset(24.3506f, 2.99985f),
            end = Offset(13.4623f, 19.6027f)
        )
    }
    val brushBottomLeft: Brush by lazy {
        Brush.linearGradient(
            colors = listOf(Color(0xFF0094F0), Color(0xFF6CE0FF)),
            start = Offset(3.76457f, 6.59994f),
            end = Offset(11.0503f, 20.8843f)
        )
    }
    val brushTopCenter: Brush by lazy {
        Brush.linearGradient(
            colors = listOf(Color(0xFF3BD5FF), Color(0xFF367AF2)),
            start = Offset(12f, 3f),
            end = Offset(12f, 11.125f)
        )
    }
    val brushBottomCenter: Brush by lazy {
        Brush.linearGradient(
            colors = listOf(Color(0xFF2052CB), Color(0xFF0FAFFF)),
            start = Offset(11.9936f, 4.8f),
            end = Offset(11.9936f, 21f)
        )
    }
    val brushPinkWash: Brush by lazy {
        Brush.linearGradient(
            colorStops = arrayOf(
                0.53288f to Color(0x00FF6CE8),
                1f to Color(0xFFFF6CE8)
            ),
            start = Offset(-0.0310272f, -13.9503f),
            end = Offset(15.1233f, 22.7985f)
        )
    }
}

/**
 * Draws the Fluent 3D faceted diamond gem icon scaled into the current DrawScope.
 */
internal fun DrawScope.drawFluentDiamondGem(includeSoftShadow: Boolean = false) {
    val s = size.minDimension / 24f
    withTransform({ scale(s, s, Offset.Zero) }) {
        if (includeSoftShadow) {
            withTransform({ translate(0f, 1.15f) }) {
                drawPath(
                    path = FluentDiamondGemGeometry.specularOverlay,
                    color = Color(0x45091538)
                )
            }
        }
        drawPath(path = FluentDiamondGemGeometry.facetTopRight, brush = FluentDiamondGemGeometry.brushTopRight)
        drawPath(path = FluentDiamondGemGeometry.facetTopLeft, brush = FluentDiamondGemGeometry.brushTopLeft)
        drawPath(path = FluentDiamondGemGeometry.facetBottomRight, brush = FluentDiamondGemGeometry.brushBottomRight)
        drawPath(path = FluentDiamondGemGeometry.facetBottomLeft, brush = FluentDiamondGemGeometry.brushBottomLeft)
        drawPath(path = FluentDiamondGemGeometry.facetTopCenter, brush = FluentDiamondGemGeometry.brushTopCenter)
        drawPath(path = FluentDiamondGemGeometry.facetBottomCenter, brush = FluentDiamondGemGeometry.brushBottomCenter)
        drawPath(
            path = FluentDiamondGemGeometry.specularOverlay,
            brush = FluentDiamondGemGeometry.brushPinkWash,
            alpha = 0.7f
        )
    }
}

/**
 * Shared Fluent 3D faceted diamond gem icon used for Pro and AI Generator entry points.
 */
@Composable
fun FluentDiamondGemIcon(
    modifier: Modifier = Modifier,
    includeSoftShadow: Boolean = false
) {
    Canvas(modifier = modifier) {
        drawFluentDiamondGem(includeSoftShadow = includeSoftShadow)
    }
}
