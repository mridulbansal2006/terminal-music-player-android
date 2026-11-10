package com.termtune.player.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.termtune.player.data.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * The single gateway to playback.
 *
 * Both typed commands and the on-screen transport buttons call into this class,
 * so there is only ever one source of truth for playback state. It wraps a
 * [MediaController] connected to [PlaybackService].
 */
class PlayerController(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private var controller: MediaController? = null
    private var queue: List<Song> = emptyList()
    private var positionTicker: Job? = null

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publishState()
    }

    /** Connects to the playback service. Safe to call more than once. */
    suspend fun connect() {
        if (controller != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val connected = MediaController.Builder(context, token).buildAsync().await()
        controller = connected
        connected.addListener(listener)
        publishState()
        startPositionTicker()
    }

    fun release() {
        positionTicker?.cancel()
        positionTicker = null
        controller?.removeListener(listener)
        controller?.release()
        controller = null
        _state.value = PlayerState()
    }

    /** Replaces the queue and starts at [startIndex]. */
    fun setQueue(songs: List<Song>, startIndex: Int, playWhenReady: Boolean) {
        val player = controller ?: return
        queue = songs
        player.setMediaItems(songs.map(::toMediaItem), startIndex, 0L)
        player.prepare()
        player.playWhenReady = playWhenReady
        publishState()
    }

    fun play() {
        val player = controller ?: return
        // After `stop` the player is idle and needs re-preparing before it plays.
        if (player.playbackState == Player.STATE_IDLE) player.prepare()
        player.play()
        publishState()
    }

    fun pause() {
        controller?.pause()
        publishState()
    }

    fun stop() {
        val player = controller ?: return
        player.stop()
        player.seekTo(0L)
        publishState()
    }

    fun next() {
        val player = controller ?: return
        // Wrap around at the end of the queue so `next` always does something.
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        } else if (player.mediaItemCount > 0) {
            player.seekTo(0, 0L)
        }
        publishState()
    }

    fun previous() {
        val player = controller ?: return
        if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else {
            player.seekTo(0L)
        }
        publishState()
    }

    fun seekToSeconds(seconds: Int) {
        controller?.seekTo(seconds * 1000L)
        publishState()
    }

    fun seekToMillis(millis: Long) {
        controller?.seekTo(millis.coerceAtLeast(0L))
        publishState()
    }

    fun setVolumePercent(percent: Int) {
        controller?.volume = (percent.coerceIn(0, 100)) / 100f
        publishState()
    }

    fun setShuffle(enabled: Boolean) {
        controller?.shuffleModeEnabled = enabled
        publishState()
    }

    fun setRepeat(enabled: Boolean) {
        controller?.repeatMode =
            if (enabled) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
        publishState()
    }

    /** Duration of the loaded track, falling back to the MediaStore value. */
    fun currentDurationMs(): Long = _state.value.durationMs

    private fun startPositionTicker() {
        positionTicker?.cancel()
        positionTicker = scope.launch {
            while (isActive) {
                delay(POSITION_POLL_MS)
                if (controller?.isPlaying == true) publishState()
            }
        }
    }

    private fun publishState() {
        val player = controller
        if (player == null) {
            _state.value = PlayerState()
            return
        }

        val currentId = player.currentMediaItem?.mediaId
        val currentSong = queue.firstOrNull { it.id.toString() == currentId }
        val reportedDuration = player.duration
        val duration = if (reportedDuration == C.TIME_UNSET || reportedDuration < 0) {
            currentSong?.durationMs ?: 0L
        } else {
            reportedDuration
        }

        _state.value = PlayerState(
            isConnected = true,
            isPlaying = player.isPlaying,
            isPrepared = player.playbackState != Player.STATE_IDLE,
            currentSong = currentSong,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = duration,
            volumePercent = (player.volume * 100).roundToInt().coerceIn(0, 100),
            shuffleEnabled = player.shuffleModeEnabled,
            repeatEnabled = player.repeatMode != Player.REPEAT_MODE_OFF,
            queueSize = player.mediaItemCount,
            queueIndex = player.currentMediaItemIndex,
        )
    }

    private fun toMediaItem(song: Song): MediaItem =
        MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(song.uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build(),
            )
            .build()

    private companion object {
        const val POSITION_POLL_MS = 500L
    }
}
