package com.jigneshgaming.micai

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jigneshgaming.micai.ai.AiClient
import com.jigneshgaming.micai.ai.LocalBrain
import com.jigneshgaming.micai.ai.WeatherClient
import com.jigneshgaming.micai.data.db.HistoryEntry
import com.jigneshgaming.micai.data.db.Note
import com.jigneshgaming.micai.data.db.Reminder
import com.jigneshgaming.micai.phone.CallAnnouncer
import com.jigneshgaming.micai.phone.MicNotificationListener
import com.jigneshgaming.micai.phone.PhoneAssistant
import com.jigneshgaming.micai.voice.SpeechEngine
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/** Visual state of the assistant orb. */
enum class AssistantState { IDLE, LISTENING, THINKING, SPEAKING }

data class ChatMessage(val role: String, val text: String, val at: Long = System.currentTimeMillis())

data class UiState(
    val state: AssistantState = AssistantState.IDLE,
    val messages: List<ChatMessage> = emptyList(),
    val notes: List<Note> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
    val history: List<HistoryEntry> = emptyList(),
    val online: Boolean = false,
    val batteryPercent: Int = -1,
    val clockText: String = "",
    val aiConfigured: Boolean = false,
    val localOnly: Boolean = false,
    val voiceEnabled: Boolean = true,
    val wakeWordEnabled: Boolean = false,
    val announceCalls: Boolean = true,
    val announceNotifications: Boolean = false,
    val speechLanguage: String = "",
    val assistantName: String = "MIC",
    val baseUrl: String = "",
    val model: String = "",
    val hasApiKey: Boolean = false,
    val lastReply: String = ""
)

/**
 * Single ViewModel for the whole app. Routes every user turn:
 *  1. local device intents (notes, reminders, apps, battery, weather…)
 *  2. online AI ([AiClient]) when configured and reachable
 *  3. offline fallback ([LocalBrain]) with an honest explanation
 *
 * Every reply is spoken (when voice is on), shown in chat and saved to
 * history. Sensitive actions (sending messages, dialling) are never
 * performed automatically — MIC AI drafts and asks.
 */
class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MicApp
    private val db = app.database
    private val settings = app.settings
    private val brain = LocalBrain()
    private val aiClient = AiClient(settings)
    private val weatherClient = WeatherClient()
    private val phone = PhoneAssistant(app)
    val speech = SpeechEngine(app, settings)
    val callAnnouncer = CallAnnouncer(app, settings, speech)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /** Emitted when the UI should launch the speech recognizer. */
    private val _listenRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val listenRequests = _listenRequests.asSharedFlow()

    private val connectivity = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = setOnline(true)
        override fun onLost(network: Network) = setOnline(false)
    }

    init {
        speech.onSpeakingChanged = { speaking ->
            _uiState.update {
                it.copy(state = if (speaking) AssistantState.SPEAKING else AssistantState.IDLE)
            }
        }
        try {
            connectivity.registerDefaultNetworkCallback(networkCallback)
        } catch (_: Exception) {
        }
        refresh()
        tickClock()
    }

    // -------------------------------------------------------------- public

    fun requestListen() {
        _uiState.update { it.copy(state = AssistantState.LISTENING) }
        _listenRequests.tryEmit(Unit)
    }

    fun onSpeechResult(text: String?) {
        if (text.isNullOrBlank()) {
            _uiState.update { it.copy(state = AssistantState.IDLE) }
            return
        }
        onUserText(text)
    }

    fun onUserText(text: String, imageFile: File? = null) {
        val clean = text.trim()
        if (clean.isBlank() && imageFile == null) return
        val imageBase64 = imageFile?.let { encodeImage(it) }
        addMessage("user", clean.ifBlank { "[image]" })
        _uiState.update { it.copy(state = AssistantState.THINKING) }
        viewModelScope.launch {
            val reply = route(clean, imageBase64)
            addMessage("assistant", reply)
            _uiState.update { it.copy(state = AssistantState.IDLE, lastReply = reply) }
            speech.speak(reply)
        }
    }

    fun sendText(text: String) = onUserText(text)

    fun stopSpeaking() = speech.stopSpeaking()

    // --------------------------------------------------------------- routing

    private suspend fun route(text: String, imageBase64: String?): String {
        val lower = text.lowercase()

        // Extra device intents handled here (need live Android state).
        if (lower.contains("notification")) {
            val recent = MicNotificationListener.recentNotifications()
            return if (recent.isEmpty()) {
                "No recent notifications, or notification access is off. Enable it in Settings if you want me to read them."
            } else {
                "Your latest notifications: " + recent.take(5).joinToString(". ")
            }
        }
        if (lower.contains("storage")) return phone.storageInfo()

        val local = brain.handle(text)
        when (val action = local.action) {
            is LocalBrain.Action.OpenApp -> {
                val opened = phone.openAppByName(action.name)
                return if (opened != null) "Opening $opened." else "I could not find an app called ${action.name}."
            }
            is LocalBrain.Action.AddNote -> {
                db.insertNote("Note", action.text)
                refreshNotes()
                return "Note saved: ${action.text}"
            }
            is LocalBrain.Action.SetReminder -> {
                val triggerAt = System.currentTimeMillis() + action.inMinutes * 60_000
                val id = db.insertReminder(action.text, triggerAt)
                phone.scheduleReminder(id, action.text, triggerAt)
                refreshReminders()
                return "Reminder set: ${action.text}, in ${action.inMinutes} minutes."
            }
            is LocalBrain.Action.SetAlarm -> {
                val ok = phone.setAlarm(action.hour, action.minute, action.label)
                return if (ok) "Alarm set for ${action.hour}:${action.minute.toString().padStart(2, '0')}."
                else "No clock app found to set the alarm."
            }
            is LocalBrain.Action.SearchContact -> {
                val found = phone.searchContacts(action.name)
                return if (found.isEmpty()) "No contact found for ${action.name}."
                else "Found: " + found.joinToString("; ")
            }
            is LocalBrain.Action.DraftMessage -> {
                val ok = phone.draftSms(action.text)
                return if (ok) "Draft ready in your messaging app. Review it before sending."
                else "No messaging app found."
            }
            is LocalBrain.Action.Weather -> {
                val city = action.city
                if (city.isNullOrBlank()) return "Which city should I check the weather for, sir?"
                return weatherClient.weatherFor(city)
                    ?: "I could not reach the weather service. Check your internet connection."
            }
            null -> Unit
        }

        // Live-value placeholders produced by the local brain.
        if (local.text == "Battery_ACTION") return phone.batteryStatus()
        if (local.text == "NOTES_ACTION") {
            val notes = db.listNotes()
            return if (notes.isEmpty()) "You have no notes yet. Say 'note' followed by what to save."
            else "Your notes: " + notes.take(5).joinToString(". ") { it.body }
        }
        if (local.text == "WEATHER_ACTION") return "Which city should I check the weather for, sir?"

        // Online AI (text + vision).
        if (imageBase64 != null || aiClient.isConfigured) {
            val history = db.listHistory(20).map { it.role to it.text }
            val online = aiClient.chat(history, text, imageBase64)
            if (online != null) return online
            if (imageBase64 != null) {
                return "Image understanding needs the online AI. Add an API key in Settings, or describe the image to me."
            }
        }

        // Offline fallback — still useful for device commands above.
        return local.text
    }

    // ---------------------------------------------------------------- notes

    fun addNote(title: String, body: String) {
        if (body.isBlank()) return
        db.insertNote(title.ifBlank { "Note" }, body)
        refreshNotes()
    }

    fun deleteNote(id: Long) {
        db.deleteNote(id)
        refreshNotes()
    }

    // ------------------------------------------------------------- reminders

    fun addReminder(text: String, inMinutes: Long) {
        if (text.isBlank()) return
        val triggerAt = System.currentTimeMillis() + inMinutes * 60_000
        val id = db.insertReminder(text, triggerAt)
        phone.scheduleReminder(id, text, triggerAt)
        refreshReminders()
    }

    fun deleteReminder(id: Long) {
        phone.cancelReminder(id)
        db.deleteReminder(id)
        refreshReminders()
    }

    fun openExactAlarmSettings() = phone.openExactAlarmSettings()

    // --------------------------------------------------------------- history

    fun clearHistory() {
        db.clearHistory()
        _uiState.update { it.copy(messages = emptyList(), history = emptyList()) }
    }

    // -------------------------------------------------------------- settings

    fun updateSettings(
        baseUrl: String? = null,
        model: String? = null,
        apiKey: String? = null,
        localOnly: Boolean? = null,
        voiceEnabled: Boolean? = null,
        wakeWordEnabled: Boolean? = null,
        announceCalls: Boolean? = null,
        announceNotifications: Boolean? = null,
        speechLanguage: String? = null,
        assistantName: String? = null
    ) {
        if (baseUrl != null) settings.aiBaseUrl = baseUrl
        if (model != null) settings.aiModel = model
        if (apiKey != null) settings.aiApiKey = apiKey
        if (localOnly != null) settings.localOnlyMode = localOnly
        if (voiceEnabled != null) settings.voiceEnabled = voiceEnabled
        if (wakeWordEnabled != null) settings.wakeWordEnabled = wakeWordEnabled
        if (announceCalls != null) settings.announceCalls = announceCalls
        if (announceNotifications != null) settings.announceNotifications = announceNotifications
        if (speechLanguage != null) settings.speechLanguage = speechLanguage
        if (assistantName != null) settings.assistantName = assistantName
        refresh()
    }

    // ----------------------------------------------------------------- misc

    fun refresh() {
        _uiState.update {
            it.copy(
                notes = db.listNotes(),
                reminders = db.listReminders(),
                history = db.listHistory(),
                batteryPercent = phone.batteryPercent(),
                aiConfigured = aiClient.isConfigured,
                localOnly = settings.localOnlyMode,
                voiceEnabled = settings.voiceEnabled,
                wakeWordEnabled = settings.wakeWordEnabled,
                announceCalls = settings.announceCalls,
                announceNotifications = settings.announceNotifications,
                speechLanguage = settings.speechLanguage,
                assistantName = settings.assistantName,
                baseUrl = settings.aiBaseUrl,
                model = settings.aiModel,
                hasApiKey = settings.hasAiCredentials
            )
        }
    }

    override fun onCleared() {
        try {
            connectivity.unregisterNetworkCallback(networkCallback)
        } catch (_: Exception) {
        }
        callAnnouncer.stop()
        speech.shutdown()
        super.onCleared()
    }

    // -------------------------------------------------------------- helpers

    private fun addMessage(role: String, text: String) {
        db.insertHistory(role, text)
        _uiState.update {
            it.copy(
                messages = (it.messages + ChatMessage(role, text)).takeLast(100),
                history = db.listHistory()
            )
        }
    }

    private fun refreshNotes() = _uiState.update { it.copy(notes = db.listNotes()) }

    private fun refreshReminders() = _uiState.update { it.copy(reminders = db.listReminders()) }

    private fun setOnline(value: Boolean) = _uiState.update { it.copy(online = value) }

    private fun tickClock() {
        viewModelScope.launch {
            while (true) {
                val cal = phone.calendar()
                val h = cal.get(java.util.Calendar.HOUR_OF_DAY)
                val m = cal.get(java.util.Calendar.MINUTE)
                val ampm = if (h < 12) "AM" else "PM"
                val h12 = when (val v = h % 12) { 0 -> 12; else -> v }
                _uiState.update {
                    it.copy(
                        clockText = "$h12:${m.toString().padStart(2, '0')} $ampm",
                        batteryPercent = phone.batteryPercent()
                    )
                }
                kotlinx.coroutines.delay(30_000)
            }
        }
    }

    private fun encodeImage(file: File): String? = try {
        val bytes = file.readBytes()
        if (bytes.size > MAX_IMAGE_BYTES) null
        else Base64.encodeToString(bytes, Base64.NO_WRAP)
    } catch (_: Exception) {
        null
    }

    companion object {
        private const val MAX_IMAGE_BYTES = 4 * 1024 * 1024 // keep requests sane
    }
}
