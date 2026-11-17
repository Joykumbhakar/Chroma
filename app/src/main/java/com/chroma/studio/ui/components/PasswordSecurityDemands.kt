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
    val emeraldBrush = remember { Brush.linearGradient(StudioDesignTokens.EmeraldMintGradient) }
    val transition = updateTransition(targetState = met, label = "checkTransition")

    val scale by transition.animateFloat(
        transitionSpec = { spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium) },
        label = "scale"
    ) { if (it) 1f else 0.75f }

    val bgModifier = if (met) {
        Modifier.background(emeraldBrush)
    } else {
        Modifier.background(Color(0xFF141724))
    }

    val borderModifier = if (met) {
        Modifier.border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.5f), Color(0xFF059669))), CircleShape)
    } else {
        Modifier.border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.15f),
                    Color.White.copy(alpha = 0.04f)
                )
            ),
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
                    .background(Color.White.copy(alpha = 0.20f))
            )
        }
    }
}

// ============================================================================
// INDIVIDUAL DEMAND ROW WITH MICRO-BEVEL & DYNAMIC CHECKMARK
// ============================================================================

@Composable
fun PasswordDemandItemRow(
    met: Boolean,
    title: String,
    modifier: Modifier = Modifier
) {
    val textColor by animateColorAsState(
        targetValue = if (met) Color.White else Color(0xFF8F9BB3),
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
                    .background(Color(0xFF059669).copy(alpha = 0.18f))
                    .border(0.5.dp, Color(0xFF10B981).copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "PASSED",
                    color = Color(0xFF34D399),
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF10131E))
            .studioBevel(cornerRadius = 14.dp, borderWidth = 1.dp)
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
                    tint = if (demands.allMet) Color(0xFF10B981) else Color(0xFF0284C7),
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "SECURITY REQUIREMENTS",
                    color = Color(0xFFB0B9CF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )
            }

            // Met counter badge
            val badgeBrush = if (demands.allMet) {
                Brush.linearGradient(StudioDesignTokens.EmeraldMintGradient)
            } else {
                Brush.linearGradient(listOf(Color(0xFF1E2438), Color(0xFF1E2438)))
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeBrush)
                    .border(
                        0.5.dp,
                        if (demands.allMet) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (demands.allMet) "All 5 Met" else "${demands.metCount} / 5 Met",
                    color = Color.White,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Multi-segment Beveled Progress Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val segmentGradient = when {
                demands.metCount <= 2 -> listOf(Color(0xFFE11D48), Color(0xFFFB7185)) // Crimson
                demands.metCount <= 4 -> listOf(Color(0xFF0284C7), Color(0xFF06B6D4)) // Cobalt
                else -> StudioDesignTokens.EmeraldMintGradient // Emerald Mint
            }
            repeat(5) { index ->
                val isSegmentMet = index < demands.metCount
                val segmentBrush = if (isSegmentMet) {
                    Brush.horizontalGradient(segmentGradient)
                } else {
                    SolidColor(Color(0xFF1D2235))
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(segmentBrush)
                        .border(
                            0.5.dp,
                            if (isSegmentMet) Color.White.copy(alpha = 0.25f) else Color.Transparent,
                            CircleShape
                        )
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
// HIGH-PERFORMANCE BEVELED STUDIO PASSWORD INPUT FIELD
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
    var showPassword by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    val activeBorderBrush = when {
        isError -> SolidColor(Color(0xFFFF453A))
        allDemandsMet -> Brush.linearGradient(StudioDesignTokens.EmeraldMintGradient)
        isFocused -> Brush.linearGradient(StudioDesignTokens.CobaltGradient)
        else -> null
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(Color(0xFF0F121C))
            .studioBevel(
                cornerRadius = 13.dp,
                borderWidth = if (isFocused || allDemandsMet || isError) 1.2.dp else 1.dp,
                accentBorder = activeBorderBrush
            )
            .padding(horizontal = 14.dp, vertical = 13.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = if (allDemandsMet) Lucide.ShieldCheck else Lucide.Lock,
                contentDescription = null,
                tint = when {
                    allDemandsMet -> Color(0xFF10B981)
                    isFocused || value.isNotEmpty() -> Color(0xFF0284C7)
                    else -> Color(0xFF6B7280)
                },
                modifier = Modifier.size(17.dp)
            )

            Spacer(Modifier.width(10.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = Color(0xFF555E75),
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif
                    ),
                    cursorBrush = SolidColor(if (allDemandsMet) Color(0xFF10B981) else Color(0xFF0284C7)),
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
                            tint = Color(0xFF8F9BB3),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// HIGH-PERFORMANCE BEVELED CONFIRM PASSWORD FIELD
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
    var showPassword by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    val matches = value.isNotEmpty() && value == targetPassword && allDemandsMet
    val isMismatch = value.isNotEmpty() && (!matches && (value.length >= targetPassword.length || !targetPassword.startsWith(value)))

    val activeBorderBrush = when {
        matches -> Brush.linearGradient(StudioDesignTokens.EmeraldMintGradient)
        isMismatch -> SolidColor(Color(0xFFFF453A))
        isFocused -> Brush.linearGradient(StudioDesignTokens.CobaltGradient)
        else -> null
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(13.dp))
                .background(Color(0xFF0F121C))
                .studioBevel(
                    cornerRadius = 13.dp,
                    borderWidth = if (isFocused || matches || isMismatch) 1.2.dp else 1.dp,
                    accentBorder = activeBorderBrush
                )
                .padding(horizontal = 14.dp, vertical = 13.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (matches) Lucide.BadgeCheck else Lucide.Lock,
                    contentDescription = null,
                    tint = when {
                        matches -> Color(0xFF10B981)
                        isMismatch -> Color(0xFFFF453A)
                        isFocused || value.isNotEmpty() -> Color(0xFF0284C7)
                        else -> Color(0xFF6B7280)
                    },
                    modifier = Modifier.size(17.dp)
                )

                Spacer(Modifier.width(10.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = Color(0xFF555E75),
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.SansSerif
                        ),
                        cursorBrush = SolidColor(if (matches) Color(0xFF10B981) else Color(0xFF0284C7)),
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
                                tint = Color(0xFF8F9BB3),
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
                        matches -> Color(0xFF34D399)
                        isMismatch -> Color(0xFFFF453A)
                        else -> Color(0xFF94A3B8)
                    },
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
