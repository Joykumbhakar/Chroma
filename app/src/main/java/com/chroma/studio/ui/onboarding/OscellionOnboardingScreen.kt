package com.chroma.studio.ui.onboarding

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Screen state machine for Oscillom Google Sign-In & Onboarding Architecture.
 */
enum class AuthScreenState {
    WELCOME,
    SIGN_IN_EMAIL,
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

    var initialSignupUsername by remember { mutableStateOf("") }
    var initialSignupPassword by remember { mutableStateOf("") }

    var isGoogleLoading by remember { mutableStateOf(false) }
    var isAppleLoading by remember { mutableStateOf(false) }
    var isConfirmingLoading by remember { mutableStateOf(false) }
    var showTermsModal by remember { mutableStateOf(false) }
    var showDuplicateDialog by remember { mutableStateOf(false) }

    var detectedProfile by remember { mutableStateOf<UserProfile?>(null) }
    var activeGoogleUserData by remember { mutableStateOf<GoogleUserData?>(null) }
    var activeFirebaseUser by remember { mutableStateOf<FirebaseUser?>(null) }
    var wizardError by remember { mutableStateOf<String?>(null) }
    var wizardErrorKey by remember { mutableIntStateOf(0) }

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
            wizardError = null
            onComplete()
        }
    }

    suspend fun routeSignedInOAuthUser(
        user: FirebaseUser,
        verifiedEmail: String,
        googleId: String,
        displayName: String,
        photoUrl: String
    ) {
        activeFirebaseUser = user
        wizardError = null
        val gData = GoogleUserData(
            email = verifiedEmail,
            googleId = googleId,
            displayName = displayName,
            photoUrl = photoUrl
        )
        activeGoogleUserData = gData

        val existing = discoverRepo.getUserProfile(user.uid)
            ?: discoverRepo.getUserByEmail(verifiedEmail)
            ?: discoverRepo.getUserByGoogleId(googleId)

        if (authIntentMode == AuthIntentMode.LOGIN) {
            if (existing != null) {
                detectedProfile = existing
                currentScreenState = AuthScreenState.ACCOUNT_CONFIRMATION
            } else {
                currentScreenState = AuthScreenState.ONBOARDING_WIZARD
            }
        } else {
            if (existing != null) {
                detectedProfile = existing
                currentScreenState = AuthScreenState.WELCOME
                showDuplicateDialog = true
            } else {
                currentScreenState = AuthScreenState.ONBOARDING_WIZARD
            }
        }
    }

    fun launchWebGoogleOAuthFallback() {
        val activity = context as? Activity
        if (activity == null) {
            isGoogleLoading = false
            currentScreenState = AuthScreenState.WELCOME
            Toast.makeText(context, "Google Sign-In window unavailable", Toast.LENGTH_SHORT).show()
            return
        }
        isGoogleLoading = true
        val provider = OAuthProvider.newBuilder("google.com")
            .addCustomParameter("prompt", "select_account")
            .build()
        auth.startActivityForSignInWithProvider(activity, provider)
            .addOnSuccessListener { authResult ->
                scope.launch {
                    currentScreenState = AuthScreenState.CHECKING_DATABASE
                    try {
                        val user = authResult.user
                        if (user != null) {
                            routeSignedInOAuthUser(
                                user = user,
                                verifiedEmail = user.email ?: "",
                                googleId = user.uid,
                                displayName = user.displayName ?: "",
                                photoUrl = user.photoUrl?.toString() ?: ""
                            )
                        } else {
                            Toast.makeText(context, "Google authentication failed", Toast.LENGTH_SHORT).show()
                            currentScreenState = AuthScreenState.WELCOME
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, e.localizedMessage ?: "Google Sign-In failed", Toast.LENGTH_LONG).show()
                        currentScreenState = AuthScreenState.WELCOME
                    } finally {
                        isGoogleLoading = false
                    }
                }
            }
            .addOnFailureListener { e ->
                isGoogleLoading = false
                currentScreenState = AuthScreenState.WELCOME
                Toast.makeText(context, e.localizedMessage ?: "Google Sign-In cancelled or failed", Toast.LENGTH_LONG).show()
            }
    }

    // Recover interrupted OAuth browser flow if Activity restarted mid-sign-in
    LaunchedEffect(Unit) {
        auth.pendingAuthResult?.addOnSuccessListener { authResult ->
            val user = authResult.user ?: return@addOnSuccessListener
            scope.launch {
                isGoogleLoading = true
                currentScreenState = AuthScreenState.CHECKING_DATABASE
                try {
                    routeSignedInOAuthUser(
                        user = user,
                        verifiedEmail = user.email ?: "",
                        googleId = user.uid,
                        displayName = user.displayName ?: "",
                        photoUrl = user.photoUrl?.toString() ?: ""
                    )
                } finally {
                    isGoogleLoading = false
                }
            }
        }
    }

    // Google Sign-In Launcher (always inspects ApiException status codes so DEVELOPER_ERROR falls back cleanly)
    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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
                    routeSignedInOAuthUser(
                        user = user,
                        verifiedEmail = account.email ?: user.email ?: "",
                        googleId = account.id ?: user.uid,
                        displayName = account.displayName ?: user.displayName ?: "",
                        photoUrl = account.photoUrl?.toString() ?: user.photoUrl?.toString() ?: ""
                    )
                } else {
                    Toast.makeText(context, "Google authentication failed", Toast.LENGTH_SHORT).show()
                    currentScreenState = AuthScreenState.WELCOME
                }
                isGoogleLoading = false
            } catch (e: ApiException) {
                when (e.statusCode) {
                    GoogleSignInStatusCodes.SIGN_IN_CANCELLED,
                    CommonStatusCodes.CANCELED -> {
                        isGoogleLoading = false
                        currentScreenState = AuthScreenState.WELCOME
                    }
                    CommonStatusCodes.DEVELOPER_ERROR,
                    GoogleSignInStatusCodes.SIGN_IN_FAILED,
                    GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS -> {
                        // Play Services OAuth client mismatch or unavailable -> seamlessly use Firebase Web OAuth
                        currentScreenState = AuthScreenState.WELCOME
                        launchWebGoogleOAuthFallback()
                    }
                    else -> {
                        isGoogleLoading = false
                        Toast.makeText(
                            context,
                            "Google Sign-In error (${e.statusCode}): ${e.localizedMessage ?: "Please try again"}",
                            Toast.LENGTH_LONG
                        ).show()
                        currentScreenState = AuthScreenState.WELCOME
                    }
                }
            } catch (e: Exception) {
                isGoogleLoading = false
                Toast.makeText(context, e.localizedMessage ?: "Google Sign-In failed", Toast.LENGTH_LONG).show()
                currentScreenState = AuthScreenState.WELCOME
            }
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
        } catch (_: Exception) {
            launchWebGoogleOAuthFallback()
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
            email = "guest@oscellion.app",
            lastLoginMillis = System.currentTimeMillis(),
            gender = "UNSPECIFIED"
        )
        SavedAccountManager.saveAccount(context, guestAccount)
        Toast.makeText(context, "Exploring Studio as Guest", Toast.LENGTH_SHORT).show()
        onLoginSuccess()
    }

    BackHandler(enabled = currentScreenState != AuthScreenState.WELCOME) {
        if (currentScreenState == AuthScreenState.ACCOUNT_CONFIRMATION) {
            purgeClientSession {
                currentScreenState = AuthScreenState.WELCOME
            }
        } else {
            currentScreenState = AuthScreenState.WELCOME
        }
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
                        isSubmitting = isConfirmingLoading,
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
                            initialSignupUsername = ""
                            initialSignupPassword = ""
                            activeGoogleUserData = null
                            activeFirebaseUser = null
                            currentScreenState = AuthScreenState.ONBOARDING_WIZARD
                        },
                        onApple = { launchAppleSignIn() },
                        onSignInExisting = {
                            currentScreenState = AuthScreenState.SIGN_IN_EMAIL
                        },
                        onExploreGuest = { enterAsGuest() },
                        onTerms = { showTermsModal = true },
                        onSignUpWithCredentials = { uname, pwd ->
                            initialSignupUsername = uname.trim().removePrefix("@").lowercase()
                            initialSignupPassword = pwd
                            activeGoogleUserData = null
                            activeFirebaseUser = null
                            wizardError = null
                            currentScreenState = AuthScreenState.ONBOARDING_WIZARD
                        },
                        onSignInWithCredentials = { uname, pwd ->
                            scope.launch {
                                isConfirmingLoading = true
                                try {
                                    val clean = uname.trim().removePrefix("@").lowercase()
                                    val isEmailFormat = clean.contains("@")
                                    val defaultCustomEmail = "$clean@oscellion.com"
                                    val resolvedProfile = if (!isEmailFormat) discoverRepo.getUserByUsername(clean) else null
                                    val resolvedEmail = when {
                                        isEmailFormat -> clean
                                        resolvedProfile != null && resolvedProfile.customEmail.isNotBlank() -> resolvedProfile.customEmail.lowercase()
                                        else -> defaultCustomEmail
                                    }
                                    val authResult = try {
                                        auth.signInWithEmailAndPassword(resolvedEmail, pwd).await()
                                    } catch (e: Exception) {
                                        if (!isEmailFormat && resolvedProfile != null && resolvedProfile.email.isNotBlank() && !resolvedProfile.email.equals(resolvedEmail, ignoreCase = true)) {
                                            auth.signInWithEmailAndPassword(resolvedProfile.email.lowercase(), pwd).await()
                                        } else {
                                            throw e
                                        }
                                    }
                                    val user = authResult.user
                                    if (user != null) {
                                        val prof = discoverRepo.getUserProfile(user.uid) ?: resolvedProfile
                                        val savedAcc = SavedAccount(
                                            uid = user.uid,
                                            username = prof?.username ?: clean.substringBefore("@"),
                                            displayName = prof?.displayName ?: clean.substringBefore("@"),
                                            avatarUrl = prof?.avatarUrl ?: "",
                                            email = user.email ?: resolvedEmail,
                                            lastLoginMillis = System.currentTimeMillis(),
                                            gender = prof?.gender ?: "UNSPECIFIED"
                                        )
                                        SavedAccountManager.saveAccount(context, savedAcc)
                                        com.chroma.studio.ChatStateManager.setCurrentUserId(user.uid)
                                        com.chroma.studio.services.ChromaMessagingService.syncTokenForCurrentUser()
                                        Toast.makeText(context, "Welcome back, ${savedAcc.displayName}!", Toast.LENGTH_SHORT).show()
                                        onLoginSuccess()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Sign in failed: ${e.localizedMessage ?: "Invalid credentials"}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isConfirmingLoading = false
                                }
                            }
                        }
                    )
                }

                AuthScreenState.SIGN_IN_EMAIL -> {
                    SignInScreen(
                        onBack = { currentScreenState = AuthScreenState.WELCOME },
                        onLoginSuccess = onLoginSuccess,
                        onCreateAccount = {
                            initialSignupUsername = ""
                            initialSignupPassword = ""
                            activeGoogleUserData = null
                            activeFirebaseUser = null
                            currentScreenState = AuthScreenState.ONBOARDING_WIZARD
                        },
                        onGoogleSignIn = { launchGoogleSignIn(AuthIntentMode.LOGIN) },
                        onAppleSignIn = { launchAppleSignIn() },
                        appleLoading = isAppleLoading,
                        onExploreGuest = { enterAsGuest() },
                        onOpenTerms = { showTermsModal = true },
                        savedAccounts = savedAccounts,
                        googleLoading = isGoogleLoading,
                        discoverRepo = discoverRepo
                    )
                }

                AuthScreenState.CHECKING_DATABASE -> {
                    val colors = com.chroma.studio.ui.theme.LocalOscellionColors.current
                    val isDark = colors.isDark
                    val screenBg = if (isDark) Color(0xFF09090B) else Color(0xFFFAFAFA)
                    val surfaceBg = if (isDark) Color(0xFF13131A) else Color(0xFFFFFFFF)
                    val textPrimary = if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B)
                    val textMuted = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(screenBg)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp, vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_oscellion_app_icon),
                                contentDescription = "Oscellion",
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            CircularProgressIndicator(
                                modifier = Modifier.size(26.dp),
                                color = colors.primary,
                                strokeWidth = 2.4.dp
                            )
                            Text(
                                text = "Verifying Oscellion account…",
                                color = textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.2).sp
                            )
                            Text(
                                text = "Checking directory & workspace profile",
                                color = textMuted,
                                fontSize = 13.sp
                            )
                        }
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
                        displayName = activeFirebaseUser?.displayName ?: initialSignupUsername,
                        photoUrl = activeFirebaseUser?.photoUrl?.toString() ?: ""
                    )
                    val isOAuthFlow = activeFirebaseUser != null || gData.googleId.isNotBlank()

                    OnboardingWizard(
                        googleData = gData,
                        initialUsername = initialSignupUsername,
                        initialPassword = initialSignupPassword,
                        isOAuthUser = isOAuthFlow,
                        externalError = wizardError,
                        errorResetKey = wizardErrorKey,
                        discoverRepo = discoverRepo,
                        onBackToWelcome = {
                            purgeClientSession {
                                currentScreenState = AuthScreenState.WELCOME
                            }
                        },
                        onComplete = { output ->
                            scope.launch {
                                wizardError = null
                                try {
                                    val cleanUsername = output.username.trim().removePrefix("@").lowercase()
                                    val customEmail = if (output.customEmail.isNotBlank()) output.customEmail.trim().lowercase() else "$cleanUsername@oscellion.com"

                                    // Verify username availability before creating Firebase Auth account so no orphan auth record is created
                                    val existingOwner = discoverRepo.getUserByUsername(cleanUsername)
                                    val currentExistingUid = activeFirebaseUser?.uid ?: auth.currentUser?.uid
                                    if (existingOwner != null && existingOwner.uid != currentExistingUid) {
                                        wizardError = "Username @$cleanUsername is already taken. Choose another handle."
                                        wizardErrorKey++
                                        return@launch
                                    }

                                    var uid = currentExistingUid
                                    if (uid.isNullOrBlank()) {
                                        if (output.password.isBlank()) {
                                            wizardError = "Please set a password for your studio account."
                                            wizardErrorKey++
                                            return@launch
                                        }
                                        val authResult = auth.createUserWithEmailAndPassword(customEmail, output.password).await()
                                        val newUser = authResult.user ?: throw IllegalStateException("Failed to create user account")
                                        uid = newUser.uid
                                        activeFirebaseUser = newUser
                                    } else {
                                        // Optional credential linking when Google/Apple user also set a studio password
                                        val currentUser = auth.currentUser
                                        if (currentUser != null && output.password.isNotBlank()) {
                                            try {
                                                val emailCred = EmailAuthProvider.getCredential(customEmail, output.password)
                                                currentUser.linkWithCredential(emailCred).await()
                                            } catch (_: Exception) {
                                                try {
                                                    currentUser.updatePassword(output.password).await()
                                                } catch (_: Exception) {}
                                            }
                                        }
                                    }

                                    try {
                                        auth.currentUser?.updateProfile(
                                            UserProfileChangeRequest.Builder()
                                                .setDisplayName(output.displayName)
                                                .build()
                                        )?.await()
                                    } catch (_: Exception) {}

                                    // Upload custom avatar to Supabase Storage if user selected an image
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
                                        username = cleanUsername,
                                        dob = output.dobMillis,
                                        avatarUrl = finalAvatar,
                                        interests = output.interests,
                                        gender = output.gender,
                                        email = if (output.email.isNotBlank()) output.email else customEmail,
                                        customEmail = customEmail,
                                        googleId = output.googleId,
                                        bio = output.bio,
                                        isOnboarded = true
                                    )

                                    if (success) {
                                        val newProfile = discoverRepo.getUserProfile(uid)
                                        if (newProfile != null) {
                                            SavedAccountManager.saveFromProfile(context, newProfile, customEmail)
                                        } else {
                                            val savedAcc = SavedAccount(
                                                uid = uid,
                                                username = cleanUsername,
                                                displayName = output.displayName,
                                                avatarUrl = finalAvatar,
                                                email = customEmail,
                                                lastLoginMillis = System.currentTimeMillis(),
                                                gender = output.gender
                                            )
                                            SavedAccountManager.saveAccount(context, savedAcc)
                                        }
                                        com.chroma.studio.ChatStateManager.setCurrentUserId(uid)
                                        com.chroma.studio.services.ChromaMessagingService.syncTokenForCurrentUser()
                                        Toast.makeText(context, "Welcome to Oscellion, ${output.displayName}!", Toast.LENGTH_SHORT).show()
                                        onLoginSuccess()
                                    } else {
                                        wizardError = "Could not save your profile to Oscellion. Tap Continue to retry."
                                        wizardErrorKey++
                                    }
                                } catch (e: Exception) {
                                    wizardError = e.localizedMessage ?: e.message ?: "Registration failed. Please check your connection."
                                    wizardErrorKey++
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

        // Terms & Privacy Policy Modal
        if (showTermsModal) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showTermsModal = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                com.chroma.studio.ui.screens.PrivacyPolicyTermsScreen(
                    isFirstLaunch = false,
                    onAccept = { showTermsModal = false },
                    onBack = { showTermsModal = false }
                )
            }
        }
    }
}
