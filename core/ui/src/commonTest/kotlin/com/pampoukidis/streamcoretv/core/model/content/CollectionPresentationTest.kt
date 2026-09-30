package com.pampoukidis.streamcoretv.core.model.content

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreCollection
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreCollectionPresentationHint
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreCollectionPurpose
import kotlin.test.Test
import kotlin.test.assertEquals

class CollectionPresentationTest {
    @Test
    fun semanticCollectionsPreserveExistingLayouts() {
        val purposes = listOf(
            StreamCoreCollectionPurpose.Featured to RowType.Featured,
            StreamCoreCollectionPurpose.ContinueWatching to RowType.ContinueWatching,
            StreamCoreCollectionPurpose.Ranked to RowType.TopTen,
        )
        purposes.forEach { (purpose, expected) ->
            assertEquals(expected, collection(purpose).toRowModel().type)
        }
    }

    @Test
    fun standardCollectionsUseOptionalProviderHintAndKeepMetadata() {
        val source = collection(StreamCoreCollectionPurpose.Standard)
        assertEquals(RowType.Poster, source.toRowModel().type)
        val projected = source.copy(presentationHint = StreamCoreCollectionPresentationHint.Landscape).toRowModel()
        assertEquals(RowType.Landscape, projected.type)
        assertEquals(source.id, projected.id)
        assertEquals(source.title, projected.title)
        assertEquals(source.subtitle, projected.subtitle)
        assertEquals(source.content, projected.content)
    }

    private fun collection(purpose: StreamCoreCollectionPurpose): StreamCoreCollection {
        return StreamCoreCollection("collection-1", "Collection", "Subtitle", emptyList(), purpose)
    }
}
