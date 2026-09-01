package com.pampoukidis.streamcoretv.web.platform

import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.network.ktor3.KtorNetworkFetcherFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js

@OptIn(ExperimentalCoilApi::class)
fun configureWebImageLoader() {
    SingletonImageLoader.setSafe { context ->
        ImageLoader.Builder(context)
            .components {
                add(WebKitImageDecoder.Factory())
                add(
                    KtorNetworkFetcherFactory(
                        httpClient = HttpClient(Js),
                    ),
                )
            }
            .build()
    }
}
