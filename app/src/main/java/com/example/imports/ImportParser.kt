package com.example.imports

import com.example.core.model.Difficulty
import com.example.core.model.QuestionType
import com.example.database.QuestionEntity
import org.json.JSONArray
import org.json.JSONObject

data class RejectedRecord(
    val rowNumber: Int,
    val rawSnippet: String,
    val reason: String
)

data class ParsedImportQuestion(
    val uniqueCode: String,
    val subjectName: String,
    val gradeLevel: Int,
    val unitTitle: String,
    val topicTitle: String,
    val difficulty: String,
    val questionType: String,
    val questionText: String,
    val correctAnswer: String,
    val acceptedAnswers: List<String>,
    val options: List<String>,
    val hints: List<String>,
    val explanation: String,
    val whyCorrect: String,
    val commonMistakes: String,
    val conceptExplanation: String,
    val workedExample: String,
    val memoryTip: String,
    val examTrap: String,
    val numericalTolerance: Double,
    val referencesText: String
)

data class ImportPreviewResult(
    val formatName: String,
    val totalParsed: Int,
    val validItems: List<ParsedImportQuestion>,
    val duplicateCodes: List<String>,
    val rejectedRecords: List<RejectedRecord>,
    val truncatedByLimit: Boolean = false
)

object ImportParser {

    const val DEFAULT_MAX_RECORDS = 10_000

    const val DOCUMENTED_JSON_SCHEMA = """{
  "questions": [
    {
      "uniqueCode": "EUEE-CHEM-12-101",
      "subject": "Chemistry",
      "grade": 12,
      "unit": "Acid-Base Equilibria",
      "topic": "pH & Buffer Solutions",
      "difficulty": "MEDIUM",
      "questionType": "MULTIPLE_CHOICE",
      "questionText": "What is the pH of 0.01 M HCl?",
      "correctAnswer": "2",
      "acceptedAnswers": ["2", "2.0"],
      "options": ["1", "2", "3", "12"],
      "hints": ["HCl is a strong monoprotic acid."],
      "explanation": "pH = -log10(0.01) = 2.",
      "whyCorrect": "Strong acid dissociates completely.",
      "commonMistakes": "Taking log10 without negative sign.",
      "conceptExplanation": "pH = -log10[H+]",
      "workedExample": "0.001 M HCl -> pH = 3",
      "memoryTip": "Count decimal places for 10^-n!",
      "examTrap": "12 is the pOH, not pH.",
      "numericalTolerance": 0.01,
      "references": "Grade 12 Chemistry"
    }
  ]
}"""

    const val DOCUMENTED_TXT_FORMAT = """---
CODE: EUEE-PHYS-11-201
SUBJECT: Physics
GRADE: 11
UNIT: Dynamics
TOPIC: Newton's Laws
DIFFICULTY: MEDIUM
TYPE: MULTIPLE_CHOICE
Q: What is the net force on a 5 kg mass accelerating at 3 m/s²?
OPTIONS: 15 N | 8 N | 1.67 N | 0 N
A: 15 N
HINT: Apply F = m * a.
EXP: F = 5 kg * 3 m/s² = 15 N.
---"""

    fun parse(
        rawContent: String,
        existingCodes: Set<String>,
        maxRecords: Int = DEFAULT_MAX_RECORDS
    ): ImportPreviewResult {
        val trimmed = rawContent.trim()
        if (trimmed.isEmpty()) {
            return ImportPreviewResult(
                formatName = "Empty",
                totalParsed = 0,
                validItems = emptyList(),
                duplicateCodes = emptyList(),
                rejectedRecords = listOf(RejectedRecord(1, "", "Input is empty."))
            )
        }

        return when {
            trimmed.startsWith("{") || trimmed.startsWith("[") ->
                parseJson(trimmed, existingCodes, maxRecords)
            trimmed.contains("CODE:") && trimmed.contains("Q:") ->
                parseTxt(trimmed, existingCodes, maxRecords)
            else ->
                parseCsv(trimmed, existingCodes, maxRecords)
        }
    }

    fun parseJson(
        rawJson: String,
        existingCodes: Set<String>,
        maxRecords: Int = DEFAULT_MAX_RECORDS
    ): ImportPreviewResult {
        val valid = mutableListOf<ParsedImportQuestion>()
        val duplicates = mutableListOf<String>()
        val rejected = mutableListOf<RejectedRecord>()
        val seenInBatch = HashSet<String>()
        var truncated = false

        val array = runCatching {
            val trimmed = rawJson.trim()
            if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else {
                val root = JSONObject(trimmed)
                root.optJSONArray("questions") ?: JSONArray()
            }
        }.getOrElse { ex ->
            return ImportPreviewResult(
                formatName = "JSON",
                totalParsed = 0,
                validItems = emptyList(),
                duplicateCodes = emptyList(),
                rejectedRecords = listOf(RejectedRecord(1, rawJson.take(80), "Invalid JSON: ${ex.message}"))
            )
        }

        val count = array.length()
        for (i in 0 until count) {
            if (valid.size >= maxRecords) {
                truncated = true
                break
            }
            val obj = array.optJSONObject(i)
            if (obj == null) {
                rejected.add(RejectedRecord(i + 1, array.optString(i).take(60), "Entry is not a JSON object."))
                continue
            }

            val qText = obj.optString("questionText").ifBlank { obj.optString("question") }.trim()
            val cAns = obj.optString("correctAnswer").ifBlank { obj.optString("answer") }.trim()
            if (qText.isBlank() || cAns.isBlank()) {
                rejected.add(RejectedRecord(i + 1, obj.toString().take(80), "Missing required questionText or correctAnswer."))
                continue
            }

            val code = obj.optString("uniqueCode").ifBlank { "IMP-${System.currentTimeMillis()}-$i" }.trim()
            if (code in existingCodes || !seenInBatch.add(code)) {
                duplicates.add(code)
            }

            val subject = obj.optString("subject").ifBlank { "General" }.trim()
            val grade = obj.optInt("grade", 12).coerceIn(9, 12)
            val unit = obj.optString("unit").ifBlank { "General Unit" }.trim()
            val topic = obj.optString("topic").ifBlank { "General Topic" }.trim()
            val diff = normalizeDifficulty(obj.optString("difficulty"))
            val qType = normalizeQuestionType(obj.optString("questionType"))

            valid.add(
                ParsedImportQuestion(
                    uniqueCode = code,
                    subjectName = subject,
                    gradeLevel = grade,
                    unitTitle = unit,
                    topicTitle = topic,
                    difficulty = diff,
                    questionType = qType,
                    questionText = qText,
                    correctAnswer = cAns,
                    acceptedAnswers = readStringList(obj.optJSONArray("acceptedAnswers")),
                    options = readStringList(obj.optJSONArray("options")),
                    hints = readStringList(obj.optJSONArray("hints")),
                    explanation = obj.optString("explanation").trim(),
                    whyCorrect = obj.optString("whyCorrect").trim(),
                    commonMistakes = obj.optString("commonMistakes").trim(),
                    conceptExplanation = obj.optString("conceptExplanation").trim(),
                    workedExample = obj.optString("workedExample").trim(),
                    memoryTip = obj.optString("memoryTip").trim(),
                    examTrap = obj.optString("examTrap").trim(),
                    numericalTolerance = obj.optDouble("numericalTolerance", 0.01).takeIf { !it.isNaN() } ?: 0.01,
                    referencesText = obj.optString("references").trim()
                )
            )
        }

        return ImportPreviewResult(
            formatName = "JSON",
            totalParsed = count,
            validItems = valid,
            duplicateCodes = duplicates.distinct(),
            rejectedRecords = rejected,
            truncatedByLimit = truncated
        )
    }

    fun parseCsv(
        rawCsv: String,
        existingCodes: Set<String>,
        maxRecords: Int = DEFAULT_MAX_RECORDS
    ): ImportPreviewResult {
        val lines = rawCsv.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return ImportPreviewResult("CSV", 0, emptyList(), emptyList(), listOf(RejectedRecord(1, "", "Empty CSV.")))
        }

        val header = parseCsvLine(lines.first()).map { it.trim().lowercase() }
        val hasHeader = header.any { it.contains("question") || it.contains("subject") || it.contains("answer") }
        val startIdx = if (hasHeader) 1 else 0

        val valid = mutableListOf<ParsedImportQuestion>()
        val duplicates = mutableListOf<String>()
        val rejected = mutableListOf<RejectedRecord>()
        val seen = HashSet<String>()
        var truncated = false

        // Expected CSV columns:
        // uniqueCode,subject,grade,unit,topic,difficulty,questionType,questionText,correctAnswer,options(|),hints(|),explanation
        for (idx in startIdx until lines.size) {
            if (valid.size >= maxRecords) {
                truncated = true
                break
            }
            val cols = parseCsvLine(lines[idx])
            if (cols.size < 4) {
                rejected.add(RejectedRecord(idx + 1, lines[idx].take(80), "Too few CSV columns (minimum 4 required)."))
                continue
            }

            val code = cols.getOrNull(0)?.trim().orEmpty().ifBlank { "CSV-${System.currentTimeMillis()}-$idx" }
            val subject = cols.getOrNull(1)?.trim().orEmpty().ifBlank { "General" }
            val grade = cols.getOrNull(2)?.trim()?.toIntOrNull()?.coerceIn(9, 12) ?: 12
            val unit = cols.getOrNull(3)?.trim().orEmpty().ifBlank { "General Unit" }
            val topic = cols.getOrNull(4)?.trim().orEmpty().ifBlank { "General Topic" }
            val diff = normalizeDifficulty(cols.getOrNull(5).orEmpty())
            val qType = normalizeQuestionType(cols.getOrNull(6).orEmpty())
            val qText = cols.getOrNull(7)?.trim().orEmpty()
            val cAns = cols.getOrNull(8)?.trim().orEmpty()

            if (qText.isBlank() || cAns.isBlank()) {
                rejected.add(RejectedRecord(idx + 1, lines[idx].take(80), "Missing questionText (col 8) or correctAnswer (col 9)."))
                continue
            }

            if (code in existingCodes || !seen.add(code)) {
                duplicates.add(code)
            }

            val options = cols.getOrNull(9)?.split("|")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
            val hints = cols.getOrNull(10)?.split("|")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
            val explanation = cols.getOrNull(11)?.trim().orEmpty()

            valid.add(
                ParsedImportQuestion(
                    uniqueCode = code,
                    subjectName = subject,
                    gradeLevel = grade,
                    unitTitle = unit,
                    topicTitle = topic,
                    difficulty = diff,
                    questionType = qType,
                    questionText = qText,
                    correctAnswer = cAns,
                    acceptedAnswers = listOf(cAns),
                    options = options,
                    hints = hints,
                    explanation = explanation,
                    whyCorrect = "",
                    commonMistakes = "",
                    conceptExplanation = "",
                    workedExample = "",
                    memoryTip = "",
                    examTrap = "",
                    numericalTolerance = 0.01,
                    referencesText = "Imported CSV"
                )
            )
        }

        return ImportPreviewResult(
            formatName = "CSV",
            totalParsed = (lines.size - startIdx).coerceAtLeast(0),
            validItems = valid,
            duplicateCodes = duplicates.distinct(),
            rejectedRecords = rejected,
            truncatedByLimit = truncated
        )
    }

    fun parseTxt(
        rawTxt: String,
        existingCodes: Set<String>,
        maxRecords: Int = DEFAULT_MAX_RECORDS
    ): ImportPreviewResult {
        val blocks = rawTxt.split(Regex("^---+$", RegexOption.MULTILINE))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val valid = mutableListOf<ParsedImportQuestion>()
        val duplicates = mutableListOf<String>()
        val rejected = mutableListOf<RejectedRecord>()
        val seen = HashSet<String>()
        var truncated = false

        blocks.forEachIndexed { idx, block ->
            if (valid.size >= maxRecords) {
                truncated = true
                return@forEachIndexed
            }
            val map = mutableMapOf<String, String>()
            block.lines().forEach { line ->
                val colon = line.indexOf(':')
                if (colon > 0) {
                    val key = line.substring(0, colon).trim().uppercase()
                    val value = line.substring(colon + 1).trim()
                    map[key] = value
                }
            }
            val qText = map["Q"] ?: map["QUESTION"] ?: ""
            val cAns = map["A"] ?: map["ANSWER"] ?: ""
            if (qText.isBlank() || cAns.isBlank()) {
                rejected.add(RejectedRecord(idx + 1, block.take(80), "TXT block missing Q: or A: field."))
                return@forEachIndexed
            }
            val code = (map["CODE"] ?: "TXT-${System.currentTimeMillis()}-$idx").trim()
            if (code in existingCodes || !seen.add(code)) duplicates.add(code)

            valid.add(
                ParsedImportQuestion(
                    uniqueCode = code,
                    subjectName = map["SUBJECT"] ?: "General",
                    gradeLevel = map["GRADE"]?.toIntOrNull()?.coerceIn(9, 12) ?: 12,
                    unitTitle = map["UNIT"] ?: "General Unit",
                    topicTitle = map["TOPIC"] ?: "General Topic",
                    difficulty = normalizeDifficulty(map["DIFFICULTY"].orEmpty()),
                    questionType = normalizeQuestionType(map["TYPE"].orEmpty()),
                    questionText = qText,
                    correctAnswer = cAns,
                    acceptedAnswers = listOf(cAns),
                    options = map["OPTIONS"]?.split("|")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
                    hints = listOfNotNull(map["HINT"]?.takeIf { it.isNotBlank() }),
                    explanation = map["EXP"] ?: map["EXPLANATION"] ?: "",
                    whyCorrect = "",
                    commonMistakes = "",
                    conceptExplanation = "",
                    workedExample = "",
                    memoryTip = "",
                    examTrap = "",
                    numericalTolerance = 0.01,
                    referencesText = "Imported TXT"
                )
            )
        }

        return ImportPreviewResult(
            formatName = "TXT",
            totalParsed = blocks.size,
            validItems = valid,
            duplicateCodes = duplicates.distinct(),
            rejectedRecords = rejected,
            truncatedByLimit = truncated
        )
    }

    fun toQuestionEntity(
        parsed: ParsedImportQuestion,
        bankId: Long,
        subjectId: Long,
        gradeId: Long,
        unitId: Long,
        topicId: Long
    ): QuestionEntity {
        return QuestionEntity(
            uniqueCode = parsed.uniqueCode,
            bankId = bankId,
            subjectId = subjectId,
            gradeId = gradeId,
            unitId = unitId,
            topicId = topicId,
            difficulty = parsed.difficulty,
            questionType = parsed.questionType,
            questionText = parsed.questionText,
            correctAnswer = parsed.correctAnswer,
            acceptedAnswersJson = JSONArray(parsed.acceptedAnswers.ifEmpty { listOf(parsed.correctAnswer) }).toString(),
            optionsJson = JSONArray(parsed.options).toString(),
            hintsJson = JSONArray(parsed.hints).toString(),
            explanation = parsed.explanation,
            whyCorrect = parsed.whyCorrect,
            commonMistakes = parsed.commonMistakes,
            conceptExplanation = parsed.conceptExplanation,
            workedExample = parsed.workedExample,
            memoryTip = parsed.memoryTip,
            examTrap = parsed.examTrap,
            numericalTolerance = parsed.numericalTolerance,
            referencesText = parsed.referencesText
        )
    }

    private fun readStringList(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return List(arr.length()) { arr.optString(it).trim() }.filter { it.isNotEmpty() }
    }

    private fun normalizeDifficulty(raw: String): String =
        Difficulty.entries.firstOrNull { it.name.equals(raw.trim(), true) }?.name ?: Difficulty.MEDIUM.name

    private fun normalizeQuestionType(raw: String): String =
        QuestionType.entries.firstOrNull { it.name.equals(raw.trim(), true) }?.name ?: QuestionType.MULTIPLE_CHOICE.name

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}
