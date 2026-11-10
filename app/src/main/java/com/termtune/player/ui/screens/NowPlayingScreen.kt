package com.termtune.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.termtune.player.terminal.formatDuration
import com.termtune.player.terminal.formatProgressBar
import com.termtune.player.ui.TerminalUiState
import com.termtune.player.ui.components.PlayerActions
import com.termtune.player.ui.components.PlayerBar
import com.termtune.player.ui.theme.LocalTerminalPalette

/** Now Playing Interface: full track detail, ASCII meter and volume control. */
@Composable
fun NowPlayingScreen(
    state: TerminalUiState,
    playerActions: PlayerActions,
    onVolumeChanged: (Int) -> Unit,
) {
    val palette = LocalTerminalPalette.current
    val player = state.player
    val song = player.currentSong

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            text = "~/now_playing",
            style = MaterialTheme.typography.labelSmall,
            color = palette.muted,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .border(1.dp, palette.border, RoundedCornerShape(4.dp))
                .background(palette.surface, RoundedCornerShape(4.dp))
                .padding(14.dp),
        ) {
            if (song == null) {
                Column {
                    Text(
                        text = "[idle]",
                        style = MaterialTheme.typography.headlineSmall,
                        color = palette.muted,
                    )
                    Text(
                        text = "No track loaded.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.muted,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Text(
                        text = "Open [lib] and tap a song, or type",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.muted,
                    )
                    Text(
                        text = "'play 1' in the terminal.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.muted,
                    )
                }
            } else {
                Column {
                    Text(
                        text = "[${player.statusLabel}]",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.accent,
                    )
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = palette.foreground,
                        modifier = Modifier.padding(top = 6.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    InfoRow(label = "artist", value = song.artist)
                    InfoRow(label = "album", value = song.album)
                    InfoRow(
                        label = "time",
                        value = "${formatDuration(player.positionMs)} / " +
                            formatDuration(player.durationMs),
                    )
                    Text(
                        text = formatProgressBar(player.positionMs, player.durationMs, width = 18),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.accent,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                    Row(
                        modifier = Modifier.padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            text = "shuffle: ${if (player.shuffleEnabled) "on" else "off"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (player.shuffleEnabled) palette.accent else palette.muted,
                        )
                        Text(
                            text = "repeat: ${if (player.repeatEnabled) "on" else "off"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (player.repeatEnabled) palette.accent else palette.muted,
                        )
                        Text(
                            text = "queue: ${player.queueIndex + 1}/${player.queueSize}",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.muted,
                        )
                    }
                }
            }
        }

        Text(
            text = "volume: ${player.volumePercent}%",
            style = MaterialTheme.typography.labelSmall,
            color = palette.muted,
            modifier = Modifier.padding(top = 16.dp),
        )
        Slider(
            value = player.volumePercent / 100f,
            onValueChange = { onVolumeChanged((it * 100).toInt()) },
            colors = SliderDefaults.colors(
                thumbColor = palette.accent,
                activeTrackColor = palette.accentDim,
                inactiveTrackColor = palette.border,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Volume slider" },
        )

        Box(modifier = Modifier.padding(top = 10.dp)) {
            PlayerBar(state = player, actions = playerActions)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val palette = LocalTerminalPalette.current
    Row(modifier = Modifier.padding(top = 4.dp)) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = palette.accentDim,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = palette.foreground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
