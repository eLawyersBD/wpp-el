package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.ChatMessage
import com.example.data.model.CustomerChat
import com.example.data.model.QuickReplyTemplate
import com.example.ui.theme.InboundBubbleDark
import com.example.ui.theme.InboundBubbleLight
import com.example.ui.theme.OutboundBubbleDark
import com.example.ui.theme.OutboundBubbleLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun ChatThreadScreen(
    chat: CustomerChat,
    messages: List<ChatMessage>,
    templates: List<QuickReplyTemplate>,
    isLiveSyncActive: Boolean,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onSendMessage: (text: String, attachmentType: String, attachmentUri: String, attachmentMeta: String) -> Unit,
    onSimulateReply: () -> Unit,
    onSaveCrm: (labels: List<String>, notes: String, isPinned: Boolean, isArchived: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (showBackButton) {
        BackHandler(onBack = onBack)
    }

    var messageInput by remember { mutableStateOf("") }
    var showQuickRepliesBar by remember { mutableStateOf(false) }
    var showAttachmentsRow by remember { mutableStateOf(false) }
    var showCrmDialog by remember { mutableStateOf(false) }
    var isRecordingVoice by remember { mutableStateOf(false) }
    var voiceSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(isRecordingVoice) {
        if (isRecordingVoice) {
            voiceSeconds = 1
            while (isRecordingVoice) {
                delay(1000L)
                voiceSeconds++
            }
        }
    }

    // Zero-permission Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onSendMessage(
                messageInput.ifBlank { "Shared product image" },
                "IMAGE",
                uri.toString(),
                "Photo Attachment"
            )
            messageInput = ""
            showAttachmentsRow = false
        }
    }

    // Zero-permission SAF Document Picker
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Business_Document.pdf"
            onSendMessage(
                messageInput.ifBlank { "Attached document: $fileName" },
                "DOCUMENT",
                uri.toString(),
                "$fileName • Verified PDF"
            )
            messageInput = ""
            showAttachmentsRow = false
        }
    }

    val matchingTemplates = remember(messageInput, templates, showQuickRepliesBar) {
        if (messageInput.startsWith("/")) {
            val query = messageInput.lowercase()
            templates.filter {
                it.shortcut.lowercase().contains(query) || it.title.lowercase().contains(query.removePrefix("/"))
            }
        } else if (showQuickRepliesBar) {
            templates
        } else {
            emptyList()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Conversation Header Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showBackButton) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("chat_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Inbox"
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    val avatarColor = try {
                        Color(android.graphics.Color.parseColor(chat.avatarColor))
                    } catch (_: Exception) {
                        MaterialTheme.colorScheme.primary
                    }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(avatarColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chat.customerName.take(2).uppercase(),
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showCrmDialog = true }
                    ) {
                        Text(
                            text = chat.customerName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (isLiveSyncActive) Color(0xFF10B981) else Color(0xFFF59E0B),
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "${chat.customerPhone} • ${if (isLiveSyncActive) "Live Sync" else "Offline Queue"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onSimulateReply,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("simulate_reply_in_thread_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sim Reply", style = MaterialTheme.typography.labelSmall)
                    }

                    IconButton(
                        onClick = { showCrmDialog = true },
                        modifier = Modifier.testTag("open_crm_sheet_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContactPage,
                            contentDescription = "Customer CRM Notes and Labels",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Quick CRM Bar summary
                if (chat.labels.isNotEmpty() || chat.crmNotes.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                            .clickable { showCrmDialog = true }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        chat.labels.forEach { label ->
                            CrmLabelBadge(label = label)
                        }
                        if (chat.crmNotes.isNotBlank()) {
                            Text(
                                text = "CRM: ${chat.crmNotes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Messages List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatMessageBubble(message = msg)
            }
        }

        // Floating Quick Reply /shortcut Drawer
        AnimatedVisibility(visible = matchingTemplates.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "QUICK REPLY TEMPLATES (Tap to insert)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        matchingTemplates.forEach { tpl ->
                            Card(
                                modifier = Modifier
                                    .widthIn(max = 260.dp)
                                    .clickable {
                                        messageInput = tpl.body.replace("{{customer_name}}", chat.customerName)
                                        showQuickRepliesBar = false
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "${tpl.shortcut} • ${tpl.title}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = tpl.body,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Rich Attachment Selector Drawer (Photos, Documents, Sample Catalog, Voice Note)
        AnimatedVisibility(visible = showAttachmentsRow) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick Photo")
                    }

                    OutlinedButton(
                        onClick = {
                            documentPickerLauncher.launch(arrayOf("application/pdf", "*/*"))
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Attach File")
                    }

                    OutlinedButton(
                        onClick = {
                            onSendMessage(
                                "Attached our latest B2B Wholesale Catalog & Price Sheet PDF.",
                                "DOCUMENT",
                                "content://bizsync/catalog_2026.pdf",
                                "Wholesale_Catalog_2026.pdf • 1.4 MB"
                            )
                            showAttachmentsRow = false
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send B2B Catalog PDF")
                    }
                }
            }
        }

        // Composer Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            if (isRecordingVoice) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = String.format(Locale.US, "Recording Voice Note… 0:%02d", voiceSeconds),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { isRecordingVoice = false }) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val durationFormatted = String.format(Locale.US, "0:%02d", voiceSeconds.coerceAtLeast(1))
                                isRecordingVoice = false
                                onSendMessage(
                                    "Voice note ($durationFormatted)",
                                    "VOICE_NOTE",
                                    "voice://recorded_${System.currentTimeMillis()}",
                                    durationFormatted
                                )
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Send Voice")
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showQuickRepliesBar = !showQuickRepliesBar },
                        modifier = Modifier.testTag("quick_reply_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Quick Replies",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = { showAttachmentsRow = !showAttachmentsRow },
                        modifier = Modifier.testTag("attach_media_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Attach Photo or Document",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        placeholder = { Text("Message or / for quick reply…") },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_message_input")
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    if (messageInput.isBlank()) {
                        IconButton(
                            onClick = { isRecordingVoice = true },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                                .testTag("record_voice_note_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Record Voice Note",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                val trimmed = messageInput.trim()
                                if (trimmed.isNotEmpty()) {
                                    onSendMessage(trimmed, "NONE", "", "")
                                    messageInput = ""
                                    showQuickRepliesBar = false
                                }
                            },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .testTag("send_message_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send WhatsApp Message",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCrmDialog) {
        CustomerCrmModal(
            chat = chat,
            onDismiss = { showCrmDialog = false },
            onSave = { labels, notes, pinned, archived ->
                onSaveCrm(labels, notes, pinned, archived)
                showCrmDialog = false
            }
        )
    }
}

@Composable
private fun ChatMessageBubble(message: ChatMessage) {
    val isDark = isSystemInDarkTheme()
    val isOutbound = message.senderType == "BUSINESS" || message.senderType == "AUTO_RULE"

    val bubbleColor = when {
        isOutbound && isDark -> OutboundBubbleDark
        isOutbound && !isDark -> OutboundBubbleLight
        !isOutbound && isDark -> InboundBubbleDark
        else -> InboundBubbleLight
    }

    val textColor = if (isDark) Color(0xFFE9EDEF) else Color(0xFF111B21)

    val timeFormatted = remember(message.createdAt) {
        val ts = message.createdAt
        if (ts != null) {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(ts.toDate())
        } else {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        }
    }

    var isVoicePlaying by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOutbound) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isOutbound) 16.dp else 4.dp,
                bottomEnd = if (isOutbound) 4.dp else 16.dp
            ),
            tonalElevation = 1.dp,
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (message.senderType == "AUTO_RULE") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AUTOMATED BUSINESS RULE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Attachment rendering
                when (message.attachmentType) {
                    "IMAGE" -> {
                        Surface(
                            color = Color.Black.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                if (message.attachmentUri.startsWith("content://") || message.attachmentUri.startsWith("http")) {
                                    AsyncImage(
                                        model = message.attachmentUri,
                                        contentDescription = "Attached Image",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = message.attachmentMeta.ifBlank { "Image Attachment" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textColor
                                    )
                                }
                            }
                        }
                    }

                    "DOCUMENT" -> {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = message.attachmentMeta.ifBlank { "Document.pdf" },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = textColor
                                    )
                                    Text(
                                        text = "WhatsApp Business Document",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textColor.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }

                    "VOICE_NOTE" -> {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { isVoicePlaying = !isVoicePlaying },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (isVoicePlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play or Pause Voice Note",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Voice Note • ${message.attachmentMeta.ifBlank { "0:12" }}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = textColor
                                )
                            }
                        }
                    }
                }

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.65f)
                    )
                    if (isOutbound) {
                        Spacer(modifier = Modifier.width(4.dp))
                        val (statusIcon, statusTint) = when (message.deliveryStatus) {
                            "QUEUED" -> Icons.Default.Schedule to Color(0xFFD97706)
                            "SENT" -> Icons.Default.Check to textColor.copy(alpha = 0.65f)
                            else -> Icons.Default.DoneAll to Color(0xFF0284C7)
                        }
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = message.deliveryStatus,
                            tint = statusTint,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerCrmModal(
    chat: CustomerChat,
    onDismiss: () -> Unit,
    onSave: (labels: List<String>, notes: String, isPinned: Boolean, isArchived: Boolean) -> Unit
) {
    val presetLabels = listOf("VIP", "New Lead", "Pending Payment", "Order Shipped", "Wholesale", "Follow Up")
    val selectedLabels = remember { mutableStateListOf<String>().apply { addAll(chat.labels) } }
    var crmNotes by remember { mutableStateOf(chat.crmNotes) }
    var isPinned by remember { mutableStateOf(chat.isPinned) }
    var isArchived by remember { mutableStateOf(chat.isArchived) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Customer CRM & Labels — ${chat.customerName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Assign Customer Labels",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetLabels.forEach { label ->
                        val isSelected = selectedLabels.contains(label)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) selectedLabels.remove(label) else selectedLabels.add(label)
                            },
                            label = { Text(label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = crmNotes,
                    onValueChange = { crmNotes = it },
                    label = { Text("Internal CRM Contact Notes") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("crm_notes_input")
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isPinned,
                        onCheckedChange = { isPinned = it }
                    )
                    Text("Pin conversation to top of inbox")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isArchived,
                        onCheckedChange = { isArchived = it }
                    )
                    Text("Archive conversation")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(selectedLabels.toList(), crmNotes.trim(), isPinned, isArchived)
                },
                modifier = Modifier.testTag("save_crm_button")
            ) {
                Text("Save CRM")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
