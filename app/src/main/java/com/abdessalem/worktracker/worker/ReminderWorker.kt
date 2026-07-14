package com.abdessalem.worktracker.worker

import android.app.NotificationManager
import android.content.Context
import androidx.hilt.EntryPoints
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.abdessalem.worktracker.data.preferences.UserPreferences
import com.abdessalem.worktracker.data.repository.ShiftRepository
import com.abdessalem.worktracker.domain.model.TimeUtils
import com.abdessalem.worktracker.notification.NotificationFactory
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderWorkerEntryPoint {
    fun repository(): ShiftRepository
    fun preferences(): UserPreferences
    fun notificationFactory(): NotificationFactory
}

class ReminderWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val entry = EntryPoints.get(applicationContext, ReminderWorkerEntryPoint::class.java)
        val settings = entry.preferences().settings.first()
        if (!settings.remindersEnabled || !settings.notificationEnabled) return Result.success()
        val active = entry.repository().getActive() ?: return Result.success()
        val net = TimeUtils.netMillis(active)
        val target = (settings.scheduledHoursPerDay * 3_600_000.0).toLong()
        val notification = when {
            net >= target + 2 * 3_600_000L -> entry.notificationFactory().reminder("Long shift detected", "You have worked ${TimeUtils.formatDuration(net)}. Consider clocking out or adding a note.")
            net >= target -> entry.notificationFactory().reminder("Daily target reached", "You completed ${TimeUtils.formatDuration(target)} of net work time.")
            else -> null
        }
        if (notification != null) {
            entry.notificationFactory().createChannels()
            applicationContext.getSystemService(NotificationManager::class.java).notify(7201, notification)
        }
        return Result.success()
    }
}
