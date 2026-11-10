package com.termtune.player.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.termtune.player.ui.components.PlayerActions
import com.termtune.player.ui.components.TermTuneDestination
import com.termtune.player.ui.components.TerminalScaffold
import com.termtune.player.ui.screens.HelpScreen
import com.termtune.player.ui.screens.LibraryScreen
import com.termtune.player.ui.screens.NowPlayingScreen
import com.termtune.player.ui.screens.SettingsScreen
import com.termtune.player.ui.screens.TerminalScreen
import com.termtune.player.ui.theme.TermTuneTheme

/**
 * Hosts the five required screens inside the shared terminal frame.
 *
 * The frame (title bar + tabs) is drawn once, outside the [NavHost], so
 * switching sections never flashes the chrome.
 */
@Composable
fun TermTuneRoot(
    viewModel: TerminalViewModel,
    onRequestPermission: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
        ?: TermTuneDestination.TERMINAL.route
    val section = TermTuneDestination.entries
        .firstOrNull { it.route == currentRoute }
        ?: TermTuneDestination.TERMINAL

    val playerActions = PlayerActions(
        onPlayPause = viewModel::onPlayPauseClicked,
        onNext = viewModel::onNextClicked,
        onPrevious = viewModel::onPreviousClicked,
        onStop = viewModel::onStopClicked,
        onShuffle = viewModel::onShuffleToggled,
        onRepeat = viewModel::onRepeatToggled,
        onSeek = viewModel::onSeekBarMoved,
    )

    TermTuneTheme(theme = state.theme) {
        TerminalScaffold(
            sectionTitle = section.title,
            currentRoute = currentRoute,
            statusLine = state.player.statusLabel,
            onNavigate = { route ->
                if (route != currentRoute) {
                    navController.navigate(route) {
                        popUpTo(TermTuneDestination.TERMINAL.route) {
                            inclusive = route == TermTuneDestination.TERMINAL.route
                        }
                        launchSingleTop = true
                    }
                }
            },
        ) {
            NavHost(
                navController = navController,
                startDestination = TermTuneDestination.TERMINAL.route,
            ) {
                composable(TermTuneDestination.TERMINAL.route) {
                    TerminalScreen(
                        state = state,
                        playerActions = playerActions,
                        onSubmitCommand = { viewModel.runCommandLine(it) },
                        onClearHistory = viewModel::clearHistory,
                    )
                }
                composable(TermTuneDestination.LIBRARY.route) {
                    LibraryScreen(
                        state = state,
                        onSongSelected = viewModel::onSongSelected,
                        onRescan = viewModel::refreshLibraryFromUi,
                        onRequestPermission = onRequestPermission,
                    )
                }
                composable(TermTuneDestination.NOW_PLAYING.route) {
                    NowPlayingScreen(
                        state = state,
                        playerActions = playerActions,
                        onVolumeChanged = viewModel::onVolumeChanged,
                    )
                }
                composable(TermTuneDestination.HELP.route) {
                    HelpScreen()
                }
                composable(TermTuneDestination.SETTINGS.route) {
                    SettingsScreen(
                        state = state,
                        onThemeSelected = viewModel::onThemeSelected,
                        onShuffleToggled = viewModel::onShuffleToggled,
                        onRepeatToggled = viewModel::onRepeatToggled,
                        onRescan = viewModel::refreshLibraryFromUi,
                        onClearHistory = viewModel::clearHistory,
                        onClearScreen = viewModel::clearScreen,
                        onRequestPermission = onRequestPermission,
                    )
                }
            }
        }
    }
}
