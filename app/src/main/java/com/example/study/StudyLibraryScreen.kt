package com.example.study

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.StudyMode
import com.example.core.ui.EmptyStateView
import com.example.core.ui.SectionHeader
import com.example.core.ui.StatusPill

@Composable
fun StudyLibraryScreen(
    viewModel: StudyViewModel,
    onLaunchStudyMode: (StudyMode, Int) -> Unit,
    onOpenQuestionEditor: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.libraryState.collectAsStateWithLifecycle()
    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var showAddUnitDialog by remember { mutableStateOf(false) }
    var showAddTopicDialog by remember { mutableStateOf(false) }
    var timedDuration by remember { mutableIntStateOf(15) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("study_library_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Subjects Selector
        item {
            SectionHeader(
                title = "EUEE Subject Library",
                subtitle = "Subject → Grade (9–12) → Unit → Topic → Questions",
                actionText = "+ Subject",
                onActionClick = { showAddSubjectDialog = true }
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = state.selectedSubjectId == null,
                        onClick = { viewModel.selectSubject(null) },
                        label = { Text("All Subjects") }
                    )
                }
                items(state.subjects, key = { it.id }) { subject ->
                    FilterChip(
                        selected = state.selectedSubjectId == subject.id,
                        onClick = { viewModel.selectSubject(subject.id) },
                        label = { Text(subject.name) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(subject.colorHex))
                            )
                        }
                    )
                }
            }
        }

        // 2. Grade 9..12 Selector
        item {
            Text("Grade Level", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = state.selectedGradeId == null,
                        onClick = { viewModel.selectGrade(null) },
                        label = { Text("All Grades") }
                    )
                }
                items(state.grades, key = { it.id }) { grade ->
                    FilterChip(
                        selected = state.selectedGradeId == grade.id,
                        onClick = { viewModel.selectGrade(grade.id) },
                        label = { Text(grade.label) }
                    )
                }
            }
        }

        // 3. Units & Topics
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Units (${state.units.size})", style = MaterialTheme.typography.labelLarge)
                if (state.selectedSubjectId != null && state.selectedGradeId != null) {
                    TextButton(onClick = { showAddUnitDialog = true }) { Text("+ Add Unit") }
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = state.selectedUnitId == null,
                        onClick = { viewModel.selectUnit(null) },
                        label = { Text("All Units") }
                    )
                }
                items(state.units, key = { it.id }) { unit ->
                    FilterChip(
                        selected = state.selectedUnitId == unit.id,
                        onClick = { viewModel.selectUnit(unit.id) },
                        label = { Text("U${unit.unitNumber}: ${unit.title}") }
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Topics (${state.topics.size})", style = MaterialTheme.typography.labelLarge)
                if (state.selectedUnitId != null) {
                    TextButton(onClick = { showAddTopicDialog = true }) { Text("+ Add Topic") }
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = state.selectedTopicId == null,
                        onClick = { viewModel.selectTopic(null) },
                        label = { Text("All Topics") }
                    )
                }
                items(state.topics, key = { it.id }) { topic ->
                    FilterChip(
                        selected = state.selectedTopicId == topic.id,
                        onClick = { viewModel.selectTopic(topic.id) },
                        label = { Text(topic.title) }
                    )
                }
            }
        }

        // 4. All 10 Study Modes Launcher
        item {
            SectionHeader(
                title = "Launch Study Mode",
                subtitle = "Matching questions in scope: ${state.previewQuestions.size}"
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Timed Drill Duration:", style = MaterialTheme.typography.bodySmall)
                listOf(10, 15, 30, 45).forEach { mins ->
                    FilterChip(
                        selected = timedDuration == mins,
                        onClick = { timedDuration = mins },
                        label = { Text("${mins}m") }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StudyMode.entries.chunked(2).forEach { rowModes ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        rowModes.forEach { mode ->
                            ElevatedCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("mode_card_${mode.name.lowercase()}")
                                    .clickable { onLaunchStudyMode(mode, timedDuration) },
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(mode.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        mode.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                        if (rowModes.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // 5. Questions Preview
        item {
            SectionHeader(
                title = "Questions in Scope",
                subtitle = "Tap any question to inspect or edit in Question Studio",
                actionText = "+ New Q",
                onActionClick = { onOpenQuestionEditor(-1L) }
            )
        }

        if (state.previewQuestions.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Default.SearchOff,
                    title = "No questions match this filter",
                    message = "Try clearing unit/topic filters, create a new question, or import a question bank.",
                    actionLabel = "Create Question",
                    onAction = { onOpenQuestionEditor(-1L) }
                )
            }
        } else {
            items(state.previewQuestions, key = { it.id }) { q ->
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenQuestionEditor(q.id) },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusPill(
                                text = q.uniqueCode,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${q.difficulty} • ${q.questionType.replace('_', ' ')}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(q.questionText, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
                    }
                }
            }
        }
    }

    if (showAddSubjectDialog) {
        var name by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Natural Science") }
        AlertDialog(
            onDismissRequest = { showAddSubjectDialog = false },
            title = { Text("Add Custom Subject") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Subject Name") }, singleLine = true)
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addCustomSubject(name, category)
                    showAddSubjectDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddSubjectDialog = false }) { Text("Cancel") } }
        )
    }

    if (showAddUnitDialog && state.selectedSubjectId != null && state.selectedGradeId != null) {
        var title by remember { mutableStateOf("") }
        var num by remember { mutableStateOf("${state.units.size + 1}") }
        AlertDialog(
            onDismissRequest = { showAddUnitDialog = false },
            title = { Text("Add Unit") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = num, onValueChange = { num = it }, label = { Text("Unit Number") }, singleLine = true)
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Unit Title") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addCustomUnit(state.selectedSubjectId!!, state.selectedGradeId!!, num.toIntOrNull() ?: 1, title)
                    showAddUnitDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddUnitDialog = false }) { Text("Cancel") } }
        )
    }

    if (showAddTopicDialog && state.selectedUnitId != null) {
        var title by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddTopicDialog = false },
            title = { Text("Add Topic") },
            text = {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Topic Title") }, singleLine = true)
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addCustomTopic(state.selectedUnitId!!, title)
                    showAddTopicDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddTopicDialog = false }) { Text("Cancel") } }
        )
    }
}
