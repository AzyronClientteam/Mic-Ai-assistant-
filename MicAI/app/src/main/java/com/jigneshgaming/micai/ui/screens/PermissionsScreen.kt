package com.jigneshgaming.micai.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.jigneshgaming.micai.phone.MicNotificationListener
import com.jigneshgaming.micai.ui.theme.JarvisOnSurfaceVariant
import com.jigneshgaming.micai.ui.theme.JarvisPrimary
import com.jigneshgaming.micai.ui.theme.JarvisSurfaceVariant

/**
 * Runtime-permission dashboard. Every permission MIC AI requests is
 * optional — the app degrades gracefully (e.g. no caller announcements
 * without Contacts permission) instead of crashing.
 */
@Composable
fun PermissionsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var refreshTick by remember { mutableStateOf(0) }
    // Read the tick so permission changes trigger recomposition.
    @Suppress("UNUSED_EXPRESSION")
    refreshTick

    fun granted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refreshTick++ }

    val micGranted = granted(Manifest.permission.RECORD_AUDIO)
    val phoneGranted = granted(Manifest.permission.READ_PHONE_STATE)
    val contactsGranted = granted(Manifest.permission.READ_CONTACTS)
    val notifGranted = granted(Manifest.permission.POST_NOTIFICATIONS)
    val notifListener = MicNotificationListener.isEnabled(context)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Permissions",
            style = MaterialTheme.typography.titleLarge,
            color = JarvisPrimary
        )
        Text(
            text = "All permissions are optional. MIC AI works without them — " +
                "features that need one simply stay off until you grant it.",
            style = MaterialTheme.typography.bodySmall,
            color = JarvisOnSurfaceVariant
        )

        PermissionCard(
            title = "Microphone",
            description = "Voice commands and the \"Hey MIC\" wake word.",
            granted = micGranted,
            onGrant = {
                permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
            }
        )
        PermissionCard(
            title = "Phone state",
            description = "Detect incoming calls so MIC can announce the caller.",
            granted = phoneGranted,
            onGrant = {
                permissionLauncher.launch(arrayOf(Manifest.permission.READ_PHONE_STATE))
            }
        )
        PermissionCard(
            title = "Contacts",
            description = "Look up caller names and search contacts by voice.",
            granted = contactsGranted,
            onGrant = {
                permissionLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS))
            }
        )
        PermissionCard(
            title = "Notifications",
            description = "Show reminder notifications.",
            granted = notifGranted,
            onGrant = {
                permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            }
        )
        PermissionCard(
            title = "Notification access",
            description = "Let MIC read and summarise your notifications on request.",
            granted = notifListener,
            onGrant = {
                context.startActivity(
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
        )
        PermissionCard(
            title = "Exact alarms",
            description = "On-time reminders on Android 12+. Without it, reminders may arrive a few minutes late.",
            granted = true,
            onGrant = {
                context.startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
        )

        Spacer(Modifier.size(4.dp))
        Text(
            text = "Tip: if \"Hey MIC\" stops working after a reboot, open MIC AI once — " +
                "the wake-word service restarts automatically.",
            style = MaterialTheme.typography.bodySmall,
            color = JarvisOnSurfaceVariant
        )
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    granted: Boolean,
    onGrant: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, color = JarvisPrimary)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = JarvisOnSurfaceVariant
                )
                Text(
                    text = if (granted) "Granted" else "Not granted",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (granted) com.jigneshgaming.micai.ui.theme.JarvisSuccess
                    else com.jigneshgaming.micai.ui.theme.JarvisError
                )
            }
            Button(onClick = onGrant) { Text("Grant") }
        }
    }
}
