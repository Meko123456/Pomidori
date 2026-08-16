package io.github.meko123456.pomidori.service

import io.github.meko123456.pomidori.timer.PomodoroConfig
import io.github.meko123456.pomidori.timer.TimerStatus
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
