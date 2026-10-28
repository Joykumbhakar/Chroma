package com.chroma.studio.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object ChromaIcons {
    private var _heartFilled: ImageVector? = null
    private var _thumbsUpFilled: ImageVector? = null
    private var _thumbsDownFilled: ImageVector? = null
    private var _pinFilled: ImageVector? = null

    val HeartFilled: ImageVector
        get() {
            if (_heartFilled != null) return _heartFilled!!
            _heartFilled = ImageVector.Builder(
                name = "HeartFilled",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                path(
                    fill = SolidColor(Color.Black),
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(12.0f, 21.35f)
                    lineTo(10.55f, 20.03f)
                    curveTo(5.4f, 15.36f, 2.0f, 12.28f, 2.0f, 8.5f)
                    curveTo(2.0f, 5.42f, 4.42f, 3.0f, 7.5f, 3.0f)
                    curveTo(9.24f, 3.0f, 10.91f, 3.81f, 12.0f, 5.09f)
                    curveTo(13.09f, 3.81f, 14.76f, 3.0f, 16.5f, 3.0f)
                    curveTo(19.58f, 3.0f, 22.0f, 5.42f, 22.0f, 8.5f)
                    curveTo(22.0f, 12.28f, 18.6f, 15.36f, 13.45f, 20.04f)
                    lineTo(12.0f, 21.35f)
                    close()
                }
            }.build()
            return _heartFilled!!
        }

    val ThumbsUpFilled: ImageVector
        get() {
            if (_thumbsUpFilled != null) return _thumbsUpFilled!!
            _thumbsUpFilled = ImageVector.Builder(
                name = "ThumbsUpFilled",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(1f, 21f)
                    horizontalLineTo(5f)
                    verticalLineTo(9f)
                    horizontalLineTo(1f)
                    verticalLineTo(21f)
                    close()
                    moveTo(23f, 10f)
                    curveTo(23f, 8.9f, 22.1f, 8f, 21f, 8f)
                    horizontalLineTo(14.69f)
                    lineTo(15.64f, 3.43f)
                    lineTo(15.67f, 3.11f)
                    curveTo(15.67f, 2.7f, 15.5f, 2.32f, 15.23f, 2.05f)
                    lineTo(14.17f, 1f)
                    lineTo(7.59f, 7.59f)
                    curveTo(7.22f, 7.95f, 7f, 8.45f, 7f, 9f)
                    verticalLineTo(19f)
                    curveTo(7f, 20.1f, 7.9f, 21f, 9f, 21f)
                    horizontalLineTo(18f)
                    curveTo(18.83f, 21f, 19.54f, 20.5f, 19.84f, 19.78f)
                    lineTo(22.86f, 12.73f)
                    curveTo(22.95f, 12.5f, 23f, 12.26f, 23f, 12f)
                    verticalLineTo(10f)
                    close()
                }
            }.build()
            return _thumbsUpFilled!!
        }

    val ThumbsDownFilled: ImageVector
        get() {
            if (_thumbsDownFilled != null) return _thumbsDownFilled!!
            _thumbsDownFilled = ImageVector.Builder(
                name = "ThumbsDownFilled",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(22f, 3f)
                    horizontalLineTo(18f)
                    verticalLineTo(15f)
                    horizontalLineTo(22f)
                    verticalLineTo(3f)
                    close()
                    moveTo(1f, 14f)
                    curveTo(1f, 15.1f, 1.9f, 16f, 3f, 16f)
                    horizontalLineTo(9.31f)
                    lineTo(8.36f, 20.57f)
                    lineTo(8.33f, 20.89f)
                    curveTo(8.33f, 21.3f, 8.5f, 21.68f, 8.77f, 21.95f)
                    lineTo(9.83f, 23f)
                    lineTo(16.41f, 16.41f)
                    curveTo(16.78f, 16.05f, 17f, 15.55f, 17f, 15f)
                    verticalLineTo(5f)
                    curveTo(17f, 3.9f, 16.1f, 3f, 15f, 3f)
                    horizontalLineTo(6f)
                    curveTo(5.17f, 3f, 4.46f, 3.5f, 4.16f, 4.22f)
                    lineTo(1.14f, 11.27f)
                    curveTo(1.05f, 11.5f, 1f, 11.74f, 1f, 12f)
                    verticalLineTo(14f)
                    close()
                }
            }.build()
            return _thumbsDownFilled!!
        }

    val PinFilled: ImageVector
        get() {
            if (_pinFilled != null) return _pinFilled!!
            _pinFilled = ImageVector.Builder(
                name = "PinFilled",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                path(fill = SolidColor(Color.Black)) {
                    moveTo(16f, 12f)
                    verticalLineTo(4f)
                    horizontalLineTo(17f)
                    verticalLineTo(2f)
                    horizontalLineTo(7f)
                    verticalLineTo(4f)
                    horizontalLineTo(8f)
                    verticalLineTo(12f)
                    lineTo(5.5f, 14f)
                    verticalLineTo(16f)
                    horizontalLineTo(11.2f)
                    verticalLineTo(22f)
                    lineTo(12f, 23f)
                    lineTo(12.8f, 22f)
                    verticalLineTo(16f)
                    horizontalLineTo(18.5f)
                    verticalLineTo(14f)
                    lineTo(16f, 12f)
                    close()
                }
            }.build()
            return _pinFilled!!
        }
}
