package com.chroma.studio.ui.components

import com.chroma.studio.model.UserProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OscellionLuxuryLogoGateTest {

    @Test
    fun `luxury logo requires BOTH verified badge AND pro user status`() {
        assertFalse(OscellionLuxuryBrandController.isLuxuryLogoEligible(verified = false, isPro = false))
        assertFalse(OscellionLuxuryBrandController.isLuxuryLogoEligible(verified = true, isPro = false))
        assertFalse(OscellionLuxuryBrandController.isLuxuryLogoEligible(verified = false, isPro = true))
        assertTrue(OscellionLuxuryBrandController.isLuxuryLogoEligible(verified = true, isPro = true))
    }

    @Test
    fun `luxury logo eligibility on UserProfile rejects missing verified or missing pro`() {
        assertFalse(OscellionLuxuryBrandController.isLuxuryLogoEligible(null))

        val standardUser = UserProfile(uid = "u1", verified = false, isPro = false)
        val verifiedOnlyUser = UserProfile(uid = "u2", verified = true, isPro = false)
        val proOnlyUser = UserProfile(uid = "u3", verified = false, isPro = true)
        val verifiedProUser = UserProfile(uid = "u4", verified = true, isPro = true)

        assertFalse(OscellionLuxuryBrandController.isLuxuryLogoEligible(standardUser))
        assertFalse(OscellionLuxuryBrandController.isLuxuryLogoEligible(verifiedOnlyUser))
        assertFalse(OscellionLuxuryBrandController.isLuxuryLogoEligible(proOnlyUser))
        assertTrue(OscellionLuxuryBrandController.isLuxuryLogoEligible(verifiedProUser))
    }
}
