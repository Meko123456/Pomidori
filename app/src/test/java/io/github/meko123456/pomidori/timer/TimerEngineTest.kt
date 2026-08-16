package io.github.meko123456.pomidori.timer

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerEngineTest {

    private val minute = 60_000L

    @Test
    fun `start runs a full-duration countdown`() {
        val s = TimerEngine.start(minute)
        assertEquals(TimerStatus.RUNNING, s.status)
        assertEquals(minute, s.remainingMillis)
        assertEquals(0f, s.progress, 0.0001f)
    }

    @Test
    fun `tick reduces remaining and advances progress`() {
        val s = TimerEngine.tick(TimerEngine.start(minute), 15_000)
        assertEquals(45_000, s.remainingMillis)
        assertEquals(0.25f, s.progress, 0.0001f)
        assertEquals(TimerStatus.RUNNING, s.status)
    }

    @Test
    fun `tick past the end clamps at zero and finishes`() {
        val s = TimerEngine.tick(TimerEngine.start(minute), minute + 5_000)
        assertEquals(0, s.remainingMillis)
        assertEquals(TimerStatus.FINISHED, s.status)
        assertEquals(1f, s.progress, 0.0001f)
    }

    @Test
    fun `paused timer ignores ticks until resumed`() {
        val paused = TimerEngine.pause(TimerEngine.start(minute))
        assertEquals(TimerStatus.PAUSED, paused.status)
        val stillPaused = TimerEngine.tick(paused, 10_000)
        assertEquals(minute, stillPaused.remainingMillis)

        val resumed = TimerEngine.resume(paused)
        assertEquals(45_000, TimerEngine.tick(resumed, 15_000).remainingMillis)
    }

    @Test
    fun `toggle flips running and paused only`() {
        val running = TimerEngine.start(minute)
        assertEquals(TimerStatus.PAUSED, TimerEngine.toggle(running).status)
        assertEquals(TimerStatus.RUNNING, TimerEngine.toggle(TimerEngine.toggle(running)).status)
        val idle = TimerEngine.reset(minute)
        assertEquals(TimerStatus.IDLE, TimerEngine.toggle(idle).status)
    }

    @Test
    fun `reset returns an idle full timer and finished state ignores ticks`() {
        val reset = TimerEngine.reset(minute)
        assertEquals(TimerStatus.IDLE, reset.status)
        assertEquals(minute, reset.remainingMillis)

        val finished = TimerEngine.tick(TimerEngine.start(1_000), 1_000)
        assertEquals(finished, TimerEngine.tick(finished, 5_000)) // no further change
    }

    @Test
    fun `non-positive delta is a no-op`() {
        val running = TimerEngine.start(minute)
        assertEquals(running, TimerEngine.tick(running, 0))
        assertEquals(running, TimerEngine.tick(running, -1_000))
    }
}
