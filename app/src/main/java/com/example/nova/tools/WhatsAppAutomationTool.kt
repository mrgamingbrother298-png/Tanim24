package com.example.nova.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.nova.model.*

class WhatsAppAutomationTool : NovaTool {
    override val id = "whatsapp_automation"
    override val name = "WhatsApp Voice & Text Automation"
    override val description = "Sends WhatsApp text messages, initiates chats, and handles WhatsApp voice automation"
    override val category = "Communication"
    override val riskLevel = RiskLevel.MEDIUM
    override val confirmationPolicy = ConfirmationPolicy.REQUIRED
    override val requiredPermissions = emptyList<String>()

    override fun validateParameters(parameters: Map<String, String>): Boolean {
        val message = parameters["message"] ?: parameters["text"] ?: parameters["query"]
        return !message.isNullOrBlank()
    }

    override suspend fun execute(context: Context, parameters: Map<String, String>): ToolResult {
        val recipient = parameters["recipient"] ?: parameters["contactName"] ?: parameters["phone"] ?: ""
        val message = parameters["message"] ?: parameters["text"] ?: "Hello from Aaiyra!"
        val actionType = parameters["actionType"] ?: "send_text"

        return try {
            val cleanPhone = recipient.filter { it.isDigit() || it == '+' }
            val uriString = if (cleanPhone.isNotBlank()) {
                "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}"
            } else {
                "https://api.whatsapp.com/send?text=${Uri.encode(message)}"
            }

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                // Fallback to general browser intent if WhatsApp package is not resolved directly
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            }

            ToolResult(
                ToolStatus.SUCCESS,
                "WhatsApp automation executed for recipient: '$recipient' with message: '$message'",
                mapOf("recipient" to recipient, "message" to message, "action" to actionType)
            )
        } catch (e: Exception) {
            ToolResult(ToolStatus.FAILED, "WhatsApp automation failed: ${e.message}")
        }
    }
}
