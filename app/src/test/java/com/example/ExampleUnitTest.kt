package com.example

import com.example.nova.model.RiskLevel
import com.example.nova.model.StructuredCommand
import com.example.nova.model.ToolResult
import com.example.nova.model.ToolStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun commandModels_verifyHighRiskFlag() {
        val cmd = StructuredCommand(
            action = "phone_call",
            target = "Mom",
            requiresConfirmation = true,
            riskLevel = RiskLevel.HIGH
        )
        assertTrue(cmd.requiresConfirmation)
        assertEquals(RiskLevel.HIGH, cmd.riskLevel)
    }

    @Test
    fun toolResult_statusVerification() {
        val success = ToolResult(ToolStatus.SUCCESS, "Executed successfully")
        assertEquals(ToolStatus.SUCCESS, success.status)

        val denied = ToolResult(ToolStatus.DENIED, "Permission rejected")
        assertEquals(ToolStatus.DENIED, denied.status)
    }
}
