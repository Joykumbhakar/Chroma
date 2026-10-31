package com.chroma.studio.ui.graphics3d

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class Apple3DGraphicId(
    val number: String,
    val title: String,
    val tag: String,
    val dialogTitle: String,
    val dialogDesc: String
) {
    TRASH_BIN("01", "Cupertino Trash Bin", "Reference Spec", "Empty System Trash?", "24 cached snapshots (1.8 GB) will be permanently removed."),
    SMART_FOLDER("02", "Smart Finder Folder", "Articulated Docs", "Mount Encrypted Folder?", "This workspace vault will be unlocked for your local session."),
    NVME_DRIVE("03", "NVMe Storage Vault", "Rotary Platter", "Format Scratch Volume?", "Reallocating NVMe cache will purge temporary indexing blocks."),
    ARCHIVE_BOX("04", "Archive Package Box", "Lifting Lid", "Compress Project Archive?", "Bundles all 30 vector assets and shader tokens into a lossless package."),
    PROTECTION_SHIELD("05", "Protection Shield", "Dialog Standard", "Enable Local Neural Core?", "Allocates 16-core on-device compute with zero cloud telemetry."),
    BIOMETRIC_LOCK("06", "Biometric Padlock", "Hinged Shackle", "Lock System Preferences?", "Changes to compute nodes will require biometric authentication."),
    HARDWARE_PASSKEY("07", "Hardware Passkey", "Milled Blade", "Generate Hardware Passkey?", "Creates a cryptographic signing key bound to your Secure Enclave."),
    BIOMETRIC_OPTIC("08", "Biometric Optic Vault", "Symmetric Ridges", "Verify Biometric Identity?", "Touch the sensor or confirm to authorize elevated kernel privileges."),
    NEURAL_CPU("09", "Neural Silicon Die", "12-Pin M-Series", "Boost Neural Clock Speed?", "Prioritizes local matrix multiplication threads for low-latency inference."),
    CLOUD_SYNC("10", "Cloud Sync Core", "3D Sync Arrow", "Sync Design Tokens to Mesh?", "Pushes updated liquid glass variables to all connected studio nodes."),
    SPATIAL_BELL("11", "Spatial Audio Bell", "Acoustic Waves", "Enable Critical Alerts?", "High-priority node diagnostics will chime even when Focus mode is active."),
    MAGSAFE_BATTERY("12", "MagSafe Battery Core", "3 Charge Cells", "Enable Low-Power Standby?", "Reduces background polling when battery charge drops below 20%."),
    PRO_CAMERA("13", "Pro Optical Camera", "Sapphire Lens", "Grant Optical Camera Access?", "Allows real-time spatial frame capture for visual grounding."),
    PHOTO_GALLERY("14", "Layered Photo Gallery", "3D Landscape", "Import High-Res Renders?", "30 transparent 2048px PNG renders will be added to your library."),
    SPATIAL_MAIL("15", "Spatial Mail Envelope", "Sliding Card", "Send Diagnostic Report?", "Dispatches an anonymized performance summary to your team."),
    TELEMETRY_CHART("16", "Live Telemetry Chart", "Volumetric Bars", "Publish Live Telemetry Poll?", "Shares real-time benchmark metrics with all participants."),
    WIFI_RADAR("17", "AirDrop Mesh Radar", "Wave Dish", "Enable AirDrop Mesh Relay?", "Allows nearby studio devices to transfer encrypted bundles over Wi-Fi 7."),
    SPATIAL_HEADPHONES("18", "Spatial Headphones", "AirPods Max", "Calibrate Spatial Audio?", "Maps ear geometry for personalized Dolby Atmos and head tracking."),
    SYSTEM_GEAR("19", "System Preferences Gear", "Rotary Hub", "Reset System Preferences?", "Restores all Liquid Glass tokens to factory Cupertino defaults."),
    NETWORK_GLOBE("20", "Global Network Sphere", "Orbital Grid", "Route Traffic via Private Relay?", "Encrypts DNS queries and masks your IP across global edge nodes."),
    STUDIO_MIC("21", "Studio Voice Mic", "Condenser", "Enable Voice Isolation?", "Uses on-device neural filtering to block ambient noise during calls."),
    SPATIAL_COMPASS("22", "Spatial Compass", "Magnetic Dial", "Lock Spatial Waypoint?", "Anchors the current 3D workspace coordinate to your room orientation."),
    SMART_HOME("23", "Smart Home Hub", "Matter Hub", "Arm Home Automation Scene?", "Locks all Matter entry points and switches lighting to Night mode."),
    DEV_TERMINAL("24", "Terminal Command Core", "Dev Shell", "Execute Elevated Shell Script?", "Runs the build bundler in a sandboxed container."),
    BLUETOOTH_RUNE("25", "Bluetooth Spatial Rune", "Mesh Rune", "Pair Spatial Audio Accessory?", "Establishes an ultra-low-latency Bluetooth 5.4 lossless audio link."),
    DISPLAY_SUN("26", "Display Brightness Sun", "XDR Sun", "Enable Reference XDR Mode?", "Locks peak luminance to 1600 nits for P3 wide-gamut grading."),
    SPATIAL_SPEAKER("27", "Spatial Speaker Volume", "Acoustic", "Boost Studio Monitor Gain?", "Increases acoustic headroom by +6 dB for spatial output."),
    LOCATION_PIN("28", "Location Navigation Pin", "Geo Pin", "Allow Precise Location?", "Used only while active for local mesh node positioning."),
    AIRTAG_TRACKER("29", "AirTag Precision Tracker", "UWB Puck", "Start Precision Finding?", "Uses Ultra-Wideband guidance to locate your tag within 5 cm."),
    ROCKET_BOOSTER("30", "Rocket Performance Booster", "Turbo Core", "Launch Turbo Build Pipeline?", "Purges incremental caches and compiles all targets in parallel."),
    WORKS("31", "Studio Artboard & Easel", "Articulated Clamp", "Create New Studio Work?", "Launches a new high-resolution canvas with default layer stack."),
    FLASH_VIDEO("32", "Pro Video Clapperboard", "Hinged Clapper", "Record New Flash Video?", "Opens the camera viewfinder for rapid vertical moment capture."),
    REMIX_NODE("33", "Dual Remix Orbit", "Interlocking Rings", "Start Co-Creation Remix?", "Forks the selected artwork with attribution linked to the original creator."),
    CHAT_BUBBLE("34", "Cupertino Spatial Chat", "Articulated Bubbles", "Start New Conversation?", "Opens an end-to-end encrypted messaging channel."),
    SAVED_WORKS("35", "Saved Works Folio", "3D Ribbon Folio", "Open Saved Works?", "View all bookmarked creations and design blueprints saved to your private collection.")
}

object AppleHigShaders {
    val PlinthShadow = Color(0xFF001645)

    fun bodyBrush(s: Float) = Brush.linearGradient(
        0.00f to Color(0xFF4CC9FF),
        0.22f to Color(0xFF1A96FF),
        0.52f to Color(0xFF007AFF),
        0.78f to Color(0xFF0051D5),
        1.00f to Color(0xFF002E8A),
        start = Offset(s * 0.16f, s * 0.06f),
        end = Offset(s * 0.84f, s * 0.96f)
    )

    fun cylindricalSheenBrush(s: Float) = Brush.horizontalGradient(
        0.00f to Color(0x8C80DCFF),
        0.14f to Color(0x52FFFFFF),
        0.30f to Color(0x1438B6FF),
        0.55f to Color(0x2E0040B8),
        0.82f to Color(0x5C002270),
        0.93f to Color(0x6147B8FF),
        1.00f to Color(0xA699E5FF),
        startX = 0f,
        endX = s
    )

    fun lidRimBrush(s: Float) = Brush.horizontalGradient(
        0.00f to Color(0xFF66D4FF),
        0.18f to Color(0xFFB8EEFF),
        0.50f to Color(0xFF0073F0),
        0.82f to Color(0xFF004BC7),
        1.00f to Color(0xFF75DAFF),
        startX = 0f,
        endX = s
    )

    fun sapphireRecessBrush(s: Float) = Brush.linearGradient(
        0.00f to Color(0xFF001645),
        0.48f to Color(0xFF002B7A),
        1.00f to Color(0xFF0042B0),
        start = Offset(s * 0.2f, 0f),
        end = Offset(s * 0.8f, s)
    )

    fun sapphireBevelBrush(s: Float) = Brush.linearGradient(
        0.00f to Color(0xD9001030),
        0.45f to Color(0x401A8CFF),
        0.85f to Color(0xE070D6FF),
        1.00f to Color(0xFAC8F4FF),
        start = Offset.Zero,
        end = Offset(s, s)
    )

    fun specularStrokeBrush(s: Float) = Brush.linearGradient(
        0.00f to Color(0xF0FFFFFF),
        0.35f to Color(0x9980DCFF),
        0.70f to Color(0x260051D5),
        1.00f to Color(0xC766D4FF),
        start = Offset.Zero,
        end = Offset(s, s)
    )

    fun pearlBrush(s: Float) = Brush.linearGradient(
        0.00f to Color(0xFFFFFFFF),
        0.55f to Color(0xFFEDF7FF),
        1.00f to Color(0xFFB0DEFF),
        start = Offset(s * 0.15f, 0f),
        end = Offset(s * 0.85f, s)
    )

    fun skyBrush(s: Float) = Brush.linearGradient(
        0.00f to Color(0xFFE0F7FF),
        0.50f to Color(0xFF75D8FF),
        1.00f to Color(0xFF1A96FF),
        start = Offset.Zero,
        end = Offset(s, s)
    )

    fun opticLensBrush(s: Float) = Brush.radialGradient(
        0.00f to Color(0xFF5CE1FF),
        0.42f to Color(0xFF0048C8),
        1.00f to Color(0xFF001133),
        center = Offset(s * 0.42f, s * 0.40f),
        radius = s * 0.42f
    )

    fun handleBrush(s: Float) = Brush.linearGradient(
        0.00f to Color(0xFFA8ECFF),
        0.30f to Color(0xFF3DB8FF),
        0.68f to Color(0xFF007AFF),
        1.00f to Color(0xFF0047BF),
        start = Offset(s * 0.25f, s * 0.15f),
        end = Offset(s * 0.75f, s * 0.65f)
    )

    fun lidTopBrush(s: Float) = Brush.linearGradient(
        0.00f to Color(0xFF9BE8FF),
        0.28f to Color(0xFF42B9FF),
        0.62f to Color(0xFF007AFF),
        0.88f to Color(0xFF0051D5),
        1.00f to Color(0xFF003599),
        start = Offset(s * 0.20f, s * 0.16f),
        end = Offset(s * 0.84f, s * 0.72f)
    )

    fun secondaryLayerBrush(s: Float) = Brush.linearGradient(
        0.00f to Color(0xFF70D6FF),
        0.45f to Color(0xFF0A84FF),
        0.85f to Color(0xFF0049C2),
        1.00f to Color(0xFF00256E),
        start = Offset(s * 0.18f, s * 0.18f),
        end = Offset(s * 0.82f, s * 0.82f)
    )

    fun capsuleRecessBrush(top: Float, bottom: Float, s: Float) = Brush.linearGradient(
        0.00f to Color(0xFF001440),
        0.48f to Color(0xFF002975),
        1.00f to Color(0xFF003DA6),
        start = Offset(s * 0.40f, top),
        end = Offset(s * 0.60f, bottom)
    )

    fun capsuleBevelBrush(top: Float, bottom: Float, s: Float) = Brush.linearGradient(
        0.00f to Color(0xD9001238),
        0.45f to Color(0x401A8CFF),
        0.85f to Color(0xE070D6FF),
        1.00f to Color(0xF5B8F0FF),
        start = Offset(s * 0.35f, top),
        end = Offset(s * 0.70f, bottom)
    )
}

/**
 * 65% Rounded Squircle Badge (32% border-radius • Single Blue Gradient Background)
 * Matches the official Apple Settings Squircle spec from the Liquid Glass Design System.
 */
@Composable
fun AppleSettingsSquircleIcon(
    id: Apple3DGraphicId,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    hasBackground: Boolean = false,
    isOpen: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    if (!hasBackground) {
        Box(
            modifier = modifier
                .size(size)
                .then(
                    if (onClick != null) Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    ) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Apple3DGraphicIcon(
                id = id,
                isOpen = isOpen,
                modifier = Modifier.size(size)
            )
        }
        return
    }

    val squircleShape = RoundedCornerShape(percent = 32)
    Box(
        modifier = modifier
            .size(size)
            .clip(squircleShape)
            .background(
                Brush.verticalGradient(
                    0.00f to Color(0xFF4DA6FF),
                    0.52f to Color(0xFF0A84FF),
                    1.00f to Color(0xFF0051D5)
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.42f), squircleShape)
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // Subtle top specular sheen
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(0.84f)
                .fillMaxHeight(0.36f)
                .clip(RoundedCornerShape(bottomStartPercent = 50, bottomEndPercent = 50))
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.White.copy(alpha = 0.30f),
                        1.0f to Color.Transparent
                    )
                )
        )
        Apple3DGraphicIcon(
            id = id,
            isOpen = isOpen,
            modifier = Modifier.size(size * 0.68f)
        )
    }
}

/**
 * Vector Icon for Settings/Drawer Options
 * By default: Clean, thin icon without background box (Matching Liquid Glass Design System Section 3)
 */
@Composable
fun AppleSettingsSquircleVectorIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    hasBackground: Boolean = false,
    iconTint: Color = Color(0xFF0A84FF),
    onClick: (() -> Unit)? = null
) {
    if (!hasBackground) {
        Box(
            modifier = modifier
                .size(size)
                .then(
                    if (onClick != null) Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    ) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(size)
            )
        }
        return
    }

    val squircleShape = RoundedCornerShape(percent = 32)
    Box(
        modifier = modifier
            .size(size)
            .clip(squircleShape)
            .background(
                Brush.verticalGradient(
                    0.00f to Color(0xFF4DA6FF),
                    0.52f to Color(0xFF0A84FF),
                    1.00f to Color(0xFF0051D5)
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.42f), squircleShape)
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // Subtle top specular sheen
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(0.84f)
                .fillMaxHeight(0.36f)
                .clip(RoundedCornerShape(bottomStartPercent = 50, bottomEndPercent = 50))
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.White.copy(alpha = 0.30f),
                        1.0f to Color.Transparent
                    )
                )
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(size * 0.58f)
        )
    }
}

@Composable
fun AppleHigDialogCard(
    graphicId: Apple3DGraphicId,
    title: String = graphicId.dialogTitle,
    description: String = graphicId.dialogDesc,
    cancelText: String = "Cancel",
    confirmText: String = "Confirm",
    onCancel: () -> Unit = {},
    onConfirm: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isOpen by remember { mutableStateOf(false) }
    val cardBg = Color.White
    val borderCol = Color(0xFFE2E8F0)
    val titleCol = Color(0xFF0F172A)
    val descCol = Color(0xFF64748B)
    val dividerCol = Color(0xFFF1F5F9)
    val bottomBarBg = Color(0xFFF8FAFD)
    val cancelTextCol = Color(0xFF0F172A)

    Surface(
        modifier = modifier.widthIn(max = 320.dp),
        shape = RoundedCornerShape(24.dp),
        color = cardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderCol)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Apple3DGraphicIcon(
                    id = graphicId,
                    isOpen = isOpen,
                    modifier = Modifier.size(72.dp),
                    onClick = { isOpen = !isOpen }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = title,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = titleCol,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = description,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    color = descCol,
                    textAlign = TextAlign.Center
                )
            }
            HorizontalDivider(color = dividerCol, thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bottomBarBg)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                com.chroma.studio.ui.components.LiquidGlassSecondaryButton(
                    onClick = onCancel,
                    isSmall = true,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(cancelText, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = cancelTextCol)
                }
                com.chroma.studio.ui.components.LiquidGlassPrimaryButton(
                    onClick = {
                        isOpen = !isOpen
                        onConfirm()
                    },
                    isSmall = true,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(confirmText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun Apple3DGraphicIcon(
    id: Apple3DGraphicId,
    modifier: Modifier = Modifier.size(112.dp),
    isOpen: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    // Articulated spring animation progress (0f = resting, 1f = articulated open/rotated/lifted)
    val progress by animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.52f,
            stiffness = 380f
        ),
        label = "Apple3DSpringProgress"
    )

    Canvas(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ) else Modifier
        )
    ) {
        val scaleFactor = size.minDimension / 100f
        scale(scaleFactor, scaleFactor, pivot = Offset.Zero) {
            drawApple3DGraphicNormalized(id = id, p = progress)
        }
    }
}

private fun DrawScope.drawSculptedSquircleBase(
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    rx: Float
) {
    val s = 100f
    // 1. Extruded Base Shadow Plinth
    drawRoundRect(
        color = AppleHigShaders.PlinthShadow,
        topLeft = Offset(x, y + 2.4f),
        size = Size(w, h),
        cornerRadius = CornerRadius(rx, rx)
    )
    // 2. Main Cupertino Blue Body
    drawRoundRect(
        brush = AppleHigShaders.bodyBrush(s),
        topLeft = Offset(x, y),
        size = Size(w, h),
        cornerRadius = CornerRadius(rx, rx)
    )
    // 3. Cylindrical Metallic Sheen
    drawRoundRect(
        brush = AppleHigShaders.cylindricalSheenBrush(s),
        topLeft = Offset(x, y),
        size = Size(w, h),
        cornerRadius = CornerRadius(rx, rx)
    )
    // 4. Outer Specular Bevel Rim
    drawRoundRect(
        brush = AppleHigShaders.specularStrokeBrush(s),
        topLeft = Offset(x, y),
        size = Size(w, h),
        cornerRadius = CornerRadius(rx, rx),
        style = Stroke(width = 2.2f)
    )
}

private fun DrawScope.drawSculptedCircleBase(cx: Float, cy: Float, r: Float) {
    val s = 100f
    drawCircle(color = AppleHigShaders.PlinthShadow, radius = r, center = Offset(cx, cy + 2.2f))
    drawCircle(brush = AppleHigShaders.bodyBrush(s), radius = r, center = Offset(cx, cy))
    drawCircle(brush = AppleHigShaders.cylindricalSheenBrush(s), radius = r, center = Offset(cx, cy))
    drawCircle(
        brush = AppleHigShaders.specularStrokeBrush(s),
        radius = r,
        center = Offset(cx, cy),
        style = Stroke(width = 2.2f)
    )
}

private fun DrawScope.drawSapphireRecessRect(x: Float, y: Float, w: Float, h: Float, rx: Float) {
    val s = 100f
    drawRoundRect(
        brush = AppleHigShaders.sapphireRecessBrush(s),
        topLeft = Offset(x, y),
        size = Size(w, h),
        cornerRadius = CornerRadius(rx, rx)
    )
    drawRoundRect(
        brush = AppleHigShaders.sapphireBevelBrush(s),
        topLeft = Offset(x, y),
        size = Size(w, h),
        cornerRadius = CornerRadius(rx, rx),
        style = Stroke(width = 2f)
    )
}

private fun DrawScope.drawSapphireRecessCircle(cx: Float, cy: Float, r: Float, isOptic: Boolean = false) {
    val s = 100f
    drawCircle(
        brush = if (isOptic) AppleHigShaders.opticLensBrush(s) else AppleHigShaders.sapphireRecessBrush(s),
        radius = r,
        center = Offset(cx, cy)
    )
    drawCircle(
        brush = AppleHigShaders.sapphireBevelBrush(s),
        radius = r,
        center = Offset(cx, cy),
        style = Stroke(width = 2.2f)
    )
}

private fun DrawScope.drawApple3DGraphicNormalized(id: Apple3DGraphicId, p: Float) {
    val s = 100f
    val pearl = AppleHigShaders.pearlBrush(s)
    val sky = AppleHigShaders.skyBrush(s)

    when (id) {
        Apple3DGraphicId.TRASH_BIN -> {
            val bodyPath = Path().apply {
                moveTo(22f, 34f)
                lineTo(78f, 34f)
                lineTo(73f, 82f)
                cubicTo(72f, 87f, 68f, 90f, 62f, 90f)
                lineTo(38f, 90f)
                cubicTo(32f, 90f, 28f, 87f, 27f, 82f)
                close()
            }
            drawPath(bodyPath, AppleHigShaders.bodyBrush(s))
            drawPath(bodyPath, AppleHigShaders.cylindricalSheenBrush(s))
            drawPath(bodyPath, AppleHigShaders.specularStrokeBrush(s), style = Stroke(width = 2f))

            drawSapphireRecessRect(33f, 44f, 7.5f, 37f, 3.75f)
            drawSapphireRecessRect(46.25f, 44f, 7.5f, 37f, 3.75f)
            drawSapphireRecessRect(59.5f, 44f, 7.5f, 37f, 3.75f)

            withTransform({
                rotate(degrees = -28f * p, pivot = Offset(22f, 32f))
                translate(left = -2f * p, top = -4f * p)
            }) {
                drawRoundRect(
                    brush = AppleHigShaders.lidRimBrush(s),
                    topLeft = Offset(36f, 13f),
                    size = Size(28f, 12f),
                    cornerRadius = CornerRadius(5f, 5f),
                    style = Stroke(width = 4.2f)
                )
                drawRoundRect(
                    brush = AppleHigShaders.lidRimBrush(s),
                    topLeft = Offset(19f, 22f),
                    size = Size(62f, 12f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.85f),
                    start = Offset(24f, 26f),
                    end = Offset(76f, 26f),
                    strokeWidth = 2.2f,
                    cap = StrokeCap.Round
                )
            }
        }

        Apple3DGraphicId.SMART_FOLDER -> {
            drawSapphireRecessRect(14f, 16f, 72f, 66f, 12f)
            translate(top = -6f * p) {
                drawRoundRect(pearl, Offset(20f, 23f), Size(60f, 38f), CornerRadius(6f, 6f))
                drawRoundRect(Color(0xFF007AFF), Offset(27f, 30f), Size(22f, 4f), CornerRadius(2f, 2f))
                drawRoundRect(Color(0xFF70D6FF), Offset(27f, 37f), Size(38f, 3.2f), CornerRadius(1.6f, 1.6f))
            }
            drawSculptedSquircleBase(11f, 32f, 78f, 52f, 12f)
            drawSapphireRecessRect(36f, 54f, 28f, 9f, 4.5f)
        }

        Apple3DGraphicId.NVME_DRIVE -> {
            drawSculptedSquircleBase(14f, 12f, 72f, 71f, 16f)
            drawSapphireRecessCircle(50f, 41.5f, 21f)
            rotate(degrees = 45f * p, pivot = Offset(50f, 41.5f)) {
                drawCircle(Color(0xFF80DCFF), radius = 13.5f, center = Offset(50f, 41.5f), style = Stroke(width = 3.2f))
                drawCircle(pearl, radius = 7f, center = Offset(50f, 41.5f))
                drawCircle(Color(0xFF0051D5), radius = 2.6f, center = Offset(50f, 41.5f))
            }
            drawSapphireRecessRect(24f, 68f, 52f, 8f, 4f)
            drawRoundRect(pearl, Offset(29f, 70.5f), Size(24f, 3f), CornerRadius(1.5f, 1.5f))
        }

        Apple3DGraphicId.ARCHIVE_BOX -> {
            drawSculptedSquircleBase(16f, 32f, 68f, 54f, 12f)
            drawSapphireRecessRect(35f, 48f, 30f, 11f, 5.5f)
            drawRoundRect(pearl, Offset(44f, 65f), Size(12f, 15f), CornerRadius(3.5f, 3.5f))
            translate(top = -6.5f * p) {
                drawRoundRect(AppleHigShaders.lidRimBrush(s), Offset(11f, 18f), Size(78f, 16f), CornerRadius(6.5f, 6.5f))
                drawRoundRect(pearl, Offset(43f, 18f), Size(14f, 16f), CornerRadius(2f, 2f))
            }
        }

        Apple3DGraphicId.PROTECTION_SHIELD -> {
            val shield = Path().apply {
                moveTo(50f, 8f)
                lineTo(84f, 20.5f)
                cubicTo(87.5f, 23f, 87.5f, 57.5f, 50f, 91.5f)
                cubicTo(12.5f, 57.5f, 12.5f, 23f, 16f, 20.5f)
                close()
            }
            drawPath(shield, AppleHigShaders.bodyBrush(s))
            drawPath(shield, AppleHigShaders.cylindricalSheenBrush(s))
            drawPath(shield, AppleHigShaders.specularStrokeBrush(s), style = Stroke(2.2f))

            val inner = Path().apply {
                moveTo(50f, 19.5f)
                lineTo(74.5f, 28.5f)
                cubicTo(74.5f, 50.5f, 63.5f, 67.5f, 50f, 77.5f)
                cubicTo(36.5f, 67.5f, 25.5f, 50.5f, 25.5f, 28.5f)
                close()
            }
            drawPath(inner, AppleHigShaders.sapphireRecessBrush(s))
            drawPath(inner, AppleHigShaders.sapphireBevelBrush(s), style = Stroke(2f))

            rotate(degrees = -8f * p, pivot = Offset(50f, 50f)) {
                val check = Path().apply {
                    moveTo(37.5f, 47.5f)
                    lineTo(46.5f, 56.5f)
                    lineTo(63.5f, 37.5f)
                }
                drawPath(check, pearl, style = Stroke(width = 6.6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }

        Apple3DGraphicId.BIOMETRIC_LOCK -> {
            withTransform({
                translate(top = -6f * p)
                rotate(degrees = 14f * p, pivot = Offset(68f, 42f))
            }) {
                drawRoundRect(
                    brush = pearl,
                    topLeft = Offset(31f, 10f),
                    size = Size(38f, 38f),
                    cornerRadius = CornerRadius(19f, 19f),
                    style = Stroke(width = 9.5f)
                )
            }
            drawSculptedSquircleBase(16f, 38.5f, 68f, 48f, 15f)
            drawSapphireRecessCircle(50f, 62.5f, 14f)
            drawCircle(pearl, radius = 4.5f, center = Offset(50f, 59.2f))
            drawRoundRect(pearl, Offset(46.8f, 60.5f), Size(6.4f, 9.5f), CornerRadius(2f, 2f))
        }

        Apple3DGraphicId.HARDWARE_PASSKEY -> {
            rotate(degrees = -8f * p, pivot = Offset(50f, 50f)) {
                drawRoundRect(pearl, Offset(44f, 42f), Size(45f, 14f), CornerRadius(4f, 4f))
                drawSapphireRecessRect(49f, 45f, 35f, 3.6f, 1.8f)
                drawRoundRect(AppleHigShaders.lidRimBrush(s), Offset(44f, 39.5f), Size(6.5f, 19f), CornerRadius(3.2f, 3.2f))
                drawSculptedSquircleBase(9f, 25f, 40f, 45f, 16f)
                drawSapphireRecessCircle(29f, 47.5f, 12f)
                drawCircle(pearl, radius = 3.6f, center = Offset(29f, 47.5f))
            }
        }

        Apple3DGraphicId.BIOMETRIC_OPTIC -> {
            drawSculptedCircleBase(50f, 50f, 37f)
            drawSapphireRecessCircle(50f, 50f, 27.5f, isOptic = true)
            rotate(degrees = 45f * p, pivot = Offset(50f, 50f)) {
                drawCircle(Color(0xFF80DCFF), radius = 19f, center = Offset(50f, 50f), style = Stroke(width = 3.4f))
                drawCircle(Color.White, radius = 13f, center = Offset(50f, 50f), style = Stroke(width = 3.4f))
                drawCircle(Color(0xFF80DCFF), radius = 7.5f, center = Offset(50f, 50f), style = Stroke(width = 3.4f))
                drawCircle(pearl, radius = 3.4f, center = Offset(50f, 50f))
            }
        }

        Apple3DGraphicId.NEURAL_CPU -> {
            val pinOffsets = listOf(29f, 46.75f, 64.5f)
            pinOffsets.forEach { pos ->
                drawRoundRect(pearl, Offset(pos, 8f), Size(6.5f, 13f), CornerRadius(3.2f, 3.2f))
                drawRoundRect(pearl, Offset(pos, 79f), Size(6.5f, 13f), CornerRadius(3.2f, 3.2f))
                drawRoundRect(pearl, Offset(8f, pos), Size(13f, 6.5f), CornerRadius(3.2f, 3.2f))
                drawRoundRect(pearl, Offset(79f, pos), Size(13f, 6.5f), CornerRadius(3.2f, 3.2f))
            }
            drawSculptedSquircleBase(17f, 17f, 66f, 66f, 16f)
            drawSapphireRecessRect(26f, 26f, 48f, 48f, 11f)
            rotate(degrees = -8f * p, pivot = Offset(50f, 50f)) {
                drawRoundRect(pearl, Offset(33.5f, 33.5f), Size(33f, 33f), CornerRadius(7.5f, 7.5f))
                drawRoundRect(AppleHigShaders.bodyBrush(s), Offset(38f, 38f), Size(10.5f, 10.5f), CornerRadius(2.8f, 2.8f))
                drawRoundRect(sky, Offset(51.5f, 38f), Size(10.5f, 10.5f), CornerRadius(2.8f, 2.8f))
                drawRoundRect(sky, Offset(38f, 51.5f), Size(10.5f, 10.5f), CornerRadius(2.8f, 2.8f))
                drawRoundRect(AppleHigShaders.bodyBrush(s), Offset(51.5f, 51.5f), Size(10.5f, 10.5f), CornerRadius(2.8f, 2.8f))
            }
        }

        Apple3DGraphicId.CLOUD_SYNC -> {
            drawSculptedSquircleBase(10f, 36f, 80f, 40f, 20f)
            drawCircle(AppleHigShaders.bodyBrush(s), radius = 22f, center = Offset(46f, 38f))
            drawCircle(AppleHigShaders.bodyBrush(s), radius = 16f, center = Offset(66f, 46f))
            drawSapphireRecessCircle(49f, 52.5f, 17f)
            translate(top = -5f * p) {
                val arrow = Path().apply {
                    moveTo(49f, 39.5f)
                    lineTo(60f, 51.5f)
                    lineTo(53.2f, 51.5f)
                    lineTo(53.2f, 64f)
                    lineTo(44.8f, 64f)
                    lineTo(44.8f, 51.5f)
                    lineTo(38f, 51.5f)
                    close()
                }
                drawPath(arrow, pearl)
            }
        }

        Apple3DGraphicId.SPATIAL_BELL -> {
            rotate(degrees = -11f * p, pivot = Offset(50f, 22f)) {
                drawRoundRect(pearl, Offset(43f, 9f), Size(14f, 13f), CornerRadius(6.5f, 6.5f))
                drawCircle(pearl, radius = 9.5f, center = Offset(50f, 78f))
                drawSculptedSquircleBase(18f, 20f, 64f, 55f, 24f)
                drawSapphireRecessRect(31f, 57.5f, 38f, 7f, 3.5f)
            }
        }

        Apple3DGraphicId.MAGSAFE_BATTERY -> {
            drawRoundRect(pearl, Offset(80f, 38.5f), Size(11f, 25f), CornerRadius(4.5f, 4.5f))
            drawSculptedSquircleBase(8f, 19f, 75f, 60f, 16f)
            drawSapphireRecessRect(16.5f, 27.5f, 58f, 43f, 10f)
            translate(top = -4f * p) {
                drawRoundRect(pearl, Offset(21.5f, 32.5f), Size(13f, 33f), CornerRadius(4.5f, 4.5f))
                drawRoundRect(pearl, Offset(38f, 32.5f), Size(13f, 33f), CornerRadius(4.5f, 4.5f))
                drawRoundRect(sky, Offset(54.5f, 32.5f), Size(13f, 33f), CornerRadius(4.5f, 4.5f))
            }
        }

        Apple3DGraphicId.PRO_CAMERA -> {
            drawRoundRect(pearl, Offset(19f, 18.5f), Size(14f, 8f), CornerRadius(3.5f, 3.5f))
            drawSculptedSquircleBase(11f, 25f, 78f, 56f, 16f)
            rotate(degrees = -8f * p, pivot = Offset(50f, 53f)) {
                drawCircle(pearl, radius = 22f, center = Offset(50f, 53f))
                drawSapphireRecessCircle(50f, 53f, 16.8f, isOptic = true)
                drawCircle(Color.White.copy(alpha = 0.9f), radius = 5f, center = Offset(44f, 46.8f))
            }
            drawCircle(pearl, radius = 4.2f, center = Offset(76f, 36f))
        }

        Apple3DGraphicId.PHOTO_GALLERY -> {
            rotate(degrees = -7f - 5f * p, pivot = Offset(50f, 50f)) {
                drawRoundRect(
                    brush = AppleHigShaders.handleBrush(s),
                    topLeft = Offset(19f, 19f),
                    size = Size(61f, 61f),
                    cornerRadius = CornerRadius(11f, 11f)
                )
                drawRoundRect(
                    brush = Brush.linearGradient(listOf(Color(0xFF00194A), Color(0xFF00388A))),
                    topLeft = Offset(24f, 24f),
                    size = Size(51f, 51f),
                    cornerRadius = CornerRadius(7.2f, 7.2f)
                )
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.9f), Color(0xFF70D6FF).copy(alpha = 0.35f), Color.Transparent)),
                    radius = 4.5f,
                    center = Offset(36f, 34.5f)
                )
                drawRoundRect(
                    brush = AppleHigShaders.specularStrokeBrush(s),
                    topLeft = Offset(19f, 19f),
                    size = Size(61f, 61f),
                    cornerRadius = CornerRadius(11f, 11f),
                    style = Stroke(width = 0.28f)
                )
            }
            drawSculptedSquircleBase(17f, 17f, 66f, 66f, 11.8f)
            val vpPath = Path().apply {
                addRoundRect(RoundRect(Rect(23.6f, 23.6f, 76.4f, 76.4f), CornerRadius(7.8f, 7.8f)))
            }
            drawRoundRect(
                brush = AppleHigShaders.capsuleRecessBrush(23.6f, 76.4f, s),
                topLeft = Offset(23.6f, 23.6f),
                size = Size(52.8f, 52.8f),
                cornerRadius = CornerRadius(7.8f, 7.8f)
            )
            clipPath(vpPath) {
                drawRect(
                    brush = Brush.verticalGradient(
                        0.00f to Color(0xFF00194A),
                        0.32f to Color(0xFF0049BA),
                        0.65f to Color(0xFF0A84FF),
                        0.88f to Color(0xFF5AC8FA),
                        1.00f to Color(0xFFB8EEFF),
                        startY = 23.6f,
                        endY = 76.4f
                    ),
                    topLeft = Offset(23.6f, 23.6f),
                    size = Size(52.8f, 52.8f)
                )
                translate(top = -1.4f * p) {
                    val sunCenter = Offset(37.2f, 36.5f - 1.8f * p)
                    drawCircle(Color(0x3566D4FF), radius = 8.8f, center = sunCenter)
                    drawCircle(Color(0x55FFE885), radius = 6.8f, center = sunCenter)
                    drawCircle(pearl, radius = 5.6f, center = sunCenter)
                    drawCircle(Color.White.copy(alpha = 0.92f), radius = 5.6f, center = sunCenter, style = Stroke(0.32f))

                    // Secondary Left Mountain Peak
                    val backPeak = Path().apply {
                        moveTo(22f, 77f)
                        lineTo(37.8f, 52.6f)
                        cubicTo(40.2f, 49.6f, 44.2f, 49.6f, 46.6f, 52.6f)
                        lineTo(64f, 77f)
                        close()
                    }
                    drawPath(backPeak, brush = AppleHigShaders.secondaryLayerBrush(s))

                    // Primary Hero Right Mountain Peak
                    val frontPeak = Path().apply {
                        moveTo(29f, 77f)
                        lineTo(57.4f, 46.2f)
                        cubicTo(60.6f, 42.8f, 65.6f, 42.8f, 68.8f, 46.2f)
                        lineTo(78f, 56f)
                        lineTo(78f, 77f)
                        close()
                    }
                    drawPath(frontPeak, brush = AppleHigShaders.lidTopBrush(s))

                    val frontCrest = Path().apply {
                        moveTo(30f, 76f)
                        lineTo(57.6f, 46.4f)
                        cubicTo(60.6f, 43.2f, 65.6f, 43.2f, 68.6f, 46.4f)
                        lineTo(77.5f, 55.5f)
                    }
                    drawPath(frontCrest, Color.White.copy(alpha = 0.94f), style = Stroke(0.52f, cap = StrokeCap.Round))
                }
                val glassSheen = Path().apply {
                    moveTo(23.6f, 23.6f)
                    lineTo(46f, 23.6f)
                    lineTo(23.6f, 46f)
                    close()
                }
                drawPath(glassSheen, Color.White.copy(alpha = 0.18f))
            }
            drawRoundRect(
                brush = AppleHigShaders.capsuleBevelBrush(23.6f, 76.4f, s),
                topLeft = Offset(23.6f, 23.6f),
                size = Size(52.8f, 52.8f),
                cornerRadius = CornerRadius(7.8f, 7.8f),
                style = Stroke(0.52f)
            )
            drawRoundRect(
                brush = AppleHigShaders.specularStrokeBrush(s),
                topLeft = Offset(17f, 17f),
                size = Size(66f, 66f),
                cornerRadius = CornerRadius(11.8f, 11.8f),
                style = Stroke(0.36f)
            )
        }

        Apple3DGraphicId.SPATIAL_MAIL -> {
            drawSapphireRecessRect(11f, 22.5f, 78f, 58f, 14f)
            translate(top = -6f * p) {
                drawRoundRect(pearl, Offset(20f, 16f), Size(60f, 44f), CornerRadius(8f, 8f))
                drawRoundRect(Color(0xFF007AFF), Offset(28f, 24f), Size(24f, 4.2f), CornerRadius(2.1f, 2.1f))
                drawRoundRect(Color(0xFF70D6FF), Offset(28f, 32f), Size(42f, 3.6f), CornerRadius(1.8f, 1.8f))
            }
            drawSculptedSquircleBase(11f, 42f, 78f, 38.5f, 14f)
        }

        Apple3DGraphicId.TELEMETRY_CHART -> {
            drawRoundRect(AppleHigShaders.lidRimBrush(s), Offset(12f, 74f), Size(76f, 10.5f), CornerRadius(5.2f, 5.2f))
            drawSculptedSquircleBase(18f, 42f, 16.5f, 29f, 6.5f)
            translate(top = -6.5f * p) {
                drawRoundRect(pearl, Offset(41.75f, 16f), Size(16.5f, 55f), CornerRadius(6.5f, 6.5f))
            }
            drawSculptedSquircleBase(65.5f, 29f, 16.5f, 42f, 6.5f)
        }

        Apple3DGraphicId.WIFI_RADAR -> {
            drawSculptedSquircleBase(13f, 13f, 74f, 72f, 20f)
            drawSapphireRecessCircle(50f, 49f, 27f)
            translate(top = -4f * p) {
                drawArc(Color.White, startAngle = 220f, sweepAngle = 100f, useCenter = false, topLeft = Offset(26f, 28f), size = Size(48f, 48f), style = Stroke(width = 5f, cap = StrokeCap.Round))
                drawArc(Color(0xFF80DCFF), startAngle = 220f, sweepAngle = 100f, useCenter = false, topLeft = Offset(34f, 38f), size = Size(32f, 32f), style = Stroke(width = 5f, cap = StrokeCap.Round))
                drawCircle(pearl, radius = 5f, center = Offset(50f, 64f))
            }
        }

        Apple3DGraphicId.SPATIAL_HEADPHONES -> {
            rotate(degrees = -8f * p, pivot = Offset(50f, 50f)) {
                drawArc(AppleHigShaders.lidRimBrush(s), startAngle = 180f, sweepAngle = 180f, useCenter = false, topLeft = Offset(21f, 14f), size = Size(58f, 58f), style = Stroke(width = 9.5f, cap = StrokeCap.Round))
                drawRoundRect(pearl, Offset(24f, 47f), Size(11f, 29f), CornerRadius(5.5f, 5.5f))
                drawRoundRect(pearl, Offset(65f, 47f), Size(11f, 29f), CornerRadius(5.5f, 5.5f))
                drawSculptedSquircleBase(11f, 44f, 20f, 36f, 10f)
                drawSculptedSquircleBase(69f, 44f, 20f, 36f, 10f)
            }
        }

        Apple3DGraphicId.SYSTEM_GEAR -> {
            rotate(degrees = 45f * p, pivot = Offset(50f, 50f)) {
                for (angle in listOf(0f, 45f, 90f, 135f)) {
                    rotate(degrees = angle, pivot = Offset(50f, 50f)) {
                        drawRoundRect(AppleHigShaders.lidRimBrush(s), Offset(42f, 10f), Size(16f, 80f), CornerRadius(6f, 6f))
                    }
                }
                drawSculptedCircleBase(50f, 50f, 31f)
                drawSapphireRecessCircle(50f, 50f, 21f)
                drawCircle(pearl, radius = 12.5f, center = Offset(50f, 50f))
                drawCircle(AppleHigShaders.bodyBrush(s), radius = 5.5f, center = Offset(50f, 50f))
            }
        }

        Apple3DGraphicId.NETWORK_GLOBE -> {
            drawSculptedCircleBase(50f, 50f, 36f)
            drawSapphireRecessCircle(50f, 50f, 28f, isOptic = true)
            rotate(degrees = -8f * p, pivot = Offset(50f, 50f)) {
                drawOval(Color.White, topLeft = Offset(36f, 22.5f), size = Size(28f, 55f), style = Stroke(width = 3.2f))
                drawOval(Color(0xFF80DCFF), topLeft = Offset(22.5f, 38f), size = Size(55f, 24f), style = Stroke(width = 3.2f))
                drawCircle(Color.White, radius = 3.6f, center = Offset(36f, 41f))
                drawCircle(Color.White, radius = 3.8f, center = Offset(64f, 50f))
            }
        }

        Apple3DGraphicId.STUDIO_MIC -> {
            drawArc(pearl, startAngle = 0f, sweepAngle = 180f, useCenter = false, topLeft = Offset(26f, 30f), size = Size(48f, 44f), style = Stroke(width = 6.5f, cap = StrokeCap.Round))
            drawRoundRect(sky, Offset(46.5f, 73f), Size(7f, 12f), CornerRadius(2f, 2f))
            drawRoundRect(AppleHigShaders.lidRimBrush(s), Offset(33f, 82f), Size(34f, 7.5f), CornerRadius(3.75f, 3.75f))
            translate(top = -5f * p) {
                drawSculptedSquircleBase(33f, 9f, 34f, 54f, 17f)
                drawSapphireRecessRect(40f, 18f, 20f, 5f, 2.5f)
                drawSapphireRecessRect(39f, 26.5f, 22f, 5f, 2.5f)
                drawSapphireRecessRect(40f, 35f, 20f, 5f, 2.5f)
                drawRoundRect(pearl, Offset(33f, 44.5f), Size(34f, 4.5f), CornerRadius(2f, 2f))
            }
        }

        Apple3DGraphicId.SPATIAL_COMPASS -> {
            drawSculptedCircleBase(50f, 50f, 37f)
            drawSapphireRecessCircle(50f, 50f, 28f)
            rotate(degrees = -32f * p, pivot = Offset(50f, 50f)) {
                val north = Path().apply { moveTo(44f, 50f); lineTo(56f, 50f); lineTo(50f, 30f); close() }
                val south = Path().apply { moveTo(44f, 50f); lineTo(56f, 50f); lineTo(50f, 70f); close() }
                drawPath(south, sky)
                drawPath(north, pearl)
                drawCircle(AppleHigShaders.bodyBrush(s), radius = 4.5f, center = Offset(50f, 50f))
            }
        }

        Apple3DGraphicId.SMART_HOME -> {
            drawSculptedSquircleBase(20f, 36f, 60f, 49f, 12f)
            drawSapphireRecessRect(38.5f, 51f, 23f, 34f, 11.5f)
            drawCircle(pearl, radius = 5f, center = Offset(50f, 63f))
            translate(top = -6f * p) {
                val roof = Path().apply {
                    moveTo(12f, 46f)
                    lineTo(50f, 14f)
                    lineTo(88f, 46f)
                    lineTo(80f, 51f)
                    lineTo(50f, 25.5f)
                    lineTo(20f, 51f)
                    close()
                }
                drawPath(roof, pearl)
            }
        }

        Apple3DGraphicId.DEV_TERMINAL -> {
            drawSculptedSquircleBase(12f, 16f, 76f, 65f, 16f)
            drawSapphireRecessRect(19f, 31f, 62f, 43f, 10f)
            rotate(degrees = -6f * p, pivot = Offset(50f, 50f)) {
                val chevron = Path().apply {
                    moveTo(29f, 42.5f)
                    lineTo(41.5f, 51.5f)
                    lineTo(29f, 60.5f)
                }
                drawPath(chevron, pearl, style = Stroke(width = 5.6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawRoundRect(sky, Offset(47f, 57.5f), Size(18f, 5.5f), CornerRadius(2.75f, 2.75f))
            }
        }

        Apple3DGraphicId.BLUETOOTH_RUNE -> {
            drawSculptedSquircleBase(16f, 11.5f, 68f, 74f, 26f)
            drawSapphireRecessRect(25f, 20.5f, 50f, 56f, 19f)
            rotate(degrees = -8f * p, pivot = Offset(50f, 48.5f)) {
                val rune = Path().apply {
                    moveTo(36f, 35f)
                    lineTo(62f, 59f)
                    lineTo(49f, 70f)
                    lineTo(49f, 27f)
                    lineTo(62f, 38f)
                    lineTo(36f, 62f)
                }
                drawPath(rune, pearl, style = Stroke(width = 5.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawCircle(Color(0xFF80DCFF), radius = 3.2f, center = Offset(31.5f, 48.5f))
                drawCircle(Color(0xFF80DCFF), radius = 3.2f, center = Offset(66.5f, 48.5f))
            }
        }

        Apple3DGraphicId.DISPLAY_SUN -> {
            drawSculptedSquircleBase(13f, 13f, 74f, 72f, 20f)
            drawSapphireRecessCircle(50f, 49f, 27f)
            rotate(degrees = 45f * p, pivot = Offset(50f, 49f)) {
                for (a in 0 until 8) {
                    rotate(degrees = a * 45f, pivot = Offset(50f, 49f)) {
                        drawRoundRect(sky, Offset(47.5f, 25f), Size(5f, 8f), CornerRadius(2.5f, 2.5f))
                    }
                }
                drawCircle(pearl, radius = 11.5f, center = Offset(50f, 49f))
            }
        }

        Apple3DGraphicId.SPATIAL_SPEAKER -> {
            drawSculptedSquircleBase(13f, 13f, 74f, 72f, 20f)
            drawSapphireRecessCircle(50f, 49f, 27.5f)
            rotate(degrees = -6f * p, pivot = Offset(50f, 49f)) {
                val cone = Path().apply {
                    moveTo(26f, 42.5f)
                    lineTo(35.5f, 42.5f)
                    lineTo(47f, 32f)
                    lineTo(47f, 66f)
                    lineTo(35.5f, 55.5f)
                    lineTo(26f, 55.5f)
                    close()
                }
                drawPath(cone, pearl)
                drawArc(Color(0xFF80DCFF), -45f, 90f, false, Offset(44f, 39f), Size(20f, 20f), style = Stroke(4.2f, cap = StrokeCap.Round))
                drawArc(Color.White, -45f, 90f, false, Offset(48f, 33f), size = Size(32f, 32f), style = Stroke(4.2f, cap = StrokeCap.Round))
            }
        }

        Apple3DGraphicId.LOCATION_PIN -> {
            translate(top = -6f * p) {
                val pin = Path().apply {
                    moveTo(50f, 10f)
                    cubicTo(31.5f, 10f, 18f, 23.5f, 18f, 41.5f)
                    cubicTo(18f, 60.5f, 40f, 78f, 50f, 86f)
                    cubicTo(60f, 78f, 82f, 60.5f, 82f, 41.5f)
                    cubicTo(82f, 23.5f, 68.5f, 10f, 50f, 10f)
                    close()
                }
                drawPath(pin, AppleHigShaders.bodyBrush(s))
                drawPath(pin, AppleHigShaders.cylindricalSheenBrush(s))
                drawPath(pin, AppleHigShaders.specularStrokeBrush(s), style = Stroke(2.2f))
                drawSapphireRecessCircle(50f, 41f, 17f)
                drawCircle(pearl, radius = 8.5f, center = Offset(50f, 41f))
            }
        }

        Apple3DGraphicId.AIRTAG_TRACKER -> {
            drawSculptedCircleBase(50f, 50f, 37f)
            drawSapphireRecessCircle(50f, 50f, 27f)
            rotate(degrees = 45f * p, pivot = Offset(50f, 50f)) {
                drawCircle(Color(0xFF80DCFF), radius = 21f, center = Offset(50f, 50f), style = Stroke(width = 2.5f))
                drawCircle(pearl, radius = 14.5f, center = Offset(50f, 50f))
                val arrow = Path().apply {
                    moveTo(50f, 42f); lineTo(55.5f, 54.5f); lineTo(50f, 51.8f); lineTo(44.5f, 54.5f); close()
                }
                drawPath(arrow, AppleHigShaders.bodyBrush(s))
            }
        }

        Apple3DGraphicId.ROCKET_BOOSTER -> {
            translate(top = -6.5f * p) {
                drawRoundRect(AppleHigShaders.lidRimBrush(s), Offset(18f, 56f), Size(64f, 16f), CornerRadius(6f, 6f))
                drawCircle(sky, radius = 9f, center = Offset(50f, 78f))
                val hull = Path().apply {
                    moveTo(50f, 9f)
                    cubicTo(36f, 22f, 31f, 40f, 33f, 72f)
                    lineTo(67f, 72f)
                    cubicTo(69f, 40f, 64f, 22f, 50f, 9f)
                    close()
                }
                drawPath(hull, AppleHigShaders.bodyBrush(s))
                drawPath(hull, AppleHigShaders.cylindricalSheenBrush(s))
                drawPath(hull, AppleHigShaders.specularStrokeBrush(s), style = Stroke(2.2f))
                drawSapphireRecessCircle(50f, 42f, 11.5f)
                drawCircle(pearl, radius = 6.5f, center = Offset(50f, 42f))
            }
        }

        Apple3DGraphicId.WORKS -> {
            drawSculptedSquircleBase(13f, 26f, 74f, 58f, 14f)
            drawSapphireRecessRect(19f, 32f, 62f, 46f, 10f)
            translate(top = -5f * p) {
                drawRoundRect(sky, Offset(26f, 40f), Size(28f, 5.5f), CornerRadius(2.75f, 2.75f))
                drawRoundRect(Brush.linearGradient(listOf(Color(0xFF80DCFF), Color(0xFF38B6FF))), Offset(26f, 48.5f), Size(38f, 4.5f), CornerRadius(2.25f, 2.25f))
                drawRoundRect(pearl, Offset(26f, 56f), Size(20f, 4.5f), CornerRadius(2.25f, 2.25f))
                drawLine(Color.White.copy(alpha = 0.7f), Offset(58f, 44f), Offset(70f, 62f), strokeWidth = 2f, cap = StrokeCap.Round)
                drawCircle(pearl, radius = 4f, center = Offset(58f, 44f))
                drawCircle(sky, radius = 4f, center = Offset(70f, 62f))
            }
            withTransform({
                translate(left = -3f * p, top = -10f * p)
                rotate(degrees = -28f * p, pivot = Offset(14f, 26f))
            }) {
                drawRoundRect(pearl, Offset(40f, 12f), Size(20f, 11f), CornerRadius(5.5f, 5.5f), style = Stroke(3.5f))
                drawRoundRect(AppleHigShaders.lidRimBrush(s), Offset(10f, 19f), Size(80f, 13f), CornerRadius(6.5f, 6.5f))
                drawRoundRect(AppleHigShaders.specularStrokeBrush(s), Offset(10f, 19f), Size(80f, 13f), CornerRadius(6.5f, 6.5f), style = Stroke(1.8f))
                listOf(26f, 36f, 46f, 56f, 66f, 74f).forEach { tx ->
                    drawLine(Color.White.copy(alpha = 0.9f), Offset(tx, 22f), Offset(tx, 29f), strokeWidth = 1.8f, cap = StrokeCap.Round)
                }
            }
        }

        Apple3DGraphicId.FLASH_VIDEO -> {
            drawSculptedSquircleBase(12f, 36f, 76f, 50f, 13f)
            drawSapphireRecessRect(18f, 42f, 64f, 38f, 9f)
            val playPath = Path().apply {
                moveTo(44f, 51f); lineTo(60f, 61f); lineTo(44f, 71f); close()
            }
            drawPath(playPath, pearl)
            drawPath(playPath, Color.White, style = Stroke(1.4f))
            listOf(24f, 37f, 50f, 63f).forEach { px ->
                drawRoundRect(Color.White.copy(alpha = 0.5f), Offset(px, 73f), Size(7f, 4f), CornerRadius(2f, 2f))
            }
            withTransform({
                translate(top = -4f * p)
                rotate(degrees = -30f * p, pivot = Offset(14f, 34f))
            }) {
                val clapperRect = Path().apply {
                    addRoundRect(RoundRect(Rect(10f, 21f, 90f, 35f), CornerRadius(6.5f, 6.5f)))
                }
                drawPath(clapperRect, AppleHigShaders.bodyBrush(s))
                drawPath(clapperRect, AppleHigShaders.cylindricalSheenBrush(s))
                clipPath(clapperRect) {
                    listOf(14f, 32f, 50f, 68f, 86f).forEach { sx ->
                        val stripe = Path().apply {
                            moveTo(sx, 18f); lineTo(sx + 10f, 18f); lineTo(sx - 2f, 38f); lineTo(sx - 12f, 38f); close()
                        }
                        drawPath(stripe, pearl)
                    }
                }
                drawPath(clapperRect, AppleHigShaders.specularStrokeBrush(s), style = Stroke(1.8f))
                drawCircle(Color.White, radius = 3.2f, center = Offset(15f, 28f))
                drawCircle(Color(0xFF0051D5), radius = 1.6f, center = Offset(15f, 28f))
            }
        }

        Apple3DGraphicId.REMIX_NODE -> {
            withTransform({
                rotate(degrees = -36f * p, pivot = Offset(50f, 50f))
                scale(1f + 0.08f * p, 1f + 0.08f * p, pivot = Offset(50f, 50f))
            }) {
                drawSculptedSquircleBase(12f, 26f, 38f, 38f, 15f)
                drawSapphireRecessRect(18f, 32f, 26f, 26f, 10f)
                drawSculptedSquircleBase(50f, 36f, 38f, 38f, 15f)
                drawSapphireRecessRect(56f, 42f, 26f, 26f, 10f)
                drawArc(pearl, 200f, 140f, false, Offset(22f, 16f), Size(56f, 48f), style = Stroke(5.2f, cap = StrokeCap.Round))
                drawArc(sky, 20f, 140f, false, Offset(22f, 36f), Size(56f, 48f), style = Stroke(5.2f, cap = StrokeCap.Round))
                val topArrow = Path().apply { moveTo(74f, 34f); lineTo(82f, 37f); lineTo(77f, 44f); close() }
                drawPath(topArrow, pearl)
                val bottomArrow = Path().apply { moveTo(26f, 66f); lineTo(18f, 63f); lineTo(23f, 56f); close() }
                drawPath(bottomArrow, sky)
                val sparkR = 6f + 2f * p
                drawCircle(Brush.radialGradient(listOf(Color.White, Color(0xFF80DCFF), Color.Transparent), center = Offset(50f, 50f), radius = sparkR * 2f), radius = sparkR * 2f, center = Offset(50f, 50f))
                drawCircle(Color.White, radius = sparkR, center = Offset(50f, 50f))
            }
        }

        Apple3DGraphicId.CHAT_BUBBLE -> {
            val primaryBubble = Path().apply {
                moveTo(24f, 38f); lineTo(76f, 38f); cubicTo(82f, 38f, 86f, 42f, 86f, 48f)
                lineTo(86f, 72f); cubicTo(86f, 78f, 82f, 82f, 76f, 82f); lineTo(34f, 82f)
                lineTo(20f, 90f); lineTo(22f, 82f); lineTo(18f, 82f)
                cubicTo(12f, 82f, 8f, 78f, 8f, 72f); lineTo(8f, 48f); cubicTo(8f, 42f, 12f, 38f, 18f, 38f); close()
            }
            drawPath(primaryBubble, color = AppleHigShaders.PlinthShadow)
            drawPath(primaryBubble, brush = AppleHigShaders.bodyBrush(s))
            drawPath(primaryBubble, brush = AppleHigShaders.cylindricalSheenBrush(s))
            drawPath(primaryBubble, brush = AppleHigShaders.specularStrokeBrush(s), style = Stroke(2.2f))

            val recessBubble = Path().apply {
                moveTo(22f, 44f); lineTo(72f, 44f); cubicTo(76f, 44f, 80f, 47f, 80f, 51f)
                lineTo(80f, 69f); cubicTo(80f, 73f, 76f, 76f, 72f, 76f); lineTo(32f, 76f)
                lineTo(24f, 80f); lineTo(25f, 76f); lineTo(18f, 76f)
                cubicTo(14f, 76f, 14f, 73f, 14f, 69f); lineTo(14f, 51f); cubicTo(14f, 47f, 18f, 44f, 22f, 44f); close()
            }
            drawPath(recessBubble, brush = AppleHigShaders.sapphireRecessBrush(s))
            drawPath(recessBubble, brush = AppleHigShaders.sapphireBevelBrush(s), style = Stroke(1.8f))
            drawRoundRect(sky, Offset(20f, 52f), Size(38f, 4.5f), CornerRadius(2.25f, 2.25f))
            drawRoundRect(Brush.linearGradient(listOf(Color(0xFF80DCFF), Color(0xFF38B6FF))), Offset(20f, 61f), Size(26f, 4.5f), CornerRadius(2.25f, 2.25f))

            withTransform({
                translate(left = 4f * p, top = -12f * p)
                rotate(degrees = -20f * p, pivot = Offset(32f, 36f))
            }) {
                val floatBubble = Path().apply {
                    moveTo(34f, 15f); lineTo(82f, 15f); cubicTo(88f, 15f, 92f, 19f, 92f, 25f)
                    lineTo(92f, 45f); cubicTo(92f, 51f, 88f, 55f, 82f, 55f); lineTo(74f, 55f)
                    lineTo(76f, 61f); lineTo(66f, 55f); lineTo(34f, 55f)
                    cubicTo(28f, 55f, 24f, 51f, 24f, 45f); lineTo(24f, 25f); cubicTo(24f, 19f, 28f, 15f, 34f, 15f); close()
                }
                drawPath(floatBubble, pearl)
                drawPath(floatBubble, Brush.linearGradient(listOf(Color.White, Color(0xFF80DCFF).copy(alpha = 0.5f)), start = Offset(24f, 15f), end = Offset(92f, 55f)), style = Stroke(1.8f))
                listOf(44f, 58f, 72f).forEach { dx ->
                    drawCircle(AppleHigShaders.bodyBrush(s), radius = 3.6f, center = Offset(dx, 35f))
                    drawCircle(Color.White.copy(alpha = 0.9f), radius = 1.4f, center = Offset(dx - 1f, 34f))
                }
            }
        }
        Apple3DGraphicId.SAVED_WORKS -> {
            // 1. Studio Floor Ambient Contact Shadow
            drawOval(
                brush = Brush.radialGradient(
                    listOf(AppleHigShaders.PlinthShadow.copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(50f, 88f),
                    radius = 36f
                ),
                topLeft = Offset(14f, 83.2f),
                size = Size(72f, 9.6f)
            )

            // 2. Backdrop Folio Gallery Canvas Card (Subtly Tilted with Spring Easing)
            val cardTilt = -2.5f + 4f * p
            withTransform({
                rotate(degrees = cardTilt, pivot = Offset(48f, 52f))
                scale(1f + 0.03f * p, 1f + 0.03f * p, pivot = Offset(48f, 52f))
            }) {
                // Outer Sculpted Squircle Plinth
                drawSculptedSquircleBase(16f, 18f, 62f, 62f, 14f)

                // Inner Sapphire Gallery Recess
                drawSapphireRecessRect(21f, 23f, 52f, 52f, 10f)

                // Miniature UI Status & Title Skeleton
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.22f),
                    topLeft = Offset(26f, 28f),
                    size = Size(18f, 2.5f),
                    cornerRadius = CornerRadius(1.25f, 1.25f)
                )

                // Mini Saved Creative Artwork Layer Bands
                // Band 1: Cyan / Apple Blue Gradient (Vector Mesh Layer)
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(Color(0xFF00C7BE), Color(0xFF007AFF))),
                    topLeft = Offset(26f, 36f),
                    size = Size(42f, 6.5f),
                    cornerRadius = CornerRadius(3.25f, 3.25f)
                )

                // Band 2: Royal Indigo / Amethyst Gradient (Shader Filter Layer)
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(Color(0xFF5856D6), Color(0xFFAF52DE))),
                    topLeft = Offset(26f, 46f),
                    size = Size(35f, 6.5f),
                    cornerRadius = CornerRadius(3.25f, 3.25f)
                )

                // Band 3: Sunset Coral / Amber Gradient (Lighting Accent Layer)
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(Color(0xFFFF2D55), Color(0xFFFF9500))),
                    topLeft = Offset(26f, 56f),
                    size = Size(28f, 6.5f),
                    cornerRadius = CornerRadius(3.25f, 3.25f)
                )

                // Diagonal Specular Glass Reflection Sheen
                drawLine(
                    brush = Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.05f), Color.Transparent),
                        start = Offset(20f, 22f),
                        end = Offset(68f, 70f)
                    ),
                    start = Offset(22f, 25f),
                    end = Offset(65f, 68f),
                    strokeWidth = 1.6f
                )
            }

            // 3. The Iconic 3D Bookmark Ribbon (Cascading over the Folio Card)
            val ribbonSway = -3.5f * kotlin.math.sin(p * Math.PI.toFloat())
            withTransform({
                rotate(degrees = ribbonSway, pivot = Offset(62f, 14f))
                translate(left = 0f, top = 2f * p)
            }) {
                // Top folded loop wrapping over edge
                drawRoundRect(
                    brush = sky,
                    topLeft = Offset(52f, 12f),
                    size = Size(20f, 6f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
                drawRoundRect(
                    brush = AppleHigShaders.specularStrokeBrush(s),
                    topLeft = Offset(52f, 12f),
                    size = Size(20f, 6f),
                    cornerRadius = CornerRadius(3f, 3f),
                    style = Stroke(width = 0.8f)
                )

                // Main Bookmark Ribbon Path with Swallowtail Peak
                val ribbonPath = Path().apply {
                    moveTo(52f, 14f)
                    lineTo(72f, 14f)
                    lineTo(72f, 76f)
                    lineTo(62f, 66f)
                    lineTo(52f, 76f)
                    close()
                }

                // Ribbon Drop Shadow on Folio
                val ribbonShadow = Path().apply {
                    moveTo(50f, 17.5f)
                    lineTo(70f, 17.5f)
                    lineTo(70f, 79.5f)
                    lineTo(60f, 69.5f)
                    lineTo(50f, 79.5f)
                    close()
                }
                drawPath(path = ribbonShadow, color = Color(0x55001238))

                // Ribbon Body Fill
                drawPath(path = ribbonPath, brush = AppleHigShaders.bodyBrush(s))

                // Embossed Tactile Center Seam
                val creasePath = Path().apply {
                    moveTo(62f, 15f)
                    lineTo(62f, 66f)
                }
                drawPath(
                    path = creasePath,
                    color = Color.White.copy(alpha = 0.38f),
                    style = Stroke(width = 1.2f, cap = StrokeCap.Round)
                )

                // Specular Edge Bevel
                drawPath(
                    path = ribbonPath,
                    brush = AppleHigShaders.specularStrokeBrush(s),
                    style = Stroke(width = 1.1f)
                )
            }

            // 4. Radiant Floating Diamond Star (Symbol of Treasured Favorite)
            val starPulse = 1f + 0.18f * kotlin.math.sin(p * Math.PI.toFloat())
            val starRotate = 18f * p
            withTransform({
                scale(starPulse, starPulse, pivot = Offset(76f, 21f))
                rotate(degrees = starRotate, pivot = Offset(76f, 21f))
            }) {
                // Ethereal Radial Ambient Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFFFFD60A).copy(alpha = 0.40f), Color(0xFFFF9500).copy(alpha = 0.15f), Color.Transparent),
                        center = Offset(76f, 21f),
                        radius = 15f
                    ),
                    radius = 15f,
                    center = Offset(76f, 21f)
                )

                // 4-Point Diamond Starburst
                val starPath = Path().apply {
                    val cx = 76f
                    val cy = 21f
                    val outerR = 8.5f
                    val innerR = 2.6f
                    moveTo(cx, cy - outerR)
                    lineTo(cx + innerR, cy - innerR)
                    lineTo(cx + outerR, cy)
                    lineTo(cx + innerR, cy + innerR)
                    lineTo(cx, cy + outerR)
                    lineTo(cx - innerR, cy + innerR)
                    lineTo(cx - outerR, cy)
                    lineTo(cx - innerR, cy - innerR)
                    close()
                }

                drawPath(
                    path = starPath,
                    brush = Brush.linearGradient(
                        listOf(Color(0xFFFFF7C2), Color(0xFFFFD60A), Color(0xFFFF9500)),
                        start = Offset(67.5f, 12.5f),
                        end = Offset(84.5f, 29.5f)
                    )
                )

                drawPath(
                    path = starPath,
                    color = Color.White.copy(alpha = 0.85f),
                    style = Stroke(width = 0.9f)
                )

                // Central Specular Sparkle Highlight
                drawCircle(
                    color = Color.White,
                    radius = 1.4f,
                    center = Offset(76f, 21f)
                )
            }
        }
    }
}

@Composable
fun Apple3DGraphicCatalogScreen() {
    var selectedGraphic by remember { mutableStateOf(Apple3DGraphicId.TRASH_BIN) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F7FB))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Live Apple HIG Dialog Preview Header
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AppleHigDialogCard(graphicId = selectedGraphic)
        }

        // 30-Icon Grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(Apple3DGraphicId.entries) { item ->
                var isCardOpen by remember { mutableStateOf(false) }
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = if (selectedGraphic == item) Color(0xFFF8FBFF) else Color.White,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selectedGraphic == item) Color(0xFF0A84FF) else Color(0xFFDFE8F3)
                    ),
                    modifier = Modifier.clickable {
                        selectedGraphic = item
                        isCardOpen = !isCardOpen
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "#${item.number} • ${item.tag}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0A84FF)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Apple3DGraphicIcon(
                            id = item,
                            isOpen = isCardOpen,
                            modifier = Modifier.size(84.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = item.title,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
