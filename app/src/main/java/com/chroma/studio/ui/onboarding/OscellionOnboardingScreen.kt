package com.chroma.studio.ui.onboarding

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chroma.studio.R
import com.chroma.studio.data.DiscoverRepository
import com.chroma.studio.data.SavedAccount
import com.chroma.studio.data.SavedAccountManager
import com.chroma.studio.data.SupabaseMediaStorage
import com.chroma.studio.model.UserProfile
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Screen state machine for Oscillom Google Sign-In & Onboarding Architecture.
 */
enum class AuthScreenState {
    WELCOME,
    CHECKING_DATABASE,
    ACCOUNT_CONFIRMATION,
    ONBOARDING_WIZARD
}

enum class AuthIntentMode {
    LOGIN,
    SIGN_UP
}

/**
 * Complete, refactored Authentication & Onboarding Screen for Oscillom.
 *
 * Implements:
 * 1. Flow A: "Continue with Google" -> OAuth -> Check DB -> (Returning ? AccountConfirmation : OnboardingWizard)
 * 2. Flow B: "Create a new account" -> OAuth -> Check DB -> (Exists ? DuplicateErrorModal : OnboardingWizard)
 * 3. AccountConfirmationScreen: "Is this your account? Continue with this account" / "This is not my account"
 * 4. Multi-Step OnboardingWizard: "New to Oscillom, let's create a new account" (Identity -> Personal -> Interests -> Completion)
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
    val auth = remember { FirebaseAuth.getInstance() }
    val discoverRepo = remember { DiscoverRepository() }

    val savedAccounts = remember { SavedAccountManager.getSavedAccounts(context) }
    val primarySaved = savedAccounts.firstOrNull()
    val savedAccountName = primarySaved?.let { it.displayName.ifBlank { it.username } }

    var currentScreenState by remember { mutableStateOf(AuthScreenState.WELCOME) }
    var authIntentMode by remember { mutableStateOf(AuthIntentMode.LOGIN) }

    var isGoogleLoading by remember { mutableStateOf(false) }
    var isAppleLoading by remember { mutableStateOf(false) }
    var isConfirmingLoading by remember { mutableStateOf(false) }
    var showTermsModal by remember { mutableStateOf(false) }
    var showDuplicateDialog by remember { mutableStateOf(false) }

    var detectedProfile by remember { mutableStateOf<UserProfile?>(null) }
    var activeGoogleUserData by remember { mutableStateOf<GoogleUserData?>(null) }
    var activeFirebaseUser by remember { mutableStateOf<com.google.firebase.auth.FirebaseUser?>(null) }

    // Helper to purge Google & Firebase credentials cleanly
    fun purgeClientSession(onComplete: () -> Unit = {}) {
        scope.launch {
            try {
                auth.signOut()
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
                val client = GoogleSignIn.getClient(context, gso)
                client.signOut().await()
                try { client.revokeAccess().await() } catch (_: Exception) {}
            } catch (_: Exception) {}
            detectedProfile = null
            activeGoogleUserData = null
            activeFirebaseUser = null
            onComplete()
        }
    }

    // Google Sign-In Launcher
    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            scope.launch {
                isGoogleLoading = true
                currentScreenState = AuthScreenState.CHECKING_DATABASE
                try {
                    val account = task.await()
                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                    val authResult = auth.signInWithCredential(credential).await()
                    val user = authResult.user
                    if (user != null) {
                        activeFirebaseUser = user
                        val verifiedEmail = account.email ?: user.email ?: ""
                        val googleId = account.id ?: user.uid
                        val displayName = account.displayName ?: user.displayName ?: ""
                        val photoUrl = account.photoUrl?.toString() ?: user.photoUrl?.toString() ?: ""

                        val gData = GoogleUserData(
                            email = verifiedEmail,
                            googleId = googleId,
                            displayName = displayName,
                            photoUrl = photoUrl
                        )
                        activeGoogleUserData = gData

                        // Query Firestore database: does this account exist?
                        val existing = discoverRepo.getUserProfile(user.uid)
                            ?: discoverRepo.getUserByEmail(verifiedEmail)
                            ?: discoverRepo.getUserByGoogleId(googleId)

                        if (authIntentMode == AuthIntentMode.LOGIN) {
                            // Flow A: "Continue with Google"
                            if (existing != null) {
                                // Case A2: Returning user -> Confirmation Screen
                                detectedProfile = existing
                                currentScreenState = AuthScreenState.ACCOUNT_CONFIRMATION
                            } else {
                                // Case A1: Brand new user -> Onboarding Wizard
                                currentScreenState = AuthScreenState.ONBOARDING_WIZARD
                            }
                        } else {
                            // Flow B: "Create a new account"
                            if (existing != null) {
                                // Validation Failure: Email already registered
                                detectedProfile = existing
                                currentScreenState = AuthScreenState.WELCOME
                                showDuplicateDialog = true
                            } else {
                                // Validation Success: Email does not exist
                                currentScreenState = AuthScreenState.ONBOARDING_WIZARD
                            }
                        }
                    } else {
                        Toast.makeText(context, "Google authentication failed", Toast.LENGTH_SHORT).show()
                        currentScreenState = AuthScreenState.WELCOME
                    }
                } catch (e: Exception) {
                    val msg = e.localizedMessage ?: e.message ?: "Google Sign-In failed"
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    currentScreenState = AuthScreenState.WELCOME
                } finally {
                    isGoogleLoading = false
                }
            }
        } else {
            isGoogleLoading = false
            currentScreenState = AuthScreenState.WELCOME
        }
    }

    fun launchGoogleSignIn(mode: AuthIntentMode) {
        authIntentMode = mode
        isGoogleLoading = true
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
            Toast.makeText(context, "Google Sign-In error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun launchAppleSignIn() {
        isAppleLoading = true
        val activity = context as? Activity
        if (activity == null) {
            isAppleLoading = false
            Toast.makeText(context, "Sign in window unavailable", Toast.LENGTH_SHORT).show()
            return
        }
        val provider = OAuthProvider.newBuilder("apple.com")
        provider.setScopes(listOf("email", "name"))

        auth.startActivityForSignInWithProvider(activity, provider.build())
            .addOnSuccessListener { authResult ->
                scope.launch {
                    try {
                        val user = authResult.user
                        if (user != null) {
                            val existing = discoverRepo.getUserProfile(user.uid)
                            if (existing != null) {
                                SavedAccountManager.saveFromProfile(context, existing, user.email)
                                com.chroma.studio.services.ChromaMessagingService.syncTokenForCurrentUser()
                                Toast.makeText(context, "Welcome back, ${existing.displayName}!", Toast.LENGTH_SHORT).show()
                                onLoginSuccess()
                            } else {
                                activeFirebaseUser = user
                                activeGoogleUserData = GoogleUserData(
                                    email = user.email ?: "",
                                    googleId = user.uid,
                                    displayName = user.displayName ?: "Creator",
                                    photoUrl = user.photoUrl?.toString() ?: ""
                                )
                                currentScreenState = AuthScreenState.ONBOARDING_WIZARD
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, e.localizedMessage ?: "Apple authentication failed", Toast.LENGTH_LONG).show()
                    } finally {
                        isAppleLoading = false
                    }
                }
            }
            .addOnFailureListener { e ->
                isAppleLoading = false
                Toast.makeText(context, e.localizedMessage ?: "Apple Sign-In failed", Toast.LENGTH_LONG).show()
            }
    }

    fun enterAsGuest() {
        val guestAccount = SavedAccount(
            uid = "guest_creator_local",
            username = "guest",
            displayName = "Guest Creator",
            avatarUrl = "",
            email = "guest@chromastudio.app",
            lastLoginMillis = System.currentTimeMillis(),
            gender = "UNSPECIFIED"
        )
        SavedAccountManager.saveAccount(context, guestAccount)
        Toast.makeText(context, "Exploring Studio as Guest", Toast.LENGTH_SHORT).show()
        onLoginSuccess()
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentScreenState,
            label = "authScreenTransition",
            transitionSpec = {
                fadeIn(tween(350)) togetherWith fadeOut(tween(250))
            }
        ) { state ->
            when (state) {
                AuthScreenState.WELCOME -> {
                    WelcomeScreen(
                        savedAccountName = savedAccountName,
                        googleLoading = isGoogleLoading,
                        appleLoading = isAppleLoading,
                        onContinueSaved = {
                            if (primarySaved != null) {
                                launchGoogleSignIn(AuthIntentMode.LOGIN)
                            } else {
                                enterAsGuest()
                            }
                        },
                        onGoogle = {
                            launchGoogleSignIn(AuthIntentMode.LOGIN)
                        },
                        onCreateAccount = {
                            launchGoogleSignIn(AuthIntentMode.SIGN_UP)
                        },
                        onApple = { launchAppleSignIn() },
                        onSignInExisting = {
                            launchGoogleSignIn(AuthIntentMode.LOGIN)
                        },
                        onExploreGuest = { enterAsGuest() },
                        onTerms = { showTermsModal = true }
                    )
                }

                AuthScreenState.CHECKING_DATABASE -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF030510)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(38.dp),
                            color = Color(0xFF0381FE),
                            strokeWidth = 3.dp
                        )
                    }
                }

                AuthScreenState.ACCOUNT_CONFIRMATION -> {
                    val prof = detectedProfile ?: UserProfile()
                    val email = activeGoogleUserData?.email ?: prof.email
                    AccountConfirmationScreen(
                        profile = prof,
                        email = email,
                        isLoading = isConfirmingLoading,
                        onContinue = {
                            isConfirmingLoading = true
                            scope.launch {
                                try {
                                    SavedAccountManager.saveFromProfile(context, prof, email)
                                    com.chroma.studio.services.ChromaMessagingService.syncTokenForCurrentUser()
                                    Toast.makeText(context, "Welcome back, ${prof.displayName}!", Toast.LENGTH_SHORT).show()
                                    onLoginSuccess()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to restore session: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isConfirmingLoading = false
                                }
                            }
                        },
                        onNotMine = {
                            purgeClientSession {
                                currentScreenState = AuthScreenState.WELCOME
                                Toast.makeText(context, "Session cleared. Please choose your account.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                AuthScreenState.ONBOARDING_WIZARD -> {
                    val gData = activeGoogleUserData ?: GoogleUserData(
                        email = activeFirebaseUser?.email ?: "",
                        googleId = activeFirebaseUser?.uid ?: "",
                        displayName = activeFirebaseUser?.displayName ?: "Creator",
                        photoUrl = activeFirebaseUser?.photoUrl?.toString() ?: ""
                    )

                    OnboardingWizard(
                        googleData = gData,
                        discoverRepo = discoverRepo,
                        onBackToWelcome = {
                            purgeClientSession {
                                currentScreenState = AuthScreenState.WELCOME
                            }
                        },
                        onComplete = { output ->
                            scope.launch {
                                val uid = activeFirebaseUser?.uid ?: auth.currentUser?.uid
                                if (uid.isNullOrBlank()) {
                                    Toast.makeText(context, "Authentication session expired. Please sign in again.", Toast.LENGTH_LONG).show()
                                    currentScreenState = AuthScreenState.WELCOME
                                    return@launch
                                }

                                // Upload custom avatar to Supabase Storage if user selected an image from gallery
                                var finalAvatar = output.avatarUrl
                                output.avatarUri?.let { uri ->
                                    val uploaded = withContext(Dispatchers.IO) {
                                        SupabaseMediaStorage.uploadAvatarFromUri(context, uid, uri)
                                    }
                                    if (uploaded != null) {
                                        finalAvatar = uploaded
                                    }
                                }

                                // Atomic commit to Firestore database
                                val success = discoverRepo.registerDetailedUser(
                                    uid = uid,
                                    displayName = output.displayName,
                                    username = output.username,
                                    dob = output.dobMillis,
                                    avatarUrl = finalAvatar,
                                    interests = output.interests,
                                    email = output.email,
                                    googleId = output.googleId,
                                    bio = output.bio,
                                    isOnboarded = true
                                )

                                if (success) {
                                    val newProfile = discoverRepo.getUserProfile(uid)
                                    if (newProfile != null) {
                                        SavedAccountManager.saveFromProfile(context, newProfile, output.email)
                                    }
                                    com.chroma.studio.services.ChromaMessagingService.syncTokenForCurrentUser()
                                    Toast.makeText(context, "Welcome to Oscillom, ${output.displayName}!", Toast.LENGTH_SHORT).show()
                                    onLoginSuccess()
                                } else {
                                    Toast.makeText(context, "Failed to complete registration. Please try again.", Toast.LENGTH_LONG).show()
                                    currentScreenState = AuthScreenState.WELCOME
                                }
                            }
                        }
                    )
                }
            }
        }

        // Duplicate Account Dialog (Flow B)
        if (showDuplicateDialog) {
            val email = activeGoogleUserData?.email ?: detectedProfile?.email ?: ""
            DuplicateAccountDialog(
                email = email,
                onGoToLogin = {
                    showDuplicateDialog = false
                    currentScreenState = AuthScreenState.ACCOUNT_CONFIRMATION
                },
                onUseDifferentAccount = {
                    showDuplicateDialog = false
                    purgeClientSession {
                        launchGoogleSignIn(AuthIntentMode.SIGN_UP)
                    }
                },
                onDismiss = {
                    showDuplicateDialog = false
                    purgeClientSession {
                        currentScreenState = AuthScreenState.WELCOME
                    }
                }
            )
        }
    }
}
