package com.example.nova.data

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiAiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }
    }

    fun isConfigured(): Boolean {
        val key = getApiKey()
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
    }

    suspend fun generateContent(prompt: String, systemInstruction: String? = null): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(IllegalStateException("Gemini API key is not configured in Secrets"))
        }

        try {
            val root = JSONObject()
            val contents = JSONArray()
            val contentObj = JSONObject()
            val parts = JSONArray()
            parts.put(JSONObject().put("text", prompt))
            contentObj.put("parts", parts)
            contents.put(contentObj)
            root.put("contents", contents)

            if (!systemInstruction.isNullOrBlank()) {
                val sysContent = JSONObject()
                val sysParts = JSONArray()
                sysParts.put(JSONObject().put("text", systemInstruction))
                sysContent.put("parts", sysParts)
                root.put("systemInstruction", sysContent)
            }

            val requestBody = root.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Gemini API error ${response.code}: $body"))
                }

                val json = JSONObject(body)
                val candidates = json.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val content = firstCandidate?.optJSONObject("content")
                val responseParts = content?.optJSONArray("parts")
                val text = responseParts?.optJSONObject(0)?.optString("text")

                if (!text.isNullOrBlank()) {
                    Result.success(text)
                } else {
                    Result.failure(Exception("Empty response from AI service"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ask Gemini to parse complex or multi-step natural language input into structured JSON
     */
    suspend fun parseIntentWithAi(userInput: String): Result<String> {
        val systemPrompt = """
            You are NOVA AI's Intent Parser. Analyze the user command and return ONLY a valid JSON object.
            Available action names:
            - open_app (params: appName)
            - system_settings (params: settingType [wifi, bluetooth, display, sound, battery, storage, notifications, location, apps])
            - device_info (params: query [all, battery, storage])
            - phone_call (params: recipient, phoneNumber)
            - send_sms (params: recipient, message)
            - send_email (params: recipient, subject, body)
            - camera (params: action [open, capture])
            - timer (params: minutes, seconds, label)
            - alarm (params: hour, minute, label)
            - stopwatch (params: action [start, stop])
            - calculator (params: expression)
            - create_note (params: title, content, category)
            - create_task (params: title, priority)
            - set_reminder (params: title, minutesFromNow, timeFormatted)
            - web_search (params: query)
            - navigation (params: destination)
            - write_text (params: prompt, type)
            - translate (params: text, targetLanguage)
            - image_generation (params: prompt, style)
            - image_edit (params: effect [cinematic, vintage, b_and_w, blur, contrast])

            JSON format:
            {
              "detectedLanguage": "English|Bengali|Tagalog|Indonesian|Arabic|etc",
              "intent": "brief explanation",
              "responseMessage": "What to tell the user",
              "commands": [
                {
                  "action": "action_name",
                  "target": "target string",
                  "parameters": { "key": "value" },
                  "requiresConfirmation": false,
                  "confirmationMessage": null,
                  "riskLevel": "LOW|MEDIUM|HIGH"
                }
              ]
            }
            High risk actions (sending sms, phone calls, file deletion) MUST set requiresConfirmation: true and riskLevel: "HIGH".
            Output ONLY valid JSON without Markdown blocks.
        """.trimIndent()

        return generateContent(userInput, systemPrompt)
    }
}
