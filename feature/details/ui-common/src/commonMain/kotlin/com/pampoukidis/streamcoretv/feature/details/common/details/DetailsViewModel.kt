package com.pampoukidis.streamcoretv.feature.details.common.details

import com.pampoukidis.streamcore.sdk.api.DetailsService
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreDetails
import com.pampoukidis.streamcore.sdk.api.LibraryService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreDetailsRequest
import com.pampoukidis.streamcore.sdk.api.PlaybackService
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DetailsViewModel constructor(
    private val detailsRepository: DetailsService,
    private val playback: PlaybackService,
    private val library: LibraryService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailsUiState())
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    private val effectsChannel = Channel<DetailsEffect>(capacity = Channel.BUFFERED)
    val effects: Flow<DetailsEffect> = effectsChannel.receiveAsFlow()

    private var activeRequest: StreamCoreDetailsRequest? = null
    private var loadJob: Job? = null
    private var progressJob: Job? = null
    private var libraryJob: Job? = null
    private var likeMutationJob: Job? = null
    private var myListMutationJob: Job? = null
    private var pendingLikeTarget: Boolean? = null
    private var pendingMyListTarget: Boolean? = null

    fun onAction(action: DetailsAction) {
        when (action) {
            is DetailsAction.Load -> load(
                request = action.request,
                initialContent = action.initialContent,
            )
            DetailsAction.Refresh -> refresh()
            is DetailsAction.RecommendationSelected -> selectRecommendation(action.content, action.sourceArtworkUrl)
            DetailsAction.PlaySelected -> selectPlay()
            DetailsAction.TrailerSelected -> selectTrailer()
            DetailsAction.LikeToggled -> toggleLike()
            DetailsAction.MyListToggled -> toggleMyList()
            DetailsAction.BackSelected -> navigateBack()
        }
    }

    private fun load(
        request: StreamCoreDetailsRequest,
        initialContent: StreamCoreContent? = null,
        force: Boolean = false,
    ) {
        val requestChanged = activeRequest != request
        if (!force && !requestChanged) {
            return
        }

        loadJob?.cancel()
        if (requestChanged) {
            activeRequest = request
            cancelPendingLibraryMutations()
            _uiState.value = DetailsUiState(
                isLoading = true,
                content = initialContent?.takeIf { content -> content.id == request.contentId },
            )
            observeProgress(request)
            observeLibraryState(request)
        } else {
            _uiState.update { state -> state.copy(isLoading = true) }
        }
        loadJob = viewModelScope.launch {
            when (val result = loadDetails(request)) {
                is StreamCoreResult.Success -> {
                    if (activeRequest != request) {
                        return@launch
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            content = result.value.content,
                            recommendations = result.value.recommendations,
                        )
                    }
                }

                is StreamCoreResult.Failure -> {
                    if (activeRequest != request) {
                        return@launch
                    }

                    _uiState.update { it.copy(isLoading = false) }
                    emitError(result.error)
                }
            }
        }
    }

    private suspend fun loadDetails(request: StreamCoreDetailsRequest): StreamCoreResult<StreamCoreDetails> {
        val content = when (val result = detailsRepository.getDetails(request.profileId, request.contentId)) {
            is StreamCoreResult.Success -> result.value
            is StreamCoreResult.Failure -> return result
        }
        return when (val result = detailsRepository.getRecommendations(request.profileId, request.contentId)) {
            is StreamCoreResult.Success -> StreamCoreResult.Success(StreamCoreDetails(content, result.value))
            is StreamCoreResult.Failure -> result
        }
    }

    private fun refresh() {
        val request = activeRequest ?: return
        load(request = request, force = true)
    }

    private fun selectRecommendation(content: StreamCoreContent, sourceArtworkUrl: String?) {
        viewModelScope.launch {
            effectsChannel.send(DetailsEffect.RecommendationSelected(content, sourceArtworkUrl))
        }
    }

    private fun selectPlay() {
        val request = activeRequest ?: return
        val content = _uiState.value.content ?: return
        viewModelScope.launch {
            effectsChannel.send(
                DetailsEffect.PlaySelected(
                    StreamCorePlaybackRequest(
                        profileId = request.profileId,
                        contentId = request.contentId,
                        contentSnapshot = content,
                    ),
                ),
            )
        }
    }

    private fun selectTrailer() {
        val trailer = _uiState.value.content?.trailers?.firstOrNull() ?: return
        viewModelScope.launch {
            effectsChannel.send(DetailsEffect.OpenTrailer(trailer))
        }
    }

    private fun toggleLike() {
        val request = activeRequest ?: return
        val state = _uiState.value
        val content = state.content ?: return
        if (!state.isLibraryAvailable || state.isLikeMutationPending) {
            return
        }

        val previousValue = state.isLiked
        val targetValue = !previousValue
        pendingLikeTarget = targetValue
        _uiState.update { current ->
            current.copy(
                isLiked = targetValue,
                isLikeMutationPending = true,
            )
        }
        likeMutationJob = viewModelScope.launch {
            when (
                val result = library.setLiked(
                    profileId = request.profileId,
                    content = content,
                    isLiked = targetValue,
                )
            ) {
                is StreamCoreResult.Success -> finishLikeMutation(
                    request = request,
                    targetValue = targetValue,
                )

                is StreamCoreResult.Failure -> failLikeMutation(
                    request = request,
                    previousValue = previousValue,
                    error = result.error,
                )
            }
        }
    }

    private fun toggleMyList() {
        val request = activeRequest ?: return
        val state = _uiState.value
        val content = state.content ?: return
        if (!state.isLibraryAvailable || state.isMyListMutationPending) {
            return
        }

        val previousValue = state.isInMyList
        val targetValue = !previousValue
        pendingMyListTarget = targetValue
        _uiState.update { current ->
            current.copy(
                isInMyList = targetValue,
                isMyListMutationPending = true,
            )
        }
        myListMutationJob = viewModelScope.launch {
            when (
                val result = library.setInMyList(
                    profileId = request.profileId,
                    content = content,
                    isInMyList = targetValue,
                )
            ) {
                is StreamCoreResult.Success -> finishMyListMutation(
                    request = request,
                    targetValue = targetValue,
                )

                is StreamCoreResult.Failure -> failMyListMutation(
                    request = request,
                    previousValue = previousValue,
                    error = result.error,
                )
            }
        }
    }

    private fun observeProgress(request: StreamCoreDetailsRequest) {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            playback.observeProgress(request.profileId).collect { result ->
                val entries = (result as? StreamCoreResult.Success)?.value.orEmpty()
                val progress = entries.firstOrNull { entry -> entry.contentId == request.contentId }
                _uiState.update { state ->
                    state.copy(
                        hasResumableProgress = progress != null,
                    )
                }
            }
        }
    }

    private fun observeLibraryState(request: StreamCoreDetailsRequest) {
        libraryJob?.cancel()
        libraryJob = viewModelScope.launch {
            library.observeContentState(
                profileId = request.profileId,
                contentId = request.contentId,
            ).collect { result ->
                if (activeRequest != request) {
                    return@collect
                }

                when (result) {
                    is StreamCoreResult.Success -> {
                        _uiState.update { state ->
                            state.copy(
                                isLibraryAvailable = true,
                                isLiked = pendingLikeTarget ?: result.value.isLiked,
                                isInMyList = pendingMyListTarget ?: result.value.isInMyList,
                            )
                        }
                    }

                    is StreamCoreResult.Failure -> {
                        _uiState.update { state -> state.copy(isLibraryAvailable = false) }
                        emitError(result.error)
                    }
                }
            }
        }
    }

    private fun finishLikeMutation(
        request: StreamCoreDetailsRequest,
        targetValue: Boolean,
    ) {
        if (activeRequest != request) {
            return
        }
        pendingLikeTarget = null
        _uiState.update { state ->
            state.copy(
                isLiked = targetValue,
                isLikeMutationPending = false,
            )
        }
    }

    private suspend fun failLikeMutation(
        request: StreamCoreDetailsRequest,
        previousValue: Boolean,
        error: StreamCoreError,
    ) {
        if (activeRequest != request) {
            return
        }
        pendingLikeTarget = null
        _uiState.update { state ->
            state.copy(
                isLiked = previousValue,
                isLikeMutationPending = false,
            )
        }
        emitError(error)
    }

    private fun finishMyListMutation(
        request: StreamCoreDetailsRequest,
        targetValue: Boolean,
    ) {
        if (activeRequest != request) {
            return
        }
        pendingMyListTarget = null
        _uiState.update { state ->
            state.copy(
                isInMyList = targetValue,
                isMyListMutationPending = false,
            )
        }
    }

    private suspend fun failMyListMutation(
        request: StreamCoreDetailsRequest,
        previousValue: Boolean,
        error: StreamCoreError,
    ) {
        if (activeRequest != request) {
            return
        }
        pendingMyListTarget = null
        _uiState.update { state ->
            state.copy(
                isInMyList = previousValue,
                isMyListMutationPending = false,
            )
        }
        emitError(error)
    }

    private fun cancelPendingLibraryMutations() {
        likeMutationJob?.cancel()
        myListMutationJob?.cancel()
        likeMutationJob = null
        myListMutationJob = null
        pendingLikeTarget = null
        pendingMyListTarget = null
    }

    private fun navigateBack() {
        viewModelScope.launch {
            effectsChannel.send(DetailsEffect.NavigateBack)
        }
    }

    private suspend fun emitError(error: StreamCoreError) {
        effectsChannel.send(DetailsEffect.ShowError(error))
    }
}
