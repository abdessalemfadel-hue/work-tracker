package com.abdessalem.worktracker.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abdessalem.worktracker.data.local.ShiftEntity
import com.abdessalem.worktracker.domain.model.TimeUtils
import com.abdessalem.worktracker.ui.theme.WorkAmber
import com.abdessalem.worktracker.ui.theme.WorkCoral
import com.abdessalem.worktracker.ui.theme.WorkGreen
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private data class CalendarCell(val date: LocalDate?, val shifts: List<ShiftEntity> = emptyList())

@Composable
fun CalendarScreen(contentPadding: PaddingValues, viewModel: CalendarViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle(); var month by remember { mutableStateOf(YearMonth.now()) }; var selected by remember { mutableStateOf<CalendarCell?>(null) }
    val grouped = state.shifts.groupBy { TimeUtils.epochDay(it.clockIn) }; val cells = remember(month, state.shifts) { monthCells(month, grouped) }
    Column(modifier = Modifier.fillMaxSize().padding(start = 20.dp, end = 20.dp, top = contentPadding.calculateTopPadding() + 18.dp, bottom = contentPadding.calculateBottomPadding() + 10.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Calendar", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("Normal days, overtime and missing clock-outs", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = { month = month.minusMonths(1) }) { Icon(Icons.Rounded.ChevronLeft, "Previous month") }; Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); IconButton(onClick = { month = month.plusMonths(1) }) { Icon(Icons.Rounded.ChevronRight, "Next month") } }
        Row(modifier = Modifier.fillMaxWidth()) { listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { Text(it, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(cells) { cell -> DayCell(cell, state.settings.scheduledHoursPerDay) { selected = cell } } }
    }
    selected?.takeIf { it.date != null }?.let { DayDialog(it, state.settings.scheduledHoursPerDay) { selected = null } }
}

@Composable
private fun DayCell(cell: CalendarCell, scheduledHours: Double, onClick: () -> Unit) {
    val net = cell.shifts.sumOf { TimeUtils.netMillis(it) }; val target = (scheduledHours * 3_600_000).toLong(); val color = when { cell.date == null -> Color.Transparent; cell.shifts.any { it.clockOut == null } -> WorkCoral; net > target -> WorkAmber; net > 0 -> WorkGreen; else -> MaterialTheme.colorScheme.surfaceVariant }
    Box(modifier = Modifier.aspectRatio(0.82f).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp)).clickable(enabled = cell.date != null, onClick = onClick).padding(6.dp), contentAlignment = Alignment.Center) { if (cell.date != null) Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(cell.date.dayOfMonth.toString(), fontWeight = if (cell.date == LocalDate.now()) FontWeight.Bold else FontWeight.Normal); Box(Modifier.padding(top = 5.dp).background(color, CircleShape).aspectRatio(1f).fillMaxWidth(0.24f)) } }
}

@Composable
private fun DayDialog(cell: CalendarCell, scheduledHours: Double, onDismiss: () -> Unit) {
    val net = cell.shifts.sumOf { TimeUtils.netMillis(it) }; val gross = cell.shifts.sumOf { TimeUtils.grossMillis(it) }; val breaks = cell.shifts.sumOf { TimeUtils.breakMillis(it) }; val overtime = (net - (scheduledHours * 3_600_000).toLong()).coerceAtLeast(0)
    AlertDialog(onDismissRequest = onDismiss, title = { Text(requireNotNull(cell.date).format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))) }, text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { if (cell.shifts.isEmpty()) Text("No work recorded", color = MaterialTheme.colorScheme.onSurfaceVariant); cell.shifts.forEach { shift -> Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Column(Modifier.padding(12.dp)) { Text("${formatTime(shift.clockIn)} → ${shift.clockOut?.let(::formatTime) ?: "Active"}", fontWeight = FontWeight.Bold); Text("${TimeUtils.formatDuration(TimeUtils.netMillis(shift))} net • ${shift.source.name.lowercase()}"); if (shift.note.isNotBlank()) Text(shift.note, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }; if (cell.shifts.isNotEmpty()) { Text("Gross ${TimeUtils.formatDuration(gross)}"); Text("Break ${TimeUtils.formatDuration(breaks)}"); Text("Net ${TimeUtils.formatDuration(net)}", fontWeight = FontWeight.Bold); Text("Overtime ${TimeUtils.formatDuration(overtime)}", color = WorkAmber) } } }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}

private fun monthCells(month: YearMonth, grouped: Map<Long, List<ShiftEntity>>): List<CalendarCell> { val first = month.atDay(1); val result = MutableList(first.dayOfWeek.value - 1) { CalendarCell(null) }; (1..month.lengthOfMonth()).forEach { day -> val date = month.atDay(day); result += CalendarCell(date, grouped[date.toEpochDay()].orEmpty()) }; while (result.size % 7 != 0) result += CalendarCell(null); return result }
private fun formatTime(millis: Long): String = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
