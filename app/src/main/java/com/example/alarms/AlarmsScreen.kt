package com.example.alarms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.AlarmCategory
import com.example.core.ui.EmptyStateView
import com.example.core.ui.SectionHeader
import com.example.core.ui.StatusPill
import com.example.database.AlarmEntity
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldSuccess
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AlarmsScreen(
    viewModel: AlarmsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var editingAlarm by remember { mutableStateOf<AlarmEntity?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshExactAlarmPermission()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("alarms_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Native Exact Alarm Permission Banner (Honest OS status)
        item {
            ElevatedCard(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (state.canScheduleExactAlarms)
                        EmeraldSuccess.copy(alpha = 0.12f)
                    else
                        AmberWarning.copy(alpha = 0.16f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            if (state.canScheduleExactAlarms) Icons.Default.AlarmOn else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (state.canScheduleExactAlarms) EmeraldSuccess else AmberWarning
                        )
                        Text(
                            text = if (state.canScheduleExactAlarms)
                                "Exact Alarm Permission Granted"
                            else
                                "Exact Alarm Permission Required for Guaranteed Timing",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (state.canScheduleExactAlarms)
                            "Android AlarmManager will fire scheduled study, break, exam, and wake-up alarms accurately and reschedule automatically after device reboot."
                        else
                            "Android has not granted exact alarm access to this app. Alarms fall back to approximate inexact windows and are NOT guaranteed to fire at the exact minute until granted.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (!state.canScheduleExactAlarms) {
                        Button(onClick = {
                            viewModel.buildExactAlarmSettingsIntent()?.let { context.startActivity(it) }
                        }) {
                            Text("Open Alarms & Reminders Settings")
                        }
                    }
                }
            }
        }

        state.lastScheduleMessage?.let { msg ->
            item {
                OutlinedCard(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(msg, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                }
            }
        }

        item {
            SectionHeader(
                title = "Study & Exam Alarms",
                subtitle = "Native Android AlarmManager",
                actionText = "+ New Alarm",
                onActionClick = { editingAlarm = null; showDialog = true }
            )
        }

        if (state.alarms.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Default.AlarmAdd,
                    title = "No alarms created",
                    message = "Schedule morning wake-ups, subject study blocks, or mock exam alerts.",
                    actionLabel = "Create Alarm",
                    onAction = { editingAlarm = null; showDialog = true }
                )
            }
        } else {
            items(state.alarms, key = { it.id }) { alarm ->
                ElevatedCard(
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().clickable { editingAlarm = alarm; showDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "%02d:%02d".format(alarm.hour, alarm.minute),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(alarm.label, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${alarm.category.replace('_', ' ')} • ${formatDaysMask(alarm.daysOfWeekMask, alarm.isRepeating)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = alarm.isEnabled, onCheckedChange = { viewModel.toggleAlarm(alarm) })
                            IconButton(onClick = { viewModel.deleteAlarm(alarm) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete alarm")
                            }
                        }
                    }
                }
            }
        }

        if (state.history.isNotEmpty()) {
            item { SectionHeader("Alarm History", "Recent triggers, snoozes, and dismissals") }
            val fmt = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
            items(state.history.take(15), key = { it.id }) { h ->
                OutlinedCard(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(h.label, style = MaterialTheme.typography.titleSmall)
                            Text(fmt.format(Date(h.triggeredAt)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        StatusPill(h.actionType, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
        }
    }

    if (showDialog) {
        var label by remember(editingAlarm) { mutableStateOf(editingAlarm?.label ?: "EUEE Study Block") }
        var hour by remember(editingAlarm) { mutableIntStateOf(editingAlarm?.hour ?: 6) }
        var minute by remember(editingAlarm) { mutableIntStateOf(editingAlarm?.minute ?: 30) }
        var mask by remember(editingAlarm) { mutableIntStateOf(editingAlarm?.daysOfWeekMask ?: 0b0111110) }
        var isRepeating by remember(editingAlarm) { mutableStateOf(editingAlarm?.isRepeating ?: true) }
        var vibrate by remember(editingAlarm) { mutableStateOf(editingAlarm?.vibrate ?: true) }
        var snoozeMin by remember(editingAlarm) { mutableIntStateOf(editingAlarm?.snoozeMinutes ?: 10) }
        var category by remember(editingAlarm) {
            mutableStateOf(runCatching { AlarmCategory.valueOf(editingAlarm?.category ?: "") }.getOrDefault(AlarmCategory.STUDY_REMINDER))
        }
        var subjectId by remember(editingAlarm) { mutableStateOf(editingAlarm?.subjectId) }

        val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editingAlarm == null) "Create Alarm" else "Edit Alarm") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Alarm Label") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = hour.toString(),
                            onValueChange = { hour = (it.toIntOrNull() ?: hour).coerceIn(0, 23) },
                            label = { Text("Hour (0-23)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = minute.toString(),
                            onValueChange = { minute = (it.toIntOrNull() ?: minute).coerceIn(0, 59) },
                            label = { Text("Minute (0-59)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = snoozeMin.toString(),
                            onValueChange = { snoozeMin = (it.toIntOrNull() ?: snoozeMin).coerceIn(1, 30) },
                            label = { Text("Snooze") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(AlarmCategory.entries) { cat ->
                            FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat.label) })
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isRepeating, onCheckedChange = { isRepeating = it })
                        Text("Repeat on weekdays")
                        Spacer(Modifier.width(12.dp))
                        Checkbox(checked = vibrate, onCheckedChange = { vibrate = it })
                        Text("Vibrate")
                    }
                    if (isRepeating) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(7) { idx ->
                                val bit = 1 shl idx
                                FilterChip(
                                    selected = (mask and bit) != 0,
                                    onClick = { mask = mask xor bit },
                                    label = { Text(dayNames[idx]) }
                                )
                            }
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(state.subjects, key = { it.id }) { s ->
                            FilterChip(selected = subjectId == s.id, onClick = { subjectId = if (subjectId == s.id) null else s.id }, label = { Text(s.name) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveAlarm(editingAlarm, label, hour, minute, mask, isRepeating, category, subjectId, vibrate, snoozeMin)
                    showDialog = false
                }) { Text("Save Alarm") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
        )
    }
}

private fun formatDaysMask(mask: Int, repeating: Boolean): String {
    if (!repeating || mask == 0) return "One-time"
    val names = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    return names.filterIndexed { idx, _ -> (mask and (1 shl idx)) != 0 }.joinToString(", ")
}
