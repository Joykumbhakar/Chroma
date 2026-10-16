package com.chroma.studio.chat.data
import com.chroma.studio.R

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class User(
    val id: String,
    val name: String,
    val username: String,
    val avatar: String,
    val banner: String = drawableUri(com.chroma.studio.R.drawable.profile_banner_joy),
    val status: String,
    val isVerified: Boolean = false
)

@Immutable
data class StoryViewerInfo(
    val id: String,
    val name: String,
    val username: String,
    val avatar: String,
    val timeAgo: String,
    val reactedEmoji: String? = null
)

@Immutable
data class Story(
    val id: String,
    val authorName: String,
    val avatar: String,
    val timeAgo: String,
    val image: String? = null,
    val caption: String,
    val seen: Boolean = false,
    val isMyStory: Boolean = false,
    val viewCount: Int = 0,
    val likesCount: Int = 0,
    val viewers: List<StoryViewerInfo> = emptyList(),
    val musicTrack: String? = null,
    val filterName: String = "Normal",
    val locationTag: String? = null,
    val bgGradientColors: List<Color>? = null,
    val textHighlightMode: String = "NONE",
    val fontIndex: Int = 0,
    val textAlignStr: String = "Center",
    val pollData: PollData? = null
)

@Immutable
data class PollVoter(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val avatar: String,
    val isCurrentUser: Boolean = false,
    val time: String = ""
)

@Immutable
data class PollOption(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val votes: Int = 0,
    val isUserVoted: Boolean = false,
    val voters: List<PollVoter> = emptyList()
)

@Immutable
data class PollData(
    val id: String = java.util.UUID.randomUUID().toString(),
    val question: String,
    val options: List<PollOption>,
    val allowMultipleAnswers: Boolean = false,
    val isAnonymous: Boolean = false,
    val isClosed: Boolean = false,
    val correctOptionIndex: Int? = null,
    val totalVotersCount: Int = 0,
    val creatorName: String? = null
)

@Immutable
data class ReplyQuote(
    val author: String,
    val text: String,
    val messageId: String? = null,
    val mediaType: String? = null
)

@Immutable
data class GroupMember(
    val name: String,
    val role: String,
    val avatar: String
)

@Immutable
data class ChatFileItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val size: String,
    val uri: String? = null,
    val extension: String = "FILE"
)

@Immutable
data class ChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val text: String? = null,
    val mediaType: String? = null, // "image", "image_stack", "voice", "poll", "location", "file", "file_stack", "call"
    val mediaUrl: String? = null,
    val mediaUrls: List<String> = emptyList(),
    val localUri: String? = null,
    val fileName: String? = null,
    val fileSize: String? = null,
    val fileItems: List<ChatFileItem> = emptyList(),
    val voiceDuration: Int = 4,
    val waveformSamples: List<Float> = emptyList(),
    val transcription: String? = null,
    val isTranscribing: Boolean = false,
    val pollData: PollData? = null,
    val locationTitle: String? = null,
    val callDuration: String? = null,
    val callType: String? = null,
    val time: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "read", // "sent", "delivered", "read"
    val reactions: Map<String, Int> = emptyMap(),
    val userReaction: String? = null,
    val replyTo: ReplyQuote? = null,
    val isEdited: Boolean = false
)

@Immutable
data class ChatConversation(
    val id: String,
    val name: String,
    val type: String, // "direct", "group"
    val avatar: String,
    val banner: String = drawableUri(com.chroma.studio.R.drawable.profile_banner_joy),
    val online: Boolean,
    val statusText: String,
    val phone: String,
    val bio: String,
    val unreadCount: Int = 0,
    val pinnedMessage: String? = null,
    val isMuted: Boolean = false,
    val isBlocked: Boolean = false,
    val isVerified: Boolean = false,
    val members: List<GroupMember>? = null,
    val messages: List<ChatMessage> = emptyList()
)

enum class AccentColorType(val title: String, val color: Color, val hoverColor: Color, val subtleColor: Color) {
    BLUE("Blue", Color(0xFF0078FE), Color(0xFF005ECB), Color(0x1F0078FE)),
    PURPLE("Purple", Color(0xFF8B5CF6), Color(0xFF7C3AED), Color(0x1F8B5CF6)),
    EMERALD("Emerald", Color(0xFF10B981), Color(0xFF059669), Color(0x1F10B981)),
    ROSE("Rose", Color(0xFFF43F5E), Color(0xFFE11D48), Color(0x1FF43F5E)),
    AMBER("Amber", Color(0xFFF59E0B), Color(0xFFD97706), Color(0x24F59E0B))
}

fun drawableUri(resId: Int): String = "android.resource://com.chroma.studio/$resId"

enum class WallpaperType(
    val title: String,
    val drawableRes: Int,
    val category: String = "Devices & OS"
) {
    WALLPAPER_IPHONE("iPhone Studio", com.chroma.studio.R.drawable.wallpaper_iphone_small_screen, "Devices & OS"),
    WALLPAPER_HD_1("Neon Horizon", com.chroma.studio.R.drawable.wallpaper_hd_1, "HD Wallpapers"),
    WALLPAPER_HD_2("Aurora Spectrum", com.chroma.studio.R.drawable.wallpaper_hd_2, "HD Wallpapers"),
    WALLPAPER_HD_3("Golden Sunset", com.chroma.studio.R.drawable.wallpaper_hd_3, "HD Wallpapers"),
    WALLPAPER_HD_4("Cyber Matrix", com.chroma.studio.R.drawable.wallpaper_hd_4, "HD Wallpapers"),
    WALLPAPER_1("Flowing Silk", com.chroma.studio.R.drawable.wallpaper1, "HD Wallpapers"),
    WALLPAPERS_ISLAND("Island Coast", com.chroma.studio.R.drawable.wallpapers, "Nature & Landscapes"),
    WALLPAPER_STOCK("Ocean Waves", com.chroma.studio.R.drawable.wallpaper_stock, "Nature & Landscapes"),
    WALLPAPER_ABSTRACT("Deep Horizon", com.chroma.studio.R.drawable.wallpaper, "Abstract"),
    WALLPAPER_MACBOOK("MacBook Petals", com.chroma.studio.R.drawable.wallpaper_macbook, "Devices & OS"),
    WALLPAPER_MACBOOK_ALT("MacBook Classic", com.chroma.studio.R.drawable.wallpaper_macbook_alt, "Devices & OS"),
    WALLPAPER_WINDOWS("Windows Bloom", com.chroma.studio.R.drawable.wallpaper_windows, "Devices & OS"),
    WALLPAPER_MAC("macOS Canvas", com.chroma.studio.R.drawable.wallpaper_mac_big_screen, "Devices & OS");

    companion object {
        val DEFAULT: WallpaperType get() = WALLPAPER_IPHONE
    }
}

data class CoverBannerPreset(
    val title: String,
    val drawableRes: Int,
    val category: String
)

enum class PointyCornerStyle(
    val id: String,
    val title: String,
    val description: String
) {
    SHARP_ZERO("sharp_zero", "Sharp Notch (0dp)", "Crisp right-angle corner notch pointing towards sender"),
    CHAMFERED_CUT("chamfered_cut", "Chamfered 45°", "Futuristic angular chamfer cut notch"),
    SUBTLE_ROUNDED("subtle_rounded", "Micro Curved (2dp)", "Apple-inspired smooth micro-rounded notch"),
    ROUNDED_MATCH("rounded_match", "Seamless Match", "Symmetrical curve matching surrounding radius"),
    NONE("none", "Flat Edge", "No directional pointy notch")
}

@Immutable
data class CustomCornerConfig(
    val customEnabled: Boolean = false,
    val topStart: Float = 18f,
    val topEnd: Float = 18f,
    val bottomStart: Float = 18f,
    val bottomEnd: Float = 4f,
    val pointyCornerStyle: PointyCornerStyle = PointyCornerStyle.SUBTLE_ROUNDED,
    val applyToBoth: Boolean = true
)

enum class BubbleAnimationStyle(
    val id: String,
    val title: String,
    val description: String
) {
    POP_ZOOM(
        id = "pop_zoom",
        title = "Pop-Up Zoom (Default)",
        description = "Snappy Apple spring zoom with overshoot bounce (Scale 0.45 ➔ 1.08 ➔ 1.0)"
    ),
    SLIDE_UP_FADE(
        id = "slide_up_fade",
        title = "Slide Up & Float",
        description = "Smooth vertical float-up from bottom with spring dampening"
    ),
    ELASTIC_SPRING(
        id = "elastic_spring",
        title = "Elastic Jelly Spring",
        description = "Playful bouncy spring with elastic physical oscillations"
    ),
    FLIP_IN_3D(
        id = "flip_in_3d",
        title = "3D Card Flip",
        description = "Subtle perspective rotation flip on arrival"
    ),
    SLIDE_IN_SIDE(
        id = "slide_in_side",
        title = "Side Edge Fly-In",
        description = "Quick horizontal fly-in directly from the sender's screen edge"
    ),
    EXPAND_REVEAL(
        id = "expand_reveal",
        title = "Clip Expand",
        description = "Crisp geometric vertical clip expand from center"
    ),
    GENTLE_FADE(
        id = "gentle_fade",
        title = "Gentle Dissolve",
        description = "Ultra-clean minimalist alpha crossfade without displacement"
    )
}

enum class BubbleStyleType(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val incomingPreview: String,
    val outgoingPreview: String
) {
    ROUNDED(
        id = "rounded",
        title = "1. Modern Rounded",
        category = "Modern",
        description = "Apple-inspired rounded pill bubble with glossy 3D rim highlight and sleek border",
        incomingPreview = "Clean, modern, and perfectly rounded.",
        outgoingPreview = "With a thin glossy 3D border! ✨"
    ),
    CLASSIC_IOS(
        id = "classic_ios",
        title = "2. Classic iOS",
        category = "Modern",
        description = "Clean Apple iOS iMessage style with light gray and blue bubbles",
        incomingPreview = "Hey! Are we still on for tonight?",
        outgoingPreview = "Yes! See you at 8. 🍕"
    ),
    WHATSAPP(
        id = "whatsapp",
        title = "3. WhatsApp",
        category = "Classic",
        description = "WhatsApp style with signature bubble tails and double blue ticks",
        incomingPreview = "Did you send the files?",
        outgoingPreview = "Just emailed them to you! ✓✓"
    ),
    MESSENGER(
        id = "messenger",
        title = "4. Messenger Gradient",
        category = "Gradients",
        description = "Smooth cyan-to-blue vibrant gradient bubbles",
        incomingPreview = "Look at this cool gradient!",
        outgoingPreview = "That is super smooth. ✨"
    ),
    NEO_BRUTALISM(
        id = "neo_brutalism",
        title = "5. Neo-Brutalism",
        category = "Bold",
        description = "High-contrast clean borders with pop-out drop shadows",
        incomingPreview = "THIS IS BOLD.",
        outgoingPreview = "Clean and readable! 📢"
    ),
    GLASSMORPHISM(
        id = "glassmorphism",
        title = "6. Glassmorphism",
        category = "Modern",
        description = "Translucent frosted glass with background blur and crystalline border",
        incomingPreview = "Can you see through this?",
        outgoingPreview = "Crystal clear. 🔮"
    ),
    MINIMAL_OUTLINED(
        id = "minimal_outlined",
        title = "7. Minimal Outlined",
        category = "Minimal",
        description = "Ultra-minimalist wireframe incoming and solid obsidian outgoing",
        incomingPreview = "Less is more.",
        outgoingPreview = "Agreed entirely."
    ),
    NEUMORPHISM(
        id = "neumorphism",
        title = "8. Soft UI Neumorphism",
        category = "Soft UI",
        description = "Pressed inset incoming shadow and extruded outgoing bubble shadows",
        incomingPreview = "Pressed into the screen...",
        outgoingPreview = "And this one pops out!"
    ),
    CYBERPUNK(
        id = "cyberpunk",
        title = "9. Cyberpunk OS",
        category = "Futuristic",
        description = "Deep obsidian with electric cyan and magenta laser glow",
        incomingPreview = "> INCOMING TRANSMISSION...",
        outgoingPreview = "> SECURE LINK ESTABLISHED."
    ),
    RETRO_PIXEL(
        id = "retro_pixel",
        title = "10. Retro 8-Bit",
        category = "Retro",
        description = "Refined arcade styling with subtle beveled accents",
        incomingPreview = "Press START to continue!",
        outgoingPreview = "READY PLAYER ONE 👾"
    ),
    TEAM_CHAT(
        id = "team_chat",
        title = "11. Team Workplace",
        category = "Modern",
        description = "Clean workplace card layout with compact badge reactions",
        incomingPreview = "I've updated the Figma file.",
        outgoingPreview = "Looking over it right now! 🔥"
    ),
    CLAYMORPHISM(
        id = "claymorphism",
        title = "12. Claymorphism",
        category = "3D",
        description = "Soft, fluffy 3D inflated digital clay bubbles",
        incomingPreview = "Soft, fluffy, and very 3D!",
        outgoingPreview = "Looks like digital clay. 🧱"
    ),
    METRO_UI(
        id = "metro_ui",
        title = "13. Metro OS",
        category = "Flat",
        description = "Modern Windows Fluent styling with sleek rounded tiles",
        incomingPreview = "Fluent design language.",
        outgoingPreview = "Subtle acrylic sheen ✨"
    ),
    HOLOGRAPHIC(
        id = "holographic",
        title = "14. Holographic Pearl",
        category = "Gradients",
        description = "Sophisticated pearlescent iridescent titanium silk",
        incomingPreview = "Look at that shimmer!",
        outgoingPreview = "Shimmering titanium silk ✨"
    ),
    SKETCHBOOK(
        id = "sketchbook",
        title = "15. Sketchbook",
        category = "Artistic",
        description = "Warm textured parchment and charcoal borders",
        incomingPreview = "Did you finish the drawing?",
        outgoingPreview = "Still sketching it out... ✏️"
    ),
    NEON_NIGHTS(
        id = "neon_nights",
        title = "16. Neon Nights",
        category = "Futuristic",
        description = "Deep dark backdrop with electric violet and cyan neon glows",
        incomingPreview = "Are we going out tonight?",
        outgoingPreview = "Meeting at the lounge. 🕹️"
    ),
    WIN95(
        id = "win95",
        title = "17. Retro OS Win95",
        category = "Retro",
        description = "Classic 3D beveled Windows button borders and retro gray",
        incomingPreview = "You've got mail!",
        outgoingPreview = "Downloading attachment (99% complete)..."
    ),
    COMIC_BOOK(
        id = "comic_book",
        title = "18. Comic Book",
        category = "Artistic",
        description = "Refined pop-art styling with clean rounded corners",
        incomingPreview = "MEANWHILE...",
        outgoingPreview = "BAM! POW! 💥"
    ),
    TERMINAL(
        id = "terminal",
        title = "19. Terminal Hacker",
        category = "Code",
        description = "Matrix green console commands on pitch black background",
        incomingPreview = "guest$ ./sendMessage.sh",
        outgoingPreview = "admin$ Acknowledged. Terminating link."
    ),
    PRESTIGE(
        id = "prestige",
        title = "20. Prestige Luxury",
        category = "Luxury",
        description = "Deep emerald forest with gold foil borders and italic serif styling",
        incomingPreview = "Elegance is beauty that never fades.",
        outgoingPreview = "Quite right, indeed. 🥂"
    ),
    ORGANIC_BLOB(
        id = "organic_blob",
        title = "21. Organic Fluid",
        category = "Fluid",
        description = "Asymmetric organic morphing silhouettes with twilight amber gradients",
        incomingPreview = "Fluid natural curves.",
        outgoingPreview = "Sunset twilight vibes. 🌊"
    )
}

enum class VoiceGlobeStyleType(
    val id: String,
    val title: String,
    val description: String,
    val previewGradient: List<Color>
) {
    ORIGINAL_BLUE(
        id = "original_blue",
        title = "Deep Ocean Classic",
        description = "Apple-inspired deep blue liquid orb with pulse rings",
        previewGradient = listOf(Color(0xFF60A5FA), Color(0xFF0078FE), Color(0xFF1E3A8A))
    ),
    CHROMATIC_3D(
        id = "chromatic_3d",
        title = "Cosmic Amethyst",
        description = "Royal purple and violet plasma orb with celestial pulse",
        previewGradient = listOf(Color(0xFFC084FC), Color(0xFF8B5CF6), Color(0xFF3B0764))
    ),
    AURORA_PULSE(
        id = "aurora_pulse",
        title = "Aurora Neon",
        description = "Vibrant hot magenta and ultraviolet glowing plasma orb",
        previewGradient = listOf(Color(0xFFF472B6), Color(0xFFEC4899), Color(0xFF831843))
    ),
    CYBER_GRID(
        id = "cyber_grid",
        title = "Cyber Emerald",
        description = "High-tech mint green and electric emerald laser sphere",
        previewGradient = listOf(Color(0xFF34D399), Color(0xFF10B981), Color(0xFF064E3B))
    ),
    SOLAR_FLARE(
        id = "solar_flare",
        title = "Solar Flame",
        description = "Radiant molten gold, fiery orange and crimson sun orb",
        previewGradient = listOf(Color(0xFFFBBF24), Color(0xFFF97316), Color(0xFF7C2D12))
    ),
    DARK_MATTER(
        id = "dark_matter",
        title = "Obsidian Quantum",
        description = "Midnight OLED obsidian core with electric indigo aura",
        previewGradient = listOf(Color(0xFF818CF8), Color(0xFF4F46E5), Color(0xFF0F172A))
    ),
    ROSE_QUARTZ(
        id = "rose_quartz",
        title = "Sakura Blossom",
        description = "Ethereal cherry blossom pastel pink and pearl white orb",
        previewGradient = listOf(Color(0xFFFDA4AF), Color(0xFFF43F5E), Color(0xFF881337))
    ),
    ELECTRIC_CYAN(
        id = "electric_cyan",
        title = "Aquamarine Prism",
        description = "Electric turquoise and deep arctic ocean cyan liquid orb",
        previewGradient = listOf(Color(0xFF22D3EE), Color(0xFF06B6D4), Color(0xFF164E63))
    )
}

object MockData {
    val currentUser = User(
        id = "user_me",
        name = "Joy (You)",
        username = "@joy_oscellion",
        avatar = drawableUri(com.chroma.studio.R.drawable.profile_avatar_joy),
        status = "Available",
        isVerified = true
    )

    fun initialStories(): List<Story> = listOf(
        Story(
            id = "s_my_1",
            authorName = "Your Story",
            avatar = drawableUri(com.chroma.studio.R.drawable.profile_avatar_joy),
            timeAgo = "25m ago",
            image = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=800&q=80",
            caption = "Sunset vibes from the studio terrace 🌇",
            seen = false,
            isMyStory = true,
            viewCount = 148,
            likesCount = 42,
            musicTrack = "Odesza • A Moment Apart",
            locationTag = "San Francisco, CA",
            viewers = listOf(
                StoryViewerInfo(id = "v1", name = "Bholanath Kumbhakar", username = "@bholanath", avatar = drawableUri(com.chroma.studio.R.drawable.ic_avatar_corporate_navy), timeAgo = "4m ago", reactedEmoji = "🔥"),
                StoryViewerInfo(id = "v2", name = "Sarah Connor", username = "@sarah_c", avatar = drawableUri(com.chroma.studio.R.drawable.ic_avatar_deep_purple), timeAgo = "12m ago", reactedEmoji = "❤️"),
                StoryViewerInfo(id = "v3", name = "Marcus Vance", username = "@marcus_v", avatar = drawableUri(com.chroma.studio.R.drawable.ic_avatar_royal_blue), timeAgo = "18m ago", reactedEmoji = "👏"),
                StoryViewerInfo(id = "v4", name = "Elena Rostova", username = "@elena_ux", avatar = drawableUri(com.chroma.studio.R.drawable.ic_avatar_emerald_focus), timeAgo = "22m ago", reactedEmoji = "❤️")
            )
        ),
        Story(
            id = "s_my_2",
            authorName = "Your Story",
            avatar = drawableUri(com.chroma.studio.R.drawable.profile_avatar_joy),
            timeAgo = "10m ago",
            image = "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?auto=format&fit=crop&w=800&q=80",
            caption = "Espresso fuel for the weekend release! ☕💻",
            seen = false,
            isMyStory = true,
            viewCount = 86,
            likesCount = 29,
            musicTrack = "Chillhop • Morning Coffee"
        ),
        Story(
            id = "s1",
            authorName = "Bholanath Kumbhakar",
            avatar = drawableUri(com.chroma.studio.R.drawable.ic_avatar_corporate_navy),
            timeAgo = "15m ago",
            image = "https://images.unsplash.com/photo-1558002038-1055907df827?auto=format&fit=crop&w=800&q=80",
            caption = "Bench testing the new smart home circuit nodes! ⚡",
            seen = false,
            viewCount = 92,
            likesCount = 28,
            musicTrack = "Synthwave • Neon Nights"
        ),
        Story(
            id = "s2_1",
            authorName = "Sarah Connor",
            avatar = drawableUri(com.chroma.studio.R.drawable.ic_avatar_deep_purple),
            timeAgo = "1h ago",
            image = "https://images.unsplash.com/photo-1498050108023-c5249f4df085?auto=format&fit=crop&w=800&q=80",
            caption = "Design system components ready for sprint review 🎨",
            seen = false,
            viewCount = 210,
            likesCount = 64
        ),
        Story(
            id = "s2_2",
            authorName = "Sarah Connor",
            avatar = drawableUri(com.chroma.studio.R.drawable.ic_avatar_deep_purple),
            timeAgo = "40m ago",
            image = "https://images.unsplash.com/photo-1550745165-9bc0b252726f?auto=format&fit=crop&w=800&q=80",
            caption = "Dark mode color tokens exported for iOS 18 🌙",
            seen = false,
            viewCount = 175,
            likesCount = 52
        ),
        Story(
            id = "s3",
            authorName = "Dev Team Alpha",
            avatar = drawableUri(com.chroma.studio.R.drawable.ic_avatar_sapphire_night),
            timeAgo = "3h ago",
            image = "https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?auto=format&fit=crop&w=800&q=80",
            caption = "Release v3.2 deployment celebration! 🚀",
            seen = true,
            viewCount = 345,
            likesCount = 112
        )
    )

    fun initialChats(): List<ChatConversation> = listOf(
        ChatConversation(
            id = "chat_1",
            name = "Bholanath Kumbhakar",
            type = "direct",
            avatar = drawableUri(com.chroma.studio.R.drawable.ic_avatar_corporate_navy),
            banner = drawableUri(com.chroma.studio.R.drawable.banner_wave_banner_midnight_glow),
            online = true,
            statusText = "Online",
            phone = "+91 98765 43210",
            bio = "Building hardware & home automation systems.",
            unreadCount = 0,
            pinnedMessage = "Perfect. Thanks!",
            isMuted = false,
            isBlocked = false,
            isVerified = true,
            messages = listOf(
                ChatMessage(id = "m1", senderId = "user_bholanath", senderName = "Bholanath", text = "Hey! How are you doing?", time = "10:24 AM", status = "read"),
                ChatMessage(id = "m2", senderId = "user_me", senderName = "Joy", text = "I'm good! Just working on some new ideas. How about you?", time = "10:26 AM", status = "read"),
                ChatMessage(id = "m3", senderId = "user_bholanath", senderName = "Bholanath", text = "That's great! I'm also working on the home automation project with Jasica AI.", time = "10:28 AM", status = "read"),
                ChatMessage(id = "m4", senderId = "user_me", senderName = "Joy", text = "Nice! Can you share the latest update?", time = "10:30 AM", status = "read"),
                ChatMessage(id = "m5", senderId = "user_bholanath", senderName = "Bholanath", text = "Sure! Here are the hardware schematics and smart module captures:", time = "10:32 AM", status = "read"),
                ChatMessage(
                    id = "m5_p1",
                    senderId = "user_bholanath",
                    senderName = "Bholanath",
                    mediaType = "image",
                    mediaUrl = "https://images.unsplash.com/photo-1558002038-1055907df827?auto=format&fit=crop&w=800&q=80",
                    time = "10:32 AM",
                    status = "read"
                ),
                ChatMessage(
                    id = "m5_p2",
                    senderId = "user_bholanath",
                    senderName = "Bholanath",
                    mediaType = "image",
                    mediaUrl = "https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?auto=format&fit=crop&w=800&q=80",
                    time = "10:32 AM",
                    status = "read"
                ),
                ChatMessage(
                    id = "m5_p3",
                    senderId = "user_bholanath",
                    senderName = "Bholanath",
                    mediaType = "image",
                    mediaUrl = "https://images.unsplash.com/photo-1550745165-9bc0b252726f?auto=format&fit=crop&w=800&q=80",
                    time = "10:32 AM",
                    status = "read"
                ),
                ChatMessage(id = "m6", senderId = "user_me", senderName = "Joy", text = "Perfect. Thanks!", time = "10:33 AM", status = "read")
            )
        ),
        ChatConversation(
            id = "chat_2",
            name = "Sarah Connor",
            type = "direct",
            avatar = drawableUri(com.chroma.studio.R.drawable.ic_avatar_deep_purple),
            banner = drawableUri(com.chroma.studio.R.drawable.banner_wave_banner_coral_sunset),
            online = true,
            statusText = "Online",
            phone = "+1 (555) 234-5678",
            bio = "Lead UX Designer. Passionate about micro-interactions and accessibility.",
            unreadCount = 2,
            pinnedMessage = null,
            isMuted = false,
            isBlocked = false,
            isVerified = true,
            messages = listOf(
                ChatMessage(id = "m201", senderId = "user_sarah", senderName = "Sarah", text = "Hey Joy, did you review the updated design prototypes?", time = "09:15 AM", status = "read"),
                ChatMessage(id = "m202", senderId = "user_sarah", senderName = "Sarah", text = "The new optical orb voice recorder flow feels amazing!", time = "09:18 AM", status = "delivered", reactions = mapOf("❤️" to 1), userReaction = "❤️")
            )
        ),
        ChatConversation(
            id = "chat_3",
            name = "Dev Team Alpha",
            type = "group",
            avatar = drawableUri(com.chroma.studio.R.drawable.ic_avatar_sapphire_night),
            banner = drawableUri(com.chroma.studio.R.drawable.banner_wave_banner_electric_blue),
            online = true,
            statusText = "4 members online",
            phone = "Group Chat",
            bio = "Engineering core, sprint deployments, CI/CD pipelines and code reviews.",
            unreadCount = 0,
            pinnedMessage = "Release v3.2 code freeze at 5:00 PM EST.",
            isMuted = false,
            isBlocked = false,
            isVerified = true,
            members = listOf(
                GroupMember(name = "Joy (You)", role = "Lead Architect", avatar = drawableUri(com.chroma.studio.R.drawable.profile_avatar_joy)),
                GroupMember(name = "Bholanath Kumbhakar", role = "Hardware Specialist", avatar = drawableUri(com.chroma.studio.R.drawable.ic_avatar_corporate_navy)),
                GroupMember(name = "Sarah Connor", role = "UI/UX Designer", avatar = drawableUri(com.chroma.studio.R.drawable.ic_avatar_deep_purple)),
                GroupMember(name = "Marcus Vance", role = "Backend Lead", avatar = drawableUri(com.chroma.studio.R.drawable.ic_avatar_royal_blue))
            ),
            messages = listOf(
                ChatMessage(id = "m301", senderId = "user_marcus", senderName = "Marcus Vance", text = "All regression pipelines are green on production! 🚀", time = "08:40 AM", status = "read", reactions = mapOf("🚀" to 3)),
                ChatMessage(id = "m302", senderId = "user_bholanath", senderName = "Bholanath", text = "Firmware endpoints are synced with websocket clusters.", time = "08:45 AM", status = "read", reactions = mapOf("👍" to 2)),
                ChatMessage(
                    id = "m303",
                    senderId = "user_sarah",
                    senderName = "Sarah Connor",
                    mediaType = "image_stack",
                    mediaUrls = listOf(
                        "https://images.unsplash.com/photo-1558002038-1055907df827?auto=format&fit=crop&w=800&q=80",
                        "https://images.unsplash.com/photo-1498050108023-c5249f4df085?auto=format&fit=crop&w=800&q=80",
                        "https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?auto=format&fit=crop&w=800&q=80",
                        "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?auto=format&fit=crop&w=800&q=80",
                        "https://images.unsplash.com/photo-1550745165-9bc0b252726f?auto=format&fit=crop&w=800&q=80"
                    ),
                    text = "Here are the initial hardware test captures and board layout schematics 📸",
                    time = "08:50 AM",
                    status = "read",
                    reactions = mapOf("❤️" to 4)
                ),
                ChatMessage(
                    id = "m304",
                    senderId = "user_me",
                    senderName = "Joy (You)",
                    mediaType = "file_stack",
                    fileItems = listOf(
                        ChatFileItem(name = "Firmware_Release_v3.2.0.pdf", size = "2.4 MB", extension = "PDF"),
                        ChatFileItem(name = "Production_Gerber_Files.zip", size = "18.6 MB", extension = "ZIP"),
                        ChatFileItem(name = "Architecture_Specs.docx", size = "780 KB", extension = "DOCX")
                    ),
                    text = "Attached all release artifacts and KiCad deliverables for deployment.",
                    time = "08:52 AM",
                    status = "read"
                )
            )
        )
    )
}





