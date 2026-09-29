package io.github.meko123456.pomidori.timer

/**
 * How a [PomodoroConfig] travels from the phone to the watch: a flat map of numbers, which is what
 * the Wearable data layer's DataMap carries on both ends without either side needing the other's
 * classes.
 *
 * The map is versioned, and [decode] refuses anything it cannot read in full rather than filling
 * gaps with defaults. An empty map, a map from a future version, or one with a length of zero
 * would otherwise decode as a perfectly valid config — the defaults — and quietly overwrite the
 * lengths the person actually chose.
 */
object ConfigWire {

    /** Where the config lives in the data layer. */
    const val PATH = "/pomidori/config"

    const val VERSION = 1L

    private const val KEY_VERSION = "version"
    private const val KEY_FOCUS = "focus_ms"
    private const val KEY_SHORT_BREAK = "short_break_ms"
    private const val KEY_LONG_BREAK = "long_break_ms"
    private const val KEY_SESSIONS = "sessions_before_long_break"
    private const val KEY_AUTO_START = "auto_start_next"

    fun encode(config: PomodoroConfig): Map<String, Long> = mapOf(
        KEY_VERSION to VERSION,
        KEY_FOCUS to config.focusMillis,
        KEY_SHORT_BREAK to config.shortBreakMillis,
        KEY_LONG_BREAK to config.longBreakMillis,
        KEY_SESSIONS to config.sessionsBeforeLongBreak.toLong(),
        KEY_AUTO_START to if (config.autoStartNext) 1L else 0L,
    )

    /** The config [map] describes, or null when it is not one this version can read in full. */
    fun decode(map: Map<String, Long>): PomodoroConfig? {
        if (map[KEY_VERSION] != VERSION) return null
        val focus = map[KEY_FOCUS]?.takeIf { it > 0 } ?: return null
        val shortBreak = map[KEY_SHORT_BREAK]?.takeIf { it > 0 } ?: return null
        val longBreak = map[KEY_LONG_BREAK]?.takeIf { it > 0 } ?: return null
        val sessions = map[KEY_SESSIONS]?.takeIf { it in 1..Int.MAX_VALUE.toLong() } ?: return null
        val autoStart = when (map[KEY_AUTO_START]) {
            1L -> true
            0L -> false
            else -> return null
        }
        return PomodoroConfig(
            focusMillis = focus,
            shortBreakMillis = shortBreak,
            longBreakMillis = longBreak,
            sessionsBeforeLongBreak = sessions.toInt(),
            autoStartNext = autoStart,
        )
    }
}
