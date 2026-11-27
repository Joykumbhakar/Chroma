package com.chroma.studio.ui.onboarding

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chroma.studio.R
import com.chroma.studio.data.DiscoverRepository
import com.chroma.studio.ui.components.BeveledCheckMark
import com.chroma.studio.ui.components.PasswordRequirementChecklist
import com.chroma.studio.ui.components.ProfileCropTarget
import com.chroma.studio.ui.components.ProfileMediaEditorModal
import com.chroma.studio.ui.components.StudioBeveledConfirmPasswordField
import com.chroma.studio.ui.components.StudioBeveledPasswordField
import com.chroma.studio.ui.components.StudioDesignTokens
import com.chroma.studio.ui.components.evaluatePasswordDemands
import com.chroma.studio.ui.theme.InterFontFamily
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*
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
    val email: String = "",
    val googleId: String = "",
    val displayName: String = "",
    val photoUrl: String = ""
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
    val customEmail: String = "",
    val gender: String = "Private"
)

private data class CreativeCategoryOption(
    val title: String,
    val icon: ImageVector
)

private fun calculateAgeYears(dobMillis: Long): Int {
    val dobCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = dobMillis }
    val nowCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    var age = nowCal.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR)
    if (nowCal.get(Calendar.DAY_OF_YEAR) < dobCal.get(Calendar.DAY_OF_YEAR)) {
        age -= 1
    }
    return age
}

/**
 * Multi-Step Account Creation Workflow (OnboardingWizard) crafted with Stageshow precision:
 *
 * - Unified Obsidian / Daylight surface hierarchy & real Oscellion product chrome
 * - Step 1 (IDENTITY): Live Creator Pass card, Avatar crop, Unique @handle lookup + Security password setup
 * - Step 2 (PERSONAL_DETAILS): Date of birth (13+ verification), Gender identity pills, Creator Bio
 * - Step 3 (INTERESTS): Icon-paired 2-column Bento discovery grid
 * - Step 4 (COMPLETING): Live provisioning sheet showing account commits & @oscellion.com identity
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingWizard(
    googleData: GoogleUserData = GoogleUserData(),
    initialUsername: String = "",
    initialPassword: String = "",
    isOAuthUser: Boolean = googleData.googleId.isNotBlank(),
    externalError: String? = null,
    errorResetKey: Int = 0,
    discoverRepo: DiscoverRepository,
    onBackToWelcome: () -> Unit,
    onComplete: (WizardOutputData) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var currentStep by remember { mutableStateOf(WizardStep.IDENTITY) }

    LaunchedEffect(errorResetKey) {
        if (errorResetKey > 0 && currentStep == WizardStep.COMPLETING) {
            currentStep = WizardStep.IDENTITY
        }
    }

    var displayName by remember {
        mutableStateOf(
            googleData.displayName.ifBlank {
                initialUsername.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
            }
        )
    }
    var username by remember {
        val base = if (initialUsername.isNotBlank()) {
            initialUsername.filter { it.isLetterOrDigit() || it == '_' }.lowercase()
        } else if (googleData.email.isNotBlank()) {
            googleData.email.substringBefore("@").filter { it.isLetterOrDigit() || it == '_' }.lowercase()
        } else {
            googleData.displayName.filter { it.isLetterOrDigit() || it == '_' }.lowercase()
        }
        mutableStateOf(base.ifBlank { "creator" })
    }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var avatarUrl by remember { mutableStateOf(googleData.photoUrl) }
    var pendingCropUri by remember { mutableStateOf<Uri?>(null) }

    var password by remember { mutableStateOf(initialPassword) }
    var confirmPassword by remember { mutableStateOf(initialPassword) }

    var usernameStatus by remember { mutableStateOf(UsernameStatus.Checking) }
    var usernameSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var usernameCheckJob by remember { mutableStateOf<Job?>(null) }

    var dobMillis by remember { mutableStateOf<Long?>(null) }
    var gender by remember { mutableStateOf("Private") }
    var bio by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    val selectedInterests = remember { mutableStateListOf("Creative Studio", "Design Systems") }

    val allCategories = remember {
        listOf(
            CreativeCategoryOption("Creative Studio", Lucide.Layers),
            CreativeCategoryOption("Color & Light", Lucide.Sun),
            CreativeCategoryOption("Generative Art", Lucide.Aperture),
            CreativeCategoryOption("Photography", Lucide.Camera),
            CreativeCategoryOption("Motion & 3D", Lucide.Box),
            CreativeCategoryOption("Typography", Lucide.Type),
            CreativeCategoryOption("Minimalism", Lucide.Compass),
            CreativeCategoryOption("Design Systems", Lucide.LayoutGrid),
            CreativeCategoryOption("Code & Dev", Lucide.Code),
            CreativeCategoryOption("Audio & Music", Lucide.Music),
            CreativeCategoryOption("Architecture", Lucide.Building2),
            CreativeCategoryOption("Illustration", Lucide.PenTool)
        )
    }

    val avatarGalleryLauncher = com.chroma.studio.ui.components.rememberOscellionMediaPicker(
        title = "Profile Avatar",
        mediaFilter = com.chroma.studio.ui.components.GalleryMediaTypeFilter.PHOTOS,
        showAlbumsTab = true
    ) { uri ->
        pendingCropUri = uri
    }

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
        WizardStep.COMPLETING -> 3
    }
    val progressRatio by animateFloatAsState(
        targetValue = stepIndex / 3f,
        animationSpec = spring(stiffness = 280f, dampingRatio = 0.85f),
        label = "wizard_progress"
    )

    val colors = LocalOscellionColors.current
    val isDark = colors.isDark
    val screenBg = if (isDark) Color(0xFF09090B) else Color(0xFFFAFAFA)
    val surfaceBg = if (isDark) Color(0xFF13131A) else Color(0xFFFFFFFF)
    val fieldBg = if (isDark) Color(0xFF1B1B24) else Color(0xFFF2F4F7)
    val hairline = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.07f)
    val textPrimary = if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B)
    val textMuted = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A)
    val brandBlue = Color(0xFF0381FE)
    val emeraldSuccess = Color(0xFF10B981)

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
                .padding(horizontal = 22.dp, vertical = 12.dp)
        ) {
            // ── TOP CHROME BAR (Real Oscellion Product Header) ───────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (currentStep != WizardStep.COMPLETING) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(surfaceBg)
                            .border(0.8.dp, hairline, CircleShape)
                            .clickable(role = Role.Button) {
                                when (currentStep) {
                                    WizardStep.IDENTITY -> onBackToWelcome()
                                    WizardStep.PERSONAL_DETAILS -> currentStep = WizardStep.IDENTITY
                                    WizardStep.INTERESTS -> currentStep = WizardStep.PERSONAL_DETAILS
                                    WizardStep.COMPLETING -> {}
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.ArrowLeft,
                            contentDescription = "Back",
                            tint = textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Spacer(Modifier.size(36.dp))
                }

                // Continuous capsule step bar
                Box(
                    modifier = Modifier
                        .width(108.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressRatio)
                            .clip(CircleShape)
                            .background(brandBlue)
                    )
                }

                // Borderless Step Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isDark) Color(0x1E0381FE) else Color(0x140381FE))
                        .padding(horizontal = 11.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$stepIndex / 3",
                        color = brandBlue,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Brand Identity Lockup Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_oscellion_app_icon),
                    contentDescription = "Oscellion",
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )
                Text(
                    text = "OSCELLION ACCOUNT SETUP",
                    color = textMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.9.sp
                )
            }

            Spacer(Modifier.height(10.dp))

            // ── MAIN WIZARD BODY ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    label = "wizardTransition",
                    transitionSpec = {
                        fadeIn(tween(260)) togetherWith fadeOut(tween(180))
                    }
                ) { step ->
                    when (step) {
                        WizardStep.IDENTITY -> {
                            val customEmailPreview = if (username.length >= 3) {
                                "${username.lowercase()}@oscellion.com"
                            } else {
                                "handle@oscellion.com"
                            }
                            val passwordDemands = remember(password) { evaluatePasswordDemands(password) }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "Claim your studio identity",
                                    color = textPrimary,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.6).sp,
                                    fontFamily = InterFontFamily
                                )

                                // ── Live Stageshow Identity Pass Card ──
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(surfaceBg)
                                        .border(1.dp, hairline, RoundedCornerShape(20.dp))
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clickable(role = Role.Button) { avatarGalleryLauncher.launch("image/*") },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .background(fieldBg)
                                                .border(1.5.dp, brandBlue, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            when {
                                                avatarUri != null -> {
                                                    AsyncImage(
                                                        model = avatarUri,
                                                        contentDescription = "Avatar",
                                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }
                                                avatarUrl.isNotBlank() -> {
                                                    AsyncImage(
                                                        model = avatarUrl,
                                                        contentDescription = "Avatar",
                                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }
                                                else -> {
                                                    Icon(
                                                        imageVector = Lucide.User,
                                                        contentDescription = null,
                                                        tint = textMuted,
                                                        modifier = Modifier.size(28.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(brandBlue)
                                                .border(2.dp, surfaceBg, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Lucide.Camera,
                                                contentDescription = "Change avatar",
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = displayName.ifBlank { "Creator Name" },
                                            color = textPrimary,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "@${username.ifBlank { "handle" }}",
                                            color = brandBlue,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        // Linked Oscellion handle email pill (borderless)
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(if (isDark) Color(0x160381FE) else Color(0x100381FE))
                                                .padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Lucide.Mail,
                                                contentDescription = null,
                                                tint = brandBlue,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Text(
                                                text = customEmailPreview,
                                                color = if (isDark) Color(0xFF93C5FD) else Color(0xFF0072DE),
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                // ── Grouped Identity Inputs Surface ──
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(surfaceBg)
                                        .border(1.dp, hairline, RoundedCornerShape(18.dp))
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Display Name
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "DISPLAY NAME",
                                            color = textMuted,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.6.sp
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(fieldBg)
                                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                        ) {
                                            if (displayName.isEmpty()) {
                                                Text(
                                                    text = "e.g. Alex Rivera",
                                                    color = textMuted.copy(alpha = 0.6f),
                                                    fontSize = 14.5.sp
                                                )
                                            }
                                            BasicTextField(
                                                value = displayName,
                                                onValueChange = { displayName = it },
                                                singleLine = true,
                                                textStyle = TextStyle(
                                                    color = textPrimary,
                                                    fontSize = 14.5.sp,
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                cursorBrush = SolidColor(brandBlue),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }

                                    // Unique Handle (@username)
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "UNIQUE HANDLE",
                                                color = textMuted,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.6.sp
                                            )
                                            when (usernameStatus) {
                                                UsernameStatus.Checking -> CircularProgressIndicator(
                                                    modifier = Modifier.size(12.dp),
                                                    strokeWidth = 1.8.dp,
                                                    color = brandBlue
                                                )
                                                UsernameStatus.Free -> Text(
                                                    text = "Available",
                                                    color = emeraldSuccess,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                UsernameStatus.Taken -> Text(
                                                    text = "Already taken",
                                                    color = Color(0xFFEF4444),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                UsernameStatus.TooShort -> Text(
                                                    text = "Min 3 chars",
                                                    color = textMuted,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(fieldBg)
                                                .border(
                                                    width = if (usernameStatus == UsernameStatus.Free || usernameStatus == UsernameStatus.Taken) 1.dp else 0.dp,
                                                    color = when (usernameStatus) {
                                                        UsernameStatus.Free -> emeraldSuccess.copy(alpha = 0.55f)
                                                        UsernameStatus.Taken -> Color(0xFFEF4444).copy(alpha = 0.65f)
                                                        else -> Color.Transparent
                                                    },
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "@",
                                                    color = brandBlue,
                                                    fontSize = 14.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                BasicTextField(
                                                    value = username,
                                                    onValueChange = { verifyUsername(it) },
                                                    singleLine = true,
                                                    textStyle = TextStyle(
                                                        color = textPrimary,
                                                        fontSize = 14.5.sp,
                                                        fontWeight = FontWeight.Medium
                                                    ),
                                                    cursorBrush = SolidColor(brandBlue),
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }

                                        if (usernameSuggestions.isNotEmpty()) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Try:",
                                                    color = textMuted,
                                                    fontSize = 11.sp
                                                )
                                                usernameSuggestions.forEach { sug ->
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(999.dp))
                                                            .background(if (isDark) Color(0x1E0381FE) else Color(0x120381FE))
                                                            .clickable { verifyUsername(sug) }
                                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(
                                                            text = "@$sug",
                                                            color = brandBlue,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // ── Studio Security Surface Card ──
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(surfaceBg)
                                        .border(1.dp, hairline, RoundedCornerShape(18.dp))
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isOAuthUser) "STUDIO PASSWORD (OPTIONAL)" else "STUDIO PASSWORD",
                                            color = textMuted,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.6.sp
                                        )
                                        if (password.isNotEmpty()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                BeveledCheckMark(met = passwordDemands.allMet, size = 14.dp)
                                                Text(
                                                    text = if (passwordDemands.allMet) "5/5 Verified" else "${passwordDemands.metCount}/5",
                                                    color = if (passwordDemands.allMet) emeraldSuccess else textMuted,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }

                                    StudioBeveledPasswordField(
                                        value = password,
                                        onValueChange = { password = it },
                                        placeholder = "Create a strong studio password",
                                        allDemandsMet = passwordDemands.allMet
                                    )

                                    PasswordRequirementChecklist(
                                        password = password,
                                        alwaysVisible = true
                                    )

                                    StudioBeveledConfirmPasswordField(
                                        value = confirmPassword,
                                        onValueChange = { confirmPassword = it },
                                        targetPassword = password,
                                        allDemandsMet = passwordDemands.allMet,
                                        placeholder = if (isOAuthUser) "Confirm password (if set)" else "Confirm password"
                                    )
                                    if (isOAuthUser && password.isEmpty()) {
                                        Text(
                                            text = "Signed in with Google/Apple. Leave blank to use instant OAuth, or set a password for @handle login too.",
                                            color = textMuted,
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }

                                Spacer(Modifier.height(4.dp))
                            }
                        }

                        WizardStep.PERSONAL_DETAILS -> {
                            val dobFormatted = remember(dobMillis) {
                                dobMillis?.let {
                                    SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(it))
                                } ?: "Select birth date"
                            }
                            val ageYears = dobMillis?.let { calculateAgeYears(it) }
                            val isUnderage = dobMillis?.let { it > latestAllowedBirthMillis() } ?: false

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Profile & age check",
                                        color = textPrimary,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.6).sp,
                                        fontFamily = InterFontFamily
                                    )
                                    Text(
                                        text = "Confirm eligibility (13+) and introduce your creative focus.",
                                        color = textMuted,
                                        fontSize = 13.5.sp
                                    )
                                }

                                // ── Age & Gender Structured Card ──
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(surfaceBg)
                                        .border(1.dp, hairline, RoundedCornerShape(18.dp))
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "DATE OF BIRTH",
                                                color = textMuted,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.6.sp
                                            )
                                            if (dobMillis != null && !isUnderage && ageYears != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(999.dp))
                                                        .background(emeraldSuccess.copy(alpha = 0.14f))
                                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "$ageYears yrs • 13+ Verified",
                                                        color = emeraldSuccess,
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(fieldBg)
                                                .clickable { showDatePicker = true }
                                                .padding(horizontal = 14.dp, vertical = 13.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = dobFormatted,
                                                color = if (dobMillis != null) textPrimary else textMuted,
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Icon(
                                                imageVector = Lucide.Calendar,
                                                contentDescription = null,
                                                tint = brandBlue,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    }

                                    if (isUnderage) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Lucide.ShieldAlert,
                                                contentDescription = null,
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "You must be 13 or older to create an Oscellion account.",
                                                color = if (isDark) Color(0xFFFFB4AB) else Color(0xFFBA1A1A),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "GENDER IDENTITY",
                                            color = textMuted,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.6.sp
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf("Male", "Female", "Non-binary", "Private").forEach { g ->
                                                val selected = gender == g
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(40.dp)
                                                        .clip(RoundedCornerShape(999.dp))
                                                        .background(if (selected) brandBlue else fieldBg)
                                                        .clickable {
                                                            gender = g
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = g,
                                                        fontSize = 11.5.sp,
                                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (selected) Color.White else textPrimary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // ── Bio Card ──
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(surfaceBg)
                                        .border(1.dp, hairline, RoundedCornerShape(18.dp))
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "CREATOR BIO",
                                            color = textMuted,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.6.sp
                                        )
                                        Text(
                                            text = "${bio.length}/150",
                                            color = textMuted,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(106.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(fieldBg)
                                            .padding(horizontal = 14.dp, vertical = 12.dp)
                                    ) {
                                        if (bio.isEmpty()) {
                                            Text(
                                                text = "Colorist, shader designer, 3D motion artist…",
                                                color = textMuted.copy(alpha = 0.65f),
                                                fontSize = 14.sp
                                            )
                                        }
                                        BasicTextField(
                                            value = bio,
                                            onValueChange = { if (it.length <= 150) bio = it },
                                            textStyle = TextStyle(color = textPrimary, fontSize = 14.sp, lineHeight = 20.sp),
                                            cursorBrush = SolidColor(brandBlue),
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }

                        WizardStep.INTERESTS -> {
                            val readyCount = selectedInterests.size >= 2
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Personalize your feed",
                                            color = textPrimary,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = (-0.6).sp,
                                            fontFamily = InterFontFamily
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "Pick 2 or more disciplines to tune your discovery stream.",
                                            color = textMuted,
                                            fontSize = 13.5.sp
                                        )
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(
                                            if (readyCount) emeraldSuccess.copy(alpha = 0.14f)
                                            else if (isDark) Color(0xFF1B1B24) else Color(0xFFF2F4F7)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = if (readyCount) "${selectedInterests.size} selected · Ready" else "${selectedInterests.size}/2 minimum selected",
                                        color = if (readyCount) emeraldSuccess else textMuted,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Spacer(Modifier.height(14.dp))

                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f).fillMaxWidth()
                                ) {
                                    items(allCategories) { item ->
                                        val isSelected = selectedInterests.contains(item.title)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(54.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(if (isSelected) brandBlue else surfaceBg)
                                                .border(
                                                    width = if (isSelected) 0.dp else 1.dp,
                                                    color = if (isSelected) Color.Transparent else hairline,
                                                    shape = RoundedCornerShape(14.dp)
                                                )
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    if (isSelected) selectedInterests.remove(item.title)
                                                    else selectedInterests.add(item.title)
                                                }
                                                .padding(horizontal = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(9.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isSelected) Lucide.Check else item.icon,
                                                contentDescription = null,
                                                tint = if (isSelected) Color.White else brandBlue,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = item.title,
                                                color = if (isSelected) Color.White else textPrimary,
                                                fontSize = 12.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        WizardStep.COMPLETING -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(22.dp))
                                        .background(surfaceBg)
                                        .border(1.dp, hairline, RoundedCornerShape(22.dp))
                                        .padding(22.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(34.dp),
                                        strokeWidth = 2.8.dp,
                                        color = brandBlue
                                    )
                                    Text(
                                        text = "Initializing @${username.trim().lowercase()}",
                                        color = textPrimary,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Securing your Oscellion keys and calibrating your studio workspace…",
                                        color = textMuted,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )

                                    HorizontalDivider(color = hairline)

                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        ProvisioningRow(
                                            label = "Handle reserved: @${username.trim().lowercase()}",
                                            done = true,
                                            textPrimary = textPrimary
                                        )
                                        ProvisioningRow(
                                            label = "Studio email: ${username.trim().lowercase()}@oscellion.com",
                                            done = true,
                                            textPrimary = textPrimary
                                        )
                                        ProvisioningRow(
                                            label = "Curating ${selectedInterests.size} creative channels",
                                            done = false,
                                            textPrimary = textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── BOTTOM ACTION CTA ────────────────────────────────────────────
            if (currentStep != WizardStep.COMPLETING) {
                val passwordSatisfied = if (isOAuthUser && password.isEmpty() && confirmPassword.isEmpty()) {
                    true
                } else {
                    evaluatePasswordDemands(password).allMet && password == confirmPassword
                }
                val canProceed = when (currentStep) {
                    WizardStep.IDENTITY -> displayName.isNotBlank() && usernameStatus == UsernameStatus.Free && passwordSatisfied
                    WizardStep.PERSONAL_DETAILS -> dobMillis != null && dobMillis!! <= latestAllowedBirthMillis()
                    WizardStep.INTERESTS -> selectedInterests.size >= 2
                    WizardStep.COMPLETING -> false
                }
                val blockerHint = when (currentStep) {
                    WizardStep.IDENTITY -> when {
                        displayName.isBlank() -> "Enter your display name to continue"
                        usernameStatus == UsernameStatus.TooShort -> "Choose a @handle of at least 3 characters"
                        usernameStatus == UsernameStatus.Checking -> "Checking @handle availability…"
                        usernameStatus == UsernameStatus.Taken -> "@$username is taken — pick another handle"
                        !passwordSatisfied && !evaluatePasswordDemands(password).allMet -> "Password must meet all 5 strength checks"
                        !passwordSatisfied && password != confirmPassword -> "Passwords do not match"
                        else -> null
                    }
                    WizardStep.PERSONAL_DETAILS -> when {
                        dobMillis == null -> "Select your date of birth to verify 13+ eligibility"
                        dobMillis!! > latestAllowedBirthMillis() -> "You must be 13+ to create an Oscellion account"
                        else -> null
                    }
                    WizardStep.INTERESTS -> if (selectedInterests.size < 2) "Select at least 2 creative interests" else null
                    WizardStep.COMPLETING -> null
                }

                if (!externalError.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = externalError,
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (blockerHint != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = blockerHint,
                        color = textMuted,
                        fontSize = 11.5.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(10.dp))

                val buttonBg = if (canProceed) {
                    Brush.linearGradient(StudioDesignTokens.CobaltGradient)
                } else {
                    SolidColor(if (isDark) Color(0xFF1A1B24) else Color(0xFFE2E8F0))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(CircleShape)
                        .background(buttonBg)
                        .clickable(enabled = canProceed, role = Role.Button) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                                            customEmail = "${username.trim().lowercase()}@oscellion.com",
                                            gender = gender
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
                        text = if (currentStep == WizardStep.INTERESTS) "Launch Oscellion Studio" else "Continue",
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

        if (showDatePicker) {
            com.chroma.studio.ui.components.StudioCustomCalendarDialog(
                initialDateMillis = dobMillis ?: latestAllowedBirthMillis(),
                maxDateMillis = latestAllowedBirthMillis(),
                onDismissRequest = { showDatePicker = false },
                onDateConfirmed = { selected ->
                    dobMillis = selected
                    showDatePicker = false
                },
                title = "Date of Birth"
            )
        }

        if (pendingCropUri != null) {
            ProfileMediaEditorModal(
                imageUri = pendingCropUri!!,
                target = ProfileCropTarget.AVATAR,
                onDismiss = { pendingCropUri = null },
                onCropSuccess = { cropped ->
                    pendingCropUri = null
                    avatarUri = cropped
                }
            )
        }
    }
}

@Composable
private fun ProvisioningRow(
    label: String,
    done: Boolean,
    textPrimary: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (done) Lucide.Check else Lucide.RefreshCw,
            contentDescription = null,
            tint = if (done) Color(0xFF10B981) else Color(0xFF0381FE),
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = label,
            color = textPrimary.copy(alpha = 0.9f),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
