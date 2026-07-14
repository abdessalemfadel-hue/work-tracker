package com.abdessalem.worktracker

import android.app.Application
import androidx.work.Configuration
import com.abdessalem.worktracker.worker.WorkScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class WorkTrackerApp : Application(), Configuration.Provider {
    @Inject lateinit var workScheduler: WorkScheduler

    override fun onCreate() {
        super.onCreate()
        workScheduler.scheduleDailyChecks()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setMinimumLoggingLevel(android.util.Log.INFO).build()
}
