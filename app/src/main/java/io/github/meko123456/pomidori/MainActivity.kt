package io.github.meko123456.pomidori

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.meko123456.pomidori.ui.TimerScreen
import io.github.meko123456.pomidori.ui.theme.PomidoriTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PomidoriTheme {
                TimerScreen()
            }
        }
    }
}
