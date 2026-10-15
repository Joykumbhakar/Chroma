package com.chroma.studio.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.chroma.studio.R

/**
 * Data model for a selectable profile banner preset.
 */
data class BannerPreset(
    val id: String,
    val name: String,
    val category: String, // "Apple", "Dark Tech", "Abstract", "Minimal"
    @DrawableRes val drawableRes: Int
)

/**
 * Curated catalog of professional vector profile banners.
 */
val AllBannerPresets: List<BannerPreset> = listOf(
    BannerPreset(id = "preset:wave_1", name = "Amethyst Spark", category = "Waves", drawableRes = R.drawable.banner_wave_banner_amethyst_spark),
    BannerPreset(id = "preset:wave_2", name = "Berry Purple", category = "Waves", drawableRes = R.drawable.banner_wave_banner_berry_purple),
    BannerPreset(id = "preset:wave_3", name = "Bronze Ember", category = "Waves", drawableRes = R.drawable.banner_wave_banner_bronze_ember),
    BannerPreset(id = "preset:wave_4", name = "Cherry Blossom", category = "Waves", drawableRes = R.drawable.banner_wave_banner_cherry_blossom),
    BannerPreset(id = "preset:wave_5", name = "Coral Sunset", category = "Waves", drawableRes = R.drawable.banner_wave_banner_coral_sunset),
    BannerPreset(id = "preset:wave_6", name = "Corporate Azure", category = "Waves", drawableRes = R.drawable.banner_wave_banner_corporate_azure),
    BannerPreset(id = "preset:wave_7", name = "Deep Rose", category = "Waves", drawableRes = R.drawable.banner_wave_banner_deep_rose),
    BannerPreset(id = "preset:wave_8", name = "Electric Blue", category = "Waves", drawableRes = R.drawable.banner_wave_banner_electric_blue),
    BannerPreset(id = "preset:wave_9", name = "Emerald Green", category = "Waves", drawableRes = R.drawable.banner_wave_banner_emerald_green),
    BannerPreset(id = "preset:wave_10", name = "Forest Mint", category = "Waves", drawableRes = R.drawable.banner_wave_banner_forest_mint),
    BannerPreset(id = "preset:wave_11", name = "Golden Amber", category = "Waves", drawableRes = R.drawable.banner_wave_banner_golden_amber),
    BannerPreset(id = "preset:wave_12", name = "Magenta Wave", category = "Waves", drawableRes = R.drawable.banner_wave_banner_magenta_wave),
    BannerPreset(id = "preset:wave_13", name = "Midnight Glow", category = "Waves", drawableRes = R.drawable.banner_wave_banner_midnight_glow),
    BannerPreset(id = "preset:wave_14", name = "Ocean Cyan", category = "Waves", drawableRes = R.drawable.banner_wave_banner_ocean_cyan),
    BannerPreset(id = "preset:wave_15", name = "Royal Indigo", category = "Waves", drawableRes = R.drawable.banner_wave_banner_royal_indigo),
    BannerPreset(id = "preset:wave_16", name = "Ruby Red", category = "Waves", drawableRes = R.drawable.banner_wave_banner_ruby_red),
    BannerPreset(id = "preset:wave_17", name = "Slate Blue", category = "Waves", drawableRes = R.drawable.banner_wave_banner_slate_blue),
    BannerPreset(id = "preset:wave_18", name = "Sunset Orange", category = "Waves", drawableRes = R.drawable.banner_wave_banner_sunset_orange),
    BannerPreset(id = "preset:wave_19", name = "Teal Aqua", category = "Waves", drawableRes = R.drawable.banner_wave_banner_teal_aqua),
    BannerPreset(id = "preset:wave_20", name = "Vibrant Violet", category = "Waves", drawableRes = R.drawable.banner_wave_banner_vibrant_violet),
    BannerPreset(
        id = "preset:banner_cupertino_mesh",
        name = "Cupertino Mesh",
        category = "Apple",
        drawableRes = R.drawable.banner_cupertino_mesh
    ),
    BannerPreset(
        id = "preset:banner_dark_titanium",
        name = "Dark Titanium",
        category = "Dark Tech",
        drawableRes = R.drawable.banner_dark_titanium
    ),
    BannerPreset(
        id = "preset:banner_abstract_waves",
        name = "Ocean Waves",
        category = "Abstract",
        drawableRes = R.drawable.banner_abstract_waves
    ),
    BannerPreset(
        id = "preset:banner_cyber_circuit",
        name = "Cyber Circuit",
        category = "Dark Tech",
        drawableRes = R.drawable.banner_cyber_circuit
    ),
    BannerPreset(
        id = "preset:banner_minimalist_geometry",
        name = "Minimal Slate",
        category = "Minimal",
        drawableRes = R.drawable.banner_minimalist_geometry
    )
)

/**
 * Resolves a banner preset string (e.g. "preset:banner_cupertino_mesh") to a drawable resource ID.
 */
fun getBannerPresetDrawableRes(bannerUrl: String?): Int? {
    if (bannerUrl == null || !bannerUrl.startsWith("preset:")) return null
    return AllBannerPresets.find { it.id == bannerUrl }?.drawableRes
}
