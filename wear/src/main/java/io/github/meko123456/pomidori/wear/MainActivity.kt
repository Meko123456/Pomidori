package io.github.meko123456.pomidori.wear

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import io.github.meko123456.pomidori.timer.TimerController
import io.github.meko123456.pomidori.timer.TimerSnapshot
import io.github.meko123456.pomidori.wear.tile.TileContent

/**
 * The watch's timer screen, and the way in from the tile.
 *
 * A tile cannot start a foreground service itself, and an activity the user just opened can, so the
 * tile's button launches this with [EXTRA_COMMAND] and the command is passed on from here.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Only on a fresh launch: after a configuration change the launch intent comes back with
        // the same extra, and running it twice would pause what the first tap started.
        if (savedInstanceState == null) handle(intent)
        askForNotifications()

        setContent {
            MaterialTheme {
                val snapshot by TimerController.state.collectAsState()
                TimerScreen(
                    snapshot = snapshot,
                    onPrimary = { send(WatchTimerService.ACTION_PRIMARY) },
                    onSkip = { send(WatchTimerService.ACTION_SKIP) },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        if (intent?.getStringExtra(EXTRA_COMMAND) == COMMAND_PRIMARY) send(WatchTimerService.ACTION_PRIMARY)
    }

    private fun send(action: String) {
        startForegroundService(Intent(this, WatchTimerService::class.java).setAction(action))
    }

    /**
     * The ongoing notification is how the countdown reaches the watch face. Asked for from Wear OS 4
     * (API 33), where it became a runtime permission; older watches grant it with the install.
     */
    private fun askForNotifications() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }
    }

    companion object {
        const val EXTRA_COMMAND = "io.github.meko123456.pomidori.wear.COMMAND"
        const val COMMAND_PRIMARY = "primary"
    }
}

@Composable
private fun TimerScreen(snapshot: TimerSnapshot, onPrimary: () -> Unit, onSkip: () -> Unit) {
    // The tile's words, for the same state, so the two never describe it differently.
    val content = TileContent.of(snapshot, nowEpochMillis = 0)
    AppScaffold {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { snapshot.timer.progress },
                modifier = Modifier.fillMaxSize().padding(2.dp),
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(content.title, style = MaterialTheme.typography.titleSmall)
                Text(
                    content.time,
                    style = MaterialTheme.typography.numeralMedium,
                    modifier = Modifier.semantics { contentDescription = "${content.time} left" },
                )
                Button(onClick = onPrimary) { Text(content.action) }
                CompactButton(onClick = onSkip) { Text("Skip") }
            }
        }
    }
}
