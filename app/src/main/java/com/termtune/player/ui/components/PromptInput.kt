package com.termtune.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.termtune.player.ui.theme.LocalTerminalPalette

/**
 * The command prompt: `$ ` plus a monospace field, a blinking cursor while the
 * field is empty, and buttons for history recall and running the line.
 *
 * Android soft keyboards have no arrow keys, so history is recalled from the
 * `[hist]` panel instead of with Up/Down.
 */
@Composable
fun PromptInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onHistoryClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalTerminalPalette.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.background)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "$",
            style = MaterialTheme.typography.bodyLarge,
            color = palette.accent,
        )

        Box(modifier = Modifier.weight(1f)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = palette.foreground,
                ),
                cursorBrush = SolidColor(palette.accent),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Send,
                ),
                keyboardActions = KeyboardActions(onSend = { onSubmit() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Terminal command input" },
            )
            if (value.isEmpty()) {
                BlinkingCursor(color = palette.accent)
            }
        }

        PromptButton(label = "hist", description = "Show command history", onClick = onHistoryClicked)
        PromptButton(label = "run", description = "Run command", onClick = onSubmit, accent = true)
    }
}

@Composable
private fun PromptButton(
    label: String,
    description: String,
    onClick: () -> Unit,
    accent: Boolean = false,
) {
    val palette = LocalTerminalPalette.current
    Box(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = if (accent) palette.accent else palette.border,
                shape = RoundedCornerShape(3.dp),
            )
            .clickable(
                role = Role.Button,
                onClickLabel = description,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Text(
            text = "[$label]",
            style = MaterialTheme.typography.labelSmall,
            color = if (accent) palette.accent else palette.muted,
        )
    }
}
