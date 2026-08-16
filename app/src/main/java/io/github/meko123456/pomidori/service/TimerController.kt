package io.github.meko123456.pomidori.service

import io.github.meko123456.pomidori.timer.CyclePosition
import io.github.meko123456.pomidori.timer.PomodoroConfig
import io.github.meko123456.pomidori.timer.PomodoroCycle
import io.github.meko123456.pomidori.timer.TimerEngine
import io.github.meko123456.pomidori.timer.TimerState
import io.github.meko123456.pomidori.timer.TimerStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class TimerSnapshot(val position: CyclePosition, val timer: TimerState) {
    val isRunning: Boolean get() = timer.status == TimerStatus.RUNNING
}

/**
 * Process-wide single source of truth for the timer, shared between the
 * foreground [TimerService] (which advances it and owns the tick loop) and the
 * UI (which observes [state] and issues commands via the service). All
 * transitions delegate to the pure TimerEngine / PomodoroCycle.
 */
object TimerController {

    @Volatile
    var config: PomodoroConfig = PomodoroConfig()

    private val _state = MutableStateFlow(idle(PomodoroCycle.start()))
    val state: StateFlow<TimerSnapshot> = _state.asStateFlow()

    val snapshot: TimerSnapshot get() = _state.value

    private fun idle(position: CyclePosition) =
        TimerSnapshot(position, TimerState.idle(PomodoroCycle.duration(position.phase, config)))

    /** Primary control: start an idle/finished phase, or pause/resume a live one. */
    fun primary() = _state.update { s ->
        when (s.timer.status) {
            TimerStatus.IDLE, TimerStatus.FINISHED ->
                s.copy(timer = TimerEngine.start(PomodoroCycle.duration(s.position.phase, config)))
            TimerStatus.RUNNING -> s.copy(timer = TimerEngine.pause(s.timer))
            TimerStatus.PAUSED -> s.copy(timer = TimerEngine.resume(s.timer))
        }
    }

    fun reset() = _state.update { idle(it.position) }

    fun skip() = _state.update { idle(PomodoroCycle.next(it.position, config)) }

    /**
     * Advances the running timer by [deltaMillis]. Returns true if this tick just
     * finished the phase — in which case the state has already advanced to the
     * next phase (idle) so callers can chime / re-notify.
     */
    fun tick(deltaMillis: Long): Boolean {
        var finished = false
        _state.update { s ->
            val ticked = TimerEngine.tick(s.timer, deltaMillis)
            if (ticked.status == TimerStatus.FINISHED) {
                finished = true
                val next = PomodoroCycle.next(s.position, config)
                val duration = PomodoroCycle.duration(next.phase, config)
                val nextTimer = if (config.autoStartNext) TimerEngine.start(duration) else TimerState.idle(duration)
                TimerSnapshot(next, nextTimer)
            } else {
                s.copy(timer = ticked)
            }
        }
        return finished
    }
}
