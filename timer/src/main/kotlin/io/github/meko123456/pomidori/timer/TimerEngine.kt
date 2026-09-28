package io.github.meko123456.pomidori.timer

/**
 * Pure countdown transitions on an immutable [TimerState]. The engine never
 * reads a clock itself — callers advance it with [tick] and the actual elapsed
 * delta — so behaviour is fully deterministic and unit-tested without real time
 * (the foreground service supplies real deltas from the monotonic clock).
 */
object TimerEngine {

    fun start(totalMillis: Long): TimerState {
        val total = totalMillis.coerceAtLeast(0)
        return TimerState(total, total, TimerStatus.RUNNING)
    }

    fun pause(state: TimerState): TimerState =
        if (state.status == TimerStatus.RUNNING) state.copy(status = TimerStatus.PAUSED) else state

    fun resume(state: TimerState): TimerState =
        if (state.status == TimerStatus.PAUSED) state.copy(status = TimerStatus.RUNNING) else state

    /** Toggles between running and paused; no-op for idle/finished. */
    fun toggle(state: TimerState): TimerState = when (state.status) {
        TimerStatus.RUNNING -> pause(state)
        TimerStatus.PAUSED -> resume(state)
        else -> state
    }

    fun reset(totalMillis: Long): TimerState = TimerState.idle(totalMillis)

    /**
     * Advances a running timer by [deltaMillis]. No-op unless RUNNING or when
     * delta is non-positive. Clamps remaining at 0 and flips to FINISHED there.
     */
    fun tick(state: TimerState, deltaMillis: Long): TimerState {
        if (state.status != TimerStatus.RUNNING || deltaMillis <= 0L) return state
        val remaining = (state.remainingMillis - deltaMillis).coerceAtLeast(0L)
        return if (remaining == 0L) {
            state.copy(remainingMillis = 0L, status = TimerStatus.FINISHED)
        } else {
            state.copy(remainingMillis = remaining)
        }
    }
}
