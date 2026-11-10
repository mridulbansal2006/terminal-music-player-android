package com.termtune.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.termtune.player.data.MediaPermissions
import com.termtune.player.ui.TermTuneRoot
import com.termtune.player.ui.TerminalViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind the system bars; the terminal frame insets itself.
        enableEdgeToEdge()

        setContent {
            val terminalViewModel: TerminalViewModel = viewModel()

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions(),
            ) {
                // The view model re-reads the real grant state rather than
                // trusting the result map, which omits already-granted entries.
                terminalViewModel.onPermissionResult()
            }

            // Ask once on first launch so `list` works without a detour.
            LaunchedEffect(Unit) {
                if (!MediaPermissions.hasAudioAccess(this@MainActivity)) {
                    permissionLauncher.launch(MediaPermissions.requestedPermissions())
                }
            }

            TermTuneRoot(
                viewModel = terminalViewModel,
                onRequestPermission = {
                    permissionLauncher.launch(MediaPermissions.requestedPermissions())
                },
            )
        }
    }
}
