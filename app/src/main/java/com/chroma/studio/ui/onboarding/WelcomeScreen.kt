package com.chroma.studio.ui.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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
import com.chroma.studio.ui.components.GhostFibers
import com.chroma.studio.ui.components.GhostFibersConfig

import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.PersonAdd

val TheSignatureFont = FontFamily(Font(R.font.thesignature))

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
    // Neon Green animated wave gradient
    val animatedWaveBrush = rememberNeonGreenWaveBrush()

    Box(Modifier.fillMaxSize().background(ObColors.Abyss)) {
        // GhostFibers animated background (React Bits port)
        GhostFibers(
            modifier = Modifier.fillMaxSize(),
            config = GhostFibersConfig(
                glowColor = androidx.compose.ui.graphics.Color(0xFF001FFF),
                lineColor = androidx.compose.ui.graphics.Color(0xFF0070FF),
                secondaryColor = androidx.compose.ui.graphics.Color(0xFF0038FF),
                layers = 3,
                twist = 1.2f,
                threads = 72,
                depthOfField = 0.85f,
                particles = 0.5f,
                sparkle = 0.5f,
                glow = 0.8f,
                brightness = 1f,
                intro = true
            )
        )
        // Protective scrims — keep text and buttons readable over the fiber animation
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            drawRect(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    0.0f to androidx.compose.ui.graphics.Color(0xCC010414),
                    0.38f to androidx.compose.ui.graphics.Color.Transparent
                )
            )
            drawRect(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    0.55f to androidx.compose.ui.graphics.Color.Transparent,
                    0.75f to androidx.compose.ui.graphics.Color(0xCC040722),
                    1.0f to androidx.compose.ui.graphics.Color(0xF2040722)
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
                    .padding(horizontal = 22.dp, vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = screenHeight - 24.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Bar & Hero Section
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OscellionAppIcon(size = 36.dp)
                        }

                        Spacer(Modifier.height(18.dp))

                        // Hero Section: "Welcome to Oscellion" with animated Neon Green wave gradient & bold title
                        Text(
                            text = "Welcome to",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = (-0.5).sp,
                            lineHeight = 40.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Oscellion",
                            fontFamily = TheSignatureFont,
                            fontSize = 58.sp,
                            lineHeight = 56.sp,
                            style = TextStyle(brush = animatedWaveBrush)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            buildAnnotatedString {
                                append("Where ")
                                withStyle(SpanStyle(color = ObColors.NeonGreen, fontWeight = FontWeight.Bold)) { append("Creativity") }
                                append(" has a Social Life")
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.88f),
                            letterSpacing = (-0.2).sp,
                            lineHeight = 19.sp
                        )
                    }

                    // Bottom Action Section (All buttons shifted to bottom)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp, bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
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
                        ObAppleButton(
                            text = "Sign in with Apple",
                            onClick = onApple,
                            loading = appleLoading
                        )
                        ObSecondaryButton("Create new account", onCreateAccount, icon = Icons.Rounded.PersonAdd)
                        ObSecondaryButton("Explore Studio (No login required)", onExploreGuest, icon = Icons.Rounded.Explore)

                        Spacer(Modifier.height(2.dp))

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            Text("Already have an account? ", color = Color(0xCCFFFFFF), fontSize = 12.5.sp, letterSpacing = (-0.3).sp)
                            Text(
                                "Sign In", color = ObColors.NeonGreen, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp,
                                modifier = Modifier.clickable(onClick = onSignInExisting)
                            )
                        }

                        Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.Center) {
                            Text("By signing up, you agree to our ", color = Color(0xCCFFFFFF), fontSize = 11.sp, letterSpacing = (-0.2).sp)
                            Text(
                                "Terms and Privacy Policy", color = ObColors.NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp,
                                modifier = Modifier.clickable(onClick = onTerms)
                            )
                        }
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
