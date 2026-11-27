package com.chroma.studio.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateManagerTest {

    private fun sampleRelease(
        latestVersion: String = "1.4.6",
        latestVersionCode: Int = 11,
        minSupportedVersionCode: Int = 1,
        forceUpdate: Boolean = false,
        directDownloadUrl: String = "https://1l8fshj0aw8hpzdk.public.blob.vercel-storage.com/apk/Oscellion-v1.4.6-release.apk?download=1"
    ) = AppUpdateRelease(
        latestVersion = latestVersion,
        latestVersionCode = latestVersionCode,
        minSupportedVersion = "1.0.0",
        minSupportedVersionCode = minSupportedVersionCode,
        forceUpdate = forceUpdate,
        title = "Oscellion v$latestVersion is available",
        summary = "Live update notifications and stability fixes",
        highlights = listOf("Live update alerts", "Community ranking improvements"),
        downloadUrl = directDownloadUrl,
        directDownloadUrl = directDownloadUrl,
        pageUrl = "https://oscellion.vercel.app/download",
        fileName = "Oscellion-v$latestVersion-release.apk",
        sizeBytes = 113_700_000L,
        sizeLabel = "108.4 MB",
        sha256 = "ABCDEF123456"
    )

    @Test
    fun `isUpdateAvailable returns true only when remote version code exceeds current and URL exists`() {
        val newer = sampleRelease(latestVersionCode = 11)
        assertTrue(AppUpdateCheckerLogic.isUpdateAvailable(newer, currentVersionCode = 10))
        assertFalse(AppUpdateCheckerLogic.isUpdateAvailable(newer, currentVersionCode = 11))
        assertFalse(AppUpdateCheckerLogic.isUpdateAvailable(newer, currentVersionCode = 12))
        assertFalse(AppUpdateCheckerLogic.isUpdateAvailable(null, currentVersionCode = 10))

        val missingDirectDownload = newer.copy(directDownloadUrl = "", downloadUrl = "")
        assertFalse(AppUpdateCheckerLogic.isUpdateAvailable(missingDirectDownload, currentVersionCode = 10))
    }

    @Test
    fun `matchesExpectedSha256 validates case-insensitively and formatBytesMb formats cleanly`() {
        assertTrue(AppUpdateCheckerLogic.matchesExpectedSha256("abcdef1234", "ABCDEF1234"))
        assertTrue(AppUpdateCheckerLogic.matchesExpectedSha256("", "anything"))
        assertFalse(AppUpdateCheckerLogic.matchesExpectedSha256("ABCDEF1234", "0000000000"))

        assertEquals("0.0 MB", AppUpdateCheckerLogic.formatBytesMb(0L))
        assertEquals("108.4 MB", AppUpdateCheckerLogic.formatBytesMb(113_688_502L))
        assertEquals(
            "https://cdn.example.com/app.apk",
            AppUpdateCheckerLogic.resolveDirectDownloadUrl(
                sampleRelease(directDownloadUrl = "").copy(downloadUrl = "https://cdn.example.com/app.apk")
            )
        )
    }

    @Test
    fun `isForceUpdateRequired honors force flag and minimum supported version code`() {
        val optionalUpdate = sampleRelease(latestVersionCode = 11, minSupportedVersionCode = 5, forceUpdate = false)
        assertFalse(AppUpdateCheckerLogic.isForceUpdateRequired(optionalUpdate, currentVersionCode = 10))
        assertTrue(AppUpdateCheckerLogic.isForceUpdateRequired(optionalUpdate, currentVersionCode = 4))

        val forcedUpdate = sampleRelease(latestVersionCode = 11, minSupportedVersionCode = 1, forceUpdate = true)
        assertTrue(AppUpdateCheckerLogic.isForceUpdateRequired(forcedUpdate, currentVersionCode = 10))
        assertFalse(AppUpdateCheckerLogic.isForceUpdateRequired(forcedUpdate, currentVersionCode = 11))
    }

    @Test
    fun `shouldTriggerUpdateNotification notifies once per version unless forced or manual check`() {
        val release = sampleRelease(latestVersionCode = 11, forceUpdate = false)

        // Fresh release -> notify
        assertTrue(
            AppUpdateCheckerLogic.shouldTriggerUpdateNotification(
                release = release,
                currentVersionCode = 10,
                lastNotifiedVersionCode = 10,
                dismissedVersionCode = 0,
                forceCheck = false
            )
        )

        // Already notified for version 11 -> skip duplicate notification
        assertFalse(
            AppUpdateCheckerLogic.shouldTriggerUpdateNotification(
                release = release,
                currentVersionCode = 10,
                lastNotifiedVersionCode = 11,
                dismissedVersionCode = 0,
                forceCheck = false
            )
        )

        // User dismissed version 11 -> skip auto prompt, still allow manual check
        assertFalse(
            AppUpdateCheckerLogic.shouldTriggerUpdateNotification(
                release = release,
                currentVersionCode = 10,
                lastNotifiedVersionCode = 10,
                dismissedVersionCode = 11,
                forceCheck = false
            )
        )
        assertTrue(
            AppUpdateCheckerLogic.shouldTriggerUpdateNotification(
                release = release,
                currentVersionCode = 10,
                lastNotifiedVersionCode = 11,
                dismissedVersionCode = 11,
                forceCheck = true
            )
        )

        // Forced update still notifies even if previously notified
        val forced = release.copy(forceUpdate = true)
        assertTrue(
            AppUpdateCheckerLogic.shouldTriggerUpdateNotification(
                release = forced,
                currentVersionCode = 10,
                lastNotifiedVersionCode = 11,
                dismissedVersionCode = 11,
                forceCheck = false
            )
        )
    }

    @Test
    fun `parseReleaseFromJson parses backend manifest accurately`() {
        val raw = """
            {
              "latestVersion": "1.4.6",
              "latestVersionCode": 11,
              "minSupportedVersion": "1.2.0",
              "minSupportedVersionCode": 4,
              "forceUpdate": false,
              "title": "Oscellion v1.4.6 is available",
              "summary": "Instant update notifications",
              "highlights": ["In-app update sheet", "Community discovery ranking"],
              "directDownloadUrl": "https://oscellion.vercel.app/api/download/apk",
              "sizeLabel": "108.5 MB",
              "sha256": "DEADBEEF"
            }
        """.trimIndent()

        val parsed = AppUpdateCheckerLogic.parseReleaseFromJson(raw)
        assertNotNull(parsed)
        assertEquals("1.4.6", parsed!!.latestVersion)
        assertEquals(11, parsed.latestVersionCode)
        assertEquals(4, parsed.minSupportedVersionCode)
        assertFalse(parsed.forceUpdate)
        assertEquals(listOf("In-app update sheet", "Community discovery ranking"), parsed.highlights)
        assertEquals("108.5 MB", parsed.sizeLabel)
        assertEquals("DEADBEEF", parsed.sha256)
        assertNull(AppUpdateCheckerLogic.parseReleaseFromJson("not-json"))
    }

    @Test
    fun `parseReleaseFromMap parses Firestore app_release fields accurately`() {
        val parsed = AppUpdateCheckerLogic.parseReleaseFromMap(
            mapOf(
                "latestVersion" to "1.4.6",
                "latestVersionCode" to 11L,
                "minSupportedVersionCode" to 2L,
                "forceUpdate" to "true",
                "summary" to "Required studio patch",
                "highlights" to listOf("Item 1", "Item 2"),
                "downloadUrl" to "https://example.com/app.apk"
            )
        )
        assertNotNull(parsed)
        assertEquals("1.4.6", parsed!!.latestVersion)
        assertEquals(11, parsed.latestVersionCode)
        assertEquals(2, parsed.minSupportedVersionCode)
        assertTrue(parsed.forceUpdate)
        assertEquals("https://example.com/app.apk", parsed.directDownloadUrl)
        assertEquals(listOf("Item 1", "Item 2"), parsed.highlights)
    }
}
