package com.pampoukidis.streamcoretv.web.platform

import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import kotlinx.browser.window
import okio.use
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image

internal class WebKitImageDecoder(
    private val source: ImageSource,
) : Decoder {
    override suspend fun decode(): DecodeResult {
        val bytes = source.source().use { bufferedSource -> bufferedSource.readByteArray() }
        val encodedImage = Image.makeFromEncoded(bytes)
        return try {
            val bitmap = Bitmap.makeFromImage(encodedImage)
            bitmap.setImmutable()
            DecodeResult(
                image = bitmap.asImage(),
                isSampled = false,
            )
        } finally {
            encodedImage.close()
        }
    }

    class Factory : Decoder.Factory {
        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            return if (isWebKitBrowser()) {
                WebKitImageDecoder(result.source)
            } else {
                null
            }
        }
    }
}

private fun isWebKitBrowser(): Boolean {
    val userAgent = window.navigator.userAgent
    return "AppleWebKit" in userAgent && "Chrome" !in userAgent && "Chromium" !in userAgent
}
