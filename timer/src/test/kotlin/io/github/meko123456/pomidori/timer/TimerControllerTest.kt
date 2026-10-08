package io.github.meko123456.pomidori.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerControllerTest {

    @Test
    fun `finishing a phase auto-starts the next one running`() {
        TimerController.config = PomodoroConfig(focusMillis = 1_000, shortBreakMillis = 1_000, autoStartNext = true)
        TimerController.reset()
        TimerController.primary() // start running
        val before = TimerController.snapshot.position.phase

        val finished = TimerController.tick(2_000) // overshoot the phase

        assertTrue(finished)
        assertNotEquals(before, TimerController.snapshot.position.phase)
        assertEquals(TimerStatus.RUNNING, TimerController.snapshot.timer.status)
    }

    @Test
    fun `with auto-start off, the next phase waits idle`() {
        TimerController.config = PomodoroConfig(focusMillis = 1_000, shortBreakMillis = 1_000, autoStartNext = false)
        TimerController.reset()
        TimerController.primary()

        TimerController.tick(2_000)

        assertEquals(TimerStatus.IDLE, TimerController.snapshot.timer.status)
    }

    // Other tests leave the cycle on any phase, and reset() keeps it, so these work out what to
    // expect from the phase they find.
    private val shorter = PomodoroConfig(focusMillis = 5 * 60_000L, shortBreakMillis = 60_000L, longBreakMillis = 10 * 60_000L)
    private val longer = PomodoroConfig(focusMillis = 50 * 60_000L, shortBreakMillis = 10 * 60_000L, longBreakMillis = 30 * 60_000L)

    @Test
    fun `new lengths redraw an idle phase`() {
        TimerController.config = PomodoroConfig()
        TimerController.reset()

        TimerController.configure(shorter)

        val phase = TimerController.snapshot.position.phase
        assertEquals(TimerStatus.IDLE, TimerController.snapshot.timer.status)
        assertEquals(PomodoroCycle.duration(phase, shorter), TimerController.snapshot.timer.remainingMillis)
    }

    @Test
    fun `new lengths leave a session under way alone`() {
        TimerController.config = PomodoroConfig()
        TimerController.reset()
        TimerController.primary() // start
        val started = TimerController.snapshot.timer.totalMillis

        TimerController.configure(shorter)
        assertEquals(TimerStatus.RUNNING, TimerController.snapshot.timer.status)
        assertEquals(started, TimerController.snapshot.timer.totalMillis)

        TimerController.primary() // pause
        TimerController.configure(longer)
        assertEquals(TimerStatus.PAUSED, TimerController.snapshot.timer.status)
        assertEquals(started, TimerController.snapshot.timer.totalMillis)
    }

    @Test
    fun `primary toggles a running timer to paused`() {
        TimerController.config = PomodoroConfig()
        TimerController.reset()
        TimerController.primary() // start
        assertEquals(TimerStatus.RUNNING, TimerController.snapshot.timer.status)
        TimerController.primary() // pause
        assertEquals(TimerStatus.PAUSED, TimerController.snapshot.timer.status)
    }
}
