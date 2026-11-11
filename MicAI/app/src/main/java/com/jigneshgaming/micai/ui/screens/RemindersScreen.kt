package com.jigneshgaming.micai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jigneshgaming.micai.data.db.Reminder
import com.jigneshgaming.micai.ui.theme.JarvisOnSurfaceVariant
import com.jigneshgaming.micai.ui.theme.JarvisPrimary
import com.jigneshgaming.micai.ui.theme.JarvisSurfaceVariant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RemindersScreen(
    reminders: List<Reminder>,
    onAddReminder: (String, Long) -> Unit,
    onDeleteReminder: (Long) -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditor by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("10") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(onClick = { showEditor = !showEditor }) {
                Icon(Icons.Filled.Add, contentDescription = "New reminder")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            if (showEditor) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Remind me to…") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.size(8.dp))
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { minutes = it.filter(Char::isDigit) },
                    label = { Text("In minutes") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.size(8.dp))
                Button(
                    onClick = {
                        val mins = minutes.toLongOrNull() ?: 10L
                        onAddReminder(text, mins)
                        text = ""
                        showEditor = false
                    }
                ) { Text("Set reminder") }
                Spacer(Modifier.size(16.dp))
            }

            Text(
                text = "Reminders fire even when the app is closed. " +
                    "On Android 12+, allow exact alarms for on-time delivery.",
                style = MaterialTheme.typography.bodySmall,
                color = JarvisOnSurfaceVariant
            )
            Spacer(Modifier.size(8.dp))
            Button(onClick = onOpenExactAlarmSettings) {
                Text("Allow exact alarms")
            }
            Spacer(Modifier.size(16.dp))

            if (reminders.isEmpty() && !showEditor) {
                Text(
                    text = "No reminders yet. Tap + or say \"remind me in 10 minutes to call mom\".",
                    style = MaterialTheme.typography.bodyMedium,
                    color = JarvisOnSurfaceVariant
                )
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(reminders, key = { it.id }) { reminder ->
                    ReminderCard(reminder = reminder, onDelete = { onDeleteReminder(reminder.id) })
                }
            }
        }
    }
}

@Composable
private fun ReminderCard(reminder: Reminder, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (reminder.done) JarvisOnSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                        .format(Date(reminder.triggerAt)) +
                        if (reminder.done) " — done" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = JarvisPrimary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete reminder")
            }
        }
    }
}
