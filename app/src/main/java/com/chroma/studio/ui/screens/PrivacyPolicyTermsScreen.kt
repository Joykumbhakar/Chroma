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
import com.chroma.studio.ui.components.LiquidGlassCheckbox
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
                        .height(54.dp)
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
                        .padding(horizontal = 24.dp, vertical = 14.dp)
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
                            LiquidGlassCheckbox(
                                checked = hasAgreed,
                                onCheckedChange = { hasAgreed = it }
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "I have read and agree to the Privacy Policy and Terms & Conditions",
                                fontSize = 13.5.sp,
                                color = colors.textMain,
                                lineHeight = 18.sp,
                                fontFamily = InterFontFamily,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        // Accept Button - Professional Clean Rectangular Button (No Pill, No Shadow)
                        Button(
                            onClick = onAccept,
                            enabled = hasAgreed,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF007AFF),
                                disabledContainerColor = Color(0xFF007AFF).copy(alpha = 0.4f)
                            ),
                            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                text = "Agree & Continue",
                                fontSize = 15.5.sp,
                                color = Color.White,
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
                                color = Color(0xFFFF3B30),
                                fontWeight = FontWeight.Medium,
                                fontFamily = InterFontFamily
                            )
                        }
                    } else {
                        Button(
                            onClick = onBack,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (colors.isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
                            ),
                            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                text = "Close",
                                fontSize = 15.sp,
                                color = colors.textMain,
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
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Header Section with Clean Vector Graphic (No Shadows, No Pills)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Lucide.ShieldCheck,
                            contentDescription = null,
                            tint = Color(0xFF007AFF),
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
                            fontFamily = InterFontFamily,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }

                item {
                    // Segmented Control Tabs (Clean Rectangular, No Pills)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (colors.isDark) Color(0xFF1C1C1E) else Color(0xFFE5E5EA))
                            .padding(2.5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(
                                    if (selectedTab == 0) (if (colors.isDark) Color(0xFF2C2C2E) else Color.White)
                                    else Color.Transparent
                                )
                                .clickable { selectedTab = 0 },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Privacy Policy",
                                fontSize = 13.5.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selectedTab == 0) colors.textMain else colors.textMuted,
                                fontFamily = InterFontFamily
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(
                                    if (selectedTab == 1) (if (colors.isDark) Color(0xFF2C2C2E) else Color.White)
                                    else Color.Transparent
                                )
                                .clickable { selectedTab = 1 },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Terms & Conditions",
                                fontSize = 13.5.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selectedTab == 1) colors.textMain else colors.textMuted,
                                fontFamily = InterFontFamily
                            )
                        }
                    }
                }

                if (selectedTab == 0) {
                    // Privacy Policy Content (Clean Typography, No Icons, No Numbers/Bullets)
                    item {
                        LegalParagraphSection(
                            title = "Information We Collect",
                            description = "When you sign in with Google or create an account, we collect your display name, username, email address, profile avatar, and user bio. We do not collect unnecessary background telemetries or private device identifiers."
                        )
                    }

                    item {
                        LegalParagraphSection(
                            title = "End-to-End Encryption",
                            description = "Your chat messages, direct communications, and private voice notes are protected by client-side End-to-End Encryption (AES-GCM & RSA-2048). Chroma Studio servers and administrators cannot read or inspect your private conversations."
                        )
                    }

                    item {
                        LegalParagraphSection(
                            title = "Cloud Storage Architecture",
                            description = "We securely utilize Firebase Firestore & Realtime Database for real-time presence and instant updates, Supabase for authenticated profile assets, and Cloudinary for optimized media delivery and gradient storage."
                        )
                    }

                    item {
                        LegalParagraphSection(
                            title = "Zero Third-Party Tracking",
                            description = "We will never sell, rent, or trade your personal data, artwork, or behavioral information to third-party data brokers or advertising networks. Your creative data belongs exclusively to you."
                        )
                    }

                    item {
                        LegalParagraphSection(
                            title = "Account & Data Deletion",
                            description = "You have full sovereignty over your data. You can delete individual posts, clear your chat history, or request a complete permanent account purge from the Account Center settings at any time."
                        )
                    }
                } else {
                    // Terms & Conditions Content (Clean Typography, No Icons, No Numbers/Bullets)
                    item {
                        LegalParagraphSection(
                            title = "Acceptance of Terms",
                            description = "By creating an account or accessing Chroma Studio, you agree to abide by these Terms of Service. If you disagree with any part of these terms, please discontinue using the application."
                        )
                    }

                    item {
                        LegalParagraphSection(
                            title = "Creative Rights & Intellectual Property",
                            description = "You retain 100% intellectual property ownership of any gradient artworks, visual compositions, flashes, and designs you produce. By publishing your work publicly, you grant other users permission to view and interact according to your remix preferences."
                        )
                    }

                    item {
                        LegalParagraphSection(
                            title = "Community Safety & Conduct",
                            description = "Chroma Studio is a creative and respectful space. Harassment, hate speech, unlawful media, impersonation, or intellectual property infringement are strictly prohibited and will lead to immediate account suspension."
                        )
                    }

                    item {
                        LegalParagraphSection(
                            title = "Remix & Collaboration Guidelines",
                            description = "When remixing another creator's published gradient work, proper artistic attribution is automatically maintained. You may not misrepresent original collaborative works as solely your own."
                        )
                    }

                    item {
                        LegalParagraphSection(
                            title = "Service Availability & Modifications",
                            description = "We continually enhance Chroma Studio. We reserve the right to update features, improve security protocols, or revise terms with reasonable notice to our creator community."
                        )
                    }
                }
            }
        }
    }
}

// ─── Professional Clean Legal Paragraph Section ───────────────────────
// No icons, no numbers, no bullets, clean typography and minimal hairline divider.

@Composable
private fun LegalParagraphSection(
    title: String,
    description: String
) {
    val colors = LocalOscellionColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = title,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textMain,
            fontFamily = InterFontFamily
        )

        Spacer(Modifier.height(5.dp))

        Text(
            text = description,
            fontSize = 13.5.sp,
            color = colors.textMuted,
            lineHeight = 20.sp,
            fontFamily = InterFontFamily
        )

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(
            color = if (colors.isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA),
            thickness = 0.5.dp
        )
    }
}
