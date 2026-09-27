package com.example.nova.tools

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import com.example.nova.model.*

class OpenAppTool : NovaTool {
    override val id = "open_app"
    override val name = "App Launcher"
    override val description = "Safely launch installed Android applications via official package manager"
    override val category = "Device"
    override val riskLevel = RiskLevel.LOW
    override val confirmationPolicy = ConfirmationPolicy.NEVER
    override val requiredPermissions = emptyList<String>()

    private val commonPackageMap = mapOf(
        "youtube" to "com.google.android.youtube",
        "maps" to "com.google.android.apps.maps",
        "google maps" to "com.google.android.apps.maps",
        "chrome" to "com.android.chrome",
        "browser" to "com.android.chrome",
        "camera" to "camera",
        "calculator" to "calculator",
        "clock" to "clock",
        "settings" to "settings",
        "play store" to "com.android.vending",
        "whatsapp" to "com.whatsapp",
        "spotify" to "com.spotify.music",
        "telegram" to "org.telegram.messenger",
        "gmail" to "com.google.android.gm"
    )

    override fun validateParameters(parameters: Map<String, String>): Boolean {
        val appName = parameters["appName"] ?: parameters["target"]
        return !appName.isNullOrBlank()
    }

    override suspend fun execute(context: Context, parameters: Map<String, String>): ToolResult {
        val appName = (parameters["appName"] ?: parameters["target"] ?: "").trim().lowercase()
        val pm = context.packageManager

        // Special handling for common built-ins
        if (appName == "camera") {
            val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return try {
                context.startActivity(intent)
                ToolResult(ToolStatus.SUCCESS, "Opened Camera application", mapOf("app" to "Camera"))
            } catch (e: Exception) {
                ToolResult(ToolStatus.FAILED, "Could not open camera: ${e.message}")
            }
        }

        if (appName == "settings") {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return ToolResult(ToolStatus.SUCCESS, "Opened Android Settings", mapOf("app" to "Settings"))
        }

        val targetPackage = commonPackageMap[appName]

        if (targetPackage != null) {
            val launchIntent = pm.getLaunchIntentForPackage(targetPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return ToolResult(
                    ToolStatus.SUCCESS,
                    "Launched $appName successfully",
                    mapOf("package" to targetPackage, "appName" to appName)
                )
            }
        }

        // Try searching installed launchable apps
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        for (info in resolveInfos) {
            val label = info.loadLabel(pm).toString().lowercase()
            if (label.contains(appName) || appName.contains(label)) {
                val launchIntent = pm.getLaunchIntentForPackage(info.activityInfo.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return ToolResult(
                        ToolStatus.SUCCESS,
                        "Launched ${info.loadLabel(pm)}",
                        mapOf("package" to info.activityInfo.packageName)
                    )
                }
            }
        }

        // Deep-link to YouTube or Play store as official fallback
        if (appName.contains("youtube")) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            return ToolResult(ToolStatus.SUCCESS, "Opened YouTube in browser", mapOf("type" to "web_fallback"))
        }

        // Offer play store link
        return ToolResult(
            ToolStatus.FAILED,
            "App \"$appName\" is not installed on this device. Would you like to check Google Play Store?",
            mapOf("appName" to appName, "canSearchStore" to "true")
        )
    }
}

class SystemSettingsTool : NovaTool {
    override val id = "system_settings"
    override val name = "Settings Navigator"
    override val description = "Opens official Android system configuration screens"
    override val category = "Device"
    override val riskLevel = RiskLevel.LOW
    override val confirmationPolicy = ConfirmationPolicy.NEVER
    override val requiredPermissions = emptyList<String>()

    override fun validateParameters(parameters: Map<String, String>): Boolean {
        return !parameters["settingType"].isNullOrBlank() || !parameters["target"].isNullOrBlank()
    }

    override suspend fun execute(context: Context, parameters: Map<String, String>): ToolResult {
        val type = (parameters["settingType"] ?: parameters["target"] ?: "").trim().lowercase()

        val action = when {
            type.contains("wifi") || type.contains("wi-fi") || type.contains("internet") -> Settings.ACTION_WIFI_SETTINGS
            type.contains("bluetooth") -> Settings.ACTION_BLUETOOTH_SETTINGS
            type.contains("display") || type.contains("brightness") || type.contains("screen") -> Settings.ACTION_DISPLAY_SETTINGS
            type.contains("sound") || type.contains("volume") || type.contains("audio") -> Settings.ACTION_SOUND_SETTINGS
            type.contains("battery") || type.contains("power") -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            type.contains("storage") -> Settings.ACTION_INTERNAL_STORAGE_SETTINGS
            type.contains("notification") -> Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
            type.contains("location") || type.contains("gps") -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
            type.contains("app") -> Settings.ACTION_APPLICATION_SETTINGS
            type.contains("date") || type.contains("time") -> Settings.ACTION_DATE_SETTINGS
            type.contains("privacy") || type.contains("security") -> Settings.ACTION_SECURITY_SETTINGS
            type.contains("accessibility") -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }

        return try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolResult(
                ToolStatus.SUCCESS,
                "Opened $type settings. Android requires manual confirmation for system setting changes.",
                mapOf("setting" to type)
            )
        } catch (e: Exception) {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
            ToolResult(ToolStatus.SUCCESS, "Opened general system settings", mapOf("setting" to "general"))
        }
    }
}

class DeviceInfoTool : NovaTool {
    override val id = "device_info"
    override val name = "Device Telemetry"
    override val description = "Retrieves battery status, storage capacity, and system specifications"
    override val category = "Device"
    override val riskLevel = RiskLevel.LOW
    override val confirmationPolicy = ConfirmationPolicy.NEVER
    override val requiredPermissions = emptyList<String>()

    override fun validateParameters(parameters: Map<String, String>) = true

    override suspend fun execute(context: Context, parameters: Map<String, String>): ToolResult {
        // Battery status
        val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, iFilter)
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        // Storage status
        val stat = StatFs(Environment.getDataDirectory().path)
        val bytesAvailable = stat.availableBlocksLong * stat.blockSizeLong
        val bytesTotal = stat.blockCountLong * stat.blockSizeLong
        val freeGb = String.format("%.1f GB", bytesAvailable.toDouble() / (1024 * 1024 * 1024))
        val totalGb = String.format("%.1f GB", bytesTotal.toDouble() / (1024 * 1024 * 1024))

        // Network status
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(activeNetwork)
        val isOnline = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val netType = when {
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular Mobile Data"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
            else -> if (isOnline) "Connected" else "Offline"
        }

        val report = buildString {
            append("• Device: ${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}\n")
            append("• OS: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
            append("• Battery: $batteryPct% ${if (isCharging) "⚡ (Charging)" else "🔋 (Discharging)"}\n")
            append("• Storage: $freeGb free of $totalGb\n")
            append("• Network: $netType (${if (isOnline) "Online" else "Offline"})")
        }

        return ToolResult(
            status = ToolStatus.SUCCESS,
            message = report,
            data = mapOf(
                "battery" to "$batteryPct%",
                "isCharging" to isCharging.toString(),
                "storageFree" to freeGb,
                "storageTotal" to totalGb,
                "model" to "${Build.MANUFACTURER} ${Build.MODEL}",
                "os" to "Android ${Build.VERSION.RELEASE}",
                "network" to netType,
                "isOnline" to isOnline.toString()
            )
        )
    }
}
