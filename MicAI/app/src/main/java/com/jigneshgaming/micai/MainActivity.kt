package com.jigneshgaming.micai

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import com.jigneshgaming.micai.ui.screens.ChatScreen
import com.jigneshgaming.micai.ui.screens.HistoryScreen
import com.jigneshgaming.micai.ui.screens.HomeScreen
import com.jigneshgaming.micai.ui.screens.NotesScreen
import com.jigneshgaming.micai.ui.screens.PermissionsScreen
import com.jigneshgaming.micai.ui.screens.RemindersScreen
import com.jigneshgaming.micai.ui.screens.SettingsScreen
import com.jigneshgaming.micai.ui.theme.MicAiTheme
import com.jigneshgaming.micai.voice.WakeWordService

/**
 * Single-activity app. Hosts the bottom-navigation shell, the speech
 * recognizer launcher, the wake-word broadcast receiver and the
 * call-announcer lifecycle.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: AssistantViewModel by viewModels()

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val text = if (result.resultCode == RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        } else {
            null
        }
        viewModel.onSpeechResult(text)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* permissions are re-checked by the screens on next visit */ }

    private val wakeWordReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == WakeWordService.ACTION_WAKE_WORD) {
                viewModel.requestListen()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ask for the mic up-front so voice works on first launch.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
        }

        setContent {
            MicAiTheme {
                MicAiApp(
                    viewModel = viewModel,
                    onListen = { launchRecognizer() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.callAnnouncer.start()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                wakeWordReceiver,
                IntentFilter(WakeWordService.ACTION_WAKE_WORD),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(
                wakeWordReceiver,
                IntentFilter(WakeWordService.ACTION_WAKE_WORD)
            )
        }
        syncWakeWordService()
    }

    override fun onPause() {
        viewModel.callAnnouncer.stop()
        try {
            unregisterReceiver(wakeWordReceiver)
        } catch (_: Exception) {
        }
        super.onPause()
    }

    private fun launchRecognizer() {
        val intent = viewModel.speech.recognizerIntent()
        if (intent != null) {
            try {
                speechLauncher.launch(intent)
                return
            } catch (_: Exception) {
            }
        }
        viewModel.onSpeechResult(null)
    }

    private fun syncWakeWordService() {
        val enabled = (application as MicApp).settings.wakeWordEnabled
        val micGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (enabled && micGranted) {
            WakeWordService.start(this)
        } else {
            WakeWordService.stop(this)
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab("home", "Home", Icons.Filled.Home),
    Tab("chat", "Chat", Icons.Filled.Chat),
    Tab("notes", "Notes", Icons.Filled.Note),
    Tab("reminders", "Reminders", Icons.Filled.Notifications),
    Tab("history", "History", Icons.Filled.History),
    Tab("settings", "Settings", Icons.Filled.Settings)
)

@Composable
private fun MicAiApp(viewModel: AssistantViewModel, onListen: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var selected by remember { mutableStateOf("home") }
    var showPermissions by remember { mutableStateOf(false) }

    // Voice requests coming from the ViewModel (mic button, wake word).
    LaunchedEffect(viewModel) {
        viewModel.listenRequests.collect { onListen() }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                TABS.forEach { tab ->
                    NavigationBarItem(
                        selected = selected == tab.route,
                        onClick = { selected = tab.route },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        if (showPermissions) {
            PermissionsScreen(modifier = Modifier.padding(padding))
            return@Scaffold
        }
        when (selected) {
            "home" -> HomeScreen(
                state = state,
                onMicClick = { viewModel.requestListen() },
                onStopClick = { viewModel.stopSpeaking() },
                onQuickCommand = { viewModel.sendText(it) },
                modifier = Modifier.padding(padding)
            )
            "chat" -> ChatScreen(
                messages = state.messages,
                onSend = { text, image -> viewModel.onUserText(text, image) },
                onMicClick = { viewModel.requestListen() },
                modifier = Modifier.padding(padding)
            )
            "notes" -> NotesScreen(
                notes = state.notes,
                onAddNote = { title, body -> viewModel.addNote(title, body) },
                onDeleteNote = { viewModel.deleteNote(it) },
                modifier = Modifier.padding(padding)
            )
            "reminders" -> RemindersScreen(
                reminders = state.reminders,
                onAddReminder = { text, mins -> viewModel.addReminder(text, mins) },
                onDeleteReminder = { viewModel.deleteReminder(it) },
                onOpenExactAlarmSettings = { viewModel.openExactAlarmSettings() },
                modifier = Modifier.padding(padding)
            )
            "history" -> HistoryScreen(
                history = state.history,
                onClear = { viewModel.clearHistory() },
                modifier = Modifier.padding(padding)
            )
            "settings" -> SettingsScreen(
                state = state,
                onSave = { baseUrl, model, apiKey, localOnly, voice, wake, calls, notifs, lang, name ->
                    viewModel.updateSettings(
                        baseUrl = baseUrl,
                        model = model,
                        apiKey = apiKey,
                        localOnly = localOnly,
                        voiceEnabled = voice,
                        wakeWordEnabled = wake,
                        announceCalls = calls,
                        announceNotifications = notifs,
                        speechLanguage = lang,
                        assistantName = name
                    )
                },
                onOpenPermissions = { showPermissions = true },
                modifier = Modifier.padding(padding)
            )
        }
    }
}
