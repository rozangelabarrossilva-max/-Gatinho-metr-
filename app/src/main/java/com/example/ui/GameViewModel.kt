package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.KittySkin
import com.example.data.model.PlayerProfile
import com.example.data.model.PowerUpType
import com.example.data.model.RunRecord
import com.example.data.repository.GameRepository
import com.example.data.repository.RunSaveOutcome
import com.example.data.repository.RunSummaryPayload
import com.example.game.GameEngine
import com.example.game.HudSnapshot
import com.example.game.SoundFxEngine
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class MenuSheet {
    NONE,
    MISSIONS,
    SHOP,
    HISTORY
}

class GameViewModel(
    private val repository: GameRepository,
    private val currentUserId: String,
    private val displayName: String
) : ViewModel() {

    val soundFx = SoundFxEngine()
    val engine = GameEngine(soundFx = soundFx) { finishedHud ->
        handleRunFinished(finishedHud)
    }

    val hudState: StateFlow<HudSnapshot> = engine.hudState

    val profileState: StateFlow<UiState<PlayerProfile>> = repository.observeProfile(currentUserId)
        .map<PlayerProfile?, UiState<PlayerProfile>> { profile ->
            val resolved = profile ?: PlayerProfile(id = currentUserId, userId = currentUserId, displayName = displayName)
            engine.syncProfileConfig(resolved)
            UiState.Success(resolved)
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing profile", rootError)
            emit(UiState.Error(rootError.message ?: "Erro ao carregar perfil"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val recentRunsState: StateFlow<UiState<List<RunRecord>>> = repository.observeRecentRuns(currentUserId)
        .map<List<RunRecord>, UiState<List<RunRecord>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing runs", rootError)
            emit(UiState.Error(rootError.message ?: "Erro ao carregar corridas"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val _activeSheet = MutableStateFlow(MenuSheet.NONE)
    val activeSheet: StateFlow<MenuSheet> = _activeSheet.asStateFlow()

    private val _lastRunOutcome = MutableStateFlow<RunSaveOutcome?>(null)
    val lastRunOutcome: StateFlow<RunSaveOutcome?> = _lastRunOutcome.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureProfileExists(displayName)
                .onFailure { err ->
                    Log.w(TAG, "ensureProfileExists failed", err)
                    _actionMessage.value = err.localizedMessage ?: "Não foi possível sincronizar o perfil."
                }
        }
    }

    fun startRun() {
        _activeSheet.value = MenuSheet.NONE
        _lastRunOutcome.value = null
        val currentProfile = (profileState.value as? UiState.Success)?.data
        if (currentProfile != null) {
            engine.syncProfileConfig(currentProfile)
        }
        engine.startNewRun()
    }

    fun openSheet(sheet: MenuSheet) {
        _activeSheet.value = sheet
    }

    fun closeSheet() {
        _activeSheet.value = MenuSheet.NONE
    }

    fun toggleMute() {
        val next = !_isMuted.value
        _isMuted.value = next
        soundFx.isMuted = next
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun equipOrUnlockSkin(skin: KittySkin) {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.equipOrUnlockSkin(profile, skin)
                .onSuccess {
                    _actionMessage.value = if (profile.unlockedSkins.contains(skin.id)) {
                        "${skin.displayName} equipado!"
                    } else {
                        "${skin.displayName} desbloqueado!"
                    }
                }
                .onFailure { err ->
                    _actionMessage.value = err.localizedMessage ?: "Moedas insuficientes."
                }
        }
    }

    fun upgradePowerUp(type: PowerUpType) {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.upgradePowerUp(profile, type)
                .onSuccess {
                    _actionMessage.value = "${type.title} melhorado!"
                }
                .onFailure { err ->
                    _actionMessage.value = err.localizedMessage ?: "Não foi possível melhorar."
                }
        }
    }

    private fun handleRunFinished(hud: HudSnapshot) {
        val profile = (profileState.value as? UiState.Success)?.data
            ?: PlayerProfile(id = currentUserId, userId = currentUserId, displayName = displayName)

        viewModelScope.launch {
            val payload = RunSummaryPayload(
                score = hud.score,
                coinsCollected = hud.coins,
                distanceMeters = hud.distanceMeters,
                magnetCoins = hud.magnetCoins,
                jetpackFlights = hud.jetpackFlights,
                jetpackMeters = hud.jetpackMeters,
                trainsPassed = hud.trainsPassed,
                skinId = hud.activeSkin.id
            )
            repository.recordCompletedRun(profile, payload)
                .onSuccess { outcome ->
                    _lastRunOutcome.value = outcome
                }
                .onFailure { err ->
                    Log.w(TAG, "Failed to save run", err)
                    _actionMessage.value = "Erro ao salvar corrida na nuvem."
                }
        }
    }

    private companion object {
        const val TAG = "GameVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
