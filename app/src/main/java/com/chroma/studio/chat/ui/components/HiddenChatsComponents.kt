package com.chroma.studio.chat.ui.components

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chroma.studio.chat.data.ChatConversation
import com.chroma.studio.chat.data.HiddenChatSecurityManager
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.chat.ui.theme.LocalChatColors
import com.chroma.studio.chat.util.BiometricHelper
import com.chroma.studio.chat.util.findActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 4-Digit PIN Authentication Dialog (Samsung One UI / Apple Glass style).
 * Handles both Initial PIN Setup (Step 1 -> Confirm Step 2) and Unlock verification.
 */
@Composable
fun HiddenChatsPinDialog(
    isResetOrChange: Boolean = false,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val isInitiallySet = remember { HiddenChatSecurityManager.isPinSet(context) && !isResetOrChange }

    var isSetupMode by remember { mutableStateOf(!isInitiallySet) }
    var setupStep by remember { mutableIntStateOf(1) } // 1 = Enter new PIN, 2 = Confirm PIN
    var firstEnteredPin by remember { mutableStateOf("") }
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Shake animation on error
    val shakeOffset = remember { Animatable(0f) }

    fun triggerShake(message: String) {
        errorMessage = message
        isError = true
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        coroutineScope.launch {
            shakeOffset.snapTo(0f)
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    0f at 0
                    -22f at 60
                    22f at 120
                    -16f at 180
                    16f at 240
                    -8f at 300
                    8f at 350
                    0f at 400
                }
            )
            delay(250)
            enteredPin = ""
            isError = false
        }
    }

    fun onDigitEntered(digit: String) {
        if (enteredPin.length >= 4) return
        val next = enteredPin + digit
        enteredPin = next

        if (next.length == 4) {
            if (isSetupMode) {
                if (setupStep == 1) {
                    firstEnteredPin = next
                    setupStep = 2
                    enteredPin = ""
                    errorMessage = null
                } else {
                    // Confirm step
                    if (next == firstEnteredPin) {
                        HiddenChatSecurityManager.setPin(context, next)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSuccess()
                    } else {
                        triggerShake("PINs do not match. Try again.")
                        setupStep = 1
                        firstEnteredPin = ""
                    }
                }
            } else {
                // Unlock Mode
                if (HiddenChatSecurityManager.verifyPin(context, next)) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSuccess()
                } else {
                    triggerShake("Incorrect PIN. Please try again.")
                }
            }
        }
    }

    fun onBackspace() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            isError = false
            errorMessage = null
        }
    }

    val activity = remember(context) { context.findActivity() }
    val canUseBiometric = remember(activity, isSetupMode) {
        !isSetupMode && activity != null && BiometricHelper.canAuthenticate(context)
    }

    val triggerBiometric = {
        if (activity != null) {
            BiometricHelper.authenticate(
                activity = activity,
                title = "Unlock Hidden Chats",
                subtitle = "Confirm identity with Biometrics",
                onSuccess = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSuccess()
                },
                onError = {
                    // Cancelled or failed, fallback to PIN
                }
            )
        }
    }

    // Auto-prompt biometric if available on open after entrance animation settles
    LaunchedEffect(canUseBiometric) {
        if (canUseBiometric) {
            delay(300)
            triggerBiometric()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.68f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {},
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF1C1C22),
                shadowElevation = 24.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Lock Shield Icon Header
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .shadow(12.dp, CircleShape, ambientColor = Color(0xFF007AFF).copy(alpha = 0.4f), spotColor = Color(0xFF007AFF))
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF007AFF), Color(0xFF0051B3))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        LucideLock(tint = Color.White, size = 26.dp)
                    }

                    // Title & Subtitle
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isSetupMode) {
                                if (setupStep == 1) "Create 4-Digit PIN" else "Confirm 4-Digit PIN"
                            } else {
                                "Hidden Chats"
                            },
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (isSetupMode) {
                                if (setupStep == 1) "Set a 4-digit PIN to secure hidden conversations" else "Re-enter the same 4-digit PIN to confirm"
                            } else {
                                "Enter your 4-digit PIN to access private vault"
                            },
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp
                        )
                    }

                    // 4 Dots PIN Indicator (With Shake Animation on Error)
                    Row(
                        modifier = Modifier
                            .graphicsLayer { translationX = shakeOffset.value }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until 4) {
                            val isFilled = i < enteredPin.length
                            val dotColor by animateColorAsState(
                                targetValue = if (isError) Color(0xFFFF3B30) else if (isFilled) Color(0xFF007AFF) else Color.Transparent,
                                animationSpec = tween(150),
                                label = "dotColor"
                            )
                            val dotScale by animateFloatAsState(
                                targetValue = if (isFilled) 1.25f else 1f,
                                animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
                                label = "dotScale"
                            )

                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .graphicsLayer {
                                        scaleX = dotScale
                                        scaleY = dotScale
                                    }
                                    .clip(CircleShape)
                                    .background(dotColor)
                                    .border(
                                        width = 1.5.dp,
                                        color = if (isError) Color(0xFFFF3B30) else if (isFilled) Color(0xFF007AFF) else Color.White.copy(alpha = 0.35f),
                                        shape = CircleShape
                                    )
                            )
                        }
                    }

                    // Error text
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = Color(0xFFFF453A),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Numeric Keypad (Samsung One UI / iOS Tactile Dialpad)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val rows = listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9"),
                            listOf("BIO", "0", "DEL")
                        )

                        for (row in rows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                for (key in row) {
                                    when (key) {
                                        "DEL" -> {
                                            KeypadCircleButton(
                                                label = "⌫",
                                                isAction = true,
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    onBackspace()
                                                }
                                            )
                                        }
                                        "BIO" -> {
                                            if (canUseBiometric) {
                                                KeypadCircleButton(
                                                    label = "⚡",
                                                    isAction = true,
                                                    icon = {
                                                        LucideFingerprint(tint = Color(0xFF007AFF), size = 24.dp)
                                                    },
                                                    onClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        triggerBiometric()
                                                    }
                                                )
                                            } else {
                                                Spacer(Modifier.size(62.dp))
                                            }
                                        }
                                        else -> {
                                            KeypadCircleButton(
                                                label = key,
                                                isAction = false,
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    onDigitEntered(key)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Cancel text button
                    Text(
                        text = "Cancel",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onDismiss()
                            }
                            .padding(vertical = 4.dp, horizontal = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadCircleButton(
    label: String,
    isAction: Boolean,
    icon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 600f),
        label = "keyScale"
    )

    Box(
        modifier = Modifier
            .size(62.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(
                if (isAction) Color.White.copy(alpha = 0.08f)
                else Color.White.copy(alpha = 0.12f)
            )
            .border(0.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            icon()
        } else {
            Text(
                text = label,
                color = Color.White,
                fontSize = if (label == "⌫") 20.sp else 23.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Fullscreen / Bottom-Sheet Modal displaying the Secret Hidden Chats Vault.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenChatsVaultModal(
    hiddenChatIds: Set<String>,
    allChats: List<ChatConversation>,
    onChatSelected: (String) -> Unit,
    onUnhideChat: (ChatConversation) -> Unit,
    onChangePin: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalChatColors.current
    var searchQuery by remember { mutableStateOf("") }
    val hiddenChats = remember(allChats, hiddenChatIds, searchQuery) {
        allChats.filter { c ->
            c.id in hiddenChatIds &&
            (searchQuery.isBlank() || c.name.contains(searchQuery.trim(), ignoreCase = true) ||
             c.messages.any { it.text?.contains(searchQuery.trim(), ignoreCase = true) == true })
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (colors.isLight) Color(0xFFF8FAFC) else Color(0xFF141721),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
        ) {
            // Header: "Hidden Chats" + Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF007AFF).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        LucideLock(tint = Color(0xFF007AFF), size = 17.dp)
                    }

                    Column {
                        Text(
                            text = "Hidden Chats",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.textDark
                        )
                        Text(
                            text = "${hiddenChats.size} protected conversations",
                            fontSize = 12.sp,
                            color = colors.textMuted
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Change PIN button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.cardBg)
                            .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(10.dp))
                            .clickable { onChangePin() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            LucideKey(tint = colors.textDark, size = 14.dp)
                            Text(
                                text = "PIN",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textDark
                            )
                        }
                    }

                    // Lock & Close Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF007AFF))
                            .clickable { onDismiss() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Lock",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Search Bar inside Vault
            if (hiddenChatIds.size > 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (colors.isLight) Color(0xFFE2E8F0) else Color(0xFF1E293B))
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LucideSearch(tint = colors.textMuted, size = 16.dp)
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 14.sp,
                                color = colors.textDark
                            ),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF007AFF)),
                            decorationBox = { inner ->
                                if (searchQuery.isEmpty()) {
                                    Text("Search hidden conversations...", fontSize = 13.sp, color = colors.textMuted)
                                }
                                inner()
                            }
                        )
                    }
                }
            }

            HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)

            // Conversations List or Empty State
            if (hiddenChats.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF007AFF).copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            LucideLock(tint = Color(0xFF007AFF), size = 32.dp)
                        }
                        Text(
                            text = if (searchQuery.isNotBlank()) "No Matching Hidden Chats" else "No Hidden Chats Yet",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textDark,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) "Try a different search query." else "Long-press any chat in your main list and select 'Hide & Lock Chat' to protect it here.",
                            fontSize = 13.sp,
                            color = colors.textMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(hiddenChats, key = { "hidden_${it.id}" }) { chat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (colors.isLight) Color.White else Color(0xFF1E2330))
                                .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(14.dp))
                                .clickable {
                                    onDismiss()
                                    onChatSelected(chat.id)
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                ChatAvatar(
                                    avatarUrl = chat.avatar,
                                    name = chat.name,
                                    size = 46.dp
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(
                                        text = chat.name,
                                        fontSize = 15.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val lastMsg = chat.messages.lastOrNull()?.text ?: "No messages"
                                    Text(
                                        text = lastMsg,
                                        fontSize = 13.sp,
                                        color = colors.textMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Unhide Action Button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color(0xFF007AFF).copy(alpha = 0.12f))
                                    .clickable {
                                        SoundManager.playReaction()
                                        onUnhideChat(chat)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    LucideEye(tint = Color(0xFF007AFF), size = 14.dp)
                                    Text(
                                        text = "Unhide",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF007AFF)
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
