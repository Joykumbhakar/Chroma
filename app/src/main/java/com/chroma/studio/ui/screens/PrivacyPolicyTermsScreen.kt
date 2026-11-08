package com.chroma.studio.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.ui.components.LiquidGlassCheckbox
import com.chroma.studio.ui.components.OneUiGlassCard
import com.chroma.studio.ui.components.doubleEdgeGlassBorder
import com.chroma.studio.ui.theme.InterFontFamily
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ShieldCheck

/**
 * Samsung One UI 7.0 Privacy Policy & Terms Acceptance Screen
 * Styled with exact Samsung One UI hierarchy and our iconic double edge glass border.
 */
@Composable
fun PrivacyPolicyTermsScreen(
    isFirstLaunch: Boolean = true,
    onAccept: () -> Unit,
    onDecline: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark
    val screenBg = if (isDark) Color(0xFF080808) else Color(0xFFFAFAFA)

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Privacy Policy, 1 = Terms & Conditions
    var hasAgreed by remember { mutableStateOf(!isFirstLaunch) }

    Scaffold(
        containerColor = screenBg,
        topBar = {
            if (!isFirstLaunch) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(52.dp)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.ChevronLeft,
                            contentDescription = "Back",
                            tint = if (isDark) Color(0xFFFAFAFA) else Color(0xFF000000),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "Legal & Agreement",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFontFamily,
                        color = if (isDark) Color(0xFFFAFAFA) else Color(0xFF000000)
                    )
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                if (isFirstLaunch) {
                    // Checkbox Agreement (Direct on main background)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { hasAgreed = !hasAgreed }
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LiquidGlassCheckbox(
                            checked = hasAgreed,
                            onCheckedChange = { hasAgreed = it }
                        )
                        Text(
                            text = "I agree to the Privacy Policy and Terms of Service",
                            fontSize = 13.5.sp,
                            color = if (isDark) Color(0xFFFAFAFA) else Color(0xFF000000),
                            lineHeight = 18.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    // Primary Action: Samsung One UI Pill Button (Borderless per standing rule)
                    val continueInteraction = remember { MutableInteractionSource() }
                    val continuePressed by continueInteraction.collectIsPressedAsState()
                    val continueScale by animateFloatAsState(
                        targetValue = if (continuePressed) 0.975f else 1f,
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
                        label = "agree_btn_press"
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
                            .background(
                                if (hasAgreed) Brush.horizontalGradient(listOf(Color(0xFF0381FE), Color(0xFF0072DE)))
                                else Brush.horizontalGradient(listOf(Color(0xFF334155), Color(0xFF1E293B)))
                            )
                            .clickable(
                                interactionSource = continueInteraction,
                                indication = null,
                                enabled = hasAgreed,
                                role = Role.Button,
                                onClick = onAccept
                            ),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Agree & Continue",
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFontFamily,
                            letterSpacing = (-0.2).sp
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // Decline Link
                    TextButton(
                        onClick = onDecline,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Decline & Exit App",
                            fontSize = 13.5.sp,
                            color = Color(0xFFF53B57),
                            fontWeight = FontWeight.Medium,
                            fontFamily = InterFontFamily
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0381FE))
                            .clickable(role = Role.Button, onClick = onBack),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Close",
                            fontSize = 15.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFontFamily
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Samsung One UI 7.0 Viewing Area
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (isFirstLaunch) 20.dp else 4.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {


                    Text(
                        text = "Legal & Privacy",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFontFamily,
                        color = if (isDark) Color(0xFFFAFAFA) else Color(0xFF000000),
                        letterSpacing = (-0.6).sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "Please review how Chroma Studio protects your creative rights, encrypted data, and privacy.",
                        fontSize = 13.5.sp,
                        color = if (isDark) Color(0xFF8E8E93) else Color(0xFF71717A),
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp,
                        fontFamily = InterFontFamily,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Samsung One UI Segmented Tab Bar inside a double edge glass container
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isDark) Color(0x1FFFFFFF) else Color(0x0C000000))
                        .padding(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Privacy Policy Tab
                        val privacyActive = selectedTab == 0
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (privacyActive) Color(0xFF0381FE)
                                    else Color.Transparent
                                )
                                .clickable { selectedTab = 0 },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Privacy Policy",
                                fontSize = 13.5.sp,
                                fontWeight = if (privacyActive) FontWeight.Bold else FontWeight.Medium,
                                color = if (privacyActive) Color.White else (if (isDark) Color(0xFF8E8E93) else Color(0xFF71717A)),
                                fontFamily = InterFontFamily
                            )
                        }

                        // Terms & Conditions Tab
                        val termsActive = selectedTab == 1
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (termsActive) Color(0xFF0381FE)
                                    else Color.Transparent
                                )
                                .clickable { selectedTab = 1 },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Terms of Service",
                                fontSize = 13.5.sp,
                                fontWeight = if (termsActive) FontWeight.Bold else FontWeight.Medium,
                                color = if (termsActive) Color.White else (if (isDark) Color(0xFF8E8E93) else Color(0xFF71717A)),
                                fontFamily = InterFontFamily
                            )
                        }
                    }
                }
            }

            // Legal Content Body (Direct on main background)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                ) {
                    if (selectedTab == 0) {
                        // Privacy Policy Content
                        OneUiLegalItem(
                            title = "Information We Collect",
                            description = "When you authenticate with Google, we securely receive your verified email, Google ID, display name, and avatar picture. We never collect or sell private device telemetry or advertising identifiers."
                        )
                        HorizontalDivider(
                            color = if (isDark) Color(0x1FFFFFFF) else Color(0x14000000),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                        OneUiLegalItem(
                            title = "End-to-End Encryption",
                            description = "Direct chat communications and private audio notes are encrypted client-side using cryptographic standards (AES-GCM & RSA-2048). Chroma Studio servers cannot inspect your private direct conversations."
                        )
                        HorizontalDivider(
                            color = if (isDark) Color(0x1FFFFFFF) else Color(0x14000000),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                        OneUiLegalItem(
                            title = "Secure Cloud Infrastructure",
                            description = "User profiles and gradient works are safely stored on Google Cloud Firestore with strict security rules. High-resolution canvas renders are cached through encrypted media pipelines."
                        )
                        HorizontalDivider(
                            color = if (isDark) Color(0x1FFFFFFF) else Color(0x14000000),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                        OneUiLegalItem(
                            title = "Your Rights & Account Deletion",
                            description = "You hold full sovereignty over your account. You can export your artworks, wipe chat logs, or permanently delete your entire profile and identity at any time from Settings."
                        )
                    } else {
                        // Terms of Service Content
                        OneUiLegalItem(
                            title = "Acceptance of Terms",
                            description = "By entering Chroma Studio, you agree to these Terms. If you disagree with any portion of the service terms, you may decline and exit the application."
                        )
                        HorizontalDivider(
                            color = if (isDark) Color(0x1FFFFFFF) else Color(0x14000000),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                        OneUiLegalItem(
                            title = "Artistic Ownership & Copyright",
                            description = "You retain 100% intellectual property ownership of every gradient, visual composition, story, and artwork you create. You decide your sharing, remix, and download permissions."
                        )
                        HorizontalDivider(
                            color = if (isDark) Color(0x1FFFFFFF) else Color(0x14000000),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                        OneUiLegalItem(
                            title = "Community Safety & Mutual Respect",
                            description = "Chroma Studio is dedicated to creative excellence. Harassment, unauthorized automated scraping, hate speech, or malicious content will result in immediate profile suspension."
                        )
                        HorizontalDivider(
                            color = if (isDark) Color(0x1FFFFFFF) else Color(0x14000000),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                        OneUiLegalItem(
                            title = "Platform Evolution",
                            description = "We continuously refine rendering pipelines, real-time filters, and cloud synchronization. Terms may be updated periodically with in-app notice."
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OneUiLegalItem(
    title: String,
    description: String
) {
    val isDark = LocalOscellionColors.current.isDark

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isDark) Color(0xFFFAFAFA) else Color(0xFF000000),
            fontFamily = InterFontFamily,
            letterSpacing = (-0.2).sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = description,
            fontSize = 13.sp,
            color = if (isDark) Color(0xFF8E8E93) else Color(0xFF71717A),
            lineHeight = 19.sp,
            fontFamily = InterFontFamily
        )
    }
}
