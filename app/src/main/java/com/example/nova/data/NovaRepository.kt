package com.example.nova.data

import com.example.nova.model.*
import kotlinx.coroutines.flow.Flow

class NovaRepository(private val dao: NovaDao) {
    val messages: Flow<List<MessageEntity>> = dao.getAllMessages()
    val memories: Flow<List<MemoryEntity>> = dao.getAllMemories()
    val notes: Flow<List<NoteEntity>> = dao.getAllNotes()
    val tasks: Flow<List<TaskEntity>> = dao.getAllTasks()
    val reminders: Flow<List<ReminderEntity>> = dao.getAllReminders()
    val logs: Flow<List<ToolLogEntity>> = dao.getRecentLogs()

    suspend fun addMessage(message: MessageEntity): Long = dao.insertMessage(message)
    suspend fun clearMessages() = dao.clearMessages()

    suspend fun addMemory(key: String, value: String, category: String = "general"): Long =
        dao.insertMemory(MemoryEntity(key = key, value = value, category = category))

    suspend fun deleteMemory(id: Long) = dao.deleteMemory(id)
    suspend fun clearMemories() = dao.clearMemories()

    suspend fun addNote(title: String, content: String, category: String = "General"): Long =
        dao.insertNote(NoteEntity(title = title, content = content, category = category))

    suspend fun deleteNote(id: Long) = dao.deleteNote(id)

    suspend fun addTask(title: String, priority: String = "Medium"): Long =
        dao.insertTask(TaskEntity(title = title, priority = priority))

    suspend fun toggleTask(task: TaskEntity) =
        dao.updateTask(task.copy(isCompleted = !task.isCompleted))

    suspend fun deleteTask(id: Long) = dao.deleteTask(id)

    suspend fun addReminder(title: String, timeFormatted: String, timestampMillis: Long): Long =
        dao.insertReminder(ReminderEntity(title = title, timeFormatted = timeFormatted, timestampMillis = timestampMillis))

    suspend fun deleteReminder(id: Long) = dao.deleteReminder(id)

    suspend fun logToolExecution(toolName: String, action: String, status: String, message: String, timeMs: Long) =
        dao.insertLog(ToolLogEntity(
            toolName = toolName,
            commandAction = action,
            status = status,
            message = message,
            executionTimeMs = timeMs
        ))
}
