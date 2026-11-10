package com.termtune.player.terminal

import com.termtune.player.ui.theme.TerminalTheme

/**
 * Pure, side-effect-free translation of a typed line into a [Command].
 *
 * Keeping parsing separate from execution is what makes the whole command
 * surface unit-testable without an Android device.
 */
object CommandParser {

    private val whitespace = Regex("\\s+")

    fun parse(input: String): ParseResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ParseResult.Empty

        val parts = trimmed.split(whitespace)
        val name = parts.first().lowercase()
        val args = parts.drop(1)
        val rest = trimmed.substring(parts.first().length).trim()

        return when (name) {
            "help", "?", "man" -> ParseResult.Parsed(Command.Help)
            "list", "ls" -> ParseResult.Parsed(Command.ListSongs)
            "play" -> ParseResult.Parsed(Command.Play(rest.ifBlank { null }))
            "pause" -> noArgs(name, args, Command.Pause)
            "resume" -> noArgs(name, args, Command.Resume)
            "next", "skip" -> noArgs(name, args, Command.Next)
            "prev", "previous", "back" -> noArgs(name, args, Command.Prev)
            "stop" -> noArgs(name, args, Command.Stop)
            "nowplaying", "np" -> noArgs(name, args, Command.NowPlaying)
            "volume", "vol" -> parseVolume(args)
            "seek" -> parseSeek(args)
            "shuffle" -> parseToggle(name, args) { Command.Shuffle(it) }
            "repeat" -> parseToggle(name, args) { Command.Repeat(it) }
            "search", "find" -> parseSearch(rest)
            "clear", "cls" -> ParseResult.Parsed(Command.Clear)
            "theme" -> parseTheme(args)
            else -> ParseResult.Unknown(parts.first())
        }
    }

    private fun noArgs(name: String, args: List<String>, command: Command): ParseResult =
        if (args.isEmpty()) {
            ParseResult.Parsed(command)
        } else {
            ParseResult.Invalid("$name: takes no arguments", "usage: $name")
        }

    private fun parseVolume(args: List<String>): ParseResult {
        val usage = "usage: volume <0-100>"
        val raw = args.firstOrNull()
            ?: return ParseResult.Invalid("volume: missing value", usage)
        if (args.size > 1) return ParseResult.Invalid("volume: too many arguments", usage)

        val value = raw.removeSuffix("%").toIntOrNull()
            ?: return ParseResult.Invalid("volume: '$raw' is not a number", usage)
        if (value !in 0..100) {
            return ParseResult.Invalid("volume: $value is out of range (0-100)", usage)
        }
        return ParseResult.Parsed(Command.Volume(value))
    }

    private fun parseSeek(args: List<String>): ParseResult {
        val usage = "usage: seek <seconds>   (also accepts m:ss)"
        val raw = args.firstOrNull()
            ?: return ParseResult.Invalid("seek: missing position", usage)
        if (args.size > 1) return ParseResult.Invalid("seek: too many arguments", usage)

        val seconds = parseTimecode(raw)
            ?: return ParseResult.Invalid("seek: '$raw' is not a valid position", usage)
        if (seconds < 0) {
            return ParseResult.Invalid("seek: position cannot be negative", usage)
        }
        return ParseResult.Parsed(Command.Seek(seconds))
    }

    /** Accepts `90`, `1:30` and `0:01:30`, returning whole seconds. */
    internal fun parseTimecode(raw: String): Int? {
        if (raw.isEmpty()) return null
        if (!raw.contains(':')) return raw.toIntOrNull()

        val segments = raw.split(':')
        if (segments.size !in 2..3) return null
        val numbers = segments.map { it.toIntOrNull() ?: return null }
        if (numbers.drop(1).any { it < 0 || it > 59 }) return null
        if (numbers.any { it < 0 }) return null

        return when (numbers.size) {
            2 -> numbers[0] * 60 + numbers[1]
            else -> numbers[0] * 3600 + numbers[1] * 60 + numbers[2]
        }
    }

    private fun parseToggle(
        name: String,
        args: List<String>,
        build: (Boolean) -> Command,
    ): ParseResult {
        val usage = "usage: $name on|off"
        val raw = args.firstOrNull()
            ?: return ParseResult.Invalid("$name: missing state", usage)
        if (args.size > 1) return ParseResult.Invalid("$name: too many arguments", usage)

        return when (raw.lowercase()) {
            "on", "true", "1", "yes" -> ParseResult.Parsed(build(true))
            "off", "false", "0", "no" -> ParseResult.Parsed(build(false))
            else -> ParseResult.Invalid("$name: unknown state '$raw'", usage)
        }
    }

    private fun parseSearch(rest: String): ParseResult =
        if (rest.isBlank()) {
            ParseResult.Invalid("search: missing query", "usage: search <text>")
        } else {
            ParseResult.Parsed(Command.Search(rest))
        }

    private fun parseTheme(args: List<String>): ParseResult {
        val usage = "usage: theme green|amber|blue"
        val raw = args.firstOrNull()
            ?: return ParseResult.Invalid("theme: missing name", usage)
        if (args.size > 1) return ParseResult.Invalid("theme: too many arguments", usage)

        val theme = TerminalTheme.fromKey(raw)
            ?: return ParseResult.Invalid("theme: unknown theme '$raw'", usage)
        return ParseResult.Parsed(Command.SetTheme(theme))
    }
}
