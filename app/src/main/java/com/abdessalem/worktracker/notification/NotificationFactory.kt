package com.abdessalem.worktracker.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.abdessalem.worktracker.R
import com.abdessalem.worktracker.data.local.ShiftEntity
import com.abdessalem.worktracker.domain.model.TimeUtils
import com.abdessalem.worktracker.domain.model.WorkSettings
import com.abdessalem.worktracker.service.ActiveShiftService
import com.abdessalem.worktracker.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationFactory @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val ACTIVE_CHANNEL = "active_shift"
        const val REMINDER_CHANNEL = "work_reminders"
        const val ACTIVE_NOTIFICATION_ID = 7101
    }

    fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                ACTIVE_CHANNEL,
                context.getString(R.string.active_shift_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.active_shift_channel_description)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                REMINDER_CHANNEL,
                "Work reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Scheduled and overtime work reminders" }
        )
    }

    fun loading(): Notification = NotificationCompat.Builder(context, ACTIVE_CHANNEL)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle("Work Tracker")
        .setContentText("Preparing active shift…")
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .build()

    fun active(shift: ShiftEntity, settings: WorkSettings, now: Long): Notification {
        val openIntent = PendingIntent.getActivity(
            context,
            10,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val breakAction = serviceAction(
            requestCode = 11,
            action = ActiveShiftService.ACTION_TOGGLE_BREAK
        )
        val clockOutAction = serviceAction(
            requestCode = 12,
            action = ActiveShiftService.ACTION_CLOCK_OUT
        )

        val net = TimeUtils.netMillis(shift, now)
        val breaks = TimeUtils.breakMillis(shift, now)
        val overtime = TimeUtils.overtimeMillis(shift, settings.scheduledHoursPerDay, now)
        val expectedFinish = TimeUtils.expectedFinishMillis(
            shift,
            settings.scheduledHoursPerDay,
            settings.expectedBreakMinutes,
            now
        )
        val isBreak = shift.breakStart != null
        val state = when {
            isBreak -> "On break"
            overtime > 0 -> "Overtime"
            else -> "Working now"
        }
        val line = buildString {
            append("Net ${TimeUtils.formatDuration(net)}")
            append(" • Break ${TimeUtils.formatDuration(breaks)}")
            append(" • Finish ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(expectedFinish)}")
        }

        return NotificationCompat.Builder(context, ACTIVE_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(state)
            .setContentText(line)
            .setStyle(NotificationCompat.BigTextStyle().bigText(line))
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setUsesChronometer(!isBreak)
            .setWhen(shift.clockIn)
            .setShowWhen(true)
            .addAction(0, if (isBreak) "Resume" else "Break", breakAction)
            .addAction(0, "Clock out", clockOutAction)
            .build()
    }

    fun reminder(title: String, body: String): Notification {
        val openIntent = PendingIntent.getActivity(
            context,
            20,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, REMINDER_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    private fun serviceAction(requestCode: Int, action: String): PendingIntent =
        PendingIntent.getService(
            context,
            requestCode,
            Intent(context, ActiveShiftService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
