package com.chroma.studio.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.chroma.studio.data.ProBadgeCatalog
import com.chroma.studio.data.ProBadgePriceTier
import com.chroma.studio.data.ProBadgeTier
import com.chroma.studio.data.ProBenefitsList
import com.chroma.studio.data.ProRepository
import com.chroma.studio.data.ProVerificationRequest
import com.chroma.studio.data.resolveProBadgePricing
import com.chroma.studio.model.UserProfile
import com.chroma.studio.ui.components.IosAvatarView
import com.chroma.studio.ui.components.ToastManager
import com.chroma.studio.ui.components.VerifiedBadge
import com.chroma.studio.ui.components.resolveVerifiedBadgeStyle
import com.chroma.studio.ui.theme.InterFontFamily
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UpgradeToProScreen(
    onBack: () -> Unit,
    onNavigateToProfile: () -> Unit = {}
) {
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val activeUid = currentUser?.uid ?: com.chroma.studio.data.SavedAccountManager.getActiveAccount(context)?.uid.orEmpty()

    val profile by remember(activeUid) {
        ProRepository.observeUserProfile(activeUid)
    }.collectAsState(initial = null)

    val requests by remember(activeUid) {
        ProRepository.observeUserProRequests(activeUid)
    }.collectAsState(initial = emptyList())

    // Show the most recent request unless the user explicitly taps "Apply / Upgrade Another"
    var forceFormMode by remember { mutableStateOf(false) }
    val latestRequest = requests.firstOrNull()
    val showTracker = !forceFormMode && latestRequest != null

    val surfaceBg = if (isDark) Color(0xFF0A0B0F) else Color(0xFFF4F6FA)
    val cardBg = if (isDark) Color(0xFF131620) else Color.White
    val borderColor = if (isDark) Color(0xFF232838) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    // Tick every 30s so 7-day SLA countdown stays fresh
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            nowMs = System.currentTimeMillis()
        }
    }

    Scaffold(
        containerColor = surfaceBg,
        topBar = {
            Surface(
                color = if (isDark) Color(0xFF0E1017) else Color.White,
                shadowElevation = 0.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = {
                                if (forceFormMode && latestRequest != null) {
                                    forceFormMode = false
                                } else {
                                    onBack()
                                }
                            }) {
                                Icon(
                                    imageVector = Lucide.ArrowLeft,
                                    contentDescription = "Back",
                                    tint = textPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Oscellion Pro",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary,
                                        fontFamily = InterFontFamily
                                    )
                                    VerifiedBadge(size = 16.dp, tone = "gold", animated = true)
                                }
                                Text(
                                    text = if (showTracker) "Application Progress & Verification" else "Verified Badge & Custom Colors",
                                    fontSize = 12.sp,
                                    color = textSecondary,
                                    fontFamily = InterFontFamily
                                )
                            }
                        }

                        if (latestRequest != null) {
                            TextButton(
                                onClick = { forceFormMode = !forceFormMode }
                            ) {
                                Text(
                                    text = if (forceFormMode) "View Progress" else "New Request",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF6366F1)
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = borderColor, thickness = 0.5.dp)
                }
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = showTracker,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            label = "UpgradeToProModeSwitch"
        ) { isTracker ->
            if (isTracker && latestRequest != null) {
                ProRequestProgressContent(
                    request = latestRequest,
                    allRequests = requests,
                    profile = profile,
                    nowMs = nowMs,
                    isDark = isDark,
                    cardBg = cardBg,
                    borderColor = borderColor,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onCopy = { label, text ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
                        ToastManager.showToast("$label copied")
                    },
                    onOpenPaymentUrl = { url ->
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            ToastManager.showToast("Could not open link cleanly; copy payload instead")
                        }
                    },
                    onSubmitPaymentProof = { requestId, txRef ->
                        scope.launch {
                            val res = ProRepository.submitPaymentProof(requestId, txRef)
                            res.onSuccess {
                                ToastManager.showToast("Payment confirmation sent! Verification activates within 7 days.")
                            }.onFailure { err ->
                                ToastManager.showToast(err.message ?: "Failed to submit payment confirmation")
                            }
                        }
                    },
                    onStartNewRequest = { forceFormMode = true },
                    onNavigateToProfile = onNavigateToProfile
                )
            } else {
                ProCheckoutFormContent(
                    uid = activeUid,
                    profile = profile,
                    defaultEmail = currentUser?.email.orEmpty(),
                    isDark = isDark,
                    cardBg = cardBg,
                    borderColor = borderColor,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onSubmitted = {
                        forceFormMode = false
                        ToastManager.showToast("Pro application submitted! Track progress live right here.")
                    }
                )
            }
        }
    }
}

@Composable
private fun ProCheckoutFormContent(
    uid: String,
    profile: UserProfile?,
    defaultEmail: String,
    isDark: Boolean,
    cardBg: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    onSubmitted: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var selectedTone by remember { mutableStateOf("gold") }
    val selectedPricing = remember(selectedTone) { resolveProBadgePricing(selectedTone) }
    var useInrDisplay by remember { mutableStateOf(false) }

    var fullName by remember(profile?.displayName) { mutableStateOf(profile?.displayName.orEmpty()) }
    var contactEmail by remember(profile?.email, profile?.customEmail, defaultEmail) {
        mutableStateOf(
            listOf(profile?.customEmail, profile?.email, defaultEmail)
                .firstOrNull { !it.isNullOrBlank() }
                .orEmpty()
        )
    }
    var category by remember(profile?.role) { mutableStateOf(profile?.role?.ifBlank { "Creator" } ?: "Creator") }
    var portfolioUrl by remember(profile?.socialLinks) {
        mutableStateOf(
            profile?.socialLinks?.values?.firstOrNull { it.isNotBlank() }.orEmpty()
        )
    }
    var reason by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val luxuryBadges = remember { ProBadgeCatalog.filter { it.tier == ProBadgeTier.LUXURY } }
    val signatureBadges = remember { ProBadgeCatalog.filter { it.tier == ProBadgeTier.SIGNATURE } }
    val standardBadges = remember { ProBadgeCatalog.filter { it.tier == ProBadgeTier.STANDARD } }
    val quickCategories = remember { listOf("Creator", "Designer", "Developer", "Artist", "Studio", "Public Figure", "Educator") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Live Identity & Badge Preview Hero
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.Transparent,
                border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = if (isDark) {
                                    listOf(Color(0xFF1A0E33), Color(0xFF12182B), Color(0xFF0F1320))
                                } else {
                                    listOf(Color(0xFFF3ECFF), Color(0xFFEEF2FF), Color.White)
                                }
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = Color(0xFF7C3AED).copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(Lucide.Sparkles, null, tint = Color(0xFFA855F7), modifier = Modifier.size(13.dp))
                                    Text(
                                        text = "LIVE BADGE PREVIEW",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFD8B4FE) else Color(0xFF6D28D9),
                                        letterSpacing = 0.8.sp
                                    )
                                }
                            }

                            // Currency toggle pill
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0).copy(alpha = 0.7f),
                                modifier = Modifier.clickable { useInrDisplay = !useInrDisplay }
                            ) {
                                Text(
                                    text = if (useInrDisplay) "INR ₹  ⇄ USD" else "USD $  ⇄ INR",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        // Profile Preview Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            IosAvatarView(
                                name = fullName.ifBlank { profile?.displayName ?: "Creator" },
                                avatarUrl = profile?.avatarUrl.orEmpty(),
                                size = 54.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = fullName.ifBlank { profile?.displayName?.ifBlank { "Your Name" } ?: "Your Name" },
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    VerifiedBadge(
                                        size = 19.dp,
                                        tone = selectedPricing.key,
                                        animated = true,
                                        showGlow = true
                                    )
                                }
                                Text(
                                    text = "@${profile?.username?.ifBlank { "creator" } ?: "creator"} · ${selectedPricing.label}",
                                    fontSize = 12.sp,
                                    color = textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (useInrDisplay) "₹${selectedPricing.priceInr}" else "$${selectedPricing.priceUsd}",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (selectedPricing.tier == ProBadgeTier.LUXURY) Color(0xFFF59E0B) else Color(0xFF3B82F6)
                                )
                                Text(
                                    text = selectedPricing.tier.label.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textSecondary
                                )
                            }
                        }

                        Text(
                            text = selectedPricing.tagline,
                            fontSize = 12.sp,
                            color = textSecondary
                        )

                        if (profile?.verified == true) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    VerifiedBadge(size = 15.dp, tone = profile.verifiedTone ?: "blue")
                                    Text(
                                        text = "Your account already has an active verified badge (${profile.verifiedTone ?: "blue"}). You can still apply here to upgrade to a Luxury / Custom badge color.",
                                        fontSize = 11.5.sp,
                                        color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF065F46),
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Luxury Tier Badges ($24 - $29)
        item {
            BadgeTierSectionCard(
                title = "Luxury & Metallic Editions",
                subtitle = "Golden & luxury shimmer badges — highest prestige tier",
                priceBadge = if (useInrDisplay) "₹1,999 – ₹2,499" else "$24 – $29",
                accentColor = Color(0xFFF59E0B),
                badges = luxuryBadges,
                selectedTone = selectedTone,
                useInrDisplay = useInrDisplay,
                cardBg = cardBg,
                borderColor = borderColor,
                isDark = isDark,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                onSelect = { selectedTone = it.key }
            )
        }

        // 3. Signature Studio Colors ($15)
        item {
            BadgeTierSectionCard(
                title = "Signature Studio Palette",
                subtitle = "Distinctive creator & studio specialty finishes",
                priceBadge = if (useInrDisplay) "₹1,299" else "$15",
                accentColor = Color(0xFF8B5CF6),
                badges = signatureBadges,
                selectedTone = selectedTone,
                useInrDisplay = useInrDisplay,
                cardBg = cardBg,
                borderColor = borderColor,
                isDark = isDark,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                onSelect = { selectedTone = it.key }
            )
        }

        // 4. Standard Verified Colors ($9)
        item {
            BadgeTierSectionCard(
                title = "Standard Verified Colors",
                subtitle = "Essential verified seal colors across Oscellion",
                priceBadge = if (useInrDisplay) "₹799" else "$9",
                accentColor = Color(0xFF3B82F6),
                badges = standardBadges,
                selectedTone = selectedTone,
                useInrDisplay = useInrDisplay,
                cardBg = cardBg,
                borderColor = borderColor,
                isDark = isDark,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                onSelect = { selectedTone = it.key }
            )
        }

        // 5. Pro Benefits & 7-Day SLA Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardBg,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Lucide.ShieldCheck, null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                        Text(
                            text = "What You Unlock + How Checkout Works",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }
                    ProBenefitsList.forEach { benefit ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Lucide.Check, null, tint = Color(0xFF10B981), modifier = Modifier.size(15.dp).padding(top = 2.dp))
                            Text(text = benefit, fontSize = 12.5.sp, color = textSecondary, lineHeight = 18.sp)
                        }
                    }
                    HorizontalDivider(color = borderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 2.dp))
                    Text(
                        text = "Workflow: Submit Form → Admin Reviews → Payment QR Sent to Your Email & App → Confirm Payment → Activated Within 7 Days.",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4338CA),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // 6. Application & Checkout Form
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = cardBg,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Pro Verification & Badge Form",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it; errorMsg = null },
                        label = { Text("Full Name / Brand Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = contactEmail,
                        onValueChange = { contactEmail = it; errorMsg = null },
                        label = { Text("Email Address (We send Payment QR here) *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Text(
                        text = "Creator Category",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textSecondary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickCategories.forEach { cat ->
                            val selected = category.equals(cat, ignoreCase = true)
                            FilterChip(
                                selected = selected,
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 12.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = portfolioUrl,
                        onValueChange = { portfolioUrl = it },
                        label = { Text("Website / Portfolio / Social Link (Optional)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Note for Admin Reviewer (Optional)") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    if (!errorMsg.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = errorMsg.orEmpty(),
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth().padding(10.dp)
                            )
                        }
                    }

                    // Checkout Summary Box
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDark) Color(0xFF1A1F2E) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, borderColor)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                VerifiedBadge(size = 20.dp, tone = selectedPricing.key, animated = true)
                                Column {
                                    Text(
                                        text = selectedPricing.label,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary
                                    )
                                    Text(
                                        text = "${selectedPricing.tier.label} Tier · Activates within 7 days of payment",
                                        fontSize = 11.sp,
                                        color = textSecondary
                                    )
                                }
                            }
                            Text(
                                text = "$${selectedPricing.priceUsd} / ₹${selectedPricing.priceInr}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (uid.isBlank()) {
                                errorMsg = "Please sign in first."
                                return@Button
                            }
                            if (fullName.trim().length < 2) {
                                errorMsg = "Please enter your full name or brand name."
                                return@Button
                            }
                            val emailTrim = contactEmail.trim()
                            if (!emailTrim.contains("@") || !emailTrim.contains(".")) {
                                errorMsg = "Please enter a valid email address so we can email your Payment QR."
                                return@Button
                            }
                            errorMsg = null
                            isSubmitting = true
                            scope.launch {
                                val res = ProRepository.submitProRequest(
                                    uid = uid,
                                    profile = profile,
                                    badgeTier = selectedPricing,
                                    fullName = fullName,
                                    contactEmail = emailTrim,
                                    category = category,
                                    portfolioUrl = portfolioUrl,
                                    reason = reason
                                )
                                isSubmitting = false
                                res.onSuccess { onSubmitted() }
                                    .onFailure { err ->
                                        errorMsg = err.message ?: "Could not submit request. Please try again."
                                    }
                            }
                        },
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6366F1),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(Lucide.Send, null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Submit Pro Request · $${selectedPricing.priceUsd}",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun BadgeTierSectionCard(
    title: String,
    subtitle: String,
    priceBadge: String,
    accentColor: Color,
    badges: List<ProBadgePriceTier>,
    selectedTone: String,
    useInrDisplay: Boolean,
    cardBg: Color,
    borderColor: Color,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    onSelect: (ProBadgePriceTier) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.5.sp,
                        color = textSecondary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = accentColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = priceBadge,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // 2-column grid of badges
            badges.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { badge ->
                        val isSelected = selectedTone.equals(badge.key, ignoreCase = true)
                        val style = remember(badge.key) { resolveVerifiedBadgeStyle(tone = badge.key) }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) {
                                accentColor.copy(alpha = if (isDark) 0.18f else 0.10f)
                            } else {
                                if (isDark) Color(0xFF1A1F2E) else Color(0xFFF8FAFC)
                            },
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) accentColor else borderColor
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onSelect(badge) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                VerifiedBadge(
                                    size = 20.dp,
                                    tone = badge.key,
                                    animated = isSelected || style.autoShimmer,
                                    showGlow = isSelected
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = badge.label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (useInrDisplay) "₹${badge.priceInr}" else "$${badge.priceUsd}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor
                                    )
                                }
                            }
                        }
                    }
                    if (pair.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProRequestProgressContent(
    request: ProVerificationRequest,
    allRequests: List<ProVerificationRequest>,
    profile: UserProfile?,
    nowMs: Long,
    isDark: Boolean,
    cardBg: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    onCopy: (String, String) -> Unit,
    onOpenPaymentUrl: (String) -> Unit,
    onSubmitPaymentProof: (String, String) -> Unit,
    onStartNewRequest: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val context = LocalContext.current
    var paymentProofInput by remember(request.id) { mutableStateOf(request.paymentProofRef.orEmpty()) }
    var proofError by remember { mutableStateOf<String?>(null) }

    val statusIndex = when (request.status) {
        "submitted" -> 0
        "under_review" -> 1
        "payment_sent" -> 2
        "payment_submitted" -> 3
        "activated" -> 4
        else -> 1
    }
    val isRejected = request.status == "rejected"
    val isActivated = request.status == "activated"

    val formatTime = remember {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Live Status Banner Card
        item {
            val statusColor = when (request.status) {
                "submitted" -> Color(0xFF3B82F6)
                "under_review" -> Color(0xFFF59E0B)
                "payment_sent" -> Color(0xFF8B5CF6)
                "payment_submitted" -> Color(0xFF06B6D4)
                "activated" -> Color(0xFF10B981)
                "rejected" -> Color(0xFFEF4444)
                else -> Color(0xFF6366F1)
            }
            val statusHeadline = when (request.status) {
                "submitted" -> "Application Received — Queued for Review"
                "under_review" -> "Admin is Reviewing Your Request"
                "payment_sent" -> "Payment Email & QR Code Ready!"
                "payment_submitted" -> "Payment Received — Activating Within 7 Days"
                "activated" -> "Pro & Verified Badge Activated!"
                "rejected" -> "Request Needs Attention / Closed"
                else -> "Pro Application"
            }

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = cardBg,
                border = BorderStroke(1.5.dp, statusColor.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            IosAvatarView(
                                name = request.displayName.ifBlank { request.fullName },
                                avatarUrl = profile?.avatarUrl ?: request.avatarUrl.orEmpty(),
                                size = 46.dp
                            )
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(
                                        text = request.badgeLabel,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary
                                    )
                                    VerifiedBadge(
                                        size = 18.dp,
                                        tone = request.badgeTone,
                                        animated = true,
                                        showGlow = isActivated
                                    )
                                }
                                Text(
                                    text = "${request.badgeTier} Tier · $${request.priceUsd} (₹${request.priceInr})",
                                    fontSize = 12.sp,
                                    color = textSecondary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = statusColor.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = request.status.replace("_", " ").uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Text(
                        text = statusHeadline,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )

                    if (!request.adminNote.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0xFF1A1F2E) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, borderColor)
                        ) {
                            Text(
                                text = "Admin Note: ${request.adminNote}",
                                fontSize = 12.sp,
                                color = textPrimary,
                                modifier = Modifier.fillMaxWidth().padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Payment QR Card & Confirmation Form (Visible whenever payment_sent or payment_submitted+)
        if (request.status == "payment_sent" || (!request.paymentQrImageUrl.isNullOrBlank() && !isActivated && !isRejected)) {
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = cardBg,
                    border = BorderStroke(1.5.dp, Color(0xFF8B5CF6).copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Lucide.QrCode, null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(19.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Scan Payment QR & Complete Checkout",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                Text(
                                    text = "Also sent to ${request.emailSentTo ?: request.contactEmail}",
                                    fontSize = 11.5.sp,
                                    color = textSecondary
                                )
                            }
                        }

                        if (!request.paymentQrImageUrl.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.size(210.dp)
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(request.paymentQrImageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Pro Upgrade Payment QR",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDark) Color(0xFF1A1F2E) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, borderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Amount: ${request.paymentAmountLabel ?: "$${request.priceUsd} USD (₹${request.priceInr})"}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                if (!request.paymentReference.isNullOrBlank()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Invoice Ref: ${request.paymentReference}",
                                            fontSize = 12.sp,
                                            color = textSecondary
                                        )
                                        TextButton(
                                            onClick = { onCopy("Invoice Reference", request.paymentReference) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                        ) {
                                            Icon(Lucide.Copy, null, modifier = Modifier.size(13.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Copy", fontSize = 11.sp)
                                        }
                                    }
                                }
                                if (!request.paymentInstructions.isNullOrBlank()) {
                                    Text(
                                        text = request.paymentInstructions,
                                        fontSize = 12.sp,
                                        color = textSecondary,
                                        lineHeight = 17.sp
                                    )
                                }
                                if (!request.paymentQrPayload.isNullOrBlank()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { onCopy("Payment QR / UPI Link", request.paymentQrPayload) },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Lucide.Copy, null, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Copy Payment Link", fontSize = 12.sp)
                                        }
                                        if (request.paymentQrPayload.startsWith("http", ignoreCase = true) ||
                                            request.paymentQrPayload.startsWith("upi://", ignoreCase = true)
                                        ) {
                                            OutlinedButton(
                                                onClick = { onOpenPaymentUrl(request.paymentQrPayload) },
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Lucide.ExternalLink, null, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Pay App", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (request.status == "payment_sent") {
                            HorizontalDivider(color = borderColor, thickness = 0.5.dp)
                            Text(
                                text = "Completed payment? Enter your Transaction ID / UTR below to start 7-day activation:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = textPrimary,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = paymentProofInput,
                                onValueChange = { paymentProofInput = it; proofError = null },
                                label = { Text("Transaction ID / UTR / Payment Reference *") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            )
                            if (!proofError.isNullOrBlank()) {
                                Text(
                                    text = proofError.orEmpty(),
                                    fontSize = 11.5.sp,
                                    color = Color(0xFFEF4444),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Button(
                                onClick = {
                                    if (paymentProofInput.trim().length < 4) {
                                        proofError = "Enter a valid Transaction ID, UTR, or payment reference."
                                        return@Button
                                    }
                                    onSubmitPaymentProof(request.id, paymentProofInput.trim())
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF10B981),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Lucide.Check, null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Confirm Payment Sent", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 3. Payment Submitted 7-Day SLA Countdown Card
        if (request.status == "payment_submitted") {
            item {
                val targetMs = request.expectedActivationBy
                    ?: ((request.paymentSubmittedAt ?: nowMs) + 7L * 24L * 60L * 60L * 1000L)
                val remainingMs = (targetMs - nowMs).coerceAtLeast(0L)
                val daysLeft = (remainingMs / (24L * 60L * 60L * 1000L)).toInt()
                val hoursLeft = ((remainingMs % (24L * 60L * 60L * 1000L)) / (60L * 60L * 1000L)).toInt()

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF06B6D4).copy(alpha = if (isDark) 0.14f else 0.08f),
                    border = BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Lucide.Clock, null, tint = Color(0xFF06B6D4), modifier = Modifier.size(18.dp))
                            Text(
                                text = "7-Day Activation Window Active",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                        }
                        Text(
                            text = "Transaction Ref submitted: ${request.paymentProofRef ?: "Pending"}",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0891B2)
                        )
                        Text(
                            text = if (remainingMs > 0L) {
                                "Estimated time remaining: $daysLeft day(s), $hoursLeft hour(s). Admin is verifying your payment and will activate your ${request.badgeLabel} badge & Pro benefits shortly."
                            } else {
                                "SLA window reached — final admin activation is queued."
                            },
                            fontSize = 12.sp,
                            color = textSecondary,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }

        // 4. Celebratory Activated Card
        if (isActivated) {
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFF10B981).copy(alpha = if (isDark) 0.16f else 0.10f),
                    border = BorderStroke(1.5.dp, Color(0xFF10B981).copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            VerifiedBadge(size = 24.dp, tone = request.badgeTone, animated = true, showGlow = true)
                            Column {
                                Text(
                                    text = "Welcome to Oscellion Pro!",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                Text(
                                    text = "Your ${request.badgeLabel} badge and Pro benefits are now live across the app.",
                                    fontSize = 12.sp,
                                    color = textSecondary
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onNavigateToProfile,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("View My Profile", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                            OutlinedButton(
                                onClick = onStartNewRequest,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Change Badge Tier", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // 5. Visual 4-Stage Workflow Stepper
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = cardBg,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Verification Progress Timeline",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )

                    ProgressStepRow(
                        stepNumber = 1,
                        title = "Form Submitted",
                        detail = if (request.submittedAt > 0L) "Submitted on ${formatTime.format(Date(request.submittedAt))} · ${request.contactEmail}" else "Form received",
                        done = statusIndex >= 0 && !isRejected,
                        active = statusIndex == 0 && !isRejected,
                        icon = Lucide.Send,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        isDark = isDark
                    )
                    ProgressStepRow(
                        stepNumber = 2,
                        title = "Admin Review",
                        detail = if (request.reviewedAt != null && request.reviewedAt > 0L) "Reviewed on ${formatTime.format(Date(request.reviewedAt))}" else "Admin verifies profile & badge tier eligibility",
                        done = statusIndex >= 2 && !isRejected,
                        active = statusIndex == 1 && !isRejected,
                        icon = Lucide.ShieldCheck,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        isDark = isDark
                    )
                    ProgressStepRow(
                        stepNumber = 3,
                        title = "Email & Payment QR Sent",
                        detail = when {
                            request.status == "payment_sent" -> "Check ${request.emailSentTo ?: request.contactEmail} or scan the QR above and confirm your Transaction ID"
                            request.paymentSentAt != null && request.paymentSentAt > 0L -> "Payment QR emailed on ${formatTime.format(Date(request.paymentSentAt))}"
                            else -> "Admin will send invoice details + scannable QR to ${request.contactEmail}"
                        },
                        done = statusIndex >= 3 && !isRejected,
                        active = statusIndex == 2 && !isRejected,
                        icon = Lucide.Mail,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        isDark = isDark
                    )
                    ProgressStepRow(
                        stepNumber = 4,
                        title = "Badge & Pro Benefits Activated (Within 7 Days)",
                        detail = when {
                            isActivated && request.activatedAt != null -> "Activated on ${formatTime.format(Date(request.activatedAt))}"
                            request.status == "payment_submitted" -> "Payment proof submitted (${request.paymentProofRef}). Verification completes within 7 days."
                            else -> "After payment confirmation, your custom badge activates within 7 days"
                        },
                        done = statusIndex >= 4,
                        active = statusIndex == 3,
                        icon = Lucide.Sparkles,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        isDark = isDark
                    )

                    if (isRejected) {
                        Button(
                            onClick = onStartNewRequest,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1), contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Lucide.RefreshCw, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Start New Pro Application", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 6. Request Details / History Summary
        if (allRequests.size > 1) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, borderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Application History (${allRequests.size})",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        allRequests.drop(1).take(4).forEach { old ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    VerifiedBadge(size = 15.dp, tone = old.badgeTone)
                                    Text(
                                        text = "${old.badgeLabel} ($${old.priceUsd})",
                                        fontSize = 12.sp,
                                        color = textPrimary
                                    )
                                }
                                Text(
                                    text = old.status.replace("_", " ").uppercase(),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ProgressStepRow(
    stepNumber: Int,
    title: String,
    detail: String,
    done: Boolean,
    active: Boolean,
    icon: ImageVector,
    textPrimary: Color,
    textSecondary: Color,
    isDark: Boolean
) {
    val accent = when {
        done -> Color(0xFF10B981)
        active -> Color(0xFF6366F1)
        else -> if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = if (done || active) 0.18f else 0.25f))
                .border(1.5.dp, accent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (done) {
                Icon(Lucide.Check, null, tint = accent, modifier = Modifier.size(16.dp))
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (active) accent else textSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "$stepNumber. $title",
                fontSize = 13.5.sp,
                fontWeight = if (active || done) FontWeight.Bold else FontWeight.SemiBold,
                color = if (active || done) textPrimary else textSecondary
            )
            Text(
                text = detail,
                fontSize = 12.sp,
                color = textSecondary,
                lineHeight = 16.sp
            )
        }
    }
}
