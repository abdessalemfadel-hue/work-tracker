package com.abdessalem.worktracker.domain.model

import com.abdessalem.worktracker.data.local.ShiftEntity

enum class ShiftSource { LIVE, MANUAL, IMPORTED }

data class WorkSettings(
    val scheduledHoursPerDay: Double = 8.0,
    val workingDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6),
    val defaultStartMinutes: Int = 8 * 60,
    val defaultEndMinutes: Int = 17 * 60,
    val expectedBreakMinutes: Int = 60,
    val monthlySalary: Double = 10_000.0,
    val currency: String = "AED",
    val overtimeMultiplier: Double = 1.25,
    val notificationEnabled: Boolean = true,
    val remindersEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val soundEnabled: Boolean = false,
    val biometricLockEnabled: Boolean = false,
    val darkMode: Boolean = true,
    val dynamicColorEnabled: Boolean = true,
    val firstDayOfWeek: Int = 1,
    val startReminderHour: Int = 8,
    val endReminderHour: Int = 18
)

data class WorkStats(
    val todayMillis: Long = 0,
    val weekMillis: Long = 0,
    val monthMillis: Long = 0,
    val overtimeMillis: Long = 0,
    val workedDays: Int = 0,
    val averageDailyMillis: Long = 0,
    val averageWeeklyMillis: Long = 0,
    val longestDayMillis: Long = 0,
    val shortestDayMillis: Long = 0,
    val averageBreakMillis: Long = 0,
    val averageStartMinutes: Int = 0,
    val averageFinishMinutes: Int = 0
)

data class DayWork(
    val epochDay: Long,
    val netMillis: Long,
    val overtimeMillis: Long,
    val shiftCount: Int,
    val hasOpenShift: Boolean,
    val hasManualShift: Boolean
)

data class DashboardState(
    val activeShift: ShiftEntity? = null,
    val settings: WorkSettings = WorkSettings(),
    val todayNetMillis: Long = 0,
    val todayGrossMillis: Long = 0,
    val todayBreakMillis: Long = 0,
    val expectedFinishMillis: Long? = null,
    val overtimeMillis: Long = 0,
    val currentTimeMillis: Long = System.currentTimeMillis(),
    val isLoading: Boolean = true
)
