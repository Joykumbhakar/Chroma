package com.chroma.studio.ui.onboarding

import android.graphics.BlurMaskFilter
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

// =============================================================================
// DESIGN TOKENS & TYPOGRAPHY
// =============================================================================

/**
 * Exact color tokens matching the Oscellion minimal light auth design & studio splash.
 */
private object OscellionColors {
    val CanvasBackground = Color(0xFFFAFCFF)
    val TextPrimary = Color(0xFF0A0E1A)
    val BrandHeading = Color(0xFF050814)
    val ButtonLabel = Color(0xFF0B0F1A)
    val ChevronTint = Color(0xFF121826)

    val EyebrowText = Color(0xFF596075)
    val TaglineText = Color(0xFF5A6175)
    val TaglineDot = Color(0xFF727A91)
    val PromptText = Color(0xFF4C5368)
    val FooterText = Color(0xFF5B6275)
    val OrText = Color(0xFF4A5168)
    val DividerLine = Color(0xFFDCE2EC)

    val GoogleBorder = Color(0xFFE7ECF4)
    val GoogleDivider = Color(0xFFE2E7F0)
    val GoogleShadow = Color(0x140F2350) // rgba(15, 35, 80, 0.08)

    val InputBg = Color(0xFFEFF1F6)
    val InputBorder = Color(0xFFE5E8F0)
    val InputPlaceholder = Color(0xFF8E95A8)
    val InputIcon = Color(0xFF5B6378)
    val FocusBlue = Color(0xFF0066FF)
    val FocusRing = Color(0x1A0066FF)

    val CtaTop = Color(0xFF0E76FF)
    val CtaMid = Color(0xFF0064FA)
    val CtaBottom = Color(0xFF0054E3)
    val CtaShadow = Color(0x6B0062F5) // rgba(0, 98, 245, 0.42)
    val LinkBlue = Color(0xFF0062FA)

    val GuestBg = Color(0xF2F6F8FC)
    val GuestBgPressed = Color(0xFFEEF2FA)
    val GuestBorder = Color(0xFFDCE2EE)

    // Official 4-Color Google G palette
    val GoogleBlue = Color(0xFF4285F4)
    val GoogleGreen = Color(0xFF34A853)
    val GoogleYellow = Color(0xFFFBBC05)
    val GoogleRed = Color(0xFFEA4335)

    // Toast palette
    val ToastInfoBg = Color(0xF20A0E1A)
    val ToastSuccessBg = Color(0xF20A162E)
    val ToastErrorBg = Color(0xF22A0E16)
    val ToastInfoIcon = Color(0xFF60A5FA)
    val ToastSuccessIcon = Color(0xFF4EA1FF)
    val ToastErrorIcon = Color(0xFFFB7185)
}

private val OscellionBrandFont: FontFamily = FontFamily.SansSerif
private val OscellionBodyFont: FontFamily = FontFamily.SansSerif

private enum class AuthMode { SIGN_UP, SIGN_IN }
private enum class LegalModalType { TERMS, PRIVACY }
private enum class ToastType { INFO, SUCCESS, ERROR }

private data class ToastState(
    val message: String,
    val type: ToastType = ToastType.INFO
)

// =============================================================================
// CUSTOM SHADOW & MATH UTILITIES
// =============================================================================

/**
 * Draws a true colored, soft pill drop shadow matching CSS box-shadow across Android API levels.
 */
private fun Modifier.softPillShadow(
    color: Color,
    blurRadius: Dp,
    offsetY: Dp = 0.dp,
    spread: Dp = 0.dp
): Modifier = drawBehind {
    val blurPx = blurRadius.toPx()
    val offsetYPx = offsetY.toPx()
    val spreadPx = spread.toPx()

    drawIntoCanvas { canvas ->
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.isAntiAlias = true
        frameworkPaint.color = color.toArgb()
        if (blurPx > 0f) {
            frameworkPaint.maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
        }
        val left = -spreadPx
        val top = offsetYPx - spreadPx
        val right = size.width + spreadPx
        val bottom = size.height + offsetYPx + spreadPx
        val radius = (bottom - top) / 2f
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, paint)
    }
}

private fun lerpFloat(start: Float, end: Float, fraction: Float): Float =
    start + (end - start) * fraction

/**
 * Evaluates a 3-keyframe closed spline loop matching SVG's `<animate calcMode="spline">` behavior.
 */
private fun evalThreePhaseLoop(
    progress: Float,
    t1: Float,
    t2: Float,
    v0: Float,
    v1: Float,
    v2: Float
): Float {
    val ease = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
    return when {
        progress <= t1 -> {
            val f = ease.transform((progress / t1).coerceIn(0f, 1f))
            lerpFloat(v0, v1, f)
        }
        progress <= t2 -> {
            val f = ease.transform(((progress - t1) / (t2 - t1)).coerceIn(0f, 1f))
            lerpFloat(v1, v2, f)
        }
        else -> {
            val f = ease.transform(((progress - t2) / (1f - t2)).coerceIn(0f, 1f))
            lerpFloat(v2, v0, f)
        }
    }
}

// =============================================================================
// MAIN ENTRY COMPOSABLE: OSCELLION AUTH SCREEN + CINEMATIC INTRO
// =============================================================================

/**
 * Drop-in Oscellion Login & Sign Up screen featuring:
 * 1. Cinematic V2 beveled Oscellion logo reveal splash screen (auto-transitions or tap to skip)
 * 2. Liquid-morphing multi-layered top-left & bottom-right vector wave background
 * 3. Staggered auth UI entrance matching every pixel, color, shadow, and icon of the reference UI
 * 4. Built-in Sign Up / Sign In switching, validation, custom toast, and Terms/Privacy modal
 */
@Composable
fun OscellionAuthScreen(
    modifier: Modifier = Modifier,
    playIntroOnStart: Boolean = true,
    isLoading: Boolean = false,
    externalToast: Pair<String, Boolean>? = null,
    externalToastKey: Int = 0,
    onIntroFinished: () -> Unit = {},
    onGoogleAuth: (isSignUp: Boolean) -> Unit = {},
    onCreateAccount: (username: String, password: String) -> Unit = { _, _ -> },
    onSignIn: (username: String, password: String) -> Unit = { _, _ -> },
    onContinueWithoutAccount: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Splash & staggered auth entrance state
    var showSplash by remember { mutableStateOf(playIntroOnStart) }
    var splashRunId by remember { mutableIntStateOf(0) }
    var authReady by remember { mutableStateOf(!playIntroOnStart) }

    // Authentication form state (defaults to Sign Up to match the reference design)
    var authMode by remember { mutableStateOf(AuthMode.SIGN_UP) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Modal & Toast state
    var activeLegalModal by remember { mutableStateOf<LegalModalType?>(null) }
    var toastState by remember { mutableStateOf<ToastState?>(null) }
    var toastJob by remember { mutableStateOf<Job?>(null) }

    fun showToast(message: String, type: ToastType = ToastType.INFO) {
        toastJob?.cancel()
        toastState = ToastState(message, type)
        toastJob = scope.launch {
            delay(3200L)
            toastState = null
        }
    }

    LaunchedEffect(externalToastKey) {
        val toast = externalToast
        if (externalToastKey > 0 && toast != null) {
            showToast(toast.first, if (toast.second) ToastType.ERROR else ToastType.SUCCESS)
        }
    }

    fun dismissSplash() {
        if (showSplash) {
            showSplash = false
            authReady = true
            onIntroFinished()
        }
    }

    fun replayIntro() {
        focusManager.clearFocus()
        authReady = false
        splashRunId += 1
        showSplash = true
    }

    BackHandler(enabled = showSplash) {
        dismissSplash()
    }

    fun submitForm() {
        if (isLoading) return
        try {
            val trimmedUser = username.trim().removePrefix("@").trim()
            if (trimmedUser.isEmpty()) {
                showToast("Please enter your username.", ToastType.ERROR)
                return
            }
            if (password.length < 6) {
                showToast("Password must be at least 6 characters.", ToastType.ERROR)
                return
            }
            focusManager.clearFocus()
            if (authMode == AuthMode.SIGN_UP) {
                onCreateAccount(trimmedUser, password)
            } else {
                onSignIn(trimmedUser, password)
            }
        } catch (e: Exception) {
            showToast("Something went wrong. Please try again.", ToastType.ERROR)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OscellionColors.CanvasBackground)
    ) {
        // 1. Living Multi-Layer Animated Vector Waves Background
        AnimatedOscellionWaves(modifier = Modifier.fillMaxSize())

        // 2. Main Scrollable Responsive Auth Content + Footer
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            val viewportHeight = maxHeight
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 420.dp)
                        .fillMaxWidth()
                        .height(maxOf(viewportHeight, 720.dp))
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Centered Auth Core
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(top = 52.dp, bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Stage 1: Brand Header Block (Tap Oscellion title to replay intro)
                        StaggeredAuthStage(visible = authReady, delayMillis = 40) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(bottom = 38.dp)
                            ) {
                                Text(
                                    text = "Welcome to",
                                    fontFamily = OscellionBodyFont,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 21.sp,
                                    letterSpacing = (-0.2).sp,
                                    color = OscellionColors.EyebrowText
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Oscellion",
                                    fontFamily = OscellionBrandFont,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 48.sp,
                                    letterSpacing = (-1.2).sp,
                                    lineHeight = 52.sp,
                                    color = OscellionColors.BrandHeading,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = { replayIntro() }
                                        )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Create",
                                        fontFamily = OscellionBodyFont,
                                        fontSize = 15.5.sp,
                                        color = OscellionColors.TaglineText
                                    )
                                    Text(
                                        text = "•",
                                        fontSize = 13.sp,
                                        color = OscellionColors.TaglineDot
                                    )
                                    Text(
                                        text = "Share",
                                        fontFamily = OscellionBodyFont,
                                        fontSize = 15.5.sp,
                                        color = OscellionColors.TaglineText
                                    )
                                    Text(
                                        text = "•",
                                        fontSize = 13.sp,
                                        color = OscellionColors.TaglineDot
                                    )
                                    Text(
                                        text = "Explore",
                                        fontFamily = OscellionBodyFont,
                                        fontSize = 15.5.sp,
                                        color = OscellionColors.TaglineText
                                    )
                                }
                            }
                        }

                        // Stage 2: Continue with Google Pill + OR Divider
                        StaggeredAuthStage(visible = authReady, delayMillis = 120) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                GoogleAuthPillButton(
                                    onClick = {
                                        if (!isLoading) {
                                            showToast("Connecting with Google...", ToastType.INFO)
                                            onGoogleAuth(authMode == AuthMode.SIGN_UP)
                                        }
                                    }
                                )

                                // "OR" Horizontal Divider
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 22.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(1.dp)
                                            .background(OscellionColors.DividerLine)
                                    )
                                    Text(
                                        text = "OR",
                                        fontFamily = OscellionBodyFont,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.5.sp,
                                        letterSpacing = 0.5.sp,
                                        color = OscellionColors.OrText
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(1.dp)
                                            .background(OscellionColors.DividerLine)
                                    )
                                }
                            }
                        }

                        // Stage 3: Credentials Pill Fields + Primary Electric Blue CTA
                        StaggeredAuthStage(visible = authReady, delayMillis = 190) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                OscellionPillInputField(
                                    value = username,
                                    onValueChange = { username = it },
                                    placeholder = "Username",
                                    leadingIcon = { UserOutlineIcon() },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Text,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                    )
                                )

                                OscellionPillInputField(
                                    value = password,
                                    onValueChange = { password = it },
                                    placeholder = "Password",
                                    leadingIcon = { LockOutlineIcon() },
                                    visualTransformation = if (passwordVisible) {
                                        VisualTransformation.None
                                    } else {
                                        PasswordVisualTransformation()
                                    },
                                    trailingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .clickable(
                                                    role = Role.Button,
                                                    onClickLabel = if (passwordVisible) "Hide password" else "Show password"
                                                ) { passwordVisible = !passwordVisible },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            EyeOutlineIcon(isVisible = passwordVisible)
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { submitForm() }
                                    )
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                PrimaryElectricCtaButton(
                                    label = if (authMode == AuthMode.SIGN_UP) "Create an Account" else "Sign In",
                                    onClick = { submitForm() }
                                )
                            }
                        }

                        // Stage 4: Mode Switch Link + Continue without Account Button
                        StaggeredAuthStage(visible = authReady, delayMillis = 260) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = if (authMode == AuthMode.SIGN_UP) {
                                            "Already have an account?"
                                        } else {
                                            "Don't have an account?"
                                        },
                                        fontFamily = OscellionBodyFont,
                                        fontSize = 15.sp,
                                        color = OscellionColors.PromptText
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (authMode == AuthMode.SIGN_UP) "Sign In" else "Create an Account",
                                        fontFamily = OscellionBodyFont,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 15.sp,
                                        color = OscellionColors.LinkBlue,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable(role = Role.Button) {
                                                authMode = if (authMode == AuthMode.SIGN_UP) {
                                                    AuthMode.SIGN_IN
                                                } else {
                                                    AuthMode.SIGN_UP
                                                }
                                            }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                ContinueGuestPillButton(
                                    onClick = {
                                        if (!isLoading) {
                                            onContinueWithoutAccount()
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Stage 5: Legal Footer
                    StaggeredAuthStage(visible = authReady, delayMillis = 330) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 26.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "By continuing, you agree to our",
                                fontFamily = OscellionBodyFont,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = OscellionColors.FooterText,
                                textAlign = TextAlign.Center
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(
                                            SpanStyle(
                                                color = OscellionColors.LinkBlue,
                                                textDecoration = TextDecoration.Underline
                                            )
                                        ) {
                                            append("Terms & Conditions")
                                        }
                                    },
                                    fontFamily = OscellionBodyFont,
                                    fontSize = 13.sp,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable(role = Role.Button) {
                                            activeLegalModal = LegalModalType.TERMS
                                        }
                                        .padding(horizontal = 2.dp, vertical = 1.dp)
                                )
                                Text(
                                    text = " and ",
                                    fontFamily = OscellionBodyFont,
                                    fontSize = 13.sp,
                                    color = OscellionColors.FooterText
                                )
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(
                                            SpanStyle(
                                                color = OscellionColors.LinkBlue,
                                                textDecoration = TextDecoration.Underline
                                            )
                                        ) {
                                            append("Privacy Policy")
                                        }
                                    },
                                    fontFamily = OscellionBodyFont,
                                    fontSize = 13.sp,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable(role = Role.Button) {
                                            activeLegalModal = LegalModalType.PRIVACY
                                        }
                                        .padding(horizontal = 2.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Subtle Top-Right "Replay Intro" Pill Button
        ReplayIntroButton(
            onClick = { replayIntro() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 12.dp, end = 14.dp)
        )

        // 4. Floating Pill Toast Notification
        OscellionToastHost(
            toastState = toastState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp, start = 20.dp, end = 20.dp)
        )

        // 5. Legal Summary Modal (Terms & Conditions / Privacy Policy)
        activeLegalModal?.let { modalType ->
            OscellionLegalDialog(
                type = modalType,
                onDismiss = { activeLegalModal = null }
            )
        }

        // 6. Full-Screen Cinematic Oscellion V2 Logo Reveal Splash Overlay
        OscellionSplashOverlay(
            visible = showSplash,
            runId = splashRunId,
            onDismiss = { dismissSplash() }
        )
    }
}

// =============================================================================
// CINEMATIC SPLASH SCREEN: OSCELLION BEVEL REVEAL V2
// =============================================================================

@Composable
private fun OscellionSplashOverlay(
    visible: Boolean,
    runId: Int,
    onDismiss: () -> Unit
) {
    // Smooth exit dissolve (opacity 1 -> 0, scale 1 -> 1.045) matching CSS #splash-overlay.is-exiting
    val exitEase = remember { CubicBezierEasing(0.22f, 1f, 0.36f, 1f) }
    val overlayAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 720, easing = exitEase),
        label = "splashAlpha"
    )
    val overlayScale by animateFloatAsState(
        targetValue = if (visible) 1f else 1.045f,
        animationSpec = tween(durationMillis = 720, easing = exitEase),
        label = "splashScale"
    )

    if (!visible && overlayAlpha <= 0.005f) return

    // Master timeline clock (0ms -> 2900ms) for exact stage synchronization
    val clockMs = remember { Animatable(0f) }

    LaunchedEffect(runId, visible) {
        if (visible) {
            clockMs.snapTo(0f)
            clockMs.animateTo(
                targetValue = 2900f,
                animationSpec = tween(durationMillis = 2900, easing = androidx.compose.animation.core.LinearEasing)
            )
            onDismiss()
        }
    }

    val t = clockMs.value // Current time in milliseconds [0f..2900f]

    // Helper for normalized eased progress within [startMs, endMs]
    fun stageProgress(startMs: Float, endMs: Float, easing: androidx.compose.animation.core.Easing): Float {
        val raw = ((t - startMs) / (endMs - startMs)).coerceIn(0f, 1f)
        return easing.transform(raw)
    }

    val expoOut = remember { CubicBezierEasing(0.16f, 1f, 0.3f, 1f) }
    val quintOut = remember { CubicBezierEasing(0.22f, 1f, 0.36f, 1f) }
    val traceEase = remember { CubicBezierEasing(0.4f, 0f, 0.2f, 1f) }
    val sweepEase = remember { CubicBezierEasing(0.25f, 1f, 0.5f, 1f) }

    // Camera settle: 1.06 -> 1.0 over 0..2400ms
    val lockupScale = lerpFloat(1.06f, 1f, stageProgress(0f, 2400f, expoOut))

    // Stage 1: Emblem aperture emergence, spin-lock & glide left (0..2200ms)
    val emblemNorm = (t / 2200f).coerceIn(0f, 1f)
    val emblemAlpha = if (emblemNorm < 0.18f) quintOut.transform(emblemNorm / 0.18f) else 1f
    val emblemTxFraction = when {
        emblemNorm < 0.44f -> 1.32f
        emblemNorm < 0.82f -> lerpFloat(1.32f, 0f, quintOut.transform((emblemNorm - 0.44f) / 0.38f))
        else -> 0f
    }
    val emblemScale = when {
        emblemNorm < 0.18f -> lerpFloat(0.35f, 1.16f, quintOut.transform(emblemNorm / 0.18f))
        emblemNorm < 0.34f -> lerpFloat(1.16f, 1.12f, quintOut.transform((emblemNorm - 0.18f) / 0.16f))
        emblemNorm < 0.44f -> 1.12f
        emblemNorm < 0.82f -> lerpFloat(1.12f, 1.0f, quintOut.transform((emblemNorm - 0.44f) / 0.38f))
        else -> 1f
    }
    val emblemRotation = when {
        emblemNorm < 0.18f -> lerpFloat(-42f, 4f, quintOut.transform(emblemNorm / 0.18f))
        emblemNorm < 0.34f -> lerpFloat(4f, 0f, quintOut.transform((emblemNorm - 0.18f) / 0.16f))
        else -> 0f
    }

    // Stage 2: Wireframe trace (80..1230ms)
    val wireNorm = ((t - 80f) / 1150f).coerceIn(0f, 1f)
    val wireStrokeFraction = traceEase.transform((wireNorm / 0.55f).coerceIn(0f, 1f))
    val wireAlpha = when {
        t < 80f -> 0f
        wireNorm < 0.55f -> lerpFloat(0.95f, 1f, wireNorm / 0.55f)
        wireNorm < 0.85f -> lerpFloat(1f, 0.35f, (wireNorm - 0.55f) / 0.30f)
        else -> lerpFloat(0.35f, 0f, (wireNorm - 0.85f) / 0.15f)
    }

    // Stage 3: Progressive Bevel Layer Ignition
    val topBevelAlpha = stageProgress(420f, 970f, expoOut)
    val bottomBevelAlpha = stageProgress(500f, 1050f, expoOut)
    val mainFaceAlpha = stageProgress(560f, 1160f, expoOut)

    // Stage 4: Wordmark "scellion" Clip-Path Unmask & Tracking Settle (960..1860ms)
    val wmP = stageProgress(960f, 1860f, expoOut)
    val wmAlpha = ((t - 960f) / 180f).coerceIn(0f, 1f)
    val wmSlideDp = lerpFloat(-22f, 0f, wmP)
    val wmTrackingEm = lerpFloat(0.04f, -0.02f, wmP)
    val wmBlurDp = lerpFloat(10f, 0f, wmP)
    val wmClipRightFraction = lerpFloat(0f, 1.06f, wmP)

    // Stage 5a: Emblem Diagonal Sheen (1560..2240ms)
    val logoSheenNorm = ((t - 1560f) / 680f).coerceIn(0f, 1f)
    val logoSheenX = lerpFloat(-420f, 940f, sweepEase.transform(logoSheenNorm))
    val logoSheenAlpha = when {
        t < 1560f || t > 2240f -> 0f
        logoSheenNorm < 0.18f -> logoSheenNorm / 0.18f
        logoSheenNorm > 0.82f -> (1f - logoSheenNorm) / 0.18f
        else -> 1f
    }

    // Stage 5b: Wordmark Diagonal Sheen (1740..2560ms)
    val textSheenNorm = ((t - 1740f) / 820f).coerceIn(0f, 1f)
    val textSheenPos = lerpFloat(-0.35f, 1.35f, sweepEase.transform(textSheenNorm))
    val textSheenAlpha = when {
        t < 1740f || t > 2560f -> 0f
        textSheenNorm < 0.15f -> textSheenNorm / 0.15f
        textSheenNorm > 0.85f -> (1f - textSheenNorm) / 0.15f
        else -> 1f
    }

    val density = LocalDensity.current

    // Parse exact Oscellion SVG emblem geometry once and cache
    val emblemPaths = remember { parseOscellionEmblemPaths() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = overlayAlpha
                scaleX = overlayScale
                scaleY = overlayScale
            }
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .semantics { contentDescription = "Oscellion intro animation. Tap anywhere to skip." },
        contentAlignment = Alignment.Center
    ) {
        // Centered Lockup Container
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .graphicsLayer {
                    scaleX = lockupScale
                    scaleY = lockupScale
                }
        ) {
            val emblemSize = 62.dp
            val emblemPx = with(density) { emblemSize.toPx() }

            // Beveled Oscellion Symbol Canvas
            Canvas(
                modifier = Modifier
                    .size(emblemSize)
                    .offset(x = 4.dp)
                    .graphicsLayer {
                        alpha = emblemAlpha
                        translationX = emblemPx * emblemTxFraction
                        scaleX = emblemScale
                        scaleY = emblemScale
                        rotationZ = emblemRotation
                        transformOrigin = TransformOrigin(0.54f, 0.50f)
                    }
            ) {
                val svgScale = min(size.width / 740f, size.height / 660f)
                val dx = (size.width - 740f * svgScale) / 2f - 100f * svgScale
                val dy = (size.height - 660f * svgScale) / 2f - 180f * svgScale

                withTransform({
                    translate(left = dx, top = dy)
                    scale(scaleX = svgScale, scaleY = svgScale, pivot = Offset.Zero)
                }) {
                    // 1. Precision Outline Trace (both outer contour & inner hole)
                    if (wireAlpha > 0.005f) {
                        val traceStroke = Stroke(
                            width = 11f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                        drawPartialContour(
                            source = emblemPaths.outerContour,
                            fraction = wireStrokeFraction,
                            color = Color.White.copy(alpha = wireAlpha),
                            stroke = traceStroke
                        )
                        drawPartialContour(
                            source = emblemPaths.innerHole,
                            fraction = wireStrokeFraction,
                            color = Color.White.copy(alpha = wireAlpha),
                            stroke = traceStroke
                        )
                    }

                    // 2. Top-left crisp bright bevel highlight
                    if (topBevelAlpha > 0.005f) {
                        withTransform({ translate(-7f, -7f) }) {
                            drawPath(
                                path = emblemPaths.combinedEvenOdd,
                                color = Color.White.copy(alpha = 0.95f * topBevelAlpha)
                            )
                        }
                    }

                    // 3. Bottom-right crisp dark bevel depth
                    if (bottomBevelAlpha > 0.005f) {
                        withTransform({ translate(7f, 7f) }) {
                            drawPath(
                                path = emblemPaths.combinedEvenOdd,
                                color = Color(0xFF5A5A60).copy(alpha = 0.90f * bottomBevelAlpha)
                            )
                        }
                    }

                    // 4. Main white metallic gradient face + specular rim + masked light sweep
                    if (mainFaceAlpha > 0.005f) {
                        clipPath(emblemPaths.combinedEvenOdd) {
                            val faceBrush = Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.00f to Color.White,
                                    0.48f to Color(0xFFF7F7F9),
                                    0.82f to Color(0xFFD4D4DA),
                                    1.00f to Color(0xFFA8A8B0)
                                ),
                                startY = 212f,
                                endY = 812f
                            )
                            drawPath(
                                path = emblemPaths.combinedEvenOdd,
                                brush = faceBrush,
                                alpha = mainFaceAlpha
                            )
                            drawPath(
                                path = emblemPaths.combinedEvenOdd,
                                color = Color.White.copy(alpha = 0.45f * mainFaceAlpha),
                                style = Stroke(width = 5f)
                            )

                            // Strictly clipped diagonal light shine bar
                            if (logoSheenAlpha > 0.005f) {
                                withTransform({
                                    translate(left = logoSheenX, top = 0f)
                                    rotate(degrees = 24f, pivot = Offset(220f, 510f))
                                }) {
                                    val sheenBrush = Brush.horizontalGradient(
                                        colorStops = arrayOf(
                                            0.0f to Color.White.copy(alpha = 0f),
                                            0.5f to Color.White.copy(alpha = 0.98f * logoSheenAlpha),
                                            1.0f to Color.White.copy(alpha = 0f)
                                        ),
                                        startX = 100f,
                                        endX = 340f
                                    )
                                    drawRect(
                                        brush = sheenBrush,
                                        topLeft = Offset(100f, 120f),
                                        size = Size(240f, 780f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Sharp Geometric Beveled Wordmark "scellion" with Clipped Reveal & Text Sheen
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = wmAlpha
                        translationX = with(density) { wmSlideDp.dp.toPx() }
                    }
                    .then(if (wmBlurDp > 0.3f) Modifier.blur(wmBlurDp.dp) else Modifier)
                    .drawWithContent {
                        clipRect(
                            left = -size.width * 0.10f,
                            top = -size.height * 0.15f,
                            right = size.width * wmClipRightFraction,
                            bottom = size.height * 1.15f
                        ) {
                            this@drawWithContent.drawContent()
                        }
                    }
                    .padding(bottom = 2.dp)
            ) {
                val baseWordmarkStyle = TextStyle(
                    fontFamily = OscellionBrandFont,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 46.sp,
                    letterSpacing = (46f * wmTrackingEm).sp
                )

                // Top-left bright bevel edge
                Text(
                    text = "scellion",
                    style = baseWordmarkStyle.copy(color = Color.White.copy(alpha = 0.95f)),
                    modifier = Modifier.offset(x = (-1).dp, y = (-1).dp)
                )

                // Bottom-right dark bevel depth
                Text(
                    text = "scellion",
                    style = baseWordmarkStyle.copy(color = Color(0xE65A5A60)),
                    modifier = Modifier.offset(x = 1.dp, y = 1.dp)
                )

                // Main vertical white-to-silver gradient face
                Text(
                    text = "scellion",
                    style = baseWordmarkStyle.copy(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.White,
                                0.48f to Color(0xFFF7F7F9),
                                0.82f to Color(0xFFD4D4DA),
                                1.00f to Color(0xFFA8A8B0)
                            )
                        )
                    )
                )

                // Text-clipped diagonal sheen continuation
                if (textSheenAlpha > 0.005f) {
                    val widthPx = with(density) { 210.dp.toPx() }
                    val center = widthPx * textSheenPos
                    val halfBand = widthPx * 0.24f
                    Text(
                        text = "scellion",
                        style = baseWordmarkStyle.copy(
                            brush = Brush.linearGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Transparent,
                                    0.5f to Color.White.copy(alpha = 0.98f * textSheenAlpha),
                                    1.0f to Color.Transparent
                                ),
                                start = Offset(center - halfBand, 0f),
                                end = Offset(center + halfBand, widthPx * 0.28f)
                            )
                        )
                    )
                }
            }
        }

        // Subtle bottom skip hint
        Text(
            text = "TAP ANYWHERE TO SKIP",
            fontFamily = OscellionBodyFont,
            fontSize = 11.sp,
            letterSpacing = 1.8.sp,
            color = Color.White.copy(alpha = 0.25f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 26.dp)
        )
    }
}

private data class OscellionEmblemPaths(
    val combinedEvenOdd: Path,
    val outerContour: Path,
    val innerHole: Path
)

/**
 * Parses the exact SVG path geometry of the Oscellion "O" brand emblem into Compose Paths.
 */
private fun parseOscellionEmblemPaths(): OscellionEmblemPaths {
    val outerSvg =
        "M 512 212 A 300 300 0 0 1 812 512 A 300 300 0 0 1 512 812 A 300 300 0 0 1 299.87 724.13 L 148.5 771 Q 120 780 136.5 755.1 L 230.24 615 A 300 300 0 0 1 512 212 Z"
    val innerSvg =
        "M 512 342 A 170 170 0 0 0 512 682 A 170 170 0 0 0 512 342 Z"
    val combinedSvg = "$outerSvg $innerSvg"

    return try {
        val combined = PathParser().parsePathString(combinedSvg).toPath().apply {
            fillType = PathFillType.EvenOdd
        }
        val outer = PathParser().parsePathString(outerSvg).toPath()
        val inner = PathParser().parsePathString(innerSvg).toPath()
        OscellionEmblemPaths(combined, outer, inner)
    } catch (_: Exception) {
        // Safe geometric fallback if path parsing is ever interrupted
        val fallback = Path().apply {
            fillType = PathFillType.EvenOdd
            addOval(Rect(212f, 212f, 812f, 812f))
            addOval(Rect(342f, 342f, 682f, 682f))
        }
        OscellionEmblemPaths(fallback, fallback, Path())
    }
}

private fun DrawScope.drawPartialContour(
    source: Path,
    fraction: Float,
    color: Color,
    stroke: Stroke
) {
    if (fraction <= 0f) return
    val measure = PathMeasure()
    measure.setPath(source, false)
    val partial = Path()
    measure.getSegment(0f, measure.length * fraction.coerceIn(0f, 1f), partial, true)
    drawPath(path = partial, color = color, style = stroke)
}

// =============================================================================
// LIQUID-MORPHING LAYERED VECTOR WAVES BACKGROUND
// =============================================================================

/**
 * Exact Compose Canvas recreation of the top-left and bottom-right layered satin blue waves,
 * featuring multi-phase harmonic Bezier morphing, anchored corner breathing, and pulsing radial glows.
 */
@Composable
private fun AnimatedOscellionWaves(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "oscellionWaves")

    // Staggered harmonic morph clocks matching the SVG <animate> durations
    val tlSoftClock by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(12500, easing = androidx.compose.animation.core.LinearEasing)),
        label = "tlSoft"
    )
    val tlOuterClock by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(10200, easing = androidx.compose.animation.core.LinearEasing)),
        label = "tlOuter"
    )
    val tlInnerClock by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8200, easing = androidx.compose.animation.core.LinearEasing)),
        label = "tlInner"
    )
    val brOuterClock by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(11200, easing = androidx.compose.animation.core.LinearEasing)),
        label = "brOuter"
    )
    val brInnerClock by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8800, easing = androidx.compose.animation.core.LinearEasing)),
        label = "brInner"
    )

    // Anchored corner breathing & ambient radial glow pulsing
    val tlBreath by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(12000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tlBreath"
    )
    val brBreath by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(13500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "brBreath"
    )
    val glowPulse by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(9500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    Box(modifier = modifier) {
        // Ambient Top-Left & Bottom-Right Radial Washes
        Canvas(modifier = Modifier.fillMaxSize()) {
            val tlRadius = size.minDimension * lerpFloat(0.62f, 0.67f, glowPulse)
            val tlCenter = Offset(
                x = lerpFloat(0f, 18.dp.toPx(), glowPulse),
                y = lerpFloat(0f, 14.dp.toPx(), glowPulse)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to Color(0xFF93C5FD).copy(alpha = lerpFloat(0.24f, 0.30f, glowPulse)),
                        0.48f to Color(0xFFDBEAFE).copy(alpha = 0.14f),
                        0.76f to Color.Transparent
                    ),
                    center = tlCenter,
                    radius = tlRadius
                ),
                radius = tlRadius,
                center = tlCenter
            )

            val brRadius = size.minDimension * lerpFloat(0.54f, 0.59f, 1f - glowPulse)
            val brCenter = Offset(
                x = size.width - lerpFloat(0f, 16.dp.toPx(), 1f - glowPulse),
                y = size.height - lerpFloat(0f, 14.dp.toPx(), 1f - glowPulse)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to Color(0xFF93C5FD).copy(alpha = lerpFloat(0.20f, 0.26f, 1f - glowPulse)),
                        0.52f to Color(0xFFDBEAFE).copy(alpha = 0.10f),
                        0.76f to Color.Transparent
                    ),
                    center = brCenter,
                    radius = brRadius
                ),
                radius = brRadius,
                center = brCenter
            )
        }

        // Top-Left Layered Sweeping Blue Ribbons (exact 360x300 SVG coordinate space)
        Canvas(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(0.84f)
                .widthIn(max = 440.dp)
                .aspectRatio(360f / 300f)
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = lerpFloat(1.0f, 1.035f, tlBreath)
                    scaleY = lerpFloat(1.0f, 1.028f, tlBreath)
                    rotationZ = lerpFloat(-0.4f, 0.65f, tlBreath)
                }
        ) {
            val sx = size.width / 360f
            val sy = size.height / 300f

            withTransform({ scale(scaleX = sx, scaleY = sy, pivot = Offset.Zero) }) {
                // Layer 1: Soft diffusing mist under outer curve
                val softPath = Path().apply {
                    moveTo(-40f, -40f)
                    lineTo(evalThreePhaseLoop(tlSoftClock, 0.36f, 0.72f, 310f, 330f, 294f), -40f)
                    cubicTo(
                        evalThreePhaseLoop(tlSoftClock, 0.36f, 0.72f, 285f, 298f, 268f),
                        evalThreePhaseLoop(tlSoftClock, 0.36f, 0.72f, 95f, 114f, 84f),
                        evalThreePhaseLoop(tlSoftClock, 0.36f, 0.72f, 232f, 248f, 216f),
                        evalThreePhaseLoop(tlSoftClock, 0.36f, 0.72f, 162f, 178f, 154f),
                        evalThreePhaseLoop(tlSoftClock, 0.36f, 0.72f, 140f, 156f, 126f),
                        evalThreePhaseLoop(tlSoftClock, 0.36f, 0.72f, 202f, 216f, 192f)
                    )
                    cubicTo(
                        evalThreePhaseLoop(tlSoftClock, 0.36f, 0.72f, 82f, 92f, 72f),
                        evalThreePhaseLoop(tlSoftClock, 0.36f, 0.72f, 227f, 241f, 216f),
                        evalThreePhaseLoop(tlSoftClock, 0.36f, 0.72f, 36f, 44f, 28f),
                        evalThreePhaseLoop(tlSoftClock, 0.36f, 0.72f, 250f, 264f, 240f),
                        -40f,
                        evalThreePhaseLoop(tlSoftClock, 0.36f, 0.72f, 296f, 312f, 284f)
                    )
                    close()
                }
                drawPath(
                    path = softPath,
                    brush = Brush.linearGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0xB8D9E8FF),
                            0.55f to Color(0x7AE7F0FF),
                            1.00f to Color(0x00F6F9FF)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(260f, 270f)
                    )
                )

                // Layer 2: Outer pale-blue sweeping arc
                val outerPath = Path().apply {
                    moveTo(-40f, -40f)
                    lineTo(evalThreePhaseLoop(tlOuterClock, 0.33f, 0.67f, 262f, 278f, 248f), -40f)
                    cubicTo(
                        evalThreePhaseLoop(tlOuterClock, 0.33f, 0.67f, 242f, 254f, 226f),
                        evalThreePhaseLoop(tlOuterClock, 0.33f, 0.67f, 78f, 96f, 68f),
                        evalThreePhaseLoop(tlOuterClock, 0.33f, 0.67f, 195f, 208f, 180f),
                        evalThreePhaseLoop(tlOuterClock, 0.33f, 0.67f, 136f, 150f, 126f),
                        evalThreePhaseLoop(tlOuterClock, 0.33f, 0.67f, 105f, 120f, 92f),
                        evalThreePhaseLoop(tlOuterClock, 0.33f, 0.67f, 170f, 184f, 160f)
                    )
                    cubicTo(
                        evalThreePhaseLoop(tlOuterClock, 0.33f, 0.67f, 54f, 66f, 44f),
                        evalThreePhaseLoop(tlOuterClock, 0.33f, 0.67f, 190f, 205f, 180f),
                        evalThreePhaseLoop(tlOuterClock, 0.33f, 0.67f, 22f, 28f, 15f),
                        evalThreePhaseLoop(tlOuterClock, 0.33f, 0.67f, 206f, 220f, 196f),
                        -40f,
                        evalThreePhaseLoop(tlOuterClock, 0.33f, 0.67f, 242f, 258f, 230f)
                    )
                    close()
                }
                drawPath(
                    path = outerPath,
                    brush = Brush.linearGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0xC7C8E0FF),
                            0.52f to Color(0x9EDCEBFF),
                            1.00f to Color(0x47EEF5FF)
                        ),
                        start = Offset(15f, 0f),
                        end = Offset(245f, 210f)
                    )
                )

                // Layer 3: Inner richer sky-blue sweeping arc + corner depth sheen
                val innerPath = Path().apply {
                    moveTo(-40f, -40f)
                    lineTo(evalThreePhaseLoop(tlInnerClock, 0.34f, 0.68f, 194f, 212f, 182f), -40f)
                    cubicTo(
                        evalThreePhaseLoop(tlInnerClock, 0.34f, 0.68f, 178f, 192f, 164f),
                        evalThreePhaseLoop(tlInnerClock, 0.34f, 0.68f, 72f, 86f, 62f),
                        evalThreePhaseLoop(tlInnerClock, 0.34f, 0.68f, 135f, 146f, 122f),
                        evalThreePhaseLoop(tlInnerClock, 0.34f, 0.68f, 126f, 138f, 118f),
                        evalThreePhaseLoop(tlInnerClock, 0.34f, 0.68f, 58f, 72f, 46f),
                        evalThreePhaseLoop(tlInnerClock, 0.34f, 0.68f, 158f, 172f, 150f)
                    )
                    cubicTo(
                        evalThreePhaseLoop(tlInnerClock, 0.34f, 0.68f, 30f, 39f, 22f),
                        evalThreePhaseLoop(tlInnerClock, 0.34f, 0.68f, 170f, 185f, 162f),
                        evalThreePhaseLoop(tlInnerClock, 0.34f, 0.68f, 13f, 19f, 8f),
                        evalThreePhaseLoop(tlInnerClock, 0.34f, 0.68f, 182f, 197f, 175f),
                        -40f,
                        evalThreePhaseLoop(tlInnerClock, 0.34f, 0.68f, 212f, 226f, 203f)
                    )
                    close()
                }
                drawPath(
                    path = innerPath,
                    brush = Brush.linearGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0xEB9BC5FF),
                            0.45f to Color(0xE094C0FF),
                            0.82f to Color(0xD1B8D5FF),
                            1.00f to Color(0xADCFE3FF)
                        ),
                        start = Offset(-10f, 10f),
                        end = Offset(180f, 175f)
                    )
                )
                drawPath(
                    path = innerPath,
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0x7A7BB1FF),
                            0.60f to Color(0x389BC6FF),
                            1.00f to Color(0x00C7DFFF)
                        ),
                        center = Offset(28f, 62f),
                        radius = 175f
                    )
                )
            }
        }

        // Bottom-Right Layered Sweeping Blue Ribbons (exact 320x260 SVG coordinate space)
        Canvas(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .fillMaxWidth(0.70f)
                .widthIn(max = 360.dp)
                .aspectRatio(320f / 260f)
                .graphicsLayer {
                    transformOrigin = TransformOrigin(1f, 1f)
                    scaleX = lerpFloat(1.0f, 1.04f, brBreath)
                    scaleY = lerpFloat(1.0f, 1.032f, brBreath)
                    rotationZ = lerpFloat(0.55f, -0.75f, brBreath)
                }
        ) {
            val sx = size.width / 320f
            val sy = size.height / 260f

            withTransform({ scale(scaleX = sx, scaleY = sy, pivot = Offset.Zero) }) {
                // Layer 1: Outer translucent sweeping curve
                val brOuterPath = Path().apply {
                    moveTo(360f, 300f)
                    lineTo(360f, evalThreePhaseLoop(brOuterClock, 0.34f, 0.68f, 30f, 16f, 44f))
                    cubicTo(
                        evalThreePhaseLoop(brOuterClock, 0.34f, 0.68f, 295f, 282f, 306f),
                        evalThreePhaseLoop(brOuterClock, 0.34f, 0.68f, 48f, 38f, 62f),
                        evalThreePhaseLoop(brOuterClock, 0.34f, 0.68f, 256f, 240f, 268f),
                        evalThreePhaseLoop(brOuterClock, 0.34f, 0.68f, 88f, 80f, 100f),
                        evalThreePhaseLoop(brOuterClock, 0.34f, 0.68f, 228f, 212f, 240f),
                        evalThreePhaseLoop(brOuterClock, 0.34f, 0.68f, 144f, 136f, 154f)
                    )
                    cubicTo(
                        evalThreePhaseLoop(brOuterClock, 0.34f, 0.68f, 202f, 186f, 214f),
                        evalThreePhaseLoop(brOuterClock, 0.34f, 0.68f, 197f, 190f, 206f),
                        evalThreePhaseLoop(brOuterClock, 0.34f, 0.68f, 156f, 140f, 168f),
                        evalThreePhaseLoop(brOuterClock, 0.34f, 0.68f, 232f, 226f, 238f),
                        evalThreePhaseLoop(brOuterClock, 0.34f, 0.68f, 56f, 40f, 74f),
                        266f
                    )
                    lineTo(evalThreePhaseLoop(brOuterClock, 0.34f, 0.68f, 44f, 28f, 62f), 300f)
                    close()
                }
                drawPath(
                    path = brOuterPath,
                    brush = Brush.linearGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0x9EC9E0FF),
                            0.55f to Color(0x94DCEBFF),
                            1.00f to Color(0x38EEF5FF)
                        ),
                        start = Offset(315f, 65f),
                        end = Offset(80f, 255f)
                    )
                )

                // Layer 2: Inner vibrant satin blue ribbon + corner depth
                val brInnerPath = Path().apply {
                    moveTo(360f, 300f)
                    lineTo(360f, evalThreePhaseLoop(brInnerClock, 0.35f, 0.70f, 96f, 80f, 110f))
                    cubicTo(
                        evalThreePhaseLoop(brInnerClock, 0.35f, 0.70f, 296f, 282f, 308f),
                        evalThreePhaseLoop(brInnerClock, 0.35f, 0.70f, 116f, 104f, 128f),
                        evalThreePhaseLoop(brInnerClock, 0.35f, 0.70f, 258f, 244f, 270f),
                        evalThreePhaseLoop(brInnerClock, 0.35f, 0.70f, 150f, 140f, 160f),
                        evalThreePhaseLoop(brInnerClock, 0.35f, 0.70f, 232f, 218f, 244f),
                        evalThreePhaseLoop(brInnerClock, 0.35f, 0.70f, 194f, 184f, 204f)
                    )
                    cubicTo(
                        evalThreePhaseLoop(brInnerClock, 0.35f, 0.70f, 211f, 196f, 224f),
                        evalThreePhaseLoop(brInnerClock, 0.35f, 0.70f, 229f, 222f, 238f),
                        evalThreePhaseLoop(brInnerClock, 0.35f, 0.70f, 188f, 172f, 200f),
                        evalThreePhaseLoop(brInnerClock, 0.35f, 0.70f, 248f, 242f, 254f),
                        evalThreePhaseLoop(brInnerClock, 0.35f, 0.70f, 146f, 128f, 162f),
                        268f
                    )
                    lineTo(evalThreePhaseLoop(brInnerClock, 0.35f, 0.70f, 136f, 118f, 152f), 300f)
                    close()
                }
                drawPath(
                    path = brInnerPath,
                    brush = Brush.linearGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0xD1A8CEFF),
                            0.48f to Color(0xE68BBDFD),
                            1.00f to Color(0xF276AEFF)
                        ),
                        start = Offset(320f, 130f),
                        end = Offset(155f, 260f)
                    )
                )
                drawPath(
                    path = brInnerPath,
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0x7369A5FF),
                            1.00f to Color(0x00A3CBFF)
                        ),
                        center = Offset(290f, 245f),
                        radius = 165f
                    )
                )
            }
        }
    }
}

// =============================================================================
// STAGGERED ENTRANCE WRAPPER
// =============================================================================

@Composable
private fun StaggeredAuthStage(
    visible: Boolean,
    delayMillis: Int,
    content: @Composable () -> Unit
) {
    val ease = remember { CubicBezierEasing(0.16f, 1f, 0.3f, 1f) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 680, delayMillis = if (visible) delayMillis else 0, easing = ease),
        label = "stageAlpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 16f,
        animationSpec = tween(durationMillis = 680, delayMillis = if (visible) delayMillis else 0, easing = ease),
        label = "stageOffsetY"
    )
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                translationY = with(density) { offsetY.dp.toPx() }
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

// =============================================================================
// PIXEL-PERFECT UI COMPONENTS
// =============================================================================

/**
 * Elevated white "Continue with Google" pill with 4-color vector G, vertical divider, and chevron.
 */
@Composable
private fun GoogleAuthPillButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.988f else 1f,
        animationSpec = tween(140),
        label = "googleScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .softPillShadow(
                color = OscellionColors.GoogleShadow,
                blurRadius = 18.dp,
                offsetY = 8.dp
            )
            .clip(CircleShape)
            .background(Color.White)
            .border(1.dp, OscellionColors.GoogleBorder, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Official 4-Color Google G Vector
        Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            GoogleGVectorIcon(modifier = Modifier.size(23.dp))
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Crisp Vertical Divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(30.dp)
                .background(OscellionColors.GoogleDivider)
        )

        // Centered Label
        Text(
            text = "Continue with Google",
            fontFamily = OscellionBodyFont,
            fontWeight = FontWeight.Medium,
            fontSize = 16.5.sp,
            letterSpacing = (-0.15).sp,
            color = OscellionColors.ButtonLabel,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .padding(end = 6.dp)
        )

        // Right Chevron
        ChevronRightIcon(color = OscellionColors.ChevronTint)
    }
}

/**
 * Soft cool-gray pill input matching the exact height, radius, outline icons, and animated blue focus ring.
 */
@Composable
private fun OscellionPillInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: @Composable () -> Unit,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    var isFocused by remember { mutableStateOf(false) }

    val bgColor by animateColorAsState(
        targetValue = if (isFocused) Color.White else OscellionColors.InputBg,
        animationSpec = tween(180),
        label = "inputBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) OscellionColors.FocusBlue else OscellionColors.InputBorder,
        animationSpec = tween(180),
        label = "inputBorder"
    )
    val ringAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(180),
        label = "focusRing"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .drawBehind {
                if (ringAlpha > 0.01f) {
                    val ringStroke = 4.dp.toPx()
                    drawRoundRect(
                        color = OscellionColors.FocusRing.copy(alpha = 0.12f * ringAlpha),
                        topLeft = Offset(-ringStroke, -ringStroke),
                        size = Size(size.width + ringStroke * 2f, size.height + ringStroke * 2f),
                        cornerRadius = CornerRadius(size.height / 2f + ringStroke)
                    )
                }
            }
            .clip(CircleShape)
            .background(bgColor)
            .border(1.dp, borderColor, CircleShape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 19.dp, end = if (trailingIcon != null) 8.dp else 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                leadingIcon()
            }

            Spacer(modifier = Modifier.width(12.dp))

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                cursorBrush = SolidColor(OscellionColors.FocusBlue),
                textStyle = TextStyle(
                    fontFamily = OscellionBodyFont,
                    fontSize = 16.sp,
                    color = OscellionColors.TextPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { isFocused = it.isFocused },
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                fontFamily = OscellionBodyFont,
                                fontSize = 16.sp,
                                color = OscellionColors.InputPlaceholder
                            )
                        }
                        innerTextField()
                    }
                }
            )

            if (trailingIcon != null) {
                trailingIcon()
            }
        }
    }
}

/**
 * Primary electric-blue vertical gradient pill CTA button with soft blue glow and right arrow.
 */
@Composable
private fun PrimaryElectricCtaButton(
    label: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.988f else 1f,
        animationSpec = tween(140),
        label = "ctaScale"
    )

    val gradient = remember {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to OscellionColors.CtaTop,
                0.52f to OscellionColors.CtaMid,
                1.00f to OscellionColors.CtaBottom
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .softPillShadow(
                color = OscellionColors.CtaShadow,
                blurRadius = 16.dp,
                offsetY = 8.dp
            )
            .clip(CircleShape)
            .background(gradient)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = OscellionBodyFont,
            fontWeight = FontWeight.Medium,
            fontSize = 17.sp,
            letterSpacing = (-0.15).sp,
            color = Color.White
        )
        Box(
            modifier = Modifier.align(Alignment.CenterEnd),
            contentAlignment = Alignment.Center
        ) {
            ArrowRightIcon(color = Color.White)
        }
    }
}

/**
 * Secondary soft pill "Continue without Account" button with right chevron.
 */
@Composable
private fun ContinueGuestPillButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.988f else 1f,
        animationSpec = tween(140),
        label = "guestScale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isPressed) OscellionColors.GuestBgPressed else OscellionColors.GuestBg,
        animationSpec = tween(140),
        label = "guestBg"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(bgColor)
            .border(1.dp, OscellionColors.GuestBorder, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Continue without Account",
            fontFamily = OscellionBodyFont,
            fontWeight = FontWeight.Medium,
            fontSize = 16.5.sp,
            letterSpacing = (-0.15).sp,
            color = OscellionColors.ButtonLabel
        )
        Box(
            modifier = Modifier.align(Alignment.CenterEnd),
            contentAlignment = Alignment.Center
        ) {
            ChevronRightIcon(color = OscellionColors.ChevronTint)
        }
    }
}

/**
 * Subtle translucent top-right pill to replay the cinematic logo reveal anytime.
 */
@Composable
private fun ReplayIntroButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0.45f,
        animationSpec = tween(160),
        label = "replayAlpha"
    )

    Row(
        modifier = modifier
            .alpha(buttonAlpha)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.78f))
            .border(1.dp, Color(0xFFE2E8F4), CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        ReplayVectorIcon(color = OscellionColors.TaglineText)
        Text(
            text = "Replay Intro",
            fontFamily = OscellionBodyFont,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = OscellionColors.TaglineText
        )
    }
}

// =============================================================================
// FLOATING TOAST & LEGAL MODAL DIALOG
// =============================================================================

@Composable
private fun OscellionToastHost(
    toastState: ToastState?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = toastState != null,
        enter = fadeIn(tween(240)) + slideInVertically(tween(260)) { -it / 2 },
        exit = fadeOut(tween(220)) + slideOutVertically(tween(220)) { -it / 2 },
        modifier = modifier
    ) {
        toastState?.let { toast ->
            val bg = when (toast.type) {
                ToastType.SUCCESS -> OscellionColors.ToastSuccessBg
                ToastType.ERROR -> OscellionColors.ToastErrorBg
                ToastType.INFO -> OscellionColors.ToastInfoBg
            }
            val border = when (toast.type) {
                ToastType.SUCCESS -> Color(0x3360A5FA)
                ToastType.ERROR -> Color(0x33FB7185)
                ToastType.INFO -> Color(0x1AFFFFFF)
            }

            Row(
                modifier = Modifier
                    .widthIn(max = 380.dp)
                    .softPillShadow(Color(0x33050814), blurRadius = 18.dp, offsetY = 6.dp)
                    .clip(CircleShape)
                    .background(bg)
                    .border(1.dp, border, CircleShape)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ToastStatusIcon(type = toast.type)
                Text(
                    text = toast.message,
                    fontFamily = OscellionBodyFont,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color.White,
                    lineHeight = 19.sp
                )
            }
        }
    }
}

@Composable
private fun OscellionLegalDialog(
    type: LegalModalType,
    onDismiss: () -> Unit
) {
    val title = if (type == LegalModalType.TERMS) "Terms & Conditions" else "Privacy Policy"
    val paragraph1 = if (type == LegalModalType.TERMS) {
        "Welcome to Oscellion. By creating an account or exploring as a guest, you agree to use our creative platform responsibly and respectfully."
    } else {
        "Your privacy matters at Oscellion. We only collect essential account details needed to keep your profile secure and personalize your experience."
    }
    val paragraph2 = if (type == LegalModalType.TERMS) {
        "You retain ownership of everything you create and share on Oscellion while granting us permission to display your public content across the community."
    } else {
        "We never sell your personal information, and you can manage or delete your account data at any time from your settings."
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 22.dp)
                .widthIn(max = 400.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 20.dp
        ) {
            Column(
                modifier = Modifier
                    .border(1.dp, Color(0xFFE5EAF3), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        fontFamily = OscellionBrandFont,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = OscellionColors.BrandHeading
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable(role = Role.Button, onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        CloseXIcon(color = OscellionColors.InputIcon)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = paragraph1,
                    fontFamily = OscellionBodyFont,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = OscellionColors.PromptText
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = paragraph2,
                    fontFamily = OscellionBodyFont,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = OscellionColors.PromptText
                )

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(CircleShape)
                        .background(OscellionColors.CtaMid)
                        .clickable(role = Role.Button, onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Got it",
                        fontFamily = OscellionBodyFont,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// =============================================================================
// CUSTOM VECTOR GRAPHIC ICONS (Exact SVG Geometry — Zero External Asset Deps)
// =============================================================================

@Composable
private fun GoogleGVectorIcon(modifier: Modifier = Modifier) {
    val paths = remember {
        listOf(
            OscellionColors.GoogleBlue to PathParser().parsePathString(
                "M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
            ).toPath(),
            OscellionColors.GoogleGreen to PathParser().parsePathString(
                "M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
            ).toPath(),
            OscellionColors.GoogleYellow to PathParser().parsePathString(
                "M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
            ).toPath(),
            OscellionColors.GoogleRed to PathParser().parsePathString(
                "M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
            ).toPath()
        )
    }

    Canvas(modifier = modifier) {
        val scale = min(size.width, size.height) / 24f
        withTransform({ scale(scale, scale, Offset.Zero) }) {
            paths.forEach { (color, path) -> drawPath(path = path, color = color) }
        }
    }
}

@Composable
private fun UserOutlineIcon(
    color: Color = OscellionColors.InputIcon,
    modifier: Modifier = Modifier.size(21.dp)
) {
    val shouldersPath = remember {
        PathParser().parsePathString("M4.8 19.6c0-3.3 3.2-5.4 7.2-5.4s7.2 2.1 7.2 5.4").toPath()
    }
    Canvas(modifier = modifier) {
        val s = min(size.width, size.height) / 24f
        val stroke = Stroke(width = 1.65f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        withTransform({ scale(s, s, Offset.Zero) }) {
            drawCircle(color = color, radius = 3.6f, center = Offset(12f, 8f), style = stroke)
            drawPath(path = shouldersPath, color = color, style = stroke)
        }
    }
}

@Composable
private fun LockOutlineIcon(
    color: Color = OscellionColors.InputIcon,
    modifier: Modifier = Modifier.size(21.dp)
) {
    val shacklePath = remember {
        PathParser().parsePathString("M8.2 10.5V7.8a3.8 3.8 0 0 1 7.6 0v2.7").toPath()
    }
    Canvas(modifier = modifier) {
        val s = min(size.width, size.height) / 24f
        val stroke = Stroke(width = 1.65f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        withTransform({ scale(s, s, Offset.Zero) }) {
            drawRoundRect(
                color = color,
                topLeft = Offset(5f, 10.5f),
                size = Size(14f, 9.5f),
                cornerRadius = CornerRadius(2.6f, 2.6f),
                style = stroke
            )
            drawPath(path = shacklePath, color = color, style = stroke)
        }
    }
}

@Composable
private fun EyeOutlineIcon(
    isVisible: Boolean,
    color: Color = OscellionColors.InputIcon,
    modifier: Modifier = Modifier.size(21.dp)
) {
    val eyeOpenOuter = remember {
        PathParser().parsePathString("M2.2 12s3.6-6.2 9.8-6.2S21.8 12 21.8 12s-3.6 6.2-9.8 6.2S2.2 12 2.2 12z").toPath()
    }
    val eyeOffArc1 = remember {
        PathParser().parsePathString("M10.58 10.58a2.8 2.8 0 0 0 3.84 3.84").toPath()
    }
    val eyeOffArc2 = remember {
        PathParser().parsePathString("M9.36 5.95A10.1 10.1 0 0 1 12 5.8c6.2 0 9.8 6.2 9.8 6.2a16.8 16.8 0 0 1-3.18 3.92").toPath()
    }
    val eyeOffArc3 = remember {
        PathParser().parsePathString("M6.42 7.55A16.5 16.5 0 0 0 2.2 12s3.6 6.2 9.8 6.2c1.28 0 2.47-.24 3.55-.65").toPath()
    }

    Canvas(modifier = modifier) {
        val s = min(size.width, size.height) / 24f
        val stroke = Stroke(width = 1.65f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        withTransform({ scale(s, s, Offset.Zero) }) {
            if (!isVisible) {
                drawPath(path = eyeOpenOuter, color = color, style = stroke)
                drawCircle(color = color, radius = 2.8f, center = Offset(12f, 12f), style = stroke)
            } else {
                drawPath(path = eyeOffArc1, color = color, style = stroke)
                drawPath(path = eyeOffArc2, color = color, style = stroke)
                drawPath(path = eyeOffArc3, color = color, style = stroke)
                drawLine(
                    color = color,
                    start = Offset(3.2f, 3.2f),
                    end = Offset(20.8f, 20.8f),
                    strokeWidth = 1.65f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun ChevronRightIcon(
    color: Color,
    modifier: Modifier = Modifier.size(18.dp)
) {
    val path = remember { PathParser().parsePathString("M9 18l6-6-6-6").toPath() }
    Canvas(modifier = modifier) {
        val s = min(size.width, size.height) / 24f
        withTransform({ scale(s, s, Offset.Zero) }) {
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 2.0f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

@Composable
private fun ArrowRightIcon(
    color: Color,
    modifier: Modifier = Modifier.size(20.dp)
) {
    val head = remember { PathParser().parsePathString("M13 6l6 6-6 6").toPath() }
    Canvas(modifier = modifier) {
        val s = min(size.width, size.height) / 24f
        val stroke = Stroke(width = 2.0f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        withTransform({ scale(s, s, Offset.Zero) }) {
            drawLine(color = color, start = Offset(5f, 12f), end = Offset(19f, 12f), strokeWidth = 2.0f, cap = StrokeCap.Round)
            drawPath(path = head, color = color, style = stroke)
        }
    }
}

@Composable
private fun ReplayVectorIcon(
    color: Color,
    modifier: Modifier = Modifier.size(14.dp)
) {
    val path = remember {
        PathParser().parsePathString(
            "M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
        ).toPath()
    }
    Canvas(modifier = modifier) {
        val s = min(size.width, size.height) / 24f
        withTransform({ scale(s, s, Offset.Zero) }) {
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 2.0f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

@Composable
private fun CloseXIcon(
    color: Color,
    modifier: Modifier = Modifier.size(18.dp)
) {
    Canvas(modifier = modifier) {
        val s = min(size.width, size.height) / 24f
        withTransform({ scale(s, s, Offset.Zero) }) {
            drawLine(color, Offset(6f, 6f), Offset(18f, 18f), strokeWidth = 2f, cap = StrokeCap.Round)
            drawLine(color, Offset(18f, 6f), Offset(6f, 18f), strokeWidth = 2f, cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun ToastStatusIcon(
    type: ToastType,
    modifier: Modifier = Modifier.size(16.dp)
) {
    val checkPath = remember { PathParser().parsePathString("M20 6L9 17l-5-5").toPath() }
    Canvas(modifier = modifier) {
        val s = min(size.width, size.height) / 24f
        val stroke = Stroke(width = 2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        withTransform({ scale(s, s, Offset.Zero) }) {
            when (type) {
                ToastType.SUCCESS -> {
                    drawPath(checkPath, OscellionColors.ToastSuccessIcon, style = stroke)
                }
                ToastType.ERROR -> {
                    drawCircle(OscellionColors.ToastErrorIcon, radius = 9f, center = Offset(12f, 12f), style = stroke)
                    drawLine(OscellionColors.ToastErrorIcon, Offset(12f, 8f), Offset(12f, 12f), 2.4f, StrokeCap.Round)
                    drawCircle(OscellionColors.ToastErrorIcon, radius = 1.1f, center = Offset(12f, 16f))
                }
                ToastType.INFO -> {
                    drawCircle(OscellionColors.ToastInfoIcon, radius = 9f, center = Offset(12f, 12f), style = stroke)
                    drawLine(OscellionColors.ToastInfoIcon, Offset(12f, 16f), Offset(12f, 12f), 2.2f, StrokeCap.Round)
                    drawCircle(OscellionColors.ToastInfoIcon, radius = 1.1f, center = Offset(12f, 8f))
                }
            }
        }
    }
}

// =============================================================================
// ANDROID STUDIO PREVIEWS
// =============================================================================

@Preview(
    name = "Oscellion Login Screen",
    showBackground = true,
    showSystemUi = true,
    device = "spec:width=412dp,height=915dp,dpi=420"
)
@Composable
private fun PreviewOscellionAuthScreen() {
    OscellionAuthScreen(playIntroOnStart = false)
}

@Preview(
    name = "Oscellion Cinematic Intro + Login",
    showBackground = true,
    showSystemUi = true,
    device = "spec:width=412dp,height=915dp,dpi=420"
)
@Composable
private fun PreviewOscellionWithSplash() {
    OscellionAuthScreen(playIntroOnStart = true)
}
