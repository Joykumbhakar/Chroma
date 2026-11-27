package com.chroma.studio.data

import android.content.Context
import android.content.ContextWrapper
import com.chroma.studio.chat.data.ChatAdapters
import com.chroma.studio.model.ChatMessage
import com.chroma.studio.model.ChatRoom
import com.chroma.studio.model.DiscoverPost
import com.chroma.studio.model.TrendingConfig
import com.chroma.studio.model.UserProfile
import com.chroma.studio.model.UserStoryGroup
import com.chroma.studio.ui.components.VerifiedBadgeRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class OfflineAndCrashFixVerificationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private class DiskTestContext(private val dir: File) : ContextWrapper(null) {
        override fun getFilesDir(): File = dir
        override fun getApplicationContext(): Context = this
    }

    @Test
    fun testChatRoomSupportsBothWebArrayPinnedByAndLegacyStringPinnedBy() {
        val webPinnedRoom = ChatRoom(
            id = "room_web",
            participants = listOf("u1", "u2"),
            pinnedBy = listOf("u1", "u2"),
            pinnedMessageBy = "u1"
        )
        assertTrue(webPinnedRoom.pinnedBy is List<*>)
        assertEquals("u1", webPinnedRoom.pinnedMessageBy)

        val legacyRoom = ChatRoom(
            id = "room_legacy",
            participants = listOf("u1", "u2"),
            pinnedBy = "u1"
        )
        assertEquals("u1", legacyRoom.pinnedBy)
    }

    @Test
    fun testGroupMessagePerUserReceiptsAndFontStyleMapping() {
        val u1 = "user_1"
        val u2 = "user_2"
        val u3 = "user_3"
        val group = listOf(u1, u2, u3)

        val profiles = mapOf(
            u1 to UserProfile(uid = u1, displayName = "Me", verified = true, verifiedTone = "gold"),
            u2 to UserProfile(uid = u2, displayName = "Alice", verified = true, verifiedTone = "chroma"),
            u3 to UserProfile(uid = u3, displayName = "Bob")
        )

        // Only u2 delivered so far -> group status must be "delivered"
        val deliveredMsg = ChatMessage(
            id = "m1",
            senderId = u1,
            text = "**Hello** group",
            timestamp = 10000L,
            deliveredTo = mapOf(u2 to 10010L),
            readBy = emptyMap(),
            fontStyle = "serif"
        )
        val uiDelivered = ChatAdapters.toDisplayMessage(
            msg = deliveredMsg,
            currentUid = u1,
            userProfiles = profiles,
            groupParticipants = group
        )
        assertEquals("delivered", uiDelivered.status)
        assertEquals("serif", uiDelivered.fontStyle)
        assertEquals(1, uiDelivered.deliveredTo.size)

        // u2 and u3 both read -> group status must become "read"
        val allReadMsg = deliveredMsg.copy(
            deliveredTo = mapOf(u2 to 10010L, u3 to 10020L),
            readBy = mapOf(u2 to 10030L, u3 to 10040L)
        )
        val uiAllRead = ChatAdapters.toDisplayMessage(
            msg = allReadMsg,
            currentUid = u1,
            userProfiles = profiles,
            groupParticipants = group
        )
        assertEquals("read", uiAllRead.status)
        assertEquals(2, uiAllRead.readBy.size)
    }

    @Test
    fun testVerifiedBadgeRegistryAndFontStyleResolution() {
        VerifiedBadgeRegistry.register("uid_gold", "gold")
        VerifiedBadgeRegistry.register("uid_chroma", "chroma")

        assertEquals("gold", VerifiedBadgeRegistry.resolveTone("uid_gold", null))
        assertEquals("chroma", VerifiedBadgeRegistry.resolveTone("uid_chroma", ""))
        assertEquals("ruby", VerifiedBadgeRegistry.resolveTone("uid_gold", "ruby"))

        assertEquals(ChatFontStyle.SERIF, ChatFontStyle.fromKey("serif"))
        assertEquals(ChatFontStyle.MONO, ChatFontStyle.fromKey("mono"))
        assertEquals(ChatFontStyle.SIGNATURE, ChatFontStyle.fromKey("signature"))
        assertEquals(ChatFontStyle.DEFAULT, ChatFontStyle.fromKey("unknown"))
    }

    @Test
    fun testOnlineStatusPrivacyHidesLastSeen() {
        val hiddenProfile = UserProfile(
            uid = "uid_private",
            displayName = "Hidden",
            showOnlineStatus = false,
            lastSeen = 1700000000000L,
            online = true
        )
        OnlineStatusManager.seedProfilePresence(hiddenProfile)
        val cachedHidden = OnlineStatusManager.getUserPresence("uid_private")
        assertFalse(cachedHidden.online)
        assertEquals(null, cachedHidden.lastOnline)

        val visibleProfile = UserProfile(
            uid = "uid_public",
            displayName = "Public",
            showOnlineStatus = true,
            lastSeen = 1700000000000L,
            online = false
        )
        OnlineStatusManager.seedProfilePresence(visibleProfile)
        val cachedPublic = OnlineStatusManager.getUserPresence("uid_public")
        assertFalse(cachedPublic.online)
        assertEquals(1700000000000L, cachedPublic.lastOnline)
    }

    @Test
    fun testOfflineDataCacheManagerPersistsPostsProfilesAndStories() {
        val rootDir = tempFolder.newFolder("offline_cache_test")
        val ctx = DiskTestContext(rootDir)

        val posts = listOf(
            DiscoverPost(
                id = "post_1",
                title = "Text note offline",
                description = "Detailed post body",
                creatorId = "u_creator",
                authorVerified = true,
                authorVerifiedTone = "emerald",
                likesCount = 12
            )
        )
        OfflineDataCacheManager.saveRecentPosts(ctx, posts)
        OfflineDataCacheManager.saveTrendingPosts(ctx, posts)
        OfflineDataCacheManager.saveTrendingConfig(ctx, TrendingConfig(mode = "curated_first", maxSpotlight = 8))

        val loadedPosts = OfflineDataCacheManager.getRecentPosts(ctx)
        assertEquals(1, loadedPosts.size)
        assertEquals("emerald", loadedPosts[0].authorVerifiedTone)
        assertEquals("emerald", VerifiedBadgeRegistry.resolveTone("u_creator", null))

        val loadedCfg = OfflineDataCacheManager.getTrendingConfig(ctx)
        assertNotNull(loadedCfg)
        assertEquals(8, loadedCfg!!.maxSpotlight)

        val prof = UserProfile(uid = "u_1", displayName = "Creator", verified = true, verifiedTone = "gold")
        OfflineDataCacheManager.saveUserProfile(ctx, prof)
        assertEquals("gold", OfflineDataCacheManager.getUserProfile(ctx, "u_1")?.verifiedTone)

        val stories = listOf(UserStoryGroup(userId = "u_1", userProfile = prof))
        OfflineDataCacheManager.saveStoryGroups(ctx, "viewer_1", stories)
        assertEquals(1, OfflineDataCacheManager.getStoryGroups(ctx, "viewer_1").size)
    }

    @Test
    fun testUnlimitedHistoryPagedLoadPushPrewriteAndPlaintextPersistence() {
        val rootDir = tempFolder.newFolder("chat_sqlite_paged_test")
        val ctx = DiskTestContext(rootDir)
        com.chroma.studio.chat.data.ChatLocalCacheManager.testCacheDir = rootDir
        try {
            val uidA = "uid_alpha"
            val uidB = "uid_beta"
            val roomId = "room_alpha_beta"

            val msgs = (1..250).map { idx ->
                ChatMessage(
                    id = "msg_$idx",
                    senderId = if (idx % 2 == 0) uidA else uidB,
                    text = "Message #$idx",
                    timestamp = 1_000_000L + idx
                )
            }
            com.chroma.studio.chat.data.ChatLocalCacheManager.saveMessages(ctx, roomId, msgs, uidA)

            // Frame-0 viewport load (40 newest) + older paging beyond old 200 limit
            com.chroma.studio.chat.data.ChatLocalCacheManager.clearMemoryCache()
            val page1 = com.chroma.studio.chat.data.ChatLocalCacheManager.getCachedMessages(ctx, roomId, uidA, limit = 40)
            assertEquals(40, page1.size)
            assertEquals("msg_250", page1.first().id)
            assertEquals("msg_211", page1.last().id)

            val page2 = com.chroma.studio.chat.data.ChatLocalCacheManager.getOlderCachedMessages(
                ctx,
                roomId = roomId,
                beforeTimestamp = page1.last().timestamp,
                limit = 40,
                userId = uidA
            )
            assertEquals(40, page2.size)
            assertEquals("msg_210", page2.first().id)
            assertEquals("msg_171", page2.last().id)

            // Deep history past index 200 still preserved
            val deepPage = com.chroma.studio.chat.data.ChatLocalCacheManager.getOlderCachedMessages(
                ctx,
                roomId = roomId,
                beforeTimestamp = 1_000_030L,
                limit = 20,
                userId = uidA
            )
            assertEquals(20, deepPage.size)
            assertEquals("msg_29", deepPage.first().id)

            // Push room preview update moves room to top and increments unread for uidA
            val roomOlder = ChatRoom(id = "room_old", participants = listOf(uidA, "u3"), lastMessage = "Old", lastMessageTime = 2000L)
            val roomTarget = ChatRoom(id = roomId, participants = listOf(uidA, uidB), lastMessage = "Prev", lastMessageTime = 1000L)
            com.chroma.studio.chat.data.ChatLocalCacheManager.saveRecentChats(ctx, listOf(roomOlder, roomTarget), uidA)
            com.chroma.studio.chat.data.ChatLocalCacheManager.updateRoomOnIncomingPush(
                context = ctx,
                roomId = roomId,
                senderId = uidB,
                lastMessagePreview = "New push preview",
                timestamp = 5000L,
                messageId = "msg_push_1",
                incrementUnread = true,
                userId = uidA
            )
            val updatedRooms = com.chroma.studio.chat.data.ChatLocalCacheManager.getCachedRecentChats(ctx, uidA)
            assertEquals(roomId, updatedRooms.first().id)
            assertEquals("New push preview", updatedRooms.first().lastMessage)
            assertEquals(1, updatedRooms.first().unreadCount[uidA])

            // Persisted E2EE plaintext cache survives memory clear, isolates per user, and wipes on logout
            com.chroma.studio.chat.data.ChatLocalCacheManager.savePersistedPlaintext(ctx, "k1", "Secret hello", uidA)
            com.chroma.studio.chat.data.ChatLocalCacheManager.clearMemoryCache()
            assertEquals("Secret hello", com.chroma.studio.chat.data.ChatLocalCacheManager.getPersistedPlaintext(ctx, "k1", uidA))
            assertEquals(null, com.chroma.studio.chat.data.ChatLocalCacheManager.getPersistedPlaintext(ctx, "k1", uidB))

            com.chroma.studio.chat.data.ChatLocalCacheManager.clearAllOnLogout(ctx)
            assertEquals(null, com.chroma.studio.chat.data.ChatLocalCacheManager.getPersistedPlaintext(ctx, "k1", uidA))
            assertEquals(0, com.chroma.studio.chat.data.ChatLocalCacheManager.getCachedRecentChats(ctx, uidA).size)
        } finally {
            com.chroma.studio.chat.data.ChatLocalCacheManager.clearAllOnLogout(ctx)
            com.chroma.studio.chat.data.ChatLocalCacheManager.testCacheDir = null
        }
    }

    @Test
    fun testFcpVideoEditorParityMathAndSfxParsing() {
        // 1. Preview ColorMatrix exposure offset aligns linearly with FFmpeg eq brightness [-1, 1]
        assertEquals(0f, com.chroma.studio.utils.FcpStudioAudioSynth.exposureOffset255(0f), 0.001f)
        assertEquals(0f, com.chroma.studio.utils.FcpStudioAudioSynth.ffmpegExposureBrightness(0f), 0.001f)
        val exp = 30f
        val expectedOffset = exp * 1.4f
        assertEquals(expectedOffset, com.chroma.studio.utils.FcpStudioAudioSynth.exposureOffset255(exp), 0.001f)
        assertEquals(
            expectedOffset / 255f,
            com.chroma.studio.utils.FcpStudioAudioSynth.ffmpegExposureBrightness(exp),
            0.0001f
        )

        // 2. Preview-to-export text overlay scale uses actual viewport dp width instead of hardcoded 360dp
        // E.g. 9:16 preview viewport width = 540px on density 3f -> 180dp, export width = 1080px -> scale = 6f
        assertEquals(
            6f,
            com.chroma.studio.utils.FcpStudioAudioSynth.computeExportTextScale(
                exportWidthPx = 1080,
                previewWidthPx = 540,
                displayDensity = 3f
            ),
            0.01f
        )
        // Fallback when previewWidthPx is unknown (0) -> 1080 / 360 = 3f
        assertEquals(
            3f,
            com.chroma.studio.utils.FcpStudioAudioSynth.computeExportTextScale(
                exportWidthPx = 1080,
                previewWidthPx = 0,
                displayDensity = 3f
            ),
            0.01f
        )

        // 3. SFX preset duration string parsing
        assertEquals(0.8f, com.chroma.studio.utils.FcpStudioAudioSynth.parseSfxDurationSec("0.8s"), 0.001f)
        assertEquals(1.6f, com.chroma.studio.utils.FcpStudioAudioSynth.parseSfxDurationSec("1.6s"), 0.001f)
        assertEquals(0.8f, com.chroma.studio.utils.FcpStudioAudioSynth.parseSfxDurationSec("bad"), 0.001f)
    }
}
