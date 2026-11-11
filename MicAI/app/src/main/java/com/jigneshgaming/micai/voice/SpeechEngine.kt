package com.jigneshgaming.micai.voice

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.jigneshgaming.micai.data.prefs.SettingsStore
import java.util.Locale

/**
 * Speech-to-text (via the system [SpeechRecognizer], which supports every
 * language pack installed on the device — English, Hindi, Gujarati and
 * more) and text-to-speech (via [TextToSpeech] with automatic locale
 * detection from the user's text).
 *
 * STT language follows the device default unless the user pins one in
 * Settings. TTS picks a voice matching the reply text (Devanagari → Hindi,
 * Gujarati script → Gujarati, otherwise the device default).
 */
class SpeechEngine(
    context: Context,
    private val settings: SettingsStore
) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var speaking = false
    private var pendingText: String? = null

    var onSpeakingChanged: ((Boolean) -> Unit)? = null

    init {
        tts = TextToSpeech(appContext, this)
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        val engine = tts ?: return
        engine.setLanguage(preferredLocale())
        engine.setSpeechRate(1.0f)
        engine.setPitch(1.0f)
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) = setSpeaking(true)
            override fun onDone(id: String?) = setSpeaking(false)
            override fun onError(id: String?) = setSpeaking(false)
            @Deprecated("Legacy callback kept for API 28 compatibility")
            override fun onError(id: String?, errorCode: Int) = setSpeaking(false)
        })
        ttsReady = true
        // A reply can arrive before the engine finishes initialising (the
        // first answer after launch). Speak it now instead of dropping it.
        pendingText?.let { queued ->
            pendingText = null
            speak(queued)
        }
    }

    /** Speak [text], interrupting anything currently playing. */
    fun speak(text: String) {
        val engine = tts ?: return
        if (!settings.voiceEnabled || text.isBlank()) return
        if (!ttsReady) {
            pendingText = text
            return
        }
        engine.stop()
        engine.language = localeFor(text)
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "mic_ai_reply")
    }

    fun stopSpeaking() {
        pendingText = null
        tts?.stop()
        setSpeaking(false)
    }

    val isSpeaking: Boolean get() = speaking

    /** Build the recognizer intent for one voice command. */
    fun recognizerIntent(): Intent? {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) return null
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, preferredLocale().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
    }

    fun shutdown() {
        pendingText = null
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    // ------------------------------------------------------------- locales

    private fun preferredLocale(): Locale {
        val pinned = settings.speechLanguage.trim()
        if (pinned.isNotBlank()) {
            return Locale.forLanguageTag(pinned)
        }
        return Locale.getDefault()
    }

    /** Pick a TTS locale that matches the script of the reply text. */
    private fun localeFor(text: String): Locale {
        val hasDevanagari = text.any { it in '\u0900'..'\u097F' }
        val hasGujarati = text.any { it in '\u0A80'..'\u0AFF' }
        return when {
            hasDevanagari -> Locale("hi", "IN")
            hasGujarati -> Locale("gu", "IN")
            else -> preferredLocale()
        }
    }

    private fun setSpeaking(value: Boolean) {
        if (speaking == value) return
        speaking = value
        onSpeakingChanged?.invoke(value)
    }
}
