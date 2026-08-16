package io.github.meko123456.pomidori.timer

enum class TimerStatus { IDLE, RUNNING, PAUSED, FINISHED }

/**
 * Immutable snapshot of a single countdown. Everything the UI needs is derived
 * here so the engine and views agree exactly.
 */
data class TimerState(
    val totalMillis: Long,
    val remainingMillis: Long,
    val status: TimerStatus = TimerStatus.IDLE,
) {
    val elapsedMillis: Long get() = (totalMillis - remainingMillis).coerceAtLeast(0)

    /** 0f..1f fraction elapsed, safe when total is 0. */
    val progress: Float get() = if (totalMillis <= 0L) 0f else (elapsedMillis.toFloat() / totalMillis).coerceIn(0f, 1f)

    val isRunning: Boolean get() = status == TimerStatus.RUNNING
    val isPaused: Boolean get() = status == TimerStatus.PAUSED

    companion object {
        fun idle(totalMillis: Long) = TimerState(totalMillis.coerceAtLeast(0), totalMillis.coerceAtLeast(0), TimerStatus.IDLE)
    }
}
