package com.example.nova.tools

import com.example.nova.data.GeminiAiService
import com.example.nova.data.NovaRepository
import com.example.nova.model.RiskLevel
import com.example.nova.model.ToolMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ToolRegistry(
    repository: NovaRepository,
    aiService: GeminiAiService
) {
    private val toolsMap = mutableMapOf<String, NovaTool>()
    private val disabledTools = mutableSetOf<String>()

    private val _registeredTools = MutableStateFlow<List<ToolMetadata>>(emptyList())
    val registeredTools: StateFlow<List<ToolMetadata>> = _registeredTools.asStateFlow()

    init {
        // Register Device tools
        register(OpenAppTool())
        register(SystemSettingsTool())
        register(DeviceInfoTool())

        // Register Communication tools (High Risk)
        register(PhoneTool())
        register(SmsTool())
        register(EmailTool())
        register(WhatsAppAutomationTool())
        register(CallAnnouncerTool())

        // Register Security & Vision tools
        register(AntiTheftTool())
        register(CameraVisionTool())

        // Register Productivity tools
        register(TimerTool())
        register(AlarmTool())
        register(CalculatorTool())
        register(NotesTool(repository))
        register(TaskTool(repository))
        register(ReminderTool(repository))

        // Register Web & Navigation tools
        register(WebSearchTool())
        register(NavigationTool())
        register(BrowserTool())

        // Register Media & Language tools
        register(CameraTool())
        register(ImageGenerationTool(aiService))
        register(ImageEditingTool())
        register(WritingTool(aiService))
        register(TranslationTool(aiService))

        updateMetadataList()
    }

    private fun register(tool: NovaTool) {
        toolsMap[tool.id] = tool
    }

    fun getTool(id: String): NovaTool? {
        if (disabledTools.contains(id)) return null
        return toolsMap[id]
    }

    fun isAllowlisted(id: String): Boolean {
        return toolsMap.containsKey(id)
    }

    fun toggleTool(id: String, enabled: Boolean) {
        if (enabled) {
            disabledTools.remove(id)
        } else {
            disabledTools.add(id)
        }
        updateMetadataList()
    }

    private fun updateMetadataList() {
        _registeredTools.value = toolsMap.values.map { tool ->
            ToolMetadata(
                id = tool.id,
                name = tool.name,
                category = tool.category,
                description = tool.description,
                riskLevel = tool.riskLevel,
                requiredPermissions = tool.requiredPermissions,
                isEnabled = !disabledTools.contains(tool.id)
            )
        }
    }
}
