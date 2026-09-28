package io.github.meko123456.pomidori.wear.tile

import io.github.meko123456.pomidori.timer.CyclePosition
import io.github.meko123456.pomidori.timer.Phase
import io.github.meko123456.pomidori.timer.TimerSnapshot
import io.github.meko123456.pomidori.timer.TimerState
import io.github.meko123456.pomidori.timer.TimerStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TileContentTest {

    private val now = 1_800_000_000_000L

    private fun snapshot(phase: Phase, status: TimerStatus, remainingMillis: Long, totalMillis: Long = 25 * 60_000L) =
        TimerSnapshot(CyclePosition(phase, completedFocusSessions = 0), TimerState(totalMillis, remainingMillis, status))

    @Test
    fun `an idle focus phase offers to start, with its whole length and nothing to count down to`() {
        val content = TileContent.of(snapshot(Phase.FOCUS, TimerStatus.IDLE, 25 * 60_000L), now)

        assertEquals(TileContent(title = "Focus", time = "25:00", endsAtEpochMillis = null, action = "Start"), content)
    }

    @Test
    fun `a running phase counts down to the moment it ends`() {
        val content = TileContent.of(snapshot(Phase.FOCUS, TimerStatus.RUNNING, 12 * 60_000L + 34_000), now)

        assertEquals("Pause", content.action)
        assertEquals(now + 12 * 60_000L + 34_000, content.endsAtEpochMillis)
        assertEquals("12:34", content.time)
    }

    @Test
    fun `a paused phase says so, holds its time and offers to resume`() {
        val content = TileContent.of(snapshot(Phase.SHORT_BREAK, TimerStatus.PAUSED, 3 * 60_000L, totalMillis = 5 * 60_000L), now)

        assertEquals("Short break · paused", content.title)
        assertEquals("3:00", content.time)
        assertNull(content.endsAtEpochMillis)
        assertEquals("Resume", content.action)
    }

    @Test
    fun `a finished phase offers to start again`() {
        val content = TileContent.of(snapshot(Phase.LONG_BREAK, TimerStatus.FINISHED, 0, totalMillis = 15 * 60_000L), now)

        assertEquals("Long break", content.title)
        assertEquals("Start", content.action)
        assertNull(content.endsAtEpochMillis)
    }

    @Test
    fun `part of a second left still reads as a whole second, as the notification does`() {
        val content = TileContent.of(snapshot(Phase.FOCUS, TimerStatus.RUNNING, 400), now)

        assertEquals("0:01", content.time)
    }
}
