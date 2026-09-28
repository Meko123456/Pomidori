package io.github.meko123456.pomidori.wear.tile

import io.github.meko123456.pomidori.timer.NotificationContent
import io.github.meko123456.pomidori.timer.TimerSnapshot
import io.github.meko123456.pomidori.timer.TimerStatus

/**
 * Everything the tile shows, as plain values.
 *
 * The ProtoLayout tree is built from this and nothing else, so what the tile says in each state is
 * a unit test rather than something read off an emulator. The words are the ongoing notification's
 * — [NotificationContent] — so the tile, the notification and the phone never disagree about what
 * a phase is called.
 */
data class TileContent(
    /** "Focus", "Short break · paused". */
    val title: String,
    /**
     * The time left, as the tile draws it on its own: exact while idle or paused, and the fallback
     * while running, for a renderer too old to count down by itself.
     */
    val time: String,
    /** While running, the wall-clock moment the phase ends, which the tile counts down to. */
    val endsAtEpochMillis: Long?,
    /** The one button: start an idle phase, pause a running one, resume a paused one. */
    val action: String,
) {
    companion object {
        fun of(snapshot: TimerSnapshot, nowEpochMillis: Long): TileContent {
            val shown = NotificationContent.of(snapshot)
            return TileContent(
                title = shown.title,
                time = shown.time,
                endsAtEpochMillis = if (snapshot.isRunning) nowEpochMillis + snapshot.timer.remainingMillis else null,
                action = when (snapshot.timer.status) {
                    TimerStatus.IDLE, TimerStatus.FINISHED -> "Start"
                    TimerStatus.RUNNING -> "Pause"
                    TimerStatus.PAUSED -> "Resume"
                },
            )
        }
    }
}
