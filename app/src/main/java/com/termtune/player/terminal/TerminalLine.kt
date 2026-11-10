package com.termtune.player.terminal

/** How a line of terminal output should be coloured. */
enum class LineStyle {
    /** The echoed prompt plus the command the user typed. */
    COMMAND,
    OUTPUT,
    SUCCESS,
    ERROR,
    /** Hints, usage strings and other secondary text. */
    DIM,
    HEADER,
}

data class TerminalLine(val text: String, val style: LineStyle = LineStyle.OUTPUT)

fun String.asOutput() = TerminalLine(this, LineStyle.OUTPUT)
fun String.asSuccess() = TerminalLine(this, LineStyle.SUCCESS)
fun String.asError() = TerminalLine(this, LineStyle.ERROR)
fun String.asDim() = TerminalLine(this, LineStyle.DIM)
fun String.asHeader() = TerminalLine(this, LineStyle.HEADER)
