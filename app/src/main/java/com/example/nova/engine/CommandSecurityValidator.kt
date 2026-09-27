package com.example.nova.engine

import com.example.nova.model.*
import com.example.nova.tools.ToolRegistry

sealed class SecurityValidationResult {
    data object Valid : SecurityValidationResult()
    data class Rejected(val reason: String, val violationType: String) : SecurityValidationResult()
}

class CommandSecurityValidator(private val registry: ToolRegistry) {

    private val forbiddenKeywords = listOf(
        "sh", "bash", "exec", "eval", "system", "su", "sudo", "root",
        "chmod", "rm -rf", "script", "javascript:", "<script>",
        "bypass", "exploit", "keystroke", "intercept", "keylogger"
    )

    fun validateCommand(command: StructuredCommand): SecurityValidationResult {
        // 1. Allowlist verification: Is the action known and registered?
        if (!registry.isAllowlisted(command.action)) {
            return SecurityValidationResult.Rejected(
                reason = "Action \"${command.action}\" is not registered in NOVA Tool Allowlist.",
                violationType = "UNSUPPORTED_ACTION"
            )
        }

        // 2. Tool availability: Is the tool active and enabled?
        val tool = registry.getTool(command.action)
        if (tool == null) {
            return SecurityValidationResult.Rejected(
                reason = "Tool \"${command.action}\" is currently disabled in assistant settings.",
                violationType = "TOOL_DISABLED"
            )
        }

        // 3. Security Firewall: Inspect targets and parameters for injection or forbidden keywords
        val target = command.target ?: ""
        for (keyword in forbiddenKeywords) {
            if (target.contains(keyword, ignoreCase = true)) {
                return SecurityValidationResult.Rejected(
                    reason = "Security Firewall blocked command containing prohibited sequence: \"$keyword\"",
                    violationType = "SECURITY_VIOLATION"
                )
            }
            for ((key, value) in command.parameters) {
                if (key.contains(keyword, ignoreCase = true) || value.contains(keyword, ignoreCase = true)) {
                    return SecurityValidationResult.Rejected(
                        reason = "Security Firewall blocked parameter containing prohibited keyword.",
                        violationType = "MALICIOUS_PAYLOAD"
                    )
                }
            }
        }

        // 4. Parameter validation by the registered Tool itself
        if (!tool.validateParameters(command.parameters)) {
            return SecurityValidationResult.Rejected(
                reason = "Invalid or incomplete parameters for ${tool.name}.",
                violationType = "INVALID_PARAMETERS"
            )
        }

        return SecurityValidationResult.Valid
    }
}
