package com.chroma.studio.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.R
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.ui.components.LiquidGlassCheckbox
import com.chroma.studio.ui.components.OneUiCollapsingHeaderLayout
import com.chroma.studio.ui.theme.InterFontFamily
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*

/**
 * Official Privacy Policy Acceptance Screen.
 * Exact match to the mobile design specification:
 * - Status bar integration & circular back button
 * - Hero Shield with Padlock and flowing satin ribbons (R.drawable.hero_privacy_shield)
 * - "Your privacy. Your choice." typography
 * - "At a glance" grouped card container
 * - "Explore the policy" 9-row navigation card container
 * - Official LiquidGlassCheckbox in PURE WHITE for acceptance
 * - "Manage your privacy settings" / "Accept & Continue" pill button
 * - "Read full policy →" expandable review sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyTermsScreen(
    isFirstLaunch: Boolean = true,
    onAccept: () -> Unit,
    onDecline: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val colors = LocalOscellionColors.current
    var hasAgreed by remember { mutableStateOf(!isFirstLaunch) }
    var selectedSectionDetail by remember { mutableStateOf<PolicySectionItem?>(null) }
    var showFullPolicySheet by remember { mutableStateOf(false) }

    val policyItems = remember {
        listOf(
            PolicySectionItem(
                title = "Information we collect",
                icon = Lucide.User,
                summary = "We collect information you provide directly, such as your profile credentials, creations, and communications. We also automatically gather minimal diagnostic data to guarantee seamless service stability without tracking across third-party apps."
            ),
            PolicySectionItem(
                title = "How we use information",
                icon = Lucide.Settings,
                summary = "Your data powers real-time creative tools, direct messaging, end-to-end encrypted audio/video calling, and community discovery. We never sell your personal data or creative assets to advertisers."
            ),
            PolicySectionItem(
                title = "Sharing and third parties",
                icon = Lucide.Share2,
                summary = "We only share data with essential infrastructure providers (such as Google Cloud Firestore and secure media caching pipelines) required to operate the service. You have total control over who sees your shared stories and artworks."
            ),
            PolicySectionItem(
                title = "Cookies and analytics",
                icon = Lucide.Cookie,
                summary = "We utilize strictly necessary local session storage to maintain authentication and user preferences. No cross-site marketing cookies or tracking beacons are deployed within Oscellion."
            ),
            PolicySectionItem(
                title = "Data retention and security",
                icon = Lucide.ShieldCheck,
                summary = "All client-to-server traffic is protected using industry-grade TLS 1.3 encryption. Chat communications and voice notes benefit from client-side cryptographic isolation. Your data is retained only as long as your account remains active."
            ),
            PolicySectionItem(
                title = "Your privacy rights",
                icon = Lucide.Scale,
                summary = "You possess full sovereignty over your identity: access, correct, export, or permanently delete your entire profile, uploaded media, and message logs at any moment directly from Account Settings."
            ),
            PolicySectionItem(
                title = "Children’s privacy",
                icon = Lucide.Users,
                summary = "Oscellion is not directed at children under the age of 13. We do not knowingly collect personal information from underage users without verifiable parental or guardian consent."
            ),
            PolicySectionItem(
                title = "Changes to this policy",
                icon = Lucide.RotateCw,
                summary = "We may update this policy to reflect new capabilities or regulatory obligations. When meaningful updates occur, you will receive prominent in-app notification before they become effective."
            ),
            PolicySectionItem(
                title = "Contact us",
                icon = Lucide.Mail,
                summary = "For any questions, rights requests, or data privacy inquiries, you can reach our dedicated data protection team directly at privacy@oscellion.com or through our in-app support portal."
            )
        )
    }

    OneUiCollapsingHeaderLayout(
        title = "Privacy & Terms",
        subtitle = "Last updated: Nov 23, 2026",
        onBack = {
            SoundManager.playReaction()
            onBack()
        },
        initialCollapsed = false,
        headerActions = { compactTitleAlpha ->
            Text(
                text = if (isFirstLaunch) (if (hasAgreed) "Accept" else "") else "Done",
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = colors.primary,
                fontFamily = InterFontFamily,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        SoundManager.playReaction()
                        if (isFirstLaunch) {
                            if (hasAgreed) onAccept()
                        } else {
                            onBack()
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
    ) { collapseProgress ->
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
            // ─── 1. HERO SHIELD GRAPHIC ──────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(185.dp)
                ) {
                    Image(
                        painter = painterResource(if (colors.isDark) R.drawable.hero_privacy_shield else R.drawable.hero_privacy_shield_light),
                        contentDescription = "Privacy Shield",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(185.dp)
                    )

                    // Smooth bottom fade into canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(65.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        colors.bg.copy(alpha = 0.5f),
                                        colors.bg
                                    )
                                )
                            )
                    )
                }
            }

            // ─── 2. HEADLINE & INTRO TEXT ─────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Your privacy. Your choice.",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain,
                        textAlign = TextAlign.Center,
                        letterSpacing = (-0.4).sp,
                        fontFamily = InterFontFamily
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "This Privacy Policy describes what information we collect, why we use it, when we share it, and the choices you have.",
                        fontSize = 13.5.sp,
                        color = colors.textMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 19.5.sp,
                        fontFamily = InterFontFamily
                    )
                }

                Spacer(Modifier.height(24.dp))
            }

            // ─── 3. "AT A GLANCE" SECTION ─────────────────────────────
            item {
                Text(
                    text = "At a glance",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain,
                    fontFamily = InterFontFamily,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )

                Spacer(Modifier.height(6.dp))

                // Grouped Card Container
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.cardSurface)
                        .border(1.dp, colors.cardBorder, RoundedCornerShape(20.dp))
                ) {
                    // Item 1: What we collect
                    GlanceRowItem(
                        icon = Lucide.User,
                        title = "What we collect",
                        subtitle = "Basic info, content and usage data."
                    )

                    HorizontalDivider(color = colors.cardBorderSubtle, thickness = 0.8.dp, modifier = Modifier.padding(horizontal = 16.dp))

                    // Item 2: How we use it
                    GlanceRowItem(
                        icon = Lucide.Settings,
                        title = "How we use it",
                        subtitle = "To improve your experience."
                    )

                    HorizontalDivider(color = colors.cardBorderSubtle, thickness = 0.8.dp, modifier = Modifier.padding(horizontal = 16.dp))

                    // Item 3: Your choices
                    GlanceRowItem(
                        icon = Lucide.ShieldCheck,
                        title = "Your choices",
                        subtitle = "Control your data and settings."
                    )
                }

                Spacer(Modifier.height(24.dp))
            }

            // ─── 4. "EXPLORE THE POLICY" SECTION ──────────────────────
            item {
                Text(
                    text = "Explore the policy",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain,
                    fontFamily = InterFontFamily,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )

                Spacer(Modifier.height(6.dp))

                // Grouped Card Container for the 9 Policy Items
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.cardSurface)
                        .border(1.dp, colors.cardBorder, RoundedCornerShape(20.dp))
                ) {
                    policyItems.forEachIndexed { index, item ->
                        ExplorePolicyRowItem(
                            item = item,
                            onClick = { selectedSectionDetail = item }
                        )

                        if (index < policyItems.lastIndex) {
                            HorizontalDivider(
                                color = colors.cardBorderSubtle,
                                thickness = 0.8.dp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }

        // ─── BOTTOM ACCEPTANCE CONTROLS & ACTION BUTTONS ─────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bg.copy(alpha = 0.95f))
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Checkbox Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { hasAgreed = !hasAgreed }
                    )
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiquidGlassCheckbox(
                    checked = hasAgreed,
                    onCheckedChange = { hasAgreed = it },
                    size = 22.dp,
                    checkedColor = colors.primary,
                    uncheckedColor = colors.textMuted
                )

                Spacer(Modifier.width(12.dp))

                Text(
                    text = "I have read and agree to the Privacy Policy",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (hasAgreed) colors.textMain else colors.textMuted,
                    fontFamily = InterFontFamily,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(10.dp))

            // Primary Action: "Manage your privacy settings" / "Accept & Continue"
            val buttonInteraction = remember { MutableInteractionSource() }
            val buttonPressed by buttonInteraction.collectIsPressedAsState()
            val buttonScale by animateFloatAsState(
                targetValue = if (buttonPressed) 0.975f else 1f,
                animationSpec = spring(stiffness = 400f, dampingRatio = 0.7f),
                label = "btn_press"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .graphicsLayer {
                        scaleX = buttonScale
                        scaleY = buttonScale
                    }
                    .clip(CircleShape)
                    .background(
                        if (hasAgreed) colors.primary
                        else colors.cardSurfaceElevated
                    )
                    .border(
                        1.dp,
                        if (hasAgreed) Color.Transparent else colors.cardBorder,
                        CircleShape
                    )
                    .clickable(
                        interactionSource = buttonInteraction,
                        indication = null,
                        onClick = {
                            if (hasAgreed) {
                                onAccept()
                            } else {
                                hasAgreed = true
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Lucide.ShieldCheck,
                        contentDescription = null,
                        tint = if (hasAgreed) colors.onPrimary else colors.textMain,
                        modifier = Modifier.size(19.dp)
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = if (isFirstLaunch) "Accept & Continue" else "Manage your privacy settings",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasAgreed) colors.onPrimary else colors.textMain,
                        fontFamily = InterFontFamily
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Secondary Action: "Read full policy →"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showFullPolicySheet = true }
                    )
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Read full policy →",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textMuted,
                    fontFamily = InterFontFamily
                )
            }
        }
    }
}

    // ─── MODAL DETAIL SHEET FOR INDIVIDUAL POLICY ITEM ─────────────
    selectedSectionDetail?.let { detail ->
        ModalBottomSheet(
            onDismissRequest = { selectedSectionDetail = null },
            containerColor = if (colors.isDark) Color(0xFF121317) else Color.White,
            contentColor = colors.textMain,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(colors.cardBorder)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.cardSurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(detail.icon, contentDescription = null, tint = colors.textMain, modifier = Modifier.size(20.dp))
                    }

                    Spacer(Modifier.width(14.dp))

                    Text(
                        text = detail.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain,
                        fontFamily = InterFontFamily
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = detail.summary,
                    fontSize = 14.5.sp,
                    color = colors.textMuted,
                    lineHeight = 22.sp,
                    fontFamily = InterFontFamily
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = { selectedSectionDetail = null },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                }
            }
        }
    }

    // ─── FULL POLICY MODAL SHEET ──────────────────────────────────
    if (showFullPolicySheet) {
        ModalBottomSheet(
            onDismissRequest = { showFullPolicySheet = false },
            containerColor = if (colors.isDark) Color(0xFF101114) else Color.White,
            contentColor = colors.textMain,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(colors.cardBorder)
                )
            }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentPadding = PaddingValues(bottom = 36.dp)
            ) {
                item {
                    Text(
                        text = "Complete Privacy Policy",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain,
                        fontFamily = InterFontFamily
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Effective Date: Nov 23, 2026",
                        fontSize = 12.sp,
                        color = colors.textMuted,
                        fontFamily = InterFontFamily
                    )
                    Spacer(Modifier.height(18.dp))
                }

                items(policyItems.size) { i ->
                    val item = policyItems[i]
                    Column(modifier = Modifier.padding(vertical = 10.dp)) {
                        Text(
                            text = item.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain,
                            fontFamily = InterFontFamily
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = item.summary,
                            fontSize = 13.5.sp,
                            color = colors.textMuted,
                            lineHeight = 19.sp,
                            fontFamily = InterFontFamily
                        )
                    }
                    if (i < policyItems.lastIndex) {
                        HorizontalDivider(color = colors.cardBorderSubtle, thickness = 0.8.dp, modifier = Modifier.padding(vertical = 6.dp))
                    }
                }

                item {
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { showFullPolicySheet = false },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold, fontFamily = InterFontFamily)
                    }
                }
            }
        }
    }
}

private data class PolicySectionItem(
    val title: String,
    val icon: ImageVector,
    val summary: String
)

@Composable
private fun GlanceRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    val colors = LocalOscellionColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.cardSurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.textMain,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMain,
                fontFamily = InterFontFamily
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.5.sp,
                color = colors.textMuted,
                fontFamily = InterFontFamily
            )
        }
    }
}

@Composable
private fun ExplorePolicyRowItem(
    item: PolicySectionItem,
    onClick: () -> Unit
) {
    val colors = LocalOscellionColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = colors.textMain,
            modifier = Modifier.size(19.dp)
        )

        Spacer(Modifier.width(14.dp))

        Text(
            text = item.title,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textMain,
            fontFamily = InterFontFamily,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Lucide.ChevronRight,
            contentDescription = null,
            tint = colors.textMuted,
            modifier = Modifier.size(17.dp)
        )
    }
}
