package com.abdessalem.worktracker.ui.analytics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abdessalem.worktracker.domain.model.DayWork
import com.abdessalem.worktracker.domain.model.TimeUtils
import com.abdessalem.worktracker.ui.theme.WorkAmber
import com.abdessalem.worktracker.ui.theme.WorkBlue
import com.abdessalem.worktracker.ui.theme.WorkCoral
import com.abdessalem.worktracker.ui.theme.WorkGreen
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max

@Composable
fun AnalyticsScreen(contentPadding: PaddingValues, viewModel: AnalyticsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = contentPadding.calculateTopPadding() + 18.dp, bottom = contentPadding.calculateBottomPadding() + 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Analytics", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("Daily, weekly and monthly evidence", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) { MetricCard("Today", TimeUtils.formatDuration(state.stats.todayMillis), WorkGreen, Modifier.weight(1f)); MetricCard("Week", TimeUtils.formatDuration(state.stats.weekMillis), WorkBlue, Modifier.weight(1f)) } }
        item { MetricCard("This month", TimeUtils.formatDuration(state.stats.monthMillis), WorkAmber, Modifier.fillMaxWidth()) }
        item { Text("Last 7 days", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        item { SevenDayChart(state.lastSevenDays) }
        item { Text("Monthly breakdown", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        item { StatsGrid(listOf("Worked days" to state.stats.workedDays.toString(), "Daily average" to TimeUtils.formatDuration(state.stats.averageDailyMillis), "Weekly average" to TimeUtils.formatDuration(state.stats.averageWeeklyMillis), "Overtime" to TimeUtils.formatDuration(state.stats.overtimeMillis), "Longest day" to TimeUtils.formatDuration(state.stats.longestDayMillis), "Shortest day" to TimeUtils.formatDuration(state.stats.shortestDayMillis), "Average break" to TimeUtils.formatDuration(state.stats.averageBreakMillis), "Extra workdays" to String.format(Locale.US, "%.1f", state.stats.overtimeMillis / (state.settings.scheduledHoursPerDay * 3_600_000.0)), "Average arrival" to minutesToTime(state.stats.averageStartMinutes), "Average finish" to minutesToTime(state.stats.averageFinishMinutes))) }
        item { Text("Monthly heatmap", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        item { MonthHeatmap(state.monthDays) }
    }
}

@Composable
private fun MetricCard(title: String, value: String, accent: Color, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(18.dp)) { Text(title, color = accent, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) } }
}

@Composable
private fun SevenDayChart(days: List<DayWork>) {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(days) { reveal.snapTo(0f); reveal.animateTo(1f, tween(700)) }
    val maxValue = max(8 * 3_600_000L, days.maxOfOrNull { it.netMillis } ?: 1L)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Canvas(Modifier.fillMaxWidth().height(180.dp)) {
                if (days.isEmpty()) return@Canvas
                val slot = size.width / days.size; val barWidth = slot * 0.55f
                days.forEachIndexed { index, day ->
                    val ratio = (day.netMillis.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f) * reveal.value; val height = size.height * ratio; val x = slot * index + (slot - barWidth) / 2
                    drawRoundRect(color = when { day.hasOpenShift -> WorkCoral; day.overtimeMillis > 0 -> WorkAmber; day.netMillis > 0 -> WorkGreen; else -> Color.White.copy(alpha = 0.08f) }, topLeft = Offset(x, size.height - height), size = androidx.compose.ui.geometry.Size(barWidth, max(5f, height)), cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f))
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) { days.forEach { day -> Text(LocalDate.ofEpochDay(day.epochDay).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        }
    }
}

@Composable
private fun StatsGrid(values: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { values.chunked(2).forEach { chunk -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) { chunk.forEach { (label, value) -> Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Column(Modifier.padding(15.dp)) { Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) } } }; if (chunk.size == 1) Spacer(Modifier.weight(1f)) } } }
}

@Composable
private fun MonthHeatmap(days: List<DayWork>) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { days.chunked(7).forEach { week -> Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) { week.forEach { day -> val color = when { day.hasOpenShift -> WorkCoral; day.overtimeMillis > 0 -> WorkAmber; day.netMillis > 0 -> WorkGreen; else -> MaterialTheme.colorScheme.surfaceVariant }; Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) { Canvas(Modifier.size(30.dp)) { drawCircle(color) }; Text(LocalDate.ofEpochDay(day.epochDay).dayOfMonth.toString(), style = MaterialTheme.typography.labelSmall) } }; repeat(7 - week.size) { Spacer(Modifier.weight(1f)) } } } }
    }
}

private fun minutesToTime(minutes: Int): String = if (minutes <= 0) "—" else String.format(Locale.US, "%02d:%02d", minutes / 60, minutes % 60)
