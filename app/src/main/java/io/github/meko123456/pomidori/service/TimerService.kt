package io.github.meko123456.pomidori.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import io.github.meko123456.pomidori.MainActivity
import io.github.meko123456.pomidori.data.SessionTallyRepository
import io.github.meko123456.pomidori.timer.Phase
import io.github.meko123456.pomidori.timer.TimerStatus
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil

/**
 * Foreground service that owns the countdown so a session keeps running with the
 * screen off or the app backgrounded. Commands arrive as intent actions; the
 * shared [TimerController] holds the state and the service drives the tick loop
 * and keeps an ongoing notification in sync.
 */
class TimerService : Service() {

    // Default, not Main.immediate. The loop does no UI work — it advances a counter and posts a
    // notification — and doing that on the main thread put it directly in front of frame rendering.
    // TimerController is a MutableStateFlow updated through `update {}`, so it is safe from here.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val tally by lazy { SessionTallyRepository(applicationContext) }
    private var loopJob: Job? = null
    private var lastMark = 0L

    /** What is currently on screen, so an unchanged tick can be skipped. */
    private var shown: NotificationContent? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureChannel()
        when (intent?.action) {
            ACTION_PRIMARY -> TimerController.primary()
            ACTION_RESET -> TimerController.reset()
            ACTION_SKIP -> TimerController.skip()
            ACTION_STOP -> { stopAll(); return START_NOT_STICKY }
        }

        val snapshot = TimerController.snapshot
        // Always posted, whatever was on screen before: startForeground is a promise to the system
        // rather than a screen update, and skipping it would drop the service.
        shown = NotificationContent.of(snapshot)
        startForeground(NOTIF_ID, buildNotification(shown!!))

        if (snapshot.timer.status == TimerStatus.IDLE) {
            // Nothing to run (reset, or a phase just ended) — clear the notification.
            stopAll()
            return START_NOT_STICKY
        }
        syncLoop()
        return START_STICKY
    }

    /** Runs the tick loop while the timer is running; idempotent. */
    private fun syncLoop() {
        if (!TimerController.snapshot.isRunning) {
            loopJob?.cancel(); loopJob = null
            updateNotification() // reflect the paused state
            return
        }
        if (loopJob != null) return
        lastMark = SystemClock.elapsedRealtime()
        loopJob = scope.launch {
            while (TimerController.snapshot.isRunning) {
                delay(250)
                val now = SystemClock.elapsedRealtime()
                val finishingPhase = TimerController.snapshot.position.phase
                val finished = TimerController.tick(now - lastMark)
                lastMark = now
                if (finished) {
                    if (finishingPhase == Phase.FOCUS) {
                        scope.launch { tally.increment(LocalDate.now().toEpochDay()) }
                    }
                    chimeAndVibrate()
                    if (!TimerController.snapshot.isRunning) {
                        // Auto-start is off — the next phase waits idle for the user.
                        updateNotification()
                        stopAll()
                        return@launch
                    }
                    // Auto-started the next phase; keep looping from now.
                    lastMark = SystemClock.elapsedRealtime()
                }
                updateNotification()
            }
        }
    }

    private fun stopAll() {
        loopJob?.cancel(); loopJob = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /** Short chime + buzz to mark a phase boundary. */
    private fun chimeAndVibrate() {
        runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            RingtoneManager.getRingtone(applicationContext, uri)?.play()
        }
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION") getSystemService(Vibrator::class.java)
        }
        runCatching {
            vibrator.vibrate(VibrationEffect.createOneShot(450, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    /**
     * Posts the notification, but only when it would look different.
     *
     * The loop ticks every 250 ms so a phase boundary is noticed promptly, while the notification
     * shows whole seconds — so three ticks in four used to rebuild and re-post something identical
     * to what was already there. Rebuilding is not cheap: it constructs three PendingIntents and a
     * full Notification, and the system then re-renders the shade row.
     *
     * Keeping the countdown accurate and the posting rate honest are separate concerns, and this is
     * where they separate.
     */
    private fun updateNotification() {
        val content = NotificationContent.of(TimerController.snapshot)
        if (content == shown) return
        shown = content
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotification(content))
    }

    private fun buildNotification(content: NotificationContent): Notification {
        val time = content.time
        val running = content.running
        val primary = if (running) {
            action(android.R.drawable.ic_media_pause, "Pause", ACTION_PRIMARY)
        } else {
            action(android.R.drawable.ic_media_play, "Resume", ACTION_PRIMARY)
        }
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(content.title)
            .setContentText(time)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(running)
            .setOnlyAlertOnce(true)
            .setContentIntent(openApp)
            .addAction(primary)
            .addAction(action(android.R.drawable.ic_media_next, "Skip", ACTION_SKIP))
            .build()
    }

    /**
     * Opens the app. Built once: it never varies, and constructing a PendingIntent per tick was part
     * of what made the countdown expensive.
     */
    private val openApp: PendingIntent by lazy {
        PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    /** A notification action button that sends [act] back to this service. */
    private fun action(iconRes: Int, label: String, act: String): Notification.Action {
        val pi = PendingIntent.getService(
            this, act.hashCode(), Intent(this, TimerService::class.java).setAction(act),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return Notification.Action.Builder(Icon.createWithResource(this, iconRes), label, pi).build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Timer", NotificationManager.IMPORTANCE_LOW),
            )
        }
    }

    override fun onDestroy() {
        loopJob?.cancel()
    }

    companion object {
        private const val CHANNEL_ID = "timer"
        private const val NOTIF_ID = 1
        const val ACTION_PRIMARY = "io.github.meko123456.pomidori.PRIMARY"
        const val ACTION_RESET = "io.github.meko123456.pomidori.RESET"
        const val ACTION_SKIP = "io.github.meko123456.pomidori.SKIP"
        const val ACTION_STOP = "io.github.meko123456.pomidori.STOP"

        /** Sends [action] to the service, starting it in the foreground. */
        fun send(context: Context, action: String) {
            val intent = Intent(context, TimerService::class.java).setAction(action)
            context.startForegroundService(intent)
        }
    }
}

