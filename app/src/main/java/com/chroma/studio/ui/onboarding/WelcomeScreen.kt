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
import com.chroma.studio.ui.theme.InterFontFamily
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*

/**
 * Pixel-perfect Login & Sign Up Screen for Oscellion.
 * Built using the Oscellion Design System & Brand Identity:
 * - 3D Character Hero on frosted obsidian/titanium glass sphere with floating media badges
 * - Smooth dynamic gradient blending into the app's canvas background
 * - Luminous Oscellion brand gradient title and subtitle
 * - "Continue with Google" glossy pill with multicolor Google logo and arrow
 * - Subtle "OR" horizontal divider
 * - Dynamic system tokenized input fields (Username & Password) with visibility toggle
 * - Mode switch between "Create an Account" and "Sign In"
 * - "Continue without Account" frosted secondary pill button
 * - Legal disclaimer footer with clickable "Terms & Conditions" and "Privacy Policy" links
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        // Ambient Brand Glow behind Hero
        Box(
            modifier = Modifier
                .size(380.dp)
                .offset(x = (-60).dp, y = (-70).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            colors.primary.copy(alpha = if (isDark) 0.22f else 0.12f),
                            colors.primaryDark.copy(alpha = if (isDark) 0.08f else 0.04f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ─── 1. HERO 3D CHARACTER SECTION ─────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.hero_oscellion_auth),
                    contentDescription = "Oscellion Hero Character",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Smooth bottom gradient overlay fading into the design system background color
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
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
            }

            // ─── 2. HEADLINE & BRANDING ──────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Welcome to",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textMain,
                    letterSpacing = (-0.3).sp,
                    fontFamily = InterFontFamily
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = "Oscellion",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.8).sp,
                    style = TextStyle(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF38B6FF),
                                colors.primary,
                                Color(0xFF7C3AED),
                                Color(0xFFA855F7)
                            )
                        )
                    ),
                    fontFamily = InterFontFamily
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Create • Share • Explore",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textMuted,
                    letterSpacing = 0.5.sp,
                    fontFamily = InterFontFamily
                )
            }

            Spacer(Modifier.height(26.dp))

            // ─── 3. FORM CONTROLS & ACTION BUTTONS ───────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Continue with Google Button (Crisp White Pill)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .graphicsLayer {
                            scaleX = googleScale
                            scaleY = googleScale
                        }
                        .clip(RoundedCornerShape(colors.pillCornerRadius))
                        .background(if (isDark) Color.White else Color.White)
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
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (googleLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color(0xFF0F172A),
                            strokeWidth = 2.4.dp
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_google_logo),
                                contentDescription = "Google Logo",
                                modifier = Modifier.size(24.dp)
                            )

                            Spacer(Modifier.width(14.dp))

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(24.dp)
                                    .background(Color(0xFFE2E8F0))
                            )

                            Text(
                                text = "Continue with Google",
                                color = Color(0xFF0F172A),
                                fontSize = 15.5.sp,
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
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // OR Divider
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
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
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(horizontal = 14.dp),
                        fontFamily = InterFontFamily
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(colors.cardBorderSubtle)
                    )
                }

                // Username Input Field (Design System Tokenized)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(colors.pillCornerRadius))
                        .background(colors.inputBg)
                        .border(1.dp, colors.inputBorder, RoundedCornerShape(colors.pillCornerRadius))
                        .padding(horizontal = 18.dp),
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
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(Modifier.width(12.dp))

                        Box(modifier = Modifier.weight(1f)) {
                            if (username.isEmpty()) {
                                Text(
                                    text = "Username",
                                    color = colors.textMuted.copy(alpha = 0.7f),
                                    fontSize = 15.sp,
                                    fontFamily = InterFontFamily
                                )
                            }
                            BasicTextField(
                                value = username,
                                onValueChange = { username = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = colors.textMain,
                                    fontSize = 15.sp,
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

                Spacer(Modifier.height(12.dp))

                // Password Input Field (Design System Tokenized)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(colors.pillCornerRadius))
                        .background(colors.inputBg)
                        .border(1.dp, colors.inputBorder, RoundedCornerShape(colors.pillCornerRadius))
                        .padding(horizontal = 18.dp),
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
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(Modifier.width(12.dp))

                        Box(modifier = Modifier.weight(1f)) {
                            if (password.isEmpty()) {
                                Text(
                                    text = "Password",
                                    color = colors.textMuted.copy(alpha = 0.7f),
                                    fontSize = 15.sp,
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
                                    fontSize = 15.sp,
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
                                .size(20.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { passwordVisible = !passwordVisible }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Primary Button ("Create an Account" / "Sign In")
                val primaryGradient = Brush.horizontalGradient(
                    colors = listOf(
                        colors.primaryHover,
                        colors.primary,
                        colors.primaryDark
                    )
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .graphicsLayer {
                            scaleX = primaryScale
                            scaleY = primaryScale
                        }
                        .shadow(
                            elevation = 12.dp,
                            shape = CircleShape,
                            ambientColor = colors.primary.copy(alpha = 0.35f),
                            spotColor = colors.primary.copy(alpha = 0.55f)
                        )
                        .clip(RoundedCornerShape(colors.pillCornerRadius))
                        .background(primaryGradient)
                        .clickable(
                            enabled = !isSubmitting,
                            interactionSource = primaryInteraction,
                            indication = null,
                            onClick = { handlePrimarySubmit() }
                        )
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = colors.onPrimary,
                            strokeWidth = 2.4.dp
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
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = InterFontFamily,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )

                            Icon(
                                imageVector = Lucide.ArrowRight,
                                contentDescription = null,
                                tint = colors.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Mode Switch Link ("Already have an account? Sign In" / "Need an account? Create an Account")
                Row(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { isSignUpMode = !isSignUpMode }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSignUpMode) "Already have an account? " else "Need a new account? ",
                        color = colors.textMuted,
                        fontSize = 13.sp,
                        fontFamily = InterFontFamily
                    )
                    Text(
                        text = if (isSignUpMode) "Sign In" else "Create an Account",
                        color = colors.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFontFamily
                    )
                }

                Spacer(Modifier.height(6.dp))

                // Continue without Account Button (Frosted Obsidian Secondary Pill)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
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
                        .padding(horizontal = 20.dp),
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
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFontFamily,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )

                        Icon(
                            imageVector = Lucide.ChevronRight,
                            contentDescription = null,
                            tint = colors.textMain,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ─── 4. LEGAL DISCLAIMER FOOTER ──────────────────────────────
                val termsText = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = colors.textMuted,
                            fontSize = 12.sp,
                            fontFamily = InterFontFamily
                        )
                    ) {
                        append("By continuing, you agree to our\n")
                    }

                    pushStringAnnotation(tag = "TERMS", annotation = "terms")
                    withStyle(
                        SpanStyle(
                            color = colors.primary,
                            fontSize = 12.sp,
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
                            fontSize = 12.sp,
                            fontFamily = InterFontFamily
                        )
                    ) {
                        append(" and ")
                    }

                    pushStringAnnotation(tag = "PRIVACY", annotation = "privacy")
                    withStyle(
                        SpanStyle(
                            color = colors.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = TextDecoration.Underline,
                            fontFamily = InterFontFamily
                        )
                    ) {
                        append("Privacy Policy")
                    }
                    pop()
                }

                ClickableText(
                    text = termsText,
                    onClick = { offset ->
                        termsText.getStringAnnotations(tag = "TERMS", start = offset, end = offset)
                            .firstOrNull()?.let { onTerms() }
                        termsText.getStringAnnotations(tag = "PRIVACY", start = offset, end = offset)
                            .firstOrNull()?.let { onTerms() }
                    },
                    style = TextStyle(textAlign = TextAlign.Center),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}
