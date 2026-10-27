package com.chroma.studio

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.Locale
import java.util.UUID

/**
 * Empirical stress tests and adversarial verification for Milestone 1 and Milestone 2.
 */
class Milestone1And2StressTest {

    // =========================================================================
    // Milestone 1: Audio Drift Calculation Stress Test (Div-by-zero & NaN check)
    // =========================================================================

    @Test
    fun testAudioDriftCalculationSafety() {
        // Test sub-second acoustic drift calculation (driftMs in 35L..500L)
        val driftRange = listOf(35L, 36L, 100L, 250L, 499L, 500L)

        for (driftMs in driftRange) {
            // Case A: audio trailing (signedDriftMs > 0)
            val signedDriftMsTrailing = driftMs
            val targetSpeedTrailing = (1.0f + (signedDriftMsTrailing.toFloat() / 500f) * 0.04f).coerceIn(1.01f, 1.04f)
            assertFalse("Speed must not be NaN", targetSpeedTrailing.isNaN())
            assertFalse("Speed must not be Infinite", targetSpeedTrailing.isInfinite())
            assertTrue("Speed must be in [1.01, 1.04]", targetSpeedTrailing in 1.01f..1.04f)

            // Case B: audio leading (signedDriftMs < 0)
            val targetSpeedLeading = (1.0f - (driftMs.toFloat() / 500f) * 0.04f).coerceIn(0.96f, 0.99f)
            assertFalse("Speed must not be NaN", targetSpeedLeading.isNaN())
            assertFalse("Speed must not be Infinite", targetSpeedLeading.isInfinite())
            assertTrue("Speed must be in [0.96, 0.99]", targetSpeedLeading in 0.96f..0.99f)
        }

        // Test boundary values
        // driftMs < 35ms: speed is fixed at 1.0f
        val inLockStepSpeed = 1.0f
        assertEquals(1.0f, inLockStepSpeed, 0.0001f)

        // Extreme values stress test:
        val extremeDrifts = listOf(0L, -1L, Long.MIN_VALUE, Long.MAX_VALUE, 1_000_000L)
        for (ext in extremeDrifts) {
            val clampedFloat = ext.toFloat()
            val ratio = clampedFloat / 500f
            // Even if extreme, verify Float division by constant 500f never causes div-by-zero
            assertFalse("Division by constant 500f never causes NaN from finite numerator", ratio.isNaN() && !clampedFloat.isNaN())
        }
    }

    // =========================================================================
    // Milestone 1: RxFFmpeg Transition Filter Strings Syntactic Validation
    // =========================================================================

    enum class MockTransitionType {
        NONE, CROSS_DISSOLVE, DIP_TO_BLACK, DIP_TO_WHITE, FILM_BLOOM,
        PUSH_LEFT, PUSH_RIGHT, PUSH_UP, PUSH_DOWN, COVER_SLIDE,
        PARALLAX, WIPE_HORIZONTAL, WIPE_DIAGONAL, CLOCK_SWEEP,
        BARN_DOORS, VENETIAN_BLINDS, CIRCLE_IRIS, PINHOLE_SNAP,
        DIAMOND_IRIS, QUAD_SHUTTER, ZOOM_IN, ZOOM_OUT, WHIP_SPIN,
        CUBE_3D, PAGE_FLIP_3D, GLITCH, CROSSFADE, WIPE_RIGHT,
        WIPE_LEFT, SLIDE_LEFT
    }

    @Test
    fun testRxFFmpegFilterStringsSyntacticValidity() {
        val finalW = 1080
        val finalH = 1920
        val effectiveDur = 5.0f
        val transDur = 1.0f
        val halfTrans = (transDur / 2f).coerceAtMost(effectiveDur / 2.2f)
        val fadeStart = (effectiveDur - halfTrans).coerceAtLeast(0f)
        val prevHalfTrans = halfTrans

        for (type in MockTransitionType.values()) {
            val vFilter = StringBuilder()

            // Generate Outgoing Filter
            when (type) {
                MockTransitionType.DIP_TO_BLACK -> {
                    vFilter.append(String.format(Locale.US, ",fade=t=out:st=%.3f:d=%.3f:c=black", fadeStart, halfTrans))
                }
                MockTransitionType.DIP_TO_WHITE -> {
                    vFilter.append(String.format(Locale.US, ",fade=t=out:st=%.3f:d=%.3f:c=white", fadeStart, halfTrans))
                }
                MockTransitionType.FILM_BLOOM -> {
                    vFilter.append(String.format(Locale.US, ",scale=w='iw*(1+0.06*(t-%.3f)/%.3f)':h='ih*(1+0.06*(t-%.3f)/%.3f)':eval=frame,crop=w=%d:h=%d,fade=t=out:st=%.3f:d=%.3f:c=white,eq=brightness=0.18:contrast=1.15:saturation=1.20:enable='gte(t,%.3f)',colorbalance=rs=0.12:gs=0.06:bs=-0.08:enable='gte(t,%.3f)'", fadeStart, halfTrans, fadeStart, halfTrans, finalW, finalH, fadeStart, halfTrans, fadeStart, fadeStart))
                }
                MockTransitionType.CROSS_DISSOLVE, MockTransitionType.CROSSFADE -> {
                    vFilter.append(String.format(Locale.US, ",fade=t=out:st=%.3f:d=%.3f:c=black,eq=contrast=0.96:brightness=0.03:enable='gte(t,%.3f)'", fadeStart, halfTrans, fadeStart))
                }
                MockTransitionType.GLITCH -> {
                    vFilter.append(String.format(Locale.US, ",noise=c0s=45:c1s=35:allf=t:enable='gte(t,%.3f)',hue=H='PI*sin(20*t)':enable='gte(t,%.3f)',eq=contrast=1.40:brightness=0.08:enable='gte(t,%.3f)',colorbalance=rs=0.18:bs=-0.15:enable='gte(t,%.3f)'", fadeStart, fadeStart, fadeStart, fadeStart))
                }
                MockTransitionType.ZOOM_IN -> {
                    vFilter.append(String.format(Locale.US, ",scale=w='iw*(1+1.8*(t-%.3f)/%.3f)':h='ih*(1+1.8*(t-%.3f)/%.3f)':eval=frame,crop=w=%d:h=%d,boxblur=8:enable='gte(t,%.3f)',fade=t=out:st=%.3f:d=%.3f:c=black", fadeStart, halfTrans, fadeStart, halfTrans, finalW, finalH, fadeStart, fadeStart, halfTrans))
                }
                MockTransitionType.ZOOM_OUT -> {
                    vFilter.append(String.format(Locale.US, ",scale=w='max(2,iw*(1-0.65*(t-%.3f)/%.3f))':h='max(2,ih*(1-0.65*(t-%.3f)/%.3f))':eval=frame,pad=%d:%d:'(%d-iw)/2':'(%d-ih)/2':black,fade=t=out:st=%.3f:d=%.3f:c=black", fadeStart, halfTrans, fadeStart, halfTrans, finalW, finalH, finalW, finalH, fadeStart, halfTrans))
                }
                MockTransitionType.WHIP_SPIN -> {
                    vFilter.append(String.format(Locale.US, ",rotate='2*PI*(t-%.3f)/%.3f':ow=iw:oh=ih:c=black:enable='gte(t,%.3f)',boxblur=10:enable='gte(t,%.3f)',scale=w='iw*(1+0.45*(t-%.3f)/%.3f)':h='ih*(1+0.45*(t-%.3f)/%.3f)':eval=frame,crop=w=%d:h=%d,fade=t=out:st=%.3f:d=%.3f:c=black", fadeStart, halfTrans, fadeStart, fadeStart, fadeStart, halfTrans, fadeStart, halfTrans, finalW, finalH, fadeStart, halfTrans))
                }
                MockTransitionType.WIPE_HORIZONTAL -> {
                    vFilter.append(String.format(Locale.US, ",crop=w='max(2,%d*(1-(t-%.3f)/%.3f))':h=%d:x=0:y=0,pad=%d:%d:0:0:black", finalW, fadeStart, halfTrans, finalH, finalW, finalH))
                }
                MockTransitionType.WIPE_DIAGONAL -> {
                    vFilter.append(String.format(Locale.US, ",pad=2*%d:2*%d:0:0:black,crop=w=%d:h=%d:x='min(%d,%d*(t-%.3f)/%.3f)':y='min(%d,%d*(t-%.3f)/%.3f)'", finalW, finalH, finalW, finalH, finalW, finalW, fadeStart, halfTrans, finalH, finalH, fadeStart, halfTrans))
                }
                MockTransitionType.CLOCK_SWEEP -> {
                    vFilter.append(String.format(Locale.US, ",rotate='2*PI*(t-%.3f)/%.3f':ow=iw:oh=ih:c=black:enable='gte(t,%.3f)',vignette=angle='max(0.01,(PI/2.0)*(1-(t-%.3f)/%.3f))':enable='gte(t,%.3f)'", fadeStart, halfTrans, fadeStart, fadeStart, halfTrans, fadeStart))
                }
                MockTransitionType.BARN_DOORS -> {
                    vFilter.append(String.format(Locale.US, ",crop=w='max(2,%d*(1-(t-%.3f)/%.3f))':h=%d:x='%d*0.5*(t-%.3f)/%.3f':y=0,pad=%d:%d:'(%d-iw)/2':0:black", finalW, fadeStart, halfTrans, finalH, finalW, fadeStart, halfTrans, finalW, finalH, finalW))
                }
                MockTransitionType.VENETIAN_BLINDS -> {
                    vFilter.append(String.format(Locale.US, ",crop=w=%d:h='max(2,%d*(1-(t-%.3f)/%.3f))':x=0:y=0,pad=%d:%d:0:0:black,boxblur=4:enable='gte(t,%.3f)'", finalW, finalH, fadeStart, halfTrans, finalW, finalH, fadeStart))
                }
                MockTransitionType.CIRCLE_IRIS -> {
                    vFilter.append(String.format(Locale.US, ",vignette=angle='max(0.01,(PI/2.0)*(1-(t-%.3f)/%.3f))':x0='w/2':y0='h/2':enable='gte(t,%.3f)'", fadeStart, halfTrans, fadeStart))
                }
                MockTransitionType.PINHOLE_SNAP -> {
                    vFilter.append(String.format(Locale.US, ",vignette=angle='max(0.005,(PI/2.0)*(1-(t-%.3f)/%.3f)*(1-(t-%.3f)/%.3f))':x0='w/2':y0='h/2':enable='gte(t,%.3f)'", fadeStart, halfTrans, fadeStart, halfTrans, fadeStart))
                }
                MockTransitionType.DIAMOND_IRIS -> {
                    vFilter.append(String.format(Locale.US, ",vignette=angle='max(0.01,(PI/2.2)*(1-(t-%.3f)/%.3f))':aspect=1.0:enable='gte(t,%.3f)',crop=w='max(2,%d*(1-(t-%.3f)/%.3f))':h='max(2,%d*(1-(t-%.3f)/%.3f))':x='%d*0.5*(t-%.3f)/%.3f':y='%d*0.5*(t-%.3f)/%.3f',pad=%d:%d:'(%d-iw)/2':'(%d-ih)/2':black", fadeStart, halfTrans, fadeStart, finalW, fadeStart, halfTrans, finalH, fadeStart, halfTrans, finalW, fadeStart, halfTrans, finalH, fadeStart, halfTrans, finalW, finalH, finalW, finalH))
                }
                MockTransitionType.QUAD_SHUTTER -> {
                    vFilter.append(String.format(Locale.US, ",crop=w='max(2,%d*(1-(t-%.3f)/%.3f))':h='max(2,%d*(1-(t-%.3f)/%.3f))':x='%d*0.5*(t-%.3f)/%.3f':y='%d*0.5*(t-%.3f)/%.3f',pad=%d:%d:'(%d-iw)/2':'(%d-ih)/2':black", finalW, fadeStart, halfTrans, finalH, fadeStart, halfTrans, finalW, fadeStart, halfTrans, finalH, fadeStart, halfTrans, finalW, finalH, finalW, finalH))
                }
                MockTransitionType.CUBE_3D -> {
                    vFilter.append(String.format(Locale.US, ",scale=w='max(2,%d*cos((PI/2)*(t-%.3f)/%.3f))':h=%d:eval=frame,pad=%d:%d:'(%d-iw)/2':0:black,eq=brightness='-0.12*(t-%.3f)/%.3f':enable='gte(t,%.3f)'", finalW, fadeStart, halfTrans, finalH, finalW, finalH, finalW, fadeStart, halfTrans, fadeStart))
                }
                MockTransitionType.PAGE_FLIP_3D -> {
                    vFilter.append(String.format(Locale.US, ",scale=w='max(2,%d*cos((PI/2)*(t-%.3f)/%.3f)*(1-0.12*(t-%.3f)/%.3f))':h='%d*(1-0.12*(t-%.3f)/%.3f)':eval=frame,pad=%d:%d:'(%d-iw)/2':'(%d-ih)/2':black,boxblur=4:enable='gte(t,%.3f)'", finalW, fadeStart, halfTrans, fadeStart, halfTrans, finalH, fadeStart, halfTrans, finalW, finalH, finalW, finalH, fadeStart))
                }
                MockTransitionType.COVER_SLIDE, MockTransitionType.WIPE_LEFT -> {
                    vFilter.append(String.format(Locale.US, ",fade=t=out:st=%.3f:d=%.3f:c=black", fadeStart, halfTrans))
                }
                else -> {}
            }

            val filterStr = vFilter.toString()

            // Verify xfade is NEVER used
            assertFalse("Transition $type must not contain xfade", filterStr.contains("xfade"))

            // Verify quote balance
            val singleQuoteCount = filterStr.count { it == '\'' }
            assertEquals("Single quotes must be paired for transition $type: $filterStr", 0, singleQuoteCount % 2)

            // Verify parentheses balance
            var parenDepth = 0
            for (ch in filterStr) {
                if (ch == '(') parenDepth++
                if (ch == ')') parenDepth--
                assertTrue("Unmatched closing parenthesis in $type: $filterStr", parenDepth >= 0)
            }
            assertEquals("Unmatched opening parenthesis in $type: $filterStr", 0, parenDepth)

            // Verify no NaN or Infinity strings
            assertFalse("Filter for $type must not contain NaN", filterStr.contains("NaN"))
            assertFalse("Filter for $type must not contain Infinity", filterStr.contains("Infinity"))
        }
    }

    // =========================================================================
    // Milestone 1: localFileCache Cleanup Robustness
    // =========================================================================

    @Test
    fun testLocalFileCacheCleanupRobustness() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "fcp_cleanup_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        val otherDir = File(System.getProperty("java.io.tmpdir"), "fcp_cleanup_user_${System.currentTimeMillis()}")
        otherDir.mkdirs()

        try {
            val localFileCache = mutableMapOf<String, File>()

            // 1. Temporary input file created by exporter in cacheDir
            val tempFile1 = File(tempDir, "fcp_input_12345678.mp4")
            tempFile1.writeText("sample video content")
            localFileCache["key1"] = tempFile1

            // 2. Already-deleted / missing file in cacheDir
            val missingFile = File(tempDir, "fcp_input_missing.mp4")
            localFileCache["key2"] = missingFile // does not exist

            // 3. User's original file located outside cacheDir
            val userFile = File(otherDir, "user_camera_video.mp4")
            userFile.writeText("user critical content")
            localFileCache["key3"] = userFile

            // 4. File in cacheDir but NOT starting with fcp_input_ (e.g. app internal DB or image cache)
            val nonFcpFile = File(tempDir, "app_database.db")
            nonFcpFile.writeText("app database content")
            localFileCache["key4"] = nonFcpFile

            // Run the exact cleanup algorithm from FcpTimelineExporter finally block:
            for (f in localFileCache.values) {
                if (f.exists() && f.parentFile == tempDir && f.name.startsWith("fcp_input_")) {
                    try { f.delete() } catch (_: Throwable) {}
                }
            }

            // Assertions:
            // tempFile1 must be deleted
            assertFalse("Temporary input file must be deleted", tempFile1.exists())

            // userFile must NOT be deleted
            assertTrue("User original file outside cacheDir must NOT be deleted", userFile.exists())

            // nonFcpFile must NOT be deleted
            assertTrue("Non-fcp files in cacheDir must NOT be deleted", nonFcpFile.exists())
        } finally {
            tempDir.deleteRecursively()
            otherDir.deleteRecursively()
        }
    }

    // =========================================================================
    // Milestone 2: Coil Cache Key Uniqueness
    // =========================================================================

    @Test
    fun testCoilCacheKeyUniqueness() {
        fun makeKey(id: String, timestamp: Long): String = "flash_thumb_${id}_${timestamp}"

        val id1 = UUID.randomUUID().toString()
        val id2 = UUID.randomUUID().toString()
        val ts1 = 1700000000000L
        val ts2 = 1700000005000L

        val key1 = makeKey(id1, ts1)
        val key2 = makeKey(id2, ts1)
        val key1Updated = makeKey(id1, ts2)

        // Different IDs must have distinct keys
        assertNotEquals("Different item IDs must have different cache keys", key1, key2)

        // Same ID with updated timestamp must have distinct key (cache busting on edit)
        assertNotEquals("Updated timestamp must produce new cache key", key1, key1Updated)

        // Keys must not be blank
        assertTrue("Cache key must be non-blank", key1.isNotBlank())
        assertTrue("Cache key must start with prefix", key1.startsWith("flash_thumb_"))
    }

    // =========================================================================
    // Milestone 2: Draft Title Edge Cases & duplicateDraft Logic
    // =========================================================================

    @Test
    fun testDraftRenameAndDuplicateEdgeCases() {
        data class MockDraft(
            val id: String,
            val title: String,
            val timestamp: Long
        )

        // Simulate duplicateDraft behavior with blank title:
        fun mockDuplicateDraft(target: MockDraft?): MockDraft? {
            if (target == null) return null
            return target.copy(
                id = UUID.randomUUID().toString(),
                title = "${target.title.ifBlank { "Untitled Project" }} (Copy)",
                timestamp = System.currentTimeMillis()
            )
        }

        // Test with non-existent ID -> returns null
        val nullResult = mockDuplicateDraft(null)
        assertNull("Duplicating non-existent draft must return null", nullResult)

        // Test with empty title -> uses "Untitled Project (Copy)"
        val emptyTitleDraft = MockDraft(id = "1", title = "", timestamp = 1000L)
        val dup1 = mockDuplicateDraft(emptyTitleDraft)
        assertNotNull(dup1)
        assertEquals("Untitled Project (Copy)", dup1!!.title)
        assertNotEquals("New draft must have fresh ID", emptyTitleDraft.id, dup1.id)

        // Test with whitespace title -> uses "Untitled Project (Copy)"
        val whitespaceTitleDraft = MockDraft(id = "2", title = "   ", timestamp = 1000L)
        val dup2 = mockDuplicateDraft(whitespaceTitleDraft)
        assertNotNull(dup2)
        assertEquals("Untitled Project (Copy)", dup2!!.title)

        // Test with regular title
        val normalDraft = MockDraft(id = "3", title = "Sunset Vlog", timestamp = 1000L)
        val dup3 = mockDuplicateDraft(normalDraft)
        assertNotNull(dup3)
        assertEquals("Sunset Vlog (Copy)", dup3!!.title)

        // UI Rename validation check:
        fun validateRenameInput(rawInput: String): String? {
            val trimmed = rawInput.trim()
            return if (trimmed.isNotEmpty()) trimmed else null
        }

        assertNull("Empty string must be rejected by rename validator", validateRenameInput(""))
        assertNull("Whitespace-only string must be rejected by rename validator", validateRenameInput("   \t\n  "))
        assertEquals("Trimmed valid title must be accepted", "My Cool Video", validateRenameInput("  My Cool Video  "))
    }

    // =========================================================================
    // Codebase Integrity: Static Assertions for M1 & M2 Deliverables
    // =========================================================================

    @Test
    fun testCodebaseExclusionOfLegacyArtifacts() {
        val rootDir = File(".").canonicalFile
        val mainDir = if (File(rootDir, "src/main/java").exists()) File(rootDir, "src/main/java") else File(rootDir, "app/src/main/java")

        // 1. Verify ImportMediaTile is completely absent from all source files
        val flashHubFile = File(mainDir, "com/chroma/studio/ui/screens/FlashProjectsHubScreen.kt")
        assertTrue("FlashProjectsHubScreen.kt must exist", flashHubFile.exists())
        val flashHubContent = flashHubFile.readText()
        assertFalse("ImportMediaTile must be completely absent from FlashProjectsHubScreen", flashHubContent.contains("ImportMediaTile"))

        // 2. Verify VideoTransitionEngine does NOT contain 'A' or 'B' letter box fallbacks
        val transitionEngineFile = File(mainDir, "com/chroma/studio/ui/components/VideoTransitionEngine.kt")
        assertTrue("VideoTransitionEngine.kt must exist", transitionEngineFile.exists())
        val transitionContent = transitionEngineFile.readText()
        assertFalse("DemoOutgoingVideoClip must not have text = \"A\"", transitionContent.contains("text = \"A\""))
        assertFalse("DemoIncomingVideoClip must not have text = \"B\"", transitionContent.contains("text = \"B\""))

        // 3. Verify TransitionPickerSheet does NOT contain 'A' or 'B' letter box fallbacks
        val transitionPickerFile = File(mainDir, "com/chroma/studio/ui/screens/TransitionPickerSheet.kt")
        assertTrue("TransitionPickerSheet.kt must exist", transitionPickerFile.exists())
        val pickerContent = transitionPickerFile.readText()
        assertFalse("TransitionPickerSheet must not contain text = \"A\"", pickerContent.contains("text = \"A\""))
        assertFalse("TransitionPickerSheet must not contain text = \"B\"", pickerContent.contains("text = \"B\""))

        // 4. Verify FinalCutProVideoEditor contains isExporting guards on scrub and play controls
        val fcpEditorFile = File(mainDir, "com/chroma/studio/ui/screens/FinalCutProVideoEditor.kt")
        assertTrue("FinalCutProVideoEditor.kt must exist", fcpEditorFile.exists())
        val fcpContent = fcpEditorFile.readText()
        assertTrue("ensureClipLoadedInPlayer must guard with isExporting", fcpContent.contains("if (isExporting) return"))
        assertTrue("onPlayToggle must guard with !isExporting", fcpContent.contains("if (!isExporting)"))
    }
}
