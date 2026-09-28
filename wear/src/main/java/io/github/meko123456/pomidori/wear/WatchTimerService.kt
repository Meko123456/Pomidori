package io.github.meko123456.pomidori.wear

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import io.github.meko123456.pomidori.timer.NotificationContent
import io.github.meko123456.pomidori.timer.TimerController
import io.github.meko123456.pomidori.timer.TimerSnapshot
import io.github.meko123456.pomidori.timer.TimerStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The watch's own countdown: the same [TimerController] the phone runs, advanced by a foreground
 * service so a session keeps going with the screen off.
 *
 * It differs from the phone's service in what it keeps up to date. The notification carries an
 * ongoing activity whose timer counts down on the watch face by itself, so it does not need a post
 * per second: it is refreshed when something a person would notice changes — a start, a pause, a
 * skip, a phase ending — and not on the ticks in between, which only keep the controller exact for
 * whoever reads it next.
 */
class WatchTimerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loopJob: Job? = null
    private var lastMark = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureChannel()
        when (intent?.action) {
            ACTION_PRIMARY -> TimerController.primary()
            ACTION_SKIP -> TimerController.skip()
            ACTION_RESET -> TimerController.reset()
        }
        // Always, whatever the command: startForeground is the promise a foreground-service start
        // has to keep, and skipping it takes the service down.
        startForeground(NOTIFICATION_ID, notification(TimerController.snapshot))
        showChange()

        if (TimerController.snapshot.timer.status == TimerStatus.IDLE) {
            stopAll()
            return START_NOT_STICKY
        }
        syncLoop()
        return START_STICKY
    }

    private fun syncLoop() {
        if (!TimerController.snapshot.isRunning) {
            loopJob?.cancel(); loopJob = null
            return
        }
        if (loopJob != null) return
        lastMark = SystemClock.elapsedRealtime()
        loopJob = scope.launch {
            while (TimerController.snapshot.isRunning) {
                delay(TICK_MILLIS)
                val now = SystemClock.elapsedRealtime()
                val finished = TimerController.tick(now - lastMark)
                lastMark = now
                if (finished) {
                    buzz()
                    showChange()
                    if (!TimerController.snapshot.isRunning) {
                        // Auto-start is off: the next phase waits, idle, for a tap.
                        stopAll()
                        return@launch
                    }
                }
            }
        }
    }

    /** Re-posts the notification, which is where a change is seen. */
    private fun showChange() {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(TimerController.snapshot))
    }

    private fun notification(snapshot: TimerSnapshot): Notification {
        val content = NotificationContent.of(snapshot)
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(content.title)
            .setContentText(content.time)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openApp)
            .addAction(0, if (content.running) "Pause" else "Resume", command(ACTION_PRIMARY))
            .addAction(0, "Skip", command(ACTION_SKIP))

        OngoingActivity.Builder(applicationContext, NOTIFICATION_ID, builder)
            .setStaticIcon(R.drawable.ic_notification)
            .setTouchIntent(openApp)
            .setStatus(status(snapshot, content))
            .build()
            .apply(applicationContext)
        return builder.build()
    }

    /** "Focus 12:34" on the watch face, counting itself down while the phase runs. */
    private fun status(snapshot: TimerSnapshot, content: NotificationContent): Status {
        val label = NotificationContent.label(snapshot.position.phase)
        val time = if (snapshot.isRunning) {
            Status.TimerPart(SystemClock.elapsedRealtime() + snapshot.timer.remainingMillis)
        } else {
            Status.TextPart(content.time)
        }
        return Status.Builder()
            .addTemplate("#label# #time#")
            .addPart("label", Status.TextPart(label))
            .addPart("time", time)
            .build()
    }

    private val openApp: PendingIntent by lazy {
        PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun command(action: String): PendingIntent = PendingIntent.getService(
        this, action.hashCode(), Intent(this, WatchTimerService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** A watch has no chime worth the name; the buzz is the whole signal. */
    private fun buzz() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION") getSystemService(Vibrator::class.java)
        }
        runCatching { vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1)) }
    }

    private fun ensureChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Timer", NotificationManager.IMPORTANCE_LOW),
        )
    }

    private fun stopAll() {
        loopJob?.cancel(); loopJob = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_PRIMARY = "io.github.meko123456.pomidori.wear.PRIMARY"
        const val ACTION_SKIP = "io.github.meko123456.pomidori.wear.SKIP"
        const val ACTION_RESET = "io.github.meko123456.pomidori.wear.RESET"

        private const val CHANNEL_ID = "timer"
        private const val NOTIFICATION_ID = 1
        private const val TICK_MILLIS = 250L
    }
}
