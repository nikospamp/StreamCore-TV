package com.pampoukidis.streamcoretv.playback.web

import com.pampoukidis.streamcoretv.playback.api.PlaybackSession
import com.pampoukidis.streamcoretv.playback.api.PlaybackSessionFactory
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertTrue

internal class WebPlaybackModuleTest {
    @Test
    fun moduleRegistersOnlyTheFactoryOwnershipBoundary() {
        val application = koinApplication {
            modules(webPlaybackModule)
        }

        val factory = application.koin.get<PlaybackSessionFactory>()

        val firstSession = factory.create()
        val secondSession = factory.create()

        assertTrue(factory is WebPlaybackSessionFactory)
        assertTrue(firstSession !== secondSession)
        assertTrue(application.koin.getAll<PlaybackSession>().isEmpty())
        firstSession.close()
        secondSession.close()
        application.close()
    }
}
