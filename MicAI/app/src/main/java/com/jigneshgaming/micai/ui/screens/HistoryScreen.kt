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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jigneshgaming.micai.data.db.HistoryEntry
import com.jigneshgaming.micai.ui.theme.JarvisOnSurfaceVariant
import com.jigneshgaming.micai.ui.theme.JarvisPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    history: List<HistoryEntry>,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Command history",
                style = MaterialTheme.typography.titleMedium,
                color = JarvisPrimary
            )
            Button(onClick = onClear) { Text("Clear") }
        }
        Spacer(Modifier.size(12.dp))

        if (history.isEmpty()) {
            Text(
                text = "No history yet. Everything you ask MIC is saved here on-device.",
                style = MaterialTheme.typography.bodyMedium,
                color = JarvisOnSurfaceVariant
            )
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(history.reversed(), key = { it.id }) { entry ->
                Column {
                    Text(
                        text = (if (entry.role == "user") "You: " else "MIC: ") + entry.text,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                            .format(Date(entry.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = JarvisOnSurfaceVariant
                    )
                }
            }
        }
    }
}
