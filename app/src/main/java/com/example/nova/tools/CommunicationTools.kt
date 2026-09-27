package com.example.nova.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.nova.model.*

class PhoneTool : NovaTool {
    override val id = "phone_call"
    override val name = "Phone Dialer"
    override val description = "Places calls or opens Android dialer with validated numbers"
    override val category = "Communication"
    override val riskLevel = RiskLevel.HIGH
    override val confirmationPolicy = ConfirmationPolicy.REQUIRED
    override val requiredPermissions = listOf(android.Manifest.permission.CALL_PHONE)

    override fun validateParameters(parameters: Map<String, String>): Boolean {
        val number = parameters["phoneNumber"] ?: parameters["target"] ?: parameters["recipient"]
        return !number.isNullOrBlank()
    }

    override suspend fun execute(context: Context, parameters: Map<String, String>): ToolResult {
        val recipient = parameters["phoneNumber"] ?: parameters["target"] ?: parameters["recipient"] ?: ""
        val cleanNumber = recipient.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }

        val uri = if (cleanNumber.isNotBlank()) {
            Uri.parse("tel:$cleanNumber")
        } else {
            Uri.parse("tel:")
        }

        val intent = Intent(Intent.ACTION_DIAL, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            ToolResult(
                ToolStatus.SUCCESS,
                "Opened phone dialer for $recipient",
                mapOf("recipient" to recipient, "number" to cleanNumber)
            )
        } catch (e: Exception) {
            ToolResult(ToolStatus.FAILED, "Could not open dialer: ${e.message}")
        }
    }
}

class SmsTool : NovaTool {
    override val id = "send_sms"
    override val name = "SMS Messenger"
    override val description = "Prepares SMS messages safely through Android messaging APIs"
    override val category = "Communication"
    override val riskLevel = RiskLevel.HIGH
    override val confirmationPolicy = ConfirmationPolicy.REQUIRED
    override val requiredPermissions = emptyList<String>()

    override fun validateParameters(parameters: Map<String, String>): Boolean {
        val msg = parameters["message"] ?: parameters["text"]
        return !msg.isNullOrBlank()
    }

    override suspend fun execute(context: Context, parameters: Map<String, String>): ToolResult {
        val recipient = parameters["recipient"] ?: parameters["phoneNumber"] ?: ""
        val message = parameters["message"] ?: parameters["text"] ?: ""

        val uri = Uri.parse("smsto:$recipient")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            ToolResult(
                ToolStatus.SUCCESS,
                "Prepared message for $recipient: \"$message\". You can review and tap send.",
                mapOf("recipient" to recipient, "message" to message)
            )
        } catch (e: Exception) {
            ToolResult(ToolStatus.FAILED, "Unable to launch messaging app: ${e.message}")
        }
    }
}

class EmailTool : NovaTool {
    override val id = "send_email"
    override val name = "Email Composer"
    override val description = "Prepares emails through the device's default email client"
    override val category = "Communication"
    override val riskLevel = RiskLevel.MEDIUM
    override val confirmationPolicy = ConfirmationPolicy.REQUIRED
    override val requiredPermissions = emptyList<String>()

    override fun validateParameters(parameters: Map<String, String>): Boolean {
        val body = parameters["body"] ?: parameters["message"] ?: parameters["text"]
        return !body.isNullOrBlank()
    }

    override suspend fun execute(context: Context, parameters: Map<String, String>): ToolResult {
        val recipient = parameters["recipient"] ?: parameters["email"] ?: ""
        val subject = parameters["subject"] ?: "NOVA AI Assistant"
        val body = parameters["body"] ?: parameters["message"] ?: parameters["text"] ?: ""

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$recipient")
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            ToolResult(
                ToolStatus.SUCCESS,
                "Email draft prepared for $recipient with subject \"$subject\"",
                mapOf("recipient" to recipient, "subject" to subject)
            )
        } catch (e: Exception) {
            ToolResult(ToolStatus.FAILED, "No email app found: ${e.message}")
        }
    }
}
