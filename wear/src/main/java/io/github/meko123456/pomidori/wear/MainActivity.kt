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
import io.github.meko123456.pomidori.timer.NotificationContent
import io.github.meko123456.pomidori.timer.TimerController
import io.github.meko123456.pomidori.timer.TimerSnapshot
import io.github.meko123456.pomidori.timer.TimerStatus

/** The watch's timer screen: the phase, the time left, and the buttons that drive the service. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
}

@Composable
private fun TimerScreen(snapshot: TimerSnapshot, onPrimary: () -> Unit, onSkip: () -> Unit) {
    val content = NotificationContent.of(snapshot)
    val action = when (snapshot.timer.status) {
        TimerStatus.IDLE, TimerStatus.FINISHED -> "Start"
        TimerStatus.RUNNING -> "Pause"
        TimerStatus.PAUSED -> "Resume"
    }
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
                Button(onClick = onPrimary) { Text(action) }
                CompactButton(onClick = onSkip) { Text("Skip") }
            }
        }
    }
}
