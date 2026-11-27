package com.chroma.studio.chat

import com.chroma.studio.ChatInAppEvent
import com.chroma.studio.chat.data.ChatAdapters
import com.chroma.studio.chat.data.FcmPayloadClassifier
import com.chroma.studio.chat.data.FcmPushKind
import com.chroma.studio.chat.data.InAppEventDeduplicator
import com.chroma.studio.chat.data.MessageDeliveryStatus
import com.chroma.studio.chat.data.MessageStatusMachine
import com.chroma.studio.chat.data.OutboxRoomStatus
import com.chroma.studio.model.ChatMessage
import com.chroma.studio.model.ChatRoom
import com.chroma.studio.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageStatusMachineAndNotificationDedupTest {

    @Test
    fun `online user without delivery receipt stays SENT, never DELIVERED or READ`() {
        val myUid = "user_a"
        val otherUid = "user_b"
        val msg = ChatMessage(
            id = "m1",
            senderId = myUid,
            text = "Hello",
            timestamp = 1000L,
            isPending = false,
            isDelivered = false,
            isRead = false
        )

        val display = ChatAdapters.toDisplayMessage(
            msg = msg,
            currentUid = myUid,
            userProfiles = emptyMap(),
            isOtherOnline = true,
            groupParticipants = listOf(myUid, otherUid)
        )

        assertEquals("sent", display.status)
    }

    @Test
    fun `room preview never marks message READ solely from unreadCount zero or DELIVERED from online`() {
        val myUid = "user_a"
        val otherUid = "user_b"
        val roomJustSent = ChatRoom(
            id = "user_a_user_b",
            isGroup = false,
            participants = listOf(myUid, otherUid),
            lastMessage = "Hey",
            lastSender = myUid,
            lastMessageTime = 2000L,
            lastMessageId = "m2",
            lastMessageIsRead = false,
            lastMessageIsDelivered = false,
            unreadCount = mapOf(myUid to 0, otherUid to 0) // Simulate race/zero unread on other side
        )

        val conv = ChatAdapters.toChatConversation(
            room = roomJustSent,
            currentUid = myUid,
            userProfiles = mapOf(otherUid to UserProfile(uid = otherUid, online = true)),
            isOnline = true
        )

        assertEquals("sent", conv.messages.last().status)

        val deliveredRoom = roomJustSent.copy(lastMessageIsDelivered = true)
        val deliveredConv = ChatAdapters.toChatConversation(
            room = deliveredRoom,
            currentUid = myUid,
            userProfiles = emptyMap(),
            isOnline = false
        )
        assertEquals("delivered", deliveredConv.messages.last().status)

        val readRoom = deliveredRoom.copy(lastMessageIsRead = true)
        val readConv = ChatAdapters.toChatConversation(
            room = readRoom,
            currentUid = myUid,
            userProfiles = emptyMap(),
            isOnline = false
        )
        assertEquals("read", readConv.messages.last().status)
    }

    @Test
    fun `room preview respects outbox pending and error states for current lastMessageId`() {
        val myUid = "user_a"
        val room = ChatRoom(
            id = "user_a_user_b",
            participants = listOf(myUid, "user_b"),
            lastMessage = "Sending...",
            lastSender = myUid,
            lastMessageTime = 3000L,
            lastMessageId = "m_outbox",
            lastMessageIsDelivered = false,
            lastMessageIsRead = false
        )

        val pendingStatus = MessageStatusMachine.resolveRoomPreviewStatus(
            room = room,
            currentUid = myUid,
            localOutboxStatus = OutboxRoomStatus("m_outbox", MessageDeliveryStatus.PENDING)
        )
        assertEquals(MessageDeliveryStatus.PENDING, pendingStatus)

        val failedStatus = MessageStatusMachine.resolveRoomPreviewStatus(
            room = room,
            currentUid = myUid,
            localOutboxStatus = OutboxRoomStatus("m_outbox", MessageDeliveryStatus.ERROR, "Upload failed")
        )
        assertEquals(MessageDeliveryStatus.ERROR, failedStatus)
    }

    @Test
    fun `monotonic merge prevents backward regression from READ or DELIVERED to SENT or PENDING`() {
        val myUid = "user_a"
        val deliveredMsg = ChatMessage(
            id = "m_mono",
            senderId = myUid,
            text = "Hello",
            timestamp = 1000L,
            isPending = false,
            isDelivered = true,
            deliveredTo = mapOf("user_b" to 1100L),
            isRead = false
        )
        val stalePendingRegress = ChatMessage(
            id = "m_mono",
            senderId = myUid,
            text = "Hello",
            timestamp = 1000L,
            isPending = true,
            isDelivered = false,
            deliveredTo = emptyMap(),
            isRead = false
        )

        val mergedAfterDeliver = MessageStatusMachine.mergeMonotonic(
            previous = deliveredMsg,
            incoming = stalePendingRegress,
            currentUid = myUid
        )
        assertFalse(mergedAfterDeliver.isPending)
        assertTrue(mergedAfterDeliver.isDelivered)
        assertEquals(MessageDeliveryStatus.DELIVERED, MessageStatusMachine.resolveMessageStatus(mergedAfterDeliver, myUid))

        val readMsg = deliveredMsg.copy(isRead = true, readBy = mapOf("user_b" to 1200L))
        val mergedAfterRead = MessageStatusMachine.mergeMonotonic(
            previous = readMsg,
            incoming = deliveredMsg,
            currentUid = myUid
        )
        assertEquals(MessageDeliveryStatus.READ, MessageStatusMachine.resolveMessageStatus(mergedAfterRead, myUid))
    }

    @Test
    fun `group message stays DELIVERED until every other member reads it`() {
        val participants = listOf("alice", "bob", "charlie")
        val partialRead = ChatMessage(
            id = "g1",
            senderId = "alice",
            text = "Group hi",
            deliveredTo = mapOf("bob" to 100L),
            readBy = mapOf("bob" to 200L),
            isDelivered = true,
            isRead = true // Simulate partial legacy flag; group rules still require all other members
        )

        assertEquals(
            MessageDeliveryStatus.DELIVERED,
            MessageStatusMachine.resolveMessageStatus(partialRead, currentUid = "alice", groupParticipants = participants)
        )

        val allRead = partialRead.copy(readBy = mapOf("bob" to 200L, "charlie" to 300L))
        assertEquals(
            MessageDeliveryStatus.READ,
            MessageStatusMachine.resolveMessageStatus(allRead, currentUid = "alice", groupParticipants = participants)
        )
    }

    @Test
    fun `dynamic island deduplicator drops repeated notifications for same messageId across FCM and Firestore`() {
        val deduplicator = InAppEventDeduplicator(ttlMs = 120_000L)
        val eventFromFirestore = ChatInAppEvent(
            roomId = "room_dedup_1",
            senderId = "sender_1",
            senderName = "Alex",
            messageText = "Sent an image",
            timestamp = 10_000L,
            messageId = "msg_unique_999"
        )
        val eventFromFcm = eventFromFirestore.copy(messageText = "📷 Sent a photo")

        assertEquals(eventFromFirestore.canonicalKey(), eventFromFcm.canonicalKey())
        assertTrue(deduplicator.shouldProcess(eventFromFirestore.canonicalKey(), nowMs = 50_000L))
        assertFalse(deduplicator.shouldProcess(eventFromFcm.canonicalKey(), nowMs = 50_150L))
        assertFalse(deduplicator.shouldProcess(eventFromFirestore.canonicalKey(), nowMs = 55_000L))

        val secondDistinctMsg = eventFromFirestore.copy(messageId = "msg_unique_1000", messageText = "Next!")
        assertNotEquals(eventFromFirestore.canonicalKey(), secondDistinctMsg.canonicalKey())
        assertTrue(deduplicator.shouldProcess(secondDistinctMsg.canonicalKey(), nowMs = 55_100L))

        // After TTL expires, an explicit redelivery beyond 120s is allowed
        assertTrue(deduplicator.shouldProcess(eventFromFirestore.canonicalKey(), nowMs = 175_000L))
    }

    @Test
    fun `fcm payload classifier separates likes follows comments from chat messages`() {
        assertEquals(
            FcmPushKind.SOCIAL_ACTIVITY,
            FcmPayloadClassifier.classify(
                mapOf(
                    "notificationType" to "LIKE",
                    "roomId" to "post_123",
                    "messageType" to "text"
                )
            )
        )
        assertEquals(
            FcmPushKind.SOCIAL_ACTIVITY,
            FcmPayloadClassifier.classify(
                mapOf(
                    "notificationType" to "FOLLOW",
                    "targetId" to "uid_target"
                )
            )
        )
        // Legacy social push (before notificationType was added): random postId had no '_' or 'group_'
        assertEquals(
            FcmPushKind.SOCIAL_ACTIVITY,
            FcmPayloadClassifier.classify(
                mapOf(
                    "roomId" to "7f9a8c2e-post-id",
                    "senderName" to "Alex (@alex)",
                    "body" to "liked your post"
                )
            )
        )
        assertEquals(
            FcmPushKind.CHAT_MESSAGE,
            FcmPayloadClassifier.classify(
                mapOf(
                    "notificationType" to "MESSAGE",
                    "roomId" to "uidA_uidB",
                    "messageId" to "msg_1"
                )
            )
        )
        assertEquals(
            FcmPushKind.CHAT_MESSAGE,
            FcmPayloadClassifier.classify(
                mapOf(
                    "roomId" to "group_999",
                    "senderName" to "Design Team"
                )
            )
        )
    }
}
