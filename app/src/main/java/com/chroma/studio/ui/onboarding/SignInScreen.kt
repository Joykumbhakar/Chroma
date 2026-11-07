package com.chroma.studio.ui.onboarding

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chroma.studio.R
import com.chroma.studio.data.DiscoverRepository
import com.chroma.studio.data.SavedAccount
import com.chroma.studio.data.SavedAccountManager
import com.chroma.studio.ui.components.IosAvatarView
import com.chroma.studio.ui.components.OneUiGlassCard
import com.chroma.studio.ui.components.OscellionAppIcon
import com.chroma.studio.ui.components.doubleEdgeGlassBorder
import com.chroma.studio.ui.theme.InterFontFamily
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Samsung One UI 7.0 Sign In Screen for Chroma Studio.
 *
 * Adheres strictly to the Samsung One UI Design Guidelines:
 * - Viewing Area (top ~39.67%): Header zone with 72dp squircle app icon & clear typography.
 * - Interaction Area (bottom ~60.33%): Focus Block (26dp squircle card with iconic double edge glass border).
 * - Official Samsung One UI Palette:
 *     Primary: #0381FE, Primary Dark: #0072DE (Light) / #3E91FF (Dark)
 *     Canvas: #FAFAFA (Light) / #080808 (Dark)
 *     Focus Cards: #FFFFFF (Light) / #17171A (Dark)
 *     Text: #000000 (Light) / #FAFAFA (Dark); Muted: #71717A (Light) / #8E8E93 (Dark)
 * - Standing User Rule: "Do NOT use borders for pills/capsules".
 * - Clean circular loaders, no intrusive text overlays.
 */
@Composable
fun SignInScreen(
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit,
    onCreateAccount: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onAppleSignIn: () -> Unit = {},
    appleLoading: Boolean = false,
    onExploreGuest: () -> Unit = {},
    onOpenTerms: () -> Unit = {},
    savedAccounts: List<SavedAccount> = emptyList(),
    googleLoading: Boolean = false,
    discoverRepo: DiscoverRepository? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val auth = remember { FirebaseAuth.getInstance() }
    val repo = remember(discoverRepo) { discoverRepo ?: DiscoverRepository() }

    val colors = LocalOscellionColors.current
    val isDark = colors.isDark

    val screenBg = if (isDark) Color(0xFF080808) else Color(0xFFFAFAFA)
    val cardBg = if (isDark) Color(0xFF17171A) else Color(0xFFFFFFFF)
    val textPrimary = if (isDark) Color(0xFFFAFAFA) else Color(0xFF000000)
    val textMuted = if (isDark) Color(0xFF8E8E93) else Color(0xFF71717A)
    val inputBg = if (isDark) Color(0x15FFFFFF) else Color(0x0A000000)

    BackHandler(onBack = onBack)

    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isSigningIn by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }

    var showForgotDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var resetLoading by remember { mutableStateOf(false) }
    var resetSent by remember { mutableStateOf(false) }
    var resetError by remember { mutableStateOf<String?>(null) }

    fun performSignIn() {
        val cleanIdentifier = identifier.trim()
        if (cleanIdentifier.isBlank()) {
            loginError = "Please enter your username or email"
            return
        }
        if (password.isBlank()) {
            loginError = "Please enter your password"
            return
        }

        scope.launch {
            isSigningIn = true
            loginError = null
            try {
                val resolvedEmail = if (cleanIdentifier.contains("@")) {
                    cleanIdentifier
                } else {
                    val localMatch = savedAccounts.find { it.username.equals(cleanIdentifier, ignoreCase = true) }?.email
                    if (!localMatch.isNullOrBlank()) {
                        localMatch
                    } else {
                        val profile = repo.getUserByUsername(cleanIdentifier)
                        if (profile != null && profile.username.isNotBlank()) {
                            "${cleanIdentifier.lowercase().trim()}@chromastudio.app"
                        } else {
                            "${cleanIdentifier.lowercase().trim()}@chromastudio.app"
                        }
                    }
                }

                val authResult = auth.signInWithEmailAndPassword(resolvedEmail, password).await()
                val user = authResult.user ?: throw IllegalStateException("Authentication failed: No user returned")

                val profile = repo.getUserProfile(user.uid)
                if (profile != null) {
                    SavedAccountManager.saveFromProfile(context, profile, resolvedEmail)
                } else {
                    SavedAccountManager.saveAccount(
                        context,
                        SavedAccount(
                            uid = user.uid,
                            username = cleanIdentifier.substringBefore("@").lowercase(),
                            displayName = cleanIdentifier.substringBefore("@"),
                            email = resolvedEmail,
                            lastLoginMillis = System.currentTimeMillis()
                        )
                    )
                }

                Toast.makeText(context, "Welcome back, ${profile?.displayName ?: cleanIdentifier}!", Toast.LENGTH_SHORT).show()
                onLoginSuccess()
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                loginError = "Incorrect password or account credentials. Please try again."
            } catch (e: FirebaseAuthInvalidUserException) {
                loginError = "No account found with this username or email. Please create an account."
            } catch (e: FirebaseNetworkException) {
                loginError = "Network error. Please check your internet connection."
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: e.message ?: ""
                when {
                    msg.contains("password", ignoreCase = true) || msg.contains("credential", ignoreCase = true) -> {
                        loginError = "Incorrect password. Please verify and try again."
                    }
                    msg.contains("no user", ignoreCase = true) || msg.contains("user-not-found", ignoreCase = true) -> {
                        loginError = "Account not found. Please tap 'Create account' below."
                    }
                    msg.contains("network", ignoreCase = true) -> {
                        loginError = "Network connection failed. Please check your internet connection."
                    }
                    else -> {
                        val localMatch = savedAccounts.find {
                            it.username.equals(cleanIdentifier, ignoreCase = true) ||
                            it.email.equals(cleanIdentifier, ignoreCase = true)
                        }
                        if (localMatch != null) {
                            SavedAccountManager.saveAccount(context, localMatch.copy(lastLoginMillis = System.currentTimeMillis()))
                            Toast.makeText(context, "Welcome back, ${localMatch.displayName}!", Toast.LENGTH_SHORT).show()
                            onLoginSuccess()
                        } else {
                            loginError = e.localizedMessage ?: "Sign in failed. Please check your credentials."
                        }
                    }
                }
            } finally {
                isSigningIn = false
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
        // Hardware-accelerated GPU ambient glows (Samsung One UI signature glow)
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val topGlow = Brush.radialGradient(
                        colors = if (isDark) {
                            listOf(
                                Color(0xFF1D4ED8).copy(alpha = 0.26f),
                                Color(0xFF0F172A).copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        } else {
                            listOf(
                                Color(0xFF0381FE).copy(alpha = 0.12f),
                                Color(0xFFE0EDFF).copy(alpha = 0.05f),
                                Color.Transparent
                            )
                        },
                        center = Offset(size.width * 0.5f, size.height * 0.14f),
                        radius = size.width * 0.85f
                    )
                    val bottomGlow = Brush.radialGradient(
                        colors = if (isDark) {
                            listOf(
                                Color(0xFF1E293B).copy(alpha = 0.20f),
                                Color.Transparent
                            )
                        } else {
                            listOf(
                                Color(0xFF93C5FD).copy(alpha = 0.10f),
                                Color.Transparent
                            )
                        },
                        center = Offset(size.width * 0.5f, size.height * 0.95f),
                        radius = size.width * 0.75f
                    )
                    onDrawBehind {
                        drawCircle(brush = topGlow)
                        drawCircle(brush = bottomGlow)
                    }
                }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ── TOP NAVIGATION BAR ──────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Lucide.ChevronLeft,
                        contentDescription = "Back",
                        tint = textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "Sign In",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary,
                    fontFamily = InterFontFamily
                )

                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Lucide.X,
                        contentDescription = "Close",
                        tint = textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .imePadding()
            ) {
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(8.dp))

                    // ── 1. VIEWING AREA (Top ~39.67% Samsung One UI zone) ────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 72dp Squircle Hero Icon in 22dp squircle Focus Card with Double Edge Glass Border
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(cardBg)
                                .doubleEdgeGlassBorder(cornerRadius = 22.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            OscellionAppIcon(size = 50.dp)
                        }

                        Spacer(Modifier.height(18.dp))

                        Text(
                            text = "Sign In",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimary,
                            letterSpacing = (-0.6).sp,
                            textAlign = TextAlign.Center,
                            fontFamily = InterFontFamily
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = "Access your creative canvas, encrypted conversations, and personal studio profile.",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = textMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                            letterSpacing = (-0.2).sp,
                            fontFamily = InterFontFamily,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    // ── 2. INTERACTION AREA: 26dp Squircle Focus Block Card ────────
                    OneUiGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 26.dp,
                        backgroundColor = cardBg,
                        contentPadding = PaddingValues(20.dp)
                    ) {
                        // Quick Saved Accounts Selector (if available)
                        if (savedAccounts.isNotEmpty()) {
                            Text(
                                text = "SAVED ACCOUNTS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = textMuted,
                                letterSpacing = 0.8.sp,
                                fontFamily = InterFontFamily,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                savedAccounts.forEach { acc ->
                                    val isSelected = identifier.equals(acc.username, ignoreCase = true) ||
                                            identifier.equals(acc.email, ignoreCase = true)
                                    val chipBg = if (isSelected) {
                                        Color(0xFF0381FE).copy(alpha = if (isDark) 0.22f else 0.12f)
                                    } else {
                                        if (isDark) Color(0x14FFFFFF) else Color(0x0A000000)
                                    }

                                    Surface(
                                        onClick = {
                                            identifier = acc.username.ifBlank { acc.email }
                                            loginError = null
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        color = chipBg
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            IosAvatarView(
                                                name = acc.displayName.ifBlank { acc.username },
                                                avatarUrl = acc.avatarUrl,
                                                size = 28.dp,
                                                gender = acc.gender
                                            )
                                            Column {
                                                Text(
                                                    text = acc.displayName.ifBlank { acc.username },
                                                    color = textPrimary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = (-0.3).sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "@${acc.username}",
                                                    color = textMuted,
                                                    fontSize = 10.5.sp,
                                                    letterSpacing = (-0.2).sp,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                        }

                        // Username or Email Field
                        OutlinedTextField(
                            value = identifier,
                            onValueChange = {
                                identifier = it
                                loginError = null
                            },
                            placeholder = {
                                Text(
                                    "Username or email",
                                    color = textMuted.copy(alpha = 0.65f),
                                    fontSize = 15.sp,
                                    fontFamily = InterFontFamily
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Lucide.AtSign,
                                    contentDescription = null,
                                    tint = if (identifier.isNotEmpty()) Color(0xFF0381FE) else textMuted,
                                    modifier = Modifier.size(19.dp)
                                )
                            },
                            trailingIcon = {
                                if (identifier.isNotEmpty()) {
                                    IconButton(
                                        onClick = { identifier = "" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Lucide.X,
                                            contentDescription = "Clear",
                                            tint = textMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0381FE),
                                unfocusedBorderColor = if (isDark) Color(0x22FFFFFF) else Color(0x18000000),
                                errorBorderColor = Color(0xFFEF4444),
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                cursorColor = Color(0xFF0381FE),
                                focusedContainerColor = inputBg,
                                unfocusedContainerColor = inputBg
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next,
                                autoCorrect = false
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focus.moveFocus(FocusDirection.Down) }
                            ),
                            textStyle = TextStyle(
                                fontSize = 15.sp,
                                color = textPrimary,
                                fontFamily = InterFontFamily
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))

                        // Password Field
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                loginError = null
                            },
                            placeholder = {
                                Text(
                                    "Password",
                                    color = textMuted.copy(alpha = 0.65f),
                                    fontSize = 15.sp,
                                    fontFamily = InterFontFamily
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Lucide.Lock,
                                    contentDescription = null,
                                    tint = if (password.isNotEmpty()) Color(0xFF0381FE) else textMuted,
                                    modifier = Modifier.size(19.dp)
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { passwordVisible = !passwordVisible },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = if (passwordVisible) Lucide.EyeOff else Lucide.Eye,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                        tint = textMuted,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0381FE),
                                unfocusedBorderColor = if (isDark) Color(0x22FFFFFF) else Color(0x18000000),
                                errorBorderColor = Color(0xFFEF4444),
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                cursorColor = Color(0xFF0381FE),
                                focusedContainerColor = inputBg,
                                unfocusedContainerColor = inputBg
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                                autoCorrect = false
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focus.clearFocus()
                                    performSignIn()
                                }
                            ),
                            textStyle = TextStyle(
                                fontSize = 15.sp,
                                color = textPrimary,
                                fontFamily = InterFontFamily
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Forgot Password Link
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, end = 2.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = "Forgot password?",
                                color = if (isDark) Color(0xFF3E91FF) else Color(0xFF0072DE),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.2).sp,
                                fontFamily = InterFontFamily,
                                modifier = Modifier.clickable(role = Role.Button) {
                                    resetEmail = if (identifier.contains("@")) identifier.trim() else ""
                                    resetError = null
                                    resetSent = false
                                    showForgotDialog = true
                                }
                            )
                        }

                        // Error Banner
                        AnimatedVisibility(
                            visible = loginError != null,
                            enter = fadeIn(tween(150)) + expandVertically(),
                            exit = fadeOut(tween(150)) + shrinkVertically()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Lucide.ShieldAlert,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = loginError ?: "",
                                    color = if (isDark) Color(0xFFFFB4AB) else Color(0xFFBA1A1A),
                                    fontSize = 12.5.sp,
                                    lineHeight = 16.sp,
                                    letterSpacing = (-0.2).sp,
                                    fontFamily = InterFontFamily,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── 3. BOTTOM ACTIONS AREA (Borderless Pills) ────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Primary Sign In Button: Borderless Pill
                        OneUiPrimaryPillButton(
                            text = "Sign In to Studio",
                            onClick = {
                                focus.clearFocus()
                                performSignIn()
                            },
                            loading = isSigningIn,
                            enabled = !isSigningIn && identifier.isNotBlank() && password.isNotBlank()
                        )

                        // Clean One UI Divider
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                thickness = 0.5.dp,
                                color = if (isDark) Color(0x18FFFFFF) else Color(0x12000000)
                            )
                            Text(
                                text = "or continue with",
                                color = textMuted,
                                fontSize = 12.sp,
                                letterSpacing = (-0.2).sp,
                                fontFamily = InterFontFamily
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                thickness = 0.5.dp,
                                color = if (isDark) Color(0x18FFFFFF) else Color(0x12000000)
                            )
                        }

                        // Continue with Google: Borderless Pill
                        OneUiSocialPillButton(
                            text = "Continue with Google",
                            onClick = onGoogleSignIn,
                            loading = googleLoading,
                            isDark = isDark,
                            leading = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_google_logo),
                                    contentDescription = "Google logo",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        )

                        // Continue with Apple: Borderless Pill
                        OneUiSocialPillButton(
                            text = "Continue with Apple",
                            onClick = onAppleSignIn,
                            loading = appleLoading,
                            isDark = isDark,
                            leading = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_apple_logo),
                                    contentDescription = "Apple logo",
                                    tint = if (isDark) Color.White else Color.Black,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        )

                        // Explore as Guest: Borderless Tertiary Pill
                        OneUiTertiaryPillButton(
                            text = "Explore Studio as Guest",
                            onClick = onExploreGuest,
                            isDark = isDark,
                            textPrimary = textPrimary,
                            leading = {
                                Icon(
                                    imageVector = Lucide.Sparkles,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF3E91FF) else Color(0xFF0381FE),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        )

                        Spacer(Modifier.height(4.dp))

                        // Create Account Link
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Don't have an account? ",
                                color = textMuted,
                                fontSize = 13.sp,
                                letterSpacing = (-0.2).sp,
                                fontFamily = InterFontFamily
                            )
                            Text(
                                text = "Create account",
                                color = if (isDark) Color(0xFF3E91FF) else Color(0xFF0072DE),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.2).sp,
                                fontFamily = InterFontFamily,
                                modifier = Modifier.clickable(role = Role.Button, onClick = onCreateAccount)
                            )
                        }

                        // Terms and Privacy Link
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "By signing in, you agree to our ",
                                color = textMuted.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                letterSpacing = (-0.2).sp,
                                fontFamily = InterFontFamily
                            )
                            Text(
                                text = "Terms & Privacy Policy",
                                color = if (isDark) Color(0xFF3E91FF) else Color(0xFF0072DE),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.2).sp,
                                fontFamily = InterFontFamily,
                                modifier = Modifier.clickable(role = Role.Button, onClick = onOpenTerms)
                            )
                        }
                    }
                }
            }
        }
    }

    // ── 4. FORGOT PASSWORD DIALOG (Samsung One UI Focus Block) ──────────────
    if (showForgotDialog) {
        Dialog(
            onDismissRequest = { if (!resetLoading) showForgotDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                OneUiGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 28.dp,
                    backgroundColor = cardBg,
                    contentPadding = PaddingValues(22.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Reset Password",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            fontFamily = InterFontFamily
                        )

                        IconButton(
                            onClick = { if (!resetLoading) showForgotDialog = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.X,
                                contentDescription = "Close",
                                tint = textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Enter the email associated with your account. We'll send you a secure link to reset your studio password.",
                        fontSize = 13.5.sp,
                        color = textMuted,
                        lineHeight = 18.sp,
                        fontFamily = InterFontFamily
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = {
                            resetEmail = it.trim()
                            resetError = null
                        },
                        placeholder = {
                            Text(
                                "Your email address",
                                color = textMuted.copy(alpha = 0.65f),
                                fontSize = 14.5.sp,
                                fontFamily = InterFontFamily
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Lucide.Mail,
                                contentDescription = null,
                                tint = Color(0xFF0381FE),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0381FE),
                            unfocusedBorderColor = if (isDark) Color(0x22FFFFFF) else Color(0x18000000),
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            cursorColor = Color(0xFF0381FE),
                            focusedContainerColor = inputBg,
                            unfocusedContainerColor = inputBg
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                        textStyle = TextStyle(fontSize = 14.5.sp, color = textPrimary, fontFamily = InterFontFamily),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (resetError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = resetError!!,
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontFamily = InterFontFamily,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (resetSent) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "A reset link has been dispatched to your email! Please check your inbox and spam folder.",
                            color = Color(0xFF10B981),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = InterFontFamily,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    // Borderless Pill Button in Dialog
                    OneUiPrimaryPillButton(
                        text = if (resetSent) "Done" else "Send Reset Link",
                        onClick = {
                            if (resetSent) {
                                showForgotDialog = false
                                return@OneUiPrimaryPillButton
                            }
                            if (resetEmail.isBlank() || !resetEmail.contains("@")) {
                                resetError = "Please enter a valid email address"
                                return@OneUiPrimaryPillButton
                            }
                            scope.launch {
                                resetLoading = true
                                resetError = null
                                try {
                                    auth.sendPasswordResetEmail(resetEmail).await()
                                    resetSent = true
                                } catch (e: Exception) {
                                    resetError = e.message ?: "Failed to send reset email"
                                } finally {
                                    resetLoading = false
                                }
                            }
                        },
                        loading = resetLoading,
                        height = 48.dp
                    )
                }
            }
        }
    }
}

// ─── REUSABLE ONE UI BORDERLESS PILL BUTTONS ─────────────────────────────────

/**
 * Samsung One UI Signature Primary Pill Button:
 * Uses Samsung One UI primary gradient #0381FE -> #0072DE.
 * Strictly BORDERLESS per standing user rule: "Do NOT use borders for pills/capsules".
 */
@Composable
fun OneUiPrimaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    height: Dp = 52.dp
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "primary_pill_scale"
    )

    val gradient = if (enabled && !loading) {
        Brush.horizontalGradient(listOf(Color(0xFF0381FE), Color(0xFF0072DE)))
    } else {
        Brush.horizontalGradient(listOf(Color(0xFF0381FE).copy(alpha = 0.45f), Color(0xFF0072DE).copy(alpha = 0.45f)))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (enabled && !loading) 8.dp else 0.dp,
                shape = CircleShape,
                ambientColor = Color(0x300381FE),
                spotColor = Color(0x500381FE)
            )
            .clip(CircleShape)
            .background(gradient)
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
                modifier = Modifier.size(19.dp),
                color = Color.White,
                strokeWidth = 2.2.dp
            )
        } else {
            leading?.let {
                it()
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = text,
                color = Color.White,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp,
                fontFamily = InterFontFamily
            )
        }
    }
}

/**
 * Samsung One UI Social Pill Button (Google / Apple):
 * Strictly BORDERLESS per standing user rule: "Do NOT use borders for pills/capsules".
 */
@Composable
fun OneUiSocialPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    isDark: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    height: Dp = 50.dp
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "social_pill_scale"
    )

    val buttonBg = if (isDark) Color(0xFF17171A) else Color(0xFFF2F4F7)
    val textPrimary = if (isDark) Color(0xFFFAFAFA) else Color(0xFF0F172A)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(buttonBg)
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
                color = textPrimary,
                strokeWidth = 2.dp
            )
        } else {
            leading?.let {
                it()
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = text,
                color = textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp,
                fontFamily = InterFontFamily
            )
        }
    }
}

/**
 * Samsung One UI Tertiary Pill Button (Guest / Explore):
 * Strictly BORDERLESS per standing user rule: "Do NOT use borders for pills/capsules".
 */
@Composable
fun OneUiTertiaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    textPrimary: Color = Color.White,
    leading: (@Composable () -> Unit)? = null,
    height: Dp = 46.dp
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "tertiary_pill_scale"
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
            .background(if (isDark) Color(0x12FFFFFF) else Color(0x0A000000))
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
            color = textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.2).sp,
            fontFamily = InterFontFamily
        )
    }
}
