package com.abdessalem.worktracker.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abdessalem.worktracker.data.local.ShiftEntity
import com.abdessalem.worktracker.data.preferences.UserPreferences
import com.abdessalem.worktracker.data.repository.ShiftRepository
import com.abdessalem.worktracker.domain.model.WorkSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class CalendarState(val shifts: List<ShiftEntity> = emptyList(), val settings: WorkSettings = WorkSettings())

@HiltViewModel
class CalendarViewModel @Inject constructor(repository: ShiftRepository, preferences: UserPreferences) : ViewModel() {
    val state: StateFlow<CalendarState> = combine(repository.observeAll(), preferences.settings) { shifts, settings -> CalendarState(shifts, settings) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarState())
}
