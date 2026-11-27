package com.chroma.studio.ui.storystudio

import com.chroma.studio.utils.DataCompressionUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class StoryInteractiveWidgetsTest {

    @Test
    fun roundTrip_preservesPollSliderQuizAndCoordinates() {
        val slide = StorySlide(
            items = listOf(
                WidgetStoryItem(
                    id = "w_poll",
                    widgetType = WidgetType.POLL,
                    xDp = 18f,
                    yDp = -90f,
                    scale = 1.15f,
                    rotation = -5f,
                    question = "Which grade?",
                    optA = "Warm",
                    optB = "Cool"
                ),
                WidgetStoryItem(
                    id = "w_slider",
                    widgetType = WidgetType.SLIDER,
                    xDp = -12f,
                    yDp = 64f,
                    scale = 0.9f,
                    rotation = 0f,
                    prompt = "Rate this",
                    emoji = "🔥",
                    sliderValue = 80
                ),
                WidgetStoryItem(
                    id = "w_quiz",
                    widgetType = WidgetType.QUIZ,
                    question = "Home?",
                    quizOpts = listOf("Bengaluru", "Tokyo", "Cupertino"),
                    correctIndex = 0
                )
            )
        )

        val encoded = slide.toPublishedWidgetsJson(stageWdp = 360f, stageHdp = 640f)
        val compressed = DataCompressionUtils.compressText(encoded)
        val decompressed = DataCompressionUtils.decompressText(compressed)

        val decoded = decodePublishedStoryWidgetsEnvelope(decompressed)
        assertNotNull(decoded)
        assertEquals(3, decoded!!.studioWidgets.size)

        val poll = decoded.studioWidgets[0]
        assertEquals("POLL", poll.widgetType)
        assertEquals(18f / 360f, poll.xNorm, 0.0001f)
        assertEquals(-90f / 640f, poll.yNorm, 0.0001f)
        assertEquals(1.15f, poll.scale, 0.0001f)
        assertEquals("Warm", poll.optA)

        val slider = decoded.studioWidgets[1]
        assertEquals("SLIDER", slider.widgetType)
        assertEquals(80, slider.sliderValue)
        assertEquals("🔥", slider.emoji)

        val quiz = decoded.studioWidgets[2]
        assertEquals("QUIZ", quiz.widgetType)
        assertEquals(3, quiz.quizOpts.size)
        assertEquals(0, quiz.correctIndex)
    }

    @Test
    fun decode_ignoresLegacyTextFormatJson() {
        assertNull(decodePublishedStoryWidgetsEnvelope(""))
        assertNull(decodePublishedStoryWidgetsEnvelope("""{"fontFamily":"Modern","isBold":true}"""))
    }
}
