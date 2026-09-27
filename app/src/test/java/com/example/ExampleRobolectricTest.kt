package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.nova.model.RiskLevel
import com.example.nova.model.StructuredCommand
import com.example.nova.model.ToolStatus
import com.example.nova.tools.CalculatorTool
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("NOVA AI", appName)
    }

    @Test
    fun `calculator evaluates basic arithmetic correctly`() = runBlocking {
        val calc = CalculatorTool()
        val context = ApplicationProvider.getApplicationContext<Context>()

        val result1 = calc.execute(context, mapOf("expression" to "250 * 35"))
        assertEquals(ToolStatus.SUCCESS, result1.status)
        assertEquals("8750", result1.data["result"])

        val result2 = calc.execute(context, mapOf("expression" to "(100 + 50) / 2"))
        assertEquals(ToolStatus.SUCCESS, result2.status)
        assertEquals("75", result2.data["result"])
    }

    @Test
    fun `high risk command flags confirmation`() {
        val cmd = StructuredCommand(
            action = "phone_call",
            target = "Mom",
            requiresConfirmation = true,
            riskLevel = RiskLevel.HIGH
        )
        assertTrue(cmd.requiresConfirmation)
        assertEquals(RiskLevel.HIGH, cmd.riskLevel)
    }
}
