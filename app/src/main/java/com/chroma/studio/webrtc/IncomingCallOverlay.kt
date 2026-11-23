package com.chroma.studio.webrtc

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.ui.components.IosAvatarView
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*
import kotlinx.coroutines.delay

@Composable
fun IncomingCallOverlay(
    call: CallModel?,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    if (call == null) return

    val colors = LocalOscellionColors.current
    val context = LocalContext.current

    // Continuous ringing sound & vibration loop
    LaunchedEffect(call.id) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        while (true) {
            SoundManager.playRing()
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 1000), 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 800, 1000), 0)
                }
            } catch (_: Exception) {}
            delay(3000)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.cancel()
            } catch (_: Exception) {}
        }
    }

    // Pulse animation for avatar rings
    val infiniteTransition = rememberInfiniteTransition(label = "ringPulse")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = EaseOutQuad),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringScale"
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = EaseOutQuad),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringAlpha"
    )

    // Breathing scale for Accept button
    val acceptPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "acceptPulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Call Type Badge
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 28.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .background(colors.primary.copy(alpha = 0.20f))
                        .border(1.dp, colors.primary.copy(alpha = 0.40f), RoundedCornerShape(9999.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (call.isVideo) Lucide.Video else Lucide.PhoneCall,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (call.isVideo) "Incoming Video Call" else "Incoming Audio Call",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    text = call.callerName.ifBlank { "Oscellion User" },
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Oscellion WebRTC Call",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.60f)
                )
            }

            // Center Section: Avatar with Pulsing Halo Rings
            Box(
                modifier = Modifier.size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                // Expanding pulsing halo ring 1
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .scale(ringScale)
                        .background(
                            (if (call.isVideo) colors.primary else colors.onlineStatus).copy(alpha = ringAlpha),
                            CircleShape
                        )
                )
                // Expanding pulsing halo ring 2
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(ringScale * 0.85f)
                        .background(
                            (if (call.isVideo) colors.primary else colors.onlineStatus).copy(alpha = ringAlpha * 0.7f),
                            CircleShape
                        )
                )

                // Avatar
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .shadow(24.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.6f))
                        .border(3.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                        .padding(3.dp)
                ) {
                    IosAvatarView(
                        name = call.callerName,
                        avatarUrl = call.callerAvatar,
                        size = 124.dp,
                        isOnline = true
                    )
                }
            }

            // Bottom Section: Decline and Accept Circular Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Decline Button (Red)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .shadow(12.dp, CircleShape, spotColor = Color(0xFFFF3B30).copy(alpha = 0.5f))
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFFFF453A), Color(0xFFFF3B30))
                                )
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onDecline
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.PhoneOff,
                            contentDescription = "Decline Call",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Decline",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Accept Button (Green with breathing pulse)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .scale(acceptPulse)
                            .shadow(16.dp, CircleShape, spotColor = Color(0xFF34C759).copy(alpha = 0.6f))
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF30D158), Color(0xFF34C759))
                                )
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onAccept
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (call.isVideo) Lucide.Video else Lucide.Phone,
                            contentDescription = "Accept Call",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Accept",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
