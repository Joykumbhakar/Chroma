package com.chroma.studio.ui.screens

import android.Manifest
import android.app.Activity
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.PermissionChecker
import coil.compose.AsyncImage
import com.chroma.studio.R
import com.chroma.studio.data.DiscoverRepository
import com.chroma.studio.data.SupabaseClient
import com.chroma.studio.model.UserProfile
import com.chroma.studio.ui.components.GlassIconButton
import com.chroma.studio.ui.components.GlassTextButton
import com.chroma.studio.ui.components.OscellionBackground
import com.chroma.studio.ui.components.OscellionLoadingSpinner
import com.chroma.studio.ui.components.OscellionLogo
import com.chroma.studio.ui.components.getDefaultAvatarDrawableRes
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.chroma.studio.data.SavedAccount
import com.chroma.studio.data.SavedAccountManager
import com.chroma.studio.utils.ImageUtils
import com.chroma.studio.utils.compressImageToUnder50KB
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ─── Password Validation ──────────────────────────────────
data class PasswordStrength(
    val hasMinLength: Boolean = false,
    val hasMaxLength: Boolean = true,
    val hasUppercase: Boolean = false,
    val hasLowercase: Boolean = false,
    val hasDigit: Boolean = false,
    val hasSpecial: Boolean = false
) {
    val isValid get() = hasMinLength && hasMaxLength && hasUppercase && hasLowercase && hasDigit && hasSpecial
}

fun checkPasswordStrength(password: String): PasswordStrength = PasswordStrength(
    hasMinLength = password.length >= 8,
    hasMaxLength = password.length <= 24,
    hasUppercase = password.any { it.isUpperCase() },
    hasLowercase = password.any { it.isLowerCase() },
    hasDigit = password.any { it.isDigit() },
    hasSpecial = password.any { !it.isLetterOrDigit() }
)

// ─── Avatar Upload (Aggressively Compressed < 25KB for Supabase Free Tier) ─────────────────────────
suspend fun uploadAvatarToSupabase(uid: String, imageBytes: ByteArray): String? {
    return try {
        val compressedBytes = ImageUtils.compressAvatarBytesForSupabase(imageBytes) ?: imageBytes
        val bucket = SupabaseClient.client.storage["avatars"]
        val path = "$uid/avatar.jpg"
        bucket.upload(path, compressedBytes, upsert = true)
        bucket.publicUrl(path)
    } catch (e: Exception) {
        Log.e("AvatarUpload", "Upload failed", e)
        null
    }
}

enum class AuthStep {
    INITIAL,
    GOOGLE_MATCH,
    LOGIN_MISSING_AVATAR,
    SIGNUP_USERNAME,
    SIGNUP_NAME,
    SIGNUP_DOB,
    SIGNUP_GENDER,
    SIGNUP_AVATAR,
    SIGNUP_INTERESTS
}

// ─── Direct Unified UI Components (No Cards, No Pills) ─────────────────────────

@Composable
fun CupertinoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true,
    isError: Boolean = false,
    enabled: Boolean = true
) {
    val colors = LocalOscellionColors.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = colors.textMuted.copy(alpha = 0.55f), fontSize = 15.sp) },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.glassBorder.copy(alpha = 0.7f),
            errorBorderColor = colors.error,
            focusedTextColor = colors.textMain,
            unfocusedTextColor = colors.textMain,
            disabledTextColor = colors.textMuted,
            cursorColor = colors.primary,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            errorContainerColor = Color.Transparent
        ),
        singleLine = singleLine,
        isError = isError,
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation
    )
}

@Composable
fun CupertinoPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    containerColor: Color = LocalOscellionColors.current.primary,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            disabledContainerColor = containerColor.copy(alpha = 0.35f),
            contentColor = Color.White,
            disabledContentColor = Color.White.copy(alpha = 0.6f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
        enabled = enabled && !isLoading
    ) {
        if (isLoading) {
            OscellionLoadingSpinner(Modifier.size(20.dp), color = Color.White)
        } else {
            content()
        }
    }
}

@Composable
fun CupertinoPrimaryButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    containerColor: Color = LocalOscellionColors.current.primary
) {
    CupertinoPrimaryButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        isLoading = isLoading,
        containerColor = containerColor
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.2).sp
        )
    }
}

@Composable
fun CupertinoSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val colors = LocalOscellionColors.current
    Surface(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, colors.glassBorder.copy(alpha = 0.75f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content
        )
    }
}

// ─── Main LoginScreen Composable ─────────────────────────

@OptIn(ExperimentalAnimationApi::class, ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val colors = LocalOscellionColors.current
    val scope = rememberCoroutineScope()
    var currentStep by remember { mutableStateOf(AuthStep.INITIAL) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val discoverRepo = remember { DiscoverRepository() }
    var savedAccounts by remember { mutableStateOf(SavedAccountManager.getSavedAccounts(context)) }

    // Shared State
    var googleUser by remember { mutableStateOf<FirebaseUser?>(null) }
    var matchedProfile by remember { mutableStateOf<UserProfile?>(null) }

    // Login State
    var loginUsernameInput by remember { mutableStateOf("") }
    var loginPasswordInput by remember { mutableStateOf("") }
    var loginPasswordVisible by remember { mutableStateOf(false) }

    // Signup State
    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var signupPasswordVisible by remember { mutableStateOf(false) }
    var isUsernameAvailable by remember { mutableStateOf<Boolean?>(null) }
    var isCheckingUsername by remember { mutableStateOf(false) }
    var usernameSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var displayNameInput by remember { mutableStateOf("") }
    var selectedInterests by remember { mutableStateOf(setOf<String>()) }
    var selectedDobMillis by remember { mutableStateOf<Long?>(null) }
    var showDobPicker by remember { mutableStateOf(false) }
    var selectedGender by remember { mutableStateOf("") }
    var selectedAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var selectedAvatarBytes by remember { mutableStateOf<ByteArray?>(null) }

    val passwordStrength = remember(passwordInput) { checkPasswordStrength(passwordInput) }

    val interestOptions = listOf(
        "Technology & Science", "Creativity & Art",
        "Application Designing", "UI/UX",
        "Photography", "Music",
        "Gaming", "Writing",
        "Education", "Lifestyle",
        "Finance", "Health & Fitness"
    )
    val genderOptions = listOf("Male", "Female", "Non-binary", "Prefer not to say")

    // Camera setup with runtime permission
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraImageUri != null) {
            selectedAvatarUri = cameraImageUri
            scope.launch { selectedAvatarBytes = compressImageToUnder50KB(context, cameraImageUri!!) }
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedAvatarUri = uri
            scope.launch { selectedAvatarBytes = compressImageToUnder50KB(context, uri) }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val tmpFile = File(context.cacheDir, "avatar_tmp.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", tmpFile)
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        } else {
            errorMessage = "Camera permission denied. Please enable it in Settings."
        }
    }

    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PermissionChecker.PERMISSION_GRANTED) {
            val tmpFile = File(context.cacheDir, "avatar_tmp.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", tmpFile)
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Google Auth Launcher
    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            scope.launch {
                isLoading = true
                errorMessage = null
                try {
                    val account = task.await()
                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                    val authResult = FirebaseAuth.getInstance().signInWithCredential(credential).await()
                    val user = authResult.user
                    if (user != null) {
                        val profile = discoverRepo.getUserProfile(user.uid)
                        if (profile != null) {
                            SavedAccountManager.saveFromProfile(context, profile, user.email)
                            matchedProfile = profile
                            currentStep = AuthStep.GOOGLE_MATCH
                        } else {
                            googleUser = user
                            val baseName = user.email?.substringBefore("@")?.filter { it.isLetterOrDigit() }?.lowercase() ?: ""
                            usernameInput = baseName
                            displayNameInput = user.displayName ?: ""
                            if (user.photoUrl != null) {
                                selectedAvatarUri = user.photoUrl
                            }
                            currentStep = AuthStep.SIGNUP_USERNAME
                        }
                    } else {
                        errorMessage = "Login failed: No user found"
                    }
                } catch (e: Exception) {
                    errorMessage = e.message ?: "Login failed"
                } finally {
                    isLoading = false
                }
            }
        } else {
            isLoading = false
        }
    }

    fun loginWithGoogle() {
        isLoading = true
        errorMessage = null
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail().build()
            val client = GoogleSignIn.getClient(context, gso)
            client.signOut().addOnCompleteListener { googleLauncher.launch(client.signInIntent) }
        } catch (e: Exception) {
            isLoading = false
            errorMessage = "Google Sign In initialization failed: ${e.message}"
        }
    }

    // Username availability debounce & suggestions
    LaunchedEffect(usernameInput) {
        if (usernameInput.length >= 3) {
            isCheckingUsername = true
            isUsernameAvailable = null
            usernameSuggestions = emptyList()
            delay(400)
            val isAvail = discoverRepo.checkUsernameAvailable(usernameInput)
            isUsernameAvailable = isAvail
            if (!isAvail) {
                usernameSuggestions = discoverRepo.suggestAvailableUsernames(usernameInput)
            }
            isCheckingUsername = false
        } else if (usernameInput.isNotEmpty()) {
            isUsernameAvailable = false
            usernameSuggestions = emptyList()
        } else {
            isUsernameAvailable = null
            usernameSuggestions = emptyList()
        }
    }

    fun completeLogin(profile: UserProfile?) {
        if (profile != null) {
            val email = googleUser?.email ?: FirebaseAuth.getInstance().currentUser?.email
            SavedAccountManager.saveFromProfile(context, profile, email)
            if (profile.avatarUrl.isBlank()) {
                matchedProfile = profile
                currentStep = AuthStep.LOGIN_MISSING_AVATAR
            } else {
                onLoginSuccess()
            }
        } else {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                googleUser = user
                val baseName = user.email?.substringBefore("@")?.filter { it.isLetterOrDigit() }?.lowercase() ?: ""
                usernameInput = baseName
                displayNameInput = user.displayName ?: ""
                currentStep = AuthStep.SIGNUP_USERNAME
            } else {
                errorMessage = "Login failed: No user found"
            }
        }
    }

    fun handleBack() {
        errorMessage = null
        when (currentStep) {
            AuthStep.INITIAL -> onCancel()
            AuthStep.GOOGLE_MATCH -> {
                FirebaseAuth.getInstance().signOut()
                currentStep = AuthStep.INITIAL
                matchedProfile = null
            }
            AuthStep.LOGIN_MISSING_AVATAR -> {
                FirebaseAuth.getInstance().signOut()
                currentStep = AuthStep.INITIAL
            }
            AuthStep.SIGNUP_USERNAME -> {
                FirebaseAuth.getInstance().signOut()
                currentStep = AuthStep.INITIAL
                selectedAvatarUri = null
                selectedAvatarBytes = null
            }
            AuthStep.SIGNUP_NAME -> currentStep = AuthStep.SIGNUP_USERNAME
            AuthStep.SIGNUP_DOB -> currentStep = AuthStep.SIGNUP_NAME
            AuthStep.SIGNUP_GENDER -> currentStep = AuthStep.SIGNUP_DOB
            AuthStep.SIGNUP_AVATAR -> currentStep = AuthStep.SIGNUP_GENDER
            AuthStep.SIGNUP_INTERESTS -> currentStep = AuthStep.SIGNUP_AVATAR
        }
    }

    val maxDobMillis = remember {
        Calendar.getInstance().apply { add(Calendar.YEAR, -13) }.timeInMillis
    }

    val totalSignupSteps = 6
    val currentSignupIndex = when (currentStep) {
        AuthStep.SIGNUP_USERNAME -> 1
        AuthStep.SIGNUP_NAME -> 2
        AuthStep.SIGNUP_DOB -> 3
        AuthStep.SIGNUP_GENDER -> 4
        AuthStep.SIGNUP_AVATAR -> 5
        AuthStep.SIGNUP_INTERESTS -> 6
        else -> 0
    }

    OscellionBackground {
        Scaffold(
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (currentStep == AuthStep.INITIAL) {
                        GlassIconButton(onClick = onCancel) {
                            Icon(Lucide.X, contentDescription = "Close", tint = colors.textMain, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.weight(1f))
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { handleBack() }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Lucide.ChevronLeft, contentDescription = "Back", tint = colors.primary, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("Back", color = colors.primary, fontSize = 16.sp, fontWeight = FontWeight.Normal)
                        }

                        if (currentSignupIndex > 0) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (i in 1..totalSignupSteps) {
                                    val active = i <= currentSignupIndex
                                    Box(
                                        modifier = Modifier
                                            .width(if (i == currentSignupIndex) 18.dp else 6.dp)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(if (active) colors.primary else colors.textMuted.copy(alpha = 0.25f))
                                            .animateContentSize()
                                    )
                                }
                            }
                        } else {
                            Spacer(Modifier.width(40.dp))
                        }
                    }
                }
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(8.dp))

                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        slideInHorizontally(initialOffsetX = { it / 2 }) + fadeIn() togetherWith
                                slideOutHorizontally(targetOffsetX = { -it / 2 }) + fadeOut()
                    },
                    label = "AuthSteps"
                ) { step ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        when (step) {

                            // ════════════════════════════════════════════════
                            // INITIAL — Login Screen
                            // ════════════════════════════════════════════════
                            AuthStep.INITIAL -> {
                                Spacer(Modifier.height(24.dp))

                                Box(
                                    modifier = Modifier
                                        .size(82.dp)
                                        .clip(RoundedCornerShape(22.dp))
                                        .background(if (colors.isDark) Color(0xFF0A0A0A) else Color(0xFFF2F2F7))
                                        .border(1.dp, colors.glassBorder, RoundedCornerShape(22.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    OscellionLogo(
                                        size = 50.dp,
                                        outerColor = if (colors.isDark) Color.White else Color(0xFF111827),
                                        innerColor = Color(0xFF007AFF)
                                    )
                                }

                                Spacer(Modifier.height(24.dp))

                                if (savedAccounts.isNotEmpty()) {
                                    Text(
                                        text = "Choose an Account",
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textMain,
                                        textAlign = TextAlign.Center,
                                        letterSpacing = (-0.5).sp
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = "Select an existing profile or continue with another Google account.",
                                        fontSize = 14.sp,
                                        color = colors.textMuted,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )

                                    Spacer(Modifier.height(28.dp))

                                    // Saved Accounts list (Cardless, uniform background, 12dp rounded corners)
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        savedAccounts.forEach { account ->
                                            val defaultAvatarRes = getDefaultAvatarDrawableRes(account.gender)
                                            Surface(
                                                onClick = { loginWithGoogle() },
                                                modifier = Modifier.fillMaxWidth().height(68.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                color = colors.primary.copy(alpha = 0.08f),
                                                border = BorderStroke(1.dp, colors.glassBorder.copy(alpha = 0.75f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(44.dp)
                                                            .clip(CircleShape)
                                                            .border(1.dp, colors.primary.copy(alpha = 0.4f), CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (account.avatarUrl.isNotBlank()) {
                                                            AsyncImage(
                                                                model = account.avatarUrl,
                                                                contentDescription = null,
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentScale = ContentScale.Crop
                                                            )
                                                        } else if (defaultAvatarRes != null) {
                                                            Image(
                                                                painter = painterResource(defaultAvatarRes),
                                                                contentDescription = null,
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentScale = ContentScale.Crop
                                                            )
                                                        } else {
                                                            Icon(Lucide.User, null, tint = colors.textMuted, modifier = Modifier.size(22.dp))
                                                        }
                                                    }

                                                    Spacer(Modifier.width(12.dp))

                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = account.displayName,
                                                            fontSize = 15.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = colors.textMain,
                                                            maxLines = 1,
                                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = "@${account.username}",
                                                            fontSize = 13.sp,
                                                            color = colors.textMuted,
                                                            maxLines = 1,
                                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                        )
                                                    }

                                                    IconButton(
                                                        onClick = {
                                                            SavedAccountManager.removeAccount(context, account.uid)
                                                            savedAccounts = SavedAccountManager.getSavedAccounts(context)
                                                        },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Lucide.X,
                                                            contentDescription = "Remove account from device",
                                                            tint = colors.textMuted.copy(alpha = 0.6f),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (errorMessage != null) {
                                        Spacer(Modifier.height(16.dp))
                                        Text(
                                            text = errorMessage!!,
                                            color = colors.error,
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    Spacer(Modifier.height(28.dp))

                                    CupertinoPrimaryButton(
                                        onClick = { loginWithGoogle() },
                                        isLoading = isLoading
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_google_logo),
                                            contentDescription = "Google",
                                            modifier = Modifier.size(20.dp),
                                            tint = Color.Unspecified
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = "Use Another Account",
                                            color = Color.White,
                                            fontSize = 15.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Spacer(Modifier.height(10.dp))

                                    GlassTextButton(
                                        onClick = { loginWithGoogle() }
                                    ) {
                                        Text(
                                            text = "Create New Account",
                                            color = colors.primary,
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "Welcome to Oscellion",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textMain,
                                        textAlign = TextAlign.Center,
                                        letterSpacing = (-0.5).sp
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "Craft high-fidelity gradients, discover artwork, and message with end-to-end encryption.",
                                        fontSize = 15.sp,
                                        color = colors.textMuted,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 22.sp,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )

                                    Spacer(Modifier.height(36.dp))

                                    // Feature highlights (Cardless, uniform background)
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Lucide.Palette, null, tint = colors.primary, modifier = Modifier.size(22.dp))
                                            Spacer(Modifier.width(14.dp))
                                            Column {
                                                Text("Creative Canvas", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = colors.textMain)
                                                Text("Multi-layer gradient studio with live rendering", fontSize = 13.sp, color = colors.textMuted)
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Lucide.Lock, null, tint = colors.primary, modifier = Modifier.size(22.dp))
                                            Spacer(Modifier.width(14.dp))
                                            Column {
                                                Text("Private Messaging", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = colors.textMain)
                                                Text("End-to-end encrypted chats, audio, and media", fontSize = 13.sp, color = colors.textMuted)
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Lucide.Zap, null, tint = colors.primary, modifier = Modifier.size(22.dp))
                                            Spacer(Modifier.width(14.dp))
                                            Column {
                                                Text("Instant Realtime", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = colors.textMain)
                                                Text("Ephemeral thought notes, stories, and feed", fontSize = 13.sp, color = colors.textMuted)
                                            }
                                        }
                                    }

                                    if (errorMessage != null) {
                                        Spacer(Modifier.height(18.dp))
                                        Text(
                                            text = errorMessage!!,
                                            color = colors.error,
                                            fontSize = 13.5.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    Spacer(Modifier.height(36.dp))

                                    CupertinoPrimaryButton(
                                        onClick = { loginWithGoogle() },
                                        isLoading = isLoading
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_google_logo),
                                            contentDescription = "Google",
                                            modifier = Modifier.size(20.dp),
                                            tint = Color.Unspecified
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = "Continue with Google",
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Spacer(Modifier.height(24.dp))
                                Text(
                                    text = "By continuing, you agree to Chroma's Terms of Service and Privacy Policy.",
                                    fontSize = 12.sp,
                                    color = colors.textMuted.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }

                            // ════════════════════════════════════════════════
                            // GOOGLE_MATCH — Existing Account Match
                            // ════════════════════════════════════════════════
                            AuthStep.GOOGLE_MATCH -> {
                                matchedProfile?.let { profile ->
                                    Spacer(Modifier.height(24.dp))
                                    Text("Is this your account?", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.textMain, letterSpacing = (-0.5).sp)
                                    Spacer(Modifier.height(8.dp))
                                    Text("We found an existing Chroma profile linked to your Google account.", color = colors.textMuted, fontSize = 14.sp, textAlign = TextAlign.Center)

                                    Spacer(Modifier.height(32.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(96.dp)
                                            .clip(CircleShape)
                                            .border(2.dp, colors.primary.copy(alpha = 0.6f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (profile.avatarUrl.isNotBlank()) {
                                            AsyncImage(profile.avatarUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                        } else {
                                            Icon(Lucide.User, null, tint = colors.textMuted, modifier = Modifier.size(44.dp))
                                        }
                                    }
                                    Spacer(Modifier.height(14.dp))
                                    Text(profile.displayName, color = colors.textMain, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    Text("@${profile.username}", color = colors.textMuted, fontSize = 15.sp)

                                    Spacer(Modifier.height(36.dp))
                                    CupertinoPrimaryButton(onClick = { completeLogin(profile) }, text = "Continue as ${profile.displayName}")
                                    Spacer(Modifier.height(12.dp))
                                    GlassTextButton(onClick = {
                                        FirebaseAuth.getInstance().signOut()
                                        currentStep = AuthStep.INITIAL
                                        matchedProfile = null
                                    }) {
                                        Text("This isn't my account", color = colors.primary, fontSize = 15.sp)
                                    }
                                }
                            }

                            // ════════════════════════════════════════════════
                            // LOGIN_MISSING_AVATAR
                            // ════════════════════════════════════════════════
                            AuthStep.LOGIN_MISSING_AVATAR -> {
                                Spacer(Modifier.height(16.dp))
                                CupertinoAvatarPickerStep(
                                    title = "Set Profile Photo",
                                    subtitle = "Add a photo to make your profile recognizable to friends.",
                                    selectedUri = selectedAvatarUri,
                                    gender = selectedGender,
                                    onGallery = { galleryLauncher.launch("image/*") },
                                    onCamera = { launchCamera() }
                                )
                                Spacer(Modifier.height(36.dp))
                                CupertinoPrimaryButton(
                                    onClick = {
                                        scope.launch {
                                            isLoading = true
                                            try {
                                                val uid = FirebaseAuth.getInstance().currentUser?.uid
                                                if (uid != null && selectedAvatarBytes != null) {
                                                    val url = uploadAvatarToSupabase(uid, selectedAvatarBytes!!)
                                                    if (url != null) {
                                                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                                            .collection("users").document(uid)
                                                            .update("avatarUrl", url).await()
                                                    }
                                                }
                                                onLoginSuccess()
                                            } finally { isLoading = false }
                                        }
                                    },
                                    isLoading = isLoading,
                                    text = if (selectedAvatarUri == null) "Skip for Now" else "Save & Continue"
                                )
                            }

                            // ════════════════════════════════════════════════
                            // SIGNUP_USERNAME — Step 1: Choose Username
                            // ════════════════════════════════════════════════
                            AuthStep.SIGNUP_USERNAME -> {
                                Spacer(Modifier.height(12.dp))
                                Text("Choose Your Username", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.textMain, letterSpacing = (-0.5).sp)
                                Spacer(Modifier.height(6.dp))
                                Text("Pick a unique handle for your profile and messages.", color = colors.textMuted, fontSize = 14.sp, textAlign = TextAlign.Center)

                                Spacer(Modifier.height(28.dp))

                                CupertinoTextField(
                                    value = usernameInput,
                                    onValueChange = {
                                        val filtered = it.filter { c -> c.isLetterOrDigit() }.lowercase()
                                        if (filtered.length <= 30) usernameInput = filtered
                                    },
                                    placeholder = "Username",
                                    leadingIcon = { Icon(Lucide.AtSign, null, tint = colors.textMuted, modifier = Modifier.size(18.dp)) },
                                    trailingIcon = {
                                        when {
                                            isCheckingUsername -> OscellionLoadingSpinner(Modifier.size(16.dp), color = colors.primary, strokeWidth = 2.dp)
                                            isUsernameAvailable == true -> Icon(Lucide.Check, null, tint = Color(0xFF34C759), modifier = Modifier.size(18.dp))
                                            isUsernameAvailable == false -> Icon(Lucide.X, null, tint = colors.error, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                )

                                if (isUsernameAvailable == false && usernameInput.length >= 3) {
                                    Spacer(Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("User already exists with this name", color = colors.error, fontSize = 12.sp)
                                        Text(
                                            text = "Sign In",
                                            color = colors.primary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.clickable {
                                                loginUsernameInput = usernameInput
                                                handleBack()
                                            }
                                        )
                                    }

                                    if (usernameSuggestions.isNotEmpty()) {
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = "Suggested handles:",
                                            color = colors.textMuted,
                                            fontSize = 12.sp,
                                            modifier = Modifier.fillMaxWidth().padding(start = 4.dp)
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            usernameSuggestions.forEach { suggestion ->
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(colors.primary.copy(alpha = 0.12f))
                                                        .border(0.5.dp, colors.primary.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                                        .clickable {
                                                            usernameInput = suggestion
                                                        }
                                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Text(
                                                        text = "@$suggestion",
                                                        color = colors.primary,
                                                        fontSize = 12.5.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (errorMessage != null) {
                                    Spacer(Modifier.height(12.dp))
                                    Text(errorMessage!!, color = colors.error, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                }

                                Spacer(Modifier.height(32.dp))

                                CupertinoPrimaryButton(
                                    onClick = {
                                        if (displayNameInput.isBlank() && !googleUser?.displayName.isNullOrBlank()) {
                                            displayNameInput = googleUser?.displayName ?: ""
                                        }
                                        if (selectedAvatarUri == null && googleUser?.photoUrl != null) {
                                            selectedAvatarUri = googleUser?.photoUrl
                                        }
                                        currentStep = AuthStep.SIGNUP_NAME
                                    },
                                    isLoading = isLoading,
                                    text = "Continue",
                                    enabled = isUsernameAvailable == true
                                )
                            }

                            // ════════════════════════════════════════════════
                            // SIGNUP_NAME — Step 2: Display Name
                            // ════════════════════════════════════════════════
                            AuthStep.SIGNUP_NAME -> {
                                Spacer(Modifier.height(12.dp))
                                Text("What's your name?", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.textMain, letterSpacing = (-0.5).sp)
                                Spacer(Modifier.height(6.dp))
                                Text("Your name will be visible to everyone on your profile.", color = colors.textMuted, fontSize = 14.sp, textAlign = TextAlign.Center)

                                Spacer(Modifier.height(28.dp))

                                CupertinoTextField(
                                    value = displayNameInput,
                                    onValueChange = { if (it.length <= 50) displayNameInput = it },
                                    placeholder = "Full Name (e.g. Alex Rivera)",
                                    leadingIcon = { Icon(Lucide.User, null, tint = colors.textMuted, modifier = Modifier.size(18.dp)) }
                                )

                                Spacer(Modifier.height(32.dp))
                                CupertinoPrimaryButton(
                                    onClick = { currentStep = AuthStep.SIGNUP_DOB },
                                    text = "Continue",
                                    enabled = displayNameInput.isNotBlank()
                                )
                            }

                            // ════════════════════════════════════════════════
                            // SIGNUP_DOB — Step 3: Date of Birth
                            // ════════════════════════════════════════════════
                            AuthStep.SIGNUP_DOB -> {
                                val dobFormatter = remember { SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()) }
                                val dobDisplay = selectedDobMillis?.let { dobFormatter.format(Date(it)) }

                                Spacer(Modifier.height(12.dp))
                                Text("When's your birthday?", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.textMain, letterSpacing = (-0.5).sp)
                                Spacer(Modifier.height(6.dp))
                                Text("This helps us verify your age and secure your account.", color = colors.textMuted, fontSize = 14.sp, textAlign = TextAlign.Center)

                                Spacer(Modifier.height(28.dp))

                                // Direct clickable selection row on the background
                                Surface(
                                    onClick = { showDobPicker = true },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.Transparent,
                                    border = BorderStroke(1.dp, colors.glassBorder.copy(alpha = 0.75f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Lucide.Calendar, null, tint = colors.primary, modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(12.dp))
                                            Text(
                                                text = dobDisplay ?: "Select Date of Birth",
                                                fontSize = 15.5.sp,
                                                color = if (dobDisplay != null) colors.textMain else colors.textMuted.copy(alpha = 0.6f),
                                                fontWeight = if (dobDisplay != null) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        }
                                        Icon(Lucide.ChevronRight, null, tint = colors.textMuted.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                                    }
                                }

                                Spacer(Modifier.height(14.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                                ) {
                                    Icon(Lucide.Info, null, tint = colors.primary, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("You must be at least 13 years old to use Chroma.", fontSize = 12.5.sp, color = colors.textMuted, fontWeight = FontWeight.Medium)
                                }

                                Spacer(Modifier.height(32.dp))
                                CupertinoPrimaryButton(
                                    onClick = { currentStep = AuthStep.SIGNUP_GENDER },
                                    text = "Continue",
                                    enabled = selectedDobMillis != null
                                )

                                if (showDobPicker) {
                                    val dpState = rememberDatePickerState(
                                        initialSelectedDateMillis = selectedDobMillis ?: maxDobMillis,
                                        selectableDates = object : SelectableDates {
                                            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= maxDobMillis
                                            override fun isSelectableYear(year: Int): Boolean {
                                                val minYear = Calendar.getInstance().apply { add(Calendar.YEAR, -13) }.get(Calendar.YEAR)
                                                return year <= minYear
                                            }
                                        }
                                    )
                                    DatePickerDialog(
                                        onDismissRequest = { showDobPicker = false },
                                        confirmButton = {
                                            TextButton(
                                                onClick = {
                                                    selectedDobMillis = dpState.selectedDateMillis
                                                    showDobPicker = false
                                                },
                                                enabled = dpState.selectedDateMillis != null
                                            ) {
                                                Text("Done", color = colors.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showDobPicker = false }) {
                                                Text("Cancel", color = colors.textMuted, fontSize = 16.sp)
                                            }
                                        }
                                    ) {
                                        DatePicker(
                                            state = dpState,
                                            title = { Text("Select Date of Birth", modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp), fontWeight = FontWeight.Bold) }
                                        )
                                    }
                                }
                            }

                            // ════════════════════════════════════════════════
                            // SIGNUP_GENDER — Step 4: Gender Selection
                            // ════════════════════════════════════════════════
                            AuthStep.SIGNUP_GENDER -> {
                                Spacer(Modifier.height(12.dp))
                                Text("What's your gender?", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.textMain, letterSpacing = (-0.5).sp)
                                Spacer(Modifier.height(6.dp))
                                Text("This helps us personalise your default profile styling.", color = colors.textMuted, fontSize = 14.sp, textAlign = TextAlign.Center)

                                Spacer(Modifier.height(28.dp))

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    genderOptions.forEach { gender ->
                                        val isSelected = selectedGender == gender
                                        val (iconRes, iconVector) = when (gender) {
                                            "Male" -> Pair(R.drawable.ic_gender_male, null)
                                            "Female" -> Pair(R.drawable.ic_gender_female, null)
                                            "Non-binary" -> Pair(null, Lucide.Sparkles)
                                            else -> Pair(null, Lucide.EyeOff)
                                        }

                                        Surface(
                                            onClick = { selectedGender = gender },
                                            modifier = Modifier.fillMaxWidth().height(52.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) colors.primary.copy(alpha = 0.12f) else Color.Transparent,
                                            border = BorderStroke(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) colors.primary else colors.glassBorder.copy(alpha = 0.7f)
                                            )
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
                                            ) {
                                                if (iconRes != null) {
                                                    Icon(painterResource(iconRes), contentDescription = null, tint = if (isSelected) colors.primary else colors.textMuted, modifier = Modifier.size(18.dp))
                                                } else if (iconVector != null) {
                                                    Icon(iconVector, contentDescription = null, tint = if (isSelected) colors.primary else colors.textMuted, modifier = Modifier.size(18.dp))
                                                }
                                                Spacer(Modifier.width(14.dp))
                                                Text(
                                                    text = gender,
                                                    fontSize = 15.5.sp,
                                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                    color = if (isSelected) colors.primary else colors.textMain
                                                )
                                                Spacer(Modifier.weight(1f))
                                                if (isSelected) {
                                                    Icon(Lucide.Check, null, tint = colors.primary, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(32.dp))
                                CupertinoPrimaryButton(
                                    onClick = { currentStep = AuthStep.SIGNUP_AVATAR },
                                    text = "Continue",
                                    enabled = selectedGender.isNotEmpty()
                                )
                            }

                            // ════════════════════════════════════════════════
                            // SIGNUP_AVATAR — Step 5: Avatar Picker
                            // ════════════════════════════════════════════════
                            AuthStep.SIGNUP_AVATAR -> {
                                Spacer(Modifier.height(12.dp))
                                CupertinoAvatarPickerStep(
                                    title = "Choose Profile Photo",
                                    subtitle = "Upload your custom photo or keep your default avatar.",
                                    selectedUri = selectedAvatarUri,
                                    gender = selectedGender,
                                    onGallery = { galleryLauncher.launch("image/*") },
                                    onCamera = { launchCamera() }
                                )
                                Spacer(Modifier.height(36.dp))
                                CupertinoPrimaryButton(
                                    onClick = { currentStep = AuthStep.SIGNUP_INTERESTS },
                                    text = if (selectedAvatarUri == null && selectedGender.isNotBlank()) "Keep Default & Continue" else if (selectedAvatarUri == null) "Skip for Now" else "Continue"
                                )
                            }

                            // ════════════════════════════════════════════════
                            // SIGNUP_INTERESTS — Step 6: Categories (No Pills)
                            // ════════════════════════════════════════════════
                            AuthStep.SIGNUP_INTERESTS -> {
                                Spacer(Modifier.height(12.dp))
                                Text("What are your interests?", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.textMain, letterSpacing = (-0.5).sp)
                                Spacer(Modifier.height(6.dp))
                                Text("Select topics you love to discover relevant content.", color = colors.textMuted, fontSize = 14.sp, textAlign = TextAlign.Center)

                                Spacer(Modifier.height(28.dp))

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    interestOptions.forEach { interest ->
                                        val isSelected = selectedInterests.contains(interest)
                                        Surface(
                                            onClick = {
                                                val s = selectedInterests.toMutableSet()
                                                if (isSelected) s.remove(interest) else s.add(interest)
                                                selectedInterests = s
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) colors.primary.copy(alpha = 0.14f) else Color.Transparent,
                                            border = BorderStroke(
                                                width = 1.dp,
                                                color = if (isSelected) colors.primary else colors.glassBorder.copy(alpha = 0.7f)
                                            )
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                                            ) {
                                                if (isSelected) {
                                                    Icon(Lucide.Check, null, tint = colors.primary, modifier = Modifier.size(14.dp))
                                                }
                                                Text(
                                                    text = interest,
                                                    color = if (isSelected) colors.primary else colors.textMain,
                                                    fontSize = 13.5.sp,
                                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }

                                if (errorMessage != null) {
                                    Spacer(Modifier.height(12.dp))
                                    Text(errorMessage!!, color = colors.error, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                }

                                Spacer(Modifier.height(36.dp))

                                CupertinoPrimaryButton(
                                    onClick = {
                                        scope.launch {
                                            isLoading = true
                                            errorMessage = null
                                            try {
                                                val uid = googleUser?.uid ?: FirebaseAuth.getInstance().currentUser?.uid
                                                if (uid == null) {
                                                    errorMessage = "Authentication session expired. Please sign in again."
                                                    return@launch
                                                }
                                                var avatarUrl = ""
                                                if (selectedAvatarBytes != null) {
                                                    avatarUrl = uploadAvatarToSupabase(uid, selectedAvatarBytes!!) ?: ""
                                                } else if (selectedAvatarUri != null && googleUser?.photoUrl != null && selectedAvatarUri == googleUser?.photoUrl) {
                                                    avatarUrl = googleUser?.photoUrl.toString()
                                                }
                                                val success = discoverRepo.registerDetailedUser(
                                                    uid = uid,
                                                    displayName = displayNameInput.trim(),
                                                    username = usernameInput.trim().lowercase(),
                                                    dob = selectedDobMillis,
                                                    avatarUrl = avatarUrl,
                                                    interests = selectedInterests.toList(),
                                                    gender = selectedGender,
                                                    agreedToTermsAt = System.currentTimeMillis()
                                                )
                                                if (success) {
                                                    onLoginSuccess()
                                                } else {
                                                    errorMessage = "Failed to create profile. Please try again."
                                                }
                                            } catch (e: Exception) {
                                                errorMessage = "Error: ${e.message}"
                                            } finally { isLoading = false }
                                        }
                                    },
                                    isLoading = isLoading,
                                    text = "Create Profile & Launch",
                                    enabled = selectedInterests.isNotEmpty()
                                )

                                Spacer(Modifier.height(14.dp))
                                Text(
                                    text = "By creating a profile, you agree to Chroma's Terms of Service and Privacy Policy.",
                                    fontSize = 12.sp,
                                    color = colors.textMuted.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        } // end when

                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

// ─── Direct Avatar Picker (No Cards, No Pills) ───────────────────────

@Composable
fun CupertinoAvatarPickerStep(
    title: String,
    subtitle: String,
    selectedUri: Uri?,
    gender: String? = null,
    onGallery: () -> Unit,
    onCamera: () -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }
    val colors = LocalOscellionColors.current
    val defaultAvatarRes = remember(gender) { getDefaultAvatarDrawableRes(gender) }

    Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.textMain, textAlign = TextAlign.Center, letterSpacing = (-0.5).sp)
    Spacer(Modifier.height(6.dp))
    Text(subtitle, color = colors.textMuted, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
    Spacer(Modifier.height(32.dp))

    Box(
        modifier = Modifier
            .size(136.dp)
            .clip(CircleShape)
            .border(2.dp, colors.primary.copy(alpha = 0.4f), CircleShape)
            .clickable { showSheet = true },
        contentAlignment = Alignment.Center
    ) {
        if (selectedUri != null) {
            AsyncImage(selectedUri, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else if (defaultAvatarRes != null) {
            Image(
                painter = painterResource(id = defaultAvatarRes),
                contentDescription = "$gender Avatar",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Lucide.Camera, null, tint = colors.textMuted.copy(alpha = 0.6f), modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(4.dp))
                Text("Add Photo", fontSize = 12.sp, color = colors.textMuted)
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .size(34.dp)
                .clip(CircleShape)
                .background(colors.primary)
                .border(2.dp, colors.bg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Lucide.Pencil, null, tint = Color.White, modifier = Modifier.size(15.dp))
        }
    }

    if (showSheet) {
        com.chroma.studio.ui.components.GlassDialog(
            onDismissRequest = { showSheet = false }
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Profile Photo",
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain,
                    fontSize = 19.sp,
                    letterSpacing = (-0.3).sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Take a new photo or choose an existing image from your photo library.",
                    color = colors.textMuted,
                    fontSize = 13.5.sp
                )
                Spacer(Modifier.height(20.dp))

                Surface(
                    onClick = { showSheet = false; onCamera() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, colors.glassBorder.copy(alpha = 0.7f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Lucide.Camera, null, tint = colors.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Take Photo", color = colors.textMain, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(Modifier.height(10.dp))

                Surface(
                    onClick = { showSheet = false; onGallery() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, colors.glassBorder.copy(alpha = 0.7f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Lucide.Image, null, tint = colors.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Choose from Library", color = colors.textMain, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showSheet = false }) {
                        Text("Cancel", color = colors.primary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

// ─── Cupertino Password Rule Checklist Item ──────────────────────

@Composable
fun CupertinoPasswordRule(label: String, met: Boolean) {
    val colors = LocalOscellionColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (met) Lucide.CircleCheck else Lucide.Circle,
            contentDescription = null,
            tint = if (met) Color(0xFF34C759) else colors.textMuted.copy(alpha = 0.35f),
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 12.5.sp,
            color = if (met) colors.textMain else colors.textMuted.copy(alpha = 0.5f),
            fontWeight = if (met) FontWeight.Medium else FontWeight.Normal
        )
    }
}
