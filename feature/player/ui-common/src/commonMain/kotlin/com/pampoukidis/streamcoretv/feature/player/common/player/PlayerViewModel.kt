package com.pampoukidis.streamcoretv.feature.player.common.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pampoukidis.streamcoretv.feature.player.domain.PlaybackProgressPolicy
import com.pampoukidis.streamcoretv.playback.api.PlaybackEngineState
import com.pampoukidis.streamcoretv.playback.api.PlaybackErrorModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackFilmstripFrameModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackPhase
import com.pampoukidis.streamcoretv.playback.api.PlaybackProgressEntryModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackProgressRepository
import com.pampoukidis.streamcoretv.playback.api.PlaybackRequestModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackSession
import com.pampoukidis.streamcoretv.playback.api.PlaybackSessionFactory
import com.pampoukidis.streamcoretv.playback.api.PlaybackSourceRepository
import com.pampoukidis.streamcoretv.playback.api.PlaybackVideoSurface
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlayerViewModel constructor(
    private val sourceRepository: PlaybackSourceRepository,
    private val progressRepository: PlaybackProgressRepository,
    private val sessionFactory: PlaybackSessionFactory,
    private val clock: PlayerClock = SystemPlayerClock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _videoSurface = MutableStateFlow<PlaybackVideoSurface?>(null)
    val videoSurface: StateFlow<PlaybackVideoSurface?> = _videoSurface.asStateFlow()

    private val effectsChannel = Channel<PlayerEffect>(Channel.BUFFERED)
    val effects: Flow<PlayerEffect> = effectsChannel.receiveAsFlow()

    private var request: PlaybackRequestModel? = null
    private var session: PlaybackSession? = null
    private var sessionStateJob: Job? = null
    private var controlsJob: Job? = null
    private var filmstripJob: Job? = null
    private var activeFilmstripPositions: List<Long> = emptyList()
    private var pendingFilmstripPositions: List<Long>? = null
    private var feedbackJob: Job? = null
    private var resumeAfterScrub = false
    private var lastValidPositionMillis = 0L
    private var lastSavedProgressBucket = 0L
    private var isBackNavigationPending = false

    fun onAction(action: PlayerAction) {
        when (action) {
            is PlayerAction.Load -> load(action.request, action.isPipSupported)
            PlayerAction.BackSelected -> back()
            PlayerAction.ToggleControls -> toggleControls()
            PlayerAction.UserInteraction -> userInteraction()
            PlayerAction.TogglePlayPause -> togglePlayPause()
            is PlayerAction.SeekBy -> seekBy(action.deltaMillis, action.showFeedback)
            PlayerAction.ScrubStarted -> startScrubbing()
            is PlayerAction.ScrubChanged -> updateScrub(action.positionMillis)
            PlayerAction.ScrubFinished -> finishScrubbing()
            is PlayerAction.OpenSettings -> openSettings(action.page)
            PlayerAction.CloseSettings -> closeSettings()
            is PlayerAction.SelectVideoTrack -> session?.selectVideoTrack(action.trackId)
            is PlayerAction.SelectAudioTrack -> session?.selectAudioTrack(action.trackId)
            is PlayerAction.SelectTextTrack -> session?.selectTextTrack(action.trackId)
            is PlayerAction.SelectSpeed -> session?.setSpeed(action.speed)
            is PlayerAction.SelectResizeMode -> session?.setResizeMode(action.mode)
            PlayerAction.Retry -> retry()
            PlayerAction.PipSelected -> requestPip()
            is PlayerAction.PipChanged -> onPipChanged(action.isInPip)
            is PlayerAction.ForegroundChanged -> onForegroundChanged(action.isForeground)
        }
    }

    override fun onCleared() {
        controlsJob?.cancel()
        filmstripJob?.cancel()
        feedbackJob?.cancel()
        sessionStateJob?.cancel()
        session?.close()
        session = null
        _videoSurface.value = null
        super.onCleared()
    }

    private fun load(newRequest: PlaybackRequestModel, isPipSupported: Boolean) {
        if (request == newRequest && session != null) {
            return
        }
        filmstripJob?.cancel()
        activeFilmstripPositions = emptyList()
        pendingFilmstripPositions = null
        request = newRequest
        _uiState.value = PlayerUiState(
            title = newRequest.contentSnapshot.title,
            phase = PlaybackPhase.Preparing,
            isPipSupported = isPipSupported,
        )
        viewModelScope.launch {
            prepareSession(newRequest)
        }
    }

    private suspend fun prepareSession(activeRequest: PlaybackRequestModel) {
        runCatching {
            val progress = progressRepository.get(activeRequest.profileId, activeRequest.contentId)
            lastValidPositionMillis = progress?.positionMillis ?: 0L
            lastSavedProgressBucket = lastValidPositionMillis / ProgressSaveIntervalMillis
            val media = sourceRepository.resolve(activeRequest)
            replaceSession().prepare(media, lastValidPositionMillis)
        }.onFailure { throwable ->
            _uiState.update { state ->
                state.copy(
                    phase = PlaybackPhase.Error,
                    controlsVisible = true,
                    error = PlaybackErrorModel(
                        code = "SOURCE_RESOLUTION_FAILED",
                        message = throwable.localizedMessage ?: "Unable to load this video.",
                        isRecoverable = true,
                    ),
                )
            }
        }
    }

    private fun replaceSession(): PlaybackSession {
        sessionStateJob?.cancel()
        session?.close()
        val newSession = sessionFactory.create()
        session = newSession
        _videoSurface.value = newSession.videoSurface
        sessionStateJob = viewModelScope.launch {
            newSession.state.collect(::applyEngineState)
        }
        return newSession
    }

    private fun applyEngineState(engine: PlaybackEngineState) {
        if (engine.positionMillis > 0L) {
            lastValidPositionMillis = engine.positionMillis
        }
        val forceControlsVisible = !engine.isPlaying ||
                engine.phase == PlaybackPhase.Buffering ||
                engine.phase == PlaybackPhase.Ended ||
                engine.phase == PlaybackPhase.Error ||
                _uiState.value.isScrubbing ||
                _uiState.value.settingsPage != null
        _uiState.update { state ->
            state.copy(
                phase = engine.phase,
                isPlaying = engine.isPlaying,
                positionMillis = if (state.isScrubbing) state.positionMillis else engine.positionMillis,
                durationMillis = engine.durationMillis,
                bufferedPositionMillis = engine.bufferedPositionMillis,
                videoAspectRatio = engine.videoAspectRatio,
                controlsVisible = if (forceControlsVisible) true else state.controlsVisible,
                videoTracks = engine.videoTracks,
                audioTracks = engine.audioTracks,
                textTracks = engine.textTracks,
                selectedVideoTrackId = engine.selectedVideoTrackId,
                selectedAudioTrackId = engine.selectedAudioTrackId,
                selectedTextTrackId = engine.selectedTextTrackId,
                speed = engine.speed,
                resizeMode = engine.resizeMode,
                error = engine.error,
            )
        }
        if (engine.phase == PlaybackPhase.Ended) {
            viewModelScope.launch { removeProgress() }
        }
        val progressBucket = engine.positionMillis / ProgressSaveIntervalMillis
        if (engine.isPlaying && progressBucket > lastSavedProgressBucket) {
            lastSavedProgressBucket = progressBucket
            viewModelScope.launch { saveProgress(positionOverride = engine.positionMillis) }
        }
        scheduleControlsHideIfEligible()
    }

    private fun toggleControls() {
        _uiState.update { state -> state.copy(controlsVisible = !state.controlsVisible) }
        scheduleControlsHideIfEligible()
    }

    private fun userInteraction() {
        controlsJob?.cancel()
        controlsJob = null
        _uiState.update { state -> state.copy(controlsVisible = true) }
        scheduleControlsHideIfEligible()
    }

    private fun togglePlayPause() {
        val state = _uiState.value
        if (state.isPlaying) {
            session?.pause()
            viewModelScope.launch { saveProgress() }
        } else {
            session?.play()
        }
        _uiState.update { it.copy(controlsVisible = true) }
        scheduleControlsHideIfEligible()
    }

    private fun seekBy(deltaMillis: Long, showFeedback: Boolean) {
        val state = _uiState.value
        if (!state.canSeek) {
            return
        }
        val target = (state.positionMillis + deltaMillis).coerceIn(0L, state.durationMillis)
        session?.seekTo(target)
        if (showFeedback) {
            showSeekFeedback((deltaMillis / 1_000L).toInt())
        }
        viewModelScope.launch { saveProgress(positionOverride = target) }
    }

    private fun startScrubbing() {
        val state = _uiState.value
        if (!state.canSeek) {
            return
        }
        resumeAfterScrub = state.isPlaying
        session?.pause()
        controlsJob?.cancel()
        _uiState.update {
            it.copy(
                isScrubbing = true,
                scrubPositionMillis = it.positionMillis,
                controlsVisible = true,
            )
        }
        requestFilmstrip(state.positionMillis)
    }

    private fun updateScrub(positionMillis: Long) {
        val duration = _uiState.value.durationMillis
        if (duration <= 0L || !_uiState.value.isScrubbing) {
            return
        }
        val position = positionMillis.coerceIn(0L, duration)
        _uiState.update { it.copy(scrubPositionMillis = position) }
        requestFilmstrip(position)
    }

    private fun finishScrubbing() {
        val state = _uiState.value
        if (!state.isScrubbing) {
            return
        }
        filmstripJob?.cancel()
        activeFilmstripPositions = emptyList()
        pendingFilmstripPositions = null
        session?.seekTo(state.scrubPositionMillis)
        if (resumeAfterScrub) {
            session?.play()
        }
        _uiState.update {
            it.copy(
                isScrubbing = false,
                positionMillis = state.scrubPositionMillis,
                filmstripFrames = emptyList(),
            )
        }
        viewModelScope.launch { saveProgress(positionOverride = state.scrubPositionMillis) }
        scheduleControlsHideIfEligible()
    }

    private fun requestFilmstrip(centerMillis: Long) {
        val durationMillis = _uiState.value.durationMillis
        val positions = filmstripPositions(centerMillis, durationMillis)
        if (positions == activeFilmstripPositions) {
            return
        }
        activeFilmstripPositions = positions
        pendingFilmstripPositions = positions
        _uiState.update { state ->
            state.copy(
                filmstripFrames = positions.mapIndexed { index, position ->
                    PlaybackFilmstripFrameModel(
                        positionMillis = position,
                        image = state.filmstripFrames.getOrNull(index)?.image,
                    )
                },
            )
        }
        if (filmstripJob?.isActive == true) {
            return
        }
        filmstripJob = viewModelScope.launch { processFilmstripRequests() }
    }

    private suspend fun processFilmstripRequests() {
        val activeSession = session ?: return
        while (true) {
            val positions = pendingFilmstripPositions ?: return
            pendingFilmstripPositions = null
            activeSession.requestFilmstrip(positions.inFilmstripExtractionOrder())
                .takeWhile { pendingFilmstripPositions == null }
                .collect { frame -> updateFilmstripFrame(positions, frame) }
        }
    }

    private fun filmstripPositions(centerMillis: Long, durationMillis: Long): List<Long> {
        val quantizedCenterMillis = (
                (centerMillis + FilmstripSpacingMillis / 2L) /
                        FilmstripSpacingMillis * FilmstripSpacingMillis
                ).coerceIn(0L, durationMillis)
        return (-2L..2L).map { offset ->
            (quantizedCenterMillis + offset * FilmstripSpacingMillis).coerceIn(0L, durationMillis)
        }
    }

    private fun List<Long>.inFilmstripExtractionOrder(): List<Long> {
        return FilmstripFrameIndicesByPriority
            .mapNotNull { index -> getOrNull(index) }
            .distinct()
    }

    private fun updateFilmstripFrame(
        positions: List<Long>,
        loadedFrame: PlaybackFilmstripFrameModel,
    ) {
        _uiState.update { state ->
            if (!state.isScrubbing || positions != activeFilmstripPositions) {
                return@update state
            }
            state.copy(
                filmstripFrames = positions.mapIndexed { index, position ->
                    val loadedImage = loadedFrame.image.takeIf {
                        loadedFrame.positionMillis == position
                    }
                    PlaybackFilmstripFrameModel(
                        positionMillis = position,
                        image = loadedImage ?: state.filmstripFrames.getOrNull(index)?.image,
                    )
                },
            )
        }
    }

    private fun openSettings(page: PlayerSettingsPage) {
        controlsJob?.cancel()
        _uiState.update { it.copy(settingsPage = page, controlsVisible = true) }
    }

    private fun closeSettings() {
        val page = _uiState.value.settingsPage ?: return
        _uiState.update {
            it.copy(settingsPage = if (page == PlayerSettingsPage.Root) null else PlayerSettingsPage.Root)
        }
        scheduleControlsHideIfEligible()
    }

    private fun back() {
        if (_uiState.value.settingsPage != null) {
            closeSettings()
            return
        }
        if (isBackNavigationPending) {
            return
        }
        isBackNavigationPending = true
        viewModelScope.launch {
            saveProgressBestEffort()
            effectsChannel.send(PlayerEffect.NavigateBack)
        }
    }

    private fun retry() {
        val activeRequest = request ?: return
        _uiState.update { it.copy(phase = PlaybackPhase.Preparing, error = null) }
        viewModelScope.launch { prepareSession(activeRequest) }
    }

    private fun requestPip() {
        if (!_uiState.value.isPipSupported) {
            return
        }
        viewModelScope.launch { effectsChannel.send(PlayerEffect.EnterPictureInPicture) }
    }

    private fun onPipChanged(isInPip: Boolean) {
        _uiState.update {
            it.copy(
                isInPip = isInPip,
                controlsVisible = if (isInPip) false else it.controlsVisible,
            )
        }
        viewModelScope.launch { saveProgress() }
    }

    private fun onForegroundChanged(isForeground: Boolean) {
        if (!isForeground && !_uiState.value.isInPip) {
            session?.pause()
            viewModelScope.launch { saveProgress() }
        }
    }

    private fun scheduleControlsHideIfEligible() {
        val state = _uiState.value
        if (!state.isPlaying || state.isBuffering || state.isScrubbing || state.isEnded ||
            state.error != null || state.settingsPage != null || state.isInPip || !state.controlsVisible
        ) {
            controlsJob?.cancel()
            controlsJob = null
            return
        }
        if (controlsJob?.isActive == true) {
            return
        }
        controlsJob = viewModelScope.launch {
            delay(ControlsAutoHideMillis)
            _uiState.update { it.copy(controlsVisible = false) }
        }
    }

    private fun showSeekFeedback(seconds: Int) {
        feedbackJob?.cancel()
        _uiState.update { it.copy(seekFeedbackSeconds = seconds) }
        feedbackJob = viewModelScope.launch {
            delay(SeekFeedbackMillis)
            _uiState.update { it.copy(seekFeedbackSeconds = null) }
        }
    }

    private suspend fun saveProgress(positionOverride: Long? = null) {
        val activeRequest = request ?: return
        val state = _uiState.value
        if (state.durationMillis <= 0L) {
            return
        }
        val position = positionOverride ?: state.positionMillis
        if (!PlaybackProgressPolicy.isResumable(position, state.durationMillis)) {
            progressRepository.remove(activeRequest.profileId, activeRequest.contentId)
            return
        }
        progressRepository.upsert(
            PlaybackProgressEntryModel(
                profileId = activeRequest.profileId,
                contentId = activeRequest.contentId,
                contentSnapshot = activeRequest.contentSnapshot,
                positionMillis = position,
                durationMillis = state.durationMillis,
                updatedAtMillis = clock.nowEpochMillis(),
            ),
        )
    }

    private suspend fun saveProgressBestEffort() {
        try {
            saveProgress()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            // Navigation must remain available when local progress persistence fails.
        }
    }

    private suspend fun removeProgress() {
        val activeRequest = request ?: return
        progressRepository.remove(activeRequest.profileId, activeRequest.contentId)
    }

    private companion object {
        const val ControlsAutoHideMillis = 10_000L
        const val ProgressSaveIntervalMillis = 10_000L
        const val FilmstripSpacingMillis = 5_000L
        const val SeekFeedbackMillis = 800L
        val FilmstripFrameIndicesByPriority = listOf(2, 1, 3, 0, 4)
    }
}
