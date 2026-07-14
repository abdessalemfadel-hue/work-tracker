package com.abdessalem.worktracker.ui.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abdessalem.worktracker.domain.model.TimeUtils
import com.abdessalem.worktracker.ui.components.ActionFeedback
import com.abdessalem.worktracker.ui.components.ActionFeedbackOverlay
import com.abdessalem.worktracker.ui.theme.WorkAmber
import com.abdessalem.worktracker.ui.theme.WorkBlue
import com.abdessalem.worktracker.ui.theme.WorkCoral
import com.abdessalem.worktracker.ui.theme.WorkGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    contentPadding: PaddingValues,
    onOpenHistory: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var overlay by remember { mutableStateOf<ActionFeedback?>(null) }
    var noteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.feedback.collect { overlay = it }
    }
    LaunchedEffect(Unit) {
        viewModel.messages.collect { android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show() }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = contentPadding.calculateTopPadding() + 18.dp,
                bottom = contentPadding.calculateBottomPadding() + 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("Work Tracker", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Your private, verifiable work record", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                ActiveTimerCard(state = state)
            }
            item {
                ActionButtons(
                    active = state.activeShift != null,
                    onBreak = state.activeShift?.breakStart != null,
                    onToggleClock = viewModel::toggleClock,
                    onToggleBreak = viewModel::toggleBreak
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(
                        onClick = { noteDialog = true },
                        enabled = state.activeShift != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.EditNote, null)
                        Spacer(Modifier.size(8.dp))
                        Text("Add note")
                    }
                    FilledTonalButton(onClick = onOpenHistory, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Add, null)
                        Spacer(Modifier.size(8.dp))
                        Text("Add shift")
                    }
                }
            }
            item {
                Text("Today", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            item {
                MetricsGrid(state)
            }
            item {
                FilledTonalButton(onClick = onOpenHistory, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.History, null)
                    Spacer(Modifier.size(8.dp))
                    Text("View timeline and history")
                }
            }
            item {
                Text(
                    "Offline by design. No account, location, microphone or internet permission.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
            }
        }

        ActionFeedbackOverlay(
            feedback = overlay,
            modifier = Modifier.matchParentSize(),
            onFinished = { overlay = null }
        )
    }

    if (noteDialog) {
        NoteDialog(
            onDismiss = { noteDialog = false },
            onSave = { viewModel.addNote(it); noteDialog = false }
        )
    }
}

@Composable
private fun ActiveTimerCard(state: com.abdessalem.worktracker.domain.model.DashboardState) {
    val active = state.activeShift
    val progress = (state.todayNetMillis / (state.settings.scheduledHoursPerDay * 3_600_000.0)).toFloat().coerceIn(0f, 1f)
    val status = when {
        active == null -> "Not working"
        active.breakStart != null -> "On break"
        state.overtimeMillis > 0 -> "Overtime"
        else -> "Working now"
    }
    val statusColor = when {
        active == null -> MaterialTheme.colorScheme.onSurfaceVariant
        active.breakStart != null -> WorkAmber
        state.overtimeMillis > 0 -> WorkCoral
        else -> WorkGreen
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AssistChip(onClick = {}, label = { Text(status) })
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(230.dp)) {
                Canvas(Modifier.fillMaxSize()) {
                    drawArc(
                        color = Color.White.copy(alpha = 0.08f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = statusColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        TimeUtils.formatDuration(state.todayNetMillis, withSeconds = active != null),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("net today", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            active?.let {
                val formatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
                Text("Started ${formatter.format(Date(it.clockIn))}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                state.expectedFinishMillis?.let { finish ->
                    Text("Expected finish ${formatter.format(Date(finish))}", color = WorkBlue)
                }
            }
        }
    }
}

@Composable
private fun ActionButtons(
    active: Boolean,
    onBreak: Boolean,
    onToggleClock: (Float, Float) -> Unit,
    onToggleBreak: (Float, Float) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        AnimatedActionButton(
            label = if (active) "Clock out" else "Clock in",
            icon = if (active) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
            color = if (active) WorkCoral else WorkGreen,
            modifier = Modifier.weight(1f),
            onClick = onToggleClock
        )
        AnimatedActionButton(
            label = if (onBreak) "Resume" else "Break",
            icon = if (onBreak) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
            color = WorkAmber,
            enabled = active,
            modifier = Modifier.weight(1f),
            onClick = onToggleBreak
        )
    }
}

@Composable
private fun AnimatedActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (Float, Float) -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, spring(), label = "buttonScale")
    var center by remember { mutableStateOf(Offset.Zero) }
    val haptic = LocalHapticFeedback.current

    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick(center.x, center.y)
        },
        enabled = enabled,
        interactionSource = interaction,
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color(0xFF07111E)),
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(vertical = 17.dp),
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onGloballyPositioned { coordinates -> center = coordinates.boundsInRoot().center }
    ) {
        Icon(icon, null)
        Spacer(Modifier.size(8.dp))
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MetricsGrid(state: com.abdessalem.worktracker.domain.model.DashboardState) {
    val values = listOf(
        "Gross" to TimeUtils.formatDuration(state.todayGrossMillis),
        "Break" to TimeUtils.formatDuration(state.todayBreakMillis),
        "Overtime" to TimeUtils.formatDuration(state.overtimeMillis),
        "Target" to String.format(Locale.US, "%.1fh", state.settings.scheduledHoursPerDay)
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        values.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { (label, value) ->
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Shift note") },
        text = {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Manager request, event, late finish…") },
                minLines = 3
            )
        },
        confirmButton = { TextButton(onClick = { onSave(note) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
