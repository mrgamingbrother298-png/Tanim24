package com.example.nova.model

enum class AssistantState {
    IDLE,
    LISTENING,
    THINKING,
    EXECUTING,
    SPEAKING,
    ERROR
}

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH
}

enum class ConfirmationPolicy {
    NEVER,
    OPTIONAL,
    REQUIRED
}

enum class ToolStatus {
    SUCCESS,
    FAILED,
    DENIED,
    NOT_SUPPORTED,
    TIMEOUT,
    CANCELLED,
    REQUIRES_PERMISSION,
    REQUIRES_CONFIRMATION
}

data class ToolResult(
    val status: ToolStatus,
    val message: String,
    val data: Map<String, String> = emptyMap(),
    val executionTimeMs: Long = 0L
)

data class StructuredCommand(
    val action: String,
    val target: String? = null,
    val parameters: Map<String, String> = emptyMap(),
    val requiresConfirmation: Boolean = false,
    val confirmationMessage: String? = null,
    val riskLevel: RiskLevel = RiskLevel.LOW
)

data class CommandPlan(
    val rawInput: String,
    val detectedLanguage: String = "English",
    val commands: List<StructuredCommand>,
    val overallRisk: RiskLevel = RiskLevel.LOW,
    val intentDescription: String = ""
)

data class PendingConfirmation(
    val id: String = java.util.UUID.randomUUID().toString(),
    val actionName: String,
    val title: String,
    val description: String,
    val riskLevel: RiskLevel,
    val command: StructuredCommand
)

data class ToolMetadata(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val riskLevel: RiskLevel,
    val requiredPermissions: List<String> = emptyList(),
    val isEnabled: Boolean = true
)

data class DeviceInfo(
    val model: String,
    val manufacturer: String,
    val androidVersion: String,
    val sdkInt: Int,
    val batteryLevel: Int,
    val isCharging: Boolean,
    val networkType: String,
    val isOnline: Boolean,
    val totalStorageGb: String,
    val freeStorageGb: String
)
