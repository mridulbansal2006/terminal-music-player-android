package com.termtune.player.ui.theme

/** The three selectable terminal colour schemes, chosen with `theme <name>`. */
enum class TerminalTheme(val key: String, val label: String) {
    GREEN("green", "matrix green"),
    AMBER("amber", "amber CRT"),
    BLUE("blue", "ice blue");

    companion object {
        fun fromKey(key: String?): TerminalTheme? =
            entries.firstOrNull { it.key == key?.trim()?.lowercase() }
    }
}
