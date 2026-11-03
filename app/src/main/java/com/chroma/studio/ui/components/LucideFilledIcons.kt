package com.chroma.studio.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Solid Filled Lucide Icons for Settings Sections.
 * Crafted with exact 24x24 Lucide geometric proportions, smooth curves,
 * and solid white silhouettes to pair with One UI squircle badges.
 */
object LucideFilled {

    // ── Profile & Identity ──────────────────────────────────────────────────
    val User: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideFilled.User",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Head (solid circle)
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 2.5f)
                arcTo(4.5f, 4.5f, 0f, true, true, 12f, 11.5f)
                arcTo(4.5f, 4.5f, 0f, true, true, 12f, 2.5f)
                close()
            }
            // Torso (solid shoulders & chest)
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 13.5f)
                curveTo(7.5f, 13.5f, 4f, 16.5f, 4f, 20f)
                arcTo(1.5f, 1.5f, 0f, false, false, 5.5f, 21.5f)
                horizontalLineTo(18.5f)
                arcTo(1.5f, 1.5f, 0f, false, false, 20f, 20f)
                curveTo(20f, 16.5f, 16.5f, 13.5f, 12f, 13.5f)
                close()
            }
        }.build()
    }

    // ── Security & Access ───────────────────────────────────────────────────
    val ShieldCheck: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideFilled.ShieldCheck",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                pathFillType = PathFillType.EvenOdd
            ) {
                // Shield contour
                moveTo(12f, 2f)
                lineTo(19.2f, 5.2f)
                arcTo(1.5f, 1.5f, 0f, false, true, 20f, 6.6f)
                verticalLineTo(11.2f)
                curveTo(20f, 16.8f, 16.6f, 20.6f, 12.3f, 22.2f)
                arcTo(1f, 1f, 0f, false, true, 11.7f, 22.2f)
                curveTo(7.4f, 20.6f, 4f, 16.8f, 4f, 11.2f)
                verticalLineTo(6.6f)
                arcTo(1.5f, 1.5f, 0f, false, true, 4.8f, 5.2f)
                close()

                // Checkmark cut-out
                moveTo(9f, 11.5f)
                lineTo(11f, 14f)
                lineTo(15.5f, 9.2f)
                lineTo(16.7f, 10.4f)
                lineTo(11f, 16.2f)
                lineTo(7.8f, 12.7f)
                close()
            }
        }.build()
    }

    // ── Privacy & Interaction ───────────────────────────────────────────────
    val Lock: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideFilled.Lock",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Shackle (stroke)
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.4f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(7.5f, 11f)
                verticalLineTo(7f)
                curveTo(7.5f, 4.5f, 9.5f, 2.5f, 12f, 2.5f)
                curveTo(14.5f, 2.5f, 16.5f, 4.5f, 16.5f, 7f)
                verticalLineTo(11f)
            }
            // Padlock Body with keyhole cut-out
            path(
                fill = SolidColor(Color.White),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(7f, 10f)
                horizontalLineTo(17f)
                arcTo(3f, 3f, 0f, false, true, 20f, 13f)
                verticalLineTo(19f)
                arcTo(3f, 3f, 0f, false, true, 17f, 22f)
                horizontalLineTo(7f)
                arcTo(3f, 3f, 0f, false, true, 4f, 19f)
                verticalLineTo(13f)
                arcTo(3f, 3f, 0f, false, true, 7f, 10f)
                close()

                // Keyhole cut-out
                moveTo(12f, 14f)
                arcTo(1.5f, 1.5f, 0f, true, true, 12f, 17f)
                arcTo(1.5f, 1.5f, 0f, true, true, 12f, 14f)
                close()
            }
        }.build()
    }

    // ── Notifications & Alerts ──────────────────────────────────────────────
    val Bell: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideFilled.Bell",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 2.5f)
                arcTo(1.5f, 1.5f, 0f, false, false, 10.5f, 4f)
                verticalLineTo(4.3f)
                curveTo(7.5f, 5.2f, 5.5f, 8f, 5.5f, 11.5f)
                verticalLineTo(16f)
                lineTo(4f, 18f)
                arcTo(0.8f, 0.8f, 0f, false, false, 4.7f, 19f)
                horizontalLineTo(19.3f)
                arcTo(0.8f, 0.8f, 0f, false, false, 20f, 18f)
                lineTo(18.5f, 16f)
                verticalLineTo(11.5f)
                curveTo(18.5f, 8f, 16.5f, 5.2f, 13.5f, 4.3f)
                verticalLineTo(4f)
                arcTo(1.5f, 1.5f, 0f, false, false, 12f, 2.5f)
                close()
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(10f, 20f)
                curveTo(10f, 21.1f, 10.9f, 22f, 12f, 22f)
                curveTo(13.1f, 22f, 14f, 21.1f, 14f, 20f)
                close()
            }
        }.build()
    }

    // ── Archived Creatives ───────────────────────────────────────────────────
    val Archive: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideFilled.Archive",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Top lid
            path(fill = SolidColor(Color.White)) {
                moveTo(5f, 3f)
                horizontalLineTo(19f)
                arcTo(2f, 2f, 0f, false, true, 21f, 5f)
                verticalLineTo(6.5f)
                arcTo(1.5f, 1.5f, 0f, false, true, 19.5f, 8f)
                horizontalLineTo(4.5f)
                arcTo(1.5f, 1.5f, 0f, false, true, 3f, 6.5f)
                verticalLineTo(5f)
                arcTo(2f, 2f, 0f, false, true, 5f, 3f)
                close()
            }
            // Bottom box with handle cut-out
            path(
                fill = SolidColor(Color.White),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(4f, 9.5f)
                horizontalLineTo(20f)
                verticalLineTo(19f)
                arcTo(2.5f, 2.5f, 0f, false, true, 17.5f, 21.5f)
                horizontalLineTo(6.5f)
                arcTo(2.5f, 2.5f, 0f, false, true, 4f, 19f)
                close()

                // Cut-out handle
                moveTo(10f, 13f)
                horizontalLineTo(14f)
                arcTo(1f, 1f, 0f, false, true, 15f, 14f)
                verticalLineTo(14.5f)
                arcTo(1f, 1f, 0f, false, true, 14f, 15.5f)
                horizontalLineTo(10f)
                arcTo(1f, 1f, 0f, false, true, 9f, 14.5f)
                verticalLineTo(14f)
                arcTo(1f, 1f, 0f, false, true, 10f, 13f)
                close()
            }
        }.build()
    }

    // ── Your Activity / History ──────────────────────────────────────────────
    val History: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideFilled.History",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                pathFillType = PathFillType.EvenOdd
            ) {
                // Circle body
                moveTo(12f, 3f)
                arcTo(9f, 9f, 0f, true, true, 12f, 21f)
                arcTo(9f, 9f, 0f, true, true, 12f, 3f)
                close()

                // Clock hands cut-out
                moveTo(11.2f, 7.5f)
                horizontalLineTo(12.8f)
                verticalLineTo(12f)
                horizontalLineTo(16.5f)
                verticalLineTo(13.5f)
                horizontalLineTo(11.2f)
                close()
            }
            // Rewind arrow tip
            path(fill = SolidColor(Color.White)) {
                moveTo(3f, 7.5f)
                lineTo(7f, 4.5f)
                verticalLineTo(10.5f)
                close()
            }
        }.build()
    }

    // ── Download Data ───────────────────────────────────────────────────────
    val Download: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideFilled.Download",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Solid arrow down
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 16.5f)
                lineTo(6f, 10.5f)
                horizontalLineTo(10f)
                verticalLineTo(3f)
                arcTo(1f, 1f, 0f, false, true, 11f, 2f)
                horizontalLineTo(13f)
                arcTo(1f, 1f, 0f, false, true, 14f, 3f)
                verticalLineTo(10.5f)
                horizontalLineTo(18f)
                close()
            }
            // Bottom tray
            path(fill = SolidColor(Color.White)) {
                moveTo(4f, 15f)
                verticalLineTo(19f)
                arcTo(2.5f, 2.5f, 0f, false, false, 6.5f, 21.5f)
                horizontalLineTo(17.5f)
                arcTo(2.5f, 2.5f, 0f, false, false, 20f, 19f)
                verticalLineTo(15f)
                arcTo(1f, 1f, 0f, false, false, 19f, 14f)
                arcTo(1f, 1f, 0f, false, false, 18f, 15f)
                verticalLineTo(19f)
                arcTo(0.5f, 0.5f, 0f, false, true, 17.5f, 19.5f)
                horizontalLineTo(6.5f)
                arcTo(0.5f, 0.5f, 0f, false, true, 6f, 19f)
                verticalLineTo(15f)
                arcTo(1f, 1f, 0f, false, false, 5f, 14f)
                arcTo(1f, 1f, 0f, false, false, 4f, 15f)
                close()
            }
        }.build()
    }

    // ── Privacy Policy & Terms ──────────────────────────────────────────────
    val FileText: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideFilled.FileText",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(5f, 2f)
                horizontalLineTo(14f)
                lineTo(20f, 8f)
                verticalLineTo(20f)
                arcTo(2.5f, 2.5f, 0f, false, true, 17.5f, 22.5f)
                horizontalLineTo(6.5f)
                arcTo(2.5f, 2.5f, 0f, false, true, 4f, 20f)
                verticalLineTo(3f)
                arcTo(1f, 1f, 0f, false, true, 5f, 2f)
                close()

                // Cut-out lines
                moveTo(8f, 12f)
                horizontalLineTo(16f)
                verticalLineTo(13.5f)
                horizontalLineTo(8f)
                close()

                moveTo(8f, 15.5f)
                horizontalLineTo(16f)
                verticalLineTo(17f)
                horizontalLineTo(8f)
                close()

                moveTo(8f, 19f)
                horizontalLineTo(13f)
                verticalLineTo(20.5f)
                horizontalLineTo(8f)
                close()
            }
        }.build()
    }

    // ── Sign Out ────────────────────────────────────────────────────────────
    val LogOut: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideFilled.LogOut",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(4f, 3f)
                arcTo(1.5f, 1.5f, 0f, false, true, 5.5f, 1.5f)
                horizontalLineTo(11f)
                arcTo(1.5f, 1.5f, 0f, false, true, 12.5f, 3f)
                verticalLineTo(6f)
                horizontalLineTo(10f)
                verticalLineTo(4f)
                horizontalLineTo(6.5f)
                verticalLineTo(20f)
                horizontalLineTo(10f)
                verticalLineTo(18f)
                horizontalLineTo(12.5f)
                verticalLineTo(21f)
                arcTo(1.5f, 1.5f, 0f, false, true, 11f, 22.5f)
                horizontalLineTo(5.5f)
                arcTo(1.5f, 1.5f, 0f, false, true, 4f, 21f)
                close()
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(16f, 8f)
                lineTo(20.5f, 12f)
                lineTo(16f, 16f)
                verticalLineTo(13.5f)
                horizontalLineTo(10f)
                verticalLineTo(10.5f)
                horizontalLineTo(16f)
                close()
            }
        }.build()
    }

    // ── Quiet Hours / Moon ──────────────────────────────────────────────────
    val Moon: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideFilled.Moon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(21f, 12.8f)
                arcTo(9f, 9f, 0f, true, true, 11.2f, 3f)
                arcTo(7f, 7f, 0f, false, false, 21f, 12.8f)
                close()
            }
        }.build()
    }

    // ── Heart / Reactions ───────────────────────────────────────────────────
    val Heart: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideFilled.Heart",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 21.35f)
                lineTo(10.55f, 20.03f)
                curveTo(5.4f, 15.36f, 2f, 12.28f, 2f, 8.5f)
                curveTo(2f, 5.42f, 4.42f, 3f, 7.5f, 3f)
                curveTo(9.24f, 3f, 10.91f, 3.81f, 12f, 5.09f)
                curveTo(13.09f, 3.81f, 14.76f, 3f, 16.5f, 3f)
                curveTo(19.58f, 3f, 22f, 5.42f, 22f, 8.5f)
                curveTo(22f, 12.28f, 18.6f, 15.36f, 13.45f, 20.04f)
                close()
            }
        }.build()
    }

    // ── Comments / MessageCircle ────────────────────────────────────────────
    val MessageCircle: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideFilled.MessageCircle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 2f)
                curveTo(6.5f, 2f, 2f, 6.2f, 2f, 11.5f)
                curveTo(2f, 14.5f, 3.5f, 17.2f, 6f, 18.9f)
                verticalLineTo(22f)
                lineTo(9.5f, 20.2f)
                curveTo(10.3f, 20.5f, 11.1f, 20.6f, 12f, 20.6f)
                curveTo(17.5f, 20.6f, 22f, 16.4f, 22f, 11.1f)
                curveTo(22f, 6.2f, 17.5f, 2f, 12f, 2f)
                close()
            }
        }.build()
    }

    // ── AI Tips / Sparkles ──────────────────────────────────────────────────
    val Sparkles: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideFilled.Sparkles",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 2f)
                curveTo(12f, 7f, 14.5f, 9.5f, 19.5f, 9.5f)
                curveTo(14.5f, 9.5f, 12f, 12f, 12f, 17f)
                curveTo(12f, 12f, 9.5f, 9.5f, 4.5f, 9.5f)
                curveTo(9.5f, 9.5f, 12f, 7f, 12f, 2f)
                close()
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(19f, 14f)
                curveTo(19f, 16.5f, 20f, 17.5f, 22.5f, 17.5f)
                curveTo(20f, 17.5f, 19f, 18.5f, 19f, 21f)
                curveTo(19f, 18.5f, 18f, 17.5f, 15.5f, 17.5f)
                curveTo(18f, 17.5f, 19f, 16.5f, 19f, 14f)
                close()
            }
        }.build()
    }

    // ── Flash Videos / Video Camera ─────────────────────────────────────────
    val Video: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideFilled.Video",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(3f, 7f)
                horizontalLineTo(14f)
                arcTo(2f, 2f, 0f, false, true, 16f, 9f)
                verticalLineTo(15f)
                arcTo(2f, 2f, 0f, false, true, 14f, 17f)
                horizontalLineTo(3f)
                arcTo(2f, 2f, 0f, false, true, 1f, 15f)
                verticalLineTo(9f)
                arcTo(2f, 2f, 0f, false, true, 3f, 7f)
                close()
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(17f, 10.5f)
                lineTo(22f, 7.5f)
                arcTo(1f, 1f, 0f, false, true, 23.5f, 8.4f)
                verticalLineTo(15.6f)
                arcTo(1f, 1f, 0f, false, true, 22f, 16.5f)
                lineTo(17f, 13.5f)
                close()
            }
        }.build()
    }

    // ── Creator Collaborations / Users ──────────────────────────────────────
    val Users: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideFilled.Users",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(9f, 3.5f)
                arcTo(3.5f, 3.5f, 0f, true, true, 9f, 10.5f)
                arcTo(3.5f, 3.5f, 0f, true, true, 9f, 3.5f)
                close()
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(9f, 12f)
                curveTo(5.5f, 12f, 2.5f, 14.2f, 2.5f, 17.5f)
                arcTo(1.5f, 1.5f, 0f, false, false, 4f, 19f)
                horizontalLineTo(14f)
                arcTo(1.5f, 1.5f, 0f, false, false, 15.5f, 17.5f)
                curveTo(15.5f, 14.2f, 12.5f, 12f, 9f, 12f)
                close()
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(16.5f, 5.5f)
                arcTo(2.8f, 2.8f, 0f, true, true, 16.5f, 11.1f)
                arcTo(2.8f, 2.8f, 0f, true, true, 16.5f, 5.5f)
                close()
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(16f, 13f)
                curveTo(18.5f, 13.2f, 20.5f, 15f, 20.5f, 17.5f)
                arcTo(1.5f, 1.5f, 0f, false, true, 19.5f, 19f)
                horizontalLineTo(16.5f)
                verticalLineTo(17.5f)
                curveTo(16.5f, 15.5f, 15.2f, 13.8f, 13.3f, 13f)
                curveTo(14.2f, 12.9f, 15.1f, 12.9f, 16f, 13f)
                close()
            }
        }.build()
    }

    // ── New Followers / UserPlus ────────────────────────────────────────────
    val UserPlus: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideFilled.UserPlus",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(9f, 3f)
                arcTo(4f, 4f, 0f, true, true, 9f, 11f)
                arcTo(4f, 4f, 0f, true, true, 9f, 3f)
                close()
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(9f, 13f)
                curveTo(5f, 13f, 2f, 15.5f, 2f, 19f)
                arcTo(1.5f, 1.5f, 0f, false, false, 3.5f, 20.5f)
                horizontalLineTo(14.5f)
                arcTo(1.5f, 1.5f, 0f, false, false, 16f, 19f)
                curveTo(16f, 15.5f, 13f, 13f, 9f, 13f)
                close()
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.4f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(19f, 8f)
                verticalLineTo(14f)
                moveTo(16f, 11f)
                horizontalLineTo(22f)
            }
        }.build()
    }

    // ── Remixes & Stitches / Layers ─────────────────────────────────────────
    val Layers: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideFilled.Layers",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 2.5f)
                lineTo(21.5f, 7.5f)
                lineTo(12f, 12.5f)
                lineTo(2.5f, 7.5f)
                close()
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.4f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 12f)
                lineTo(12f, 17f)
                lineTo(21f, 12f)
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.4f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 16.5f)
                lineTo(12f, 21.5f)
                lineTo(21f, 16.5f)
            }
        }.build()
    }

    // ── Mentions & Tags / AtSign ────────────────────────────────────────────
    val AtSign: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideFilled.AtSign",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(16f, 8f)
                verticalLineTo(12.5f)
                arcTo(2.5f, 2.5f, 0f, false, false, 18.5f, 15f)
                curveTo(20.5f, 15f, 21f, 13f, 21f, 12f)
                arcTo(9f, 9f, 0f, true, false, 9f, 20.5f)
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 8.5f)
                arcTo(3.5f, 3.5f, 0f, true, true, 12f, 15.5f)
                arcTo(3.5f, 3.5f, 0f, true, true, 12f, 8.5f)
                close()
            }
        }.build()
    }

    // ── Weekly Digest / Mail ────────────────────────────────────────────────
    val Mail: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideFilled.Mail",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(3.5f, 4f)
                horizontalLineTo(20.5f)
                arcTo(2f, 2f, 0f, false, true, 22.5f, 6f)
                verticalLineTo(18f)
                arcTo(2f, 2f, 0f, false, true, 20.5f, 20f)
                horizontalLineTo(3.5f)
                arcTo(2f, 2f, 0f, false, true, 1.5f, 18f)
                verticalLineTo(6f)
                arcTo(2f, 2f, 0f, false, true, 3.5f, 4f)
                close()

                moveTo(4f, 6.5f)
                lineTo(12f, 12.5f)
                lineTo(20f, 6.5f)
                lineTo(19f, 5.5f)
                lineTo(12f, 10.8f)
                lineTo(5f, 5.5f)
                close()
            }
        }.build()
    }

    // ── Notification Sound / Volume2 ────────────────────────────────────────
    val Volume2: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideFilled.Volume2",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(11f, 4.5f)
                lineTo(6.5f, 8f)
                horizontalLineTo(3f)
                arcTo(1.5f, 1.5f, 0f, false, false, 1.5f, 9.5f)
                verticalLineTo(14.5f)
                arcTo(1.5f, 1.5f, 0f, false, false, 3f, 16f)
                horizontalLineTo(6.5f)
                lineTo(11f, 19.5f)
                arcTo(1f, 1f, 0f, false, false, 12.5f, 18.7f)
                verticalLineTo(5.3f)
                arcTo(1f, 1f, 0f, false, false, 11f, 4.5f)
                close()
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.4f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(15.5f, 8.5f)
                arcTo(5f, 5f, 0f, false, true, 15.5f, 15.5f)
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.4f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(18.5f, 5.5f)
                arcTo(9f, 9f, 0f, false, true, 18.5f, 18.5f)
            }
        }.build()
    }

    // ── Vibration / Smartphone ──────────────────────────────────────────────
    val Smartphone: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideFilled.Smartphone",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(7f, 2f)
                horizontalLineTo(17f)
                arcTo(2.5f, 2.5f, 0f, false, true, 19.5f, 4.5f)
                verticalLineTo(19.5f)
                arcTo(2.5f, 2.5f, 0f, false, true, 17f, 22f)
                horizontalLineTo(7f)
                arcTo(2.5f, 2.5f, 0f, false, true, 4.5f, 19.5f)
                verticalLineTo(4.5f)
                arcTo(2.5f, 2.5f, 0f, false, true, 7f, 2f)
                close()

                moveTo(7f, 5.5f)
                horizontalLineTo(17f)
                verticalLineTo(17f)
                horizontalLineTo(7f)
                close()
            }
        }.build()
    }

    // ── Delete / Trash2 ─────────────────────────────────────────────────────
    val Trash2: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideFilled.Trash2",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(9f, 3f)
                horizontalLineTo(15f)
                arcTo(1f, 1f, 0f, false, true, 16f, 4f)
                verticalLineTo(5f)
                horizontalLineTo(21f)
                verticalLineTo(6.5f)
                horizontalLineTo(3f)
                verticalLineTo(5f)
                horizontalLineTo(8f)
                verticalLineTo(4f)
                arcTo(1f, 1f, 0f, false, true, 9f, 3f)
                close()
            }
            path(
                fill = SolidColor(Color.White),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(5f, 8f)
                horizontalLineTo(19f)
                lineTo(17.5f, 20.5f)
                arcTo(2f, 2f, 0f, false, true, 15.5f, 22f)
                horizontalLineTo(8.5f)
                arcTo(2f, 2f, 0f, false, true, 6.5f, 20.5f)
                close()

                moveTo(10f, 11f)
                horizontalLineTo(11.2f)
                verticalLineTo(18.5f)
                horizontalLineTo(10f)
                close()

                moveTo(12.8f, 11f)
                horizontalLineTo(14f)
                verticalLineTo(18.5f)
                horizontalLineTo(12.8f)
                close()
            }
        }.build()
    }

    // ── Theme / Palette ─────────────────────────────────────────────────────
    val Palette: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideFilled.Palette",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(12f, 2f)
                curveTo(6.5f, 2f, 2f, 6.5f, 2f, 12f)
                curveTo(2f, 17f, 5.5f, 21.5f, 10.5f, 21.9f)
                curveTo(11.5f, 22f, 12.5f, 21.2f, 12.5f, 20.2f)
                curveTo(12.5f, 19.5f, 12.2f, 18.9f, 12.2f, 18.2f)
                curveTo(12.2f, 17f, 13.2f, 16f, 14.5f, 16f)
                horizontalLineTo(16.5f)
                curveTo(19.5f, 16f, 22f, 13.5f, 22f, 10.5f)
                curveTo(22f, 5.8f, 17.5f, 2f, 12f, 2f)
                close()

                moveTo(6.5f, 10.5f)
                arcTo(1.2f, 1.2f, 0f, true, true, 6.5f, 12.9f)
                arcTo(1.2f, 1.2f, 0f, true, true, 6.5f, 10.5f)
                close()

                moveTo(9f, 6.5f)
                arcTo(1.2f, 1.2f, 0f, true, true, 9f, 8.9f)
                arcTo(1.2f, 1.2f, 0f, true, true, 9f, 6.5f)
                close()

                moveTo(14.5f, 6.5f)
                arcTo(1.2f, 1.2f, 0f, true, true, 14.5f, 8.9f)
                arcTo(1.2f, 1.2f, 0f, true, true, 14.5f, 6.5f)
                close()

                moveTo(17.5f, 10.5f)
                arcTo(1.2f, 1.2f, 0f, true, true, 17.5f, 12.9f)
                arcTo(1.2f, 1.2f, 0f, true, true, 17.5f, 10.5f)
                close()
            }
        }.build()
    }
}
