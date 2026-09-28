package io.github.meko123456.pomidori.timer

import kotlinx.coroutines.delay

/**
 * Advances [TimerController] in real time while it runs: the loop the phone's and the watch's
 * foreground services each used to carry a copy of, untested in both.
 *
 * Every [tickMillis] it moves the controller on by what [clock] says actually elapsed, not by the
 * nominal tick — a delay can overrun, and a countdown built on nominal ticks drifts. When a phase
 * ends it starts measuring the next one from that moment, so the time spent reacting to the end
 * (a chime, a buzz, a notification) is not charged to it.
 *
 * [clock] is milliseconds from a monotonic source: `SystemClock.elapsedRealtime` in the services,
 * the test scheduler's virtual time in the tests.
 */
class TimerLoop(
    private val clock: () -> Long,
    private val tickMillis: Long = 250,
) {
    /**
     * Runs until the timer stops running — paused, reset, or a phase ended with auto-start off —
     * and returns. A caller that pauses the timer from elsewhere can also simply cancel it.
     *
     * [onPhaseEnd] is told which phase just ended; by then the controller has already moved to the
     * next one, running or idle. [onTick] follows every other tick.
     */
    suspend fun run(onPhaseEnd: (ended: Phase) -> Unit = {}, onTick: () -> Unit = {}) {
        var lastMark = clock()
        while (TimerController.snapshot.isRunning) {
            delay(tickMillis)
            val now = clock()
            val ending = TimerController.snapshot.position.phase
            val finished = TimerController.tick(now - lastMark)
            lastMark = now
            if (finished) {
                onPhaseEnd(ending)
                lastMark = clock()
            } else {
                onTick()
            }
        }
    }
}
