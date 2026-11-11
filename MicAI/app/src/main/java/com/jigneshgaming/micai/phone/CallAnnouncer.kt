package com.jigneshgaming.micai.phone

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.jigneshgaming.micai.data.prefs.SettingsStore
import com.jigneshgaming.micai.voice.SpeechEngine

/**
 * Announces incoming callers by name using text-to-speech:
 * "Incoming call from Rahul" for saved contacts, "Incoming call from an
 * unknown number" otherwise. Requires READ_PHONE_STATE + READ_CONTACTS.
 *
 * Registered from [com.jigneshgaming.micai.MainActivity] while the app is
 * in the foreground (Android 12+ restricts background phone-state
 * listeners). The announcer is a no-op when the user disabled it in
 * Settings or the permissions are missing.
 */
class CallAnnouncer(
    context: Context,
    private val settings: SettingsStore,
    private val speech: SpeechEngine
) {

    private val appContext = context.applicationContext
    private val phoneAssistant = PhoneAssistant(appContext)
    private var telephony: TelephonyManager? = null
    private var wasRinging = false

    private val listener = object : PhoneStateListener() {
        @Deprecated("PhoneStateListener is deprecated on API 31+ but still functional")
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> {
                    if (!wasRinging) {
                        wasRinging = true
                        announce(phoneNumber.orEmpty())
                    }
                }
                TelephonyManager.CALL_STATE_IDLE -> wasRinging = false
            }
        }
    }

    fun start() {
        if (!settings.announceCalls) return
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_PHONE_STATE) !=
            PackageManager.PERMISSION_GRANTED
        ) return
        val manager = appContext.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            ?: return
        telephony = manager
        @Suppress("DEPRECATION")
        manager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    fun stop() {
        telephony?.listen(listener, PhoneStateListener.LISTEN_NONE)
        telephony = null
    }

    private fun announce(number: String) {
        if (!settings.announceCalls) return
        val name = phoneAssistant.contactNameFor(number)
        val message = if (name != null) {
            "Incoming call from $name"
        } else {
            "Incoming call from an unknown number"
        }
        speech.speak(message)
    }

    companion object {
        /** True when the runtime permissions for announcements are granted. */
        fun hasPermissions(context: Context): Boolean {
            val readState = ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
            val readContacts = ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
            return readState && readContacts
        }

        @Suppress("UNUSED_PARAMETER")
        fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }
}
