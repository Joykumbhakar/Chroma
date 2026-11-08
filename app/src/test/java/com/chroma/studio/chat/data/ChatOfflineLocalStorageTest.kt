package com.chroma.studio.chat.data

import android.content.Context
import android.content.ContextWrapper
import com.chroma.studio.model.ChatMessage
import com.chroma.studio.model.ChatRoom
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Unit tests verifying production offline chat local storage and media caching architecture.
 * Guarantees zero "opening from zero" regressions, offline Airplane mode durability, and media URI safety.
 */
class ChatOfflineLocalStorageTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var mockFilesDir: File
    private lateinit var dummyContext: Context

    private class TestContext(private val dir: File) : ContextWrapper(null) {
        override fun getFilesDir(): File = dir
    }

    @Before
    fun setUp() {
        mockFilesDir = tempFolder.newFolder("mock_files_dir")
        dummyContext = TestContext(mockFilesDir)
        ChatLocalCacheManager.testCacheDir = mockFilesDir
        ChatLocalCacheManager.clearMemoryCache()
    }

    @After
    fun tearDown() {
        ChatLocalCacheManager.testCacheDir = null
        ChatLocalCacheManager.clearMemoryCache()
    }

    @Test
    fun testInstantL1MemoryCacheHit() {
        val roomId = "test_room_instant_l1"
        val sampleMsgs = listOf(
            ChatMessage(id = "msg_1", text = "Hello offline world", timestamp = 1000L),
            ChatMessage(id = "msg_2", text = "Second message", timestamp = 2000L)
        )

        // Save messages
        ChatLocalCacheManager.saveMessages(dummyContext, roomId, sampleMsgs)

        // L1 memory cache hit must be synchronous and immediate
        val memoryCached = ChatLocalCacheManager.getMemoryCachedMessages(roomId)
        assertNotNull("L1 memory cache must immediately return non-null", memoryCached)
        assertEquals(2, memoryCached!!.size)
        assertEquals("msg_2", memoryCached[0].id) // Sorted by timestamp descending
    }

    @Test
    fun testOfflineDiskL2DurabilityAcrossSessionClear() {
        val roomId = "test_room_l2_persistence"
        val sampleMsgs = listOf(
            ChatMessage(id = "msg_10", text = "Offline access verified", timestamp = 5000L)
        )

        ChatLocalCacheManager.saveMessages(dummyContext, roomId, sampleMsgs)

        // Give IO coroutine a moment to flush to disk
        Thread.sleep(300)

        // Simulate app restart / memory eviction
        ChatLocalCacheManager.clearMemoryCache()
        assertNull(ChatLocalCacheManager.getMemoryCachedMessages(roomId))

        // Loading via getCachedMessages must seamlessly read from disk L2 store
        val diskLoaded = ChatLocalCacheManager.getCachedMessages(dummyContext, roomId)
        assertEquals(1, diskLoaded.size)
        assertEquals("msg_10", diskLoaded[0].id)
        assertEquals("Offline access verified", diskLoaded[0].text)
    }

    @Test
    fun testRecentChatsOfflineStorage() {
        val sampleRooms = listOf(
            ChatRoom(id = "room_1", lastMessage = "Hey there", lastMessageTime = 3000L),
            ChatRoom(id = "room_2", lastMessage = "Photo sent", lastMessageTime = 4000L)
        )

        ChatLocalCacheManager.saveRecentChats(dummyContext, sampleRooms)
        Thread.sleep(300)

        // Verify synchronous retrieval
        val cached = ChatLocalCacheManager.getCachedRecentChats(dummyContext)
        assertEquals(2, cached.size)
        assertEquals("room_1", cached[0].id)

        // Verify across memory clear (simulating cold boot in Airplane Mode)
        ChatLocalCacheManager.clearMemoryCache()
        val reloaded = ChatLocalCacheManager.getCachedRecentChats(dummyContext)
        assertEquals(2, reloaded.size)
        assertEquals("room_2", reloaded[1].id)
    }

    @Test
    fun testMediaCacheFileNameGeneration() {
        val url = "https://example.com/storage/v1/chat_image_123.jpg?token=xyz"
        val fileName = ChatLocalCacheManager.urlToCacheFileName(url)

        assertTrue("File name must start with media_", fileName.startsWith("media_"))
        assertTrue("File name must preserve jpg extension", fileName.endsWith(".jpg"))

        // Deterministic check
        val fileNameAgain = ChatLocalCacheManager.urlToCacheFileName(url)
        assertEquals("File name must be deterministic", fileName, fileNameAgain)
    }

    @Test
    fun testOfflineSafeMediaUrlResolution() {
        val remoteUrl = "https://example.com/audio/voice_note_99.m4a"

        // Before caching: returns original remote URL
        val unCachedResolved = ChatLocalCacheManager.getOfflineSafeMediaUrl(dummyContext, remoteUrl)
        assertEquals(remoteUrl, unCachedResolved)

        // Manually place cached file in media dir to simulate completed download
        val mediaDir = ChatLocalCacheManager.getMediaCacheDir(dummyContext)
        val expectedFileName = ChatLocalCacheManager.urlToCacheFileName(remoteUrl)
        val cachedFile = File(mediaDir, expectedFileName)
        cachedFile.writeText("fake audio bytes")

        // After caching: must return file:/// URI for 100% offline access
        val cachedResolved = ChatLocalCacheManager.getOfflineSafeMediaUrl(dummyContext, remoteUrl)
        assertTrue(cachedResolved.startsWith("file://"))
        assertTrue(cachedResolved.endsWith(expectedFileName))

        val resolvedFile = ChatLocalCacheManager.getCachedMediaFile(dummyContext, remoteUrl)
        assertNotNull(resolvedFile)
        assertTrue(resolvedFile!!.exists())
    }
}
