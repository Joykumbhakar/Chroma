package com.chroma.studio.chat.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.chat.ui.theme.LocalChatColors

enum class PermissionRequestType(
    val title: String,
    val description: String,
    val buttonText: String
) {
    MICROPHONE(
        title = "“ChatApp” Would Like to Access the Microphone",
        description = "This allows you to record crystal-clear voice notes, power liquid voice globes, and transcribe your speech into text in real time.",
        buttonText = "Allow Microphone"
    ),
    CAMERA(
        title = "“ChatApp” Would Like to Access the Camera",
        description = "This enables taking live photos, recording video stories, and sharing visual moments directly within your chats.",
        buttonText = "Allow Camera"
    ),
    GALLERY(
        title = "“ChatApp” Would Like to Access Your Photos",
        description = "ChatApp uses this to let you browse your photo library, preview media in high resolution, and share memories with friends.",
        buttonText = "Allow Access"
    ),
    SPEECH(
        title = "“ChatApp” Would Like to Use Speech Recognition",
        description = "Voice recognition transcribes your spoken audio into formatted text messages automatically on-device.",
        buttonText = "Enable Voice Typing"
    )
}

/**
 * Authentic Apple iOS System Permission Dialog Box.
 * Clean, pure Apple typography with NO bulky or unprofessional icon badges.
 */
@Composable
fun PermissionExplanationModal(
    type: PermissionRequestType,
    onDismiss: () -> Unit,
    onGrant: () -> Unit,
    onOpenSettings: (() -> Unit)? = null
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
                    // Header Content (Clean iOS Typography - NO bulky icon box)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Title
                        Text(
                            text = type.title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textDark,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )

                        Spacer(Modifier.height(2.dp))

                        // Description
                        Text(
                            text = type.description,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = colors.textMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.5.sp
                        )
                    }

                    // Hairline Divider
                    HorizontalDivider(
                        color = if (colors.isLight) Color(0x2E3C3C43) else Color(0x38545458),
                        thickness = 0.5.dp
                    )

                    // iOS Action Rows
                    // 1. Primary Allow Action
                    val allowInteraction = remember { MutableInteractionSource() }
                    val isAllowPressed by allowInteraction.collectIsPressedAsState()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .background(
                                if (isAllowPressed) {
                                    if (colors.isLight) Color(0x1F000000) else Color(0x1FFFFFFF)
                                } else Color.Transparent
                            )
                            .clickable(
                                interactionSource = allowInteraction,
                                indication = null
                            ) {
                                SoundManager.playReaction()
                                onGrant()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = type.buttonText,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.iosBlue,
                            textAlign = TextAlign.Center
                        )
                    }

                    // 2. Optional Open Settings Action
                    if (onOpenSettings != null) {
                        HorizontalDivider(
                            color = if (colors.isLight) Color(0x2E3C3C43) else Color(0x38545458),
                            thickness = 0.5.dp
                        )

                        val settingsInteraction = remember { MutableInteractionSource() }
                        val isSettingsPressed by settingsInteraction.collectIsPressedAsState()

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .background(
                                    if (isSettingsPressed) {
                                        if (colors.isLight) Color(0x1F000000) else Color(0x1FFFFFFF)
                                    } else Color.Transparent
                                )
                                .clickable(
                                    interactionSource = settingsInteraction,
                                    indication = null
                                ) {
                                    SoundManager.playReaction()
                                    onOpenSettings()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Open Settings",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Normal,
                                color = colors.iosBlue,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // 3. Don't Allow Action
                    HorizontalDivider(
                        color = if (colors.isLight) Color(0x2E3C3C43) else Color(0x38545458),
                        thickness = 0.5.dp
                    )

                    val dismissInteraction = remember { MutableInteractionSource() }
                    val isDismissPressed by dismissInteraction.collectIsPressedAsState()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .background(
                                if (isDismissPressed) {
                                    if (colors.isLight) Color(0x1F000000) else Color(0x1FFFFFFF)
                                } else Color.Transparent
                            )
                            .clickable(
                                interactionSource = dismissInteraction,
                                indication = null
                            ) {
                                SoundManager.playReaction()
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Don’t Allow",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Normal,
                            color = colors.iosBlue,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

