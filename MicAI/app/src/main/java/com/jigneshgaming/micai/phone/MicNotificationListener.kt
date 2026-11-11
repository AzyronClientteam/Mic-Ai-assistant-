package com.jigneshgaming.micai.phone

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Reads notifications so MIC AI can summarise them on request
 * ("read my notifications"). The user must enable MIC AI in
 * Settings → Notifications → Notification access. This service only
 * stores the latest notifications in memory — nothing leaves the device.
 */
class MicNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        val notification = sbn.notification
        val title = notification.extras.getCharSequence("android.title")?.toString().orEmpty()
        val text = notification.extras.getCharSequence("android.text")?.toString().orEmpty()
        val app = sbn.packageName.substringAfterLast('.')
        val summary = listOf(title, text).filter { it.isNotBlank() }.joinToString(": ")
        if (summary.isNotBlank()) {
            synchronized(recent) {
                recent.add(0, "$app — $summary")
                while (recent.size > MAX_RECENT) recent.removeAt(recent.size - 1)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
    }

    companion object {
        private const val MAX_RECENT = 30
        private val recent = mutableListOf<String>()

        /** Latest notifications, newest first. Empty when access is off. */
        fun recentNotifications(): List<String> = synchronized(recent) { recent.toList() }

        /** Whether the user has granted notification-listener access. */
        fun isEnabled(context: android.content.Context): Boolean {
            val enabled = android.provider.Settings.Secure.getString(
                context.contentResolver, "enabled_notification_listeners"
            ) ?: return false
            return enabled.contains(context.packageName)
        }
    }
}
