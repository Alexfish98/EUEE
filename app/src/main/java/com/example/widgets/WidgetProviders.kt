package com.example.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.PreferencesRepository
import com.example.database.EueeDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

object WidgetUpdateHelper {
    fun requestRefreshAll(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val progressIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, DailyProgressWidgetProvider::class.java)
        )
        if (progressIds.isNotEmpty()) {
            DailyProgressWidgetProvider.updateWidgets(context, appWidgetManager, progressIds)
        }
        val taskIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, TodayTasksWidgetProvider::class.java)
        )
        if (taskIds.isNotEmpty()) {
            TodayTasksWidgetProvider.updateWidgets(context, appWidgetManager, taskIds)
        }
    }
}

class DailyProgressWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun updateWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = EueeDatabase.getInstance(context)
                val prefsRepo = PreferencesRepository(context)
                val prefs = prefsRepo.preferencesFlow.first()
                val dueCount = db.learningProgressDao().observeDueReviewCount(System.currentTimeMillis()).first()
                val correct = db.learningProgressDao().observeTotalCorrectAnswers().first()
                val incorrect = db.learningProgressDao().observeTotalIncorrectAnswers().first()
                val totalAns = correct + incorrect
                val accuracy = if (totalAns == 0) 0 else (correct * 100) / totalAns
                val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val todayMinutes = (db.learningProgressDao().observeStudyTimeSince(startOfDay).first() / 60_000L).toInt()
                val nextAlarm = db.plannerAndAlarmDao().getEnabledAlarms().firstOrNull()
                val nextAlarmText = if (nextAlarm != null) {
                    "Next alarm: %02d:%02d • %s".format(nextAlarm.hour, nextAlarm.minute, nextAlarm.label)
                } else {
                    "Next alarm: None scheduled"
                }

                val openIntent = PendingIntent.getActivity(
                    context,
                    701,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                for (id in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_daily_progress).apply {
                        setTextViewText(R.id.widget_streak, "Streak: ${prefs.currentStreakDays}d")
                        setTextViewText(R.id.widget_due_reviews, "Due for review: $dueCount questions")
                        setTextViewText(
                            R.id.widget_daily_progress,
                            "Today: $todayMinutes / ${prefs.dailyStudyGoalMinutes} min • Accuracy: $accuracy%"
                        )
                        setTextViewText(R.id.widget_next_alarm, nextAlarmText)
                        setOnClickPendingIntent(R.id.widget_progress_root, openIntent)
                    }
                    appWidgetManager.updateAppWidget(id, views)
                }
            }
        }
    }
}

class TodayTasksWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun updateWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = EueeDatabase.getInstance(context)
                val prefsRepo = PreferencesRepository(context)
                val today = LocalDate.now().toEpochDay()
                val tasks = db.plannerAndAlarmDao().observeTasksForDate(today).first()
                val pending = tasks.filter { !it.isCompleted }
                val (focusActive, focusEnd) = prefsRepo.focusActiveFlow.first()
                val focusText = if (focusActive && focusEnd > System.currentTimeMillis()) {
                    val minsLeft = ((focusEnd - System.currentTimeMillis()) / 60_000L).coerceAtLeast(1L)
                    "Focus: ${minsLeft}m left"
                } else {
                    "Focus: Idle"
                }
                val summaryText = if (pending.isEmpty()) {
                    if (tasks.isEmpty()) "No tasks scheduled today. Tap to plan." else "All ${tasks.size} tasks completed today!"
                } else {
                    pending.take(3).joinToString("\n") { task ->
                        val h = task.startTimeMinutes / 60
                        val m = task.startTimeMinutes % 60
                        "• %02d:%02d %s".format(h, m, task.title)
                    }
                }

                val openIntent = PendingIntent.getActivity(
                    context,
                    702,
                    Intent(context, MainActivity::class.java).apply { putExtra("route", "planner") },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                for (id in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_today_tasks).apply {
                        setTextViewText(R.id.widget_focus_status, focusText)
                        setTextViewText(R.id.widget_task_summary, summaryText)
                        setOnClickPendingIntent(R.id.widget_tasks_root, openIntent)
                    }
                    appWidgetManager.updateAppWidget(id, views)
                }
            }
        }
    }
}
