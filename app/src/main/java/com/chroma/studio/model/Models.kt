package com.chroma.studio.model

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import java.util.UUID

// ──────────────────────────────────────────────────────
// Social Features / Firestore Models
// ──────────────────────────────────────────────────────
data class UserProfile(
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val bio: String = "",
    val avatarUrl: String = "",
    val publicKey: String = "",
    val socialLinks: Map<String, String> = emptyMap(),
    val dob: Long? = null,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val role: String = "",
    val location: String = "",
    val officeLocation: String = "",
    val homeLocation: String = "",
    val countryLocation: String = "",
    val verified: Boolean = false,
    val online: Boolean = false,
    val showLocation: Boolean = false,
    val lastSeen: Long? = null,
    val bannerUrl: String = "",
    val bannerWorkId: String? = null,
    val createdAt: Any? = null,
    val dateOfBirth: Any? = null,
    val interests: List<String> = emptyList(),
    val gender: String = "",
    val agreedToTermsAt: Long? = null,
    
    // Account Center Preferences
    @get:com.google.firebase.firestore.PropertyName("privateAccount")
    @set:com.google.firebase.firestore.PropertyName("privateAccount")
    var isPrivateAccount: Boolean = false,
    val allowTagging: Boolean = true,
    val showOnlineStatus: Boolean = true,
    val pushNotificationsEnabled: Boolean = true,
    val emailNotificationsEnabled: Boolean = true,
    val quietModeEnabled: Boolean = false,
    val unrecognizedLoginsEnabled: Boolean = false,
    val contentTags: List<String> = emptyList(),
    val dataVisibility: Map<String, String> = emptyMap()
) {
    @get:com.google.firebase.firestore.Exclude
    val createdAtAsLong: Long?
        get() = when (createdAt) {
            is Long -> createdAt
            is Double -> createdAt.toLong()
            is com.google.firebase.Timestamp -> createdAt.seconds * 1000 + createdAt.nanoseconds / 1000000
            is java.util.Date -> createdAt.time
            else -> null
        }

    @get:com.google.firebase.firestore.Exclude
    val dateOfBirthAsLong: Long?
        get() = when (dateOfBirth) {
            is Long -> dateOfBirth
            is Double -> dateOfBirth.toLong()
            is com.google.firebase.Timestamp -> dateOfBirth.seconds * 1000 + dateOfBirth.nanoseconds / 1000000
            is java.util.Date -> dateOfBirth.time
            else -> null
        }

}

data class DiscoverPost(
    val id: String = "",
    val creatorId: String = "",
    val authorUsername: String = "",
    val authorDisplayName: String = "",
    val authorAvatarUrl: String = "",
    val authorVerified: Boolean = false,
    val title: String = "",
    val description: String = "",
    val aspectRatio: String = "4:3",
    val likesCount: Int = 0,
    val likedBy: List<String> = emptyList(),
    val remixCount: Int = 0,
    val remixedBy: List<String> = emptyList(),
    val downloadCount: Int = 0,
    val downloadedBy: List<String> = emptyList(),
    val shareCount: Int = 0,
    val sharedBy: List<String> = emptyList(),
    val saveCount: Int = 0,
    val savedBy: List<String> = emptyList(),
    val jsonUrl: String = "",
    val imageUrl: String? = null,
    val imageUrls: List<String> = emptyList(),
    val workUrls: List<String> = emptyList(),
    val videoUrl: String? = null,       // For postType = "video" community posts
    val thumbnailUrl: String? = null,   // Poster frame for video posts
    val postType: String = "work", // "work" for JSON, "image" for photos, "video" for video posts
    val tags: List<String> = emptyList(),
    val mentions: List<String> = emptyList(),
    val trendingTag: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    
    // Privacy & Visibility Controls for Action Counters
    val hideLikesCount: Boolean = false,
    val hideSharesCount: Boolean = false,
    val hideRemixesCount: Boolean = false,
    val hideDownloadsCount: Boolean = false,
    val hideSavesCount: Boolean = false,
    val turnOffComments: Boolean = false,
    val turnOffRemix: Boolean = false,
    
    // Remix Metadata
    val remixedFromPostId: String? = null,
    val remixedFromAuthorId: String? = null,
    val remixedFromAuthorUsername: String? = null,
    val remixedFromAuthorDisplayName: String? = null,
    val remixedFromAuthorAvatar: String? = null,
    val remixedFromAuthorVerified: Boolean = false,
    
    // Archive state
    @get:com.google.firebase.firestore.PropertyName("archived")
    @set:com.google.firebase.firestore.PropertyName("archived")
    var isArchived: Boolean = false
)

data class CreatorPostDraft(
    val id: String = java.util.UUID.randomUUID().toString(),
    val mode: String = "post", // "post", "flash", "work"
    val title: String = "",
    val description: String = "",
    val aspectRatio: String = "4:3",
    val categoryTag: String? = null,
    val hashtags: List<String> = emptyList(),
    val mentions: List<String> = emptyList(),
    val hideLikeCount: Boolean = false,
    val turnOffComments: Boolean = false,
    val turnOffRemix: Boolean = false,
    val imageUris: List<String> = emptyList(),
    val videoUri: String? = null,
    val workIds: List<String> = emptyList(),
    val flashTrimStartMs: Long = 0L,
    val flashTrimEndMs: Long = 15000L,
    val timestamp: Long = System.currentTimeMillis(),
    val customCoverUri: String? = null,
    val selectedCoverFrameMs: Long = 0L,
    val audienceVisibility: String = "public",
    val includeOriginalAudio: Boolean = true,
    val allowDownloads: Boolean = true,
    val highQualityUpload: Boolean = true
)

data class ChatRoom(
    val id: String = "",
    val name: String? = null,
    @get:com.google.firebase.firestore.PropertyName("isGroup")
    @set:com.google.firebase.firestore.PropertyName("isGroup")
    var isGroup: Boolean = false,
    val avatarUrl: String? = null,
    val participants: List<String> = emptyList(),
    val admins: List<String> = emptyList(),
    val createdBy: String? = null,
    val groupDescription: String? = null,
    val lastMessage: String = "",
    val lastSender: String? = null,
    val lastMessageTime: Long = 0L,
    val unreadCount: Map<String, Int> = emptyMap(),
    val favoritedBy: List<String> = emptyList(),
    val deletedBy: List<String> = emptyList(),
    // Per-user deletion timestamp: uid -> timestamp when they deleted the chat
    // Messages before this timestamp are hidden for that user
    val deletedAt: Map<String, Long> = emptyMap()
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val imageUrl: String? = null,
    val mediaUrl: String? = null,
    val mediaType: String? = null, // e.g., "video/mp4", "audio/mpeg", "image/jpeg"
    val workId: String? = null,
    @get:com.google.firebase.firestore.Exclude
    val isPending: Boolean = false,
    @get:com.google.firebase.firestore.Exclude
    val isError: Boolean = false,
    val messageType: String = "text", // "text", "image", "video", "audio", "system"
    val iv: String = "", // Initialization Vector for AES
    val encryptedAesKeys: Map<String, String> = emptyMap(), // participantId -> RSA Encrypted AES Key
    val reactions: Map<String, String> = emptyMap(), // Maps userId -> emoji (e.g. "❤️", "👍")
    val durationSeconds: Int? = null, // For audio voice notes
    val replyToMessageId: String? = null,
    val pollVotes: Map<String, String> = emptyMap(), // userId -> comma-separated option indexes e.g. "0" or "0,2"
    val isEdited: Boolean = false,
    val deletedFor: List<String> = emptyList(), // User IDs who have deleted this message for themselves
    val isPollClosed: Boolean = false
)

@kotlinx.serialization.Serializable
data class EncryptedPayload(
    val encryptedText: String,
    val iv: String,
    val encryptedAesKeys: Map<String, String>,
    val imageUrl: String? = null
)

data class Story(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val mediaUrl: String = "",
    val textContent: String = "",
    val textFormat: String = "{}", // JSON of text formatting
    val backgroundColor: String = "",
    val audienceType: String = "public", // "public", "followers", "custom"
    val customAudienceIds: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 86400000, // 24h
    val views: Int = 0,
    val likes: Int = 0,
    val shares: Int = 0,
    val viewedBy: List<String> = emptyList(),
    val likedBy: List<String> = emptyList()
)

data class UserStoryGroup(
    val userId: String = "",
    val userProfile: UserProfile = UserProfile(),
    val stories: List<Story> = emptyList(),
    val allSeen: Boolean = false
)

// ────────────────────────────────────────────────────────────────────────────
// Notifications & Activity Models
// ────────────────────────────────────────────────────────────────────────────

enum class NotificationActionType {
    LIKE,
    COMMENT,
    FOLLOW,
    MESSAGE,
    REMIX,
    MENTION,
    SYSTEM_DROP,
    PROJECT_EXPORT,
    PROJECT_COLLAB
}

data class ChromaNotification(
    val id: String = UUID.randomUUID().toString(),
    val type: NotificationActionType = NotificationActionType.LIKE,
    val actorUid: String = "",
    val actorName: String = "",
    val actorHandle: String = "",
    val actorAvatarUrl: String? = null,
    val actorGender: String? = null,
    val isVerified: Boolean = false,
    val actionText: String = "",
    val detailText: String? = null,
    val targetPreviewUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val targetId: String? = null,
    val following: Boolean = false,
    val likedComment: Boolean = false,
    val likedBack: Boolean = false
) {
    val isUnread: Boolean get() = !isRead

    fun toMap(): Map<String, Any?> = mapOf(
        "type" to type.name,
        "actorUid" to actorUid,
        "actorName" to actorName,
        "actorHandle" to actorHandle,
        "actorAvatarUrl" to actorAvatarUrl,
        "actorGender" to actorGender,
        "isVerified" to isVerified,
        "actionText" to actionText,
        "detailText" to detailText,
        "targetPreviewUrl" to targetPreviewUrl,
        "timestamp" to timestamp,
        "isRead" to isRead,
        "targetId" to targetId
    )
}

// ──────────────────────────────────────────────────────
// Flash Feed — Short video / photo posts (≤30 s)
// ──────────────────────────────────────────────────────

enum class FlashMediaType { VIDEO, PHOTO, SLIDESHOW }

data class FlashComment(
    val id: String = UUID.randomUUID().toString(),
    val videoId: String = "",
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
    val verified: Boolean = false,
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val likesCount: Int = 0,
    val likedBy: List<String> = emptyList(),
    val replyToCommentId: String? = null,
    val replyFlashVideoUrl: String? = null,
    val isPending: Boolean = false,
    val isError: Boolean = false
)

data class FlashVideo(
    val id: String = "",
    val creatorId: String = "",
    val authorUsername: String = "",
    val authorDisplayName: String = "",
    val authorAvatarUrl: String = "",
    val authorVerified: Boolean = false,
    val videoUrl: String = "",      // Cloudinary optimised URL
    val thumbnailUrl: String = "",  // Cloudinary image URL
    val caption: String = "",
    val hashtags: List<String> = emptyList(),
    val mentions: List<String> = emptyList(),
    val likesCount: Int = 0,
    val likedBy: List<String> = emptyList(),
    val commentCount: Int = 0,
    val remixCount: Int = 0,
    val remixedBy: List<String> = emptyList(),
    val shareCount: Int = 0,
    val sharedBy: List<String> = emptyList(),
    val saveCount: Int = 0,
    val savedBy: List<String> = emptyList(),
    val duration: Long = 0L,       // ms, capped at 30 000
    val remixedFromPostId: String? = null,
    val remixedFromAuthorUsername: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val hideLikesCount: Boolean = false,
    val turnOffComments: Boolean = false,
    val turnOffRemix: Boolean = false,
    @get:com.google.firebase.firestore.PropertyName("archived")
    @set:com.google.firebase.firestore.PropertyName("archived")
    var isArchived: Boolean = false
)

/** Locally persisted draft before publishing a Flash. */
data class FlashDraft(
    val id: String = UUID.randomUUID().toString(),
    val localUri: String = "",           // content:// URI string
    val mediaType: FlashMediaType = FlashMediaType.VIDEO,
    val thumbnailUri: String = "",
    val caption: String = "",
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    val filterName: String = "None",
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val hue: Float = 0f,
    val createdAt: Long = System.currentTimeMillis()
)

// ──────────────────────────────────────────────────────
// Persisted work (saved locally in SharedPreferences)
// ──────────────────────────────────────────────────────
data class ChromaWork(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Untitled",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastModifiedAt: Long = System.currentTimeMillis(),
    val layersJson: String = "[]",  // Gson-serialized List<GradientLayer>
    val canvasShape: String = "rounded",
    val isHomeBackground: Boolean = false,
    val isDeleted: Boolean = false,
    
    // Remix Metadata
    val remixedFromPostId: String? = null,
    val remixedFromAuthorId: String? = null,
    val remixedFromAuthorUsername: String? = null,
    val remixedFromAuthorDisplayName: String? = null,
    val remixedFromAuthorAvatar: String? = null,
    val remixedFromAuthorVerified: Boolean = false
)
enum class LayerType(val label: String, val icon: String) {
    LINEAR("Linear", "ph-arrow-up-right"),
    RADIAL("Radial", "ph-circle"),
    CONIC("Conic", "ph-cone"),
    MESH("Mesh", "ph-grid-four"),
    BLOB("Blob", "ph-drop"),
    AURORA("Aurora", "ph-sparkle"),
    LIQUID("Liquid", "ph-drop")
}

enum class ChromaBlendMode(val label: String, val compose: BlendMode) {
    NORMAL("Normal", BlendMode.SrcOver),
    MULTIPLY("Multiply", BlendMode.Multiply),
    SCREEN("Screen", BlendMode.Screen),
    OVERLAY("Overlay", BlendMode.Overlay),
    DARKEN("Darken", BlendMode.Darken),
    LIGHTEN("Lighten", BlendMode.Lighten),
    COLOR_DODGE("Color Dodge", BlendMode.ColorDodge),
    DIFFERENCE("Difference", BlendMode.Difference)
}

enum class ColorBlindMode(val label: String) {
    NONE("Normal Vision"),
    PROTANOPIA("Protanopia (Red-Blind)"),
    DEUTERANOPIA("Deuteranopia (Green-Blind)"),
    TRITANOPIA("Tritanopia (Blue-Blind)")
}

enum class PostProcessingFx(val label: String) {
    NONE("None (Clean)"),
    GRAIN("Film Grain"),
    HALFTONE("Retro Halftone"),
    DITHER("Brutalist Dither")
}

enum class AnimStyle(val label: String) {
    DRIFT("Drift (Pan Position)"),
    PULSE("Pulse (Scale & Opacity)"),
    HUE("Hue Shift (Color Rotation)"),
    BREATHE("Breathe (Size Expansion)")
}

enum class AnimStatus { STOPPED, PLAYING, PAUSED }


data class ColorStop(
    val id: String = UUID.randomUUID().toString(),
    val color: Color,
    val position: Float // 0f..100f, matches `pos` in the JS stop model
)

/**
 * Mirrors the JS blob object: { x, y, width, height, feather, opacity }
 * x/y are % positions (0..100), width/height are % of canvas size (10..200),
 * feather is 0..100, opacity is 0..1, rotation is degrees.
 */
data class BlobPoint(
    val x: Float = 50f,
    val y: Float = 50f,
    val width: Float = 40f,
    val height: Float = 40f,
    val feather: Float = 100f,
    val opacity: Float = 1f,
    val rotation: Float = 0f    // degrees, applied via canvas transform
)

data class GradientLayer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: LayerType = LayerType.LINEAR,
    val blendMode: ChromaBlendMode = ChromaBlendMode.NORMAL,
    val opacity: Float = 1f,       // 0..1
    val angle: Float = 90f,        // degrees, used for LINEAR
    val centerX: Float = 50f,      // % , used for RADIAL/CONIC
    val centerY: Float = 50f,      // %
    val visible: Boolean = true,
    val expanded: Boolean = false,
    val animated: Boolean = false,
    val animSpeed: Float = 50f,     // 1..100
    val animIntensity: Float = 50f, // 0..100
    val repeatPattern: Boolean = false,
    val width: Float = 70f,         // for RADIAL width
    val height: Float = 70f,        // for RADIAL height
    val feather: Float = 100f,      // global fallback
    val waveSpeed: Float = 50f,
    val complexity: Float = 50f,
    val brightness: Float = 50f,
    val columns: Int = 3,
    val rows: Int = 3,
    val hasBaseBackground: Boolean = false,
    val blobBgColor: Color = Color.Black,
    val activeBlobIdx: Int = 0,
    // Per-blob settings matching the JS { x, y, width, height, feather, opacity } structure
    val blobs: List<BlobPoint> = listOf(
        BlobPoint(x = 25f, y = 40f, width = 40f, height = 40f),
        BlobPoint(x = 75f, y = 60f, width = 40f, height = 40f)
    ),
    // Mesh control points (non-blob layers still use this)
    val meshPoints: List<androidx.compose.ui.geometry.Offset> = listOf(
        androidx.compose.ui.geometry.Offset(25f, 40f),
        androidx.compose.ui.geometry.Offset(75f, 60f)
    ),
    val stops: List<ColorStop> = listOf(
        ColorStop(color = Color(0xFF4F46E5), position = 0f),
        ColorStop(color = Color(0xFF818CF8), position = 100f)
    )
)

// Apple-style presets, ported from the `preset-grid` data in index.html's JS
data class GradientPreset(val name: String, val colors: List<Color>, val type: LayerType)

val ApplePresets = listOf(
    GradientPreset("Sunset", listOf(Color(0xFFFF7E5F), Color(0xFFFEB47B)), LayerType.LINEAR),
    GradientPreset("Ocean", listOf(Color(0xFF2E3192), Color(0xFF1BFFFF)), LayerType.LINEAR),
    GradientPreset("Candy", listOf(Color(0xFFFC466B), Color(0xFF3F5EFB)), LayerType.LINEAR),
    GradientPreset("Mint", listOf(Color(0xFF00B09B), Color(0xFF96C93D)), LayerType.LINEAR),
    GradientPreset("Peach", listOf(Color(0xFFFFECD2), Color(0xFFFCB69F)), LayerType.RADIAL),
    GradientPreset("Aurora", listOf(Color(0xFF00C9FF), Color(0xFF92FE9D)), LayerType.AURORA),
    GradientPreset("Grape", listOf(Color(0xFF7F00FF), Color(0xFFE100FF)), LayerType.CONIC),
    GradientPreset("Fire", listOf(Color(0xFFF83600), Color(0xFFF9D423)), LayerType.LINEAR)
)
