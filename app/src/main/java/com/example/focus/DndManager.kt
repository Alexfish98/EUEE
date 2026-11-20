package com.example.focus

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings

data class DndStatus(
    val hasPolicyAccess: Boolean,
    val currentFilter: Int,
    val isDndCurrentlyActive: Boolean
)

object DndManager {

    fun getStatus(context: Context): DndStatus {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val granted = nm.isNotificationPolicyAccessGranted
        val filter = nm.currentInterruptionFilter
        val active = filter != NotificationManager.INTERRUPTION_FILTER_ALL
        return DndStatus(
            hasPolicyAccess = granted,
            currentFilter = filter,
            isDndCurrentlyActive = active
        )
    }

    fun buildPolicyAccessIntent(): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Enables Priority-Only DND if Notification Policy Access was explicitly granted by the user.
     * Returns the previous interruption filter so it can be restored when the focus session ends.
     */
    fun enableFocusDnd(context: Context): Int? {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!nm.isNotificationPolicyAccessGranted) return null
        val previous = nm.currentInterruptionFilter
        return runCatching {
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            previous
        }.getOrNull()
    }

    /**
     * Safely restores the interruption filter captured before the focus session began.
     */
    fun restoreDnd(context: Context, previousFilter: Int?) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!nm.isNotificationPolicyAccessGranted || previousFilter == null) return
        runCatching {
            nm.setInterruptionFilter(previousFilter)
        }
    }
}
