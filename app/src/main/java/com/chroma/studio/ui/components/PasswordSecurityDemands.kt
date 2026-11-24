package com.chroma.studio.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.*

// ============================================================================
// PASSWORD SECURITY DEMANDS EVALUATOR & DATA MODELS
// ============================================================================

/**
 * Encapsulates the 5 strict password security demands:
 * 1. Minimum 8 characters
 * 2. Capital letters (A–Z)
 * 3. Small letters (a–z)
 * 4. Numbers (0–9)
 * 5. Symbols / special characters (!@#$%^&*...)
 */
data class PasswordDemandsState(
    val hasMinLength: Boolean = false,
    val hasCapital: Boolean = false,
    val hasSmall: Boolean = false,
    val hasNumber: Boolean = false,
    val hasSymbol: Boolean = false
) {
    val metCount: Int get() = listOf(hasMinLength, hasCapital, hasSmall, hasNumber, hasSymbol).count { it }
    val allMet: Boolean get() = metCount == 5
}

fun evaluatePasswordDemands(pw: String): PasswordDemandsState = PasswordDemandsState(
    hasMinLength = pw.length >= 8,
    hasCapital = pw.any { it.isUpperCase() },
    hasSmall = pw.any { it.isLowerCase() },
    hasNumber = pw.any { it.isDigit() },
    hasSymbol = pw.any { !it.isLetterOrDigit() }
)

// ============================================================================
// SLEEK BEVELED CHECKMARK BADGE (TACTILE & FEATHERLIGHT)
// ============================================================================

@Composable
fun BeveledCheckMark(
    met: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp
) {
    val isDark = com.chroma.studio.ui.theme.LocalOscellionColors.current.isDark
    val brandBlueBrush = remember { Brush.linearGradient(listOf(Color(0xFF0381FE), Color(0xFF3E91FF))) }
    val transition = updateTransition(targetState = met, label = "checkTransition")

    val scale by transition.animateFloat(
        transitionSpec = { spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium) },
        label = "scale"
    ) { if (it) 1f else 0.75f }

    val bgModifier = if (met) {
        Modifier.background(brandBlueBrush)
    } else {
        Modifier.background(if (isDark) Color(0xFF222634) else Color(0xFFE2E8F0))
    }

    val borderModifier = if (met) {
        Modifier.border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.5f), Color(0xFF0072DE))), CircleShape)
    } else {
        Modifier.border(
            width = 0.8.dp,
            color = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
            shape = CircleShape
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(bgModifier)
            .then(borderModifier),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = met,
            enter = scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Icon(
                imageVector = Lucide.Check,
                contentDescription = "Satisfied",
                tint = Color.White,
                modifier = Modifier.size(size * 0.65f)
            )
        }
        if (!met) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.25f))
            )
        }
    }
}

// ============================================================================
// INDIVIDUAL DEMAND ROW WITH DYNAMIC THEME SUPPORT
// ============================================================================

@Composable
fun PasswordDemandItemRow(
    met: Boolean,
    title: String,
    modifier: Modifier = Modifier
) {
    val isDark = com.chroma.studio.ui.theme.LocalOscellionColors.current.isDark
    val textColor by animateColorAsState(
        targetValue = if (met) {
            if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
        } else {
            if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
        },
        animationSpec = tween(180),
        label = "textColor"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BeveledCheckMark(met = met, size = 18.dp)

        Text(
            text = title,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (met) FontWeight.SemiBold else FontWeight.Normal,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.weight(1f)
        )

        AnimatedVisibility(
            visible = met,
            enter = fadeIn(tween(150)) + scaleIn(tween(150)),
            exit = fadeOut(tween(100))
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isDark) Color(0xFF0381FE).copy(alpha = 0.18f) else Color(0xFFE0F2FE))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "PASSED",
                    color = Color(0xFF0381FE),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp
                )
            }
        }
    }
}

// ============================================================================
// COMPLETE PASSWORD SECURITY REQUIREMENTS CHECKLIST CARD
// ============================================================================

@Composable
fun PasswordRequirementChecklist(
    password: String,
    modifier: Modifier = Modifier,
    alwaysVisible: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    val isDark = com.chroma.studio.ui.theme.LocalOscellionColors.current.isDark
    val demands = remember(password) { evaluatePasswordDemands(password) }

    // Haptic pulse when a new demand gets ticked off
    var lastMetCount by remember { mutableIntStateOf(demands.metCount) }
    LaunchedEffect(demands.metCount) {
        if (demands.metCount > lastMetCount) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        lastMetCount = demands.metCount
    }

    if (!alwaysVisible && password.isEmpty()) return

    val cardBg = if (isDark) Color(0xFF17171A) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0x1FFFFFFF) else Color(0x14000000)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(0.5.dp, cardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        // ── Header & Progress Bar ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Lucide.ShieldCheck,
                    contentDescription = null,
                    tint = if (demands.allMet) Color(0xFF00C853) else Color(0xFF0381FE),
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "SECURITY REQUIREMENTS",
                    color = if (isDark) Color(0xFFB0B9CF) else Color(0xFF475569),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )
            }

            // Met counter badge
            val badgeBrush = if (demands.allMet) {
                Brush.linearGradient(listOf(Color(0xFF0381FE), Color(0xFF3E91FF)))
            } else {
                SolidColor(if (isDark) Color(0xFF222634) else Color(0xFFE2E8F0))
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeBrush)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (demands.allMet) "All 5 Met" else "${demands.metCount} / 5 Met",
                    color = if (demands.allMet) Color.White else if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Multi-segment Progress Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val segmentGradient = when {
                demands.metCount <= 2 -> listOf(Color(0xFFEF4444), Color(0xFFF87171)) // Crimson
                demands.metCount <= 4 -> listOf(Color(0xFF0381FE), Color(0xFF3E91FF)) // Brand Blue
                else -> listOf(Color(0xFF00C853), Color(0xFF69F0AE)) // Emerald
            }
            repeat(5) { index ->
                val isSegmentMet = index < demands.metCount
                val segmentBrush = if (isSegmentMet) {
                    Brush.horizontalGradient(segmentGradient)
                } else {
                    SolidColor(if (isDark) Color(0xFF222634) else Color(0xFFE2E8F0))
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(segmentBrush)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── 5 Demands Checklist ──
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            PasswordDemandItemRow(
                met = demands.hasMinLength,
                title = "Minimum 8 characters"
            )
            PasswordDemandItemRow(
                met = demands.hasCapital,
                title = "Capital letters (A–Z)"
            )
            PasswordDemandItemRow(
                met = demands.hasSmall,
                title = "Small letters (a–z)"
            )
            PasswordDemandItemRow(
                met = demands.hasNumber,
                title = "Numbers (0–9)"
            )
            PasswordDemandItemRow(
                met = demands.hasSymbol,
                title = "Symbols / special characters (!@#$%^&*...)"
            )
        }
    }
}

// ============================================================================
// HIGH-PERFORMANCE STUDIO PASSWORD INPUT FIELD
// ============================================================================

@Composable
fun StudioBeveledPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "Enter studio password",
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    allDemandsMet: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {}
) {
    val isDark = com.chroma.studio.ui.theme.LocalOscellionColors.current.isDark
    var showPassword by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    val fieldBg = if (isDark) Color(0xFF222226) else Color(0xFFF2F4F7)
    val fieldBorder = if (isDark) Color(0x1FFFFFFF) else Color(0x14000000)
    val textPrimary = if (isDark) Color(0xFFFAFAFA) else Color(0xFF000000)
    val textMuted = if (isDark) Color(0xFF8E8E93) else Color(0xFF71717A)

    val activeBorderColor = when {
        isError -> Color(0xFFFF453A)
        allDemandsMet -> Color(0xFF00C853)
        isFocused -> Color(0xFF0381FE)
        else -> fieldBorder
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(fieldBg)
            .border(
                width = if (isFocused || allDemandsMet || isError) 1.dp else 0.5.dp,
                color = activeBorderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = if (allDemandsMet) Lucide.ShieldCheck else Lucide.Lock,
                contentDescription = null,
                tint = when {
                    allDemandsMet -> Color(0xFF00C853)
                    isFocused || value.isNotEmpty() -> Color(0xFF0381FE)
                    else -> textMuted
                },
                modifier = Modifier.size(17.dp)
            )

            Spacer(Modifier.width(10.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = textMuted,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(
                        color = textPrimary,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif
                    ),
                    cursorBrush = SolidColor(if (allDemandsMet) Color(0xFF00C853) else Color(0xFF0381FE)),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = imeAction,
                        autoCorrect = false
                    ),
                    keyboardActions = KeyboardActions(onAny = { onImeAction() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused }
                )
            }

            // Trailing actions: Demand checkmark indicator + Show/Hide eye button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (value.isNotEmpty()) {
                    BeveledCheckMark(
                        met = allDemandsMet,
                        size = 18.dp
                    )
                    IconButton(
                        onClick = { showPassword = !showPassword },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (showPassword) Lucide.EyeOff else Lucide.Eye,
                            contentDescription = if (showPassword) "Hide password" else "Show password",
                            tint = textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// HIGH-PERFORMANCE CONFIRM PASSWORD FIELD
// ============================================================================

@Composable
fun StudioBeveledConfirmPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    targetPassword: String,
    allDemandsMet: Boolean,
    placeholder: String = "Confirm studio password",
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {}
) {
    val isDark = com.chroma.studio.ui.theme.LocalOscellionColors.current.isDark
    var showPassword by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    val fieldBg = if (isDark) Color(0xFF222226) else Color(0xFFF2F4F7)
    val fieldBorder = if (isDark) Color(0x1FFFFFFF) else Color(0x14000000)
    val textPrimary = if (isDark) Color(0xFFFAFAFA) else Color(0xFF000000)
    val textMuted = if (isDark) Color(0xFF8E8E93) else Color(0xFF71717A)

    val matches = value.isNotEmpty() && value == targetPassword && allDemandsMet
    val isMismatch = value.isNotEmpty() && (!matches && (value.length >= targetPassword.length || !targetPassword.startsWith(value)))

    val activeBorderColor = when {
        matches -> Color(0xFF00C853)
        isMismatch -> Color(0xFFFF453A)
        isFocused -> Color(0xFF0381FE)
        else -> fieldBorder
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(fieldBg)
                .border(
                    width = if (isFocused || matches || isMismatch) 1.dp else 0.5.dp,
                    color = activeBorderColor,
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (matches) Lucide.BadgeCheck else Lucide.Lock,
                    contentDescription = null,
                    tint = when {
                        matches -> Color(0xFF00C853)
                        isMismatch -> Color(0xFFFF453A)
                        isFocused || value.isNotEmpty() -> Color(0xFF0381FE)
                        else -> textMuted
                    },
                    modifier = Modifier.size(17.dp)
                )

                Spacer(Modifier.width(10.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = textMuted,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        textStyle = TextStyle(
                            color = textPrimary,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.SansSerif
                        ),
                        cursorBrush = SolidColor(if (matches) Color(0xFF00C853) else Color(0xFF0381FE)),
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = imeAction,
                            autoCorrect = false
                        ),
                        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isFocused = it.isFocused }
                    )
                }

                // Trailing actions: Match checkmark indicator + Show/Hide eye button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (value.isNotEmpty()) {
                        BeveledCheckMark(
                            met = matches,
                            size = 18.dp
                        )
                        IconButton(
                            onClick = { showPassword = !showPassword },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (showPassword) Lucide.EyeOff else Lucide.Eye,
                                contentDescription = if (showPassword) "Hide password" else "Show password",
                                tint = textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Live validation feedback micro-row
        if (value.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.padding(start = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BeveledCheckMark(met = matches, size = 14.dp)
                Text(
                    text = when {
                        matches -> "Passwords match securely"
                        !allDemandsMet -> "Complete all 5 security requirements first"
                        else -> "Passwords do not match yet"
                    },
                    color = when {
                        matches -> Color(0xFF00C853)
                        isMismatch -> Color(0xFFFF453A)
                        else -> textMuted
                    },
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
