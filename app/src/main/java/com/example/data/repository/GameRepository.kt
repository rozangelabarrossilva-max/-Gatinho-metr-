package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.KittySkin
import com.example.data.model.PlayerProfile
import com.example.data.model.PowerUpType
import com.example.data.model.RunRecord
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class RunSummaryPayload(
    val score: Long,
    val coinsCollected: Long,
    val distanceMeters: Long,
    val magnetCoins: Long,
    val jetpackFlights: Long,
    val jetpackMeters: Long,
    val trainsPassed: Long,
    val skinId: String
)

data class RunSaveOutcome(
    val isNewHighScore: Boolean,
    val missionTierAdvanced: Boolean,
    val newMultiplier: Long,
    val bonusCoinsAwarded: Long
)

class GameRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Usuário precisa estar autenticado com o Google para acessar o Firestore.")
    }

    fun observeProfile(userId: String): Flow<PlayerProfile?> = flow {
        val docRef = db.collection("users").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            PlayerProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path, auth)
                    }
                    throw error
                }
        )
    }

    fun observeRecentRuns(userId: String): Flow<List<RunRecord>> = flow {
        val colRef = db.collection("users").document(userId).collection("runs")
        emitAll(
            colRef.orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(15)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        RunRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path, auth)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureProfileExists(displayName: String): Result<PlayerProfile> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                val existing = snap.toObject(
                    PlayerProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: PlayerProfile(id = uid, userId = uid)
                Result.success(existing)
            } else {
                val safeName = displayName.trim().ifBlank { "Gatinho" }.take(60)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "highScore" to 0L,
                    "totalCoins" to 0L,
                    "totalRuns" to 0L,
                    "scoreMultiplier" to 1L,
                    "selectedSkin" to KittySkin.GINGER.id,
                    "unlockedSkins" to listOf(KittySkin.GINGER.id),
                    "magnetLevel" to 1L,
                    "jetpackLevel" to 1L,
                    "missionTier" to 1L,
                    "missionCoinsWithMagnet" to 0L,
                    "missionJetpackFlights" to 0L,
                    "missionTotalMeters" to 0L,
                    "missionTrainsPassed" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                val created = docRef.get().await().toObject(
                    PlayerProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: PlayerProfile(id = uid, userId = uid, displayName = safeName)
                Result.success(created)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun recordCompletedRun(
        currentProfile: PlayerProfile,
        summary: RunSummaryPayload
    ): Result<RunSaveOutcome> {
        val uid = requireUserId()
        val profileRef = db.collection("users").document(uid)
        val runId = UUID.randomUUID().toString()
        val runRef = profileRef.collection("runs").document(runId)

        return try {
            val newMagnetCoins = currentProfile.missionCoinsWithMagnet + summary.magnetCoins
            val newJetpackFlights = currentProfile.missionJetpackFlights + summary.jetpackFlights
            val newTotalMeters = currentProfile.missionTotalMeters + summary.distanceMeters
            val newTrainsPassed = currentProfile.missionTrainsPassed + summary.trainsPassed

            val updatedPreview = currentProfile.copy(
                missionCoinsWithMagnet = newMagnetCoins,
                missionJetpackFlights = newJetpackFlights,
                missionTotalMeters = newTotalMeters,
                missionTrainsPassed = newTrainsPassed
            )
            val allCompleted = updatedPreview.activeMissions().all { it.isCompleted }

            val nextTier = if (allCompleted) (currentProfile.missionTier + 1L).coerceAtMost(100L) else currentProfile.missionTier
            val nextMultiplier = if (allCompleted) (currentProfile.scoreMultiplier + 1L).coerceAtMost(50L) else currentProfile.scoreMultiplier
            val bonusCoins = if (allCompleted) 50L * currentProfile.missionTier else 0L

            val isNewHigh = summary.score > currentProfile.highScore
            val nextHighScore = maxOf(currentProfile.highScore, summary.score)
            val nextTotalCoins = currentProfile.totalCoins + summary.coinsCollected + bonusCoins

            val profileUpdates = mapOf(
                "highScore" to nextHighScore,
                "totalCoins" to nextTotalCoins,
                "totalRuns" to (currentProfile.totalRuns + 1L),
                "scoreMultiplier" to nextMultiplier,
                "missionTier" to nextTier,
                "missionCoinsWithMagnet" to (if (allCompleted) 0L else newMagnetCoins),
                "missionJetpackFlights" to (if (allCompleted) 0L else newJetpackFlights),
                "missionTotalMeters" to (if (allCompleted) 0L else newTotalMeters),
                "missionTrainsPassed" to (if (allCompleted) 0L else newTrainsPassed),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val safeSkin = KittySkin.fromId(summary.skinId).id
            val runPayload = mapOf(
                "userId" to uid,
                "score" to summary.score.coerceAtLeast(0L),
                "coinsCollected" to summary.coinsCollected.coerceAtLeast(0L),
                "distanceMeters" to summary.distanceMeters.coerceAtLeast(0L),
                "magnetCoins" to summary.magnetCoins.coerceAtLeast(0L),
                "jetpackMeters" to summary.jetpackMeters.coerceAtLeast(0L),
                "skinId" to safeSkin,
                "createdAt" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()
            batch.update(profileRef, profileUpdates)
            batch.set(runRef, runPayload)
            batch.commit().await()

            Result.success(
                RunSaveOutcome(
                    isNewHighScore = isNewHigh,
                    missionTierAdvanced = allCompleted,
                    newMultiplier = nextMultiplier,
                    bonusCoinsAwarded = bonusCoins
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, profileRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun equipOrUnlockSkin(
        profile: PlayerProfile,
        skin: KittySkin
    ): Result<Unit> {
        val uid = requireUserId()
        val profileRef = db.collection("users").document(uid)
        val alreadyUnlocked = profile.unlockedSkins.contains(skin.id)
        if (!alreadyUnlocked && profile.totalCoins < skin.priceCoins) {
            return Result.failure(IllegalStateException("Moedas insuficientes para desbloquear ${skin.displayName}."))
        }
        return try {
            val newUnlocked = if (alreadyUnlocked) profile.unlockedSkins else (profile.unlockedSkins + skin.id).distinct()
            val newCoins = if (alreadyUnlocked) profile.totalCoins else profile.totalCoins - skin.priceCoins
            profileRef.update(
                mapOf(
                    "selectedSkin" to skin.id,
                    "unlockedSkins" to newUnlocked,
                    "totalCoins" to newCoins,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, profileRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun upgradePowerUp(
        profile: PlayerProfile,
        type: PowerUpType
    ): Result<Unit> {
        val uid = requireUserId()
        val profileRef = db.collection("users").document(uid)
        val currentLevel = when (type) {
            PowerUpType.MAGNET -> profile.magnetLevel
            PowerUpType.JETPACK -> profile.jetpackLevel
        }
        if (currentLevel >= 5L) {
            return Result.failure(IllegalStateException("Nível máximo já alcançado!"))
        }
        val cost = upgradeCostForLevel(currentLevel)
        if (profile.totalCoins < cost) {
            return Result.failure(IllegalStateException("Moedas insuficientes!"))
        }
        return try {
            val fieldName = when (type) {
                PowerUpType.MAGNET -> "magnetLevel"
                PowerUpType.JETPACK -> "jetpackLevel"
            }
            profileRef.update(
                mapOf(
                    fieldName to (currentLevel + 1L),
                    "totalCoins" to (profile.totalCoins - cost),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, profileRef.path, auth)
            Result.failure(e)
        }
    }

    companion object {
        fun upgradeCostForLevel(currentLevel: Long): Long = when (currentLevel) {
            1L -> 75L
            2L -> 150L
            3L -> 250L
            4L -> 400L
            else -> 0L
        }
    }
}
