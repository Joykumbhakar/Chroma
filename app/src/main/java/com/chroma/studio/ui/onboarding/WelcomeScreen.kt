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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.chroma.studio.ui.components.OscellionAppIcon
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Palette
import com.composables.icons.lucide.ShieldCheck
import com.composables.icons.lucide.Sparkles

val TheSignatureFont = FontFamily(Font(R.font.thesignature))

/**
 * Recreated Apple-inspired Welcome / First-Time Login Screen for Chroma Studio.
 *
 * Design Architecture:
 * - High signal-to-noise ratio: clean, tightened, Apple-standard Cupertino layout.
 * - Deep obsidian canvas with hardware-accelerated sapphire ambient mesh (zero per-frame CPU load).
 * - Apple HIG typography, milled 3D app icon, and concise value proposition showcase.
 * - Single-tap Google and Apple OAuth pills, quick resume for saved accounts, and frictionless guest entry.
 * - Seamless email sign-in / registration links with legal terms attribution.
 */
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030510))
    ) {
        // Hardware-accelerated GPU ambient glows (zero CPU overhead)
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Top ambient sapphire bloom behind app icon
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1D4ED8).copy(alpha = 0.28f),
                        Color(0xFF0F172A).copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.16f),
                    radius = size.width * 0.85f
                )
            )
            // Bottom soft ambient glow behind action buttons
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1E293B).copy(alpha = 0.22f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.94f),
                    radius = size.width * 0.75f
                )
            )
        }

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
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = screenHeight - 24.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ── TOP & HERO SECTION ──────────────────────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 3D Milled App Icon with subtle halo
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(92.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(86.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2563EB).copy(alpha = 0.16f))
                            )
                            OscellionAppIcon(size = 72.dp)
                        }

                        Spacer(Modifier.height(18.dp))

                        Text(
                            text = "Chroma Studio",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = (-0.8).sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = "Where creativity meets community.",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF94A3B8),
                            letterSpacing = (-0.2).sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )

                        Spacer(Modifier.height(28.dp))

                        // ── TIGHTENED VALUE PROPOSITION CARDS ────────────────
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0x0CFFFFFF))
                                .border(BorderStroke(1.dp, Color(0x18FFFFFF)), RoundedCornerShape(20.dp))
                                .padding(horizontal = 18.dp, vertical = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            WelcomeFeatureRow(
                                icon = Lucide.Palette,
                                iconColor = Color(0xFF60A5FA),
                                title = "Creative Canvas",
                                description = "Multi-layer studio with real-time shader rendering"
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.6.dp)
                                    .background(Color(0x14FFFFFF))
                            )
                            WelcomeFeatureRow(
                                icon = Lucide.Globe,
                                iconColor = Color(0xFF38BDF8),
                                title = "Global Community",
                                description = "Explore trending masterworks, video drops & remixes"
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.6.dp)
                                    .background(Color(0x14FFFFFF))
                            )
                            WelcomeFeatureRow(
                                icon = Lucide.ShieldCheck,
                                iconColor = Color(0xFF4ADE80),
                                title = "Encrypted Messaging",
                                description = "End-to-end encrypted direct chats, audio & media"
                            )
                        }
                    }

                    // ── BOTTOM ACTIONS SECTION ──────────────────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp, bottom = 8.dp),
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

                        // Primary OAuth: Continue with Google
                        WelcomePrimaryPillButton(
                            text = "Continue with Google",
                            onClick = onGoogle,
                            loading = googleLoading,
                            leading = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_google_logo),
                                    contentDescription = "Google logo",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        )

                        // Secondary OAuth: Continue with Apple
                        WelcomeSecondaryPillButton(
                            text = "Continue with Apple",
                            onClick = onApple,
                            loading = appleLoading,
                            leading = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_apple_logo),
                                    contentDescription = "Apple logo",
                                    tint = Color.White,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        )

                        // Frictionless Guest Exploration
                        WelcomeTertiaryPillButton(
                            text = "Explore Studio as Guest",
                            onClick = onExploreGuest,
                            leading = {
                                Icon(
                                    imageVector = Lucide.Sparkles,
                                    contentDescription = "Guest explore",
                                    tint = Color(0xFF93C5FD),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        )

                        Spacer(Modifier.height(4.dp))

                        // Clean inline navigation for Email sign-in / registration
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sign in with Email",
                                color = Color(0xFF93C5FD),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.2).sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(role = Role.Button, onClick = onSignInExisting)
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                            Text(
                                text = "•",
                                color = Color(0x66FFFFFF),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                            Text(
                                text = "Create Account",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.2).sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(role = Role.Button, onClick = onCreateAccount)
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }

                        Spacer(Modifier.height(2.dp))

                        // Terms & Privacy attribution
                        Text(
                            text = buildAnnotatedString {
                                append("By continuing, you agree to our ")
                                withStyle(SpanStyle(color = Color(0xFF93C5FD), fontWeight = FontWeight.SemiBold)) {
                                    append("Terms and Privacy Policy")
                                }
                            },
                            color = Color(0x80BAC7E8),
                            fontSize = 11.5.sp,
                            letterSpacing = (-0.2).sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(role = Role.Button, onClick = onTerms)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Clean Cupertino feature highlight row.
 */
@Composable
private fun WelcomeFeatureRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0x14FFFFFF))
                .border(BorderStroke(1.dp, Color(0x1FFFFFFF)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(19.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                letterSpacing = (-0.3).sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF94A3B8),
                letterSpacing = (-0.2).sp,
                lineHeight = 17.sp
            )
        }
    }
}

/**
 * Apple-standard White Primary Pill Button.
 */
@Composable
private fun WelcomePrimaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    height: Dp = 50.dp
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "btn_press"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(if (enabled) 6.dp else 0.dp, CircleShape, ambientColor = Color(0x40000000), spotColor = Color(0x60000000))
            .clip(CircleShape)
            .background(Color.White)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled && !loading,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = Color(0xFF0F172A),
                strokeWidth = 2.dp
            )
        } else {
            leading?.let {
                it()
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = text,
                color = Color(0xFF0F172A),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp
            )
        }
    }
}

/**
 * Cupertino Frosted Dark Secondary Pill Button (Apple HIG).
 */
@Composable
private fun WelcomeSecondaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    height: Dp = 50.dp
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "apple_press"
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
            .background(Color(0xFF141824))
            .border(BorderStroke(1.dp, Color(0x2EFFFFFF)), CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled && !loading,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            leading?.let {
                it()
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = text,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp
            )
        }
    }
}

/**
 * Cupertino Frosted Glass Tertiary Pill Button (Guest / Explore).
 */
@Composable
private fun WelcomeTertiaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    height: Dp = 46.dp
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "guest_press"
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
            .background(Color(0x12FFFFFF))
            .border(BorderStroke(1.dp, Color(0x1EFFFFFF)), CircleShape)
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
        leading?.let {
            it()
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            color = Color(0xFFE2E8F0),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.2).sp
        )
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
    height: Dp = 50.dp
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
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
            .shadow(8.dp, CircleShape, ambientColor = Color(0x302563EB), spotColor = Color(0x502563EB))
            .clip(CircleShape)
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
                )
            )
            .border(BorderStroke(1.dp, Color(0x4DFFFFFF)), CircleShape)
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
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Continue as $accountName",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp
        )
    }
}

/**
 * Pixel-perfect clean artwork matching the reference illustration.
 * Preserved for backwards compatibility with existing callers.
 */
@Composable
fun SunsetArtwork(modifier: Modifier = Modifier) {
    Box(modifier) {
        Image(
            painter = painterResource(R.drawable.welcome_illustration),
            contentDescription = "Oscellion Welcome Artwork",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                Brush.verticalGradient(
                    0.0f to Color(0xB3020518),
                    0.32f to Color.Transparent
                )
            )
            drawRect(
                Brush.verticalGradient(
                    0.58f to Color.Transparent,
                    0.78f to Color(0xCC040722),
                    1.0f to Color(0xF2040722)
                )
            )
        }
    }
}
