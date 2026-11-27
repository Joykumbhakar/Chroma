package com.chroma.studio.ui.components

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.R
import com.chroma.studio.model.UserProfile
import com.chroma.studio.ui.theme.InterFontFamily
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Entitlement & state controller for the Premium Luxury Oscellion Icon & Crest.
 * Strictly unlocked ONLY when the active authenticated user has BOTH:
 * - a verified badge (`verified == true`)
 * - an active Pro membership (`isPro == true`)
 */
object OscellionLuxuryBrandController {

    private const val PREFS_NAME = "osc_luxury_brand_prefs"
    private const val KEY_ACTIVE_UID = "active_uid"
    private const val KEY_LUXURY_ACTIVE = "luxury_active"
    private const val ALIAS_DEFAULT = "com.chroma.studio.HomeActivityDefault"
    private const val ALIAS_LUXURY = "com.chroma.studio.HomeActivityLuxury"

    private val _isLuxuryEnabled = MutableStateFlow(false)
    val isLuxuryEnabled: StateFlow<Boolean> = _isLuxuryEnabled.asStateFlow()

    /**
     * Pure eligibility check: ONLY users with BOTH a verified badge and Pro tier qualify.
     */
    fun isLuxuryLogoEligible(verified: Boolean, isPro: Boolean): Boolean = verified && isPro

    fun isLuxuryLogoEligible(profile: UserProfile?): Boolean =
        profile != null && isLuxuryLogoEligible(verified = profile.verified, isPro = profile.isPro)

    /**
     * Warms up cached tier state at startup before Firestore responds so splash & headers match immediately.
     */
    fun warmup(context: Context, currentUid: String? = null) {
        val activeUid = currentUid ?: resolveFirebaseUid()
        if (activeUid.isNullOrBlank()) {
            _isLuxuryEnabled.value = false
            syncLauncherIconAlias(context, false)
            return
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cachedUid = prefs.getString(KEY_ACTIVE_UID, null)
        val cachedLuxury = if (cachedUid == activeUid) prefs.getBoolean(KEY_LUXURY_ACTIVE, false) else false
        _isLuxuryEnabled.value = cachedLuxury
        syncLauncherIconAlias(context, cachedLuxury)
    }

    /**
     * Updates luxury brand state only when [profile] belongs to the currently authenticated user.
     */
    fun updateForProfileIfCurrentUser(
        context: Context,
        profile: UserProfile?,
        currentUid: String? = null
    ) {
        if (profile == null || profile.uid.isBlank()) return
        val activeUid = currentUid ?: resolveFirebaseUid() ?: return
        if (profile.uid != activeUid) return

        val eligible = isLuxuryLogoEligible(profile)
        _isLuxuryEnabled.value = eligible

        runCatching {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ACTIVE_UID, activeUid)
                .putBoolean(KEY_LUXURY_ACTIVE, eligible)
                .apply()
        }
        syncLauncherIconAlias(context, eligible)
    }

    /**
     * Clears luxury logo entitlement immediately on sign-out / account removal.
     */
    fun resetForLogout(context: Context) {
        _isLuxuryEnabled.value = false
        runCatching {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_ACTIVE_UID)
                .putBoolean(KEY_LUXURY_ACTIVE, false)
                .apply()
        }
        syncLauncherIconAlias(context, false)
    }

    private fun resolveFirebaseUid(): String? =
        runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull()

    /**
     * Switches the Android Home Screen launcher icon between Standard and Luxury
     * without killing the foreground activity, and skips redundant updates.
     */
    fun syncLauncherIconAlias(context: Context, luxuryEnabled: Boolean) {
        runCatching {
            val appCtx = context.applicationContext ?: context
            val pm = appCtx.packageManager
            val defaultComponent = ComponentName(appCtx.packageName, ALIAS_DEFAULT)
            val luxuryComponent = ComponentName(appCtx.packageName, ALIAS_LUXURY)

            val desiredDefaultState = if (luxuryEnabled) {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            }
            val desiredLuxuryState = if (luxuryEnabled) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }

            if (pm.getComponentEnabledSetting(luxuryComponent) != desiredLuxuryState) {
                pm.setComponentEnabledSetting(
                    luxuryComponent,
                    desiredLuxuryState,
                    PackageManager.DONT_KILL_APP
                )
            }
            if (pm.getComponentEnabledSetting(defaultComponent) != desiredDefaultState) {
                pm.setComponentEnabledSetting(
                    defaultComponent,
                    desiredDefaultState,
                    PackageManager.DONT_KILL_APP
                )
            }
        }
    }
}

/**
 * Official Oscellion Brand Identity System.
 * Standard Geometry: M 512 212 A 300 300 0 0 1 812 512 A 300 300 0 0 1 512 812 A 300 300 0 0 1 299.87 724.13 L 148.5 771 Q 120 780 136.5 755.1 L 230.24 615 A 300 300 0 0 1 512 212 Z M 512 342 A 170 170 0 0 0 512 682 A 170 170 0 0 0 512 342 Z
 * Luxury Crest Geometry (oscellion_luxury_app_icon): M 532 126 A 384 384 ...
 */
object OscellionBrand {
    const val PATH_DATA = "M 512 212 A 300 300 0 0 1 812 512 A 300 300 0 0 1 512 812 A 300 300 0 0 1 299.87 724.13 L 148.5 771 Q 120 780 136.5 755.1 L 230.24 615 A 300 300 0 0 1 512 212 Z M 512 342 A 170 170 0 0 0 512 682 A 170 170 0 0 0 512 342 Z"
    const val LUXURY_PATH_DATA = "M 532 126 A 384 384 0 0 1 916 510 A 384 384 0 0 1 532 894 A 384 384 0 0 1 260.47 781.53 L 82.05 836.97 Q 53 846 69.54 817.35 L 171.16 641.34 A 384 384 0 0 1 532 126 Z M 532 302 A 208 208 0 0 0 532 718 A 208 208 0 0 0 532 302 Z"

    // Metallic silver gradient stops per brand guidelines
    val MetalGradientStops = listOf(
        0.00f to Color(0xFFFFFFFF),
        0.25f to Color(0xFFF5F5F7),
        0.50f to Color(0xFFD1D1D6),
        0.85f to Color(0xFF8E8E93),
        1.00f to Color(0xFF48484A)
    )

    // Royal Sapphire Blue Radial Tile Stops (oscellion_luxury_app_icon)
    val LuxurySapphireStops = arrayOf(
        0.00f to Color(0xFF3B82F6),
        0.48f to Color(0xFF1E3A8A),
        1.00f to Color(0xFF091333)
    )

    // Base 24K Champagne-Gold Linear Fill Stops
    val LuxuryGoldFillStops = arrayOf(
        0.00f to Color(0xFFFFFDF5),
        0.20f to Color(0xFFF7E3A1),
        0.48f to Color(0xFFDFB958),
        0.76f to Color(0xFFAA7D22),
        1.00f to Color(0xFF6E4E10)
    )

    // Warm Specular Sheen Stops
    val LuxurySpecularSheenStops = arrayOf(
        0.00f to Color(0xD9FFFFFF),
        0.40f to Color(0x52FFF2C6),
        1.00f to Color(0x00D4AF37)
    )

    // Bold Chamfered Top-Left Highlight Stops
    val LuxuryTopHighlightStops = arrayOf(
        0.00f to Color(0xFFFFFFFF),
        0.45f to Color(0x73FCEBB6),
        0.80f to Color(0x00D4AF37)
    )

    // Bold Chamfered Bottom-Right Shadow Stops
    val LuxuryBottomShadowStops = arrayOf(
        0.45f to Color(0x00291A03),
        1.00f to Color(0xEB1C1102)
    )

    // Champagne-Gold Tile Border Stops
    val LuxuryBorderStops = listOf(
        Color(0xFFF7E3A1),
        Color(0xFFDFB958),
        Color(0xFFAA7D22)
    )

    val AppIconBgGradient = Brush.verticalGradient(
        listOf(Color(0xFF2C2C2E), Color(0xFF121213))
    )

    val DynamicIslandTileBg = Color(0xFF121213)
}

enum class OscellionLogoVariant {
    APP_ICON,            // 1. Main App Icon Squircle
    TRANSPARENT_METALLIC,// 2. Web Logo (Transparent)
    SPLASH,              // 3. Android Splash Screen Mockup & Asset
    FAVICON,             // 4. Compact / Favicon High Contrast
    NOTIFICATION,        // 5. Monochromatic Pure White
    MONOCHROME_BLACK,    // 6. Monochromatic Solid Black
    DYNAMIC_ISLAND,      // 7. Dynamic Island Alert Badge
    WATCH_CIRCULAR,      // 8. Watch OS Circular Complication
    WATERMARK_OUTLINE,   // 9. Minimalist Wireframe Outline
    SPINNER              // 10. Animated In-App Loader
}

/**
 * Universal Oscellion Logo Component that renders any brand variation.
 */
@Composable
fun OscellionLogo(
    variant: OscellionLogoVariant,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    tint: Color? = null,
    forceLuxury: Boolean? = null
) {
    when (variant) {
        OscellionLogoVariant.APP_ICON -> OscellionAppIcon(modifier = modifier, size = size, forceLuxury = forceLuxury)
        OscellionLogoVariant.TRANSPARENT_METALLIC -> OscellionWebLogo(modifier = modifier, size = size, forceLuxury = forceLuxury)
        OscellionLogoVariant.SPLASH -> OscellionSplashBrandView(modifier = modifier, size = size, forceLuxury = forceLuxury)
        OscellionLogoVariant.FAVICON -> OscellionFavicon(modifier = modifier, size = size)
        OscellionLogoVariant.NOTIFICATION -> OscellionNotificationBadge(modifier = modifier, size = size, tint = tint ?: Color.White)
        OscellionLogoVariant.MONOCHROME_BLACK -> OscellionMonochromeLogo(modifier = modifier, size = size, color = tint ?: Color.Black)
        OscellionLogoVariant.DYNAMIC_ISLAND -> OscellionDynamicIslandBadge(modifier = modifier, size = size, forceLuxury = forceLuxury)
        OscellionLogoVariant.WATCH_CIRCULAR -> OscellionWatchComplicationBadge(modifier = modifier, size = size, forceLuxury = forceLuxury)
        OscellionLogoVariant.WATERMARK_OUTLINE -> OscellionWatermarkView(modifier = modifier, size = size, color = tint ?: Color.White)
        OscellionLogoVariant.SPINNER -> OscellionSpinner(modifier = modifier, size = size, color = tint ?: Color.White)
    }
}

/**
 * Variation 1: Main App Icon Squircle.
 * Automatically switches to the Premium Royal Sapphire & 24K Champagne-Gold crest
 * exclusively when the active user holds BOTH a Verified badge and Pro status.
 */
@Composable
fun OscellionAppIcon(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    forceLuxury: Boolean? = null
) {
    val isLuxuryUnlocked by OscellionLuxuryBrandController.isLuxuryEnabled.collectAsState()
    if (forceLuxury ?: isLuxuryUnlocked) {
        OscellionLuxuryAppIcon(modifier = modifier, size = size)
        return
    }

    val squircleShape = RoundedCornerShape(size * (226f / 1024f))

    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = size * 0.12f,
                shape = squircleShape,
                spotColor = Color.Black.copy(alpha = 0.65f),
                ambientColor = Color.Black.copy(alpha = 0.35f)
            )
            .clip(squircleShape)
            .background(OscellionBrand.AppIconBgGradient)
            .border(
                width = (size * 0.015f).coerceAtLeast(1.dp),
                brush = Brush.linearGradient(
                    listOf(
                        Color(0xFF48484A).copy(alpha = 0.8f),
                        Color(0xFF2C2C2E).copy(alpha = 0.2f),
                        Color(0xFF000000).copy(alpha = 0.9f)
                    )
                ),
                shape = squircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        // Milled Metallic Symbol
        OscellionVectorSymbol(
            modifier = Modifier.size(size * 0.72f),
            brush = Brush.linearGradient(
                colorStops = OscellionBrand.MetalGradientStops.toTypedArray(),
                start = Offset.Zero,
                end = Offset.Infinite
            ),
            edgeHighlight = true
        )

        // Top Glass Gloss Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.48f)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

/**
 * Exact Jetpack Compose realization of oscellion_luxury_app_icon.html:
 * Royal Sapphire Blue radial tile + Champagne-Gold bevelled tile border +
 * 24K Champagne-Gold crest with warm specular sheen and bold chamfered highlight/shadow.
 */
@Composable
fun OscellionLuxuryAppIcon(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    showBorder: Boolean = true
) {
    val squircleShape = RoundedCornerShape(size * (226f / 1024f))
    val borderWidth = if (showBorder) (size * 0.022f).coerceIn(1.dp, 2.5.dp) else 0.dp

    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = size * 0.14f,
                shape = squircleShape,
                spotColor = Color(0xFF091333).copy(alpha = 0.8f),
                ambientColor = Color(0xFF1E3A8A).copy(alpha = 0.45f)
            )
            .clip(squircleShape)
            .then(
                if (showBorder) {
                    Modifier.border(
                        width = borderWidth,
                        brush = Brush.linearGradient(
                            colors = OscellionBrand.LuxuryBorderStops,
                            start = Offset.Zero,
                            end = Offset.Infinite
                        ),
                        shape = squircleShape
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Royal Sapphire Blue Radial Tile Background (centered at 0.38w, 0.32h, radius 0.72 * minDim)
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colorStops = OscellionBrand.LuxurySapphireStops,
                    center = Offset(this.size.width * 0.38f, this.size.height * 0.32f),
                    radius = this.size.minDimension * 0.72f
                )
            )
        }

        // 24K Champagne-Gold Sculpted Crest
        OscellionLuxuryEmblemCanvas(
            modifier = Modifier.size(size * 0.70f),
            drawDropShadow = true
        )
    }
}

/**
 * Renders the 24K Champagne-Gold Oscellion crest vector geometry verbatim from oscellion_luxury_app_icon.html.
 */
@Composable
fun OscellionLuxuryEmblemCanvas(
    modifier: Modifier = Modifier,
    drawDropShadow: Boolean = false
) {
    val parsedPath = remember {
        PathParser().parsePathString(OscellionBrand.LUXURY_PATH_DATA).toPath().apply {
            fillType = PathFillType.EvenOdd
        }
    }

    val goldFillBrush = remember {
        Brush.linearGradient(
            colorStops = OscellionBrand.LuxuryGoldFillStops,
            start = Offset(122.88f, 81.92f),
            end = Offset(901.12f, 942.08f)
        )
    }
    val specularSheenBrush = remember {
        Brush.radialGradient(
            colorStops = OscellionBrand.LuxurySpecularSheenStops,
            center = Offset(327.68f, 266.24f),
            radius = 614.4f
        )
    }
    val topHighlightBrush = remember {
        Brush.linearGradient(
            colorStops = OscellionBrand.LuxuryTopHighlightStops,
            start = Offset.Zero,
            end = Offset(1024f, 1024f)
        )
    }
    val bottomShadowBrush = remember {
        Brush.linearGradient(
            colorStops = OscellionBrand.LuxuryBottomShadowStops,
            start = Offset.Zero,
            end = Offset(1024f, 1024f)
        )
    }

    Canvas(modifier = modifier) {
        val scale = size.minDimension / 1024f
        val offsetX = (size.width - 1024f * scale) / 2f
        val offsetY = (size.height - 1024f * scale) / 2f

        drawIntoCanvas { canvas ->
            canvas.save()
            canvas.translate(offsetX, offsetY)
            canvas.scale(scale, scale)

            if (drawDropShadow) {
                canvas.save()
                canvas.translate(18f, 22f)
                val shadowPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color(0xFF241502).copy(alpha = 0.44f)
                    style = PaintingStyle.Fill
                }
                canvas.drawPath(parsedPath, shadowPaint)
                canvas.restore()
            }

            canvas.save()
            canvas.clipPath(parsedPath)

            // 1. Base 24K Champagne-Gold Fill
            val fillPaint = Paint().apply {
                isAntiAlias = true
                style = PaintingStyle.Fill
            }
            goldFillBrush.applyTo(Size(1024f, 1024f), fillPaint, 1.0f)
            canvas.drawPath(parsedPath, fillPaint)

            // 2. Warm Specular Sheen
            val sheenPaint = Paint().apply {
                isAntiAlias = true
                style = PaintingStyle.Fill
            }
            specularSheenBrush.applyTo(Size(1024f, 1024f), sheenPaint, 1.0f)
            canvas.drawRect(Rect(0f, 0f, 1024f, 1024f), sheenPaint)

            // 3. Bold Chamfered Top-Left Highlight
            val highlightPaint = Paint().apply {
                isAntiAlias = true
                style = PaintingStyle.Stroke
                strokeWidth = 26f
            }
            topHighlightBrush.applyTo(Size(1024f, 1024f), highlightPaint, 0.95f)
            canvas.drawPath(parsedPath, highlightPaint)

            // 4. Bold Chamfered Bottom-Right Shadow
            val depthPaint = Paint().apply {
                isAntiAlias = true
                style = PaintingStyle.Stroke
                strokeWidth = 30f
            }
            bottomShadowBrush.applyTo(Size(1024f, 1024f), depthPaint, 0.9f)
            canvas.drawPath(parsedPath, depthPaint)

            canvas.restore()
            canvas.restore()
        }
    }
}

/**
 * Variation 2: Web Logo (Transparent 3D Metallic Symbol, or 24K Gold crest for Verified Pro users)
 */
@Composable
fun OscellionWebLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    forceLuxury: Boolean? = null
) {
    val isLuxuryUnlocked by OscellionLuxuryBrandController.isLuxuryEnabled.collectAsState()
    val useLuxury = forceLuxury ?: isLuxuryUnlocked

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (useLuxury) {
            OscellionLuxuryEmblemCanvas(
                modifier = Modifier.fillMaxSize(),
                drawDropShadow = false
            )
        } else {
            OscellionVectorSymbol(
                modifier = Modifier.fillMaxSize(),
                brush = Brush.linearGradient(
                    colorStops = OscellionBrand.MetalGradientStops.toTypedArray(),
                    start = Offset.Zero,
                    end = Offset.Infinite
                ),
                edgeHighlight = true
            )
        }
    }
}

/**
 * Variation 3: Android Splash Screen Cold-Boot Mockup & Composable
 */
@Composable
fun OscellionSplashBrandView(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    forceLuxury: Boolean? = null
) {
    val isLuxuryUnlocked by OscellionLuxuryBrandController.isLuxuryEnabled.collectAsState()
    val useLuxury = forceLuxury ?: isLuxuryUnlocked

    val infiniteTransition = rememberInfiniteTransition(label = "SplashPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .scale(pulseScale),
            contentAlignment = Alignment.Center
        ) {
            // Subtle ambient radial glow behind symbol
            Box(
                modifier = Modifier
                    .size(size * 1.5f)
                    .background(
                        Brush.radialGradient(
                            if (useLuxury) {
                                listOf(
                                    Color(0xFF3B82F6).copy(alpha = 0.24f * pulseAlpha),
                                    Color(0xFFDFB958).copy(alpha = 0.08f * pulseAlpha),
                                    Color.Transparent
                                )
                            } else {
                                listOf(
                                    Color.White.copy(alpha = 0.12f * pulseAlpha),
                                    Color.Transparent
                                )
                            }
                        )
                    )
            )

            if (useLuxury) {
                OscellionLuxuryAppIcon(
                    modifier = Modifier.size(size),
                    size = size,
                    showBorder = true
                )
            } else {
                // Scaled metallic symbol
                OscellionVectorSymbol(
                    modifier = Modifier.size(size),
                    brush = Brush.linearGradient(
                        listOf(Color(0xFFFFFFFF), Color(0xFF888888))
                    ),
                    edgeHighlight = true
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "OSCELLION",
            fontSize = (size.value * 0.14f).coerceAtLeast(13f).sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = InterFontFamily,
            color = (if (useLuxury) Color(0xFFF7E3A1) else Color.White).copy(alpha = pulseAlpha),
            letterSpacing = 4.sp
        )
    }
}

/**
 * Variation 4: Favicon / Compact High-Contrast Symbol
 */
@Composable
fun OscellionFavicon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    OscellionVectorSymbol(
        modifier = modifier.size(size),
        brush = Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFF8E8E93))),
        edgeHighlight = true,
        edgeStrokeColor = Color(0xFF48484A)
    )
}

/**
 * Variation 5: Monochromatic Pure White Notification Silhouette
 */
@Composable
fun OscellionNotificationBadge(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    tint: Color = Color.White
) {
    OscellionVectorSymbol(
        modifier = modifier.size(size),
        color = tint,
        edgeHighlight = false
    )
}

/**
 * Variation 6: Monochromatic Solid Black Print Logo
 */
@Composable
fun OscellionMonochromeLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color = Color.Black
) {
    OscellionVectorSymbol(
        modifier = modifier.size(size),
        color = color,
        edgeHighlight = false
    )
}

/**
 * Variation 7: Dynamic Island Alert Miniature Squircle Badge
 */
@Composable
fun OscellionDynamicIslandBadge(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    forceLuxury: Boolean? = null
) {
    val isLuxuryUnlocked by OscellionLuxuryBrandController.isLuxuryEnabled.collectAsState()
    if (forceLuxury ?: isLuxuryUnlocked) {
        OscellionLuxuryAppIcon(modifier = modifier, size = size, showBorder = true)
        return
    }

    val squircleRadius = size * (226f / 1024f)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(squircleRadius))
            .background(OscellionBrand.DynamicIslandTileBg)
            .border(
                0.5.dp,
                Color.White.copy(alpha = 0.18f),
                RoundedCornerShape(squircleRadius)
            ),
        contentAlignment = Alignment.Center
    ) {
        OscellionVectorSymbol(
            modifier = Modifier.size(size * 0.70f),
            brush = Brush.linearGradient(
                listOf(Color(0xFFFFFFFF), Color(0xFF8E8E93))
            ),
            edgeHighlight = true
        )
    }
}

/**
 * Variation 8: Watch OS Circular Wearable Complication
 */
@Composable
fun OscellionWatchComplicationBadge(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    forceLuxury: Boolean? = null
) {
    val isLuxuryUnlocked by OscellionLuxuryBrandController.isLuxuryEnabled.collectAsState()
    val useLuxury = forceLuxury ?: isLuxuryUnlocked

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(
                if (useLuxury) {
                    Modifier.border(
                        (size * 0.022f).coerceAtLeast(1.dp),
                        Brush.linearGradient(OscellionBrand.LuxuryBorderStops),
                        CircleShape
                    )
                } else {
                    Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF2C2C2E), Color(0xFF0A0A0B))
                            )
                        )
                        .border(
                            (size * 0.016f).coerceAtLeast(1.dp),
                            Color(0x993A3A3C),
                            CircleShape
                        )
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (useLuxury) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = OscellionBrand.LuxurySapphireStops,
                        center = Offset(this.size.width * 0.38f, this.size.height * 0.32f),
                        radius = this.size.minDimension * 0.72f
                    )
                )
            }
            OscellionLuxuryEmblemCanvas(
                modifier = Modifier.size(size * 0.68f),
                drawDropShadow = true
            )
        } else {
            OscellionVectorSymbol(
                modifier = Modifier.size(size * 0.68f),
                brush = Brush.linearGradient(
                    listOf(Color(0xFFFFFFFF), Color(0xFFD1D1D6), Color(0xFF48484A))
                ),
                edgeHighlight = true
            )
        }
    }
}

/**
 * Variation 9: Minimalist Wireframe Watermark (Stroke Only)
 */
@Composable
fun OscellionWatermarkView(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color = Color.White.copy(alpha = 0.5f),
    strokeWidthDp: Dp = 2.dp
) {
    val parsedPath = remember {
        PathParser().parsePathString(OscellionBrand.PATH_DATA).toPath().apply {
            fillType = PathFillType.EvenOdd
        }
    }

    Canvas(modifier = modifier.size(size)) {
        val scale = size.toPx() / 1024f
        val strokePx = (strokeWidthDp.toPx() * (1024f / size.toPx())).coerceAtLeast(2f)

        drawIntoCanvas { canvas ->
            canvas.save()
            canvas.scale(scale, scale)
            canvas.drawPath(
                parsedPath,
                Paint().apply {
                    this.color = color
                    this.style = PaintingStyle.Stroke
                    this.strokeWidth = strokePx
                    this.strokeCap = StrokeCap.Round
                    this.strokeJoin = StrokeJoin.Round
                    this.isAntiAlias = true
                }
            )
            canvas.restore()
        }
    }
}

/**
 * Variation 10: In-App Animated Loader / Spinner (Exact Flat Loader & Overflowed Checkmark Morph)
 */
@Composable
fun OscellionSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    color: Color = Color.White,
    isCompleted: Boolean = false,
    checkColor: Color = Color.White
) {
    OscellionMorphLoader(
        modifier = modifier.size(size),
        isCompleted = isCompleted,
        color = color,
        checkColor = checkColor
    )
}

/**
 * Low-level vector renderer for the official Oscellion symbol path.
 */
@Composable
private fun OscellionVectorSymbol(
    modifier: Modifier = Modifier,
    color: Color? = null,
    brush: Brush? = null,
    edgeHighlight: Boolean = false,
    edgeStrokeColor: Color = Color.White.copy(alpha = 0.6f)
) {
    val parsedPath = remember {
        PathParser().parsePathString(OscellionBrand.PATH_DATA).toPath().apply {
            fillType = PathFillType.EvenOdd
        }
    }

    Canvas(modifier = modifier) {
        val scale = size.minDimension / 1024f
        val offsetX = (size.width - 1024f * scale) / 2f
        val offsetY = (size.height - 1024f * scale) / 2f

        drawIntoCanvas { canvas ->
            canvas.save()
            canvas.translate(offsetX, offsetY)
            canvas.scale(scale, scale)

            // Fill
            val paint = Paint().apply {
                this.isAntiAlias = true
                this.style = PaintingStyle.Fill
            }
            if (brush != null) {
                brush.applyTo(Size(1024f, 1024f), paint, 1.0f)
            } else {
                paint.color = color ?: Color.White
            }
            canvas.drawPath(parsedPath, paint)

            // Optional specular highlight stroke
            if (edgeHighlight) {
                val strokePaint = Paint().apply {
                    this.isAntiAlias = true
                    this.style = PaintingStyle.Stroke
                    this.color = edgeStrokeColor
                    this.strokeWidth = 6f
                }
                canvas.drawPath(parsedPath, strokePaint)
            }

            canvas.restore()
        }
    }
}
