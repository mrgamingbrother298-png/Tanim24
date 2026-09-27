package com.example.nova.tools

import android.content.Context
import com.example.nova.model.*

interface NovaTool {
    val id: String
    val name: String
    val description: String
    val category: String
    val riskLevel: RiskLevel
    val confirmationPolicy: ConfirmationPolicy
    val requiredPermissions: List<String>

    fun isSupported(context: Context): Boolean = true
    fun validateParameters(parameters: Map<String, String>): Boolean
    suspend fun execute(context: Context, parameters: Map<String, String>): ToolResult
    fun verifyResult(result: ToolResult): Boolean = result.status == ToolStatus.SUCCESS
}
