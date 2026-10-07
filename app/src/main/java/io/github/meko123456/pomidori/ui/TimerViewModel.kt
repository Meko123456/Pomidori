package io.github.meko123456.pomidori.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.meko123456.pomidori.data.SessionTallyRepository
import io.github.meko123456.pomidori.service.TimerService
import io.github.meko123456.pomidori.timer.TimerController
import io.github.meko123456.pomidori.timer.TimerSnapshot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate

/**
 * Thin bridge between the UI and the timer: observes the shared [TimerController]
 * state and forwards controls to the [TimerService], which owns the countdown so
 * it survives the screen turning off.
 */
class TimerViewModel(app: Application) : AndroidViewModel(app) {

    val state: StateFlow<TimerSnapshot> = TimerController.state

    // Bumped each time the timer screen resumes; see tallyToday.
    private val resumes = MutableStateFlow(0)

    /**
     * Focus sessions completed today, persisted and reset at local midnight.
     *
     * The date is read again on every change to the tally and every time the screen resumes. It was
     * read once, when this ViewModel was created, so an app left open overnight showed last night's
     * count in the morning, and showed the first session of the new day as none.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val tallyToday: StateFlow<Int> =
        resumes
            .flatMapLatest { SessionTallyRepository(app).todayCount { LocalDate.now().toEpochDay() } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    /** The timer screen calls this when it resumes: the day may have turned while it was away. */
    fun recheckDay() = resumes.update { it + 1 }

    fun primary() = TimerService.send(getApplication(), TimerService.ACTION_PRIMARY)
    fun reset() = TimerService.send(getApplication(), TimerService.ACTION_RESET)
    fun skip() = TimerService.send(getApplication(), TimerService.ACTION_SKIP)
}
