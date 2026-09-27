package com.example.nova.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String, // "user" or "nova"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val toolType: String? = null,
    val toolStatus: String? = null,
    val toolDataJson: String? = null
)

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String,
    val value: String,
    val category: String = "general", // "preference", "user_fact", "assistant_config"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val category: String = "General",
    val isPinned: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val isCompleted: Boolean = false,
    val priority: String = "Medium", // "Low", "Medium", "High"
    val dueDate: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val timeFormatted: String,
    val timestampMillis: Long,
    val isTriggered: Boolean = false
)

@Entity(tableName = "tool_logs")
data class ToolLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val toolName: String,
    val commandAction: String,
    val status: String,
    val message: String,
    val executionTimeMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)
