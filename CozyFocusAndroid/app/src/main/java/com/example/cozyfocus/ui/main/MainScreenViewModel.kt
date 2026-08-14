package com.example.cozyfocus.ui.main

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cozyfocus.audio.AmbientAudioPlayer
import com.example.cozyfocus.audio.AmbientSound
import com.example.cozyfocus.audio.AnimalSoundSynthesizer
import com.example.cozyfocus.audio.MeditationBellPlayer
import com.example.cozyfocus.data.DataRepository
import com.example.cozyfocus.model.CompanionAnimal
import com.example.cozyfocus.model.Cosmetic
import com.example.cozyfocus.services.DistractionShieldManager
import com.example.cozyfocus.services.HapticsManager
import com.example.cozyfocus.ui.components.ShareCardGenerator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max

data class MainUiState(
    val remainingSeconds: Long = 1500L,
    val isRunning: Boolean = false,
    val isComplete: Boolean = false,
    val durationIndex: Int = 5,
    val selectedCompanion: CompanionAnimal = CompanionAnimal.RED_PANDA,
    val equippedCosmetic: Cosmetic? = null,
    val coinBalance: Int = 0,
    val hapticsEnabled: Boolean = true,
    val selectedSound: AmbientSound = AmbientSound.RAINFALL,
    val isShielding: Boolean = false,
    val shieldStatusText: String = "Not enabled",
    val completionToastMessage: String? = null
)

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {
    val repository = DataRepository(application)
    private val animalSynthesizer = AnimalSoundSynthesizer()
    private val ambientPlayer = AmbientAudioPlayer(application)
    private val meditationBell = MeditationBellPlayer(application)
    private val hapticsManager = HapticsManager(application)
    private val shieldManager = DistractionShieldManager(application)

    val durationOptions = listOf(1, 2, 5, 10, 15, 25, 30, 35, 40, 45, 50, 55, 60).map { it * 60L }

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var lastPulseMinute = -1

    val coinLedger = repository.coinLedger.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val inventory = repository.inventory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            repository.coinLedger.collect { entries ->
                val balance = entries.sumOf { it.amount }
                _uiState.update { it.copy(coinBalance = balance) }
            }
        }

        viewModelScope.launch {
            repository.timerState.collect { prefs ->
                val companion = CompanionAnimal.fromRaw(prefs.selectedCompanion)
                val cosmetic = Cosmetic.fromRaw(prefs.equippedCosmetic)
                val sessionDuration = durationOptions.getOrElse(prefs.durationIndex) { 1500L }

                val now = System.currentTimeMillis()
                if (prefs.targetDeadlineTimestamp != null) {
                    val secondsLeft = max(0L, (prefs.targetDeadlineTimestamp - now) / 1000)
                    if (secondsLeft > 0) {
                        _uiState.update {
                            it.copy(
                                remainingSeconds = secondsLeft,
                                isRunning = true,
                                isComplete = false,
                                durationIndex = prefs.durationIndex,
                                selectedCompanion = companion,
                                equippedCosmetic = cosmetic,
                                hapticsEnabled = prefs.hapticsEnabled
                            )
                        }
                        ambientPlayer.play()
                        startTicker()
                    } else {
                        _uiState.update {
                            it.copy(
                                remainingSeconds = 0L,
                                isRunning = false,
                                isComplete = true,
                                durationIndex = prefs.durationIndex,
                                selectedCompanion = companion,
                                equippedCosmetic = cosmetic,
                                hapticsEnabled = prefs.hapticsEnabled
                            )
                        }
                    }
                } else {
                    val remaining = prefs.pausedRemainingSeconds ?: sessionDuration
                    _uiState.update {
                        it.copy(
                            remainingSeconds = remaining,
                            isRunning = false,
                            isComplete = false,
                            durationIndex = prefs.durationIndex,
                            selectedCompanion = companion,
                            equippedCosmetic = cosmetic,
                            hapticsEnabled = prefs.hapticsEnabled
                        )
                    }
                }
            }
        }
    }

    val currentSessionDuration: Long
        get() = durationOptions.getOrElse(uiState.value.durationIndex) { 1500L }

    val durationAdjective: String
        get() = "${(currentSessionDuration / 60)}-minute"

    val durationText: String
        get() {
            val mins = (currentSessionDuration / 60).toInt()
            return "$mins minute${if (mins == 1) "" else "s"}"
        }

    val primaryButtonLabel: String
        get() {
            val state = uiState.value
            if (state.isRunning) return "Pause gently"
            if (state.isComplete) return "Begin another $durationAdjective session"
            if (state.remainingSeconds < currentSessionDuration) return "Resume focus"
            return "Begin $durationAdjective focus"
        }

    val canStop: Boolean
        get() {
            val state = uiState.value
            return state.isRunning || state.remainingSeconds < currentSessionDuration || state.isComplete
        }

    fun toggleTimer() {
        if (uiState.value.isRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    private fun startTimer() {
        val remaining = if (uiState.value.isComplete) currentSessionDuration else uiState.value.remainingSeconds
        val deadline = System.currentTimeMillis() + (remaining * 1000)
        _uiState.update { it.copy(isRunning = true, isComplete = false, remainingSeconds = remaining) }
        viewModelScope.launch { repository.preferences.startTimer(deadline) }
        ambientPlayer.play()
        startTicker()
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        ambientPlayer.stop()
        shieldManager.disableShielding()
        val remaining = uiState.value.remainingSeconds
        _uiState.update {
            it.copy(
                isRunning = false,
                isShielding = false,
                shieldStatusText = shieldManager.statusText
            )
        }
        viewModelScope.launch { repository.preferences.pauseTimer(remaining) }
    }

    fun stopTimer() {
        timerJob?.cancel()
        ambientPlayer.stop()
        shieldManager.disableShielding()
        lastPulseMinute = -1
        val duration = currentSessionDuration
        _uiState.update {
            it.copy(
                remainingSeconds = duration,
                isRunning = false,
                isComplete = false,
                isShielding = false,
                shieldStatusText = shieldManager.statusText
            )
        }
        viewModelScope.launch { repository.preferences.resetTimer() }
    }

    fun chooseDuration(index: Int) {
        if (uiState.value.isRunning) return
        val boundedIndex = index.coerceIn(0, durationOptions.lastIndex)
        val duration = durationOptions[boundedIndex]
        lastPulseMinute = -1
        _uiState.update { it.copy(durationIndex = boundedIndex, remainingSeconds = duration, isComplete = false) }
        viewModelScope.launch { repository.preferences.saveDurationIndex(boundedIndex) }
    }

    fun selectCompanion(companion: CompanionAnimal) {
        _uiState.update { it.copy(selectedCompanion = companion) }
        viewModelScope.launch {
            repository.preferences.setSelectedCompanion(companion.id)
            animalSynthesizer.playGreeting(companion)
        }
    }

    fun selectAmbientSound(sound: AmbientSound) {
        ambientPlayer.setSelectedSound(sound)
        _uiState.update { it.copy(selectedSound = sound) }
    }

    fun toggleHaptics() {
        val newEnabled = !uiState.value.hapticsEnabled
        _uiState.update { it.copy(hapticsEnabled = newEnabled) }
        viewModelScope.launch {
            repository.preferences.setHapticsEnabled(newEnabled)
        }
    }

    fun toggleDistractionShielding() {
        shieldManager.toggleShielding()
        _uiState.update {
            it.copy(
                isShielding = shieldManager.isShielding,
                shieldStatusText = shieldManager.statusText
            )
        }
    }

    fun purchaseCosmetic(cosmetic: Cosmetic) {
        viewModelScope.launch {
            val balance = uiState.value.coinBalance
            val success = repository.purchaseCosmetic(cosmetic.id, cosmetic.price, balance)
            if (success) {
                _uiState.update { it.copy(equippedCosmetic = cosmetic) }
            }
        }
    }

    fun equipCosmetic(cosmetic: Cosmetic) {
        viewModelScope.launch {
            repository.preferences.setEquippedCosmetic(cosmetic.id)
            _uiState.update { it.copy(equippedCosmetic = cosmetic) }
        }
    }

    fun shareJourneyCard(context: Context, completedSessions: Int, totalMinutes: Int) {
        val state = uiState.value
        ShareCardGenerator.generateAndShare(
            context = context,
            companion = state.selectedCompanion,
            cosmetic = state.equippedCosmetic,
            completedSessions = completedSessions,
            totalMinutes = totalMinutes
        )
    }

    private fun startTicker() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.isRunning && _uiState.value.remainingSeconds > 0) {
                delay(1000)
                _uiState.update { current ->
                    val nextRemaining = max(0L, current.remainingSeconds - 1)
                    val elapsedMinutes = ((currentSessionDuration - nextRemaining) / 60).toInt()

                    if (current.hapticsEnabled && elapsedMinutes > 0 && elapsedMinutes % 5 == 0 && elapsedMinutes != lastPulseMinute) {
                        hapticsManager.pulseSoft()
                        lastPulseMinute = elapsedMinutes
                    }

                    if (nextRemaining == 0L) {
                        onTimerComplete()
                        current.copy(remainingSeconds = 0L, isRunning = false, isComplete = true)
                    } else {
                        current.copy(remainingSeconds = nextRemaining)
                    }
                }
            }
        }
    }

    private fun onTimerComplete() {
        ambientPlayer.stop()
        shieldManager.disableShielding()
        meditationBell.play()
        hapticsManager.pulseSuccess()

        viewModelScope.launch {
            repository.completeSession(
                durationSeconds = currentSessionDuration,
                companionRaw = uiState.value.selectedCompanion.id
            )
            _uiState.update {
                it.copy(
                    isShielding = false,
                    shieldStatusText = shieldManager.statusText,
                    completionToastMessage = "+5 cozy coins — you did it"
                )
            }
            delay(3000)
            _uiState.update { it.copy(completionToastMessage = null) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ambientPlayer.stop()
    }
}
