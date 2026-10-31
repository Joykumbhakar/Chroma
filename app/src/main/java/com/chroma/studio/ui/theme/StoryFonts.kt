package com.chroma.studio.ui.theme

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.text.font.FontFamily

/**
 * Metadata and loader for custom fonts located in `assets/fonts/`.
 */
data class StoryFontItem(
    val id: String,
    val displayName: String,
    val assetFileName: String,
    val styleCategory: String = "Custom"
)

object StoryFonts {
    val items = listOf(
        StoryFontItem(id = "Sans", displayName = "Inter", assetFileName = "", styleCategory = "Default"),
        StoryFontItem(id = "Serif", displayName = "Serif", assetFileName = "", styleCategory = "Default"),
        StoryFontItem(id = "Mono", displayName = "Mono", assetFileName = "", styleCategory = "Default"),
        StoryFontItem(id = "Cursive", displayName = "Script", assetFileName = "", styleCategory = "Default"),

        // Custom fonts from assets/fonts/
        StoryFontItem(id = "Comfortaa", displayName = "Comfortaa", assetFileName = "Comfortaa-Bold.ttf", styleCategory = "Rounded"),
        StoryFontItem(id = "HighSummit", displayName = "High Summit", assetFileName = "HighSummit.ttf", styleCategory = "Display"),
        StoryFontItem(id = "Istok", displayName = "Istok", assetFileName = "Istok-Regular.ttf", styleCategory = "Clean"),
        StoryFontItem(id = "Malvides", displayName = "Malvides", assetFileName = "Malvides.otf", styleCategory = "Script"),
        StoryFontItem(id = "Mayonice", displayName = "Mayonice", assetFileName = "Mayonice.ttf", styleCategory = "Vintage"),
        StoryFontItem(id = "Nunito", displayName = "Nunito", assetFileName = "Nunito-Bold.ttf", styleCategory = "Modern"),
        StoryFontItem(id = "Outfit", displayName = "Outfit", assetFileName = "Outfit-Bold.ttf", styleCategory = "Geometric"),
        StoryFontItem(id = "Quicksand", displayName = "Quicksand", assetFileName = "Quicksand-Bold.ttf", styleCategory = "Soft"),
        StoryFontItem(id = "RainyDays", displayName = "Rainy Days", assetFileName = "rainydays.ttf", styleCategory = "Playful"),
        StoryFontItem(id = "SouthTown", displayName = "South Town", assetFileName = "SouthTown.otf", styleCategory = "Editorial"),
        StoryFontItem(id = "TheSignature", displayName = "Signature", assetFileName = "Thesignature.ttf", styleCategory = "Handwritten"),
        StoryFontItem(id = "Wotham", displayName = "Wotham", assetFileName = "Wotham.ttf", styleCategory = "Bold Display")
    )

    private val fontCache = mutableMapOf<String, FontFamily>()

    fun getFontFamily(context: Context, fontIdOrName: String?): FontFamily {
        if (fontIdOrName.isNullOrBlank() || fontIdOrName == "Sans") return InterFontFamily
        if (fontIdOrName == "Serif") return FontFamily.Serif
        if (fontIdOrName == "Mono") return MonoFontFamily
        if (fontIdOrName == "Cursive") return FontFamily.Cursive

        val item = items.find { it.id.equals(fontIdOrName, ignoreCase = true) || it.displayName.equals(fontIdOrName, ignoreCase = true) || it.assetFileName.equals(fontIdOrName, ignoreCase = true) }
        val assetName = item?.assetFileName ?: fontIdOrName

        if (assetName.isBlank()) return InterFontFamily

        return fontCache.getOrPut(assetName) {
            try {
                val tf = Typeface.createFromAsset(context.assets, "fonts/$assetName")
                FontFamily(tf)
            } catch (e: Exception) {
                try {
                    // Try without spaces or with fallback extension
                    val altName = assetName.replace(" ", "")
                    val tfAlt = Typeface.createFromAsset(context.assets, "fonts/$altName")
                    FontFamily(tfAlt)
                } catch (e2: Exception) {
                    InterFontFamily
                }
            }
        }
    }
}
