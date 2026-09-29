package io.github.meko123456.pomidori.wear

import android.app.Application
import io.github.meko123456.pomidori.wear.sync.WatchConfig

/** Puts the phone's timer lengths in place before anything on the watch reads them. */
class PomidoriWatchApp : Application() {
    override fun onCreate() {
        super.onCreate()
        WatchConfig.restore(this)
    }
}
