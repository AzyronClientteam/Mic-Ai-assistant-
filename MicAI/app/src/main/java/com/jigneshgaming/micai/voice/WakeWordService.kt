package com.jigneshgaming.micai.voice

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import com.jigneshgaming.micai.MicApp
import com.jigneshgaming.micai.R

/**
 * "Hey MIC" wake-word listener.
 *
 * Runs as a foreground service and loops a [SpeechRecognizer] in free-form
 * mode; when a result contains the wake phrase ("hey mic", "hey mike",
 * "ok mic") it broadcasts [ACTION_WAKE_WORD] so the UI can start a full
 * command listen. This needs no proprietary hotword SDK and works with any
 * installed speech pack, at the cost of slightly higher battery use than a
 * dedicated on-device hotword model (see docs/ARCHITECTURE.md for the
 * Porcupine upgrade path).
 *
 * The service only listens while the user has enabled the wake word in
 * Settings and granted RECORD_AUDIO.
 */
class WakeWordService : Service() {

    private var recognizer: SpeechRecognizer? = null
    private var running = false

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onPartialResults(partialResults: Bundle?) = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val heard = matches?.firstOrNull()?.lowercase().orEmpty()
            Log.d(TAG, "heard: $heard")
            if (WAKE_PHRASES.any { heard.contains(it) }) {
                val broadcast = Intent(ACTION_WAKE_WORD).setPackage(packageName)
                sendBroadcast(broadcast)
            }
            if (running) listenOnce()
        }

        override fun onError(error: Int) {
            // SpeechRecognizer errors (no speech, network, busy…) are routine
            // in a listen loop — just listen again after a short pause.
            if (running) scheduleRetry()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopListening()
            stopSelf()
            return START_NOT_STICKY
        }
        startListening()
        return START_STICKY
    }

    private fun startListening() {
        if (running) return
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.w(TAG, "Speech recognition unavailable on this device")
            return
        }
        running = true
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(recognitionListener)
        }
        listenOnce()
    }

    private fun listenOnce() {
        if (!running) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try {
            recognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Wake-word listen failed: ${e.message}")
            scheduleRetry()
        }
    }

    private fun scheduleRetry() {
        // Small delay avoids a tight error loop on devices without a
        // recognizer service; the handler is process-local and cheap.
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
            { if (running) listenOnce() }, 800
        )
    }

    private fun stopListening() {
        running = false
        try {
            recognizer?.cancel()
            recognizer?.destroy()
        } catch (_: Exception) {
        }
        recognizer = null
    }

    override fun onDestroy() {
        stopListening()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder(this, MicApp.CHANNEL_ASSISTANT)
        } else {
            @Suppress("DEPRECATION")
            NotificationCompat.Builder(this)
        }
        return builder
            .setContentTitle("MIC AI is listening for \"Hey MIC\"")
            .setContentText("Tap MIC AI to open the assistant")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_WAKE_WORD = "com.jigneshgaming.micai.WAKE_WORD"
        const val ACTION_STOP = "com.jigneshgaming.micai.STOP_WAKE_WORD"
        private const val NOTIFICATION_ID = 1001
        private const val TAG = "WakeWordService"
        private val WAKE_PHRASES = listOf("hey mic", "hey mike", "ok mic", "okay mic")

        fun start(context: android.content.Context) {
            val intent = Intent(context, WakeWordService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: android.content.Context) {
            context.startService(Intent(context, WakeWordService::class.java).setAction(ACTION_STOP))
        }
    }
}
