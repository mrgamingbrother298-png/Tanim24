package com.example.nova.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nova.model.MemoryEntity
import com.example.nova.ui.NovaViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(
    viewModel: NovaViewModel,
    onBack: () -> Unit
) {
    val memories by viewModel.memories.collectAsState()
    val isMemoryEnabled by viewModel.memoryManager.isMemoryEnabled.collectAsState()

    var newKey by remember { mutableStateOf("") }
    var newValue by remember { mutableStateOf("") }

    Scaffold(
        containerColor = NovaDarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("Personal Memory Manager", fontWeight = FontWeight.Bold, color = NovaTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("memory_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NovaNeonCyan)
                    }
                },
                actions = {
                    if (memories.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearMemories() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All Memories", tint = NovaNeonRed)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NovaSurfaceDark)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Enable / Disable Memory Card
            Card(
                colors = CardDefaults.cardColors(containerColor = NovaSurfaceCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Personal Assistant Memory", fontWeight = FontWeight.SemiBold, color = NovaTextPrimary, fontSize = 15.sp)
                        Text(
                            "Allows NOVA to remember preferences and authorized user facts locally. Never shared with third parties.",
                            fontSize = 12.sp,
                            color = NovaTextSecondary
                        )
                    }
                    Switch(
                        checked = isMemoryEnabled,
                        onCheckedChange = { viewModel.toggleMemory(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NovaNeonCyan,
                            checkedTrackColor = NovaNeonCyan.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add Manual Memory Entry
            Card(
                colors = CardDefaults.cardColors(containerColor = NovaSurfaceCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Add Memory Fact", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = NovaNeonCyan)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = newKey,
                            onValueChange = { newKey = it },
                            placeholder = { Text("Topic (e.g. Favorite Editor)", color = NovaTextMuted, fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = NovaTextPrimary,
                                unfocusedTextColor = NovaTextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = newValue,
                            onValueChange = { newValue = it },
                            placeholder = { Text("Value (e.g. Android Studio)", color = NovaTextMuted, fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = NovaTextPrimary,
                                unfocusedTextColor = NovaTextPrimary
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (newKey.isNotBlank() && newValue.isNotBlank()) {
                                viewModel.addMemory(newKey.trim(), newValue.trim(), "user_fact")
                                newKey = ""
                                newValue = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NovaNeonCyan, contentColor = Color.Black),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Save Memory")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Stored Memories (${memories.size})", fontWeight = FontWeight.Bold, color = NovaTextSecondary, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))

            if (memories.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No personal memories stored yet.", color = NovaTextMuted, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(memories, key = { it.id }) { memory ->
                        MemoryItemCard(memory = memory, onDelete = { viewModel.deleteMemory(memory.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun MemoryItemCard(memory: MemoryEntity, onDelete: () -> Unit) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = NovaSurfaceCard),
        modifier = Modifier.fillMaxWidth().testTag("memory_item_${memory.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = memory.key, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = NovaNeonCyan)
                Text(text = memory.value, fontSize = 13.sp, color = NovaTextPrimary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Memory", tint = NovaNeonRed)
            }
        }
    }
}
