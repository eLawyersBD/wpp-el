package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AutomationRule
import com.example.data.model.BroadcastCampaign
import com.example.data.model.ChatMessage
import com.example.data.model.CustomerChat
import com.example.data.model.LinkedSession
import com.example.data.model.QuickReplyTemplate
import com.example.data.model.UiState
import com.example.data.repository.BizSyncRepository
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BizSyncViewModel(
    private val repository: BizSyncRepository,
    private val currentUserId: String,
    deviceOnlineFlow: Flow<Boolean>
) : ViewModel() {

    val isDeviceOnline: StateFlow<Boolean> = deviceOnlineFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), true)

    val sessionState: StateFlow<UiState<LinkedSession?>> = repository.observeSession(currentUserId)
        .map<LinkedSession?, UiState<LinkedSession?>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing session", error)
            emit(UiState.Error(error.message ?: "Failed to load session"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val chatsState: StateFlow<UiState<List<CustomerChat>>> = repository.observeChats(currentUserId)
        .map<List<CustomerChat>, UiState<List<CustomerChat>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing chats", error)
            emit(UiState.Error(error.message ?: "Failed to load chats"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val templatesState: StateFlow<UiState<List<QuickReplyTemplate>>> = repository.observeTemplates(currentUserId)
        .map<List<QuickReplyTemplate>, UiState<List<QuickReplyTemplate>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing templates", error)
            emit(UiState.Error(error.message ?: "Failed to load templates"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val broadcastsState: StateFlow<UiState<List<BroadcastCampaign>>> = repository.observeBroadcasts(currentUserId)
        .map<List<BroadcastCampaign>, UiState<List<BroadcastCampaign>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing broadcasts", error)
            emit(UiState.Error(error.message ?: "Failed to load broadcasts"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val rulesState: StateFlow<UiState<List<AutomationRule>>> = repository.observeRules(currentUserId)
        .map<List<AutomationRule>, UiState<List<AutomationRule>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing rules", error)
            emit(UiState.Error(error.message ?: "Failed to load rules"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _selectedChatId = MutableStateFlow<String?>(null)
    val selectedChatId: StateFlow<String?> = _selectedChatId.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeChatMessagesState: StateFlow<UiState<List<ChatMessage>>> = _selectedChatId
        .flatMapLatest { chatId ->
            if (chatId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeMessages(currentUserId, chatId)
                    .map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
                    .catch { error ->
                        Log.w(TAG, "Error observing messages", error)
                        emit(UiState.Error(error.message ?: "Failed to load messages"))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    // True when BOTH Android device internet AND the QR-linked WhatsApp Business number have internet
    val isLiveSyncActive: StateFlow<Boolean> = combine(
        isDeviceOnline,
        sessionState
    ) { deviceOnline, sessionUi ->
        val session = (sessionUi as? UiState.Success)?.data
        deviceOnline && session != null && session.isPaired && session.isBusinessNumberOnline
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), true)

    init {
        viewModelScope.launch {
            repository.seedWorkspaceIfNeeded()
        }
        // Automatically flush any queued messages whenever live internet sync resumes
        viewModelScope.launch {
            isLiveSyncActive.collect { isLive ->
                if (isLive) {
                    val chats = (chatsState.value as? UiState.Success)?.data.orEmpty()
                    var totalFlushed = 0
                    for (chat in chats) {
                        val flushed = repository.flushQueuedMessagesForChat(chat.id).getOrDefault(0)
                        totalFlushed += flushed
                    }
                    if (totalFlushed > 0) {
                        showTransientBanner("Live Sync Restored: $totalFlushed queued message(s) delivered to WhatsApp Business number")
                    }
                }
            }
        }
    }

    fun showTransientBanner(message: String) {
        viewModelScope.launch {
            _statusBannerMessage.value = message
            delay(4000L)
            if (_statusBannerMessage.value == message) {
                _statusBannerMessage.value = null
            }
        }
    }

    fun selectChat(chatId: String?) {
        _selectedChatId.value = chatId
        if (!chatId.isNullOrBlank()) {
            viewModelScope.launch {
                repository.markChatRead(chatId)
            }
        }
    }

    fun pairWhatsAppBusinessQr(
        businessName: String,
        phoneNumber: String,
        qrToken: String = "WA-BIZ-${UUID.randomUUID().toString().take(8).uppercase()}",
        deviceName: String = "BizSync Android Companion"
    ) {
        viewModelScope.launch {
            val result = repository.linkBusinessSession(
                businessName = businessName.ifBlank { "Apex Global Commerce" },
                phoneNumber = phoneNumber.ifBlank { "+1 (555) 019-8842" },
                qrToken = qrToken,
                deviceName = deviceName,
                isPaired = true,
                isBusinessNumberOnline = true
            )
            if (result.isSuccess) {
                showTransientBanner("QR Code Verified: Linked live to $phoneNumber")
            } else {
                showTransientBanner("Failed to link QR session: ${result.exceptionOrNull()?.localizedMessage}")
            }
        }
    }

    fun toggleBusinessNumberInternet(isOnline: Boolean) {
        viewModelScope.launch {
            repository.setBusinessNumberOnline(isOnline)
            if (isOnline) {
                showTransientBanner("WhatsApp Business number reconnected to internet — live sync active")
            } else {
                showTransientBanner("WhatsApp Business number offline — outbound messages will queue until reconnected")
            }
        }
    }

    fun unlinkBusinessSession() {
        viewModelScope.launch {
            repository.unlinkBusinessSession()
            showTransientBanner("WhatsApp Business number unlinked. Scan QR to pair again.")
        }
    }

    fun createNewChat(
        customerName: String,
        customerPhone: String,
        labels: List<String>,
        crmNotes: String,
        initialMessage: String,
        onCreated: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val colors = listOf("#0B6E4F", "#0F4C5C", "#D97706", "#7C3AED", "#0284C7", "#BE123C")
            val pickedColor = colors[kotlin.math.abs(customerName.hashCode()) % colors.size]
            val result = repository.createCustomerChat(
                customerName = customerName,
                customerPhone = customerPhone,
                avatarColor = pickedColor,
                labels = labels,
                crmNotes = crmNotes,
                initialMessage = initialMessage
            )
            result.onSuccess { chatId ->
                if (initialMessage.isNotBlank()) {
                    repository.sendMessage(
                        chatId = chatId,
                        text = initialMessage,
                        senderType = "BUSINESS",
                        isLiveConnected = isLiveSyncActive.value
                    )
                }
                _selectedChatId.value = chatId
                onCreated(chatId)
            }
        }
    }

    fun sendChatMessage(
        chatId: String,
        text: String,
        attachmentType: String = "NONE",
        attachmentUri: String = "",
        attachmentMeta: String = ""
    ) {
        if (text.isBlank() && attachmentType == "NONE") return
        viewModelScope.launch {
            val isLive = isLiveSyncActive.value
            val result = repository.sendMessage(
                chatId = chatId,
                text = text,
                senderType = "BUSINESS",
                attachmentType = attachmentType,
                attachmentUri = attachmentUri,
                attachmentMeta = attachmentMeta,
                isLiveConnected = isLive
            )
            if (result.isSuccess && !isLive) {
                showTransientBanner("Message queued: Will auto-send as soon as WhatsApp Business number & device are online")
            }
        }
    }

    fun simulateIncomingCustomerMessage(
        targetChatId: String? = null,
        customText: String? = null
    ) {
        viewModelScope.launch {
            if (!isLiveSyncActive.value) {
                showTransientBanner("Cannot receive live WhatsApp sync while Business Number or Device is offline")
                return@launch
            }
            val chats = (chatsState.value as? UiState.Success)?.data.orEmpty()
            val targetChat = if (targetChatId != null) {
                chats.find { it.id == targetChatId }
            } else {
                chats.firstOrNull()
            }

            if (targetChat == null) return@launch

            val sampleInquiries = listOf(
                "Hi! Can you confirm current pricing and bulk availability for 80 units?",
                "Hello team, we just transferred the invoice payment. Please check and confirm dispatch!",
                "Do you have your updated product catalog PDF available to share on WhatsApp?",
                "Hi, what are your express shipping lead times for next week?"
            )
            val incomingText = customText ?: sampleInquiries.random()

            repository.sendMessage(
                chatId = targetChat.id,
                text = incomingText,
                senderType = "CUSTOMER",
                isLiveConnected = true
            )

            // Evaluate active Automation Rules (AWAY, KEYWORD, GREETING)
            val rules = (rulesState.value as? UiState.Success)?.data.orEmpty().filter { it.isEnabled }
            val awayRule = rules.find { it.ruleType == "AWAY" }
            val keywordRule = rules.find { rule ->
                rule.ruleType == "KEYWORD" &&
                    rule.triggerCondition.split(",")
                        .map { it.trim().lowercase() }
                        .any { kw -> kw.isNotEmpty() && incomingText.lowercase().contains(kw) }
            }
            val greetingRule = rules.find { it.ruleType == "GREETING" }

            val matchedRule = awayRule ?: keywordRule ?: greetingRule
            if (matchedRule != null) {
                delay(700L)
                val replyBody = matchedRule.replyMessage.replace("{{customer_name}}", targetChat.customerName)
                repository.sendMessage(
                    chatId = targetChat.id,
                    text = replyBody,
                    senderType = "AUTO_RULE",
                    isLiveConnected = true
                )
                showTransientBanner("Auto-Rule Triggered (${matchedRule.title}) → Replied to ${targetChat.customerName}")
            } else {
                showTransientBanner("Live WhatsApp Msg from ${targetChat.customerName}")
            }
        }
    }

    fun updateCustomerCrm(
        chatId: String,
        labels: List<String>,
        crmNotes: String,
        isPinned: Boolean,
        isArchived: Boolean
    ) {
        viewModelScope.launch {
            repository.updateChatCrm(chatId, labels, crmNotes, isPinned, isArchived)
            showTransientBanner("Saved CRM tags & notes")
        }
    }

    fun saveQuickReply(shortcut: String, title: String, body: String, category: String) {
        viewModelScope.launch {
            repository.saveQuickReplyTemplate(shortcut, title, body, category)
            showTransientBanner("Saved Quick Reply $shortcut")
        }
    }

    fun deleteQuickReply(templateId: String) {
        viewModelScope.launch {
            repository.deleteTemplate(templateId)
        }
    }

    fun sendBroadcastCampaign(
        title: String,
        targetLabels: List<String>,
        messageBody: String
    ) {
        viewModelScope.launch {
            val allChats = (chatsState.value as? UiState.Success)?.data.orEmpty()
            val recipients = if (targetLabels.isEmpty() || targetLabels.contains("All")) {
                allChats
            } else {
                allChats.filter { chat -> chat.labels.any { it in targetLabels } }
            }
            if (recipients.isEmpty()) {
                showTransientBanner("No matching contacts found for selected labels")
                return@launch
            }
            val isLive = isLiveSyncActive.value
            val result = repository.createBroadcastCampaign(
                title = title,
                targetLabels = targetLabels,
                messageBody = messageBody,
                matchingChats = recipients,
                isLiveConnected = isLive
            )
            if (result.isSuccess) {
                showTransientBanner("Broadcast '${title}' dispatched to ${recipients.size} WhatsApp contact(s)")
            }
        }
    }

    fun saveRule(
        ruleId: String?,
        ruleType: String,
        title: String,
        triggerCondition: String,
        replyMessage: String,
        isEnabled: Boolean
    ) {
        viewModelScope.launch {
            repository.saveAutomationRule(
                ruleId = ruleId,
                ruleType = ruleType,
                title = title,
                triggerCondition = triggerCondition,
                replyMessage = replyMessage,
                isEnabled = isEnabled
            )
            showTransientBanner("Updated Automation Rule: $title")
        }
    }

    fun toggleRule(ruleId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.toggleAutomationRule(ruleId, isEnabled)
        }
    }

    private companion object {
        const val TAG = "BizSyncVM"
    }
}
