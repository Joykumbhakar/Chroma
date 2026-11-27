package com.chroma.studio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CollabHelpersTest {

    @Test
    fun `normalizeInvitedCollaborators removes self, duplicates, and caps at 4`() {
        val raw = listOf(
            CollabParticipant(uid = "creator", username = "creator"),
            CollabParticipant(uid = "u1", username = "alice"),
            CollabParticipant(uid = "u1", username = "alice"),
            CollabParticipant(uid = "u2", username = "bob"),
            CollabParticipant(uid = "u3", username = "carol"),
            CollabParticipant(uid = "u4", username = "dave"),
            CollabParticipant(uid = "u5", username = "eve")
        )
        val normalized = normalizeInvitedCollaborators(creatorUid = "creator", selected = raw, nowMs = 100L)
        assertEquals(listOf("u1", "u2", "u3", "u4"), normalized.map { it.uid })
        assertTrue(normalized.all { it.status == "pending" && it.invitedAt == 100L })
    }

    @Test
    fun `applyCollabInviteResponse accepts and declines cleanly`() {
        val invited = listOf(
            CollabParticipant(uid = "u1", username = "alice", status = "pending"),
            CollabParticipant(uid = "u2", username = "bob", status = "pending")
        )
        val acceptedState = applyCollabInviteResponse(
            participants = invited,
            pendingIds = listOf("u1", "u2"),
            acceptedIds = emptyList(),
            responderUid = "u1",
            accept = true,
            nowMs = 500L
        )
        assertTrue(acceptedState.changed)
        assertEquals(listOf("u2"), acceptedState.pendingCollaboratorIds)
        assertEquals(listOf("u1"), acceptedState.collaboratorIds)
        assertEquals("accepted", acceptedState.collaborators.first { it.uid == "u1" }.status)

        val declinedState = applyCollabInviteResponse(
            participants = acceptedState.collaborators,
            pendingIds = acceptedState.pendingCollaboratorIds,
            acceptedIds = acceptedState.collaboratorIds,
            responderUid = "u2",
            accept = false,
            nowMs = 600L
        )
        assertTrue(declinedState.changed)
        assertTrue(declinedState.pendingCollaboratorIds.isEmpty())
        assertEquals(listOf("u1"), declinedState.collaboratorIds)
        assertEquals("declined", declinedState.collaborators.first { it.uid == "u2" }.status)
    }

    @Test
    fun `removeCollabParticipant drops target cleanly for Stop sharing`() {
        val participants = listOf(
            CollabParticipant(uid = "u1", username = "alice", status = "accepted"),
            CollabParticipant(uid = "u2", username = "bob", status = "pending")
        )
        val removed = removeCollabParticipant(
            participants = participants,
            pendingIds = listOf("u2"),
            acceptedIds = listOf("u1"),
            targetUid = "u1"
        )
        assertTrue(removed.changed)
        assertTrue(removed.collaboratorIds.isEmpty())
        assertEquals(listOf("u2"), removed.collaborators.map { it.uid })
        assertFalse(removeCollabParticipant(participants, listOf("u2"), listOf("u1"), "unknown").changed)
    }

    @Test
    fun `formatCollabHeaderAttribution formats solo, dual, and multi collaborator headers`() {
        val solo = formatCollabHeaderAttribution("alice", emptyList())
        assertEquals("alice", solo.primaryLabel)
        assertNull(solo.connector)

        val dual = formatCollabHeaderAttribution("alice", listOf("bob"))
        assertEquals("alice", dual.primaryLabel)
        assertEquals(" and ", dual.connector)
        assertEquals("bob", dual.secondaryLabel)
        assertFalse(dual.hasMultipleCollaborators)

        val multi = formatCollabHeaderAttribution("alice", listOf("bob", "carol"))
        assertEquals("2 others", multi.secondaryLabel)
        assertTrue(multi.hasMultipleCollaborators)
    }
}
