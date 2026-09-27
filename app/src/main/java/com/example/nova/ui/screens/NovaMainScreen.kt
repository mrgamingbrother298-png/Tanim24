package com.example.nova.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.nova.model.AssistantState
import com.example.nova.model.MessageEntity
import com.example.nova.model.PendingConfirmation
import com.example.nova.model.RiskLevel
import com.example.nova.ui.NovaViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaMainScreen(
    viewModel: NovaViewModel,
    onNavigateToTools: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToMemory: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val assistantState by viewModel.assistantState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val pendingConfirmation by viewModel.pendingConfirmation.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val isSpeakingEnabled by viewModel.isVoiceSpeakingEnabled.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val quickCommands = listOf(
        "Open YouTube",
        "Set a timer for 15 minutes",
        "Battery info",
        "Calculate 250 × 35",
        "Directions to Dubai Mall",
        "Create note buy groceries",
        "Write a polite message to my manager",
        "Translate hello to Bengali",
        "Open Bluetooth settings",
        "Call Mom"
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("nova_main_screen"),
        containerColor = NovaDarkBackground,
        topBar = {
            NovaTopBar(
                assistantState = assistantState,
                isOnline = isOnline,
                isSpeakingEnabled = isSpeakingEnabled,
                onToggleSpeaker = { viewModel.toggleVoiceSpeaker(!isSpeakingEnabled) },
                onStop = { viewModel.stopSpeaking() },
                onNavigateToTools = onNavigateToTools,
                onNavigateToNotes = onNavigateToNotes,
                onNavigateToMemory = onNavigateToMemory,
                onNavigateToPrivacy = onNavigateToPrivacy,
                onNavigateToSettings = onNavigateToSettings
            )
        },
        bottomBar = {
            NovaBottomInputBar(
                inputText = inputText,
                assistantState = assistantState,
                quickCommands = quickCommands,
                onInputChanged = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        viewModel.processUserCommand(inputText)
                        inputText = ""
                    }
                },
                onMicClick = {
                    if (assistantState == AssistantState.LISTENING) {
                        viewModel.stopListening()
                    } else {
                        viewModel.startListening()
                    }
                },
                onQuickCommandClick = { command ->
                    viewModel.processUserCommand(command)
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Pending High-Risk Confirmation Card
            AnimatedVisibility(visible = pendingConfirmation != null) {
                pendingConfirmation?.let { confirmation ->
                    ConfirmationCard(
                        confirmation = confirmation,
                        onConfirm = { viewModel.confirmPendingAction() },
                        onCancel = { viewModel.cancelPendingAction() }
                    )
                }
            }

            // Interactive NOVA Avatar & State Badge Header
            NovaAssistantStatusHeader(assistantState = assistantState)

            // Conversation Chat Stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    ChatMessageItem(message = message)
                }
            }
        }
    }
}

@Composable
fun NovaTopBar(
    assistantState: AssistantState,
    isOnline: Boolean,
    isSpeakingEnabled: Boolean,
    onToggleSpeaker: () -> Unit,
    onStop: () -> Unit,
    onNavigateToTools: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToMemory: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Surface(
        color = NovaSurfaceDark,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mini logo / Title
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "NOVA AI",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NovaNeonCyan,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isOnline) Color(0x3310B981) else Color(0x33EF4444))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isOnline) "ONLINE" else "OFFLINE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isOnline) NovaNeonEmerald else NovaNeonRed
                        )
                    }
                }
                Text(
                    text = "Personal Operating Assistant",
                    fontSize = 11.sp,
                    color = NovaTextMuted
                )
            }

            // Active Voice Stop button when speaking
            if (assistantState == AssistantState.SPEAKING) {
                IconButton(
                    onClick = onStop,
                    modifier = Modifier.testTag("stop_speaking_button")
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop Speaking", tint = NovaNeonRed)
                }
            }

            // Speaker toggle button
            IconButton(
                onClick = onToggleSpeaker,
                modifier = Modifier.testTag("speaker_toggle_button")
            ) {
                Icon(
                    imageVector = if (isSpeakingEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = "Voice Audio Toggle",
                    tint = if (isSpeakingEnabled) NovaNeonCyan else NovaTextMuted
                )
            }

            // Notes / Tasks
            IconButton(
                onClick = onNavigateToNotes,
                modifier = Modifier.testTag("notes_button")
            ) {
                Icon(Icons.Default.TaskAlt, contentDescription = "Notes and Tasks", tint = NovaTextPrimary)
            }

            // Tools Hub
            IconButton(
                onClick = onNavigateToTools,
                modifier = Modifier.testTag("tools_hub_button")
            ) {
                Icon(Icons.Default.DashboardCustomize, contentDescription = "Tools Hub", tint = NovaNeonPurple)
            }

            // More Menu Dropdown (Memory, Privacy, Settings)
            var showMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.testTag("more_options_button")
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = NovaTextPrimary)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Personal Memory") },
                        leadingIcon = { Icon(Icons.Default.Psychology, contentDescription = null) },
                        onClick = { showMenu = false; onNavigateToMemory() }
                    )
                    DropdownMenuItem(
                        text = { Text("Privacy & Audit Log") },
                        leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) },
                        onClick = { showMenu = false; onNavigateToPrivacy() }
                    )
                    DropdownMenuItem(
                        text = { Text("Settings") },
                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        onClick = { showMenu = false; onNavigateToSettings() }
                    )
                }
            }
        }
    }
}

@Composable
fun NovaAssistantStatusHeader(assistantState: AssistantState) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val (stateText, stateColor, icon) = when (assistantState) {
        AssistantState.IDLE -> Triple("System Ready", NovaNeonCyan, Icons.Default.CheckCircle)
        AssistantState.LISTENING -> Triple("Listening to voice...", NovaNeonPurple, Icons.Default.Mic)
        AssistantState.THINKING -> Triple("Analyzing intent & planning tools...", NovaNeonCyanDim, Icons.Default.AutoAwesome)
        AssistantState.EXECUTING -> Triple("Executing verified tool...", NovaNeonAmber, Icons.Default.FlashOn)
        AssistantState.SPEAKING -> Triple("Speaking response...", NovaNeonEmerald, Icons.AutoMirrored.Filled.VolumeUp)
        AssistantState.ERROR -> Triple("Attention Needed", NovaNeonRed, Icons.Default.Warning)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = NovaSurfaceCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, stateColor.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .scale(if (assistantState != AssistantState.IDLE) pulseScale else 1f)
                        .clip(CircleShape)
                        .background(stateColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = stateColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stateText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = NovaTextPrimary
                )
            }
        }
    }
}

@Composable
fun ConfirmationCard(
    confirmation: PendingConfirmation,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("confirmation_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1128)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, NovaNeonRed)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = "Security Alert",
                    tint = NovaNeonRed,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = confirmation.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = NovaNeonRed
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF5A1827))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "HIGH RISK",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = confirmation.description,
                fontSize = 14.sp,
                color = NovaTextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Target Action: ${confirmation.command.action} | Confirmation required by NOVA Security Policy",
                fontSize = 11.sp,
                color = NovaTextMuted
            )

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NovaTextSecondary),
                    modifier = Modifier.testTag("cancel_confirmation_button")
                ) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = NovaNeonRed),
                    modifier = Modifier.testTag("proceed_confirmation_button")
                ) {
                    Text("Authorize & Execute")
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: MessageEntity) {
    val isUser = message.sender == "user"
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(0.92f),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            if (!isUser) {
                // NOVA mini avatar badge
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(NovaNeonCyan, NovaNeonPurple))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("N", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.Black)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Surface(
                shape = RoundedCornerShape(
                    topStart = if (isUser) 16.dp else 4.dp,
                    topEnd = 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = if (isUser) NovaSurfaceCardElevated else NovaSurfaceCard,
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (isUser) NovaNeonPurple.copy(alpha = 0.4f) else NovaNeonCyan.copy(alpha = 0.25f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = message.content,
                        fontSize = 14.sp,
                        color = NovaTextPrimary,
                        lineHeight = 20.sp
                    )

                    // If a tool result status is attached
                    if (!message.toolType.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF090D16))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (message.toolStatus == "SUCCESS") Icons.Default.CheckCircle else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (message.toolStatus == "SUCCESS") NovaNeonEmerald else NovaNeonAmber,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tool: ${message.toolType.uppercase()}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NovaNeonCyan
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = { clipboardManager.setText(AnnotatedString(message.content)) },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy text", tint = NovaTextMuted, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NovaBottomInputBar(
    inputText: String,
    assistantState: AssistantState,
    quickCommands: List<String>,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
    onQuickCommandClick: (String) -> Unit
) {
    Surface(
        color = NovaSurfaceDark,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 6.dp)
        ) {
            // Horizontal Quick Suggestions
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickCommands) { cmd ->
                    Surface(
                        onClick = { onQuickCommandClick(cmd) },
                        shape = RoundedCornerShape(12.dp),
                        color = NovaSurfaceCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF24324D)),
                        modifier = Modifier.testTag("quick_chip_$cmd")
                    ) {
                        Text(
                            text = cmd,
                            fontSize = 12.sp,
                            color = NovaNeonCyan,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Text Input Field
                TextField(
                    value = inputText,
                    onValueChange = onInputChanged,
                    placeholder = {
                        Text(
                            text = "Ask NOVA (Voice or Text)...",
                            color = NovaTextMuted,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("command_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = NovaSurfaceCard,
                        unfocusedContainerColor = NovaSurfaceCard,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = NovaTextPrimary,
                        unfocusedTextColor = NovaTextPrimary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Microphone Button (Voice Trigger)
                val isListening = assistantState == AssistantState.LISTENING
                IconButton(
                    onClick = onMicClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isListening) NovaNeonRed else NovaNeonPurple)
                        .testTag("voice_mic_button")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Send Button
                IconButton(
                    onClick = onSend,
                    enabled = inputText.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank()) NovaNeonCyan else Color(0xFF1E293B))
                        .testTag("send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Command",
                        tint = if (inputText.isNotBlank()) Color.Black else NovaTextMuted
                    )
                }
            }
        }
    }
}
