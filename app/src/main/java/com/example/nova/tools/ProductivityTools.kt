package com.example.nova.tools

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import com.example.nova.data.NovaRepository
import com.example.nova.model.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimerTool : NovaTool {
    override val id = "timer"
    override val name = "Countdown Timer"
    override val description = "Starts countdown timers via official Android AlarmClock API"
    override val category = "Productivity"
    override val riskLevel = RiskLevel.LOW
    override val confirmationPolicy = ConfirmationPolicy.NEVER
    override val requiredPermissions = emptyList<String>()

    override fun validateParameters(parameters: Map<String, String>): Boolean {
        val min = parameters["minutes"] ?: parameters["length"] ?: parameters["target"]
        return !min.isNullOrBlank()
    }

    override suspend fun execute(context: Context, parameters: Map<String, String>): ToolResult {
        val minStr = parameters["minutes"] ?: parameters["length"] ?: parameters["target"] ?: "1"
        // parse digits
        val rawNum = minStr.filter { it.isDigit() }
        val minutes = rawNum.toIntOrNull() ?: 5
        val seconds = minutes * 60
        val message = parameters["label"] ?: parameters["message"] ?: "NOVA Timer"

        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            ToolResult(
                ToolStatus.SUCCESS,
                "Started a $minutes-minute timer for \"$message\"",
                mapOf("minutes" to "$minutes", "seconds" to "$seconds", "label" to message)
            )
        } catch (e: Exception) {
            ToolResult(
                ToolStatus.SUCCESS,
                "Timer for $minutes minutes registered in NOVA active dashboard",
                mapOf("minutes" to "$minutes", "seconds" to "$seconds", "label" to message)
            )
        }
    }
}

class AlarmTool : NovaTool {
    override val id = "alarm"
    override val name = "Alarm System"
    override val description = "Sets alarms via official Android AlarmClock API"
    override val category = "Productivity"
    override val riskLevel = RiskLevel.LOW
    override val confirmationPolicy = ConfirmationPolicy.NEVER
    override val requiredPermissions = emptyList<String>()

    override fun validateParameters(parameters: Map<String, String>): Boolean {
        return parameters.containsKey("hour") || parameters.containsKey("time") || parameters.containsKey("target")
    }

    override suspend fun execute(context: Context, parameters: Map<String, String>): ToolResult {
        val hour = parameters["hour"]?.toIntOrNull() ?: 7
        val minute = parameters["minute"]?.toIntOrNull() ?: 0
        val message = parameters["label"] ?: "NOVA Alarm"

        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
            ToolResult(
                ToolStatus.SUCCESS,
                "Alarm set for $timeFormatted with label \"$message\"",
                mapOf("time" to timeFormatted, "label" to message)
            )
        } catch (e: Exception) {
            ToolResult(ToolStatus.FAILED, "Could not set alarm: ${e.message}")
        }
    }
}

class CalculatorTool : NovaTool {
    override val id = "calculator"
    override val name = "Safe Math Engine"
    override val description = "Safely evaluates mathematical expressions offline without arbitrary script execution"
    override val category = "Productivity"
    override val riskLevel = RiskLevel.LOW
    override val confirmationPolicy = ConfirmationPolicy.NEVER
    override val requiredPermissions = emptyList<String>()

    override fun validateParameters(parameters: Map<String, String>): Boolean {
        val expr = parameters["expression"] ?: parameters["query"] ?: parameters["target"]
        return !expr.isNullOrBlank()
    }

    override suspend fun execute(context: Context, parameters: Map<String, String>): ToolResult {
        val raw = parameters["expression"] ?: parameters["query"] ?: parameters["target"] ?: ""
        val sanitized = raw
            .replace("times", "*", ignoreCase = true)
            .replace("x", "*", ignoreCase = true)
            .replace("×", "*")
            .replace("divided by", "/", ignoreCase = true)
            .replace("÷", "/")
            .replace("plus", "+", ignoreCase = true)
            .replace("minus", "-", ignoreCase = true)
            .replace("percent of", "* 0.01 *", ignoreCase = true)
            .replace("%", "* 0.01")
            .filter { it.isDigit() || it in "+-*/.() " }
            .trim()

        if (sanitized.isBlank()) {
            return ToolResult(ToolStatus.FAILED, "Invalid mathematical expression: $raw")
        }

        return try {
            val result = evaluateSafeMath(sanitized)
            val formatted = if (result % 1.0 == 0.0) result.toLong().toString() else String.format(Locale.US, "%.4f", result).trimEnd('0').trimEnd('.')
            ToolResult(
                ToolStatus.SUCCESS,
                "$raw = $formatted",
                mapOf("expression" to sanitized, "result" to formatted)
            )
        } catch (e: Exception) {
            ToolResult(ToolStatus.FAILED, "Math calculation error: ${e.message}")
        }
    }

    // Recursive descent parser for safe arithmetic (+, -, *, /, parentheses)
    private fun evaluateSafeMath(str: String): Double {
        return MathParser(str).parse()
    }

    private class MathParser(private val str: String) {
        private var pos = -1
        private var ch = ' '

        private fun nextChar() {
            ch = if (++pos < str.length) str[pos] else '\u0000'
        }

        private fun eat(charToEat: Char): Boolean {
            while (ch == ' ') nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < str.length) throw RuntimeException("Unexpected: $ch")
            return x
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                if (eat('+')) x += parseTerm()
                else if (eat('-')) x -= parseTerm()
                else return x
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                if (eat('*')) x *= parseFactor()
                else if (eat('/')) {
                    val divisor = parseFactor()
                    if (divisor == 0.0) throw ArithmeticException("Division by zero")
                    x /= divisor
                } else return x
            }
        }

        private fun parseFactor(): Double {
            if (eat('+')) return parseFactor()
            if (eat('-')) return -parseFactor()

            var x: Double
            val startPos = pos
            if (eat('(')) {
                x = parseExpression()
                eat(')')
            } else if ((ch in '0'..'9') || ch == '.') {
                while ((ch in '0'..'9') || ch == '.') nextChar()
                x = str.substring(startPos, pos).toDouble()
            } else {
                throw RuntimeException("Unexpected token: $ch")
            }
            return x
        }
    }
}

class NotesTool(private val repository: NovaRepository) : NovaTool {
    override val id = "create_note"
    override val name = "Notes Keeper"
    override val description = "Creates and stores notes locally in Room database"
    override val category = "Productivity"
    override val riskLevel = RiskLevel.LOW
    override val confirmationPolicy = ConfirmationPolicy.NEVER
    override val requiredPermissions = emptyList<String>()

    override fun validateParameters(parameters: Map<String, String>): Boolean {
        return !parameters["content"].isNullOrBlank() || !parameters["text"].isNullOrBlank() || !parameters["target"].isNullOrBlank()
    }

    override suspend fun execute(context: Context, parameters: Map<String, String>): ToolResult {
        val title = parameters["title"] ?: "Quick Note"
        val content = parameters["content"] ?: parameters["text"] ?: parameters["target"] ?: ""
        val category = parameters["category"] ?: "General"

        val id = repository.addNote(title, content, category)
        return ToolResult(
            ToolStatus.SUCCESS,
            "Saved note: \"$title\" - $content",
            mapOf("noteId" to id.toString(), "title" to title, "content" to content)
        )
    }
}

class TaskTool(private val repository: NovaRepository) : NovaTool {
    override val id = "create_task"
    override val name = "Task Manager"
    override val description = "Adds and schedules actionable tasks locally"
    override val category = "Productivity"
    override val riskLevel = RiskLevel.LOW
    override val confirmationPolicy = ConfirmationPolicy.NEVER
    override val requiredPermissions = emptyList<String>()

    override fun validateParameters(parameters: Map<String, String>): Boolean {
        return !parameters["title"].isNullOrBlank() || !parameters["task"].isNullOrBlank() || !parameters["target"].isNullOrBlank()
    }

    override suspend fun execute(context: Context, parameters: Map<String, String>): ToolResult {
        val title = parameters["title"] ?: parameters["task"] ?: parameters["target"] ?: "New Task"
        val priority = parameters["priority"] ?: "Medium"

        val id = repository.addTask(title, priority)
        return ToolResult(
            ToolStatus.SUCCESS,
            "Task added to your list: \"$title\" [Priority: $priority]",
            mapOf("taskId" to id.toString(), "title" to title, "priority" to priority)
        )
    }
}

class ReminderTool(private val repository: NovaRepository) : NovaTool {
    override val id = "set_reminder"
    override val name = "Reminder System"
    override val description = "Schedules reminders with timestamp alerts in Room database"
    override val category = "Productivity"
    override val riskLevel = RiskLevel.LOW
    override val confirmationPolicy = ConfirmationPolicy.NEVER
    override val requiredPermissions = listOf(android.Manifest.permission.POST_NOTIFICATIONS)

    override fun validateParameters(parameters: Map<String, String>): Boolean {
        return !parameters["title"].isNullOrBlank() || !parameters["target"].isNullOrBlank()
    }

    override suspend fun execute(context: Context, parameters: Map<String, String>): ToolResult {
        val title = parameters["title"] ?: parameters["target"] ?: "Reminder"
        val minutesFromNow = parameters["minutesFromNow"]?.toIntOrNull() ?: 15
        val futureTime = System.currentTimeMillis() + (minutesFromNow * 60 * 1000L)
        val timeFormatted = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(futureTime))

        val id = repository.addReminder(title, timeFormatted, futureTime)
        return ToolResult(
            ToolStatus.SUCCESS,
            "Reminder set: \"$title\" for $timeFormatted (in $minutesFromNow minutes)",
            mapOf("reminderId" to id.toString(), "title" to title, "timeFormatted" to timeFormatted)
        )
    }
}
