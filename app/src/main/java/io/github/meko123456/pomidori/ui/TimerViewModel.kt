package io.github.meko123456.pomidori.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.github.meko123456.pomidori.service.TimerController
import io.github.meko123456.pomidori.service.TimerService
import io.github.meko123456.pomidori.service.TimerSnapshot
import kotlinx.coroutines.flow.StateFlow

/**
 * Thin bridge between the UI and the timer: observes the shared [TimerController]
 * state and forwards controls to the [TimerService], which owns the countdown so
 * it survives the screen turning off.
 */
class TimerViewModel(app: Application) : AndroidViewModel(app) {

    val state: StateFlow<TimerSnapshot> = TimerController.state

    fun primary() = TimerService.send(getApplication(), TimerService.ACTION_PRIMARY)
    fun reset() = TimerService.send(getApplication(), TimerService.ACTION_RESET)
    fun skip() = TimerService.send(getApplication(), TimerService.ACTION_SKIP)
}
