package com.chroma.studio.ui.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
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
    discoverRepo: DiscoverRepository? = null
) {
    val focus = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
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
        ObNavBar(onBack = onBack, step = 2, totalSteps = 4) {
            ObBadge("Step 2/4", ObColors.NeonGreen)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ObHeader(
                firstLine = "Create your",
                highlightLine = "studio identity",
                subtitle = "Set up your handle, profile avatar, and creator bio."
            )
            Spacer(Modifier.height(16.dp))

            // Studio Profile Identity & Apple Avatar View + Crop Editor (Centered at top)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .border(2.dp, ObColors.NeonGreen, CircleShape)
                        .clickable { galleryLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    IosAvatarView(
                        name = state.fullName.ifBlank { state.username.ifBlank { "Creator" } },
                        avatarUrl = state.avatarUri?.toString() ?: state.avatarPresetId ?: "",
                        size = 84.dp,
                        gender = state.gender.name,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Small camera badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0C2088))
                            .border(1.5.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CameraAlt,
                            contentDescription = "Change Avatar",
                            tint = ObColors.NeonGreen,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(18.dp))

            // Direct profile fields
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Full Name with Uiverse floating label
                ObTextField(
                    value = state.fullName,
                    onValueChange = { state.fullName = it.take(50) },
                    placeholder = "Full name",
                    leading = Icons.Rounded.Person,
                    isError = tried && state.fullName.isBlank(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) })
                )
                if (tried && state.fullName.isBlank()) {
                    Text(
                        text = "Please enter your full name",
                        color = ObColors.Danger,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }

                // Username with live debounced checking and Uiverse floating label
                ObTextField(
                    value = state.username,
                    onValueChange = { state.updateUsername(it, discoverRepo, scope) },
                    placeholder = "Username (@handle)",
                    leading = Icons.Rounded.AlternateEmail,
                    isError = tried && state.usernameStatus != UsernameStatus.Free,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Next, autoCorrect = false),
                    keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
                    trailing = {
                        if (state.username.isNotEmpty()) {
                            when (state.usernameStatus) {
                                UsernameStatus.Free -> {
                                    Icon(Icons.Rounded.CheckCircle, null, tint = ObColors.NeonGreen, modifier = Modifier.size(16.dp))
                                    Text("Free", color = ObColors.NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = ObColors.NeonGreen, strokeWidth = 2.dp)
                                }
                            }
                        }
                    }
                )

                if (tried && state.usernameStatus != UsernameStatus.Free) {
                    val errorMsg = when (state.usernameStatus) {
                        UsernameStatus.TooShort -> "Username must be at least 3 characters"
                        UsernameStatus.Taken -> "This username is already taken"
                        UsernameStatus.Checking -> "Checking availability..."
                        UsernameStatus.Free -> ""
                    }
                    if (errorMsg.isNotEmpty()) {
                        Text(
                            text = errorMsg,
                            color = ObColors.Danger,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }

                // Suggested usernames if taken
                if (state.usernameStatus == UsernameStatus.Taken && state.usernameSuggestions.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Suggestions:", color = ObColors.TextFaint, fontSize = 11.sp)
                        state.usernameSuggestions.take(3).forEach { sug ->
                            Text(
                                text = "@$sug",
                                color = ObColors.NeonGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x1F00FF87))
                                    .clickable {
                                        state.updateUsername(sug, discoverRepo, scope)
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Birthday with Uiverse floating label
                ObTextField(
                    value = dobText,
                    onValueChange = {},
                    placeholder = "Birthday (13+)",
                    leading = Icons.Rounded.CalendarMonth,
                    isError = tried && !dobOk,
                    onClick = { focus.clearFocus(); showDob = true }
                )
                if (tried && !dobOk) {
                    Text(
                        text = if (state.dobMillis == null) "Please select your birthday" else "You must be 13 or older to join.",
                        color = ObColors.Danger,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }

                // Gender Selector
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Gender.values().forEach { g ->
                        val sel = state.gender == g
                        Box(
                            Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(10.dp))
                                .background(if (sel) Color.White else Color(0xA0060F42))
                                .border(if (sel) 1.5.dp else 1.dp, if (sel) ObColors.NeonGreen else Color(0x3D8CB6FF), RoundedCornerShape(10.dp))
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
            }

            Spacer(Modifier.height(16.dp))

            // Multiline Personality / Bio input with Uiverse floating label & transparent background
            var isBioFocused by remember { mutableStateOf(false) }
            val isBioActive = isBioFocused || state.bio.isNotEmpty()
            val bioBorderColor by animateColorAsState(
                targetValue = if (isBioActive) Color(0xFF1A73E8) else Color(0xFF9E9E9E),
                animationSpec = tween(150, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)),
                label = "bioBorder"
            )
            val bioLabelColor by animateColorAsState(
                targetValue = if (isBioActive) Color(0xFF2196F3) else Color(0xFFE8E8E8),
                animationSpec = tween(150, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)),
                label = "bioLabelColor"
            )
            val bioLabelOffsetY by animateDpAsState(
                targetValue = if (isBioActive) (-9).dp else 17.dp,
                animationSpec = tween(150, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)),
                label = "bioLabelOffset"
            )
            val bioLabelScale by animateFloatAsState(
                targetValue = if (isBioActive) 0.8f else 1.0f,
                animationSpec = tween(150, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)),
                label = "bioLabelScale"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 9.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent, RoundedCornerShape(16.dp))
                        .border(1.5.dp, bioBorderColor, RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    BasicTextField(
                        value = state.bio,
                        onValueChange = { if (it.length <= 500) state.bio = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 84.dp)
                            .onFocusChanged { isBioFocused = it.isFocused },
                        textStyle = TextStyle(color = Color(0xFFF5F5F5), fontSize = 15.sp, lineHeight = 22.sp),
                        cursorBrush = SolidColor(Color(0xFF2196F3)),
                        decorationBox = { innerTextField ->
                            if (state.bio.isEmpty() && isBioFocused) {
                                Text(
                                    text = "e.g., Designing cinematic LUTs, visual shaders, and motion graphics...",
                                    color = Color(0x66FFFFFF),
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
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

                // Floating label with smooth navy background
                Box(
                    modifier = Modifier
                        .padding(start = 15.dp)
                        .offset(y = bioLabelOffsetY)
                        .graphicsLayer {
                            scaleX = bioLabelScale
                            scaleY = bioLabelScale
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                        }
                        .then(
                            if (isBioActive) {
                                Modifier
                                    .background(Color(0xFF0E143A), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            } else Modifier
                        )
                ) {
                    Text(
                        text = "Creator voice & bio",
                        color = bioLabelColor,
                        fontSize = 16.sp,
                        fontWeight = if (isBioActive) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }



            Spacer(Modifier.height(20.dp))
        }
        ObPrimaryButton(
            text = "Next: Set Password",
            onClick = {
                tried = true
                if (!state.accountValid) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    return@ObPrimaryButton
                }
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
