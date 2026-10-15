package com.chroma.studio.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object ChromaIcons {
    private var _heartFilled: ImageVector? = null

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
}
