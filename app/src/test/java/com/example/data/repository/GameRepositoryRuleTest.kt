package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.KittySkin
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class GameRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureProfileExists_authenticatedOwner_createsAndObservesProfile() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = GameRepository(firestore, auth)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureProfileExists("Miau Alice")
        }
        assertTrue("Profile creation should succeed: ${createResult.exceptionOrNull()}", createResult.isSuccess)
        val profile = createResult.getOrThrow()
        assertEquals(uid, profile.userId)
        assertEquals(KittySkin.GINGER.id, profile.selectedSkin)

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeProfile(uid).first { it != null }
        }
        assertNotNull(observed)
        assertEquals(uid, observed?.userId)
    }

    @Test
    fun recordCompletedRun_updatesProfileAndAddsRunRecord() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = GameRepository(firestore, auth)

        val profile = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureProfileExists("Miau Alice").getOrThrow()
        }

        val outcomeResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.recordCompletedRun(
                currentProfile = profile,
                summary = RunSummaryPayload(
                    score = 1850L,
                    coinsCollected = 48L,
                    distanceMeters = 410L,
                    magnetCoins = 22L,
                    jetpackFlights = 1L,
                    jetpackMeters = 130L,
                    trainsPassed = 6L,
                    skinId = KittySkin.GINGER.id
                )
            )
        }
        assertTrue("Run save should succeed: ${outcomeResult.exceptionOrNull()}", outcomeResult.isSuccess)
        val outcome = outcomeResult.getOrThrow()
        assertTrue(outcome.isNewHighScore)

        val runs = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeRecentRuns(uid).first { it.isNotEmpty() }
        }
        assertTrue(runs.any { it.score == 1850L && it.magnetCoins == 22L && it.jetpackMeters == 130L })
    }

    @Test
    fun crossUserAccess_readOtherUserProfile_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = GameRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.ensureProfileExists("Alice").getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid).get().await()
            }
            fail("Expected PERMISSION_DENIED reading another user's profile")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", firestoreEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    @Test
    fun observeRecentRuns_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = GameRepository(firestore, auth)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeRecentRuns(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated runs listener")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice_cat@test.com"
        const val BOB_EMAIL = "bob_cat@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
