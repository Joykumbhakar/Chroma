package com.chroma.studio.webrtc

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
        val vibrator = resolveVibrator(context)
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
                resolveVibrator(context)?.cancel()
            } catch (_: Exception) {}
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "acceptPulse")
    val acceptScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "acceptScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF06080C))
    ) {
        if (call.callerAvatar.isNotBlank()) {
            AsyncImage(
                model = call.callerAvatar,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(56.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0B0E15).copy(alpha = 0.78f),
                            Color(0xFF06080C).copy(alpha = 0.90f),
                            Color(0xFF030406).copy(alpha = 0.96f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Apple FaceTime / iOS Contact Poster Header (Status above large Contact Name)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(
                        imageVector = if (call.isVideo) Lucide.Video else Lucide.PhoneCall,
                        contentDescription = null,
                        tint = if (call.isVideo) Color(0xFF0381FE) else Color(0xFF30D158),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = if (call.isVideo) "FaceTime Video" else "Oscellion Audio",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.80f)
                    )
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = call.callerName.ifBlank { "Oscellion User" },
                    fontSize = 34.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Lucide.Lock,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.50f),
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "End-to-end encrypted",
                        fontSize = 12.5.sp,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                }
            }

            // Center Peer Avatar (legitimate avatar, no artificial halos or boxes)
            Box(
                modifier = Modifier.size(144.dp),
                contentAlignment = Alignment.Center
            ) {
                IosAvatarView(
                    name = call.callerName,
                    avatarUrl = call.callerAvatar,
                    size = 136.dp,
                    isOnline = true
                )
            }

            // Clean Action Dock (Rounded rectangles, no circular icon backgrounds or pills)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFFF3B30))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDecline
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Lucide.PhoneOff,
                        contentDescription = "Decline Call",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Decline",
                        color = Color.White,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .scale(acceptScale)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF34C759))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onAccept
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (call.isVideo) Lucide.Video else Lucide.Phone,
                        contentDescription = "Accept Call",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Accept",
                        color = Color.White,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun resolveVibrator(context: Context): Vibrator? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
}
