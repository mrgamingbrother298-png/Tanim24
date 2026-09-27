package com.example.nova.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nova.model.NoteEntity
import com.example.nova.model.ReminderEntity
import com.example.nova.model.TaskEntity
import com.example.nova.ui.NovaViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesTasksScreen(
    viewModel: NovaViewModel,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Tasks", "Notes", "Reminders")

    val tasks by viewModel.tasks.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val reminders by viewModel.reminders.collectAsState()

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = NovaDarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("Productivity Hub", fontWeight = FontWeight.Bold, color = NovaTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("productivity_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NovaNeonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NovaSurfaceDark)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) showAddTaskDialog = true else if (selectedTab == 1) showAddNoteDialog = true
                },
                containerColor = NovaNeonCyan,
                contentColor = Color.Black,
                modifier = Modifier.testTag("fab_add_item")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = NovaSurfaceDark,
                contentColor = NovaNeonCyan
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            when (selectedTab) {
                0 -> TasksListTab(tasks = tasks, onToggle = { viewModel.toggleTask(it) }, onDelete = { viewModel.deleteTask(it) })
                1 -> NotesListTab(notes = notes, onDelete = { viewModel.deleteNote(it) })
                2 -> RemindersListTab(reminders = reminders, onDelete = { viewModel.deleteReminder(it) })
            }
        }
    }

    if (showAddTaskDialog) {
        var title by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = { Text("Add New Task", color = NovaTextPrimary) },
            text = {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Task description...", color = NovaTextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = NovaTextPrimary,
                        unfocusedTextColor = NovaTextPrimary
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.processUserCommand("add $title to my tasks")
                            showAddTaskDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NovaNeonCyan, contentColor = Color.Black)
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddTaskDialog = false }) { Text("Cancel", color = NovaTextSecondary) }
            },
            containerColor = NovaSurfaceCard
        )
    }

    if (showAddNoteDialog) {
        var title by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = { Text("Create Note", color = NovaTextPrimary) },
            text = {
                Column {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("Title", color = NovaTextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = NovaTextPrimary,
                            unfocusedTextColor = NovaTextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        placeholder = { Text("Content...", color = NovaTextMuted) },
                        modifier = Modifier.height(100.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = NovaTextPrimary,
                            unfocusedTextColor = NovaTextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (content.isNotBlank()) {
                            viewModel.processUserCommand("create note $title: $content")
                            showAddNoteDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NovaNeonCyan, contentColor = Color.Black)
                ) { Text("Save Note") }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoteDialog = false }) { Text("Cancel", color = NovaTextSecondary) }
            },
            containerColor = NovaSurfaceCard
        )
    }
}

@Composable
fun TasksListTab(
    tasks: List<TaskEntity>,
    onToggle: (TaskEntity) -> Unit,
    onDelete: (Long) -> Unit
) {
    if (tasks.isEmpty()) {
        EmptyStateView(message = "No tasks yet. Say \"Add buy groceries to my task list\"")
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tasks, key = { it.id }) { task ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = NovaSurfaceCard),
                    modifier = Modifier.fillMaxWidth().testTag("task_item_${task.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = task.isCompleted,
                            onCheckedChange = { onToggle(task) },
                            colors = CheckboxDefaults.colors(checkedColor = NovaNeonCyan)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = task.title,
                            fontSize = 14.sp,
                            color = if (task.isCompleted) NovaTextMuted else NovaTextPrimary,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onDelete(task.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Task", tint = NovaNeonRed)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotesListTab(
    notes: List<NoteEntity>,
    onDelete: (Long) -> Unit
) {
    if (notes.isEmpty()) {
        EmptyStateView(message = "No notes saved. Say \"Create note: idea for project\"")
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(notes, key = { it.id }) { note ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = NovaSurfaceCard),
                    modifier = Modifier.fillMaxWidth().testTag("note_item_${note.id}")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = note.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = NovaNeonCyan,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onDelete(note.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Note", tint = NovaTextMuted)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = note.content, fontSize = 13.sp, color = NovaTextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun RemindersListTab(
    reminders: List<ReminderEntity>,
    onDelete: (Long) -> Unit
) {
    if (reminders.isEmpty()) {
        EmptyStateView(message = "No reminders scheduled. Say \"Remind me in 30 minutes\"")
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(reminders, key = { it.id }) { rem ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = NovaSurfaceCard),
                    modifier = Modifier.fillMaxWidth().testTag("reminder_item_${rem.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Alarm, contentDescription = null, tint = NovaNeonAmber)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = rem.title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = NovaTextPrimary)
                            Text(text = rem.timeFormatted, fontSize = 12.sp, color = NovaTextSecondary)
                        }
                        IconButton(onClick = { onDelete(rem.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Reminder", tint = NovaNeonRed)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = NovaTextMuted,
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
