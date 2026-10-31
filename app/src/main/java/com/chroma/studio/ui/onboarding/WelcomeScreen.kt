package com.chroma.studio.ui.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.chroma.studio.R
import com.chroma.studio.ui.components.OscellionAppIcon

val TheSignatureFont = FontFamily(Font(R.font.thesignature))

@Composable
fun WelcomeScreen(
    savedAccountName: String?,
    googleLoading: Boolean,
    onContinueSaved: () -> Unit,
    onGoogle: () -> Unit,
    onCreateAccount: () -> Unit,
    onSignInExisting: () -> Unit = {},
    onTerms: () -> Unit
) {
    var showAbout by remember { mutableStateOf(false) }

    // Chromatic animated wave gradient
    val infiniteTransition = rememberInfiniteTransition(label = "waveGradient")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradientPhase"
    )

    val waveColors = listOf(
        Color(0xFF38EF7D), // Vibrant Lime
        Color(0xFF00F2FE), // Electric Cyan
        Color(0xFF4FACFE), // Azure Sky
        Color(0xFF9D4EDD), // Neon Violet
        Color(0xFFFF5E7E), // Radiant Coral
        Color(0xFFFFAA00), // Amber Gold
        Color(0xFF38EF7D)  // Seamless cycle back to Lime
    )

    val animatedWaveBrush = remember(phase) {
        val spread = 700f
        val offset = phase * spread
        Brush.linearGradient(
            colors = waveColors,
            start = Offset(offset, 0f),
            end = Offset(offset + spread, spread * 0.45f),
            tileMode = TileMode.Repeated
        )
    }

    Box(Modifier.fillMaxSize().background(ObColors.Abyss)) {
        SunsetArtwork(Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 12.dp)
        ) {
            // Top Bar
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OscellionAppIcon(size = 36.dp)
                Row(
                    Modifier
                        .clickable(role = androidx.compose.ui.semantics.Role.Button) { showAbout = true }
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = ObColors.Lime, modifier = Modifier.size(16.dp))
                    Text("About", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(20.dp))

            // Hero Section: "Welcome to Oscellion" with animated wave gradient & bold title
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Welcome to",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = (-0.5).sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Oscellion",
                    fontFamily = TheSignatureFont,
                    fontSize = 58.sp,
                    lineHeight = 62.sp,
                    style = TextStyle(brush = animatedWaveBrush)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    buildAnnotatedString {
                        append("Where ")
                        withStyle(SpanStyle(color = ObColors.Lime, fontWeight = FontWeight.Bold)) { append("Creativity") }
                        append(" has a Social Life")
                    },
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.88f),
                    letterSpacing = 0.2.sp
                )
            }

            Spacer(Modifier.weight(1f))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (savedAccountName != null) {
                    ObPrimaryButton(
                        "Continue as $savedAccountName", onContinueSaved,
                        leading = { Icon(Icons.Rounded.AccountCircle, null, tint = Color(0xFF090C22), modifier = Modifier.size(20.dp)) }
                    )
                }
                ObPrimaryButton(
                    text = "Sign in with Google",
                    onClick = onGoogle,
                    loading = googleLoading,
                    trailingIcon = null,
                    leading = {
                        Icon(
                            painter = painterResource(R.drawable.ic_google_logo),
                            contentDescription = "Google logo",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                )
                ObSecondaryButton("Create new account", onCreateAccount, icon = Icons.Rounded.PersonAdd)

                Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.Center) {
                    Text("Already have an account? ", color = Color(0xCCFFFFFF), fontSize = 12.sp)
                    Text(
                        "Sign In", color = ObColors.Lime, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(onClick = onSignInExisting)
                    )
                }

                Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.Center) {
                    Text("By signing up, you agree to our ", color = Color(0xCCFFFFFF), fontSize = 11.sp)
                    Text(
                        "Terms and Privacy Policy", color = ObColors.Lime, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable(onClick = onTerms)
                    )
                }
            }
        }
        if (showAbout) {
            Dialog(onDismissRequest = { showAbout = false }) {
                ObGlassCard(Modifier.fillMaxWidth(), RoundedCornerShape(24.dp)) {
                    Column(Modifier.background(ObColors.Night.copy(alpha = 0.85f)).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        OscellionAppIcon(size = 48.dp)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Welcome to Oscellion",
                            fontFamily = TheSignatureFont,
                            fontSize = 32.sp,
                            style = TextStyle(brush = animatedWaveBrush),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Design multi-layer shaders and wallpapers, share them, and message with end-to-end encryption.",
                            style = ObType.Body, textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(18.dp))
                        ObPrimaryButton("Start onboarding", { showAbout = false; onCreateAccount() })
                    }
                }
            }
        }
    }
}

/**
 * Pixel-perfect clean artwork matching the reference illustration with 100% fidelity.
 * Features starry sapphire sky, warm glowing horizon sunset, skyscraper silhouettes with glowing windows,
 * and the 5 cobalt rim-lit figures on the rooftop.
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
        // Protective scrims ensuring headline and auth buttons remain razor-sharp and accessible across all aspect ratios
        Canvas(Modifier.fillMaxSize()) {
            // Top scrim for status bar & headline readability
            drawRect(
                Brush.verticalGradient(
                    0.0f to Color(0xB3020518),
                    0.32f to Color.Transparent
                )
            )
            // Bottom scrim to ensure auth buttons and legal text pop crisply
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
