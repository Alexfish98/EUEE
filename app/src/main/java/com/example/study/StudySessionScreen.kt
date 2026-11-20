package com.example.study

import android.app.Activity
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import com.example.core.haptic.HapticHelper
import com.example.core.model.QuestionType
import com.example.core.model.StudyMode
import com.example.core.speech.SpeechHelper
import com.example.core.ui.EmptyStateView
import com.example.core.ui.KeepScreenAwakeEffect
import com.example.core.ui.StatusPill
import com.example.domain.AnswerValidator
import com.example.ui.theme.CoralError
import com.example.ui.theme.EmeraldSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudySessionScreen(
    viewModel: StudyViewModel,
    speechHelper: SpeechHelper,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.sessionState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    BackHandler {
        speechHelper.stop()
        onNavigateBack()
    }

    KeepScreenAwakeEffect(enabled = state.keepScreenAwake && !state.isCompleted)

    val speechAvailable = remember { SpeechHelper.isSpeechRecognitionAvailable(context) }
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                viewModel.updateUserInput(spoken)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.title, style = MaterialTheme.typography.titleMedium)
                        if (state.questions.isNotEmpty() && !state.isCompleted) {
                            Text(
                                text = "Question ${state.currentIndex + 1} of ${state.questions.size}" +
                                    if (state.isTimedMode) " • %02d:%02d".format(state.remainingSeconds / 60, state.remainingSeconds % 60) else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        speechHelper.stop()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val q = state.currentQuestion
                    if (q != null && !state.isCompleted) {
                        IconButton(onClick = { speechHelper.speak(q.questionText, state.speechRate) }) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Read question aloud")
                        }
                        IconButton(
                            onClick = {
                                HapticHelper.tap(context, state.vibrationEnabled)
                                viewModel.toggleBookmarkCurrent()
                            },
                            modifier = Modifier.testTag("bookmark_button")
                        ) {
                            Icon(
                                imageVector = if (q.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark question",
                                tint = if (q.isBookmarked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (state.questions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateView(
                    icon = Icons.Default.Quiz,
                    title = "No questions available",
                    message = "Select a broader subject/grade filter in Study Library or add questions in Question Studio.",
                    actionLabel = "Back to Library",
                    onAction = onNavigateBack
                )
            }
            return@Scaffold
        }

        if (state.isCompleted) {
            val acc = if (state.attemptedCount == 0) 0 else (state.correctCount * 100) / state.attemptedCount
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(68.dp))
                Spacer(Modifier.height(16.dp))
                Text("Session Complete!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Score: ${state.correctCount} / ${state.attemptedCount} ($acc% accuracy)",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Spaced repetition schedules and any missed-question notebook entries have been saved offline.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Text("Return to Library")
                }
            }
            return@Scaffold
        }

        val q = state.currentQuestion!!
        val hints = remember(q.id) { AnswerValidator.parseStringArrayJson(q.hintsJson) }
        val qType = remember(q.id) { runCatching { QuestionType.valueOf(q.questionType) }.getOrDefault(QuestionType.MULTIPLE_CHOICE) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            LinearProgressIndicator(
                progress = { (state.currentIndex + 1).toFloat() / state.questions.size },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(q.uniqueCode, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                StatusPill(q.difficulty, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
            }

            // Flashcard Mode
            if (state.mode == StudyMode.FLASHCARDS) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 200.dp)
                        .clickable { viewModel.flipFlashcard() }
                        .testTag("flashcard_surface"),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (!state.isCardFlipped) "QUESTION (Tap card to flip)" else "ANSWER & EXPLANATION",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (!state.isCardFlipped) q.questionText else q.correctAnswer,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (state.isCardFlipped && q.explanation.isNotBlank()) {
                            Text(q.explanation, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            } else {
                ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = q.questionText,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }

            // Hints
            if (hints.isNotEmpty() && !state.isAnswerRevealed) {
                if (state.hintsRevealed > 0) {
                    OutlinedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            hints.take(state.hintsRevealed).forEachIndexed { idx, h ->
                                Text("Hint ${idx + 1}: $h", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                if (state.hintsRevealed < hints.size) {
                    TextButton(onClick = { viewModel.revealHint() }) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Reveal Hint (${state.hintsRevealed}/${hints.size})")
                    }
                }
            }

            // Input Controls based on Study Mode & Question Type
            if (state.mode == StudyMode.ACTIVE_RECALL || state.mode == StudyMode.FLASHCARDS) {
                if (!state.isAnswerRevealed) {
                    Button(
                        onClick = { viewModel.revealActiveRecallAnswer() },
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("reveal_answer_btn")
                    ) {
                        Text("Reveal Answer")
                    }
                } else {
                    Text("How well did you recall this?", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                HapticHelper.error(context, state.vibrationEnabled)
                                viewModel.recordSelfGradedRecall(false)
                            },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) { Text("Need Practice") }
                        Button(
                            onClick = {
                                HapticHelper.success(context, state.vibrationEnabled)
                                viewModel.recordSelfGradedRecall(true)
                            },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) { Text("Recalled!") }
                    }
                }
            } else {
                // Interactive answering (MCQ or Typed Input)
                val showChoices = state.currentOptions.isNotEmpty() &&
                    (qType == QuestionType.MULTIPLE_CHOICE || qType == QuestionType.TRUE_FALSE || state.mode == StudyMode.MULTIPLE_CHOICE)

                if (showChoices) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.currentOptions.forEachIndexed { idx, option ->
                            val isSelected = state.userInput == option
                            val isCorrectOpt = option == q.correctAnswer
                            val borderColor = when {
                                !state.isAnswerRevealed && isSelected -> MaterialTheme.colorScheme.primary
                                state.isAnswerRevealed && isCorrectOpt -> EmeraldSuccess
                                state.isAnswerRevealed && isSelected && !isCorrectOpt -> CoralError
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            }
                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("answer_option_$idx")
                                    .clickable(enabled = !state.isAnswerRevealed) {
                                        viewModel.submitAnswer(option)
                                        val correct = AnswerValidator.evaluate(q, option).isCorrect
                                        if (correct) HapticHelper.success(context, state.vibrationEnabled)
                                        else HapticHelper.error(context, state.vibrationEnabled)
                                    },
                                border = BorderStroke(1.5.dp, borderColor),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("${'A' + idx})", fontWeight = FontWeight.Bold)
                                    Text(option, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = state.userInput,
                        onValueChange = { viewModel.updateUserInput(it) },
                        label = { Text("Enter your answer (supports formulas & tolerance)") },
                        enabled = !state.isAnswerRevealed,
                        modifier = Modifier.fillMaxWidth().testTag("answer_text_input"),
                        trailingIcon = {
                            if (speechAvailable && !state.isAnswerRevealed) {
                                IconButton(onClick = {
                                    voiceLauncher.launch(SpeechHelper.createVoiceInputIntent())
                                }) {
                                    Icon(Icons.Default.Mic, contentDescription = "Voice input (may require online service)")
                                }
                            }
                        }
                    )
                    if (speechAvailable) {
                        Text(
                            text = "Note: Voice input delegates to Android's speech service and may require network connectivity depending on device language packs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!state.isAnswerRevealed) {
                        Button(
                            onClick = {
                                viewModel.submitAnswer(state.userInput)
                                val correct = AnswerValidator.evaluate(q, state.userInput).isCorrect
                                if (correct) HapticHelper.success(context, state.vibrationEnabled)
                                else HapticHelper.error(context, state.vibrationEnabled)
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("submit_answer_btn")
                        ) {
                            Text("Check Answer")
                        }
                    }
                }
            }

            // Confidence Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Confidence: ${state.confidenceRating}/5", style = MaterialTheme.typography.labelLarge)
                Row {
                    (1..5).forEach { rating ->
                        IconButton(onClick = { viewModel.setConfidence(rating) }) {
                            Icon(
                                imageVector = if (rating <= state.confidenceRating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Confidence $rating",
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }

            // Rich Pedagogical Feedback Panel
            AnimatedVisibility(visible = state.isAnswerRevealed) {
                val eval = state.evaluation
                ElevatedCard(
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().testTag("feedback_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (eval != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (eval.isCorrect) "Correct!" else "Incorrect — Added to Notebook",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (eval.isCorrect) EmeraldSuccess else CoralError,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = {
                                    speechHelper.speak("${q.correctAnswer}. ${q.explanation}", state.speechRate)
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Read explanation")
                                }
                            }
                            Text("Accepted Answer: ${q.correctAnswer}", fontWeight = FontWeight.SemiBold)
                        }
                        if (q.explanation.isNotBlank()) Text("Explanation: ${q.explanation}", style = MaterialTheme.typography.bodyMedium)
                        if (q.whyCorrect.isNotBlank()) Text("Why it's right: ${q.whyCorrect}", style = MaterialTheme.typography.bodySmall)
                        if (q.commonMistakes.isNotBlank()) Text("Common Mistake: ${q.commonMistakes}", style = MaterialTheme.typography.bodySmall)
                        if (q.conceptExplanation.isNotBlank()) Text("Core Concept: ${q.conceptExplanation}", style = MaterialTheme.typography.bodySmall)
                        if (q.workedExample.isNotBlank()) Text("Worked Example: ${q.workedExample}", style = MaterialTheme.typography.bodySmall)
                        if (q.memoryTip.isNotBlank()) Text("Memory Tip: ${q.memoryTip}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        if (q.examTrap.isNotBlank()) Text("Exam Trap: ${q.examTrap}", style = MaterialTheme.typography.bodySmall, color = CoralError)
                        if (q.referencesText.isNotBlank()) Text("Ref: ${q.referencesText}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(Modifier.height(6.dp))
                        Button(
                            onClick = { viewModel.nextQuestion() },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("next_question_btn")
                        ) {
                            Text(if (state.currentIndex + 1 >= state.questions.size) "Finish Session" else "Next Question")
                        }
                    }
                }
            }
        }
    }
}
