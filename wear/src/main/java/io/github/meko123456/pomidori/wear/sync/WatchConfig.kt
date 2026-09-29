package io.github.meko123456.pomidori.wear.sync

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.wear.tiles.TileService
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import io.github.meko123456.pomidori.timer.ConfigWire
import io.github.meko123456.pomidori.timer.TimerController
import io.github.meko123456.pomidori.timer.TimerStatus
import io.github.meko123456.pomidori.wear.tile.PomidoriTileService

/**
 * The watch's side of the settings sync: where the phone's lengths land, and how they take effect.
 *
 * A config that does not decode is ignored, so the watch keeps what it had. One that does is kept
 * for the next cold start, becomes the controller's config for every phase from here on, and — if
 * nothing is running — redraws the idle phase at its new length, on the screen and the tile. A
 * session under way is left alone: its lengths change at its next phase, not mid-countdown.
 */
object WatchConfig {

    fun apply(context: Context, wire: Map<String, Long>) {
        val config = ConfigWire.decode(wire) ?: run {
            Log.w(TAG, "ignored a timer config this version cannot read: ${wire.keys}")
            return
        }
        WatchConfigStore(context).save(config)
        TimerController.config = config
        if (TimerController.snapshot.timer.status == TimerStatus.IDLE) TimerController.reset()
        TileService.getUpdater(context.applicationContext).requestUpdate(PomidoriTileService::class.java)
    }

    /** At process start: the kept config first, then whatever the data layer holds now. */
    fun restore(context: Context) {
        WatchConfigStore(context).load()?.let { TimerController.config = it; TimerController.reset() }
        val uri = Uri.Builder().scheme("wear").path(ConfigWire.PATH).build()
        Wearable.getDataClient(context).getDataItems(uri)
            .addOnSuccessListener { items ->
                items.forEach { item ->
                    val dataMap = DataMapItem.fromDataItem(item).dataMap
                    apply(context, dataMap.keySet().associateWith { dataMap.getLong(it) })
                }
                items.release()
            }
            .addOnFailureListener { Log.i(TAG, "no phone config to catch up on", it) }
    }

    private const val TAG = "WatchConfig"
}
