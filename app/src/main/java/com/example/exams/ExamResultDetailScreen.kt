package com.example.exams

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.ui.SectionHeader
import com.example.core.ui.StatusPill
import com.example.database.ExamResultEntity
import com.example.database.QuestionEntity
import com.example.ui.theme.CoralError
import com.example.ui.theme.EmeraldSuccess
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamResultDetailScreen(
    resultId: Long,
    viewModel: MockExamViewModel,
    onNavigateBack: () -> Unit
) {
    var result by remember { mutableStateOf<ExamResultEntity?>(null) }
    var incorrectQuestions by remember { mutableStateOf<List<QuestionEntity>>(emptyList()) }
    var answerMap by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }

    LaunchedEffect(resultId) {
        val loaded = viewModel.loadExamResult(resultId)
        result = loaded
        if (loaded != null) {
            val incArr = runCatching { JSONArray(loaded.incorrectQuestionIdsJson) }.getOrDefault(JSONArray())
            val incIds = List(incArr.length()) { incArr.optLong(it) }
            incorrectQuestions = viewModel.loadQuestions(incIds)
            val ansObj = runCatching { JSONObject(loaded.answerMapJson) }.getOrDefault(JSONObject())
            val map = mutableMapOf<Long, String>()
            ansObj.keys().forEach { k -> k.toLongOrNull()?.let { map[it] = ansObj.optString(k) } }
            answerMap = map
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Examination Report") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        val res = result
        if (res == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding)) {
                CircularProgressIndicator(Modifier.padding(24.dp))
            }
            return@Scaffold
        }

        val breakdown = remember(res.topicBreakdownJson) {
            val arr = runCatching { JSONArray(res.topicBreakdownJson) }.getOrDefault(JSONArray())
            List(arr.length()) { i ->
                val obj = arr.getJSONObject(i)
                Triple(obj.optString("subject"), obj.optInt("correct"), obj.optInt("total"))
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(res.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "Score: ${res.correctCount} / ${res.totalQuestions} (${res.scorePercent.toInt()}%)",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (res.scorePercent >= 50) EmeraldSuccess else CoralError
                        )
                        Text(
                            "Answered: ${res.answeredCount}/${res.totalQuestions} • Time: ${res.timeTakenSeconds / 60}m ${res.timeTakenSeconds % 60}s",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            item {
                SectionHeader("Subject Performance Breakdown")
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    breakdown.forEach { (sub, c, t) ->
                        val pct = if (t > 0) (c * 100) / t else 0
                        OutlinedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(sub, fontWeight = FontWeight.SemiBold)
                                StatusPill("$c/$t ($pct%)", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader("Incorrect Question Analysis", "${incorrectQuestions.size} items to review")
            }

            items(incorrectQuestions, key = { it.id }) { q ->
                ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(q.uniqueCode, style = MaterialTheme.typography.labelMedium, color = CoralError)
                        Text(q.questionText, style = MaterialTheme.typography.titleSmall)
                        Text("Your answer: ${answerMap[q.id]?.ifBlank { "(Unanswered)" } ?: "(Unanswered)"}", style = MaterialTheme.typography.bodySmall, color = CoralError)
                        Text("Correct answer: ${q.correctAnswer}", style = MaterialTheme.typography.bodySmall, color = EmeraldSuccess, fontWeight = FontWeight.Bold)
                        if (q.explanation.isNotBlank()) {
                            Text(q.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
