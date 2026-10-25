package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.UiState
import com.example.data.repository.BizSyncRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.components.QrPairingModal
import com.example.ui.screens.BroadcastsScreen
import com.example.ui.screens.BusinessToolsScreen
import com.example.ui.screens.ChatThreadScreen
import com.example.ui.screens.ChatsInboxScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BizSyncViewModel
import com.example.util.NetworkConnectivityMonitor
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppNavigation()
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

enum class BizSyncTab {
    CHATS,
    BROADCASTS,
    TOOLS
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { })
    } else {
        BizSyncAppShell(currentUserId = user.uid)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BizSyncAppShell(currentUserId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val viewModel: BizSyncViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                BizSyncViewModel(
                    repository = BizSyncRepository(db),
                    currentUserId = currentUserId,
                    deviceOnlineFlow = NetworkConnectivityMonitor.observeIsDeviceOnline(app)
                )
            }
        }
    )

    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val chatsState by viewModel.chatsState.collectAsStateWithLifecycle()
    val messagesState by viewModel.activeChatMessagesState.collectAsStateWithLifecycle()
    val templatesState by viewModel.templatesState.collectAsStateWithLifecycle()
    val broadcastsState by viewModel.broadcastsState.collectAsStateWithLifecycle()
    val rulesState by viewModel.rulesState.collectAsStateWithLifecycle()
    val selectedChatId by viewModel.selectedChatId.collectAsStateWithLifecycle()
    val isDeviceOnline by viewModel.isDeviceOnline.collectAsStateWithLifecycle()
    val isLiveSyncActive by viewModel.isLiveSyncActive.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBannerMessage.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(BizSyncTab.CHATS) }
    var showQrModal by rememberSaveable { mutableStateOf(false) }

    val session = (sessionState as? UiState.Success)?.data
    val chats = (chatsState as? UiState.Success)?.data.orEmpty()
    val messages = (messagesState as? UiState.Success)?.data.orEmpty()
    val templates = (templatesState as? UiState.Success)?.data.orEmpty()
    val broadcasts = (broadcastsState as? UiState.Success)?.data.orEmpty()
    val rules = (rulesState as? UiState.Success)?.data.orEmpty()

    val activeChat = remember(chats, selectedChatId) {
        chats.find { it.id == selectedChatId }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 720.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "BizSync WA",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            LiveSyncStatusBadge(
                                isLiveSyncActive = isLiveSyncActive,
                                isPaired = session?.isPaired == true,
                                isDeviceOnline = isDeviceOnline,
                                onClick = { showQrModal = true }
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showQrModal = true },
                            modifier = Modifier.testTag("top_bar_qr_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Link WhatsApp Business QR"
                            )
                        }
                        IconButton(
                            onClick = {
                                signOutUser(
                                    context = context,
                                    onSignOutComplete = { },
                                    scope = scope
                                )
                            },
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Sign Out"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                // Hide bottom bar on compact screen when inside an open chat thread so the keyboard and composer have full space
                if (!isExpandedScreen && (currentTab != BizSyncTab.CHATS || activeChat == null)) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentTab == BizSyncTab.CHATS,
                            onClick = { currentTab = BizSyncTab.CHATS },
                            icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null) },
                            label = { Text("Chats") },
                            modifier = Modifier.testTag("nav_tab_chats")
                        )
                        NavigationBarItem(
                            selected = currentTab == BizSyncTab.BROADCASTS,
                            onClick = { currentTab = BizSyncTab.BROADCASTS },
                            icon = { Icon(Icons.Default.Campaign, contentDescription = null) },
                            label = { Text("Broadcasts") },
                            modifier = Modifier.testTag("nav_tab_broadcasts")
                        )
                        NavigationBarItem(
                            selected = currentTab == BizSyncTab.TOOLS,
                            onClick = { currentTab = BizSyncTab.TOOLS },
                            icon = { Icon(Icons.Default.Build, contentDescription = null) },
                            label = { Text("Biz Tools & QR") },
                            modifier = Modifier.testTag("nav_tab_tools")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedVisibility(visible = statusBanner != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = statusBanner ?: "",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxSize()) {
                    if (isExpandedScreen) {
                        NavigationRail {
                            NavigationRailItem(
                                selected = currentTab == BizSyncTab.CHATS,
                                onClick = { currentTab = BizSyncTab.CHATS },
                                icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null) },
                                label = { Text("Chats") }
                            )
                            NavigationRailItem(
                                selected = currentTab == BizSyncTab.BROADCASTS,
                                onClick = { currentTab = BizSyncTab.BROADCASTS },
                                icon = { Icon(Icons.Default.Campaign, contentDescription = null) },
                                label = { Text("Broadcasts") }
                            )
                            NavigationRailItem(
                                selected = currentTab == BizSyncTab.TOOLS,
                                onClick = { currentTab = BizSyncTab.TOOLS },
                                icon = { Icon(Icons.Default.Build, contentDescription = null) },
                                label = { Text("Biz Tools") }
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        when (currentTab) {
                            BizSyncTab.CHATS -> {
                                if (isExpandedScreen) {
                                    // Canonical List-Detail Layout for Tablets / Foldables
                                    Row(modifier = Modifier.fillMaxSize()) {
                                        ChatsInboxScreen(
                                            chats = chats,
                                            selectedChatId = selectedChatId,
                                            session = session,
                                            isDeviceOnline = isDeviceOnline,
                                            isLiveSyncActive = isLiveSyncActive,
                                            onSelectChat = { viewModel.selectChat(it) },
                                            onOpenQrModal = { showQrModal = true },
                                            onToggleBusinessNumberOnline = { viewModel.toggleBusinessNumberInternet(it) },
                                            onSimulateIncomingMessage = { viewModel.simulateIncomingCustomerMessage() },
                                            onCreateNewChat = { name, phone, labels, notes, msg ->
                                                viewModel.createNewChat(name, phone, labels, notes, msg)
                                            },
                                            modifier = Modifier
                                                .weight(0.42f)
                                                .fillMaxHeight()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .weight(0.58f)
                                                .fillMaxHeight()
                                        ) {
                                            val currentActive = activeChat ?: chats.firstOrNull()?.also {
                                                viewModel.selectChat(it.id)
                                            }
                                            if (currentActive != null) {
                                                ChatThreadScreen(
                                                    chat = currentActive,
                                                    messages = messages,
                                                    templates = templates,
                                                    isLiveSyncActive = isLiveSyncActive,
                                                    showBackButton = false,
                                                    onBack = { },
                                                    onSendMessage = { txt, type, uri, meta ->
                                                        viewModel.sendChatMessage(currentActive.id, txt, type, uri, meta)
                                                    },
                                                    onSimulateReply = {
                                                        viewModel.simulateIncomingCustomerMessage(targetChatId = currentActive.id)
                                                    },
                                                    onSaveCrm = { labels, notes, pinned, archived ->
                                                        viewModel.updateCustomerCrm(currentActive.id, labels, notes, pinned, archived)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // Compact Handheld Layout
                                    if (activeChat != null) {
                                        ChatThreadScreen(
                                            chat = activeChat,
                                            messages = messages,
                                            templates = templates,
                                            isLiveSyncActive = isLiveSyncActive,
                                            showBackButton = true,
                                            onBack = { viewModel.selectChat(null) },
                                            onSendMessage = { txt, type, uri, meta ->
                                                viewModel.sendChatMessage(activeChat.id, txt, type, uri, meta)
                                            },
                                            onSimulateReply = {
                                                viewModel.simulateIncomingCustomerMessage(targetChatId = activeChat.id)
                                            },
                                            onSaveCrm = { labels, notes, pinned, archived ->
                                                viewModel.updateCustomerCrm(activeChat.id, labels, notes, pinned, archived)
                                            }
                                        )
                                    } else {
                                        ChatsInboxScreen(
                                            chats = chats,
                                            selectedChatId = selectedChatId,
                                            session = session,
                                            isDeviceOnline = isDeviceOnline,
                                            isLiveSyncActive = isLiveSyncActive,
                                            onSelectChat = { viewModel.selectChat(it) },
                                            onOpenQrModal = { showQrModal = true },
                                            onToggleBusinessNumberOnline = { viewModel.toggleBusinessNumberInternet(it) },
                                            onSimulateIncomingMessage = { viewModel.simulateIncomingCustomerMessage() },
                                            onCreateNewChat = { name, phone, labels, notes, msg ->
                                                viewModel.createNewChat(name, phone, labels, notes, msg)
                                            }
                                        )
                                    }
                                }
                            }

                            BizSyncTab.BROADCASTS -> {
                                BroadcastsScreen(
                                    chats = chats,
                                    broadcasts = broadcasts,
                                    isLiveSyncActive = isLiveSyncActive,
                                    onSendBroadcast = { title, labels, body ->
                                        viewModel.sendBroadcastCampaign(title, labels, body)
                                    }
                                )
                            }

                            BizSyncTab.TOOLS -> {
                                BusinessToolsScreen(
                                    session = session,
                                    isDeviceOnline = isDeviceOnline,
                                    isLiveSyncActive = isLiveSyncActive,
                                    rules = rules,
                                    templates = templates,
                                    onOpenQrModal = { showQrModal = true },
                                    onToggleBusinessNumberOnline = { viewModel.toggleBusinessNumberInternet(it) },
                                    onUnlinkSession = { viewModel.unlinkBusinessSession() },
                                    onToggleRule = { id, enabled -> viewModel.toggleRule(id, enabled) },
                                    onSaveRule = { id, type, title, trigger, reply, enabled ->
                                        viewModel.saveRule(id, type, title, trigger, reply, enabled)
                                    },
                                    onSaveTemplate = { shortcut, title, body, category ->
                                        viewModel.saveQuickReply(shortcut, title, body, category)
                                    },
                                    onDeleteTemplate = { viewModel.deleteQuickReply(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showQrModal) {
        QrPairingModal(
            currentSession = session,
            onDismiss = { showQrModal = false },
            onPairSuccess = { bizName, phone, token, devName ->
                viewModel.pairWhatsAppBusinessQr(bizName, phone, token, devName)
            }
        )
    }
}

@Composable
private fun LiveSyncStatusBadge(
    isLiveSyncActive: Boolean,
    isPaired: Boolean,
    isDeviceOnline: Boolean,
    onClick: () -> Unit
) {
    val (bg, dotColor, label) = when {
        isLiveSyncActive -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            Color(0xFF10B981),
            "LIVE & SYNCED"
        )
        !isPaired -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            Color(0xFFEF4444),
            "QR UNPAIRED"
        )
        !isDeviceOnline -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            Color(0xFFF59E0B),
            "APP OFFLINE"
        )
        else -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            Color(0xFFF59E0B),
            "WA NUMBER OFFLINE"
        )
    }

    Surface(
        onClick = onClick,
        color = bg,
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(dotColor, CircleShape)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
