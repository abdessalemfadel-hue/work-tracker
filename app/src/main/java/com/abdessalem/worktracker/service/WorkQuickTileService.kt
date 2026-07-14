package com.abdessalem.worktracker.service

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.os.Build
import androidx.core.content.ContextCompat
import com.abdessalem.worktracker.data.repository.ShiftRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WorkQuickTileService : TileService() {
    @Inject lateinit var repository: ShiftRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            if (repository.getActive() == null) repository.clockIn() else repository.clockOut()
            if (repository.getActive() != null) {
                ContextCompat.startForegroundService(
                    this@WorkQuickTileService,
                    Intent(this@WorkQuickTileService, ActiveShiftService::class.java)
                        .setAction(ActiveShiftService.ACTION_REFRESH)
                )
            }
            refreshTile()
        }
    }

    private fun refreshTile() {
        scope.launch {
            val active = repository.getActive() != null
            qsTile?.apply {
                state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                label = if (active) "Clock out" else "Clock in"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    subtitle = if (active) "Shift running" else "Work Tracker"
                }
                updateTile()
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
