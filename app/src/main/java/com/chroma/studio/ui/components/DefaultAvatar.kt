package com.chroma.studio.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.R

/**
 * Data model for a selectable avatar preset.
 */
data class AvatarPreset(
    val id: String,
    val name: String,
    val category: String, // "Male", "Female", "Creative", "Special"
    @DrawableRes val drawableRes: Int
)

/**
 * Curated catalog of beautiful high-res vector avatar presets.
 */
val AllAvatarPresets: List<AvatarPreset> = listOf(
    AvatarPreset("preset:burgundy_board", "Burgundy", "Modern", R.drawable.ic_avatar_burgundy_board),
    AvatarPreset("preset:charcoal", "Charcoal", "Modern", R.drawable.ic_avatar_charcoal),
    AvatarPreset("preset:classic_mac", "Classic Mac", "Modern", R.drawable.ic_avatar_classic_mac),
    AvatarPreset("preset:corporate_navy", "Navy", "Modern", R.drawable.ic_avatar_corporate_navy),
    AvatarPreset("preset:deep_purple", "Deep Purple", "Modern", R.drawable.ic_avatar_deep_purple),
    AvatarPreset("preset:emerald_focus", "Emerald", "Modern", R.drawable.ic_avatar_emerald_focus),
    AvatarPreset("preset:executive_forest", "Forest", "Modern", R.drawable.ic_avatar_executive_forest),
    AvatarPreset("preset:gold_standard", "Gold", "Modern", R.drawable.ic_avatar_gold_standard),
    AvatarPreset("preset:midnight", "Midnight", "Modern", R.drawable.ic_avatar_midnight),
    AvatarPreset("preset:ocean_depth", "Ocean", "Modern", R.drawable.ic_avatar_ocean_depth),
    AvatarPreset("preset:royal_blue", "Royal Blue", "Modern", R.drawable.ic_avatar_royal_blue),
    AvatarPreset("preset:sapphire_night", "Sapphire", "Modern", R.drawable.ic_avatar_sapphire_night),
    AvatarPreset("preset:slate_blue", "Slate", "Modern", R.drawable.ic_avatar_slate_blue),
    AvatarPreset("preset:space_gray", "Space Gray", "Modern", R.drawable.ic_avatar_space_gray),
    AvatarPreset("preset:steel_grey", "Steel", "Modern", R.drawable.ic_avatar_steel_grey),
    AvatarPreset("preset:teal_pro", "Teal", "Modern", R.drawable.ic_avatar_teal_pro)
)

/**
 * Look up preset by id or raw name.
 */
fun getAvatarPresetById(id: String?): AvatarPreset? {
    if (id.isNullOrBlank()) return null
    val cleanId = if (!id.startsWith("preset:")) "preset:$id" else id
    return AllAvatarPresets.firstOrNull { it.id.equals(cleanId, ignoreCase = true) || it.id.removePrefix("preset:").equals(id.removePrefix("preset:"), ignoreCase = true) }
}

/**
 * Returns whether a given string is a preset avatar identifier.
 */
fun isPresetAvatar(urlOrId: String?): Boolean {
    if (urlOrId.isNullOrBlank()) return false
    if (urlOrId.startsWith("preset:")) return true
    return AllAvatarPresets.any { it.id.removePrefix("preset:").equals(urlOrId, ignoreCase = true) }
}

/**
 * Resolves any avatar URL or preset ID into a local vector drawable resource if applicable.
 */
@DrawableRes
fun getAvatarPresetDrawableRes(urlOrId: String?): Int? {
    if (urlOrId.isNullOrBlank()) return null
    return getAvatarPresetById(urlOrId)?.drawableRes
}

/**
 * Resolves default vector avatar drawable resource.
 * Returns Apple style default avatar if gender is unspecified.
 */
@DrawableRes
fun getDefaultAvatarDrawableRes(gender: String?): Int {
    if (gender.isNullOrBlank()) return R.drawable.default_avatar_apple
    return when (gender.trim().lowercase()) {
        "male", "man", "boy", "he", "he/him" -> R.drawable.default_avatar_male
        "female", "woman", "girl", "she", "she/her" -> R.drawable.default_avatar_female
        else -> R.drawable.default_avatar_apple
    }
}

/**
 * Returns whether a gender has a dedicated default avatar artwork.
 */
fun hasDefaultGenderAvatar(gender: String?): Boolean {
    return getDefaultAvatarDrawableRes(gender) != null
}

/**
 * Dedicated Composable to render the default gender or preset avatar.
 */
@Composable
fun DefaultGenderAvatar(
    gender: String? = null,
    avatarUrlOrPreset: String? = null,
    name: String = "",
    size: Dp = 52.dp,
    modifier: Modifier = Modifier
) {
    val presetRes = remember(avatarUrlOrPreset) { getAvatarPresetDrawableRes(avatarUrlOrPreset) }
    val genderRes = remember(gender) { getDefaultAvatarDrawableRes(gender) }
    val avatarRes = presetRes ?: genderRes
    val gradient = remember(name) { getAvatarGradient(name) }
    val initials = remember(name) { getInitials(name) }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.1f))
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (avatarRes != null) {
            Image(
                painter = painterResource(id = avatarRes),
                contentDescription = "$gender Avatar",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(gradient)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials.ifBlank { "?" },
                    color = Color.White,
                    fontSize = (size.value * 0.36f).sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
