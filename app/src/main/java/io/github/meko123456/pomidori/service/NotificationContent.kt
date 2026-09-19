package io.github.meko123456.pomidori.service

import io.github.meko123456.pomidori.timer.Phase

import io.github.meko123456.pomidori.timer.TimerStatus
import kotlin.math.ceil

/**
 * Everything the ongoing notification actually shows, and nothing else.
 *
 * Extracted from [TimerService] so the service can tell whether a tick changed anything a person
 * could see. It is a value with `equals`, which is the whole point: the countdown ticks four times a
 * second and displays whole seconds, so three ticks in four produce a notification identical to the
 * one already on screen.
 *
 * Pure, and free of Android types, so the rule it encodes is unit-tested rather than assumed.
 */
internal data class NotificationContent(
    val title: String,
    val time: String,
    val running: Boolean,
) {
    companion object {
        /** What [snapshot] should display. */
        fun of(snapshot: TimerSnapshot): NotificationContent {
            val remaining = ceil(snapshot.timer.remainingMillis / 1000.0).toLong()
            val paused = if (snapshot.timer.status == TimerStatus.PAUSED) " · paused" else ""
            return NotificationContent(
                title = "${label(snapshot.position.phase)}$paused",
                time = "%d:%02d".format(remaining / 60, remaining % 60),
                running = snapshot.timer.status == TimerStatus.RUNNING,
            )
        }

        fun label(phase: Phase): String = when (phase) {
            Phase.FOCUS -> "Focus"
            Phase.SHORT_BREAK -> "Short break"
            Phase.LONG_BREAK -> "Long break"
        }
    }
}
