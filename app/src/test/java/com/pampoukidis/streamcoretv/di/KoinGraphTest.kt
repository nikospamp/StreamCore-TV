package com.pampoukidis.streamcoretv.di

import android.content.Context
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.verify.verify

@OptIn(KoinExperimentalAPI::class)
class KoinGraphTest {

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun selectedFlavorGraphVerifiesStrictly() {
        val commonModules = commonAndroidModules()
        val selectedProviderModules = providerModules()
        val completeGraph = module {
            includes(commonModules)
            includes(selectedProviderModules)
        }

        assertEquals(EXPECTED_COMMON_MODULE_COUNT, commonModules.size)
        assertEquals(EXPECTED_PROVIDER_MODULE_COUNT, selectedProviderModules.size)
        completeGraph.verify(
            extraTypes = verifierProvidedTypes(),
        )
    }

    @Test
    fun selectedFlavorGraphStartsInIsolation() {
        stopKoin()

        startKoin {
            modules(commonAndroidModules() + providerModules())
        }
    }

    private fun verifierProvidedTypes(): List<kotlin.reflect.KClass<*>> {
        return buildList {
            add(Context::class)
            runCatching {
                Class.forName(KTOR_HTTP_CLIENT_ENGINE_CLASS).kotlin
            }.getOrNull()?.let(::add)
        }
    }

    private companion object {
        const val EXPECTED_COMMON_MODULE_COUNT = 19
        const val EXPECTED_PROVIDER_MODULE_COUNT = 4
        const val KTOR_HTTP_CLIENT_ENGINE_CLASS = "io.ktor.client.engine.HttpClientEngine"
    }
}
