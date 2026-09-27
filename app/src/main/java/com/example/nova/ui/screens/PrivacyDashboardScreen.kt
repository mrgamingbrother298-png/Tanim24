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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nova.model.ToolLogEntity
import com.example.nova.ui.NovaViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyDashboardScreen(
    viewModel: NovaViewModel,
    onBack: () -> Unit
) {
    val toolLogs by viewModel.toolLogs.collectAsState()

    Scaffold(
        containerColor = NovaDarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("Privacy & Security Audit", fontWeight = FontWeight.Bold, color = NovaTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("privacy_back_button")) {
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
                .padding(16.dp)
        ) {
            // Security Firewall Principles
            Card(
                colors = CardDefaults.cardColors(containerColor = NovaSurfaceCard),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NovaNeonEmerald.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = NovaNeonEmerald)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Active Security Firewall", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NovaNeonEmerald)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "• Unrestricted OS/Shell access is permanently disabled.\n" +
                        "• High-risk actions (SMS, Phone calls, File deletion) require explicit user confirmation.\n" +
                        "• All device capabilities execute strictly through official Android APIs.\n" +
                        "• Offline-first design allows local operations without cloud telemetry.",
                        fontSize = 12.sp,
                        color = NovaTextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            Text("Tool Execution Audit Trail (${toolLogs.size})", fontWeight = FontWeight.Bold, color = NovaTextSecondary, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))

            if (toolLogs.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No tool actions executed yet.", color = NovaTextMuted, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(toolLogs, key = { it.id }) { log ->
                        AuditLogItem(log = log)
                    }
                }
            }
        }
    }
}

@Composable
fun AuditLogItem(log: ToolLogEntity) {
    val dateStr = remember(log.timestamp) {
        SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date(log.timestamp))
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = NovaSurfaceCard),
        modifier = Modifier.fillMaxWidth().testTag("audit_log_${log.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = log.toolName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = NovaTextPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (log.status == "SUCCESS") NovaNeonEmerald.copy(alpha = 0.2f) else NovaNeonAmber.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = log.status,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (log.status == "SUCCESS") NovaNeonEmerald else NovaNeonAmber
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = log.message, fontSize = 11.sp, color = NovaTextSecondary, maxLines = 2)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(text = dateStr, fontSize = 10.sp, color = NovaTextMuted)
                Text(text = "${log.executionTimeMs}ms", fontSize = 10.sp, color = NovaNeonCyanDim)
            }
        }
    }
}
