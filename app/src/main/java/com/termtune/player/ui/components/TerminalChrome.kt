package com.termtune.player.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.termtune.player.ui.theme.LocalTerminalPalette

/** A text-mode cursor that blinks the way a real terminal's does. */
@Composable
fun BlinkingCursor(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "cursor")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cursorAlpha",
    )
    Box(
        modifier = modifier
            .width(9.dp)
            .height(17.dp)
            .background(color.copy(alpha = alpha)),
    )
}

/** One `[term]`-style navigation chip. */
@Composable
private fun TerminalTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalTerminalPalette.current
    Box(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = if (selected) palette.accent else palette.border,
                shape = RoundedCornerShape(3.dp),
            )
            .background(
                color = if (selected) palette.accent.copy(alpha = 0.14f) else Color.Transparent,
                shape = RoundedCornerShape(3.dp),
            )
            .clickable(
                role = Role.Tab,
                onClickLabel = "Open $label section",
                onClick = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 5.dp),
    ) {
        Text(
            text = "[$label]",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) palette.accent else palette.muted,
        )
    }
}

/**
 * Shared window frame: title bar, section tabs and the screen's own content.
 *
 * Every screen sits inside this so the terminal aesthetic holds together while
 * navigation stays obvious.
 */
@Composable
fun TerminalScaffold(
    sectionTitle: String,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    statusLine: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalTerminalPalette.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .systemBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.surface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(palette.accent, RoundedCornerShape(4.dp)),
            )
            Text(
                text = "  TermTune",
                style = MaterialTheme.typography.titleMedium,
                color = palette.accent,
            )
            Text(
                text = " :: $sectionTitle",
                style = MaterialTheme.typography.labelMedium,
                color = palette.muted,
                modifier = Modifier.weight(1f),
            )
            statusLine?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.accentDim,
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.border),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.surface.copy(alpha = 0.6f))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TermTuneDestination.entries.forEach { destination ->
                TerminalTab(
                    label = destination.tabLabel,
                    selected = destination.route == currentRoute,
                    onClick = { onNavigate(destination.route) },
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.border),
        )

        content()
    }
}

/** The five required screens, in tab order. */
enum class TermTuneDestination(
    val route: String,
    val tabLabel: String,
    val title: String,
) {
    TERMINAL("terminal", "term", "terminal"),
    LIBRARY("library", "lib", "library"),
    NOW_PLAYING("nowplaying", "now", "now playing"),
    HELP("help", "help", "commands"),
    SETTINGS("settings", "cfg", "settings"),
}
