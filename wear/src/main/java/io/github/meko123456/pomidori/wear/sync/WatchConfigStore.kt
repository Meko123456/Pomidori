package io.github.meko123456.pomidori.wear.sync

import android.content.Context
import io.github.meko123456.pomidori.timer.ConfigWire
import io.github.meko123456.pomidori.timer.PomodoroConfig

/**
 * The last timer config the phone sent, kept on the watch so it outlives the process: the watch
 * app is started cold by a tile, a tap or a phase ending, often with no phone in reach.
 *
 * Stored in the same versioned shape it travels in, so reading it back is one [ConfigWire.decode]
 * and a store written by some other version reads as nothing rather than as the defaults.
 */
class WatchConfigStore(context: Context) {

    private val prefs = context.getSharedPreferences("watch_config", Context.MODE_PRIVATE)

    fun load(): PomodoroConfig? = ConfigWire.decode(prefs.all.mapNotNull { (key, value) -> (value as? Long)?.let { key to it } }.toMap())

    fun save(config: PomodoroConfig) {
        prefs.edit().clear().apply { ConfigWire.encode(config).forEach { (key, value) -> putLong(key, value) } }.apply()
    }
}
