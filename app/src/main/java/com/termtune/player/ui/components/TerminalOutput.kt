package com.termtune.player.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.termtune.player.terminal.LineStyle
import com.termtune.player.terminal.TerminalLine
import com.termtune.player.ui.theme.LocalTerminalPalette
import com.termtune.player.ui.theme.TerminalPalette

fun TerminalPalette.colorFor(style: LineStyle): Color = when (style) {
    LineStyle.COMMAND -> accent
    LineStyle.OUTPUT -> foreground
    LineStyle.SUCCESS -> accent
    LineStyle.ERROR -> error
    LineStyle.DIM -> muted
    LineStyle.HEADER -> accent
}

/** Scrollable command output that follows the newest line. */
@Composable
fun TerminalOutput(
    lines: List<TerminalLine>,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val palette = LocalTerminalPalette.current
    val listState = rememberLazyListState()

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.lastIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 12.dp,
            vertical = 8.dp,
        ),
    ) {
        items(items = lines) { line ->
            Text(
                // Blank lines still need to take up a row of height.
                text = line.text.ifEmpty { " " },
                style = MaterialTheme.typography.bodySmall,
                color = palette.colorFor(line.style),
            )
        }
        trailing?.let {
            item {
                Row(modifier = Modifier.padding(top = 2.dp)) { it() }
            }
        }
    }
}
