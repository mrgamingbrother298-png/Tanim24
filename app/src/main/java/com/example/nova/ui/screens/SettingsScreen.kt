package com.example.nova.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nova.ui.NovaViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: NovaViewModel,
    onBack: () -> Unit
) {
    val isSpeakingEnabled by viewModel.isVoiceSpeakingEnabled.collectAsState()
    val isAiConfigured = remember { viewModel.aiService.isConfigured() }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = NovaDarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("Settings & Configuration", fontWeight = FontWeight.Bold, color = NovaTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Account & Authentication Status
            val userProfile by viewModel.authManager.userProfile.collectAsState()
            val isFirebaseConfigured by viewModel.authManager.isFirebaseConfigured.collectAsState()

            Card(
                colors = CardDefaults.cardColors(containerColor = NovaSurfaceCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = NovaNeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Personal Account & Cloud Sync", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = NovaTextPrimary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (userProfile != null && userProfile!!.isSignedIn) {
                        Text(
                            text = "Signed in as: ${userProfile!!.displayName ?: userProfile!!.email ?: userProfile!!.uid}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = NovaNeonEmerald
                        )
                        userProfile!!.email?.let {
                            Text(text = it, fontSize = 12.sp, color = NovaTextSecondary)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.authManager.signOut() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                        ) {
                            Text("Sign Out", color = NovaTextPrimary)
                        }
                    } else {
                        Text(
                            text = if (isFirebaseConfigured)
                                "Firebase Auth and Credential Manager are active. Ready for Google Sign-In and secure account synchronization."
                            else
                                "Firebase Auth & Google Credential Manager dependencies are integrated. Connect your google-services.json to sync profiles across devices.",
                            fontSize = 12.sp,
                            color = NovaTextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // AI Configuration Status
            Card(
                colors = CardDefaults.cardColors(containerColor = NovaSurfaceCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = NovaNeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI Provider (Gemini 3.5 Flash)", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = NovaTextPrimary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (isAiConfigured)
                            "Gemini API key is active. Full online reasoning, complex intent planning, and generative tools enabled."
                        else
                            "Running in Autonomous Offline Mode. Local rule engine, calculator, timers, notes, telemetry, and system tools work 100% without internet. Configure GEMINI_API_KEY in Secrets for cloud generative AI.",
                        fontSize = 12.sp,
                        color = NovaTextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }

            // Voice Settings
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
                        Text("Text-to-Speech Voice Output", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = NovaTextPrimary)
                        Text("Audibly speak assistant answers through device speaker", fontSize = 12.sp, color = NovaTextSecondary)
                    }
                    Switch(
                        checked = isSpeakingEnabled,
                        onCheckedChange = { viewModel.toggleVoiceSpeaker(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NovaNeonCyan,
                            checkedTrackColor = NovaNeonCyan.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            // Multi-Language Support
            Card(
                colors = CardDefaults.cardColors(containerColor = NovaSurfaceCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Supported Languages", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = NovaTextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "NOVA auto-detects language in both speech and text:\n" +
                        "• English (Universal)\n" +
                        "• Bengali (বাংলা) — e.g. \"আজ ৮টার জন্য একটা reminder set করো\"\n" +
                        "• Tagalog / Filipino — e.g. \"Kamusta\"\n" +
                        "• Indonesian / Malay\n" +
                        "• Arabic (العربية)",
                        fontSize = 12.sp,
                        color = NovaTextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }

            // Data & History
            Card(
                colors = CardDefaults.cardColors(containerColor = NovaSurfaceCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Data Management", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = NovaTextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { showClearHistoryDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A1525)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = NovaNeonRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear Conversation History", color = NovaNeonRed)
                    }
                }
            }

            // About NOVA AI
            Card(
                colors = CardDefaults.cardColors(containerColor = NovaSurfaceCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("NOVA AI Assistant", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NovaNeonCyan)
                    Text("Version 1.0.0 (Production Build)", fontSize = 12.sp, color = NovaTextMuted)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "A-to-Z Android Personal AI Operating Assistant built on Clean Architecture, Jetpack Compose, Room persistence, and explicit Tool-based device control.",
                        fontSize = 12.sp,
                        color = NovaTextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear Chat History?", color = NovaTextPrimary) },
            text = { Text("This will remove all message logs from local storage.", color = NovaTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearChat()
                        showClearHistoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NovaNeonRed)
                ) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) { Text("Cancel", color = NovaTextSecondary) }
            },
            containerColor = NovaSurfaceCard
        )
    }
}
