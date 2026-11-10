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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.termtune.player.ui.TerminalUiState
import com.termtune.player.ui.components.BlinkingCursor
import com.termtune.player.ui.components.PlayerActions
import com.termtune.player.ui.components.PlayerBar
import com.termtune.player.ui.components.PromptInput
import com.termtune.player.ui.components.TerminalOutput
import com.termtune.player.ui.theme.LocalTerminalPalette

/**
 * Terminal Home Screen: scrollable output, the compact player, and the prompt.
 *
 * `imePadding` keeps the prompt above the soft keyboard, which is the main
 * usability trap in a mobile terminal UI.
 */
@Composable
fun ColumnScope.TerminalScreen(
    state: TerminalUiState,
    playerActions: PlayerActions,
    onSubmitCommand: (String) -> Unit,
    onClearHistory: () -> Unit,
) {
    val palette = LocalTerminalPalette.current
    var input by remember { mutableStateOf("") }
    var historyVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .weight(1f)
            .imePadding(),
    ) {
        TerminalOutput(
            lines = state.lines,
            modifier = Modifier.weight(1f),
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$ ",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.accent,
                    )
                    BlinkingCursor(color = palette.accent)
                }
            },
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.border),
        )

        PlayerBar(state = state.player, actions = playerActions)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.border),
        )

        PromptInput(
            value = input,
            onValueChange = { input = it },
            onSubmit = {
                onSubmitCommand(input)
                input = ""
            },
            onHistoryClicked = { historyVisible = true },
        )
    }

    if (historyVisible) {
        HistoryDialog(
            history = state.history,
            onDismiss = { historyVisible = false },
            onPick = { command ->
                input = command
                historyVisible = false
            },
            onClear = {
                onClearHistory()
                historyVisible = false
            },
        )
    }
}

/** Command history panel, the mobile stand-in for arrow-key recall. */
@Composable
private fun HistoryDialog(
    history: List<String>,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
    onClear: () -> Unit,
) {
    val palette = LocalTerminalPalette.current

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, palette.accent, RoundedCornerShape(4.dp))
                .background(palette.surface, RoundedCornerShape(4.dp))
                .padding(12.dp),
        ) {
            Text(
                text = "command history",
                style = MaterialTheme.typography.titleMedium,
                color = palette.accent,
            )
            Text(
                text = "tap an entry to reuse it",
                style = MaterialTheme.typography.labelSmall,
                color = palette.muted,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            if (history.isEmpty()) {
                Text(
                    text = "(no commands yet)",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.muted,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                    items(items = history) { command ->
                        Text(
                            text = "  $command",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.foreground,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    role = Role.Button,
                                    onClickLabel = "Reuse command $command",
                                ) { onPick(command) }
                                .padding(vertical = 7.dp),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DialogButton(label = "clear", onClick = onClear)
                DialogButton(label = "close", onClick = onDismiss, accent = true)
            }
        }
    }
}

@Composable
private fun DialogButton(
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
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(
            text = "[$label]",
            style = MaterialTheme.typography.labelSmall,
            color = if (accent) palette.accent else palette.muted,
        )
    }
}
