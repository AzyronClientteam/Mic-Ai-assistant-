package com.jigneshgaming.micai.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jigneshgaming.micai.ChatMessage
import com.jigneshgaming.micai.ui.theme.JarvisOnSurfaceVariant
import com.jigneshgaming.micai.ui.theme.JarvisPrimary
import com.jigneshgaming.micai.ui.theme.JarvisSurfaceVariant
import java.io.File

@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    onSend: (String, File?) -> Unit,
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    var pendingImage by remember { mutableStateOf<File?>(null) }
    val listState = rememberLazyListState()

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            // Copy to a private cache file so the ViewModel can read it
            // after the picker permission is gone.
            pendingImage = uri.let { copied ->
                try {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val file = File(context.cacheDir, "mic_image_${System.currentTimeMillis()}.jpg")
                    context.contentResolver.openInputStream(copied)?.use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    }
                    file
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(12.dp)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.role + it.at + it.text.hashCode() }) { message ->
                MessageBubble(message)
            }
        }

        if (pendingImage != null) {
            Text(
                text = "Image attached: ${pendingImage?.name}. Ask MIC about it.",
                style = MaterialTheme.typography.bodySmall,
                color = JarvisPrimary
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { imagePicker.launch("image/*") }) {
                Icon(Icons.Filled.AttachFile, contentDescription = "Attach image")
            }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Ask MIC anything…") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                singleLine = false,
                maxLines = 4
            )
            IconButton(onClick = onMicClick) {
                Icon(Icons.Filled.Mic, contentDescription = "Voice input")
            }
            IconButton(
                onClick = {
                    if (text.isNotBlank() || pendingImage != null) {
                        onSend(text, pendingImage)
                        text = ""
                        pendingImage = null
                    }
                }
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Send")
            }
        }
        Spacer(Modifier.size(4.dp))
        Text(
            text = "Images and documents need the online AI (Settings → API key).",
            style = MaterialTheme.typography.labelSmall,
            color = JarvisOnSurfaceVariant
        )
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) JarvisPrimary.copy(alpha = 0.18f)
                else JarvisSurfaceVariant
            ),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
