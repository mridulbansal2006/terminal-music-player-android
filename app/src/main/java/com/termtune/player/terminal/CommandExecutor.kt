package com.termtune.player.terminal

import com.termtune.player.data.MediaPermissions
import com.termtune.player.data.SettingsStore
import com.termtune.player.data.Song
import com.termtune.player.player.PlayerController

/** What the shell should do with the screen after a command runs. */
data class ExecResult(
    val lines: List<TerminalLine> = emptyList(),
    val clearScreen: Boolean = false,
)

/**
 * Turns a parsed [Command] into real playback effects plus terminal output.
 *
 * Every state change goes through [PlayerController], which is the same object
 * the on-screen transport buttons use, so the two input styles can never drift
 * apart.
 */
class CommandExecutor(
    private val player: PlayerController,
    private val settings: SettingsStore,
) {

    suspend fun execute(
        command: Command,
        library: List<Song>,
        permissionGranted: Boolean,
    ): ExecResult = when (command) {
        Command.Help -> ExecResult(HelpText.helpLines())
        Command.Clear -> ExecResult(clearScreen = true)
        Command.ListSongs -> listSongs(library, permissionGranted)
        is Command.Play -> play(command.target, library, permissionGranted)
        Command.Pause -> pause()
        Command.Resume -> resume()
        Command.Next -> skip(forward = true)
        Command.Prev -> skip(forward = false)
        Command.Stop -> stop()
        Command.NowPlaying -> nowPlaying()
        is Command.Volume -> setVolume(command.percent)
        is Command.Seek -> seek(command.seconds)
        is Command.Shuffle -> setShuffle(command.enabled)
        is Command.Repeat -> setRepeat(command.enabled)
        is Command.Search -> search(command.query, library, permissionGranted)
        is Command.SetTheme -> setTheme(command)
    }

    // ---------------------------------------------------------------- library

    private fun listSongs(library: List<Song>, permissionGranted: Boolean): ExecResult {
        if (!permissionGranted) return ExecResult(permissionLines())
        if (library.isEmpty()) return ExecResult(HelpText.emptyLibraryHelp())

        val lines = buildList {
            add("${library.size} track(s) in local library".asHeader())
            add("".asOutput())
            library.forEachIndexed { index, song ->
                add(formatSongTitleRow(index, song).asSuccess())
                add(formatSongDetailRow(song).asDim())
            }
            add("".asOutput())
            add("Use 'play <number>' or 'play <name>'.".asDim())
        }
        return ExecResult(lines)
    }

    private fun search(
        query: String,
        library: List<Song>,
        permissionGranted: Boolean,
    ): ExecResult {
        if (!permissionGranted) return ExecResult(permissionLines())
        if (library.isEmpty()) return ExecResult(HelpText.emptyLibraryHelp())

        val matches = searchSongs(library, query)
        if (matches.isEmpty()) {
            return ExecResult(
                listOf(
                    "search: no matches for '$query'".asError(),
                    "Try 'list' to see everything available.".asDim(),
                ),
            )
        }

        val lines = buildList {
            add("${matches.size} match(es) for '$query'".asHeader())
            add("".asOutput())
            matches.forEach { index ->
                add(formatSongTitleRow(index, library[index]).asSuccess())
                add(formatSongDetailRow(library[index]).asDim())
            }
        }
        return ExecResult(lines)
    }

    // --------------------------------------------------------------- playback

    private fun play(
        target: String?,
        library: List<Song>,
        permissionGranted: Boolean,
    ): ExecResult {
        if (!permissionGranted) return ExecResult(permissionLines())
        if (library.isEmpty()) return ExecResult(HelpText.emptyLibraryHelp())
        notConnected()?.let { return it }

        val state = player.state.value

        if (target == null) {
            return when {
                state.isPlaying -> ExecResult(
                    listOf("already playing: ${state.currentSong?.title ?: "unknown"}".asDim()),
                )
                state.hasQueue -> {
                    player.play()
                    ExecResult(listOf("> resumed: ${state.currentSong?.title ?: "track"}".asSuccess()))
                }
                else -> start(library, 0)
            }
        }

        val index = resolveSong(library, target)
            ?: return ExecResult(
                listOf(
                    "play: no song matching '$target'".asError(),
                    "Run 'list' for track numbers or 'search <text>'.".asDim(),
                ),
            )
        return start(library, index)
    }

    private fun start(library: List<Song>, index: Int): ExecResult {
        player.setQueue(library, index, playWhenReady = true)
        val song = library[index]
        return ExecResult(
            listOf(
                "> playing ${formatIndex(index)} ${song.title}".asSuccess(),
                "  ${song.artist} - ${formatDuration(song.durationMs)}".asDim(),
            ),
        )
    }

    private fun pause(): ExecResult {
        notConnected()?.let { return it }
        val state = player.state.value
        if (!state.isPlaying) {
            return ExecResult(listOf("pause: nothing is playing".asError()))
        }
        player.pause()
        return ExecResult(listOf("|| paused: ${state.currentSong?.title ?: "track"}".asSuccess()))
    }

    private fun resume(): ExecResult {
        notConnected()?.let { return it }
        val state = player.state.value
        return when {
            !state.hasQueue -> ExecResult(
                listOf(
                    "resume: no track loaded".asError(),
                    "Start one with 'play' or 'play <number>'.".asDim(),
                ),
            )
            state.isPlaying -> ExecResult(listOf("resume: already playing".asDim()))
            else -> {
                player.play()
                ExecResult(
                    listOf("> resumed: ${state.currentSong?.title ?: "track"}".asSuccess()),
                )
            }
        }
    }

    private fun skip(forward: Boolean): ExecResult {
        notConnected()?.let { return it }
        val label = if (forward) "next" else "prev"
        if (!player.state.value.hasQueue) {
            return ExecResult(
                listOf(
                    "$label: queue is empty".asError(),
                    "Start playback first with 'play'.".asDim(),
                ),
            )
        }
        if (forward) player.next() else player.previous()
        val song = player.state.value.currentSong
        return ExecResult(
            listOf(">> $label: ${song?.title ?: "track"}".asSuccess()),
        )
    }

    private fun stop(): ExecResult {
        notConnected()?.let { return it }
        if (!player.state.value.hasQueue) {
            return ExecResult(listOf("stop: nothing to stop".asError()))
        }
        player.stop()
        return ExecResult(listOf("[] playback stopped".asSuccess()))
    }

    private fun nowPlaying(): ExecResult {
        notConnected()?.let { return it }
        val state = player.state.value
        val song = state.currentSong
            ?: return ExecResult(
                listOf(
                    "nowplaying: nothing loaded".asError(),
                    "Run 'list' then 'play <number>'.".asDim(),
                ),
            )

        val lines = buildList {
            add("now playing".asHeader())
            add("  track   : ${song.title}".asOutput())
            add("  artist  : ${song.artist}".asOutput())
            add("  album   : ${song.album}".asOutput())
            add(
                (
                    "  time    : ${formatDuration(state.positionMs)} / " +
                        formatDuration(state.durationMs)
                    ).asOutput(),
            )
            add("  ${formatProgressBar(state.positionMs, state.durationMs)}".asSuccess())
            add("  status  : ${state.statusLabel}".asOutput())
            add(
                (
                    "  shuffle : ${onOff(state.shuffleEnabled)}   " +
                        "repeat: ${onOff(state.repeatEnabled)}"
                    ).asOutput(),
            )
            add("  volume  : ${state.volumePercent}%".asOutput())
            add("  queue   : ${state.queueIndex + 1}/${state.queueSize}".asOutput())
        }
        return ExecResult(lines)
    }

    private fun setVolume(percent: Int): ExecResult {
        notConnected()?.let { return it }
        player.setVolumePercent(percent)
        return ExecResult(listOf("volume set to $percent%".asSuccess()))
    }

    private fun seek(seconds: Int): ExecResult {
        notConnected()?.let { return it }
        val state = player.state.value
        if (!state.hasQueue || state.currentSong == null) {
            return ExecResult(
                listOf(
                    "seek: no track loaded".asError(),
                    "Start playback first with 'play'.".asDim(),
                ),
            )
        }

        val durationMs = state.durationMs
        if (durationMs > 0 && seconds * 1000L > durationMs) {
            return ExecResult(
                listOf(
                    "seek: ${seconds}s is past the end of this track".asError(),
                    "Track length is ${formatDuration(durationMs)}.".asDim(),
                ),
            )
        }

        player.seekToSeconds(seconds)
        return ExecResult(
            listOf("seeked to ${formatDuration(seconds * 1000L)}".asSuccess()),
        )
    }

    private suspend fun setShuffle(enabled: Boolean): ExecResult {
        notConnected()?.let { return it }
        player.setShuffle(enabled)
        settings.setShuffle(enabled)
        return ExecResult(listOf("shuffle ${onOff(enabled)}".asSuccess()))
    }

    private suspend fun setRepeat(enabled: Boolean): ExecResult {
        notConnected()?.let { return it }
        player.setRepeat(enabled)
        settings.setRepeat(enabled)
        return ExecResult(listOf("repeat ${onOff(enabled)}".asSuccess()))
    }

    private suspend fun setTheme(command: Command.SetTheme): ExecResult {
        settings.setTheme(command.theme)
        return ExecResult(
            listOf("theme set to ${command.theme.key} (${command.theme.label})".asSuccess()),
        )
    }

    // ----------------------------------------------------------------- shared

    private fun notConnected(): ExecResult? =
        if (player.state.value.isConnected) {
            null
        } else {
            ExecResult(
                listOf(
                    "player: playback service not connected yet".asError(),
                    "Wait a moment and try again.".asDim(),
                ),
            )
        }

    private fun permissionLines(): List<TerminalLine> = listOf(
        "permission denied: ${MediaPermissions.audioPermissionLabel()}".asError(),
        "TermTune needs audio access to read your".asDim(),
        "local music. Grant it from [cfg] settings.".asDim(),
    )

    private fun onOff(value: Boolean) = if (value) "on" else "off"
}
