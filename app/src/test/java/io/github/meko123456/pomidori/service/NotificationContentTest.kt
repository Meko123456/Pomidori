package io.github.meko123456.pomidori.service

import io.github.meko123456.pomidori.timer.Phase
import io.github.meko123456.pomidori.timer.PomodoroConfig
import io.github.meko123456.pomidori.timer.TimerStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The countdown ticks four times a second so a phase boundary is noticed promptly, and shows whole
 * seconds. Those two facts together are why the service used to rebuild and re-post an identical
 * notification three times out of four, and why this type exists.
 */
class NotificationContentTest {

    /**
     * A running focus phase, from a known start.
     *
     * The winding is not ceremony. [TimerController] is an `object`, and its `reset()` keeps the
     * current phase rather than returning to the beginning, so a test that ran before this one can
     * leave the cycle parked on a break — and then `focusMillis` is not the duration in play and
     * every assertion here is about the wrong phase. Two of these tests failed exactly that way
     * before this loop existed.
     */
    private fun running(focusMillis: Long = 25 * 60 * 1000L) {
        TimerController.config = PomodoroConfig(focusMillis = focusMillis, autoStartNext = false)
        TimerController.reset()
        var guard = 0
        while (TimerController.snapshot.position.phase != Phase.FOCUS && guard++ < 16) {
            TimerController.skip()
        }
        TimerController.reset()
        TimerController.primary()
    }

    @Test
    fun `three ticks in every four change nothing a person could see`() {
        running()
        val seen = (1..4).map {
            TimerController.tick(250)
            NotificationContent.of(TimerController.snapshot)
        }

        // The first three land inside the same displayed second; the fourth crosses it.
        assertEquals("three of four should be identical: $seen", 3, seen.count { it == seen.first() })
        assertEquals("only the tick that crosses a second should differ: $seen", 2, seen.toSet().size)
        assertNotEquals(seen.first(), seen.last())
    }

    @Test
    fun `crossing a second is the tick that changes something`() {
        running()
        val before = NotificationContent.of(TimerController.snapshot)
        repeat(4) { TimerController.tick(250) }
        val after = NotificationContent.of(TimerController.snapshot)

        assertNotEquals("a second passing must be visible", before, after)
    }

    @Test
    fun `a whole pomodoro renders one screen per second, not four`() {
        // The measurement the fix is for: 60 ticks at 250 ms is 15 seconds, and 15 is the number of
        // notifications a person could tell apart.
        running()
        val seen = mutableSetOf<NotificationContent>()
        seen += NotificationContent.of(TimerController.snapshot)
        repeat(60) {
            TimerController.tick(250)
            seen += NotificationContent.of(TimerController.snapshot)
        }

        assertEquals(16, seen.size) // the starting screen plus fifteen seconds
    }

    @Test
    fun `pausing changes the screen even though the time does not`() {
        // Worth pinning: skipping unchanged content must not swallow a state change that happens to
        // land on the same second.
        running()
        val beforePause = NotificationContent.of(TimerController.snapshot)
        TimerController.primary() // pause
        val afterPause = NotificationContent.of(TimerController.snapshot)

        assertEquals(TimerStatus.PAUSED, TimerController.snapshot.timer.status)
        assertEquals(beforePause.time, afterPause.time)
        assertNotEquals(beforePause, afterPause)
        assertTrue(afterPause.title.endsWith("paused"))
    }

    @Test
    fun `the phase name is part of what is shown`() {
        running(focusMillis = 1_000)
        val focus = NotificationContent.of(TimerController.snapshot)
        TimerController.tick(2_000) // finish the phase; auto-start is off
        val next = NotificationContent.of(TimerController.snapshot)

        assertNotEquals(focus.title, next.title)
        assertTrue(focus.title.startsWith("Focus"))
    }

    @Test
    fun `time is rounded up, so a running timer never shows zero early`() {
        running(focusMillis = 2_000)
        assertEquals("0:02", NotificationContent.of(TimerController.snapshot).time)
        TimerController.tick(1)
        assertEquals("0:02", NotificationContent.of(TimerController.snapshot).time)
    }
}
