package com.termtune.player.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.termtune.player.terminal.CommandDoc
import com.termtune.player.terminal.HelpText
import com.termtune.player.ui.theme.LocalTerminalPalette

/** Help / Commands Screen: the same reference the `help` command prints. */
@Composable
fun HelpScreen() {
    val palette = LocalTerminalPalette.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Text(
                    text = "man termtune",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.accent,
                )
                Text(
                    text = "All commands work from the prompt on",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.muted,
                )
                Text(
                    text = "the [term] screen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.muted,
                )
            }
        }

        items(items = HelpText.commands) { doc -> CommandEntry(doc) }

        item {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                Text(
                    text = "aliases",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.accent,
                )
                listOf(
                    "ls   -> list",
                    "np   -> nowplaying",
                    "vol  -> volume",
                    "cls  -> clear",
                    "find -> search",
                    "skip -> next",
                    "back -> prev",
                ).forEach {
                    Text(
                        text = "  $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.muted,
                    )
                }
            }
        }
    }
}

@Composable
private fun CommandEntry(doc: CommandDoc) {
    val palette = LocalTerminalPalette.current
    Column(modifier = Modifier.padding(vertical = 7.dp)) {
        Text(
            text = doc.usage,
            style = MaterialTheme.typography.labelLarge,
            color = palette.accent,
        )
        Text(
            text = "  ${doc.description}",
            style = MaterialTheme.typography.bodySmall,
            color = palette.foreground,
        )
        Text(
            text = "  e.g. ${doc.example}",
            style = MaterialTheme.typography.labelSmall,
            color = palette.muted,
        )
    }
}
