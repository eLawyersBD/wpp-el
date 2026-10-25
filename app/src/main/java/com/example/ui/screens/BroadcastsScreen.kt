package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BroadcastCampaign
import com.example.data.model.CustomerChat

@Composable
fun BroadcastsScreen(
    chats: List<CustomerChat>,
    broadcasts: List<BroadcastCampaign>,
    isLiveSyncActive: Boolean,
    onSendBroadcast: (title: String, targetLabels: List<String>, messageBody: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var campaignTitle by remember { mutableStateOf("") }
    var messageBody by remember {
        mutableStateOf("Hi {{customer_name}}, exclusive WhatsApp Business flash update: enjoy 15% off priority restock orders placed today!")
    }
    val selectedLabels = remember { mutableStateListOf("VIP") }
    val availableLabels = listOf("All", "VIP", "New Lead", "Pending Payment", "Order Shipped")

    val matchingRecipientsCount = remember(chats, selectedLabels.toList()) {
        if (selectedLabels.isEmpty() || selectedLabels.contains("All")) {
            chats.size
        } else {
            chats.count { chat -> chat.labels.any { it in selectedLabels } }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "New WhatsApp Bulk Broadcast",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Dispatch personalized bulk campaigns across your synced WhatsApp Business customer segments. Use {{customer_name}} for automatic personalization.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = campaignTitle,
                        onValueChange = { campaignTitle = it },
                        label = { Text("Campaign Name (e.g., Q4 VIP Restock Alert)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("broadcast_title_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Target Customer Labels ($matchingRecipientsCount matching contacts)",
                        style = MaterialTheme.typography.labelLarge
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableLabels.forEach { label ->
                            val isSelected = selectedLabels.contains(label)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (label == "All") {
                                        selectedLabels.clear()
                                        selectedLabels.add("All")
                                    } else {
                                        selectedLabels.remove("All")
                                        if (isSelected) selectedLabels.remove(label) else selectedLabels.add(label)
                                    }
                                },
                                label = { Text(label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = messageBody,
                        onValueChange = { messageBody = it },
                        label = { Text("Broadcast Message Template") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("broadcast_body_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val titleToUse = campaignTitle.ifBlank { "WhatsApp Broadcast Campaign" }
                            onSendBroadcast(titleToUse, selectedLabels.toList(), messageBody)
                            campaignTitle = ""
                        },
                        enabled = messageBody.isNotBlank() && matchingRecipientsCount > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("send_broadcast_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isLiveSyncActive) {
                                "Dispatch Live Broadcast ($matchingRecipientsCount Contacts)"
                            } else {
                                "Queue Broadcast ($matchingRecipientsCount Contacts)"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Broadcast Campaign History (${broadcasts.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        if (broadcasts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = "No bulk broadcasts sent yet. Create your first campaign above to message multiple customer chats simultaneously.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(broadcasts, key = { it.id }) { bc ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = bc.title,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Surface(
                                color = if (bc.status == "SENT") {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.tertiaryContainer
                                },
                                shape = RoundedCornerShape(50)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (bc.status == "SENT") Icons.Default.CheckCircle else Icons.Default.Schedule,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${bc.status} • ${bc.recipientCount} Recipients",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = bc.messageBody,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (bc.targetLabels.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                bc.targetLabels.forEach { label ->
                                    CrmLabelBadge(label = label)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
