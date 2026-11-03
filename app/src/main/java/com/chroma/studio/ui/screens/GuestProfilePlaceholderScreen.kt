package com.chroma.studio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.ui.components.GlassButton
import com.chroma.studio.ui.components.LiquidGlassSecondaryButton
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Sparkles

@Composable
fun GuestProfilePlaceholderScreen(
    onSignInClick: () -> Unit,
    onExploreStudioClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark
    val scrollState = rememberScrollState()

    val cardBg = if (isDark) Color(0xFF16181F) else Color(0xFFF7F8FC)
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
    val accentBlue = Color(0xFF0A84FF)
    val accentGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF0A84FF), Color(0xFF5E5CE6), Color(0xFFFF2D55))
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Glowing Icon Header
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .shadow(16.dp, CircleShape, spotColor = accentBlue.copy(alpha = 0.35f))
                    .clip(CircleShape)
                    .background(
                        if (isDark) Color(0xFF1C1E26) else Color.White
                    )
                    .border(2.dp, accentGradient, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.Sparkles,
                    contentDescription = null,
                    tint = accentBlue,
                    modifier = Modifier.size(42.dp)
                )
            }

            // Mode Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(accentBlue.copy(alpha = 0.12f))
                    .border(1.dp, accentBlue.copy(alpha = 0.3f), RoundedCornerShape(100.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "EXPLORE STUDIO MODE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentBlue,
                    letterSpacing = 0.8.sp
                )
            }

            // Title & Description
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Explore & Create Gradients",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "You're exploring as a guest. You can freely create gradients on the canvas and browse community posts. Sign in or create an account to unlock all features.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            // Capabilities Checklist Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "FEATURE ACCESS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    color = colors.textMuted
                )

                // Unlocked items
                FeatureRow(
                    isUnlocked = true,
                    text = "Design & edit custom gradients on canvas",
                    textColor = colors.textMain
                )
                FeatureRow(
                    isUnlocked = true,
                    text = "Browse community posts & explore trends",
                    textColor = colors.textMain
                )

                // Locked items
                FeatureRow(
                    isUnlocked = false,
                    text = "Publish works to community feed",
                    textColor = colors.textMuted
                )
                FeatureRow(
                    isUnlocked = false,
                    text = "Like, save, remix & comment on posts",
                    textColor = colors.textMuted
                )
                FeatureRow(
                    isUnlocked = false,
                    text = "Direct messages & creator chat",
                    textColor = colors.textMuted
                )
                FeatureRow(
                    isUnlocked = false,
                    text = "Stories, Flash Video editor & cloud sync",
                    textColor = colors.textMuted
                )
                FeatureRow(
                    isUnlocked = false,
                    text = "Personal profile, followers & archives",
                    textColor = colors.textMuted
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Primary Sign In / Create Account Button
                GlassButton(
                    onClick = onSignInClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "Sign In or Create Account",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                // Secondary Return to Canvas Button
                LiquidGlassSecondaryButton(
                    onClick = onExploreStudioClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "Continue Designing Gradients",
                        color = colors.textMain,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FeatureRow(
    isUnlocked: Boolean,
    text: String,
    textColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (isUnlocked) Color(0xFF34C759).copy(alpha = 0.15f)
                    else Color(0xFFFF9500).copy(alpha = 0.12f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isUnlocked) Lucide.Check else Lucide.Lock,
                contentDescription = null,
                tint = if (isUnlocked) Color(0xFF34C759) else Color(0xFFFF9500),
                modifier = Modifier.size(13.dp)
            )
        }
        Text(
            text = text,
            fontSize = 13.5.sp,
            color = textColor,
            fontWeight = if (isUnlocked) FontWeight.Medium else FontWeight.Normal
        )
    }
}
