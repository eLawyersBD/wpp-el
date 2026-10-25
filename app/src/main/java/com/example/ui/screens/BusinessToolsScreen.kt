package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AutomationRule
import com.example.data.model.LinkedSession
import com.example.data.model.QuickReplyTemplate

@Composable
fun BusinessToolsScreen(
    session: LinkedSession?,
    isDeviceOnline: Boolean,
    isLiveSyncActive: Boolean,
    rules: List<AutomationRule>,
    templates: List<QuickReplyTemplate>,
    onOpenQrModal: () -> Unit,
    onToggleBusinessNumberOnline: (Boolean) -> Unit,
    onUnlinkSession: () -> Unit,
    onToggleRule: (ruleId: String, enabled: Boolean) -> Unit,
    onSaveRule: (ruleId: String?, ruleType: String, title: String, trigger: String, reply: String, enabled: Boolean) -> Unit,
    onSaveTemplate: (shortcut: String, title: String, body: String, category: String) -> Unit,
    onDeleteTemplate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddTemplateDialog by remember { mutableStateOf(false) }
    var showAddRuleDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Linked WhatsApp Business Number & QR Session Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Linked WhatsApp Business Number",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        Surface(
                            color = if (isLiveSyncActive) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.tertiaryContainer
                            },
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = if (isLiveSyncActive) "LIVE SYNC" else "PAUSED",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Business Name: ${session?.businessName ?: "Not Linked"}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "WhatsApp Number: ${session?.phoneNumber ?: "Scan QR to pair"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "QR Session Token: ${session?.qrToken ?: "—"} • Device: ${session?.deviceName ?: "—"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "WhatsApp Business Number Internet Status",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "When both your business number and this mobile app are online, message sync is live.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = session?.isBusinessNumberOnline == true,
                            onCheckedChange = onToggleBusinessNumberOnline
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onOpenQrModal,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (session?.isPaired == true) "Update / Scan QR" else "Pair via QR Code")
                        }

                        if (session?.isPaired == true) {
                            OutlinedButton(
                                onClick = onUnlinkSession,
                                modifier = Modifier.weight(0.8f)
                            ) {
                                Icon(imageVector = Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Unlink")
                            }
                        }
                    }
                }
            }
        }

        // 2. Auto-Greeting, Away & Keyword Automation Rules
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Auto-Greeting & Away Rules",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                OutlinedButton(
                    onClick = { showAddRuleDialog = true },
                    modifier = Modifier.testTag("add_rule_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Rule")
                }
            }
        }

        items(rules, key = { it.id }) { rule ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = rule.ruleType,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = rule.title,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Trigger: ${rule.triggerCondition}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = rule.replyMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = rule.isEnabled,
                        onCheckedChange = { enabled -> onToggleRule(rule.id, enabled) }
                    )
                }
            }
        }

        // 3. Quick Reply Templates (/shortcut)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Quick Reply Templates (/shortcut)",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                OutlinedButton(
                    onClick = { showAddTemplateDialog = true },
                    modifier = Modifier.testTag("add_template_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New /Reply")
                }
            }
        }

        items(templates, key = { it.id }) { tpl ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = tpl.shortcut,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tpl.title,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tpl.body,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onDeleteTemplate(tpl.id) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Template",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showAddTemplateDialog) {
        AddQuickReplyDialog(
            onDismiss = { showAddTemplateDialog = false },
            onSave = { shortcut, title, body, category ->
                onSaveTemplate(shortcut, title, body, category)
                showAddTemplateDialog = false
            }
        )
    }

    if (showAddRuleDialog) {
        AddAutomationRuleDialog(
            onDismiss = { showAddRuleDialog = false },
            onSave = { ruleType, title, trigger, reply ->
                onSaveRule(null, ruleType, title, trigger, reply, true)
                showAddRuleDialog = false
            }
        )
    }
}

@Composable
private fun AddQuickReplyDialog(
    onDismiss: () -> Unit,
    onSave: (shortcut: String, title: String, body: String, category: String) -> Unit
) {
    var shortcut by remember { mutableStateOf("/discount") }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("Hi {{customer_name}}, here is your special 10% loyalty discount code: BIZ10.") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Quick Reply Template") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = shortcut,
                    onValueChange = { shortcut = it },
                    label = { Text("Slash Shortcut (e.g. /discount)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Template Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Message Body") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (shortcut.isNotBlank() && body.isNotBlank()) {
                        onSave(shortcut.trim(), title.ifBlank { "Custom Reply" }.trim(), body.trim(), "Sales")
                    }
                }
            ) {
                Text("Save Template")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AddAutomationRuleDialog(
    onDismiss: () -> Unit,
    onSave: (ruleType: String, title: String, trigger: String, reply: String) -> Unit
) {
    var ruleType by remember { mutableStateOf("KEYWORD") }
    var title by remember { mutableStateOf("") }
    var trigger by remember { mutableStateOf("invoice, catalog, help") }
    var reply by remember { mutableStateOf("Hi {{customer_name}}, our automated desk logged your request and is sending the details right over!") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Automated Reply Rule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("GREETING", "AWAY", "KEYWORD").forEach { type ->
                        FilterChip(
                            selected = ruleType == type,
                            onClick = { ruleType = type },
                            label = { Text(type) }
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Rule Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = trigger,
                    onValueChange = { trigger = it },
                    label = { Text("Trigger Condition / Keywords (comma separated)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reply,
                    onValueChange = { reply = it },
                    label = { Text("Automated Reply Message") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (reply.isNotBlank()) {
                        onSave(
                            ruleType,
                            title.ifBlank { "$ruleType Auto-Reply" }.trim(),
                            trigger.ifBlank { "Any incoming inquiry" }.trim(),
                            reply.trim()
                        )
                    }
                }
            ) {
                Text("Save Rule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
