package io.github.meko123456.pomidori.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.meko123456.pomidori.timer.PomodoroConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "pomidori_settings")

/**
 * Persists the [PomodoroConfig] with DataStore. Durations are stored in whole
 * minutes (what the UI edits); everything reads back through [config].
 */
class SettingsRepository(private val context: Context) {

    val config: Flow<PomodoroConfig> = context.dataStore.data.map { p ->
        PomodoroConfig(
            focusMillis = (p[FOCUS] ?: 25) * 60_000L,
            shortBreakMillis = (p[SHORT_BREAK] ?: 5) * 60_000L,
            longBreakMillis = (p[LONG_BREAK] ?: 15) * 60_000L,
            sessionsBeforeLongBreak = p[SESSIONS] ?: 4,
            autoStartNext = p[AUTO_START] ?: true,
        )
    }

    suspend fun setFocusMinutes(min: Int) = edit { it[FOCUS] = min }
    suspend fun setShortBreakMinutes(min: Int) = edit { it[SHORT_BREAK] = min }
    suspend fun setLongBreakMinutes(min: Int) = edit { it[LONG_BREAK] = min }
    suspend fun setSessionsBeforeLongBreak(n: Int) = edit { it[SESSIONS] = n }
    suspend fun setAutoStartNext(on: Boolean) = edit { it[AUTO_START] = on }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    private companion object {
        val FOCUS = intPreferencesKey("focus_min")
        val SHORT_BREAK = intPreferencesKey("short_break_min")
        val LONG_BREAK = intPreferencesKey("long_break_min")
        val SESSIONS = intPreferencesKey("sessions_before_long")
        val AUTO_START = booleanPreferencesKey("auto_start_next")
    }
}
