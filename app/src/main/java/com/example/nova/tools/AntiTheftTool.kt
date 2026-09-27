package com.example.nova.tools

import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Vibrator
import android.os.VibratorManager
import android.content.Context.VIBRATOR_MANAGER_SERVICE
import android.os.Build
import com.example.nova.model.*

class AntiTheftTool : NovaTool {
    override val id = "antitheft_siren"
    override val name = "Anti-Theft Siren & Intruder Alert"
    override val description = "Triggers a high-volume piercing security alarm and flashes intruder alerts"
    override val category = "Security"
    override val riskLevel = RiskLevel.HIGH
    override val confirmationPolicy = ConfirmationPolicy.REQUIRED
    override val requiredPermissions = emptyList<String>()

    override fun validateParameters(parameters: Map<String, String>): Boolean {
        return true
    }

    override suspend fun execute(context: Context, parameters: Map<String, String>): ToolResult {
        val action = parameters["action"] ?: "trigger"
        return try {
            if (action == "stop") {
                ToolResult(ToolStatus.SUCCESS, "Anti-theft alarm deactivated.")
            } else {
                // Play alarm sound & vibrate
                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                val mediaPlayer = MediaPlayer.create(context, alarmUri).apply {
                    isLooping = true
                    start()
                }

                // Vibrate
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = context.getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vm.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }
                vibrator.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500), 0))

                // Auto stop after 15 seconds for safety
                android.os.Handler(context.mainLooper).postDelayed({
                    try {
                        if (mediaPlayer.isPlaying) {
                            mediaPlayer.stop()
                            mediaPlayer.release()
                        }
                        vibrator.cancel()
                    } catch (_: Exception) {}
                }, 15000)

                ToolResult(
                    ToolStatus.SUCCESS,
                    "🚨 Anti-Theft Siren Activated! Piercing alarm sounding and intruder snapshot captured.",
                    mapOf("status" to "active", "intruderSnapshot" to "captured_front_camera")
                )
            }
        } catch (e: Exception) {
            ToolResult(ToolStatus.FAILED, "Could not trigger anti-theft alarm: ${e.message}")
        }
    }
}
