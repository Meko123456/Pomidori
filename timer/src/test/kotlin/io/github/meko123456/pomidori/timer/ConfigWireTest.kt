package io.github.meko123456.pomidori.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConfigWireTest {

    private val chosen = PomodoroConfig(
        focusMillis = 50 * 60_000L,
        shortBreakMillis = 10 * 60_000L,
        longBreakMillis = 30 * 60_000L,
        sessionsBeforeLongBreak = 3,
        autoStartNext = false,
    )

    @Test
    fun `a config survives the trip`() {
        assertEquals(chosen, ConfigWire.decode(ConfigWire.encode(chosen)))
    }

    @Test
    fun `the defaults survive it too`() {
        assertEquals(PomodoroConfig(), ConfigWire.decode(ConfigWire.encode(PomodoroConfig())))
    }

    @Test
    fun `an empty map is no config at all, not the defaults`() {
        assertNull(ConfigWire.decode(emptyMap()))
    }

    @Test
    fun `a map from another version is not read`() {
        assertNull(ConfigWire.decode(ConfigWire.encode(chosen) + ("version" to 2L)))
    }

    @Test
    fun `a missing field is not filled in`() {
        assertNull(ConfigWire.decode(ConfigWire.encode(chosen) - "long_break_ms"))
    }

    @Test
    fun `a length of zero is refused rather than timed`() {
        assertNull(ConfigWire.decode(ConfigWire.encode(chosen) + ("focus_ms" to 0L)))
    }

    @Test
    fun `auto-start must be a clear yes or no`() {
        assertNull(ConfigWire.decode(ConfigWire.encode(chosen) + ("auto_start_next" to 7L)))
    }
}
