package com.jigneshgaming.micai

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.jigneshgaming.micai.data.db.MicDatabase
import com.jigneshgaming.micai.data.prefs.SettingsStore

/**
 * Application entry point. Owns the singletons every other layer uses:
 * the local database and the settings store. Notification channels are
 * created once here so services/receivers can post without setup code.
 */
class MicApp : Application() {

    lateinit var database: MicDatabase
        private set

    lateinit var settings: SettingsStore
        private set

    override fun onCreate() {
        super.onCreate()
        database = MicDatabase(this)
        settings = SettingsStore(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channels = listOf(
            NotificationChannel(
                CHANNEL_ASSISTANT, getString(R.string.notification_channel_assistant),
                NotificationManager.IMPORTANCE_LOW
            ),
            NotificationChannel(
                CHANNEL_REMINDERS, getString(R.string.notification_channel_reminders),
                NotificationManager.IMPORTANCE_HIGH
            ),
            NotificationChannel(
                CHANNEL_CALLS, getString(R.string.notification_channel_calls),
                NotificationManager.IMPORTANCE_HIGH
            )
        )
        manager.createNotificationChannels(channels)
    }

    companion object {
        const val CHANNEL_ASSISTANT = "mic_assistant"
        const val CHANNEL_REMINDERS = "mic_reminders"
        const val CHANNEL_CALLS = "mic_calls"
    }
}
