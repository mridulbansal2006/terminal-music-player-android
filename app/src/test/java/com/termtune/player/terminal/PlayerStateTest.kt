package com.termtune.player.terminal

import com.termtune.player.data.Song
import com.termtune.player.player.PlayerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerStateTest {

    private val song = Song(
        id = 7,
        title = "Take Five",
        artist = "Dave Brubeck",
        album = "Time Out",
        durationMs = 324_000,
        uri = "content://media/external/audio/media/7",
    )

    @Test
    fun `default state is idle and empty`() {
        val state = PlayerState()
        assertFalse(state.hasQueue)
        assertEquals("connecting", state.statusLabel)
    }

    @Test
    fun `status label tracks the playback lifecycle`() {
        val connected = PlayerState(isConnected = true)
        assertEquals("idle", connected.statusLabel)

        val playing = connected.copy(
            isPlaying = true,
            isPrepared = true,
            currentSong = song,
            queueSize = 3,
        )
        assertEquals("playing", playing.statusLabel)
        assertTrue(playing.hasQueue)

        val paused = playing.copy(isPlaying = false)
        assertEquals("paused", paused.statusLabel)

        val stopped = paused.copy(isPrepared = false)
        assertEquals("stopped", stopped.statusLabel)
    }
}
