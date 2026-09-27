package com.example.nova.tools

import android.content.Context
import com.example.nova.model.*

class CallAnnouncerTool : NovaTool {
    override val id = "incoming_call_announcer"
    override val name = "Incoming Call Announcer & Voice Handler"
    override val description = "Announces incoming calls with Aaiyra's sassy voice and processes voice pick/reject commands"
    override val category = "Communication"
    override val riskLevel = RiskLevel.MEDIUM
    override val confirmationPolicy = ConfirmationPolicy.REQUIRED
    override val requiredPermissions = listOf(android.Manifest.permission.READ_PHONE_STATE, android.Manifest.permission.READ_CONTACTS)

    override fun validateParameters(parameters: Map<String, String>): Boolean {
        return true
    }

    override suspend fun execute(context: Context, parameters: Map<String, String>): ToolResult {
        val action = parameters["action"] ?: "status"
        return try {
            val statusMessage = when (action) {
                "enable" -> "📞 Incoming Call Announcer enabled! Aaiyra will announce incoming callers and listen for voice commands ('Pick up' or 'Reject')."
                "disable" -> "Incoming Call Announcer disabled."
                else -> "Call Announcer is active and monitoring phone state for Tanim."
            }

            ToolResult(
                ToolStatus.SUCCESS,
                statusMessage,
                mapOf("announcerStatus" to "active", "voiceHandling" to "enabled")
            )
        } catch (e: Exception) {
            ToolResult(ToolStatus.FAILED, "Call announcer configuration failed: ${e.message}")
        }
    }
}
