package com.jigneshgaming.micai.service

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.jigneshgaming.micai.MicApp
import com.jigneshgaming.micai.R

/**
 * Fires when a reminder alarm goes off: posts a high-priority notification
 * and marks the reminder done in the local database.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, -1L)
        val text = intent.getStringExtra(EXTRA_TEXT) ?: "Reminder"
        val app = context.applicationContext as? MicApp ?: return

        if (id >= 0) {
            app.database.markReminderDone(id)
        }

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder(context, MicApp.CHANNEL_REMINDERS)
        } else {
            @Suppress("DEPRECATION")
            NotificationCompat.Builder(context)
        }
        val notification: Notification = builder
            .setContentTitle("MIC AI reminder")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                (id % Int.MAX_VALUE).toInt(), notification
            )
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted — reminder is still marked done.
        }
    }

    companion object {
        const val EXTRA_ID = "reminder_id"
        const val EXTRA_TEXT = "reminder_text"
    }
}
