package com.termtune.player.terminal

/** One row of the built-in command reference, shared by `help` and the Help screen. */
data class CommandDoc(
    val usage: String,
    val description: String,
    val example: String,
)

/** The authoritative command reference for TermTune. */
object HelpText {

    val commands: List<CommandDoc> = listOf(
        CommandDoc("help", "Show available commands", "help"),
        CommandDoc("list", "List locally available songs", "list"),
        CommandDoc("play [n|name]", "Play a track by number or name", "play 3"),
        CommandDoc("pause", "Pause playback", "pause"),
        CommandDoc("resume", "Resume playback", "resume"),
        CommandDoc("next", "Play next track", "next"),
        CommandDoc("prev", "Play previous track", "prev"),
        CommandDoc("stop", "Stop playback and rewind", "stop"),
        CommandDoc("nowplaying", "Display the current track", "nowplaying"),
        CommandDoc("volume <0-100>", "Set playback volume", "volume 50"),
        CommandDoc("seek <sec>", "Seek within the current track", "seek 30"),
        CommandDoc("shuffle on|off", "Toggle shuffle", "shuffle on"),
        CommandDoc("repeat on|off", "Toggle repeat mode", "repeat on"),
        CommandDoc("search <text>", "Search the local library", "search queen"),
        CommandDoc("clear", "Clear terminal output", "clear"),
        CommandDoc("theme <name>", "Change theme: green, amber, blue", "theme amber"),
    )

    /** `help` output, formatted for a narrow phone terminal. */
    fun helpLines(): List<TerminalLine> = buildList {
        add("TermTune command reference".asHeader())
        add("".asOutput())
        commands.forEach { doc ->
            add("  ${doc.usage}".asSuccess())
            add("      ${doc.description}".asOutput())
        }
        add("".asOutput())
        add("Aliases: ls, np, vol, cls, find, skip, back".asDim())
        add("Tap [hist] to reuse a previous command.".asDim())
    }

    /** Shown when the library is empty, per the project's empty-state requirement. */
    fun emptyLibraryHelp(): List<TerminalLine> = listOf(
        "No audio files found on this device.".asError(),
        "".asOutput(),
        "To load test audio:".asDim(),
        "  1. adb push song.mp3 /sdcard/Music/".asOutput(),
        "  2. adb shell am broadcast \\".asOutput(),
        "       -a android.intent.action.MEDIA_SCANNER_SCAN_FILE \\".asOutput(),
        "       -d file:///sdcard/Music/song.mp3".asOutput(),
        "  3. run 'list' again".asOutput(),
        "".asOutput(),
        "On a physical device, copy music into".asDim(),
        "the Music folder and re-run 'list'.".asDim(),
    )

    /** The start-up banner. Kept under 32 columns so it never wraps on a phone. */
    fun banner(): List<TerminalLine> = listOf(
        "+------------------------------+".asSuccess(),
        "|  TermTune :: v1.0            |".asSuccess(),
        "|  terminal music player       |".asSuccess(),
        "+------------------------------+".asSuccess(),
        "".asOutput(),
        "media3 exoplayer .......... ready".asDim(),
        "mediastore scanner ........ ready".asDim(),
        "".asOutput(),
        "Type 'help' to see all commands.".asOutput(),
        "".asOutput(),
    )
}
