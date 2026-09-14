package com.pampoukidis.streamcoretv.feature.library.common.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.core.model.library.LibraryModel
import com.pampoukidis.streamcoretv.feature.library.domain.ObserveLibraryUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LibraryViewModel constructor(
    private val observeLibrary: ObserveLibraryUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val effectsChannel = Channel<LibraryEffect>(capacity = Channel.BUFFERED)
    val effects: Flow<LibraryEffect> = effectsChannel.receiveAsFlow()

    private var activeProfileId: String? = null
    private var observeJob: Job? = null

    fun onAction(action: LibraryAction) {
        when (action) {
            is LibraryAction.Load -> load(action.profileId)
            LibraryAction.Retry -> retry()
            is LibraryAction.ContentSelected -> selectContent(action.content, action.sourceArtworkUrl)
        }
    }

    private fun load(
        profileId: String,
        force: Boolean = false,
    ) {
        if (!force && activeProfileId == profileId && observeJob?.isActive == true) {
            return
        }

        activeProfileId = profileId
        observeJob?.cancel()
        _uiState.update { state -> state.copy(isLoading = true, error = null) }
        observeJob = viewModelScope.launch {
            observeLibrary(profileId).collect { result ->
                when (result) {
                    is AppResult.Success -> applyLibrary(result.value)
                    is AppResult.Failure -> {
                        _uiState.update { state ->
                            state.copy(isLoading = false, error = result.error)
                        }
                        effectsChannel.send(LibraryEffect.ShowError(result.error))
                    }
                }
            }
        }
    }

    private fun retry() {
        val profileId = activeProfileId ?: return
        load(profileId = profileId, force = true)
    }

    private fun applyLibrary(library: LibraryModel) {
        _uiState.value = LibraryUiState(
            isLoading = false,
            continueWatching = library.continueWatching,
            likedContent = library.likedContent,
            myListContent = library.myListContent,
        )
    }

    private fun selectContent(content: ContentModel, sourceArtworkUrl: String?) {
        viewModelScope.launch {
            effectsChannel.send(LibraryEffect.ContentSelected(content, sourceArtworkUrl))
        }
    }
}
