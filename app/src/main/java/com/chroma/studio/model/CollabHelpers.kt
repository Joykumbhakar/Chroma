package com.chroma.studio.model

const val MAX_COLLAB_PARTICIPANTS = 4

enum class CollabInviteStatus(val wireValue: String) {
    PENDING("pending"),
    ACCEPTED("accepted"),
    DECLINED("declined");

    companion object {
        fun fromWire(value: String?): CollabInviteStatus = when (value?.lowercase()) {
            "accepted" -> ACCEPTED
            "declined" -> DECLINED
            else -> PENDING
        }
    }
}

data class CollabStateUpdate(
    val collaborators: List<CollabParticipant>,
    val pendingCollaboratorIds: List<String>,
    val collaboratorIds: List<String>,
    val changed: Boolean
)

data class CollabHeaderAttribution(
    val primaryLabel: String,
    val connector: String?,
    val secondaryLabel: String?,
    val hasMultipleCollaborators: Boolean
)

/**
 * Normalizes invited collaborators: excludes the primary creator, deduplicates UIDs, and enforces [MAX_COLLAB_PARTICIPANTS].
 */
fun normalizeInvitedCollaborators(
    creatorUid: String,
    selected: List<CollabParticipant>,
    nowMs: Long = System.currentTimeMillis()
): List<CollabParticipant> {
    if (selected.isEmpty()) return emptyList()
    val seen = HashSet<String>()
    val normalized = ArrayList<CollabParticipant>(MAX_COLLAB_PARTICIPANTS)
    for (p in selected) {
        val uid = p.uid.trim()
        if (uid.isEmpty() || uid == creatorUid || !seen.add(uid)) continue
        normalized.add(
            p.copy(
                uid = uid,
                status = CollabInviteStatus.PENDING.wireValue,
                invitedAt = nowMs,
                respondedAt = null
            )
        )
        if (normalized.size >= MAX_COLLAB_PARTICIPANTS) break
    }
    return normalized
}

/**
 * Transitions a pending collaborator invite for [responderUid] to accepted or declined.
 */
fun applyCollabInviteResponse(
    participants: List<CollabParticipant>,
    pendingIds: List<String>,
    acceptedIds: List<String>,
    responderUid: String,
    accept: Boolean,
    nowMs: Long = System.currentTimeMillis()
): CollabStateUpdate {
    if (responderUid.isBlank()) {
        return CollabStateUpdate(participants, pendingIds, acceptedIds, changed = false)
    }
    var found = false
    val updatedParticipants = participants.map { p ->
        if (p.uid == responderUid) {
            found = true
            p.copy(
                status = if (accept) CollabInviteStatus.ACCEPTED.wireValue else CollabInviteStatus.DECLINED.wireValue,
                respondedAt = nowMs
            )
        } else {
            p
        }
    }
    val wasPending = pendingIds.contains(responderUid)
    val wasAccepted = acceptedIds.contains(responderUid)
    if (!found && !wasPending) {
        return CollabStateUpdate(participants, pendingIds, acceptedIds, changed = false)
    }

    val nextPending = pendingIds.filterNot { it == responderUid }.distinct()
    val nextAccepted = if (accept) {
        (acceptedIds + responderUid).distinct()
    } else {
        acceptedIds.filterNot { it == responderUid }.distinct()
    }
    val changed = found || wasPending || (accept != wasAccepted)
    return CollabStateUpdate(
        collaborators = updatedParticipants,
        pendingCollaboratorIds = nextPending,
        collaboratorIds = nextAccepted,
        changed = changed
    )
}

/**
 * Removes [targetUid] from collaborators (used for collaborator "Stop sharing" or creator removal).
 */
fun removeCollabParticipant(
    participants: List<CollabParticipant>,
    pendingIds: List<String>,
    acceptedIds: List<String>,
    targetUid: String
): CollabStateUpdate {
    if (targetUid.isBlank()) {
        return CollabStateUpdate(participants, pendingIds, acceptedIds, changed = false)
    }
    val nextParticipants = participants.filterNot { it.uid == targetUid }
    val nextPending = pendingIds.filterNot { it == targetUid }.distinct()
    val nextAccepted = acceptedIds.filterNot { it == targetUid }.distinct()
    val changed = nextParticipants.size != participants.size ||
        nextPending.size != pendingIds.size ||
        nextAccepted.size != acceptedIds.size
    return CollabStateUpdate(
        collaborators = nextParticipants,
        pendingCollaboratorIds = nextPending,
        collaboratorIds = nextAccepted,
        changed = changed
    )
}

/**
 * Returns accepted collaborators backed by both [participants] metadata and [acceptedIds] authoritativeness.
 */
fun resolveAcceptedCollaborators(
    participants: List<CollabParticipant>,
    acceptedIds: List<String>
): List<CollabParticipant> {
    if (acceptedIds.isEmpty()) return emptyList()
    val acceptedSet = acceptedIds.toSet()
    val byUid = participants.associateBy { it.uid }
    return acceptedSet.mapNotNull { uid ->
        val p = byUid[uid]
        when {
            p != null -> p.copy(status = CollabInviteStatus.ACCEPTED.wireValue)
            else -> null
        }
    }
}

/**
 * Builds Instagram-style single-line attribution (`alice`, `alice and bob`, or `alice and 2 others`).
 */
fun formatCollabHeaderAttribution(
    primaryLabel: String,
    acceptedLabels: List<String>
): CollabHeaderAttribution {
    val cleanPrimary = primaryLabel.ifBlank { "creator" }
    val cleanOthers = acceptedLabels.filter { it.isNotBlank() && it != cleanPrimary }.distinct()
    return when (cleanOthers.size) {
        0 -> CollabHeaderAttribution(
            primaryLabel = cleanPrimary,
            connector = null,
            secondaryLabel = null,
            hasMultipleCollaborators = false
        )
        1 -> CollabHeaderAttribution(
            primaryLabel = cleanPrimary,
            connector = " and ",
            secondaryLabel = cleanOthers.first(),
            hasMultipleCollaborators = false
        )
        else -> CollabHeaderAttribution(
            primaryLabel = cleanPrimary,
            connector = " and ",
            secondaryLabel = "${cleanOthers.size} others",
            hasMultipleCollaborators = true
        )
    }
}

/**
 * Merges creator-owned posts with accepted collab posts cleanly without duplicates.
 */
fun mergeCreatorAndCollabPosts(
    creatorPosts: List<DiscoverPost>,
    collabPosts: List<DiscoverPost>
): List<DiscoverPost> {
    if (collabPosts.isEmpty()) return creatorPosts
    val merged = LinkedHashMap<String, DiscoverPost>(creatorPosts.size + collabPosts.size)
    for (p in creatorPosts) merged[p.id] = p
    for (p in collabPosts) merged.putIfAbsent(p.id, p)
    return merged.values.sortedByDescending { it.timestamp }
}

/**
 * Merges creator-owned Flashes with accepted collab Flashes cleanly without duplicates.
 */
fun mergeCreatorAndCollabFlashes(
    creatorFlashes: List<FlashVideo>,
    collabFlashes: List<FlashVideo>
): List<FlashVideo> {
    if (collabFlashes.isEmpty()) return creatorFlashes
    val merged = LinkedHashMap<String, FlashVideo>(creatorFlashes.size + collabFlashes.size)
    for (f in creatorFlashes) merged[f.id] = f
    for (f in collabFlashes) merged.putIfAbsent(f.id, f)
    return merged.values.sortedByDescending { it.timestamp }
}
