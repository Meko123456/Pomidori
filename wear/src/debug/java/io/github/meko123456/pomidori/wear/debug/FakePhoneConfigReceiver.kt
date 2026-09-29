package io.github.meko123456.pomidori.wear.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.meko123456.pomidori.timer.ConfigWire
import io.github.meko123456.pomidori.timer.PomodoroConfig
import io.github.meko123456.pomidori.wear.sync.WatchConfig

/**
 * Debug-only: plays the phone's part in the settings sync, for a watch with no phone paired.
 *
 * It hands [WatchConfig.apply] exactly the map the phone would put in the data layer, so
 * everything after the Bluetooth hop — decoding, keeping it, the controller, the idle phase and
 * the tile — runs as it would for a real push.
 *
 *     adb shell am broadcast -n io.github.meko123456.pomidori/.wear.debug.FakePhoneConfigReceiver \
 *         --el focus_minutes 50 --el short_break_minutes 10
 */
class FakePhoneConfigReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val defaults = PomodoroConfig()
        val config = PomodoroConfig(
            focusMillis = intent.getLongExtra("focus_minutes", defaults.focusMillis / 60_000) * 60_000,
            shortBreakMillis = intent.getLongExtra("short_break_minutes", defaults.shortBreakMillis / 60_000) * 60_000,
            longBreakMillis = intent.getLongExtra("long_break_minutes", defaults.longBreakMillis / 60_000) * 60_000,
            sessionsBeforeLongBreak = intent.getIntExtra("sessions", defaults.sessionsBeforeLongBreak),
            autoStartNext = intent.getBooleanExtra("auto_start", defaults.autoStartNext),
        )
        WatchConfig.apply(context, ConfigWire.encode(config))
    }
}
