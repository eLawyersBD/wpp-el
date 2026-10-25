# BizSync WA — Live WhatsApp Business Companion & CRM

BizSync WA is an Android business messaging companion app that pairs with a WhatsApp Business number via QR code and maintains a live, cloud-synced connection whenever both the business number and mobile app are online. It empowers business owners and support teams to read and send live messages, attach media, organize customers with CRM labels and notes, trigger quick replies, run broadcast campaigns, and automate greeting and away responses.

## User Review & Critical Decisions

> [!IMPORTANT]
> **Confirmed Product Scope & Architecture**
> All preferences from our initial clarification have been incorporated into this plan:

- **Connection & Pairing Model**: **Cloud-Synced Team Messaging Simulator with QR Device Pairing** — Supports live camera QR code scanning as well as an interactive QR pairing generator/pairing code handshake to link a WhatsApp Business number (`+1 (555) 019-2834` or custom business number), backed by Firebase Firestore for real-time multi-device synchronization and `ConnectivityManager` for live internet status monitoring.
- **Business Messaging Suite**:
  - **Quick Reply Templates & Canned Responses** with `/shortcut` trigger support in the chat composer.
  - **Customer Labels & CRM Contact Notes** (e.g., *New Lead*, *VIP*, *Pending Payment*, *Order Shipped*, plus rich timestamped notes per customer).
  - **Rich Attachments**: Image picker (Android Photo Picker), document attachments, and interactive voice note recording/playback inside conversations.
  - **Broadcast Lists & Bulk Campaigns** to send targeted announcements or promotions across labeled customer segments at once.
- **Automation & Response Workflow**: **Manual Live Replies + Configurable Auto-Greeting & Away Rules** — Automatically dispatches instant welcome greetings to first-time customer inquiries or scheduled away replies when outside business hours, alongside a built-in **"Simulate Customer Message"** trigger so you can test live incoming sync and auto-replies in real time.

---

## 1. Overview & Core Concept

- **What It Does**: Provides a dedicated mobile command center linked to a business's WhatsApp number via QR code. As long as the business number session and the device have active internet connectivity, all incoming and outgoing customer messages, delivery receipts (`Sent`, `Delivered`, `Read`), CRM tags, and automated rules stay synchronized in real time.
- **Target Audience / Persona**: Small-to-medium business owners, sales reps, and customer support operators who manage customer conversations, orders, and follow-ups on a shared WhatsApp Business number.
- **Key Value**: Combines instant QR session linking and live connection health monitoring with high-efficiency business tools—canned responses, CRM contact notes, bulk broadcasts, and automated greeting/away rules—in one cohesive workspace.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Authentication & Workspace Entry**:
   - Users sign in securely with Google Sign-In (`CredentialManager`) to access their cloud-synced business workspace.
2. **QR Code Business Number Pairing & Live Sync Status**:
   - From the **Link Business Number** screen (or top status banner), users can scan a WhatsApp Business QR code using the live camera scanner or use the built-in interactive **Instant QR Handshake** to pair their business number and configure the business profile (Business Name, WhatsApp Business Phone Number, Category).
   - A persistent **Live Sync Pulse Bar** in the top app bar shows real-time status:
     - **Live & Synced**: Device internet is online AND the WhatsApp Business number session is online.
     - **Business Number Offline / Paused**: Simulates or reflects when the host WhatsApp Business phone loses internet connection, queuing outbound messages until reconnected.
     - **Device Offline**: Detects actual Android network loss via `ConnectivityManager` and displays offline queuing status.
3. **Live Inbox & Rich Messaging**:
   - Filterable conversation list (`All`, `Unread`, `VIP`, `Pending Payment`, `New Lead`, `Archived`) with search, unread badges, customer label pills, and last-message delivery ticks.
   - Inside a conversation thread:
     - Send text messages, attach photos via the zero-permission Android Photo Picker, attach documents/catalogs, or record/send voice notes with waveform visualization.
     - Type `/` or tap the **Quick Reply** lightning button to insert canned responses with dynamic placeholders (e.g., `{{customer_name}}`, `{{business_name}}`).
     - Tap **"Simulate Incoming Reply"** in the thread header or inbox to immediately test live incoming customer messages and observe automatic greeting/away rule triggers.
4. **Customer CRM Drawer / Sheet**:
   - Tapping a customer's header opens their **CRM Profile**: assign color-coded labels, edit deal value/company details, and add or review timestamped internal CRM notes.
5. **Broadcasts & Bulk Campaigns**:
   - Create broadcast lists filtered by customer labels (e.g., all *VIP* or *New Lead* contacts) or manual selection, preview the message or template, and dispatch bulk messages that populate directly into each recipient's live chat thread.
6. **Business Tools (Quick Replies & Auto-Reply Rules)**:
   - Manage `/shortcut` canned responses and configure **Auto-Greeting** (for first message from a customer) and **Away Message** rules (when toggle is active or outside business hours) with custom response text and delay settings.

### Visual Identity & Theme

- **Aesthetic Direction**: **Modern Emerald Command Studio** — High-craft, tactile messaging interface inspired by contemporary fintech and enterprise communication tools, pairing crisp typographic hierarchy with unmistakable live-connection telemetry.
- **Color Palette & Mood**:
  - **Primary (`#0B6E4F` Light / `#34D399` Dark)**: Deep, authoritative Emerald Green for primary actions, active sync indicators, and outbound message accents.
  - **Secondary (`#0F4C5C` Light / `#38BDF8` Dark)**: Slate Teal for CRM badges, broadcast cards, and secondary controls.
  - **Tertiary (`#D97706` Light / `#FBBF24` Dark)**: Warm Amber for automation triggers, away-mode banners, and VIP customer tags.
  - **Surface & Conversation Canvas**: Warm alabaster (`#F4F7F5`) in light mode and rich obsidian (`#0B141A`) in dark mode, with subtle geometric chat-canvas depth and elevated message bubbles (`#D9FDD3` / `#005C4B` for outbound business messages; `#FFFFFF` / `#1F2C34` for incoming customer messages).
- **Typography & Hierarchy**:
  - **Display & Headings**: `Plus Jakarta Sans` (bundled locally via `font-util`) for crisp headers, customer names, and metric cards.
  - **Body & Chat Messages**: `Inter` (bundled locally via `font-util`) for effortless readability across dense message threads and CRM notes.
  - **Monospace Telemetry**: `JetBrains Mono` (bundled locally via `font-util`) for phone numbers, QR session tokens, `/shortcut` tags, and sync latency readouts.
- **Interactive Feedback & Motion**:
  - Pulsing emerald connection dot when both device and business number are online.
  - Animated double-checkmarks transitioning from `Pending` (clock) -> `Sent` (single tick) -> `Delivered` (double tick) -> `Read` (emerald/blue double tick).
  - Smooth spring transitions for the `/shortcut` quick-reply suggestion bar and CRM bottom sheets.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Dual Live Connectivity Model (Real Device Network + Linked Number Session State)**
  - *Chosen Approach*: Combine Android's `ConnectivityManager.NetworkCallback` (real hardware internet state) with a cloud-synced `LinkedDeviceSession` document in Firestore that tracks whether the linked WhatsApp Business number is online.
  - *Why*: Directly fulfills the requirement that *"If the WhatsApp business number is connected to the internet, this app will also be connected to the internet. Then the sync with this WhatsApp number will be live, and this app can send the message and read the message."* Users can also toggle the simulated business number's internet state to observe offline message queuing and instant live re-sync when connection is restored.
- **Decision 2: Camera QR Scanner + Instant Interactive QR Handshake**
  - *Chosen Approach*: Provide both a live camera QR scanner (`CAMERA` permission with graceful runtime permission handling) and a one-tap **"Scan / Pair Demo Business QR"** interactive modal displaying a scannable QR matrix and session verification handshake.
  - *Why*: Allows users on an emulator or without a second physical screen in front of them to complete the full QR code pairing flow seamlessly while still supporting real camera QR scanning on physical devices.
- **Decision 3: Cloud Persistence via Firebase Firestore & Google Sign-In**
  - *Chosen Approach*: Store all linked sessions, customer conversations, messages, CRM notes, quick reply templates, broadcasts, and automation rules under the authenticated user's workspace in Firestore (`/users/{userId}/...`).
  - *Why*: Ensures real-time snapshot listeners (`snapshots()`) immediately push incoming and outgoing messages across screens and devices without losing business data on app restart.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```
┌────────────────────────────────────────────────────────────────────────────┐
│                        MainActivity (Edge-to-Edge)                         │
│             AuthGateComposable (Google Sign-In / CredentialManager)        │
└─────────────────────────────────────┬──────────────────────────────────────┘
                                      │ Authenticated User (uid)
                                      ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                     BizSyncAppShell (Adaptive Scaffold)                    │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │ LiveSyncHeaderBar (Device Net State + Business Number Live Status)   │  │
│  └──────────┬───────────────────────┬───────────────────────┬───────────┘  │
│             │                       │                       │              │
│             ▼                       ▼                       ▼              │
│  ┌────────────────────┐  ┌────────────────────┐  ┌──────────────────────┐  │
│  │   Chats & Thread   │  │   Broadcasts Hub   │  │ Business Tools & QR  │  │
│  │  - Inbox Filters   │  │  - Segment Picker  │  │  - QR Device Linker  │  │
│  │  - Live Chat View  │  │  - Campaign Composer│  │  - Quick Replies     │  │
│  │  - /Quick Replies  │  │  - Delivery Stats  │  │  - Auto-Reply Rules  │  │
│  │  - Attachments Bar │  └──────────┬─────────┘  └──────────┬───────────┘  │
│  │  - CRM Drawer      │             │                       │              │
│  └──────────┬─────────┘             │                       │              │
└─────────────┼───────────────────────┼───────────────────────┼──────────────┘
              │                       │                       │
              ▼                       ▼                       ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                   BizSyncViewModel (StateFlow + UI State)                  │
│   - Reactive flows for Conversations, Messages, Templates, Broadcasts      │
│   - Automation Engine (evaluates Greeting & Away rules on incoming msg)    │
│   - Offline Queue Processor (flushes queued messages when both are online) │
└─────────────────────────────┬───────────────────────────────┬──────────────┘
                              │                               │
                              ▼                               ▼
┌──────────────────────────────────────────┐   ┌─────────────────────────────┐
│        BizSyncFirestoreRepository        │   │   NetworkConnectivityMonitor│
│  /users/{uid}/sessions/{sessionId}       │   │   (ConnectivityManager      │
│  /users/{uid}/chats/{chatId}             │   │    NetworkCallback Flow)    │
│  /users/{uid}/chats/{chatId}/messages    │   └─────────────────────────────┘
│  /users/{uid}/templates/{templateId}     │
│  /users/{uid}/broadcasts/{broadcastId}   │
│  /users/{uid}/rules/{ruleId}             │
└──────────────────────────────────────────┘
```

### Data Model & State

1. **`LinkedBusinessSession`** (`/users/{userId}/sessions/{sessionId}`):
   - `businessName`, `phoneNumber`, `qrToken`, `isPaired`, `isBusinessNumberOnline`, `deviceName`, `pairedAt`, `lastSyncAt`.
2. **`CustomerChat`** (`/users/{userId}/chats/{chatId}`):
   - `customerName`, `customerPhone`, `avatarColor`, `labels` (list of CRM tag strings), `crmNotes` (multiline notes), `lastMessageText`, `lastMessageTime`, `unreadCount`, `isPinned`, `isArchived`.
3. **`ChatMessage`** (`/users/{userId}/chats/{chatId}/messages/{messageId}`):
   - `chatId`, `senderType` (`BUSINESS`, `CUSTOMER`, `AUTO_RULE`), `text`, `attachmentType` (`NONE`, `IMAGE`, `DOCUMENT`, `VOICE_NOTE`), `attachmentUri`, `attachmentMeta` (filename/duration), `deliveryStatus` (`QUEUED`, `SENT`, `DELIVERED`, `READ`), `createdAt`.
4. **`QuickReplyTemplate`** (`/users/{userId}/templates/{templateId}`):
   - `shortcut` (e.g., `/price`, `/shipping`, `/hours`), `title`, `body`, `category`.
5. **`BroadcastCampaign`** (`/users/{userId}/broadcasts/{broadcastId}`):
   - `title`, `targetLabels`, `recipientCount`, `messageBody`, `status`, `createdAt`.
6. **`AutomationRule`** (`/users/{userId}/rules/{ruleId}`):
   - `ruleType` (`GREETING`, `AWAY`, `KEYWORD`), `title`, `triggerCondition`, `replyMessage`, `isEnabled`.

### Interactive Component & State Mapping

- **QR Pairing Flow**: Scanning a QR code (via CameraX barcode/QR analyzer or interactive QR Pairing simulator) writes an active `LinkedBusinessSession` to Firestore and seeds initial realistic business conversations, templates, and rules if the workspace is empty.
- **Live Connectivity & Message Sync**:
  - When both `deviceNetworkOnline == true` and `session.isBusinessNumberOnline == true`, sending a message immediately writes `SENT` -> transitions to `DELIVERED`/`READ`, and any messages with `QUEUED` status are automatically flushed and delivered.
  - If either the device or the linked WhatsApp Business number is toggled offline, new outbound messages are saved with `QUEUED` status and a clear banner explains that messages will sync automatically as soon as the WhatsApp Business number reconnects.
- **Attachment & `/shortcut` Composer**:
  - Typing `/` in the message input surfaces a floating list of matching `QuickReplyTemplate` items; tapping one substitutes variables like `{{customer_name}}` and populates the composer.
  - Tapping the attachment button opens a selector for **Photo** (`PickVisualMedia`), **Document** (`OpenDocument`), or **Voice Note** (interactive recorder with live timer and waveform preview).
- **Automated Greeting & Away Engine**:
  - When a customer message arrives (or is triggered via the **"Simulate Customer Message"** action), the ViewModel checks active `AutomationRule` items (`GREETING` for first-time messages or `AWAY` when away mode is enabled) and automatically dispatches the configured business reply into the live chat thread.
