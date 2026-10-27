package com.chroma.studio.ml.safety

/**
 * Data structures and classification categories for ChromaStudio On-Device Content Moderation.
 */
enum class SafetyCategory(val displayName: String, val defaultThreshold: Float, val explanation: String) {
    NUDITY_EXPLICIT(
        displayName = "Explicit Nudity / Adult Acts",
        defaultThreshold = 0.55f,
        explanation = "Full or partial nudity, exposed genitalia, or explicit sexual interactions."
    ),
    ADULT_SUGGESTIVE(
        displayName = "Suggestive Adult Content",
        defaultThreshold = 0.65f,
        explanation = "Exposed intimate body areas, erotic underwear/lingerie, or sexually provocative poses."
    ),
    VIOLENCE_WEAPONS(
        displayName = "Graphic Violence / Weapons",
        defaultThreshold = 0.55f,
        explanation = "Visible blood, traumatic gore, or brandished firearms/weapons."
    ),
    ILLICIT_SUBSTANCES(
        displayName = "Illegal Contraband / Drugs",
        defaultThreshold = 0.60f,
        explanation = "Illegal narcotics, substance paraphernalia, or prohibited contraband."
    ),
    SAFE(
        displayName = "Safe for Community",
        defaultThreshold = 0.50f,
        explanation = "Appropriate content complying with Chroma Community Guidelines."
    )
}

enum class SafetyVerdict {
    SAFE,
    SUSPICIOUS,
    PROHIBITED
}

data class SafetyViolation(
    val category: SafetyCategory,
    val confidence: Float,
    val details: String
)

data class SafetyScanResult(
    val verdict: SafetyVerdict,
    val violations: List<SafetyViolation>,
    val categoryScores: Map<SafetyCategory, Float>,
    val scanDurationMs: Long
) {
    val isSafe: Boolean get() = verdict == SafetyVerdict.SAFE && violations.isEmpty()
    val primaryViolation: SafetyViolation? get() = violations.maxByOrNull { it.confidence }
}
