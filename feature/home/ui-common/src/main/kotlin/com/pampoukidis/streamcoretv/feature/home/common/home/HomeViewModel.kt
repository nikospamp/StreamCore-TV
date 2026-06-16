package com.pampoukidis.streamcoretv.feature.home.common.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.feature.home.domain.LoadHomeRowsUseCase
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
class HomeViewModel @Inject constructor(
    private val loadHomeRows: LoadHomeRowsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val effectsChannel = Channel<HomeEffect>(capacity = Channel.BUFFERED)
    val effects: Flow<HomeEffect> = effectsChannel.receiveAsFlow()

    private var activeProfileId: String? = null
    private var loadJob: Job? = null

    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.Load -> load(action.profileId)
            is HomeAction.ContentSelected -> selectContent(action.content)
            HomeAction.Refresh -> refresh()
        }
    }

    private fun load(
        profileId: String,
        force: Boolean = false,
    ) {
        if (!force && activeProfileId == profileId) {
            return
        }

        activeProfileId = profileId
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (val result = loadHomeRows(profileId)) {
                is AppResult.Success -> {
                    if (activeProfileId != profileId) {
                        return@launch
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            rows = result.value,
                        )
                    }
                }

                is AppResult.Failure -> {
                    if (activeProfileId != profileId) {
                        return@launch
                    }

                    _uiState.update { it.copy(isLoading = false) }
                    emitError(result.error)
                }
            }
        }
    }

    private fun refresh() {
        val profileId = activeProfileId ?: return
        load(profileId = profileId, force = true)
    }

    private fun selectContent(content: ContentModel) {
        viewModelScope.launch {
            effectsChannel.send(HomeEffect.ContentSelected(content))
        }
    }

    private suspend fun emitError(error: AppError) {
        effectsChannel.send(HomeEffect.ShowError(error))
    }
}