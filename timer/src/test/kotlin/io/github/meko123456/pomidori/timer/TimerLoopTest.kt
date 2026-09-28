package io.github.meko123456.pomidori.timer

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimerLoopTest {

    /**
     * Starts a focus phase under [config], with no long break within reach.
     *
     * TimerController is one per process, and neither reset() nor anything else takes it back to the
     * start of the cycle: each test inherits the position and the count of focus sessions the last
     * one left. So this walks it back to focus, and moves the long break out of the way of whatever
     * that count has reached.
     */
    private fun start(config: PomodoroConfig) {
        TimerController.config = config.copy(sessionsBeforeLongBreak = Int.MAX_VALUE)
        while (TimerController.snapshot.position.phase != Phase.FOCUS) TimerController.skip()
        TimerController.reset()
        TimerController.primary()
    }

    private fun TestScope.virtualClock(): () -> Long = { testScheduler.currentTime }

    @Test
    fun `a phase runs its length, ends once, and with auto-start off the loop returns`() = runTest {
        start(PomodoroConfig(focusMillis = 1_000, shortBreakMillis = 500, autoStartNext = false))
        val ended = mutableListOf<Phase>()

        TimerLoop(virtualClock()).run(onPhaseEnd = { ended += it })

        assertEquals(listOf(Phase.FOCUS), ended)
        assertEquals(1_000L, testScheduler.currentTime)
        assertEquals(Phase.SHORT_BREAK, TimerController.snapshot.position.phase)
        assertEquals(TimerStatus.IDLE, TimerController.snapshot.timer.status)
    }

    @Test
    fun `with auto-start on it carries straight on into the next phases`() = runTest {
        start(PomodoroConfig(focusMillis = 1_000, shortBreakMillis = 500, autoStartNext = true))
        val ended = mutableListOf<Phase>()

        val loop = launch { TimerLoop(virtualClock()).run(onPhaseEnd = { ended += it }) }
        advanceTimeBy(2_600)
        runCurrent()
        loop.cancel()

        assertEquals(listOf(Phase.FOCUS, Phase.SHORT_BREAK, Phase.FOCUS), ended)
        assertTrue(TimerController.snapshot.isRunning)
    }

    @Test
    fun `time is what the clock says elapsed, not the number of ticks`() = runTest {
        start(PomodoroConfig(focusMillis = 10_000, autoStartNext = false))
        // Every delay overruns by 750 ms, as a busy or dozing device will make it: four ticks are
        // four seconds, not one.
        var overrun = 0L
        val clock = { testScheduler.currentTime + overrun }
        var ticks = 0

        val loop = launch { TimerLoop(clock).run(onTick = { ticks++; overrun += 750 }) }
        advanceTimeBy(1_000)
        runCurrent()
        loop.cancel()

        assertEquals(4, ticks)
        assertEquals(10_000L - 4 * 250 - 3 * 750, TimerController.snapshot.timer.remainingMillis)
    }

    @Test
    fun `reacting to a phase end is not charged to the phase that follows`() = runTest {
        start(PomodoroConfig(focusMillis = 500, shortBreakMillis = 10_000, autoStartNext = true))
        var extra = 0L
        val clock = { testScheduler.currentTime + extra }

        // The chime and the notification take a second of the clock between the phase ending and
        // the next one being measured.
        val loop = launch { TimerLoop(clock).run(onPhaseEnd = { extra += 1_000 }) }
        advanceTimeBy(1_000)
        runCurrent()
        loop.cancel()

        assertEquals(Phase.SHORT_BREAK, TimerController.snapshot.position.phase)
        assertEquals(10_000L - 500, TimerController.snapshot.timer.remainingMillis)
    }

    @Test
    fun `pausing ends the loop at its next tick`() = runTest {
        start(PomodoroConfig(focusMillis = 60_000, autoStartNext = false))

        val loop = launch { TimerLoop(virtualClock()).run() }
        advanceTimeBy(1_000)
        TimerController.primary() // pause
        runCurrent()
        advanceTimeBy(250)
        runCurrent()

        assertTrue(loop.isCompleted)
        assertEquals(TimerStatus.PAUSED, TimerController.snapshot.timer.status)
    }
}
