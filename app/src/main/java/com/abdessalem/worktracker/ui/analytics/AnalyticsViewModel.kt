package com.abdessalem.worktracker.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abdessalem.worktracker.data.local.ShiftEntity
import com.abdessalem.worktracker.data.preferences.UserPreferences
import com.abdessalem.worktracker.data.repository.ShiftRepository
import com.abdessalem.worktracker.domain.model.DayWork
import com.abdessalem.worktracker.domain.model.TimeUtils
import com.abdessalem.worktracker.domain.model.WorkSettings
import com.abdessalem.worktracker.domain.model.WorkStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class AnalyticsState(val settings: WorkSettings = WorkSettings(), val stats: WorkStats = WorkStats(), val lastSevenDays: List<DayWork> = emptyList(), val monthDays: List<DayWork> = emptyList())

@HiltViewModel
class AnalyticsViewModel @Inject constructor(repository: ShiftRepository, preferences: UserPreferences) : ViewModel() {
    val state: StateFlow<AnalyticsState> = combine(repository.observeAll(), preferences.settings) { shifts, settings -> calculate(shifts, settings) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsState())

    private fun calculate(shifts: List<ShiftEntity>, settings: WorkSettings): AnalyticsState {
        val now = System.currentTimeMillis(); val todayStart = TimeUtils.startOfDayMillis(now); val weekStart = TimeUtils.startOfWeekMillis(now); val monthStart = TimeUtils.startOfMonthMillis(now); val monthEnd = TimeUtils.endOfMonthMillis(now); val grouped = shifts.groupBy { TimeUtils.epochDay(it.clockIn) }; val target = (settings.scheduledHoursPerDay * 3_600_000).toLong()
        fun total(from: Long, to: Long) = shifts.filter { it.clockIn in from until to }.sumOf { TimeUtils.netMillis(it, now) }
        val monthGroups = grouped.filterKeys { epoch -> LocalDate.ofEpochDay(epoch).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() in monthStart until monthEnd }
        val dailyTotals = monthGroups.values.map { day -> day.sumOf { TimeUtils.netMillis(it, now) } }
        val completed = shifts.filter { it.clockOut != null }
        val averageStart = completed.takeIf { it.isNotEmpty() }?.map { TimeUtils.minutesOfDay(it.clockIn) }?.average()?.toInt() ?: 0
        val averageFinish = completed.takeIf { it.isNotEmpty() }?.map { TimeUtils.minutesOfDay(requireNotNull(it.clockOut)) }?.average()?.toInt() ?: 0
        val averageBreak = completed.takeIf { it.isNotEmpty() }?.map { TimeUtils.breakMillis(it) }?.average()?.toLong() ?: 0L
        val overtime = dailyTotals.sumOf { (it - target).coerceAtLeast(0) }
        val stats = WorkStats(todayMillis = total(todayStart, now + 1), weekMillis = total(weekStart, now + 1), monthMillis = total(monthStart, now + 1), overtimeMillis = overtime, workedDays = dailyTotals.size, averageDailyMillis = dailyTotals.takeIf { it.isNotEmpty() }?.average()?.toLong() ?: 0L, averageWeeklyMillis = if (dailyTotals.isEmpty()) 0L else dailyTotals.sum() / ((dailyTotals.size + 6) / 7).coerceAtLeast(1), longestDayMillis = dailyTotals.maxOrNull() ?: 0L, shortestDayMillis = dailyTotals.minOrNull() ?: 0L, averageBreakMillis = averageBreak, averageStartMinutes = averageStart, averageFinishMinutes = averageFinish)
        val today = LocalDate.now(); val lastSeven = (6 downTo 0).map { offset -> val date = today.minusDays(offset.toLong()); toDayWork(date, grouped[date.toEpochDay()].orEmpty(), target, now) }; val monthDays = (1..today.lengthOfMonth()).map { day -> val date = today.withDayOfMonth(day); toDayWork(date, grouped[date.toEpochDay()].orEmpty(), target, now) }
        return AnalyticsState(settings, stats, lastSeven, monthDays)
    }

    private fun toDayWork(date: LocalDate, shifts: List<ShiftEntity>, target: Long, now: Long): DayWork { val net = shifts.sumOf { TimeUtils.netMillis(it, now) }; return DayWork(date.toEpochDay(), net, (net - target).coerceAtLeast(0), shifts.size, shifts.any { it.clockOut == null }, shifts.any { it.source.name == "MANUAL" }) }
}
