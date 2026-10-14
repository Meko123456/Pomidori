package io.github.meko123456.pomidori.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.meko123456.pomidori.timer.Phase
import io.github.meko123456.pomidori.timer.TimerStatus
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(onOpenSettings: () -> Unit = {}, vm: TimerViewModel = viewModel()) {
    val snapshot by vm.state.collectAsState()
    val tallyToday by vm.tallyToday.collectAsState()
    LifecycleResumeEffect(vm) {
        vm.recheckDay()
        onPauseOrDispose { }
    }
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
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            if (maxWidth > maxHeight) {
                // Side by side on a screen wider than it is tall. Stacked, a phone in landscape
                // left the ring a sliver of height between the labels and the buttons.
                val side = min(maxHeight, maxWidth * 0.5f)
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TimerRing(timer.remainingMillis, timer.progress, accent, Modifier.size(side))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PhaseLabels(phaseLabel(position.phase), tallyToday, accent)
                        Spacer(Modifier.height(24.dp))
                        TimerControls(timer.status, vm::reset, vm::primary, vm::skip)
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    PhaseLabels(phaseLabel(position.phase), tallyToday, accent)
                    // The ring takes what height is left, up to 80% of the width. At a fixed 80%
                    // of the width the column was taller than the screen at 200% text, and the
                    // buttons under the ring were cut off.
                    TimerRing(
                        timer.remainingMillis,
                        timer.progress,
                        accent,
                        Modifier.padding(vertical = 40.dp).weight(1f, fill = false).fillMaxWidth(0.8f),
                    )
                    TimerControls(timer.status, vm::reset, vm::primary, vm::skip, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun PhaseLabels(phase: String, tallyToday: Int, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = phase,
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
    }
}

/** The largest circle that fits the space [modifier] gives it, with the time left inside. */
@Composable
private fun TimerRing(remainingMillis: Long, progress: Float, accent: Color, modifier: Modifier) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val side = min(maxWidth, maxHeight)
        val track = MaterialTheme.colorScheme.surfaceVariant
        Canvas(modifier = Modifier.size(side)) {
            val stroke = Stroke(width = size.minDimension * 0.06f, cap = StrokeCap.Round)
            drawArc(color = track, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = stroke)
            // Remaining fraction, depleting clockwise from the top.
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = (1f - progress) * 360f,
                useCenter = false,
                style = stroke,
            )
        }
        // Inside the ring: full size where it fits, smaller in a small ring rather than spilling
        // over it.
        val display = MaterialTheme.typography.displayMedium
        Text(
            text = formatTime(remainingMillis),
            style = display,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            textAlign = TextAlign.Center,
            autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = display.fontSize),
            modifier = Modifier.width(side * 0.7f),
        )
    }
}

@Composable
private fun TimerControls(
    status: TimerStatus,
    onReset: () -> Unit,
    onPrimary: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Narrower side padding leaves "Resume" room on a 360dp-wide phone at 1.3x text.
    val padding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    // The widest the main button's label gets, so the layout doesn't jump as Start becomes Pause
    // and Resume.
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelLarge
    val widestLabel = remember(measurer, labelStyle) {
        listOf("Start", "Pause", "Resume").maxOf { measurer.measure(it, labelStyle, maxLines = 1, softWrap = false).size.width }
    }
    Layout(
        modifier = modifier,
        content = {
            OutlinedButton(onClick = onReset, contentPadding = padding) { Text("Reset") }
            Button(onClick = onPrimary, modifier = Modifier.heightIn(min = 48.dp), contentPadding = padding) {
                // Shrinks to fit rather than clipping, should even its own row be too narrow.
                Text(
                    primaryLabel(status),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = LocalTextStyle.current.fontSize),
                )
            }
            OutlinedButton(onClick = onSkip, contentPadding = padding) { Text("Skip") }
        },
    ) { measurables, constraints ->
        val gap = 12.dp.roundToPx()
        val reset = measurables[0].measure(Constraints())
        val skip = measurables[2].measure(Constraints())
        val sides = reset.width + skip.width + 2 * gap
        if (sides + widestLabel + 32.dp.roundToPx() <= constraints.maxWidth) {
            // One row. The main button takes the room the other two leave, up to 140dp: at a fixed
            // 140dp, Skip, measured last, got about 56dp on a 360dp-wide phone, and its label
            // wrapped one or two letters to a line.
            val primaryWidth = minOf(140.dp.roundToPx(), constraints.maxWidth - sides)
            val primary = measurables[1].measure(Constraints.fixedWidth(primaryWidth))
            val total = sides + primary.width
            val width = if (constraints.hasFixedWidth) constraints.maxWidth else total
            layout(width, maxOf(reset.height, primary.height, skip.height)) {
                var x = Alignment.CenterHorizontally.align(total, width, layoutDirection)
                reset.place(x, 0)
                x += reset.width + gap
                primary.place(x, 0)
                x += primary.width + gap
                skip.place(x, 0)
            }
        } else {
            // The main button on a row of its own above Reset and Skip. Kept in one row at large
            // text it got whatever the other two left, and "Start" shrank to 8sp: the action the
            // screen is for became the hardest word on it to read.
            val primaryWidth = minOf(maxOf(140.dp.roundToPx(), widestLabel + 32.dp.roundToPx()), constraints.maxWidth)
            val primary = measurables[1].measure(Constraints.fixedWidth(primaryWidth))
            val width = if (constraints.hasFixedWidth) constraints.maxWidth else maxOf(primary.width, sides - gap)
            val rowHeight = maxOf(reset.height, skip.height)
            layout(width, primary.height + gap + rowHeight) {
                primary.place(Alignment.CenterHorizontally.align(primary.width, width, layoutDirection), 0)
                var x = Alignment.CenterHorizontally.align(sides - gap, width, layoutDirection)
                reset.place(x, primary.height + gap)
                x += reset.width + gap
                skip.place(x, primary.height + gap)
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
