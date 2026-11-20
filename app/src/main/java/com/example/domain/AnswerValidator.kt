package com.example.domain

import com.example.core.model.AnswerEvaluation
import com.example.core.model.QuestionType
import com.example.database.QuestionEntity
import org.json.JSONArray
import kotlin.math.abs

object AnswerValidator {

    private val subscriptMap = mapOf(
        '₀' to '0', '₁' to '1', '₂' to '2', '₃' to '3', '₄' to '4',
        '₅' to '5', '₆' to '6', '₇' to '7', '₈' to '8', '₉' to '9'
    )

    private val superscriptMap = mapOf(
        '⁰' to '0', '¹' to '1', '²' to '2', '³' to '3', '⁴' to '4',
        '⁵' to '5', '⁶' to '6', '⁷' to '7', '⁸' to '8', '⁹' to '9',
        '⁺' to '+', '⁻' to '-'
    )

    fun evaluate(question: QuestionEntity, userAnswerRaw: String): AnswerEvaluation {
        val userTrimmed = userAnswerRaw.trim()
        if (userTrimmed.isEmpty()) {
            return AnswerEvaluation(
                isCorrect = false,
                normalizedUserAnswer = "",
                matchedAcceptedAnswer = question.correctAnswer,
                feedbackNote = "No answer entered."
            )
        }

        val acceptedList = buildList {
            add(question.correctAnswer)
            runCatching {
                val arr = JSONArray(question.acceptedAnswersJson)
                for (i in 0 until arr.length()) {
                    val item = arr.optString(i).trim()
                    if (item.isNotEmpty() && item !in this) add(item)
                }
            }
        }

        val type = runCatching { QuestionType.valueOf(question.questionType) }
            .getOrDefault(QuestionType.MULTIPLE_CHOICE)

        if (type == QuestionType.NUMERICAL) {
            val userNum = parseNumeric(userTrimmed)
            if (userNum != null) {
                for (acc in acceptedList) {
                    val accNum = parseNumeric(acc)
                    if (accNum != null && abs(userNum - accNum) <= question.numericalTolerance) {
                        return AnswerEvaluation(
                            isCorrect = true,
                            normalizedUserAnswer = userTrimmed,
                            matchedAcceptedAnswer = acc,
                            feedbackNote = "Within numerical tolerance (±${question.numericalTolerance})."
                        )
                    }
                }
            }
        }

        val normUser = normalizeText(userTrimmed)
        for (acc in acceptedList) {
            val normAcc = normalizeText(acc)
            if (normUser == normAcc) {
                return AnswerEvaluation(
                    isCorrect = true,
                    normalizedUserAnswer = normUser,
                    matchedAcceptedAnswer = acc,
                    feedbackNote = "Exact match."
                )
            }
        }

        // Also check stripped unit numeric fallback for short answer / mixed units
        val userNumFallback = parseNumeric(userTrimmed)
        if (userNumFallback != null) {
            for (acc in acceptedList) {
                val accNum = parseNumeric(acc)
                if (accNum != null && abs(userNumFallback - accNum) <= question.numericalTolerance) {
                    return AnswerEvaluation(
                        isCorrect = true,
                        normalizedUserAnswer = userTrimmed,
                        matchedAcceptedAnswer = acc,
                        feedbackNote = "Numerically equivalent."
                    )
                }
            }
        }

        return AnswerEvaluation(
            isCorrect = false,
            normalizedUserAnswer = normUser,
            matchedAcceptedAnswer = question.correctAnswer,
            feedbackNote = "Expected: ${question.correctAnswer}"
        )
    }

    fun normalizeText(input: String): String {
        val sb = StringBuilder()
        for (ch in input.trim()) {
            when {
                ch in subscriptMap -> sb.append(subscriptMap.getValue(ch))
                ch in superscriptMap -> sb.append(superscriptMap.getValue(ch))
                ch == '−' || ch == '–' -> sb.append('-')
                ch == '×' -> sb.append('*')
                !ch.isWhitespace() -> sb.append(ch.lowercaseChar())
            }
        }
        return sb.toString()
    }

    fun parseNumeric(raw: String): Double? {
        val cleaned = normalizeText(raw)
            .replace(Regex("[^0-9eE.+\\-/]"), "")
            .trim()
        if (cleaned.isEmpty()) return null

        // Handle simple fractions like 5/2
        if ('/' in cleaned) {
            val parts = cleaned.split('/')
            if (parts.size == 2) {
                val num = parts[0].toDoubleOrNull()
                val den = parts[1].toDoubleOrNull()
                if (num != null && den != null && den != 0.0) {
                    return num / den
                }
            }
            return null
        }

        return cleaned.toDoubleOrNull()
    }

    fun parseStringArrayJson(json: String): List<String> {
        return runCatching {
            val arr = JSONArray(json)
            List(arr.length()) { idx -> arr.optString(idx) }.filter { it.isNotBlank() }
        }.getOrDefault(emptyList())
    }
}
