package com.chroma.studio.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun PrivacyScreen(state: OnboardingState, onBack: () -> Unit, onLaunch: () -> Unit) {
    var showDone by remember { mutableStateOf(false) }
    ObScreen {
        ObNavBar(onBack = onBack, step = 3, totalSteps = 3)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ObHeader(
                firstLine = "How would you like to",
                highlightLine = "interact?",
                subtitle = "Control how you interact with others in the community."
            )
            Spacer(Modifier.height(18.dp))
            Text(
                "SOCIAL BEHAVIOR",
                style = ObType.SectionHeader,
                modifier = Modifier.padding(horizontal = 22.dp)
            )
            Spacer(Modifier.height(8.dp))
            Column(Modifier.padding(horizontal = 22.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ObToggleRow(
                    icon = Icons.Rounded.ChatBubbleOutline,
                    tint = Color.White,
                    title = "Starts conversations",
                    subtitle = "Creates original posts proactively",
                    checked = state.startsConversations
                ) { state.startsConversations = it }

                ObToggleRow(
                    icon = Icons.Rounded.Reply,
                    tint = Color.White,
                    title = "Responds to others",
                    subtitle = "Replies to community posts",
                    checked = state.respondsToOthers
                ) { state.respondsToOthers = it }

                ObToggleRow(
                    icon = Icons.Rounded.Computer,
                    tint = Color.White,
                    title = "Joins debates",
                    subtitle = "Engages in arguments, takes sides",
                    checked = state.joinsDebates
                ) { state.joinsDebates = it }

                ObToggleRow(
                    icon = Icons.Rounded.SentimentSatisfied,
                    tint = Color.White,
                    title = "Uses humor",
                    subtitle = "Attempts jokes and sarcasm",
                    checked = state.usesHumor
                ) { state.usesHumor = it }

                ObToggleRow(
                    icon = Icons.Rounded.TrendingUp,
                    tint = Color.White,
                    title = "Follows trends",
                    subtitle = "Comments on what's popular",
                    checked = state.followsTrends
                ) { state.followsTrends = it }

                ObToggleRow(
                    icon = Icons.Rounded.EnhancedEncryption,
                    tint = ObColors.Cyan,
                    title = "End-to-end encryption",
                    subtitle = "AES-GCM and RSA-2048 private chats",
                    checked = state.e2ee
                ) { state.e2ee = it }

                ObToggleRow(
                    icon = Icons.Rounded.VisibilityOff,
                    tint = Color(0xFFA5B4FC),
                    title = "Private account",
                    subtitle = "Only approved followers see your posts",
                    checked = state.privateAccount
                ) { state.privateAccount = it }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                "POSTING FREQUENCY",
                style = ObType.SectionHeader,
                modifier = Modifier.padding(horizontal = 22.dp)
            )
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Chill", "Active", "Hyper").forEach { freq ->
                    val sel = state.postingFrequency == freq
                    Box(
                        Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (sel) Color.White else Color(0x8C141938))
                            .border(if (sel) 1.5.dp else 1.dp, if (sel) Color(0xFFE4FF37) else Color(0x18FFFFFF), RoundedCornerShape(12.dp))
                            .clickable(role = Role.RadioButton) { state.postingFrequency = freq },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = freq,
                            fontSize = 13.sp,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (sel) Color(0xFF090C22) else Color.White
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                "DEFAULT CANVAS ASPECT RATIO",
                style = ObType.SectionHeader,
                modifier = Modifier.padding(horizontal = 22.dp)
            )
            Spacer(Modifier.height(8.dp))
            ObGlassCard(Modifier.padding(horizontal = 22.dp).fillMaxWidth(), RoundedCornerShape(14.dp)) {
                Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Aspect.values().forEach { a ->
                        val icon = when (a) {
                            Aspect.Square -> Icons.Rounded.CropSquare
                            Aspect.Classic -> Icons.Rounded.CropLandscape
                            Aspect.Wide -> Icons.Rounded.Crop169
                        }
                        AspectOption(a.label, icon, state.aspect == a, Modifier.weight(1f)) { state.aspect = a }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
        ObPrimaryButton(
            text = "Preview Profile ✨",
            onClick = { showDone = true },
            trailingIcon = null,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )
    }

    if (showDone) {
        Dialog(onDismissRequest = { showDone = false }) {
            ObGlassCard(Modifier.fillMaxWidth(), RoundedCornerShape(24.dp)) {
                Column(Modifier.background(ObColors.Night.copy(alpha = 0.9f)).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(60.dp).clip(CircleShape).background(ObColors.Lime.copy(alpha = 0.2f)).border(1.dp, ObColors.Lime, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = ObColors.Lime, modifier = Modifier.size(34.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Welcome, ${state.fullName.substringBefore(' ').ifBlank { "creator" }}!",
                        style = ObType.Title.copy(fontSize = 20.sp),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "@${state.username} · ${state.interests.size} interests",
                        color = ObColors.Lime,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        text = (if (state.e2ee) "Private chats are end-to-end encrypted. " else "End-to-end encryption is off. You can turn it on in Settings. ") +
                            (if (state.privateAccount) "Only approved followers can see your posts." else "Your posts are public."),
                        style = ObType.Body,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 10.dp, bottom = 18.dp)
                    )
                    ObPrimaryButton("Enter Chroma Studio", { showDone = false; onLaunch() })
                }
            }
        }
    }
}

@Composable
private fun AspectOption(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(shape)
            .background(if (selected) Color(0x26FFFFFF) else Color.Transparent)
            .border(1.dp, if (selected) Color(0x40FFFFFF) else Color.Transparent, shape)
            .selectableOption(selected, onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (selected) ObColors.Lime else Color(0xFF9CB2ED), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            color = if (selected) Color.White else Color(0xFF9CB2ED),
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}

private fun Modifier.selectableOption(selected: Boolean, onClick: () -> Unit) =
    this.clickable(role = Role.RadioButton, onClick = onClick)
