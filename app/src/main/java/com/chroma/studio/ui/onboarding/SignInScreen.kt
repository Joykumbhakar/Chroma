package com.chroma.studio.ui.onboarding

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.chroma.studio.R
import com.chroma.studio.data.DiscoverRepository
import com.chroma.studio.data.SavedAccount
import com.chroma.studio.data.SavedAccountManager
import com.chroma.studio.ui.components.IosAvatarView
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Dedicated, full-screen Sign In experience for Chroma Studio / Oscellion.
 *
 * Implements:
 * - Matching deep navy flow gradient background ([ObColors.FlowBackground])
 * - Clean, professional layout with glass containers and high-contrast typography
 * - Unified username and email authentication
 * - Password field with visibility toggle and hardware/IME done actions
 * - Quick-fill selector for locally saved accounts
 * - In-place password reset link dispatch
 * - Apple and Google quick sign-in actions
 * - Seamless navigation back to Welcome or into Account creation
 */
@Composable
fun SignInScreen(
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit,
    onCreateAccount: () -> Unit,
    onGoogleSignIn: () -> Unit,
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
                val email = if (cleanIdentifier.contains("@")) {
                    cleanIdentifier
                } else {
                    "${cleanIdentifier.lowercase().trim()}@chromastudio.app"
                }

                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                val user = authResult.user ?: throw IllegalStateException("Authentication failed: No user returned")

                val profile = repo.getUserProfile(user.uid)
                if (profile != null) {
                    SavedAccountManager.saveFromProfile(context, profile, email)
                }

                Toast.makeText(context, "Welcome back, ${profile?.displayName ?: cleanIdentifier}!", Toast.LENGTH_SHORT).show()
                onLoginSuccess()
            } catch (e: Exception) {
                loginError = when (e) {
                    is FirebaseAuthInvalidUserException -> "No account found for this username or email."
                    is FirebaseAuthInvalidCredentialsException -> "Incorrect password or username. Please try again."
                    is FirebaseNetworkException -> "Network connection failed. Please check your internet."
                    else -> e.message ?: "Authentication failed. Please verify your details."
                }
            } finally {
                isSigningIn = false
            }
        }
    }

    ObScreen {
        // Navigation bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ObIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                description = "Back to Welcome",
                onClick = onBack
            )
            ObBadge(text = "Sign In", color = ObColors.Lime)
            ObIconButton(
                icon = Icons.Rounded.Close,
                description = "Close",
                onClick = onBack
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(6.dp))

            ObHeader(
                firstLine = "Sign in to",
                highlightLine = "Chroma Studio",
                subtitle = "Access your creative canvas, encrypted conversations, and personal studio profile."
            )

            Spacer(Modifier.height(18.dp))

            // Quick select for saved accounts if available
            if (savedAccounts.isNotEmpty()) {
                Column(Modifier.padding(horizontal = 22.dp)) {
                    Text(
                        text = "SAVED ACCOUNTS",
                        style = ObType.SectionHeader,
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
                            ObGlassCard(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable(role = Role.Button) {
                                        identifier = acc.username.ifBlank { acc.email }
                                        loginError = null
                                    },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .background(if (isSelected) Color(0x35C8F535) else Color(0x75111636))
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) ObColors.Lime else Color(0x22FFFFFF),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF0C1238))
                                    ) {
                                        IosAvatarView(
                                            name = acc.displayName.ifBlank { acc.username },
                                            avatarUrl = acc.avatarUrl,
                                            size = 28.dp,
                                            gender = acc.gender,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = acc.displayName.ifBlank { acc.username },
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "@${acc.username}",
                                            color = ObColors.TextMuted,
                                            fontSize = 10.5.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }

            // Credentials Card
            ObGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(Color(0x950E1333))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Field 1: Identifier
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "USERNAME OR EMAIL",
                            style = ObType.SectionHeader
                        )
                        ObTextField(
                            value = identifier,
                            onValueChange = {
                                identifier = it
                                loginError = null
                            },
                            placeholder = "username or name@domain.com",
                            leading = Icons.Rounded.AlternateEmail,
                            leadingTint = if (identifier.isNotBlank()) ObColors.Lime else ObColors.Sky,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next,
                                autoCorrect = false
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focus.moveFocus(FocusDirection.Down) }
                            ),
                            trailing = {
                                if (identifier.isNotEmpty()) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Clear",
                                        tint = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable(role = Role.Button) { identifier = "" }
                                    )
                                }
                            }
                        )
                    }

                    // Field 2: Password
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PASSWORD",
                                style = ObType.SectionHeader
                            )
                            Text(
                                text = "Forgot password?",
                                color = ObColors.Lime,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable(role = Role.Button) {
                                    resetEmail = if (identifier.contains("@")) identifier.trim() else ""
                                    resetError = null
                                    resetSent = false
                                    showForgotDialog = true
                                }
                            )
                        }
                        ObTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                loginError = null
                            },
                            placeholder = "Enter your password",
                            leading = Icons.Rounded.Lock,
                            leadingTint = if (password.isNotBlank()) ObColors.Lime else ObColors.Sky,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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
                            trailing = {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable(role = Role.Button) { passwordVisible = !passwordVisible }
                                )
                            }
                        )
                    }

                    // Error message display
                    AnimatedVisibility(
                        visible = loginError != null,
                        enter = fadeIn(tween(150)) + expandVertically(),
                        exit = fadeOut(tween(150)) + shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ObColors.Danger.copy(alpha = 0.15f))
                                .border(1.dp, ObColors.Danger.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = ObColors.Danger,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = loginError ?: "",
                                color = Color.White,
                                fontSize = 12.5.sp,
                                lineHeight = 17.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // Sign In Button
                    ObPrimaryButton(
                        text = "Sign In to Studio",
                        onClick = {
                            focus.clearFocus()
                            performSignIn()
                        },
                        loading = isSigningIn,
                        enabled = !isSigningIn && identifier.isNotBlank() && password.isNotBlank()
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Divider: or continue with
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(Modifier.weight(1f).height(1.dp).background(Color(0x22FFFFFF)))
                Text("or continue with", color = ObColors.TextFaint, fontSize = 12.sp)
                Box(Modifier.weight(1f).height(1.dp).background(Color(0x22FFFFFF)))
            }

            Spacer(Modifier.height(14.dp))

            // Quick Social Sign-In buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ObPrimaryButton(
                    text = "Sign in with Google",
                    onClick = onGoogleSignIn,
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
            }

            Spacer(Modifier.height(20.dp))

            // Bottom prompt to create new account
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Don't have a studio account? ", color = Color(0xCCFFFFFF), fontSize = 13.sp)
                Text(
                    text = "Create account",
                    color = ObColors.Lime,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(role = Role.Button, onClick = onCreateAccount)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Terms and privacy link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("By signing in, you agree to our ", color = Color(0x99FFFFFF), fontSize = 11.sp)
                Text(
                    text = "Terms & Privacy Policy",
                    color = ObColors.Lime,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(role = Role.Button, onClick = onOpenTerms)
                )
            }
        }
    }

    // Password reset dialog
    if (showForgotDialog) {
        Dialog(onDismissRequest = { if (!resetLoading) showForgotDialog = false }) {
            ObGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(ObColors.Night.copy(alpha = 0.96f))
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Reset Password", style = ObType.Title.copy(fontSize = 20.sp))
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(22.dp)
                                .clickable { if (!resetLoading) showForgotDialog = false }
                        )
                    }

                    Text(
                        text = "Enter the email associated with your account. We'll send you a secure link to reset your studio password.",
                        style = ObType.Body,
                        color = ObColors.TextMuted
                    )

                    ObTextField(
                        value = resetEmail,
                        onValueChange = {
                            resetEmail = it.trim()
                            resetError = null
                        },
                        placeholder = "yourname@domain.com",
                        leading = Icons.Rounded.Email,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() })
                    )

                    if (resetError != null) {
                        Text(
                            text = resetError!!,
                            color = ObColors.Danger,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (resetSent) {
                        Text(
                            text = "A reset link has been dispatched to your email! Please check your inbox and spam folder.",
                            color = ObColors.Lime,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    ObPrimaryButton(
                        text = if (resetSent) "Done" else "Send Reset Link",
                        onClick = {
                            if (resetSent) {
                                showForgotDialog = false
                                return@ObPrimaryButton
                            }
                            if (resetEmail.isBlank() || !resetEmail.contains("@")) {
                                resetError = "Please enter a valid email address"
                                return@ObPrimaryButton
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
                        loading = resetLoading
                    )
                }
            }
        }
    }
}
