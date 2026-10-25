package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@IgnoreExtraProperties
data class LinkedSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val businessName: String = "",
    val phoneNumber: String = "",
    val qrToken: String = "",
    @get:PropertyName("isPaired") @set:PropertyName("isPaired") var isPaired: Boolean = false,
    @get:PropertyName("isBusinessNumberOnline") @set:PropertyName("isBusinessNumberOnline") var isBusinessNumberOnline: Boolean = true,
    val deviceName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CustomerChat(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val avatarColor: String = "#0B6E4F",
    val labels: List<String> = emptyList(),
    val crmNotes: String = "",
    val lastMessageText: String = "",
    val unreadCount: Long = 0L,
    @get:PropertyName("isPinned") @set:PropertyName("isPinned") var isPinned: Boolean = false,
    @get:PropertyName("isArchived") @set:PropertyName("isArchived") var isArchived: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val chatId: String = "",
    val senderType: String = "BUSINESS", // BUSINESS, CUSTOMER, AUTO_RULE
    val text: String = "",
    val attachmentType: String = "NONE", // NONE, IMAGE, DOCUMENT, VOICE_NOTE
    val attachmentUri: String = "",
    val attachmentMeta: String = "",
    val deliveryStatus: String = "SENT", // QUEUED, SENT, DELIVERED, READ
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class QuickReplyTemplate(
    @DocumentId val id: String = "",
    val userId: String = "",
    val shortcut: String = "",
    val title: String = "",
    val body: String = "",
    val category: String = "General",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BroadcastCampaign(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val targetLabels: List<String> = emptyList(),
    val recipientCount: Long = 0L,
    val messageBody: String = "",
    val status: String = "SENT", // QUEUED, SENT
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AutomationRule(
    @DocumentId val id: String = "",
    val userId: String = "",
    val ruleType: String = "GREETING", // GREETING, AWAY, KEYWORD
    val title: String = "",
    val triggerCondition: String = "",
    val replyMessage: String = "",
    @get:PropertyName("isEnabled") @set:PropertyName("isEnabled") var isEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
