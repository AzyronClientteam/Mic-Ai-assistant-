package com.jigneshgaming.micai.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * User preferences. The AI API key is stored in [EncryptedSharedPreferences]
 * (AES-256 at rest) and is never compiled into the APK. Everything else is
 * plain preferences so it survives backup/restore predictably.
 */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val securePrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            SECURE_PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // ------------------------------------------------------------ AI backend

    var aiBaseUrl: String
        get() = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
        set(value) = prefs.edit().putString(KEY_BASE_URL, value).apply()

    var aiModel: String
        get() = prefs.getString(KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
        set(value) = prefs.edit().putString(KEY_MODEL, value).apply()

    /** API key — encrypted at rest, sent only to the configured endpoint. */
    var aiApiKey: String
        get() = securePrefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = securePrefs.edit().putString(KEY_API_KEY, value).apply()

    /** When true, MIC AI never calls the network AI and uses the local brain. */
    var localOnlyMode: Boolean
        get() = prefs.getBoolean(KEY_LOCAL_ONLY, false)
        set(value) = prefs.edit().putBoolean(KEY_LOCAL_ONLY, value).apply()

    // ---------------------------------------------------------------- voice

    /** BCP-47 tag such as "en-IN", "hi-IN", "gu-IN". Empty = auto (device). */
    var speechLanguage: String
        get() = prefs.getString(KEY_SPEECH_LANG, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SPEECH_LANG, value).apply()

    var voiceEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOICE, true)
        set(value) = prefs.edit().putBoolean(KEY_VOICE, value).apply()

    var wakeWordEnabled: Boolean
        get() = prefs.getBoolean(KEY_WAKE_WORD, false)
        set(value) = prefs.edit().putBoolean(KEY_WAKE_WORD, value).apply()

    // -------------------------------------------------------------- phone AI

    var announceCalls: Boolean
        get() = prefs.getBoolean(KEY_ANNOUNCE_CALLS, true)
        set(value) = prefs.edit().putBoolean(KEY_ANNOUNCE_CALLS, value).apply()

    var announceNotifications: Boolean
        get() = prefs.getBoolean(KEY_ANNOUNCE_NOTIFS, false)
        set(value) = prefs.edit().putBoolean(KEY_ANNOUNCE_NOTIFS, value).apply()

    var assistantName: String
        get() = prefs.getString(KEY_NAME, "MIC") ?: "MIC"
        set(value) = prefs.edit().putString(KEY_NAME, value).apply()

    val hasAiCredentials: Boolean get() = aiApiKey.isNotBlank()

    companion object {
        private const val PREFS = "mic_ai_prefs"
        private const val SECURE_PREFS = "mic_ai_secure_prefs"
        private const val KEY_BASE_URL = "ai_base_url"
        private const val KEY_MODEL = "ai_model"
        private const val KEY_API_KEY = "ai_api_key"
        private const val KEY_LOCAL_ONLY = "local_only"
        private const val KEY_SPEECH_LANG = "speech_lang"
        private const val KEY_VOICE = "voice_enabled"
        private const val KEY_WAKE_WORD = "wake_word"
        private const val KEY_ANNOUNCE_CALLS = "announce_calls"
        private const val KEY_ANNOUNCE_NOTIFS = "announce_notifs"
        private const val KEY_NAME = "assistant_name"

        const val DEFAULT_BASE_URL = "https://api.openai.com/v1"
        const val DEFAULT_MODEL = "gpt-4o-mini"
    }
}
