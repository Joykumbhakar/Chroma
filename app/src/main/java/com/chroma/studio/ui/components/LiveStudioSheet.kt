package com.chroma.studio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chroma.studio.model.LivePulseReaction
import com.chroma.studio.model.LiveStudioComposerMode
import com.chroma.studio.model.LiveStudioConnectionState
import com.chroma.studio.model.LiveStudioEventRow
import com.chroma.studio.model.LiveStudioHelpers
import com.chroma.studio.model.LiveStudioRoom
import com.chroma.studio.ui.theme.ChromaIcons
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.chroma.studio.viewmodel.LiveStudioViewModel
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.Palette
import com.composables.icons.lucide.Radio
import com.composables.icons.lucide.RefreshCw
import com.composables.icons.lucide.Send
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.X
import com.google.firebase.auth.FirebaseAuth
import kotlin.math.roundToInt

private val LiveReactions = listOf("✨", "🔥", "🎨", "👏", "❤️")
private val LiveEaseOut = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveStudioSheet(
    onDismiss: () -> Unit,
    onRequireLogin: () -> Unit = {},
    liveStudioVm: LiveStudioViewModel = viewModel()
) {
    val colors = LocalOscellionColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptics = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    val selectedRoom by liveStudioVm.selectedRoom.collectAsState()
    val connectionState by liveStudioVm.connectionState.collectAsState()
    val connectionHint by liveStudioVm.connectionHint.collectAsState()
    val events by liveStudioVm.events.collectAsState()
    val presenceMembers by liveStudioVm.presenceMembers.collectAsState()
    val reactionBursts by liveStudioVm.reactionBursts.collectAsState()
    val isLoading by liveStudioVm.isLoading.collectAsState()

    var composerMode by remember { mutableStateOf(LiveStudioComposerMode.NOTE) }
    var noteDraft by remember { mutableStateOf("") }
    var paletteTitleDraft by remember { mutableStateOf("") }
    var paletteHexDraft by remember { mutableStateOf("#0F172A, #0381FE, #38BDF8, #F43F5E") }
    var pollQuestionDraft by remember { mutableStateOf("") }
    var pollOptionADraft by remember { mutableStateOf("") }
    var pollOptionBDraft by remember { mutableStateOf("") }
    var pendingDeleteEventId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        liveStudioVm.connectRoom(selectedRoom)
    }

    DisposableEffect(Unit) {
        onDispose {
            liveStudioVm.disconnectRoom()
        }
    }

    val sheetBg = if (colors.isDark) Color(0xFF09090B) else Color(0xFFFFFFFF)
    val subtleSurface = if (colors.isDark) Color(0xFF141418) else Color(0xFFF6F6F9)
    val dividerColor = if (colors.isDark) Color(0xFF22222B) else Color(0xFFEAEAEF)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        contentColor = colors.textMain,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null,
        windowInsets = WindowInsets(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .background(sheetBg)
                .imePadding()
                .navigationBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Drag Handle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colors.textMuted.copy(alpha = 0.35f))
                    )
                }

                // Header (Borderless)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.Radio,
                                contentDescription = null,
                                tint = Color(0xFF0381FE),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Live Studio",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain
                            )
                            LiveStatusDotLabel(connectionState = connectionState, mutedColor = colors.textMuted)
                        }
                        Text(
                            text = if (presenceMembers.isNotEmpty()) {
                                "${presenceMembers.size} active · ${selectedRoom.subtitle}"
                            } else {
                                selectedRoom.subtitle
                            },
                            fontSize = 12.sp,
                            color = colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { liveStudioVm.connectRoom(selectedRoom) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.RefreshCw,
                                contentDescription = "Reconnect",
                                tint = colors.textMuted,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.X,
                                contentDescription = "Close",
                                tint = colors.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Room Underline Tabs (Zero Pills)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LiveStudioRoom.entries.forEach { room ->
                        val selected = selectedRoom == room
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    liveStudioVm.connectRoom(room)
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = room.title,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (selected) colors.textMain else colors.textMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(if (selected) 20.dp else 0.dp)
                                    .height(2.dp)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(if (selected) Color(0xFF0381FE) else Color.Transparent)
                            )
                        }
                    }
                }

                // Online Creators Presence Row
                if (presenceMembers.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        presenceMembers.take(12).forEach { member ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                IosAvatarView(
                                    name = member.name,
                                    avatarUrl = member.avatarUrl,
                                    size = 20.dp,
                                    isOnline = true
                                )
                                Text(
                                    text = member.name.substringBefore(" "),
                                    fontSize = 11.5.sp,
                                    color = colors.textMuted
                                )
                            }
                        }
                    }
                }

                connectionHint?.let { hint ->
                    Text(
                        text = hint,
                        fontSize = 11.5.sp,
                        color = Color(0xFFF59E0B),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                HorizontalDivider(thickness = 0.5.dp, color = dividerColor)

                // Live Feed
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (isLoading && events.isEmpty()) {
                        item(key = "live-loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 44.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Connecting to Supabase Realtime...",
                                    fontSize = 13.sp,
                                    color = colors.textMuted
                                )
                            }
                        }
                    } else if (events.isEmpty()) {
                        item(key = "live-empty") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Lucide.Radio,
                                    contentDescription = null,
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = "No live drops in ${selectedRoom.title} yet",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textMain
                                )
                                Text(
                                    text = "Share a critique note, color palette, or live poll below.",
                                    fontSize = 12.5.sp,
                                    color = colors.textMuted
                                )
                            }
                        }
                    } else {
                        items(events, key = { it.id }) { event ->
                            LiveStudioEventRowItem(
                                event = event,
                                currentUid = currentUid,
                                isDark = colors.isDark,
                                subtleSurface = subtleSurface,
                                dividerColor = dividerColor,
                                textMain = colors.textMain,
                                textMuted = colors.textMuted,
                                onToggleLike = {
                                    if (currentUid.isBlank()) {
                                        onRequireLogin()
                                    } else {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        liveStudioVm.toggleLike(event.id)
                                    }
                                },
                                onVotePoll = { optionId ->
                                    if (currentUid.isBlank()) {
                                        onRequireLogin()
                                    } else {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        liveStudioVm.votePoll(event.id, optionId)
                                    }
                                },
                                onCopyHex = { hex ->
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    clipboard.setText(AnnotatedString(hex))
                                    ToastManager.showToast("Copied $hex")
                                },
                                onRequestDelete = {
                                    pendingDeleteEventId = event.id
                                }
                            )
                            HorizontalDivider(thickness = 0.5.dp, color = dividerColor.copy(alpha = 0.7f))
                        }
                    }
                }

                // Live Broadcast Reaction Bar
                HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Live pulse",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textMuted
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LiveReactions.forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 18.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        liveStudioVm.sendReactionBurst(emoji)
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Composer Mode Tabs (Note | Palette | Poll)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiveStudioComposerMode.entries.forEach { mode ->
                        val active = composerMode == mode
                        Text(
                            text = mode.label,
                            fontSize = 12.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            color = if (active) Color(0xFF0381FE) else colors.textMuted,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { composerMode = mode }
                                .padding(vertical = 3.dp, horizontal = 2.dp)
                        )
                    }
                }

                // Composer Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .padding(bottom = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (composerMode) {
                        LiveStudioComposerMode.NOTE -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(subtleSurface)
                                    .border(0.5.dp, dividerColor, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                BasicTextField(
                                    value = noteDraft,
                                    onValueChange = { if (it.length <= 400) noteDraft = it },
                                    textStyle = TextStyle(color = colors.textMain, fontSize = 14.sp),
                                    cursorBrush = SolidColor(Color(0xFF0381FE)),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(onSend = {
                                        if (currentUid.isBlank()) {
                                            onRequireLogin()
                                        } else if (noteDraft.trim().isNotEmpty()) {
                                            liveStudioVm.postNote(noteDraft)
                                            noteDraft = ""
                                        }
                                    }),
                                    modifier = Modifier.weight(1f),
                                    decorationBox = { inner ->
                                        if (noteDraft.isEmpty()) {
                                            Text("Share live feedback in ${selectedRoom.title}...", fontSize = 13.5.sp, color = colors.textMuted)
                                        }
                                        inner()
                                    }
                                )
                                val canSend = noteDraft.trim().isNotEmpty()
                                Icon(
                                    imageVector = Lucide.Send,
                                    contentDescription = "Send",
                                    tint = if (canSend) Color(0xFF0381FE) else colors.textMuted.copy(alpha = 0.45f),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable(enabled = canSend) {
                                            if (currentUid.isBlank()) {
                                                onRequireLogin()
                                            } else {
                                                liveStudioVm.postNote(noteDraft)
                                                noteDraft = ""
                                            }
                                        }
                                )
                            }
                        }
                        LiveStudioComposerMode.PALETTE -> {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LiveStudioHelpers.CuratedPalettes.forEach { preset ->
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(0.5.dp, dividerColor, RoundedCornerShape(8.dp))
                                            .clickable {
                                                paletteTitleDraft = preset.title
                                                paletteHexDraft = preset.hexes.joinToString(", ")
                                            }
                                            .padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        preset.hexes.take(3).forEach { hex ->
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(parseColorSafe(hex))
                                            )
                                        }
                                        Text(preset.title, fontSize = 11.sp, color = colors.textMain)
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicTextField(
                                    value = paletteHexDraft,
                                    onValueChange = { paletteHexDraft = it },
                                    singleLine = true,
                                    textStyle = TextStyle(color = colors.textMain, fontSize = 12.5.sp, fontFamily = FontFamily.Monospace),
                                    cursorBrush = SolidColor(Color(0xFF0381FE)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(subtleSurface)
                                        .border(0.5.dp, dividerColor, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp, vertical = 9.dp),
                                    decorationBox = { inner ->
                                        if (paletteHexDraft.isEmpty()) Text("#0F172A, #0381FE, #F43F5E", fontSize = 12.sp, color = colors.textMuted)
                                        inner()
                                    }
                                )
                                val parsedHexes = remember(paletteHexDraft) { LiveStudioHelpers.parseHexPaletteInput(paletteHexDraft) }
                                Text(
                                    text = "Share (${parsedHexes.size})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (parsedHexes.isNotEmpty()) Color(0xFF0381FE) else colors.textMuted,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable(enabled = parsedHexes.isNotEmpty()) {
                                            if (currentUid.isBlank()) {
                                                onRequireLogin()
                                            } else {
                                                liveStudioVm.postPalette(paletteTitleDraft, parsedHexes)
                                                paletteTitleDraft = ""
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        }
                        LiveStudioComposerMode.POLL -> {
                            BasicTextField(
                                value = pollQuestionDraft,
                                onValueChange = { if (it.length <= 140) pollQuestionDraft = it },
                                singleLine = true,
                                textStyle = TextStyle(color = colors.textMain, fontSize = 13.5.sp),
                                cursorBrush = SolidColor(Color(0xFF0381FE)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(subtleSurface)
                                    .border(0.5.dp, dividerColor, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                decorationBox = { inner ->
                                    if (pollQuestionDraft.isEmpty()) Text("Ask a quick poll question...", fontSize = 13.sp, color = colors.textMuted)
                                    inner()
                                }
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicTextField(
                                    value = pollOptionADraft,
                                    onValueChange = { if (it.length <= 50) pollOptionADraft = it },
                                    singleLine = true,
                                    textStyle = TextStyle(color = colors.textMain, fontSize = 12.5.sp),
                                    cursorBrush = SolidColor(Color(0xFF0381FE)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(subtleSurface)
                                        .border(0.5.dp, dividerColor, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    decorationBox = { inner ->
                                        if (pollOptionADraft.isEmpty()) Text("Option 1", fontSize = 12.sp, color = colors.textMuted)
                                        inner()
                                    }
                                )
                                BasicTextField(
                                    value = pollOptionBDraft,
                                    onValueChange = { if (it.length <= 50) pollOptionBDraft = it },
                                    singleLine = true,
                                    textStyle = TextStyle(color = colors.textMain, fontSize = 12.5.sp),
                                    cursorBrush = SolidColor(Color(0xFF0381FE)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(subtleSurface)
                                        .border(0.5.dp, dividerColor, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    decorationBox = { inner ->
                                        if (pollOptionBDraft.isEmpty()) Text("Option 2", fontSize = 12.sp, color = colors.textMuted)
                                        inner()
                                    }
                                )
                                val canSendPoll = pollQuestionDraft.trim().isNotEmpty() &&
                                    pollOptionADraft.trim().isNotEmpty() &&
                                    pollOptionBDraft.trim().isNotEmpty()
                                Text(
                                    text = "Post",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (canSendPoll) Color(0xFF0381FE) else colors.textMuted,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable(enabled = canSendPoll) {
                                            if (currentUid.isBlank()) {
                                                onRequireLogin()
                                            } else {
                                                liveStudioVm.postPoll(
                                                    pollQuestionDraft,
                                                    listOf(pollOptionADraft, pollOptionBDraft)
                                                )
                                                pollQuestionDraft = ""
                                                pollOptionADraft = ""
                                                pollOptionBDraft = ""
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Floating Realtime Broadcast Reaction Overlay
            LiveBurstOverlay(
                bursts = reactionBursts,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    pendingDeleteEventId?.let { targetId ->
        IosAlertDialog(
            title = "Delete Live Item?",
            message = "Are you sure you want to remove this item from ${selectedRoom.title}? This syncs live for everyone.",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = {
                pendingDeleteEventId = null
                liveStudioVm.deleteEvent(targetId)
            },
            dismissText = "Cancel",
            onDismiss = { pendingDeleteEventId = null }
        )
    }
}

@Composable
private fun LiveStatusDotLabel(
    connectionState: LiveStudioConnectionState,
    mutedColor: Color
) {
    val (dotColor, label) = when (connectionState) {
        LiveStudioConnectionState.LIVE -> Color(0xFF10B981) to "Live"
        LiveStudioConnectionState.CONNECTING -> Color(0xFFF59E0B) to "Connecting"
        LiveStudioConnectionState.OFFLINE -> mutedColor to "Offline"
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = dotColor
        )
    }
}

@Composable
private fun LiveStudioEventRowItem(
    event: LiveStudioEventRow,
    currentUid: String,
    isDark: Boolean,
    subtleSurface: Color,
    dividerColor: Color,
    textMain: Color,
    textMuted: Color,
    onToggleLike: () -> Unit,
    onVotePoll: (String) -> Unit,
    onCopyHex: (String) -> Unit,
    onRequestDelete: () -> Unit
) {
    val isLiked = currentUid.isNotBlank() && event.likedBy.contains(currentUid)
    val isMine = currentUid.isNotBlank() && event.authorUid == currentUid

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                modifier = Modifier.weight(1f)
            ) {
                IosAvatarView(
                    name = event.authorName.ifBlank { event.authorHandle }.ifBlank { "Creator" },
                    avatarUrl = event.authorAvatarUrl,
                    size = 32.dp
                )
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "@${event.authorHandle.ifBlank { event.authorName }}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textMain,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (event.authorVerified) {
                            VerifiedBadge(size = 12.dp, tone = event.authorVerifiedTone)
                        }
                        Text(
                            text = "· ${formatRelativeAgo(event.createdAtMs)}",
                            fontSize = 11.5.sp,
                            color = textMuted
                        )
                    }
                }
            }

            if (isMine) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onRequestDelete),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.Trash2,
                        contentDescription = "Delete",
                        tint = textMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        if (event.body.isNotBlank()) {
            Text(
                text = event.body,
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                color = textMain.copy(alpha = 0.92f)
            )
        }

        if (event.paletteHexes.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                event.paletteHexes.forEach { hex ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onCopyHex(hex) }
                            .padding(2.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 48.dp, height = 34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(parseColorSafe(hex))
                                .border(0.5.dp, dividerColor, RoundedCornerShape(8.dp))
                        )
                        Text(
                            text = hex,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = textMuted
                        )
                    }
                }
            }
        }

        if (event.pollQuestion.isNotBlank() && event.pollOptions.isNotEmpty()) {
            val totalVotes = event.pollOptions.sumOf { it.voters.size }.coerceAtLeast(1)
            val hasVoted = currentUid.isNotBlank() && event.pollOptions.any { it.voters.contains(currentUid) }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = event.pollQuestion,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textMain,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${event.pollOptions.sumOf { it.voters.size }} votes",
                        fontSize = 11.sp,
                        color = textMuted
                    )
                }

                event.pollOptions.forEach { opt ->
                    val isSelected = currentUid.isNotBlank() && opt.voters.contains(currentUid)
                    val targetFraction = if (hasVoted) opt.voters.size.toFloat() / totalVotes.toFloat() else 0f
                    val animatedFraction by animateFloatAsState(
                        targetValue = targetFraction,
                        animationSpec = tween(360, easing = LiveEaseOut),
                        label = "livePollBar_${event.id}_${opt.id}"
                    )
                    val percent = if (hasVoted) ((opt.voters.size.toFloat() / totalVotes.toFloat()) * 100f).roundToInt() else 0

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(subtleSurface)
                            .border(
                                width = 0.5.dp,
                                color = if (isSelected) Color(0xFF0381FE) else dividerColor,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onVotePoll(opt.id) }
                    ) {
                        if (animatedFraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(animatedFraction.coerceIn(0f, 1f))
                                    .background(
                                        if (isSelected) Color(0xFF0381FE).copy(alpha = if (isDark) 0.25f else 0.14f)
                                        else dividerColor.copy(alpha = 0.55f)
                                    )
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = opt.label,
                                fontSize = 12.5.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF0381FE) else textMain,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (hasVoted) {
                                Text(
                                    text = "$percent%",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) Color(0xFF0381FE) else textMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        // Borderless Like Row (zero circle background on Heart)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onToggleLike)
                .padding(vertical = 2.dp)
        ) {
            Icon(
                imageVector = if (isLiked) ChromaIcons.HeartFilled else Lucide.Heart,
                contentDescription = "Like",
                tint = if (isLiked) Color(0xFFF43F5E) else textMuted,
                modifier = Modifier.size(16.dp)
            )
            if (event.likesCount > 0) {
                Text(
                    text = event.likesCount.toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isLiked) Color(0xFFF43F5E) else textMuted
                )
            }
        }
    }
}

@Composable
private fun LiveBurstOverlay(
    bursts: List<LivePulseReaction>,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        bursts.forEachIndexed { idx, burst ->
            key(burst.id) {
                val anim = remember { Animatable(0f) }
                LaunchedEffect(burst.id) {
                    anim.animateTo(1f, tween(1150, easing = LiveEaseOut))
                }
                val p = anim.value
                val alpha = if (p < 0.2f) p / 0.2f else (1f - (p - 0.2f) / 0.8f).coerceIn(0f, 1f)
                val xFraction = 0.62f + ((idx % 4) * 0.08f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 120.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Text(
                        text = burst.emoji,
                        fontSize = 22.sp,
                        modifier = Modifier
                            .fillMaxWidth(xFraction)
                            .offset(y = (-220f * p).dp)
                            .graphicsLayer {
                                this.alpha = alpha
                                val s = 0.8f + p * 0.45f
                                scaleX = s
                                scaleY = s
                            }
                    )
                }
            }
        }
    }
}

private fun parseColorSafe(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        Color(0xFF0381FE)
    }
}

private fun formatRelativeAgo(timestampMs: Long): String {
    val diffSec = ((System.currentTimeMillis() - timestampMs) / 1000L).coerceAtLeast(0L)
    return when {
        diffSec < 15 -> "now"
        diffSec < 60 -> "${diffSec}s"
        diffSec < 3600 -> "${diffSec / 60}m"
        diffSec < 86400 -> "${diffSec / 3600}h"
        else -> "${diffSec / 86400}d"
    }
}
