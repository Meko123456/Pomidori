package io.github.meko123456.pomidori.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, vm: SettingsViewModel = viewModel()) {
    val config by vm.config.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            MinutesSlider("Focus", (config.focusMillis / 60_000).toInt(), 5..60, vm::setFocus)
            MinutesSlider("Short break", (config.shortBreakMillis / 60_000).toInt(), 1..30, vm::setShortBreak)
            MinutesSlider("Long break", (config.longBreakMillis / 60_000).toInt(), 5..45, vm::setLongBreak)
            Stepper("Sessions before a long break", config.sessionsBeforeLongBreak, 2..8, vm::setSessions)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Auto-start next phase", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = config.autoStartNext, onCheckedChange = vm::setAutoStart)
            }
        }
    }
}

@Composable
private fun MinutesSlider(label: String, minutes: Int, range: IntRange, onCommit: (Int) -> Unit) {
    var value by remember(minutes) { mutableIntStateOf(minutes) }
    Column {
        Text("$label — $value min", style = MaterialTheme.typography.bodyLarge)
        Slider(
            value = value.toFloat(),
            onValueChange = { value = it.toInt() },
            onValueChangeFinished = { onCommit(value) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = (range.last - range.first - 1).coerceAtLeast(0),
        )
    }
}

@Composable
private fun Stepper(label: String, value: Int, range: IntRange, onCommit: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = { if (value > range.first) onCommit(value - 1) }, enabled = value > range.first) { Text("−") }
        Text("  $value  ", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = { if (value < range.last) onCommit(value + 1) }, enabled = value < range.last) { Text("+") }
    }
}
