package com.chroma.studio.chat

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.chroma.studio.chat.ui.components.parseMarkdownToAnnotatedString
import com.chroma.studio.data.ChatFontStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatFontFormattingTest {

    @Test
    fun inlineFontTagKeysAndStripHelpersWorkForSpecificParts() {
        assertEquals(ChatFontStyle.SERIF, ChatFontStyle.fromKey("serif"))
        assertEquals(ChatFontStyle.MONO, ChatFontStyle.fromKey("mono"))
        assertEquals(ChatFontStyle.CURSIVE, ChatFontStyle.fromKey("script"))
        assertEquals(ChatFontStyle.CURSIVE, ChatFontStyle.fromKey("cursive"))
        assertEquals(ChatFontStyle.SIGNATURE, ChatFontStyle.fromKey("signature"))
        assertEquals(ChatFontStyle.DEFAULT, ChatFontStyle.fromKey("unknown"))

        val styledMsg = "Happy [signature]Birthday[/signature], [serif]Maya[/serif]!"
        assertTrue(ChatFontStyle.containsInlineFontMarkup(styledMsg))
        assertEquals("Happy Birthday, Maya!", ChatFontStyle.stripInlineFontMarkup(styledMsg))
        assertFalse(ChatFontStyle.containsInlineFontMarkup("Plain [notafont] text"))
    }

    @Test
    fun parseMarkdownAppliesFontStyleOnlyToTargetedParts() {
        val raw = "Hey [signature]Maya[/signature], see [serif]**important** note[/serif]!"
        val annotated = parseMarkdownToAnnotatedString(
            text = raw,
            textColor = Color.Black,
            mentionColor = Color.Blue,
            baseFontWeight = FontWeight.Normal
        )

        assertEquals("Hey Maya, see important note!", annotated.text)

        val spans = annotated.spanStyles
        // Plain prefix "Hey " (0..4) must NOT have Signature or Serif font family
        val prefixHasCustomFont = spans.any { span ->
            span.start <= 0 && span.end >= 4 &&
                (span.item.fontFamily == ChatFontStyle.SIGNATURE.fontFamily ||
                    span.item.fontFamily == FontFamily.Serif)
        }
        assertFalse(prefixHasCustomFont)

        // "Maya" (4..8) MUST have Signature font family
        assertTrue(
            spans.any { it.start <= 4 && it.end >= 8 && it.item.fontFamily == ChatFontStyle.SIGNATURE.fontFamily }
        )

        // "important" (14..23) inside [serif]**important** note[/serif] must have BOTH Serif and Bold
        assertTrue(
            spans.any { it.start <= 14 && it.end >= 23 && it.item.fontFamily == FontFamily.Serif }
        )
        assertTrue(
            spans.any { it.start == 14 && it.end == 23 && it.item.fontWeight == FontWeight.Bold }
        )
    }

    @Test
    fun multipleInlineFontSpansInOneMessageStayIndependent() {
        val raw = "[mono]git status[/mono] and [script]Best wishes[/script]"
        val annotated = parseMarkdownToAnnotatedString(
            text = raw,
            textColor = Color.White,
            mentionColor = Color.Cyan,
            baseFontWeight = FontWeight.Normal
        )

        assertEquals("git status and Best wishes", annotated.text)
        assertTrue(
            annotated.spanStyles.any {
                it.start <= 0 && it.end >= 10 && it.item.fontFamily == FontFamily.Monospace
            }
        )
        assertTrue(
            annotated.spanStyles.any {
                it.start <= 15 && it.end >= 26 && it.item.fontFamily == FontFamily.Cursive
            }
        )
    }
}
