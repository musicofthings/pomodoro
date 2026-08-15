package com.cozyfocus.app.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cozyfocus.app.audio.AmbientAudioPlayer
import com.cozyfocus.app.audio.AmbientSound
import com.cozyfocus.app.audio.AnimalSoundSynthesizer
import com.cozyfocus.app.audio.MeditationBellPlayer
import com.cozyfocus.app.data.DataRepository
import com.cozyfocus.app.data.preferences.TimerState
import com.cozyfocus.app.model.CompanionAnimal
import com.cozyfocus.app.model.Cosmetic
import com.cozyfocus.app.notifications.CompletionNotificationManager
import com.cozyfocus.app.services.FocusSettingsManager
import com.cozyfocus.app.services.HapticsManager
import com.cozyfocus.app.timer.CompletionAlarmScheduler
import com.cozyfocus.app.timer.CompletionResult
import com.cozyfocus.app.timer.FocusTimerMath
import com.cozyfocus.app.timer.SessionCompletionCoordinator
import com.cozyfocus.app.ui.components.ShareCardGenerator
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val focusSettingsStatusText: String = "Use Android Focus or Do Not Disturb settings",
    val completionAlertsEnabled: Boolean = false,
    val notificationPermissionRequested: Boolean = false,
    val completionToastMessage: String? = null
)

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {
    val repository = DataRepository(application)
    private val animalSynthesizer = AnimalSoundSynthesizer()
    private val ambientPlayer = AmbientAudioPlayer(application)
    private val meditationBell = MeditationBellPlayer(application)
    private val hapticsManager = HapticsManager(application)
    private val focusSettings = FocusSettingsManager(application)
    private val alarmScheduler = CompletionAlarmScheduler(application)
    private val notifications = CompletionNotificationManager(application)
    private val completionCoordinator = SessionCompletionCoordinator(
        context = application,
        repository = repository,
        scheduler = alarmScheduler,
        notifications = notifications
    )

    val durationOptions = listOf(1, 2, 5, 10, 15, 25, 30, 35, 40, 45, 50, 55, 60)
        .map { it * 60L }

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var completionJob: Job? = null
    private var toastJob: Job? = null
    private var lastPulseMinute = -1
    private var currentDeadlineTimestamp: Long? = null
    private var latestTimerState = TimerState()
    private var lastAnnouncedCompletionId: String? = null

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
                _uiState.update { it.copy(coinBalance = entries.sumOf { entry -> entry.amount }) }
            }
        }

        viewModelScope.launch {
            repository.timerState.collect(::applyPersistedTimerState)
        }
    }

    val currentSessionDuration: Long
        get() = durationOptions.getOrElse(uiState.value.durationIndex) { 1500L }

    val durationAdjective: String
        get() = "${currentSessionDuration / 60}-minute"

    val durationText: String
        get() {
            val minutes = currentSessionDuration / 60
            return "$minutes minute${if (minutes == 1L) "" else "s"}"
        }

    val primaryButtonLabel: String
        get() = when {
            uiState.value.isRunning -> "Pause gently"
            uiState.value.isComplete -> "Begin another $durationAdjective session"
            uiState.value.remainingSeconds < currentSessionDuration -> "Resume focus"
            else -> "Begin $durationAdjective focus"
        }

    val canStop: Boolean
        get() = uiState.value.let {
            it.isRunning || it.remainingSeconds < currentSessionDuration || it.isComplete
        }

    fun toggleTimer() {
        if (uiState.value.isRunning) pauseTimer() else startTimer()
    }

    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        alarmScheduler.cancel()
        ambientPlayer.stop()
        lastPulseMinute = -1
        currentDeadlineTimestamp = null
        _uiState.update {
            it.copy(
                remainingSeconds = currentSessionDuration,
                isRunning = false,
                isComplete = false
            )
        }
        viewModelScope.launch { repository.preferences.resetTimer() }
    }

    fun chooseDuration(index: Int) {
        if (uiState.value.isRunning) return
        val boundedIndex = index.coerceIn(0, durationOptions.lastIndex)
        val duration = durationOptions[boundedIndex]
        lastPulseMinute = -1
        currentDeadlineTimestamp = null
        _uiState.update {
            it.copy(durationIndex = boundedIndex, remainingSeconds = duration, isComplete = false)
        }
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
        viewModelScope.launch { repository.preferences.setSelectedSound(sound.id) }
    }

    fun toggleHaptics() {
        val enabled = !uiState.value.hapticsEnabled
        _uiState.update { it.copy(hapticsEnabled = enabled) }
        viewModelScope.launch { repository.preferences.setHapticsEnabled(enabled) }
    }

    fun openSystemFocusSettings() {
        val opened = focusSettings.openSystemFocusSettings()
        _uiState.update {
            it.copy(
                focusSettingsStatusText = if (opened) {
                    "Configure Focus or Do Not Disturb in Android settings"
                } else {
                    "Android focus settings are unavailable on this device"
                }
            )
        }
    }

    fun purchaseCosmetic(cosmetic: Cosmetic) {
        viewModelScope.launch {
            if (repository.purchaseCosmetic(cosmetic.id, cosmetic.price)) {
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

    fun shareJourneyCard(context: android.content.Context, completedSessions: Int, totalMinutes: Int) {
        ShareCardGenerator.generateAndShare(
            context = context,
            companion = uiState.value.selectedCompanion,
            cosmetic = uiState.value.equippedCosmetic,
            completedSessions = completedSessions,
            totalMinutes = totalMinutes
        )
    }

    fun markNotificationPermissionRequested() {
        _uiState.update { it.copy(notificationPermissionRequested = true) }
        viewModelScope.launch { repository.preferences.markNotificationPermissionRequested() }
    }

    fun refreshNotificationStatus() {
        _uiState.update { it.copy(completionAlertsEnabled = notifications.canPostNotifications()) }
    }

    fun reconcileTimer() {
        refreshNotificationStatus()
        val sessionId = latestTimerState.activeSessionId ?: return
        val deadline = latestTimerState.targetDeadlineTimestamp ?: return
        if (deadline <= System.currentTimeMillis()) completeExpiredSession(sessionId)
    }

    private fun startTimer() {
        val state = uiState.value
        val remaining = if (state.isComplete) currentSessionDuration else state.remainingSeconds
        val resuming = !state.isComplete && latestTimerState.activeSessionId != null
        val sessionId = if (resuming) latestTimerState.activeSessionId!! else UUID.randomUUID().toString()
        val duration = if (resuming) {
            latestTimerState.activeSessionDurationSeconds ?: currentSessionDuration
        } else {
            currentSessionDuration
        }
        val companion = if (resuming) {
            latestTimerState.activeSessionCompanion ?: state.selectedCompanion.id
        } else {
            state.selectedCompanion.id
        }
        val deadline = System.currentTimeMillis() + remaining * 1000
        currentDeadlineTimestamp = deadline
        _uiState.update {
            it.copy(isRunning = true, isComplete = false, remainingSeconds = remaining)
        }
        ambientPlayer.play()
        startTicker()
        viewModelScope.launch {
            repository.preferences.startTimer(sessionId, deadline, duration, companion)
            alarmScheduler.schedule(sessionId, deadline)
        }
    }

    private fun pauseTimer() {
        val remaining = currentDeadlineTimestamp?.let(::secondsUntil) ?: uiState.value.remainingSeconds
        timerJob?.cancel()
        timerJob = null
        alarmScheduler.cancel()
        ambientPlayer.stop()
        currentDeadlineTimestamp = null
        _uiState.update {
            it.copy(isRunning = false, remainingSeconds = remaining)
        }
        viewModelScope.launch { repository.preferences.pauseTimer(remaining) }
    }

    private fun applyPersistedTimerState(prefs: TimerState) {
        latestTimerState = prefs
        val duration = durationOptions.getOrElse(prefs.durationIndex) { 1500L }
        val companion = CompanionAnimal.fromRaw(prefs.selectedCompanion)
        val cosmetic = Cosmetic.fromRaw(prefs.equippedCosmetic)
        val sound = AmbientSound.fromId(prefs.selectedSound)
        if (ambientPlayer.selectedSound != sound) ambientPlayer.setSelectedSound(sound)

        val deadline = prefs.targetDeadlineTimestamp
        val activeSessionId = prefs.activeSessionId
        val now = System.currentTimeMillis()
        when {
            deadline != null && activeSessionId != null && deadline > now -> {
                currentDeadlineTimestamp = deadline
                _uiState.update {
                    it.copy(
                        remainingSeconds = secondsUntil(deadline, now),
                        isRunning = true,
                        isComplete = false,
                        durationIndex = prefs.durationIndex,
                        selectedCompanion = companion,
                        equippedCosmetic = cosmetic,
                        hapticsEnabled = prefs.hapticsEnabled,
                        selectedSound = sound,
                        completionAlertsEnabled = notifications.canPostNotifications(),
                        notificationPermissionRequested = prefs.notificationPermissionRequested
                    )
                }
                ambientPlayer.play()
                startTicker()
            }
            deadline != null && activeSessionId != null -> {
                currentDeadlineTimestamp = deadline
                _uiState.update {
                    it.copy(remainingSeconds = 0, isRunning = false, isComplete = true)
                }
                completeExpiredSession(activeSessionId)
            }
            prefs.lastCompletedSessionId != null -> {
                timerJob?.cancel()
                timerJob = null
                currentDeadlineTimestamp = null
                ambientPlayer.stop()
                _uiState.update {
                    it.copy(
                        remainingSeconds = 0,
                        isRunning = false,
                        isComplete = true,
                        durationIndex = prefs.durationIndex,
                        selectedCompanion = companion,
                        equippedCosmetic = cosmetic,
                        hapticsEnabled = prefs.hapticsEnabled,
                        selectedSound = sound,
                        completionAlertsEnabled = notifications.canPostNotifications(),
                        notificationPermissionRequested = prefs.notificationPermissionRequested
                    )
                }
                announceCompletionIfNeeded(prefs.lastCompletedSessionId)
            }
            else -> {
                timerJob?.cancel()
                timerJob = null
                currentDeadlineTimestamp = null
                ambientPlayer.stop()
                _uiState.update {
                    it.copy(
                        remainingSeconds = prefs.pausedRemainingSeconds ?: duration,
                        isRunning = false,
                        isComplete = false,
                        durationIndex = prefs.durationIndex,
                        selectedCompanion = companion,
                        equippedCosmetic = cosmetic,
                        hapticsEnabled = prefs.hapticsEnabled,
                        selectedSound = sound,
                        completionAlertsEnabled = notifications.canPostNotifications(),
                        notificationPermissionRequested = prefs.notificationPermissionRequested
                    )
                }
            }
        }
    }

    private fun startTicker() {
        if (timerJob?.isActive == true) return
        timerJob = viewModelScope.launch {
            while (_uiState.value.isRunning) {
                val deadline = currentDeadlineTimestamp ?: break
                val remaining = secondsUntil(deadline)
                if (remaining <= 0) {
                    _uiState.update { it.copy(remainingSeconds = 0, isRunning = false, isComplete = true) }
                    latestTimerState.activeSessionId?.let(::completeExpiredSession)
                    break
                }
                val activeDuration = latestTimerState.activeSessionDurationSeconds ?: currentSessionDuration
                val elapsedMinutes = FocusTimerMath.elapsedWholeMinutes(activeDuration, remaining)
                if (_uiState.value.hapticsEnabled && elapsedMinutes > 0 &&
                    elapsedMinutes % 5 == 0 && elapsedMinutes != lastPulseMinute
                ) {
                    hapticsManager.pulseSoft()
                    lastPulseMinute = elapsedMinutes
                }
                _uiState.update { it.copy(remainingSeconds = remaining) }
                delay(250)
            }
        }
    }

    private fun completeExpiredSession(sessionId: String) {
        if (completionJob?.isActive == true) return
        ambientPlayer.stop()
        completionJob = viewModelScope.launch {
            when (completionCoordinator.completeIfExpired(expectedSessionId = sessionId)) {
                CompletionResult.COMPLETED,
                CompletionResult.ALREADY_COMPLETED -> Unit
                else -> Unit
            }
        }
    }

    private fun announceCompletionIfNeeded(sessionId: String) {
        if (lastAnnouncedCompletionId == sessionId) return
        lastAnnouncedCompletionId = sessionId
        meditationBell.play()
        hapticsManager.pulseSuccess()
        toastJob?.cancel()
        _uiState.update { it.copy(completionToastMessage = "+5 cozy coins — you did it") }
        toastJob = viewModelScope.launch {
            delay(3000)
            _uiState.update { it.copy(completionToastMessage = null) }
        }
    }

    private fun secondsUntil(
        deadlineTimestamp: Long,
        nowTimestamp: Long = System.currentTimeMillis()
    ): Long {
        return FocusTimerMath.remainingSeconds(deadlineTimestamp, nowTimestamp)
    }

    override fun onCleared() {
        ambientPlayer.stop()
    }
}
