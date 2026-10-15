package com.chroma.studio.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.ui.components.OscellionBackground
import com.chroma.studio.ui.theme.InterFontFamily
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.*

@Composable
fun PrivacyPolicyTermsScreen(
    isFirstLaunch: Boolean = true,
    onAccept: () -> Unit,
    onDecline: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val colors = LocalOscellionColors.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Privacy Policy, 1 = Terms & Conditions
    var hasAgreed by remember { mutableStateOf(!isFirstLaunch) }

    OscellionBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isFirstLaunch) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.ChevronLeft,
                                contentDescription = "Back",
                                tint = colors.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Spacer(Modifier.width(8.dp))
                    }

                    Text(
                        text = "Legal & Agreement",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFontFamily,
                        color = colors.textMain,
                        modifier = Modifier.weight(1f),
                        textAlign = if (!isFirstLaunch) TextAlign.Start else TextAlign.Center
                    )

                    if (isFirstLaunch) {
                        Spacer(Modifier.width(8.dp))
                    }
                }
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    if (isFirstLaunch) {
                        // Agreement Checkbox Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { hasAgreed = !hasAgreed }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = hasAgreed,
                                onCheckedChange = { hasAgreed = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = colors.primary,
                                    uncheckedColor = colors.textMuted
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "I have read and agree to the Privacy Policy and Terms & Conditions",
                                fontSize = 13.sp,
                                color = colors.textMain,
                                lineHeight = 17.sp,
                                fontFamily = InterFontFamily,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        // Accept Button
                        Button(
                            onClick = onAccept,
                            enabled = hasAgreed,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                disabledContainerColor = colors.primary.copy(alpha = 0.35f),
                                contentColor = Color.White,
                                disabledContentColor = Color.White.copy(alpha = 0.6f)
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
                        ) {
                            Text(
                                text = "Agree & Continue",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = InterFontFamily
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        // Decline Button
                        TextButton(
                            onClick = onDecline,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Decline & Exit App",
                                fontSize = 14.sp,
                                color = colors.error,
                                fontWeight = FontWeight.Medium,
                                fontFamily = InterFontFamily
                            )
                        }
                    } else {
                        Button(
                            onClick = onBack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
                        ) {
                            Text(
                                text = "Close",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
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
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Header Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Lucide.ShieldCheck,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(38.dp)
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "Privacy Policy & Terms",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFontFamily,
                            color = colors.textMain,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = "Please review how we protect your data and the terms governing your experience in Chroma Studio.",
                            fontSize = 14.sp,
                            color = colors.textMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp,
                            fontFamily = InterFontFamily
                        )
                    }
                }

                item {
                    // Direct Segmented Control Tabs (No Pills)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, colors.glassBorder.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                            .padding(3.dp)
                    ) {
                        Surface(
                            onClick = { selectedTab = 0 },
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedTab == 0) colors.primary.copy(alpha = 0.15f) else Color.Transparent
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Privacy Policy",
                                    fontSize = 14.sp,
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == 0) colors.primary else colors.textMuted,
                                    fontFamily = InterFontFamily
                                )
                            }
                        }

                        Surface(
                            onClick = { selectedTab = 1 },
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedTab == 1) colors.primary.copy(alpha = 0.15f) else Color.Transparent
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Terms & Conditions",
                                    fontSize = 14.sp,
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == 1) colors.primary else colors.textMuted,
                                    fontFamily = InterFontFamily
                                )
                            }
                        }
                    }
                }

                if (selectedTab == 0) {
                    // Privacy Policy Content
                    item {
                        AgreementSection(
                            icon = Lucide.Database,
                            iconColor = colors.primary,
                            title = "1. Information We Collect",
                            description = "When you sign in with Google or create an account, we collect your display name, username, email address, profile avatar, and user bio. We do not collect unnecessary background telemetries or private device identifiers."
                        )
                    }

                    item {
                        AgreementSection(
                            icon = Lucide.Lock,
                            iconColor = Color(0xFF34C759),
                            title = "2. End-to-End Encryption",
                            description = "Your chat messages, direct communications, and private voice notes are protected by client-side End-to-End Encryption (AES-GCM & RSA-2048). Chroma Studio servers and administrators cannot read or inspect your private conversations."
                        )
                    }

                    item {
                        AgreementSection(
                            icon = Lucide.Cloud,
                            iconColor = Color(0xFF5856D6),
                            title = "3. Cloud Storage Architecture",
                            description = "We securely utilize Firebase Firestore & Realtime Database for real-time presence and instant updates, Supabase for authenticated profile assets, and Cloudinary for optimized media delivery and gradient storage."
                        )
                    }

                    item {
                        AgreementSection(
                            icon = Lucide.Shield,
                            iconColor = Color(0xFFFF9500),
                            title = "4. Zero Third-Party Tracking",
                            description = "We will never sell, rent, or trade your personal data, artwork, or behavioral information to third-party data brokers or advertising networks. Your creative data belongs exclusively to you."
                        )
                    }

                    item {
                        AgreementSection(
                            icon = Lucide.Trash2,
                            iconColor = Color(0xFFFF3B30),
                            title = "5. Account & Data Deletion",
                            description = "You have full sovereignty over your data. You can delete individual posts, clear your chat history, or request a complete permanent account purge from the Account Center settings at any time."
                        )
                    }
                } else {
                    // Terms & Conditions Content
                    item {
                        AgreementSection(
                            icon = Lucide.FileText,
                            iconColor = colors.primary,
                            title = "1. Acceptance of Terms",
                            description = "By creating an account or accessing Chroma Studio, you agree to abide by these Terms of Service. If you disagree with any part of these terms, please discontinue using the application."
                        )
                    }

                    item {
                        AgreementSection(
                            icon = Lucide.Sparkles,
                            iconColor = Color(0xFFFF9500),
                            title = "2. Creative Rights & Intellectual Property",
                            description = "You retain 100% intellectual property ownership of any gradient artworks, visual compositions, flashes, and designs you produce. By publishing your work publicly, you grant other users permission to view and interact according to your remix preferences."
                        )
                    }

                    item {
                        AgreementSection(
                            icon = Lucide.Users,
                            iconColor = Color(0xFF34C759),
                            title = "3. Community Safety & Conduct",
                            description = "Chroma Studio is a creative and respectful space. Harassment, hate speech, unlawful media, impersonation, or intellectual property infringement are strictly prohibited and will lead to immediate account suspension."
                        )
                    }

                    item {
                        AgreementSection(
                            icon = Lucide.Share2,
                            iconColor = Color(0xFF5856D6),
                            title = "4. Remix & Collaboration Guidelines",
                            description = "When remixing another creator's published gradient work, proper artistic attribution is automatically maintained. You may not misrepresent original collaborative works as solely your own."
                        )
                    }

                    item {
                        AgreementSection(
                            icon = Lucide.Info,
                            iconColor = Color(0xFFFF3B30),
                            title = "5. Service Availability & Modifications",
                            description = "We continually enhance Chroma Studio. We reserve the right to update features, improve security protocols, or revise terms with reasonable notice to our creator community."
                        )
                    }
                }
            }
        }
    }
}

// ─── Direct Section (No Cards) ───────────────────────

@Composable
private fun AgreementSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    description: String
) {
    val colors = LocalOscellionColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textMain,
                fontFamily = InterFontFamily
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = description,
            fontSize = 13.5.sp,
            color = colors.textMuted,
            lineHeight = 19.sp,
            fontFamily = InterFontFamily,
            modifier = Modifier.padding(start = 30.dp)
        )

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = colors.glassBorder.copy(alpha = 0.4f), thickness = 0.5.dp)
    }
}
