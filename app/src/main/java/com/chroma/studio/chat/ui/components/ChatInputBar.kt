package com.chroma.studio.chat.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.chat.data.ReplyQuote
import com.chroma.studio.chat.ui.theme.LocalChatColors
import kotlinx.coroutines.launch

const val MAX_MESSAGE_CHARACTERS = 4096

@Composable
fun ChatInputBar(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    replyQuote: ReplyQuote?,
    onCancelReply: () -> Unit,
    editingPreviewText: String? = null,
    onCancelEdit: () -> Unit = {},
    isBlocked: Boolean,
    onCameraClick: () -> Unit,
    onPhotoClick: () -> Unit,
    onFileClick: () -> Unit,
    onPollClick: () -> Unit,
    onStickerClick: () -> Unit = {},
    onFlashVideoClick: () -> Unit = {},
    onMusicClick: () -> Unit = {},
    onVoiceClick: () -> Unit,
    onSendClick: () -> Unit,
    onDictateClick: () -> Unit = {},
    isDictating: Boolean = false,
    dictationRms: Float = 0f,
    modifier: Modifier = Modifier
) {
    val colors = LocalChatColors.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var showFormatToolbar by remember { mutableStateOf(false) }

    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = inputText, selection = TextRange(inputText.length)))
    }
    LaunchedEffect(inputText) {
        if (inputText != textFieldValue.text) {
            textFieldValue = TextFieldValue(text = inputText, selection = TextRange(inputText.length))
        }
    }

    val fontTagSpanRegex = remember {
        Regex("\\[(serif|mono|script|cursive|signature)\\]([\\s\\S]*?)\\[/\\1\\]", RegexOption.IGNORE_CASE)
    }

    val activeInlineFont = remember(textFieldValue) {
        val raw = textFieldValue.text
        val selStart = minOf(textFieldValue.selection.start, textFieldValue.selection.end).coerceIn(0, raw.length)
        val selEnd = maxOf(textFieldValue.selection.start, textFieldValue.selection.end).coerceIn(0, raw.length)
        val enclosing = fontTagSpanRegex.findAll(raw).firstOrNull { match ->
            selStart >= match.range.first && selEnd <= match.range.last + 1
        }
        if (enclosing != null) {
            com.chroma.studio.data.ChatFontStyle.fromKey(enclosing.groupValues[1])
        } else {
            com.chroma.studio.data.ChatFontStyle.DEFAULT
        }
    }

    fun applyMarkupFormat(prefix: String, suffix: String) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val current = textFieldValue
        val raw = current.text
        val start = minOf(current.selection.start, current.selection.end).coerceIn(0, raw.length)
        val end = maxOf(current.selection.start, current.selection.end).coerceIn(0, raw.length)
        val updated = if (start != end) {
            val selected = raw.substring(start, end)
            val newStr = (raw.substring(0, start) + prefix + selected + suffix + raw.substring(end))
                .take(MAX_MESSAGE_CHARACTERS)
            val endCaret = (start + prefix.length + selected.length + suffix.length).coerceAtMost(newStr.length)
            TextFieldValue(newStr, TextRange(endCaret))
        } else {
            val newStr = (raw.substring(0, start) + prefix + suffix + raw.substring(end))
                .take(MAX_MESSAGE_CHARACTERS)
            val innerCaret = (start + prefix.length).coerceAtMost(newStr.length)
            TextFieldValue(newStr, TextRange(innerCaret))
        }
        textFieldValue = updated
        onInputTextChange(updated.text)
        try { focusRequester.requestFocus() } catch (_: Exception) {}
    }

    fun applyFontStyleToSelection(fontOpt: com.chroma.studio.data.ChatFontStyle) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val current = textFieldValue
        val raw = current.text
        val start = minOf(current.selection.start, current.selection.end).coerceIn(0, raw.length)
        val end = maxOf(current.selection.start, current.selection.end).coerceIn(0, raw.length)

        val enclosing = fontTagSpanRegex.findAll(raw).firstOrNull { match ->
            start > match.range.first && end <= match.range.last + 1 &&
                start >= match.range.first + "[${match.groupValues[1]}]".length &&
                end <= match.range.last + 1 - "[/${match.groupValues[1]}]".length
        }

        val updated = if (fontOpt == com.chroma.studio.data.ChatFontStyle.DEFAULT) {
            when {
                enclosing != null -> {
                    val inner = enclosing.groupValues[2]
                    val openLen = "[${enclosing.groupValues[1]}]".length
                    val newStr = raw.substring(0, enclosing.range.first) + inner + raw.substring(enclosing.range.last + 1)
                    val newCaret = (start - openLen).coerceIn(0, newStr.length)
                    TextFieldValue(newStr, TextRange(newCaret))
                }
                start != end -> {
                    val clean = com.chroma.studio.data.ChatFontStyle.stripInlineFontMarkup(raw.substring(start, end))
                    val newStr = raw.substring(0, start) + clean + raw.substring(end)
                    TextFieldValue(newStr, TextRange(start + clean.length))
                }
                else -> current
            }
        } else {
            val openTag = "[${fontOpt.tagName}]"
            val closeTag = "[/${fontOpt.tagName}]"
            when {
                start == end && enclosing != null -> {
                    val inner = enclosing.groupValues[2]
                    val prevOpenLen = "[${enclosing.groupValues[1]}]".length
                    val newBlock = openTag + inner + closeTag
                    val newStr = (raw.substring(0, enclosing.range.first) + newBlock + raw.substring(enclosing.range.last + 1))
                        .take(MAX_MESSAGE_CHARACTERS)
                    val innerOffset = (start - enclosing.range.first - prevOpenLen).coerceIn(0, inner.length)
                    val newCaret = (enclosing.range.first + openTag.length + innerOffset).coerceAtMost(newStr.length)
                    TextFieldValue(newStr, TextRange(newCaret))
                }
                start != end -> {
                    val cleanSelected = com.chroma.studio.data.ChatFontStyle.stripInlineFontMarkup(raw.substring(start, end))
                    val newStr = (raw.substring(0, start) + openTag + cleanSelected + closeTag + raw.substring(end))
                        .take(MAX_MESSAGE_CHARACTERS)
                    val endCaret = (start + openTag.length + cleanSelected.length + closeTag.length).coerceAtMost(newStr.length)
                    TextFieldValue(newStr, TextRange(endCaret))
                }
                else -> {
                    val newStr = (raw.substring(0, start) + openTag + closeTag + raw.substring(end))
                        .take(MAX_MESSAGE_CHARACTERS)
                    val innerCaret = (start + openTag.length).coerceAtMost(newStr.length)
                    TextFieldValue(newStr, TextRange(innerCaret))
                }
            }
        }
        textFieldValue = updated
        onInputTextChange(updated.text)
        try { focusRequester.requestFocus() } catch (_: Exception) {}
    }

    // Focus input immediately when entering Reply or Edit mode
    LaunchedEffect(replyQuote?.messageId, editingPreviewText) {
        if (replyQuote != null || editingPreviewText != null) {
            try {
                focusRequester.requestFocus()
                keyboardController?.show()
            } catch (_: Exception) {}
        }
    }

    androidx.activity.compose.BackHandler(enabled = showAttachmentMenu) {
        showAttachmentMenu = false
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .imePadding()
    ) {
        // ── Liquid-glass action menu (GlassIcons) floating over the wallpaper ──
        AnimatedVisibility(
            visible = showAttachmentMenu,
            enter = expandVertically(
                animationSpec = tween(
                    durationMillis = ACTION_MENU_ANIM_DURATION_MS,
                    easing = ActionMenuPopEasing
                )
            ) + fadeIn(animationSpec = tween(260)),
            exit = shrinkVertically(
                animationSpec = tween(
                    durationMillis = ACTION_MENU_CLOSE_DURATION_MS,
                    easing = ActionMenuSmoothOutEasing
                )
            ) + fadeOut(animationSpec = tween(220))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                GlassIconsActionMenu(
                    isOpen = showAttachmentMenu,
                    onOptionSelected = { option ->
                        showAttachmentMenu = false
                        when (option) {
                            ActionMenuOption.CAMERA -> onCameraClick()
                            ActionMenuOption.PHOTOS -> onPhotoClick()
                            ActionMenuOption.FILES -> onFileClick()
                            ActionMenuOption.POLL -> onPollClick()
                            ActionMenuOption.STICKER -> onStickerClick()
                            ActionMenuOption.FLASH_VIDEO -> onFlashVideoClick()
                            ActionMenuOption.MUSIC -> onMusicClick()
                        }
                    }
                )
            }
        }

        Surface(
            color = colors.headerBg,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {

            // ── Top hairline separator ─────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(colors.separator)
            )

            val isImeOpen = WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .then(
                        if (!isImeOpen) Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                        else Modifier
                    )
            ) {

                // ── Reply / Edit Preview Banner ───────────────────
                AnimatedVisibility(
                    visible = replyQuote != null || editingPreviewText != null,
                    enter = expandVertically(
                        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                    ) + fadeIn(animationSpec = tween(180)),
                    exit = shrinkVertically(animationSpec = tween(150)) + fadeOut(animationSpec = tween(150))
                ) {
                    if (editingPreviewText != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.surfaceSecondary)
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(30.dp)
                                        .background(Color(0xFFF59E0B), RoundedCornerShape(2.dp))
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "Editing message",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFF59E0B),
                                        lineHeight = 14.sp
                                    )
                                    Text(
                                        text = com.chroma.studio.data.ChatFontStyle.stripInlineFontMarkup(editingPreviewText),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = colors.textDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable { onCancelEdit() },
                                contentAlignment = Alignment.Center
                            ) {
                                PhosphorCloseX(tint = colors.textMuted, size = 13.dp)
                            }
                        }
                    } else if (replyQuote != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.surfaceSecondary)
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(30.dp)
                                        .background(colors.iosBlue, RoundedCornerShape(2.dp))
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "Replying to ${replyQuote.author}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.iosBlue,
                                        lineHeight = 14.sp
                                    )
                                    Text(
                                        text = com.chroma.studio.data.ChatFontStyle.stripInlineFontMarkup(replyQuote.text),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = colors.textDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable { onCancelReply() },
                                contentAlignment = Alignment.Center
                            ) {
                                PhosphorCloseX(tint = colors.textMuted, size = 13.dp)
                            }
                        }
                    }
                }

                // ── Emoji Picker ──────────────────────────────────
                AnimatedVisibility(
                    visible = showEmojiPicker,
                    enter = expandVertically(
                        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                    ) + scaleIn(initialScale = 0.92f) + fadeIn(),
                    exit = shrinkVertically(animationSpec = tween(150)) + scaleOut(targetScale = 0.92f) + fadeOut()
                ) {
                    val emojiList = listOf(
                        "😀", "😂", "🙌", "🔥", "❤️", "👍",
                        "🎉", "🚀", "✨", "🤔", "👏", "💯"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (colors.isLight) Color(0xF8FFFFFF) else Color(0xF81C1C1E)
                            )
                            .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(6),
                            modifier = Modifier.height(80.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(emojiList) { emoji ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            val current = textFieldValue
                                            val raw = current.text
                                            val caret = current.selection.end.coerceIn(0, raw.length)
                                            val nextStr = (raw.substring(0, caret) + emoji + raw.substring(caret)).take(MAX_MESSAGE_CHARACTERS)
                                            val nextCaret = (caret + emoji.length).coerceAtMost(nextStr.length)
                                            textFieldValue = TextFieldValue(nextStr, TextRange(nextCaret))
                                            onInputTextChange(nextStr)
                                            showEmojiPicker = false
                                        }
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 22.sp)
                                }
                            }
                        }
                    }
                }

                // ── Quick Markup Formatting & Font Style Toolbar ──────────────────────
                AnimatedVisibility(
                    visible = showFormatToolbar,
                    enter = expandVertically(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (colors.isLight) Color(0xFFF6F7FB) else Color(0xFF1F2024))
                            .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Row 1: Markdown Formatting Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MarkupToolChip(label = "B", weight = FontWeight.ExtraBold, onClick = { applyMarkupFormat("**", "**") })
                            MarkupToolChip(label = "I", fontStyle = FontStyle.Italic, onClick = { applyMarkupFormat("*", "*") })
                            MarkupToolChip(label = "S", textDecoration = TextDecoration.LineThrough, onClick = { applyMarkupFormat("~~", "~~") })
                            MarkupToolChip(label = "</>", isMono = true, onClick = { applyMarkupFormat("`", "`") })
                            MarkupToolChip(label = "Code Block", isMono = true, onClick = { applyMarkupFormat("```\n", "\n```") })
                            MarkupToolChip(label = "|| Spoiler ||", onClick = { applyMarkupFormat("||", "||") })
                        }
                        // Row 2: Live Font Style Selector (for selected part or current span in this message)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Style:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textMuted,
                                modifier = Modifier.padding(end = 2.dp)
                            )
                            com.chroma.studio.data.ChatFontStyle.entries.forEach { fontOpt ->
                                val chipHighlighted = activeInlineFont == fontOpt
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (chipHighlighted) colors.iosBlue.copy(alpha = 0.16f)
                                            else (if (colors.isLight) Color.White else Color(0xFF2C2C2E))
                                        )
                                        .border(
                                            width = if (chipHighlighted) 1.dp else 0.5.dp,
                                            color = if (chipHighlighted) colors.iosBlue else colors.borderSubtle,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { applyFontStyleToSelection(fontOpt) }
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = fontOpt.label,
                                        fontSize = 12.sp,
                                        fontFamily = fontOpt.fontFamily,
                                        fontWeight = if (chipHighlighted) FontWeight.Bold else FontWeight.Medium,
                                        color = if (chipHighlighted) colors.iosBlue else colors.textDark
                                    )
                                }
                            }
                        }

                        // Compact live rich preview when the draft contains inline font or text styling
                        val hasInlineFormatting = com.chroma.studio.data.ChatFontStyle.containsInlineFontMarkup(inputText) ||
                            inputText.contains("**") ||
                            inputText.contains("~~") ||
                            inputText.contains("||") ||
                            inputText.contains("`")
                        if (hasInlineFormatting && inputText.isNotBlank()) {
                            val livePreviewAnnotated = remember(inputText, colors.textDark, colors.iosBlue) {
                                parseMarkdownToAnnotatedString(
                                    text = inputText,
                                    textColor = colors.textDark,
                                    mentionColor = colors.iosBlue,
                                    baseFontWeight = FontWeight.Normal,
                                    isOutgoing = false
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (colors.isLight) Color.White else Color(0xFF2C2C2E))
                                    .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Preview",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.iosBlue
                                )
                                Text(
                                    text = livePreviewAnnotated,
                                    fontSize = 13.5.sp,
                                    color = colors.textDark,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // ── Main Input Row ────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ── Synchronized Rotating Action Menu Trigger Button (Blue '+' that spins to 'x') ──
                    SynchronizedActionMenuTriggerButton(
                        isOpen = showAttachmentMenu,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showAttachmentMenu = !showAttachmentMenu
                            if (showAttachmentMenu) showEmojiPicker = false
                        }
                    )

                    // Text input capsule
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp, max = 120.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                if (isDictating) colors.iosBlueSubtle
                                else (if (colors.isLight) Color(0xFFFFFFFF) else colors.surfaceTertiary)
                            )
                            .border(
                                if (isDictating) 1.5.dp else 0.5.dp,
                                if (isDictating) colors.iosBlue
                                else (if (colors.isLight) Color(0xFFD0D0D5) else Color(0xFF3A3A3C)),
                                RoundedCornerShape(22.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = { newValue ->
                                if (showAttachmentMenu) showAttachmentMenu = false
                                val clippedText = if (newValue.text.length <= MAX_MESSAGE_CHARACTERS) {
                                    newValue.text
                                } else {
                                    newValue.text.take(MAX_MESSAGE_CHARACTERS)
                                }
                                textFieldValue = if (clippedText.length == newValue.text.length) {
                                    newValue
                                } else {
                                    newValue.copy(text = clippedText)
                                }
                                onInputTextChange(clippedText)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            enabled = !isBlocked,
                            maxLines = 5,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                autoCorrect = true
                            ),
                            cursorBrush = SolidColor(colors.iosBlue),
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                color = colors.textDark,
                                fontWeight = FontWeight.Normal,
                                lineHeight = 20.sp
                            ),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (inputText.isEmpty()) {
                                        Text(
                                            text = if (isBlocked) "You can't message this contact"
                                                   else if (isDictating) "Listening... Speak now"
                                                   else "Message... (**bold**, *italic*)",
                                            fontSize = 15.5.sp,
                                            fontWeight = if (isDictating) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (isDictating) colors.iosBlue else colors.textMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        // Remaining characters indicator (when message is long)
                        if (inputText.length > 3000) {
                            Text(
                                text = "${inputText.length}/$MAX_MESSAGE_CHARACTERS",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (inputText.length >= MAX_MESSAGE_CHARACTERS) Color(0xFFFF3B30) else colors.textMuted
                            )
                        }

                        // Text Formatting & Font Style Toggle Pill ("Aa")
                        val formatActive = showFormatToolbar || com.chroma.studio.data.ChatFontStyle.containsInlineFontMarkup(inputText)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (showFormatToolbar) colors.iosBlue.copy(alpha = 0.14f) else Color.Transparent)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showFormatToolbar = !showFormatToolbar
                                    if (showFormatToolbar) showAttachmentMenu = false
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aa",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (formatActive) colors.iosBlue else colors.textMuted
                            )
                        }

                        // Speech Dictation button
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onDictateClick()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDictating) {
                                PhosphorSparkleIcon(tint = colors.iosBlue, size = 18.dp)
                            } else {
                                PhosphorVoiceMicIcon(tint = colors.textMuted, size = 18.dp)
                            }
                        }

                        // Emoji toggle
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clickable {
                                    showEmojiPicker = !showEmojiPicker
                                    if (showEmojiPicker) showAttachmentMenu = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            PhosphorSmileyIcon(
                                tint = if (showEmojiPicker) colors.iosBlue else colors.textMuted,
                                size = 20.dp
                            )
                        }
                    }

                    // Voice / Send Action Button (Animated toggle between Voice Mic and Send Arrow)
                    val hasSendContent = inputText.trim().isNotEmpty()
                    val sendBtnScale = remember { Animatable(1f) }
                    val sendBtnRotation = remember { Animatable(0f) }
                    val sendBtnScope = rememberCoroutineScope()

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (hasSendContent) colors.iosBlue
                                else (if (colors.isLight) Color(0xFFF2F2F7) else Color(0xFF2C2C2E))
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (hasSendContent) {
                                    sendBtnScope.launch {
                                        sendBtnScale.snapTo(0.68f)
                                        sendBtnRotation.snapTo(-22f)
                                        launch {
                                            sendBtnScale.animateTo(
                                                1f,
                                                animationSpec = spring(
                                                    dampingRatio = 0.48f,
                                                    stiffness = Spring.StiffnessMediumLow
                                                )
                                            )
                                        }
                                        launch {
                                            sendBtnRotation.animateTo(
                                                0f,
                                                animationSpec = spring(
                                                    dampingRatio = 0.52f,
                                                    stiffness = Spring.StiffnessMediumLow
                                                )
                                            )
                                        }
                                    }
                                    onSendClick()
                                } else {
                                    sendBtnScope.launch {
                                        sendBtnScale.snapTo(0.70f)
                                        sendBtnScale.animateTo(
                                            1f,
                                            animationSpec = spring(
                                                dampingRatio = 0.46f,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        )
                                    }
                                    onVoiceClick()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = hasSendContent,
                            transitionSpec = {
                                (scaleIn(spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(140)))
                                    .togetherWith(scaleOut(tween(100)) + fadeOut(tween(100)))
                            },
                            label = "sendVoiceToggle"
                        ) { hasText ->
                            Box(
                                modifier = Modifier.graphicsLayer {
                                    scaleX = sendBtnScale.value
                                    scaleY = sendBtnScale.value
                                    rotationZ = sendBtnRotation.value
                                },
                                contentAlignment = Alignment.Center
                            ) {
                                if (hasText) {
                                    if (editingPreviewText != null) {
                                        PhosphorSingleCheck(tint = Color.White, size = 19.dp)
                                    } else {
                                        PhosphorPaperPlaneTilt(tint = Color.White, size = 19.dp)
                                    }
                                } else {
                                    PhosphorVoiceMicIcon(tint = if (colors.isLight) colors.textDark else Color.White, size = 20.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun MarkupToolChip(
    label: String,
    weight: FontWeight = FontWeight.SemiBold,
    fontStyle: FontStyle = FontStyle.Normal,
    textDecoration: TextDecoration? = null,
    isMono: Boolean = false,
    onClick: () -> Unit
) {
    val colors = LocalChatColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (colors.isLight) Color.White else Color(0xFF2C2C2E))
            .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = weight,
            fontStyle = fontStyle,
            textDecoration = textDecoration,
            fontFamily = if (isMono) androidx.compose.ui.text.font.FontFamily.Monospace else androidx.compose.ui.text.font.FontFamily.Default,
            color = colors.textDark
        )
    }
}



