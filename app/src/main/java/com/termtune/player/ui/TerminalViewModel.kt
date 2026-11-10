package com.termtune.player.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.termtune.player.data.LibraryResult
import com.termtune.player.data.MediaPermissions
import com.termtune.player.data.MusicRepository
import com.termtune.player.data.SettingsStore
import com.termtune.player.data.Song
import com.termtune.player.player.PlayerController
import com.termtune.player.player.PlayerState
import com.termtune.player.terminal.Command
import com.termtune.player.terminal.CommandExecutor
import com.termtune.player.terminal.CommandParser
import com.termtune.player.terminal.HelpText
import com.termtune.player.terminal.LineStyle
import com.termtune.player.terminal.ParseResult
import com.termtune.player.terminal.TerminalLine
import com.termtune.player.terminal.asDim
import com.termtune.player.terminal.asError
import com.termtune.player.terminal.asSuccess
import com.termtune.player.ui.theme.TerminalTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the terminal UI renders. */
data class TerminalUiState(
    val lines: List<TerminalLine> = HelpText.banner(),
    val library: List<Song> = emptyList(),
    val history: List<String> = emptyList(),
    val theme: TerminalTheme = TerminalTheme.GREEN,
    val player: PlayerState = PlayerState(),
    val isScanning: Boolean = false,
    val permissionGranted: Boolean = false,
    val libraryError: String? = null,
)

/** State owned locally by the view model, as opposed to DataStore or the player. */
private data class ShellState(
    val lines: List<TerminalLine> = HelpText.banner(),
    val library: List<Song> = emptyList(),
    val isScanning: Boolean = false,
    val permissionGranted: Boolean = false,
    val libraryError: String? = null,
    val hasScanned: Boolean = false,
)

class TerminalViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    private val settings = SettingsStore(application)
    private val playerController = PlayerController(application)
    private val executor = CommandExecutor(playerController, settings)

    private val shell = MutableStateFlow(
        ShellState(permissionGranted = MediaPermissions.hasAudioAccess(application)),
    )

    val uiState: StateFlow<TerminalUiState> = combine(
        shell,
        settings.preferences,
        playerController.state,
    ) { shellState, preferences, playerState ->
        TerminalUiState(
            lines = shellState.lines,
            library = shellState.library,
            history = preferences.history.asReversed(),
            theme = preferences.theme,
            player = playerState,
            isScanning = shellState.isScanning,
            permissionGranted = shellState.permissionGranted,
            libraryError = shellState.libraryError,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = TerminalUiState(),
    )

    init {
        viewModelScope.launch {
            playerController.connect()
            // Restore the playback modes the user last chose.
            val preferences = settings.preferences.first()
            playerController.setShuffle(preferences.shuffleEnabled)
            playerController.setRepeat(preferences.repeatEnabled)
            append(listOf("playback service ............. ok".asDim()))
            if (shell.value.permissionGranted) refreshLibrary(announce = true)
        }
    }

    // --------------------------------------------------------------- commands

    /**
     * Runs a line exactly as if it had been typed at the prompt.
     *
     * The on-screen buttons call this too (with [recordHistory] off), which is
     * why typed commands and taps can never disagree about playback state.
     */
    fun runCommandLine(raw: String, recordHistory: Boolean = true) {
        val input = raw.trim()
        append(listOf(TerminalLine("$PROMPT$input", LineStyle.COMMAND)))

        viewModelScope.launch {
            if (input.isNotEmpty() && recordHistory) settings.pushHistory(input)

            when (val parsed = CommandParser.parse(input)) {
                ParseResult.Empty -> Unit

                is ParseResult.Unknown -> append(
                    listOf(
                        "termtune: command not found: ${parsed.name}".asError(),
                        "Type 'help' for the list of commands.".asDim(),
                    ),
                )

                is ParseResult.Invalid -> append(
                    buildList {
                        add(parsed.message.asError())
                        parsed.usage?.let { add(it.asDim()) }
                    },
                )

                is ParseResult.Parsed -> executeCommand(parsed.command)
            }
        }
    }

    private suspend fun executeCommand(command: Command) {
        // Commands that read the library trigger a first scan on demand.
        if (command.needsLibrary && !shell.value.hasScanned && shell.value.permissionGranted) {
            refreshLibrary(announce = false)
        }

        val state = shell.value
        val result = executor.execute(command, state.library, state.permissionGranted)

        if (result.clearScreen) {
            shell.update { it.copy(lines = clearedScreen()) }
        }
        if (result.lines.isNotEmpty()) append(result.lines)
    }

    private val Command.needsLibrary: Boolean
        get() = this is Command.ListSongs || this is Command.Play || this is Command.Search

    // ------------------------------------------------------- transport buttons

    fun onPlayPauseClicked() {
        val playing = uiState.value.player.isPlaying
        runCommandLine(if (playing) "pause" else "play", recordHistory = false)
    }

    fun onNextClicked() = runCommandLine("next", recordHistory = false)

    fun onPreviousClicked() = runCommandLine("prev", recordHistory = false)

    fun onStopClicked() = runCommandLine("stop", recordHistory = false)

    fun onShuffleToggled() {
        val next = !uiState.value.player.shuffleEnabled
        runCommandLine("shuffle ${if (next) "on" else "off"}", recordHistory = false)
    }

    fun onRepeatToggled() {
        val next = !uiState.value.player.repeatEnabled
        runCommandLine("repeat ${if (next) "on" else "off"}", recordHistory = false)
    }

    fun onSongSelected(index: Int) =
        runCommandLine("play ${index + 1}", recordHistory = false)

    /** Slider drags move the player directly; logging every frame would spam the shell. */
    fun onSeekBarMoved(positionMs: Long) = playerController.seekToMillis(positionMs)

    fun onVolumeChanged(percent: Int) = playerController.setVolumePercent(percent)

    fun onThemeSelected(theme: TerminalTheme) =
        runCommandLine("theme ${theme.key}", recordHistory = false)

    // ---------------------------------------------------------------- library

    fun refreshLibraryFromUi() {
        viewModelScope.launch { refreshLibrary(announce = true) }
    }

    private suspend fun refreshLibrary(announce: Boolean) {
        shell.update { it.copy(isScanning = true) }
        when (val result = repository.loadSongs()) {
            is LibraryResult.Success -> {
                shell.update {
                    it.copy(
                        library = result.songs,
                        isScanning = false,
                        libraryError = null,
                        hasScanned = true,
                    )
                }
                if (announce) {
                    append(
                        listOf(
                            "mediastore scan: ${result.songs.size} track(s) found".asSuccess(),
                        ),
                    )
                }
            }

            LibraryResult.PermissionDenied -> {
                shell.update {
                    it.copy(
                        isScanning = false,
                        permissionGranted = false,
                        libraryError = "permission denied",
                        hasScanned = false,
                    )
                }
                if (announce) {
                    append(
                        listOf(
                            "scan failed: ${MediaPermissions.audioPermissionLabel()} denied"
                                .asError(),
                        ),
                    )
                }
            }

            is LibraryResult.Failure -> {
                shell.update {
                    it.copy(
                        isScanning = false,
                        libraryError = result.message,
                        hasScanned = true,
                    )
                }
                if (announce) {
                    append(listOf("scan failed: ${result.message}".asError()))
                }
            }
        }
    }

    /** Called after the runtime permission dialog closes. */
    fun onPermissionResult() {
        val granted = MediaPermissions.hasAudioAccess(getApplication())
        shell.update { it.copy(permissionGranted = granted) }
        viewModelScope.launch {
            if (granted) {
                append(listOf("permission granted: audio access ok".asSuccess()))
                refreshLibrary(announce = true)
            } else {
                append(
                    listOf(
                        "permission denied: ${MediaPermissions.audioPermissionLabel()}".asError(),
                        "TermTune cannot list songs without it.".asDim(),
                    ),
                )
            }
        }
    }

    // ---------------------------------------------------------------- history

    fun clearHistory() {
        viewModelScope.launch {
            settings.clearHistory()
            append(listOf("command history cleared".asSuccess()))
        }
    }

    fun clearScreen() = runCommandLine("clear", recordHistory = false)

    // ----------------------------------------------------------------- shared

    private fun append(newLines: List<TerminalLine>) {
        if (newLines.isEmpty()) return
        shell.update { current ->
            current.copy(lines = (current.lines + newLines).takeLast(MAX_LINES))
        }
    }

    private fun clearedScreen(): List<TerminalLine> = listOf(
        "TermTune :: v1.0".asSuccess(),
        "screen cleared - type 'help' for commands".asDim(),
        TerminalLine(""),
    )

    override fun onCleared() {
        playerController.release()
        super.onCleared()
    }

    companion object {
        const val PROMPT = "termtune:~$ "
        private const val MAX_LINES = 500
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
