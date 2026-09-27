package com.example.nova.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nova.model.RiskLevel
import com.example.nova.model.ToolMetadata
import com.example.nova.ui.NovaViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsDashboardScreen(
    viewModel: NovaViewModel,
    onBack: () -> Unit
) {
    val tools by viewModel.registeredTools.collectAsState()
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Device", "Communication", "Productivity", "Information", "Language", "Creative")

    val filteredTools = if (selectedCategory == "All") {
        tools
    } else {
        tools.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    Scaffold(
        containerColor = NovaDarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Connected Tools Registry", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NovaTextPrimary)
                        Text("${tools.count { it.isEnabled }} active of ${tools.size} registered tools", fontSize = 12.sp, color = NovaTextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("tools_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NovaNeonCyan)
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
        ) {
            // Category Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NovaNeonCyan.copy(alpha = 0.2f),
                            selectedLabelColor = NovaNeonCyan,
                            containerColor = NovaSurfaceCard,
                            labelColor = NovaTextSecondary
                        )
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredTools, key = { it.id }) { tool ->
                    ToolItemCard(
                        tool = tool,
                        onToggle = { isChecked -> viewModel.toggleTool(tool.id, isChecked) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun ToolItemCard(
    tool: ToolMetadata,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tool_card_${tool.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NovaSurfaceCard),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (tool.isEnabled) NovaBorderGlow else Color(0xFF1E293B)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tool.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (tool.isEnabled) NovaTextPrimary else NovaTextMuted
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Category badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1E293B))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(tool.category, fontSize = 9.sp, color = NovaNeonCyanDim)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Risk badge
                    val riskColor = when (tool.riskLevel) {
                        RiskLevel.HIGH -> NovaNeonRed
                        RiskLevel.MEDIUM -> NovaNeonAmber
                        RiskLevel.LOW -> NovaNeonEmerald
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(riskColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = tool.riskLevel.name,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = riskColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tool.description,
                    fontSize = 12.sp,
                    color = NovaTextSecondary,
                    lineHeight = 16.sp
                )

                if (tool.requiredPermissions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Requires: ${tool.requiredPermissions.joinToString { it.substringAfterLast('.') }}",
                        fontSize = 10.sp,
                        color = NovaNeonPurple
                    )
                }
            }

            Switch(
                checked = tool.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NovaNeonCyan,
                    checkedTrackColor = NovaNeonCyan.copy(alpha = 0.3f),
                    uncheckedThumbColor = NovaTextMuted,
                    uncheckedTrackColor = Color(0xFF0F172A)
                ),
                modifier = Modifier.testTag("toggle_${tool.id}")
            )
        }
    }
}
