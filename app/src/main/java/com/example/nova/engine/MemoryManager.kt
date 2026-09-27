package com.example.nova.engine

import com.example.nova.data.NovaRepository
import com.example.nova.model.MemoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MemoryManager(private val repository: NovaRepository) {

    private val _isMemoryEnabled = MutableStateFlow(true)
    val isMemoryEnabled: StateFlow<Boolean> = _isMemoryEnabled.asStateFlow()

    val memories: Flow<List<MemoryEntity>> = repository.memories

    fun setMemoryEnabled(enabled: Boolean) {
        _isMemoryEnabled.value = enabled
    }

    suspend fun saveMemory(key: String, value: String, category: String = "general"): Boolean {
        if (!_isMemoryEnabled.value) return false
        repository.addMemory(key, value, category)
        return true
    }

    suspend fun deleteMemory(id: Long) {
        repository.deleteMemory(id)
    }

    suspend fun clearAll() {
        repository.clearMemories()
    }
}
