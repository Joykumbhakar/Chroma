package com.chroma.studio.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.ui.theme.InterFontFamily
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.X
import kotlinx.coroutines.delay

/**
 * Universal Global Search Bar used across all Chroma Studio screens:
 * - Rounded corners (22.dp pill squircle)
 * - NO border when inactive / unfocused (seamless blend)
 * - Subtle glass border when active / focused
 * - Smooth slide out/in animation when clicked
 */
@Composable
fun ChromaGlobalExpandingSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    isExpanded: Boolean,
    onToggleExpand: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search...",
    maxExpandedWidth: Dp = 260.dp,
    onSearchAction: (() -> Unit)? = null
) {
    val colors = LocalOscellionColors.current
    val isDark = isSystemInDarkTheme()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var isFocused by remember { mutableStateOf(false) }

    val animatedWidth by animateDpAsState(
        targetValue = if (isExpanded) maxExpandedWidth else 38.dp,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "globalSearchBarWidth"
    )

    val bgColor by animateColorAsState(
        targetValue = if (isExpanded) {
            if (isDark) Color(0xFF16161A) else Color(0xFFEFF2F6)
        } else {
            Color.Transparent
        },
        animationSpec = tween(240),
        label = "globalSearchBarBg"
    )

    // No border when not active / not focused!
    val borderColor by animateColorAsState(
        targetValue = if (isExpanded && (isFocused || query.isNotEmpty())) {
            if (isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.08f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(200),
        label = "globalSearchBarBorder"
    )

    val shape = RoundedCornerShape(22.dp)

    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            delay(150)
            try { focusRequester.requestFocus() } catch (_: Throwable) {}
        } else {
            isFocused = false
            keyboardController?.hide()
        }
    }

    Box(
        modifier = modifier
            .width(animatedWidth)
            .height(40.dp)
            .clip(shape)
            .background(bgColor)
            .then(
                if (isExpanded && (isFocused || query.isNotEmpty())) {
                    Modifier.border(1.dp, borderColor, shape)
                } else Modifier
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search Icon trigger
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable {
                        onToggleExpand(!isExpanded)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.Search,
                    contentDescription = "Search",
                    tint = if (isExpanded) colors.primary else colors.textMain,
                    modifier = Modifier.size(19.dp)
                )
            }

            // Slide out/in input field
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandHorizontally(
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(tween(180)),
                exit = shrinkHorizontally(
                    animationSpec = tween(180)
                ) + fadeOut(tween(120)),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                text = placeholder,
                                fontSize = 13.5.sp,
                                fontFamily = InterFontFamily,
                                color = colors.textMuted.copy(alpha = 0.75f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 13.5.sp,
                                fontFamily = InterFontFamily,
                                color = colors.textMain,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(colors.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                keyboardController?.hide()
                                onSearchAction?.invoke()
                            }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .onFocusChanged { isFocused = it.isFocused }
                        )
                    }

                    if (query.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color.White.copy(0.12f) else Color.Black.copy(0.08f))
                                .clickable { onQueryChange("") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.X,
                                contentDescription = "Clear",
                                tint = colors.textMuted,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Universal Global Search Bar for in-content placements (e.g. Explore tab).
 * - Rounded corners (22.dp)
 * - NO border when inactive / unfocused
 * - Subtle glass border when active / focused
 */
@Composable
fun ChromaGlobalInlineSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search...",
    onClear: () -> Unit = { onQueryChange("") },
    onSearchAction: (() -> Unit)? = null
) {
    val colors = LocalOscellionColors.current
    val isDark = isSystemInDarkTheme()
    var isFocused by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    val shape = RoundedCornerShape(22.dp)
    val bgColor = if (isDark) Color(0xFF16161A) else Color(0xFFEFF2F6)
    val activeBorderColor = if (isDark) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.12f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(shape)
            .background(bgColor)
            .then(
                if (isFocused || query.isNotEmpty()) {
                    Modifier.border(1.dp, activeBorderColor, shape)
                } else Modifier
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Lucide.Search,
                contentDescription = "Search",
                tint = if (isFocused) colors.primary else colors.textMuted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 14.sp,
                        fontFamily = InterFontFamily,
                        color = colors.textMuted.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        fontFamily = InterFontFamily,
                        color = colors.textMain,
                        fontWeight = FontWeight.Normal
                    ),
                    cursorBrush = SolidColor(colors.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboardController?.hide()
                        onSearchAction?.invoke()
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused }
                )
            }
            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color.White.copy(0.12f) else Color.Black.copy(0.08f))
                        .clickable { onClear() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.X,
                        contentDescription = "Clear",
                        tint = colors.textMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
