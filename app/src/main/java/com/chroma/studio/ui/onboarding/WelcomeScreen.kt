package com.chroma.studio.ui.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.R
import com.chroma.studio.ui.theme.InterFontFamily
import com.composables.icons.lucide.*
import kotlinx.coroutines.launch

val TheSignatureFont = FontFamily(Font(R.font.thesignature))

enum class WelcomeFeatureDetailType {
    CANVAS,
    COMMUNITY,
    ENCRYPTION
}

/**
 * Recreated Apple-grade Welcome / Onboarding Screen for Chroma Studio.
 * Faithfully matches the exact design from chroma_studio_onboarding_ui.html:
 * - Fluid silk wave gradient background with animated breathing curves
 * - Crisp "Chroma Studio" gradient typography
 * - Frosted white 32dp feature card with 3D pastel squircle icons:
 *     1. Creative Canvas (Pastel Blue Squircle with Palette)
 *     2. Global Community (Pastel Lavender Squircle with Community)
 *     3. Encrypted Messaging (Pastel Mint Squircle with Emerald Shield)
 * - Symmetrically aligned glossy pill action buttons:
 *     1. Continue with Google (Glossy Google Blue Gradient + White Badge + Right Translucent Chevron)
 *     2. Create a new account (Frosted White Glass + Blue UserPlus Icon + Chevron)
 *     3. Explore Studio as Guest (Frosted White Glass + Blue Sparkles Cluster + Chevron)
 * - Sign in with Email • Create Account footer links & legal terms
 * - Interactive feature detail modal sheets with live status preview cards
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    savedAccountName: String?,
    googleLoading: Boolean,
    appleLoading: Boolean = false,
    onContinueSaved: () -> Unit,
    onGoogle: () -> Unit,
    onApple: () -> Unit = {},
    onCreateAccount: () -> Unit,
    onSignInExisting: () -> Unit = {},
    onExploreGuest: () -> Unit = {},
    onTerms: () -> Unit
) {
    val colors = com.chroma.studio.ui.theme.LocalOscellionColors.current
    val isDark = colors.isDark

    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0A1122)
    val textMuted = if (isDark) Color(0xFF94A3B8) else Color(0xFF516079)

    var activeDetailSheet by remember { mutableStateOf<WelcomeFeatureDetailType?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // ── FLUID SILK WAVE BACKGROUND ──────────────────────────────────────────
        ChromaSilkWaveBackground(isDark = isDark)

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            val screenHeight = maxHeight
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = screenHeight - 20.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ── 1. BESPOKE BRAND EMBLEM & CLEAN TITLE HEADER ─────────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Clean One UI Brand Squircle Emblem
                        val oneUiBlue = Color(0xFF0381FE)
                        val iconContainerBg = if (isDark) oneUiBlue.copy(alpha = 0.18f) else oneUiBlue.copy(alpha = 0.10f)
                        val dividerColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFECEFF4)
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(oneUiBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.Sparkles,
                                contentDescription = "Chroma Studio",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = buildAnnotatedString {
                                append("Chroma ")
                                withStyle(SpanStyle(color = oneUiBlue)) {
                                    append("Studio")
                                }
                            },
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textPrimary,
                            letterSpacing = (-0.6).sp,
                            textAlign = TextAlign.Center,
                            fontFamily = InterFontFamily
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = "Where creativity meets community.",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = textMuted,
                            letterSpacing = (-0.2).sp,
                            textAlign = TextAlign.Center,
                            fontFamily = InterFontFamily
                        )

                        Spacer(Modifier.height(20.dp))

                        // ── 2. CLEAN ONE UI FOCUS BLOCK FEATURE CARD ─────────────────
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(26.dp))
                                .background(if (isDark) Color(0xFF17171A) else Color.White)
                                .border(
                                    1.dp,
                                    if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE5E7EB),
                                    RoundedCornerShape(26.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Column {
                                // Row 1: Creative Canvas
                                ChromaFeatureRow(
                                    title = "Creative Canvas",
                                    subtitle = "Multi-layer studio with real-time shader rendering",
                                    iconBgColor = iconContainerBg,
                                    iconVector = Lucide.Palette,
                                    iconTint = oneUiBlue,
                                    isDark = isDark,
                                    onClick = { activeDetailSheet = WelcomeFeatureDetailType.CANVAS }
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .height(1.dp)
                                        .background(dividerColor)
                                )

                                // Row 2: Global Community
                                ChromaFeatureRow(
                                    title = "Global Community",
                                    subtitle = "Explore trending masterworks, video drops & remixes",
                                    iconBgColor = iconContainerBg,
                                    iconVector = Lucide.Users,
                                    iconTint = oneUiBlue,
                                    isDark = isDark,
                                    onClick = { activeDetailSheet = WelcomeFeatureDetailType.COMMUNITY }
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .height(1.dp)
                                        .background(dividerColor)
                                )

                                // Row 3: Encrypted Messaging
                                ChromaFeatureRow(
                                    title = "Encrypted Messaging",
                                    subtitle = "End-to-end encrypted direct chats, audio & media",
                                    iconBgColor = iconContainerBg,
                                    iconVector = Lucide.ShieldCheck,
                                    iconTint = oneUiBlue,
                                    isDark = isDark,
                                    onClick = { activeDetailSheet = WelcomeFeatureDetailType.ENCRYPTION }
                                )
                            }
                        }
                    }

                    // ── 3. CLEAN & PROFESSIONAL ACTION BUTTONS ───────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Optional: Saved Account Quick Login
                        if (!savedAccountName.isNullOrBlank()) {
                            WelcomeQuickResumePill(
                                accountName = savedAccountName,
                                onClick = onContinueSaved
                            )
                        }

                        // Button 1: Continue with Google (Primary - Clean & Centered)
                        GlossyGooglePillButton(
                            text = "Continue with Google",
                            loading = googleLoading,
                            onClick = onGoogle
                        )

                        // Button 2: Sign in with Email (Secondary - Frosted Glass Pill)
                        GlossySecondaryPillButton(
                            text = "Sign in with Email",
                            iconVector = Lucide.Mail,
                            isDark = isDark,
                            onClick = onSignInExisting
                        )

                        // Subtle Divider with "or"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(0.8.dp)
                                    .background(if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFE2E8F0))
                            )
                            Text(
                                text = "or",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = textMuted
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(0.8.dp)
                                    .background(if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFE2E8F0))
                            )
                        }

                        // Button 3: Create a new account
                        GlossySecondaryPillButton(
                            text = "Create a new account",
                            iconVector = Lucide.UserPlus,
                            isDark = isDark,
                            onClick = onCreateAccount
                        )

                        // Subtle "Explore Studio as Guest"
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable(role = Role.Button, onClick = onExploreGuest)
                                .padding(horizontal = 16.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.Sparkles,
                                contentDescription = null,
                                tint = textMuted,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Explore Studio as Guest",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textMuted,
                                letterSpacing = (-0.2).sp
                            )
                        }

                        Spacer(Modifier.height(2.dp))

                        // Terms and Privacy Policy Footer
                        Text(
                            text = buildAnnotatedString {
                                append("By continuing, you agree to our ")
                                withStyle(SpanStyle(color = Color(0xFF0B63F6), fontWeight = FontWeight.SemiBold)) {
                                    append("Terms")
                                }
                                append(" and ")
                                withStyle(SpanStyle(color = Color(0xFF0B63F6), fontWeight = FontWeight.SemiBold)) {
                                    append("Privacy Policy")
                                }
                            },
                            color = textMuted,
                            fontSize = 12.sp,
                            letterSpacing = (-0.2).sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(role = Role.Button, onClick = onTerms)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }

    // ── FEATURE DETAILS INTERACTIVE MODAL SHEET ─────────────────────────────
    if (activeDetailSheet != null) {
        val detail = activeDetailSheet!!
        ModalBottomSheet(
            onDismissRequest = { activeDetailSheet = null },
            sheetState = sheetState,
            containerColor = if (isDark) Color(0xFF131B2C) else Color(0xFFF9FBFF),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(42.dp)
                        .height(4.5.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1))
                )
            }
        ) {
            WelcomeFeatureDetailSheetContent(
                detailType = detail,
                isDark = isDark,
                onClose = { activeDetailSheet = null }
            )
        }
    }
}

/**
 * Clean One UI solid neutral surface background — no distracting gradients or blobs.
 */
@Composable
private fun ChromaSilkWaveBackground(isDark: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF080808) else Color(0xFFF7F8FA))
    )
}

/**
 * Clean One UI Feature Row with unified icon tile and subtle chevron.
 */
@Composable
private fun ChromaFeatureRow(
    title: String,
    subtitle: String,
    iconBgColor: Color,
    iconVector: ImageVector,
    iconTint: Color,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
        label = "feature_row_press"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Unified One UI Squircle Icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(21.dp)
            )
        }

        // Text Content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF111827),
                letterSpacing = (-0.2).sp,
                fontFamily = InterFontFamily
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Normal,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                lineHeight = 16.5.sp,
                fontFamily = InterFontFamily
            )
        }

        // Right Circular Chevron Button
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Lucide.ChevronRight,
                contentDescription = "Details",
                tint = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

/**
 * Primary Solid One UI Blue Pill Button.
 */
@Composable
private fun GlossyGooglePillButton(
    text: String,
    loading: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 450f),
        label = "google_press"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFF0381FE))
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = !loading,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_google_logo),
                        contentDescription = "Google",
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp,
                    fontFamily = InterFontFamily
                )
            }
        }
    }
}

/**
 * Secondary Clean One UI Surface Pill Button.
 */
@Composable
private fun GlossySecondaryPillButton(
    text: String,
    iconVector: ImageVector,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 450f),
        label = "sec_press"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(999.dp))
            .background(if (isDark) Color(0xFF17171A) else Color.White)
            .border(
                1.dp,
                if (isDark) Color.White.copy(alpha = 0.10f) else Color(0xFFE5E7EB),
                RoundedCornerShape(999.dp)
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = Color(0xFF0381FE),
                modifier = Modifier.size(19.dp)
            )

            Text(
                text = text,
                color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF111827),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp,
                fontFamily = InterFontFamily
            )
        }
    }
}

/**
 * High-prominence quick-resume pill for saved accounts.
 */
@Composable
private fun WelcomeQuickResumePill(
    accountName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 54.dp
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 450f),
        label = "resume_press"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(Color(0xFF0381FE))
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.AccountCircle,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Continue as $accountName",
            color = Color.White,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp,
            fontFamily = InterFontFamily
        )
    }
}

/**
 * Feature Details Modal Bottom Sheet Content matching the script data in HTML.
 */
@Composable
private fun WelcomeFeatureDetailSheetContent(
    detailType: WelcomeFeatureDetailType,
    isDark: Boolean,
    onClose: () -> Unit
) {
    val title = when (detailType) {
        WelcomeFeatureDetailType.CANVAS -> "Creative Canvas"
        WelcomeFeatureDetailType.COMMUNITY -> "Global Community"
        WelcomeFeatureDetailType.ENCRYPTION -> "Encrypted Messaging"
    }
    val subtitle = when (detailType) {
        WelcomeFeatureDetailType.CANVAS -> "120Hz Real-Time Shader & Layer Engine"
        WelcomeFeatureDetailType.COMMUNITY -> "Masterworks, Video Drops & Remixes"
        WelcomeFeatureDetailType.ENCRYPTION -> "Zero-Knowledge Direct Collaboration"
    }
    val badge = when (detailType) {
        WelcomeFeatureDetailType.CANVAS -> "GLSL + Metal/WebGPU"
        WelcomeFeatureDetailType.COMMUNITY -> "24.8k Active Creators"
        WelcomeFeatureDetailType.ENCRYPTION -> "X25519 + AES-256-GCM"
    }
    val body = when (detailType) {
        WelcomeFeatureDetailType.CANVAS -> "Combine node-based procedural shaders, vector brushes, and non-destructive blend modes with zero latency."
        WelcomeFeatureDetailType.COMMUNITY -> "Fork any open project on the discovery feed, inspect full layer trees, and publish your own interactive shader drops."
        WelcomeFeatureDetailType.ENCRYPTION -> "Share uncompressed project files, voice notes, and direct messages protected by audited end-to-end encryption."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Row: Title + Badge + Close button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0D1526),
                    letterSpacing = (-0.4).sp,
                    fontFamily = InterFontFamily
                )
                Text(
                    text = subtitle,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0B63F6),
                    fontFamily = InterFontFamily
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFF0B63F6).copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badge,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0B63F6)
                )
            }
        }

        // Clean One UI preview banner
        val oneUiBlue = Color(0xFF0381FE)
        val previewLabel = when (detailType) {
            WelcomeFeatureDetailType.CANVAS -> "Live Shader Pipeline Ready"
            WelcomeFeatureDetailType.COMMUNITY -> "Global Discovery Feed"
            WelcomeFeatureDetailType.ENCRYPTION -> "Device Keys Verified on Enclave"
        }
        val previewBadge = when (detailType) {
            WelcomeFeatureDetailType.CANVAS -> "120 FPS"
            WelcomeFeatureDetailType.COMMUNITY -> "+1,420 remixes today"
            WelcomeFeatureDetailType.ENCRYPTION -> "E2EE"
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isDark) Color(0xFF1E2430) else Color(0xFFEFF6FF))
                .border(1.dp, oneUiBlue.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(oneUiBlue)
                )
                Text(
                    text = previewLabel,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(oneUiBlue)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(previewBadge, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Body description
        Text(
            text = body,
            fontSize = 14.sp,
            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF5B687C),
            lineHeight = 20.sp,
            fontFamily = InterFontFamily
        )

        Spacer(Modifier.height(4.dp))

        // Solid One UI Continue button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(oneUiBlue)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Continue",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp
            )
        }
    }
}
