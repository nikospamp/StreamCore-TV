package com.pampoukidis.streamcoretv.feature.details.common.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.feature.details.data.DetailsRequest
import com.pampoukidis.streamcoretv.feature.details.domain.LoadDetailsUseCase
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailsUiState())
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    private val effectsChannel = Channel<DetailsEffect>(capacity = Channel.BUFFERED)
    val effects: Flow<DetailsEffect> = effectsChannel.receiveAsFlow()

    private var activeRequest: DetailsRequest? = null
    private var loadJob: Job? = null

    fun onAction(action: DetailsAction) {
        when (action) {
            is DetailsAction.Load -> load(action.request)
            DetailsAction.Refresh -> refresh()
            is DetailsAction.RecommendationSelected -> selectRecommendation(action.contentId)
            DetailsAction.BackSelected -> navigateBack()
        }
    }

    private fun load(
        request: DetailsRequest,
        force: Boolean = false,
    ) {
        if (!force && activeRequest == request) {
            return
        }

        activeRequest = request
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

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

    private fun selectRecommendation(contentId: String) {
        viewModelScope.launch {
            effectsChannel.send(DetailsEffect.RecommendationSelected(contentId))
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