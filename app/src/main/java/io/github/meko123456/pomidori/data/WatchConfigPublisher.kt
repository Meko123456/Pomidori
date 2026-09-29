package io.github.meko123456.pomidori.data

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import io.github.meko123456.pomidori.timer.ConfigWire
import io.github.meko123456.pomidori.timer.PomodoroConfig

/**
 * Hands the phone's timer settings to the watch app through the Wearable data layer.
 *
 * A data item rather than a message: the data layer keeps it, so a watch that is off, out of range
 * or has not opened the app yet still gets the latest lengths the next time it connects, and
 * putting the same config again is not a change the watch hears about. On a phone with no watch
 * paired the call fails, which is logged and otherwise nothing.
 */
class WatchConfigPublisher(context: Context) {

    private val dataClient = Wearable.getDataClient(context)

    fun publish(config: PomodoroConfig) {
        val request = PutDataMapRequest.create(ConfigWire.PATH).apply {
            ConfigWire.encode(config).forEach { (key, value) -> dataMap.putLong(key, value) }
        }.asPutDataRequest().setUrgent()
        dataClient.putDataItem(request).addOnFailureListener { Log.i(TAG, "timer settings not sent to a watch", it) }
    }

    private companion object {
        const val TAG = "WatchConfigPublisher"
    }
}
