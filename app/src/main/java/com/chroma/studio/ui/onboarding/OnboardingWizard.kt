package com.chroma.studio.ui.onboarding

import androidx.compose.ui.draw.drawWithCache

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chroma.studio.data.DiscoverRepository
import com.chroma.studio.ui.components.OneUiGlassCard
import com.chroma.studio.ui.components.doubleEdgeGlassBorder
import com.chroma.studio.ui.theme.InterFontFamily
import com.composables.icons.lucide.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class WizardStep {
    IDENTITY,
    PERSONAL_DETAILS,
    INTERESTS,
    COMPLETING
}

data class GoogleUserData(
    val email: String,
    val googleId: String,
    val displayName: String,
    val photoUrl: String
)

data class WizardOutputData(
    val email: String,
    val googleId: String,
    val displayName: String,
    val username: String,
    val avatarUrl: String,
    val avatarUri: Uri?,
    val dobMillis: Long?,
    val bio: String,
    val interests: List<String>,
    val password: String = "",
    val customEmail: String = ""
)

/**
 * Multi-Step Onboarding Flow (OnboardingWizard)
 *
 * Heading on the screen: "New to Oscillom, let's create a new account"
 * Step-by-step interactive form:
 * - Step 1 (Identity): Choose unique username (@handle), display name, avatar
 * - Step 2 (Personal Details): Date of birth (age verification) and short bio
 * - Step 3 (Preferences/Interests): Topic tags or discovery categories
 * - Step 4 (Completion): Atomic database commit, session establishment, route to Home
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingWizard(
    googleData: GoogleUserData,
    discoverRepo: DiscoverRepository,
    onBackToWelcome: () -> Unit,
    onComplete: (WizardOutputData) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentStep by remember { mutableStateOf(WizardStep.IDENTITY) }

    // Form states pre-filled with Google data
    var displayName by remember { mutableStateOf(googleData.displayName) }
    var username by remember {
        val base = googleData.email.substringBefore("@").filter { it.isLetterOrDigit() || it == '_' }.lowercase()
        mutableStateOf(base.ifBlank { "creator" })
    }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var avatarUrl by remember { mutableStateOf(googleData.photoUrl) }

    // Password states
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

    // Username status
    var usernameStatus by remember { mutableStateOf(UsernameStatus.Checking) }
    var usernameSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var usernameCheckJob by remember { mutableStateOf<Job?>(null) }

    // Step 2 states
    var dobMillis by remember { mutableStateOf<Long?>(null) }
    var bio by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    // Step 3 states
    val selectedInterests = remember { mutableStateListOf<String>("Creative Studio", "Design") }

    val allCategories = remember {
        listOf(
            "Creative Studio", "Color & Light", "Generative Art",
            "Photography", "Motion & 3D", "Typography",
            "Minimalism", "Design Systems", "Code & Dev",
            "Audio & Music", "Architecture", "Illustration"
        )
    }

    // Avatar picker launcher
    val avatarGalleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            avatarUri = uri
        }
    }

    // Live username checker debounced
    fun verifyUsername(candidate: String) {
        val clean = candidate.filter { it.code < 128 && (it.isLetterOrDigit() || it == '_') }.lowercase().take(30)
        username = clean

        usernameCheckJob?.cancel()
        if (clean.length < 3) {
            usernameStatus = UsernameStatus.TooShort
            usernameSuggestions = emptyList()
            return
        }

        usernameStatus = UsernameStatus.Checking
        usernameCheckJob = scope.launch {
            delay(350)
            val available = discoverRepo.checkUsernameAvailable(clean)
            if (available) {
                usernameStatus = UsernameStatus.Free
                usernameSuggestions = emptyList()
            } else {
                usernameStatus = UsernameStatus.Taken
                usernameSuggestions = discoverRepo.suggestAvailableUsernames(clean).take(3)
            }
        }
    }

    // Initial check on mount
    LaunchedEffect(Unit) {
        verifyUsername(username)
    }

    BackHandler(enabled = true) {
        when (currentStep) {
            WizardStep.IDENTITY -> onBackToWelcome()
            WizardStep.PERSONAL_DETAILS -> currentStep = WizardStep.IDENTITY
            WizardStep.INTERESTS -> currentStep = WizardStep.PERSONAL_DETAILS
            WizardStep.COMPLETING -> {}
        }
    }

    val stepIndex = when (currentStep) {
        WizardStep.IDENTITY -> 1
        WizardStep.PERSONAL_DETAILS -> 2
        WizardStep.INTERESTS -> 3
        WizardStep.COMPLETING -> 4
    }

    val colors = com.chroma.studio.ui.theme.LocalOscellionColors.current
    val isDark = colors.isDark
    val screenBg = if (isDark) Color(0xFF080808) else Color(0xFFFAFAFA)
    val cardBg = if (isDark) Color(0xFF17171A) else Color(0xFFFFFFFF)
    val fieldBg = if (isDark) Color(0xFF222226) else Color(0xFFF2F4F7)
    val fieldBorder = if (isDark) Color(0x1FFFFFFF) else Color(0x14000000)
    val textPrimary = if (isDark) Color(0xFFFAFAFA) else Color(0xFF000000)
    val textMuted = if (isDark) Color(0xFF8E8E93) else Color(0xFF71717A)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            // ── UNIFIED HEADING BANNER: "New to Oscillom, let's create a new account" ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x1FFFFFFF) else Color(0x14000000))
                            .clickable(role = Role.Button, onClick = {
                                when (currentStep) {
                                    WizardStep.IDENTITY -> onBackToWelcome()
                                    WizardStep.PERSONAL_DETAILS -> currentStep = WizardStep.IDENTITY
                                    WizardStep.INTERESTS -> currentStep = WizardStep.PERSONAL_DETAILS
                                    WizardStep.COMPLETING -> {}
                                }
                            }),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.ArrowLeft,
                            contentDescription = "Back",
                            tint = textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Progress Rail
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(3) { idx ->
                            val active = idx + 1 <= stepIndex
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(if (active) Color(0xFF0381FE) else if (isDark) Color(0x22FFFFFF) else Color(0x14000000))
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (isDark) Color(0x1FFFFFFF) else Color(0x0A000000))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Step $stepIndex/3",
                            color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0381FE),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Mandatory Prompt Banner (no border on pill/capsule)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isDark) Color(0x220066FF) else Color(0x100381FE))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.Sparkles,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF0381FE),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "New to Chroma Studio, let's create a new account",
                            color = if (isDark) Color(0xFF93C5FD) else Color(0xFF0381FE),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.1).sp
                        )
                    }
                }
            }

            // ── STEP CONTENT ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    label = "wizardTransition",
                    transitionSpec = {
                        fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                    }
                ) { step ->
                    when (step) {
                        WizardStep.IDENTITY -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Choose your identity",
                                    color = textPrimary,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )

                                // Avatar Preview with Edit Icon
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .background(fieldBg)
                                        .border(BorderStroke(1.5.dp, Color(0xFF0381FE)), CircleShape)
                                        .clickable { avatarGalleryLauncher.launch("image/*") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (avatarUri != null) {
                                        AsyncImage(
                                            model = avatarUri,
                                            contentDescription = "Avatar",
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else if (avatarUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = avatarUrl,
                                            contentDescription = "Avatar",
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Lucide.User,
                                            contentDescription = null,
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(46.dp)
                                        )
                                    }

                                    // Camera pill icon
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF0381FE))
                                            .border(BorderStroke(1.5.dp, screenBg), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Lucide.Camera,
                                            contentDescription = "Change photo",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "Tap to change avatar (Pre-filled from Google)",
                                    color = textMuted,
                                    fontSize = 12.sp
                                )

                                // Display Name
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "DISPLAY NAME",
                                        color = textMuted,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(fieldBg)
                                            .border(0.5.dp, fieldBorder, RoundedCornerShape(14.dp))
                                            .padding(horizontal = 16.dp, vertical = 14.dp)
                                    ) {
                                        BasicTextField(
                                            value = displayName,
                                            onValueChange = { displayName = it },
                                            textStyle = TextStyle(color = textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                                            cursorBrush = SolidColor(Color(0xFF0381FE)),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                // Unique Handle (@username)
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "UNIQUE HANDLE",
                                            color = textMuted,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                        when (usernameStatus) {
                                            UsernameStatus.Checking -> {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(12.dp),
                                                    strokeWidth = 1.8.dp,
                                                    color = Color(0xFF0381FE)
                                                )
                                            }
                                            UsernameStatus.Free -> {
                                                Text(
                                                    text = "Available",
                                                    color = Color(0xFF00C853),
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            UsernameStatus.Taken -> {
                                                Text(
                                                    text = "Taken",
                                                    color = Color(0xFFFF453A),
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            UsernameStatus.TooShort -> {
                                                Text(
                                                    text = "Min 3 chars",
                                                    color = textMuted,
                                                    fontSize = 11.5.sp
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(fieldBg)
                                            .border(
                                                BorderStroke(
                                                    if (usernameStatus == UsernameStatus.Free || usernameStatus == UsernameStatus.Taken) 1.dp else 0.5.dp,
                                                    when (usernameStatus) {
                                                        UsernameStatus.Free -> Color(0xFF00C853)
                                                        UsernameStatus.Taken -> Color(0xFFFF453A)
                                                        else -> fieldBorder
                                                    }
                                                ),
                                                RoundedCornerShape(14.dp)
                                            )
                                            .padding(horizontal = 16.dp, vertical = 14.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "@",
                                                color = Color(0xFF0381FE),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            BasicTextField(
                                                value = username,
                                                onValueChange = { verifyUsername(it) },
                                                textStyle = TextStyle(color = textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                                                cursorBrush = SolidColor(Color(0xFF0381FE)),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }

                                    // Username suggestions if taken
                                    if (usernameSuggestions.isNotEmpty()) {
                                        Spacer(Modifier.height(8.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Suggestions:",
                                                color = textMuted,
                                                fontSize = 11.5.sp
                                            )
                                            usernameSuggestions.forEach { sug ->
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(999.dp))
                                                        .background(if (isDark) Color(0x1FFFFFFF) else Color(0x0A000000))
                                                        .clickable { verifyUsername(sug) }
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = "@$sug",
                                                        color = Color(0xFF0381FE),
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // ── CUSTOM EMAIL BANNER (@oscellion.com) ──
                                val customEmailPreview = if (username.length >= 3) "${username.lowercase()}@oscellion.com" else "username@oscellion.com"
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isDark) Color(0x120381FE) else Color(0x080381FE))
                                        .border(0.5.dp, Color(0xFF0381FE).copy(alpha = 0.22f), RoundedCornerShape(14.dp))
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Lucide.Mail,
                                            contentDescription = null,
                                            tint = Color(0xFF0381FE),
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "LINKED CUSTOM EMAIL",
                                                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0381FE),
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                            Text(
                                                text = customEmailPreview,
                                                color = textPrimary,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                // ── PASSWORD FIELD ──
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "SET PASSWORD",
                                        color = textMuted,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(fieldBg)
                                            .border(0.5.dp, fieldBorder, RoundedCornerShape(14.dp))
                                            .padding(horizontal = 16.dp, vertical = 14.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Lucide.Lock,
                                                contentDescription = null,
                                                tint = if (password.isNotEmpty()) Color(0xFF0381FE) else textMuted,
                                                modifier = Modifier.size(17.dp)
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            BasicTextField(
                                                value = password,
                                                onValueChange = { password = it },
                                                textStyle = TextStyle(color = textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                                                cursorBrush = SolidColor(Color(0xFF0381FE)),
                                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (password.isNotEmpty()) {
                                                IconButton(
                                                    onClick = { showPassword = !showPassword },
                                                    modifier = Modifier.size(22.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (showPassword) Lucide.EyeOff else Lucide.Eye,
                                                        contentDescription = if (showPassword) "Hide password" else "Show password",
                                                        tint = textMuted,
                                                        modifier = Modifier.size(17.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    if (password.isNotEmpty() && password.length < 6) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "Minimum 6 characters required",
                                            color = Color(0xFFFF453A),
                                            fontSize = 11.5.sp
                                        )
                                    }
                                }

                                // ── CONFIRM PASSWORD FIELD ──
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "CONFIRM PASSWORD",
                                            color = textMuted,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                        if (confirmPassword.isNotEmpty()) {
                                            if (confirmPassword == password && password.length >= 6) {
                                                Text(
                                                    text = "Passwords match",
                                                    color = Color(0xFF00C853),
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            } else if (confirmPassword != password) {
                                                Text(
                                                    text = "Passwords do not match",
                                                    color = Color(0xFFFF453A),
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(fieldBg)
                                            .border(
                                                BorderStroke(
                                                    0.5.dp,
                                                    if (confirmPassword.isEmpty()) fieldBorder
                                                    else if (confirmPassword == password && password.length >= 6) Color(0xFF00C853)
                                                    else Color(0xFFFF453A)
                                                ),
                                                RoundedCornerShape(14.dp)
                                            )
                                            .padding(horizontal = 16.dp, vertical = 14.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Lucide.Lock,
                                                contentDescription = null,
                                                tint = if (confirmPassword.isNotEmpty()) Color(0xFF0381FE) else textMuted,
                                                modifier = Modifier.size(17.dp)
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            BasicTextField(
                                                value = confirmPassword,
                                                onValueChange = { confirmPassword = it },
                                                textStyle = TextStyle(color = textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                                                cursorBrush = SolidColor(Color(0xFF0381FE)),
                                                visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (confirmPassword.isNotEmpty()) {
                                                IconButton(
                                                    onClick = { showConfirmPassword = !showConfirmPassword },
                                                    modifier = Modifier.size(22.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (showConfirmPassword) Lucide.EyeOff else Lucide.Eye,
                                                        contentDescription = if (showConfirmPassword) "Hide password" else "Show password",
                                                        tint = textMuted,
                                                        modifier = Modifier.size(17.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        WizardStep.PERSONAL_DETAILS -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Personal Details",
                                    color = textPrimary,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = "Add your birthday for age verification (13+) and tell creators about your creative journey.",
                                    color = textMuted,
                                    fontSize = 13.5.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 19.sp
                                )

                                Spacer(Modifier.height(4.dp))

                                // Date of Birth Picker Button
                                val dobFormatted = remember(dobMillis) {
                                    dobMillis?.let {
                                        SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(it))
                                    } ?: "Select your date of birth"
                                }

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "DATE OF BIRTH",
                                        color = textMuted,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(fieldBg)
                                            .border(0.5.dp, fieldBorder, RoundedCornerShape(14.dp))
                                            .clickable { showDatePicker = true }
                                            .padding(horizontal = 16.dp, vertical = 14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = dobFormatted,
                                                color = if (dobMillis != null) textPrimary else textMuted,
                                                fontSize = 15.sp
                                            )
                                            Icon(
                                                imageVector = Lucide.Calendar,
                                                contentDescription = null,
                                                tint = Color(0xFF0381FE),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                // Short Bio
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "SHORT BIO",
                                            color = textMuted,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            text = "${bio.length}/150",
                                            color = textMuted,
                                            fontSize = 11.5.sp
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(100.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(fieldBg)
                                            .border(0.5.dp, fieldBorder, RoundedCornerShape(14.dp))
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        if (bio.isEmpty()) {
                                            Text(
                                                text = "Share what inspires you, what you build, or how you create…",
                                                color = textMuted.copy(alpha = 0.65f),
                                                fontSize = 14.sp
                                            )
                                        }
                                        BasicTextField(
                                            value = bio,
                                            onValueChange = { if (it.length <= 150) bio = it },
                                            textStyle = TextStyle(color = textPrimary, fontSize = 14.sp, lineHeight = 20.sp),
                                            cursorBrush = SolidColor(Color(0xFF0381FE)),
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }

                        WizardStep.INTERESTS -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Creative Interests",
                                    color = textPrimary,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(Modifier.height(6.dp))

                                Text(
                                    text = "Select 2 or more topics to curate your Chroma Studio community feed.",
                                    color = textMuted,
                                    fontSize = 13.5.sp,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(Modifier.height(20.dp))

                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f).fillMaxWidth()
                                ) {
                                    items(allCategories) { tag ->
                                        val isSelected = selectedInterests.contains(tag)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(if (isSelected) Color(0xFF0381FE) else fieldBg)
                                                .border(0.5.dp, if (isSelected) Color.Transparent else fieldBorder, RoundedCornerShape(18.dp))
                                                .clickable {
                                                    if (isSelected) {
                                                        if (selectedInterests.size > 1) selectedInterests.remove(tag)
                                                    } else {
                                                        selectedInterests.add(tag)
                                                    }
                                                }
                                                .padding(horizontal = 14.dp, vertical = 14.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Lucide.Check,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                                Text(
                                                    text = tag,
                                                    color = if (isSelected) Color.White else textPrimary,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        WizardStep.COMPLETING -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(38.dp),
                                    strokeWidth = 3.dp,
                                    color = Color(0xFF0381FE)
                                )
                            }
                        }
                    }
                }
            }

            // ── BOTTOM ACTION BAR ───────────────────────────────────────────
            if (currentStep != WizardStep.COMPLETING) {
                val canProceed = when (currentStep) {
                    WizardStep.IDENTITY -> displayName.isNotBlank() && usernameStatus == UsernameStatus.Free && password.length >= 6 && password == confirmPassword
                    WizardStep.PERSONAL_DETAILS -> dobMillis != null
                    WizardStep.INTERESTS -> selectedInterests.isNotEmpty()
                    WizardStep.COMPLETING -> false
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(CircleShape)
                        .background(if (canProceed) Color(0xFF0381FE) else if (isDark) Color(0xFF222226) else Color(0xFFE2E8F0))
                        .clickable(enabled = canProceed, role = Role.Button) {
                            when (currentStep) {
                                WizardStep.IDENTITY -> currentStep = WizardStep.PERSONAL_DETAILS
                                WizardStep.PERSONAL_DETAILS -> currentStep = WizardStep.INTERESTS
                                WizardStep.INTERESTS -> {
                                    currentStep = WizardStep.COMPLETING
                                    onComplete(
                                        WizardOutputData(
                                            email = googleData.email,
                                            googleId = googleData.googleId,
                                            displayName = displayName.trim(),
                                            username = username.trim().lowercase(),
                                            avatarUrl = avatarUrl,
                                            avatarUri = avatarUri,
                                            dobMillis = dobMillis,
                                            bio = bio.trim(),
                                            interests = selectedInterests.toList(),
                                            password = password,
                                            customEmail = "${username.trim().lowercase()}@oscellion.com"
                                        )
                                    )
                                }
                                WizardStep.COMPLETING -> {}
                            }
                        }
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (currentStep == WizardStep.INTERESTS) "Complete Setup" else "Continue",
                        color = if (canProceed) Color.White else textMuted,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Lucide.ArrowRight,
                        contentDescription = null,
                        tint = if (canProceed) Color.White else textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Material3 DatePickerDialog
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = latestAllowedBirthMillis()
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val selected = datePickerState.selectedDateMillis
                        if (selected != null && selected <= latestAllowedBirthMillis()) {
                            dobMillis = selected
                            showDatePicker = false
                        }
                    }) {
                        Text("Confirm", color = if (isDark) Color(0xFF3E91FF) else Color(0xFF0072DE), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel", color = textMuted)
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}
