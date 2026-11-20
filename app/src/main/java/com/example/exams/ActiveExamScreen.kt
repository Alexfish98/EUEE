package com.example.exams

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.KeepScreenAwakeEffect
import com.example.domain.AnswerValidator
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveExamScreen(
    viewModel: MockExamViewModel,
    onExitToLobby: () -> Unit,
    onExamCompleted: (Long) -> Unit
) {
    val state by viewModel.activeExam.collectAsStateWithLifecycle()
    var showNavigatorSheet by remember { mutableStateOf(false) }
    var showSubmitConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(state.completedResultId) {
        state.completedResultId?.let { onExamCompleted(it) }
    }

    BackHandler { showSubmitConfirm = true }
    KeepScreenAwakeEffect(enabled = state.keepScreenAwake)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Time Left: %02d:%02d • Q%d/%d".format(
                                state.remainingSeconds / 60,
                                state.remainingSeconds % 60,
                                state.currentIndex + 1,
                                state.questions.size
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    val q = state.currentQuestion
                    if (q != null) {
                        val isFlagged = q.id in state.flaggedIds
                        IconButton(onClick = { viewModel.toggleFlag(q.id) }) {
                            Icon(
                                Icons.Default.Flag,
                                contentDescription = "Flag question for review",
                                tint = if (isFlagged) AmberWarning else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = { showNavigatorSheet = true }) {
                        Icon(Icons.Default.GridView, contentDescription = "Question Navigator")
                    }
                    TextButton(onClick = { showSubmitConfirm = true }, modifier = Modifier.testTag("exam_submit_top_btn")) {
                        Text("Submit")
                    }
                }
            )
        }
    ) { innerPadding ->
        val q = state.currentQuestion
        if (q == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Button(onClick = onExitToLobby) { Text("Return to Exam Lobby") }
            }
            return@Scaffold
        }

        val options = remember(q.id) { AnswerValidator.parseStringArrayJson(q.optionsJson) }
        val selected = state.selectedAnswers[q.id].orEmpty()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(q.questionText, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(18.dp))
                }

                if (options.isNotEmpty()) {
                    options.forEachIndexed { idx, opt ->
                        val isSel = selected == opt
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("exam_option_$idx")
                                .clickable { viewModel.selectAnswer(q.id, opt) },
                            border = BorderStroke(
                                if (isSel) 2.dp else 1.dp,
                                if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("${'A' + idx})", fontWeight = FontWeight.Bold)
                                Text(opt, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = selected,
                        onValueChange = { viewModel.selectAnswer(q.id, it) },
                        label = { Text("Enter answer") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = { viewModel.jumpToQuestion(state.currentIndex - 1) },
                    enabled = state.currentIndex > 0,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Previous")
                }
                if (state.currentIndex + 1 < state.questions.size) {
                    Button(
                        onClick = { viewModel.jumpToQuestion(state.currentIndex + 1) },
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Next")
                    }
                } else {
                    Button(
                        onClick = { showSubmitConfirm = true },
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Review & Submit")
                    }
                }
            }
        }
    }

    if (showNavigatorSheet) {
        ModalBottomSheet(onDismissRequest = { showNavigatorSheet = false }) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Question Navigator (${state.unansweredCount} unanswered)", style = MaterialTheme.typography.titleMedium)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 320.dp)
                ) {
                    items(state.questions.size) { idx ->
                        val q = state.questions[idx]
                        val answered = !state.selectedAnswers[q.id].isNullOrBlank()
                        val flagged = q.id in state.flaggedIds
                        val bg = when {
                            flagged -> AmberWarning.copy(alpha = 0.25f)
                            answered -> EmeraldSuccess.copy(alpha = 0.25f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        Box(
                            modifier = Modifier
                                .height(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(bg)
                                .clickable {
                                    viewModel.jumpToQuestion(idx)
                                    showNavigatorSheet = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${idx + 1}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showSubmitConfirm) {
        AlertDialog(
            onDismissRequest = { showSubmitConfirm = false },
            title = { Text("Submit Examination?") },
            text = {
                val warn = if (state.unansweredCount > 0) {
                    "Warning: You still have ${state.unansweredCount} unanswered question(s) and ${state.flaggedIds.size} flagged."
                } else {
                    "All ${state.questions.size} questions answered. Ready to grade?"
                }
                Text(warn)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSubmitConfirm = false
                        viewModel.submitExam(onExamCompleted)
                    },
                    modifier = Modifier.testTag("confirm_submit_exam_btn")
                ) { Text("Submit Now") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showSubmitConfirm = false; onExitToLobby() }) { Text("Save & Exit") }
                    TextButton(onClick = { showSubmitConfirm = false }) { Text("Continue Exam") }
                }
            }
        )
    }
}
