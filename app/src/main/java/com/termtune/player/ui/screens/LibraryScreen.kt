package com.termtune.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.termtune.player.data.MediaPermissions
import com.termtune.player.data.Song
import com.termtune.player.terminal.formatDuration
import com.termtune.player.terminal.formatIndex
import com.termtune.player.terminal.searchSongs
import com.termtune.player.ui.TerminalUiState
import com.termtune.player.ui.theme.LocalTerminalPalette

/** Local Music Library: searchable, scrollable, tap to play. */
@Composable
fun ColumnScope.LibraryScreen(
    state: TerminalUiState,
    onSongSelected: (Int) -> Unit,
    onRescan: () -> Unit,
    onRequestPermission: () -> Unit,
) {
    val palette = LocalTerminalPalette.current
    var query by remember { mutableStateOf("") }

    // Filtering reuses the same matcher the `search` command uses.
    val visibleIndices = remember(state.library, query) {
        if (query.isBlank()) state.library.indices.toList()
        else searchSongs(state.library, query)
    }

    Column(
        modifier = Modifier
            .weight(1f)
            .imePadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "search>",
                style = MaterialTheme.typography.labelSmall,
                color = palette.accent,
            )
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(color = palette.foreground),
                cursorBrush = SolidColor(palette.accent),
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Search local songs" },
            )
            ActionChip(label = "rescan", onClick = onRescan)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.border)
                .padding(top = 0.dp),
        )

        when {
            !state.permissionGranted -> LibraryMessage(
                title = "audio permission required",
                body = listOf(
                    "TermTune needs ${MediaPermissions.audioPermissionLabel()}",
                    "to read local music from MediaStore.",
                ),
                actionLabel = "grant permission",
                onAction = onRequestPermission,
            )

            state.isScanning -> LibraryMessage(
                title = "scanning mediastore...",
                body = listOf("Reading local audio files."),
            )

            state.libraryError != null -> LibraryMessage(
                title = "scan failed",
                body = listOf(state.libraryError),
                actionLabel = "retry",
                onAction = onRescan,
            )

            state.library.isEmpty() -> LibraryMessage(
                title = "library is empty",
                body = listOf(
                    "No audio files were found on this device.",
                    "",
                    "Push a test file, then rescan:",
                    "adb push song.mp3 /sdcard/Music/",
                    "adb shell am broadcast -a android.intent",
                    "  .action.MEDIA_SCANNER_SCAN_FILE",
                    "  -d file:///sdcard/Music/song.mp3",
                ),
                actionLabel = "rescan",
                onAction = onRescan,
            )

            visibleIndices.isEmpty() -> LibraryMessage(
                title = "no matches",
                body = listOf("Nothing in the library matches '$query'."),
            )

            else -> {
                Text(
                    text = "  ${visibleIndices.size} / ${state.library.size} track(s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.muted,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(visibleIndices) { _, libraryIndex ->
                        SongRow(
                            index = libraryIndex,
                            song = state.library[libraryIndex],
                            isCurrent = state.player.currentSong?.id ==
                                state.library[libraryIndex].id,
                            onClick = { onSongSelected(libraryIndex) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SongRow(
    index: Int,
    song: Song,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalTerminalPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isCurrent) palette.accent.copy(alpha = 0.10f) else palette.background)
            .clickable(
                role = Role.Button,
                onClickLabel = "Play ${song.title} by ${song.artist}",
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatIndex(index),
                style = MaterialTheme.typography.bodySmall,
                color = if (isCurrent) palette.accent else palette.accentDim,
            )
            Text(
                text = " ${song.title}",
                style = MaterialTheme.typography.bodySmall,
                color = if (isCurrent) palette.accent else palette.foreground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatDuration(song.durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = palette.muted,
            )
        }
        Text(
            text = "     ${song.artist}",
            style = MaterialTheme.typography.labelSmall,
            color = palette.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ColumnScope.LibraryMessage(
    title: String,
    body: List<String>,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val palette = LocalTerminalPalette.current
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Text(
            text = "!! $title",
            style = MaterialTheme.typography.titleMedium,
            color = palette.error,
        )
        Box(modifier = Modifier.padding(top = 8.dp)) {
            Column {
                body.forEach { line ->
                    Text(
                        text = line.ifEmpty { " " },
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.muted,
                    )
                }
            }
        }
        if (actionLabel != null && onAction != null) {
            Box(modifier = Modifier.padding(top = 14.dp)) {
                ActionChip(label = actionLabel, onClick = onAction, accent = true)
            }
        }
    }
}

@Composable
fun ActionChip(
    label: String,
    onClick: () -> Unit,
    accent: Boolean = false,
) {
    val palette = LocalTerminalPalette.current
    Box(
        modifier = Modifier
            .border(
                1.dp,
                if (accent) palette.accent else palette.border,
                RoundedCornerShape(3.dp),
            )
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = "[$label]",
            style = MaterialTheme.typography.labelSmall,
            color = if (accent) palette.accent else palette.muted,
        )
    }
}
