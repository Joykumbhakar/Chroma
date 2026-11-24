package com.chroma.studio.ui.onboarding

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.R
import com.chroma.studio.ui.components.OscellionLoadingSpinner
import com.chroma.studio.ui.theme.InterFontFamily
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*

/**
 * Compact, Professional Login & Sign Up Screen for Oscellion.
 *
 * Design Guidelines:
 * - Fits completely on a single viewport without scrolling on standard mobile screens.
 * - Uses strictly professional Oscellion brand identity palette (Azure, Brand Blue #0381FE, Deep Royal #0072DE).
 * - Zero purple/violet gradients.
 * - Balanced, compact vertical rhythm:
 *     1. Hero section: 180dp with smooth background fade
 *     2. Branding: Crisp "Welcome to" & professional blue gradient "Oscellion"
 *     3. Continue with Google: 48dp crisp white pill
 *     4. Inputs: 46dp compact pill fields with design system tokens
 *     5. Primary action: 48dp professional blue gradient button
 *     6. Secondary action: 46dp frosted glass pill button
 *     7. Footer legal: 11.5sp compact disclaimer
 */
@Composable
fun WelcomeScreen(
    savedAccountName: String? = null,
    googleLoading: Boolean = false,
    appleLoading: Boolean = false,
    isSubmitting: Boolean = false,
    onContinueSaved: () -> Unit = {},
    onGoogle: () -> Unit = {},
    onApple: () -> Unit = {},
    onCreateAccount: () -> Unit = {},
    onSignInExisting: () -> Unit = {},
    onExploreGuest: () -> Unit = {},
    onTerms: () -> Unit = {},
    onSignUpWithCredentials: ((username: String, password: String) -> Unit)? = null,
    onSignInWithCredentials: ((username: String, password: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isSignUpMode by remember { mutableStateOf(true) }

    val scrollState = rememberScrollState()

    // Google Button Animation
    val googleInteraction = remember { MutableInteractionSource() }
    val googlePressed by googleInteraction.collectIsPressedAsState()
    val googleScale by animateFloatAsState(
        targetValue = if (googlePressed) 0.975f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioLowBouncy),
        label = "googleScale"
    )

    // Primary Button Animation
    val primaryInteraction = remember { MutableInteractionSource() }
    val primaryPressed by primaryInteraction.collectIsPressedAsState()
    val primaryScale by animateFloatAsState(
        targetValue = if (primaryPressed) 0.975f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioLowBouncy),
        label = "primaryScale"
    )

    // Guest Button Animation
    val guestInteraction = remember { MutableInteractionSource() }
    val guestPressed by guestInteraction.collectIsPressedAsState()
    val guestScale by animateFloatAsState(
        targetValue = if (guestPressed) 0.975f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioLowBouncy),
        label = "guestScale"
    )

    fun handlePrimarySubmit() {
        val cleanUser = username.trim()
        val cleanPass = password.trim()

        if (cleanUser.isEmpty() && cleanPass.isEmpty()) {
            if (isSignUpMode) {
                onCreateAccount()
            } else {
                onSignInExisting()
            }
            return
        }

        if (cleanUser.length < 3) {
            Toast.makeText(context, "Username must be at least 3 characters", Toast.LENGTH_SHORT).show()
            return
        }
        if (cleanPass.length < 6) {
            Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        focusManager.clearFocus()
        if (isSignUpMode) {
            if (onSignUpWithCredentials != null) {
                onSignUpWithCredentials(cleanUser, cleanPass)
            } else {
                onCreateAccount()
            }
        } else {
            if (onSignInWithCredentials != null) {
                onSignInWithCredentials(cleanUser, cleanPass)
            } else {
                onSignInExisting()
            }
        }
    }

    // ─── LEGAL DISCLAIMER ANNOTATED STRING ────────────────────────────
    val termsText = remember(colors) {
        buildAnnotatedString {
            withStyle(
                SpanStyle(
                    color = colors.textMuted,
                    fontSize = 11.5.sp,
                    fontFamily = InterFontFamily
                )
            ) {
                append("By continuing, you agree to our ")
            }

            pushStringAnnotation(tag = "TERMS", annotation = "terms")
            withStyle(
                SpanStyle(
                    color = colors.primary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline,
                    fontFamily = InterFontFamily
                )
            ) {
                append("Terms & Conditions")
            }
            pop()

            withStyle(
                SpanStyle(
                    color = colors.textMuted,
                    fontSize = 11.5.sp,
                    fontFamily = InterFontFamily
                )
            ) {
                append(" and ")
            }

            pushStringAnnotation(tag = "PRIVACY", annotation = "privacy")
            withStyle(
                SpanStyle(
                    color = colors.primary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline,
                    fontFamily = InterFontFamily
                )
            ) {
                append("Privacy Policy")
            }
            pop()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        // Ambient Brand Blue Glow behind Hero (Strictly professional blue, zero purple)
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(x = (-50).dp, y = (-50).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            colors.primary.copy(alpha = if (isDark) 0.18f else 0.08f),
                            colors.primaryDark.copy(alpha = if (isDark) 0.06f else 0.02f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ─── 1. HERO 3D CHARACTER SECTION (DEVICE WIDTH = IMAGE WIDTH, NO STRETCHING) ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(if (colors.isDark) R.drawable.hero_oscellion_auth else R.drawable.hero_oscellion_auth_light),
                    contentDescription = "Oscellion Hero Character",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(888f / 1024f)
                )

                // Smooth bottom gradient overlay fading into the canvas background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    colors.bg.copy(alpha = 0.45f),
                                    colors.bg.copy(alpha = 0.85f),
                                    colors.bg
                                )
                            )
                        )
                )

                // ─── 2. HEADLINE & BRANDING (OFFICIAL OSCELLION BRAND LOCKUP OVERLAID ON SPHERE) ────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Welcome to",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textMain,
                        letterSpacing = (-0.2).sp,
                        fontFamily = InterFontFamily
                    )

                    Spacer(Modifier.height(4.dp))

                    Image(
                        painter = painterResource(R.drawable.oscellion_brand_lockup),
                        contentDescription = "Oscellion",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .height(34.dp)
                            .aspectRatio(455f / 95f)
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Create • Share • Explore",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textMuted,
                        letterSpacing = 0.4.sp,
                        fontFamily = InterFontFamily
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // ─── 3. FORM CONTROLS & ACTION BUTTONS ───────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Continue with Google Button (Crisp White Pill - 48dp Compact)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .graphicsLayer {
                            scaleX = googleScale
                            scaleY = googleScale
                        }
                        .clip(RoundedCornerShape(colors.pillCornerRadius))
                        .background(Color.White)
                        .clickable(
                            enabled = !googleLoading,
                            interactionSource = googleInteraction,
                            indication = null,
                            onClick = onGoogle
                        )
                        .border(
                            1.dp,
                            if (isDark) Color.Transparent else Color(0xFFE2E8F0),
                            RoundedCornerShape(colors.pillCornerRadius)
                        )
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (googleLoading) {
                        OscellionLoadingSpinner(
                            modifier = Modifier.size(22.dp),
                            color = Color(0xFF0F172A)
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_google_logo),
                                contentDescription = "Google Logo",
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(Modifier.width(12.dp))

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(20.dp)
                                    .background(Color(0xFFE2E8F0))
                            )

                            Text(
                                text = "Continue with Google",
                                color = Color(0xFF0F172A),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = InterFontFamily,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                textAlign = TextAlign.Center
                            )

                            Icon(
                                imageVector = Lucide.ArrowRight,
                                contentDescription = null,
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // OR Divider (Tight Vertical Margin)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(colors.cardBorderSubtle)
                    )
                    Text(
                        text = "OR",
                        color = colors.textMuted,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(horizontal = 12.dp),
                        fontFamily = InterFontFamily
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(colors.cardBorderSubtle)
                    )
                }

                // Username Input Field (46dp Compact)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(colors.pillCornerRadius))
                        .background(colors.inputBg)
                        .border(1.dp, colors.inputBorder, RoundedCornerShape(colors.pillCornerRadius))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Lucide.User,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(Modifier.width(10.dp))

                        Box(modifier = Modifier.weight(1f)) {
                            if (username.isEmpty()) {
                                Text(
                                    text = "Username",
                                    color = colors.textMuted.copy(alpha = 0.7f),
                                    fontSize = 14.sp,
                                    fontFamily = InterFontFamily
                                )
                            }
                            BasicTextField(
                                value = username,
                                onValueChange = { username = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = colors.textMain,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = InterFontFamily
                                ),
                                cursorBrush = SolidColor(colors.primary),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Password Input Field (46dp Compact)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(colors.pillCornerRadius))
                        .background(colors.inputBg)
                        .border(1.dp, colors.inputBorder, RoundedCornerShape(colors.pillCornerRadius))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Lucide.Lock,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(Modifier.width(10.dp))

                        Box(modifier = Modifier.weight(1f)) {
                            if (password.isEmpty()) {
                                Text(
                                    text = "Password",
                                    color = colors.textMuted.copy(alpha = 0.7f),
                                    fontSize = 14.sp,
                                    fontFamily = InterFontFamily
                                )
                            }
                            BasicTextField(
                                value = password,
                                onValueChange = { password = it },
                                singleLine = true,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                textStyle = TextStyle(
                                    color = colors.textMain,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = InterFontFamily
                                ),
                                cursorBrush = SolidColor(colors.primary),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { handlePrimarySubmit() }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Icon(
                            imageVector = if (passwordVisible) Lucide.EyeOff else Lucide.Eye,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = colors.textMuted,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { passwordVisible = !passwordVisible }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Primary Button ("Create an Account" / "Sign In" - Professional Blue Gradient)
                val primaryGradient = Brush.horizontalGradient(
                    colors = listOf(
                        colors.primaryHover, // Azure Blue (#3E91FF)
                        colors.primary,      // Brand Blue (#0381FE)
                        colors.primaryDark   // Deep Royal Blue (#0072DE)
                    )
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .graphicsLayer {
                            scaleX = primaryScale
                            scaleY = primaryScale
                        }
                        .shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            ambientColor = colors.primary.copy(alpha = 0.25f),
                            spotColor = colors.primary.copy(alpha = 0.45f)
                        )
                        .clip(RoundedCornerShape(colors.pillCornerRadius))
                        .background(primaryGradient)
                        .clickable(
                            enabled = !isSubmitting,
                            interactionSource = primaryInteraction,
                            indication = null,
                            onClick = { handlePrimarySubmit() }
                        )
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSubmitting) {
                        OscellionLoadingSpinner(
                            modifier = Modifier.size(22.dp),
                            color = colors.onPrimary
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isSignUpMode) "Create an Account" else "Sign In",
                                color = colors.onPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = InterFontFamily,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )

                            Icon(
                                imageVector = Lucide.ArrowRight,
                                contentDescription = null,
                                tint = colors.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Mode Switch Link ("Already have an account? Sign In" / "Need an account? Create an Account")
                Row(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { isSignUpMode = !isSignUpMode }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSignUpMode) "Already have an account? " else "Need a new account? ",
                        color = colors.textMuted,
                        fontSize = 12.5.sp,
                        fontFamily = InterFontFamily
                    )
                    Text(
                        text = if (isSignUpMode) "Sign In" else "Create an Account",
                        color = colors.primary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFontFamily
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Continue without Account Button (46dp Compact Frosted Pill)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .graphicsLayer {
                            scaleX = guestScale
                            scaleY = guestScale
                        }
                        .clip(RoundedCornerShape(colors.pillCornerRadius))
                        .background(colors.glassBg.copy(alpha = 0.65f))
                        .border(1.dp, colors.glassBorder, RoundedCornerShape(colors.pillCornerRadius))
                        .clickable(
                            interactionSource = guestInteraction,
                            indication = null,
                            onClick = onExploreGuest
                        )
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Continue without Account",
                            color = colors.textMain,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFontFamily,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )

                        Icon(
                            imageVector = Lucide.ChevronRight,
                            contentDescription = null,
                            tint = colors.textMain,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Clearance spacing so scrollable content isn't obscured by the bottom snapped footer
            Spacer(Modifier.height(52.dp))
            Spacer(Modifier.navigationBarsPadding())
        }

        // ─── 4. LEGAL DISCLAIMER FOOTER (ALWAYS SNAPPED TO BOTTOM END) ───
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            colors.bg.copy(alpha = 0.85f),
                            colors.bg,
                            colors.bg
                        )
                    )
                )
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 10.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            ClickableText(
                text = termsText,
                onClick = { offset ->
                    termsText.getStringAnnotations(tag = "TERMS", start = offset, end = offset)
                        .firstOrNull()?.let { onTerms() }
                    termsText.getStringAnnotations(tag = "PRIVACY", start = offset, end = offset)
                        .firstOrNull()?.let { onTerms() }
                },
                style = TextStyle(textAlign = TextAlign.Center)
            )
        }
    }
}
