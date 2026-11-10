package com.termtune.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.termtune.player.R
import com.termtune.player.player.PlayerState
import com.termtune.player.terminal.formatDuration
import com.termtune.player.ui.theme.LocalTerminalPalette

/** Callbacks shared by the compact player bar and the Now Playing screen. */
data class PlayerActions(
    val onPlayPause: () -> Unit,
    val onNext: () -> Unit,
    val onPrevious: () -> Unit,
    val onStop: () -> Unit,
    val onShuffle: () -> Unit,
    val onRepeat: () -> Unit,
    val onSeek: (Long) -> Unit,
)

/**
 * The compact music-control area: current track, transport buttons, seek bar
 * and progress times. Backed by the same [PlayerState] the commands mutate.
 */
@Composable
fun PlayerBar(
    state: PlayerState,
    actions: PlayerActions,
    modifier: Modifier = Modifier,
) {
    val palette = LocalTerminalPalette.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "[${state.statusLabel}]",
                style = MaterialTheme.typography.labelSmall,
                color = palette.accent,
            )
            Text(
                text = "  ${state.currentSong?.title ?: "no track loaded"}",
                style = MaterialTheme.typography.bodySmall,
                color = if (state.currentSong == null) palette.muted else palette.foreground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        state.currentSong?.let { song ->
            Text(
                text = "${song.artist} - ${song.album}",
                style = MaterialTheme.typography.labelSmall,
                color = palette.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        SeekRow(state = state, onSeek = actions.onSeek)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TransportButton(
                icon = Icons.Filled.SkipPrevious,
                label = stringResource(R.string.cd_previous),
                onClick = actions.onPrevious,
                tint = palette.foreground,
            )
            TransportButton(
                icon = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                label = stringResource(
                    if (state.isPlaying) R.string.cd_pause else R.string.cd_play,
                ),
                onClick = actions.onPlayPause,
                tint = palette.accent,
                testTag = "playPause",
            )
            TransportButton(
                icon = Icons.Filled.SkipNext,
                label = stringResource(R.string.cd_next),
                onClick = actions.onNext,
                tint = palette.foreground,
            )
            TransportButton(
                icon = Icons.Filled.Stop,
                label = stringResource(R.string.cd_stop),
                onClick = actions.onStop,
                tint = palette.foreground,
            )
            TransportButton(
                icon = Icons.Filled.Shuffle,
                label = stringResource(R.string.cd_shuffle),
                onClick = actions.onShuffle,
                tint = if (state.shuffleEnabled) palette.accent else palette.muted,
            )
            TransportButton(
                icon = Icons.Filled.Repeat,
                label = stringResource(R.string.cd_repeat),
                onClick = actions.onRepeat,
                tint = if (state.repeatEnabled) palette.accent else palette.muted,
            )
        }
    }
}

/** Seek bar plus the `1:12 / 5:55` read-out. */
@Composable
fun SeekRow(
    state: PlayerState,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalTerminalPalette.current
    var dragFraction by remember { mutableStateOf<Float?>(null) }

    val durationMs = state.durationMs.coerceAtLeast(0L)
    val liveFraction = if (durationMs <= 0L) {
        0f
    } else {
        (state.positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    }
    val fraction = dragFraction ?: liveFraction
    val shownPosition = if (dragFraction != null) {
        (fraction * durationMs).toLong()
    } else {
        state.positionMs
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = fraction,
            onValueChange = { dragFraction = it },
            onValueChangeFinished = {
                dragFraction?.let { onSeek((it * durationMs).toLong()) }
                dragFraction = null
            },
            enabled = state.hasQueue && durationMs > 0L,
            colors = SliderDefaults.colors(
                thumbColor = palette.accent,
                activeTrackColor = palette.accent,
                inactiveTrackColor = palette.border,
                disabledThumbColor = palette.muted,
                disabledActiveTrackColor = palette.border,
                disabledInactiveTrackColor = palette.border,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .semantics { contentDescription = "Playback position slider" },
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = formatDuration(shownPosition),
                style = MaterialTheme.typography.labelSmall,
                color = palette.accentDim,
            )
            Box(modifier = Modifier.weight(1f))
            Text(
                text = formatDuration(durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = palette.accentDim,
            )
        }
    }
}

@Composable
private fun TransportButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color,
    testTag: String? = null,
) {
    IconButton(
        onClick = onClick,
        modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}
