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
                        // Luminous Brand Squircle Emblem
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .shadow(
                                    elevation = 12.dp,
                                    shape = RoundedCornerShape(18.dp),
                                    ambientColor = Color(0x332563EB),
                                    spotColor = Color(0x4D2563EB)
                                )
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF38BDF8), Color(0xFF2563EB), Color(0xFF4F46E5))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.Sparkles,
                                contentDescription = "Chroma Studio",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        Text(
                            text = buildAnnotatedString {
                                append("Chroma ")
                                withStyle(
                                    SpanStyle(
                                        brush = Brush.horizontalGradient(
                                            listOf(Color(0xFF1A6DFF), Color(0xFF115AE8), Color(0xFF266EF9))
                                        )
                                    )
                                ) {
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

                        Spacer(Modifier.height(16.dp))

                        // ── 2. FROSTED WHITE COMPACT FEATURE CARD ────────────────────
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(
                                    elevation = 10.dp,
                                    shape = RoundedCornerShape(26.dp),
                                    ambientColor = Color(0x1F173E94),
                                    spotColor = Color(0x28173E94)
                                )
                                .clip(RoundedCornerShape(26.dp))
                                .background(
                                    if (isDark) Color(0xFF111726).copy(alpha = 0.88f)
                                    else Color.White.copy(alpha = 0.85f)
                                )
                                .border(
                                    1.2.dp,
                                    if (isDark) Color.White.copy(alpha = 0.12f) else Color.White,
                                    RoundedCornerShape(26.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Column {
                                // Row 1: Creative Canvas
                                ChromaFeatureRow(
                                    title = "Creative Canvas",
                                    subtitle = "Multi-layer studio with real-time shader rendering",
                                    iconBgBrush = Brush.linearGradient(
                                        if (isDark) listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF1E3A8A))
                                        else listOf(Color(0xFFF0F8FF), Color(0xFFCBE7FF), Color(0xFFB4DBFF))
                                    ),
                                    iconVector = Lucide.Palette,
                                    iconTint = Color(0xFF1267F6),
                                    isDark = isDark,
                                    onClick = { activeDetailSheet = WelcomeFeatureDetailType.CANVAS }
                                )

                                // Soft Gradient Divider
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp)
                                        .height(1.dp)
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    Color.Transparent,
                                                    if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                )

                                // Row 2: Global Community
                                ChromaFeatureRow(
                                    title = "Global Community",
                                    subtitle = "Explore trending masterworks, video drops & remixes",
                                    iconBgBrush = Brush.linearGradient(
                                        if (isDark) listOf(Color(0xFF281F3E), Color(0xFF1C132E), Color(0xFF3B1E8A))
                                        else listOf(Color(0xFFF7F3FF), Color(0xFFE3D7FF), Color(0xFFD3C2FF))
                                    ),
                                    iconVector = Lucide.Users,
                                    iconTint = Color(0xFF6336F5),
                                    isDark = isDark,
                                    onClick = { activeDetailSheet = WelcomeFeatureDetailType.COMMUNITY }
                                )

                                // Soft Gradient Divider
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp)
                                        .height(1.dp)
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    Color.Transparent,
                                                    if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                )

                                // Row 3: Encrypted Messaging
                                ChromaFeatureRow(
                                    title = "Encrypted Messaging",
                                    subtitle = "End-to-end encrypted direct chats, audio & media",
                                    iconBgBrush = Brush.linearGradient(
                                        if (isDark) listOf(Color(0xFF102820), Color(0xFF091A14), Color(0xFF0D3B24))
                                        else listOf(Color(0xFFEEFCF6), Color(0xFFC7F5E2), Color(0xFFB0EED5))
                                    ),
                                    iconVector = Lucide.ShieldCheck,
                                    iconTint = Color(0xFF0EB658),
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
 * Fluid Silk Wave Canvas Background with subtle breathing motion.
 * Matches SVG waves from chroma_studio_onboarding_ui.html.
 */
@Composable
private fun ChromaSilkWaveBackground(isDark: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "silkTransition")
    val shiftX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shiftX"
    )
    val shiftY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shiftY"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Base gradient
        val baseGradient = Brush.verticalGradient(
            colors = if (isDark) {
                listOf(Color(0xFF070B14), Color(0xFF0B1324), Color(0xFF090E1A))
            } else {
                listOf(Color(0xFFD7EAFF), Color(0xFFF3F7FF), Color(0xFFEDF2FF))
            }
        )
        drawRect(brush = baseGradient)

        // Top-Left Sweeping Silk Wave 1
        val wave1Path = Path().apply {
            moveTo(-80f + shiftX, -40f + shiftY)
            lineTo(w * 0.62f + shiftX, -40f + shiftY)
            cubicTo(
                w * 0.48f + shiftX, h * 0.18f + shiftY,
                w * 0.26f + shiftX, h * 0.25f + shiftY,
                -40f + shiftX, h * 0.48f + shiftY
            )
            close()
        }
        val topWave1Brush = Brush.linearGradient(
            colors = if (isDark) {
                listOf(Color(0xFF1E3A8A).copy(alpha = 0.45f), Color(0xFF3B82F6).copy(alpha = 0.15f), Color.Transparent)
            } else {
                listOf(Color(0xFF68B0FF).copy(alpha = 0.72f), Color(0xFFA6D1FF).copy(alpha = 0.42f), Color.Transparent)
            },
            start = Offset(0f, 0f),
            end = Offset(w * 0.6f, h * 0.45f)
        )
        drawPath(wave1Path, brush = topWave1Brush)

        // Top-Left Sweeping Silk Wave 2
        val wave2Path = Path().apply {
            moveTo(-60f + shiftX, -40f + shiftY)
            lineTo(w * 0.82f + shiftX, -40f + shiftY)
            cubicTo(
                w * 0.62f + shiftX, h * 0.22f + shiftY,
                w * 0.32f + shiftX, h * 0.32f + shiftY,
                -60f + shiftX, h * 0.62f + shiftY
            )
            close()
        }
        val topWave2Brush = Brush.linearGradient(
            colors = if (isDark) {
                listOf(Color(0xFF2563EB).copy(alpha = 0.25f), Color(0xFF1D4ED8).copy(alpha = 0.10f), Color.Transparent)
            } else {
                listOf(Color(0xFF89C3FF).copy(alpha = 0.52f), Color(0xFFCBE3FF).copy(alpha = 0.32f), Color.Transparent)
            },
            start = Offset(w * 0.1f, 0f),
            end = Offset(w * 0.7f, h * 0.55f)
        )
        drawPath(wave2Path, brush = topWave2Brush)

        // Ambient Center-Right Soft Orb
        drawCircle(
            brush = Brush.radialGradient(
                colors = if (isDark) {
                    listOf(Color(0xFF3B82F6).copy(alpha = 0.22f), Color.Transparent)
                } else {
                    listOf(Color(0xFFB5CEFF).copy(alpha = 0.45f), Color.Transparent)
                },
                center = Offset(w * 0.95f, h * 0.28f),
                radius = w * 0.55f
            ),
            radius = w * 0.55f,
            center = Offset(w * 0.95f, h * 0.28f)
        )

        // Ambient Center-Left Soft Glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = if (isDark) {
                    listOf(Color(0xFF1D4ED8).copy(alpha = 0.18f), Color.Transparent)
                } else {
                    listOf(Color(0xFFBCE3FF).copy(alpha = 0.40f), Color.Transparent)
                },
                center = Offset(w * 0.05f, h * 0.46f),
                radius = w * 0.45f
            ),
            radius = w * 0.45f,
            center = Offset(w * 0.05f, h * 0.46f)
        )

        // Bottom-Right Corner Waves
        val bottomWave1 = Path().apply {
            moveTo(w, h * 0.72f - shiftY)
            cubicTo(
                w * 0.75f - shiftX, h * 0.76f - shiftY,
                w * 0.50f - shiftX, h * 0.88f - shiftY,
                w * 0.35f - shiftX, h
            )
            lineTo(w, h)
            close()
        }
        val bottomWave1Brush = Brush.linearGradient(
            colors = if (isDark) {
                listOf(Color(0xFF1E1B4B).copy(alpha = 0.4f), Color(0xFF3730A3).copy(alpha = 0.55f), Color(0xFF4338CA).copy(alpha = 0.75f))
            } else {
                listOf(Color(0xFFDCE8FF).copy(alpha = 0.45f), Color(0xFF96B8FF).copy(alpha = 0.58f), Color(0xFF6989FA).copy(alpha = 0.82f))
            },
            start = Offset(w * 0.45f, h * 0.75f),
            end = Offset(w, h)
        )
        drawPath(bottomWave1, brush = bottomWave1Brush)

        val bottomWave2 = Path().apply {
            moveTo(w, h * 0.84f - shiftY)
            cubicTo(
                w * 0.80f - shiftX, h * 0.86f - shiftY,
                w * 0.62f - shiftX, h * 0.92f - shiftY,
                w * 0.50f - shiftX, h
            )
            lineTo(w, h)
            close()
        }
        val bottomWave2Brush = Brush.linearGradient(
            colors = if (isDark) {
                listOf(Color(0xFF312E81).copy(alpha = 0.4f), Color(0xFF4F46E5).copy(alpha = 0.75f))
            } else {
                listOf(Color(0xFF8BA8FF).copy(alpha = 0.48f), Color(0xFF7F66FF).copy(alpha = 0.82f))
            },
            start = Offset(w * 0.55f, h * 0.85f),
            end = Offset(w, h)
        )
        drawPath(bottomWave2, brush = bottomWave2Brush)
    }
}

/**
 * Feature Row matching HTML with 3D squircle icon and circular right chevron button.
 */
@Composable
private fun ChromaFeatureRow(
    title: String,
    subtitle: String,
    iconBgBrush: Brush,
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
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Pastel 3D Squircle Icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(14.dp),
                    ambientColor = iconTint.copy(alpha = 0.2f),
                    spotColor = iconTint.copy(alpha = 0.3f)
                )
                .clip(RoundedCornerShape(14.dp))
                .background(iconBgBrush)
                .border(
                    1.dp,
                    if (isDark) Color.White.copy(alpha = 0.16f) else Color.White,
                    RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }

        // Text Content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0C1425),
                letterSpacing = (-0.3).sp,
                fontFamily = InterFontFamily
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = subtitle,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Normal,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF566479),
                lineHeight = 16.sp,
                fontFamily = InterFontFamily
            )
        }

        // Right Circular Chevron Button
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0x1FFFFFFF) else Color(0xFFE5EEF9)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Lucide.ChevronRight,
                contentDescription = "Details",
                tint = if (isDark) Color(0xFFCBD5E1) else Color(0xFF1E293B),
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

/**
 * Primary Google Blue Glossy Pill Button.
 * Clean, centered, and modern executive action button.
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
            .height(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(999.dp),
                ambientColor = Color(0x441158EB),
                spotColor = Color(0x601158EB)
            )
            .clip(RoundedCornerShape(999.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
                )
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = !loading,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 14.dp),
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
                        .background(Color.White)
                        .shadow(2.dp, CircleShape),
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
 * Secondary Frosted Glass Pill Button.
 * Clean, centered, and modern executive action button.
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
            .height(50.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(999.dp),
                ambientColor = Color(0x101E40AF),
                spotColor = Color(0x181E40AF)
            )
            .clip(RoundedCornerShape(999.dp))
            .background(
                Brush.verticalGradient(
                    if (isDark) {
                        listOf(Color(0xFF1D2638).copy(alpha = 0.95f), Color(0xFF141C2B).copy(alpha = 0.88f))
                    } else {
                        listOf(Color.White.copy(alpha = 0.96f), Color(0xFFF0F6FF).copy(alpha = 0.86f))
                    }
                )
            )
            .border(
                1.2.dp,
                if (isDark) Color.White.copy(alpha = 0.12f) else Color.White,
                RoundedCornerShape(999.dp)
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = Color(0xFF2563EB),
                modifier = Modifier.size(19.dp)
            )

            Text(
                text = text,
                color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0D1526),
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
            .shadow(10.dp, CircleShape, ambientColor = Color(0x352563EB), spotColor = Color(0x552563EB))
            .clip(CircleShape)
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF0381FE), Color(0xFF1667F2))
                )
            )
            .border(1.dp, Color(0x8072BEFF), CircleShape)
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

        // Live preview banner matching HTML preview block
        when (detailType) {
            WelcomeFeatureDetailType.CANVAS -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF))
                        .border(1.dp, Color(0xFF3B82F6).copy(alpha = 0.25f), RoundedCornerShape(16.dp))
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
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3B82F6))
                        )
                        Text(
                            text = "Live Shader Pipeline Ready",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1267F6))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("120 FPS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            WelcomeFeatureDetailType.COMMUNITY -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Color(0xFF281F3E) else Color(0xFFF5F3FF))
                        .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Global Discovery Feed",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF6336F5))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("+1,420 remixes today", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            WelcomeFeatureDetailType.ENCRYPTION -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Color(0xFF102820) else Color(0xFFECFDF5))
                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Device Keys Verified on Enclave",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF065F46)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0EB658))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("E2EE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
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

        // Glossy Continue button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF329EFF), Color(0xFF1667F2), Color(0xFF0D51D4))
                    )
                )
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
