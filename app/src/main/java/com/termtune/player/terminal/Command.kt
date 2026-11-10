package com.termtune.player.terminal

import com.termtune.player.ui.theme.TerminalTheme

/** A successfully parsed terminal command. */
sealed interface Command {
    data object Help : Command
    data object ListSongs : Command

    /** `play`, `play 3`, `play bohemian`. A `null` target resumes or starts the queue. */
    data class Play(val target: String?) : Command
    data object Pause : Command
    data object Resume : Command
    data object Next : Command
    data object Prev : Command
    data object Stop : Command
    data object NowPlaying : Command
    data class Volume(val percent: Int) : Command
    data class Seek(val seconds: Int) : Command
    data class Shuffle(val enabled: Boolean) : Command
    data class Repeat(val enabled: Boolean) : Command
    data class Search(val query: String) : Command
    data object Clear : Command
    data class SetTheme(val theme: TerminalTheme) : Command
}

/** Outcome of running raw input through [CommandParser]. */
sealed interface ParseResult {
    /** Input was blank; the shell just prints a new prompt. */
    data object Empty : ParseResult

    data class Parsed(val command: Command) : ParseResult

    /** The command is known but its arguments are not usable. */
    data class Invalid(val message: String, val usage: String? = null) : ParseResult

    /** No such command. */
    data class Unknown(val name: String) : ParseResult
}
