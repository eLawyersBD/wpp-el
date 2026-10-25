const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read sessions or chats", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("sessions").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("chats").get());
});

test("Authenticated user can create and read their own linked WhatsApp Business session", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const sessionRef = aliceDb.collection("users").doc(ALICE_UID).collection("sessions").doc("primary_session");
  await assertSucceeds(
    sessionRef.set({
      userId: ALICE_UID,
      businessName: "Emerald Roasters Co.",
      phoneNumber: "+1 (555) 019-2834",
      qrToken: "WA-QR-98234-LIVE",
      isPaired: true,
      isBusinessNumberOnline: true,
      deviceName: "Android Companion #1",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(sessionRef.get());
});

test("Authenticated user cannot read or modify another user's chats or messages", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).collection("chats").doc("chat_1").set({
      userId: BOB_UID,
      customerName: "Sarah Jenkins",
      customerPhone: "+1 555-230-9911",
      avatarColor: "#0B6E4F",
      labels: ["VIP"],
      crmNotes: "Repeat wholesale buyer",
      lastMessageText: "Need 20 bags by Friday",
      unreadCount: 1,
      isPinned: true,
      isArchived: false,
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("chats").doc("chat_1").get());
});

test("Shadow update with unauthorized extra field is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const ruleRef = aliceDb.collection("users").doc(ALICE_UID).collection("rules").doc("rule_1");
  await assertFails(
    ruleRef.set({
      userId: ALICE_UID,
      ruleType: "GREETING",
      title: "Welcome Greeting",
      triggerCondition: "First message",
      replyMessage: "Thanks for reaching out!",
      isEnabled: true,
      isVerifiedAdmin: true,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Client query alignment: list query with matching userId filter succeeds", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("templates").doc("tpl_1").set({
      userId: ALICE_UID,
      shortcut: "/price",
      title: "Standard Pricing",
      body: "Our wholesale catalog starts at $18/unit.",
      category: "Sales",
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("templates").where("userId", "==", ALICE_UID).get()
  );
});
