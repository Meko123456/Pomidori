package io.github.meko123456.pomidori.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.tallyStore by preferencesDataStore(name = "pomidori_tally")

/**
 * Counts focus sessions completed *today*, persisted with DataStore and reset at
 * local midnight (the stored day-stamp is compared to the current day, so a stale
 * count from a previous day reads as 0 and is overwritten on the next increment).
 */
class SessionTallyRepository(private val context: Context) {

    /** Today's count for [todayEpochDay]; 0 when the stored tally is from another day. */
    fun todayCount(todayEpochDay: Long): Flow<Int> = context.tallyStore.data.map { p ->
        if (p[DAY] == todayEpochDay) (p[COUNT] ?: 0) else 0
    }

    /** Records one completed focus session for [todayEpochDay], rolling over across days. */
    suspend fun increment(todayEpochDay: Long) {
        context.tallyStore.edit { p ->
            val sameDay = p[DAY] == todayEpochDay
            p[DAY] = todayEpochDay
            p[COUNT] = if (sameDay) (p[COUNT] ?: 0) + 1 else 1
        }
    }

    private companion object {
        val DAY = longPreferencesKey("tally_day")
        val COUNT = intPreferencesKey("tally_count")
    }
}
