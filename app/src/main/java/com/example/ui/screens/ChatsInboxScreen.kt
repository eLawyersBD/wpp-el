package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CustomerChat
import com.example.data.model.LinkedSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatsInboxScreen(
    chats: List<CustomerChat>,
    selectedChatId: String?,
    session: LinkedSession?,
    isDeviceOnline: Boolean,
    isLiveSyncActive: Boolean,
    onSelectChat: (String) -> Unit,
    onOpenQrModal: () -> Unit,
    onToggleBusinessNumberOnline: (Boolean) -> Unit,
    onSimulateIncomingMessage: () -> Unit,
    onCreateNewChat: (name: String, phone: String, labels: List<String>, notes: String, firstMsg: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var showNewChatDialog by remember { mutableStateOf(false) }

    val filters = listOf("All", "Unread", "VIP", "New Lead", "Pending Payment", "Order Shipped", "Archived")

    val filteredChats = remember(chats, searchQuery, selectedFilter) {
        chats.filter { chat ->
            val matchesFilter = when (selectedFilter) {
                "All" -> !chat.isArchived
                "Unread" -> chat.unreadCount > 0 && !chat.isArchived
                "Archived" -> chat.isArchived
                else -> chat.labels.contains(selectedFilter) && !chat.isArchived
            }
            val matchesQuery = searchQuery.isBlank() ||
                chat.customerName.contains(searchQuery, ignoreCase = true) ||
                chat.customerPhone.contains(searchQuery, ignoreCase = true) ||
                chat.crmNotes.contains(searchQuery, ignoreCase = true) ||
                chat.lastMessageText.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Live WhatsApp Business Connection & Internet Sync Telemetry Card
            LiveConnectionBannerCard(
                session = session,
                isDeviceOnline = isDeviceOnline,
                isLiveSyncActive = isLiveSyncActive,
                onOpenQrModal = onOpenQrModal,
                onToggleBusinessNumberOnline = onToggleBusinessNumberOnline,
                onSimulateIncomingMessage = onSimulateIncomingMessage
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search customer, WhatsApp number, or CRM notes…") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search conversations"
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("inbox_search_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // CRM & Inbox Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredChats.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "No conversations match \"$selectedFilter\"",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Start a new customer conversation or tap 'Simulate Customer Msg' to test live WhatsApp Business synchronization.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(onClick = { showNewChatDialog = true }) {
                            Icon(imageVector = Icons.Default.AddComment, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start New Customer Chat")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("chats_lazy_column"),
                    contentPadding = PaddingValues(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredChats, key = { it.id }) { chat ->
                        CustomerChatCard(
                            chat = chat,
                            isSelected = chat.id == selectedChatId,
                            isLiveSyncActive = isLiveSyncActive,
                            onClick = { onSelectChat(chat.id) }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showNewChatDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("new_chat_fab")
        ) {
            Icon(
                imageVector = Icons.Default.AddComment,
                contentDescription = "New Customer Chat"
            )
        }
    }

    if (showNewChatDialog) {
        NewCustomerChatDialog(
            onDismiss = { showNewChatDialog = false },
            onConfirm = { name, phone, labels, notes, firstMsg ->
                onCreateNewChat(name, phone, labels, notes, firstMsg)
                showNewChatDialog = false
            }
        )
    }
}

@Composable
private fun LiveConnectionBannerCard(
    session: LinkedSession?,
    isDeviceOnline: Boolean,
    isLiveSyncActive: Boolean,
    onOpenQrModal: () -> Unit,
    onToggleBusinessNumberOnline: (Boolean) -> Unit,
    onSimulateIncomingMessage: () -> Unit
) {
    val isPaired = session?.isPaired == true
    val isNumberOnline = session?.isBusinessNumberOnline == true

    val containerColor = when {
        isLiveSyncActive -> MaterialTheme.colorScheme.primaryContainer
        !isPaired -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val contentColor = when {
        isLiveSyncActive -> MaterialTheme.colorScheme.onPrimaryContainer
        !isPaired -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onTertiaryContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isLiveSyncActive) Icons.Default.Wifi else Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = when {
                                !isPaired -> "QR NOT LINKED — Tap 'Pair QR' to connect business number"
                                !isDeviceOnline -> "APP INTERNET OFFLINE — Messages queued locally"
                                !isNumberOnline -> "WA BUSINESS NUMBER OFFLINE — Waiting for host phone internet"
                                else -> "LIVE SYNC: ${session?.businessName ?: "Business"} (${session?.phoneNumber ?: ""})"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = contentColor
                        )
                        Text(
                            text = if (isLiveSyncActive) {
                                "Both WhatsApp Business number & mobile app are online • Live read/send active"
                            } else {
                                "Sync paused until both WhatsApp Business number and mobile app are connected to internet"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.85f)
                        )
                    }
                }

                if (isPaired) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isNumberOnline) "WA Online" else "WA Offline",
                            style = MaterialTheme.typography.labelMedium,
                            color = contentColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = isNumberOnline,
                            onCheckedChange = onToggleBusinessNumberOnline,
                            modifier = Modifier.testTag("toggle_wa_number_online_switch")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenQrModal,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_qr_scanner_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isPaired) "QR Session" else "Link via QR")
                }

                Button(
                    onClick = onSimulateIncomingMessage,
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("simulate_incoming_msg_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Simulate Live Msg")
                }
            }
        }
    }
}

@Composable
private fun CustomerChatCard(
    chat: CustomerChat,
    isSelected: Boolean,
    isLiveSyncActive: Boolean,
    onClick: () -> Unit
) {
    val avatarColor = try {
        Color(android.graphics.Color.parseColor(chat.avatarColor))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val initials = chat.customerName
        .split(" ")
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .take(2)
        .joinToString("")
        .ifEmpty { "WA" }

    val timeFormatted = remember(chat.updatedAt, chat.createdAt) {
        val ts = chat.updatedAt ?: chat.createdAt
        if (ts != null) {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(ts.toDate())
        } else {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("chat_item_${chat.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with live dot
            Box {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(avatarColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .align(Alignment.BottomEnd)
                        .background(
                            if (isLiveSyncActive) Color(0xFF10B981) else Color(0xFFF59E0B),
                            CircleShape
                        )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = chat.customerName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (chat.isPinned) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (chat.unreadCount > 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                Text(
                    text = chat.customerPhone,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isLiveSyncActive) Icons.Default.DoneAll else Icons.Default.Check,
                        contentDescription = null,
                        tint = if (isLiveSyncActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = chat.lastMessageText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (chat.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ) {
                            Text(
                                text = chat.unreadCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                if (chat.labels.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        chat.labels.take(3).forEach { label ->
                            CrmLabelBadge(label = label)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CrmLabelBadge(label: String) {
    val (bg, fg) = when (label) {
        "VIP" -> Color(0xFFFEF3C7) to Color(0xFF92400E)
        "New Lead" -> Color(0xFFD1FAE5) to Color(0xFF065F46)
        "Pending Payment" -> Color(0xFFFEE2E2) to Color(0xFF991B1B)
        "Order Shipped" -> Color(0xFFE0F2FE) to Color(0xFF075985)
        else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun NewCustomerChatDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, labels: List<String>, notes: String, firstMsg: String) -> Unit
) {
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("+1 (555) ") }
    var selectedLabel by remember { mutableStateOf("New Lead") }
    var crmNotes by remember { mutableStateOf("") }
    var firstMessage by remember { mutableStateOf("Hi! Following up from our WhatsApp Business desk.") }

    val availableLabels = listOf("New Lead", "VIP", "Pending Payment", "Order Shipped")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start New WhatsApp Business Chat") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Customer Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = { customerPhone = it },
                    label = { Text("Customer WhatsApp Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableLabels.forEach { label ->
                        FilterChip(
                            selected = selectedLabel == label,
                            onClick = { selectedLabel = label },
                            label = { Text(label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = crmNotes,
                    onValueChange = { crmNotes = it },
                    label = { Text("CRM Contact Note (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = firstMessage,
                    onValueChange = { firstMessage = it },
                    label = { Text("Initial Outbound Message") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (customerName.isNotBlank() && customerPhone.isNotBlank()) {
                        onConfirm(
                            customerName.trim(),
                            customerPhone.trim(),
                            listOf(selectedLabel),
                            crmNotes.trim(),
                            firstMessage.trim()
                        )
                    }
                },
                enabled = customerName.isNotBlank() && customerPhone.isNotBlank()
            ) {
                Text("Start Live Chat")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
