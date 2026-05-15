package com.ganesh.hisabkitabpro.ui.ai

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ganesh.hisabkitabpro.R
import com.ganesh.hisabkitabpro.domain.ai.AIResponse
import com.ganesh.hisabkitabpro.domain.voice.SpeechRecognizerManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatScreen(
    assistantSampleCustomerNames: List<String> = emptyList(),
    onOpenCustomersClick: () -> Unit = {},
    onSendMessage: (String) -> Unit,
    chatMessages: List<ChatMessage>,
    onActionClick: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val primarySampleName = assistantSampleCustomerNames.firstOrNull()
    val secondSampleName = assistantSampleCustomerNames.getOrNull(1) ?: primarySampleName
    val thirdSampleName = assistantSampleCustomerNames.getOrNull(2) ?: primarySampleName
    val scrollChips = rememberScrollState()
    var textState by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var isAITyping by remember { mutableStateOf(false) }
    var pendingActionRoute by remember { mutableStateOf<String?>(null) }
    var sensitiveConfirmText by remember { mutableStateOf("") }
    var showMicDisclosureDialog by remember { mutableStateOf(false) }

    fun prettyActionName(route: String): String {
        return when {
            route.startsWith("customer_ledger/") -> "Open Customer Ledger"
            route == "settings" -> "Open Settings"
            route == "business_insights" -> "Open Business Insights"
            route == "customers" -> "Open Customers"
            route == "suppliers" -> "Open Suppliers"
            route == "ai_pilot" -> "Open Business Insights"
            else -> "Run AI Suggested Action"
        }
    }

    fun isSensitiveAction(route: String): Boolean {
        return route !in setOf(
            "settings",
            "business_insights",
            "customers",
            "suppliers"
        ) && !route.startsWith("customer_ledger/")
    }

    fun actionRiskLabel(route: String): String {
        return when {
            !isSensitiveAction(route) -> "Safe: Navigation only"
            else -> "Sensitive: Verify before continuing"
        }
    }

    val speechManager = remember {
        SpeechRecognizerManager(
            context = context,
            onResult = { result ->
                isListening = false
                if (result.isNotBlank()) onSendMessage(result)
            },
            onError = { 
                isListening = false
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
        )
    }
    DisposableEffect(Unit) {
        onDispose {
            speechManager.destroy()
        }
    }

    // --- 🛡️ SCDS AUTO-PERMISSION GUARD ---
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isListening = true
            speechManager.startListening()
        } else {
            Toast.makeText(context, "Voice access denied. Please allow in settings.", Toast.LENGTH_LONG).show()
        }
    }

    fun handleMicClick() {
        if (isListening) {
            speechManager.stopListening()
            isListening = false
        } else {
            val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                isListening = true
                speechManager.startListening()
            } else {
                showMicDisclosureDialog = true
            }
        }
    }

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty() && chatMessages.last().isUser) {
            isAITyping = true
        } else {
            isAITyping = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI Khata Assistant", fontWeight = FontWeight.ExtraBold, color = colorScheme.primary)
                        Text(
                            text = "Stable assistant — ledger-safe suggestions & voice",
                            fontSize = 11.sp,
                            color = colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.surface,
                    titleContentColor = colorScheme.primary,
                    navigationIconContentColor = colorScheme.primary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(colorScheme.background)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Quick commands — your customers",
                        color = colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = when {
                            primarySampleName == null ->
                                "Customers tab se pehle ग्राहक जोड़ें; phir usi naam se likhein: naam ko 500 add karo | naam ka bill clear | reminder. Zyada kaam: ek line me | (pipe) se alag karein."
                            assistantSampleCustomerNames.size == 1 ->
                                "Jaise: $primarySampleName ko 500 add karo | $primarySampleName ka bill clear karo | $primarySampleName ko reminder bhejo"
                            else ->
                                "Apke ${assistantSampleCustomerNames.size} customers me se kisi naam se — misaal: $primarySampleName, $secondSampleName… Ek saath kai commands: | ya alag line."
                        },
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (primarySampleName == null) {
                        TextButton(onClick = onOpenCustomersClick, contentPadding = PaddingValues(0.dp)) {
                            Text("Customers खोलकर जोड़ें", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Row(
                            modifier = Modifier.horizontalScroll(scrollChips),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SuggestionChip(
                                onClick = { onSendMessage("$primarySampleName ko 500 add karo") },
                                label = { Text("Ledger +500", fontSize = 11.sp) }
                            )
                            SuggestionChip(
                                onClick = {
                                    val n = secondSampleName ?: primarySampleName
                                    onSendMessage("$n ka bill clear karo")
                                },
                                label = { Text("Bill clear", fontSize = 11.sp) }
                            )
                            SuggestionChip(
                                onClick = {
                                    val n = thirdSampleName ?: primarySampleName
                                    onSendMessage("$n ko reminder bhejo")
                                },
                                label = { Text("Reminder", fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                reverseLayout = true,
                verticalArrangement = Arrangement.Top
            ) {
                if (isAITyping) {
                    item { TypingAnimationItem() }
                }
                items(chatMessages.reversed()) { message ->
                    ChatBubble(
                        message = message,
                        onActionClick = { route -> pendingActionRoute = route }
                    )
                }
            }

            Surface(
                tonalElevation = 12.dp,
                shadowElevation = 12.dp,
                color = colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column {
                    if (isListening) {
                        VoiceWaveformUI()
                    }
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FloatingActionButton(
                            onClick = { handleMicClick() },
                            containerColor = if (isListening) colorScheme.error else colorScheme.primary,
                            contentColor = colorScheme.onPrimary,
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(if (isListening) Icons.Default.Stop else Icons.Default.Mic, contentDescription = null)
                        }

                        Spacer(Modifier.width(12.dp))

                        OutlinedTextField(
                            value = textState,
                            onValueChange = { textState = it },
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text(
                                    text = if (primarySampleName != null) {
                                        "जैसे: $primarySampleName ka balance / $primarySampleName ko 500 add…"
                                    } else {
                                        "Customer ka naam + kaam (balance, 500 add, bill clear…)"
                                    },
                                    fontSize = 13.sp
                                )
                            },
                            shape = RoundedCornerShape(28.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colorScheme.primary,
                                unfocusedBorderColor = colorScheme.outline
                            )
                        )

                        Spacer(Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (textState.isNotBlank()) {
                                    onSendMessage(textState)
                                    textState = ""
                                }
                            },
                            enabled = textState.isNotBlank()
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                tint = if (textState.isNotBlank()) colorScheme.primary else colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    pendingActionRoute?.let { route ->
        val actionName = prettyActionName(route)
        val riskLabel = actionRiskLabel(route)
        val sensitive = isSensitiveAction(route)
        val canContinue = !sensitive || sensitiveConfirmText.trim().uppercase() == "CONFIRM"
        AlertDialog(
            onDismissRequest = {
                pendingActionRoute = null
                sensitiveConfirmText = ""
            },
            confirmButton = {
                Button(
                    onClick = {
                        onActionClick(route)
                        pendingActionRoute = null
                        sensitiveConfirmText = ""
                    },
                    enabled = canContinue
                ) { Text("Continue") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingActionRoute = null
                        sensitiveConfirmText = ""
                    }
                ) { Text("Cancel") }
            },
            title = { Text("Confirm AI Action") },
            text = {
                Column {
                    Text("$actionName\n$riskLabel\n\nTarget: $route")
                    if (sensitive) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Type CONFIRM to proceed.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = sensitiveConfirmText,
                            onValueChange = { sensitiveConfirmText = it.take(16) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("CONFIRM") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters
                            )
                        )
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Continue?")
                    }
                }
            }
        )
    }

    if (showMicDisclosureDialog) {
        AlertDialog(
            onDismissRequest = { showMicDisclosureDialog = false },
            title = { Text(stringResource(R.string.permission_microphone_title)) },
            text = { Text(stringResource(R.string.permission_microphone_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showMicDisclosureDialog = false
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                ) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showMicDisclosureDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun TypingAnimationItem() {
    Row(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val infiniteTransition = rememberInfiniteTransition(label = "typing")
            val dy by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -10f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400, delayMillis = index * 100, easing = LinearOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot"
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size(6.dp)
                    .offset(y = dy.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}

@Composable
fun VoiceWaveformUI() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(15) {
            val infiniteTransition = rememberInfiniteTransition(label = "wave")
            val height by infiniteTransition.animateFloat(
                initialValue = 5f,
                targetValue = 25f,
                animationSpec = infiniteRepeatable(
                    animation = tween(300, delayMillis = (it * 50) % 200),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar"
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(height.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val aiResponse: AIResponse? = null,
    val riskBadge: String? = null
)

@Composable
fun ChatBubble(message: ChatMessage, onActionClick: (String) -> Unit) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val shape = if (message.isUser) {
        RoundedCornerShape(topStart = 20.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
    }
    
    val containerColor = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val contentColor = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalAlignment = alignment) {
        Surface(
            shape = shape,
            color = containerColor,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (!message.isUser && !message.riskBadge.isNullOrBlank()) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(message.riskBadge, fontSize = 11.sp) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(text = message.text, color = contentColor, fontSize = 15.sp, lineHeight = 20.sp)
                
                message.aiResponse?.let { response ->
                    if (response.actions.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        response.actions.forEach { action ->
                            Button(
                                onClick = { onActionClick(action.route) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(action.label, color = contentColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
