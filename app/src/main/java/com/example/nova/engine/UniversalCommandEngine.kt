package com.example.nova.engine

import android.content.Context
import com.example.nova.data.GeminiAiService
import com.example.nova.data.NovaRepository
import com.example.nova.model.*
import com.example.nova.tools.NovaTool
import com.example.nova.tools.ToolRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class UniversalCommandEngine(
    private val context: Context,
    private val registry: ToolRegistry,
    private val securityValidator: CommandSecurityValidator,
    private val aiService: GeminiAiService,
    private val repository: NovaRepository
) {

    /**
     * Full execution pipeline:
     * User Input -> Language Detection -> Intent Parsing -> Command Planning ->
     * Security Validation -> Risk Check -> Confirmation (if high-risk) -> Execution -> Verification
     */
    suspend fun processInput(
        userInput: String,
        onConfirmationNeeded: (PendingConfirmation) -> Unit
    ): ExecutionSummary = withContext(Dispatchers.Default) {
        val trimmedInput = userInput.trim()
        val language = detectLanguage(trimmedInput)

        // 1. Build Command Plan (Online AI or Offline Rule Parser)
        val plan = planCommands(trimmedInput, language)

        // 2. Validate all commands through Security Firewall first
        for (cmd in plan.commands) {
            val validation = securityValidator.validateCommand(cmd)
            if (validation is SecurityValidationResult.Rejected) {
                return@withContext ExecutionSummary(
                    success = false,
                    responseMessage = "⚠️ Security Violation: ${validation.reason}",
                    results = listOf(ToolResult(ToolStatus.DENIED, validation.reason))
                )
            }
        }

        // 3. Check for High-Risk actions requiring confirmation
        val pendingAction = plan.commands.firstOrNull { it.requiresConfirmation }
        if (pendingAction != null) {
            val confirmation = PendingConfirmation(
                actionName = pendingAction.action,
                title = "Confirmation Required: ${pendingAction.action.replace('_', ' ').uppercase()}",
                description = pendingAction.confirmationMessage ?: "Do you want to proceed with this action?",
                riskLevel = pendingAction.riskLevel,
                command = pendingAction
            )
            onConfirmationNeeded(confirmation)
            return@withContext ExecutionSummary(
                success = true,
                responseMessage = "I've prepared ${pendingAction.action.replace('_', ' ')}. Please confirm before I proceed.",
                pendingConfirmation = confirmation
            )
        }

        // 4. Sequential execution of all commands in plan
        val results = mutableListOf<ToolResult>()
        val messages = mutableListOf<String>()

        for ((index, cmd) in plan.commands.withIndex()) {
            val tool = registry.getTool(cmd.action)
            if (tool == null) {
                results.add(ToolResult(ToolStatus.NOT_SUPPORTED, "Tool ${cmd.action} is unavailable."))
                break
            }

            val startTime = System.currentTimeMillis()
            val result = try {
                tool.execute(context, cmd.parameters)
            } catch (e: Exception) {
                ToolResult(ToolStatus.FAILED, "Tool error: ${e.message}")
            }
            val elapsed = System.currentTimeMillis() - startTime

            // Verify result
            val isVerified = tool.verifyResult(result)
            val finalResult = if (!isVerified && result.status == ToolStatus.SUCCESS) {
                result.copy(status = ToolStatus.FAILED, message = "Could not verify execution of ${tool.name}")
            } else {
                result.copy(executionTimeMs = elapsed)
            }

            results.add(finalResult)
            messages.add(finalResult.message)

            // Log to local Room audit log
            repository.logToolExecution(
                toolName = tool.name,
                action = cmd.action,
                status = finalResult.status.name,
                message = finalResult.message,
                timeMs = elapsed
            )

            // Multi-action safety: stop sequential execution if a prerequisite action fails
            if (finalResult.status != ToolStatus.SUCCESS) {
                if (index < plan.commands.size - 1) {
                    messages.add("Remaining ${plan.commands.size - 1 - index} action(s) halted because previous action failed.")
                }
                break
            }
        }

        val finalMessage = if (messages.isNotEmpty()) messages.joinToString("\n\n") else "Action completed."
        ExecutionSummary(
            success = results.all { it.status == ToolStatus.SUCCESS },
            responseMessage = finalMessage,
            results = results
        )
    }

    /**
     * Executes a confirmed action
     */
    suspend fun executeConfirmed(command: StructuredCommand): ToolResult = withContext(Dispatchers.Default) {
        val tool = registry.getTool(command.action)
            ?: return@withContext ToolResult(ToolStatus.NOT_SUPPORTED, "Tool ${command.action} not found")

        val start = System.currentTimeMillis()
        val result = try {
            tool.execute(context, command.parameters)
        } catch (e: Exception) {
            ToolResult(ToolStatus.FAILED, "Execution error: ${e.message}")
        }
        val elapsed = System.currentTimeMillis() - start

        repository.logToolExecution(
            toolName = tool.name,
            action = command.action,
            status = result.status.name,
            message = result.message,
            timeMs = elapsed
        )

        result.copy(executionTimeMs = elapsed)
    }

    private fun detectLanguage(text: String): String {
        // Bengali unicode block: \u0980-\u09FF
        if (text.any { it in '\u0980'..'\u09FF' }) return "Bengali"
        val lower = text.lowercase()
        if (lower.contains("kamusta") || lower.contains("salamat") || lower.contains("magandang")) return "Tagalog"
        if (lower.contains("selamat") || lower.contains("apa kabar") || lower.contains("terima kasih")) return "Indonesian"
        if (text.any { it in '\u0600'..'\u06FF' }) return "Arabic"
        return "English"
    }

    private suspend fun planCommands(input: String, language: String): CommandPlan {
        // Try Online AI Plan if available
        if (aiService.isConfigured()) {
            val aiParsed = aiService.parseIntentWithAi(input)
            if (aiParsed.isSuccess) {
                val planFromAi = parseAiJsonPlan(input, language, aiParsed.getOrNull() ?: "")
                if (planFromAi != null && planFromAi.commands.isNotEmpty()) {
                    return planFromAi
                }
            }
        }

        // Offline Rule-Based Universal Parser (100% offline capable)
        return parseOfflineRulePlan(input, language)
    }

    private fun parseAiJsonPlan(input: String, language: String, jsonStr: String): CommandPlan? {
        return try {
            val cleanJson = jsonStr.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val obj = JSONObject(cleanJson)
            val intent = obj.optString("intent", "")
            val commandsJson = obj.optJSONArray("commands") ?: return null

            val commandsList = mutableListOf<StructuredCommand>()
            for (i in 0 until commandsJson.length()) {
                val c = commandsJson.getJSONObject(i)
                val action = c.optString("action")
                val target = if (c.has("target")) c.optString("target") else null
                val paramsJson = c.optJSONObject("parameters")
                val params = mutableMapOf<String, String>()
                paramsJson?.keys()?.forEach { k ->
                    params[k] = paramsJson.optString(k, "")
                }
                val reqConf = c.optBoolean("requiresConfirmation", false)
                val confMsg = if (c.has("confirmationMessage")) c.optString("confirmationMessage") else null
                val risk = when (c.optString("riskLevel", "LOW").uppercase()) {
                    "HIGH" -> RiskLevel.HIGH
                    "MEDIUM" -> RiskLevel.MEDIUM
                    else -> RiskLevel.LOW
                }

                commandsList.add(
                    StructuredCommand(
                        action = action,
                        target = target,
                        parameters = params,
                        requiresConfirmation = reqConf,
                        confirmationMessage = confMsg,
                        riskLevel = risk
                    )
                )
            }

            CommandPlan(
                rawInput = input,
                detectedLanguage = language,
                commands = commandsList,
                intentDescription = intent
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseOfflineRulePlan(input: String, language: String): CommandPlan {
        val lower = input.lowercase().trim()
        val commands = mutableListOf<StructuredCommand>()

        // Check for multi-command separators (" and ", " then ", " এবং ")
        val subClauses = if (lower.contains(" and ") && !lower.contains("calculate")) {
            lower.split(" and ").map { it.trim() }
        } else if (lower.contains(" then ")) {
            lower.split(" then ").map { it.trim() }
        } else if (lower.contains(" এবং ")) {
            lower.split(" এবং ").map { it.trim() }
        } else {
            listOf(lower)
        }

        for (clause in subClauses) {
            val cmd = matchSingleCommand(clause)
            if (cmd != null) {
                commands.add(cmd)
            }
        }

        if (commands.isEmpty()) {
            // General query fallback -> Web search or Writing
            commands.add(
                StructuredCommand(
                    action = "web_search",
                    target = input,
                    parameters = mapOf("query" to input)
                )
            )
        }

        return CommandPlan(
            rawInput = input,
            detectedLanguage = language,
            commands = commands,
            intentDescription = "Universal Rule-based Execution Plan"
        )
    }

    private fun matchSingleCommand(clause: String): StructuredCommand? {
        val c = clause.trim()

        // 1. App Launcher: "open youtube", "launch maps", "open camera"
        if (c.startsWith("open ") || c.startsWith("launch ")) {
            val appTarget = c.removePrefix("open ").removePrefix("launch ").trim()
            if (appTarget.contains("setting")) {
                return StructuredCommand(
                    action = "system_settings",
                    target = appTarget,
                    parameters = mapOf("settingType" to appTarget)
                )
            }
            return StructuredCommand(
                action = "open_app",
                target = appTarget,
                parameters = mapOf("appName" to appTarget)
            )
        }

        // 2. Timer: "set a timer for 15 minutes", "timer 20 min"
        if (c.contains("timer")) {
            val minutes = c.filter { it.isDigit() }.ifBlank { "5" }
            return StructuredCommand(
                action = "timer",
                target = "$minutes minutes",
                parameters = mapOf("minutes" to minutes, "label" to "NOVA Timer")
            )
        }

        // 3. Alarm: "set alarm for 7:30", "alarm 7 am"
        if (c.contains("alarm")) {
            return StructuredCommand(
                action = "alarm",
                target = "Alarm",
                parameters = mapOf("hour" to "7", "minute" to "0", "label" to "NOVA Morning Alarm")
            )
        }

        // 4. Calculator: "calculate 250 * 35", "what is 50 + 20"
        if (c.startsWith("calculate ") || c.startsWith("what is ") || c.contains(" × ") || c.contains(" + ") || c.contains(" * ")) {
            val expr = c.removePrefix("calculate ").removePrefix("what is ").trim()
            return StructuredCommand(
                action = "calculator",
                target = expr,
                parameters = mapOf("expression" to expr)
            )
        }

        // 5. Device Telemetry: "battery", "storage", "device info", "battery info"
        if (c.contains("battery") || c.contains("device info") || c.contains("storage") || c.contains("system status")) {
            return StructuredCommand(
                action = "device_info",
                target = "telemetry",
                parameters = mapOf("query" to "all")
            )
        }

        // 6. Navigation: "navigate to dubai mall", "directions to airport"
        if (c.startsWith("navigate to ") || c.startsWith("directions to ") || c.contains("directions to ")) {
            val dest = c.substringAfter("directions to ").substringAfter("navigate to ").trim()
            return StructuredCommand(
                action = "navigation",
                target = dest,
                parameters = mapOf("destination" to dest)
            )
        }

        // 7. Communication: "call mom", "call 017112233" (HIGH RISK -> requires confirmation!)
        if (c.startsWith("call ")) {
            val recipient = c.removePrefix("call ").trim()
            return StructuredCommand(
                action = "phone_call",
                target = recipient,
                parameters = mapOf("phoneNumber" to recipient, "recipient" to recipient),
                requiresConfirmation = true,
                confirmationMessage = "Are you sure you want to call $recipient?",
                riskLevel = RiskLevel.HIGH
            )
        }

        // 8. SMS: "send message to john: hello" (HIGH RISK -> requires confirmation!)
        if (c.startsWith("send message to ") || c.startsWith("message ") || c.startsWith("sms ")) {
            val target = c.removePrefix("send message to ").removePrefix("message ").removePrefix("sms ").trim()
            val recipient = target.substringBefore(":").substringBefore(" saying ").trim()
            val text = if (target.contains(":")) target.substringAfter(":") else target.substringAfter(" saying ")
            return StructuredCommand(
                action = "send_sms",
                target = recipient,
                parameters = mapOf("recipient" to recipient, "message" to text.trim()),
                requiresConfirmation = true,
                confirmationMessage = "Send SMS to $recipient with message: \"${text.trim()}\"?",
                riskLevel = RiskLevel.HIGH
            )
        }

        // 9. Notes: "create note buy groceries", "note: ..."
        if (c.startsWith("create note ") || c.startsWith("note ") || c.contains("note saying ")) {
            val content = c.removePrefix("create note ").removePrefix("note ").substringAfter("note saying ").trim()
            return StructuredCommand(
                action = "create_note",
                target = content,
                parameters = mapOf("title" to "Quick Note", "content" to content, "category" to "General")
            )
        }

        // 10. Tasks: "add buy groceries to my task list", "task call plumber"
        if (c.contains("task") || c.startsWith("todo ")) {
            val title = c.removePrefix("add ").removePrefix("task ").removeSuffix(" to my task list").removeSuffix(" to my tasks").trim()
            return StructuredCommand(
                action = "create_task",
                target = title,
                parameters = mapOf("title" to title, "priority" to "Medium")
            )
        }

        // 11. Reminders: "remind me in 30 minutes", "আজ ৮টার জন্য একটা reminder set করো"
        if (c.contains("remind") || c.contains("reminder")) {
            val title = if (c.contains("to ")) c.substringAfter("to ").trim() else "NOVA Reminder"
            val mins = c.filter { it.isDigit() }.ifBlank { "30" }
            return StructuredCommand(
                action = "set_reminder",
                target = title,
                parameters = mapOf("title" to title, "minutesFromNow" to mins)
            )
        }

        // 12. Translation: "translate ... to bengali", "translate to tagalog"
        if (c.contains("translate")) {
            val targetLang = when {
                c.contains("bengali") || c.contains("bangla") -> "Bengali"
                c.contains("tagalog") || c.contains("filipino") -> "Tagalog"
                c.contains("indonesian") -> "Indonesian"
                c.contains("arabic") -> "Arabic"
                else -> "Bengali"
            }
            val text = c.removePrefix("translate ").substringBefore(" to ").substringBefore(" into ").trim()
            return StructuredCommand(
                action = "translate",
                target = text,
                parameters = mapOf("text" to text, "targetLanguage" to targetLang)
            )
        }

        // 13. Writing: "write message to my manager", "write email"
        if (c.startsWith("write ") || c.startsWith("compose ")) {
            val prompt = c.removePrefix("write ").removePrefix("compose ").trim()
            return StructuredCommand(
                action = "write_text",
                target = prompt,
                parameters = mapOf("prompt" to prompt, "type" to "message")
            )
        }

        // 14. Camera
        if (c == "camera" || c == "take a photo" || c == "take photo") {
            return StructuredCommand(action = "camera", target = "camera")
        }

        // 15. Image Generation / Avatar: "create a profile picture", "generate a gaming logo"
        if (c.contains("profile picture") || c.contains("avatar") || c.contains("wallpaper") || c.contains("generate image") || c.contains("create logo")) {
            return StructuredCommand(
                action = "image_generation",
                target = c,
                parameters = mapOf("prompt" to c, "style" to "cyberpunk")
            )
        }

        // 16. Image Editing: "make it cinematic", "remove background", "vintage filter"
        if (c.contains("cinematic") || c.contains("vintage") || c.contains("filter") || c.contains("edit photo")) {
            return StructuredCommand(
                action = "image_edit",
                target = c,
                parameters = mapOf("effect" to if (c.contains("vintage")) "vintage" else "cinematic")
            )
        }

        // 17. Settings shortcuts
        if (c.contains("bluetooth") || c.contains("wifi") || c.contains("display") || c.contains("sound")) {
            return StructuredCommand(
                action = "system_settings",
                target = c,
                parameters = mapOf("settingType" to c)
            )
        }

        return null
    }
}

data class ExecutionSummary(
    val success: Boolean,
    val responseMessage: String,
    val results: List<ToolResult> = emptyList(),
    val pendingConfirmation: PendingConfirmation? = null
)
