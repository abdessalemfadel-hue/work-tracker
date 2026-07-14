package com.abdessalem.worktracker.ui.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abdessalem.worktracker.data.local.ShiftEntity
import com.abdessalem.worktracker.data.preferences.UserPreferences
import com.abdessalem.worktracker.data.repository.ShiftRepository
import com.abdessalem.worktracker.domain.model.TimeUtils
import com.abdessalem.worktracker.domain.model.WorkSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class MoreState(val settings: WorkSettings = WorkSettings(), val shifts: List<ShiftEntity> = emptyList())

@HiltViewModel
class MoreViewModel @Inject constructor(private val repository: ShiftRepository, private val preferences: UserPreferences) : ViewModel() {
    val state: StateFlow<MoreState> = combine(repository.observeAll(), preferences.settings) { shifts, settings -> MoreState(settings, shifts) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MoreState())
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 4)

    fun updateSettings(settings: WorkSettings) { viewModelScope.launch { preferences.update { settings }; messages.emit("Settings saved") } }
    fun deleteAll() { viewModelScope.launch { repository.deleteAll(); messages.emit("All work records deleted") } }

    suspend fun csvText(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US).withZone(ZoneId.systemDefault())
        return buildString {
            appendLine("Date,Clock In,Clock Out,Gross Hours,Break Minutes,Net Hours,Overtime Hours,Notes,Source")
            repository.getAllOnce().forEach { shift ->
                val gross = TimeUtils.grossMillis(shift); val breaks = TimeUtils.breakMillis(shift); val net = TimeUtils.netMillis(shift); val overtime = TimeUtils.overtimeMillis(shift, state.value.settings.scheduledHoursPerDay)
                append(csv(formatter.format(Instant.ofEpochMilli(shift.clockIn)).substringBefore(' '))).append(',')
                append(csv(formatter.format(Instant.ofEpochMilli(shift.clockIn)))).append(',')
                append(csv(shift.clockOut?.let { formatter.format(Instant.ofEpochMilli(it)) } ?: "")).append(',')
                append(String.format(Locale.US, "%.2f", gross / 3_600_000.0)).append(',')
                append(breaks / 60_000L).append(',')
                append(String.format(Locale.US, "%.2f", net / 3_600_000.0)).append(',')
                append(String.format(Locale.US, "%.2f", overtime / 3_600_000.0)).append(',')
                append(csv(shift.note)).append(',').appendLine(shift.source.name)
            }
        }
    }

    suspend fun jsonText(): String {
        val root = JSONObject().put("version", 1).put("exportedAt", System.currentTimeMillis())
        val array = JSONArray()
        repository.getAllOnce().forEach { shift -> array.put(JSONObject().apply { put("clockIn", shift.clockIn); put("clockOut", shift.clockOut ?: JSONObject.NULL); put("totalBreakMillis", shift.totalBreakMillis); put("note", shift.note); put("source", shift.source.name); put("createdAt", shift.createdAt); put("updatedAt", shift.updatedAt) }) }
        return root.put("shifts", array).toString(2)
    }

    suspend fun restoreJson(text: String): Result<Int> = runCatching {
        val array = JSONObject(text).getJSONArray("shifts"); var imported = 0
        for (index in 0 until array.length()) { val item = array.getJSONObject(index); repository.importShift(item.getLong("clockIn"), if (item.isNull("clockOut")) null else item.getLong("clockOut"), item.optLong("totalBreakMillis", 0L), item.optString("note", "")).getOrThrow(); imported++ }
        messages.emit("Imported $imported shifts"); imported
    }

    private fun csv(value: String): String = "\"${value.replace("\"", "\"\"")}\""
}
