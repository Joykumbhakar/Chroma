package com.chroma.studio.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicRepositoryTest {

    @Test
    fun moodCollectionFromRawMatchesIdsAndTitlesCaseInsensitively() {
        assertEquals(MusicMoodCollection.MORNING_COFFEE, MusicMoodCollection.fromRaw("morning_coffee"))
        assertEquals(MusicMoodCollection.GOLDEN_HOUR, MusicMoodCollection.fromRaw("Golden Hour"))
        assertEquals(MusicMoodCollection.WEEKEND_DRIVE, MusicMoodCollection.fromRaw("  WEEKEND_DRIVE  "))
        assertEquals(MusicMoodCollection.CITY_NIGHTS, MusicMoodCollection.fromRaw("unknown_mood"))
    }

    @Test
    fun catalogTrackToAttachedSelectionClampsBestPartSafely() {
        val track = CatalogMusicTrack(
            id = "track_123",
            title = "Neon Skyline",
            artist = "Aria",
            album = "Midnight LP",
            coverUrl = "https://res.cloudinary.com/demo/image/upload/cover.jpg",
            audioUrl = "https://res.cloudinary.com/o9evpcft/video/upload/song.mp3",
            durationSec = 180,
            bestPartStartSec = 90,
            bestPartEndSec = 110
        )

        val selection = track.toAttachedSelection()
        assertEquals("track_123", selection.trackId)
        assertEquals(90, selection.startSec)
        assertEquals(20, selection.durationSec)
        assertEquals(180, selection.totalTrackSec)
        assertEquals("https://res.cloudinary.com/o9evpcft/video/upload/song.mp3", selection.audioUrl)
    }

    @Test
    fun catalogTrackToAttachedSelectionEnforcesMinimumClipLength() {
        val shortTrack = CatalogMusicTrack(
            id = "track_short",
            title = "Bumper",
            artist = "Chroma",
            durationSec = 12,
            bestPartStartSec = 10,
            bestPartEndSec = 11
        )
        val selection = shortTrack.toAttachedSelection()
        assertEquals(5, selection.durationSec)
    }

    @Test
    fun pendingMusicHandoffPeekAndConsumeWorkAtomically() {
        PendingMusicHandoff.set(null)
        assertNull(PendingMusicHandoff.peek())

        val selection = AttachedMusicSelection(
            trackId = "t_abc",
            title = "Sunbeam",
            artist = "Kora",
            album = "Prism",
            coverUrl = "",
            audioUrl = "https://example.com/a.mp3",
            startSec = 15,
            durationSec = 20,
            totalTrackSec = 95
        )
        PendingMusicHandoff.set(selection)
        assertEquals("t_abc", PendingMusicHandoff.peek()?.trackId)
        val consumed = PendingMusicHandoff.consume()
        assertTrue(consumed != null && consumed.title == "Sunbeam")
        assertNull(PendingMusicHandoff.peek())
    }
}
