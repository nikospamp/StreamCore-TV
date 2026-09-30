package com.pampoukidis.streamcoretv.feature.player.common.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pampoukidis.streamcoretv.playback.api.PlaybackEngineState
import com.pampoukidis.streamcoretv.playback.api.PlaybackErrorModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackFilmstripFrameModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackPhase
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.api.PlaybackService
import com.pampoukidis.streamcore.sdk.api.PlaybackProgressRecorder
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackProgressEvent
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackRequest
import com.pampoukidis.streamcoretv.playback.api.PlaybackSession
import com.pampoukidis.streamcoretv.playback.api.PlaybackSessionFactory
import com.pampoukidis.streamcoretv.playback.api.PlaybackVideoSurface
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

class PlayerViewModel constructor(
    private val playback: PlaybackService,
    private val sessionFactory: PlaybackSessionFactory,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _videoSurface = MutableStateFlow<PlaybackVideoSurface?>(null)
    val videoSurface: StateFlow<PlaybackVideoSurface?> = _videoSurface.asStateFlow()

    private val effectsChannel = Channel<PlayerEffect>(Channel.BUFFERED)
    val effects: Flow<PlayerEffect> = effectsChannel.receiveAsFlow()

    private var request: StreamCorePlaybackRequest? = null
    private val session: PlaybackSession = sessionFactory.create()
    private val sessionStateJob: Job
    private var preparationJob: Job? = null
    private var hasPreparedSession = false
    private var isForeground = true
    private var playWhenPrepared = true
    private val preparedSession: PlaybackSession?
        get() {
            return session.takeIf { hasPreparedSession }
        }
    private var controlsJob: Job? = null
    private var filmstripJob: Job? = null
    private var activeFilmstripPositions: List<Long> = emptyList()
    private var pendingFilmstripPositions: List<Long>? = null
    private var feedbackJob: Job? = null
    private var resumeAfterScrub = false
    private var lastValidPositionMillis = 0L
    private var progressRecorder: PlaybackProgressRecorder? = null
    private var isBackNavigationPending = false

    init {
        sessionStateJob = viewModelScope.launch {
            session.state.collect(::applyEngineState)
        }
    }

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
            is PlayerAction.SelectVideoTrack -> preparedSession?.selectVideoTrack(action.trackId)
            is PlayerAction.SelectAudioTrack -> preparedSession?.selectAudioTrack(action.trackId)
            is PlayerAction.SelectTextTrack -> preparedSession?.selectTextTrack(action.trackId)
            is PlayerAction.SelectSpeed -> preparedSession?.setSpeed(action.speed)
            is PlayerAction.SelectResizeMode -> preparedSession?.setResizeMode(action.mode)
            PlayerAction.Retry -> retry()
            PlayerAction.PipSelected -> requestPip()
            is PlayerAction.PipChanged -> onPipChanged(action.isInPip)
            is PlayerAction.ForegroundChanged -> onForegroundChanged(action.isForeground)
        }
    }

    override fun onCleared() {
        preparationJob?.cancel()
        controlsJob?.cancel()
        filmstripJob?.cancel()
        feedbackJob?.cancel()
        sessionStateJob.cancel()
        session.close()
        _videoSurface.value = null
        super.onCleared()
    }

    private fun load(newRequest: StreamCorePlaybackRequest, isPipSupported: Boolean) {
        if (isBackNavigationPending ||
            (request == newRequest && (hasPreparedSession || preparationJob?.isActive == true))
        ) {
            return
        }
        filmstripJob?.cancel()
        activeFilmstripPositions = emptyList()
        pendingFilmstripPositions = null
        if (request != null && request != newRequest) {
            playWhenPrepared = isForeground || _uiState.value.isInPip
        }
        request = newRequest
        progressRecorder = null
        _uiState.value = PlayerUiState(
            title = newRequest.contentSnapshot.title,
            phase = PlaybackPhase.Preparing,
            isPipSupported = isPipSupported,
        )
        startPreparation(newRequest)
    }

    private fun startPreparation(activeRequest: StreamCorePlaybackRequest) {
        preparationJob?.cancel()
        val previousSession = preparedSession
        hasPreparedSession = false
        previousSession?.pause()
        resumeAfterScrub = false
        preparationJob = viewModelScope.launch { prepareSession(activeRequest) }
    }

    private suspend fun prepareSession(activeRequest: StreamCorePlaybackRequest) {
        try {
            val progress = when (val result = playback.getProgress(activeRequest.profileId, activeRequest.contentId)) {
                is StreamCoreResult.Success -> result.value
                is StreamCoreResult.Failure -> null
            }
            coroutineContext.ensureActive()
            val media = when (val result = playback.resolveSource(activeRequest)) {
                is StreamCoreResult.Success -> result.value
                is StreamCoreResult.Failure -> error("Playback source resolution failed")
            }
            coroutineContext.ensureActive()
            _uiState.update { state -> state.copy(title = media.title) }
            lastValidPositionMillis = progress?.positionMillis ?: 0L
            progressRecorder = playback.createProgressRecorder(activeRequest, lastValidPositionMillis)
            hasPreparedSession = true
            _videoSurface.value = session.videoSurface
            session.prepare(media, lastValidPositionMillis)
            if (!playWhenPrepared) {
                session.pause()
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            coroutineContext.ensureActive()
            _uiState.update { state ->
                state.copy(
                    phase = PlaybackPhase.Error,
                    controlsVisible = true,
                    error = PlaybackErrorModel(
                        code = "SOURCE_RESOLUTION_FAILED",
                        message = SourceResolutionErrorMessage,
                        isRecoverable = true,
                    ),
                )
            }
        }
    }

    private fun applyEngineState(engine: PlaybackEngineState) {
        if (!hasPreparedSession || isBackNavigationPending) {
            return
        }
        val normalizedEngine = engine.normalizedForUi()
        if (normalizedEngine.positionMillis > 0L) {
            lastValidPositionMillis = normalizedEngine.positionMillis
        }
        val forceControlsVisible = !normalizedEngine.isPlaying ||
                normalizedEngine.phase == PlaybackPhase.Buffering ||
                normalizedEngine.phase == PlaybackPhase.Ended ||
                normalizedEngine.phase == PlaybackPhase.Error ||
                _uiState.value.isScrubbing ||
                _uiState.value.settingsPage != null
        _uiState.update { state ->
            state.copy(
                phase = normalizedEngine.phase,
                isPlaying = normalizedEngine.isPlaying,
                positionMillis = if (state.isScrubbing) state.positionMillis else normalizedEngine.positionMillis,
                durationMillis = normalizedEngine.durationMillis,
                bufferedPositionMillis = normalizedEngine.bufferedPositionMillis,
                videoAspectRatio = normalizedEngine.videoAspectRatio,
                controlsVisible = if (forceControlsVisible) true else state.controlsVisible,
                videoTracks = normalizedEngine.videoTracks,
                audioTracks = normalizedEngine.audioTracks,
                textTracks = normalizedEngine.textTracks,
                selectedVideoTrackId = normalizedEngine.selectedVideoTrackId,
                selectedAudioTrackId = normalizedEngine.selectedAudioTrackId,
                selectedTextTrackId = normalizedEngine.selectedTextTrackId,
                speed = normalizedEngine.speed,
                resizeMode = normalizedEngine.resizeMode,
                error = normalizedEngine.error?.toUiError(),
            )
        }
        if (normalizedEngine.phase == PlaybackPhase.Ended) {
            viewModelScope.launch { removeProgress() }
        }
        if (normalizedEngine.isPlaying) {
            val recorder = progressRecorder
            viewModelScope.launch {
                recorder?.reportEvent(
                    StreamCorePlaybackProgressEvent.Periodic,
                    normalizedEngine.positionMillis,
                    normalizedEngine.durationMillis,
                )
            }
        }
        scheduleControlsHideIfEligible()
    }

    private fun PlaybackEngineState.normalizedForUi(): PlaybackEngineState {
        if (phase != PlaybackPhase.Preparing) {
            return this
        }
        return PlaybackEngineState(
            phase = PlaybackPhase.Preparing,
            speed = speed,
            resizeMode = resizeMode,
        )
    }

    private fun PlaybackErrorModel.toUiError(): PlaybackErrorModel {
        return PlaybackErrorModel(
            code = PlaybackFailureErrorCode,
            message = PlaybackFailureErrorMessage,
            isRecoverable = isRecoverable,
        )
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
        val shouldPause = if (state.phase == PlaybackPhase.Preparing) playWhenPrepared else state.isPlaying
        playWhenPrepared = !shouldPause
        if (shouldPause) {
            resumeAfterScrub = false
            preparedSession?.pause()
            viewModelScope.launch { saveProgress() }
        } else {
            preparedSession?.play()
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
        preparedSession?.seekTo(target)
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
        preparedSession?.pause()
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
        preparedSession?.seekTo(state.scrubPositionMillis)
        if (resumeAfterScrub) {
            preparedSession?.play()
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
        val activeSession = preparedSession ?: return
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
        preparationJob?.cancel()
        playWhenPrepared = false
        resumeAfterScrub = false
        preparedSession?.pause()
        viewModelScope.launch {
            saveProgress()
            effectsChannel.send(PlayerEffect.NavigateBack)
        }
    }

    private fun retry() {
        if (isBackNavigationPending) {
            return
        }
        val activeRequest = request ?: return
        _uiState.update { it.copy(phase = PlaybackPhase.Preparing, error = null) }
        startPreparation(activeRequest)
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
        this.isForeground = isForeground
        if (!isForeground && !_uiState.value.isInPip) {
            playWhenPrepared = false
            resumeAfterScrub = false
            preparedSession?.pause()
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
        val state = _uiState.value
        progressRecorder?.reportEvent(
            StreamCorePlaybackProgressEvent.Checkpoint,
            positionOverride ?: state.positionMillis,
            state.durationMillis,
        )
    }

    private suspend fun removeProgress() {
        val state = _uiState.value
        progressRecorder?.reportEvent(StreamCorePlaybackProgressEvent.Completed, state.positionMillis, state.durationMillis)
    }

    private companion object {
        const val ControlsAutoHideMillis = 10_000L
        const val FilmstripSpacingMillis = 5_000L
        const val SeekFeedbackMillis = 800L
        const val SourceResolutionErrorMessage = "Unable to load this video."
        const val PlaybackFailureErrorCode = "PLAYBACK_FAILED"
        const val PlaybackFailureErrorMessage = "Playback failed."
        val FilmstripFrameIndicesByPriority = listOf(2, 1, 3, 0, 4)
    }
}
