package com.chroma.studio.chat.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupOnboardingHelpersTest {

    @Test
    fun `generateGroupInviteCode creates clean 8-char alphanumeric codes`() {
        val codeA = generateGroupInviteCode(seed = 1001L)
        val codeB = generateGroupInviteCode(seed = 2002L)

        assertEquals(8, codeA.length)
        assertEquals(8, codeB.length)
        assertNotEquals(codeA, codeB)
        assertTrue(codeA.all { it in 'A'..'Z' || it in '2'..'9' })
        assertFalse(codeA.contains('O'))
        assertFalse(codeA.contains('I'))
        assertFalse(codeA.contains('0'))
        assertFalse(codeA.contains('1'))
    }

    @Test
    fun `disappearingTimerLabel maps durations cleanly`() {
        assertEquals("Off", disappearingTimerLabel(0))
        assertEquals("24h", disappearingTimerLabel(24))
        assertEquals("7d", disappearingTimerLabel(168))
        assertEquals("90d", disappearingTimerLabel(2160))
    }

    @Test
    fun `buildGroupInviteUrl produces canonical oscellion join link`() {
        assertEquals(
            "https://oscellion.vercel.app/join/AB23CD45",
            buildGroupInviteUrl("AB23CD45")
        )
    }
}
