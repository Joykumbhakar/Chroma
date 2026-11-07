package com.chroma.studio.ui.screens

import android.Manifest
import android.app.Activity
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.PermissionChecker
import coil.compose.AsyncImage
import com.chroma.studio.R
import com.chroma.studio.data.DiscoverRepository
import com.chroma.studio.data.SavedAccount
import com.chroma.studio.data.SavedAccountManager
import com.chroma.studio.model.UserProfile
import com.chroma.studio.ui.components.IosAvatarView
import com.chroma.studio.ui.components.OneUiGlassCard
import com.chroma.studio.ui.components.OscellionAppIcon
import com.chroma.studio.ui.components.doubleEdgeGlassBorder
import com.chroma.studio.ui.components.getDefaultAvatarDrawableRes
import com.chroma.studio.ui.theme.InterFontFamily
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
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
    return com.chroma.studio.data.SupabaseMediaStorage.uploadAvatarBytes(uid, imageBytes)
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

// ─── Direct Unified UI Components (Samsung One UI Standard) ─────────────────────────

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
    val isDark = colors.isDark
    val textPrimary = if (isDark) Color(0xFFFAFAFA) else Color(0xFF000000)
    val textMuted = if (isDark) Color(0xFF8E8E93) else Color(0xFF71717A)
    val inputBg = if (isDark) Color(0x15FFFFFF) else Color(0x0A000000)

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                color = textMuted.copy(alpha = 0.65f),
                fontSize = 15.sp,
                fontFamily = InterFontFamily
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF0381FE),
            unfocusedBorderColor = if (isDark) Color(0x22FFFFFF) else Color(0x18000000),
            errorBorderColor = Color(0xFFEF4444),
            focusedTextColor = textPrimary,
            unfocusedTextColor = textPrimary,
            disabledTextColor = textMuted,
            cursorColor = Color(0xFF0381FE),
            focusedContainerColor = inputBg,
            unfocusedContainerColor = inputBg,
            errorContainerColor = Color(0xFFEF4444).copy(alpha = 0.08f)
        ),
        singleLine = singleLine,
        isError = isError,
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        textStyle = TextStyle(
            fontSize = 15.5.sp,
            color = textPrimary,
            fontWeight = FontWeight.Normal,
            fontFamily = InterFontFamily
        )
    )
}

/**
 * Samsung One UI Signature Primary Pill Button:
 * Uses Samsung One UI primary gradient #0381FE -> #0072DE.
 * Strictly BORDERLESS per standing user rule: "Do NOT use borders for pills/capsules".
 * Backward compatible with callers like PostDetailsScreen.
 */
@Composable
fun CupertinoPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    containerColor: Color = LocalOscellionColors.current.primary,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.975f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioLowBouncy),
        label = "btnScale"
    )

    val shape = CircleShape
    val buttonGradient = if (enabled && !isLoading) {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF0381FE),
                Color(0xFF0072DE)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF0381FE).copy(alpha = 0.45f),
                Color(0xFF0072DE).copy(alpha = 0.45f)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (enabled && !isLoading) 8.dp else 0.dp,
                shape = shape,
                ambientColor = Color(0x300381FE),
                spotColor = Color(0x500381FE)
            )
            .clip(shape)
            .background(buttonGradient)
            .clickable(
                enabled = enabled && !isLoading,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(19.dp),
                    color = Color.White,
                    strokeWidth = 2.2.dp
                )
            } else {
                content()
            }
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
            color = Color.White,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp,
            fontFamily = InterFontFamily
        )
    }
}

/**
 * Samsung One UI Secondary Pill Button:
 * Strictly BORDERLESS per standing user rule: "Do NOT use borders for pills/capsules".
 */
@Composable
fun CupertinoSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark
    val textPrimary = if (isDark) Color(0xFFFAFAFA) else Color(0xFF0F172A)
    val buttonBg = if (isDark) Color(0xFF17171A) else Color(0xFFF2F4F7)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.975f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioLowBouncy),
        label = "btnSecScale"
    )

    val shape = CircleShape

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(buttonBg)
            .clickable(
                enabled = enabled && !isLoading,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = textPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                content()
            }
        }
    }
}

/**
 * Clean One UI Feature Item Row inside a 26dp Squircle Focus Card.
 */
@Composable
private fun LoginFeatureRow(
    icon: ImageVector,
    iconGradient: List<Color>,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark
    val textPrimary = if (isDark) Color(0xFFFAFAFA) else Color(0xFF000000)
    val textMuted = if (isDark) Color(0xFF8E8E93) else Color(0xFF71717A)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(iconGradient)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.5.sp,
                color = textPrimary,
                letterSpacing = (-0.3).sp,
                lineHeight = 18.sp,
                fontFamily = InterFontFamily
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = subtitle,
                fontSize = 12.5.sp,
                color = textMuted,
                lineHeight = 16.sp,
                letterSpacing = (-0.2).sp,
                fontFamily = InterFontFamily
            )
        }
    }
}

/**
 * Samsung One UI Saved Account Card with 20dp Squircle & Double Edge Glass Border.
 */
@Composable
private fun LoginSavedAccountCard(
    account: SavedAccount,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark
    val cardBg = if (isDark) Color(0xFF17171A) else Color(0xFFFFFFFF)
    val textPrimary = if (isDark) Color(0xFFFAFAFA) else Color(0xFF000000)
    val textMuted = if (isDark) Color(0xFF8E8E93) else Color(0xFF71717A)

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .doubleEdgeGlassBorder(cornerRadius = 20.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IosAvatarView(
                name = account.displayName.ifBlank { account.username },
                avatarUrl = account.avatarUrl,
                size = 44.dp,
                gender = account.gender
            )

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.displayName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = (-0.3).sp,
                    fontFamily = InterFontFamily
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "@${account.username}",
                    fontSize = 12.5.sp,
                    color = if (isDark) Color(0xFF3E91FF) else Color(0xFF0072DE),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = (-0.2).sp,
                    fontFamily = InterFontFamily
                )
            }

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Lucide.X,
                    contentDescription = "Remove account",
                    tint = textMuted.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ─── Main LoginScreen Composable ─────────────────────────

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark
    val scope = rememberCoroutineScope()
    var currentStep by remember { mutableStateOf(AuthStep.INITIAL) }

    val screenBg = if (isDark) Color(0xFF080808) else Color(0xFFFAFAFA)
    val cardBg = if (isDark) Color(0xFF17171A) else Color(0xFFFFFFFF)
    val cardDivider = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000)
    val textPrimary = if (isDark) Color(0xFFFAFAFA) else Color(0xFF000000)
    val textMuted = if (isDark) Color(0xFF8E8E93) else Color(0xFF71717A)

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
    var pendingCropUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraImageUri != null) {
            pendingCropUri = cameraImageUri
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            pendingCropUri = uri
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

    if (pendingCropUri != null) {
        com.chroma.studio.ui.components.ProfileMediaEditorModal(
            imageUri = pendingCropUri!!,
            target = com.chroma.studio.ui.components.ProfileCropTarget.AVATAR,
            onDismiss = { pendingCropUri = null },
            onCropSuccess = { croppedUri ->
                pendingCropUri = null
                selectedAvatarUri = croppedUri
                scope.launch { selectedAvatarBytes = com.chroma.studio.utils.ImageUtils.compressAvatarForSupabase(context, croppedUri) }
            }
        )
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
                            com.chroma.studio.services.ChromaMessagingService.syncTokenForCurrentUser()
                            onLoginSuccess()
                        } else {
                            val baseName = user.email?.substringBefore("@")?.filter { it.isLetterOrDigit() || it == '_' }?.lowercase()?.ifBlank { "creator" } ?: "creator"
                            var targetUsername = baseName
                            if (!discoverRepo.checkUsernameAvailable(targetUsername)) {
                                val suggestions = discoverRepo.suggestAvailableUsernames(baseName)
                                targetUsername = suggestions.firstOrNull() ?: "${baseName}${System.currentTimeMillis() % 1000}"
                            }
                            val displayName = user.displayName?.ifBlank { baseName } ?: baseName
                            val avatarUrl = user.photoUrl?.toString() ?: ""

                            val registered = discoverRepo.registerDetailedUser(
                                uid = user.uid,
                                displayName = displayName,
                                username = targetUsername,
                                dob = null,
                                avatarUrl = avatarUrl,
                                interests = listOf("Creative Studio", "Design", "Photography"),
                                gender = "UNSPECIFIED",
                                agreedToTermsAt = System.currentTimeMillis()
                            )

                            if (registered) {
                                val newProfile = discoverRepo.getUserProfile(user.uid)
                                if (newProfile != null) {
                                    SavedAccountManager.saveFromProfile(context, newProfile, user.email)
                                }
                                com.chroma.studio.services.ChromaMessagingService.syncTokenForCurrentUser()
                                onLoginSuccess()
                            } else {
                                errorMessage = "Could not initialize your studio profile. Please try again."
                            }
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
                com.chroma.studio.services.ChromaMessagingService.syncTokenForCurrentUser()
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

    BackHandler { handleBack() }

    Box(
        modifier = Modifier
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
                        IconButton(onClick = onCancel, modifier = Modifier.size(38.dp)) {
                            Icon(Lucide.X, contentDescription = "Close", tint = textPrimary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.weight(1f))
                    } else {
                        IconButton(
                            onClick = { handleBack() },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.ChevronLeft,
                                contentDescription = "Back",
                                tint = textPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        if (currentSignupIndex > 0) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (i in 1..totalSignupSteps) {
                                    val active = i <= currentSignupIndex
                                    Box(
                                        modifier = Modifier
                                            .width(if (i == currentSignupIndex) 20.dp else 6.dp)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(if (active) Color(0xFF0381FE) else textMuted.copy(alpha = 0.25f))
                                            .animateContentSize()
                                    )
                                }
                            }
                        } else {
                            Spacer(Modifier.width(40.dp))
                        }

                        IconButton(onClick = onCancel, modifier = Modifier.size(38.dp)) {
                            Icon(Lucide.X, contentDescription = "Close", tint = textPrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .imePadding()
            ) {
                val screenHeight = maxHeight
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            slideInHorizontally(initialOffsetX = { it / 2 }) + fadeIn() togetherWith
                                    slideOutHorizontally(targetOffsetX = { -it / 2 }) + fadeOut()
                        },
                        label = "AuthSteps",
                        modifier = Modifier.fillMaxWidth()
                    ) { step ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = screenHeight - 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            when (step) {

                                // ════════════════════════════════════════════════
                                // INITIAL — Login Screen
                                // ════════════════════════════════════════════════
                                AuthStep.INITIAL -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Spacer(Modifier.height(8.dp))

                                        // ── 1. VIEWING AREA ──────────────────────────────────────────
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 10.dp, bottom = 20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            // 72dp Squircle App Icon in Focus Block
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
                                                text = if (savedAccounts.isNotEmpty()) "Choose an Account" else "Chroma Studio",
                                                fontSize = 32.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = textPrimary,
                                                letterSpacing = (-0.6).sp,
                                                textAlign = TextAlign.Center,
                                                fontFamily = InterFontFamily
                                            )

                                            Spacer(Modifier.height(6.dp))

                                            Text(
                                                text = if (savedAccounts.isNotEmpty())
                                                    "Select an existing profile or continue with another Google account."
                                                else
                                                    "Craft high-fidelity visual artwork, explore creations, and connect with a community of designers.",
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

                                        // ── 2. INTERACTION AREA (26dp Squircle Focus Blocks) ──────────
                                        if (savedAccounts.isNotEmpty()) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                savedAccounts.forEach { account ->
                                                    LoginSavedAccountCard(
                                                        account = account,
                                                        onClick = { loginWithGoogle() },
                                                        onRemove = {
                                                            SavedAccountManager.removeAccount(context, account.uid)
                                                            savedAccounts = SavedAccountManager.getSavedAccounts(context)
                                                        }
                                                    )
                                                }
                                            }
                                        } else {
                                            // Feature Highlights grouped inside a single 26dp Squircle Focus Card
                                            OneUiGlassCard(
                                                modifier = Modifier.fillMaxWidth(),
                                                cornerRadius = 26.dp,
                                                backgroundColor = cardBg,
                                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
                                            ) {
                                                LoginFeatureRow(
                                                    icon = Lucide.Palette,
                                                    iconGradient = listOf(Color(0xFF0381FE), Color(0xFF0072DE)),
                                                    title = "Creative Studio",
                                                    subtitle = "Multi-layer gradient studio with real-time rendering"
                                                )
                                                HorizontalDivider(
                                                    color = cardDivider,
                                                    thickness = 0.5.dp,
                                                    modifier = Modifier.padding(vertical = 10.dp)
                                                )
                                                LoginFeatureRow(
                                                    icon = Lucide.Lock,
                                                    iconGradient = listOf(Color(0xFF10B981), Color(0xFF06B6D4)),
                                                    title = "Private Messaging",
                                                    subtitle = "Encrypted direct chats, audio, and media sharing"
                                                )
                                                HorizontalDivider(
                                                    color = cardDivider,
                                                    thickness = 0.5.dp,
                                                    modifier = Modifier.padding(vertical = 10.dp)
                                                )
                                                LoginFeatureRow(
                                                    icon = Lucide.Zap,
                                                    iconGradient = listOf(Color(0xFFFF2D55), Color(0xFFA855F7)),
                                                    title = "Instant Stories",
                                                    subtitle = "Share daily status, audio waveforms, and community art"
                                                )
                                            }
                                        }

                                        if (errorMessage != null) {
                                            Spacer(Modifier.height(14.dp))
                                            Text(
                                                text = errorMessage!!,
                                                color = Color(0xFFEF4444),
                                                fontSize = 13.sp,
                                                textAlign = TextAlign.Center,
                                                lineHeight = 16.sp,
                                                letterSpacing = (-0.2).sp,
                                                fontFamily = InterFontFamily,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }

                                    // ── 3. BOTTOM ACTIONS AREA (Borderless Pills) ────────────────
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 24.dp, bottom = 20.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Continue with Google: Borderless Pill
                                        CupertinoPrimaryButton(
                                            onClick = { loginWithGoogle() },
                                            isLoading = isLoading
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_google_logo),
                                                contentDescription = "Google",
                                                modifier = Modifier.size(19.dp),
                                                tint = Color.Unspecified
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Text(
                                                text = if (savedAccounts.isNotEmpty()) "Use Another Account" else "Continue with Google",
                                                color = Color.White,
                                                fontSize = 15.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                letterSpacing = (-0.3).sp,
                                                fontFamily = InterFontFamily
                                            )
                                        }

                                        // Continue with Apple: Borderless Pill
                                        CupertinoSecondaryButton(
                                            onClick = { loginWithGoogle() }
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_apple_logo),
                                                contentDescription = "Apple",
                                                modifier = Modifier.size(19.dp),
                                                tint = if (isDark) Color.White else Color.Black
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Text(
                                                text = "Continue with Apple",
                                                color = textPrimary,
                                                fontSize = 15.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                letterSpacing = (-0.3).sp,
                                                fontFamily = InterFontFamily
                                            )
                                        }

                                        Spacer(Modifier.height(2.dp))

                                        Text(
                                            text = "By continuing, you agree to Chroma Studio's Terms of Service and Privacy Policy.",
                                            fontSize = 11.5.sp,
                                            lineHeight = 15.sp,
                                            letterSpacing = (-0.2).sp,
                                            color = textMuted.copy(alpha = 0.8f),
                                            textAlign = TextAlign.Center,
                                            fontFamily = InterFontFamily,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                }

                                // ════════════════════════════════════════════════
                                // GOOGLE_MATCH — Existing Account Match
                                // ════════════════════════════════════════════════
                                AuthStep.GOOGLE_MATCH -> {
                                    matchedProfile?.let { profile ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Spacer(Modifier.height(16.dp))

                                            Text(
                                                text = "Is this your account?",
                                                fontSize = 30.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = textPrimary,
                                                letterSpacing = (-0.6).sp,
                                                fontFamily = InterFontFamily
                                            )
                                            Spacer(Modifier.height(6.dp))
                                            Text(
                                                text = "We found an existing Chroma Studio profile linked to your Google account.",
                                                color = textMuted,
                                                fontSize = 13.5.sp,
                                                textAlign = TextAlign.Center,
                                                letterSpacing = (-0.2).sp,
                                                lineHeight = 18.sp,
                                                fontFamily = InterFontFamily
                                            )

                                            Spacer(Modifier.height(28.dp))

                                            // 26dp Squircle Focus Card with Double Edge Glass Border
                                            OneUiGlassCard(
                                                modifier = Modifier.fillMaxWidth(),
                                                cornerRadius = 26.dp,
                                                backgroundColor = cardBg,
                                                contentPadding = PaddingValues(24.dp)
                                            ) {
                                                Column(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    IosAvatarView(
                                                        name = profile.displayName,
                                                        avatarUrl = profile.avatarUrl,
                                                        size = 96.dp,
                                                        gender = profile.gender
                                                    )
                                                    Spacer(Modifier.height(14.dp))
                                                    Text(
                                                        text = profile.displayName,
                                                        color = textPrimary,
                                                        fontSize = 20.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        letterSpacing = (-0.4).sp,
                                                        fontFamily = InterFontFamily
                                                    )
                                                    Spacer(Modifier.height(2.dp))
                                                    Text(
                                                        text = "@${profile.username}",
                                                        color = if (isDark) Color(0xFF3E91FF) else Color(0xFF0072DE),
                                                        fontSize = 14.sp,
                                                        letterSpacing = (-0.2).sp,
                                                        fontFamily = InterFontFamily
                                                    )
                                                }
                                            }
                                        }

                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 24.dp, bottom = 20.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            CupertinoPrimaryButton(
                                                onClick = { completeLogin(profile) },
                                                text = "Continue as ${profile.displayName}"
                                            )
                                            Text(
                                                text = "This isn't my account",
                                                color = if (isDark) Color(0xFF3E91FF) else Color(0xFF0072DE),
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                letterSpacing = (-0.2).sp,
                                                fontFamily = InterFontFamily,
                                                modifier = Modifier
                                                    .clickable(role = Role.Button) {
                                                        FirebaseAuth.getInstance().signOut()
                                                        currentStep = AuthStep.INITIAL
                                                        matchedProfile = null
                                                    }
                                                    .padding(8.dp)
                                            )
                                        }
                                    }
                                }

                                // ════════════════════════════════════════════════
                                // LOGIN_MISSING_AVATAR
                                // ════════════════════════════════════════════════
                                AuthStep.LOGIN_MISSING_AVATAR -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Spacer(Modifier.height(16.dp))
                                        CupertinoAvatarPickerStep(
                                            title = "Set Profile Photo",
                                            subtitle = "Add a photo to make your profile recognizable to friends.",
                                            selectedUri = selectedAvatarUri,
                                            gender = selectedGender,
                                            onGallery = { galleryLauncher.launch("image/*") },
                                            onCamera = { launchCamera() }
                                        )
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 24.dp, bottom = 20.dp)
                                    ) {
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
                                                        com.chroma.studio.services.ChromaMessagingService.syncTokenForCurrentUser()
                                                        onLoginSuccess()
                                                    } finally { isLoading = false }
                                                }
                                            },
                                            isLoading = isLoading,
                                            text = if (selectedAvatarUri == null) "Skip for Now" else "Save & Continue"
                                        )
                                    }
                                }

                                // ════════════════════════════════════════════════
                                // SIGNUP_USERNAME — Step 1: Choose Username
                                // ════════════════════════════════════════════════
                                AuthStep.SIGNUP_USERNAME -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Spacer(Modifier.height(16.dp))
                                        Text(
                                            text = "Choose Username",
                                            fontSize = 30.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textPrimary,
                                            letterSpacing = (-0.6).sp,
                                            fontFamily = InterFontFamily
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = "Pick a unique handle for your profile and messages.",
                                            color = textMuted,
                                            fontSize = 13.5.sp,
                                            textAlign = TextAlign.Center,
                                            letterSpacing = (-0.2).sp,
                                            lineHeight = 18.sp,
                                            fontFamily = InterFontFamily
                                        )

                                        Spacer(Modifier.height(24.dp))

                                        // Focus Block Card
                                        OneUiGlassCard(
                                            modifier = Modifier.fillMaxWidth(),
                                            cornerRadius = 26.dp,
                                            backgroundColor = cardBg,
                                            contentPadding = PaddingValues(20.dp)
                                        ) {
                                            CupertinoTextField(
                                                value = usernameInput,
                                                onValueChange = {
                                                    val filtered = it.filter { c -> c.isLetterOrDigit() || c == '_' }.lowercase()
                                                    if (filtered.length <= 30) usernameInput = filtered
                                                },
                                                placeholder = "Username",
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Lucide.AtSign,
                                                        contentDescription = null,
                                                        tint = if (usernameInput.isNotEmpty()) Color(0xFF0381FE) else textMuted,
                                                        modifier = Modifier.size(19.dp)
                                                    )
                                                },
                                                trailingIcon = {
                                                    when {
                                                        isCheckingUsername -> CircularProgressIndicator(
                                                            modifier = Modifier.size(16.dp),
                                                            color = Color(0xFF0381FE),
                                                            strokeWidth = 2.dp
                                                        )
                                                        isUsernameAvailable == true -> Icon(
                                                            imageVector = Lucide.Check,
                                                            contentDescription = "Available",
                                                            tint = Color(0xFF10B981),
                                                            modifier = Modifier.size(19.dp)
                                                        )
                                                        isUsernameAvailable == false -> Icon(
                                                            imageVector = Lucide.X,
                                                            contentDescription = "Taken",
                                                            tint = Color(0xFFEF4444),
                                                            modifier = Modifier.size(19.dp)
                                                        )
                                                    }
                                                }
                                            )

                                            if (isUsernameAvailable == false && usernameInput.length >= 3) {
                                                Spacer(Modifier.height(8.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "Handle already taken",
                                                        color = Color(0xFFEF4444),
                                                        fontSize = 12.sp,
                                                        letterSpacing = (-0.2).sp,
                                                        fontFamily = InterFontFamily
                                                    )
                                                }

                                                if (usernameSuggestions.isNotEmpty()) {
                                                    Spacer(Modifier.height(10.dp))
                                                    Text(
                                                        text = "Suggested handles:",
                                                        color = textMuted,
                                                        fontSize = 12.sp,
                                                        letterSpacing = (-0.2).sp,
                                                        fontFamily = InterFontFamily
                                                    )
                                                    Spacer(Modifier.height(6.dp))
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .horizontalScroll(rememberScrollState()),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        usernameSuggestions.forEach { suggestion ->
                                                            Surface(
                                                                onClick = { usernameInput = suggestion },
                                                                shape = RoundedCornerShape(12.dp),
                                                                color = Color(0xFF0381FE).copy(alpha = if (isDark) 0.20f else 0.12f)
                                                            ) {
                                                                Text(
                                                                    text = "@$suggestion",
                                                                    color = if (isDark) Color(0xFF3E91FF) else Color(0xFF0072DE),
                                                                    fontSize = 12.5.sp,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    letterSpacing = (-0.2).sp,
                                                                    fontFamily = InterFontFamily,
                                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        if (errorMessage != null) {
                                            Spacer(Modifier.height(12.dp))
                                            Text(
                                                text = errorMessage!!,
                                                color = Color(0xFFEF4444),
                                                fontSize = 13.sp,
                                                textAlign = TextAlign.Center,
                                                fontFamily = InterFontFamily,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 24.dp, bottom = 20.dp)
                                    ) {
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
                                }

                                // ════════════════════════════════════════════════
                                // SIGNUP_NAME — Step 2: Display Name
                                // ════════════════════════════════════════════════
                                AuthStep.SIGNUP_NAME -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Spacer(Modifier.height(16.dp))
                                        Text(
                                            text = "What's your name?",
                                            fontSize = 30.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textPrimary,
                                            letterSpacing = (-0.6).sp,
                                            fontFamily = InterFontFamily
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = "Your name will be visible to everyone on your profile.",
                                            color = textMuted,
                                            fontSize = 13.5.sp,
                                            textAlign = TextAlign.Center,
                                            letterSpacing = (-0.2).sp,
                                            lineHeight = 18.sp,
                                            fontFamily = InterFontFamily
                                        )

                                        Spacer(Modifier.height(24.dp))

                                        OneUiGlassCard(
                                            modifier = Modifier.fillMaxWidth(),
                                            cornerRadius = 26.dp,
                                            backgroundColor = cardBg,
                                            contentPadding = PaddingValues(20.dp)
                                        ) {
                                            CupertinoTextField(
                                                value = displayNameInput,
                                                onValueChange = { if (it.length <= 50) displayNameInput = it },
                                                placeholder = "Full Name (e.g. Alex Rivera)",
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Lucide.User,
                                                        contentDescription = null,
                                                        tint = if (displayNameInput.isNotEmpty()) Color(0xFF0381FE) else textMuted,
                                                        modifier = Modifier.size(19.dp)
                                                    )
                                                }
                                            )
                                        }
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 24.dp, bottom = 20.dp)
                                    ) {
                                        CupertinoPrimaryButton(
                                            onClick = { currentStep = AuthStep.SIGNUP_DOB },
                                            text = "Continue",
                                            enabled = displayNameInput.isNotBlank()
                                        )
                                    }
                                }

                                // ════════════════════════════════════════════════
                                // SIGNUP_DOB — Step 3: Date of Birth
                                // ════════════════════════════════════════════════
                                AuthStep.SIGNUP_DOB -> {
                                    val dobFormatter = remember { SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()) }
                                    val dobDisplay = selectedDobMillis?.let { dobFormatter.format(Date(it)) }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Spacer(Modifier.height(16.dp))
                                        Text(
                                            text = "When's your birthday?",
                                            fontSize = 30.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textPrimary,
                                            letterSpacing = (-0.6).sp,
                                            fontFamily = InterFontFamily
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = "This helps us verify your age and secure your account.",
                                            color = textMuted,
                                            fontSize = 13.5.sp,
                                            textAlign = TextAlign.Center,
                                            letterSpacing = (-0.2).sp,
                                            lineHeight = 18.sp,
                                            fontFamily = InterFontFamily
                                        )

                                        Spacer(Modifier.height(24.dp))

                                        OneUiGlassCard(
                                            modifier = Modifier.fillMaxWidth(),
                                            cornerRadius = 26.dp,
                                            backgroundColor = cardBg,
                                            contentPadding = PaddingValues(20.dp)
                                        ) {
                                            Surface(
                                                onClick = { showDobPicker = true },
                                                modifier = Modifier.fillMaxWidth().height(54.dp),
                                                shape = RoundedCornerShape(16.dp),
                                                color = if (isDark) Color(0x15FFFFFF) else Color(0x0A000000)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Lucide.Calendar, null, tint = Color(0xFF0381FE), modifier = Modifier.size(20.dp))
                                                        Spacer(Modifier.width(12.dp))
                                                        Text(
                                                            text = dobDisplay ?: "Select Date of Birth",
                                                            fontSize = 15.sp,
                                                            color = if (dobDisplay != null) textPrimary else textMuted.copy(alpha = 0.65f),
                                                            fontWeight = if (dobDisplay != null) FontWeight.SemiBold else FontWeight.Normal,
                                                            letterSpacing = (-0.2).sp,
                                                            fontFamily = InterFontFamily
                                                        )
                                                    }
                                                    Icon(Lucide.ChevronRight, null, tint = textMuted.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                                                }
                                            }

                                            Spacer(Modifier.height(14.dp))

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                                            ) {
                                                Icon(Lucide.Info, null, tint = Color(0xFF0381FE), modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = "You must be at least 13 years old to use Chroma Studio.",
                                                    fontSize = 12.5.sp,
                                                    color = textMuted,
                                                    fontWeight = FontWeight.Medium,
                                                    lineHeight = 16.sp,
                                                    letterSpacing = (-0.2).sp,
                                                    fontFamily = InterFontFamily
                                                )
                                            }
                                        }
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 24.dp, bottom = 20.dp)
                                    ) {
                                        CupertinoPrimaryButton(
                                            onClick = { currentStep = AuthStep.SIGNUP_GENDER },
                                            text = "Continue",
                                            enabled = selectedDobMillis != null
                                        )
                                    }

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
                                                    Text(
                                                        text = "Done",
                                                        color = Color(0xFF0381FE),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 16.sp,
                                                        fontFamily = InterFontFamily
                                                    )
                                                }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = { showDobPicker = false }) {
                                                    Text("Cancel", color = textMuted, fontSize = 16.sp, fontFamily = InterFontFamily)
                                                }
                                            }
                                        ) {
                                            DatePicker(
                                                state = dpState,
                                                title = {
                                                    Text(
                                                        text = "Select Date of Birth",
                                                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontFamily = InterFontFamily
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }

                                // ════════════════════════════════════════════════
                                // SIGNUP_GENDER — Step 4: Gender Selection
                                // ════════════════════════════════════════════════
                                AuthStep.SIGNUP_GENDER -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Spacer(Modifier.height(16.dp))
                                        Text(
                                            text = "What's your gender?",
                                            fontSize = 30.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textPrimary,
                                            letterSpacing = (-0.6).sp,
                                            fontFamily = InterFontFamily
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = "This helps us personalise your default profile styling.",
                                            color = textMuted,
                                            fontSize = 13.5.sp,
                                            textAlign = TextAlign.Center,
                                            letterSpacing = (-0.2).sp,
                                            lineHeight = 18.sp,
                                            fontFamily = InterFontFamily
                                        )

                                        Spacer(Modifier.height(24.dp))

                                        OneUiGlassCard(
                                            modifier = Modifier.fillMaxWidth(),
                                            cornerRadius = 26.dp,
                                            backgroundColor = cardBg,
                                            contentPadding = PaddingValues(16.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                genderOptions.forEach { gender ->
                                                    val isSelected = selectedGender == gender
                                                    val (iconRes, iconVector) = when (gender) {
                                                        "Male" -> Pair(R.drawable.ic_gender_male, null)
                                                        "Female" -> Pair(R.drawable.ic_gender_female, null)
                                                        "Non-binary" -> Pair(null, Lucide.Heart)
                                                        else -> Pair(null, Lucide.EyeOff)
                                                    }

                                                    Surface(
                                                        onClick = { selectedGender = gender },
                                                        modifier = Modifier.fillMaxWidth().height(52.dp),
                                                        shape = RoundedCornerShape(14.dp),
                                                        color = if (isSelected) {
                                                            Color(0xFF0381FE).copy(alpha = if (isDark) 0.22f else 0.12f)
                                                        } else {
                                                            if (isDark) Color(0x10FFFFFF) else Color(0x06000000)
                                                        }
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
                                                        ) {
                                                            if (iconRes != null) {
                                                                Icon(
                                                                    painter = painterResource(iconRes),
                                                                    contentDescription = null,
                                                                    tint = if (isSelected) Color(0xFF0381FE) else textMuted,
                                                                    modifier = Modifier.size(19.dp)
                                                                )
                                                            } else if (iconVector != null) {
                                                                Icon(
                                                                    imageVector = iconVector,
                                                                    contentDescription = null,
                                                                    tint = if (isSelected) Color(0xFF0381FE) else textMuted,
                                                                    modifier = Modifier.size(19.dp)
                                                                )
                                                            }
                                                            Spacer(Modifier.width(14.dp))
                                                            Text(
                                                                text = gender,
                                                                fontSize = 15.sp,
                                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                                letterSpacing = (-0.2).sp,
                                                                color = if (isSelected) (if (isDark) Color(0xFF3E91FF) else Color(0xFF0072DE)) else textPrimary,
                                                                fontFamily = InterFontFamily
                                                            )
                                                            Spacer(Modifier.weight(1f))
                                                            if (isSelected) {
                                                                Icon(
                                                                    imageVector = Lucide.Check,
                                                                    contentDescription = null,
                                                                    tint = Color(0xFF0381FE),
                                                                    modifier = Modifier.size(19.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 24.dp, bottom = 20.dp)
                                    ) {
                                        CupertinoPrimaryButton(
                                            onClick = { currentStep = AuthStep.SIGNUP_AVATAR },
                                            text = "Continue",
                                            enabled = selectedGender.isNotEmpty()
                                        )
                                    }
                                }

                                // ════════════════════════════════════════════════
                                // SIGNUP_AVATAR — Step 5: Avatar Picker
                                // ════════════════════════════════════════════════
                                AuthStep.SIGNUP_AVATAR -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Spacer(Modifier.height(16.dp))
                                        CupertinoAvatarPickerStep(
                                            title = "Choose Profile Photo",
                                            subtitle = "Upload your custom photo or keep your default avatar.",
                                            selectedUri = selectedAvatarUri,
                                            gender = selectedGender,
                                            onGallery = { galleryLauncher.launch("image/*") },
                                            onCamera = { launchCamera() }
                                        )
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 24.dp, bottom = 20.dp)
                                    ) {
                                        CupertinoPrimaryButton(
                                            onClick = { currentStep = AuthStep.SIGNUP_INTERESTS },
                                            text = if (selectedAvatarUri == null && selectedGender.isNotBlank()) "Keep Default & Continue" else if (selectedAvatarUri == null) "Skip for Now" else "Continue"
                                        )
                                    }
                                }

                                // ════════════════════════════════════════════════
                                // SIGNUP_INTERESTS — Step 6: Categories
                                // ════════════════════════════════════════════════
                                AuthStep.SIGNUP_INTERESTS -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Spacer(Modifier.height(16.dp))
                                        Text(
                                            text = "What are your interests?",
                                            fontSize = 30.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textPrimary,
                                            letterSpacing = (-0.6).sp,
                                            fontFamily = InterFontFamily
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = "Select topics you love to discover relevant content.",
                                            color = textMuted,
                                            fontSize = 13.5.sp,
                                            textAlign = TextAlign.Center,
                                            letterSpacing = (-0.2).sp,
                                            lineHeight = 18.sp,
                                            fontFamily = InterFontFamily
                                        )

                                        Spacer(Modifier.height(24.dp))

                                        OneUiGlassCard(
                                            modifier = Modifier.fillMaxWidth(),
                                            cornerRadius = 26.dp,
                                            backgroundColor = cardBg,
                                            contentPadding = PaddingValues(18.dp)
                                        ) {
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
                                                        shape = RoundedCornerShape(12.dp),
                                                        color = if (isSelected) {
                                                            Color(0xFF0381FE).copy(alpha = if (isDark) 0.22f else 0.12f)
                                                        } else {
                                                            if (isDark) Color(0x10FFFFFF) else Color(0x06000000)
                                                        }
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                                                        ) {
                                                            if (isSelected) {
                                                                Icon(
                                                                    imageVector = Lucide.Check,
                                                                    contentDescription = null,
                                                                    tint = Color(0xFF0381FE),
                                                                    modifier = Modifier.size(15.dp)
                                                                )
                                                            }
                                                            Text(
                                                                text = interest,
                                                                color = if (isSelected) (if (isDark) Color(0xFF3E91FF) else Color(0xFF0072DE)) else textPrimary,
                                                                fontSize = 13.5.sp,
                                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                                                letterSpacing = (-0.2).sp,
                                                                fontFamily = InterFontFamily
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        if (errorMessage != null) {
                                            Spacer(Modifier.height(12.dp))
                                            Text(
                                                text = errorMessage!!,
                                                color = Color(0xFFEF4444),
                                                fontSize = 13.sp,
                                                textAlign = TextAlign.Center,
                                                fontFamily = InterFontFamily,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 24.dp, bottom = 20.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
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
                                                            com.chroma.studio.services.ChromaMessagingService.syncTokenForCurrentUser()
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

                                        Text(
                                            text = "By creating a profile, you agree to Chroma Studio's Terms of Service and Privacy Policy.",
                                            fontSize = 11.5.sp,
                                            lineHeight = 15.sp,
                                            letterSpacing = (-0.2).sp,
                                            color = textMuted.copy(alpha = 0.8f),
                                            textAlign = TextAlign.Center,
                                            fontFamily = InterFontFamily,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Direct Avatar Picker Step (Samsung One UI Standard) ───────────────────────

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
    val isDark = colors.isDark
    val textPrimary = if (isDark) Color(0xFFFAFAFA) else Color(0xFF000000)
    val textMuted = if (isDark) Color(0xFF8E8E93) else Color(0xFF71717A)
    val cardBg = if (isDark) Color(0xFF17171A) else Color(0xFFFFFFFF)

    Text(
        text = title,
        fontSize = 30.sp,
        fontWeight = FontWeight.SemiBold,
        color = textPrimary,
        textAlign = TextAlign.Center,
        letterSpacing = (-0.6).sp,
        fontFamily = InterFontFamily
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = subtitle,
        color = textMuted,
        fontSize = 13.5.sp,
        textAlign = TextAlign.Center,
        letterSpacing = (-0.2).sp,
        lineHeight = 18.sp,
        fontFamily = InterFontFamily,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
    Spacer(Modifier.height(32.dp))

    // Focus Block with 26dp Squircle & Double Edge Glass Border containing Avatar Picker
    OneUiGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 26.dp,
        backgroundColor = cardBg,
        contentPadding = PaddingValues(28.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(142.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(136.dp)
                        .clip(CircleShape)
                        .clickable { showSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedUri != null) {
                        AsyncImage(selectedUri, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        IosAvatarView(
                            name = title,
                            avatarUrl = "",
                            size = 136.dp,
                            gender = gender,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Pencil badge in One UI primary blue
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(38.dp)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color(0xFF0381FE))
                        .clickable { showSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.Pencil,
                        contentDescription = "Edit photo",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Action triggers inside the Focus Block
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = { onCamera() },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color(0x15FFFFFF) else Color(0x0A000000)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Lucide.Camera, null, tint = Color(0xFF0381FE), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Camera",
                            color = textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = InterFontFamily
                        )
                    }
                }

                Surface(
                    onClick = { onGallery() },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color(0x15FFFFFF) else Color(0x0A000000)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Lucide.Image, null, tint = Color(0xFF0381FE), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Gallery",
                            color = textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = InterFontFamily
                        )
                    }
                }
            }
        }
    }
}
