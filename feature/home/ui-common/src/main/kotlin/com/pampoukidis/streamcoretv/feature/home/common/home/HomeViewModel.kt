package com.pampoukidis.streamcoretv.feature.home.common.home

import com.pampoukidis.streamcoretv.core.tracing.benchmarkCounter
import com.pampoukidis.streamcoretv.core.tracing.benchmarkTrace
import com.pampoukidis.streamcoretv.core.tracing.BenchmarkTracingEnabled

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.PlaybackProgressModel
import com.pampoukidis.streamcoretv.core.model.content.RowModel
import com.pampoukidis.streamcoretv.core.model.content.RowType
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.feature.home.domain.LoadHomeRowsUseCase
import com.pampoukidis.streamcoretv.playback.api.PlaybackProgressEntryModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackProgressRepository
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
    private val progressRepository: PlaybackProgressRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val effectsChannel = Channel<HomeEffect>(capacity = Channel.BUFFERED)
    val effects: Flow<HomeEffect> = effectsChannel.receiveAsFlow()

    private var activeProfileId: String? = null
    private var loadJob: Job? = null
    private var progressJob: Job? = null
    private var backendRows: List<RowModel> = emptyList()
    private var progressEntries: List<PlaybackProgressEntryModel> = emptyList()
    private var publicationGeneration = 0

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
        if (BenchmarkTracingEnabled) {
            publicationGeneration += 1
            benchmarkCounter("SC.Home.publicationGeneration", publicationGeneration)
        }
        observeProgress(profileId)
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            publish("loading") { state -> state.copy(isLoading = true) }

            when (val result = loadHomeRows(profileId)) {
                is AppResult.Success -> {
                    if (activeProfileId != profileId) {
                        return@launch
                    }

                    backendRows = result.value
                    publish("backend") { state ->
                        state.copy(
                            isLoading = false,
                            rows = mergedRows(),
                        )
                    }
                }

                is AppResult.Failure -> {
                    if (activeProfileId != profileId) {
                        return@launch
                    }

                    publish("loading") { state -> state.copy(isLoading = false) }
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

    private fun observeProgress(profileId: String) {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            progressRepository.observe(profileId).collect { entries ->
                if (activeProfileId != profileId) {
                    return@collect
                }
                progressEntries = entries
                publish("progress") { state -> state.copy(rows = mergedRows()) }
            }
        }
    }

    private fun mergedRows(): List<RowModel> {
        val providerRows = backendRows.filterNot { row -> row.type == RowType.ContinueWatching }
        if (progressEntries.isEmpty()) {
            return providerRows
        }
        val continueWatching = RowModel(
            id = ContinueWatchingRowId,
            title = "Continue Watching",
            subtitle = "Pick up where you left off",
            type = RowType.ContinueWatching,
            content = progressEntries.map { entry ->
                entry.contentSnapshot.copy(
                    row = ContinueWatchingRowId,
                    playbackProgress = PlaybackProgressModel(
                        positionMillis = entry.positionMillis,
                        durationMillis = entry.durationMillis,
                    ),
                )
            },
        )
        return listOf(continueWatching) + providerRows
    }

    private suspend fun emitError(error: AppError) {
        effectsChannel.send(HomeEffect.ShowError(error))
    }

    private inline fun publish(
        reason: String,
        crossinline transform: (HomeUiState) -> HomeUiState,
    ) {
        benchmarkTrace("SC.Home.publish.$reason") {
            _uiState.update(transform)
            if (BenchmarkTracingEnabled) {
                val state = _uiState.value
                benchmarkCounter("SC.Home.rowCount", state.rows.size)
                benchmarkCounter(
                    "SC.Home.visibleContentCount",
                    state.rows.sumOf { row -> row.content.size },
                )
                benchmarkCounter(
                    "SC.Home.continueWatchingCount",
                    state.rows.firstOrNull { row -> row.type == RowType.ContinueWatching }?.content?.size ?: 0,
                )
            }
        }
    }

    private companion object {
        const val ContinueWatchingRowId = "continue-watching"
    }
}
