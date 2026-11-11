package com.jigneshgaming.micai.data.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Local storage for MIC AI: notes, reminders and conversation history.
 * Implemented with [SQLiteOpenHelper] (no code generation) so the project
 * builds with zero annotation processors and works fully offline.
 */
class MicDatabase(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE notes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT NOT NULL, " +
                "body TEXT NOT NULL, " +
                "created_at INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE reminders (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "text TEXT NOT NULL, " +
                "trigger_at INTEGER NOT NULL, " +
                "done INTEGER NOT NULL DEFAULT 0)"
        )
        db.execSQL(
            "CREATE TABLE history (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "role TEXT NOT NULL, " +
                "text TEXT NOT NULL, " +
                "created_at INTEGER NOT NULL)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // v1 is the first shipped schema; future versions add migrations here.
        if (oldVersion < 1) onCreate(db)
    }

    // ---------------------------------------------------------------- notes

    fun insertNote(title: String, body: String): Long {
        val values = ContentValues().apply {
            put("title", title)
            put("body", body)
            put("created_at", System.currentTimeMillis())
        }
        return writableDatabase.insert("notes", null, values)
    }

    fun listNotes(): List<Note> {
        val out = mutableListOf<Note>()
        readableDatabase.query(
            "notes", null, null, null, null, null, "created_at DESC"
        ).use { c ->
            while (c.moveToNext()) {
                out += Note(
                    id = c.getLong(0), title = c.getString(1),
                    body = c.getString(2), createdAt = c.getLong(3)
                )
            }
        }
        return out
    }

    fun deleteNote(id: Long) {
        writableDatabase.delete("notes", "id=?", arrayOf(id.toString()))
    }

    // ------------------------------------------------------------- reminders

    fun insertReminder(text: String, triggerAt: Long): Long {
        val values = ContentValues().apply {
            put("text", text)
            put("trigger_at", triggerAt)
            put("done", 0)
        }
        return writableDatabase.insert("reminders", null, values)
    }

    fun listReminders(): List<Reminder> {
        val out = mutableListOf<Reminder>()
        readableDatabase.query(
            "reminders", null, null, null, null, null, "trigger_at ASC"
        ).use { c ->
            while (c.moveToNext()) {
                out += Reminder(
                    id = c.getLong(0), text = c.getString(1),
                    triggerAt = c.getLong(2), done = c.getInt(3) == 1
                )
            }
        }
        return out
    }

    fun markReminderDone(id: Long) {
        val values = ContentValues().apply { put("done", 1) }
        writableDatabase.update("reminders", values, "id=?", arrayOf(id.toString()))
    }

    fun deleteReminder(id: Long) {
        writableDatabase.delete("reminders", "id=?", arrayOf(id.toString()))
    }

    // --------------------------------------------------------------- history

    fun insertHistory(role: String, text: String) {
        val values = ContentValues().apply {
            put("role", role)
            put("text", text)
            put("created_at", System.currentTimeMillis())
        }
        writableDatabase.insert("history", null, values)
    }

    fun listHistory(limit: Int = 200): List<HistoryEntry> {
        val out = mutableListOf<HistoryEntry>()
        readableDatabase.query(
            "history", null, null, null, null, null, "id DESC", limit.toString()
        ).use { c ->
            while (c.moveToNext()) {
                out += HistoryEntry(
                    id = c.getLong(0), role = c.getString(1),
                    text = c.getString(2), createdAt = c.getLong(3)
                )
            }
        }
        return out.reversed()
    }

    fun clearHistory() {
        writableDatabase.delete("history", null, null)
    }

    companion object {
        private const val DB_NAME = "mic_ai.db"
        private const val DB_VERSION = 1
    }
}

data class Note(val id: Long, val title: String, val body: String, val createdAt: Long)

data class Reminder(val id: Long, val text: String, val triggerAt: Long, val done: Boolean)

data class HistoryEntry(val id: Long, val role: String, val text: String, val createdAt: Long)
