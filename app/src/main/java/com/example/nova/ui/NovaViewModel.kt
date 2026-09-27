package com.example.nova.ui

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nova.data.*
import com.example.nova.engine.*
import com.example.nova.model.*
import com.example.nova.tools.ToolRegistry
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NovaViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val database = NovaDatabase.getDatabase(context)
    val repository = NovaRepository(database.novaDao())
    val aiService = GeminiAiService()
    val toolRegistry = ToolRegistry(repository, aiService)
    val securityValidator = CommandSecurityValidator(toolRegistry)
    val memoryManager = MemoryManager(repository)
    val authManager = AuthManager(context)

    private val commandEngine = UniversalCommandEngine(
        context = context,
        registry = toolRegistry,
        securityValidator = securityValidator,
        aiService = aiService,
        repository = repository
    )

    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<PendingConfirmation?>(null)
    val pendingConfirmation: StateFlow<PendingConfirmation?> = _pendingConfirmation.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    val messages = repository.messages.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val notes = repository.notes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val tasks = repository.tasks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val reminders = repository.reminders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val memories = repository.memories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val toolLogs = repository.logs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val registeredTools = toolRegistry.registeredTools

    private val voiceEngine: VoiceEngine = VoiceEngine(
        context = context,
        onSpeechRecognized = { spokenText ->
            processUserCommand(spokenText)
        },
        onStateChanged = { newState ->
            _assistantState.value = newState
        }
    )

    val isVoiceSpeakingEnabled = voiceEngine.isSpeakingEnabled

    init {
        updateNetworkState()
        // Welcome message if messages is empty
        viewModelScope.launch {
            if (database.novaDao().getRecentLogs(1).first().isEmpty() && database.novaDao().getAllMessages().first().isEmpty()) {
                repository.addMessage(
                    MessageEntity(
                        sender = "nova",
                        content = "Hey Tanim... Aaiyra is online. Ready to keep you organized and entertained? Speak or type, handsome.",
                        toolType = "system_status",
                        toolStatus = "SUCCESS"
                    )
                )
            }
        }
    }

    fun updateNetworkState() {
        val cm = context.getSystemService(Application.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(net)
        _isOnline.value = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    fun startListening() {
        _assistantState.value = AssistantState.LISTENING
        voiceEngine.startListening()
    }

    fun stopListening() {
        voiceEngine.stopListening()
        _assistantState.value = AssistantState.IDLE
    }

    fun stopSpeaking() {
        voiceEngine.stopSpeaking()
        _assistantState.value = AssistantState.IDLE
    }

    fun toggleVoiceSpeaker(enabled: Boolean) {
        voiceEngine.toggleSpeaking(enabled)
    }

    fun processUserCommand(input: String) {
        if (input.isBlank()) return
        updateNetworkState()

        viewModelScope.launch {
            // Save user message
            repository.addMessage(
                MessageEntity(sender = "user", content = input)
            )

            _assistantState.value = AssistantState.THINKING

            val summary = commandEngine.processInput(
                userInput = input,
                onConfirmationNeeded = { confirmation ->
                    _pendingConfirmation.value = confirmation
                }
            )

            _assistantState.value = AssistantState.EXECUTING

            // Save NOVA's response
            val toolType = summary.results.firstOrNull()?.data?.get("app")
                ?: summary.results.firstOrNull()?.data?.get("setting")
                ?: summary.results.firstOrNull()?.data?.get("expression")
                ?: summary.results.firstOrNull()?.status?.name

            val status = if (summary.success) "SUCCESS" else "FAILED"

            repository.addMessage(
                MessageEntity(
                    sender = "nova",
                    content = summary.responseMessage,
                    toolType = toolType,
                    toolStatus = status
                )
            )

            // Voice response
            if (voiceEngine.isSpeakingEnabled.value) {
                _assistantState.value = AssistantState.SPEAKING
                voiceEngine.speak(summary.responseMessage)
            } else {
                _assistantState.value = AssistantState.IDLE
            }
        }
    }

    fun confirmPendingAction() {
        val pending = _pendingConfirmation.value ?: return
        _pendingConfirmation.value = null

        viewModelScope.launch {
            _assistantState.value = AssistantState.EXECUTING
            val result = commandEngine.executeConfirmed(pending.command)

            val status = if (result.status == ToolStatus.SUCCESS) "SUCCESS" else "FAILED"
            repository.addMessage(
                MessageEntity(
                    sender = "nova",
                    content = "✅ Confirmed: ${result.message}",
                    toolType = pending.command.action,
                    toolStatus = status
                )
            )

            if (voiceEngine.isSpeakingEnabled.value) {
                voiceEngine.speak(result.message)
            }
            _assistantState.value = AssistantState.IDLE
        }
    }

    fun cancelPendingAction() {
        val pending = _pendingConfirmation.value ?: return
        _pendingConfirmation.value = null
        viewModelScope.launch {
            repository.addMessage(
                MessageEntity(
                    sender = "nova",
                    content = "❌ Action \"${pending.actionName}\" was cancelled by user.",
                    toolType = pending.command.action,
                    toolStatus = "CANCELLED"
                )
            )
            _assistantState.value = AssistantState.IDLE
        }
    }

    fun toggleTool(toolId: String, enabled: Boolean) {
        toolRegistry.toggleTool(toolId, enabled)
    }

    fun deleteNote(id: Long) = viewModelScope.launch { repository.deleteNote(id) }
    fun toggleTask(task: TaskEntity) = viewModelScope.launch { repository.toggleTask(task) }
    fun deleteTask(id: Long) = viewModelScope.launch { repository.deleteTask(id) }
    fun deleteReminder(id: Long) = viewModelScope.launch { repository.deleteReminder(id) }

    fun addMemory(key: String, value: String, category: String) =
        viewModelScope.launch { memoryManager.saveMemory(key, value, category) }

    fun deleteMemory(id: Long) = viewModelScope.launch { memoryManager.deleteMemory(id) }
    fun clearMemories() = viewModelScope.launch { memoryManager.clearAll() }
    fun toggleMemory(enabled: Boolean) = memoryManager.setMemoryEnabled(enabled)

    fun clearChat() = viewModelScope.launch { repository.clearMessages() }

    override fun onCleared() {
        super.onCleared()
        voiceEngine.destroy()
    }
}
