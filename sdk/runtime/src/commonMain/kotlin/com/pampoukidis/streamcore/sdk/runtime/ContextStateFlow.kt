package com.pampoukidis.streamcore.sdk.runtime

import com.pampoukidis.streamcore.sdk.model.StreamCoreContext
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Synchronous view of one atomic runtime state; no second mutable context or collector scope. */
@OptIn(InternalCoroutinesApi::class)
internal class ContextStateFlow<S>(
    private val source: StateFlow<S>,
    private val snapshot: (S) -> StreamCoreContext,
) : StateFlow<StreamCoreContext> {
    override val value: StreamCoreContext get() = snapshot(source.value)
    override val replayCache: List<StreamCoreContext> get() = listOf(value)
    override suspend fun collect(collector: FlowCollector<StreamCoreContext>): Nothing {
        source.map { snapshot(it) }.distinctUntilChanged().collect(collector)
        awaitCancellation()
    }
}
