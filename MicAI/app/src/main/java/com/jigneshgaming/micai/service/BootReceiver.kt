package com.jigneshgaming.micai.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jigneshgaming.micai.MicApp
import com.jigneshgaming.micai.phone.PhoneAssistant
import com.jigneshgaming.micai.voice.WakeWordService

/**
 * After a reboot or app update: re-schedules every future reminder with
 * [AlarmManager] and restarts the wake-word listener if the user enabled it.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? MicApp ?: return
        val phone = PhoneAssistant(context)
        val now = System.currentTimeMillis()
        for (reminder in app.database.listReminders()) {
            if (!reminder.done && reminder.triggerAt > now) {
                phone.scheduleReminder(reminder.id, reminder.text, reminder.triggerAt)
            }
        }
        if (app.settings.wakeWordEnabled) {
            WakeWordService.start(context)
        }
    }
}
