package com.chroma.studio.ui.components

import androidx.compose.foundation.layout.WindowInsets
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Bookmark
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.CheckCheck
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.ChevronUp
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.CornerLeftUp
import com.composables.icons.lucide.EllipsisVertical
import com.composables.icons.lucide.Flag
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.MessageCircle
import com.composables.icons.lucide.Mic
import com.composables.icons.lucide.Moon
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Pause
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Share2
import com.composables.icons.lucide.SlidersHorizontal
import com.composables.icons.lucide.Sun
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.X
import com.chroma.studio.ui.theme.ChromaIcons
import androidx.compose.material3.Icon
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.aspectRatio


import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// ============================================================================
// 1. DESIGN TOKENS & MOTION CURVES
// ============================================================================
private val Blue400 = Color(0xFF60A5FA)
private val Blue600 = Color(0xFF2563EB)
private val Blue700 = Color(0xFF1D4ED8)
private val Rose500 = Color(0xFFF43F5E)
private val Emerald400 = Color(0xFF34D399)
private val Emerald600 = Color(0xFF059669)
private val Amber400 = Color(0xFFFBBF24)
private val Amber600 = Color(0xFFD97706)
private val Neutral300 = Color(0xFFD4D4D4)
private val Neutral600 = Color(0xFF525252)

private val SmoothEaseOut = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
private val LaunchEaseIn = CubicBezierEasing(0.32f, 0f, 0.67f, 0f)

// Stageshow two-curve motion hierarchy (benjitaylor/skills)
private val StageTravelEasing = CubicBezierEasing(0.45f, 0f, 0.1f, 1f)
private val StageArrivalEasing = CubicBezierEasing(0.25f, 0.8f, 0.25f, 1f)

private data class CommentSheetPalette(
    val isDark: Boolean,
    val sheetBg: Color,
    val cardBg: Color,
    val surfaceSubtle: Color,
    val surfaceChip: Color,
    val divider: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val underlineInactive: Color
)

private fun commentSheetPalette(isDark: Boolean): CommentSheetPalette =
    if (isDark) {
        CommentSheetPalette(
            isDark = true,
            sheetBg = Color(0xFF0A0A0A),
            cardBg = Color(0xFF141416),
            surfaceSubtle = Color(0xFF17171A),
            surfaceChip = Color(0xFF1F1F23),
            divider = Color(0xFF222226),
            border = Color(0xFF2A2A30),
            textPrimary = Color(0xFFFAFAFA),
            textSecondary = Color(0xFFD4D4D8),
            textMuted = Color(0xFF8E8E93),
            underlineInactive = Color(0xFF3F3F46)
        )
    } else {
        CommentSheetPalette(
            isDark = false,
            sheetBg = Color(0xFFFFFFFF),
            cardBg = Color(0xFFFFFFFF),
            surfaceSubtle = Color(0xFFFAFAFA),
            surfaceChip = Color(0xFFF4F4F5),
            divider = Color(0xFFF1F1F4),
            border = Color(0xFFE4E4E7),
            textPrimary = Color(0xFF171717),
            textSecondary = Color(0xFF3F3F46),
            textMuted = Color(0xFF71717A),
            underlineInactive = Color(0xFFD4D4D4)
        )
    }

// ============================================================================
// 2. DATA MODELS & ICONORA STUDIO OVERRIDES
// ============================================================================
enum class AvatarStyle(val start: Color, val end: Color) {
    CLASSIC_MAC(Color(0xFFE2E5E9), Color(0xFF9BA2AE)),
    ROYAL_BLUE(Color(0xFF2563EB), Color(0xFF1E3A8A)),
    EMERALD_FOCUS(Color(0xFF059669), Color(0xFF064E3B)),
    AMETHYST_DARK(Color(0xFF7E22CE), Color(0xFF4C1D95)),
    CRIMSON_VELVET(Color(0xFFBE123C), Color(0xFF881337)),
    TEAL_PRO(Color(0xFF0F766E), Color(0xFF042F2E))
}

enum class VerifiedBadgeType(val start: Color, val end: Color) {
    NONE(Color.Transparent, Color.Transparent),
    BLUE(Color(0xFF3B82F6), Color(0xFF1D4ED8)),
    GOLD(Color(0xFFFBBF24), Color(0xFFB45309)),
    EMERALD(Color(0xFF10B981), Color(0xFF047857))
}

enum class CommentSort(val label: String) {
    TOP("Top"),
    NEWEST("Newest"),
    DISCUSSED("Discussed"),
    CREATOR_LIKED("Creator Liked")
}

enum class CommentFilter(val label: String) {
    ALL("All"),
    VERIFIED("Verified"),
    POLLS("Polls"),
    VOICE("Voice"),
    MEDIA("Media"),
    SAVED("Saved")
}

enum class MediaCardTheme { BLUEPRINT, MOCKUP }

typealias ReactionCount = ReactionData
typealias MediaPreviewTheme = MediaCardTheme


enum class IconoraMenuIconType {
    CREATOR_HEART,
    PIN,
    BOOKMARK,
    TRANSLATE,
    COPY_TEXT,
    SHARE_LINK,
    EDIT,
    DELETE,
    REPORT
}

/**
 * Optional drawable overrides if your team imports exported PNG/WebP icon packs
 * directly from Iconora Studio (https://iconorastudio.com/).
 * Any null property falls back to the built-in 3D Liquid Glass Canvas vector icon.
 */
data class IconoraMenuIconOverrides(
    @DrawableRes val creatorHeart: Int? = null,
    @DrawableRes val pin: Int? = null,
    @DrawableRes val bookmark: Int? = null,
    @DrawableRes val translate: Int? = null,
    @DrawableRes val copyText: Int? = null,
    @DrawableRes val shareLink: Int? = null,
    @DrawableRes val edit: Int? = null,
    @DrawableRes val delete: Int? = null,
    @DrawableRes val report: Int? = null
) {
    fun resolve(type: IconoraMenuIconType): Int? = when (type) {
        IconoraMenuIconType.CREATOR_HEART -> creatorHeart
        IconoraMenuIconType.PIN -> pin
        IconoraMenuIconType.BOOKMARK -> bookmark
        IconoraMenuIconType.TRANSLATE -> translate
        IconoraMenuIconType.COPY_TEXT -> copyText
        IconoraMenuIconType.SHARE_LINK -> shareLink
        IconoraMenuIconType.EDIT -> edit
        IconoraMenuIconType.DELETE -> delete
        IconoraMenuIconType.REPORT -> report
    }
}

data class CommentAuthor(
    val name: String,
    val handle: String,
    val avatarStyle: AvatarStyle = AvatarStyle.ROYAL_BLUE,
    val verified: VerifiedBadgeType? = VerifiedBadgeType.NONE,
    val role: String? = null,
    val isSelf: Boolean = false,
    val avatarUrl: String = "",
    val userId: String = "",
    val isOnline: Boolean = false
)

data class ReactionData(
    val count: Int,
    val activeByMe: Boolean
)

data class CommentMediaAttachment(
    val theme: MediaCardTheme,
    val title: String,
    val caption: String
)

data class CommentPollOption(
    val id: String,
    val label: String,
    val votes: Int
)

data class CommentPoll(
    val question: String,
    val options: List<CommentPollOption>,
    val selectedOptionId: String? = null
)

data class CommentVoiceNote(
    val durationSeconds: Int,
    val waveform: List<Float>
)

data class CommentTranslation(
    val fromLanguage: String,
    val translatedText: String
)

data class CommentReply(
    val id: String,
    val author: CommentAuthor,
    val text: String,
    val createdAtMillis: Long,
    val edited: Boolean = false,
    val likes: Int = 0,
    val likedByMe: Boolean = false,
    val creatorHeart: Boolean = false,
    val reactions: Map<String, ReactionData> = emptyMap()
)

data class PostComment(
    val id: String,
    val author: CommentAuthor,
    val text: String,
    val createdAtMillis: Long,
    val edited: Boolean = false,
    val pinned: Boolean = false,
    val likes: Int = 0,
    val likedByMe: Boolean = false,
    val creatorHeart: Boolean = false,
    val bookmarked: Boolean = false,
    val reactions: Map<String, ReactionData> = emptyMap(),
    val media: CommentMediaAttachment? = null,
    val poll: CommentPoll? = null,
    val voiceNote: CommentVoiceNote? = null,
    val translation: CommentTranslation? = null,
    val showTranslation: Boolean = false,
    val expanded: Boolean = false,
    val repliesExpanded: Boolean = false,
    val visibleRepliesCount: Int = 2,
    val replies: List<CommentReply> = emptyList()
)

data class PhotoPostSummary(
    val id: String = "",
    val author: CommentAuthor? = null,
    val authorHandle: String = author?.handle.orEmpty().ifBlank { "creator" },
    val authorVerified: VerifiedBadgeType = author?.verified ?: VerifiedBadgeType.NONE,
    val location: String = "",
    val relativeTime: String = "Active",
    val timeAgo: String = relativeTime,
    val caption: String = "",
    val likeCountLabel: String = "Likes",
    val thumbnailUrl: String = ""
)

private data class TargetRef(val commentId: String, val replyId: String? = null)
private data class ReplyTarget(val commentId: String, val handle: String)
private data class ToastBannerState(val message: String, val canUndo: Boolean = false)
private data class DeletedCommentRecord(
    val parentCommentId: String?,
    val index: Int,
    val comment: PostComment? = null,
    val reply: CommentReply? = null
)

private data class FloatingEmojiParticle(
    val id: Long,
    val emoji: String,
    val startFractionX: Float,
    val driftDp: Float
)

private enum class CommentSendAnimStage { IDLE, PULLBACK, LAUNCH, CONFIRM, RETURN }

private val QuickEmojis = listOf("❤️", "🙌", "🔥", "👏", "😂", "😍", "😮", "💯")
private val ReactionPalette = listOf("🔥", "👏", "💡", "❤️", "😂", "🚀")
private val VoiceSpeeds = listOf(1f, 1.5f, 2f)

val DemoCurrentUser = CommentAuthor(
    name = "Alex Morgan",
    handle = "alexmorgan",
    avatarStyle = AvatarStyle.CLASSIC_MAC,
    verified = VerifiedBadgeType.GOLD,
    role = "Creator",
    isSelf = true
)

val DemoMentionDirectory = listOf(
    DemoCurrentUser,
    CommentAuthor("Maya Lin", "mayalin", AvatarStyle.ROYAL_BLUE, VerifiedBadgeType.BLUE),
    CommentAuthor("Daniel Park", "dpark_dev", AvatarStyle.EMERALD_FOCUS, VerifiedBadgeType.EMERALD),
    CommentAuthor("Sofia Rossi", "sofiarossi", AvatarStyle.AMETHYST_DARK, VerifiedBadgeType.NONE),
    CommentAuthor("Liam O'Connor", "liamux", AvatarStyle.TEAL_PRO, VerifiedBadgeType.BLUE),
    CommentAuthor("Priya Sharma", "priyasharma", AvatarStyle.CRIMSON_VELVET, VerifiedBadgeType.NONE)
)

fun createInitialDemoComments(now: Long = System.currentTimeMillis()): List<PostComment> = listOf(
    PostComment(
        id = "c-101",
        author = DemoCurrentUser,
        text = "Shot on golden hour overlooking the bay! Tap 01:42 for the color grading breakdown or tap ||hidden editing preset|| to watch the dust reveal. #Photography #GoldenHour",
        createdAtMillis = now - 5L * 3600_000L,
        pinned = true,
        likes = 148,
        likedByMe = true,
        bookmarked = true,
        reactions = mapOf(
            "🔥" to ReactionData(24, true),
            "👏" to ReactionData(16, false)
        ),
        poll = CommentPoll(
            question = "Which photo breakdown should I post next?",
            options = listOf(
                CommentPollOption("opt-1", "Golden Hour LUT Curve", 84),
                CommentPollOption("opt-2", "Moody Night Cobalt", 41),
                CommentPollOption("opt-3", "35mm Film Grain Recipe", 63)
            )
        ),
        repliesExpanded = true,
        visibleRepliesCount = 2,
        replies = listOf(
            CommentReply(
                id = "r-201",
                author = CommentAuthor("Maya Lin", "mayalin", AvatarStyle.ROYAL_BLUE, VerifiedBadgeType.BLUE, "Top Fan"),
                text = "@alexmorgan The sky tones and crisp highlights at 01:42 look unreal!",
                createdAtMillis = now - 3L * 3600_000L,
                likes = 31,
                creatorHeart = true,
                reactions = mapOf("🚀" to ReactionData(7, false))
            ),
            CommentReply(
                id = "r-202",
                author = CommentAuthor("Daniel Park", "dpark_dev", AvatarStyle.EMERALD_FOCUS, VerifiedBadgeType.EMERALD, "Mod"),
                text = "@mayalin Agreed! The dynamic range with `ISO 100` kept zero noise in the shadows.",
                createdAtMillis = now - 90L * 60_000L,
                edited = true,
                likes = 19,
                likedByMe = true,
                reactions = mapOf("💡" to ReactionData(5, true))
            ),
            CommentReply(
                id = "r-203",
                author = CommentAuthor("Priya Sharma", "priyasharma", AvatarStyle.CRIMSON_VELVET, VerifiedBadgeType.NONE, "Top Fan"),
                text = "@dpark_dev Just tried this exact framing at sunset — works like magic.",
                createdAtMillis = now - 25L * 60_000L,
                likes = 9
            )
        )
    ),
    PostComment(
        id = "c-102",
        author = CommentAuthor("Liam O'Connor", "liamux", AvatarStyle.TEAL_PRO, VerifiedBadgeType.BLUE),
        text = "Here is the exact lighting & curve breakdown I noticed in this photo. Keeping **warm highlights** paired with subtle cool shadows gives it that cinema-grade depth without feeling over-processed. Even zoomed in, the reflections along the horizon stay super clean and balanced.",
        createdAtMillis = now - 2L * 3600_000L,
        likes = 92,
        creatorHeart = true,
        reactions = mapOf("💡" to ReactionData(14, false), "🔥" to ReactionData(8, false)),
        media = CommentMediaAttachment(
            theme = MediaCardTheme.BLUEPRINT,
            title = "Color Grading Tone Curve",
            caption = "Warm highlights (+14) • Lifted cobalt shadows • Filmic roll-off."
        ),
        replies = listOf(
            CommentReply(
                id = "r-204",
                author = DemoCurrentUser,
                text = "@liamux Spot on! That subtle cobalt lift in the lower curve is the whole secret.",
                createdAtMillis = now - 48L * 60_000L,
                likes = 14
            )
        )
    ),
    PostComment(
        id = "c-103",
        author = CommentAuthor("Sofia Rossi", "sofiarossi", AvatarStyle.AMETHYST_DARK, VerifiedBadgeType.NONE),
        text = "Adoro questa foto! I colori del tramonto e i piccoli dettagli sulle nuvole sono *semplicemente perfetti*. #SunsetVibes",
        createdAtMillis = now - 35L * 60_000L,
        likes = 41,
        reactions = mapOf("❤️" to ReactionData(11, false)),
        voiceNote = CommentVoiceNote(
            durationSeconds = 14,
            waveform = listOf(
                0.24f, 0.54f, 0.88f, 0.76f, 0.42f, 0.94f, 0.68f, 0.50f,
                0.35f, 0.80f, 0.98f, 0.72f, 0.48f, 0.84f, 0.58f, 0.36f,
                0.74f, 0.90f, 0.52f, 0.30f, 0.64f, 0.40f, 0.28f, 0.22f
            )
        ),
        translation = CommentTranslation(
            fromLanguage = "Italian",
            translatedText = "I love this photo! The sunset colors and tiny details on the clouds are *simply perfect*. #SunsetVibes"
        )
    )
)

fun createExtraDemoComments(now: Long = System.currentTimeMillis()): List<PostComment> = listOf(
    PostComment(
        id = "c-104",
        author = CommentAuthor("Priya Sharma", "priyasharma", AvatarStyle.CRIMSON_VELVET, VerifiedBadgeType.NONE, "Top Fan"),
        text = "Saved this for weekend shoot inspiration! Also shared my quick composition layout card below.",
        createdAtMillis = now - 9L * 3600_000L,
        likes = 27,
        reactions = mapOf("👏" to ReactionData(6, false)),
        media = CommentMediaAttachment(
            theme = MediaCardTheme.MOCKUP,
            title = "Rule-of-Thirds Frame Study",
            caption = "Horizon positioned cleanly along the lower third guide."
        )
    ),
    PostComment(
        id = "c-105",
        author = CommentAuthor("Daniel Park", "dpark_dev", AvatarStyle.EMERALD_FOCUS, VerifiedBadgeType.EMERALD, "Mod"),
        text = "Friendly reminder: wrap camera gear spoilers in ||secret lens setup|| tags! Check 03:20 for the flare comparison.",
        createdAtMillis = now - 14L * 3600_000L,
        likes = 18,
        creatorHeart = true
    )
)

// ============================================================================
// 3. SAMPLE PHOTO POST SCREEN (Tapping Comment Icon Opens Bottom Sheet Drawer)
// ============================================================================
@Composable
fun PhotoPostScreenDemo(
    modifier: Modifier = Modifier
) {
    var isDark by rememberSaveable { mutableStateOf(false) }
    var showCommentsSheet by rememberSaveable { mutableStateOf(false) }
    var postLiked by rememberSaveable { mutableStateOf(false) }
    var postSaved by rememberSaveable { mutableStateOf(false) }
    var liveCommentCount by rememberSaveable { mutableIntStateOf(7) }

    val palette = remember(isDark) { commentSheetPalette(isDark) }
    val postSummary = remember(postLiked) {
        PhotoPostSummary(
            authorHandle = "alexmorgan",
            authorVerified = VerifiedBadgeType.GOLD,
            location = "San Francisco, CA",
            relativeTime = "5h",
            caption = "Golden hour over the coastal bridge — chasing clean light and quiet horizons. #Photography #GoldenHour",
            likeCountLabel = if (postLiked) "1,249" else "1,248"
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = palette.sheetBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Photo Post",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Surface(
                    shape = CircleShape,
                    color = palette.surfaceChip,
                    modifier = Modifier.clickable { isDark = !isDark }
                ) {
                    Text(
                        text = if (isDark) "Light" else "Dark",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }

            HorizontalDivider(color = palette.divider)

            // Author row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MacProfileAvatar(
                        style = AvatarStyle.CLASSIC_MAC,
                        ringColor = Blue600.copy(alpha = 0.25f),
                        modifier = Modifier.size(36.dp)
                    )
                    // Use existing profile avatars
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = postSummary.authorHandle,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.textPrimary
                            )
                            VerifiedSealBadge(postSummary.authorVerified, size = 15.dp)
                        }
                        Text(
                            text = postSummary.location,
                            fontSize = 11.5.sp,
                            color = palette.textMuted
                        )
                    }
                }
                Text(text = postSummary.relativeTime, fontSize = 12.sp, color = palette.textMuted)
            }

            // Sample Photo Artwork
            GoldenHourPhotoArtwork(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3.5f)
            )

            // Action Row (Like, Comment -> Opens Drawer, Bookmark)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.clickable { postLiked = !postLiked }
                    ) {
                        HeartOutlineOrFilledIcon(
                            filled = postLiked,
                            color = if (postLiked) Rose500 else palette.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = postSummary.likeCountLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textPrimary
                        )
                    }

                    // COMMENT ICON -> Launches Bottom Sheet Drawer
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showCommentsSheet = true }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        CommentBubbleIcon(
                            color = palette.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = liveCommentCount.toString(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textPrimary
                        )
                    }
                }

                BookmarkOutlineOrFilledIcon(
                    filled = postSaved,
                    color = if (postSaved) Blue600 else palette.textPrimary,
                    modifier = Modifier
                        .size(21.dp)
                        .clickable { postSaved = !postSaved }
                )
            }

            // Caption + View all comments trigger
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = buildAnnotatedString {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = palette.textPrimary))
                        append("${postSummary.authorHandle} ")
                        pop()
                        append(postSummary.caption)
                    },
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                    color = palette.textSecondary
                )
                Text(
                    text = "View all $liveCommentCount comments",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = palette.textMuted,
                    modifier = Modifier.clickable { showCommentsSheet = true }
                )
            }
        }
    }

    if (showCommentsSheet) {
        PhotoPostCommentBottomSheet(
            postSummary = postSummary,
            currentUser = DemoCurrentUser,
            onDismissRequest = { showCommentsSheet = false },
            isDarkTheme = isDark,
            onToggleTheme = { isDark = !isDark },
            onCommentCountChanged = { liveCommentCount = it }
        )
    }
}

// ============================================================================
// 4. COMPLETE PHOTO POST COMMENT BOTTOM SHEET DRAWER (DROP-IN COMPOSABLE)
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoPostCommentBottomSheet(
    postSummary: PhotoPostSummary,
    currentUser: CommentAuthor,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    initialComments: List<PostComment> = emptyList(),
    extraCommentsPool: List<PostComment> = emptyList(),
    mentionDirectory: List<CommentAuthor> = emptyList(),
    iconoraOverrides: IconoraMenuIconOverrides = IconoraMenuIconOverrides(),
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    reducedMotion: Boolean = false,
    onToggleTheme: (() -> Unit)? = null,
    onCommentCountChanged: ((Int) -> Unit)? = null,
    onTotalCommentCountChanged: ((Int) -> Unit)? = null,
    onTimestampClick: ((String) -> Unit)? = null,
    onSubmitComment: ((text: String, parentCommentId: String?, media: CommentMediaAttachment?) -> Unit)? = null,
    onToggleLike: ((commentId: String, replyId: String?) -> Unit)? = null,
    onToggleCreatorHeart: ((commentId: String, replyId: String?) -> Unit)? = null,
    onTogglePin: ((commentId: String) -> Unit)? = null,
    onDeleteComment: ((commentId: String, replyId: String?) -> Unit)? = null,
    onVotePoll: ((commentId: String, optionId: String) -> Unit)? = null,
    onReportComment: ((commentId: String, reason: String) -> Unit)? = null
) {
    val palette = remember(isDarkTheme) { commentSheetPalette(isDarkTheme) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val haptics = LocalHapticFeedback.current
    val composerFocusRequester = remember { FocusRequester() }

    // --- Core Feed State ---
    val comments = remember { mutableStateListOf<PostComment>().apply { addAll(initialComments) } }
    LaunchedEffect(initialComments) {
        comments.clear()
        comments.addAll(initialComments)
    }
    val extraPool = remember { mutableStateListOf<PostComment>().apply { addAll(extraCommentsPool) } }
    val revealedSpoilers = remember { mutableStateMapOf<String, Boolean>() }

    // --- Header / Filter / Search / Sort State ---
    var showPostPreview by rememberSaveable { mutableStateOf(true) }
    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var activeFilter by rememberSaveable { mutableStateOf(CommentFilter.ALL) }
    var activeSort by rememberSaveable { mutableStateOf(CommentSort.TOP) }
    var showSortDropdown by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }

    // --- Composer & Reply State ---
    var composerText by rememberSaveable { mutableStateOf("") }
    var replyTarget by remember { mutableStateOf<ReplyTarget?>(null) }
    var showRichTools by remember { mutableStateOf(false) }
    var attachedMedia by remember { mutableStateOf<CommentMediaAttachment?>(null) }
    var attachedPoll by remember { mutableStateOf<CommentPoll?>(null) }
    var attachedVoiceNote by remember { mutableStateOf<CommentVoiceNote?>(null) }
    var isRecordingVoice by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }

    // --- Interactive Overlays State ---
    var activeReactionPickerKey by remember { mutableStateOf<String?>(null) }
    var editingTarget by remember { mutableStateOf<TargetRef?>(null) }
    var menuTarget by remember { mutableStateOf<TargetRef?>(null) }
    var reportTarget by remember { mutableStateOf<TargetRef?>(null) }
    var lightboxMedia by remember { mutableStateOf<CommentMediaAttachment?>(null) }
    var lastDeleted by remember { mutableStateOf<DeletedCommentRecord?>(null) }
    var toastState by remember { mutableStateOf<ToastBannerState?>(null) }

    // --- Voice Note Playback State ---
    var playingVoiceCommentId by remember { mutableStateOf<String?>(null) }
    var isVoicePlaying by remember { mutableStateOf(false) }
    var voiceProgress by remember { mutableStateOf(0f) }
    var voiceSpeedIndex by remember { mutableIntStateOf(0) }

    // --- Floating Emoji Celebrations & Live Typing Presence ---
    val celebrations = remember { mutableStateListOf<FloatingEmojiParticle>() }
    var showLiveTypingIndicator by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(8500)
            showLiveTypingIndicator = !showLiveTypingIndicator
        }
    }

    LaunchedEffect(isRecordingVoice) {
        if (isRecordingVoice) {
            recordingSeconds = 1
            while (isRecordingVoice && recordingSeconds < 60) {
                delay(1000)
                recordingSeconds += 1
            }
        }
    }

    LaunchedEffect(playingVoiceCommentId, isVoicePlaying, voiceSpeedIndex) {
        val activeId = playingVoiceCommentId
        if (activeId != null && isVoicePlaying) {
            val targetComment = comments.find { it.id == activeId }
            val durationSec = targetComment?.voiceNote?.durationSeconds ?: 10
            val speed = VoiceSpeeds[voiceSpeedIndex]
            val stepMs = 60L
            val delta = stepMs.toFloat() / ((durationSec * 1000f) / speed)
            while (isVoicePlaying && playingVoiceCommentId == activeId) {
                delay(stepMs)
                val next = voiceProgress + delta
                if (next >= 1f) {
                    voiceProgress = 0f
                    isVoicePlaying = false
                } else {
                    voiceProgress = next
                }
            }
        }
    }

    val totalCount by remember {
        derivedStateOf { comments.sumOf { 1 + it.replies.size } }
    }

    LaunchedEffect(totalCount) {
        onTotalCommentCountChanged?.invoke(totalCount)
        onCommentCountChanged?.invoke(totalCount)
    }

    fun showToast(message: String, canUndo: Boolean = false) {
        toastState = ToastBannerState(message, canUndo)
        scope.launch {
            delay(if (canUndo) 4800 else 3000)
            if (toastState?.message == message) {
                toastState = null
            }
        }
    }

    fun spawnEmojiCelebration(emoji: String, count: Int = 4) {
        if (reducedMotion) return
        val baseId = System.nanoTime()
        val batch = List(count) { i ->
            FloatingEmojiParticle(
                id = baseId + i,
                emoji = emoji,
                startFractionX = (0.18f + i * 0.19f).coerceIn(0.12f, 0.86f),
                driftDp = if (i % 2 == 0) (14f + i * 5f) else -(14f + i * 5f)
            )
        }
        celebrations.addAll(batch)
        scope.launch {
            delay(1250)
            celebrations.removeAll(batch.toSet())
        }
    }

    fun updateComment(commentId: String, transform: (PostComment) -> PostComment) {
        val idx = comments.indexOfFirst { it.id == commentId }
        if (idx >= 0) comments[idx] = transform(comments[idx])
    }

    fun updateReply(commentId: String, replyId: String, transform: (CommentReply) -> CommentReply) {
        updateComment(commentId) { parent ->
            parent.copy(replies = parent.replies.map { r -> if (r.id == replyId) transform(r) else r })
        }
    }

    val filteredComments by remember {
        derivedStateOf {
            val q = searchQuery.trim().lowercase()
            comments.filter { c ->
                val matchesFilter = when (activeFilter) {
                    CommentFilter.ALL -> true
                    CommentFilter.VERIFIED -> c.author.verified != VerifiedBadgeType.NONE
                    CommentFilter.POLLS -> c.poll != null
                    CommentFilter.VOICE -> c.voiceNote != null
                    CommentFilter.MEDIA -> c.media != null
                    CommentFilter.SAVED -> c.bookmarked
                }
                val matchesQuery = q.isEmpty() ||
                    c.text.lowercase().contains(q) ||
                    c.author.handle.lowercase().contains(q) ||
                    c.author.name.lowercase().contains(q) ||
                    c.replies.any { r ->
                        r.text.lowercase().contains(q) ||
                            r.author.handle.lowercase().contains(q) ||
                            r.author.name.lowercase().contains(q)
                    }
                matchesFilter && matchesQuery
            }.sortedWith { a, b ->
                if (q.isEmpty() && a.pinned != b.pinned) {
                    return@sortedWith if (a.pinned) -1 else 1
                }
                when (activeSort) {
                    CommentSort.NEWEST -> b.createdAtMillis.compareTo(a.createdAtMillis)
                    CommentSort.DISCUSSED -> {
                        val cmp = b.replies.size.compareTo(a.replies.size)
                        if (cmp != 0) cmp else b.likes.compareTo(a.likes)
                    }
                    CommentSort.CREATOR_LIKED -> {
                        if (a.creatorHeart != b.creatorHeart) {
                            if (a.creatorHeart) -1 else 1
                        } else {
                            b.likes.compareTo(a.likes)
                        }
                    }
                    CommentSort.TOP -> (b.likes + b.replies.size * 4).compareTo(a.likes + a.replies.size * 4)
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = palette.sheetBg,
        contentColor = palette.textPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null,
        windowInsets = WindowInsets(0.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .background(palette.sheetBg)
                .imePadding()
                .navigationBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ------------------------------------------------------------
                // A. TOP SHEET DRAG HANDLE & HEADER BAR
                // ------------------------------------------------------------
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(palette.border)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Toggle photo post context strip
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable { showPostPreview = !showPostPreview },
                        contentAlignment = Alignment.Center
                    ) {
                        ChevronDownOrUpIcon(
                            pointingUp = showPostPreview,
                            color = palette.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Title + Rolling Comment Count Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Comments",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Surface(
                            shape = CircleShape,
                            color = palette.surfaceChip
                        ) {
                            RollingCountText(
                                value = totalCount,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.textSecondary,
                                reducedMotion = reducedMotion,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Search, Theme & Close Actions
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable { showSearchBar = !showSearchBar },
                            contentAlignment = Alignment.Center
                        ) {
                            SearchMagnifierIcon(
                                color = if (showSearchBar) Blue600 else palette.textSecondary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        if (onToggleTheme != null) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onToggleTheme),
                                contentAlignment = Alignment.Center
                            ) {
                                ThemeSunMoonIcon(
                                    isDark = isDarkTheme,
                                    color = palette.textSecondary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onDismissRequest),
                            contentAlignment = Alignment.Center
                        ) {
                            CloseCrossIcon(
                                color = palette.textMuted,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                // ------------------------------------------------------------
                // B. COLLAPSIBLE ORIGINAL PHOTO POST CONTEXT STRIP
                // ------------------------------------------------------------
                AnimatedVisibility(visible = showPostPreview) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(palette.surfaceSubtle)
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GoldenHourPhotoArtwork(
                            thumbnailUrl = postSummary.thumbnailUrl,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, palette.border, RoundedCornerShape(8.dp))
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = postSummary.authorHandle,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.textPrimary
                                )
                                VerifiedSealBadge(postSummary.authorVerified, size = 13.dp)
                                Text(
                                    text = "• ${postSummary.relativeTime}",
                                    fontSize = 11.sp,
                                    color = palette.textMuted
                                )
                            }
                            Text(
                                text = postSummary.caption,
                                fontSize = 12.sp,
                                color = palette.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = palette.cardBg,
                            border = BorderStroke(1.dp, palette.border)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                HeartOutlineOrFilledIcon(
                                    filled = true,
                                    color = Rose500,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = postSummary.likeCountLabel,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = palette.textSecondary
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = palette.divider)

                // ------------------------------------------------------------
                // C. FILTER CHIPS + SORT DROPDOWN BAR
                // ------------------------------------------------------------
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CommentFilter.entries.forEach { filter ->
                            val selected = activeFilter == filter
                            Surface(
                                shape = CircleShape,
                                color = if (selected) palette.textPrimary else palette.surfaceChip,
                                modifier = Modifier.clickable { activeFilter = filter }
                            ) {
                                Text(
                                    text = filter.label,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selected) palette.sheetBg else palette.textSecondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Box {
                        Surface(
                            shape = CircleShape,
                            color = palette.surfaceChip,
                            modifier = Modifier.clickable { showSortDropdown = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = activeSort.label,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.textSecondary
                                )
                                ChevronDownOrUpIcon(
                                    pointingUp = showSortDropdown,
                                    color = palette.textMuted,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = showSortDropdown,
                            onDismissRequest = { showSortDropdown = false }
                        ) {
                            CommentSort.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.label,
                                            fontWeight = if (option == activeSort) FontWeight.Bold else FontWeight.Normal,
                                            color = if (option == activeSort) Blue600 else palette.textPrimary
                                        )
                                    },
                                    onClick = {
                                        activeSort = option
                                        showSortDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // ------------------------------------------------------------
                // D. EXPANDABLE LIVE SEARCH BAR
                // ------------------------------------------------------------
                AnimatedVisibility(visible = showSearchBar) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 14.dp, bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(palette.surfaceChip)
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SearchMagnifierIcon(color = palette.textMuted, modifier = Modifier.size(14.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(color = palette.textPrimary, fontSize = 13.sp),
                                cursorBrush = SolidColor(Blue600),
                                modifier = Modifier.weight(1f),
                                decorationBox = { inner ->
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search comments, @users, #tags...",
                                            fontSize = 13.sp,
                                            color = palette.textMuted
                                        )
                                    }
                                    inner()
                                }
                            )
                            if (searchQuery.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .clickable { searchQuery = "" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    CloseCrossIcon(
                                        color = palette.textMuted,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = palette.divider)

                // ------------------------------------------------------------
                // E. SCROLLABLE COMMENTS FEED (Dynamic Edge Fades Only Once Scrolled)
                // ------------------------------------------------------------
                val showTopFade by remember { derivedStateOf { listState.canScrollBackward } }
                val showBottomFade by remember { derivedStateOf { listState.canScrollForward } }
                val topFadeAlpha by animateFloatAsState(
                    targetValue = if (showTopFade) 1f else 0f,
                    animationSpec = tween(220, easing = StageTravelEasing),
                    label = "feedTopEdgeFade"
                )
                val bottomFadeAlpha by animateFloatAsState(
                    targetValue = if (showBottomFade) 1f else 0f,
                    animationSpec = tween(220, easing = StageTravelEasing),
                    label = "feedBottomEdgeFade"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        item(key = "gesture-hint") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Double-tap to like · Swipe right to reply · Tap shimmering spoilers",
                                    fontSize = 11.sp,
                                    color = palette.textMuted
                                )
                            }
                        }

                        if (filteredComments.isEmpty()) {
                            item(key = "empty-feed") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 44.dp, horizontal = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(palette.surfaceChip),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (comments.isEmpty()) Lucide.MessageCircle else Lucide.Search,
                                            contentDescription = null,
                                            tint = palette.textMuted,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Text(
                                        text = if (comments.isEmpty()) "No comments yet" else "No matching comments",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textPrimary
                                    )
                                    Text(
                                        text = if (comments.isEmpty()) "Be the first to share your thoughts!" else "Try clearing your search or switching filters.",
                                        fontSize = 13.sp,
                                        color = palette.textMuted
                                    )
                                    if (comments.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            shape = CircleShape,
                                            color = Blue600,
                                            modifier = Modifier.clickable {
                                                activeFilter = CommentFilter.ALL
                                                searchQuery = ""
                                            }
                                        ) {
                                            Text(
                                                text = "Show all comments",
                                                fontSize = 12.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            items(
                                items = filteredComments,
                                key = { it.id }
                            ) { comment ->
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CommentThreadItem(
                                        comment = comment,
                                        currentUser = currentUser,
                                        palette = palette,
                                        reducedMotion = reducedMotion,
                                        revealedSpoilers = revealedSpoilers,
                                        editingTarget = editingTarget,
                                        activeReactionPickerKey = activeReactionPickerKey,
                                        isVoicePlaying = playingVoiceCommentId == comment.id && isVoicePlaying,
                                        voiceProgress = if (playingVoiceCommentId == comment.id) voiceProgress else 0f,
                                        voiceSpeedLabel = "${VoiceSpeeds[if (playingVoiceCommentId == comment.id) voiceSpeedIndex else 0]}x",
                                        onToggleLike = { ref ->
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            if (ref.replyId == null) {
                                                updateComment(ref.commentId) { c ->
                                                    val nextLiked = !c.likedByMe
                                                    c.copy(
                                                        likedByMe = nextLiked,
                                                        likes = (c.likes + if (nextLiked) 1 else -1).coerceAtLeast(0)
                                                    )
                                                }
                                            } else {
                                                updateReply(ref.commentId, ref.replyId) { r ->
                                                    val nextLiked = !r.likedByMe
                                                    r.copy(
                                                        likedByMe = nextLiked,
                                                        likes = (r.likes + if (nextLiked) 1 else -1).coerceAtLeast(0)
                                                    )
                                                }
                                            }
                                            onToggleLike?.invoke(ref.commentId, ref.replyId)
                                        },
                                        onDoubleTapLike = { ref ->
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            if (ref.replyId == null) {
                                                updateComment(ref.commentId) { c ->
                                                    if (c.likedByMe) c else c.copy(likedByMe = true, likes = c.likes + 1)
                                                }
                                            } else {
                                                updateReply(ref.commentId, ref.replyId) { r ->
                                                    if (r.likedByMe) r else r.copy(likedByMe = true, likes = r.likes + 1)
                                                }
                                            }
                                            onToggleLike?.invoke(ref.commentId, ref.replyId)
                                            spawnEmojiCelebration("❤️", count = 3)
                                        },
                                        onToggleCreatorHeart = { ref ->
                                            if (ref.replyId == null) {
                                                updateComment(ref.commentId) { it.copy(creatorHeart = !it.creatorHeart) }
                                            } else {
                                                updateReply(ref.commentId, ref.replyId) { it.copy(creatorHeart = !it.creatorHeart) }
                                            }
                                            onToggleCreatorHeart?.invoke(ref.commentId, ref.replyId)
                                        },
                                        onToggleReaction = { ref, emoji ->
                                            fun toggleMap(map: Map<String, ReactionData>): Map<String, ReactionData> {
                                                val mutable = map.toMutableMap()
                                                val prev = mutable[emoji] ?: ReactionData(0, false)
                                                if (prev.activeByMe) {
                                                    val newCount = (prev.count - 1).coerceAtLeast(0)
                                                    if (newCount == 0) mutable.remove(emoji) else mutable[emoji] = ReactionData(newCount, false)
                                                } else {
                                                    mutable[emoji] = ReactionData(prev.count + 1, true)
                                                    spawnEmojiCelebration(emoji, count = 4)
                                                }
                                                return mutable
                                            }
                                            if (ref.replyId == null) {
                                                updateComment(ref.commentId) { it.copy(reactions = toggleMap(it.reactions)) }
                                            } else {
                                                updateReply(ref.commentId, ref.replyId) { it.copy(reactions = toggleMap(it.reactions)) }
                                            }
                                            activeReactionPickerKey = null
                                        },
                                        onOpenReactionPicker = { key ->
                                            activeReactionPickerKey = if (activeReactionPickerKey == key) null else key
                                        },
                                        onReplyClick = { commentId, handle ->
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            replyTarget = ReplyTarget(commentId, handle)
                                            scope.launch {
                                                runCatching { composerFocusRequester.requestFocus() }
                                            }
                                        },
                                        onToggleReplies = { commentId ->
                                            updateComment(commentId) { it.copy(repliesExpanded = !it.repliesExpanded) }
                                        },
                                        onShowMoreReplies = { commentId ->
                                            updateComment(commentId) { it.copy(visibleRepliesCount = it.visibleRepliesCount + 5) }
                                        },
                                        onToggleExpand = { commentId ->
                                            updateComment(commentId) { it.copy(expanded = !it.expanded) }
                                        },
                                        onToggleTranslate = { commentId ->
                                            updateComment(commentId) { it.copy(showTranslation = !it.showTranslation) }
                                        },
                                        onOpenMedia = { media -> lightboxMedia = media },
                                        onOpenMenu = { ref -> menuTarget = ref },
                                        onSaveEdit = { ref, newText ->
                                            val cleaned = newText.trim()
                                            if (cleaned.isNotEmpty()) {
                                                if (ref.replyId == null) {
                                                    updateComment(ref.commentId) { it.copy(text = cleaned, edited = true) }
                                                } else {
                                                    updateReply(ref.commentId, ref.replyId) { it.copy(text = cleaned, edited = true) }
                                                }
                                                editingTarget = null
                                                showToast("Comment updated")
                                            }
                                        },
                                        onCancelEdit = { editingTarget = null },
                                        onVotePoll = { commentId, optionId ->
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            updateComment(commentId) { parent ->
                                                val poll = parent.poll ?: return@updateComment parent
                                                val prev = poll.selectedOptionId
                                                if (prev == optionId) return@updateComment parent
                                                val updatedOptions = poll.options.map { opt ->
                                                    when (opt.id) {
                                                        optionId -> opt.copy(votes = opt.votes + 1)
                                                        prev -> opt.copy(votes = (opt.votes - 1).coerceAtLeast(0))
                                                        else -> opt
                                                    }
                                                }
                                                parent.copy(poll = poll.copy(options = updatedOptions, selectedOptionId = optionId))
                                            }
                                            onVotePoll?.invoke(commentId, optionId)
                                            showToast("Vote recorded")
                                        },
                                        onToggleVoicePlay = { commentId ->
                                            if (playingVoiceCommentId != commentId) {
                                                playingVoiceCommentId = commentId
                                                voiceProgress = 0f
                                                isVoicePlaying = true
                                            } else {
                                                isVoicePlaying = !isVoicePlaying
                                            }
                                        },
                                        onScrubVoice = { commentId, fraction ->
                                            playingVoiceCommentId = commentId
                                            voiceProgress = fraction.coerceIn(0f, 1f)
                                            isVoicePlaying = true
                                        },
                                        onCycleVoiceSpeed = { commentId ->
                                            if (playingVoiceCommentId != commentId) {
                                                playingVoiceCommentId = commentId
                                                voiceProgress = 0f
                                            }
                                            voiceSpeedIndex = (voiceSpeedIndex + 1) % VoiceSpeeds.size
                                        },
                                        onSpoilerRevealed = {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        },
                                        onMentionOrTagClick = { term ->
                                            showSearchBar = true
                                            searchQuery = term
                                            showToast("Filtered by $term")
                                        },
                                        onTimestampClick = { ts ->
                                            onTimestampClick?.invoke(ts)
                                            showToast("Jumped to $ts")
                                        }
                                    )
                                    HorizontalDivider(color = palette.divider)
                                }
                            }

                            // Load More Button
                            if (extraPool.isNotEmpty() && activeFilter == CommentFilter.ALL && searchQuery.isEmpty()) {
                                item(key = "load-more") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = palette.surfaceChip,
                                            modifier = Modifier.clickable(enabled = !isLoadingMore) {
                                                isLoadingMore = true
                                                scope.launch {
                                                    delay(450)
                                                    val batch = extraPool.take(2)
                                                    extraPool.removeAll(batch.toSet())
                                                    comments.addAll(batch)
                                                    isLoadingMore = false
                                                    showToast("Loaded ${batch.size} more comments")
                                                }
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = if (isLoadingMore) "Loading..." else "View more comments",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = palette.textSecondary
                                                )
                                                Surface(
                                                    shape = CircleShape,
                                                    color = palette.cardBg
                                                ) {
                                                    Text(
                                                        text = extraPool.size.toString(),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = palette.textMuted,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Subtle Live Typing Indicator at Bottom of Feed
                            if (showLiveTypingIndicator && searchQuery.isEmpty() && activeFilter == CommentFilter.ALL) {
                                item(key = "live-typing") {
                                    LiveTypingIndicatorRow(
                                        handle = "mayalin",
                                        avatarStyle = AvatarStyle.ROYAL_BLUE,
                                        palette = palette,
                                        reducedMotion = reducedMotion
                                    )
                                }
                            }
                        }
                    }

                    // Stageshow Scroll Craft: soften only edges actively hiding scrolled content
                    if (topFadeAlpha > 0.01f) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .height(20.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            palette.sheetBg.copy(alpha = 0.92f * topFadeAlpha),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }
                    if (bottomFadeAlpha > 0.01f) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(20.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            palette.sheetBg.copy(alpha = 0.92f * bottomFadeAlpha)
                                        )
                                    )
                                )
                        )
                    }
                }

                // ------------------------------------------------------------
                // F. BOTTOM-DOCKED SIGNATURE COMPOSER BAR
                //    (Avatar + Animated Underline + Custom Filled Send Button)
                // ------------------------------------------------------------
                DockedCommentComposer(
                    text = composerText,
                    onTextChange = { composerText = it },
                    currentUser = currentUser,
                    palette = palette,
                    mentionDirectory = mentionDirectory,
                    replyTarget = replyTarget,
                    showRichTools = showRichTools,
                    attachedMedia = attachedMedia,
                    attachedPoll = attachedPoll,
                    attachedVoiceNote = attachedVoiceNote,
                    isRecordingVoice = isRecordingVoice,
                    recordingSeconds = recordingSeconds,
                    focusRequester = composerFocusRequester,
                    onToggleRichTools = { showRichTools = !showRichTools },
                    onQuickEmojiClick = { emoji ->
                        composerText += emoji
                        spawnEmojiCelebration(emoji, count = 4)
                    },
                    onCancelReply = { replyTarget = null },
                    onCycleAttachMedia = {
                        val nextTheme = if (attachedMedia?.theme == MediaCardTheme.MOCKUP) {
                            MediaCardTheme.BLUEPRINT
                        } else {
                            MediaCardTheme.MOCKUP
                        }
                        attachedMedia = CommentMediaAttachment(
                            theme = nextTheme,
                            title = if (nextTheme == MediaCardTheme.MOCKUP) "Composition Snapshot" else "Color Curve Spec",
                            caption = "Attached from the photo post comment bar."
                        )
                    },
                    onRemoveMedia = { attachedMedia = null },
                    onToggleVoiceRecord = {
                        if (isRecordingVoice) {
                            isRecordingVoice = false
                            attachedVoiceNote = CommentVoiceNote(
                                durationSeconds = recordingSeconds.coerceAtLeast(2),
                                waveform = listOf(
                                    0.28f, 0.52f, 0.85f, 0.64f, 0.40f, 0.92f, 0.72f, 0.56f,
                                    0.34f, 0.78f, 0.94f, 0.66f, 0.46f, 0.82f, 0.54f, 0.38f,
                                    0.70f, 0.86f, 0.48f, 0.30f, 0.60f, 0.42f, 0.30f, 0.22f
                                )
                            )
                            showToast("Voice note attached")
                        } else {
                            attachedVoiceNote = null
                            isRecordingVoice = true
                        }
                    },
                    onCancelVoiceNote = {
                        isRecordingVoice = false
                        attachedVoiceNote = null
                    },
                    onAttachQuickPoll = {
                        attachedPoll = CommentPoll(
                            question = "Rate this photo shot!",
                            options = listOf(
                                CommentPollOption("qp-1", "Pure Magic", 1),
                                CommentPollOption("qp-2", "Clean Framing", 0),
                                CommentPollOption("qp-3", "Great Colors", 0)
                            )
                        )
                        showToast("Quick poll attached")
                    },
                    onRemovePoll = { attachedPoll = null },
                    onSend = {
                        val raw = composerText.trim()
                        val hasExtra = attachedMedia != null || attachedVoiceNote != null || attachedPoll != null
                        if (raw.isEmpty() && !hasExtra) return@DockedCommentComposer

                        val activeReply = replyTarget
                        if (activeReply != null) {
                            val prefixed = if (raw.startsWith("@")) raw else "@${activeReply.handle} $raw"
                            val newReply = CommentReply(
                                id = "r-${System.currentTimeMillis()}",
                                author = currentUser,
                                text = prefixed.trim(),
                                createdAtMillis = System.currentTimeMillis()
                            )
                            updateComment(activeReply.commentId) { parent ->
                                parent.copy(
                                    replies = parent.replies + newReply,
                                    repliesExpanded = true,
                                    visibleRepliesCount = maxOf(parent.visibleRepliesCount, parent.replies.size + 1)
                                )
                            }
                            onSubmitComment?.invoke(prefixed.trim(), activeReply.commentId, null)
                            replyTarget = null
                            showToast("Reply posted")
                        } else {
                            val fallback = when {
                                attachedVoiceNote != null -> "Shared a voice note"
                                attachedPoll != null -> "Started a quick poll"
                                else -> "Shared an attachment"
                            }
                            val finalText = raw.ifEmpty { fallback }
                            val newComment = PostComment(
                                id = "c-${System.currentTimeMillis()}",
                                author = currentUser,
                                text = finalText,
                                createdAtMillis = System.currentTimeMillis(),
                                media = attachedMedia,
                                poll = attachedPoll,
                                voiceNote = attachedVoiceNote
                            )
                            comments.add(0, newComment)
                            onSubmitComment?.invoke(finalText, null, attachedMedia)
                            showToast("Comment posted")
                            scope.launch { listState.animateScrollToItem(0) }
                        }

                        composerText = ""
                        attachedMedia = null
                        attachedPoll = null
                        attachedVoiceNote = null
                        isRecordingVoice = false
                        showRichTools = false
                    }
                )
            }

            // Floating Emoji Celebration Overlay
            FloatingEmojiCelebrationOverlay(
                particles = celebrations,
                modifier = Modifier.fillMaxSize()
            )

            // Floating Toast Banner with Undo Support
            androidx.compose.animation.AnimatedVisibility(
                visible = toastState != null,
                enter = fadeIn(tween(240, easing = StageArrivalEasing)) +
                    slideInVertically(tween(280, easing = StageArrivalEasing)) { it / 2 },
                exit = fadeOut(tween(220, easing = StageTravelEasing)) +
                    slideOutVertically(tween(240, easing = StageTravelEasing)) { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 118.dp, start = 20.dp, end = 20.dp)
            ) {
                toastState?.let { toast ->
                    Surface(
                        shape = CircleShape,
                        color = if (palette.isDark) Color(0xFFF4F4F5) else Color(0xFF18181B),
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = toast.message,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (palette.isDark) Color(0xFF18181B) else Color.White
                            )
                            if (toast.canUndo && lastDeleted != null) {
                                Text(
                                    text = "Undo",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (palette.isDark) Blue600 else Blue400,
                                    modifier = Modifier.clickable {
                                        val rec = lastDeleted ?: return@clickable
                                        if (rec.parentCommentId == null && rec.comment != null) {
                                            comments.add(rec.index.coerceIn(0, comments.size), rec.comment)
                                        } else if (rec.parentCommentId != null && rec.reply != null) {
                                            updateComment(rec.parentCommentId) { parent ->
                                                val mutable = parent.replies.toMutableList()
                                                mutable.add(rec.index.coerceIn(0, mutable.size), rec.reply)
                                                parent.copy(replies = mutable)
                                            }
                                        }
                                        lastDeleted = null
                                        toastState = null
                                        showToast("Comment restored")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ========================================================================
    // G. ICONORA STUDIO 3D LIQUID GLASS COMMENT OPTIONS SHEET
    // ========================================================================
    menuTarget?.let { ref ->
        val parentComment = comments.find { it.id == ref.commentId }
        val targetReply = ref.replyId?.let { rid -> parentComment?.replies?.find { it.id == rid } }
        val isReply = targetReply != null
        val isSelf = targetReply?.author?.isSelf ?: parentComment?.author?.isSelf ?: false
        val hasCreatorHeart = targetReply?.creatorHeart ?: parentComment?.creatorHeart ?: false
        val isPinned = parentComment?.pinned == true
        val isBookmarked = parentComment?.bookmarked == true
        val hasTranslation = !isReply && parentComment?.translation != null
        val showingTranslation = parentComment?.showTranslation == true
        val rawText = targetReply?.text ?: parentComment?.text ?: ""

        if (parentComment != null) {
            CommentMenuOptionsSheet(
                palette = palette,
                iconoraOverrides = iconoraOverrides,
                isReply = isReply,
                isSelf = isSelf,
                hasCreatorHeart = hasCreatorHeart,
                isPinned = isPinned,
                isBookmarked = isBookmarked,
                hasTranslation = hasTranslation,
                showingTranslation = showingTranslation,
                onDismiss = { menuTarget = null },
                onAction = { action ->
                    menuTarget = null
                    when (action) {
                        IconoraMenuIconType.CREATOR_HEART -> {
                            if (ref.replyId == null) {
                                updateComment(ref.commentId) { it.copy(creatorHeart = !it.creatorHeart) }
                            } else {
                                updateReply(ref.commentId, ref.replyId) { it.copy(creatorHeart = !it.creatorHeart) }
                            }
                            onToggleCreatorHeart?.invoke(ref.commentId, ref.replyId)
                            showToast(if (hasCreatorHeart) "Removed Creator Heart" else "Added Creator Heart")
                        }
                        IconoraMenuIconType.PIN -> {
                            val willPin = !isPinned
                            for (i in comments.indices) {
                                comments[i] = comments[i].copy(pinned = comments[i].id == ref.commentId && willPin)
                            }
                            onTogglePin?.invoke(ref.commentId)
                            showToast(if (willPin) "Pinned to top" else "Comment unpinned")
                        }
                        IconoraMenuIconType.BOOKMARK -> {
                            updateComment(ref.commentId) { it.copy(bookmarked = !it.bookmarked) }
                            showToast(if (isBookmarked) "Removed from saved" else "Saved comment")
                        }
                        IconoraMenuIconType.TRANSLATE -> {
                            updateComment(ref.commentId) { it.copy(showTranslation = !it.showTranslation) }
                        }
                        IconoraMenuIconType.COPY_TEXT -> {
                            clipboard.setText(AnnotatedString(rawText))
                            showToast("Comment text copied")
                        }
                        IconoraMenuIconType.SHARE_LINK -> {
                            clipboard.setText(AnnotatedString("https://app.example.com/post/photo#${ref.replyId ?: ref.commentId}"))
                            showToast("Comment link copied")
                        }
                        IconoraMenuIconType.EDIT -> {
                            editingTarget = ref
                        }
                        IconoraMenuIconType.DELETE -> {
                            if (ref.replyId == null) {
                                val idx = comments.indexOfFirst { it.id == ref.commentId }
                                if (idx >= 0) {
                                    val removed = comments.removeAt(idx)
                                    lastDeleted = DeletedCommentRecord(null, idx, comment = removed)
                                    onDeleteComment?.invoke(ref.commentId, null)
                                }
                            } else {
                                val parentIdx = comments.indexOfFirst { it.id == ref.commentId }
                                if (parentIdx >= 0) {
                                    val p = comments[parentIdx]
                                    val rIdx = p.replies.indexOfFirst { it.id == ref.replyId }
                                    if (rIdx >= 0) {
                                        val removed = p.replies[rIdx]
                                        comments[parentIdx] = p.copy(replies = p.replies.filterNot { it.id == ref.replyId })
                                        lastDeleted = DeletedCommentRecord(ref.commentId, rIdx, reply = removed)
                                        onDeleteComment?.invoke(ref.commentId, ref.replyId)
                                    }
                                }
                            }
                            showToast("Comment deleted", canUndo = true)
                        }
                        IconoraMenuIconType.REPORT -> {
                            reportTarget = ref
                        }
                    }
                }
            )
        }
    }

    // Report Dialog
    reportTarget?.let {
        ReportCommentDialog(
            palette = palette,
            onDismiss = { reportTarget = null },
            onSubmit = { reason ->
                onReportComment?.invoke(reportTarget?.commentId ?: "", reason)
                reportTarget = null
                showToast("Reported: $reason")
            }
        )
    }

    // Media Attachment Lightbox Dialog
    lightboxMedia?.let { media ->
        Dialog(onDismissRequest = { lightboxMedia = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF141416),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Column {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        MediaAttachmentCardGraphic(
                            theme = media.theme,
                            title = media.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(640f / 300f)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f))
                                .clickable { lightboxMedia = null },
                            contentAlignment = Alignment.Center
                        ) {
                            CloseCrossIcon(color = Color.White, modifier = Modifier.size(13.dp))
                        }
                    }
                    Text(
                        text = media.caption,
                        fontSize = 12.sp,
                        color = Color(0xFFD4D4D8),
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
    }
}

// ============================================================================
// 5. ICONORA STUDIO 3D LIQUID GLASS MENU OPTIONS SHEET
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommentMenuOptionsSheet(
    palette: CommentSheetPalette,
    iconoraOverrides: IconoraMenuIconOverrides,
    isReply: Boolean,
    isSelf: Boolean,
    hasCreatorHeart: Boolean,
    isPinned: Boolean,
    isBookmarked: Boolean,
    hasTranslation: Boolean,
    showingTranslation: Boolean,
    onDismiss: () -> Unit,
    onAction: (IconoraMenuIconType) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.cardBg,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Comment Options",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.textMuted,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )

            MenuOptionRow(
                type = IconoraMenuIconType.CREATOR_HEART,
                label = if (hasCreatorHeart) "Remove Creator Heart" else "Give Creator Heart",
                palette = palette,
                overrides = iconoraOverrides,
                onClick = { onAction(IconoraMenuIconType.CREATOR_HEART) }
            )
            if (!isReply) {
                MenuOptionRow(
                    type = IconoraMenuIconType.PIN,
                    label = if (isPinned) "Unpin comment" else "Pin to top",
                    palette = palette,
                    overrides = iconoraOverrides,
                    onClick = { onAction(IconoraMenuIconType.PIN) }
                )
                MenuOptionRow(
                    type = IconoraMenuIconType.BOOKMARK,
                    label = if (isBookmarked) "Remove from saved" else "Save comment",
                    palette = palette,
                    overrides = iconoraOverrides,
                    onClick = { onAction(IconoraMenuIconType.BOOKMARK) }
                )
                if (hasTranslation) {
                    MenuOptionRow(
                        type = IconoraMenuIconType.TRANSLATE,
                        label = if (showingTranslation) "Show original text" else "Translate comment",
                        palette = palette,
                        overrides = iconoraOverrides,
                        onClick = { onAction(IconoraMenuIconType.TRANSLATE) }
                    )
                }
            }
            MenuOptionRow(
                type = IconoraMenuIconType.COPY_TEXT,
                label = "Copy text",
                palette = palette,
                overrides = iconoraOverrides,
                onClick = { onAction(IconoraMenuIconType.COPY_TEXT) }
            )
            MenuOptionRow(
                type = IconoraMenuIconType.SHARE_LINK,
                label = "Copy comment link",
                palette = palette,
                overrides = iconoraOverrides,
                onClick = { onAction(IconoraMenuIconType.SHARE_LINK) }
            )
            if (isSelf) {
                MenuOptionRow(
                    type = IconoraMenuIconType.EDIT,
                    label = "Edit comment",
                    palette = palette,
                    overrides = iconoraOverrides,
                    onClick = { onAction(IconoraMenuIconType.EDIT) }
                )
                MenuOptionRow(
                    type = IconoraMenuIconType.DELETE,
                    label = "Delete comment",
                    danger = true,
                    palette = palette,
                    overrides = iconoraOverrides,
                    onClick = { onAction(IconoraMenuIconType.DELETE) }
                )
            } else {
                MenuOptionRow(
                    type = IconoraMenuIconType.REPORT,
                    label = "Report comment",
                    danger = true,
                    palette = palette,
                    overrides = iconoraOverrides,
                    onClick = { onAction(IconoraMenuIconType.REPORT) }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.surfaceChip,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDismiss)
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
private fun MenuOptionRow(
    type: IconoraMenuIconType,
    label: String,
    palette: CommentSheetPalette,
    overrides: IconoraMenuIconOverrides,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val overrideRes = overrides.resolve(type)
        if (overrideRes != null) {
            Image(
                painter = painterResource(id = overrideRes),
                contentDescription = label,
                modifier = Modifier.size(32.dp)
            )
        } else {
            IconoraLiquidGlassIcon(
                type = type,
                isDark = palette.isDark,
                modifier = Modifier.size(32.dp)
            )
        }
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (danger) Rose500 else palette.textPrimary
        )
    }
}

/**
 * Iconora Studio (https://iconorastudio.com/) inspired 3D Liquid Glass menu icons.
 * Features a soft neutral grounding shadow, translucent specular top sheen, and luminous rim.
 */
@Composable
private fun IconoraLiquidGlassIcon(
    type: IconoraMenuIconType,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val (topColor, bottomColor, rimColor) = when (type) {
        IconoraMenuIconType.CREATOR_HEART -> Triple(Color(0xFFFB7185), Color(0xFFE11D48), Color(0xFFFDA4AF))
        IconoraMenuIconType.PIN -> Triple(Color(0xFF60A5FA), Color(0xFF1D4ED8), Color(0xFF93C5FD))
        IconoraMenuIconType.BOOKMARK -> Triple(Color(0xFFFBBF24), Color(0xFFD97706), Color(0xFFFDE68A))
        IconoraMenuIconType.TRANSLATE -> Triple(Color(0xFF34D399), Color(0xFF047857), Color(0xFFA7F3D0))
        IconoraMenuIconType.COPY_TEXT -> Triple(Color(0xFF38BDF8), Color(0xFF0369A1), Color(0xFFBAE6FD))
        IconoraMenuIconType.SHARE_LINK -> Triple(Color(0xFF22D3EE), Color(0xFF0E7490), Color(0xFFA5F3FC))
        IconoraMenuIconType.EDIT -> Triple(Color(0xFFA78BFA), Color(0xFF6D28D9), Color(0xFFDDD6FE))
        IconoraMenuIconType.DELETE -> Triple(Color(0xFFF87171), Color(0xFFB91C1C), Color(0xFFFECACA))
        IconoraMenuIconType.REPORT -> Triple(Color(0xFFFB923C), Color(0xFFC2410C), Color(0xFFFED7AA))
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val corner = CornerRadius(w * 0.30f, h * 0.30f)

        // 1. Soft neutral grounding shadow (Stageshow craft: avoid neon colored glow)
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Black.copy(alpha = if (isDark) 0.36f else 0.16f),
                    Color.Transparent
                ),
                center = Offset(w * 0.5f, h * 0.58f),
                radius = w * 0.58f
            ),
            size = size,
            cornerRadius = corner
        )

        // 2. 3D Liquid Glass Squircle Tile
        val inset = w * 0.06f
        val tileSize = Size(w - inset * 2f, h - inset * 2f)
        val tileOffset = Offset(inset, inset)
        drawRoundRect(
            brush = Brush.linearGradient(listOf(topColor, bottomColor)),
            topLeft = tileOffset,
            size = tileSize,
            cornerRadius = corner
        )

        // 3. Top Glass Specular Sheen
        drawRoundRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0.0f to Color.White.copy(alpha = 0.54f),
                    0.44f to Color.White.copy(alpha = 0.12f),
                    0.48f to Color.Transparent,
                    1.0f to Color.Black.copy(alpha = 0.18f)
                )
            ),
            topLeft = tileOffset,
            size = tileSize,
            cornerRadius = corner
        )

        // 4. Specular Rim Bevel
        drawRoundRect(
            brush = Brush.linearGradient(
                listOf(Color.White.copy(alpha = 0.85f), rimColor.copy(alpha = 0.4f), Color.White.copy(alpha = 0.25f))
            ),
            topLeft = tileOffset,
            size = tileSize,
            cornerRadius = corner,
            style = Stroke(width = w * 0.04f)
        )

        // 5. Crisp Inner White Glyph
        val sx = w / 24f
        val sy = h / 24f
        val glyphBrush = Brush.verticalGradient(listOf(Color.White, Color.White.copy(alpha = 0.82f)))
        val stroke = Stroke(width = 1.9f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round)

        when (type) {
            IconoraMenuIconType.CREATOR_HEART -> {
                val p = Path().apply {
                    moveTo(12f * sx, 18.2f * sy)
                    cubicTo(12f * sx, 18.2f * sy, 6.2f * sx, 14.3f * sy, 6.2f * sx, 10.2f * sy)
                    cubicTo(6.2f * sx, 8.1f * sy, 7.8f * sx, 6.7f * sy, 9.6f * sx, 6.7f * sy)
                    cubicTo(10.8f * sx, 6.7f * sy, 11.6f * sx, 7.4f * sy, 12f * sx, 8.2f * sy)
                    cubicTo(12.4f * sx, 7.4f * sy, 13.2f * sx, 6.7f * sy, 14.4f * sx, 6.7f * sy)
                    cubicTo(16.2f * sx, 6.7f * sy, 17.8f * sx, 8.1f * sy, 17.8f * sx, 10.2f * sy)
                    cubicTo(17.8f * sx, 14.3f * sy, 12f * sx, 18.2f * sy, 12f * sx, 18.2f * sy)
                    close()
                }
                drawPath(p, brush = glyphBrush)
            }
            IconoraMenuIconType.PIN -> {
                val head = Path().apply {
                    moveTo(9.2f * sx, 6.5f * sy)
                    lineTo(14.8f * sx, 6.5f * sy)
                    lineTo(14.2f * sx, 11.2f * sy)
                    lineTo(16.5f * sx, 13.4f * sy)
                    lineTo(7.5f * sx, 13.4f * sy)
                    lineTo(9.8f * sx, 11.2f * sy)
                    close()
                }
                drawPath(head, brush = glyphBrush)
                drawLine(Color.White, Offset(12f * sx, 13.4f * sy), Offset(12f * sx, 18.5f * sy), strokeWidth = 2f * sx, cap = StrokeCap.Round)
            }
            IconoraMenuIconType.BOOKMARK -> {
                val p = Path().apply {
                    moveTo(8.2f * sx, 6.4f * sy)
                    lineTo(15.8f * sx, 6.4f * sy)
                    lineTo(15.8f * sx, 18f * sy)
                    lineTo(12f * sx, 15.2f * sy)
                    lineTo(8.2f * sx, 18f * sy)
                    close()
                }
                drawPath(p, brush = glyphBrush)
            }
            IconoraMenuIconType.TRANSLATE -> {
                drawCircle(Color.White, radius = 5.4f * sx, center = Offset(12f * sx, 12f * sy), style = stroke)
                drawLine(Color.White, Offset(6.8f * sx, 12f * sy), Offset(17.2f * sx, 12f * sy), strokeWidth = 1.5f * sx)
                drawOval(
                    color = Color.White,
                    topLeft = Offset(9.5f * sx, 6.6f * sy),
                    size = Size(5f * sx, 10.8f * sy),
                    style = Stroke(width = 1.4f * sx)
                )
            }
            IconoraMenuIconType.COPY_TEXT -> {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.55f),
                    topLeft = Offset(7.2f * sx, 6.6f * sy),
                    size = Size(7.6f * sx, 9.2f * sy),
                    cornerRadius = CornerRadius(2f * sx, 2f * sy)
                )
                drawRoundRect(
                    brush = glyphBrush,
                    topLeft = Offset(9.4f * sx, 8.6f * sy),
                    size = Size(7.6f * sx, 9.2f * sy),
                    cornerRadius = CornerRadius(2f * sx, 2f * sy)
                )
            }
            IconoraMenuIconType.SHARE_LINK -> {
                drawLine(Color.White, Offset(9.4f * sx, 14.6f * sy), Offset(14.6f * sx, 9.4f * sy), strokeWidth = 2f * sx, cap = StrokeCap.Round)
                drawCircle(Color.White, radius = 2.6f * sx, center = Offset(8.5f * sx, 15.5f * sy), style = stroke)
                drawCircle(Color.White, radius = 2.6f * sx, center = Offset(15.5f * sx, 8.5f * sy), style = stroke)
            }
            IconoraMenuIconType.EDIT -> {
                val p = Path().apply {
                    moveTo(15.2f * sx, 6.5f * sy)
                    lineTo(17.5f * sx, 8.8f * sy)
                    lineTo(9.8f * sx, 16.5f * sy)
                    lineTo(6.8f * sx, 17.2f * sy)
                    lineTo(7.5f * sx, 14.2f * sy)
                    close()
                }
                drawPath(p, brush = glyphBrush)
            }
            IconoraMenuIconType.DELETE -> {
                drawLine(Color.White, Offset(7.2f * sx, 8.4f * sy), Offset(16.8f * sx, 8.4f * sy), strokeWidth = 1.9f * sx, cap = StrokeCap.Round)
                drawLine(Color.White, Offset(10.2f * sx, 6.6f * sy), Offset(13.8f * sx, 6.6f * sy), strokeWidth = 1.8f * sx, cap = StrokeCap.Round)
                val bin = Path().apply {
                    moveTo(8.4f * sx, 9.4f * sy)
                    lineTo(15.6f * sx, 9.4f * sy)
                    lineTo(14.8f * sx, 17.6f * sy)
                    lineTo(9.2f * sx, 17.6f * sy)
                    close()
                }
                drawPath(bin, brush = glyphBrush)
            }
            IconoraMenuIconType.REPORT -> {
                val flag = Path().apply {
                    moveTo(8.5f * sx, 6.8f * sy)
                    lineTo(16.6f * sx, 6.8f * sy)
                    lineTo(14.6f * sx, 10.2f * sy)
                    lineTo(16.6f * sx, 13.6f * sy)
                    lineTo(8.5f * sx, 13.6f * sy)
                    close()
                }
                drawPath(flag, brush = glyphBrush)
                drawLine(Color.White, Offset(8.5f * sx, 6.6f * sy), Offset(8.5f * sx, 18.2f * sy), strokeWidth = 2f * sx, cap = StrokeCap.Round)
            }
        }
    }
}

// ============================================================================
// 6. COMMENT THREAD ITEM (SWIPE-TO-REPLY, DOUBLE-TAP HEART, POLLS, VOICE, SPOILERS)
// ============================================================================
@Composable
private fun CommentThreadItem(
    comment: PostComment,
    currentUser: CommentAuthor,
    palette: CommentSheetPalette,
    reducedMotion: Boolean,
    revealedSpoilers: MutableMap<String, Boolean>,
    editingTarget: TargetRef?,
    activeReactionPickerKey: String?,
    isVoicePlaying: Boolean,
    voiceProgress: Float,
    voiceSpeedLabel: String,
    onToggleLike: (TargetRef) -> Unit,
    onDoubleTapLike: (TargetRef) -> Unit,
    onToggleCreatorHeart: (TargetRef) -> Unit,
    onToggleReaction: (TargetRef, String) -> Unit,
    onOpenReactionPicker: (String) -> Unit,
    onReplyClick: (commentId: String, handle: String) -> Unit,
    onToggleReplies: (String) -> Unit,
    onShowMoreReplies: (String) -> Unit,
    onToggleExpand: (String) -> Unit,
    onToggleTranslate: (String) -> Unit,
    onOpenMedia: (CommentMediaAttachment) -> Unit,
    onOpenMenu: (TargetRef) -> Unit,
    onSaveEdit: (TargetRef, String) -> Unit,
    onCancelEdit: () -> Unit,
    onVotePoll: (commentId: String, optionId: String) -> Unit,
    onToggleVoicePlay: (String) -> Unit,
    onScrubVoice: (String, Float) -> Unit,
    onCycleVoiceSpeed: (String) -> Unit,
    onSpoilerRevealed: () -> Unit,
    onMentionOrTagClick: (String) -> Unit,
    onTimestampClick: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val swipeOffsetX = remember { Animatable(0f) }
    val maxSwipePx = with(density) { 64.dp.toPx() }
    val triggerSwipePx = with(density) { 44.dp.toPx() }
    var doubleTapBurstNonce by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        if (comment.pinned) {
            Row(
                modifier = Modifier.padding(start = 42.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Pinned",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Blue600
                )
            }
        }

        // Swipe-right-to-reply container + Double-tap heart burst overlay
        Box(modifier = Modifier.fillMaxWidth()) {
            val swipeProgress = (swipeOffsetX.value / triggerSwipePx).coerceIn(0f, 1f)
            if (swipeProgress > 0.05f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 6.dp)
                        .size(32.dp)
                        .graphicsLayer {
                            alpha = swipeProgress
                            scaleX = 0.55f + swipeProgress * 0.45f
                            scaleY = 0.55f + swipeProgress * 0.45f
                        }
                        .clip(CircleShape)
                        .background(if (swipeProgress >= 1f) Blue600 else Blue600.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    ReplyArrowIcon(
                        color = if (swipeProgress >= 1f) Color.White else Blue600,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(swipeOffsetX.value.roundToInt(), 0) }
                    .background(palette.sheetBg)
                    .pointerInput(comment.id) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { _, dragAmount ->
                                val next = (swipeOffsetX.value + dragAmount * 0.72f).coerceIn(0f, maxSwipePx)
                                scope.launch { swipeOffsetX.snapTo(next) }
                            },
                            onDragEnd = {
                                val triggered = swipeOffsetX.value >= triggerSwipePx
                                scope.launch {
                                    swipeOffsetX.animateTo(
                                        0f,
                                        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                                    )
                                }
                                if (triggered) onReplyClick(comment.id, comment.author.handle)
                            },
                            onDragCancel = {
                                scope.launch { swipeOffsetX.animateTo(0f, tween(220)) }
                            }
                        )
                    },
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MacProfileAvatar(
                    style = comment.author.avatarStyle,
                    ringColor = palette.border.copy(alpha = 0.5f),
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(32.dp)
                )
                // Use existing profile avatars

                Column(modifier = Modifier.weight(1f)) {
                    AuthorMetaHeader(
                        author = comment.author,
                        createdAtMillis = comment.createdAtMillis,
                        edited = comment.edited,
                        bookmarked = comment.bookmarked,
                        palette = palette
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    val isEditingMain = editingTarget == TargetRef(comment.id)
                    if (isEditingMain) {
                        InlineEditBox(
                            initialText = comment.text,
                            palette = palette,
                            onSave = { onSaveEdit(TargetRef(comment.id), it) },
                            onCancel = onCancelEdit
                        )
                    } else {
                        val sourceText = if (comment.showTranslation && comment.translation != null) {
                            comment.translation.translatedText
                        } else {
                            comment.text
                        }
                        val isTruncated = sourceText.length > 200 && !comment.expanded
                        val visibleText = if (isTruncated) sourceText.take(200).trimEnd() + "…" else sourceText

                        RichCommentText(
                            itemKey = comment.id,
                            rawText = visibleText,
                            palette = palette,
                            reducedMotion = reducedMotion,
                            revealedSpoilers = revealedSpoilers,
                            onMentionOrTagClick = onMentionOrTagClick,
                            onTimestampClick = onTimestampClick,
                            onSpoilerRevealed = onSpoilerRevealed,
                            onDoubleTapLike = {
                                doubleTapBurstNonce += 1
                                onDoubleTapLike(TargetRef(comment.id))
                            }
                        )

                        if (sourceText.length > 200) {
                            Text(
                                text = if (comment.expanded) "Show less" else "Read more",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.textMuted,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .clickable { onToggleExpand(comment.id) }
                            )
                        }

                        comment.translation?.let { tr ->
                            Text(
                                text = if (comment.showTranslation) "See original (${tr.fromLanguage})" else "See translation",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (palette.isDark) Blue400 else Blue600,
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .clickable { onToggleTranslate(comment.id) }
                            )
                        }

                        // Voice Note Player Bubble
                        comment.voiceNote?.let { voice ->
                            Spacer(modifier = Modifier.height(8.dp))
                            VoiceNotePlayerBubble(
                                voice = voice,
                                isPlaying = isVoicePlaying,
                                progress = voiceProgress,
                                speedLabel = voiceSpeedLabel,
                                palette = palette,
                                onTogglePlay = { onToggleVoicePlay(comment.id) },
                                onScrub = { frac -> onScrubVoice(comment.id, frac) },
                                onCycleSpeed = { onCycleVoiceSpeed(comment.id) }
                            )
                        }

                        // Interactive Mini Poll Card
                        comment.poll?.let { poll ->
                            Spacer(modifier = Modifier.height(8.dp))
                            CommentPollCard(
                                poll = poll,
                                palette = palette,
                                reducedMotion = reducedMotion,
                                onVote = { optionId -> onVotePoll(comment.id, optionId) }
                            )
                        }

                        // Attached Vector Media Card
                        comment.media?.let { media ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, palette.border),
                                modifier = Modifier
                                    .widthIn(max = 250.dp)
                                    .clickable { onOpenMedia(media) }
                            ) {
                                MediaAttachmentCardGraphic(
                                    theme = media.theme,
                                    title = media.title,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(640f / 300f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    SocialMicroActionRow(
                        currentUser = currentUser,
                        creatorHeart = comment.creatorHeart,
                        reactions = comment.reactions,
                        isPickerOpen = activeReactionPickerKey == comment.id,
                        palette = palette,
                        onReply = { onReplyClick(comment.id, comment.author.handle) },
                        onToggleCreatorHeart = { onToggleCreatorHeart(TargetRef(comment.id)) },
                        onToggleReaction = { em -> onToggleReaction(TargetRef(comment.id), em) },
                        onOpenPicker = { onOpenReactionPicker(comment.id) },
                        onOpenMenu = { onOpenMenu(TargetRef(comment.id)) }
                    )
                }

                SideHeartLikeButton(
                    likes = comment.likes,
                    liked = comment.likedByMe,
                    palette = palette,
                    reducedMotion = reducedMotion,
                    onClick = { onToggleLike(TargetRef(comment.id)) }
                )
            }

            // Double-Tap Floating Heart & Ring Burst Overlay
            if (doubleTapBurstNonce > 0 && !reducedMotion) {
                DoubleTapHeartBurstOverlay(
                    triggerNonce = doubleTapBurstNonce,
                    modifier = Modifier.matchParentSize()
                )
            }
        }

        // Collapsible Nested Replies
        if (comment.replies.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .padding(start = 42.dp, top = 10.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.clickable { onToggleReplies(comment.id) }
                ) {
                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .height(1.dp)
                            .background(palette.border)
                    )
                    Text(
                        text = if (comment.repliesExpanded) {
                            "Hide replies"
                        } else {
                            "View ${comment.replies.size} ${if (comment.replies.size == 1) "reply" else "replies"}"
                        },
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textMuted
                    )
                }

                AnimatedVisibility(visible = comment.repliesExpanded) {
                    val visibleReplies = comment.replies.take(comment.visibleRepliesCount)
                    val remaining = comment.replies.size - visibleReplies.size
                    Column(
                        modifier = Modifier.padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        visibleReplies.forEach { reply ->
                            val replyRef = TargetRef(comment.id, reply.id)
                            val pickerKey = "${comment.id}:${reply.id}"

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MacProfileAvatar(
                                    style = reply.author.avatarStyle,
                                    ringColor = palette.border.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(24.dp)
                                )
                                // Use existing profile avatars

                                Column(modifier = Modifier.weight(1f)) {
                                    AuthorMetaHeader(
                                        author = reply.author,
                                        createdAtMillis = reply.createdAtMillis,
                                        edited = reply.edited,
                                        bookmarked = false,
                                        palette = palette
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    if (editingTarget == replyRef) {
                                        InlineEditBox(
                                            initialText = reply.text,
                                            palette = palette,
                                            onSave = { onSaveEdit(replyRef, it) },
                                            onCancel = onCancelEdit
                                        )
                                    } else {
                                        RichCommentText(
                                            itemKey = reply.id,
                                            rawText = reply.text,
                                            palette = palette,
                                            reducedMotion = reducedMotion,
                                            revealedSpoilers = revealedSpoilers,
                                            onMentionOrTagClick = onMentionOrTagClick,
                                            onTimestampClick = onTimestampClick,
                                            onSpoilerRevealed = onSpoilerRevealed,
                                            onDoubleTapLike = { onDoubleTapLike(replyRef) }
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(5.dp))
                                    SocialMicroActionRow(
                                        currentUser = currentUser,
                                        creatorHeart = reply.creatorHeart,
                                        reactions = reply.reactions,
                                        isPickerOpen = activeReactionPickerKey == pickerKey,
                                        palette = palette,
                                        onReply = { onReplyClick(comment.id, reply.author.handle) },
                                        onToggleCreatorHeart = { onToggleCreatorHeart(replyRef) },
                                        onToggleReaction = { em -> onToggleReaction(replyRef, em) },
                                        onOpenPicker = { onOpenReactionPicker(pickerKey) },
                                        onOpenMenu = { onOpenMenu(replyRef) }
                                    )
                                }

                                SideHeartLikeButton(
                                    likes = reply.likes,
                                    liked = reply.likedByMe,
                                    palette = palette,
                                    reducedMotion = reducedMotion,
                                    onClick = { onToggleLike(replyRef) }
                                )
                            }
                        }

                        if (remaining > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.clickable { onShowMoreReplies(comment.id) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(18.dp)
                                        .height(1.dp)
                                        .background(palette.border)
                                )
                                Text(
                                    text = "View $remaining more ${if (remaining == 1) "reply" else "replies"}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (palette.isDark) Blue400 else Blue600
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthorMetaHeader(
    author: CommentAuthor,
    createdAtMillis: Long,
    edited: Boolean,
    bookmarked: Boolean,
    palette: CommentSheetPalette
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = author.handle,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        VerifiedSealBadge(author.verified, size = 14.dp)
        author.role?.let { role ->
            val (bg, fg) = when (role) {
                "Creator" -> Blue600.copy(alpha = 0.12f) to if (palette.isDark) Blue400 else Blue600
                "Mod" -> Emerald600.copy(alpha = 0.12f) to if (palette.isDark) Emerald400 else Emerald600
                else -> Amber600.copy(alpha = 0.14f) to if (palette.isDark) Amber400 else Amber600
            }
            Surface(shape = RoundedCornerShape(5.dp), color = bg) {
                Text(
                    text = role,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = fg,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        }
        Text(
            text = formatRelativeTime(createdAtMillis),
            fontSize = 11.5.sp,
            color = palette.textMuted
        )
        if (edited) {
            Text(text = "(edited)", fontSize = 11.sp, color = palette.textMuted)
        }
        if (bookmarked) {
            Text(
                text = "• Saved",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (palette.isDark) Blue400 else Blue600
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SocialMicroActionRow(
    currentUser: CommentAuthor,
    creatorHeart: Boolean,
    reactions: Map<String, ReactionData>,
    isPickerOpen: Boolean,
    palette: CommentSheetPalette,
    onReply: () -> Unit,
    onToggleCreatorHeart: () -> Unit,
    onToggleReaction: (String) -> Unit,
    onOpenPicker: () -> Unit,
    onOpenMenu: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Reply",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.textMuted,
                modifier = Modifier.clickable(onClick = onReply)
            )

            // Creator Heart Avatar Badge
            if (creatorHeart) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clickable(onClick = onToggleCreatorHeart)
                ) {
                    MacProfileAvatar(
                        style = currentUser.avatarStyle,
                        ringColor = palette.border,
                        modifier = Modifier.size(17.dp)
                    )
                    // Use existing profile avatars
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(palette.sheetBg),
                        contentAlignment = Alignment.Center
                    ) {
                        HeartOutlineOrFilledIcon(
                            filled = true,
                            color = Rose500,
                            modifier = Modifier.size(8.dp)
                        )
                    }
                }
            }

            // Active Emoji Reaction Pills
            reactions.entries.filter { it.value.count > 0 }.forEach { (emoji, data) ->
                Surface(
                    shape = CircleShape,
                    color = if (data.activeByMe) Blue600.copy(alpha = 0.12f) else palette.surfaceChip,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (data.activeByMe) Blue600.copy(alpha = 0.45f) else Color.Transparent
                    ),
                    modifier = Modifier.clickable { onToggleReaction(emoji) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(text = emoji, fontSize = 11.5.sp)
                        Text(
                            text = data.count.toString(),
                            fontSize = 11.sp,
                            fontWeight = if (data.activeByMe) FontWeight.Bold else FontWeight.Medium,
                            color = if (data.activeByMe) Blue600 else palette.textSecondary
                        )
                    }
                }
            }

            // Reaction Picker Trigger
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onOpenPicker),
                contentAlignment = Alignment.Center
            ) {
                ReactionSmileyIcon(
                    color = if (isPickerOpen) Blue600 else palette.textMuted,
                    modifier = Modifier.size(15.dp)
                )
            }

            // Menu Options Trigger (...) -> Opens Iconora Studio 3D Liquid Glass Action Sheet
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onOpenMenu),
                contentAlignment = Alignment.Center
            ) {
                MoreDotsIcon(color = palette.textMuted, modifier = Modifier.size(14.dp))
            }
        }

        AnimatedVisibility(visible = isPickerOpen) {
            Surface(
                shape = CircleShape,
                color = palette.cardBg,
                border = BorderStroke(1.dp, palette.border),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ReactionPalette.forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onToggleReaction(emoji) }
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SideHeartLikeButton(
    likes: Int,
    liked: Boolean,
    palette: CommentSheetPalette,
    reducedMotion: Boolean = false,
    onClick: () -> Unit
) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(liked) {
        if (liked && !reducedMotion) {
            scale.snapTo(1f)
            scale.animateTo(1.28f, tween(130, easing = StageTravelEasing))
            scale.animateTo(1f, tween(190, easing = StageArrivalEasing))
        }
    }

    Column(
        modifier = Modifier
            .width(36.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        HeartOutlineOrFilledIcon(
            filled = liked,
            color = if (liked) Rose500 else palette.textMuted,
            modifier = Modifier
                .size(17.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }
        )
        if (likes > 0) {
            RollingCountText(
                value = likes,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = if (liked) Rose500 else palette.textMuted,
                reducedMotion = reducedMotion
            )
        }
    }
}

@Composable
private fun InlineEditBox(
    initialText: String,
    palette: CommentSheetPalette,
    onSave: (String) -> Unit,
    onCancel: () -> Unit
) {
    var draft by remember(initialText) { mutableStateOf(initialText) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BasicTextField(
            value = draft,
            onValueChange = { if (it.length <= 500) draft = it },
            textStyle = TextStyle(color = palette.textPrimary, fontSize = 13.5.sp),
            cursorBrush = SolidColor(Blue600),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(palette.surfaceSubtle)
                .border(1.dp, Blue600, RoundedCornerShape(10.dp))
                .padding(8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Cancel",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.textMuted,
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .clickable(onClick = onCancel)
            )
            Surface(
                shape = CircleShape,
                color = Blue600,
                modifier = Modifier.clickable { onSave(draft) }
            ) {
                Text(
                    text = "Save",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// ============================================================================
// 7. RICH COMMENT TEXT WITH SHIMMERING DUST TAP-TO-REVEAL SPOILERS
// ============================================================================
@Composable
private fun RichCommentText(
    itemKey: String,
    rawText: String,
    palette: CommentSheetPalette,
    reducedMotion: Boolean = false,
    revealedSpoilers: MutableMap<String, Boolean>,
    onMentionOrTagClick: (String) -> Unit,
    onTimestampClick: (String) -> Unit,
    onSpoilerRevealed: () -> Unit = {},
    onDoubleTapLike: () -> Unit = {}
) {
    val spoilerSnapshot = revealedSpoilers.toMap()
    val annotated = remember(itemKey, rawText, palette, spoilerSnapshot) {
        buildRichCommentAnnotatedString(
            itemKey = itemKey,
            rawText = rawText,
            palette = palette,
            revealedSpoilers = spoilerSnapshot
        )
    }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val spoilerAnnotations = remember(annotated) {
        annotated.getStringAnnotations("SPOILER", 0, annotated.length)
    }
    val hasHiddenSpoiler = spoilerAnnotations.any { revealedSpoilers[it.item] != true }

    val shimmerPhase = if (hasHiddenSpoiler && !reducedMotion) {
        val infiniteTransition = rememberInfiniteTransition(label = "spoilerShimmer")
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1900, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "spoilerDustPhase"
        ).value
    } else {
        0.35f
    }

    val scope = rememberCoroutineScope()
    var burstSpoilerKey by remember { mutableStateOf<String?>(null) }
    val burstProgress = remember { Animatable(1f) }

    Text(
        text = annotated,
        style = TextStyle(
            fontSize = 13.5.sp,
            lineHeight = 19.5.sp,
            color = palette.textSecondary
        ),
        onTextLayout = { textLayoutResult = it },
        modifier = Modifier
            .fillMaxWidth()
            .drawWithContent {
                drawContent()
                val layout = textLayoutResult ?: return@drawWithContent
                spoilerAnnotations.forEach { ann ->
                    val isRevealed = revealedSpoilers[ann.item] == true
                    val startLine = layout.getLineForOffset(ann.start)
                    val endLine = layout.getLineForOffset((ann.end - 1).coerceAtLeast(ann.start))
                    for (line in startLine..endLine) {
                        val segStart = if (line == startLine) ann.start else layout.getLineStart(line)
                        val segEnd = if (line == endLine) ann.end else layout.getLineEnd(line, visibleEnd = true)
                        if (segEnd <= segStart) continue

                        val startBox = layout.getBoundingBox(segStart)
                        val endBox = layout.getBoundingBox((segEnd - 1).coerceAtLeast(segStart))
                        val left = minOf(startBox.left, endBox.left) - 4f
                        val right = maxOf(startBox.right, endBox.right) + 4f
                        val top = minOf(startBox.top, endBox.top) + 1f
                        val bottom = maxOf(startBox.bottom, endBox.bottom) - 1f
                        val pillW = (right - left).coerceAtLeast(12f)
                        val pillH = (bottom - top).coerceAtLeast(12f)

                        if (!isRevealed) {
                            val baseA = if (palette.isDark) Color(0xFF27272A) else Color(0xFFE4E4E7)
                            val baseB = if (palette.isDark) Color(0xFF52525B) else Color(0xFFF4F4F5)
                            val sweepX = left - pillW + (pillW * 3f * shimmerPhase)
                            drawRoundRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(baseA, baseB, baseA),
                                    start = Offset(sweepX, top),
                                    end = Offset(sweepX + pillW, bottom)
                                ),
                                topLeft = Offset(left, top),
                                size = Size(pillW, pillH),
                                cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
                            )

                            val dotColor = if (palette.isDark) Color.White else Color(0xFF71717A)
                            val dotCount = (pillW / 12f).toInt().coerceIn(4, 18)
                            for (i in 0 until dotCount) {
                                val phase = (shimmerPhase + i * 0.17f) % 1f
                                val alpha = (0.3f + 0.6f * sin(phase * PI).toFloat()).coerceIn(0f, 1f)
                                val dx = left + 5f + ((i + 0.5f) / dotCount) * (pillW - 10f)
                                val dy = top + 3f + (((i * 37) % 10) / 10f) * (pillH - 6f)
                                drawCircle(
                                    color = dotColor.copy(alpha = alpha * 0.85f),
                                    radius = if (i % 2 == 0) 1.4f else 1.0f,
                                    center = Offset(dx, dy)
                                )
                            }
                        } else if (burstSpoilerKey == ann.item && burstProgress.value < 1f && !reducedMotion) {
                            val p = burstProgress.value
                            val fade = (1f - p).coerceIn(0f, 1f)
                            val dustColor = if (palette.isDark) Color.White else Color(0xFF52525B)

                            drawRoundRect(
                                color = Blue600.copy(alpha = 0.16f * fade),
                                topLeft = Offset(left - 3f * p, top - 1.5f * p),
                                size = Size(pillW + 6f * p, pillH + 3f * p),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                            )

                            for (s in 0 until 8) {
                                val angle = (s * 45.0) * (PI / 180.0)
                                val dist = (8f + (s % 2) * 5f) * p
                                val px = ((left + right) / 2f) + (cos(angle) * dist * 1.5f).toFloat()
                                val py = ((top + bottom) / 2f) + (sin(angle) * dist).toFloat()
                                drawCircle(
                                    color = dustColor.copy(alpha = 0.55f * fade),
                                    radius = (2f * fade).coerceAtLeast(0.5f),
                                    center = Offset(px, py)
                                )
                            }
                        }
                    }
                }
            }
            .pointerInput(annotated) {
                detectTapGestures(
                    onDoubleTap = { onDoubleTapLike() },
                    onTap = { pos ->
                        val layout = textLayoutResult ?: return@detectTapGestures
                        val offset = layout.getOffsetForPosition(pos)

                        annotated.getStringAnnotations("SPOILER", offset, offset).firstOrNull()?.let { ann ->
                            val nowRevealed = !(revealedSpoilers[ann.item] == true)
                            revealedSpoilers[ann.item] = nowRevealed
                            if (nowRevealed) {
                                onSpoilerRevealed()
                                if (!reducedMotion) {
                                    burstSpoilerKey = ann.item
                                    scope.launch {
                                        burstProgress.snapTo(0f)
                                        burstProgress.animateTo(1f, tween(380, easing = StageArrivalEasing))
                                    }
                                }
                            }
                            return@detectTapGestures
                        }
                        annotated.getStringAnnotations("TIMESTAMP", offset, offset).firstOrNull()?.let { ann ->
                            onTimestampClick(ann.item)
                            return@detectTapGestures
                        }
                        annotated.getStringAnnotations("TAG", offset, offset).firstOrNull()?.let { ann ->
                            onMentionOrTagClick(ann.item)
                            return@detectTapGestures
                        }
                    }
                )
            }
    )
}

private val RichTokenRegex = Regex(
    """(`[^`]+`|\*\*[^*]+\*\*|\*[^*]+\*|\|\|[^|]+\|\||@[a-zA-Z0-9_]+|#[a-zA-Z0-9_]+|\b\d{1,2}:\d{2}\b)"""
)

private fun buildRichCommentAnnotatedString(
    itemKey: String,
    rawText: String,
    palette: CommentSheetPalette,
    revealedSpoilers: Map<String, Boolean>
): AnnotatedString = buildAnnotatedString {
    var cursor = 0
    var spoilerIndex = 0
    val accentBlue = if (palette.isDark) Blue400 else Blue600

    RichTokenRegex.findAll(rawText).forEach { match ->
        if (match.range.first > cursor) {
            append(rawText.substring(cursor, match.range.first))
        }
        val token = match.value
        when {
            token.startsWith("`") && token.endsWith("`") -> {
                pushStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.5.sp,
                        color = accentBlue,
                        background = palette.surfaceChip
                    )
                )
                append(" ${token.drop(1).dropLast(1)} ")
                pop()
            }
            token.startsWith("**") && token.endsWith("**") -> {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = palette.textPrimary))
                append(token.drop(2).dropLast(2))
                pop()
            }
            token.startsWith("*") && token.endsWith("*") -> {
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                append(token.drop(1).dropLast(1))
                pop()
            }
            token.startsWith("||") && token.endsWith("||") -> {
                val key = "$itemKey:sp:${spoilerIndex++}"
                val revealed = revealedSpoilers[key] == true
                pushStringAnnotation("SPOILER", key)
                pushStyle(
                    SpanStyle(
                        color = if (revealed) palette.textPrimary else Color.Transparent,
                        background = if (revealed) Blue600.copy(alpha = 0.14f) else Color.Transparent
                    )
                )
                append(" ${token.drop(2).dropLast(2)} ")
                pop()
                pop()
            }
            token.startsWith("@") || token.startsWith("#") -> {
                pushStringAnnotation("TAG", token)
                pushStyle(SpanStyle(color = accentBlue, fontWeight = FontWeight.Medium))
                append(token)
                pop()
                pop()
            }
            else -> {
                pushStringAnnotation("TIMESTAMP", token)
                pushStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.5.sp,
                        color = accentBlue,
                        background = Blue600.copy(alpha = 0.10f),
                        fontWeight = FontWeight.SemiBold
                    )
                )
                append(" $token ")
                pop()
                pop()
            }
        }
        cursor = match.range.last + 1
    }
    if (cursor < rawText.length) {
        append(rawText.substring(cursor))
    }
}

// ============================================================================
// 8. BOTTOM-DOCKED SIGNATURE COMPOSER BAR
//    (Avatar + Animated Blue Underline + Custom Filled Origami Send Button)
// ============================================================================
@Composable
private fun DockedCommentComposer(
    text: String,
    onTextChange: (String) -> Unit,
    currentUser: CommentAuthor,
    palette: CommentSheetPalette,
    mentionDirectory: List<CommentAuthor>,
    replyTarget: ReplyTarget?,
    showRichTools: Boolean,
    attachedMedia: CommentMediaAttachment?,
    attachedPoll: CommentPoll?,
    attachedVoiceNote: CommentVoiceNote?,
    isRecordingVoice: Boolean,
    recordingSeconds: Int,
    focusRequester: FocusRequester,
    onToggleRichTools: () -> Unit,
    onQuickEmojiClick: (String) -> Unit,
    onCancelReply: () -> Unit,
    onCycleAttachMedia: () -> Unit,
    onRemoveMedia: () -> Unit,
    onToggleVoiceRecord: () -> Unit,
    onCancelVoiceNote: () -> Unit,
    onAttachQuickPoll: () -> Unit,
    onRemovePoll: () -> Unit,
    onSend: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    var animStage by remember { mutableStateOf(CommentSendAnimStage.IDLE) }

    val isAnimating = animStage != CommentSendAnimStage.IDLE
    val hasContent = text.trim().isNotEmpty() || attachedMedia != null || attachedPoll != null || attachedVoiceNote != null
    val isInputActive = isFocused || hasContent || replyTarget != null || isRecordingVoice

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val avatarRingColor by animateColorAsState(
        targetValue = if (isInputActive) Blue600.copy(alpha = 0.28f) else Color.Transparent,
        animationSpec = tween(300),
        label = "composerAvatarRing"
    )
    val underlineScaleX by animateFloatAsState(
        targetValue = if (isInputActive) 1f else 0f,
        animationSpec = tween(300, easing = StageTravelEasing),
        label = "composerUnderlineScale"
    )

    // 4-Stage Send Icon Animatables
    val dartOffsetX = remember { Animatable(0f) }
    val dartOffsetY = remember { Animatable(0f) }
    val dartScale = remember { Animatable(1f) }
    val dartAlpha = remember { Animatable(1f) }
    val streakProgress = remember { Animatable(0f) }
    val checkScale = remember { Animatable(0.75f) }
    val checkAlpha = remember { Animatable(0f) }
    val checkDrawProgress = remember { Animatable(0f) }

    fun triggerAnimatedSend() {
        if (isAnimating || !hasContent) return
        onSend()
        scope.launch {
            try {
                // Stage 1A: Pullback
                animStage = CommentSendAnimStage.PULLBACK
                val pbX = with(density) { (-4).dp.toPx() }
                val pbY = with(density) { 4.dp.toPx() }
                val a1 = launch { dartOffsetX.animateTo(pbX, tween(120, easing = SmoothEaseOut)) }
                val a2 = launch { dartOffsetY.animateTo(pbY, tween(120, easing = SmoothEaseOut)) }
                val a3 = launch { dartScale.animateTo(0.9f, tween(120, easing = SmoothEaseOut)) }
                a1.join(); a2.join(); a3.join()

                // Stage 1B: Launch + Thrust Streaks
                animStage = CommentSendAnimStage.LAUNCH
                val lx = with(density) { 36.dp.toPx() }
                val ly = with(density) { (-36).dp.toPx() }
                streakProgress.snapTo(0f)
                launch { streakProgress.animateTo(1f, tween(420, easing = SmoothEaseOut)) }
                launch { dartOffsetX.animateTo(lx, tween(380, easing = LaunchEaseIn)) }
                launch { dartOffsetY.animateTo(ly, tween(380, easing = LaunchEaseIn)) }
                launch { dartScale.animateTo(0.75f, tween(380, easing = LaunchEaseIn)) }
                launch { dartAlpha.animateTo(0f, tween(300, easing = FastOutLinearInEasing)) }
                delay(360)

                // Stage 2: Stroke-Drawn Checkmark Confirmation
                animStage = CommentSendAnimStage.CONFIRM
                streakProgress.snapTo(0f)
                dartOffsetX.snapTo(with(density) { (-24).dp.toPx() })
                dartOffsetY.snapTo(with(density) { 24.dp.toPx() })
                dartScale.snapTo(0.75f)
                dartAlpha.snapTo(0f)
                checkDrawProgress.snapTo(0f)
                launch { checkScale.animateTo(1f, tween(300, easing = SmoothEaseOut)) }
                launch { checkAlpha.animateTo(1f, tween(260, easing = FastOutSlowInEasing)) }
                launch { checkDrawProgress.animateTo(1f, tween(360, easing = SmoothEaseOut)) }
                delay(740)

                // Stage 3: Glide Return
                animStage = CommentSendAnimStage.RETURN
                launch { checkScale.animateTo(0.75f, tween(300, easing = SmoothEaseOut)) }
                launch { checkAlpha.animateTo(0f, tween(260, easing = FastOutSlowInEasing)) }
                val r1 = launch { dartOffsetX.animateTo(0f, tween(460, easing = SmoothEaseOut)) }
                val r2 = launch { dartOffsetY.animateTo(0f, tween(460, easing = SmoothEaseOut)) }
                val r3 = launch { dartScale.animateTo(1f, tween(460, easing = SmoothEaseOut)) }
                val r4 = launch { dartAlpha.animateTo(1f, tween(380, easing = FastOutSlowInEasing)) }
                r1.join(); r2.join(); r3.join(); r4.join()
            } finally {
                checkDrawProgress.snapTo(0f)
                checkAlpha.snapTo(0f)
                checkScale.snapTo(0.75f)
                streakProgress.snapTo(0f)
                dartOffsetX.snapTo(0f)
                dartOffsetY.snapTo(0f)
                dartScale.snapTo(1f)
                dartAlpha.snapTo(1f)
                animStage = CommentSendAnimStage.IDLE
            }
        }
    }

    fun insertSnippet(snippet: String, wrapBefore: String = "", wrapAfter: String = "", placeholder: String = "") {
        val next = if (wrapBefore.isNotEmpty()) {
            "$text$wrapBefore$placeholder$wrapAfter"
        } else {
            "$text$snippet"
        }
        onTextChange(next.take(500))
    }

    // Detect active @mention query at end of composer text
    val mentionQuery = remember(text) {
        Regex("""(?:^|\s)@([a-zA-Z0-9_]*)$""").find(text)?.groupValues?.getOrNull(1)
    }
    val mentionSuggestions = remember(mentionQuery, mentionDirectory) {
        if (mentionQuery == null) {
            emptyList()
        } else {
            mentionDirectory.filter {
                it.handle.contains(mentionQuery, ignoreCase = true) ||
                    it.name.contains(mentionQuery, ignoreCase = true)
            }.take(4)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.sheetBg)
    ) {
        HorizontalDivider(color = palette.divider)

        // 1. Upward @Mention Autocomplete List
        AnimatedVisibility(visible = mentionSuggestions.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.cardBg)
                    .padding(vertical = 4.dp)
            ) {
                mentionSuggestions.forEach { user ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val updated = text.replace(Regex("""@([a-zA-Z0-9_]*)$"""), "@${user.handle} ")
                                onTextChange(updated)
                            }
                            .padding(horizontal = 16.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MacProfileAvatar(
                            style = user.avatarStyle,
                            ringColor = palette.border,
                            author = user,
                            modifier = Modifier.size(26.dp)
                        )
                        // Use existing profile avatars
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = user.name,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.textPrimary
                                )
                                VerifiedSealBadge(user.verified, size = 13.dp)
                            }
                            Text(text = "@${user.handle}", fontSize = 11.sp, color = palette.textMuted)
                        }
                    }
                }
                HorizontalDivider(color = palette.divider)
            }
        }

        // 2. Compact Rich Formatting & Attachment Tools Bar
        AnimatedVisibility(visible = showRichTools) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surfaceSubtle)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RichToolChip(label = "||Spoiler||", palette = palette) {
                    insertSnippet("", "||", "||", "secret"); onToggleRichTools()
                }
                RichToolChip(
                    label = if (isRecordingVoice) "Stop Mic" else "Voice",
                    palette = palette,
                    leadingIcon = {
                        MicIcon(
                            color = if (isRecordingVoice) Rose500 else palette.textSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                ) { onToggleVoiceRecord(); onToggleRichTools() }
                RichToolChip(
                    label = "Poll",
                    palette = palette,
                    leadingIcon = {
                        PollBarsIcon(
                            color = palette.textSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                ) { onAttachQuickPoll(); onToggleRichTools() }
                RichToolChip(label = "B", palette = palette) { insertSnippet("", "**", "**", "bold"); onToggleRichTools() }
                RichToolChip(label = "I", palette = palette) { insertSnippet("", "*", "*", "italic"); onToggleRichTools() }
                RichToolChip(label = "</>", palette = palette) { insertSnippet("", "`", "`", "code"); onToggleRichTools() }
                RichToolChip(label = "@", palette = palette) { insertSnippet("@"); onToggleRichTools() }
                RichToolChip(label = "01:42", palette = palette) { insertSnippet("01:42 "); onToggleRichTools() }
                RichToolChip(label = "Attach Card", palette = palette) { onCycleAttachMedia(); onToggleRichTools() }
            }
        }

        // 2B. Voice Recorder / Attached Voice Note / Attached Poll Status Bar
        AnimatedVisibility(visible = isRecordingVoice || attachedVoiceNote != null || attachedPoll != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surfaceSubtle)
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isRecordingVoice) {
                    Surface(
                        shape = CircleShape,
                        color = Rose500.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, Rose500.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Rose500)
                                )
                                Text(
                                    text = "Recording voice • 0:${recordingSeconds.toString().padStart(2, '0')}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Rose500
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Done",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Blue600,
                                    modifier = Modifier.clickable(onClick = onToggleVoiceRecord)
                                )
                                CloseCrossIcon(
                                    color = palette.textMuted,
                                    modifier = Modifier
                                        .size(13.dp)
                                        .clickable(onClick = onCancelVoiceNote)
                                )
                            }
                        }
                    }
                } else {
                    attachedVoiceNote?.let { voice ->
                        Surface(
                            shape = CircleShape,
                            color = Blue600.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Blue600.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                MicIcon(
                                    color = if (palette.isDark) Blue400 else Blue600,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Voice 0:${voice.durationSeconds.toString().padStart(2, '0')}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (palette.isDark) Blue400 else Blue600
                                )
                                CloseCrossIcon(
                                    color = palette.textMuted,
                                    modifier = Modifier
                                        .size(11.dp)
                                        .clickable(onClick = onCancelVoiceNote)
                                )
                            }
                        }
                    }
                    attachedPoll?.let { poll ->
                        Surface(
                            shape = CircleShape,
                            color = Emerald600.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Emerald600.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PollBarsIcon(
                                    color = if (palette.isDark) Emerald400 else Emerald600,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Poll: ${poll.question}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (palette.isDark) Emerald400 else Emerald600
                                )
                                CloseCrossIcon(
                                    color = palette.textMuted,
                                    modifier = Modifier
                                        .size(11.dp)
                                        .clickable(onClick = onRemovePoll)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Replying-To Context Banner
        AnimatedVisibility(visible = replyTarget != null) {
            replyTarget?.let { target ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surfaceSubtle)
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = buildAnnotatedString {
                            append("Replying to ")
                            pushStyle(SpanStyle(color = Blue600, fontWeight = FontWeight.SemiBold))
                            append("@${target.handle}")
                            pop()
                        },
                        fontSize = 12.sp,
                        color = palette.textSecondary
                    )
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onCancelReply),
                        contentAlignment = Alignment.Center
                    ) {
                        CloseCrossIcon(
                            color = palette.textMuted,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        // 4. Attached Media Preview inside Docked Bar
        AnimatedVisibility(visible = attachedMedia != null) {
            attachedMedia?.let { media ->
                Box(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp)
                        .width(176.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, palette.border)
                    ) {
                        MediaAttachmentCardGraphic(
                            theme = media.theme,
                            title = media.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(640f / 300f)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(5.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.72f))
                            .clickable(onClick = onRemoveMedia),
                        contentAlignment = Alignment.Center
                    ) {
                        CloseCrossIcon(color = Color.White, modifier = Modifier.size(10.dp))
                    }
                }
            }
        }

        // 5. One-Tap Quick Emoji Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            QuickEmojis.forEach { emoji ->
                Text(
                    text = emoji,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onQuickEmojiClick(emoji) }
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        }

        // 6. Signature Input Row: Profile Avatar + Expanding Blue Underline + Custom Animated Send Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 10.dp, top = 2.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MacProfileAvatar(
                    style = currentUser.avatarStyle,
                    ringColor = avatarRingColor,
                    author = currentUser,
                    modifier = Modifier
                        .padding(bottom = 2.dp)
                        .size(36.dp)
                )
                // Use existing profile avatars

                Box(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            BasicTextField(
                                value = text,
                                onValueChange = { if (it.length <= 500) onTextChange(it) },
                                maxLines = 4,
                                textStyle = TextStyle(
                                    color = palette.textPrimary,
                                    fontSize = 14.5.sp,
                                    lineHeight = 20.sp
                                ),
                                cursorBrush = SolidColor(Blue600),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = { triggerAnimatedSend() }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { isFocused = it.isFocused }
                                    .padding(vertical = 8.dp),
                                decorationBox = { inner ->
                                    if (text.isEmpty()) {
                                        Text(
                                            text = if (replyTarget != null) {
                                                "Reply to @${replyTarget.handle}..."
                                            } else {
                                                "Add a comment..."
                                            },
                                            fontSize = 14.5.sp,
                                            color = palette.textMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    inner()
                                }
                            )

                            // Static hairline underline
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(palette.underlineInactive)
                            )

                            // Animated expanding blue underline
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .graphicsLayer {
                                        transformOrigin = TransformOrigin(0f, 0.5f)
                                        scaleX = underlineScaleX
                                    }
                                    .background(Blue600)
                            )
                        }

                        // Identity-preserving (+) -> (×) Rich Tools trigger
                        Box(
                            modifier = Modifier
                                .padding(bottom = 4.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (showRichTools) Blue600.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable(onClick = onToggleRichTools),
                            contentAlignment = Alignment.Center
                        ) {
                            PlusToCloseIcon(
                                isClose = showRichTools,
                                color = if (showRichTools) Blue600 else palette.textMuted,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            // Custom Filled Animated Send Button (No background, Blue active / Gray disabled)
            AnimatedFilledSendButton(
                enabled = hasContent && !isAnimating,
                hasContent = hasContent,
                isDark = palette.isDark,
                animStage = animStage,
                dartOffsetX = dartOffsetX.value,
                dartOffsetY = dartOffsetY.value,
                dartScale = dartScale.value,
                dartAlpha = dartAlpha.value,
                streakProgress = streakProgress.value,
                checkScale = checkScale.value,
                checkAlpha = checkAlpha.value,
                checkDrawProgress = checkDrawProgress.value,
                onClick = { triggerAnimatedSend() }
            )
        }
    }
}

@Composable
private fun RichToolChip(
    label: String,
    palette: CommentSheetPalette,
    leadingIcon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = palette.cardBg,
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            leadingIcon?.invoke()
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.textSecondary
            )
        }
    }
}

@Composable
private fun AnimatedFilledSendButton(
    enabled: Boolean,
    hasContent: Boolean,
    isDark: Boolean,
    animStage: CommentSendAnimStage,
    dartOffsetX: Float,
    dartOffsetY: Float,
    dartScale: Float,
    dartAlpha: Float,
    streakProgress: Float,
    checkScale: Float,
    checkAlpha: Float,
    checkDrawProgress: Float,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isIdle = animStage == CommentSendAnimStage.IDLE
    val disabledColor = if (isDark) Neutral600 else Neutral300

    val pressScale by animateFloatAsState(
        targetValue = if (enabled && isIdle && isPressed) 0.92f else 1f,
        animationSpec = tween(160, easing = SmoothEaseOut),
        label = "sendPressScale"
    )

    val targetColor = when (animStage) {
        CommentSendAnimStage.PULLBACK, CommentSendAnimStage.LAUNCH, CommentSendAnimStage.CONFIRM -> Blue600
        CommentSendAnimStage.RETURN -> if (hasContent) Blue600 else disabledColor
        CommentSendAnimStage.IDLE -> when {
            !hasContent -> disabledColor
            isPressed -> Blue700
            else -> Blue600
        }
    }
    val iconColor by animateColorAsState(targetColor, tween(280), label = "sendBtnColor")

    val upperWing = remember { Path() }
    val leftWing = remember { Path() }
    val shadowWedge = remember { Path() }
    val fullCheck = remember { Path() }
    val animCheck = remember { Path() }
    val measure = remember { PathMeasure() }

    Box(
        modifier = Modifier
            .size(40.dp)
            .semantics { contentDescription = "Post comment" }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(28.dp)
                .graphicsLayer {
                    clip = false
                    translationX = dartOffsetX
                    translationY = dartOffsetY
                    scaleX = dartScale * pressScale
                    scaleY = dartScale * pressScale
                    alpha = dartAlpha
                }
        ) {
            val sx = size.width / 28f
            val sy = size.height / 28f

            if (streakProgress > 0f && streakProgress < 1f) {
                val streakAlpha = if (streakProgress <= 0.35f) streakProgress / 0.35f else 1f - ((streakProgress - 0.35f) / 0.65f)
                val sFrac = ((streakProgress - 0.5f) * 2f).coerceIn(0f, 1f)
                val eFrac = (streakProgress * 2f).coerceIn(0f, 1f)
                fun drawStreak(x1: Float, y1: Float, x2: Float, y2: Float, w: Float) {
                    drawLine(
                        color = iconColor.copy(alpha = streakAlpha.coerceIn(0f, 1f)),
                        start = Offset((x1 + (x2 - x1) * sFrac) * sx, (y1 + (y2 - y1) * sFrac) * sy),
                        end = Offset((x1 + (x2 - x1) * eFrac) * sx, (y1 + (y2 - y1) * eFrac) * sy),
                        strokeWidth = w * sx,
                        cap = StrokeCap.Round
                    )
                }
                drawStreak(2.5f, 21.5f, 7.5f, 16.5f, 2f)
                drawStreak(5.5f, 24.5f, 10f, 20f, 1.75f)
            }

            upperWing.reset()
            upperWing.moveTo(24.6f * sx, 3.4f * sy)
            upperWing.cubicTo(25.1f * sx, 3.9f * sy, 25.2f * sx, 4.6f * sy, 24.9f * sx, 5.3f * sy)
            upperWing.lineTo(17.4f * sx, 23.6f * sy)
            upperWing.cubicTo(17.1f * sx, 24.4f * sy, 16.3f * sx, 24.9f * sy, 15.4f * sx, 24.8f * sy)
            upperWing.cubicTo(14.6f * sx, 24.8f * sy, 13.8f * sx, 24.2f * sy, 13.6f * sx, 23.4f * sy)
            upperWing.lineTo(11.5f * sx, 16.5f * sy)
            upperWing.lineTo(24.6f * sx, 3.4f * sy)
            upperWing.close()
            drawPath(upperWing, color = iconColor)

            leftWing.reset()
            leftWing.moveTo(24.6f * sx, 3.4f * sy)
            leftWing.lineTo(11.5f * sx, 16.5f * sy)
            leftWing.lineTo(4.6f * sx, 14.4f * sy)
            leftWing.cubicTo(3.8f * sx, 14.2f * sy, 3.2f * sx, 13.4f * sy, 3.2f * sx, 12.6f * sy)
            leftWing.cubicTo(3.1f * sx, 11.7f * sy, 3.6f * sx, 10.9f * sy, 4.4f * sx, 10.6f * sy)
            leftWing.lineTo(22.7f * sx, 3.1f * sy)
            leftWing.cubicTo(23.4f * sx, 2.8f * sy, 24.1f * sx, 2.9f * sy, 24.6f * sx, 3.4f * sy)
            leftWing.close()
            drawPath(leftWing, color = iconColor.copy(alpha = 0.82f))

            shadowWedge.reset()
            shadowWedge.moveTo(11.5f * sx, 16.5f * sy)
            shadowWedge.lineTo(15.8f * sx, 18.8f * sy)
            shadowWedge.lineTo(13.6f * sx, 23.4f * sy)
            shadowWedge.close()
            drawPath(shadowWedge, color = iconColor.copy(alpha = 0.55f))
        }

        if (checkAlpha > 0f && checkDrawProgress > 0f) {
            Canvas(
                modifier = Modifier
                    .size(28.dp)
                    .graphicsLayer {
                        scaleX = checkScale
                        scaleY = checkScale
                        alpha = checkAlpha
                    }
            ) {
                val sx = size.width / 28f
                val sy = size.height / 28f
                fullCheck.reset()
                fullCheck.moveTo(6.5f * sx, 14.8f * sy)
                fullCheck.lineTo(11.6f * sx, 19.8f * sy)
                fullCheck.lineTo(21.8f * sx, 8.8f * sy)
                measure.setPath(fullCheck, false)
                animCheck.reset()
                measure.getSegment(0f, measure.length * checkDrawProgress.coerceIn(0f, 1f), animCheck, true)
                drawPath(
                    path = animCheck,
                    color = Blue600,
                    style = Stroke(width = 2.8f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}

// ============================================================================
// 9. INTERACTIVE POLLS, VOICE NOTE PLAYER, DOUBLE-TAP HEART, TYPING & DIALOGS
// ============================================================================
@Composable
private fun CommentPollCard(
    poll: CommentPoll,
    palette: CommentSheetPalette,
    reducedMotion: Boolean = false,
    onVote: (String) -> Unit
) {
    val totalVotes = poll.options.sumOf { it.votes }.coerceAtLeast(1)
    val hasVoted = poll.selectedOptionId != null

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = palette.surfaceSubtle,
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = poll.question,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                RollingCountText(
                    value = totalVotes,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = palette.textMuted,
                    suffix = " votes",
                    reducedMotion = reducedMotion
                )
            }

            poll.options.forEach { opt ->
                val isSelected = poll.selectedOptionId == opt.id
                val targetFraction = if (hasVoted) opt.votes.toFloat() / totalVotes.toFloat() else 0f
                val animatedFraction by animateFloatAsState(
                    targetValue = targetFraction,
                    animationSpec = tween(durationMillis = if (reducedMotion) 0 else 520, easing = StageTravelEasing),
                    label = "pollBar_${opt.id}"
                )
                val percent = ((opt.votes.toFloat() / totalVotes.toFloat()) * 100f).roundToInt()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.cardBg)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) Blue600 else palette.border,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onVote(opt.id) }
                ) {
                    if (animatedFraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedFraction.coerceIn(0f, 1f))
                                .background(
                                    if (isSelected) Blue600.copy(alpha = if (palette.isDark) 0.25f else 0.14f)
                                    else palette.surfaceChip
                                )
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isSelected) {
                                CheckMiniIcon(
                                    color = if (palette.isDark) Blue400 else Blue600,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Text(
                                text = opt.label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isSelected) (if (palette.isDark) Blue400 else Blue600) else palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (hasVoted) {
                            RollingCountText(
                                value = percent,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) (if (palette.isDark) Blue400 else Blue600) else palette.textMuted,
                                suffix = "%",
                                reducedMotion = reducedMotion
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceNotePlayerBubble(
    voice: CommentVoiceNote,
    isPlaying: Boolean,
    progress: Float,
    speedLabel: String,
    palette: CommentSheetPalette,
    onTogglePlay: () -> Unit,
    onScrub: (Float) -> Unit,
    onCycleSpeed: () -> Unit
) {
    val remainingSec = ((1f - progress) * voice.durationSeconds).roundToInt().coerceAtLeast(0)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = palette.surfaceSubtle,
        border = BorderStroke(1.dp, if (isPlaying) Blue600.copy(alpha = 0.5f) else palette.border),
        modifier = Modifier.widthIn(max = 290.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Blue600)
                    .clickable(onClick = onTogglePlay),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(14.dp)) {
                    val w = size.width
                    val h = size.height
                    if (isPlaying) {
                        drawRoundRect(Color.White, Offset(w * 0.16f, h * 0.14f), Size(w * 0.24f, h * 0.72f), CornerRadius(2f, 2f))
                        drawRoundRect(Color.White, Offset(w * 0.60f, h * 0.14f), Size(w * 0.24f, h * 0.72f), CornerRadius(2f, 2f))
                    } else {
                        val p = Path().apply {
                            moveTo(w * 0.24f, h * 0.14f)
                            lineTo(w * 0.88f, h * 0.50f)
                            lineTo(w * 0.24f, h * 0.86f)
                            close()
                        }
                        drawPath(p, Color.White)
                    }
                }
            }

            // Tappable Waveform Scrubber
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
                    .pointerInput(voice) {
                        detectTapGestures { offset ->
                            onScrub((offset.x / size.width.toFloat()).coerceIn(0f, 1f))
                        }
                    }
            ) {
                val bars = voice.waveform.ifEmpty { listOf(0.4f, 0.7f, 0.5f) }
                val gap = 2.dp.toPx()
                val barW = ((size.width - gap * (bars.size - 1)) / bars.size).coerceAtLeast(2f)
                val activeIdx = (progress * bars.size).toInt()
                bars.forEachIndexed { idx, amp ->
                    val barH = (size.height * amp.coerceIn(0.18f, 1f))
                    val x = idx * (barW + gap)
                    val y = (size.height - barH) / 2f
                    val isActive = idx <= activeIdx && (progress > 0f || isPlaying)
                    drawRoundRect(
                        color = if (isActive) Blue600 else palette.border,
                        topLeft = Offset(x, y),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "0:${remainingSec.toString().padStart(2, '0')}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textSecondary
                )
                Text(
                    text = speedLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (palette.isDark) Blue400 else Blue600,
                    modifier = Modifier.clickable(onClick = onCycleSpeed)
                )
            }
        }
    }
}

@Composable
private fun DoubleTapHeartBurstOverlay(
    triggerNonce: Int,
    modifier: Modifier = Modifier
) {
    val progress = remember(triggerNonce) { Animatable(0f) }
    LaunchedEffect(triggerNonce) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(650, easing = StageArrivalEasing))
    }
    if (progress.value < 1f) {
        val p = progress.value
        val heartScale = when {
            p < 0.28f -> 0.3f + (p / 0.28f) * 0.95f
            p < 0.62f -> 1.25f - ((p - 0.28f) / 0.34f) * 0.23f
            else -> 1.02f + ((p - 0.62f) / 0.38f) * 0.45f
        }
        val heartAlpha = if (p < 0.62f) 1f else (1f - (p - 0.62f) / 0.38f).coerceIn(0f, 1f)
        val ringScale = 0.35f + p * 1.6f
        val ringAlpha = (1f - p * 1.4f).coerceIn(0f, 1f)

        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .graphicsLayer {
                        scaleX = ringScale
                        scaleY = ringScale
                        alpha = ringAlpha
                    }
                    .border(3.dp, Rose500.copy(alpha = 0.7f), CircleShape)
            )
            HeartOutlineOrFilledIcon(
                filled = true,
                color = Rose500,
                modifier = Modifier
                    .size(46.dp)
                    .graphicsLayer {
                        scaleX = heartScale
                        scaleY = heartScale
                        alpha = heartAlpha
                    }
            )
        }
    }
}

@Composable
private fun FloatingEmojiCelebrationOverlay(
    particles: List<FloatingEmojiParticle>,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        particles.forEach { particle ->
            key(particle.id) {
                val anim = remember { Animatable(0f) }
                LaunchedEffect(particle.id) {
                    anim.animateTo(1f, tween(1050, easing = SmoothEaseOut))
                }
                val p = anim.value
                val alpha = when {
                    p < 0.18f -> p / 0.18f
                    else -> (1f - (p - 0.18f) / 0.82f).coerceIn(0f, 1f)
                }
                val scale = if (p < 0.2f) 0.5f + p * 3.5f else 1.2f - (p - 0.2f) * 0.55f

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 112.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Text(
                        text = particle.emoji,
                        fontSize = 22.sp,
                        modifier = Modifier
                            .fillMaxWidth(particle.startFractionX)
                            .offset(
                                x = (particle.driftDp * p).dp,
                                y = (-190f * p).dp
                            )
                            .graphicsLayer {
                                this.alpha = alpha
                                scaleX = scale
                                scaleY = scale
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveTypingIndicatorRow(
    handle: String,
    avatarStyle: AvatarStyle,
    palette: CommentSheetPalette,
    reducedMotion: Boolean = false
) {
    val phase = if (reducedMotion) {
        0.5f
    } else {
        val transition = rememberInfiniteTransition(label = "typingDots")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
            label = "typingPhase"
        ).value
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MacProfileAvatar(
            style = avatarStyle,
            ringColor = palette.border,
            modifier = Modifier.size(20.dp)
        )
        // Use existing profile avatars
        Surface(shape = CircleShape, color = palette.surfaceChip) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "$handle is typing",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = palette.textMuted
                )
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(3) { i ->
                        val wave = sin(((phase + i * 0.22f) % 1f) * 2.0 * PI).toFloat()
                        val dy = if (reducedMotion) 0f else (-2.5f * wave.coerceAtLeast(0f))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .offset(y = dy.dp)
                                .clip(CircleShape)
                                .background(Blue600.copy(alpha = 0.4f + 0.6f * wave.coerceAtLeast(0f)))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportCommentDialog(
    palette: CommentSheetPalette,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    val reasons = listOf(
        "Spam or misleading content",
        "Harassment or hate speech",
        "Unmarked spoilers",
        "Inappropriate or unsafe content"
    )
    var selected by remember { mutableStateOf(reasons.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.cardBg,
        title = { Text("Report comment", fontWeight = FontWeight.Bold, color = palette.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Why are you reporting this comment? Your report stays anonymous.",
                    fontSize = 12.sp,
                    color = palette.textMuted
                )
                reasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selected = reason }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected == reason, onClick = { selected = reason })
                        Text(text = reason, fontSize = 13.sp, color = palette.textPrimary)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selected) },
                colors = ButtonDefaults.buttonColors(containerColor = Blue600)
            ) { Text("Submit", color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = palette.textSecondary) }
        }
    )
}

// ============================================================================
// 10. GEOMETRIC AVATARS, VERIFIED BADGES, MOTION COUNTS & DRAWN UI ICONS
// ============================================================================
@Composable
private fun MacProfileAvatar(
    style: AvatarStyle = AvatarStyle.ROYAL_BLUE,
    ringColor: Color = Color.Transparent,
    author: CommentAuthor? = null,
    modifier: Modifier = Modifier
) {
    val avatarUrl = author?.avatarUrl.orEmpty()
    val name = author?.name.orEmpty().ifBlank { author?.handle.orEmpty() }
    if (avatarUrl.isNotBlank()) {
        IosAvatarView(
            name = name.ifBlank { "User" },
            avatarUrl = avatarUrl,
            size = 32.dp,
            isOnline = author?.isOnline ?: false,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier
                .shadow(elevation = 1.dp, shape = CircleShape, clip = false)
                .border(width = 1.5.dp, color = ringColor, shape = CircleShape)
                .clip(CircleShape)
        ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val center = Offset(w / 2f, h / 2f)
            val radius = minOf(w, h) / 2f
            val scale = minOf(w, h) / 512f

            drawCircle(
                brush = Brush.verticalGradient(listOf(style.start, style.end)),
                radius = radius,
                center = center
            )

            val silhouette = Brush.verticalGradient(
                colorStops = arrayOf(0.10f to Color.White, 0.90f to Color(0xFFD5D9E0)),
                startY = 0f,
                endY = h
            )
            val clipCircle = Path().apply { addOval(Rect(center = center, radius = radius)) }
            clipPath(clipCircle) {
                drawCircle(silhouette, radius = 95f * scale, center = Offset(256f * scale, 165f * scale))
                drawCircle(silhouette, radius = 250f * scale, center = Offset(256f * scale, 530f * scale))
            }
            drawCircle(
                brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.7f), Color.Black.copy(alpha = 0.2f))),
                radius = 254.5f * scale,
                center = center,
                style = Stroke(width = 3f * scale)
            )
        }
    }
    }
}
// Use existing profile avatars

@Composable
private fun VerifiedSealBadge(
    type: VerifiedBadgeType?,
    size: Dp = 14.dp,
    modifier: Modifier = Modifier
) {
    if (type == null || type == VerifiedBadgeType.NONE) return
    val tier = when (type) {
        VerifiedBadgeType.GOLD -> VerifiedBadgeTier.GOLD
        VerifiedBadgeType.EMERALD -> VerifiedBadgeTier.CHROMA
        VerifiedBadgeType.BLUE -> VerifiedBadgeTier.BLUE
        else -> VerifiedBadgeTier.BLUE
    }
    VerifiedBadge(
        modifier = modifier.padding(start = 2.dp),
        size = size,
        tier = tier
    )
}

@Composable
private fun VerifiedSealCanvas(
    type: VerifiedBadgeType,
    size: Dp = 14.dp,
    modifier: Modifier = Modifier
) {
    if (type == VerifiedBadgeType.NONE) return
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val r = minOf(w, h) / 2f
        val center = Offset(w / 2f, h / 2f)

        // Scalloped 8-lobe seal
        val sealPath = Path()
        val lobes = 8
        for (i in 0 until lobes * 2) {
            val angle = (i * PI / lobes) - PI / 2.0
            val rad = if (i % 2 == 0) r else r * 0.84f
            val x = center.x + (cos(angle) * rad).toFloat()
            val y = center.y + (sin(angle) * rad).toFloat()
            if (i == 0) sealPath.moveTo(x, y) else sealPath.lineTo(x, y)
        }
        sealPath.close()
        drawPath(sealPath, brush = Brush.verticalGradient(listOf(type.start, type.end)))

        val check = Path().apply {
            moveTo(w * 0.28f, h * 0.52f)
            lineTo(w * 0.43f, h * 0.67f)
            lineTo(w * 0.73f, h * 0.35f)
        }
        drawPath(check, color = Color.White, style = Stroke(width = w * 0.13f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun GoldenHourPhotoArtwork(thumbnailUrl: String = "", modifier: Modifier = Modifier) {
    if (thumbnailUrl.isNotBlank()) {
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0.0f to Color(0xFF0F172A),
                    0.38f to Color(0xFF312E81),
                    0.68f to Color(0xFFEA580C),
                    0.84f to Color(0xFFFBBF24),
                    1.0f to Color(0xFF1E293B)
                )
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFEF08A), Color(0xFFF97316).copy(alpha = 0.36f), Color.Transparent),
                center = Offset(w * 0.62f, h * 0.66f),
                radius = w * 0.34f
            ),
            radius = w * 0.34f,
            center = Offset(w * 0.62f, h * 0.66f)
        )
        drawCircle(Color(0xFFFFFBEB), radius = w * 0.085f, center = Offset(w * 0.62f, h * 0.66f))
        val hills = Path().apply {
            moveTo(0f, h * 0.78f)
            cubicTo(w * 0.25f, h * 0.62f, w * 0.45f, h * 0.76f, w * 0.72f, h * 0.70f)
            lineTo(w, h * 0.76f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(hills, Color(0xFF0F172A))
        for (i in 0..3) {
            val y = h * (0.82f + i * 0.036f)
            val inset = w * (0.48f + i * 0.025f)
            drawLine(
                color = Color(0xFFFDE68A).copy(alpha = 0.55f - i * 0.1f),
                start = Offset(inset, y),
                end = Offset(w - (inset - w * 0.24f), y),
                strokeWidth = (3f - i * 0.4f).dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
    }
}

@Composable
private fun MediaAttachmentCardGraphic(
    theme: MediaCardTheme,
    title: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (theme == MediaCardTheme.BLUEPRINT) {
                drawRect(Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1E3A8A))))
                for (i in 1..4) {
                    drawLine(Color.White.copy(alpha = 0.08f), Offset(0f, h * i / 5f), Offset(w, h * i / 5f), 1f)
                    drawLine(Color.White.copy(alpha = 0.08f), Offset(w * i / 5f, 0f), Offset(w * i / 5f, h), 1f)
                }
                val curve = Path().apply {
                    moveTo(w * 0.12f, h * 0.80f)
                    cubicTo(w * 0.30f, h * 0.80f, w * 0.36f, h * 0.25f, w * 0.88f, h * 0.22f)
                }
                drawPath(curve, Color(0xFF60A5FA), style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round))
                drawCircle(Color.White, 5.dp.toPx(), Offset(w * 0.12f, h * 0.80f))
                drawCircle(Color.White, 5.dp.toPx(), Offset(w * 0.88f, h * 0.22f))
            } else {
                drawRect(Brush.linearGradient(listOf(Color(0xFF064E3B), Color(0xFF0F766E))))
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.12f),
                    topLeft = Offset(w * 0.08f, h * 0.32f),
                    size = Size(w * 0.84f, h * 0.48f),
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                )
                drawCircle(Color(0xFFE2E5E9), 12.dp.toPx(), Offset(w * 0.18f, h * 0.56f))
                drawRoundRect(Color.White.copy(alpha = 0.35f), Offset(w * 0.26f, h * 0.52f), Size(w * 0.45f, 6.dp.toPx()), CornerRadius(3.dp.toPx(), 3.dp.toPx()))
                drawRoundRect(Color(0xFF34D399), Offset(w * 0.26f, h * 0.62f), Size(w * 0.45f, 2.dp.toPx()), CornerRadius(1.dp.toPx(), 1.dp.toPx()))
            }
        }
        Text(
            text = title,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.92f),
            modifier = Modifier.padding(10.dp)
        )
    }
}

@Composable
private fun HeartOutlineOrFilledIcon(filled: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val p = Path().apply {
            moveTo(12f * sx, 20.5f * sy)
            cubicTo(12f * sx, 20.5f * sy, 3.5f * sx, 14.8f * sy, 3.5f * sx, 8.8f * sy)
            cubicTo(3.5f * sx, 5.7f * sy, 5.9f * sx, 3.5f * sy, 8.6f * sx, 3.5f * sy)
            cubicTo(10.3f * sx, 3.5f * sy, 11.4f * sx, 4.4f * sy, 12f * sx, 5.5f * sy)
            cubicTo(12.6f * sx, 4.4f * sy, 13.7f * sx, 3.5f * sy, 15.4f * sx, 3.5f * sy)
            cubicTo(18.1f * sx, 3.5f * sy, 20.5f * sx, 5.7f * sy, 20.5f * sx, 8.8f * sy)
            cubicTo(20.5f * sx, 14.8f * sy, 12f * sx, 20.5f * sy, 12f * sx, 20.5f * sy)
            close()
        }
        if (filled) drawPath(p, color = color)
        else drawPath(p, color = color, style = Stroke(width = 2.1f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun CommentBubbleIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val p = Path().apply {
            moveTo(21f * sx, 11.5f * sy)
            cubicTo(21f * sx, 16.5f * sy, 16.9f * sx, 20.3f * sy, 12f * sx, 20.3f * sy)
            cubicTo(10.4f * sx, 20.3f * sy, 8.9f * sx, 19.9f * sy, 7.6f * sx, 19.1f * sy)
            lineTo(3f * sx, 20.8f * sy)
            lineTo(4.7f * sx, 16.3f * sy)
            cubicTo(3.7f * sx, 14.9f * sy, 3.1f * sx, 13.2f * sy, 3.1f * sx, 11.5f * sy)
            cubicTo(3.1f * sx, 6.6f * sy, 7.1f * sx, 2.8f * sy, 12f * sx, 2.8f * sy)
            cubicTo(17f * sx, 2.8f * sy, 21f * sx, 6.6f * sy, 21f * sx, 11.5f * sy)
            close()
        }
        drawPath(p, color = color, style = Stroke(width = 2.1f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun BookmarkOutlineOrFilledIcon(filled: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val p = Path().apply {
            moveTo(6f * sx, 4f * sy)
            lineTo(18f * sx, 4f * sy)
            lineTo(18f * sx, 20.5f * sy)
            lineTo(12f * sx, 16.2f * sy)
            lineTo(6f * sx, 20.5f * sy)
            close()
        }
        if (filled) drawPath(p, color = color)
        else drawPath(p, color = color, style = Stroke(width = 2.1f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun SearchMagnifierIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        drawCircle(color, radius = 7.5f * sx, center = Offset(10.5f * sx, 10.5f * sy), style = Stroke(2.2f * sx))
        drawLine(color, Offset(16f * sx, 16f * sy), Offset(21f * sx, 21f * sy), strokeWidth = 2.3f * sx, cap = StrokeCap.Round)
    }
}

@Composable
private fun ThemeSunMoonIcon(isDark: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        if (isDark) {
            drawCircle(color, radius = 4.2f * sx, center = Offset(12f * sx, 12f * sy), style = Stroke(2f * sx))
            for (i in 0 until 8) {
                val a = i * PI / 4.0
                val p1 = Offset(12f * sx + (cos(a) * 6.8f * sx).toFloat(), 12f * sy + (sin(a) * 6.8f * sy).toFloat())
                val p2 = Offset(12f * sx + (cos(a) * 9.5f * sx).toFloat(), 12f * sy + (sin(a) * 9.5f * sy).toFloat())
                drawLine(color, p1, p2, strokeWidth = 2f * sx, cap = StrokeCap.Round)
            }
        } else {
            val moon = Path().apply {
                addOval(Rect(Offset(4.5f * sx, 4.5f * sy), Size(15f * sx, 15f * sy)))
            }
            drawPath(moon, color = color, style = Stroke(2.1f * sx))
        }
    }
}

@Composable
private fun MoreDotsIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cy = size.height / 2f
        val r = size.width * 0.11f
        drawCircle(color, r, Offset(size.width * 0.18f, cy))
        drawCircle(color, r, Offset(size.width * 0.50f, cy))
        drawCircle(color, r, Offset(size.width * 0.82f, cy))
    }
}

@Composable
private fun ReplyArrowIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val stroke = Stroke(width = 2.3f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val arrow = Path().apply {
            moveTo(9f * sx, 14f * sy)
            lineTo(4f * sx, 9f * sy)
            lineTo(9f * sx, 4f * sy)
        }
        val curve = Path().apply {
            moveTo(4f * sx, 9f * sy)
            lineTo(15f * sx, 9f * sy)
            cubicTo(18.5f * sx, 9f * sy, 20f * sx, 11.5f * sy, 20f * sx, 15f * sy)
            lineTo(20f * sx, 20f * sy)
        }
        drawPath(arrow, color, style = stroke)
        drawPath(curve, color, style = stroke)
    }
}

@Composable
private fun ChevronDownOrUpIcon(pointingUp: Boolean, color: Color, modifier: Modifier = Modifier) {
    // Stageshow: rotate the same strokes smoothly instead of swapping paths in 1 frame
    val rotation by animateFloatAsState(
        targetValue = if (pointingUp) 180f else 0f,
        animationSpec = tween(260, easing = StageTravelEasing),
        label = "chevronRotation"
    )
    Canvas(modifier = modifier.graphicsLayer { rotationZ = rotation }) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val p = Path().apply {
            moveTo(6f * sx, 9f * sy)
            lineTo(12f * sx, 15f * sy)
            lineTo(18f * sx, 9f * sy)
        }
        drawPath(p, color = color, style = Stroke(width = 2.2f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun PlusToCloseIcon(isClose: Boolean, color: Color, modifier: Modifier = Modifier) {
    // Stageshow: identity-preserving morph (plus rotates 45° into close)
    val rotation by animateFloatAsState(
        targetValue = if (isClose) 45f else 0f,
        animationSpec = tween(260, easing = StageTravelEasing),
        label = "plusToCloseRotation"
    )
    Canvas(modifier = modifier.graphicsLayer { rotationZ = rotation }) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val w = 2.3f * sx
        drawLine(color, Offset(12f * sx, 5f * sy), Offset(12f * sx, 19f * sy), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(color, Offset(5f * sx, 12f * sy), Offset(19f * sx, 12f * sy), strokeWidth = w, cap = StrokeCap.Round)
    }
}

@Composable
private fun CloseCrossIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val w = 2.4f * sx
        drawLine(color, Offset(6f * sx, 6f * sy), Offset(18f * sx, 18f * sy), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(color, Offset(18f * sx, 6f * sy), Offset(6f * sx, 18f * sy), strokeWidth = w, cap = StrokeCap.Round)
    }
}

@Composable
private fun MicIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val stroke = Stroke(width = 2.1f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawRoundRect(
            color = color,
            topLeft = Offset(9f * sx, 3f * sy),
            size = Size(6f * sx, 11f * sy),
            cornerRadius = CornerRadius(3f * sx, 3f * sy),
            style = stroke
        )
        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(5.5f * sx, 6.5f * sy),
            size = Size(13f * sx, 11.5f * sy),
            style = stroke
        )
        drawLine(color, Offset(12f * sx, 18f * sy), Offset(12f * sx, 21.5f * sy), strokeWidth = 2.1f * sx, cap = StrokeCap.Round)
    }
}

@Composable
private fun PollBarsIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val w = 2.6f * sx
        drawLine(color, Offset(5f * sx, 19f * sy), Offset(5f * sx, 12f * sy), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(color, Offset(12f * sx, 19f * sy), Offset(12f * sx, 5f * sy), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(color, Offset(19f * sx, 19f * sy), Offset(19f * sx, 9f * sy), strokeWidth = w, cap = StrokeCap.Round)
    }
}

@Composable
private fun ReactionSmileyIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val stroke = Stroke(width = 2f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawCircle(color = color, radius = 9.2f * sx, center = Offset(12f * sx, 12f * sy), style = stroke)
        drawCircle(color = color, radius = 1.2f * sx, center = Offset(9f * sx, 10f * sy))
        drawCircle(color = color, radius = 1.2f * sx, center = Offset(15f * sx, 10f * sy))
        drawArc(
            color = color,
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(7.8f * sx, 9.2f * sy),
            size = Size(8.4f * sx, 7.6f * sy),
            style = stroke
        )
    }
}

@Composable
private fun CheckMiniIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val p = Path().apply {
            moveTo(5f * sx, 12.5f * sy)
            lineTo(10f * sx, 17.5f * sy)
            lineTo(19.5f * sx, 7f * sy)
        }
        drawPath(p, color = color, style = Stroke(width = 2.6f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/**
 * Stageshow Count rule: digits roll vertically on value change with tabular numerals;
 * unchanged digits stay anchored.
 */
@Composable
private fun RollingCountText(
    value: Int,
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight = FontWeight.SemiBold,
    suffix: String = "",
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val formatted = value.toString()
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        formatted.forEachIndexed { index, digitChar ->
            val placeSlot = formatted.length - index
            key(placeSlot) {
                if (reducedMotion) {
                    Text(
                        text = digitChar.toString(),
                        fontSize = fontSize,
                        fontWeight = fontWeight,
                        color = color,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                } else {
                    AnimatedContent(
                        targetState = digitChar,
                        transitionSpec = {
                            val rollingUp = targetState > initialState
                            (slideInVertically(tween(240, easing = StageTravelEasing)) { h ->
                                if (rollingUp) h / 2 else -h / 2
                            } + fadeIn(tween(180))) togetherWith
                                (slideOutVertically(tween(200, easing = StageTravelEasing)) { h ->
                                    if (rollingUp) -h / 2 else h / 2
                                } + fadeOut(tween(140)))
                        },
                        label = "rollingDigit_$placeSlot"
                    ) { char ->
                        Text(
                            text = char.toString(),
                            fontSize = fontSize,
                            fontWeight = fontWeight,
                            color = color,
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )
                    }
                }
            }
        }
        if (suffix.isNotEmpty()) {
            Text(
                text = suffix,
                fontSize = fontSize,
                fontWeight = fontWeight,
                color = color,
                style = TextStyle(fontFeatureSettings = "tnum")
            )
        }
    }
}

private fun formatRelativeTime(createdAtMillis: Long): String {
    val diffSec = ((System.currentTimeMillis() - createdAtMillis) / 1000L).coerceAtLeast(0L)
    return when {
        diffSec < 15 -> "Just now"
        diffSec < 60 -> "${diffSec}s"
        diffSec < 3600 -> "${diffSec / 60}m"
        diffSec < 86400 -> "${diffSec / 3600}h"
        else -> "${diffSec / 86400}d"
    }
}

// ============================================================================
// 11. ANDROID STUDIO PREVIEW
// ============================================================================
@Preview(showBackground = true, widthDp = 412, heightDp = 860)
@Composable
fun PhotoPostScreenDemoPreview() {
    MaterialTheme {
        PhotoPostScreenDemo()
    }
}