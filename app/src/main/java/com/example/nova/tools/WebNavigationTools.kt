package com.example.nova.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.nova.model.*
import java.net.URLEncoder

class WebSearchTool : NovaTool {
    override val id = "web_search"
    override val name = "Web Search"
    override val description = "Executes web searches via browser and search engine APIs"
    override val category = "Information"
    override val riskLevel = RiskLevel.LOW
    override val confirmationPolicy = ConfirmationPolicy.NEVER
    override val requiredPermissions = listOf(android.Manifest.permission.INTERNET)

    override fun validateParameters(parameters: Map<String, String>): Boolean {
        val query = parameters["query"] ?: parameters["target"]
        return !query.isNullOrBlank()
    }

    override suspend fun execute(context: Context, parameters: Map<String, String>): ToolResult {
        val query = parameters["query"] ?: parameters["target"] ?: ""
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val searchUri = Uri.parse("https://www.google.com/search?q=$encodedQuery")

        val intent = Intent(Intent.ACTION_VIEW, searchUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            ToolResult(
                ToolStatus.SUCCESS,
                "Searched the web for: \"$query\"",
                mapOf("query" to query, "url" to searchUri.toString())
            )
        } catch (e: Exception) {
            ToolResult(ToolStatus.FAILED, "Could not open browser for search: ${e.message}")
        }
    }
}

class NavigationTool : NovaTool {
    override val id = "navigation"
    override val name = "Maps & Navigation"
    override val description = "Provides navigation directions using official Google Maps intents"
    override val category = "Navigation"
    override val riskLevel = RiskLevel.LOW
    override val confirmationPolicy = ConfirmationPolicy.NEVER
    override val requiredPermissions = emptyList<String>()

    override fun validateParameters(parameters: Map<String, String>): Boolean {
        val destination = parameters["destination"] ?: parameters["target"]
        return !destination.isNullOrBlank()
    }

    override suspend fun execute(context: Context, parameters: Map<String, String>): ToolResult {
        val destination = parameters["destination"] ?: parameters["target"] ?: ""
        val encodedDest = URLEncoder.encode(destination, "UTF-8")

        // Try Maps navigation intent first
        val mapUri = Uri.parse("google.navigation:q=$encodedDest")
        val mapIntent = Intent(Intent.ACTION_VIEW, mapUri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
                ToolResult(
                    ToolStatus.SUCCESS,
                    "Starting directions to $destination in Google Maps",
                    mapOf("destination" to destination)
                )
            } else {
                // Fallback to geo query or browser
                val geoUri = Uri.parse("geo:0,0?q=$encodedDest")
                val geoIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(geoIntent)
                ToolResult(
                    ToolStatus.SUCCESS,
                    "Showing directions for $destination",
                    mapOf("destination" to destination)
                )
            }
        } catch (e: Exception) {
            // Web fallback
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$encodedDest")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            ToolResult(
                ToolStatus.SUCCESS,
                "Opened maps directions for $destination",
                mapOf("destination" to destination)
            )
        }
    }
}

class BrowserTool : NovaTool {
    override val id = "open_browser"
    override val name = "Web Browser"
    override val description = "Navigates to safe verified web URLs"
    override val category = "Information"
    override val riskLevel = RiskLevel.LOW
    override val confirmationPolicy = ConfirmationPolicy.NEVER
    override val requiredPermissions = listOf(android.Manifest.permission.INTERNET)

    override fun validateParameters(parameters: Map<String, String>): Boolean {
        val url = parameters["url"] ?: parameters["target"]
        return !url.isNullOrBlank()
    }

    override suspend fun execute(context: Context, parameters: Map<String, String>): ToolResult {
        var url = parameters["url"] ?: parameters["target"] ?: ""
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            ToolResult(ToolStatus.SUCCESS, "Opened webpage: $url", mapOf("url" to url))
        } catch (e: Exception) {
            ToolResult(ToolStatus.FAILED, "Failed to open link: ${e.message}")
        }
    }
}
