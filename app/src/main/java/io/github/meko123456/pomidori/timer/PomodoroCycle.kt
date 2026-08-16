package io.github.meko123456.pomidori.timer

enum class Phase { FOCUS, SHORT_BREAK, LONG_BREAK }

/** User-tunable lengths and cadence. Defaults are the classic 25/5/15, long break every 4th focus. */
data class PomodoroConfig(
    val focusMillis: Long = 25 * 60_000L,
    val shortBreakMillis: Long = 5 * 60_000L,
    val longBreakMillis: Long = 15 * 60_000L,
    val sessionsBeforeLongBreak: Int = 4,
    /** When true, a finished phase rolls straight into the next one running. */
    val autoStartNext: Boolean = true,
)

/** Where we are in the repeating cycle: the current phase and how many focus sessions are done. */
data class CyclePosition(
    val phase: Phase = Phase.FOCUS,
    val completedFocusSessions: Int = 0,
)

/**
 * Pure sequencing of focus sessions and breaks. A completed focus session leads
 * to a long break every [PomodoroConfig.sessionsBeforeLongBreak]th time, and a
 * short break otherwise; a completed break always leads back to focus.
 */
object PomodoroCycle {

    fun start(): CyclePosition = CyclePosition(Phase.FOCUS, 0)

    fun duration(phase: Phase, config: PomodoroConfig): Long = when (phase) {
        Phase.FOCUS -> config.focusMillis
        Phase.SHORT_BREAK -> config.shortBreakMillis
        Phase.LONG_BREAK -> config.longBreakMillis
    }

    /** The position after [position]'s phase finishes. */
    fun next(position: CyclePosition, config: PomodoroConfig): CyclePosition = when (position.phase) {
        Phase.FOCUS -> {
            val done = position.completedFocusSessions + 1
            val every = config.sessionsBeforeLongBreak.coerceAtLeast(1)
            val phase = if (done % every == 0) Phase.LONG_BREAK else Phase.SHORT_BREAK
            CyclePosition(phase, done)
        }
        Phase.SHORT_BREAK, Phase.LONG_BREAK -> CyclePosition(Phase.FOCUS, position.completedFocusSessions)
    }
}
