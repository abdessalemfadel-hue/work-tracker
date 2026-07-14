package com.abdessalem.worktracker.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.abdessalem.worktracker.domain.model.WorkSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "work_settings")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val scheduledHours = doublePreferencesKey("scheduled_hours")
        val workingDays = stringPreferencesKey("working_days")
        val startMinutes = intPreferencesKey("start_minutes")
        val endMinutes = intPreferencesKey("end_minutes")
        val breakMinutes = intPreferencesKey("break_minutes")
        val salary = doublePreferencesKey("salary")
        val currency = stringPreferencesKey("currency")
        val overtimeMultiplier = doublePreferencesKey("overtime_multiplier")
        val notifications = booleanPreferencesKey("notifications")
        val reminders = booleanPreferencesKey("reminders")
        val vibration = booleanPreferencesKey("vibration")
        val sound = booleanPreferencesKey("sound")
        val biometric = booleanPreferencesKey("biometric")
        val darkMode = booleanPreferencesKey("dark_mode")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val firstDay = intPreferencesKey("first_day")
        val startReminderHour = intPreferencesKey("start_reminder_hour")
        val endReminderHour = intPreferencesKey("end_reminder_hour")
    }

    val settings: Flow<WorkSettings> = context.dataStore.data.map { prefs ->
        WorkSettings(
            scheduledHoursPerDay = prefs[Keys.scheduledHours] ?: 8.0,
            workingDays = prefs[Keys.workingDays]?.split(',')?.mapNotNull { it.toIntOrNull() }?.toSet()
                ?: setOf(1, 2, 3, 4, 5, 6),
            defaultStartMinutes = prefs[Keys.startMinutes] ?: 480,
            defaultEndMinutes = prefs[Keys.endMinutes] ?: 1020,
            expectedBreakMinutes = prefs[Keys.breakMinutes] ?: 60,
            monthlySalary = prefs[Keys.salary] ?: 10_000.0,
            currency = prefs[Keys.currency] ?: "AED",
            overtimeMultiplier = prefs[Keys.overtimeMultiplier] ?: 1.25,
            notificationEnabled = prefs[Keys.notifications] ?: true,
            remindersEnabled = prefs[Keys.reminders] ?: true,
            vibrationEnabled = prefs[Keys.vibration] ?: true,
            soundEnabled = prefs[Keys.sound] ?: false,
            biometricLockEnabled = prefs[Keys.biometric] ?: false,
            darkMode = prefs[Keys.darkMode] ?: true,
            dynamicColorEnabled = prefs[Keys.dynamicColor] ?: true,
            firstDayOfWeek = prefs[Keys.firstDay] ?: 1,
            startReminderHour = prefs[Keys.startReminderHour] ?: 8,
            endReminderHour = prefs[Keys.endReminderHour] ?: 18
        )
    }

    suspend fun update(transform: (WorkSettings) -> WorkSettings) {
        var current = WorkSettings()
        context.dataStore.edit { prefs ->
            current = WorkSettings(
                scheduledHoursPerDay = prefs[Keys.scheduledHours] ?: 8.0,
                workingDays = prefs[Keys.workingDays]?.split(',')?.mapNotNull { it.toIntOrNull() }?.toSet()
                    ?: setOf(1, 2, 3, 4, 5, 6),
                defaultStartMinutes = prefs[Keys.startMinutes] ?: 480,
                defaultEndMinutes = prefs[Keys.endMinutes] ?: 1020,
                expectedBreakMinutes = prefs[Keys.breakMinutes] ?: 60,
                monthlySalary = prefs[Keys.salary] ?: 10_000.0,
                currency = prefs[Keys.currency] ?: "AED",
                overtimeMultiplier = prefs[Keys.overtimeMultiplier] ?: 1.25,
                notificationEnabled = prefs[Keys.notifications] ?: true,
                remindersEnabled = prefs[Keys.reminders] ?: true,
                vibrationEnabled = prefs[Keys.vibration] ?: true,
                soundEnabled = prefs[Keys.sound] ?: false,
                biometricLockEnabled = prefs[Keys.biometric] ?: false,
                darkMode = prefs[Keys.darkMode] ?: true,
                dynamicColorEnabled = prefs[Keys.dynamicColor] ?: true,
                firstDayOfWeek = prefs[Keys.firstDay] ?: 1,
                startReminderHour = prefs[Keys.startReminderHour] ?: 8,
                endReminderHour = prefs[Keys.endReminderHour] ?: 18
            )
            val next = transform(current)
            prefs[Keys.scheduledHours] = next.scheduledHoursPerDay
            prefs[Keys.workingDays] = next.workingDays.sorted().joinToString(",")
            prefs[Keys.startMinutes] = next.defaultStartMinutes
            prefs[Keys.endMinutes] = next.defaultEndMinutes
            prefs[Keys.breakMinutes] = next.expectedBreakMinutes
            prefs[Keys.salary] = next.monthlySalary
            prefs[Keys.currency] = next.currency
            prefs[Keys.overtimeMultiplier] = next.overtimeMultiplier
            prefs[Keys.notifications] = next.notificationEnabled
            prefs[Keys.reminders] = next.remindersEnabled
            prefs[Keys.vibration] = next.vibrationEnabled
            prefs[Keys.sound] = next.soundEnabled
            prefs[Keys.biometric] = next.biometricLockEnabled
            prefs[Keys.darkMode] = next.darkMode
            prefs[Keys.dynamicColor] = next.dynamicColorEnabled
            prefs[Keys.firstDay] = next.firstDayOfWeek
            prefs[Keys.startReminderHour] = next.startReminderHour
            prefs[Keys.endReminderHour] = next.endReminderHour
        }
    }
}
