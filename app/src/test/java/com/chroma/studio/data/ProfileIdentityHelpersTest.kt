package com.chroma.studio.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileIdentityHelpersTest {

    @Test
    fun `isPrivilegedAccountRole identifies Admin Moderator and SuperAdmin`() {
        assertTrue(isPrivilegedAccountRole("Admin"))
        assertTrue(isPrivilegedAccountRole("Moderator"))
        assertTrue(isPrivilegedAccountRole("SuperAdmin"))
        assertFalse(isPrivilegedAccountRole("Creator"))
        assertFalse(isPrivilegedAccountRole("Developer"))
        assertFalse(isPrivilegedAccountRole(null))
    }

    @Test
    fun `resolveSafeProfileRole permits creator professions and preserves staff roles`() {
        assertEquals("Developer", resolveSafeProfileRole("Creator", "Developer"))
        assertEquals("Designer", resolveSafeProfileRole("", "Designer"))
        assertEquals("Admin", resolveSafeProfileRole("Admin", "Developer"))
        assertEquals("Moderator", resolveSafeProfileRole("Moderator", "Creator"))
        assertEquals("Creator", resolveSafeProfileRole("Creator", "Admin"))
        assertEquals("Creator", resolveSafeProfileRole("", "SuperAdmin"))
    }

    @Test
    fun `validateProfileHandleUpdate validates display name and handle format`() {
        assertNull(validateProfileHandleUpdate("Joy Kumbhakar", "joy.studio_1"))
        assertEquals("Display name cannot be empty", validateProfileHandleUpdate("   ", "joy"))
        assertEquals("Username must be between 3 and 30 characters", validateProfileHandleUpdate("Joy", "ab"))
        assertEquals(
            "Username can only contain lowercase letters, numbers, periods, and underscores",
            validateProfileHandleUpdate("Joy", "joy@invalid!")
        )
    }
}
