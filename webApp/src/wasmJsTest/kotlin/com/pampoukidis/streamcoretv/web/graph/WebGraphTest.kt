package com.pampoukidis.streamcoretv.web.graph

import com.pampoukidis.streamcoretv.web.config.WebRuntimeConfig
import com.pampoukidis.streamcoretv.playback.api.PlaybackSessionFactory
import org.koin.core.context.GlobalContext
import org.koin.dsl.koinApplication
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class WebGraphTest {
    @AfterTest
    fun tearDown() {
        GlobalContext.getOrNull()?.close()
    }

    @Test
    fun tmdbOnlyGraphResolvesEveryRepositoryAndViewModelFactory() {
        val application = koinApplication {
            modules(
                webModules(
                    config = WebRuntimeConfig(
                        tmdbBaseUrl = "https://api.example.test/3/",
                        tmdbReadAccessToken = "browser-visible-token",
                        tmdbAccountId = "42",
                    ),
                    useSessionStorage = true,
                ),
            )
        }

        val resolved = resolveWebGraph(application.koin)

        assertEquals(17, resolved.size)
        assertEquals(1, application.koin.getAll<PlaybackSessionFactory>().size)
        application.close()
    }
}
