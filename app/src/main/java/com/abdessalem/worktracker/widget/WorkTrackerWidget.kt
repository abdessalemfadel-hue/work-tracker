package com.abdessalem.worktracker.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.unit.dp
import com.abdessalem.worktracker.R
import com.abdessalem.worktracker.data.local.WorkDatabase
import com.abdessalem.worktracker.data.repository.ShiftRepository
import com.abdessalem.worktracker.domain.model.TimeUtils
import com.abdessalem.worktracker.service.ActiveShiftService

class WorkTrackerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val active = WorkDatabase.get(context).shiftDao().getActive()
        provideContent { WidgetContent(active != null, active?.let { TimeUtils.netMillis(it) } ?: 0L) }
    }

    companion object {
        suspend fun updateAll(context: Context) {
            WorkTrackerWidget().updateAll(context)
        }
    }
}

@Composable
private fun WidgetContent(active: Boolean, netMillis: Long) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(R.color.widget_background))
            .padding(16.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Text(
            text = if (active) "Working now" else "Not working",
            style = TextStyle(
                color = ColorProvider(if (active) R.color.widget_primary else R.color.widget_muted),
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(GlanceModifier.height(6.dp))
        Text(
            text = if (active) TimeUtils.formatDuration(netMillis) else "00:00",
            style = TextStyle(
                color = ColorProvider(R.color.widget_text),
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(GlanceModifier.height(10.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                text = if (active) "Tap to clock out" else "Tap to clock in",
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .clickable(actionRunCallback<ToggleShiftWidgetAction>()),
                style = TextStyle(color = ColorProvider(R.color.widget_primary))
            )
        }
    }
}

class ToggleShiftWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val database = WorkDatabase.get(context)
        val repository = ShiftRepository(database, database.shiftDao())
        if (repository.getActive() == null) repository.clockIn() else repository.clockOut()
        if (repository.getActive() != null) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, ActiveShiftService::class.java).setAction(ActiveShiftService.ACTION_REFRESH)
            )
        }
        WorkTrackerWidget().updateAll(context)
    }
}

class WorkTrackerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WorkTrackerWidget()
}
