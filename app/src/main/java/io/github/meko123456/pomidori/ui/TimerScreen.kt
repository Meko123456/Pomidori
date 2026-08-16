package io.github.meko123456.pomidori.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.meko123456.pomidori.timer.Phase
import io.github.meko123456.pomidori.timer.TimerStatus
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(onOpenSettings: () -> Unit = {}, vm: TimerViewModel = viewModel()) {
    val snapshot by vm.state.collectAsState()
    val tallyToday by vm.tallyToday.collectAsState()
    val timer = snapshot.timer
    val position = snapshot.position
    val accent = when (position.phase) {
        Phase.FOCUS -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = phaseLabel(position.phase),
                style = MaterialTheme.typography.titleLarge,
                color = accent,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "🍅 $tallyToday focus ${if (tallyToday == 1) "session" else "sessions"} today",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            Box(
                modifier = Modifier.padding(vertical = 40.dp).fillMaxWidth(0.8f).aspectRatio(1f),
                contentAlignment = Alignment.Center,
            ) {
                val track = MaterialTheme.colorScheme.surfaceVariant
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = Stroke(width = size.minDimension * 0.06f, cap = StrokeCap.Round)
                    drawArc(color = track, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = stroke)
                    // Remaining fraction, depleting clockwise from the top.
                    drawArc(
                        color = accent,
                        startAngle = -90f,
                        sweepAngle = (1f - timer.progress) * 360f,
                        useCenter = false,
                        style = stroke,
                    )
                }
                Text(
                    text = formatTime(timer.remainingMillis),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Medium,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            ) {
                OutlinedButton(onClick = vm::reset) { Text("Reset") }
                Button(onClick = vm::primary, modifier = Modifier.size(width = 140.dp, height = 48.dp)) {
                    Text(primaryLabel(timer.status))
                }
                OutlinedButton(onClick = vm::skip) { Text("Skip") }
            }
        }
    }
}

private fun phaseLabel(phase: Phase): String = when (phase) {
    Phase.FOCUS -> "Focus"
    Phase.SHORT_BREAK -> "Short break"
    Phase.LONG_BREAK -> "Long break"
}

private fun primaryLabel(status: TimerStatus): String = when (status) {
    TimerStatus.RUNNING -> "Pause"
    TimerStatus.PAUSED -> "Resume"
    else -> "Start"
}

/** mm:ss, rounding up so a fresh 25-minute timer reads 25:00 and the final second shows 0:01. */
private fun formatTime(remainingMillis: Long): String {
    val totalSeconds = ceil(remainingMillis / 1000.0).toLong()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
