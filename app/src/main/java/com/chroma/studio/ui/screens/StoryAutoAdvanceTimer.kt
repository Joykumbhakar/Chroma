package com.chroma.studio.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlin.math.roundToInt

/**
 * Shared lifecycle observer so story auto-advance timers pause whenever the viewer leaves the foreground.
 */
@Composable
fun rememberStoryLifecycleResumed(): Boolean {
    val lifecycleOwner = LocalLifecycleOwner.current
    var isLifecycleResumed by remember(lifecycleOwner) {
        mutableStateOf(
            lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        )
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isLifecycleResumed = true
                Lifecycle.Event.ON_PAUSE -> isLifecycleResumed = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return isLifecycleResumed
}

/**
 * Shared timing configuration and pure helpers for story auto-advance timers.
 */
object StoryAutoAdvanceDefaults {
    const val DEFAULT_MEDIA_DURATION_MS = 5_000L
    const val BASE_TEXT_ONLY_DURATION_MS = 5_000L
    const val MIN_DURATION_MS = 5_000L
    const val MAX_DURATION_MS = 12_000L
    const val BASE_WORDS_INCLUDED = 8
    const val MS_PER_WORD = 220L
    const val INTERACTIVE_WIDGET_BONUS_MS = 2_000L
    const val MIN_ANIMATION_STEP_MS = 16
    const val COMPLETION_EPSILON = 0.999f
}

/**
 * Counts whitespace-delimited words in [text] for reading-time estimation.
 */
fun countStoryWords(text: String): Int {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return 0
    return trimmed.split(Regex("\\s+")).count { it.isNotBlank() }
}

/**
 * Returns true if the story embeds interactive widgets (polls, quizzes, sliders, questions, links, or countdowns).
 */
fun hasInteractiveStoryWidgets(textFormatJson: String, hasInlinePoll: Boolean = false): Boolean {
    if (hasInlinePoll) return true
    if (textFormatJson.isBlank() || textFormatJson == "{}") return false
    if (!textFormatJson.contains("\"widgets\"", ignoreCase = true)) return false
    return textFormatJson.contains("\"poll\"", ignoreCase = true) ||
        textFormatJson.contains("\"quiz\"", ignoreCase = true) ||
        textFormatJson.contains("\"slider\"", ignoreCase = true) ||
        textFormatJson.contains("\"question\"", ignoreCase = true) ||
        textFormatJson.contains("\"link\"", ignoreCase = true) ||
        textFormatJson.contains("\"countdown\"", ignoreCase = true)
}

/**
 * Computes an adaptive auto-advance duration (5s–12s) based on story media, word count, and interactive widgets.
 */
fun computeStoryAutoAdvanceDurationMs(
    hasVisualMedia: Boolean,
    textContent: String,
    hasInteractiveWidgets: Boolean = false,
    explicitDurationMs: Long? = null
): Long {
    if (explicitDurationMs != null && explicitDurationMs > 0L) {
        return explicitDurationMs.coerceIn(
            StoryAutoAdvanceDefaults.MIN_DURATION_MS,
            StoryAutoAdvanceDefaults.MAX_DURATION_MS
        )
    }
    val wordCount = countStoryWords(textContent)
    val extraWords = (wordCount - StoryAutoAdvanceDefaults.BASE_WORDS_INCLUDED).coerceAtLeast(0)
    val readingExtensionMs = extraWords * StoryAutoAdvanceDefaults.MS_PER_WORD
    val widgetBonusMs = if (hasInteractiveWidgets) StoryAutoAdvanceDefaults.INTERACTIVE_WIDGET_BONUS_MS else 0L
    val baseMs = if (hasVisualMedia) {
        StoryAutoAdvanceDefaults.DEFAULT_MEDIA_DURATION_MS
    } else {
        StoryAutoAdvanceDefaults.BASE_TEXT_ONLY_DURATION_MS
    }
    return (baseMs + readingExtensionMs + widgetBonusMs).coerceIn(
        StoryAutoAdvanceDefaults.MIN_DURATION_MS,
        StoryAutoAdvanceDefaults.MAX_DURATION_MS
    )
}

/**
 * Advances normalized story progress in [0f, 1f] using real elapsed frame milliseconds without losing state on pause/resume.
 */
fun advanceStoryProgress(
    currentProgress: Float,
    deltaMs: Long,
    totalDurationMs: Long
): Float {
    val safeProgress = currentProgress.coerceIn(0f, 1f)
    if (deltaMs <= 0L || totalDurationMs <= 0L) return safeProgress
    val step = deltaMs.toFloat() / totalDurationMs.toFloat()
    return (safeProgress + step).coerceIn(0f, 1f)
}

/**
 * Computes the remaining animation duration in milliseconds from [currentProgress] in [0f, 1f].
 */
fun computeRemainingStoryDurationMs(
    currentProgress: Float,
    totalDurationMs: Long
): Int {
    val safeTotal = totalDurationMs.coerceAtLeast(1L)
    val remainingFraction = (1f - currentProgress.coerceIn(0f, 1f)).coerceIn(0f, 1f)
    if (remainingFraction <= 0f || isStoryProgressComplete(currentProgress)) return 0
    return (safeTotal * remainingFraction)
        .roundToInt()
        .coerceAtLeast(StoryAutoAdvanceDefaults.MIN_ANIMATION_STEP_MS)
}

/**
 * Returns true once [progress] has reached the auto-advance completion threshold.
 */
fun isStoryProgressComplete(progress: Float): Boolean =
    progress >= StoryAutoAdvanceDefaults.COMPLETION_EPSILON
