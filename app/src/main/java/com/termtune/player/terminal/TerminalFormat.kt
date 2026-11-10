package com.termtune.player.terminal

import com.termtune.player.data.Song

/** Formats a millisecond duration as `m:ss`, or `h:mm:ss` for long tracks. */
fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

/** Zero-padded track number as shown in the terminal, e.g. `[07]`. */
fun formatIndex(index: Int): String = "[%02d]".format(index + 1)

/** First line of a two-line track listing: `[07] Bohemian Rhapsody`. */
fun formatSongTitleRow(index: Int, song: Song): String =
    "${formatIndex(index)} ${song.title}"

/** Second line of a two-line track listing: `     Queen - 5:55`. */
fun formatSongDetailRow(song: Song): String =
    "     ${song.artist} - ${formatDuration(song.durationMs)}"

/**
 * Resolves the `play <target>` argument to a library index.
 *
 * A target is matched, in order, as a 1-based track number, an exact
 * case-insensitive title, a title substring, and finally an artist substring.
 * Returns `null` when nothing matches.
 */
fun resolveSong(library: List<Song>, target: String): Int? {
    val needle = target.trim()
    if (needle.isEmpty() || library.isEmpty()) return null

    needle.toIntOrNull()?.let { number ->
        val index = number - 1
        return if (index in library.indices) index else null
    }

    val lower = needle.lowercase()
    library.indexOfFirst { it.title.lowercase() == lower }
        .takeIf { it >= 0 }?.let { return it }
    library.indexOfFirst { it.title.lowercase().contains(lower) }
        .takeIf { it >= 0 }?.let { return it }
    library.indexOfFirst { it.artist.lowercase().contains(lower) }
        .takeIf { it >= 0 }?.let { return it }
    return null
}

/** Library indices whose title, artist or album contains [query]. */
fun searchSongs(library: List<Song>, query: String): List<Int> {
    val lower = query.trim().lowercase()
    if (lower.isEmpty()) return emptyList()
    return library.indices.filter { index ->
        val song = library[index]
        song.title.lowercase().contains(lower) ||
            song.artist.lowercase().contains(lower) ||
            song.album.lowercase().contains(lower)
    }
}

/** Renders a `[####------] 42%` style progress meter for `nowplaying`. */
fun formatProgressBar(positionMs: Long, durationMs: Long, width: Int = 20): String {
    val safeWidth = width.coerceAtLeast(1)
    val fraction = if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    val filled = (fraction * safeWidth).toInt().coerceIn(0, safeWidth)
    val percent = (fraction * 100).toInt()
    return "[" + "#".repeat(filled) + "-".repeat(safeWidth - filled) + "] " + percent + "%"
}
