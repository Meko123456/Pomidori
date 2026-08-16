package io.github.meko123456.pomidori.timer

import org.junit.Assert.assertEquals
import org.junit.Test

class PomodoroCycleTest {

    private val config = PomodoroConfig()

    @Test
    fun `starts on a focus session`() {
        assertEquals(CyclePosition(Phase.FOCUS, 0), PomodoroCycle.start())
    }

    @Test
    fun `focus leads to a short break and counts the session`() {
        val after = PomodoroCycle.next(PomodoroCycle.start(), config)
        assertEquals(Phase.SHORT_BREAK, after.phase)
        assertEquals(1, after.completedFocusSessions)
    }

    @Test
    fun `a break leads back to focus, preserving the count`() {
        val onBreak = CyclePosition(Phase.SHORT_BREAK, 1)
        assertEquals(CyclePosition(Phase.FOCUS, 1), PomodoroCycle.next(onBreak, config))
    }

    @Test
    fun `every fourth focus session leads to a long break`() {
        // Walk a full round: F, SB, F, SB, F, SB, F, LB
        var pos = PomodoroCycle.start()
        val phases = mutableListOf<Phase>()
        repeat(8) {
            pos = PomodoroCycle.next(pos, config)
            phases += pos.phase
        }
        assertEquals(
            listOf(
                Phase.SHORT_BREAK, Phase.FOCUS,
                Phase.SHORT_BREAK, Phase.FOCUS,
                Phase.SHORT_BREAK, Phase.FOCUS,
                Phase.LONG_BREAK, Phase.FOCUS,
            ),
            phases,
        )
        assertEquals(4, pos.completedFocusSessions)
    }

    @Test
    fun `sessionsBeforeLongBreak of 1 means every focus gets a long break`() {
        val cfg = config.copy(sessionsBeforeLongBreak = 1)
        assertEquals(Phase.LONG_BREAK, PomodoroCycle.next(PomodoroCycle.start(), cfg).phase)
    }

    @Test
    fun `durations map to config`() {
        assertEquals(config.focusMillis, PomodoroCycle.duration(Phase.FOCUS, config))
        assertEquals(config.shortBreakMillis, PomodoroCycle.duration(Phase.SHORT_BREAK, config))
        assertEquals(config.longBreakMillis, PomodoroCycle.duration(Phase.LONG_BREAK, config))
    }
}
