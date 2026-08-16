package io.github.meko123456.pomidori.ui

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.meko123456.pomidori.timer.CyclePosition
import io.github.meko123456.pomidori.timer.PomodoroCycle
import io.github.meko123456.pomidori.timer.PomodoroConfig
import io.github.meko123456.pomidori.timer.TimerEngine
import io.github.meko123456.pomidori.timer.TimerState
import io.github.meko123456.pomidori.timer.TimerStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Drives the in-app countdown: ticks the pure [TimerEngine] from the monotonic
 * clock while running, and advances the [PomodoroCycle] when a phase finishes.
 * (The foreground service in a later issue reuses this same engine so a session
 * keeps running with the screen off.)
 */
class TimerViewModel : ViewModel() {

    // Settings arrive in a later issue; classic defaults for now.
    private val config = PomodoroConfig()

    var position by mutableStateOf(PomodoroCycle.start())
        private set
    var timer by mutableStateOf(TimerState.idle(phaseDuration()))
        private set

    private var tickJob: Job? = null
    private var lastMark = 0L

    private fun phaseDuration() = PomodoroCycle.duration(position.phase, config)

    /** Primary button: start the current phase, or pause/resume a live one. */
    fun toggle() {
        when (timer.status) {
            TimerStatus.IDLE -> {
                timer = TimerEngine.start(phaseDuration())
                startLoop()
            }
            TimerStatus.RUNNING -> {
                timer = TimerEngine.pause(timer)
                stopLoop()
            }
            TimerStatus.PAUSED -> {
                timer = TimerEngine.resume(timer)
                startLoop()
            }
            TimerStatus.FINISHED -> Unit
        }
    }

    /** Restart the current phase from full. */
    fun reset() {
        stopLoop()
        timer = TimerEngine.reset(phaseDuration())
    }

    /** Skip to the next phase in the cycle, idle at its full duration. */
    fun skip() {
        stopLoop()
        position = PomodoroCycle.next(position, config)
        timer = TimerEngine.reset(phaseDuration())
    }

    private fun startLoop() {
        stopLoop()
        lastMark = SystemClock.elapsedRealtime()
        tickJob = viewModelScope.launch {
            while (timer.status == TimerStatus.RUNNING) {
                delay(200)
                val now = SystemClock.elapsedRealtime()
                val delta = now - lastMark
                lastMark = now
                timer = TimerEngine.tick(timer, delta)
                if (timer.status == TimerStatus.FINISHED) onPhaseFinished()
            }
        }
    }

    private fun onPhaseFinished() {
        stopLoop()
        position = PomodoroCycle.next(position, config)
        timer = TimerEngine.reset(phaseDuration())
    }

    private fun stopLoop() {
        tickJob?.cancel()
        tickJob = null
    }

    override fun onCleared() {
        stopLoop()
    }
}
