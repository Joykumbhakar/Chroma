package com.chroma.studio.ui.onboarding

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chroma.studio.R
import com.chroma.studio.data.DiscoverRepository
import com.chroma.studio.data.SavedAccount
import com.chroma.studio.data.SavedAccountManager
import com.chroma.studio.data.SupabaseMediaStorage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Full-featured, production-wired Onboarding Screen for Chroma Studio / Oscellion.
 * Integrates:
 * - The animated 5-step flow (Welcome -> Interests -> Account -> Password -> Privacy)
 * - Google Sign-In with auto profile discovery and prefill
 * - Saved account continuation
 * - Direct email/password Sign In modal for existing accounts
 * - Live username availability check via Firestore DiscoverRepository
 * - Supabase profile avatar upload
 * - Complete UserProfile and account center preferences storage
 */
@Composable
fun OscellionOnboardingScreen(
    onLoginSuccess: () -> Unit,
    onCancel: () -> Unit = {},
    modifier: Modifier = Modifier,
    vm: OnboardingViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val auth = remember { FirebaseAuth.getInstance() }
    val discoverRepo = remember { DiscoverRepository() }

    val savedAccounts = remember { SavedAccountManager.getSavedAccounts(context) }
    val primarySaved = savedAccounts.firstOrNull()
    val savedAccountName = primarySaved?.let { it.displayName.ifBlank { it.username } }

    var isGoogleLoading by remember { mutableStateOf(false) }
    var isCreatingAccount by remember { mutableStateOf(false) }
    var showTermsModal by remember { mutableStateOf(false) }
    var generalError by remember { mutableStateOf<String?>(null) }

    // Google Sign-In Launcher
    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            scope.launch {
                isGoogleLoading = true
                generalError = null
                try {
                    val account = task.await()
                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                    val authResult = auth.signInWithCredential(credential).await()
                    val user = authResult.user
                    if (user != null) {
                        val existingProfile = discoverRepo.getUserProfile(user.uid)
                        if (existingProfile != null) {
                            SavedAccountManager.saveFromProfile(context, existingProfile, user.email)
                            Toast.makeText(context, "Welcome back, ${existingProfile.displayName}!", Toast.LENGTH_SHORT).show()
                            onLoginSuccess()
                        } else {
                            // New Google user: populate state and go straight to Interests step
                            val baseUsername = user.email?.substringBefore("@")?.filter { it.isLetterOrDigit() || it == '_' }?.lowercase() ?: ""
                            vm.state.username = baseUsername
                            vm.state.fullName = user.displayName ?: ""
                            user.photoUrl?.let { vm.state.avatarUri = it }
                            vm.state.step = ObStep.Interests
                        }
                    } else {
                        generalError = "Google authentication failed"
                    }
                } catch (e: Exception) {
                    generalError = e.message ?: "Sign in failed"
                } finally {
                    isGoogleLoading = false
                }
            }
        } else {
            isGoogleLoading = false
        }
    }

    fun launchGoogleSignIn() {
        isGoogleLoading = true
        generalError = null
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            val client = GoogleSignIn.getClient(context, gso)
            client.signOut().addOnCompleteListener {
                googleLauncher.launch(client.signInIntent)
            }
        } catch (e: Exception) {
            isGoogleLoading = false
            generalError = "Google Sign In setup error: ${e.message}"
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        OnboardingFlow(
            onFinished = { finalState ->
                scope.launch {
                    isCreatingAccount = true
                    generalError = null
                    try {
                        val email = "${finalState.username.lowercase().trim()}@chromastudio.app"
                        val pass = if (finalState.password.isNotBlank()) {
                            finalState.password
                        } else {
                            "Oscellion#" + (System.currentTimeMillis() % 100000) + "!"
                        }
                        val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
                        val uid = authResult.user?.uid ?: throw IllegalStateException("Failed to retrieve user ID")

                        // Upload avatar if custom selected
                        var uploadedAvatarUrl = finalState.avatarPresetId ?: ""
                        finalState.avatarUri?.let { uri ->
                            val url = withContext(Dispatchers.IO) {
                                SupabaseMediaStorage.uploadAvatarFromUri(context, uid, uri)
                            }
                            if (url != null) {
                                uploadedAvatarUrl = url
                            }
                        }

                        // Register complete profile in Firestore
                        val registered = discoverRepo.registerDetailedUser(
                            uid = uid,
                            displayName = finalState.fullName.trim(),
                            username = finalState.username.trim().lowercase(),
                            dob = finalState.dobMillis,
                            avatarUrl = uploadedAvatarUrl,
                            interests = finalState.interests.toList(),
                            gender = finalState.gender.name,
                            agreedToTermsAt = System.currentTimeMillis()
                        )

                        if (!registered) {
                            throw IllegalStateException("Profile registration could not be finalized")
                        }

                        // Save personality and privacy preferences
                        FirebaseFirestore.getInstance().collection("users").document(uid).update(
                            mapOf(
                                "bio" to finalState.bio,
                                "startsConversations" to finalState.startsConversations,
                                "respondsToOthers" to finalState.respondsToOthers,
                                "joinsDebates" to finalState.joinsDebates,
                                "usesHumor" to finalState.usesHumor,
                                "followsTrends" to finalState.followsTrends,
                                "postingFrequency" to finalState.postingFrequency,
                                "privateAccount" to finalState.privateAccount,
                                "allowTagging" to finalState.allowMentions,
                                "showOnlineStatus" to finalState.showOnline,
                                "defaultAspectRatio" to finalState.aspect.label
                            )
                        ).await()

                        // Save account locally
                        SavedAccountManager.saveAccount(
                            context,
                            SavedAccount(
                                uid = uid,
                                username = finalState.username.trim().lowercase(),
                                displayName = finalState.fullName.trim(),
                                avatarUrl = uploadedAvatarUrl,
                                email = email,
                                lastLoginMillis = System.currentTimeMillis(),
                                gender = finalState.gender.name
                            )
                        )

                        Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT).show()
                        onLoginSuccess()
                    } catch (e: Exception) {
                        generalError = e.message ?: "Failed to create account"
                        Toast.makeText(context, "Registration error: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally {
                        isCreatingAccount = false
                    }
                }
            },
            onGoogleSignIn = { launchGoogleSignIn() },
            onContinueSaved = {
                if (primarySaved != null) {
                    launchGoogleSignIn()
                }
            },
            onLoginSuccess = onLoginSuccess,
            onSignInExisting = {
                vm.state.step = ObStep.SignIn
            },
            onOpenTerms = { showTermsModal = true },
            savedAccountName = savedAccountName,
            savedAccounts = savedAccounts,
            googleLoading = isGoogleLoading,
            discoverRepo = discoverRepo,
            vm = vm
        )

        // One UI 7.0 Loading Overlay while finalizing account creation
        if (isCreatingAccount) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.70f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.padding(32.dp),
                    shape = RoundedCornerShape(26.dp),
                    color = Color(0xFF12141A),
                    border = BorderStroke(1.dp, Color(0xFF1F2430)),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(38.dp),
                            color = Color(0xFF2563EB),
                            strokeWidth = 3.5.dp
                        )
                        Text(
                            text = "Setting up your studio…",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "Configuring cryptographic keys, canvas settings, and studio profile.",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }



        // Terms and Privacy Modal
        if (showTermsModal) {
            Dialog(onDismissRequest = { showTermsModal = false }) {
                ObGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .background(ObColors.Night.copy(alpha = 0.95f))
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Terms & Privacy Policy", style = ObType.Title.copy(fontSize = 19.sp))
                        Text(
                            text = "Oscellion / Chroma Studio provides an artistic ecosystem for multi-layer shaders, 3D graphics, and encrypted messaging.\n\n" +
                                "• Respect intellectual property and original works.\n" +
                                "• Your private direct messages are protected by end-to-end encryption.\n" +
                                "• You control your profile visibility and mention permissions at any time in Account Settings.",
                            style = ObType.Body,
                            textAlign = TextAlign.Start
                        )
                        Spacer(Modifier.height(8.dp))
                        ObPrimaryButton("Close", { showTermsModal = false })
                    }
                }
            }
        }
    }
}
