package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BizSyncRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun linkBusinessSession_and_createChat_succeedsForAuthenticatedOwner() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = BizSyncRepository(firestore)

        val sessionResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.linkBusinessSession(
                businessName = "Emerald Coffee Co",
                phoneNumber = "+1 555-010-9988",
                qrToken = "WA-QR-TEST-001"
            )
        }
        assertTrue(sessionResult.isSuccess)

        val chatResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createCustomerChat(
                customerName = "Liam Carter",
                customerPhone = "+1 555-222-3333",
                labels = listOf("VIP")
            )
        }
        assertTrue(chatResult.isSuccess)
        val chatId = chatResult.getOrThrow()

        val msgResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.sendMessage(
                chatId = chatId,
                text = "Hello Liam, your order is ready!",
                senderType = "BUSINESS",
                isLiveConnected = true
            )
        }
        assertTrue(msgResult.isSuccess)

        val emittedChats = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeChats(uid).first { list -> list.any { it.id == chatId } }
        }
        assertTrue(emittedChats.any { it.customerName == "Liam Carter" })
    }

    @Test
    fun getChatById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = BizSyncRepository(firestore)
        val chatId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createCustomerChat(
                customerName = "Private Client",
                customerPhone = "+1 555-999-0000"
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = BizSyncRepository(firestore)
        val crossResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getChatById(aliceUid, chatId)
        }
        assertTrue(crossResult.isFailure)
        val firestoreEx = crossResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun observeChats_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = BizSyncRepository(firestore)

        var caughtException: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeChats(aliceUid).first()
            }
        } catch (e: Throwable) {
            caughtException = e
        }
        assertNotNull(caughtException)
        val firestoreException = checkNotNull(caughtException?.unwrapFirestoreException()) {
            "Expected nested FirebaseFirestoreException, got $caughtException"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@bizsync.test"
        const val BOB_EMAIL = "bob@bizsync.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
