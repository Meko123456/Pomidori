package io.github.meko123456.pomidori.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.meko123456.pomidori.data.SettingsRepository
import io.github.meko123456.pomidori.service.TimerController
import io.github.meko123456.pomidori.timer.PomodoroConfig
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs the settings screen and keeps [TimerController.config] in sync with the
 * persisted config so the timer always uses the latest durations.
 */
class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SettingsRepository(app)

    val config: StateFlow<PomodoroConfig> =
        repo.config.stateIn(viewModelScope, SharingStarted.Eagerly, PomodoroConfig())

    init {
        viewModelScope.launch { repo.config.collect { TimerController.config = it } }
    }

    fun setFocus(min: Int) = viewModelScope.launch { repo.setFocusMinutes(min) }
    fun setShortBreak(min: Int) = viewModelScope.launch { repo.setShortBreakMinutes(min) }
    fun setLongBreak(min: Int) = viewModelScope.launch { repo.setLongBreakMinutes(min) }
    fun setSessions(n: Int) = viewModelScope.launch { repo.setSessionsBeforeLongBreak(n) }
    fun setAutoStart(on: Boolean) = viewModelScope.launch { repo.setAutoStartNext(on) }
}
