package com.abdessalem.worktracker

import android.app.Application
import com.abdessalem.worktracker.worker.WorkScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class WorkTrackerApp : Application() {
    @Inject lateinit var workScheduler: WorkScheduler

    override fun onCreate() {
        super.onCreate()
        workScheduler.scheduleDailyChecks()
    }
}
