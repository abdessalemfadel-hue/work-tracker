package com.abdessalem.worktracker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.abdessalem.worktracker.data.repository.ShiftRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var repository: ShiftRepository

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (repository.getActive() != null) {
                    ContextCompat.startForegroundService(
                        context,
                        Intent(context, ActiveShiftService::class.java).setAction(ActiveShiftService.ACTION_REFRESH)
                    )
                }
            } finally {
                pending.finish()
            }
        }
    }
}
