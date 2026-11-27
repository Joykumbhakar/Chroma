package com.chroma.studio.chat

import com.chroma.studio.ui.components.DynamicIslandEvent
import com.chroma.studio.ui.components.DynamicIslandQueueCoordinator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DynamicIslandQueueCoordinatorTest {

    private fun status(title: String) = DynamicIslandEvent.Status(title = title)

    private fun chat(roomId: String, msgId: String, text: String = "Hi") =
        DynamicIslandEvent.ChatMessage(
            roomId = roomId,
            messageId = msgId,
            senderName = "Alex",
            messageText = text
        )

    @Test
    fun `urgent chat message preempts idle status toast immediately`() {
        val coordinator = DynamicIslandQueueCoordinator()
        coordinator.enqueue(status("Copied"))
        assertTrue(coordinator.currentPresentation.value?.event is DynamicIslandEvent.Status)

        val chatEvent = chat("r1", "m1", "Hello!")
        coordinator.enqueue(chatEvent)

        assertEquals(chatEvent, coordinator.currentPresentation.value?.event)
        assertEquals(0, coordinator.pendingCount())
    }

    @Test
    fun `user interacting protects expanded reply from incoming notifications and coalesces status toasts`() {
        val coordinator = DynamicIslandQueueCoordinator()
        val firstChat = coordinator.enqueue(chat("r1", "m1", "Reply me"))!!
        coordinator.setUserInteracting(true)

        val secondChat = chat("r2", "m2", "Wait for me")
        coordinator.enqueue(secondChat)
        coordinator.enqueue(status("Saved 1"))
        coordinator.enqueue(status("Saved 2"))

        // Active expanded item stays visible uninterrupted
        assertEquals(firstChat.instanceId, coordinator.currentPresentation.value?.instanceId)
        // Queue holds secondChat + single coalesced Status("Saved 2")
        assertEquals(2, coordinator.pendingCount())

        // Dismissing current promotes secondChat
        coordinator.dismiss(firstChat.instanceId)
        assertEquals(secondChat, coordinator.currentPresentation.value?.event)

        // Dismissing secondChat promotes the coalesced Status("Saved 2")
        coordinator.dismiss()
        assertEquals("Saved 2", (coordinator.currentPresentation.value?.event as? DynamicIslandEvent.Status)?.title)
    }

    @Test
    fun `stale dismiss with outdated instanceId never kills newly active notification`() {
        val coordinator = DynamicIslandQueueCoordinator()
        val first = coordinator.enqueue(status("First"))!!
        val second = coordinator.enqueue(status("Second"))!!

        // Stale timer from "First" arrives late after "Second" took over
        coordinator.dismiss(expectedInstanceId = first.instanceId)
        assertEquals(second.instanceId, coordinator.currentPresentation.value?.instanceId)

        // Matching dismiss clears "Second" cleanly
        coordinator.dismiss(expectedInstanceId = second.instanceId)
        assertNull(coordinator.currentPresentation.value)
    }

    @Test
    fun `suppression clears queue and blocks new presentations cleanly`() {
        val coordinator = DynamicIslandQueueCoordinator()
        coordinator.enqueue(chat("r1", "m1"))
        coordinator.setSuppressed(true)

        assertNull(coordinator.currentPresentation.value)
        assertEquals(0, coordinator.pendingCount())
        assertNull(coordinator.enqueue(chat("r1", "m2")))

        coordinator.setSuppressed(false)
        assertEquals("m2", (coordinator.enqueue(chat("r1", "m2"))?.event as? DynamicIslandEvent.ChatMessage)?.messageId)
    }
}
