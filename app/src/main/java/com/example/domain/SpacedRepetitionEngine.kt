package com.example.domain

import com.example.core.model.MasteryStatus
import com.example.core.model.SrsUpdateResult
import com.example.database.QuestionProgressEntity
import kotlin.math.max
import kotlin.math.roundToLong

object SpacedRepetitionEngine {
    private const val DAY_MS = 24L * 60L * 60L * 1000L

    /**
     * Computes updated spaced-repetition scheduling using an SM-2 adaptation.
     *
     * Rules:
     * - An individual correct answer NEVER immediately marks a question as MASTERED.
     * - Mastery requires at least 3 consecutive correct reviews, interval >= 14 days,
     *   and confidence >= 4.
     * - Incorrect answers reset consecutive streak, lower ease factor, and schedule
     *   an immediate learning re-queue (4 hours).
     */
    fun computeNextSchedule(
        current: QuestionProgressEntity?,
        isCorrect: Boolean,
        confidenceRating: Int, // 1..5
        hintsUsed: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): SrsUpdateResult {
        val clampedConfidence = confidenceRating.coerceIn(1, 5)
        val prevEase = current?.easeFactor ?: 2.5
        val prevInterval = current?.intervalDays ?: 0.0
        val prevConsecutive = current?.consecutiveCorrect ?: 0

        if (!isCorrect) {
            val updatedEase = max(1.3, prevEase - 0.20)
            val nextReview = nowMillis + (4L * 60L * 60L * 1000L) // 4 hours later
            return SrsUpdateResult(
                newMasteryStatus = MasteryStatus.LEARNING,
                newIntervalDays = 0.25,
                newEaseFactor = updatedEase,
                nextReviewAt = nextReview,
                newStreak = 0
            )
        }

        // Penalize quality slightly if hints were needed
        val effectiveQuality = (clampedConfidence - if (hintsUsed > 0) 1 else 0).coerceIn(2, 5)
        val delta = 0.1 - (5 - effectiveQuality) * (0.08 + (5 - effectiveQuality) * 0.02)
        val newEase = max(1.3, prevEase + delta)

        val newStreak = prevConsecutive + 1
        val newIntervalDays = when {
            newStreak == 1 -> 1.0
            newStreak == 2 -> 4.0
            else -> (prevInterval * newEase).coerceIn(4.0, 120.0)
        }

        val newStatus = when {
            newStreak >= 3 && newIntervalDays >= 14.0 && clampedConfidence >= 4 && hintsUsed == 0 ->
                MasteryStatus.MASTERED
            newStreak >= 2 ->
                MasteryStatus.REVIEWING
            else ->
                MasteryStatus.LEARNING
        }

        val nextReviewAt = nowMillis + (newIntervalDays * DAY_MS).roundToLong()

        return SrsUpdateResult(
            newMasteryStatus = newStatus,
            newIntervalDays = newIntervalDays,
            newEaseFactor = newEase,
            nextReviewAt = nextReviewAt,
            newStreak = newStreak
        )
    }
}
