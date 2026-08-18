package com.pampoukidis.streamcoretv.feature.details.common.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.feature.details.data.DetailsRequest
import com.pampoukidis.streamcoretv.feature.details.domain.LoadDetailsUseCase
import com.pampoukidis.streamcoretv.feature.player.domain.PlaybackProgressPolicy
import com.pampoukidis.streamcoretv.playback.api.PlaybackProgressRepository
import com.pampoukidis.streamcoretv.playback.api.PlaybackRequestModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val loadDetails: LoadDetailsUseCase,
    private val progressRepository: PlaybackProgressRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailsUiState())
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    private val effectsChannel = Channel<DetailsEffect>(capacity = Channel.BUFFERED)
    val effects: Flow<DetailsEffect> = effectsChannel.receiveAsFlow()

    private var activeRequest: DetailsRequest? = null
    private var loadJob: Job? = null
    private var progressJob: Job? = null

    fun onAction(action: DetailsAction) {
        when (action) {
            is DetailsAction.Load -> load(
                request = action.request,
                initialContent = action.initialContent,
            )
            DetailsAction.Refresh -> refresh()
            is DetailsAction.RecommendationSelected -> selectRecommendation(action.content)
            DetailsAction.PlaySelected -> selectPlay()
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
            observeProgress(request)
            _uiState.value = DetailsUiState(
                isLoading = true,
                content = initialContent?.takeIf { content -> content.id == request.contentId },
            )
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

    private fun navigateBack() {
        viewModelScope.launch {
            effectsChannel.send(DetailsEffect.NavigateBack)
        }
    }

    private suspend fun emitError(error: AppError) {
        effectsChannel.send(DetailsEffect.ShowError(error))
    }
}