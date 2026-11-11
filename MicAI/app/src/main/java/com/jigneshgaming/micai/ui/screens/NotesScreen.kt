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
import com.jigneshgaming.micai.data.db.Note
import com.jigneshgaming.micai.ui.theme.JarvisOnSurfaceVariant
import com.jigneshgaming.micai.ui.theme.JarvisPrimary
import com.jigneshgaming.micai.ui.theme.JarvisSurfaceVariant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotesScreen(
    notes: List<Note>,
    onAddNote: (String, String) -> Unit,
    onDeleteNote: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditor by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(onClick = { showEditor = !showEditor }) {
                Icon(Icons.Filled.Add, contentDescription = "New note")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            if (showEditor) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.size(8.dp))
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Spacer(Modifier.size(8.dp))
                Button(
                    onClick = {
                        onAddNote(title, body)
                        title = ""
                        body = ""
                        showEditor = false
                    }
                ) { Text("Save note") }
                Spacer(Modifier.size(16.dp))
            }

            if (notes.isEmpty() && !showEditor) {
                Text(
                    text = "No notes yet. Tap + or say \"note buy milk\".",
                    style = MaterialTheme.typography.bodyMedium,
                    color = JarvisOnSurfaceVariant
                )
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(notes, key = { it.id }) { note ->
                    NoteCard(note = note, onDelete = { onDeleteNote(note.id) })
                }
            }
        }
    }
}

@Composable
private fun NoteCard(note: Note, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = JarvisPrimary
                )
                Text(
                    text = note.body,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                        .format(Date(note.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = JarvisOnSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete note")
            }
        }
    }
}
