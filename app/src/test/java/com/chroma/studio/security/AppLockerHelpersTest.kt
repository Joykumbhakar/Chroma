package com.chroma.studio.security

import com.chroma.studio.security.applock.AppLockCategory
import com.chroma.studio.security.applock.AppLockTarget
import com.chroma.studio.security.applock.AppLockerManager
import com.chroma.studio.security.applock.categorizeAppTarget
import com.chroma.studio.security.applock.filterAppLockTargets
import com.chroma.studio.security.applock.formatRelockModeDescription
import com.chroma.studio.security.applock.formatRelockModeLabel
import com.chroma.studio.security.applock.getRelockGraceDurationMs
import com.chroma.studio.security.applock.isAntiTamperPackage
import com.chroma.studio.security.applock.isRecommendedPrivacyPackage
import com.chroma.studio.security.applock.shouldSessionStayUnlocked
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockerHelpersTest {

    @Test
    fun `relock policy grace durations and labels cover all modes`() {
        assertEquals(0L, getRelockGraceDurationMs(AppLockerManager.RELOCK_IMMEDIATELY))
        assertEquals(30_000L, getRelockGraceDurationMs(AppLockerManager.RELOCK_30_SEC))
        assertEquals(60_000L, getRelockGraceDurationMs(AppLockerManager.RELOCK_1_MIN))
        assertEquals(300_000L, getRelockGraceDurationMs(AppLockerManager.RELOCK_5_MIN))
        assertEquals(0L, getRelockGraceDurationMs(AppLockerManager.RELOCK_SCREEN_OFF))

        assertEquals("On Exit", formatRelockModeLabel(AppLockerManager.RELOCK_IMMEDIATELY))
        assertEquals("After 30s", formatRelockModeLabel(AppLockerManager.RELOCK_30_SEC))
        assertEquals("After 1 min", formatRelockModeLabel(AppLockerManager.RELOCK_1_MIN))
        assertEquals("After 5 min", formatRelockModeLabel(AppLockerManager.RELOCK_5_MIN))
        assertEquals("Screen Off", formatRelockModeLabel(AppLockerManager.RELOCK_SCREEN_OFF))
        assertTrue(formatRelockModeDescription(AppLockerManager.RELOCK_30_SEC).contains("30 seconds"))
    }

    @Test
    fun `shouldSessionStayUnlocked enforces immediate grace and screen off policies`() {
        val unlockedAt = 1_000_000L
        assertFalse(shouldSessionStayUnlocked(null, null, AppLockerManager.RELOCK_IMMEDIATELY, unlockedAt))

        // On Exit stays unlocked while foregrounded (leftAt == null), locks once user leaves
        assertTrue(shouldSessionStayUnlocked(unlockedAt, null, AppLockerManager.RELOCK_IMMEDIATELY, unlockedAt + 5_000L))
        assertFalse(shouldSessionStayUnlocked(unlockedAt, unlockedAt + 1_000L, AppLockerManager.RELOCK_IMMEDIATELY, unlockedAt + 1_100L))

        // 30s grace window allows return at +29s, blocks at +30s
        val leftAt = unlockedAt + 10_000L
        assertTrue(shouldSessionStayUnlocked(unlockedAt, leftAt, AppLockerManager.RELOCK_30_SEC, leftAt + 29_999L))
        assertFalse(shouldSessionStayUnlocked(unlockedAt, leftAt, AppLockerManager.RELOCK_30_SEC, leftAt + 30_000L))

        // 5 min grace window allows 4m59s, blocks at 5m00s
        assertTrue(shouldSessionStayUnlocked(unlockedAt, leftAt, AppLockerManager.RELOCK_5_MIN, leftAt + 299_000L))
        assertFalse(shouldSessionStayUnlocked(unlockedAt, leftAt, AppLockerManager.RELOCK_5_MIN, leftAt + 300_000L))

        // Screen off stays unlocked even long after leaving the app
        assertTrue(shouldSessionStayUnlocked(unlockedAt, leftAt, AppLockerManager.RELOCK_SCREEN_OFF, leftAt + 3_600_000L))
    }

    @Test
    fun `isAntiTamperPackage matches system settings installer and resolved OEM package`() {
        assertTrue(isAntiTamperPackage("com.android.settings"))
        assertTrue(isAntiTamperPackage("com.google.android.packageinstaller"))
        assertTrue(isAntiTamperPackage("com.android.packageinstaller"))
        assertTrue(isAntiTamperPackage("com.samsung.android.settings", resolvedSettingsPackage = "com.samsung.android.settings"))
        assertFalse(isAntiTamperPackage("com.whatsapp", resolvedSettingsPackage = "com.android.settings"))
    }

    @Test
    fun `categorizeAppTarget and isRecommendedPrivacyPackage classify apps accurately`() {
        assertEquals(AppLockCategory.FINANCE, categorizeAppTarget("com.phonepe.app", "PhonePe", false))
        assertEquals(AppLockCategory.SOCIAL, categorizeAppTarget("com.whatsapp", "WhatsApp", false))
        assertEquals(AppLockCategory.PRODUCTIVITY, categorizeAppTarget("com.google.android.gm", "Gmail", true))
        assertEquals(AppLockCategory.MEDIA, categorizeAppTarget("com.google.android.apps.photos", "Photos", true))
        assertEquals(AppLockCategory.SYSTEM, categorizeAppTarget("com.android.calculator2", "Calculator", true))
        assertEquals(AppLockCategory.OTHER, categorizeAppTarget("com.example.game", "Puzzle", false))

        assertTrue(isRecommendedPrivacyPackage("com.whatsapp", "WhatsApp"))
        assertTrue(isRecommendedPrivacyPackage("com.example.mybank", "HDFC Bank"))
        assertFalse(isRecommendedPrivacyPackage("com.example.game", "Puzzle"))
    }

    @Test
    fun `filterAppLockTargets handles tabs and search queries`() {
        val apps = listOf(
            AppLockTarget("com.whatsapp", "WhatsApp", null, isLocked = true, isSystem = false, category = AppLockCategory.SOCIAL),
            AppLockTarget("com.google.android.gm", "Gmail", null, isLocked = false, isSystem = true, category = AppLockCategory.PRODUCTIVITY),
            AppLockTarget("com.example.notes", "Notes", null, isLocked = false, isSystem = false, category = AppLockCategory.PRODUCTIVITY)
        )

        assertEquals(3, filterAppLockTargets(apps, "ALL", "").size)
        assertEquals(listOf("com.whatsapp"), filterAppLockTargets(apps, "LOCKED", "").map { it.packageName })
        assertEquals(listOf("com.whatsapp", "com.example.notes"), filterAppLockTargets(apps, "DOWNLOADED", "").map { it.packageName })
        assertEquals(listOf("com.google.android.gm"), filterAppLockTargets(apps, "SYSTEM", "").map { it.packageName })
        assertEquals(listOf("com.google.android.gm"), filterAppLockTargets(apps, "ALL", "gmail").map { it.packageName })
        assertEquals(listOf("com.example.notes"), filterAppLockTargets(apps, "DOWNLOADED", "example").map { it.packageName })
    }

    @Test
    fun `PIN lockout state escalates after 5 failed attempts and counts remaining seconds`() {
        val now = 10_000L
        val a1 = computeNextPinLockoutState(0, now)
        assertEquals(1, a1.failedAttempts)
        assertEquals(0, a1.lockoutSeconds)
        assertEquals(0L, a1.lockoutUntilMs)

        val a4 = computeNextPinLockoutState(3, now)
        assertEquals(4, a4.failedAttempts)
        assertEquals(0, a4.lockoutSeconds)

        val a5 = computeNextPinLockoutState(4, now)
        assertEquals(5, a5.failedAttempts)
        assertEquals(30, a5.lockoutSeconds)
        assertEquals(40_000L, a5.lockoutUntilMs)

        val a6 = computeNextPinLockoutState(5, now)
        assertEquals(6, a6.failedAttempts)
        assertEquals(60, a6.lockoutSeconds)

        val a7 = computeNextPinLockoutState(6, now)
        assertEquals(7, a7.failedAttempts)
        assertEquals(120, a7.lockoutSeconds)

        assertEquals(30, computeRemainingLockoutSeconds(40_000L, 10_000L))
        assertEquals(1, computeRemainingLockoutSeconds(40_000L, 39_500L))
        assertEquals(0, computeRemainingLockoutSeconds(40_000L, 40_000L))
    }
}
