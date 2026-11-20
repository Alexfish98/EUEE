package com.example.questions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.Difficulty
import com.example.core.model.QuestionType
import com.example.database.QuestionEntity
import com.example.domain.AnswerValidator
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionEditorScreen(
    questionId: Long,
    viewModel: QuestionStudioViewModel,
    onNavigateBack: () -> Unit
) {
    val studioState by viewModel.uiState.collectAsStateWithLifecycle()
    var loaded by remember { mutableStateOf<QuestionEntity?>(null) }

    var uniqueCode by remember { mutableStateOf("EUEE-CUSTOM-${System.currentTimeMillis() % 100000}") }
    var subjectId by remember { mutableStateOf<Long?>(null) }
    var gradeId by remember { mutableStateOf<Long?>(null) }
    var unitId by remember { mutableStateOf<Long?>(null) }
    var topicId by remember { mutableStateOf<Long?>(null) }
    var difficulty by remember { mutableStateOf(Difficulty.MEDIUM) }
    var questionType by remember { mutableStateOf(QuestionType.MULTIPLE_CHOICE) }
    var questionText by remember { mutableStateOf("") }
    var correctAnswer by remember { mutableStateOf("") }
    var acceptedAnswersPipe by remember { mutableStateOf("") }
    var optionsPipe by remember { mutableStateOf("") }
    var hintsPipe by remember { mutableStateOf("") }
    var explanation by remember { mutableStateOf("") }
    var whyCorrect by remember { mutableStateOf("") }
    var commonMistakes by remember { mutableStateOf("") }
    var conceptExplanation by remember { mutableStateOf("") }
    var workedExample by remember { mutableStateOf("") }
    var memoryTip by remember { mutableStateOf("") }
    var examTrap by remember { mutableStateOf("") }
    var toleranceStr by remember { mutableStateOf("0.01") }
    var referencesText by remember { mutableStateOf("") }

    LaunchedEffect(questionId, studioState.subjects) {
        if (questionId > 0L) {
            val q = viewModel.getQuestion(questionId)
            loaded = q
            if (q != null) {
                uniqueCode = q.uniqueCode
                subjectId = q.subjectId
                gradeId = q.gradeId
                unitId = q.unitId
                topicId = q.topicId
                difficulty = runCatching { Difficulty.valueOf(q.difficulty) }.getOrDefault(Difficulty.MEDIUM)
                questionType = runCatching { QuestionType.valueOf(q.questionType) }.getOrDefault(QuestionType.MULTIPLE_CHOICE)
                questionText = q.questionText
                correctAnswer = q.correctAnswer
                acceptedAnswersPipe = AnswerValidator.parseStringArrayJson(q.acceptedAnswersJson).joinToString(" | ")
                optionsPipe = AnswerValidator.parseStringArrayJson(q.optionsJson).joinToString(" | ")
                hintsPipe = AnswerValidator.parseStringArrayJson(q.hintsJson).joinToString(" | ")
                explanation = q.explanation
                whyCorrect = q.whyCorrect
                commonMistakes = q.commonMistakes
                conceptExplanation = q.conceptExplanation
                workedExample = q.workedExample
                memoryTip = q.memoryTip
                examTrap = q.examTrap
                toleranceStr = q.numericalTolerance.toString()
                referencesText = q.referencesText
            }
        } else if (subjectId == null) {
            subjectId = studioState.subjects.firstOrNull()?.id
            gradeId = studioState.grades.lastOrNull()?.id
            val matchingUnit = studioState.units.firstOrNull { it.subjectId == subjectId }
            unitId = matchingUnit?.id ?: studioState.units.firstOrNull()?.id
            topicId = studioState.topics.firstOrNull { it.unitId == unitId }?.id ?: studioState.topics.firstOrNull()?.id
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (questionId > 0L) "Edit Question" else "New EUEE Question") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            val sub = subjectId ?: studioState.subjects.firstOrNull()?.id ?: return@Button
                            val grd = gradeId ?: studioState.grades.firstOrNull()?.id ?: return@Button
                            val unit = unitId ?: studioState.units.firstOrNull()?.id ?: return@Button
                            val topic = topicId ?: studioState.topics.firstOrNull()?.id ?: return@Button

                            val accepted = acceptedAnswersPipe.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                            val opts = optionsPipe.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                            val hints = hintsPipe.split("|").map { it.trim() }.filter { it.isNotEmpty() }

                            val entity = QuestionEntity(
                                id = loaded?.id ?: 0L,
                                uniqueCode = uniqueCode.trim(),
                                bankId = loaded?.bankId ?: 1L,
                                subjectId = sub,
                                gradeId = grd,
                                unitId = unit,
                                topicId = topic,
                                difficulty = difficulty.name,
                                questionType = questionType.name,
                                questionText = questionText.trim(),
                                correctAnswer = correctAnswer.trim(),
                                acceptedAnswersJson = JSONArray(accepted.ifEmpty { listOf(correctAnswer.trim()) }).toString(),
                                optionsJson = JSONArray(opts).toString(),
                                hintsJson = JSONArray(hints).toString(),
                                explanation = explanation.trim(),
                                whyCorrect = whyCorrect.trim(),
                                commonMistakes = commonMistakes.trim(),
                                conceptExplanation = conceptExplanation.trim(),
                                workedExample = workedExample.trim(),
                                memoryTip = memoryTip.trim(),
                                examTrap = examTrap.trim(),
                                numericalTolerance = toleranceStr.toDoubleOrNull() ?: 0.01,
                                referencesText = referencesText.trim(),
                                isBookmarked = loaded?.isBookmarked ?: false,
                                createdAt = loaded?.createdAt ?: System.currentTimeMillis()
                            )
                            viewModel.saveQuestion(entity, onNavigateBack)
                        },
                        enabled = questionText.isNotBlank() && correctAnswer.isNotBlank()
                    ) { Text("Save") }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(uniqueCode, { uniqueCode = it }, label = { Text("Unique ID / Code") }, modifier = Modifier.fillMaxWidth())

            Text("Subject & Grade", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(studioState.subjects, key = { it.id }) { s ->
                    FilterChip(selected = subjectId == s.id, onClick = { subjectId = s.id }, label = { Text(s.name) })
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(studioState.grades, key = { it.id }) { g ->
                    FilterChip(selected = gradeId == g.id, onClick = { gradeId = g.id }, label = { Text(g.label) })
                }
            }

            Text("Type & Difficulty", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(QuestionType.entries) { t ->
                    FilterChip(selected = questionType == t, onClick = { questionType = t }, label = { Text(t.label) })
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(Difficulty.entries) { d ->
                    FilterChip(selected = difficulty == d, onClick = { difficulty = d }, label = { Text(d.label) })
                }
            }

            OutlinedTextField(questionText, { questionText = it }, label = { Text("Question Text (supports Unicode formulas)") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(correctAnswer, { correctAnswer = it }, label = { Text("Primary Correct Answer") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(acceptedAnswersPipe, { acceptedAnswersPipe = it }, label = { Text("Other Accepted Answers (separated by | )") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(optionsPipe, { optionsPipe = it }, label = { Text("MCQ Options (separated by | )") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(hintsPipe, { hintsPipe = it }, label = { Text("Hints (separated by | )") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(explanation, { explanation = it }, label = { Text("Explanation") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(whyCorrect, { whyCorrect = it }, label = { Text("Why the answer is correct") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(commonMistakes, { commonMistakes = it }, label = { Text("Common Mistakes") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(conceptExplanation, { conceptExplanation = it }, label = { Text("Concept Explanation") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(workedExample, { workedExample = it }, label = { Text("Worked Example") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(memoryTip, { memoryTip = it }, label = { Text("Memory Tip") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(examTrap, { examTrap = it }, label = { Text("Examination Trap") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(toleranceStr, { toleranceStr = it }, label = { Text("Numerical Tolerance") }, modifier = Modifier.weight(1f))
                OutlinedTextField(referencesText, { referencesText = it }, label = { Text("Reference") }, modifier = Modifier.weight(2f))
            }
        }
    }
}
