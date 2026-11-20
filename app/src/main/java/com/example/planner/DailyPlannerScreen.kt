package com.example.planner

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.TaskPriority
import com.example.core.ui.EmptyStateView
import com.example.core.ui.SectionHeader
import com.example.core.ui.StatusPill
import com.example.database.StudyTaskEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DailyPlannerScreen(
    viewModel: PlannerViewModel,
    onOpenAlarms: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editingTask by remember { mutableStateOf<StudyTaskEntity?>(null) }
    var showTaskDialog by remember { mutableStateOf(false) }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }
    val subjectsById = remember(state.subjects) { state.subjects.associateBy { it.id } }

    Column(modifier = modifier.fillMaxSize().testTag("planner_screen")) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(state.selectedDate.format(dateFormatter), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(
                                    "${state.completedCount}/${state.dayTasks.size} tasks • ${state.completedMinutes}/${state.dailyStudyGoalMinutes} goal min",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(onClick = onOpenAlarms) {
                                    Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Alarms")
                                }
                                Button(
                                    onClick = { editingTask = null; showTaskDialog = true },
                                    modifier = Modifier.testTag("add_task_btn")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add study task")
                                }
                            }
                        }
                        val pct = if (state.dailyStudyGoalMinutes > 0) {
                            (state.completedMinutes.toFloat() / state.dailyStudyGoalMinutes).coerceIn(0f, 1f)
                        } else 0f
                        LinearProgressIndicator(progress = { pct }, modifier = Modifier.fillMaxWidth())

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = !state.isWeeklyView, onClick = { viewModel.setWeeklyView(false) }, label = { Text("Daily View") })
                            FilterChip(selected = state.isWeeklyView, onClick = { viewModel.setWeeklyView(true) }, label = { Text("Weekly Overview") })
                        }

                        // Quick 7-day date strip
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items((-1..5).toList()) { offset ->
                                val d = LocalDate.now().plusDays(offset.toLong())
                                FilterChip(
                                    selected = d == state.selectedDate,
                                    onClick = { viewModel.selectDate(d) },
                                    label = { Text(if (offset == 0) "Today" else d.format(DateTimeFormatter.ofPattern("EEE d"))) }
                                )
                            }
                        }
                    }
                }
            }

            val tasksToShow = if (state.isWeeklyView) state.weekTasks else state.dayTasks
            item {
                SectionHeader(
                    title = if (state.isWeeklyView) "This Week's Study Plan" else "Scheduled Tasks",
                    subtitle = "Planned duration: ${tasksToShow.sumOf { it.durationMinutes }} min"
                )
            }

            if (tasksToShow.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.EventNote,
                        title = "No tasks scheduled",
                        message = "Plan focused blocks linked to specific EUEE subjects, units, and topics.",
                        actionLabel = "Add Study Task",
                        onAction = { editingTask = null; showTaskDialog = true }
                    )
                }
            } else {
                items(tasksToShow, key = { it.id }) { task ->
                    val subjectName = task.subjectId?.let { subjectsById[it]?.name } ?: "General"
                    val taskDate = LocalDate.ofEpochDay(task.scheduledDateEpochDay)
                    ElevatedCard(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editingTask = task; showTaskDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = task.isCompleted,
                                onCheckedChange = { viewModel.toggleCompleted(task) }
                            )
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(task.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${if (state.isWeeklyView) "${taskDate.format(DateTimeFormatter.ofPattern("EEE"))} • " else ""}%02d:%02d • %d min • %s".format(
                                        task.startTimeMinutes / 60,
                                        task.startTimeMinutes % 60,
                                        task.durationMinutes,
                                        subjectName
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (task.notes.isNotBlank()) {
                                    Text(task.notes, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            StatusPill(
                                text = task.priority,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            IconButton(onClick = { viewModel.deleteTask(task) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete task")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTaskDialog) {
        var title by remember(editingTask) { mutableStateOf(editingTask?.title ?: "") }
        var notes by remember(editingTask) { mutableStateOf(editingTask?.notes ?: "") }
        var subId by remember(editingTask) { mutableStateOf(editingTask?.subjectId ?: state.subjects.firstOrNull()?.id) }
        var unitId by remember(editingTask) { mutableStateOf(editingTask?.unitId) }
        var topicId by remember(editingTask) { mutableStateOf(editingTask?.topicId) }
        var startHour by remember(editingTask) { mutableIntStateOf((editingTask?.startTimeMinutes ?: 16 * 60) / 60) }
        var startMin by remember(editingTask) { mutableIntStateOf((editingTask?.startTimeMinutes ?: 0) % 60) }
        var duration by remember(editingTask) { mutableIntStateOf(editingTask?.durationMinutes ?: 45) }
        var priority by remember(editingTask) {
            mutableStateOf(runCatching { TaskPriority.valueOf(editingTask?.priority ?: "") }.getOrDefault(TaskPriority.MEDIUM))
        }

        val matchingUnits = state.units.filter { subId == null || it.subjectId == subId }
        val matchingTopics = state.topics.filter { unitId == null || it.unitId == unitId }

        AlertDialog(
            onDismissRequest = { showTaskDialog = false },
            title = { Text(if (editingTask == null) "Schedule Study Task" else "Edit Task") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Task Title") }, singleLine = true)
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes / Target Questions") })

                    Text("Subject", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(state.subjects, key = { it.id }) { s ->
                            FilterChip(selected = subId == s.id, onClick = { subId = s.id; unitId = null; topicId = null }, label = { Text(s.name) })
                        }
                    }

                    if (matchingUnits.isNotEmpty()) {
                        Text("Link Unit (Optional)", style = MaterialTheme.typography.labelMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(matchingUnits, key = { it.id }) { u ->
                                FilterChip(selected = unitId == u.id, onClick = { unitId = if (unitId == u.id) null else u.id; topicId = null }, label = { Text("U${u.unitNumber}: ${u.title}") })
                            }
                        }
                    }

                    if (unitId != null && matchingTopics.isNotEmpty()) {
                        Text("Link Topic (Optional)", style = MaterialTheme.typography.labelMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(matchingTopics, key = { it.id }) { t ->
                                FilterChip(selected = topicId == t.id, onClick = { topicId = if (topicId == t.id) null else t.id }, label = { Text(t.title) })
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startHour.toString(),
                            onValueChange = { startHour = (it.toIntOrNull() ?: startHour).coerceIn(0, 23) },
                            label = { Text("Hour (0-23)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = startMin.toString(),
                            onValueChange = { startMin = (it.toIntOrNull() ?: startMin).coerceIn(0, 59) },
                            label = { Text("Min") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = duration.toString(),
                            onValueChange = { duration = (it.toIntOrNull() ?: duration).coerceIn(5, 360) },
                            label = { Text("Duration") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(TaskPriority.entries) { p ->
                            FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(p.label) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveTask(editingTask, title, notes, subId, unitId, topicId, state.selectedDate, startHour, startMin, duration, priority)
                    showTaskDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showTaskDialog = false }) { Text("Cancel") } }
        )
    }
}
