package com.jigneshgaming.micai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jigneshgaming.micai.UiState
import com.jigneshgaming.micai.ui.theme.JarvisOnSurfaceVariant
import com.jigneshgaming.micai.ui.theme.JarvisPrimary
import com.jigneshgaming.micai.ui.theme.JarvisSurfaceVariant

@Composable
fun SettingsScreen(
    state: UiState,
    onSave: (
        baseUrl: String,
        model: String,
        apiKey: String?,
        localOnly: Boolean,
        voiceEnabled: Boolean,
        wakeWordEnabled: Boolean,
        announceCalls: Boolean,
        announceNotifications: Boolean,
        speechLanguage: String,
        assistantName: String
    ) -> Unit,
    onOpenPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    var baseUrl by remember(state.baseUrl) { mutableStateOf(state.baseUrl) }
    var model by remember(state.model) { mutableStateOf(state.model) }
    var apiKey by remember { mutableStateOf("") }
    var localOnly by remember(state.localOnly) { mutableStateOf(state.localOnly) }
    var voiceEnabled by remember(state.voiceEnabled) { mutableStateOf(state.voiceEnabled) }
    var wakeWord by remember(state.wakeWordEnabled) { mutableStateOf(state.wakeWordEnabled) }
    var announceCalls by remember(state.announceCalls) { mutableStateOf(state.announceCalls) }
    var announceNotifs by remember(state.announceNotifications) {
        mutableStateOf(state.announceNotifications)
    }
    var speechLang by remember(state.speechLanguage) { mutableStateOf(state.speechLanguage) }
    var assistantName by remember(state.assistantName) { mutableStateOf(state.assistantName) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleLarge,
            color = JarvisPrimary
        )

        SettingsCard(title = "Online AI brain") {
            Text(
                text = "MIC AI talks to any OpenAI-compatible endpoint. " +
                    "Your key is encrypted on this device and never shipped in the APK. " +
                    "For extra security, point the endpoint at your own proxy (see MicAI/server).",
                style = MaterialTheme.typography.bodySmall,
                color = JarvisOnSurfaceVariant
            )
            Spacer(Modifier.size(8.dp))
            OutlinedTextField(
                value = baseUrl, onValueChange = { baseUrl = it },
                label = { Text("API base URL") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Spacer(Modifier.size(8.dp))
            OutlinedTextField(
                value = model, onValueChange = { model = it },
                label = { Text("Model") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Spacer(Modifier.size(8.dp))
            OutlinedTextField(
                value = apiKey, onValueChange = { apiKey = it },
                label = {
                    Text(if (state.hasApiKey) "API key (saved — enter to replace)" else "API key")
                },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            SettingRow(label = "Local-only mode (never use the internet)", checked = localOnly) {
                localOnly = it
            }
            Text(
                text = if (state.aiConfigured) "Status: online AI ready."
                else "Status: offline brain only. Add a key to unlock general knowledge, coding help and image understanding.",
                style = MaterialTheme.typography.bodySmall,
                color = JarvisOnSurfaceVariant
            )
        }

        SettingsCard(title = "Voice") {
            SettingRow(label = "Speak replies aloud", checked = voiceEnabled) { voiceEnabled = it }
            SettingRow(label = "\"Hey MIC\" wake word (foreground service)", checked = wakeWord) {
                wakeWord = it
            }
            OutlinedTextField(
                value = speechLang, onValueChange = { speechLang = it },
                label = { Text("Speech language (blank = auto, e.g. en-IN, hi-IN, gu-IN)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = assistantName, onValueChange = { assistantName = it },
                label = { Text("Assistant name") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
        }

        SettingsCard(title = "Phone assistant") {
            SettingRow(label = "Announce incoming callers", checked = announceCalls) {
                announceCalls = it
            }
            SettingRow(label = "Read notifications aloud on request", checked = announceNotifs) {
                announceNotifs = it
            }
            Text(
                text = "Caller announcements need Phone + Contacts permission. " +
                    "Notification reading needs Notification access (system Settings).",
                style = MaterialTheme.typography.bodySmall,
                color = JarvisOnSurfaceVariant
            )
        }

        Button(
            onClick = {
                onSave(
                    baseUrl, model,
                    if (apiKey.isNotBlank()) apiKey else null,
                    localOnly, voiceEnabled, wakeWord, announceCalls, announceNotifs,
                    speechLang, assistantName
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save settings") }

        Button(onClick = onOpenPermissions, modifier = Modifier.fillMaxWidth()) {
            Text("Permissions")
        }

        Text(
            text = "MIC AI 1.0.0 — by JIGNESH GAMING. " +
                "Offline features: voice commands, notes, reminders, caller names, " +
                "battery/storage, alarms, calculator. Online features: general knowledge, " +
                "coding, Minecraft/Discord/VPS help, image & document understanding, weather.",
            style = MaterialTheme.typography.bodySmall,
            color = JarvisOnSurfaceVariant
        )
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = JarvisPrimary
            )
            Spacer(Modifier.size(10.dp))
            content()
        }
    }
}

@Composable
private fun SettingRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
