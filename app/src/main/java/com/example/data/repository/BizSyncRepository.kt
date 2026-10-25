package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AutomationRule
import com.example.data.model.BroadcastCampaign
import com.example.data.model.ChatMessage
import com.example.data.model.CustomerChat
import com.example.data.model.LinkedSession
import com.example.data.model.QuickReplyTemplate
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class BizSyncRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- Real-time Observation Flows ---

    fun observeSession(userId: String): Flow<LinkedSession?> {
        val path = "users/$userId/sessions"
        return db.collection("users").document(userId).collection("sessions")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.firstOrNull()?.toObject(
                    LinkedSession::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeChats(userId: String): Flow<List<CustomerChat>> {
        val path = "users/$userId/chats"
        return db.collection("users").document(userId).collection("chats")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CustomerChat::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedWith(
                    compareByDescending<CustomerChat> { it.isPinned }
                        .thenByDescending { (it.updatedAt ?: it.createdAt ?: Timestamp(0, 0)).seconds }
                )
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeMessages(userId: String, chatId: String): Flow<List<ChatMessage>> {
        val path = "users/$userId/chats/$chatId/messages"
        return db.collection("users").document(userId)
            .collection("chats").document(chatId)
            .collection("messages")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ChatMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { (it.createdAt ?: Timestamp(0, 0)).seconds * 1_000_000_000L + (it.createdAt?.nanoseconds ?: 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeTemplates(userId: String): Flow<List<QuickReplyTemplate>> {
        val path = "users/$userId/templates"
        return db.collection("users").document(userId).collection("templates")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    QuickReplyTemplate::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.shortcut }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeBroadcasts(userId: String): Flow<List<BroadcastCampaign>> {
        val path = "users/$userId/broadcasts"
        return db.collection("users").document(userId).collection("broadcasts")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    BroadcastCampaign::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { (it.createdAt ?: Timestamp(0, 0)).seconds }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeRules(userId: String): Flow<List<AutomationRule>> {
        val path = "users/$userId/rules"
        return db.collection("users").document(userId).collection("rules")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    AutomationRule::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    // --- Session & QR Pairing Operations ---

    suspend fun linkBusinessSession(
        businessName: String,
        phoneNumber: String,
        qrToken: String,
        deviceName: String = "BizSync Android Companion",
        isPaired: Boolean = true,
        isBusinessNumberOnline: Boolean = true
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val sessionId = "primary_session"
        val docRef = db.collection("users").document(uid).collection("sessions").document(sessionId)
        val existingSnap = try {
            docRef.get().await()
        } catch (e: Exception) {
            null
        }
        try {
            if (existingSnap != null && existingSnap.exists()) {
                val updates = mapOf(
                    "businessName" to businessName.take(120),
                    "phoneNumber" to phoneNumber.take(40),
                    "qrToken" to qrToken.take(200),
                    "isPaired" to isPaired,
                    "isBusinessNumberOnline" to isBusinessNumberOnline,
                    "deviceName" to deviceName.take(100),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "businessName" to businessName.take(120),
                    "phoneNumber" to phoneNumber.take(40),
                    "qrToken" to qrToken.take(200),
                    "isPaired" to isPaired,
                    "isBusinessNumberOnline" to isBusinessNumberOnline,
                    "deviceName" to deviceName.take(100),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            sessionId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun setBusinessNumberOnline(isOnline: Boolean): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("sessions").document("primary_session")
        try {
            docRef.update(
                mapOf(
                    "isBusinessNumberOnline" to isOnline,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun unlinkBusinessSession(): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("sessions").document("primary_session")
        try {
            docRef.update(
                mapOf(
                    "isPaired" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- Customer Chat & CRM Operations ---

    suspend fun createCustomerChat(
        customerName: String,
        customerPhone: String,
        avatarColor: String = "#0B6E4F",
        labels: List<String> = listOf("New Lead"),
        crmNotes: String = "",
        initialMessage: String = "Conversation started"
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val chatId = "chat_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val docRef = db.collection("users").document(uid).collection("chats").document(chatId)
        val payload = mapOf(
            "userId" to uid,
            "customerName" to customerName.take(120),
            "customerPhone" to customerPhone.take(40),
            "avatarColor" to avatarColor.take(16),
            "labels" to labels.take(20),
            "crmNotes" to crmNotes.take(2000),
            "lastMessageText" to initialMessage.take(1000),
            "unreadCount" to 0L,
            "isPinned" to false,
            "isArchived" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            chatId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun getChatById(targetUserId: String, chatId: String): Result<CustomerChat?> = runCatching {
        val docRef = db.collection("users").document(targetUserId).collection("chats").document(chatId)
        try {
            docRef.get().await().toObject(
                CustomerChat::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun updateChatCrm(
        chatId: String,
        labels: List<String>,
        crmNotes: String,
        isPinned: Boolean,
        isArchived: Boolean
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("chats").document(chatId)
        try {
            docRef.update(
                mapOf(
                    "labels" to labels.take(20),
                    "crmNotes" to crmNotes.take(2000),
                    "isPinned" to isPinned,
                    "isArchived" to isArchived,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun markChatRead(chatId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("chats").document(chatId)
        try {
            docRef.update(
                mapOf(
                    "unreadCount" to 0L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- Live Message Operations ---

    suspend fun sendMessage(
        chatId: String,
        text: String,
        senderType: String = "BUSINESS",
        attachmentType: String = "NONE",
        attachmentUri: String = "",
        attachmentMeta: String = "",
        isLiveConnected: Boolean = true
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val messageId = "msg_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val msgRef = db.collection("users").document(uid)
            .collection("chats").document(chatId)
            .collection("messages").document(messageId)
        val chatRef = db.collection("users").document(uid)
            .collection("chats").document(chatId)

        val deliveryStatus = when {
            senderType == "CUSTOMER" -> "READ"
            isLiveConnected -> "DELIVERED"
            else -> "QUEUED"
        }

        val payload = mapOf(
            "userId" to uid,
            "chatId" to chatId,
            "senderType" to senderType,
            "text" to text.take(4000),
            "attachmentType" to attachmentType,
            "attachmentUri" to attachmentUri.take(1000),
            "attachmentMeta" to attachmentMeta.take(300),
            "deliveryStatus" to deliveryStatus,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val previewPrefix = when (attachmentType) {
            "IMAGE" -> "[Photo] "
            "DOCUMENT" -> "[Doc: $attachmentMeta] "
            "VOICE_NOTE" -> "[Voice Note $attachmentMeta] "
            else -> ""
        }
        val previewText = (previewPrefix + text).take(200)

        try {
            msgRef.set(payload).await()
            val chatUpdates = mutableMapOf<String, Any>(
                "lastMessageText" to previewText,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (senderType == "CUSTOMER") {
                chatUpdates["unreadCount"] = FieldValue.increment(1L)
            }
            chatRef.update(chatUpdates).await()
            messageId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            throw e
        }
    }

    suspend fun flushQueuedMessagesForChat(chatId: String): Result<Int> = runCatching {
        val uid = requireUserId()
        val messagesCol = db.collection("users").document(uid)
            .collection("chats").document(chatId)
            .collection("messages")
        try {
            val snap = messagesCol.whereEqualTo("userId", uid).get().await()
            var flushed = 0
            for (doc in snap.documents) {
                val status = doc.getString("deliveryStatus") ?: "SENT"
                if (status == "QUEUED" || status == "SENT") {
                    doc.reference.update(
                        mapOf(
                            "deliveryStatus" to "READ",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    flushed++
                }
            }
            flushed
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, messagesCol.path)
            throw e
        }
    }

    // --- Quick Reply Templates ---

    suspend fun saveQuickReplyTemplate(
        shortcut: String,
        title: String,
        body: String,
        category: String = "Sales"
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val normalizedShortcut = if (shortcut.startsWith("/")) shortcut else "/$shortcut"
        val templateId = "tpl_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val docRef = db.collection("users").document(uid).collection("templates").document(templateId)
        val payload = mapOf(
            "userId" to uid,
            "shortcut" to normalizedShortcut.take(40),
            "title" to title.take(100),
            "body" to body.take(2000),
            "category" to category.take(60),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            templateId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteTemplate(templateId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("templates").document(templateId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Broadcast Campaigns ---

    suspend fun createBroadcastCampaign(
        title: String,
        targetLabels: List<String>,
        messageBody: String,
        matchingChats: List<CustomerChat>,
        isLiveConnected: Boolean
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val broadcastId = "bc_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val bcRef = db.collection("users").document(uid).collection("broadcasts").document(broadcastId)
        val status = if (isLiveConnected) "SENT" else "QUEUED"
        val payload = mapOf(
            "userId" to uid,
            "title" to title.take(120),
            "targetLabels" to targetLabels.take(20),
            "recipientCount" to matchingChats.size.toLong(),
            "messageBody" to messageBody.take(3000),
            "status" to status,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            bcRef.set(payload).await()
            for (chat in matchingChats) {
                val personalized = messageBody.replace("{{customer_name}}", chat.customerName)
                sendMessage(
                    chatId = chat.id,
                    text = personalized,
                    senderType = "BUSINESS",
                    isLiveConnected = isLiveConnected
                )
            }
            broadcastId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, bcRef.path)
            throw e
        }
    }

    // --- Automation Rules ---

    suspend fun saveAutomationRule(
        ruleId: String? = null,
        ruleType: String,
        title: String,
        triggerCondition: String,
        replyMessage: String,
        isEnabled: Boolean
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val id = ruleId ?: "rule_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val docRef = db.collection("users").document(uid).collection("rules").document(id)
        val existingSnap = try {
            docRef.get().await()
        } catch (e: Exception) {
            null
        }
        try {
            if (existingSnap != null && existingSnap.exists()) {
                docRef.update(
                    mapOf(
                        "ruleType" to ruleType,
                        "title" to title.take(100),
                        "triggerCondition" to triggerCondition.take(200),
                        "replyMessage" to replyMessage.take(2000),
                        "isEnabled" to isEnabled,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "ruleType" to ruleType,
                        "title" to title.take(100),
                        "triggerCondition" to triggerCondition.take(200),
                        "replyMessage" to replyMessage.take(2000),
                        "isEnabled" to isEnabled,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun toggleAutomationRule(ruleId: String, isEnabled: Boolean): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("rules").document(ruleId)
        try {
            docRef.update(
                mapOf(
                    "isEnabled" to isEnabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- Initial Workspace Seed ---

    suspend fun seedWorkspaceIfNeeded() {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return
        }
        val chatsCol = db.collection("users").document(uid).collection("chats")
        val existingChats = try {
            chatsCol.whereEqualTo("userId", uid).get().await()
        } catch (e: Exception) {
            return
        }
        if (!existingChats.isEmpty) return

        // 1. Seed default Linked WhatsApp Business session
        linkBusinessSession(
            businessName = "Apex Global Commerce",
            phoneNumber = "+1 (555) 019-8842",
            qrToken = "WA-BIZ-QR-9402-LIVE",
            deviceName = "Android Enterprise Terminal",
            isPaired = true,
            isBusinessNumberOnline = true
        )

        // 2. Seed Quick Reply Templates
        saveQuickReplyTemplate(
            shortcut = "/welcome",
            title = "Welcome & Catalog Intro",
            body = "Hi {{customer_name}}! Thank you for messaging Apex Global Commerce on WhatsApp Business. How can our team help you today?",
            category = "Greeting"
        )
        saveQuickReplyTemplate(
            shortcut = "/pricing",
            title = "Wholesale Tier Pricing",
            body = "Hi {{customer_name}}, our current B2B wholesale tiers start at $24/unit for 50+ units with priority 48-hour express shipping.",
            category = "Sales"
        )
        saveQuickReplyTemplate(
            shortcut = "/payment",
            title = "Invoice & Payment Link",
            body = "Your pro-forma invoice is ready! Please confirm your billing address so we can activate instant dispatch.",
            category = "Billing"
        )
        saveQuickReplyTemplate(
            shortcut = "/shipping",
            title = "Order Dispatch Update",
            body = "Great news {{customer_name}} — your shipment has been handed over to DHL Express and live tracking is active.",
            category = "Support"
        )

        // 3. Seed Automation Rules (Greeting & Away)
        saveAutomationRule(
            ruleId = "rule_greeting",
            ruleType = "GREETING",
            title = "Instant New Lead Greeting",
            triggerCondition = "First incoming customer inquiry",
            replyMessage = "Welcome to Apex Global Commerce! We received your WhatsApp message and a dedicated specialist is reviewing your request right now.",
            isEnabled = true
        )
        saveAutomationRule(
            ruleId = "rule_away",
            ruleType = "AWAY",
            title = "After-Hours Away Auto-Reply",
            triggerCondition = "When Away Mode is toggled active",
            replyMessage = "Thanks for reaching out! Our team is currently away from the desk, but your message is logged and we will reply first thing.",
            isEnabled = false
        )
        saveAutomationRule(
            ruleId = "rule_keyword_price",
            ruleType = "KEYWORD",
            title = "Pricing & Quote Auto-Responder",
            triggerCondition = "price, quote, cost, catalog",
            replyMessage = "Looking for a fast quote? Share your desired item quantity and destination city, and we will send our wholesale rate sheet immediately.",
            isEnabled = true
        )

        // 4. Seed Realistic Customer Conversations & Messages
        val chat1 = createCustomerChat(
            customerName = "Elena Rostova",
            customerPhone = "+1 (555) 234-8910",
            avatarColor = "#0B6E4F",
            labels = listOf("VIP", "Pending Payment"),
            crmNotes = "Q4 recurring distributor in Chicago. Prefers express air freight. Approved for Net-30 terms.",
            initialMessage = "Can we lock in 120 units for Friday delivery?"
        ).getOrNull()

        if (chat1 != null) {
            sendMessage(
                chatId = chat1,
                text = "Hi team! We are finalizing our Q4 inventory order.",
                senderType = "CUSTOMER"
            )
            sendMessage(
                chatId = chat1,
                text = "Hi Elena! Great to hear from you. Our warehouse has full stock ready for immediate dispatch.",
                senderType = "BUSINESS"
            )
            sendMessage(
                chatId = chat1,
                text = "Q4_Wholesale_Order_Sheet.pdf",
                senderType = "CUSTOMER",
                attachmentType = "DOCUMENT",
                attachmentUri = "content://com.aistudio.bizsyncwa/documents/q4_order.pdf",
                attachmentMeta = "Q4_Wholesale_Order_Sheet.pdf • 420 KB"
            )
            sendMessage(
                chatId = chat1,
                text = "Can we lock in 120 units for Friday delivery?",
                senderType = "CUSTOMER"
            )
            updateChatCrm(
                chatId = chat1,
                labels = listOf("VIP", "Pending Payment"),
                crmNotes = "Q4 recurring distributor in Chicago. Prefers express air freight. Approved for Net-30 terms.",
                isPinned = true,
                isArchived = false
            )
        }

        val chat2 = createCustomerChat(
            customerName = "Marcus Vance",
            customerPhone = "+1 (555) 876-5432",
            avatarColor = "#0F4C5C",
            labels = listOf("New Lead"),
            crmNotes = "Found us via Instagram QR campaign. Interested in starter retail bundle.",
            initialMessage = "Hi, could you share your latest wholesale catalog and price list?"
        ).getOrNull()

        if (chat2 != null) {
            sendMessage(
                chatId = chat2,
                text = "Hi, could you share your latest wholesale catalog and price list?",
                senderType = "CUSTOMER"
            )
            sendMessage(
                chatId = chat2,
                text = "Welcome to Apex Global Commerce! We received your WhatsApp message and a dedicated specialist is reviewing your request right now.",
                senderType = "AUTO_RULE"
            )
        }

        val chat3 = createCustomerChat(
            customerName = "Sofia Al-Mansoor",
            customerPhone = "+971 50 492 1833",
            avatarColor = "#D97706",
            labels = listOf("VIP", "Order Shipped"),
            crmNotes = "Dubai showroom partner. Order #AX-8821 shipped via DHL Priority.",
            initialMessage = "Voice note received (0:18) — Confirmed customs clearance!"
        ).getOrNull()

        if (chat3 != null) {
            sendMessage(
                chatId = chat3,
                text = "Confirmed customs clearance! Everything arrived in pristine condition.",
                senderType = "CUSTOMER",
                attachmentType = "VOICE_NOTE",
                attachmentUri = "voice://note_sofia_018",
                attachmentMeta = "0:18"
            )
            markChatRead(chat3)
        }
    }
}
