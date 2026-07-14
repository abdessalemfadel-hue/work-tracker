package com.abdessalem.worktracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abdessalem.worktracker.data.local.ShiftEntity
import com.abdessalem.worktracker.data.repository.ShiftRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: ShiftRepository
) : ViewModel() {
    val shifts: StateFlow<List<ShiftEntity>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val messages = MutableSharedFlow<String>(extraBufferCapacity = 4)

    fun save(shift: ShiftEntity?) {
        if (shift == null) return
        viewModelScope.launch {
            val result = if (shift.id == 0L) {
                repository.addManual(
                    shift.clockIn,
                    requireNotNull(shift.clockOut),
                    shift.totalBreakMillis,
                    shift.note
                ).map { Unit }
            } else {
                repository.updateShift(shift)
            }
            result.onSuccess { messages.emit("Shift saved") }
                .onFailure { messages.emit(it.message ?: "Could not save shift") }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            repository.delete(id)
            messages.emit("Shift deleted")
        }
    }

    fun duplicate(id: Long) {
        viewModelScope.launch {
            repository.duplicate(id)
                .onSuccess { messages.emit("Shift duplicated") }
                .onFailure { messages.emit(it.message ?: "Could not duplicate shift") }
        }
    }
}
