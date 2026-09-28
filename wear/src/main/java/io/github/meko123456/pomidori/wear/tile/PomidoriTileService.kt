package io.github.meko123456.pomidori.wear.tile

import android.content.ComponentName
import androidx.wear.protolayout.ActionBuilders.launchAction
import androidx.wear.protolayout.ActionBuilders.stringExtra
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.TypeBuilders.StringLayoutConstraint
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInstant
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInt32
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.textEdgeButton
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.types.LayoutString
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.Material3TileService
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.TileBuilders.Tile
import io.github.meko123456.pomidori.timer.TimerController
import io.github.meko123456.pomidori.wear.MainActivity
import java.time.Instant

/**
 * A focus session from the wrist: the phase, its time, and one button to start, pause or resume it.
 *
 * While a phase runs, the tile counts down by itself — the renderer is handed the moment the phase
 * ends rather than the time left, so nothing has to push it an update per second. The timer
 * service asks for a fresh tile when anything else changes, and a running tile expires at the end
 * of its phase in case that request never comes.
 */
class PomidoriTileService : Material3TileService() {

    override suspend fun MaterialScope.tileResponse(requestParams: TileRequest): Tile {
        val now = System.currentTimeMillis()
        val content = TileContent.of(TimerController.snapshot, now)

        val layout = primaryLayout(
            titleSlot = { text(content.title.layoutString) },
            mainSlot = { text(timeLeft(content), typography = Typography.NUMERAL_MEDIUM) },
            bottomSlot = {
                textEdgeButton(onClick = clickable(openAppTo(COMMAND_ID), id = COMMAND_ID)) {
                    text(content.action.layoutString)
                }
            },
        )
        return Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(Timeline.fromLayoutElement(layout))
            .setFreshnessIntervalMillis(content.endsAtEpochMillis?.let { (it - now).coerceAtLeast(0) } ?: 0)
            .build()
    }

    /** The app, told what was tapped: an activity may start a foreground service, a tile may not. */
    private fun openAppTo(command: String) = launchAction(
        ComponentName(this, MainActivity::class.java),
        mapOf(MainActivity.EXTRA_COMMAND to stringExtra(command)),
    )

    /**
     * "12:34", counting down on the watch while the phase runs. It stops at 0:00 rather than
     * counting back up from the end, for the moment between a phase ending and the new tile.
     */
    private fun timeLeft(content: TileContent): LayoutString {
        val endsAt = content.endsAtEpochMillis ?: return content.time.layoutString
        val left = DynamicInstant.platformTimeWithSecondsPrecision()
            .durationUntil(DynamicInstant.withSecondsPrecision(Instant.ofEpochMilli(endsAt)))
        val twoDigits = DynamicInt32.IntFormatter.Builder().setMinIntegerDigits(2).build()
        val counting = left.toIntMinutes().format()
            .concat(DynamicString.constant(":"))
            .concat(left.secondsPart.format(twoDigits))
        val clamped = DynamicString.onCondition(left.toIntSeconds().gt(0)).use(counting).elseUse("0:00")
        return LayoutString(content.time, clamped, StringLayoutConstraint.Builder("00:00").build())
    }

    private companion object {
        const val RESOURCES_VERSION = "1"
        const val COMMAND_ID = MainActivity.COMMAND_PRIMARY
    }
}
