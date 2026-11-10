package com.termtune.player.terminal

import com.termtune.player.data.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalFormatTest {

    private fun song(
        id: Long,
        title: String,
        artist: String = "unknown artist",
        album: String = "unknown album",
        durationMs: Long = 180_000,
    ) = Song(id, title, artist, album, durationMs, "content://media/external/audio/media/$id")

    private val library = listOf(
        song(1, "Bohemian Rhapsody", "Queen", "A Night at the Opera", 355_000),
        song(2, "Money", "Pink Floyd", "The Dark Side of the Moon", 382_000),
        song(3, "Take Five", "Dave Brubeck", "Time Out", 324_000),
    )

    @Test
    fun `durations format as minutes and seconds`() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:00", formatDuration(-10))
        assertEquals("0:05", formatDuration(5_000))
        assertEquals("3:00", formatDuration(180_000))
        assertEquals("5:55", formatDuration(355_000))
        assertEquals("1:00:00", formatDuration(3_600_000))
    }

    @Test
    fun `track numbers are one based and padded`() {
        assertEquals("[01]", formatIndex(0))
        assertEquals("[10]", formatIndex(9))
        assertEquals("[100]", formatIndex(99))
    }

    @Test
    fun `song rows contain the title artist and duration`() {
        assertEquals("[01] Bohemian Rhapsody", formatSongTitleRow(0, library[0]))
        assertEquals("     Queen - 5:55", formatSongDetailRow(library[0]))
    }

    @Test
    fun `resolve by track number`() {
        assertEquals(0, resolveSong(library, "1"))
        assertEquals(2, resolveSong(library, "3"))
        assertNull(resolveSong(library, "0"))
        assertNull(resolveSong(library, "4"))
    }

    @Test
    fun `resolve by exact and partial title`() {
        assertEquals(0, resolveSong(library, "Bohemian Rhapsody"))
        assertEquals(0, resolveSong(library, "bohemian rhapsody"))
        assertEquals(0, resolveSong(library, "bohemian"))
        assertEquals(2, resolveSong(library, "five"))
    }

    @Test
    fun `resolve falls back to artist`() {
        assertEquals(1, resolveSong(library, "pink floyd"))
    }

    @Test
    fun `resolve returns null when nothing matches`() {
        assertNull(resolveSong(library, "nonexistent track"))
        assertNull(resolveSong(library, ""))
        assertNull(resolveSong(emptyList(), "anything"))
    }

    @Test
    fun `exact title wins over a substring match`() {
        val tricky = listOf(
            song(1, "Money For Nothing", "Dire Straits"),
            song(2, "Money", "Pink Floyd"),
        )
        assertEquals(1, resolveSong(tricky, "Money"))
    }

    @Test
    fun `search matches title artist and album`() {
        assertEquals(listOf(0), searchSongs(library, "rhapsody"))
        assertEquals(listOf(1), searchSongs(library, "floyd"))
        assertEquals(listOf(2), searchSongs(library, "time out"))
        assertTrue(searchSongs(library, "zzz").isEmpty())
        assertTrue(searchSongs(library, "  ").isEmpty())
    }

    @Test
    fun `search is case insensitive`() {
        assertEquals(listOf(0), searchSongs(library, "QUEEN"))
    }

    @Test
    fun `progress bar reflects position`() {
        assertEquals("[--------------------] 0%", formatProgressBar(0, 100_000))
        assertEquals("[##########----------] 50%", formatProgressBar(50_000, 100_000))
        assertEquals("[####################] 100%", formatProgressBar(100_000, 100_000))
        // An unknown duration must not divide by zero.
        assertEquals("[--------------------] 0%", formatProgressBar(5_000, 0))
        // Position past the end is clamped rather than overflowing the bar.
        assertEquals("[####################] 100%", formatProgressBar(200_000, 100_000))
    }
}
