package com.pampoukidis.streamcoretv.feature.details.common.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.feature.details.data.DetailsRequest
import com.pampoukidis.streamcoretv.feature.details.domain.LoadDetailsUseCase
import com.pampoukidis.streamcoretv.feature.library.domain.ObserveContentLibraryStateUseCase
import com.pampoukidis.streamcoretv.feature.library.domain.SetContentInMyListUseCase
import com.pampoukidis.streamcoretv.feature.library.domain.SetContentLikedUseCase
import com.pampoukidis.streamcoretv.feature.player.domain.PlaybackProgressPolicy
import com.pampoukidis.streamcoretv.playback.api.PlaybackProgressRepository
import com.pampoukidis.streamcoretv.playback.api.PlaybackRequestModel
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
    private val loadDetails: LoadDetailsUseCase,
    private val progressRepository: PlaybackProgressRepository,
    private val observeContentLibraryState: ObserveContentLibraryStateUseCase,
    private val setContentLiked: SetContentLikedUseCase,
    private val setContentInMyList: SetContentInMyListUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailsUiState())
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    private val effectsChannel = Channel<DetailsEffect>(capacity = Channel.BUFFERED)
    val effects: Flow<DetailsEffect> = effectsChannel.receiveAsFlow()

    private var activeRequest: DetailsRequest? = null
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
            is DetailsAction.RecommendationSelected -> selectRecommendation(action.content)
            DetailsAction.PlaySelected -> selectPlay()
            DetailsAction.TrailerSelected -> selectTrailer()
            DetailsAction.LikeToggled -> toggleLike()
            DetailsAction.MyListToggled -> toggleMyList()
            DetailsAction.BackSelected -> navigateBack()
        }
    }

    private fun load(
        request: DetailsRequest,
        initialContent: ContentModel? = null,
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
                is AppResult.Success -> {
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

                is AppResult.Failure -> {
                    if (activeRequest != request) {
                        return@launch
                    }

                    _uiState.update { it.copy(isLoading = false) }
                    emitError(result.error)
                }
            }
        }
    }

    private fun refresh() {
        val request = activeRequest ?: return
        load(request = request, force = true)
    }

    private fun selectRecommendation(content: ContentModel) {
        viewModelScope.launch {
            effectsChannel.send(DetailsEffect.RecommendationSelected(content))
        }
    }

    private fun selectPlay() {
        val request = activeRequest ?: return
        val content = _uiState.value.content ?: return
        viewModelScope.launch {
            effectsChannel.send(
                DetailsEffect.PlaySelected(
                    PlaybackRequestModel(
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
                val result = setContentLiked(
                    profileId = request.profileId,
                    content = content,
                    isLiked = targetValue,
                )
            ) {
                is AppResult.Success -> finishLikeMutation(
                    request = request,
                    targetValue = targetValue,
                )

                is AppResult.Failure -> failLikeMutation(
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
                val result = setContentInMyList(
                    profileId = request.profileId,
                    content = content,
                    isInMyList = targetValue,
                )
            ) {
                is AppResult.Success -> finishMyListMutation(
                    request = request,
                    targetValue = targetValue,
                )

                is AppResult.Failure -> failMyListMutation(
                    request = request,
                    previousValue = previousValue,
                    error = result.error,
                )
            }
        }
    }

    private fun observeProgress(request: DetailsRequest) {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            progressRepository.observe(request.profileId).collect { entries ->
                val progress = entries.firstOrNull { entry -> entry.contentId == request.contentId }
                _uiState.update { state ->
                    state.copy(
                        hasResumableProgress = progress?.let { entry ->
                            PlaybackProgressPolicy.isResumable(
                                positionMillis = entry.positionMillis,
                                durationMillis = entry.durationMillis,
                            )
                        } == true,
                    )
                }
            }
        }
    }

    private fun observeLibraryState(request: DetailsRequest) {
        libraryJob?.cancel()
        libraryJob = viewModelScope.launch {
            observeContentLibraryState(
                profileId = request.profileId,
                contentId = request.contentId,
            ).collect { result ->
                if (activeRequest != request) {
                    return@collect
                }

                when (result) {
                    is AppResult.Success -> {
                        _uiState.update { state ->
                            state.copy(
                                isLibraryAvailable = true,
                                isLiked = pendingLikeTarget ?: result.value.isLiked,
                                isInMyList = pendingMyListTarget ?: result.value.isInMyList,
                            )
                        }
                    }

                    is AppResult.Failure -> {
                        _uiState.update { state -> state.copy(isLibraryAvailable = false) }
                        emitError(result.error)
                    }
                }
            }
        }
    }

    private fun finishLikeMutation(
        request: DetailsRequest,
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
        request: DetailsRequest,
        previousValue: Boolean,
        error: AppError,
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
        request: DetailsRequest,
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
        request: DetailsRequest,
        previousValue: Boolean,
        error: AppError,
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

    private suspend fun emitError(error: AppError) {
        effectsChannel.send(DetailsEffect.ShowError(error))
    }
}
