package com.termtune.player.player

import com.termtune.player.data.Song

/** Immutable snapshot of playback, rendered identically by commands and buttons. */
data class PlayerState(
    val isConnected: Boolean = false,
    val isPlaying: Boolean = false,
    /** False once playback has been stopped or never started. */
    val isPrepared: Boolean = false,
    val currentSong: Song? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val volumePercent: Int = 100,
    val shuffleEnabled: Boolean = false,
    val repeatEnabled: Boolean = false,
    val queueSize: Int = 0,
    val queueIndex: Int = 0,
) {
    val hasQueue: Boolean get() = queueSize > 0

    /** Terminal status word shown in the player bar and `nowplaying`. */
    val statusLabel: String
        get() = when {
            !isConnected -> "connecting"
            isPlaying -> "playing"
            hasQueue && isPrepared -> "paused"
            hasQueue -> "stopped"
            else -> "idle"
        }
}
