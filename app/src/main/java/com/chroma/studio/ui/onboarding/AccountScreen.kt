package com.chroma.studio.ui.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.data.DiscoverRepository
import com.chroma.studio.ui.components.IosAvatarView
import com.chroma.studio.ui.components.ProfileCropTarget
import com.chroma.studio.ui.components.ProfileMediaEditorModal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun AccountScreen(
    state: OnboardingState,
    onBack: () -> Unit,
    onNext: () -> Unit,
    discoverRepo: DiscoverRepository? = null,
    showPresets: Boolean = true
) {
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    var showDob by remember { mutableStateOf(false) }
    var tried by remember { mutableStateOf(false) }

    var pendingCropUri by remember { mutableStateOf<Uri?>(null) }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            pendingCropUri = uri
        }
    }

    val dobOk = state.dobMillis?.let { it <= latestAllowedBirthMillis() } ?: false
    val dobText = state.dobMillis?.let {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(it))
    } ?: ""

    ObScreen {
        ObNavBar(onBack = onBack, step = 2, totalSteps = 3)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ObHeader(
                firstLine = "Describe your",
                highlightLine = "personality",
                subtitle = "Tell us about your personality, voice, and vibe. Be as specific or vague as you want."
            )
            Spacer(Modifier.height(14.dp))

            // Multiline Personality / Bio Card with 0/500 counter (matching reference screen 3)
            ObGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(Color(0x8C111636))
                        .padding(14.dp)
                ) {
                    BasicTextField(
                        value = state.bio,
                        onValueChange = { if (it.length <= 500) state.bio = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 96.dp),
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp, lineHeight = 21.sp),
                        cursorBrush = SolidColor(ObColors.Lime),
                        decorationBox = { innerTextField ->
                            if (state.bio.isEmpty()) {
                                Text(
                                    text = "e.g., A sarcastic food critic from Dubai who secretly loves fast food but publicly only reviews fine dining restaurants. Thinks every meal deserves a dramatic backstory...",
                                    color = Color(0x66FFFFFF),
                                    fontSize = 13.5.sp,
                                    lineHeight = 19.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${state.bio.length} / 500",
                        color = Color(0x60FFFFFF),
                        fontSize = 11.5.sp,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }

            if (showPresets) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "OR TRY ONE OF THESE",
                    style = ObType.SectionHeader,
                    modifier = Modifier.padding(horizontal = 22.dp)
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PersonalityCard(
                        name = "Lex Voidwalker",
                        snippet = "Your favorite game is mid. Your marketing strategy is a PowerPoint funeral. I have receipts.",
                        gender = Gender.Male
                    ) {
                        state.bio = "Your favorite game is mid. Your marketing strategy is a PowerPoint funeral. I have receipts."
                        state.updateUsername("lexvoidwalker", discoverRepo, scope)
                        state.fullName = "Lex Voidwalker"
                        state.gender = Gender.Male
                        state.dobMillis = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { set(2001, 3, 12) }.timeInMillis
                    }
                    PersonalityCard(
                        name = "The Hype Beast",
                        snippet = "If your launch doesn't break the internet, did it even happen? Everything is a masterclass.",
                        gender = Gender.NonBinary
                    ) {
                        state.bio = "If your launch doesn't break the internet, did it even happen? Everything is a masterclass."
                        state.updateUsername("thehypebeast", discoverRepo, scope)
                        state.fullName = "The Hype Beast"
                        state.gender = Gender.NonBinary
                        state.dobMillis = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { set(2000, 7, 24) }.timeInMillis
                    }
                    PersonalityCard(
                        name = "Elena Vance",
                        snippet = "Creative director, 3D motion shaders. Designing high-density visual universes.",
                        gender = Gender.Female
                    ) {
                        state.bio = "Creative director, 3D motion shaders. Designing high-density visual universes."
                        state.updateUsername("elenavance", discoverRepo, scope)
                        state.fullName = "Elena Vance"
                        state.gender = Gender.Female
                        state.dobMillis = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { set(2002, 8, 22) }.timeInMillis
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Studio Profile Identity & Apple Avatar View + Crop Editor
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, ObColors.Lime, CircleShape)
                        .clickable { galleryLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    IosAvatarView(
                        name = state.fullName.ifBlank { state.username },
                        avatarUrl = state.avatarUri?.toString() ?: state.avatarPresetId ?: "",
                        size = 76.dp,
                        gender = state.gender.name,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Small camera badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0C2088))
                            .border(1.5.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CameraAlt,
                            contentDescription = "Change Avatar",
                            tint = ObColors.Lime,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            ObGlassCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Username with live debounced checking
                    ObTextField(
                        value = state.username,
                        onValueChange = { state.updateUsername(it, discoverRepo, scope) },
                        placeholder = "username",
                        leading = Icons.Rounded.AlternateEmail,
                        leadingTint = ObColors.Lime,
                        isError = tried && state.usernameStatus != UsernameStatus.Free,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Next, autoCorrect = false),
                        keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
                        trailing = {
                            if (state.username.isNotEmpty()) {
                                when (state.usernameStatus) {
                                    UsernameStatus.Free -> {
                                        Icon(Icons.Rounded.CheckCircle, null, tint = ObColors.Lime, modifier = Modifier.size(16.dp))
                                        Text("Free", color = ObColors.Lime, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    UsernameStatus.Taken -> {
                                        Icon(Icons.Rounded.Cancel, null, tint = ObColors.Danger, modifier = Modifier.size(16.dp))
                                        Text("Taken", color = ObColors.Danger, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    UsernameStatus.TooShort -> {
                                        Icon(Icons.Rounded.Error, null, tint = ObColors.Amber, modifier = Modifier.size(16.dp))
                                        Text("Min 3", color = ObColors.Amber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    UsernameStatus.Checking -> {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = ObColors.Lime, strokeWidth = 2.dp)
                                    }
                                }
                            }
                        }
                    )

                    // Suggested usernames if taken
                    if (state.usernameStatus == UsernameStatus.Taken && state.usernameSuggestions.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Suggestions:", color = ObColors.TextFaint, fontSize = 11.sp)
                            state.usernameSuggestions.take(2).forEach { sug ->
                                Text(
                                    text = "@$sug",
                                    color = ObColors.Lime,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable {
                                        state.updateUsername(sug, discoverRepo, scope)
                                    }
                                )
                            }
                        }
                    }

                    // Full Name
                    ObTextField(
                        value = state.fullName,
                        onValueChange = { state.fullName = it.take(50) },
                        placeholder = "Full name",
                        leading = Icons.Rounded.Person,
                        isError = tried && state.fullName.isBlank(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() })
                    )

                    // Birthday
                    ObTextField(
                        value = dobText,
                        onValueChange = {},
                        placeholder = "Birthday (13+)",
                        leading = Icons.Rounded.CalendarMonth,
                        isError = tried && !dobOk,
                        onClick = { focus.clearFocus(); showDob = true }
                    )

                    // Gender Selector
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Gender.values().forEach { g ->
                            val sel = state.gender == g
                            Box(
                                Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(10.dp))
                                    .background(if (sel) Color.White else Color(0xA0060F42))
                                    .border(if (sel) 1.5.dp else 1.dp, if (sel) Color(0xFFFACC15) else Color(0x3D8CB6FF), RoundedCornerShape(10.dp))
                                    .clickable(role = Role.RadioButton) { state.gender = g },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    g.label, fontSize = 11.sp, maxLines = 1, fontWeight = if (sel) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (sel) Color(0xFF09155C) else Color(0xDDE4EEFF)
                                )
                            }
                        }
                    }
                    if (tried && !dobOk && state.dobMillis != null) Text("You must be 13 or older to join.", color = ObColors.Danger, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        ObPrimaryButton(
            text = "Next: Set Password",
            onClick = {
                state.ensureAccountDefaults()
                onNext()
            },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )
    }

    if (showDob) DobPickerDialog(state.dobMillis, { showDob = false }) { state.dobMillis = it; showDob = false }

    // Integrated Profile Avatar Crop & Transform Editor Modal
    if (pendingCropUri != null) {
        ProfileMediaEditorModal(
            imageUri = pendingCropUri!!,
            target = ProfileCropTarget.AVATAR,
            onDismiss = { pendingCropUri = null },
            onCropSuccess = { croppedUri ->
                pendingCropUri = null
                state.avatarUri = croppedUri
            }
        )
    }
}

@Composable
private fun PersonalityCard(
    name: String,
    snippet: String,
    gender: Gender,
    onClick: () -> Unit
) {
    ObGlassCard(
        modifier = Modifier
            .width(200.dp)
            .clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .background(Color(0x99111636))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(Color(0xFF0C1238))
                        .border(1.dp, Color(0x60FFFFFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    IosAvatarView(
                        name = name,
                        avatarUrl = "",
                        size = 34.dp,
                        gender = gender.name,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Text(name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            Text(snippet, color = Color(0xCCB0C2F0), fontSize = 11.5.sp, lineHeight = 16.sp, maxLines = 3)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DobPickerDialog(initial: Long?, onDismiss: () -> Unit, onPick: (Long) -> Unit) {
    val maxMillis = remember { latestAllowedBirthMillis() }
    val maxYear = remember { Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = maxMillis }.get(Calendar.YEAR) }
    val picker = rememberDatePickerState(
        initialSelectedDateMillis = initial,
        yearRange = 1920..maxYear,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= maxMillis
            override fun isSelectableYear(year: Int) = year <= maxYear
        }
    )
    val colors = DatePickerDefaults.colors(
        containerColor = ObColors.Night,
        titleContentColor = ObColors.TextMuted,
        headlineContentColor = Color.White,
        weekdayContentColor = ObColors.TextMuted,
        subheadContentColor = ObColors.TextMuted,
        navigationContentColor = Color.White,
        yearContentColor = Color.White,
        currentYearContentColor = ObColors.Lime,
        selectedYearContentColor = ObColors.OnLime,
        selectedYearContainerColor = ObColors.Lime,
        dayContentColor = Color.White,
        disabledDayContentColor = Color(0x55FFFFFF),
        selectedDayContentColor = ObColors.OnLime,
        selectedDayContainerColor = ObColors.Lime,
        todayContentColor = ObColors.Lime,
        todayDateBorderColor = ObColors.Lime
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        colors = colors,
        confirmButton = {
            TextButton({ picker.selectedDateMillis?.let(onPick) }, enabled = picker.selectedDateMillis != null) {
                Text("OK", color = ObColors.Lime, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel", color = Color.White) } }
    ) { DatePicker(picker, colors = colors) }
}
