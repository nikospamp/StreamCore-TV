package com.pampoukidis.streamcoretv.image

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.time.TimeSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CoilDiskCacheTest {

    @Test
    fun reloadAfterMemoryEvictionUsesDiskWithoutSecondNetworkRequest() {
        LocalImageServer().use { server ->
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val cacheDirectory = context.cacheDir.resolve("coil-kmp06-cache-${System.nanoTime()}")
            val imageLoader = ImageLoader.Builder(context)
                .memoryCache {
                    MemoryCache.Builder()
                        .maxSizeBytes(OneMegabyte)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(cacheDirectory)
                        .maxSizeBytes(FourMegabytes)
                        .build()
                }
                .build()

            try {
                val request = ImageRequest.Builder(context)
                    .data(server.url)
                    .memoryCacheKey(CacheKey)
                    .diskCacheKey(CacheKey)
                    .build()

                val coldStart = TimeSource.Monotonic.markNow()
                val cold = runBlocking { imageLoader.execute(request) }
                val coldMillis = coldStart.elapsedNow().inWholeMilliseconds
                assertTrue(cold is SuccessResult)
                cold as SuccessResult
                assertEquals(DataSource.NETWORK, cold.dataSource)
                assertEquals(1, server.requestCount.get())

                assertNotNull(cold.memoryCacheKey)
                val memoryCacheKey = requireNotNull(cold.memoryCacheKey)
                assertTrue(imageLoader.memoryCache?.remove(memoryCacheKey) == true)

                val warmStart = TimeSource.Monotonic.markNow()
                val warm = runBlocking { imageLoader.execute(request) }
                val warmMillis = warmStart.elapsedNow().inWholeMilliseconds
                assertTrue(warm is SuccessResult)
                warm as SuccessResult
                assertEquals(DataSource.DISK, warm.dataSource)
                assertEquals(1, server.requestCount.get())
                println(
                    "KMP06_COIL_CACHE coldMs=$coldMillis warmMs=$warmMillis " +
                        "coldSource=${cold.dataSource} warmSource=${warm.dataSource} requests=1",
                )
            } finally {
                imageLoader.shutdown()
                cacheDirectory.deleteRecursively()
            }
        }
    }

    private class LocalImageServer : AutoCloseable {
        private val serverSocket = ServerSocket(
            0,
            1,
            InetAddress.getByName(LoopbackAddress),
        )
        private val worker = thread(
            name = "coil-kmp06-http-server",
            isDaemon = true,
        ) {
            while (!serverSocket.isClosed) {
                runCatching {
                    serverSocket.accept().use { socket ->
                        val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                        while (!reader.readLine().isNullOrEmpty()) {
                            // Consume the request headers.
                        }
                        requestCount.incrementAndGet()
                        socket.getOutputStream().buffered().use { output ->
                            output.write(
                                (
                                    "HTTP/1.1 200 OK\r\n" +
                                        "Content-Type: image/gif\r\n" +
                                        "Cache-Control: public, max-age=3600\r\n" +
                                        "Content-Length: ${ImageBytes.size}\r\n" +
                                        "Connection: close\r\n\r\n"
                                    ).encodeToByteArray(),
                            )
                            output.write(ImageBytes)
                        }
                    }
                }
            }
        }

        val requestCount = AtomicInteger()
        val url: String = "http://$LoopbackAddress:${serverSocket.localPort}/avatar.gif"

        override fun close() {
            serverSocket.close()
            worker.join(ServerShutdownTimeoutMillis)
        }
    }

    private companion object {
        const val CacheKey = "kmp06-avatar"
        const val LoopbackAddress = "127.0.0.1"
        const val OneMegabyte = 1L * 1024L * 1024L
        const val FourMegabytes = 4L * 1024L * 1024L
        const val ServerShutdownTimeoutMillis = 1_000L

        val ImageBytes: ByteArray = Base64.decode(
            "R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==",
            Base64.DEFAULT,
        )
    }
}
