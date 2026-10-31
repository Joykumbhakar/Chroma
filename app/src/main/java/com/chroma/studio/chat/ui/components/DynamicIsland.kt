package com.chroma.studio.chat.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.chat.ui.theme.LocalChatColors
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

enum class IslandType {
    INFO,
    SUCCESS,
    WARNING,
    ERROR,
    CALL,
    VOICE_RECORDING
}

data class IslandNotification(
    val title: String,
    val subtitle: String? = null,
    val type: IslandType = IslandType.SUCCESS,
    val durationSec: Int = 0
)

/**
 * High-fidelity Apple Dynamic Island component for Jetpack Compose.
 *
 * Features:
 * - Fluid physics-based morphing between Compact Pill and Expanded Interactive Card
 * - OLED deep black finish with glassmorphic border & specular reflection
 * - Live animated audio visualizers for Calls and Voice Recording
 * - Tap-to-expand / Tap-to-collapse with sound feedback
 * - Interactive controls: End Call, Mute, Full Call modal, Voice Memo send/cancel
 * - Swipe-up to dismiss gesture
 */
@Composable
fun DynamicNotchIsland(
    notification: IslandNotification?,
    isCallActive: Boolean = false,
    callDurationSec: Int = 0,
    callContactName: String = "Contact",
    callAvatarUrl: String = "",
    isRecording: Boolean = false,
    recordingDurationSec: Int = 0,
    voiceAmplitude: Float? = null,
    globeStyle: com.chroma.studio.chat.data.VoiceGlobeStyleType = com.chroma.studio.chat.data.VoiceGlobeStyleType.ORIGINAL_BLUE,
    onGlobeStyleChange: (com.chroma.studio.chat.data.VoiceGlobeStyleType) -> Unit = {},
    onIslandClick: () -> Unit = {},
    onEndCall: () -> Unit = {},
    onToggleMute: () -> Unit = {},
    onStopRecording: () -> Unit = {},
    onCancelRecording: () -> Unit = {},
    onDismissNotification: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = LocalChatColors.current
    var isExpanded by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(false) }

    // Auto-collapse when active state changes to IDLE
    val activeState = when {
        notification != null -> "NOTIFICATION"
        isCallActive -> "CALL"
        isRecording -> "RECORDING"
        else -> "IDLE"
    }

    LaunchedEffect(activeState) {
        if (activeState == "IDLE") {
            isExpanded = false
        }
    }

    // Auto-expand dynamically when recording starts
    LaunchedEffect(isRecording) {
        if (isRecording) {
            isExpanded = true
        }
    }

    val isVisible = activeState != "IDLE"

    // Hardware Notch Jelly & Spring & Jump physics
    val rubberScaleX = remember { Animatable(1f) }
    val rubberScaleY = remember { Animatable(1f) }
    val jumpOffsetY = remember { Animatable(0f) }
    var prevExpanded by remember { mutableStateOf(false) }

    // Play dynamic island sound when notification appears
    LaunchedEffect(notification) {
        if (notification != null) {
            SoundManager.playDynamicIsland()
        }
    }

    LaunchedEffect(notification, isExpanded, activeState) {
        if (isVisible) {
            if (isExpanded != prevExpanded) {
                // Expand / Compress Jump & Jelly animation
                if (isExpanded) {
                    launch {
                        jumpOffsetY.animateTo(10f, spring(dampingRatio = 0.52f, stiffness = Spring.StiffnessMediumLow))
                        jumpOffsetY.animateTo(0f, spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow))
                    }
                    launch {
                        rubberScaleX.animateTo(0.93f, spring(dampingRatio = 0.48f, stiffness = Spring.StiffnessMediumLow))
                        rubberScaleX.animateTo(1.0f, spring(dampingRatio = 0.60f, stiffness = Spring.StiffnessLow))
                    }
                    launch {
                        rubberScaleY.animateTo(1.12f, spring(dampingRatio = 0.48f, stiffness = Spring.StiffnessMediumLow))
                        rubberScaleY.animateTo(1.0f, spring(dampingRatio = 0.60f, stiffness = Spring.StiffnessLow))
                    }
                } else {
                    launch {
                        jumpOffsetY.animateTo(-6f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow))
                        jumpOffsetY.animateTo(0f, spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessLow))
                    }
                    launch {
                        rubberScaleX.animateTo(1.07f, spring(dampingRatio = 0.50f, stiffness = Spring.StiffnessMediumLow))
                        rubberScaleX.animateTo(1.0f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessLow))
                    }
                    launch {
                        rubberScaleY.animateTo(0.89f, spring(dampingRatio = 0.50f, stiffness = Spring.StiffnessMediumLow))
                        rubberScaleY.animateTo(1.0f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessLow))
                    }
                }
                prevExpanded = isExpanded
            } else {
                // Entrance Drop Jump + Jelly Wobble from top notch edge
                jumpOffsetY.snapTo(-35f)
                rubberScaleX.snapTo(0.86f)
                rubberScaleY.snapTo(1.24f)
                launch {
                    jumpOffsetY.animateTo(0f, spring(dampingRatio = 0.52f, stiffness = Spring.StiffnessMediumLow))
                }
                launch {
                    rubberScaleX.animateTo(1.09f, spring(dampingRatio = 0.50f, stiffness = Spring.StiffnessMediumLow))
                    rubberScaleX.animateTo(0.97f, spring(dampingRatio = 0.58f, stiffness = Spring.StiffnessLow))
                    rubberScaleX.animateTo(1.0f, spring(dampingRatio = 0.70f, stiffness = Spring.StiffnessLow))
                }
                launch {
                    rubberScaleY.animateTo(0.92f, spring(dampingRatio = 0.50f, stiffness = Spring.StiffnessMediumLow))
                    rubberScaleY.animateTo(1.03f, spring(dampingRatio = 0.58f, stiffness = Spring.StiffnessLow))
                    rubberScaleY.animateTo(1.0f, spring(dampingRatio = 0.70f, stiffness = Spring.StiffnessLow))
                }
            }
        }
    }

    // Dynamic Island Shape & Sizing fluid animations
    val cornerRadius by animateDpAsState(
        targetValue = if (isExpanded) 42.dp else 28.dp,
        animationSpec = spring(
            dampingRatio = 0.56f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "islandCornerRadius"
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(150)) +
                scaleIn(
                    initialScale = 0.30f,
                    transformOrigin = TransformOrigin(0.5f, 0f),
                    animationSpec = spring(dampingRatio = 0.54f, stiffness = Spring.StiffnessMediumLow)
                ) +
                slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = spring(dampingRatio = 0.56f, stiffness = Spring.StiffnessMediumLow)
                ),
        exit = fadeOut(animationSpec = tween(140)) +
               scaleOut(
                   targetScale = 0.25f,
                   transformOrigin = TransformOrigin(0.5f, 0f),
                   animationSpec = tween(160, easing = FastOutSlowInEasing)
               ) +
               slideOutVertically(
                   targetOffsetY = { -it },
                   animationSpec = tween(160, easing = FastOutSlowInEasing)
               ),
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 8.dp, start = 12.dp, end = 12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            val islandShape = RoundedCornerShape(cornerRadius)

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationY = jumpOffsetY.value
                        scaleX = rubberScaleX.value
                        scaleY = rubberScaleY.value
                        transformOrigin = TransformOrigin(0.5f, 0f)
                    }
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = 0.54f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                    .widthIn(
                        min = if (isExpanded) 340.dp else 180.dp,
                        max = if (isExpanded) 390.dp else 360.dp
                    )
                    .shadow(
                        elevation = if (isExpanded) 14.dp else 8.dp,
                        shape = islandShape,
                        clip = false,
                        spotColor = Color.Black.copy(alpha = 0.50f),
                        ambientColor = Color.Black.copy(alpha = 0.25f)
                    )
                    .clip(islandShape)
                    .background(Color(0xFF000000)) // 100% opaque base ensures nothing behind bleeds through
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF161616), // Solid deep charcoal black (100% opaque)
                                Color(0xFF000000)  // Pure darkest pitch black
                            )
                        )
                    )
                    .border(
                        width = 1.25.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.65f), // Crisp top specular glass gleam
                                Color.White.copy(alpha = 0.35f), // Upper curve glass refraction
                                Color.White.copy(alpha = 0.20f), // Side glass bezel (clearly visible)
                                Color.White.copy(alpha = 0.28f), // Lower bezel transition
                                Color.White.copy(alpha = 0.45f)  // Bottom upward bounce reflection
                            )
                        ),
                        shape = islandShape
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                SoundManager.playReaction()
                                if (activeState == "CALL" || activeState == "RECORDING") {
                                    isExpanded = !isExpanded
                                } else {
                                    onIslandClick()
                                }
                            },
                            onLongPress = {
                                SoundManager.playReaction()
                                isExpanded = !isExpanded
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            // Swipe up gesture to dismiss notification
                            if (dragAmount.y < -12 && activeState == "NOTIFICATION") {
                                change.consume()
                                SoundManager.playReaction()
                                onDismissNotification()
                            }
                        }
                    }
                    .padding(
                        horizontal = if (isExpanded) 18.dp else 12.dp,
                        vertical = if (isExpanded) 16.dp else 8.dp
                    )
            ) {
                when (activeState) {
                    "NOTIFICATION" -> {
                        if (notification != null) {
                            CompactNotificationView(
                                notification = notification,
                                onDismiss = onDismissNotification
                            )
                        }
                    }

                    "CALL" -> {
                        if (isExpanded) {
                            ExpandedCallView(
                                contactName = callContactName,
                                avatarUrl = callAvatarUrl,
                                durationSec = callDurationSec,
                                isMuted = isMuted,
                                isSpeakerOn = isSpeakerOn,
                                onToggleMute = {
                                    isMuted = !isMuted
                                    SoundManager.playReaction()
                                    onToggleMute()
                                },
                                onToggleSpeaker = {
                                    isSpeakerOn = !isSpeakerOn
                                    SoundManager.playReaction()
                                },
                                onOpenFullCall = {
                                    isExpanded = false
                                    onIslandClick()
                                },
                                onEndCall = {
                                    isExpanded = false
                                    onEndCall()
                                },
                                onCollapse = { isExpanded = false }
                            )
                        } else {
                            CompactCallView(
                                contactName = callContactName,
                                avatarUrl = callAvatarUrl,
                                durationSec = callDurationSec
                            )
                        }
                    }

                    "RECORDING" -> {
                        if (isExpanded) {
                            ExpandedRecordingView(
                                durationSec = recordingDurationSec,
                                voiceAmplitude = voiceAmplitude,
                                globeStyle = globeStyle,
                                onGlobeStyleChange = onGlobeStyleChange,
                                onCancel = {
                                    isExpanded = false
                                    onCancelRecording()
                                },
                                onSend = {
                                    isExpanded = false
                                    onStopRecording()
                                },
                                onCollapse = { isExpanded = false }
                            )
                        } else {
                            CompactRecordingView(durationSec = recordingDurationSec)
                        }
                    }
                }
            }
        }
    }
}

// ====================================================================
// COMPACT VIEWS
// ====================================================================

@Composable
private fun CompactNotificationView(
    notification: IslandNotification,
    onDismiss: () -> Unit
) {
    val colors = LocalChatColors.current

    val badgeColor = when (notification.type) {
        IslandType.SUCCESS -> Color(0xFF34C759)
        IslandType.INFO -> colors.iosBlue
        IslandType.WARNING -> Color(0xFFFF9500)
        IslandType.ERROR -> Color(0xFFFF3B30)
        IslandType.CALL -> Color(0xFF34C759)
        IslandType.VOICE_RECORDING -> Color(0xFFFF3B30)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left Icon Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            when (notification.type) {
                IslandType.SUCCESS -> PhosphorGreenCheck(size = 18.dp)
                IslandType.CALL -> PhosphorPhone(tint = badgeColor, size = 18.dp)
                IslandType.VOICE_RECORDING -> PhosphorVoiceMicIcon(tint = badgeColor, size = 18.dp)
                IslandType.WARNING -> Text("!", color = badgeColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                IslandType.ERROR -> LucideBan(tint = badgeColor, size = 18.dp)
                else -> LucideInfo(tint = badgeColor, size = 18.dp)
            }

            // Text
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = notification.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!notification.subtitle.isNullOrBlank()) {
                    Text(
                        text = notification.subtitle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFFA1A1AA),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Right Pulse Indicator
        IslandPulseDot(color = badgeColor)
    }
}

@Composable
private fun CompactCallView(
    contactName: String,
    avatarUrl: String,
    durationSec: Int
) {
    val min = durationSec / 60
    val sec = durationSec % 60
    val timerStr = "$min:${sec.toString().padStart(2, '0')}"

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Avatar + Caller details
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.size(28.dp)) {
                if (avatarUrl.isNotEmpty()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = contactName,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .border(1.dp, Color(0xFF34C759), CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    PhosphorPhone(tint = Color(0xFF34C759), size = 18.dp)
                }
            }

            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = contactName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = timerStr,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF34C759)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // Right: Animated Green Equalizer
        IslandEqualizer(color = Color(0xFF34C759), barCount = 5)
    }
}

@Composable
private fun CompactRecordingView(
    durationSec: Int
) {
    val min = durationSec / 60
    val sec = durationSec % 60
    val timerStr = "$min:${sec.toString().padStart(2, '0')}"

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Red recording pulse + Label
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IslandPulseDot(color = Color(0xFFFF3B30))
            Text(
                text = "Recording",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        Spacer(Modifier.width(12.dp))

        // Right: Equalizer + Timer
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IslandEqualizer(color = Color(0xFFFF3B30), barCount = 4)
            Text(
                text = timerStr,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFFF453A)
            )
        }
    }
}

// ====================================================================
// EXPANDED VIEWS (Interactive Rich Cards)
// ====================================================================

@Composable
private fun ExpandedCallView(
    contactName: String,
    avatarUrl: String,
    durationSec: Int,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onOpenFullCall: () -> Unit,
    onEndCall: () -> Unit,
    onCollapse: () -> Unit
) {
    val min = durationSec / 60
    val sec = durationSec % 60
    val timerStr = "$min:${sec.toString().padStart(2, '0')}"

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Row: Avatar, Contact info, Timer & Collapse chevron
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Large Avatar with glowing border
                Box(modifier = Modifier.size(46.dp)) {
                    if (avatarUrl.isNotEmpty()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = contactName,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .border(1.5.dp, Color(0xFF34C759), CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        PhosphorPhone(tint = Color(0xFF34C759), size = 24.dp)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = contactName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "FaceTime Audio",
                            fontSize = 12.sp,
                            color = Color(0xFFA1A1AA)
                        )
                        Text(
                            text = "·",
                            fontSize = 12.sp,
                            color = Color(0xFFA1A1AA)
                        )
                        Text(
                            text = timerStr,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF34C759)
                        )
                    }
                }
            }

            // Live Equalizer in header
            IslandEqualizer(color = Color(0xFF34C759), barCount = 6)
        }

        // Action Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mute Button
            IslandActionButton(
                icon = {
                    if (isMuted) LucideMicOff(tint = Color.White, size = 18.dp)
                    else LucideMic(tint = Color.White, size = 18.dp)
                },
                label = if (isMuted) "Unmute" else "Mute",
                bgColor = if (isMuted) Color(0xFF3A3A3C) else Color(0xFF2C2C2E),
                onClick = onToggleMute
            )

            // Speaker Button
            IslandActionButton(
                icon = { LucideVolume2(tint = if (isSpeakerOn) Color(0xFF34C759) else Color.White, size = 18.dp) },
                label = "Speaker",
                bgColor = if (isSpeakerOn) Color(0xFF3A3A3C) else Color(0xFF2C2C2E),
                onClick = onToggleSpeaker
            )

            // Fullscreen Call Modal Button
            IslandActionButton(
                icon = { LucideVideo(tint = Color.White, size = 18.dp) },
                label = "Expand",
                bgColor = Color(0xFF2C2C2E),
                onClick = onOpenFullCall
            )

            // End Call Button
            IslandActionButton(
                icon = { LucideCallEnd(tint = Color.White, size = 20.dp) },
                label = "End",
                bgColor = Color(0xFFFF3B30),
                onClick = onEndCall
            )
        }
    }
}

@Composable
private fun ExpandedRecordingView(
    durationSec: Int,
    voiceAmplitude: Float?,
    globeStyle: com.chroma.studio.chat.data.VoiceGlobeStyleType,
    onGlobeStyleChange: (com.chroma.studio.chat.data.VoiceGlobeStyleType) -> Unit,
    onCancel: () -> Unit,
    onSend: () -> Unit,
    onCollapse: () -> Unit
) {
    val min = durationSec / 60
    val sec = durationSec % 60
    val timerStr = "$min:${sec.toString().padStart(2, '0')}"
    val allGlobeStyles = remember { com.chroma.studio.chat.data.VoiceGlobeStyleType.values() }

    fun cycleNextStyle() {
        val nextIndex = (allGlobeStyles.indexOf(globeStyle) + 1) % allGlobeStyles.size
        val nextStyle = allGlobeStyles[nextIndex]
        SoundManager.playReaction()
        onGlobeStyleChange(nextStyle)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Row: Title + Live Timer + Pulsing Dot
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IslandPulseDot(color = Color(0xFFFF3B30))
                Text(
                    text = "Voice Memo",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Text(
                text = timerStr,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF453A)
            )
        }

        // 3D Liquid Plasma AI Globe (Matching Deep OLED Black Theme)
        DynamicIslandLiquidGlobe(
            globeStyle = globeStyle,
            voiceAmplitude = voiceAmplitude,
            size = 115.dp,
            modifier = Modifier.padding(vertical = 2.dp)
        )

        // Bottom Controls: Discard vs Send Note
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cancel / Discard button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF2C2C2E))
                    .clickable { onCancel() }
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LucideTrash(tint = Color(0xFFA1A1AA), size = 15.dp)
                    Text("Discard", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFFA1A1AA))
                }
            }

            // Send voice note button (Matches active globe theme accent)
            val sendAccent = globeStyle.previewGradient.getOrNull(1) ?: Color(0xFF007AFF)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(sendAccent)
                    .clickable { onSend() }
                    .padding(horizontal = 20.dp, vertical = 9.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    UiverseSendUpArrowIcon(tint = Color.White, size = 15.dp)
                    Text("Send Note", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

/**
 * Color Palette mapping for the 3D Liquid Globe.
 */
data class GlobePalette(
    val coreColors: List<Color>,
    val blob1: List<Color>,
    val blob2: List<Color>,
    val blob3: List<Color>,
    val waveColor: Color
)

fun getGlobePalette(style: com.chroma.studio.chat.data.VoiceGlobeStyleType): GlobePalette {
    return when (style) {
        com.chroma.studio.chat.data.VoiceGlobeStyleType.ORIGINAL_BLUE -> GlobePalette(
            coreColors = listOf(Color(0xFF38BDF8), Color(0xFF0284C7), Color(0xFF1D4ED8), Color(0xFF091536)),
            blob1 = listOf(Color(0xFF38BDF8).copy(alpha = 0.85f), Color(0xFF0284C7).copy(alpha = 0f)),
            blob2 = listOf(Color(0xFFFFFFFF).copy(alpha = 0.75f), Color(0xFF7DD3FC).copy(alpha = 0f)),
            blob3 = listOf(Color(0xFF60A5FA).copy(alpha = 0.7f), Color.Transparent),
            waveColor = Color(0xFF38BDF8)
        )
        com.chroma.studio.chat.data.VoiceGlobeStyleType.CHROMATIC_3D -> GlobePalette(
            coreColors = listOf(Color(0xFFD8B4FE), Color(0xFF9333EA), Color(0xFF6B21A8), Color(0xFF2E1065)),
            blob1 = listOf(Color(0xFFC084FC).copy(alpha = 0.85f), Color(0xFF8B5CF6).copy(alpha = 0f)),
            blob2 = listOf(Color(0xFFFFFFFF).copy(alpha = 0.75f), Color(0xFFF3E8FF).copy(alpha = 0f)),
            blob3 = listOf(Color(0xFFE879F9).copy(alpha = 0.7f), Color.Transparent),
            waveColor = Color(0xFFC084FC)
        )
        com.chroma.studio.chat.data.VoiceGlobeStyleType.AURORA_PULSE -> GlobePalette(
            coreColors = listOf(Color(0xFFF472B6), Color(0xFFDB2777), Color(0xFF9D174D), Color(0xFF500724)),
            blob1 = listOf(Color(0xFFF472B6).copy(alpha = 0.85f), Color(0xFFEC4899).copy(alpha = 0f)),
            blob2 = listOf(Color(0xFFFFFFFF).copy(alpha = 0.75f), Color(0xFFFCE7F3).copy(alpha = 0f)),
            blob3 = listOf(Color(0xFFFB7185).copy(alpha = 0.7f), Color.Transparent),
            waveColor = Color(0xFFF472B6)
        )
        com.chroma.studio.chat.data.VoiceGlobeStyleType.CYBER_GRID -> GlobePalette(
            coreColors = listOf(Color(0xFF2DD4BF), Color(0xFF059669), Color(0xFF047857), Color(0xFF022C22)),
            blob1 = listOf(Color(0xFF34D399).copy(alpha = 0.85f), Color(0xFF10B981).copy(alpha = 0f)),
            blob2 = listOf(Color(0xFFFFFFFF).copy(alpha = 0.75f), Color(0xFFD1FAE5).copy(alpha = 0f)),
            blob3 = listOf(Color(0xFF22D3EE).copy(alpha = 0.7f), Color.Transparent),
            waveColor = Color(0xFF34D399)
        )
        com.chroma.studio.chat.data.VoiceGlobeStyleType.SOLAR_FLARE -> GlobePalette(
            coreColors = listOf(Color(0xFFFB923C), Color(0xFFEA580C), Color(0xFF9A3412), Color(0xFF431407)),
            blob1 = listOf(Color(0xFFFBBF24).copy(alpha = 0.85f), Color(0xFFEA580C).copy(alpha = 0f)),
            blob2 = listOf(Color(0xFFFFFFFF).copy(alpha = 0.75f), Color(0xFFFEF08A).copy(alpha = 0f)),
            blob3 = listOf(Color(0xFFF87171).copy(alpha = 0.7f), Color.Transparent),
            waveColor = Color(0xFFFB923C)
        )
        com.chroma.studio.chat.data.VoiceGlobeStyleType.DARK_MATTER -> GlobePalette(
            coreColors = listOf(Color(0xFF818CF8), Color(0xFF4338CA), Color(0xFF1E1B4B), Color(0xFF020617)),
            blob1 = listOf(Color(0xFF818CF8).copy(alpha = 0.85f), Color(0xFF4F46E5).copy(alpha = 0f)),
            blob2 = listOf(Color(0xFFFFFFFF).copy(alpha = 0.75f), Color(0xFFE0E7FF).copy(alpha = 0f)),
            blob3 = listOf(Color(0xFFA5B4FC).copy(alpha = 0.7f), Color.Transparent),
            waveColor = Color(0xFF818CF8)
        )
        com.chroma.studio.chat.data.VoiceGlobeStyleType.ROSE_QUARTZ -> GlobePalette(
            coreColors = listOf(Color(0xFFFDA4AF), Color(0xFFE11D48), Color(0xFF9F1239), Color(0xFF4C0519)),
            blob1 = listOf(Color(0xFFFDA4AF).copy(alpha = 0.85f), Color(0xFFF43F5E).copy(alpha = 0f)),
            blob2 = listOf(Color(0xFFFFFFFF).copy(alpha = 0.75f), Color(0xFFFFF1F2).copy(alpha = 0f)),
            blob3 = listOf(Color(0xFFFBCFE8).copy(alpha = 0.7f), Color.Transparent),
            waveColor = Color(0xFFFDA4AF)
        )
        com.chroma.studio.chat.data.VoiceGlobeStyleType.STRANDS -> GlobePalette(
            coreColors = listOf(Color(0xFF93F1FF), Color(0xFF3569FA), Color(0xFF193D9B), Color(0xFF050B24)),
            blob1 = listOf(Color(0xFFFFA24C).copy(alpha = 0.85f), Color(0xFFD95B56).copy(alpha = 0f)),
            blob2 = listOf(Color(0xFFFFFFFF).copy(alpha = 0.75f), Color(0xFFFFE0B3).copy(alpha = 0f)),
            blob3 = listOf(Color(0xFF93F1FF).copy(alpha = 0.7f), Color.Transparent),
            waveColor = Color(0xFF3569FA)
        )
        com.chroma.studio.chat.data.VoiceGlobeStyleType.ELECTRIC_CYAN -> GlobePalette(
            coreColors = listOf(Color(0xFF38BDF8), Color(0xFF0891B2), Color(0xFF0E7490), Color(0xFF083344)),
            blob1 = listOf(Color(0xFF22D3EE).copy(alpha = 0.85f), Color(0xFF06B6D4).copy(alpha = 0f)),
            blob2 = listOf(Color(0xFFFFFFFF).copy(alpha = 0.75f), Color(0xFFECFEFF).copy(alpha = 0f)),
            blob3 = listOf(Color(0xFF67E8F9).copy(alpha = 0.7f), Color.Transparent),
            waveColor = Color(0xFF22D3EE)
        )
        com.chroma.studio.chat.data.VoiceGlobeStyleType.CHROMA_BLOB -> GlobePalette(
            coreColors = listOf(Color(0xFFFF50A1), Color(0xFFFF00A7), Color(0xFF8800CC), Color(0xFF004DFF)),
            blob1 = listOf(Color(0xFFFF50A1).copy(alpha = 0.85f), Color(0xFFFF6700).copy(alpha = 0f)),
            blob2 = listOf(Color(0xFFFFFFFF).copy(alpha = 0.75f), Color(0xFFFFCCE8).copy(alpha = 0f)),
            blob3 = listOf(Color(0xFFFF00A7).copy(alpha = 0.7f), Color.Transparent),
            waveColor = Color(0xFFFF50A1)
        )
    }
}

/**
 * 3D Liquid AI Plasma Globe inside Dynamic Island.
 * Dynamically supports all 8 color themes with interactive tap-to-cycle animation.
 */
@Composable
fun DynamicIslandLiquidGlobe(
    globeStyle: com.chroma.studio.chat.data.VoiceGlobeStyleType = com.chroma.studio.chat.data.VoiceGlobeStyleType.ORIGINAL_BLUE,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    size: Dp = 112.dp,
    voiceAmplitude: Float? = null
) {
    if (globeStyle == com.chroma.studio.chat.data.VoiceGlobeStyleType.STRANDS) {
        StrandsVoiceGlobe(
            level = voiceAmplitude,
            listening = voiceAmplitude == null,   // simulated speech when no mic level is supplied
            modifier = modifier
                .size(size)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { onClick() },
            params = StrandsParams(glassSize = 1.1f)
        )
        return
    }

    if (globeStyle == com.chroma.studio.chat.data.VoiceGlobeStyleType.CHROMA_BLOB) {
        AIBlobVoiceGlobe(
            level = voiceAmplitude,
            listening = voiceAmplitude == null,
            modifier = modifier
                .size(size)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { onClick() },
            params = AIBlobParams()
        )
        return
    }

    val palette = remember(globeStyle) { getGlobePalette(globeStyle) }
    val infiniteTransition = rememberInfiniteTransition(label = "DynamicIslandGlobe")

    val waveScale1 by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1"
    )
    val waveAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveAlpha1"
    )

    val waveScale2 by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2"
    )
    val waveAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveAlpha2"
    )

    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    val angle1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(5400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle1"
    )
    val angle2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(4600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle2"
    )
    val angle3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle3"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = breathScale
                scaleY = breathScale
            }
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Outer Radar Soundwaves (matches theme wave color)
        Box(
            modifier = Modifier
                .fillMaxSize(0.85f)
                .graphicsLayer {
                    scaleX = waveScale1
                    scaleY = waveScale1
                    alpha = waveAlpha1
                }
                .border(1.2.dp, palette.waveColor.copy(alpha = 0.6f), CircleShape)
        )
        Box(
            modifier = Modifier
                .fillMaxSize(0.85f)
                .graphicsLayer {
                    scaleX = waveScale2
                    scaleY = waveScale2
                    alpha = waveAlpha2
                }
                .border(1.2.dp, palette.waveColor.copy(alpha = 0.6f), CircleShape)
        )

        // Orb Core (Dark Theme Matching OLED Deep Black)
        Box(
            modifier = Modifier
                .fillMaxSize(0.82f)
                .clip(CircleShape)
                .background(Brush.radialGradient(colors = palette.coreColors)),
            contentAlignment = Alignment.Center
        ) {
            // Liquid Blob 1
            val rad1 = Math.toRadians(angle1.toDouble())
            val x1 = (cos(rad1) * 16).toFloat()
            val y1 = (sin(rad1) * 16).toFloat()
            Box(
                modifier = Modifier
                    .size((size.value * 0.58f).dp)
                    .offset(x = x1.dp, y = y1.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(colors = palette.blob1))
            )

            // Liquid Blob 2
            val rad2 = Math.toRadians(angle2.toDouble())
            val x2 = (cos(rad2) * 12).toFloat()
            val y2 = (sin(rad2) * 12).toFloat()
            Box(
                modifier = Modifier
                    .size((size.value * 0.42f).dp)
                    .offset(x = x2.dp, y = y2.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(colors = palette.blob2))
            )

            // Liquid Blob 3
            val rad3 = Math.toRadians(angle3.toDouble())
            val x3 = (cos(rad3) * 14).toFloat()
            val y3 = (sin(rad3) * 14).toFloat()
            Box(
                modifier = Modifier
                    .size((size.value * 0.52f).dp)
                    .offset(x = x3.dp, y = y3.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(colors = palette.blob3))
            )

            // Glass Lens Reflection Overlay & Center Equalizer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.38f),
                                Color.White.copy(alpha = 0.08f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.45f)
                            )
                        )
                    )
                    .border(
                        0.75.dp,
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.65f),
                                Color.White.copy(alpha = 0.1f)
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Live Audio Waveform Bars in Center of Globe
                IslandEqualizer(
                    color = Color.White,
                    barCount = 5
                )
            }
        }
    }
}

// ====================================================================
// SHARED MICRO-COMPOSABLES & ANIMATIONS
// ====================================================================

@Composable
private fun IslandActionButton(
    icon: @Composable () -> Unit,
    label: String,
    bgColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFA1A1AA)
        )
    }
}

/**
 * Animated audio equalizer bars for calls & compact previews.
 */
@Composable
fun IslandEqualizer(
    color: Color,
    barCount: Int = 5,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "IslandEQ")

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.height(14.dp)
    ) {
        repeat(barCount) { index ->
            val duration = 400 + (index * 70) % 350
            val delay = (index * 90) % 300
            val scale by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = duration, delayMillis = delay, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "eqBar$index"
            )
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .fillMaxHeight(scale)
                    .background(color, CircleShape)
            )
        }
    }
}

/**
 * Wide waveform visualizer for recording sessions.
 */
@Composable
private fun IslandWideWaveform(
    color: Color,
    barCount: Int = 18
) {
    val transition = rememberInfiniteTransition(label = "WideWave")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(barCount) { index ->
            val duration = 350 + (sin(index.toDouble()) * 200).toInt().coerceAtLeast(150)
            val delay = (index * 45) % 250
            val minHeight = 0.15f + (sin(index * 0.5) * 0.1f).toFloat().coerceAtLeast(0.05f)
            val maxHeight = 0.5f + (sin(index * 0.8) * 0.5f).toFloat().coerceIn(0.4f, 1f)

            val heightScale by transition.animateFloat(
                initialValue = minHeight,
                targetValue = maxHeight,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = duration, delayMillis = delay, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "wave$index"
            )

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(heightScale)
                    .background(color.copy(alpha = 0.85f), CircleShape)
            )
        }
    }
}

/**
 * Pulsing privacy indicator dot with breathing aura glow.
 */
@Composable
fun IslandPulseDot(
    color: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "DotPulse")
    val pulseScale by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier.size(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer aura ring
        Box(
            modifier = Modifier
                .size(10.dp)
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                    alpha = pulseAlpha
                }
                .background(color, CircleShape)
        )
        // Solid core dot
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color, CircleShape)
        )
    }
}

