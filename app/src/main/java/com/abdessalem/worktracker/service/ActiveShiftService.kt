package com.abdessalem.worktracker.service

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.abdessalem.worktracker.data.preferences.UserPreferences
import com.abdessalem.worktracker.data.repository.ShiftRepository
import com.abdessalem.worktracker.notification.NotificationFactory
import com.abdessalem.worktracker.widget.WorkTrackerWidget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ActiveShiftService : Service() {
    companion object {
        const val ACTION_REFRESH = "com.abdessalem.worktracker.action.REFRESH"
        const val ACTION_TOGGLE_BREAK = "com.abdessalem.worktracker.action.TOGGLE_BREAK"
        const val ACTION_CLOCK_OUT = "com.abdessalem.worktracker.action.CLOCK_OUT"
        const val ACTION_STATE_CHANGED = "com.abdessalem.worktracker.action.STATE_CHANGED"
    }

    @Inject lateinit var repository: ShiftRepository
    @Inject lateinit var preferences: UserPreferences
    @Inject lateinit var notificationFactory: NotificationFactory

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var refreshJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        notificationFactory.createChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NotificationFactory.ACTIVE_NOTIFICATION_ID, notificationFactory.loading())
        scope.launch {
            when (intent?.action) {
                ACTION_TOGGLE_BREAK -> repository.toggleBreak()
                ACTION_CLOCK_OUT -> repository.clockOut()
            }
            broadcastState()
            startRefreshLoop()
        }
        return START_STICKY
    }

    private fun startRefreshLoop() {
        if (refreshJob?.isActive == true) return
        refreshJob = scope.launch {
            while (isActive) {
                val active = repository.getActive()
                if (active == null) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    broadcastState()
                    WorkTrackerWidget.updateAll(applicationContext)
                    break
                }
                val settings = preferences.settings.first()
                val notification = notificationFactory.active(active, settings, System.currentTimeMillis())
                if (canPostNotifications()) {
                    NotificationManagerCompat.from(this@ActiveShiftService)
                        .notify(NotificationFactory.ACTIVE_NOTIFICATION_ID, notification)
                }
                WorkTrackerWidget.updateAll(applicationContext)
                delay(30_000L)
            }
        }
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun broadcastState() {
        sendBroadcast(Intent(ACTION_STATE_CHANGED).setPackage(packageName))
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
