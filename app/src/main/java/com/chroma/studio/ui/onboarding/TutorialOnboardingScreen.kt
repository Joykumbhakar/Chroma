package com.chroma.studio.ui.onboarding

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chroma.studio.chat.sound.SoundManager
import com.chroma.studio.ui.components.AppleStatusCloseFriendGreen
import com.chroma.studio.ui.components.AppleStatusGoldGradient
import com.chroma.studio.ui.components.AppleStatusSolidBlue
import com.chroma.studio.ui.theme.LocalOscellionColors
import com.composables.icons.lucide.*
import kotlinx.coroutines.launch

/**
 * Flagship First-Time Open Tutorial Onboarding Screen.
 *
 * Authored using the Stageshow design skill specification:
 * - Miniature authentic product UI framed in an accurate device stage (Phone with Dynamic Island & chrome)
 * - Quiet, structured hierarchy with 3-7 real words per card and calm grays for filler
 * - Satellite floating cards straddling the stage edges for depth and visual storytelling
 * - Cohesive Apple System Blue (#0381FE), Emerald Green (#00C853), and Gold (#FFD700) palette
 * - Kinetic morphing pill pagination and spring press actions
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TutorialOnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = LocalOscellionColors.current
    val isDark = colors.isDark

    val totalPages = 3
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { totalPages })

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090A0F)) // Pure deep space canvas
    ) {
        // ── 1. Stageshow Ambient Stage Lighting (Subtle Aurora Glow) ──
        StageshowAmbientGlow(currentPage = pagerState.currentPage)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── 2. Top Navigation Bar (Logo Mark & Quiet Skip Pill) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Brand Mark with subtle glow
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF0381FE), Color(0xFF00C6FF))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.Sparkle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "Chroma Studio",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.02).sp
                    )
                }

                // Quiet Skip Button
                if (pagerState.currentPage < totalPages - 1) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                            .clickable {
                                SoundManager.playReaction()
                                onFinish()
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Skip",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(30.dp))
                }
            }

            // ── 3. Interactive Pager with Miniature Stages ──
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> TutorialStagePhotoStudio()
                    1 -> TutorialStageVisualCommunity()
                    2 -> TutorialStageDirectSpaces()
                }
            }

            // ── 4. Bottom Navigation & Action Bar ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp, top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Morphing Pill Dot Indicator
                StageshowPillIndicator(
                    pageCount = totalPages,
                    currentPage = pagerState.currentPage
                )

                // Primary Spring Action Button
                val isLastPage = pagerState.currentPage == totalPages - 1
                val buttonInteraction = remember { MutableInteractionSource() }
                val isPressed by buttonInteraction.collectIsPressedAsState()
                val buttonScale by animateFloatAsState(
                    targetValue = if (isPressed) 0.965f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "btn_scale"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .graphicsLayer {
                            scaleX = buttonScale
                            scaleY = buttonScale
                        }
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0381FE), Color(0xFF0072DE))
                            )
                        )
                        .clickable(interactionSource = buttonInteraction, indication = null) {
                            SoundManager.playReaction()
                            if (isLastPage) {
                                onFinish()
                            } else {
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        page = pagerState.currentPage + 1,
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy)
                                    )
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isLastPage) "Get Started" else "Continue",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.01).sp
                        )
                        Icon(
                            imageVector = if (isLastPage) Lucide.Check else Lucide.ArrowRight,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// STAGE 1: Pro Photo & Video Studio (Miniature Color Lab & Curves)
// ============================================================================
@Composable
private fun TutorialStagePhotoStudio() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Miniature Phone Device Stage with Floating Satellites
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            StageshowDeviceFrame(
                width = 250.dp,
                height = 360.dp
            ) {
                // Miniature Photo Canvas
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF10121A))
                ) {
                    // Stylized Art Graphic
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(0xFF1E3A8A), Color(0xFF0F172A), Color(0xFF020617)),
                                    radius = 500f
                                )
                            )
                    ) {
                        // Color Grade Split Mask
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                brush = Brush.linearGradient(
                                    listOf(Color(0xFFFF9500).copy(alpha = 0.45f), Color(0xFF0381FE).copy(alpha = 0.35f))
                                ),
                                radius = size.width * 0.45f,
                                center = Offset(size.width * 0.5f, size.height * 0.45f)
                            )
                        }

                        // Split comparison hairline
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.5.dp)
                                .align(Alignment.Center)
                                .background(Color.White.copy(alpha = 0.65f))
                        )
                    }

                    // Bottom miniature tool ribbon
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(Color(0xCC090A0F))
                            .border(0.5.dp, Color.White.copy(alpha = 0.1f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(Lucide.Crop, Lucide.SlidersHorizontal, Lucide.Activity, Lucide.Palette).forEachIndexed { i, icon ->
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (i == 1) Color(0xFF0381FE) else Color.White.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (i == 1) Color.White else Color.White.copy(alpha = 0.65f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Satellite 1: Top-Right LUT Chip (Straddling Frame Edge)
            StageshowSatelliteCard(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = 20.dp),
                icon = Lucide.Film,
                iconTint = Color(0xFFFFA000),
                title = "Kodak Portra 400",
                badge = "+18% Warmth"
            )

            // Satellite 2: Bottom-Left Curves Chip (Straddling Frame Edge)
            StageshowSatelliteCard(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-4).dp, y = (-26).dp),
                icon = Lucide.Activity,
                iconTint = Color(0xFF0381FE),
                title = "S-Curve Tone",
                badge = "Deep Blacks"
            )
        }

        // Quiet Editorial Text Block
        StageshowStoryText(
            tag = "PRECISION CREATIVE ENGINE",
            title = "Pro Studio & Color Lab",
            description = "Master tone curves, cinematic 3D LUTs, and optical effects on a high-precision multi-layer canvas."
        )
    }
}

// ============================================================================
// STAGE 2: Visual Community & Remix Engine (Verified Rings & Stories)
// ============================================================================
@Composable
private fun TutorialStageVisualCommunity() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            StageshowDeviceFrame(
                width = 250.dp,
                height = 360.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0F1118))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Miniature Stories Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Golden Ring User (Verified VIP)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .border(1.8.dp, Brush.sweepGradient(AppleStatusGoldGradient), CircleShape)
                                .padding(2.5.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3B2D11)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("VIP", color = Color(0xFFFFD700), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }

                        // Green Ring User (Close Friends)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .border(1.8.dp, SolidColor(AppleStatusCloseFriendGreen), CircleShape)
                                .padding(2.5.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0A301E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("CF", color = AppleStatusCloseFriendGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }

                        // Solid Blue Ring User (Normal)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .border(1.8.dp, SolidColor(AppleStatusSolidBlue), CircleShape)
                                .padding(2.5.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0D2545)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("PRO", color = AppleStatusSolidBlue, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Miniature Post Feed Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF181B26))
                            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRect(
                                brush = Brush.linearGradient(
                                    listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                )
                            )
                            drawCircle(
                                brush = Brush.linearGradient(
                                    listOf(Color(0xFF38BDF8).copy(alpha = 0.35f), Color(0xFF6366F1).copy(alpha = 0.25f))
                                ),
                                radius = size.width * 0.4f,
                                center = Offset(size.width * 0.5f, size.height * 0.5f)
                            )
                        }

                        // Post interaction badge
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Lucide.Heart, contentDescription = null, tint = Color(0xFFFF2D55), modifier = Modifier.size(11.dp))
                            Text("1.2k", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(2.dp))
                            Icon(Lucide.MessageCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                            Text("84", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Satellite 1: Top-Right Verified Gold Chip
            StageshowSatelliteCard(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = 16.dp),
                icon = Lucide.BadgeCheck,
                iconTint = Color(0xFFFFD700),
                title = "Gold Verified Story",
                badge = "VIP Ring"
            )

            // Satellite 2: Bottom-Left Remix Stems Chip
            StageshowSatelliteCard(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-4).dp, y = (-26).dp),
                icon = Lucide.Layers,
                iconTint = Color(0xFF0381FE),
                title = "Community Remix",
                badge = "Stems Attached"
            )
        }

        StageshowStoryText(
            tag = "DISCOVERY & SOCIAL",
            title = "Visual Feed & Remix Engine",
            description = "Explore visual art from creators worldwide, remix community assets, and share your creative vision."
        )
    }
}

// ============================================================================
// STAGE 3: Direct Spaces & Real-Time Chat (Voice & Stories)
// ============================================================================
@Composable
private fun TutorialStageDirectSpaces() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            StageshowDeviceFrame(
                width = 250.dp,
                height = 360.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF090A0E))
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Chat header line
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sarah Connor", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00C853))
                        )
                    }

                    // Received message bubble
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.82f)
                            .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 4.dp))
                            .background(Color(0xFF1E222D))
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "Love the grade on that last shot! Send me the preset?",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 10.sp,
                            lineHeight = 13.sp
                        )
                    }

                    // Outgoing Voice Waveform Note Bubble
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .align(Alignment.End)
                            .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 4.dp))
                            .background(Color(0xFF0381FE))
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Lucide.Play, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                            }

                            // Waveform bars simulation
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                val heights = listOf(8, 14, 18, 10, 16, 20, 12, 14, 9, 15, 7)
                                heights.forEach { h ->
                                    Box(
                                        modifier = Modifier
                                            .width(2.5.dp)
                                            .height(h.dp)
                                            .clip(RoundedCornerShape(1.dp))
                                            .background(Color.White)
                                    )
                                }
                            }

                            Text("0:42", color = Color.White.copy(alpha = 0.85f), fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                            Icon(Lucide.CheckCheck, contentDescription = null, tint = Color(0xFF67E8F9), modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }

            // Satellite 1: Top-Right Voice Note Chip
            StageshowSatelliteCard(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = 20.dp),
                icon = Lucide.Mic,
                iconTint = Color(0xFF00C853),
                title = "Voice Audio Note",
                badge = "Live Waveform"
            )

            // Satellite 2: Bottom-Left Close Friends Circle Chip
            StageshowSatelliteCard(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-4).dp, y = (-26).dp),
                icon = Lucide.Star,
                iconTint = Color(0xFF00C853),
                title = "Only Friends",
                badge = "Emerald Circle"
            )
        }

        StageshowStoryText(
            tag = "COLLABORATION & SPACES",
            title = "Direct Chat & Story Circles",
            description = "Stay close with voice notes, high-fidelity media sharing, and private ephemeral circles."
        )
    }
}

// ============================================================================
// STAGESHOW REUSABLE MINIATURE FRAME & COMPONENTS
// ============================================================================

/**
 * Authentic miniature phone device frame with rounded corners, glass border,
 * status bar, and native Dynamic Island geometry.
 */
@Composable
private fun StageshowDeviceFrame(
    width: Dp,
    height: Dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .size(width, height)
            .shadow(24.dp, RoundedCornerShape(26.dp), spotColor = Color(0x660381FE))
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xFF000000))
            .border(1.5.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(26.dp))
    ) {
        // App Content viewport
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 26.dp, bottom = 12.dp)
        ) {
            content()
        }

        // Top Dynamic Island Pill
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 7.dp)
                .width(68.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFF141416))
                .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(999.dp)),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Lens dot
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF26262A))
                )
                // Ambient green indicator dot
                Box(
                    modifier = Modifier
                        .size(3.5.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00C853))
                )
            }
        }

        // Bottom Home Indicator Bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 5.dp)
                .width(64.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.35f))
        )
    }
}

/**
 * Satellite floating card that straddles the hero's edge as defined in Stageshow craft.
 */
@Composable
private fun StageshowSatelliteCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    title: String,
    badge: String
) {
    Row(
        modifier = modifier
            .shadow(16.dp, RoundedCornerShape(12.dp), spotColor = Color.Black.copy(alpha = 0.6f))
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xF0181A22))
            .border(0.8.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(11.dp)
            )
        }
        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.01).sp
            )
            Text(
                text = badge,
                color = iconTint,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Stageshow quiet editorial text block below the stage.
 */
@Composable
private fun StageshowStoryText(
    tag: String,
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = tag,
            color = Color(0xFF0381FE),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = title,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.02).sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = description,
            color = Color.White.copy(alpha = 0.72f),
            fontSize = 13.5.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

/**
 * Animated Stageshow morphing pill page indicator.
 */
@Composable
private fun StageshowPillIndicator(
    pageCount: Int,
    currentPage: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            val width by animateDpAsState(
                targetValue = if (isSelected) 24.dp else 7.dp,
                animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "indicator_width"
            )
            val color by animateColorAsState(
                targetValue = if (isSelected) Color(0xFF0381FE) else Color.White.copy(alpha = 0.22f),
                animationSpec = tween(220),
                label = "indicator_color"
            )

            Box(
                modifier = Modifier
                    .height(7.dp)
                    .width(width)
                    .clip(RoundedCornerShape(999.dp))
                    .background(color)
            )
        }
    }
}

/**
 * Subtle ambient stage backlight that smoothly shifts hue with each slide.
 */
@Composable
private fun StageshowAmbientGlow(currentPage: Int) {
    val glowColor by animateColorAsState(
        targetValue = when (currentPage) {
            0 -> Color(0xFF0381FE)
            1 -> Color(0xFFF59E0B)
            else -> Color(0xFF00C853)
        },
        animationSpec = tween(600),
        label = "ambient_glow_color"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .blur(80.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor.copy(alpha = 0.25f), Color.Transparent),
                    radius = size.width * 0.7f,
                    center = Offset(size.width * 0.5f, size.height * 0.35f)
                )
            )
        }
    }
}
