package com.example.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.PreferencesRepository
import com.example.database.EueeDatabase
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class StudyReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return runCatching {
            val prefs = PreferencesRepository(applicationContext).preferencesFlow.first()
            if (!prefs.notificationsEnabled) return Result.success()

            val db = EueeDatabase.getInstance(applicationContext)
            val dueCount = db.learningProgressDao().observeDueReviewCount(System.currentTimeMillis()).first()
            if (dueCount > 0) {
                NotificationHelper.showReviewReminder(applicationContext, dueCount)
            }
            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object {
        private const val WORK_NAME = "euee_daily_review_check"

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<StudyReminderWorker>(6, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
