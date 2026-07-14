package com.abdessalem.worktracker.ui.history

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abdessalem.worktracker.data.local.ShiftEntity
import com.abdessalem.worktracker.domain.model.ShiftSource
import com.abdessalem.worktracker.domain.model.TimeUtils
import com.abdessalem.worktracker.ui.theme.WorkAmber
import com.abdessalem.worktracker.ui.theme.WorkCoral
import com.abdessalem.worktracker.ui.theme.WorkGreen
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class HistoryFilter { ALL, COMPLETED, ACTIVE, OVERTIME, MANUAL }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(contentPadding: PaddingValues, viewModel: HistoryViewModel = hiltViewModel()) {
    val shifts by viewModel.shifts.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(HistoryFilter.ALL) }
    var filterMenu by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ShiftEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<ShiftEntity?>(null) }

    LaunchedEffect(Unit) { viewModel.messages.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() } }

    val visible = shifts.filter { shift ->
        val filterMatches = when (filter) {
            HistoryFilter.ALL -> true
            HistoryFilter.COMPLETED -> shift.clockOut != null
            HistoryFilter.ACTIVE -> shift.clockOut == null
            HistoryFilter.OVERTIME -> TimeUtils.netMillis(shift) > 8 * 3_600_000L
            HistoryFilter.MANUAL -> shift.source == ShiftSource.MANUAL
        }
        val queryMatches = search.isBlank() || shift.note.contains(search, ignoreCase = true) || formatDate(shift.clockIn).contains(search, ignoreCase = true)
        filterMatches && queryMatches
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = contentPadding.calculateTopPadding() + 18.dp, bottom = contentPadding.calculateBottomPadding() + 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("History", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("Review, correct and document your records", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { OutlinedTextField(search, { search = it }, leadingIcon = { Icon(Icons.Rounded.Search, null) }, label = { Text("Search notes or dates") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { editing = newManualShift(); showEditor = true }, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.size(6.dp)); Text("Add shift") }
                Column(modifier = Modifier.weight(1f)) {
                    FilledTonalButton(onClick = { filterMenu = true }, modifier = Modifier.fillMaxWidth()) { Text(filter.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    DropdownMenu(expanded = filterMenu, onDismissRequest = { filterMenu = false }) { HistoryFilter.entries.forEach { option -> DropdownMenuItem(text = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) }, onClick = { filter = option; filterMenu = false }) } }
                }
            }
        }
        if (visible.isEmpty()) item { Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(22.dp)) { Column(Modifier.padding(24.dp)) { Text("No matching shifts", style = MaterialTheme.typography.titleLarge); Text("Clock in or add a past shift manually.", color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
        else items(visible, key = { it.id }) { shift -> ShiftCard(shift, { editing = shift; showEditor = true }, { deleting = shift }, { viewModel.duplicate(shift.id) }) }
    }

    if (showEditor && editing != null) ShiftEditorDialog(requireNotNull(editing), { showEditor = false }, { viewModel.save(it); showEditor = false })
    deleting?.let { shift -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Delete this shift?") }, text = { Text("This cannot be undone.") }, confirmButton = { TextButton(onClick = { viewModel.delete(shift.id); deleting = null }) { Text("Delete", color = WorkCoral) } }, dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }) }
}

@Composable
private fun ShiftCard(shift: ShiftEntity, onEdit: () -> Unit, onDelete: () -> Unit, onDuplicate: () -> Unit) {
    val net = TimeUtils.netMillis(shift); val overtime = (net - 8 * 3_600_000L).coerceAtLeast(0)
    val accent = when { shift.clockOut == null -> WorkCoral; overtime > 0 -> WorkAmber; else -> WorkGreen }
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text(formatDate(shift.clockIn), fontWeight = FontWeight.Bold); Text("${formatTime(shift.clockIn)} → ${shift.clockOut?.let(::formatTime) ?: "Active"}", color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text(TimeUtils.formatDuration(net), color = accent, fontWeight = FontWeight.Bold) }
            Text("Gross ${TimeUtils.formatDuration(TimeUtils.grossMillis(shift))} • Break ${TimeUtils.formatDuration(TimeUtils.breakMillis(shift))}" + if (overtime > 0) " • OT ${TimeUtils.formatDuration(overtime)}" else "", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            AnimatedVisibility(shift.note.isNotBlank()) { Text("“${shift.note}”") }
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) { IconButton(onClick = onDuplicate, enabled = shift.clockOut != null) { Icon(Icons.Rounded.ContentCopy, "Duplicate") }; IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, "Edit") }; IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, "Delete", tint = WorkCoral) } }
        }
    }
}

@Composable
private fun ShiftEditorDialog(shift: ShiftEntity, onDismiss: () -> Unit, onSave: (ShiftEntity) -> Unit) {
    var date by remember(shift.id) { mutableStateOf(formatDateInput(shift.clockIn)) }; var start by remember(shift.id) { mutableStateOf(formatTime(shift.clockIn)) }; var end by remember(shift.id) { mutableStateOf(formatTime(shift.clockOut ?: System.currentTimeMillis())) }; var breakMinutes by remember(shift.id) { mutableStateOf((shift.totalBreakMillis / 60_000L).toString()) }; var note by remember(shift.id) { mutableStateOf(shift.note) }; var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (shift.id == 0L) "Add shift" else "Edit shift") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(date, { date = it }, label = { Text("Date yyyy-MM-dd") }, singleLine = true); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(start, { start = it }, label = { Text("Start HH:mm") }, modifier = Modifier.weight(1f), singleLine = true); OutlinedTextField(end, { end = it }, label = { Text("End HH:mm") }, modifier = Modifier.weight(1f), singleLine = true) }; OutlinedTextField(breakMinutes, { breakMinutes = it.filter(Char::isDigit) }, label = { Text("Break minutes") }, singleLine = true); OutlinedTextField(note, { note = it }, label = { Text("Note") }, minLines = 2); error?.let { Text(it, color = MaterialTheme.colorScheme.error) } } }, confirmButton = { TextButton(onClick = { runCatching { val startMillis = parseDateTime(date, start); val endMillis = parseDateTime(date, end); require(endMillis > startMillis) { "End must be after start" }; val breaks = (breakMinutes.toLongOrNull() ?: 0L) * 60_000L; require(breaks < endMillis - startMillis) { "Break is longer than the shift" }; shift.copy(clockIn = startMillis, clockOut = endMillis, breakStart = null, totalBreakMillis = breaks, note = note, source = if (shift.id == 0L) ShiftSource.MANUAL else shift.source) }.onSuccess(onSave).onFailure { error = it.message ?: "Invalid values" } }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

private fun newManualShift(): ShiftEntity { val now = System.currentTimeMillis(); return ShiftEntity(clockIn = now - 8 * 3_600_000L, clockOut = now, totalBreakMillis = 60 * 60_000L, source = ShiftSource.MANUAL) }
private val displayDate = DateTimeFormatter.ofPattern("EEEE, d MMM yyyy"); private val inputDate = DateTimeFormatter.ISO_LOCAL_DATE; private val inputTime = DateTimeFormatter.ofPattern("HH:mm")
private fun formatDate(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(displayDate)
private fun formatDateInput(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(inputDate)
private fun formatTime(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime().format(inputTime)
private fun parseDateTime(date: String, time: String): Long = LocalDateTime.of(LocalDate.parse(date.trim()), LocalTime.parse(time.trim(), inputTime)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
