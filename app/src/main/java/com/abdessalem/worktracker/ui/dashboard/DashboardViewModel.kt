package com.abdessalem.worktracker.ui.dashboard

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abdessalem.worktracker.data.preferences.UserPreferences
import com.abdessalem.worktracker.data.repository.ShiftRepository
import com.abdessalem.worktracker.domain.model.DashboardState
import com.abdessalem.worktracker.domain.model.TimeUtils
import com.abdessalem.worktracker.service.ActiveShiftService
import com.abdessalem.worktracker.ui.components.ActionFeedback
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: ShiftRepository,
    preferences: UserPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000L)
        }
    }

    val feedback = MutableSharedFlow<ActionFeedback>(extraBufferCapacity = 4)
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 4)

    val state: StateFlow<DashboardState> = combine(
        repository.observeActive(),
        repository.observeAll(),
        preferences.settings,
        ticker
    ) { active, shifts, settings, now ->
        val dayStart = TimeUtils.startOfDayMillis(now)
        val today = shifts.filter { it.clockIn >= dayStart && it.clockIn <= now }
        val net = today.sumOf { TimeUtils.netMillis(it, now) }
        val gross = today.sumOf { TimeUtils.grossMillis(it, now) }
        val breaks = today.sumOf { TimeUtils.breakMillis(it, now) }
        DashboardState(
            activeShift = active,
            settings = settings,
            todayNetMillis = net,
            todayGrossMillis = gross,
            todayBreakMillis = breaks,
            expectedFinishMillis = active?.let {
                TimeUtils.expectedFinishMillis(it, settings.scheduledHoursPerDay, settings.expectedBreakMinutes, now)
            },
            overtimeMillis = (net - (settings.scheduledHoursPerDay * 3_600_000).toLong()).coerceAtLeast(0),
            currentTimeMillis = now,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    fun toggleClock(originX: Float, originY: Float) {
        viewModelScope.launch {
            val active = repository.getActive()
            if (active == null) {
                repository.clockIn().onSuccess {
                    startService()
                    feedback.emit(
                        ActionFeedback.clockIn(
                            originX,
                            originY,
                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                        )
                    )
                }.onFailure { messages.emit(it.message ?: "Could not clock in") }
            } else {
                val net = TimeUtils.netMillis(active)
                val breaks = TimeUtils.breakMillis(active)
                repository.clockOut().onSuccess {
                    context.stopService(Intent(context, ActiveShiftService::class.java))
                    feedback.emit(
                        ActionFeedback.clockOut(
                            originX,
                            originY,
                            "${TimeUtils.formatDuration(net)} net • ${TimeUtils.formatDuration(breaks)} break"
                        )
                    )
                }.onFailure { messages.emit(it.message ?: "Could not clock out") }
            }
        }
    }

    fun toggleBreak(originX: Float, originY: Float) {
        viewModelScope.launch {
            repository.toggleBreak().onSuccess { started ->
                startService()
                feedback.emit(
                    if (started) ActionFeedback.breakStarted(originX, originY)
                    else ActionFeedback.resumed(originX, originY)
                )
            }.onFailure { messages.emit(it.message ?: "Could not update break") }
        }
    }

    fun addNote(note: String) {
        viewModelScope.launch {
            val active = repository.getActive()
            if (active == null) {
                messages.emit("Clock in before adding a note")
            } else {
                repository.updateNote(active.id, note)
                    .onSuccess { messages.emit("Note saved") }
                    .onFailure { messages.emit(it.message ?: "Could not save note") }
            }
        }
    }

    private fun startService() {
        ContextCompat.startForegroundService(
            context,
            Intent(context, ActiveShiftService::class.java).setAction(ActiveShiftService.ACTION_REFRESH)
        )
    }
}
