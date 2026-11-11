package com.jigneshgaming.micai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jigneshgaming.micai.AssistantState
import com.jigneshgaming.micai.UiState
import com.jigneshgaming.micai.ui.components.AssistantOrb
import com.jigneshgaming.micai.ui.components.VoiceWaveform
import com.jigneshgaming.micai.ui.theme.JarvisOnSurfaceVariant
import com.jigneshgaming.micai.ui.theme.JarvisPrimary

@Composable
fun HomeScreen(
    state: UiState,
    onMicClick: () -> Unit,
    onStopClick: () -> Unit,
    onQuickCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status row: clock, battery, connectivity
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = state.clockText.ifBlank { "--:--" },
                style = MaterialTheme.typography.titleMedium,
                color = JarvisPrimary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.BatteryStd,
                    contentDescription = "Battery",
                    tint = JarvisOnSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (state.batteryPercent >= 0) "${state.batteryPercent}%" else "--",
                    style = MaterialTheme.typography.bodySmall,
                    color = JarvisOnSurfaceVariant
                )
                Spacer(Modifier.size(12.dp))
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            if (state.online) "ONLINE" else "OFFLINE",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (state.online) JarvisPrimary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "MIC AI",
            style = MaterialTheme.typography.headlineMedium,
            color = JarvisPrimary
        )
        Text(
            text = "by JIGNESH GAMING",
            style = MaterialTheme.typography.labelMedium,
            color = JarvisOnSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))
        AssistantOrb(state = state.state)
        Spacer(Modifier.height(12.dp))
        Text(
            text = when (state.state) {
                AssistantState.IDLE -> "Tap the mic or say \"Hey MIC\""
                AssistantState.LISTENING -> "Listening…"
                AssistantState.THINKING -> "Thinking…"
                AssistantState.SPEAKING -> "Speaking…"
            },
            style = MaterialTheme.typography.titleSmall,
            color = JarvisPrimary
        )
        Spacer(Modifier.height(8.dp))
        VoiceWaveform(state = state.state, modifier = Modifier.fillMaxWidth().height(48.dp))

        Spacer(Modifier.height(20.dp))
        if (state.state == AssistantState.SPEAKING) {
            Button(
                onClick = onStopClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Filled.Stop, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Stop")
            }
        } else {
            Button(
                onClick = onMicClick,
                modifier = Modifier.size(76.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = JarvisPrimary)
            ) {
                Icon(
                    Icons.Filled.Mic,
                    contentDescription = "Speak",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        if (state.lastReply.isNotBlank()) {
            Spacer(Modifier.height(20.dp))
            Text(
                text = state.lastReply,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = "Try saying",
            style = MaterialTheme.typography.labelMedium,
            color = JarvisOnSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        val suggestions = listOf(
            "What time is it?",
            "Open YouTube",
            "Note buy milk",
            "Remind me in 10 minutes to call mom",
            "What is the weather in Rajkot?",
            "What is my battery?"
        )
        suggestions.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                row.forEach { suggestion ->
                    AssistChip(
                        onClick = { onQuickCommand(suggestion) },
                        label = { Text(suggestion) },
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        if (!state.online) {
            Text(
                text = "Offline mode: device commands, notes and reminders work. " +
                    "General knowledge, coding help and image understanding need internet + an API key (Settings).",
                style = MaterialTheme.typography.bodySmall,
                color = JarvisOnSurfaceVariant
            )
        }
    }
}
