package com.jigneshgaming.micai.ai

import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Offline brain: rule-based intent handling that works with no internet.
 * Anything that needs the cloud (general knowledge, coding help, image
 * understanding…) is answered by [AiClient] when online; this class covers
 * device commands, notes, reminders, time/date, battery, calculator, jokes
 * and small talk so MIC AI stays useful in airplane mode.
 *
 * The brain never touches Android APIs directly — it returns a [Reply] with
 * an optional [Action] that the ViewModel executes. That keeps it unit
 * testable and keeps side effects (opening apps, setting alarms) in one
 * permission-aware place.
 */
class LocalBrain {

    sealed interface Action {
        data class OpenApp(val name: String) : Action
        data class AddNote(val text: String) : Action
        data class SetReminder(val text: String, val inMinutes: Long) : Action
        data class SetAlarm(val hour: Int, val minute: Int, val label: String) : Action
        data class SearchContact(val name: String) : Action
        data class DraftMessage(val text: String) : Action
        data class Weather(val city: String?) : Action
    }

    data class Reply(val text: String, val action: Action? = null)

    fun handle(rawInput: String): Reply {
        val input = rawInput.trim().lowercase(Locale.ROOT)
        if (input.isBlank()) return Reply("I did not catch that. Please say it again.")

        // ---- identity / small talk -------------------------------------
        if (input.matches(Regex("^(hi|hello|hey|namaste|yo)\\b.*"))) {
            return Reply("Hello sir. MIC online and ready. How can I help you today?")
        }
        if (input.contains("who are you") || input.contains("your name")) {
            return Reply("I am MIC, your personal AI assistant, created by JIGNESH GAMING. Inspired by JARVIS, at your service.")
        }
        if (input.contains("thank")) return Reply("Always a pleasure, sir.")
        if (input.contains("how are you")) return Reply("All systems are running smoothly. Thank you for asking. What can I do for you?")
        if (input.contains("bye") || input.contains("good night") || input.contains("goodbye")) {
            return Reply("Goodbye sir. I will be here whenever you need me.")
        }
        if (input.contains("joke")) return Reply(JOKES.random())

        // ---- time & date ------------------------------------------------
        if (input.contains("time")) {
            val cal = Calendar.getInstance()
            val h = cal.get(Calendar.HOUR_OF_DAY)
            val m = cal.get(Calendar.MINUTE)
            val ampm = if (h < 12) "AM" else "PM"
            val h12 = when (val v = h % 12) { 0 -> 12; else -> v }
            return Reply("It is $h12:${m.toString().padStart(2, '0')} $ampm.")
        }
        if (input.contains("date") || input.contains("day is") || input.contains("today")) {
            val cal = Calendar.getInstance()
            val days = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
            val months = arrayOf(
                "January", "February", "March", "April", "May", "June", "July",
                "August", "September", "October", "November", "December"
            )
            return Reply(
                "Today is ${days[cal.get(Calendar.DAY_OF_WEEK) - 1]}, " +
                    "${cal.get(Calendar.DAY_OF_MONTH)} ${months[cal.get(Calendar.MONTH)]} " +
                    "${cal.get(Calendar.YEAR)}."
            )
        }

        // ---- device info -------------------------------------------------
        if (input.contains("battery")) {
            return Reply("Battery_ACTION") // replaced by ViewModel with live value
        }

        // ---- notes -------------------------------------------------------
        if (input.startsWith("note") || input.startsWith("take a note") ||
            input.startsWith("remember this") || input.startsWith("save a note")
        ) {
            val text = rawInput.substringAfter("note", "")
                .substringAfter("remember this", "")
                .trim().trimStart(':', '-').trim()
            if (text.isBlank()) return Reply("What should I write in the note, sir?")
            return Reply("Note saved: $text", Action.AddNote(text))
        }
        if (input.contains("my notes") || input.contains("read my notes")) {
            return Reply("NOTES_ACTION")
        }

        // ---- reminders ----------------------------------------------------
        if (input.contains("remind me")) {
            val minutes = parseInMinutes(input)
            val text = input.substringAfter("remind me").trim()
                .replace(Regex("in\\s+\\d+\\s+(minute|minutes|hour|hours|second|seconds)"), "")
                .trim().trimStart(':', '-').trim()
            val label = text.ifBlank { "Reminder" }
            return Reply(
                "Reminder set for $label in ${minutes} minute${if (minutes == 1L) "" else "s"}.",
                Action.SetReminder(label, minutes)
            )
        }

        // ---- alarms --------------------------------------------------------
        if (input.contains("alarm")) {
            val time = parseClockTime(input)
            if (time == null) return Reply("What time should I set the alarm for, sir? For example: set an alarm for 7 AM.")
            val (hour, minute) = time
            return Reply(
                "Alarm set for ${formatClock(hour, minute)}.",
                Action.SetAlarm(hour, minute, "MIC AI alarm")
            )
        }

        // ---- open app ------------------------------------------------------
        if (input.startsWith("open ") || input.startsWith("launch ")) {
            val name = rawInput.trim().substringAfter("open").substringAfter("launch").trim()
            if (name.isBlank()) return Reply("Which app should I open, sir?")
            return Reply("Opening $name.", Action.OpenApp(name))
        }

        // ---- contacts ------------------------------------------------------
        if (input.contains("contact") || input.startsWith("call ") || input.startsWith("who is")) {
            val name = input.substringAfter("contact").substringAfter("call").substringAfter("who is")
                .trim().trimStart(':', '-').trim()
            if (name.isBlank()) return Reply("Which contact, sir?")
            return Reply("Looking up $name.", Action.SearchContact(name))
        }

        // ---- weather -------------------------------------------------------
        if (input.contains("weather")) {
            val city = input.substringAfter("weather").substringAfter("in").trim()
                .takeIf { it.isNotBlank() && it.length < 40 }
            return Reply("WEATHER_ACTION", Action.Weather(city))
        }

        // ---- calculator ----------------------------------------------------
        if (input.startsWith("calculate ") || input.startsWith("what is ") && input.any { it in "0123456789" }) {
            val expr = input.substringAfter("calculate").substringAfter("what is").trim()
            val value = evaluate(expr)
            if (value != null) return Reply("That equals ${formatNumber(value)}.")
        }

        // ---- draft message -------------------------------------------------
        if (input.startsWith("message ") || input.startsWith("send a message")) {
            val text = rawInput.substringAfter("message").trim()
            if (text.isBlank()) return Reply("What message should I draft, sir?")
            return Reply("Draft ready. Review it before sending.", Action.DraftMessage(text))
        }

        // ---- capabilities --------------------------------------------------
        if (input.contains("what can you do") || input.contains("help")) {
            return Reply(
                "I can open apps, set alarms and reminders, take notes, check battery, " +
                    "look up contacts, read notifications, check the weather, do maths, " +
                    "and — when online — answer questions, help with coding, Minecraft, " +
                    "Discord bots and servers. Try: 'open YouTube', 'remind me in 10 minutes to call mom', " +
                    "'note buy milk', or 'what is the weather in Rajkot'."
            )
        }

        return Reply(
            "I can handle that best with my online brain. Connect to the internet and " +
                "add an API key in Settings, or ask me something offline like the time, " +
                "battery, a reminder, or a note."
        )
    }

    // ------------------------------------------------------------- helpers

    private fun parseInMinutes(input: String): Long {
        Regex("(\\d+)\\s*(hour|hours|hr|hrs)").find(input)?.let {
            return it.groupValues[1].toLong() * 60
        }
        Regex("(\\d+)\\s*(minute|minutes|min|mins)").find(input)?.let {
            return it.groupValues[1].toLong()
        }
        Regex("(\\d+)\\s*(second|seconds|sec|secs)").find(input)?.let {
            return maxOf(1L, it.groupValues[1].toLong() / 60)
        }
        return 5L // sensible default: "remind me to X" → 5 minutes
    }

    /** Parses "7 am", "7:30 pm", "19:00" → (hour24, minute). */
    private fun parseClockTime(input: String): Pair<Int, Int>? {
        val match = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?").find(input) ?: return null
        var hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].toIntOrNull() ?: 0
        when (match.groupValues[3]) {
            "pm" -> if (hour < 12) hour += 12
            "am" -> if (hour == 12) hour = 0
        }
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour to minute
    }

    private fun formatClock(hour: Int, minute: Int): String {
        val ampm = if (hour < 12) "AM" else "PM"
        val h12 = when (val v = hour % 12) { 0 -> 12; else -> v }
        return "$h12:${minute.toString().padStart(2, '0')} $ampm"
    }

    /**
     * Tiny safe arithmetic evaluator (+ - * / % and parentheses).
     * No reflection, no scripting engine — just a recursive-descent parser.
     */
    private fun evaluate(expr: String): Double? {
        val cleaned = expr.replace(" ", "").replace("x", "*").replace("÷", "/")
        if (cleaned.isEmpty() || cleaned.any { it !in "0123456789.+-*/%()" }) return null
        return try {
            val parser = ExprParser(cleaned)
            val value = parser.parseExpression()
            if (parser.hasMore()) null else value
        } catch (_: Exception) {
            null
        }
    }

    private fun formatNumber(value: Double): String =
        if (value == value.roundToInt().toDouble()) value.roundToInt().toString()
        else String.format(Locale.US, "%.4f", value).trimEnd('0').trimEnd('.')

    private class ExprParser(private val s: String) {
        private var pos = 0

        fun parseExpression(): Double {
            var value = parseTerm()
            while (pos < s.length && (s[pos] == '+' || s[pos] == '-')) {
                val op = s[pos++]
                val rhs = parseTerm()
                value = if (op == '+') value + rhs else value - rhs
            }
            return value
        }

        private fun parseTerm(): Double {
            var value = parseFactor()
            while (pos < s.length && (s[pos] == '*' || s[pos] == '/' || s[pos] == '%')) {
                val op = s[pos++]
                val rhs = parseFactor()
                value = when (op) {
                    '*' -> value * rhs
                    '/' -> value / rhs
                    else -> value % rhs
                }
            }
            return value
        }

        private fun parseFactor(): Double {
            if (pos < s.length && s[pos] == '(') {
                pos++
                val value = parseExpression()
                require(pos < s.length && s[pos] == ')') { "missing )" }
                pos++
                return value
            }
            if (pos < s.length && s[pos] == '-') {
                pos++
                return -parseFactor()
            }
            val start = pos
            while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
            require(pos > start) { "expected number" }
            return s.substring(start, pos).toDouble()
        }

        fun hasMore(): Boolean = pos < s.length
    }

    companion object {
        private val JOKES = listOf(
            "Why did the robot go on a diet? Too many bytes.",
            "I would tell you a UDP joke, but you might not get it — and I would not know.",
            "There are only 10 types of people: those who understand binary and those who do not.",
            "Why do programmers prefer dark mode? Because light attracts bugs."
        )
    }
}
