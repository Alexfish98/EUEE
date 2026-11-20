package com.example

import com.example.core.model.MasteryStatus
import com.example.core.model.QuestionType
import com.example.database.QuestionEntity
import com.example.database.QuestionProgressEntity
import com.example.domain.AnswerValidator
import com.example.domain.SpacedRepetitionEngine
import com.example.imports.ImportParser
import com.example.tools.EvalResult
import com.example.tools.MathEngine
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DomainAndMathUnitTests {

    @Test
    fun `spaced repetition does not master on single correct answer`() {
        val first = SpacedRepetitionEngine.computeNextSchedule(
            current = null,
            isCorrect = true,
            confidenceRating = 5,
            hintsUsed = 0,
            nowMillis = 1_000_000L
        )
        assertNotEquals(MasteryStatus.MASTERED, first.newMasteryStatus)
        assertEquals(MasteryStatus.LEARNING, first.newMasteryStatus)
        assertEquals(1, first.newStreak)
        assertEquals(1.0, first.newIntervalDays, 0.01)

        // Simulate progression through 2nd and 3rd reviews with high interval
        val progress2 = QuestionProgressEntity(
            questionId = 1L,
            attempts = 2,
            correctCount = 2,
            consecutiveCorrect = 2,
            intervalDays = 6.0,
            easeFactor = 2.6
        )
        val third = SpacedRepetitionEngine.computeNextSchedule(
            current = progress2,
            isCorrect = true,
            confidenceRating = 5,
            hintsUsed = 0,
            nowMillis = 10_000_000L
        )
        assertEquals(MasteryStatus.MASTERED, third.newMasteryStatus)
        assertTrue(third.newIntervalDays >= 14.0)
    }

    @Test
    fun `spaced repetition resets streak on incorrect answer`() {
        val current = QuestionProgressEntity(
            questionId = 1L,
            consecutiveCorrect = 4,
            intervalDays = 16.0,
            easeFactor = 2.5,
            masteryStatus = MasteryStatus.MASTERED.name
        )
        val next = SpacedRepetitionEngine.computeNextSchedule(current, isCorrect = false, confidenceRating = 2, hintsUsed = 0)
        assertEquals(MasteryStatus.LEARNING, next.newMasteryStatus)
        assertEquals(0, next.newStreak)
    }

    @Test
    fun `answer validator handles numerical tolerance, fractions, and chemical formulas`() {
        val numQ = QuestionEntity(
            uniqueCode = "Q1",
            subjectId = 1,
            gradeId = 1,
            unitId = 1,
            topicId = 1,
            questionType = QuestionType.NUMERICAL.name,
            questionText = "Compute acceleration",
            correctAnswer = "2.5",
            acceptedAnswersJson = """["2.5","5/2"]""",
            numericalTolerance = 0.05
        )
        assertTrue(AnswerValidator.evaluate(numQ, "2.48").isCorrect)
        assertTrue(AnswerValidator.evaluate(numQ, "5/2").isCorrect)
        assertFalse(AnswerValidator.evaluate(numQ, "3.1").isCorrect)

        val chemQ = QuestionEntity(
            uniqueCode = "Q2",
            subjectId = 3,
            gradeId = 2,
            unitId = 1,
            topicId = 1,
            questionType = QuestionType.SHORT_ANSWER.name,
            questionText = "Formula of aluminum oxide",
            correctAnswer = "Al2O3",
            acceptedAnswersJson = """["Al₂O₃"]"""
        )
        assertTrue(AnswerValidator.evaluate(chemQ, "Al₂O₃").isCorrect)
        assertTrue(AnswerValidator.evaluate(chemQ, "al2o3").isCorrect)
    }

    @Test
    fun `import parser validates JSON, CSV, TXT and flags duplicates and invalid rows`() {
        val json = """
            {
              "questions": [
                {
                  "uniqueCode": "IMP-1",
                  "subject": "Physics",
                  "grade": 11,
                  "questionText": "State F=ma",
                  "correctAnswer": "Newton 2nd Law"
                },
                {
                  "uniqueCode": "IMP-BAD",
                  "questionText": ""
                }
              ]
            }
        """.trimIndent()
        val res = ImportParser.parse(json, existingCodes = setOf("IMP-1"))
        assertEquals("JSON", res.formatName)
        assertEquals(1, res.validItems.size)
        assertEquals(1, res.duplicateCodes.size)
        assertEquals(1, res.rejectedRecords.size)

        val csv = """
            uniqueCode,subject,grade,unit,topic,difficulty,questionType,questionText,correctAnswer,options,hints,explanation
            CSV-1,Biology,10,Cells,Osmosis,EASY,TRUE_FALSE,Water moves across membrane,True,True|False,Passive,Osmosis
        """.trimIndent()
        val csvRes = ImportParser.parse(csv, emptySet())
        assertEquals("CSV", csvRes.formatName)
        assertEquals(1, csvRes.validItems.size)
    }

    @Test
    fun `math engine evaluates expressions, quadratic, molar mass, and balances chemical equations`() {
        val eval = MathEngine.evaluateExpression("sin(30) + log(100) + sqrt(9)", useDegrees = true)
        assertTrue(eval is EvalResult.Success)
        assertEquals(5.5, (eval as EvalResult.Success).value, 1e-6)

        val quad = MathEngine.solveQuadratic(1.0, -5.0, 6.0).getOrThrow()
        assertEquals(1.0, quad.discriminant, 1e-6)
        assertTrue(quad.description.contains("3") && quad.description.contains("2"))

        val (molarMass, counts) = MathEngine.calculateMolarMass("Ca(NO3)2").getOrThrow()
        assertEquals(164.086, molarMass, 0.2)
        assertEquals(6, counts["O"])

        val balanced = MathEngine.balanceChemicalEquation("Fe + O2 -> Fe2O3").getOrThrow()
        assertEquals("4Fe + 3O2 → 2Fe2O3", balanced)
    }
}
