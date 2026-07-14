package com.abdessalem.worktracker.ui.more

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abdessalem.worktracker.domain.model.TimeUtils
import com.abdessalem.worktracker.domain.model.WorkSettings
import com.abdessalem.worktracker.ui.theme.WorkCoral
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

private enum class ExportType { CSV, JSON }

@Composable
fun MoreScreen(contentPadding: PaddingValues, viewModel: MoreViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle(); val context = LocalContext.current; val scope = rememberCoroutineScope()
    var settingsDialog by remember { mutableStateOf(false) }; var salaryDialog by remember { mutableStateOf(false) }; var aboutDialog by remember { mutableStateOf(false) }; var deleteDialog by remember { mutableStateOf(false) }; var pendingExport by remember { mutableStateOf(ExportType.CSV) }
    val createDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri -> if (uri != null) scope.launch { writeText(context, uri, if (pendingExport == ExportType.CSV) viewModel.csvText() else viewModel.jsonText()); Toast.makeText(context, "Export complete", Toast.LENGTH_SHORT).show() } }
    val restoreDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) scope.launch { val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }; if (text == null) Toast.makeText(context, "Could not read backup", Toast.LENGTH_SHORT).show() else viewModel.restoreJson(text).onFailure { Toast.makeText(context, it.message ?: "Restore failed", Toast.LENGTH_LONG).show() } } }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> Toast.makeText(context, if (granted) "Notifications enabled" else "Notification permission denied", Toast.LENGTH_SHORT).show() }
    LaunchedEffect(Unit) { viewModel.messages.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() } }

    val monthlyNet = state.shifts.filter { java.time.Instant.ofEpochMilli(it.clockIn).atZone(ZoneId.systemDefault()).toLocalDate().month == LocalDate.now().month }.sumOf { TimeUtils.netMillis(it) }
    val hourly = estimatedHourlyRate(state.settings)

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = contentPadding.calculateTopPadding() + 18.dp, bottom = contentPadding.calculateBottomPadding() + 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("More", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("Reports, salary estimates, settings and backups", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { SectionCard(Icons.Rounded.Calculate, "Salary estimate") { Text("${state.settings.currency} ${String.format(Locale.US, "%.2f", hourly)} estimated hourly rate"); Text("${state.settings.currency} ${String.format(Locale.US, "%.0f", monthlyNet / 3_600_000.0 * hourly)} estimated from this month’s recorded hours"); Text("Estimates only — not legal payroll calculations.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); FilledTonalButton(onClick = { salaryDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("Configure salary") } } }
        item { SectionCard(Icons.Rounded.Description, "Reports") { Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) { Button(onClick = { pendingExport = ExportType.CSV; createDocument.launch("work-hours-${LocalDate.now()}.csv") }, modifier = Modifier.weight(1f)) { Text("Export CSV") }; FilledTonalButton(onClick = { pendingExport = ExportType.JSON; createDocument.launch("work-tracker-backup-${LocalDate.now()}.json") }, modifier = Modifier.weight(1f)) { Text("Backup JSON") } } } }
        item { SectionCard(Icons.Rounded.Backup, "Backup and restore") { FilledTonalButton(onClick = { restoreDocument.launch(arrayOf("application/json", "text/plain")) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Restore, null); Spacer(Modifier.size(8.dp)); Text("Restore JSON backup") } } }
        item { SectionCard(Icons.Rounded.Notifications, "Notifications") { Text("Persistent timer, lock-screen controls, target and overtime reminders."); FilledTonalButton(onClick = { if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else Toast.makeText(context, "Notifications are available", Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth()) { Text("Enable notifications") } } }
        item { SectionCard(Icons.Rounded.Settings, "Settings") { Text("Daily target: ${state.settings.scheduledHoursPerDay}h"); Text("Expected break: ${state.settings.expectedBreakMinutes} min"); Text("Theme: ${if (state.settings.darkMode) "Dark" else "Light"}"); FilledTonalButton(onClick = { settingsDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("Open settings") } } }
        item { SectionCard(Icons.Rounded.Info, "About") { Text("Offline-first. No ads, accounts, analytics, location, microphone or internet permission."); Text("Samsung Now Brief has no public third-party API; this app uses supported live notifications, widgets and a Quick Settings tile.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); FilledTonalButton(onClick = { aboutDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("App details") } } }
        item { FilledTonalButton(onClick = { deleteDialog = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.DeleteForever, null, tint = WorkCoral); Spacer(Modifier.size(8.dp)); Text("Delete all data", color = WorkCoral) } }
    }

    if (settingsDialog) SettingsDialog(state.settings, { settingsDialog = false }) { viewModel.updateSettings(it); settingsDialog = false }
    if (salaryDialog) SalaryDialog(state.settings, { salaryDialog = false }) { viewModel.updateSettings(it); salaryDialog = false }
    if (aboutDialog) AlertDialog(onDismissRequest = { aboutDialog = false }, title = { Text("Work Tracker 4.0") }, text = { Text("Kotlin, Jetpack Compose, Material 3, Room, DataStore and Hilt. Your data stays on this device unless you export it.") }, confirmButton = { TextButton(onClick = { aboutDialog = false }) { Text("Close") } })
    if (deleteDialog) AlertDialog(onDismissRequest = { deleteDialog = false }, title = { Text("Delete all records?") }, text = { Text("Every shift will be permanently removed from this device.") }, confirmButton = { TextButton(onClick = { viewModel.deleteAll(); deleteDialog = false }) { Text("Delete", color = WorkCoral) } }, dismissButton = { TextButton(onClick = { deleteDialog = false }) { Text("Cancel") } })
}

@Composable
private fun SectionCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, content: @Composable ColumnScope.() -> Unit) { Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null); Spacer(Modifier.size(8.dp)); Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }; content() } } }

@Composable
private fun SettingsDialog(settings: WorkSettings, onDismiss: () -> Unit, onSave: (WorkSettings) -> Unit) {
    var hours by remember { mutableStateOf(settings.scheduledHoursPerDay.toString()) }; var breakMinutes by remember { mutableStateOf(settings.expectedBreakMinutes.toString()) }; var reminders by remember { mutableStateOf(settings.remindersEnabled) }; var dark by remember { mutableStateOf(settings.darkMode) }; var dynamic by remember { mutableStateOf(settings.dynamicColorEnabled) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Settings") }, text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { OutlinedTextField(hours, { hours = it }, label = { Text("Scheduled hours per day") }, singleLine = true); OutlinedTextField(breakMinutes, { breakMinutes = it.filter(Char::isDigit) }, label = { Text("Expected break minutes") }, singleLine = true); ToggleRow("Reminders", reminders) { reminders = it }; ToggleRow("Dark mode", dark) { dark = it }; ToggleRow("Dynamic color", dynamic) { dynamic = it } } }, confirmButton = { TextButton(onClick = { onSave(settings.copy(scheduledHoursPerDay = hours.toDoubleOrNull()?.coerceIn(1.0, 24.0) ?: settings.scheduledHoursPerDay, expectedBreakMinutes = breakMinutes.toIntOrNull()?.coerceIn(0, 240) ?: settings.expectedBreakMinutes, remindersEnabled = reminders, darkMode = dark, dynamicColorEnabled = dynamic)) }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun SalaryDialog(settings: WorkSettings, onDismiss: () -> Unit, onSave: (WorkSettings) -> Unit) {
    var salary by remember { mutableStateOf(settings.monthlySalary.toString()) }; var currency by remember { mutableStateOf(settings.currency) }; var multiplier by remember { mutableStateOf(settings.overtimeMultiplier.toString()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Salary estimate") }, text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { OutlinedTextField(salary, { salary = it }, label = { Text("Monthly salary") }, singleLine = true); OutlinedTextField(currency, { currency = it.take(6).uppercase() }, label = { Text("Currency") }, singleLine = true); OutlinedTextField(multiplier, { multiplier = it }, label = { Text("Overtime multiplier") }, singleLine = true) } }, confirmButton = { TextButton(onClick = { onSave(settings.copy(monthlySalary = salary.toDoubleOrNull()?.coerceAtLeast(0.0) ?: settings.monthlySalary, currency = currency.ifBlank { settings.currency }, overtimeMultiplier = multiplier.toDoubleOrNull()?.coerceIn(1.0, 5.0) ?: settings.overtimeMultiplier)) }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(label); Switch(checked = checked, onCheckedChange = onChecked) } }
private fun estimatedHourlyRate(settings: WorkSettings): Double { val monthlyHours = settings.workingDays.size.coerceAtLeast(1) * 4.33 * settings.scheduledHoursPerDay; return if (monthlyHours <= 0.0) 0.0 else settings.monthlySalary / monthlyHours }
private fun writeText(context: android.content.Context, uri: Uri, text: String) { context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(text) } }
