package com.termtune.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.termtune.player.data.MediaPermissions
import com.termtune.player.ui.TerminalUiState
import com.termtune.player.ui.theme.LocalTerminalPalette
import com.termtune.player.ui.theme.TerminalTheme
import com.termtune.player.ui.theme.palette

/** Settings Screen: theme, playback modes, library and permission status. */
@Composable
fun SettingsScreen(
    state: TerminalUiState,
    onThemeSelected: (TerminalTheme) -> Unit,
    onShuffleToggled: () -> Unit,
    onRepeatToggled: () -> Unit,
    onRescan: () -> Unit,
    onClearHistory: () -> Unit,
    onClearScreen: () -> Unit,
    onRequestPermission: () -> Unit,
) {
    val palette = LocalTerminalPalette.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        SectionTitle("terminal theme")
        Text(
            text = "also settable with: theme green|amber|blue",
            style = MaterialTheme.typography.labelSmall,
            color = palette.muted,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        TerminalTheme.entries.forEach { option ->
            ThemeRow(
                option = option,
                selected = option == state.theme,
                onClick = { onThemeSelected(option) },
            )
        }

        SectionTitle("playback", top = 22.dp)
        ToggleRow(
            label = "shuffle",
            enabled = state.player.shuffleEnabled,
            onClick = onShuffleToggled,
        )
        ToggleRow(
            label = "repeat",
            enabled = state.player.repeatEnabled,
            onClick = onRepeatToggled,
        )

        SectionTitle("library", top = 22.dp)
        InfoLine("tracks", "${state.library.size}")
        InfoLine(
            "permission",
            if (state.permissionGranted) "granted" else "denied",
        )
        InfoLine("scope", MediaPermissions.audioPermissionLabel())
        Row(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ActionChip(label = "rescan", onClick = onRescan, accent = true)
            if (!state.permissionGranted) {
                ActionChip(label = "grant", onClick = onRequestPermission)
            }
        }

        SectionTitle("terminal", top = 22.dp)
        InfoLine("history entries", "${state.history.size}")
        Row(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ActionChip(label = "clear history", onClick = onClearHistory)
            ActionChip(label = "clear screen", onClick = onClearScreen)
        }

        SectionTitle("about", top = 22.dp)
        InfoLine("app", "TermTune 1.0")
        InfoLine("engine", "AndroidX Media3 ExoPlayer")
        InfoLine("ui", "Jetpack Compose")
        InfoLine("course", "Application Development")
        Text(
            text = "Permissions requested: audio read, " +
                "notifications, media playback foreground service.",
            style = MaterialTheme.typography.labelSmall,
            color = palette.muted,
            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
        )
    }
}

@Composable
private fun SectionTitle(text: String, top: androidx.compose.ui.unit.Dp = 0.dp) {
    val palette = LocalTerminalPalette.current
    Text(
        text = "## $text",
        style = MaterialTheme.typography.titleMedium,
        color = palette.accent,
        modifier = Modifier.padding(top = top, bottom = 6.dp),
    )
}

@Composable
private fun ThemeRow(
    option: TerminalTheme,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalTerminalPalette.current
    val optionPalette = option.palette

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (selected) palette.accent else palette.border,
                RoundedCornerShape(3.dp),
            )
            .clickable(
                role = Role.RadioButton,
                onClickLabel = "Use ${option.label} theme",
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (selected) "[x]" else "[ ]",
            style = MaterialTheme.typography.bodySmall,
            color = palette.accent,
        )
        Text(
            text = "  ${option.key}",
            style = MaterialTheme.typography.bodySmall,
            color = palette.foreground,
        )
        Text(
            text = "  ${option.label}",
            style = MaterialTheme.typography.labelSmall,
            color = palette.muted,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(optionPalette.accent, RoundedCornerShape(2.dp)),
        )
    }
    Box(modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun ToggleRow(label: String, enabled: Boolean, onClick: () -> Unit) {
    val palette = LocalTerminalPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Switch,
                onClickLabel = "Toggle $label",
                onClick = onClick,
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (enabled) "[x]" else "[ ]",
            style = MaterialTheme.typography.bodySmall,
            color = palette.accent,
        )
        Text(
            text = "  $label",
            style = MaterialTheme.typography.bodySmall,
            color = palette.foreground,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (enabled) "on" else "off",
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) palette.accent else palette.muted,
        )
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    val palette = LocalTerminalPalette.current
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = palette.accentDim,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = palette.foreground,
        )
    }
}
