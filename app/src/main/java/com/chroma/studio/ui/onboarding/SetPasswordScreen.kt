package com.chroma.studio.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Dedicated password step: two boxes (Create / Confirm).
 * Each box shows a check mark when its own need is complete, and every requirement row ticks off as it is met.
 */
@Composable
fun SetPasswordScreen(state: OnboardingState, onBack: () -> Unit, onNext: () -> Unit) {
    val focus = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
    val rules = evaluatePassword(state.password)
    val matches = state.confirm.isNotEmpty() && state.confirm == state.password
    val mismatch = state.confirm.isNotEmpty() && state.confirm.length >= state.password.length && !matches
    var showPw by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    // Light tap each time a requirement flips to met.
    var lastCount by remember { mutableIntStateOf(rules.metCount) }
    LaunchedEffect(rules.metCount) {
        if (rules.metCount > lastCount) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        lastCount = rules.metCount
    }

    ObScreen {
        ObNavBar(onBack, progress = 0.5f + 0.1f * (rules.metCount / 5f) + if (matches) 0.1f else 0f) { ObBadge("Step 4/5", Color.White) }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ObHeader(
            firstLine = "Set your studio",
            highlightLine = "password",
            subtitle = "You’ll use it with @${state.username.ifBlank { "your username" }} to sign in. Use something only you know."
        )
            Spacer(Modifier.height(18.dp))

            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // ── Box 1: create ─────────────────────────────────────────
                FieldLabel("Create password")
                ObTextField(
                    value = state.password,
                    onValueChange = { state.password = it.take(24) },
                    placeholder = "8–24 characters",
                    leading = Icons.Rounded.Lock,
                    leadingTint = if (rules.allMet) ObColors.Lime else ObColors.Sky,
                    visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next, autoCorrect = false),
                    keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
                    trailing = {
                        ObCheck(rules.allMet)
                        EyeToggle(showPw) { showPw = !showPw }
                    }
                )
                StrengthMeter(rules.metCount, state.password.isNotEmpty())
                Spacer(Modifier.height(2.dp))
                ObGlassCard(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp)) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        RuleRow(rules.length, "8 to 24 characters")
                        RuleRow(rules.upper, "One uppercase letter (A–Z)")
                        RuleRow(rules.lower, "One lowercase letter (a–z)")
                        RuleRow(rules.digit, "One number (0–9)")
                        RuleRow(rules.special, "One symbol (#, !, @…)")
                    }
                }

                Spacer(Modifier.height(14.dp))

                // ── Box 2: confirm ────────────────────────────────────────
                FieldLabel("Confirm password")
                ObTextField(
                    value = state.confirm,
                    onValueChange = { state.confirm = it.take(24) },
                    placeholder = "Type it again",
                    leading = if (matches) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                    leadingTint = if (matches) ObColors.Lime else ObColors.Sky,
                    isError = mismatch,
                    visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done, autoCorrect = false),
                    keyboardActions = KeyboardActions(onDone = { if (state.passwordValid) onNext() else focus.clearFocus() }),
                    trailing = {
                        ObCheck(matches)
                        EyeToggle(showConfirm) { showConfirm = !showConfirm }
                    }
                )
                Row(Modifier.padding(start = 2.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ObCheck(matches, size = 18.dp)
                    Text(
                        text = if (mismatch) "Passwords don’t match yet" else "Passwords match",
                        color = when { mismatch -> ObColors.Danger; matches -> Color.White; else -> ObColors.TextFaint },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }
        ObPrimaryButton(
            text = "Next: Privacy",
            onClick = onNext,
            enabled = state.passwordValid,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )
    }
}

@Composable
private fun FieldLabel(text: String) = Text(text, color = ObColors.TextMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

@Composable
private fun EyeToggle(visible: Boolean, onToggle: () -> Unit) {
    Icon(
        imageVector = if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
        contentDescription = null,
        tint = Color(0xB3FFFFFF),
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onToggle)
            .padding(7.dp)
            .semantics { contentDescription = if (visible) "Hide password" else "Show password" }
    )
}

@Composable
private fun RuleRow(met: Boolean, text: String) {
    val color by animateColorAsState(if (met) Color.White else ObColors.TextFaint, label = "rule")
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ObCheck(met, size = 18.dp)
        Text(text, color = color, fontSize = 13.sp, fontWeight = if (met) FontWeight.SemiBold else FontWeight.Medium)
    }
}

@Composable
private fun StrengthMeter(met: Int, started: Boolean) {
    val (label, color) = when {
        !started -> "" to ObColors.TextFaint
        met <= 2 -> "Weak" to ObColors.Danger
        met == 3 -> "Fair" to ObColors.Amber
        met == 4 -> "Good" to ObColors.Cyan
        else -> "Strong" to ObColors.Lime
    }
    val filled = if (!started) 0 else when { met <= 2 -> 1; met == 3 -> 2; met == 4 -> 3; else -> 4 }
    Row(Modifier.fillMaxWidth().padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(4) { i ->
            val c by animateColorAsState(if (i < filled) color else Color(0x338CAFFF), label = "seg")
            Box(Modifier.weight(1f).height(4.dp).clip(CircleShape).background(c))
        }
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.widthIn(min = 44.dp))
    }
}
