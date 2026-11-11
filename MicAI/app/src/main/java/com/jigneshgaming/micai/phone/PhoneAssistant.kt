package com.jigneshgaming.micai.phone

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import com.jigneshgaming.micai.service.ReminderReceiver
import java.io.File
import java.util.Calendar

/**
 * Phone operations MIC AI can perform with public Android APIs:
 * contacts lookup, opening installed apps, battery/storage info, alarms,
 * reminders (via [AlarmManager]) and message/email drafts (via intents —
 * the user always reviews before anything is sent).
 */
class PhoneAssistant(private val context: Context) {

    private val appContext = context.applicationContext

    // --------------------------------------------------------------- contacts

    /** Returns the saved display name for [number], or null if unknown. */
    fun contactNameFor(number: String): String? {
        if (number.isBlank()) return null
        val digits = number.filter(Char::isDigit)
        if (digits.isEmpty()) return null
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number)
        )
        return try {
            appContext.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (_: SecurityException) {
            null
        }
    }

    /** Searches contacts by name; returns "Name — number" lines. */
    fun searchContacts(query: String): List<String> {
        if (query.isBlank()) return emptyList()
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(query)
        )
        val results = mutableListOf<String>()
        try {
            appContext.contentResolver.query(
                uri,
                arrayOf(
                    ContactsContract.PhoneLookup.DISPLAY_NAME,
                    ContactsContract.PhoneLookup.NUMBER
                ),
                null, null, null
            )?.use { cursor ->
                while (cursor.moveToNext() && results.size < 5) {
                    val name = cursor.getString(0) ?: continue
                    val number = cursor.getString(1) ?: ""
                    results += if (number.isNotBlank()) "$name — $number" else name
                }
            }
        } catch (_: SecurityException) {
            return emptyList()
        }
        return results
    }

    // ------------------------------------------------------------------ apps

    /** All launchable apps, sorted by label. */
    fun launchableApps(): List<Pair<String, String>> {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val pm = appContext.packageManager
        return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .map { it.activityInfo.packageName to pm.getApplicationLabel(it.applicationInfo).toString() }
            .sortedBy { it.second.lowercase() }
    }

    /** Opens the first installed app whose label contains [name]. */
    fun openAppByName(name: String): String? {
        val needle = name.lowercase().trim()
        if (needle.isBlank()) return null
        val match = launchableApps().firstOrNull { it.second.lowercase().contains(needle) }
            ?: return null
        val launch = appContext.packageManager.getLaunchIntentForPackage(match.first)
            ?: return null
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(launch)
        return match.second
    }

    // ------------------------------------------------------------ device info

    fun batteryPercent(): Int {
        val manager = appContext.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            ?: return -1
        return manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    fun batteryStatus(): String {
        val percent = batteryPercent()
        val charging = try {
            val sticky = appContext.registerReceiver(
                null,
                android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            val status = sticky?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        } catch (_: Exception) {
            false
        }
        val base = if (percent >= 0) "Battery is at $percent percent" else "Battery level unknown"
        return if (charging) "$base and charging." else "$base."
    }

    fun storageInfo(): String {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val totalBytes = stat.totalBytes
        val freeBytes = stat.freeBytes
        val totalGb = totalBytes / 1_000_000_000.0
        val freeGb = freeBytes / 1_000_000_000.0
        return String.format(
            java.util.Locale.US,
            "Storage: %.1f GB free out of %.1f GB total.", freeGb, totalGb
        )
    }

    // ----------------------------------------------------------------- alarms

    /** Sets a clock alarm via the system clock app (user can edit/cancel). */
    fun setAlarm(hour: Int, minute: Int, label: String): Boolean {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            appContext.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    // -------------------------------------------------------------- reminders

    /**
     * Schedules an exact reminder with [AlarmManager]. Falls back to an
     * inexact alarm when the exact-alarm permission is not granted
     * (Android 12+), so reminders still fire — possibly a few minutes late.
     */
    fun scheduleReminder(reminderId: Long, text: String, triggerAtMillis: Long) {
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return
        val intent = Intent(appContext, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_ID, reminderId)
            putExtra(ReminderReceiver.EXTRA_TEXT, text)
        }
        val pending = PendingIntent.getBroadcast(
            appContext,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, triggerAtMillis, pending
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, triggerAtMillis, pending
            )
        }
    }

    fun cancelReminder(reminderId: Long) {
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return
        val intent = Intent(appContext, ReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            appContext,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
        pending.cancel()
    }

    /** Opens the system screen where exact-alarm permission is granted. */
    fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:${appContext.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            appContext.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    // ---------------------------------------------------------------- drafts

    /** Opens the SMS app with [text] pre-filled (never auto-sends). */
    fun draftSms(text: String): Boolean {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:")
            putExtra("sms_body", text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            appContext.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    /** Opens the mail app with [text] as the body (never auto-sends). */
    fun draftEmail(text: String): Boolean {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            appContext.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    // ----------------------------------------------------------------- files

    /** Lists recent images so the user can ask MIC to look at one. */
    fun recentImages(limit: Int = 10): List<File> {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        val files = mutableListOf<File>()
        try {
            appContext.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, sortOrder
            )?.use { cursor ->
                while (cursor.moveToNext() && files.size < limit) {
                    val path = cursor.getString(0) ?: continue
                    val file = File(path)
                    if (file.exists()) files += file
                }
            }
        } catch (_: Exception) {
        }
        return files
    }

    fun nowMillis(): Long = System.currentTimeMillis()

    fun calendar(): Calendar = Calendar.getInstance()
}
