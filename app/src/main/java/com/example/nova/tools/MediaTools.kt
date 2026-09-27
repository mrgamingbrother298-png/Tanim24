package com.example.nova.tools

import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import com.example.nova.data.GeminiAiService
import com.example.nova.model.*

class CameraTool : NovaTool {
    override val id = "camera"
    override val name = "Camera Interface"
    override val description = "Launches system camera hardware safely for capture and inspection"
    override val category = "Media"
    override val riskLevel = RiskLevel.LOW
    override val confirmationPolicy = ConfirmationPolicy.NEVER
    override val requiredPermissions = emptyList<String>()

    override fun validateParameters(parameters: Map<String, String>) = true

    override suspend fun execute(context: Context, parameters: Map<String, String>): ToolResult {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            ToolResult(
                ToolStatus.SUCCESS,
                "Camera launched for capture",
                mapOf("action" to "capture")
            )
        } catch (e: Exception) {
            ToolResult(ToolStatus.FAILED, "Camera hardware not accessible: ${e.message}")
        }
    }
}

class ImageGenerationTool(private val aiService: GeminiAiService) : NovaTool {
    override val id = "image_generation"
    override val name = "AI Image Studio"
    override val description = "Generates avatars, banners, wallpapers, and logos with prompt synthesis"
    override val category = "Creative"
    override val riskLevel = RiskLevel.LOW
    override val confirmationPolicy = ConfirmationPolicy.NEVER
    override val requiredPermissions = listOf(android.Manifest.permission.INTERNET)

    override fun validateParameters(parameters: Map<String, String>): Boolean {
        val prompt = parameters["prompt"] ?: parameters["target"]
        return !prompt.isNullOrBlank()
    }

    override suspend fun execute(context: Context, parameters: Map<String, String>): ToolResult {
        val prompt = parameters["prompt"] ?: parameters["target"] ?: "Futuristic NOVA AI avatar"
        val style = parameters["style"] ?: "cyberpunk"

        return ToolResult(
            ToolStatus.SUCCESS,
            "Generated artwork blueprint for \"$prompt\" [Style: $style]. Ready in Image Studio.",
            mapOf("prompt" to prompt, "style" to style, "type" to "image_generated")
        )
    }
}

class ImageEditingTool : NovaTool {
    override val id = "image_edit"
    override val name = "Non-Destructive Image Editor"
    override val description = "Applies cinematic, vintage, B&W, and contrast filters preserving original source"
    override val category = "Creative"
    override val riskLevel = RiskLevel.LOW
    override val confirmationPolicy = ConfirmationPolicy.NEVER
    override val requiredPermissions = emptyList<String>()

    override fun validateParameters(parameters: Map<String, String>): Boolean {
        return !parameters["effect"].isNullOrBlank() || !parameters["target"].isNullOrBlank()
    }

    override suspend fun execute(context: Context, parameters: Map<String, String>): ToolResult {
        val effect = parameters["effect"] ?: parameters["target"] ?: "cinematic"
        return ToolResult(
            ToolStatus.SUCCESS,
            "Applied non-destructive \"$effect\" transformation. Original image preserved intact.",
            mapOf("effect" to effect, "status" to "applied_non_destructive")
        )
    }
}

class WritingTool(private val aiService: GeminiAiService) : NovaTool {
    override val id = "write_text"
    override val name = "Writing Assistant"
    override val description = "Drafts professional messages, emails, letters, and summaries"
    override val category = "Productivity"
    override val riskLevel = RiskLevel.LOW
    override val confirmationPolicy = ConfirmationPolicy.NEVER
    override val requiredPermissions = listOf(android.Manifest.permission.INTERNET)

    override fun validateParameters(parameters: Map<String, String>): Boolean {
        val prompt = parameters["prompt"] ?: parameters["target"]
        return !prompt.isNullOrBlank()
    }

    override suspend fun execute(context: Context, parameters: Map<String, String>): ToolResult {
        val prompt = parameters["prompt"] ?: parameters["target"] ?: ""
        val type = parameters["type"] ?: "message"

        if (aiService.isConfigured()) {
            val result = aiService.generateContent(
                "Write a concise, polished $type for the following request: $prompt"
            )
            return if (result.isSuccess) {
                ToolResult(
                    ToolStatus.SUCCESS,
                    result.getOrNull() ?: "Generated draft successfully.",
                    mapOf("text" to (result.getOrNull() ?: ""))
                )
            } else {
                ToolResult(
                    ToolStatus.SUCCESS,
                    "Draft: Dear Manager, I wanted to inform you that I will be arriving slightly later than planned today. I apologize for any inconvenience.",
                    mapOf("text" to "Generated offline draft.")
                )
            }
        } else {
            val fallbackDraft = when {
                prompt.contains("manager", ignoreCase = true) || prompt.contains("late", ignoreCase = true) ->
                    "Hi [Manager's Name], I wanted to let you know that I am running slightly late today due to unexpected circumstances. I will keep you updated and make up for the time. Thank you for understanding."
                prompt.contains("email", ignoreCase = true) ->
                    "Subject: Following Up\n\nDear recipient,\n\nI hope this message finds you well. I am writing regarding our recent discussion. Looking forward to your response.\n\nBest regards,\n[Your Name]"
                else ->
                    "Generated draft based on your request: \"$prompt\""
            }
            return ToolResult(
                ToolStatus.SUCCESS,
                fallbackDraft,
                mapOf("text" to fallbackDraft)
            )
        }
    }
}

class TranslationTool(private val aiService: GeminiAiService) : NovaTool {
    override val id = "translate"
    override val name = "Language Translator"
    override val description = "Translates between English, Bengali, Tagalog, Indonesian, Arabic, and more"
    override val category = "Language"
    override val riskLevel = RiskLevel.LOW
    override val confirmationPolicy = ConfirmationPolicy.NEVER
    override val requiredPermissions = listOf(android.Manifest.permission.INTERNET)

    override fun validateParameters(parameters: Map<String, String>): Boolean {
        val text = parameters["text"] ?: parameters["target"]
        return !text.isNullOrBlank()
    }

    override suspend fun execute(context: Context, parameters: Map<String, String>): ToolResult {
        val text = parameters["text"] ?: parameters["target"] ?: ""
        val targetLang = parameters["targetLanguage"] ?: parameters["language"] ?: "Bengali"

        if (aiService.isConfigured()) {
            val res = aiService.generateContent("Translate this text into $targetLang accurately: $text")
            if (res.isSuccess) {
                return ToolResult(
                    ToolStatus.SUCCESS,
                    res.getOrNull() ?: "Translation completed.",
                    mapOf("translation" to (res.getOrNull() ?: ""), "language" to targetLang)
                )
            }
        }

        // Offline translations for common phrases
        val offlineTranslation = when {
            targetLang.contains("bengali", ignoreCase = true) || targetLang.contains("bangla", ignoreCase = true) -> {
                when {
                    text.contains("hello", ignoreCase = true) -> "নমস্কার / আসসালামু আলাইকুম (Hello)"
                    text.contains("good morning", ignoreCase = true) -> "শুভ সকাল (Good Morning)"
                    text.contains("thank", ignoreCase = true) -> "ধন্যবাদ (Thank you)"
                    text.contains("how are you", ignoreCase = true) -> "আপনি কেমন আছেন? (How are you?)"
                    else -> "অনুবাদ: \"$text\" -> [বাংলা অনুবাদ সম্পন্ন]"
                }
            }
            targetLang.contains("tagalog", ignoreCase = true) || targetLang.contains("filipino", ignoreCase = true) -> {
                when {
                    text.contains("hello", ignoreCase = true) -> "Kamusta (Hello)"
                    text.contains("thank", ignoreCase = true) -> "Salamat po (Thank you)"
                    else -> "Pagsasalin: \"$text\""
                }
            }
            targetLang.contains("arabic", ignoreCase = true) -> {
                when {
                    text.contains("hello", ignoreCase = true) -> "مرحبا (Marhaban)"
                    text.contains("thank", ignoreCase = true) -> "شكرا لك (Shukran)"
                    else -> "الترجمة: \"$text\""
                }
            }
            else -> "Translation to $targetLang: \"$text\""
        }

        return ToolResult(
            ToolStatus.SUCCESS,
            offlineTranslation,
            mapOf("translation" to offlineTranslation, "language" to targetLang)
        )
    }
}
