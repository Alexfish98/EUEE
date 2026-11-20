package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {
    const val CHANNEL_ALARMS = "euee_alarms"
    const val CHANNEL_FOCUS = "euee_focus"
    const val CHANNEL_REMINDERS = "euee_reminders"
    const val CHANNEL_REVIEWS = "euee_reviews"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val alarmChannel = NotificationChannel(
            CHANNEL_ALARMS,
            "Study & Exam Alarms",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Scheduled wake-up, study, break, and examination alarms"
            enableVibration(true)
        }

        val focusChannel = NotificationChannel(
            CHANNEL_FOCUS,
            "Focus & Pomodoro Timer",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Persistent countdown notification during focus sessions"
            setShowBadge(false)
        }

        val reminderChannel = NotificationChannel(
            CHANNEL_REMINDERS,
            "Study Reminders & Goals",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Daily study goal and planner task reminders"
        }

        val reviewChannel = NotificationChannel(
            CHANNEL_REVIEWS,
            "Spaced Repetition Queue",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications when questions are due for review"
        }

        nm.createNotificationChannels(listOf(alarmChannel, focusChannel, reminderChannel, reviewChannel))
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun showReviewReminder(context: Context, dueCount: Int) {
        if (!hasNotificationPermission(context) || dueCount <= 0) return
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("route", "study_smart")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            401,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REVIEWS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("EUEE Review Queue Ready")
            .setContentText("$dueCount question${if (dueCount == 1) "" else "s"} due for spaced repetition today.")
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(401, notification)
    }

    fun showFocusCompletedNotification(context: Context, label: String, minutes: Int) {
        if (!hasNotificationPermission(context)) return
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("route", "focus")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            402,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Focus Session Complete")
            .setContentText("$label ($minutes min) finished. Great work!")
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(402, notification)
    }
}
