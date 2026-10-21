package io.github.meko123456.pomidori

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.meko123456.pomidori.service.TimerService
import io.github.meko123456.pomidori.timer.TimerController
import io.github.meko123456.pomidori.ui.SettingsScreen
import io.github.meko123456.pomidori.ui.SettingsViewModel
import io.github.meko123456.pomidori.ui.TimerScreen
import io.github.meko123456.pomidori.ui.theme.PomidoriTheme

class MainActivity : ComponentActivity() {

    /** Set by the launcher's Start the timer shortcut (res/xml/shortcuts.xml) until it has. */
    private var startRequested by mutableStateOf(false)

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        maybeRequestNotifications()
        // A fresh start only: after a turn the same intent would press Start again, and a second
        // press pauses.
        if (savedInstanceState == null && intent?.action == ACTION_START_TIMER) startRequested = true
        setContent {
            PomidoriTheme {
                // Created here so its init syncs saved settings into the timer on launch.
                val settingsVm: SettingsViewModel = viewModel()
                var showSettings by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(startRequested) {
                    if (startRequested) {
                        // The saved lengths go to the timer first: on a cold start the shortcut
                        // could otherwise start before they're read, at the default length.
                        settingsVm.configured()
                        // What Start would do: start or resume, and leave a running timer alone.
                        if (!TimerController.snapshot.isRunning) {
                            TimerService.send(this@MainActivity, TimerService.ACTION_PRIMARY)
                        }
                        startRequested = false
                    }
                }

                BackHandler(enabled = showSettings) { showSettings = false }

                if (showSettings) {
                    SettingsScreen(onBack = { showSettings = false }, vm = settingsVm)
                } else {
                    TimerScreen(onOpenSettings = { showSettings = true })
                }
            }
        }
    }

    /** The ongoing timer notification needs POST_NOTIFICATIONS on Android 13+. */
    private fun maybeRequestNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object {
        const val ACTION_START_TIMER = "io.github.meko123456.pomidori.action.START_TIMER"
    }
}
