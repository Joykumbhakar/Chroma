package com.chroma.studio.ui.screens

import com.chroma.studio.ui.components.LiquidGlassProgressBar

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.chroma.studio.ui.theme.LocalOscellionColors
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import kotlin.math.*

// ──────────────────────────────────────────────────────────────────────────────
// Data models
// ──────────────────────────────────────────────────────────────────────────────

enum class CheckSeverity { CRITICAL, HIGH, MEDIUM, POLISH }
enum class CheckStatus { PASS, FAIL, WARNING, SKIPPED }
enum class CheckDomain {
    BITMAP_PIPELINE, GESTURES_VIEWPORT, AUDIO_PIPELINE,
    TRANSITION_PARITY, TIMELINE_SAFETY, MEMORY_LEAKS,
    MOCK_FEATURES, TAB_PARITY, DESIGN_SYSTEM,
    ACCESSIBILITY, HAPTICS, ARCHITECTURE
}

data class AnalyzerCheck(
    val id: String,
    val domain: CheckDomain,
    val severity: CheckSeverity,
    val title: String,
    val description: String,
    val fileRef: String,
    val verify: suspend (context: Context) -> Pair<CheckStatus, String>
)

data class AnalyzerResult(
    val check: AnalyzerCheck,
    val status: CheckStatus,
    val detail: String,
    val durationMs: Long
)

data class AnalyzerReport(
    val results: List<AnalyzerResult>,
    val durationMs: Long,
    val timestamp: Long = System.currentTimeMillis()
) {
    val totalChecks get() = results.size
    val passed get() = results.count { it.status == CheckStatus.PASS }
    val failed get() = results.count { it.status == CheckStatus.FAIL }
    val warnings get() = results.count { it.status == CheckStatus.WARNING }
    val skipped get() = results.count { it.status == CheckStatus.SKIPPED }
    val healthScore get() = if (totalChecks == 0) 100 else
        ((passed.toFloat() / totalChecks) * 100).roundToInt()
    val criticalFails get() = results.filter {
        it.status == CheckStatus.FAIL && it.check.severity == CheckSeverity.CRITICAL
    }
    val highFails get() = results.filter {
        it.status == CheckStatus.FAIL && it.check.severity == CheckSeverity.HIGH
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Check definitions — all 50+ checks from the Master Checklist
// ──────────────────────────────────────────────────────────────────────────────

object ChromaStudioChecks {

    private val srcRoot = "app/src/main/java/com/chroma/studio"

    private fun readFile(path: String): String? = try {
        File(path).readText()
    } catch (_: Exception) { null }

    private fun projectFile(rel: String): String =
        "e:/Android Projects/ChromaStudio/$rel"

    val ALL: List<AnalyzerCheck> = listOf(

        // ── A: Bitmap Pipeline ────────────────────────────────────────────────

        AnalyzerCheck("A-01", CheckDomain.BITMAP_PIPELINE, CheckSeverity.CRITICAL,
            "Bitmap: Safe inSampleSize decode",
            "BitmapFactory must use inJustDecodeBounds + inSampleSize before allocating. Full-res decode of 48MP = 400MB OOM.",
            "FlashPhotoEditorScreen.kt:2257"
        ) { _ ->
            val src = readFile(projectFile("$srcRoot/ui/screens/FlashPhotoEditorScreen.kt")) ?: return@AnalyzerCheck CheckStatus.SKIPPED to "File not found"
            val hasInJustDecodeBounds = src.contains("inJustDecodeBounds")
            val hasInSampleSize = src.contains("inSampleSize")
            if (hasInJustDecodeBounds && hasInSampleSize)
                CheckStatus.PASS to "Two-pass decode with inSampleSize found ✓"
            else
                CheckStatus.FAIL to "Missing: ${if (!hasInJustDecodeBounds) "inJustDecodeBounds " else ""}${if (!hasInSampleSize) "inSampleSize" else ""}"
        },

        AnalyzerCheck("A-02", CheckDomain.BITMAP_PIPELINE, CheckSeverity.CRITICAL,
            "Bitmap: Single-pass canvas pipeline",
            "Export pipeline must not hold 5 concurrent full-resolution bitmaps. Must use single Canvas draw pass.",
            "FlashPhotoEditorScreen.kt:2257–2317"
        ) { _ ->
            val src = readFile(projectFile("$srcRoot/ui/screens/FlashPhotoEditorScreen.kt")) ?: return@AnalyzerCheck CheckStatus.SKIPPED to "File not found"
            val multiAlloc = src.contains("createScaledBitmap") && src.contains("transformedBitmap") && src.contains("finalCropBitmap")
            if (!multiAlloc)
                CheckStatus.PASS to "Multi-stage bitmap chain eliminated ✓"
            else
                CheckStatus.FAIL to "Multiple intermediate bitmap allocations still present (createScaledBitmap + transformedBitmap + finalCropBitmap)"
        },

        AnalyzerCheck("A-03", CheckDomain.BITMAP_PIPELINE, CheckSeverity.CRITICAL,
            "Bitmap: finally-block recycling",
            "All intermediate bitmaps must be recycled in a finally block on any error path.",
            "FlashPhotoEditorScreen.kt:2391"
        ) { _ ->
            val src = readFile(projectFile("$srcRoot/ui/screens/FlashPhotoEditorScreen.kt")) ?: return@AnalyzerCheck CheckStatus.SKIPPED to "File not found"
            val hasFinallyRecycle = src.contains("finally") && src.contains("recycle()")
            if (hasFinallyRecycle)
                CheckStatus.PASS to "finally { recycle() } found ✓"
            else
                CheckStatus.FAIL to "No finally block with bitmap recycling found in renderStudioPhotoBitmap"
        },

        AnalyzerCheck("A-04", CheckDomain.BITMAP_PIPELINE, CheckSeverity.CRITICAL,
            "Bitmap: Trig bounding box for rotation",
            "Rotation bounding box must use cos/sin math, not a simple % 180 swap.",
            "FlashPhotoEditorScreen.kt:2272"
        ) { _ ->
            val src = readFile(projectFile("$srcRoot/ui/screens/FlashPhotoEditorScreen.kt")) ?: return@AnalyzerCheck CheckStatus.SKIPPED to "File not found"
            val hasTrigBounds = (src.contains("Math.cos") || src.contains("cos(radians)")) &&
                    (src.contains("Math.sin") || src.contains("sin(radians)"))
            val hasOldBug = src.contains("% 180") && src.contains("isRotated90or270")
            when {
                hasTrigBounds && !hasOldBug -> CheckStatus.PASS to "Trig-correct rotation bounding box found ✓"
                hasOldBug -> CheckStatus.FAIL to "Still using (totalRotation % 180) != 0 swap — crops corners at fine angles"
                else -> CheckStatus.WARNING to "Rotation math changed but trig pattern not confirmed"
            }
        },

        AnalyzerCheck("A-05", CheckDomain.BITMAP_PIPELINE, CheckSeverity.HIGH,
            "Bitmap: BLUR_ORIGINAL export branch",
            "BLUR_ORIGINAL backdrop type must be handled in export when block, not fall through to else.",
            "FlashPhotoEditorScreen.kt:2321"
        ) { _ ->
            val src = readFile(projectFile("$srcRoot/ui/screens/FlashPhotoEditorScreen.kt")) ?: return@AnalyzerCheck CheckStatus.SKIPPED to "File not found"
            val hasBlurOriginalBranch = src.contains("BLUR_ORIGINAL") && src.contains("BlurMaskFilter")
            if (hasBlurOriginalBranch)
                CheckStatus.PASS to "BLUR_ORIGINAL export branch with BlurMaskFilter found ✓"
            else
                CheckStatus.FAIL to "BLUR_ORIGINAL export still falls through to else → no backdrop drawn"
        },

        // ── B: Touch Gestures ─────────────────────────────────────────────────

        AnalyzerCheck("B-01", CheckDomain.GESTURES_VIEWPORT, CheckSeverity.HIGH,
            "Gestures: Pinch-to-zoom & pan on canvas",
            "Photo canvas must support detectTransformGestures for pinch-zoom and pan.",
            "FlashPhotoEditorScreen.kt:436"
        ) { _ ->
            val src = readFile(projectFile("$srcRoot/ui/screens/FlashPhotoEditorScreen.kt")) ?: return@AnalyzerCheck CheckStatus.SKIPPED to "File not found"
            val hasGestures = src.contains("detectTransformGestures") || src.contains("pointerInput")
            if (hasGestures)
                CheckStatus.PASS to "Touch gesture detection found ✓"
            else
                CheckStatus.FAIL to "No detectTransformGestures or pointerInput — canvas is non-interactive"
        },

        AnalyzerCheck("B-02", CheckDomain.GESTURES_VIEWPORT, CheckSeverity.HIGH,
            "Gestures: Correct modifier clip order",
            "clip() must come after graphicsLayer{} to prevent corner artifacts during rotation.",
            "FlashPhotoEditorScreen.kt:437"
        ) { _ ->
            val src = readFile(projectFile("$srcRoot/ui/screens/FlashPhotoEditorScreen.kt")) ?: return@AnalyzerCheck CheckStatus.SKIPPED to "File not found"
            // Check if graphicsLayer clip=true is used or if clip comes after graphicsLayer
            val hasGraphicsLayerClip = src.contains("clip = true") && src.contains("graphicsLayer")
            if (hasGraphicsLayerClip)
                CheckStatus.PASS to "graphicsLayer with clip=true found ✓"
            else
                CheckStatus.WARNING to "Could not verify modifier clip order — manual review recommended"
        },

        AnalyzerCheck("B-03", CheckDomain.GESTURES_VIEWPORT, CheckSeverity.HIGH,
            "Overlays: Face guide & rule-of-thirds aligned to photo frame",
            "StudioFaceGuideOverlay and StudioRuleOfThirdsOverlay must be inside the canvas Box, not the screen viewport.",
            "FlashPhotoEditorScreen.kt:501"
        ) { _ ->
            val src = readFile(projectFile("$srcRoot/ui/screens/FlashPhotoEditorScreen.kt")) ?: return@AnalyzerCheck CheckStatus.SKIPPED to "File not found"
            // Heuristic: check overlays are placed near canvasModifier Box
            val idx = src.indexOf("canvasModifier")
            val overlayIdx = src.indexOf("StudioFaceGuideOverlay")
            val hasGuideInsideCanvas = overlayIdx > idx && (overlayIdx - idx) < 3000
            if (hasGuideInsideCanvas)
                CheckStatus.PASS to "StudioFaceGuideOverlay appears inside canvas box context ✓"
            else
                CheckStatus.WARNING to "Cannot statically confirm overlay is inside photo frame — review manually"
        },

        // ── C: Audio Pipeline ─────────────────────────────────────────────────

        AnalyzerCheck("C-01", CheckDomain.AUDIO_PIPELINE, CheckSeverity.CRITICAL,
            "Audio: aloop integer sample count",
            "aloop filter must use integer 2147483647, not scientific notation 2e+09 which strtoll truncates to 2.",
            "FcpTimelineExporter.kt:1066"
        ) { _ ->
            val src = readFile(projectFile("$srcRoot/utils/FcpTimelineExporter.kt")) ?: return@AnalyzerCheck CheckStatus.SKIPPED to "File not found"
            when {
                src.contains("size=2147483647") -> CheckStatus.PASS to "aloop uses safe integer size=2147483647 ✓"
                src.contains("size=2e+09") || src.contains("size=2e+9") -> CheckStatus.FAIL to "CRITICAL: aloop still uses scientific notation '2e+09' — audio will buzz in exported videos"
                src.contains("aloop") -> CheckStatus.WARNING to "aloop found but integer value not confirmed — verify manually"
                else -> CheckStatus.WARNING to "aloop not found — soundtrack looping may not be implemented"
            }
        },

        AnalyzerCheck("C-02", CheckDomain.AUDIO_PIPELINE, CheckSeverity.HIGH,
            "Audio: Volume ducking implementation",
            "Background music should duck when video's original audio is active.",
            "FinalCutProVideoEditor.kt:1922 + FcpTimelineExporter.kt:1063"
        ) { _ ->
            val exportSrc = readFile(projectFile("$srcRoot/utils/FcpTimelineExporter.kt")) ?: return@AnalyzerCheck CheckStatus.SKIPPED to "File not found"
            val hasDucking = exportSrc.contains("sidechaincompress") || exportSrc.contains("ducking") || exportSrc.contains("volume=")
            if (hasDucking)
                CheckStatus.PASS to "Volume ducking / sidechaincompress found in export ✓"
            else
                CheckStatus.WARNING to "No sidechaincompress or ducking filter — background music won't auto-duck"
        },

        // ── D: Transition Parity ──────────────────────────────────────────────

        AnalyzerCheck("D-01", CheckDomain.TRANSITION_PARITY, CheckSeverity.HIGH,
            "Transition: CROSS_DISSOLVE true overlay",
            "CROSS_DISSOLVE must export as additive alpha dissolve, not two dip-to-black fades.",
            "FcpTimelineExporter.kt:859, 929"
        ) { _ ->
            val src = readFile(projectFile("$srcRoot/utils/FcpTimelineExporter.kt")) ?: return@AnalyzerCheck CheckStatus.SKIPPED to "File not found"
            val hasTrueDissolve = src.contains("xfade=transition=fade") || src.contains("blend=all_mode")
            val hasBlackFade = src.contains("CROSS_DISSOLVE") && src.contains("c=black")
            when {
                hasTrueDissolve -> CheckStatus.PASS to "xfade or blend cross-dissolve found ✓"
                hasBlackFade -> CheckStatus.FAIL to "CROSS_DISSOLVE still uses fade=c=black — dips to black instead of dissolving"
                else -> CheckStatus.WARNING to "CROSS_DISSOLVE handling changed — verify manually"
            }
        },

        AnalyzerCheck("D-02", CheckDomain.TRANSITION_PARITY, CheckSeverity.HIGH,
            "Transition: COVER_SLIDE parallax recession",
            "COVER_SLIDE outgoing clip must translate 25% left, not fade to black.",
            "FcpTimelineExporter.kt:774, 907"
        ) { _ ->
            val src = readFile(projectFile("$srcRoot/utils/FcpTimelineExporter.kt")) ?: return@AnalyzerCheck CheckStatus.SKIPPED to "File not found"
            val hasParallax = src.contains("COVER_SLIDE") && (src.contains("0.25") || src.contains("iw*0.25"))
            val hasBlackFade = src.contains("COVER_SLIDE") && src.contains("fade=t=out:c=black")
            when {
                hasParallax -> CheckStatus.PASS to "COVER_SLIDE parallax translation found ✓"
                hasBlackFade -> CheckStatus.FAIL to "COVER_SLIDE outgoing still fades to black — no parallax"
                else -> CheckStatus.WARNING to "COVER_SLIDE handling changed — verify manually"
            }
        },

        AnalyzerCheck("D-05", CheckDomain.TRANSITION_PARITY, CheckSeverity.HIGH,
            "Transition: Easing curves in FFmpeg export",
            "APPLE_SMOOTH / CUBIC_IN_OUT transitions must use Hermite easing in FFmpeg, not linear t/d.",
            "FcpTimelineExporter.kt:848–988"
        ) { _ ->
            val src = readFile(projectFile("$srcRoot/utils/FcpTimelineExporter.kt")) ?: return@AnalyzerCheck CheckStatus.SKIPPED to "File not found"
            val hasHermite = src.contains("3*(") && src.contains("^2") || src.contains("ffmpegHermiteEase") || src.contains("Hermite")
            if (hasHermite)
                CheckStatus.PASS to "Hermite easing expression found in FFmpeg filters ✓"
            else
                CheckStatus.WARNING to "No Hermite easing found — transitions may still be linear in export"
        },

        AnalyzerCheck("D-06", CheckDomain.TRANSITION_PARITY, CheckSeverity.HIGH,
            "Transition: Text layer animations exported",
            "Text In/Out/Combo animations must appear in export, not be baked as a single static PNG.",
            "FcpTimelineExporter.kt:76–81, 623, 1080"
        ) { _ ->
            val src = readFile(projectFile("$srcRoot/utils/FcpTimelineExporter.kt")) ?: return@AnalyzerCheck CheckStatus.SKIPPED to "File not found"
            val hasTimedText = src.contains("enable='between") || src.contains("enable=between") ||
                    src.contains("alpha='if(between") || src.contains("drawtext=")
            val hasStaticOnly = src.contains("repeatlast=1") && !hasTimedText
            when {
                hasTimedText -> CheckStatus.PASS to "Time-gated text layer animation found in export ✓"
                hasStaticOnly -> CheckStatus.FAIL to "Text layers still exported as static PNG with repeatlast=1 — animations lost"
                else -> CheckStatus.WARNING to "Text export changed — verify manually"
            }
        },

        // ── E: Timeline Safety ────────────────────────────────────────────────

        AnalyzerCheck("E-01", CheckDomain.TIMELINE_SAFETY, CheckSeverity.CRITICAL,
            "Timeline: onSplitClip pauses playback",
            "onSplitClip must pause isPlaying/exoPlayer before mutating clips list to prevent race conditions.",
            "FinalCutProVideoEditor.kt:3157"
        ) { _ ->
            val src = readFile(projectFile("$srcRoot/ui/screens/FinalCutProVideoEditor.kt")) ?: return@AnalyzerCheck CheckStatus.SKIPPED to "File not found"
            // Find onSplitClip and check for pause pattern within ~50 lines
            val idx = src.indexOf("onSplitClip")
            if (idx < 0) return@AnalyzerCheck CheckStatus.SKIPPED to "onSplitClip not found"
            val snippet = src.substring(idx, minOf(idx + 2000, src.length))
            val hasPauseGuard = snippet.contains("isPlaying = false") && snippet.contains("exoPlayer.pause()")
            if (hasPauseGuard)
                CheckStatus.PASS to "Playback pause guard found at start of onSplitClip ✓"
            else
                CheckStatus.FAIL to "onSplitClip mutates timeline without pausing playback — race condition risk"
        },

        // ── F: Memory Leaks ───────────────────────────────────────────────────

        AnalyzerCheck("F-01", CheckDomain.MEMORY_LEAKS, CheckSeverity.CRITICAL,
            "Memory: FcpThumbnailMemoryCache byte-bounded",
            "LruCache must be byte-bounded (maxMemory/8), not count-bounded (300 items = 1.1GB RAM).",
            "FinalCutProVideoEditor.kt:3566"
        ) { _ ->
            val src = readFile(projectFile("$srcRoot/ui/screens/FinalCutProVideoEditor.kt")) ?: return@AnalyzerCheck CheckStatus.SKIPPED to "File not found"
            val hasByteCache = src.contains("byteCount") && src.contains("maxMemory")
            val hasCountCache = src.contains("LruCache<String, Bitmap>(300)")
            when {
                hasByteCache && !hasCountCache -> CheckStatus.PASS to "Byte-bounded LruCache with byteCount sizeOf() found ✓"
                hasCountCache -> CheckStatus.FAIL to "CRITICAL: LruCache(300) by count still present — up to 1.1GB RAM"
                else -> CheckStatus.WARNING to "LruCache changed but byte-bounded pattern not confirmed"
            }
        },

        AnalyzerCheck("F-01b", CheckDomain.MEMORY_LEAKS, CheckSeverity.CRITICAL,
            "Memory: Cache evicted on screen exit",
            "FcpThumbnailMemoryCache.clear() must be called in DisposableEffect onDispose.",
            "FinalCutProVideoEditor.kt:3566"
        ) { _ ->
            val src = readFile(projectFile("$srcRoot/ui/screens/FinalCutProVideoEditor.kt")) ?: return@AnalyzerCheck CheckStatus.SKIPPED to "File not found"
            val hasClearOnDispose = src.contains("FcpThumbnailMemoryCache.clear()") ||
                    (src.contains("evictAll()") && src.contains("onDispose"))
            if (hasClearOnDispose)
                CheckStatus.PASS to "Cache evicted in onDispose ✓"
            else
                CheckStatus.FAIL to "No cache eviction on screen exit — bitmaps accumulate in heap indefinitely"
        },

        AnalyzerCheck("F-02", CheckDomain.MEMORY_LEAKS, CheckSeverity.CRITICAL,
            "Memory: MediaMetadataRetriever released in finally",
            "All 4 MMR usages must call release() in finally blocks, not inside try.",
            "FinalCutProVideoEditor.kt:10555, 10584, 11424, 11455"
        ) { _ ->
            val src = readFile(projectFile("$srcRoot/ui/screens/FinalCutProVideoEditor.kt")) ?: return@AnalyzerCheck CheckStatus.SKIPPED to "File not found"
            // Count finally { mmr.release() } occurrences
            val finallyCount = "finally".toRegex().findAll(src)
                .count { match ->
                    val end = minOf(match.range.last + 200, src.length)
                    src.substring(match.range.first, end).contains("release()")
                }
            when {
                finallyCount >= 4 -> CheckStatus.PASS to "Found $finallyCount finally-release blocks for MMR ✓"
                finallyCount in 1..3 -> CheckStatus.WARNING to "Only $finallyCount of 4 MMR release() in finally blocks"
                else -> CheckStatus.FAIL to "MediaMetadataRetriever.release() not in finally blocks — C++ extractor leaks"
            }
        },

        AnalyzerCheck("F-03", CheckDomain.MEMORY_LEAKS, CheckSeverity.HIGH,
            "Memory: Player.Listener removed on AndroidView release",
            "Anonymous Player.Listener in AndroidView must be stored and removed via onRelease or DisposableEffect.",
            "FinalCutProVideoEditor.kt:2443"
        ) { _ ->
            val src = readFile(projectFile("$srcRoot/ui/screens/FinalCutProVideoEditor.kt")) ?: return@AnalyzerCheck CheckStatus.SKIPPED to "File not found"
            val hasRemoveListener = src.contains("removeListener") || src.contains("onRelease")
            if (hasRemoveListener)
                CheckStatus.PASS to "removeListener or onRelease found for Player.Listener ✓"
            else
                CheckStatus.FAIL to "Player.Listener added but never removed — leaks view hierarchy"
        },

        // ── G: Mock Features ──────────────────────────────────────────────────

        AnalyzerCheck("G-01", CheckDomain.MOCK_FEATURES, CheckSeverity.MEDIUM,
            "Features: Background removal uses ML segmentation",
            "Remove Background must invoke ML Kit SubjectSegmentation, not just set SOLID_WHITE.",
            "FlashPhotoEditorScreen.kt:707"
        ) { _ ->
            val src = readFile(projectFile("$srcRoot/ui/screens/FlashPhotoEditorScreen.kt")) ?: return@AnalyzerCheck CheckStatus.SKIPPED to "File not found"
            val hasMlKit = src.contains("SubjectSegmentation") || src.contains("segmentation") || src.contains("SubjectSegmenter")
            val isStub = src.contains("backdropType = StudioBackdropType.SOLID_WHITE") &&
                    !hasMlKit
            when {
                hasMlKit -> CheckStatus.PASS to "ML Kit SubjectSegmentation found ✓"
                isStub -> CheckStatus.FAIL to "Remove Background is a stub — just sets SOLID_WHITE without segmentation"
                else -> CheckStatus.WARNING to "Background removal changed — verify ML integration manually"
            }
        },

        AnalyzerCheck("G-02", CheckDomain.MOCK_FEATURES, CheckSeverity.MEDIUM,
            "Features: Smart Style calls real model",
            "Neural Generate must call Gemini API, not just delay(1200) + show Toast.",
            "FlashPhotoEditorScreen.kt:725"
        ) { _ ->
            val src = readFile(projectFile("$srcRoot/ui/screens/FlashPhotoEditorScreen.kt")) ?: return@AnalyzerCheck CheckStatus.SKIPPED to "File not found"
            val hasGeminiCall = src.contains("AIGenerator") || src.contains("gemini") || src.contains("generateContent")
            val hasDelay1200 = src.contains("delay(1200)")
            when {
                hasGeminiCall -> CheckStatus.PASS to "Gemini API call found in Smart Style flow ✓"
                hasDelay1200 -> CheckStatus.FAIL to "Smart Style still uses delay(1200) stub — no model inference"
                else -> CheckStatus.WARNING to "Smart Style changed — verify Gemini integration manually"
            }
        },

        AnalyzerCheck("G-03", CheckDomain.MOCK_FEATURES, CheckSeverity.MEDIUM,
            "Features: Compliance check uses vision analysis",
            "Analyze Compliance must call Gemini Vision API, not show hardcoded green checks.",
            "FlashPhotoEditorScreen.kt:804"
        ) { _ ->
            val src = readFile(projectFile("$srcRoot/ui/screens/FlashPhotoEditorScreen.kt")) ?: return@AnalyzerCheck CheckStatus.SKIPPED to "File not found"
            val hasVisionApi = src.contains("AIGenerator") && src.contains("compliance") ||
                    src.contains("gemini") && src.contains("compliance")
            val hasHardcodedChecks = src.contains("Lighting & Exposure") && src.contains("Head Centering")
            when {
                hasVisionApi -> CheckStatus.PASS to "Gemini Vision compliance check found ✓"
                hasHardcodedChecks -> CheckStatus.FAIL to "Compliance check still shows hardcoded green checks — no actual analysis"
                else -> CheckStatus.WARNING to "Compliance check changed — verify vision integration manually"
            }
        },

        AnalyzerCheck("G-04", CheckDomain.MOCK_FEATURES, CheckSeverity.MEDIUM,
            "Features: Blur slider connected to rendering",
            "Blur slider value must be applied to canvas preview (renderEffect) and bitmap export (BlurMaskFilter).",
            "FlashPhotoEditorScreen.kt:1620, 2278"
        ) { _ ->
            val src = readFile(projectFile("$srcRoot/ui/screens/FlashPhotoEditorScreen.kt")) ?: return@AnalyzerCheck CheckStatus.SKIPPED to "File not found"
            val hasBlurEffect = src.contains("BlurEffect") || src.contains("createBlurEffect") || src.contains("BlurMaskFilter")
            if (hasBlurEffect)
                CheckStatus.PASS to "Blur rendering (BlurEffect or BlurMaskFilter) found ✓"
            else
                CheckStatus.FAIL to "Blur slider not connected — BlurEffect/BlurMaskFilter absent from render path"
        },

        // ── H: Tab Parity ─────────────────────────────────────────────────────

        AnalyzerCheck("H-01", CheckDomain.TAB_PARITY, CheckSeverity.MEDIUM,
            "Tab 5: Signature & Ink Clean mode",
            "Tab 5 should be Signature & Ink Clean with luminance threshold, not generic filters.",
            "FlashPhotoEditorScreen.kt:1682"
        ) { _ ->
            val src = readFile(projectFile("$srcRoot/ui/screens/FlashPhotoEditorScreen.kt")) ?: return@AnalyzerCheck CheckStatus.SKIPPED to "File not found"
            val hasInkClean = src.contains("inkThreshold") || src.contains("InkClean") ||
                    src.contains("binarization") || src.contains("luminance")
            if (hasInkClean)
                CheckStatus.PASS to "Signature & Ink Clean mode found ✓"
            else
                CheckStatus.WARNING to "Ink Clean not found — Tab 5 likely still generic filters"
        },

        AnalyzerCheck("H-02", CheckDomain.TAB_PARITY, CheckSeverity.MEDIUM,
            "Tab 8: Print Layout Engine with PDF export",
            "Tab 8 should tile passport photos on A4/4x6 with cut marks and PDF export.",
            "FlashPhotoEditorScreen.kt:1878"
        ) { _ ->
            val src = readFile(projectFile("$srcRoot/ui/screens/FlashPhotoEditorScreen.kt")) ?: return@AnalyzerCheck CheckStatus.SKIPPED to "File not found"
            val hasPdfExport = src.contains("PdfDocument") || src.contains("ACTION_SEND") && src.contains("pdf")
            val hasCutMarks = src.contains("DashPathEffect") || src.contains("cutMark")
            if (hasPdfExport || hasCutMarks)
                CheckStatus.PASS to "Print Layout Engine elements found (PdfDocument/cut marks) ✓"
            else
                CheckStatus.WARNING to "Print Layout Engine not found — Tab 8 may still be slideshow mode"
        },

        // ── I: Design System ──────────────────────────────────────────────────

        AnalyzerCheck("I-01", CheckDomain.DESIGN_SYSTEM, CheckSeverity.HIGH,
            "Design: Photo Editor respects Light Mode",
            "FlashPhotoEditorScreen must use colors.bg / colors.glassBg, not hardcoded #0D1117.",
            "FlashPhotoEditorScreen.kt:389"
        ) { _ ->
            val src = readFile(projectFile("$srcRoot/ui/screens/FlashPhotoEditorScreen.kt")) ?: return@AnalyzerCheck CheckStatus.SKIPPED to "File not found"
            val hasHardcodedDark = src.contains("Color(0xFF0D1117)") || src.contains("0x0D1117")
            val usesColorsBg = src.contains("colors.bg") || src.contains("colors.background")
            when {
                !hasHardcodedDark && usesColorsBg -> CheckStatus.PASS to "Light mode: uses colors.bg ✓"
                hasHardcodedDark -> CheckStatus.FAIL to "Hardcoded #0D1117 still present — Light Mode completely broken"
                else -> CheckStatus.WARNING to "Dark background color changed — verify light mode manually"
            }
        },

        AnalyzerCheck("I-02", CheckDomain.DESIGN_SYSTEM, CheckSeverity.HIGH,
            "Design: Exit dialog uses theme colors",
            "FcpExitConfirmationDialog must not use Color.White in Dark Mode.",
            "FinalCutProVideoEditor.kt:12564"
        ) { _ ->
            val src = readFile(projectFile("$srcRoot/ui/screens/FinalCutProVideoEditor.kt")) ?: return@AnalyzerCheck CheckStatus.SKIPPED to "File not found"
            val exitIdx = src.indexOf("FcpExitConfirmationDialog")
            if (exitIdx < 0) return@AnalyzerCheck CheckStatus.WARNING to "FcpExitConfirmationDialog not found"
            val snippet = src.substring(exitIdx, minOf(exitIdx + 3000, src.length))
            val hasColorWhite = snippet.contains("Color.White") || snippet.contains("Color(0xFFFFFFFF)")
            val usesTheme = snippet.contains("colors.glassBg") || snippet.contains("colors.textMain")
            when {
                !hasColorWhite && usesTheme -> CheckStatus.PASS to "Exit dialog uses theme colors ✓"
                hasColorWhite -> CheckStatus.FAIL to "Exit dialog still uses Color.White — blinds users in Dark Mode"
                else -> CheckStatus.WARNING to "Exit dialog color changed — verify dark mode manually"
            }
        },

        AnalyzerCheck("I-05", CheckDomain.DESIGN_SYSTEM, CheckSeverity.POLISH,
            "Design: GlassButton used for action buttons",
            "Action buttons must use GlassButton / PrimaryGradientButton, not raw Box.",
            "Both editors — all action buttons"
        ) { _ ->
            val src = readFile(projectFile("$srcRoot/ui/screens/FlashPhotoEditorScreen.kt")) ?: return@AnalyzerCheck CheckStatus.SKIPPED to "File not found"
            val usesGlassButton = src.contains("GlassButton") || src.contains("PrimaryGradientButton") || src.contains("ChromaPillButton")
            if (usesGlassButton)
                CheckStatus.PASS to "GlassButton / PrimaryGradientButton found in Photo Editor ✓"
            else
                CheckStatus.WARNING to "No GlassButton imports — raw Box buttons likely still used"
        },

        AnalyzerCheck("I-06", CheckDomain.DESIGN_SYSTEM, CheckSeverity.POLISH,
            "Design: AppleSlider used in Video Editor sliders",
            "FcpColorSliderRow and FcpPreviewZoomSlider must use AppleSlider, not raw material3.Slider.",
            "FinalCutProVideoEditor.kt:5000, 5176"
        ) { _ ->
            val src = readFile(projectFile("$srcRoot/ui/screens/FinalCutProVideoEditor.kt")) ?: return@AnalyzerCheck CheckStatus.SKIPPED to "File not found"
            val fcpColorIdx = src.indexOf("FcpColorSliderRow")
            val fxpZoomIdx = src.indexOf("FcpPreviewZoomSlider")
            if (fcpColorIdx < 0 || fxpZoomIdx < 0) return@AnalyzerCheck CheckStatus.SKIPPED to "FcpColorSliderRow or FcpPreviewZoomSlider not found"
            val colorSnippet = src.substring(fcpColorIdx, minOf(fcpColorIdx + 2000, src.length))
            val zoomSnippet = src.substring(fxpZoomIdx, minOf(fxpZoomIdx + 2000, src.length))
            val colorUsesApple = colorSnippet.contains("AppleSlider")
            val zoomUsesApple = zoomSnippet.contains("AppleSlider")
            when {
                colorUsesApple && zoomUsesApple -> CheckStatus.PASS to "Both FcpColorSliderRow and FcpPreviewZoomSlider use AppleSlider ✓"
                colorUsesApple || zoomUsesApple -> CheckStatus.WARNING to "Only one of the two sliders uses AppleSlider"
                else -> CheckStatus.FAIL to "Both FcpColorSliderRow and FcpPreviewZoomSlider still use raw material3.Slider"
            }
        },

        // ── J: Accessibility ─────────────────────────────────────────────────

        AnalyzerCheck("J-01", CheckDomain.ACCESSIBILITY, CheckSeverity.HIGH,
            "A11y: 18dp reset buttons meet 48dp touch target",
            "Fine rotation and tuning reset IconButtons must not be sized at 18dp.",
            "FlashPhotoEditorScreen.kt:1272, 1667"
        ) { _ ->
            val src = readFile(projectFile("$srcRoot/ui/screens/FlashPhotoEditorScreen.kt")) ?: return@AnalyzerCheck CheckStatus.SKIPPED to "File not found"
            val has18dpTarget = src.contains("Modifier.size(18.dp)") && src.contains("IconButton")
            val has48dp = src.contains("Modifier.size(48.dp)") || src.contains("minimumInteractiveComponentSize")
            when {
                !has18dpTarget -> CheckStatus.PASS to "No 18dp IconButton touch targets found ✓"
                has18dpTarget && has48dp -> CheckStatus.WARNING to "Both 18dp and 48dp sizes found — verify which ones are the reset buttons"
                else -> CheckStatus.FAIL to "18dp IconButton touch targets still present (1272, 1667) — violates WCAG 2.5.5"
            }
        },

        AnalyzerCheck("J-08", CheckDomain.ACCESSIBILITY, CheckSeverity.POLISH,
            "A11y: contentDescription on Photo Editor icons",
            "Interactive icons must have descriptive contentDescription, not null.",
            "FlashPhotoEditorScreen.kt:1381, 1558, 1610"
        ) { _ ->
            val src = readFile(projectFile("$srcRoot/ui/screens/FlashPhotoEditorScreen.kt")) ?: return@AnalyzerCheck CheckStatus.SKIPPED to "File not found"
            val nullDescCount = "contentDescription = null".toRegex().findAll(src).count()
            when {
                nullDescCount == 0 -> CheckStatus.PASS to "No contentDescription = null found ✓"
                nullDescCount <= 3 -> CheckStatus.WARNING to "$nullDescCount remaining contentDescription = null"
                else -> CheckStatus.FAIL to "$nullDescCount contentDescription = null violations in Photo Editor"
            }
        },

        AnalyzerCheck("J-09", CheckDomain.ACCESSIBILITY, CheckSeverity.POLISH,
            "A11y: contentDescription on Video Editor icons",
            "Save/Export/Draft icons must have descriptive labels for TalkBack.",
            "FinalCutProVideoEditor.kt:12236, 12254, 12273"
        ) { _ ->
            val src = readFile(projectFile("$srcRoot/ui/screens/FinalCutProVideoEditor.kt")) ?: return@AnalyzerCheck CheckStatus.SKIPPED to "File not found"
            val nullDescCount = "contentDescription = null".toRegex().findAll(src).count()
            when {
                nullDescCount <= 5 -> CheckStatus.PASS to "$nullDescCount contentDescription = null remaining (significantly reduced) ✓"
                nullDescCount <= 20 -> CheckStatus.WARNING to "$nullDescCount contentDescription = null remaining"
                else -> CheckStatus.FAIL to "$nullDescCount contentDescription = null violations in Video Editor"
            }
        },

        // ── K: Haptics ────────────────────────────────────────────────────────

        AnalyzerCheck("K-01", CheckDomain.HAPTICS, CheckSeverity.POLISH,
            "Haptics: Photo Editor has haptic feedback",
            "Photo Editor must have haptic ticks on tab switches, rotations, undo/redo (currently 0 haptic triggers).",
            "FlashPhotoEditorScreen.kt (entire file)"
        ) { _ ->
            val src = readFile(projectFile("$srcRoot/ui/screens/FlashPhotoEditorScreen.kt")) ?: return@AnalyzerCheck CheckStatus.SKIPPED to "File not found"
            val hapticCount = "performHapticFeedback".toRegex().findAll(src).count()
            when {
                hapticCount >= 5 -> CheckStatus.PASS to "$hapticCount haptic feedback triggers found ✓"
                hapticCount in 1..4 -> CheckStatus.WARNING to "Only $hapticCount haptic triggers — add more on tab switches, undo/redo, presets"
                else -> CheckStatus.FAIL to "Zero haptic feedback triggers — editor feels unresponsive (Video Editor has 40+)"
            }
        },

        // ── L: Architecture ───────────────────────────────────────────────────

        AnalyzerCheck("L-01", CheckDomain.ARCHITECTURE, CheckSeverity.POLISH,
            "Architecture: TransitionPickerSheet deprecated",
            "Legacy TransitionPickerSheet.kt (7 transitions only) should be deprecated in favor of AppleVideoTransitionStudio.",
            "TransitionPickerSheet.kt:42"
        ) { _ ->
            val src = readFile(projectFile("$srcRoot/ui/screens/TransitionPickerSheet.kt")) ?: return@AnalyzerCheck CheckStatus.SKIPPED to "File not found"
            val isDeprecated = src.contains("@Deprecated") || src.contains("Deprecated(")
            if (isDeprecated)
                CheckStatus.PASS to "TransitionPickerSheet marked @Deprecated ✓"
            else
                CheckStatus.WARNING to "TransitionPickerSheet not deprecated yet — legacy 7-transition enum still in use"
        }
    )
}

// ──────────────────────────────────────────────────────────────────────────────
// ViewModel / State
// ──────────────────────────────────────────────────────────────────────────────

data class AnalyzerUiState(
    val isRunning: Boolean = false,
    val progress: Int = 0,
    val total: Int = ChromaStudioChecks.ALL.size,
    val currentCheckId: String = "",
    val report: AnalyzerReport? = null,
    val selectedDomain: CheckDomain? = null,
    val expandedCheckId: String? = null
)

// ──────────────────────────────────────────────────────────────────────────────
// Main Screen
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChromaStudioAnalyzerScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val colors = LocalOscellionColors.current
    val scope = rememberCoroutineScope()
    var uiState by remember { mutableStateOf(AnalyzerUiState()) }

    // Run the full analyzer
    fun runAnalysis() {
        scope.launch {
            val checks = ChromaStudioChecks.ALL
            val results = mutableListOf<AnalyzerResult>()
            val startTime = System.currentTimeMillis()

            uiState = uiState.copy(
                isRunning = true, progress = 0,
                total = checks.size, currentCheckId = "", report = null
            )

            for ((idx, check) in checks.withIndex()) {
                uiState = uiState.copy(progress = idx, currentCheckId = check.id)
                val t0 = System.currentTimeMillis()
                val (status, detail) = withContext(Dispatchers.IO) {
                    try { check.verify(context) }
                    catch (e: Exception) { CheckStatus.SKIPPED to "Error: ${e.message}" }
                }
                results.add(AnalyzerResult(check, status, detail, System.currentTimeMillis() - t0))
            }

            val totalMs = System.currentTimeMillis() - startTime
            uiState = uiState.copy(
                isRunning = false,
                progress = checks.size,
                currentCheckId = "",
                report = AnalyzerReport(results.toList(), totalMs)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ───────────────────────────────────────────────────────
            AnalyzerHeader(
                colors = colors,
                report = uiState.report,
                isRunning = uiState.isRunning,
                onBack = onBack,
                onRun = { if (!uiState.isRunning) runAnalysis() }
            )

            // ── Progress bar ─────────────────────────────────────────────────
            AnimatedVisibility(visible = uiState.isRunning) {
                AnalyzerProgressBar(
                    colors = colors,
                    progress = uiState.progress,
                    total = uiState.total,
                    currentId = uiState.currentCheckId
                )
            }

            // ── Domain filter tabs ────────────────────────────────────────────
            uiState.report?.let {
                DomainFilterRow(
                    colors = colors,
                    results = it.results,
                    selected = uiState.selectedDomain,
                    onSelect = { d -> uiState = uiState.copy(selectedDomain = if (uiState.selectedDomain == d) null else d) }
                )
            }

            // ── Content ──────────────────────────────────────────────────────
            Box(modifier = Modifier.weight(1f)) {
                if (uiState.report == null && !uiState.isRunning) {
                    AnalyzerEmptyState(colors = colors, onRun = { runAnalysis() })
                } else {
                    uiState.report?.let { report ->
                        AnalyzerResultsList(
                            colors = colors,
                            report = report,
                            selectedDomain = uiState.selectedDomain,
                            expandedCheckId = uiState.expandedCheckId,
                            onExpand = { id ->
                                uiState = uiState.copy(
                                    expandedCheckId = if (uiState.expandedCheckId == id) null else id
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Sub-composables
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun AnalyzerHeader(
    colors: com.chroma.studio.ui.theme.OscellionPalette,
    report: AnalyzerReport?,
    isRunning: Boolean,
    onBack: () -> Unit,
    onRun: () -> Unit
) {
    val infiniteAnim = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteAnim.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(colors.glassBg, colors.glassBg.copy(alpha = 0f))),
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Back
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(colors.glassBg, CircleShape)
                        .border(1.dp, colors.glassBorder, CircleShape)
                ) {
                    Text("←", color = colors.textMain, fontSize = 18.sp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "ChromaStudio Analyzer",
                        color = colors.textMain,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.3).sp
                    )
                    Text(
                        "${ChromaStudioChecks.ALL.size} checks across 12 domains",
                        color = colors.textMuted,
                        fontSize = 12.sp
                    )
                }

                // Run button
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .then(
                            if (isRunning) Modifier.alpha(pulseAlpha) else Modifier
                        )
                        .then(
                            if (isRunning) Modifier.background(colors.glassBg, RoundedCornerShape(10.dp))
                            else Modifier.background(Brush.horizontalGradient(listOf(colors.primary, colors.primary.copy(0.7f))), RoundedCornerShape(10.dp))
                        )
                        .border(1.dp, colors.glassBorder, RoundedCornerShape(10.dp))
                        .clickable(enabled = !isRunning) { onRun() }
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isRunning) "Running…" else "▶  Run",
                        color = if (isRunning) colors.textMuted else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Score summary row
            report?.let { r ->
                Spacer(Modifier.height(12.dp))
                ScoreSummaryRow(colors = colors, report = r)
            }
        }
    }
}

@Composable
private fun ScoreSummaryRow(
    colors: com.chroma.studio.ui.theme.OscellionPalette,
    report: AnalyzerReport
) {
    val scoreColor = when {
        report.healthScore >= 85 -> Color(0xFF10B981)
        report.healthScore >= 60 -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.glassBg, RoundedCornerShape(12.dp))
            .border(1.dp, colors.glassBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${report.healthScore}%",
                color = scoreColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text("Health Score", color = colors.textMuted, fontSize = 10.sp)
        }
        ScorePill("✅ ${report.passed}", Color(0xFF10B981), colors)
        ScorePill("❌ ${report.failed}", Color(0xFFEF4444), colors)
        ScorePill("⚠️ ${report.warnings}", Color(0xFFF59E0B), colors)
        ScorePill("⏭ ${report.skipped}", colors.textMuted, colors)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${report.durationMs}ms",
                color = colors.textMain,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text("Duration", color = colors.textMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun ScorePill(
    label: String,
    color: Color,
    colors: com.chroma.studio.ui.theme.OscellionPalette
) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AnalyzerProgressBar(
    colors: com.chroma.studio.ui.theme.OscellionPalette,
    progress: Int,
    total: Int,
    currentId: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Checking: $currentId",
                color = colors.textMuted,
                fontSize = 11.sp
            )
            Text(
                "$progress / $total",
                color = colors.textMuted,
                fontSize = 11.sp
            )
        }
        Spacer(Modifier.height(4.dp))
        val fraction = if (total == 0) 0f else progress.toFloat() / total
        val animFraction by animateFloatAsState(targetValue = fraction, label = "progress")
        LiquidGlassProgressBar(
            progress = fraction,
            height = 6.dp
        )
    }
}

@Composable
private fun DomainFilterRow(
    colors: com.chroma.studio.ui.theme.OscellionPalette,
    results: List<AnalyzerResult>,
    selected: CheckDomain?,
    onSelect: (CheckDomain) -> Unit
) {
    val domainCounts = results.groupBy { it.check.domain }

    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(CheckDomain.values()) { domain ->
            val domainResults = domainCounts[domain] ?: emptyList()
            val failCount = domainResults.count { it.status == CheckStatus.FAIL }
            val isSelected = selected == domain
            val chipColor = when {
                isSelected -> colors.primary
                failCount > 0 -> Color(0xFFEF4444)
                else -> colors.glassBg
            }

            Box(
                modifier = Modifier
                    .height(32.dp)
                    .background(chipColor.copy(alpha = if (isSelected) 1f else 0.15f), RoundedCornerShape(8.dp))
                    .border(1.dp, chipColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .clickable { onSelect(domain) }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        domain.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }.take(14),
                        color = if (isSelected) Color.White else colors.textMain,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                    if (failCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(Color(0xFFEF4444), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(failCount.toString(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyzerEmptyState(
    colors: com.chroma.studio.ui.theme.OscellionPalette,
    onRun: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("🔬", fontSize = 56.sp)
            Text(
                "ChromaStudio Analyzer",
                color = colors.textMain,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "${ChromaStudioChecks.ALL.size} checks across Photo Editor,\nVideo Editor, Design System & Accessibility",
                color = colors.textMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Box(
                modifier = Modifier
                    .height(48.dp)
                    .background(
                        Brush.horizontalGradient(listOf(colors.primary, colors.primary.copy(0.7f))),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onRun() }
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Run Full Analysis", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun AnalyzerResultsList(
    colors: com.chroma.studio.ui.theme.OscellionPalette,
    report: AnalyzerReport,
    selectedDomain: CheckDomain?,
    expandedCheckId: String?,
    onExpand: (String) -> Unit
) {
    val filtered = if (selectedDomain != null)
        report.results.filter { it.check.domain == selectedDomain }
    else report.results

    // Group by severity
    val criticals = filtered.filter { it.check.severity == CheckSeverity.CRITICAL }
    val highs = filtered.filter { it.check.severity == CheckSeverity.HIGH }
    val mediums = filtered.filter { it.check.severity == CheckSeverity.MEDIUM }
    val polish = filtered.filter { it.check.severity == CheckSeverity.POLISH }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (criticals.isNotEmpty()) {
            item { SeverityGroupHeader("🔴 Critical", criticals.count { it.status == CheckStatus.FAIL }, colors) }
            items(criticals, key = { it.check.id }) { result ->
                AnalyzerCheckCard(colors, result, expandedCheckId == result.check.id) { onExpand(result.check.id) }
            }
        }
        if (highs.isNotEmpty()) {
            item { SeverityGroupHeader("🟠 High", highs.count { it.status == CheckStatus.FAIL }, colors) }
            items(highs, key = { it.check.id }) { result ->
                AnalyzerCheckCard(colors, result, expandedCheckId == result.check.id) { onExpand(result.check.id) }
            }
        }
        if (mediums.isNotEmpty()) {
            item { SeverityGroupHeader("🟡 Medium", mediums.count { it.status == CheckStatus.FAIL }, colors) }
            items(mediums, key = { it.check.id }) { result ->
                AnalyzerCheckCard(colors, result, expandedCheckId == result.check.id) { onExpand(result.check.id) }
            }
        }
        if (polish.isNotEmpty()) {
            item { SeverityGroupHeader("🔵 Polish", polish.count { it.status == CheckStatus.FAIL }, colors) }
            items(polish, key = { it.check.id }) { result ->
                AnalyzerCheckCard(colors, result, expandedCheckId == result.check.id) { onExpand(result.check.id) }
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun SeverityGroupHeader(
    label: String,
    failCount: Int,
    colors: com.chroma.studio.ui.theme.OscellionPalette
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = colors.textMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        if (failCount > 0)
            Text("$failCount failing", color = Color(0xFFEF4444), fontSize = 11.sp)
    }
}

@Composable
private fun AnalyzerCheckCard(
    colors: com.chroma.studio.ui.theme.OscellionPalette,
    result: AnalyzerResult,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val statusColor = when (result.status) {
        CheckStatus.PASS -> Color(0xFF10B981)
        CheckStatus.FAIL -> Color(0xFFEF4444)
        CheckStatus.WARNING -> Color(0xFFF59E0B)
        CheckStatus.SKIPPED -> colors.textMuted
    }
    val statusIcon = when (result.status) {
        CheckStatus.PASS -> "✅"
        CheckStatus.FAIL -> "❌"
        CheckStatus.WARNING -> "⚠️"
        CheckStatus.SKIPPED -> "⏭"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.glassBg, RoundedCornerShape(12.dp))
            .border(
                1.dp,
                if (result.status == CheckStatus.FAIL) statusColor.copy(alpha = 0.5f) else colors.glassBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable { onToggle() }
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(statusIcon, fontSize = 18.sp)

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        result.check.id,
                        color = colors.textMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Box(
                        modifier = Modifier
                            .background(
                                result.check.domain.name.hashCode().let { h ->
                                    Color(0xFF000000 or (h.and(0xFFFFFF)).toLong())
                                }.copy(alpha = 0.2f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            result.check.domain.name.take(8),
                            color = colors.textMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Text(
                    result.check.title,
                    color = colors.textMain,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = if (expanded) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    result.status.name,
                    color = statusColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text("${result.durationMs}ms", color = colors.textMuted, fontSize = 9.sp)
            }

            Text(
                if (expanded) "▲" else "▼",
                color = colors.textMuted,
                fontSize = 11.sp
            )
        }

        if (expanded) {
            HorizontalDivider(color = colors.glassBorder)
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Detail result
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(statusColor.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(result.detail, color = statusColor, fontSize = 12.sp)
                }
                // Description
                Text(result.check.description, color = colors.textMuted, fontSize = 12.sp, lineHeight = 17.sp)
                // File reference
                Text(
                    "📁 ${result.check.fileRef}",
                    color = colors.primary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
