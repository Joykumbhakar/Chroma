package com.chroma.studio.ui.onboarding

import androidx.compose.ui.draw.drawWithCache

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import com.chroma.studio.R
import com.chroma.studio.model.UserProfile
import com.chroma.studio.ui.components.doubleEdgeGlassBorder
import com.chroma.studio.ui.components.VerifiedBadge
import com.chroma.studio.ui.theme.InterFontFamily
import com.composables.icons.lucide.ArrowRight
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.User

/**
 * AccountConfirmationScreen (AccountConfirmationView)
 *
 * Implements Returning User confirmation flow:
 * - Prompt: "Is this your account? Continue with this account"
 * - User profile preview (avatar, display name, handle/email).
 * - Primary CTA: "Continue with this account" -> logs in to Home.
 * - Secondary Link: "This is not my account" -> purges session and returns to Welcome.
 */
@Composable
fun AccountConfirmationScreen(
    profile: UserProfile,
    email: String,
    isLoading: Boolean = false,
    onContinue: () -> Unit,
    onNotMine: () -> Unit
) {
    val colors = com.chroma.studio.ui.theme.LocalOscellionColors.current
    val isDark = colors.isDark
    val screenBg = if (isDark) Color(0xFF080808) else Color(0xFFFAFAFA)
    val cardBg = if (isDark) Color(0xFF17171A) else Color(0xFFFFFFFF)
    val textPrimary = if (isDark) Color(0xFFFAFAFA) else Color(0xFF000000)
    val textMuted = if (isDark) Color(0xFF8E8E93) else Color(0xFF71717A)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── TOP HEADER SECTION ──────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Badge (no border on pill/capsule)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isDark) Color(0xFF222226) else Color(0xFFF2F4F7))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF3E91FF) else Color(0xFF0072DE),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Account Found",
                            color = if (isDark) Color(0xFF3E91FF) else Color(0xFF0072DE),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.3.sp
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    text = "Is this your account?",
                    color = textPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFontFamily,
                    letterSpacing = (-0.6).sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Continue with this account",
                    color = textMuted,
                    fontSize = 14.sp,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = (-0.2).sp,
                    textAlign = TextAlign.Center
                )
            }

            // ── USER PROFILE PREVIEW (DIRECT ON MAIN BACKGROUND) ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                    // Avatar with glowing gradient ring
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    listOf(
                                        Color(0xFF38BDF8),
                                        Color(0xFF2563EB),
                                        Color(0xFF9333EA),
                                        Color(0xFF38BDF8)
                                    )
                                )
                            )
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0B0F19)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profile.avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = profile.avatarUrl,
                                contentDescription = profile.displayName,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Lucide.User,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Display Name
                    Text(
                        text = profile.displayName.ifBlank { "Oscellion Creator" },
                        color = textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.4).sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(4.dp))

                    // @username Handle
                    Text(
                        text = "@${profile.username.ifBlank { "creator" }}",
                        color = if (isDark) Color(0xFF3E91FF) else Color(0xFF0072DE),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-0.2).sp,
                        textAlign = TextAlign.Center
                    )

                    if (email.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (isDark) Color(0xFF222226) else Color(0xFFF2F4F7))
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.ic_google_logo),
                                    contentDescription = "Google",
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = email,
                                    color = textMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }
                }

            // ── BOTTOM ACTIONS SECTION ──────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Primary CTA: "Continue with this account"
                val continueInteraction = remember { MutableInteractionSource() }
                val continuePressed by continueInteraction.collectIsPressedAsState()
                val continueScale by animateFloatAsState(
                    targetValue = if (continuePressed) 0.975f else 1f,
                    animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
                    label = "continueScale"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .graphicsLayer {
                            scaleX = continueScale
                            scaleY = continueScale
                        }
                        .shadow(8.dp, CircleShape, ambientColor = Color(0x40000000), spotColor = Color(0x60000000))
                        .clip(CircleShape)
                        .background(Color(0xFF0381FE))
                        .clickable(
                            interactionSource = continueInteraction,
                            indication = null,
                            enabled = !isLoading,
                            role = Role.Button,
                            onClick = onContinue
                        )
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.4.dp
                        )
                    } else {
                        Text(
                            text = "Continue with this account",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Lucide.ArrowRight,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Secondary Link: "This is not my account"
                Text(
                    text = "This is not my account",
                    color = textMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            role = Role.Button,
                            enabled = !isLoading,
                            onClick = onNotMine
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}
