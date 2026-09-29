package io.github.meko123456.pomidori.wear.sync

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import io.github.meko123456.pomidori.timer.ConfigWire

/** Takes the phone's timer settings as they change; see [WatchConfig.apply] for what that does. */
class ConfigListenerService : WearableListenerService() {

    override fun onDataChanged(events: DataEventBuffer) {
        events.forEach { event ->
            if (event.type != DataEvent.TYPE_CHANGED || event.dataItem.uri.path != ConfigWire.PATH) return@forEach
            val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
            WatchConfig.apply(this, dataMap.keySet().associateWith { dataMap.getLong(it) })
        }
    }
}
