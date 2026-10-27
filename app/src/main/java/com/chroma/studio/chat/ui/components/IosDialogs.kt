@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.chroma.studio.chat.ui.components

import androidx.compose.animation.*

import androidx.compose.animation.core.Spring

import androidx.compose.animation.core.spring

import androidx.compose.animation.core.tween

import androidx.compose.foundation.background

import androidx.compose.foundation.border

import androidx.compose.foundation.clickable

import androidx.compose.foundation.interaction.MutableInteractionSource

import androidx.compose.foundation.interaction.collectIsPressedAsState

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.text.BasicTextField

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.draw.scale

import androidx.compose.ui.draw.shadow

import androidx.compose.ui.graphics.Brush

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.SolidColor

import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.text.TextStyle

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import androidx.compose.ui.window.Dialog

import androidx.compose.ui.window.DialogProperties

import coil.compose.AsyncImage

import com.chroma.studio.chat.data.*

import com.chroma.studio.chat.sound.SoundManager

import com.chroma.studio.chat.ui.theme.LocalChatColors

import com.chroma.studio.ui.components.AppleSwitch
import com.chroma.studio.ui.components.LiquidGlassProgressBar
import com.chroma.studio.ui.components.LiquidGlassPrimaryButton
import com.chroma.studio.ui.components.LiquidGlassSecondaryButton

import com.composables.icons.lucide.Lucide

import com.composables.icons.lucide.ChartColumn

/**

 * Data class representing an action button inside an Apple iOS style alert or action sheet.

 */

data class IosAlertAction(

    val title: String,

    val isDestructive: Boolean = false,

    val isDefault: Boolean = false,

    val isCancel: Boolean = false,

    val onClick: () -> Unit

)

/**

 * Authentic Apple iOS HIG Alert Dialog Box (UIAlertController alert style).

 * - Exact 270dp-290dp width

 * - 14dp rounded corners with 0.5dp hairline border

 * - Frosted glass translucent background with backdrop scrim

 * - Centered title (17sp SemiBold) & message (13sp Regular)

 * - Thin 0.5dp hairline button separators

 * - Side-by-side layout for 2 buttons; stacked layout for 3+ buttons

 */

@Composable

fun IosAlertDialog(

    title: String,

    message: String? = null,

    icon: (@Composable () -> Unit)? = null,

    actions: List<IosAlertAction>,

    onDismiss: () -> Unit,

    modifier: Modifier = Modifier

) {

    val colors = LocalChatColors.current

    Dialog(

        onDismissRequest = onDismiss,

        properties = DialogProperties(

            dismissOnBackPress = true,

            dismissOnClickOutside = true,

            usePlatformDefaultWidth = false

        )

    ) {

        Box(

            modifier = Modifier

                .fillMaxSize()

                .background(Color.Black.copy(alpha = 0.40f))

                .clickable(

                    interactionSource = remember { MutableInteractionSource() },

                    indication = null

                ) { onDismiss() }

                .padding(horizontal = 36.dp),

            contentAlignment = Alignment.Center

        ) {

            Box(

                modifier = modifier

                    .width(280.dp)

                    .clip(RoundedCornerShape(14.dp))

                    .background(

                        if (colors.isLight) Color(0xF7F2F2F7) else Color(0xF2252528)

                    )

                    .border(

                        0.5.dp,

                        if (colors.isLight) Color(0x22000000) else Color(0x33FFFFFF),

                        RoundedCornerShape(14.dp)

                    )

                    .clickable(

                        interactionSource = remember { MutableInteractionSource() },

                        indication = null

                    ) {}

            ) {

                Column(

                    modifier = Modifier.fillMaxWidth(),

                    horizontalAlignment = Alignment.CenterHorizontally

                ) {

                    // Header Content (Icon, Title, Message)

                    Column(

                        modifier = Modifier

                            .fillMaxWidth()

                            .padding(

                                start = 18.dp,

                                end = 18.dp,

                                top = if (icon != null) 16.dp else 20.dp,

                                bottom = 18.dp

                            ),

                        horizontalAlignment = Alignment.CenterHorizontally,

                        verticalArrangement = Arrangement.spacedBy(4.dp)

                    ) {

                        if (icon != null) {

                            Box(

                                modifier = Modifier.padding(bottom = 6.dp),

                                contentAlignment = Alignment.Center

                            ) {

                                icon()

                            }

                        }

                        Text(

                            text = title,

                            fontSize = 17.sp,

                            fontWeight = FontWeight.SemiBold,

                            color = colors.textDark,

                            textAlign = TextAlign.Center,

                            lineHeight = 22.sp

                        )

                        if (!message.isNullOrBlank()) {

                            Spacer(Modifier.height(2.dp))

                            Text(

                                text = message,

                                fontSize = 13.sp,

                                fontWeight = FontWeight.Normal,

                                color = colors.textMuted,

                                textAlign = TextAlign.Center,

                                lineHeight = 17.5.sp

                            )

                        }

                    }

                    // Hairline separator before action buttons

                    HorizontalDivider(

                        color = if (colors.isLight) Color(0x2E3C3C43) else Color(0x38545458),

                        thickness = 0.5.dp

                    )

                    // Button Action Rows

                    if (actions.size == 2) {

                        // iOS Standard 2-Button Row (Equal half-width with vertical divider)

                        Row(

                            modifier = Modifier

                                .fillMaxWidth()

                                .height(44.dp),

                            verticalAlignment = Alignment.CenterVertically

                        ) {

                            val leftAction = actions.getOrNull(0) ?: IosAlertAction(title = "Cancel", onClick = onDismiss)

                            val rightAction = actions.getOrNull(1) ?: IosAlertAction(title = "OK", onClick = onDismiss)

                            IosAlertButton(

                                action = leftAction,

                                modifier = Modifier

                                    .weight(1f)

                                    .fillMaxHeight(),

                                onDismiss = onDismiss

                            )

                            VerticalDivider(

                                color = if (colors.isLight) Color(0x2E3C3C43) else Color(0x38545458),

                                thickness = 0.5.dp,

                                modifier = Modifier.fillMaxHeight()

                            )

                            IosAlertButton(

                                action = rightAction,

                                modifier = Modifier

                                    .weight(1f)

                                    .fillMaxHeight(),

                                onDismiss = onDismiss

                            )

                        }

                    } else {

                        // Vertical Stacked Buttons (1 button or 3+ buttons)

                        actions.forEachIndexed { index, action ->

                            IosAlertButton(

                                action = action,

                                modifier = Modifier

                                    .fillMaxWidth()

                                    .height(44.dp),

                                onDismiss = onDismiss

                            )

                            if (index < actions.size - 1) {

                                HorizontalDivider(

                                    color = if (colors.isLight) Color(0x2E3C3C43) else Color(0x38545458),

                                    thickness = 0.5.dp

                                )

                            }

                        }

                    }

                }

            }

        }

    }

}

@Composable

private fun IosAlertButton(

    action: IosAlertAction,

    modifier: Modifier = Modifier,

    onDismiss: () -> Unit

) {

    val colors = LocalChatColors.current

    val interactionSource = remember { MutableInteractionSource() }

    val isPressed by interactionSource.collectIsPressedAsState()

    val buttonBg = when {

        isPressed -> if (colors.isLight) Color(0x1F000000) else Color(0x1FFFFFFF)

        else -> Color.Transparent

    }

    val textColor = when {

        action.isDestructive -> Color(0xFFFF3B30)

        action.isCancel -> colors.iosBlue

        action.isDefault -> colors.iosBlue

        else -> colors.iosBlue

    }

    val textWeight = when {

        action.isDefault || action.isCancel -> FontWeight.SemiBold

        else -> FontWeight.Normal

    }

    Box(

        modifier = modifier

            .background(buttonBg)

            .clickable(

                interactionSource = interactionSource,

                indication = null

            ) {

                SoundManager.playReaction()

                action.onClick()

                onDismiss()

            },

        contentAlignment = Alignment.Center

    ) {

        Text(

            text = action.title,

            fontSize = 17.sp,

            fontWeight = textWeight,

            color = textColor,

            textAlign = TextAlign.Center,

            maxLines = 1

        )

    }

}

/**

 * Authentic Apple iOS Action Sheet (UIAlertController actionSheet style).

 * - Floats gracefully from bottom with 10dp margins

 * - Grouped card for options with 14dp corners and 0.5dp separators

 * - Dedicated Cancel card with 14dp corners and 8dp gap

 */

@Composable

fun IosActionSheet(

    title: String? = null,

    message: String? = null,

    actions: List<IosAlertAction>,

    cancelAction: IosAlertAction = IosAlertAction("Cancel", isCancel = true, onClick = {}),

    onDismiss: () -> Unit

) {

    val colors = LocalChatColors.current

    Box(

        modifier = Modifier

            .fillMaxSize()

            .background(Color.Black.copy(alpha = 0.45f))

            .clickable(

                interactionSource = remember { MutableInteractionSource() },

                indication = null

            ) { onDismiss() }

            .padding(horizontal = 10.dp, vertical = 12.dp)

            .windowInsetsPadding(WindowInsets.navigationBars),

        contentAlignment = Alignment.BottomCenter

    ) {

        Column(

            modifier = Modifier

                .fillMaxWidth()

                .widthIn(max = 414.dp)

                .clickable(

                    interactionSource = remember { MutableInteractionSource() },

                    indication = null

                ) {},

            verticalArrangement = Arrangement.spacedBy(8.dp)

        ) {

            // Main Actions Group Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (colors.isLight) Color(0xF6FFFFFF) else Color(0xF21C1C1E)
                    )
                    .border(
                        0.5.dp,
                        if (colors.isLight) Color(0x1F000000) else Color(0x2EFFFFFF),
                        RoundedCornerShape(24.dp)
                    )
            ) {

                Column(

                    modifier = Modifier.fillMaxWidth(),

                    horizontalAlignment = Alignment.CenterHorizontally

                ) {

                    // Header (if title or message provided)

                    if (!title.isNullOrBlank() || !message.isNullOrBlank()) {

                        Column(

                            modifier = Modifier

                                .fillMaxWidth()

                                .padding(horizontal = 16.dp, vertical = 14.dp),

                            horizontalAlignment = Alignment.CenterHorizontally,

                            verticalArrangement = Arrangement.spacedBy(3.dp)

                        ) {

                            if (!title.isNullOrBlank()) {

                                Text(

                                    text = title,

                                    fontSize = 13.sp,

                                    fontWeight = FontWeight.SemiBold,

                                    color = colors.textMuted,

                                    textAlign = TextAlign.Center

                                )

                            }

                            if (!message.isNullOrBlank()) {

                                Text(

                                    text = message,

                                    fontSize = 12.sp,

                                    color = colors.textMuted,

                                    textAlign = TextAlign.Center,

                                    lineHeight = 16.sp

                                )

                            }

                        }

                        HorizontalDivider(

                            color = if (colors.isLight) Color(0x2E3C3C43) else Color(0x38545458),

                            thickness = 0.5.dp

                        )

                    }

                    // Action rows

                    actions.forEachIndexed { index, action ->

                        val interactionSource = remember { MutableInteractionSource() }

                        val isPressed by interactionSource.collectIsPressedAsState()

                        val rowBg = if (isPressed) {

                            if (colors.isLight) Color(0x1F000000) else Color(0x1FFFFFFF)

                        } else Color.Transparent

                        val textColor = if (action.isDestructive) Color(0xFFFF3B30) else colors.iosBlue

                        Box(

                            modifier = Modifier

                                .fillMaxWidth()

                                .height(56.dp)

                                .background(rowBg)

                                .clickable(

                                    interactionSource = interactionSource,

                                    indication = null

                                ) {

                                    SoundManager.playReaction()

                                    action.onClick()

                                    onDismiss()

                                },

                            contentAlignment = Alignment.Center

                        ) {

                            Text(

                                text = action.title,

                                fontSize = 19.sp,

                                fontWeight = if (action.isDefault) FontWeight.SemiBold else FontWeight.Normal,

                                color = textColor,

                                textAlign = TextAlign.Center

                            )

                        }

                        if (index < actions.size - 1) {

                            HorizontalDivider(

                                color = if (colors.isLight) Color(0x2E3C3C43) else Color(0x38545458),

                                thickness = 0.5.dp

                            )

                        }

                    }

                }

            }

            // Distinct Isolated Cancel Button Card

            val cancelInteraction = remember { MutableInteractionSource() }

            val isCancelPressed by cancelInteraction.collectIsPressedAsState()

            Box(

                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (isCancelPressed) {
                            if (colors.isLight) Color(0xDEFFFFFF) else Color(0xDE2C2C2E)
                        } else {
                            if (colors.isLight) Color(0xFFFFFFFF) else Color(0xFF1C1C1E)
                        }
                    )
                    .border(
                        0.5.dp,
                        if (colors.isLight) Color(0x1F000000) else Color(0x2EFFFFFF),
                        RoundedCornerShape(24.dp)
                    )

                    .clickable(

                        interactionSource = cancelInteraction,

                        indication = null

                    ) {

                        SoundManager.playReaction()

                        cancelAction.onClick()

                        onDismiss()

                    },

                contentAlignment = Alignment.Center

            ) {

                Text(

                    text = cancelAction.title,

                    fontSize = 19.sp,

                    fontWeight = FontWeight.SemiBold,

                    color = colors.iosBlue,

                    textAlign = TextAlign.Center

                )

            }

        }

    }

}

/**

 * Authentic Apple iOS Text Input Alert Dialog.

 * Used for text prompts like rename, custom question, notes, etc.

 */

@Composable

fun IosInputDialog(

    title: String,

    message: String? = null,

    initialValue: String = "",

    placeholder: String = "",

    confirmTitle: String = "OK",

    cancelTitle: String = "Cancel",

    onConfirm: (String) -> Unit,

    onDismiss: () -> Unit

) {

    val colors = LocalChatColors.current

    var textValue by remember { mutableStateOf(initialValue) }

    Dialog(

        onDismissRequest = onDismiss,

        properties = DialogProperties(

            dismissOnBackPress = true,

            dismissOnClickOutside = true,

            usePlatformDefaultWidth = false

        )

    ) {

        Box(

            modifier = Modifier

                .fillMaxSize()

                .background(Color.Black.copy(alpha = 0.40f))

                .clickable(

                    interactionSource = remember { MutableInteractionSource() },

                    indication = null

                ) { onDismiss() }

                .padding(horizontal = 36.dp),

            contentAlignment = Alignment.Center

        ) {

            Box(

                modifier = Modifier

                    .width(280.dp)

                    .clip(RoundedCornerShape(14.dp))

                    .background(

                        if (colors.isLight) Color(0xF7F2F2F7) else Color(0xF2252528)

                    )

                    .border(

                        0.5.dp,

                        if (colors.isLight) Color(0x22000000) else Color(0x33FFFFFF),

                        RoundedCornerShape(14.dp)

                    )

                    .clickable(

                        interactionSource = remember { MutableInteractionSource() },

                        indication = null

                    ) {}

            ) {

                Column(

                    modifier = Modifier.fillMaxWidth(),

                    horizontalAlignment = Alignment.CenterHorizontally

                ) {

                    // Header & Input Field

                    Column(

                        modifier = Modifier

                            .fillMaxWidth()

                            .padding(horizontal = 16.dp, vertical = 18.dp),

                        horizontalAlignment = Alignment.CenterHorizontally,

                        verticalArrangement = Arrangement.spacedBy(8.dp)

                    ) {

                        Text(

                            text = title,

                            fontSize = 17.sp,

                            fontWeight = FontWeight.SemiBold,

                            color = colors.textDark,

                            textAlign = TextAlign.Center

                        )

                        if (!message.isNullOrBlank()) {

                            Text(

                                text = message,

                                fontSize = 13.sp,

                                color = colors.textMuted,

                                textAlign = TextAlign.Center

                            )

                        }

                        // iOS Styled Input Box

                        Box(

                            modifier = Modifier

                                .fillMaxWidth()

                                .clip(RoundedCornerShape(6.dp))

                                .background(if (colors.isLight) Color.White else Color(0xFF1C1C1E))

                                .border(

                                    0.5.dp,

                                    if (colors.isLight) Color(0x33000000) else Color(0x33FFFFFF),

                                    RoundedCornerShape(6.dp)

                                )

                                .padding(horizontal = 8.dp, vertical = 6.dp)

                        ) {

                            BasicTextField(

                                value = textValue,

                                onValueChange = { textValue = it },

                                modifier = Modifier.fillMaxWidth(),

                                textStyle = TextStyle(

                                    fontSize = 14.sp,

                                    color = colors.textDark

                                ),

                                cursorBrush = SolidColor(colors.iosBlue),

                                singleLine = true,

                                decorationBox = { inner ->

                                    if (textValue.isEmpty() && placeholder.isNotEmpty()) {

                                        Text(

                                            text = placeholder,

                                            fontSize = 14.sp,

                                            color = colors.textMuted.copy(alpha = 0.7f)

                                        )

                                    }

                                    inner()

                                }

                            )

                        }

                    }

                    HorizontalDivider(

                        color = if (colors.isLight) Color(0x2E3C3C43) else Color(0x38545458),

                        thickness = 0.5.dp

                    )

                    // 2-Button Row (Cancel & Confirm)

                    Row(

                        modifier = Modifier

                            .fillMaxWidth()

                            .height(44.dp),

                        verticalAlignment = Alignment.CenterVertically

                    ) {

                        IosAlertButton(

                            action = IosAlertAction(

                                title = cancelTitle,

                                isCancel = true,

                                onClick = onDismiss

                            ),

                            modifier = Modifier

                                .weight(1f)

                                .fillMaxHeight(),

                            onDismiss = onDismiss

                        )

                        VerticalDivider(

                            color = if (colors.isLight) Color(0x2E3C3C43) else Color(0x38545458),

                            thickness = 0.5.dp,

                            modifier = Modifier.fillMaxHeight()

                        )

                        IosAlertButton(

                            action = IosAlertAction(

                                title = confirmTitle,

                                isDefault = true,

                                onClick = { onConfirm(textValue) }

                            ),

                            modifier = Modifier

                                .weight(1f)

                                .fillMaxHeight(),

                            onDismiss = onDismiss

                        )

                    }

                }

            }

        }

    }

}

/**

 * Reusable Context Menu Action Row with iOS pressed effect and hairline separator.

 */

@Composable

private fun ContextMenuItemRow(

    title: String,

    icon: @Composable () -> Unit,

    isDestructive: Boolean = false,

    isLast: Boolean = false,

    onClick: () -> Unit

) {

    val colors = LocalChatColors.current

    val interactionSource = remember { MutableInteractionSource() }

    val isPressed by interactionSource.collectIsPressedAsState()

    Column(modifier = Modifier.fillMaxWidth()) {

        Row(

            modifier = Modifier

                .fillMaxWidth()

                .height(46.dp)

                .background(if (isPressed) (if (colors.isLight) Color(0x1F000000) else Color(0x1FFFFFFF)) else Color.Transparent)

                .clickable(

                    interactionSource = interactionSource,

                    indication = null

                ) {

                    SoundManager.playReaction()

                    onClick()

                }

                .padding(horizontal = 16.dp),

            verticalAlignment = Alignment.CenterVertically,

            horizontalArrangement = Arrangement.SpaceBetween

        ) {

            Text(

                text = title,

                fontSize = 15.5.sp,

                fontWeight = FontWeight.Normal,

                color = if (isDestructive) Color(0xFFFF3B30) else colors.textDark

            )

            icon()

        }

        if (!isLast) {

            HorizontalDivider(

                color = colors.separator,

                thickness = 0.5.dp,

                modifier = Modifier.padding(start = 16.dp)

            )

        }

    }

}

/**

 * iOS 18 Style Long-Press Context Menu for Messages.

 */

@Composable

fun IosMessageContextMenuModal(

    message: ChatMessage,

    isOutgoing: Boolean,

    onDismiss: () -> Unit,

    onReaction: (String) -> Unit,

    onReply: () -> Unit,

    onCopy: () -> Unit,

    onEdit: () -> Unit,

    onPin: () -> Unit,

    isPinned: Boolean = false,

    onForward: () -> Unit,

    onStar: () -> Unit,

    isStarred: Boolean = false,

    onSelect: () -> Unit = {},

    onTranscribe: () -> Unit = {},

    onSaveMedia: () -> Unit = {},

    onInfo: () -> Unit = {},

    onOpenEmojiPicker: (() -> Unit)? = null,
    onDelete: () -> Unit

) {

    val colors = LocalChatColors.current

    Dialog(

        onDismissRequest = onDismiss,

        properties = DialogProperties(

            dismissOnBackPress = true,

            dismissOnClickOutside = true,

            usePlatformDefaultWidth = false

        )

    ) {

        Box(

            modifier = Modifier

                .fillMaxSize()

                .background(Color.Black.copy(alpha = 0.52f))

                .clickable(

                    interactionSource = remember { MutableInteractionSource() },

                    indication = null

                ) { onDismiss() }

                .padding(horizontal = 20.dp, vertical = 24.dp)

                .windowInsetsPadding(WindowInsets.systemBars),

            contentAlignment = Alignment.Center

        ) {

            Column(

                modifier = Modifier

                    .widthIn(max = 330.dp)

                    .fillMaxWidth()

                    .clickable(

                        interactionSource = remember { MutableInteractionSource() },

                        indication = null

                    ) {},

                horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start,

                verticalArrangement = Arrangement.spacedBy(10.dp)

            ) {

                // 1. Floating Emoji Reactions Bar (Apple iOS Spring Dock)
                Row(
                    modifier = Modifier
                        .shadow(10.dp, RoundedCornerShape(26.dp), ambientColor = Color.Black.copy(alpha = 0.25f))
                        .clip(RoundedCornerShape(26.dp))
                        .background(if (colors.isLight) Color(0xF8FFFFFF) else Color(0xF42C2C2E))
                        .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(26.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val emojis = listOf("❤️", "👍", "👎", "😂", "😮", "😢")
                    emojis.forEach { emoji ->
                        val isSelected = message.userReaction == emoji
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) colors.iosBlueSubtle else Color.Transparent)
                                .border(
                                    width = if (isSelected) 1.dp else 0.dp,
                                    color = if (isSelected) colors.iosBlue else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    onReaction(emoji)
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 19.sp)
                        }
                    }

                    // More Emojis (+) Button
                    PhosphorPlusIcon(
                        tint = colors.textDark,
                        size = 20.dp,
                        modifier = Modifier
                            .clickable {
                                onDismiss()
                                onOpenEmojiPicker?.invoke()
                            }
                            .padding(6.dp)
                    )
                }

                // 2. Message Preview Box

                Box(

                    modifier = Modifier

                        .fillMaxWidth()

                        .clip(RoundedCornerShape(24.dp))
                        .background(if (colors.isLight) Color(0xF8FFFFFF) else Color(0xF01C1C1E))
                        .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(24.dp))

                        .padding(horizontal = 14.dp, vertical = 10.dp)

                ) {

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {

                        Row(

                            modifier = Modifier.fillMaxWidth(),

                            horizontalArrangement = Arrangement.SpaceBetween,

                            verticalAlignment = Alignment.CenterVertically

                        ) {

                            Text(

                                text = if (isOutgoing) "You" else message.senderName,

                                fontSize = 12.sp,

                                fontWeight = FontWeight.Bold,

                                color = colors.iosBlue

                            )

                            Text(

                                text = message.time,

                                fontSize = 11.sp,

                                color = colors.textMuted

                            )

                        }

                        val previewText = when {

                            !message.text.isNullOrBlank() -> message.text

                            message.mediaType == "voice" -> "Voice Message (${message.voiceDuration}s)"

                            message.mediaType == "image" -> "Photo Attachment"

                            message.mediaType == "poll" -> "Poll: ${message.pollData?.question ?: "Vote"}"

                            message.mediaType == "file" -> "${message.fileName ?: "Document"}"

                            message.mediaType == "location" -> "Live Location"

                            else -> "Message"

                        }

                        Text(

                            text = previewText,

                            fontSize = 14.sp,

                            fontWeight = FontWeight.Normal,

                            color = colors.textDark,

                            maxLines = 3,

                            overflow = TextOverflow.Ellipsis

                        )

                    }

                }

                // 3. Actions Card List

                Box(

                    modifier = Modifier

                        .fillMaxWidth()

                        .clip(RoundedCornerShape(24.dp))
                        .background(if (colors.isLight) Color(0xF8FFFFFF) else Color(0xF4252528))
                        .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(24.dp))

                ) {

                    Column(modifier = Modifier.fillMaxWidth()) {

                        // Reply

                        ContextMenuItemRow(

                            title = "Reply",

                            icon = { LucideReply(tint = colors.iosBlue, size = 18.dp) },

                            onClick = { onReply(); onDismiss() }

                        )

                        // Copy

                        ContextMenuItemRow(

                            title = if (message.mediaType == "voice") "Copy Voice Link" else "Copy",

                            icon = { LucideCopy(tint = colors.textDark, size = 18.dp) },

                            onClick = { onCopy(); onDismiss() }

                        )

                        // Edit (Only for outgoing text messages)

                        if (isOutgoing && !message.text.isNullOrBlank()) {

                            ContextMenuItemRow(

                                title = "Edit Message",

                                icon = { LucideEdit(tint = colors.textDark, size = 18.dp) },

                                onClick = { onEdit(); onDismiss() }

                            )

                        }

                        // Pin / Unpin

                        ContextMenuItemRow(

                            title = if (isPinned) "Unpin Message" else "Pin Message",

                            icon = { LucidePin(tint = colors.textDark, size = 18.dp) },

                            onClick = { onPin(); onDismiss() }

                        )

                        // Forward

                        ContextMenuItemRow(

                            title = "Forward",

                            icon = { LucideForward(tint = colors.textDark, size = 18.dp) },

                            onClick = { onForward(); onDismiss() }

                        )

                        // Star / Bookmark

                        ContextMenuItemRow(

                            title = if (isStarred) "Unstar" else "Star",

                            icon = { LucideStar(tint = if (isStarred) Color(0xFFFF9500) else colors.textDark, size = 18.dp) },

                            onClick = { onStar(); onDismiss() }

                        )

                        // Voice Transcribe option

                        if (message.mediaType == "voice") {

                            ContextMenuItemRow(

                                title = "Transcribe Audio",

                                icon = { LucideSparkles(tint = colors.iosBlue, size = 18.dp) },

                                onClick = { onTranscribe(); onDismiss() }

                            )

                        }

                        // Save media

                        if (message.mediaType in listOf("image", "voice", "file")) {

                            ContextMenuItemRow(

                                title = "Save to Device",

                                icon = { LucideDownload(tint = colors.textDark, size = 18.dp) },

                                onClick = { onSaveMedia(); onDismiss() }

                            )

                        }

                        // Message Info
                        ContextMenuItemRow(
                            title = "Message Info",
                            icon = { LucideInfo(tint = colors.textDark, size = 18.dp) },
                            onClick = { onInfo(); onDismiss() }
                        )

                        // Select Multiple
                        ContextMenuItemRow(
                            title = "Select",
                            icon = { LucideCheck(tint = colors.iosBlue, size = 18.dp) },
                            onClick = { onSelect(); onDismiss() }
                        )

                        // Delete

                        ContextMenuItemRow(

                            title = "Delete Message",

                            icon = { LucideTrash(tint = Color(0xFFFF3B30), size = 18.dp) },

                            isDestructive = true,

                            isLast = true,

                            onClick = { onDelete(); onDismiss() }

                        )

                    }

                }

            }

        }

    }

}

/**

 * iOS 18 Style Long-Press Context Menu for Contact / Conversation List Rows.

 */

@Composable

fun IosConversationContextMenuModal(

    chat: ChatConversation,

    isPinned: Boolean = false,

    onDismiss: () -> Unit,

    onSelect: () -> Unit = {},

    onPinToggle: () -> Unit,

    onMuteToggle: () -> Unit,

    onReadToggle: () -> Unit,

    onLockToggle: () -> Unit,

    onArchive: () -> Unit,

    onClearHistory: () -> Unit,

    onExportChat: () -> Unit,

    onDeleteChat: () -> Unit

) {

    val colors = LocalChatColors.current

    Dialog(

        onDismissRequest = onDismiss,

        properties = DialogProperties(

            dismissOnBackPress = true,

            dismissOnClickOutside = true,

            usePlatformDefaultWidth = false

        )

    ) {

        Box(

            modifier = Modifier

                .fillMaxSize()

                .background(Color.Black.copy(alpha = 0.52f))

                .clickable(

                    interactionSource = remember { MutableInteractionSource() },

                    indication = null

                ) { onDismiss() }

                .padding(horizontal = 20.dp, vertical = 24.dp)

                .windowInsetsPadding(WindowInsets.systemBars),

            contentAlignment = Alignment.Center

        ) {

            Column(

                modifier = Modifier

                    .widthIn(max = 330.dp)

                    .fillMaxWidth()

                    .clickable(

                        interactionSource = remember { MutableInteractionSource() },

                        indication = null

                    ) {},

                verticalArrangement = Arrangement.spacedBy(10.dp)

            ) {

                // 1. Conversation Header Card

                Row(

                    modifier = Modifier

                        .fillMaxWidth()

                        .clip(RoundedCornerShape(16.dp))

                        .background(if (colors.isLight) Color(0xF8FFFFFF) else Color(0xF01C1C1E))

                        .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(16.dp))

                        .padding(horizontal = 14.dp, vertical = 12.dp),

                    verticalAlignment = Alignment.CenterVertically,

                    horizontalArrangement = Arrangement.spacedBy(12.dp)

                ) {

                    Box(modifier = Modifier.size(48.dp)) {

                        AsyncImage(

                            model = chat.avatar,

                            contentDescription = chat.name,

                            modifier = Modifier

                                .fillMaxSize()

                                .clip(CircleShape)

                                .border(0.5.dp, colors.borderSubtle, CircleShape),

                            contentScale = ContentScale.Crop

                        )

                        if (chat.online) {

                            Box(

                                modifier = Modifier

                                    .size(12.dp)

                                    .align(Alignment.BottomEnd)

                                    .background(colors.success, CircleShape)

                                    .border(1.5.dp, colors.cardBg, CircleShape)

                            )

                        }

                    }

                    Column(

                        modifier = Modifier.weight(1f),

                        verticalArrangement = Arrangement.spacedBy(2.dp)

                    ) {

                        Text(

                            text = chat.name,

                            fontSize = 16.sp,

                            fontWeight = FontWeight.SemiBold,

                            color = colors.textDark,

                            maxLines = 1,

                            overflow = TextOverflow.Ellipsis

                        )

                        Text(

                            text = if (chat.type == "group") "${chat.members?.size ?: 3} members" else if (chat.online) "Online now" else chat.statusText,

                            fontSize = 13.sp,

                            color = if (chat.online) colors.success else colors.textMuted,

                            maxLines = 1,

                            overflow = TextOverflow.Ellipsis

                        )

                    }

                }

                // 2. Action Menu Card

                Box(

                    modifier = Modifier

                        .fillMaxWidth()

                        .clip(RoundedCornerShape(16.dp))

                        .background(if (colors.isLight) Color(0xF8FFFFFF) else Color(0xF4252528))

                        .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(16.dp))

                ) {

                    Column(modifier = Modifier.fillMaxWidth()) {

                        // Select Chats

                        ContextMenuItemRow(

                            title = "Select",

                            icon = { LucideCheck(tint = colors.iosBlue, size = 18.dp) },

                            onClick = { onSelect(); onDismiss() }

                        )

                        // Pin / Unpin

                        ContextMenuItemRow(

                            title = if (isPinned) "Unpin Conversation" else "Pin Conversation",

                            icon = { LucidePin(tint = colors.iosBlue, size = 18.dp) },

                            onClick = { onPinToggle(); onDismiss() }

                        )

                        // Mute / Unmute

                        ContextMenuItemRow(

                            title = if (chat.isMuted) "Unmute Notifications" else "Mute Notifications",

                            icon = {

                                if (chat.isMuted) LucideBell(tint = colors.textDark, size = 18.dp)

                                else LucideBellOff(tint = colors.textDark, size = 18.dp)

                            },

                            onClick = { onMuteToggle(); onDismiss() }

                        )

                        // Mark as Read / Unread

                        ContextMenuItemRow(

                            title = if (chat.unreadCount > 0) "Mark as Read" else "Mark as Unread",

                            icon = { LucideCheckCheck(tint = colors.iosBlue, size = 18.dp) },

                            onClick = { onReadToggle(); onDismiss() }

                        )

                        // Lock Chat

                        ContextMenuItemRow(

                            title = "Lock with Biometrics",

                            icon = { LucideLock(tint = colors.textDark, size = 18.dp) },

                            onClick = { onLockToggle(); onDismiss() }

                        )

                        // Archive

                        ContextMenuItemRow(

                            title = "Archive Conversation",

                            icon = { LucideArchive(tint = colors.textDark, size = 18.dp) },

                            onClick = { onArchive(); onDismiss() }

                        )

                        // Export Chat

                        ContextMenuItemRow(

                            title = "Export Chat Transcript",

                            icon = { LucideShare(tint = colors.textDark, size = 18.dp) },

                            onClick = { onExportChat(); onDismiss() }

                        )

                        // Clear History

                        ContextMenuItemRow(

                            title = "Clear Chat History",

                            icon = { LucideTrash(tint = Color(0xFFFF9500), size = 18.dp) },

                            onClick = { onClearHistory(); onDismiss() }

                        )

                        // Delete Conversation

                        ContextMenuItemRow(

                            title = "Delete Conversation",

                            icon = { LucideTrash(tint = Color(0xFFFF3B30), size = 18.dp) },

                            isDestructive = true,

                            isLast = true,

                            onClick = { onDeleteChat(); onDismiss() }

                        )

                    }

                }

            }

        }

    }

}

/**

 * iOS 18 Style Long-Press Context Menu for Story Items.

 */

@Composable

fun IosStoryContextMenuModal(

    story: Story,

    isMyStory: Boolean,

    onDismiss: () -> Unit,

    onViewAnalytics: () -> Unit = {},

    onAddNewStory: () -> Unit = {},

    onMuteStories: () -> Unit = {},

    onShareStory: () -> Unit = {},

    onDeleteStory: () -> Unit = {}

) {

    val colors = LocalChatColors.current

    Dialog(

        onDismissRequest = onDismiss,

        properties = DialogProperties(

            dismissOnBackPress = true,

            dismissOnClickOutside = true,

            usePlatformDefaultWidth = false

        )

    ) {

        Box(

            modifier = Modifier

                .fillMaxSize()

                .background(Color.Black.copy(alpha = 0.52f))

                .clickable(

                    interactionSource = remember { MutableInteractionSource() },

                    indication = null

                ) { onDismiss() }

                .padding(horizontal = 20.dp, vertical = 24.dp)

                .windowInsetsPadding(WindowInsets.systemBars),

            contentAlignment = Alignment.Center

        ) {

            Column(

                modifier = Modifier

                    .widthIn(max = 330.dp)

                    .fillMaxWidth()

                    .clickable(

                        interactionSource = remember { MutableInteractionSource() },

                        indication = null

                    ) {},

                verticalArrangement = Arrangement.spacedBy(10.dp)

            ) {

                // Header Preview

                Row(

                    modifier = Modifier

                        .fillMaxWidth()

                        .clip(RoundedCornerShape(16.dp))

                        .background(if (colors.isLight) Color(0xF8FFFFFF) else Color(0xF01C1C1E))

                        .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(16.dp))

                        .padding(horizontal = 14.dp, vertical = 12.dp),

                    verticalAlignment = Alignment.CenterVertically,

                    horizontalArrangement = Arrangement.spacedBy(12.dp)

                ) {

                    AsyncImage(

                        model = story.avatar,

                        contentDescription = story.authorName,

                        modifier = Modifier

                            .size(44.dp)

                            .clip(CircleShape)

                            .border(1.dp, colors.iosBlue, CircleShape),

                        contentScale = ContentScale.Crop

                    )

                    Column(modifier = Modifier.weight(1f)) {

                        Text(

                            text = if (isMyStory) "Your Story" else story.authorName,

                            fontSize = 16.sp,

                            fontWeight = FontWeight.SemiBold,

                            color = colors.textDark

                        )

                        Text(

                            text = "${story.timeAgo} � ${story.caption.ifEmpty { "Story update" }}",

                            fontSize = 12.5.sp,

                            color = colors.textMuted,

                            maxLines = 1,

                            overflow = TextOverflow.Ellipsis

                        )

                    }

                }

                // Actions Card

                Box(

                    modifier = Modifier

                        .fillMaxWidth()

                        .clip(RoundedCornerShape(16.dp))

                        .background(if (colors.isLight) Color(0xF8FFFFFF) else Color(0xF4252528))

                        .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(16.dp))

                ) {

                    Column(modifier = Modifier.fillMaxWidth()) {

                        if (isMyStory) {

                            ContextMenuItemRow(

                                title = "Story Insights & Analytics",

                                icon = { LucideSparkles(tint = colors.iosBlue, size = 18.dp) },

                                onClick = { onViewAnalytics(); onDismiss() }

                            )

                            ContextMenuItemRow(

                                title = "Add Another Story",

                                icon = { PhosphorPlusIcon(tint = colors.textDark, size = 18.dp) },

                                onClick = { onAddNewStory(); onDismiss() }

                            )

                            ContextMenuItemRow(

                                title = "Share Story",

                                icon = { LucideShare(tint = colors.textDark, size = 18.dp) },

                                onClick = { onShareStory(); onDismiss() }

                            )

                            ContextMenuItemRow(

                                title = "Delete Story",

                                icon = { LucideTrash(tint = Color(0xFFFF3B30), size = 18.dp) },

                                isDestructive = true,

                                isLast = true,

                                onClick = { onDeleteStory(); onDismiss() }

                            )

                        } else {

                            ContextMenuItemRow(

                                title = "Mute Stories from ${story.authorName.split(" ").firstOrNull() ?: story.authorName}",

                                icon = { LucideBellOff(tint = colors.textDark, size = 18.dp) },

                                onClick = { onMuteStories(); onDismiss() }

                            )

                            ContextMenuItemRow(

                                title = "Share Story Link",

                                icon = { LucideShare(tint = colors.textDark, size = 18.dp) },

                                isLast = true,

                                onClick = { onShareStory(); onDismiss() }

                            )

                        }

                    }

                }

            }

        }

    }

}

/**

 * iOS Message Info Details Modal.

 */

@Composable

fun IosMessageInfoModal(

    message: ChatMessage,

    isOutgoing: Boolean,

    onDismiss: () -> Unit

) {

    val colors = LocalChatColors.current

    IosModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Message Info",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textDark,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            HorizontalDivider(color = colors.separator, thickness = 0.5.dp)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoRow(label = "Sender", value = if (isOutgoing) "You" else message.senderName)
                InfoRow(label = "Sent Time", value = message.time)
                InfoRow(label = "Delivery Status", value = message.status.replaceFirstChar { it.uppercase() })
                if (message.mediaType != null) {
                    InfoRow(label = "Type", value = message.mediaType.uppercase())
                }
                if (message.text != null) {
                    InfoRow(label = "Length", value = "${message.text.length} characters • ${message.text.split(" ").size} words")
                }
                if (message.isEdited) {
                    InfoRow(label = "Status", value = "Edited message")
                }
            }
            Spacer(Modifier.height(8.dp))
            LiquidGlassPrimaryButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Done",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

/**

 * Helper row for Message Info Modal.

 */

@Composable

private fun InfoRow(label: String, value: String) {

    val colors = LocalChatColors.current

    Row(

        modifier = Modifier.fillMaxWidth(),

        horizontalArrangement = Arrangement.SpaceBetween,

        verticalAlignment = Alignment.CenterVertically

    ) {

        Text(text = label, fontSize = 13.5.sp, color = colors.textMuted)

        Text(text = value, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = colors.textDark)

    }

}

/**

 * iOS Forward Message Contact Picker Sheet.

 */

@Composable

fun IosForwardMessageModal(
    chats: List<ChatConversation>,
    onDismiss: () -> Unit,
    onSelectChat: (ChatConversation) -> Unit
) {
    val colors = LocalChatColors.current

    IosModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Forward To...",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textDark
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    PhosphorCloseX(tint = colors.textMuted, size = 16.dp)
                }
            }

            HorizontalDivider(color = colors.separator, thickness = 0.5.dp)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(chats) { chat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                SoundManager.playSend()
                                onSelectChat(chat)
                                onDismiss()
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AsyncImage(
                            model = chat.avatar,
                            contentDescription = chat.name,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(0.5.dp, colors.borderSubtle, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = chat.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textDark
                            )
                            Text(
                                text = if (chat.type == "group") "Group Chat" else "Direct Message",
                                fontSize = 12.sp,
                                color = colors.textMuted
                            )
                        }
                        LucideForward(tint = colors.iosBlue, size = 18.dp)
                    }
                }
            }
        }
    }
}

/**

 * Apple iOS Modal to Add New Participants to an Existing Group.

 */

@Composable

fun IosAddGroupMemberModal(

    existingMemberNames: Set<String>,

    onDismiss: () -> Unit,

    onAddMembers: (List<GroupContact>) -> Unit

) {

    val colors = LocalChatColors.current

    var searchQuery by remember { mutableStateOf("") }

    val selectedContacts = remember { mutableStateListOf<GroupContact>() }

    val discoverRepo = remember { com.chroma.studio.data.DiscoverRepository() }
    var candidates by remember { mutableStateOf<List<GroupContact>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery, existingMemberNames) {
        val q = searchQuery.trim()
        if (q.length >= 2) {
            isSearching = true
            try {
                val found = discoverRepo.searchUsers(q)
                candidates = found.filter { !existingMemberNames.contains(it.displayName.ifBlank { it.username }) }.map { prof ->
                    GroupContact(
                        id = prof.uid,
                        name = prof.displayName.ifBlank { prof.username.ifBlank { "User" } },
                        role = if (prof.bio.isNotBlank()) prof.bio else "@${prof.username}",
                        avatar = prof.avatarUrl,
                        status = if (prof.online) "Online" else "Active on Oscellion"
                    )
                }
            } catch (_: Exception) {}
            isSearching = false
        } else {
            try {
                val suggested = discoverRepo.getSuggestedCreators(8)
                candidates = suggested.filter { !existingMemberNames.contains(it.displayName.ifBlank { it.username }) }.map { prof ->
                    GroupContact(
                        id = prof.uid,
                        name = prof.displayName.ifBlank { prof.username.ifBlank { "User" } },
                        role = if (prof.bio.isNotBlank()) prof.bio else "@${prof.username}",
                        avatar = prof.avatarUrl,
                        status = if (prof.online) "Online" else "Active on Oscellion"
                    )
                }
            } catch (_: Exception) {
                candidates = emptyList()
            }
        }
    }

    IosModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

                    // Header Row

                    Row(

                        modifier = Modifier.fillMaxWidth(),

                        horizontalArrangement = Arrangement.SpaceBetween,

                        verticalAlignment = Alignment.CenterVertically

                    ) {

                        Text(

                            text = "Add Participants",

                            fontSize = 18.sp,

                            fontWeight = FontWeight.Bold,

                            color = colors.textDark

                        )

                        Box(

                            modifier = Modifier

                                .size(28.dp)

                                .clip(CircleShape)

                                .background(colors.surfaceSecondary)

                                .clickable { onDismiss() },

                            contentAlignment = Alignment.Center

                        ) {

                            PhosphorCloseX(tint = colors.textMuted, size = 12.dp)

                        }

                    }

                    // Search input

                    Box(

                        modifier = Modifier

                            .fillMaxWidth()

                            .height(36.dp)

                            .clip(RoundedCornerShape(10.dp))

                            .background(colors.surfaceSecondary)

                            .padding(horizontal = 10.dp),

                        contentAlignment = Alignment.CenterStart

                    ) {

                        Row(

                            verticalAlignment = Alignment.CenterVertically,

                            horizontalArrangement = Arrangement.spacedBy(6.dp)

                        ) {

                            LucideSearch(tint = colors.textMuted, size = 16.dp)

                            BasicTextField(

                                value = searchQuery,

                                onValueChange = { searchQuery = it },

                                modifier = Modifier.weight(1f),

                                textStyle = TextStyle(fontSize = 14.sp, color = colors.textDark),

                                singleLine = true,

                                decorationBox = { inner ->

                                    if (searchQuery.isEmpty()) {

                                        Text("Search contacts...", fontSize = 14.sp, color = colors.textMuted)

                                    }

                                    inner()

                                }

                            )

                        }

                    }

                    // Contact candidates list

                    LazyColumn(

                        modifier = Modifier.weight(1f),

                        verticalArrangement = Arrangement.spacedBy(6.dp)

                    ) {

                        items(candidates) { contact ->

                            val isSelected = selectedContacts.contains(contact)

                            Row(

                                modifier = Modifier

                                    .fillMaxWidth()

                                    .clip(RoundedCornerShape(12.dp))

                                    .background(if (isSelected) colors.iosBlueSubtle else Color.Transparent)

                                    .clickable {

                                        SoundManager.playReaction()

                                        if (isSelected) selectedContacts.remove(contact)

                                        else selectedContacts.add(contact)

                                    }

                                    .padding(horizontal = 10.dp, vertical = 8.dp),

                                verticalAlignment = Alignment.CenterVertically,

                                horizontalArrangement = Arrangement.spacedBy(12.dp)

                            ) {

                                AsyncImage(

                                    model = contact.avatar,

                                    contentDescription = contact.name,

                                    modifier = Modifier

                                        .size(40.dp)

                                        .clip(CircleShape)

                                        .border(0.5.dp, colors.borderSubtle, CircleShape),

                                    contentScale = ContentScale.Crop

                                )

                                Column(modifier = Modifier.weight(1f)) {

                                    Text(

                                        text = contact.name,

                                        fontSize = 15.sp,

                                        fontWeight = FontWeight.SemiBold,

                                        color = colors.textDark

                                    )

                                    Text(

                                        text = contact.role,

                                        fontSize = 12.sp,

                                        color = colors.textMuted

                                    )

                                }

                                // Checkmark circle

                                Box(

                                    modifier = Modifier

                                        .size(22.dp)

                                        .clip(CircleShape)

                                        .background(if (isSelected) colors.iosBlue else Color.Transparent)

                                        .border(

                                            width = if (isSelected) 0.dp else 1.5.dp,

                                            color = if (isSelected) Color.Transparent else colors.textMuted.copy(alpha = 0.4f),

                                            shape = CircleShape

                                        ),

                                    contentAlignment = Alignment.Center

                                ) {

                                    if (isSelected) {

                                        LucideCheck(tint = Color.White, size = 13.dp)

                                    }

                                }

                            }

                        }

                    }

                    // Add Button

                    LiquidGlassPrimaryButton(
                        onClick = {
                            if (selectedContacts.isNotEmpty()) {
                                SoundManager.playSend()
                                onAddMembers(selectedContacts.toList())
                                onDismiss()
                            }
                        },
                        enabled = selectedContacts.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text(
                            text = if (selectedContacts.isEmpty()) "Select Contacts" else "Add (${selectedContacts.size}) Participants",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                }

    }

}

/**

 * Apple iOS Group Permissions & Settings Modal.

 */

@Composable

fun IosGroupPermissionsModal(

    groupName: String,

    onDismiss: () -> Unit,

    onSave: () -> Unit

) {

    val colors = LocalChatColors.current

    var sendMessagesAdminsOnly by remember { mutableStateOf(false) }

    var editInfoAdminsOnly by remember { mutableStateOf(false) }

    var addMembersAdminsOnly by remember { mutableStateOf(false) }

    var disappearingTimer by remember { mutableStateOf("Off") }

    IosModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

                    // Header

                    Row(

                        modifier = Modifier.fillMaxWidth(),

                        horizontalArrangement = Arrangement.SpaceBetween,

                        verticalAlignment = Alignment.CenterVertically

                    ) {

                        Row(

                            verticalAlignment = Alignment.CenterVertically,

                            horizontalArrangement = Arrangement.spacedBy(8.dp)

                        ) {

                            LucideLock(tint = colors.iosBlue, size = 20.dp)

                            Text(

                                text = "Group Permissions",

                                fontSize = 17.5.sp,

                                fontWeight = FontWeight.Bold,

                                color = colors.textDark

                            )

                        }

                        Box(

                            modifier = Modifier

                                .size(28.dp)

                                .clip(CircleShape)

                                .background(colors.surfaceSecondary)

                                .clickable { onDismiss() },

                            contentAlignment = Alignment.Center

                        ) {

                            PhosphorCloseX(tint = colors.textMuted, size = 12.dp)

                        }

                    }

                    HorizontalDivider(color = colors.separator, thickness = 0.5.dp)

                    // Permission Toggles Card

                    Column(

                        modifier = Modifier

                            .fillMaxWidth()

                            .clip(RoundedCornerShape(14.dp))

                            .background(colors.surfaceSecondary)

                    ) {

                        PermissionToggleRow(

                            title = "Edit Group Info",

                            subtitle = if (editInfoAdminsOnly) "Admins only" else "All members",

                            isChecked = editInfoAdminsOnly,

                            onToggle = { editInfoAdminsOnly = it }

                        )

                        HorizontalDivider(color = colors.separator, thickness = 0.5.dp, modifier = Modifier.padding(start = 14.dp))

                        PermissionToggleRow(

                            title = "Send Messages",

                            subtitle = if (sendMessagesAdminsOnly) "Admins only" else "All members",

                            isChecked = sendMessagesAdminsOnly,

                            onToggle = { sendMessagesAdminsOnly = it }

                        )

                        HorizontalDivider(color = colors.separator, thickness = 0.5.dp, modifier = Modifier.padding(start = 14.dp))

                        PermissionToggleRow(

                            title = "Add Other Members",

                            subtitle = if (addMembersAdminsOnly) "Admins only" else "All members",

                            isChecked = addMembersAdminsOnly,

                            onToggle = { addMembersAdminsOnly = it }

                        )

                    }

                    // Disappearing Messages

                    Column(

                        modifier = Modifier

                            .fillMaxWidth()

                            .clip(RoundedCornerShape(14.dp))

                            .background(colors.surfaceSecondary)

                            .padding(12.dp),

                        verticalArrangement = Arrangement.spacedBy(8.dp)

                    ) {

                        Text(

                            text = "Disappearing Messages",

                            fontSize = 13.5.sp,

                            fontWeight = FontWeight.SemiBold,

                            color = colors.textDark

                        )

                        Row(

                            modifier = Modifier.fillMaxWidth(),

                            horizontalArrangement = Arrangement.spacedBy(6.dp)

                        ) {

                            listOf("Off", "24h", "7d", "90d").forEach { timer ->

                                val isSelected = disappearingTimer == timer

                                Box(

                                    modifier = Modifier

                                        .weight(1f)

                                        .clip(RoundedCornerShape(8.dp))

                                        .background(if (isSelected) colors.iosBlue else colors.cardBg)

                                        .clickable {

                                            SoundManager.playReaction()

                                            disappearingTimer = timer

                                        }

                                        .padding(vertical = 6.dp),

                                    contentAlignment = Alignment.Center

                                ) {

                                    Text(

                                        text = timer,

                                        fontSize = 12.sp,

                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,

                                        color = if (isSelected) Color.White else colors.textDark

                                    )

                                }

                            }

                        }

                    }

                    // Save Button

                    LiquidGlassPrimaryButton(
                        onClick = {
                            SoundManager.playReaction()
                            onSave()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text("Apply Settings", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }

                }

    }

}

@Composable

private fun PermissionToggleRow(

    title: String,

    subtitle: String,

    isChecked: Boolean,

    onToggle: (Boolean) -> Unit

) {

    val colors = LocalChatColors.current

    Row(

        modifier = Modifier

            .fillMaxWidth()

            .clickable { onToggle(!isChecked) }

            .padding(horizontal = 14.dp, vertical = 10.dp),

        horizontalArrangement = Arrangement.SpaceBetween,

        verticalAlignment = Alignment.CenterVertically

    ) {

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {

            Text(text = title, fontSize = 14.5.sp, fontWeight = FontWeight.Medium, color = colors.textDark)

            Text(text = subtitle, fontSize = 12.sp, color = colors.textMuted)

        }

        AppleSwitch(
            checked = isChecked,
            onCheckedChange = onToggle
        )

    }

}

/**

 * Apple iOS Member Action Sheet (View Profile, Promote Admin, Remove).

 */

@Composable

fun IosMemberActionSheetModal(

    member: GroupMember,

    onDismiss: () -> Unit,

    onDirectMessage: () -> Unit,

    onToggleAdmin: () -> Unit,

    onRemoveMember: () -> Unit

) {

    val colors = LocalChatColors.current

    val isAdmin = member.role.contains("Admin", ignoreCase = true) || member.role.contains("Owner", ignoreCase = true)

    IosModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Member Header Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surfaceSecondary)
                    .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AsyncImage(
                    model = member.avatar,
                    contentDescription = member.name,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .border(0.5.dp, colors.borderSubtle, CircleShape),
                    contentScale = ContentScale.Crop
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = member.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textDark
                    )
                    Text(
                        text = member.role,
                        fontSize = 12.5.sp,
                        color = colors.textMuted
                    )
                }
            }

            // Actions Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (colors.isLight) Color(0xF8FFFFFF) else Color(0xF4252528))
                    .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ContextMenuItemRow(
                        title = "Message ${member.name.split(" ").firstOrNull() ?: member.name}",
                        icon = { LucideMail(tint = colors.iosBlue, size = 18.dp) },
                        onClick = { onDirectMessage(); onDismiss() }
                    )
                    ContextMenuItemRow(
                        title = if (isAdmin) "Dismiss as Admin" else "Make Group Admin",
                        icon = { LucideLock(tint = colors.iosBlue, size = 18.dp) },
                        onClick = { onToggleAdmin(); onDismiss() }
                    )
                    ContextMenuItemRow(
                        title = "Remove from Group",
                        icon = { LucideTrash(tint = Color(0xFFFF3B30), size = 18.dp) },
                        isDestructive = true,
                        isLast = true,
                        onClick = { onRemoveMember(); onDismiss() }
                    )
                }
            }
        }
    }
}

/**

 * Apple iOS WhatsApp-Style Interactive Poll Details Bottom Sheet ("View Votes").

 * Displays vote breakdown per option with voter names, macOS style geometric avatars,

 * and percentage metrics.

 */

/**
 * iOS Liquid Glass Bottom Sheet Drawer
 * Uses Google Material 3 ModalBottomSheet with native sheet dragging and velocity gestures.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IosModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor: Color = LocalChatColors.current.cardBg,
    contentColor: Color = LocalChatColors.current.textDark,
    dragHandle: @Composable (() -> Unit)? = {
        Box(
            modifier = Modifier
                .padding(top = 10.dp, bottom = 8.dp)
                .width(36.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(9999.dp))
                .background(LocalChatColors.current.textMuted.copy(alpha = 0.35f))
        )
    },
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = containerColor,
        contentColor = contentColor,
        scrimColor = Color.Black.copy(alpha = 0.45f),
        tonalElevation = 0.dp,
        dragHandle = dragHandle,
        content = content
    )
}

@Composable

fun IosPollDetailsModal(

    poll: PollData,

    currentUser: User,

    onDismiss: () -> Unit

) {

    val colors = LocalChatColors.current

    val totalVotes = poll.options.sumOf { it.votes }

    val sortedOptions = remember(poll.options) {

        poll.options.sortedByDescending { it.votes }

    }

    IosModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)) {

                    // Sheet Header

                    Row(

                        modifier = Modifier

                            .fillMaxWidth()

                            .background(if (colors.isLight) Color.White else Color(0xFF2C2C2E))

                            .padding(horizontal = 16.dp, vertical = 14.dp),

                        horizontalArrangement = Arrangement.SpaceBetween,

                        verticalAlignment = Alignment.CenterVertically

                    ) {

                        Text(

                            text = "Close",

                            fontSize = 17.sp,

                            fontWeight = FontWeight.Normal,

                            color = colors.iosBlue,

                            modifier = Modifier

                                .clickable(

                                    interactionSource = remember { MutableInteractionSource() },

                                    indication = null

                                ) {

                                    SoundManager.playReaction()

                                    onDismiss()

                                }

                        )

                        Text(

                            text = "Poll Details",

                            fontSize = 17.sp,

                            fontWeight = FontWeight.SemiBold,

                            color = colors.textDark

                        )

                        // Spacer to balance

                        Box(modifier = Modifier.width(42.dp))

                    }

                    HorizontalDivider(color = colors.separator, thickness = 0.5.dp)

                    // Poll Question Summary Header

                    Column(

                        modifier = Modifier

                            .fillMaxWidth()

                            .background(if (colors.isLight) Color.White else Color(0xFF2C2C2E))

                            .padding(horizontal = 16.dp, vertical = 12.dp),

                        verticalArrangement = Arrangement.spacedBy(4.dp)

                    ) {

                        Row(

                            verticalAlignment = Alignment.CenterVertically,

                            horizontalArrangement = Arrangement.spacedBy(6.dp)

                        ) {

                            Icon(Lucide.ChartColumn, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(15.dp))

                            Text(

                                text = if (poll.allowMultipleAnswers) "Select one or more options" else "Single Choice",

                                fontSize = 12.5.sp,

                                color = colors.textMuted

                            )

                        }

                        Text(

                            text = poll.question,

                            fontSize = 16.sp,

                            fontWeight = FontWeight.SemiBold,

                            color = colors.textDark,

                            lineHeight = 20.sp

                        )

                        Text(

                            text = "$totalVotes total vote${if (totalVotes == 1) "" else "s"}",

                            fontSize = 12.5.sp,

                            color = colors.iosBlue,

                            fontWeight = FontWeight.Medium

                        )

                    }

                    HorizontalDivider(color = colors.separator, thickness = 0.5.dp)

                    // Options Breakdown List

                    if (totalVotes == 0) {

                        Box(

                            modifier = Modifier

                                .fillMaxWidth()

                                .weight(1f)

                                .padding(32.dp),

                            contentAlignment = Alignment.Center

                        ) {

                            Column(

                                horizontalAlignment = Alignment.CenterHorizontally,

                                verticalArrangement = Arrangement.spacedBy(8.dp)

                            ) {

                                Icon(Lucide.ChartColumn, contentDescription = null, tint = colors.textMuted.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))

                                Text(

                                    text = "No votes cast yet",

                                    fontSize = 16.sp,

                                    fontWeight = FontWeight.Medium,

                                    color = colors.textMuted

                                )

                            }

                        }

                    } else {

                        LazyColumn(

                            modifier = Modifier

                                .fillMaxWidth()

                                .weight(1f),

                            contentPadding = PaddingValues(16.dp),

                            verticalArrangement = Arrangement.spacedBy(14.dp)

                        ) {

                            items(sortedOptions.filter { it.votes > 0 }) { option ->

                                val percentage = if (totalVotes > 0) (option.votes.toFloat() / totalVotes * 100).toInt() else 0

                                Column(

                                    modifier = Modifier

                                        .fillMaxWidth()

                                        .clip(RoundedCornerShape(14.dp))

                                        .background(if (colors.isLight) Color.White else Color(0xFF2C2C2E))

                                        .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(14.dp))

                                ) {

                                    // Option Header

                                    Row(

                                        modifier = Modifier

                                            .fillMaxWidth()

                                            .background(if (colors.isLight) Color(0xFFF9F9FB) else Color(0xFF343438))

                                            .padding(horizontal = 14.dp, vertical = 10.dp),

                                        horizontalArrangement = Arrangement.SpaceBetween,

                                        verticalAlignment = Alignment.CenterVertically

                                    ) {

                                        Text(

                                            text = option.text,

                                            fontSize = 15.sp,

                                            fontWeight = FontWeight.SemiBold,

                                            color = colors.textDark,

                                            modifier = Modifier.weight(1f).padding(end = 8.dp)

                                        )

                                        Text(

                                            text = "${option.votes} (${percentage}%)",

                                            fontSize = 13.5.sp,

                                            fontWeight = FontWeight.Medium,

                                            color = colors.textMuted

                                        )

                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        LiquidGlassProgressBar(
                                            progress = if (totalVotes > 0) (option.votes.toFloat() / totalVotes) else 0f,
                                            height = 6.dp
                                        )
                                    }

                                    HorizontalDivider(color = colors.separator, thickness = 0.5.dp)

                                    if (poll.isAnonymous) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            LucideLock(tint = colors.textMuted, size = 15.dp)
                                            Text(
                                                text = "Anonymous voting — voter identities are hidden",
                                                fontSize = 13.sp,
                                                color = colors.textMuted
                                            )
                                        }
                                    } else {
                                        // Voters List
                                        val voters = if (option.voters.isNotEmpty()) {
                                            option.voters
                                        } else {
                                            buildList {
                                                if (option.isUserVoted) {
                                                    add(PollVoter(name = "${currentUser.name} (You)", avatar = currentUser.avatar, isCurrentUser = true, time = "Just now"))
                                                }
                                                val remaining = option.votes - (if (option.isUserVoted) 1 else 0)
                                                for (i in 0 until remaining) {
                                                    add(PollVoter(name = "Participant ${i + 1}", avatar = "", isCurrentUser = false, time = ""))
                                                }
                                            }
                                        }

                                    voters.forEachIndexed { voterIdx, voter ->

                                        Row(

                                            modifier = Modifier

                                                .fillMaxWidth()

                                                .padding(horizontal = 14.dp, vertical = 9.dp),

                                            verticalAlignment = Alignment.CenterVertically,

                                            horizontalArrangement = Arrangement.spacedBy(10.dp)

                                        ) {

                                            AsyncImage(

                                                model = voter.avatar,

                                                contentDescription = voter.name,

                                                modifier = Modifier

                                                    .size(34.dp)

                                                    .clip(CircleShape)

                                                    .border(0.5.dp, colors.borderSubtle, CircleShape),

                                                contentScale = ContentScale.Crop

                                            )

                                            Column(modifier = Modifier.weight(1f)) {

                                                Text(

                                                    text = voter.name,

                                                    fontSize = 15.sp,

                                                    fontWeight = if (voter.isCurrentUser) FontWeight.SemiBold else FontWeight.Normal,

                                                    color = if (voter.isCurrentUser) colors.iosBlue else colors.textDark

                                                )

                                                if (voter.time.isNotBlank()) {

                                                    Text(

                                                        text = voter.time,

                                                        fontSize = 11.5.sp,

                                                        color = colors.textMuted

                                                    )

                                                }

                                            }

                                        }

                                        if (voterIdx < voters.size - 1) {

                                            HorizontalDivider(

                                                color = colors.separator,

                                                thickness = 0.5.dp,

                                                modifier = Modifier.padding(start = 58.dp)

                                            )

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


