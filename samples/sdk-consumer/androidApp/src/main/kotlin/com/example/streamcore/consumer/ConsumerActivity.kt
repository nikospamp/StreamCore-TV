package com.example.streamcore.consumer

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import com.pampoukidis.streamcore.sdk.providers.clientb.ClientBSdk
import com.pampoukidis.streamcore.sdk.providers.clientb.ClientBSdkConfiguration
import com.pampoukidis.streamcore.sdk.providers.clientb.createAndroid
import com.pampoukidis.streamcore.sdk.providers.tmdb.TmdbConnectionConfiguration
import com.pampoukidis.streamcore.sdk.providers.tmdb.TmdbSdk
import com.pampoukidis.streamcore.sdk.providers.tmdb.TmdbSdkConfiguration
import com.pampoukidis.streamcore.sdk.providers.tmdb.createAndroid
import com.pampoukidis.streamcore.sdk.api.StreamCoreClient
import com.pampoukidis.streamcore.sdk.model.StreamCorePersistenceMode
import com.pampoukidis.streamcore.sdk.model.StreamCoreConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

class ConsumerActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var client: StreamCoreClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val status = TextView(this)
        status.setPadding(24, 96, 24, 24)
        status.text = "Running published SDK journeyâ€¦"
        setContentView(status)
        val resultFile = File(filesDir, "journey-result.txt")
        resultFile.writeText("running")
        // Optional live-test input is installed into this app's private storage, never built into the APK.
        val liveFile = File(filesDir, "live-tmdb.json")
        val live = if (liveFile.isFile) {
            try { JSONObject(liveFile.readText()) } finally { check(liveFile.delete()) }
        } else null
        val sdk = if (live != null) {
            TmdbSdk.createAndroid(
                this,
                TmdbSdkConfiguration(
                    common = StreamCoreConfiguration("tmdb-production", "external-live-android", persistence = StreamCorePersistenceMode.InMemory),
                    connection = TmdbConnectionConfiguration("https://api.themoviedb.org/", live.getString("readAccessToken")),
                    demoPlayback = true,
                ),
            )
        } else ClientBSdk.createAndroid(
            this,
            ClientBSdkConfiguration(
                common = StreamCoreConfiguration("clientb-reference", "external-android", persistence = StreamCorePersistenceMode.InMemory),
                demoPlayback = true,
            ),
        )
        client = sdk
        // Construction and close deliberately exercise no TMDB authentication or network work.
        TmdbSdk.createAndroid(
            this,
            TmdbSdkConfiguration(
                common = StreamCoreConfiguration("tmdb-reference", "external-tmdb", persistence = StreamCorePersistenceMode.InMemory),
                connection = TmdbConnectionConfiguration("https://api.themoviedb.org/3/", "construction-only"),
            ),
        ).close()
        scope.launch {
            val message = try {
                if (live == null) exerciseClient(sdk)
                else exerciseClient(sdk, live.getString("username"), live.getString("password"))
                "Published SDK journey passed (${if (live == null) "ClientB" else "TMDB live"})"
            } catch (failure: Exception) {
                "Published SDK journey failed (${failure::class.simpleName})"
            }
            resultFile.writeText(message)
            runOnUiThread { status.text = message }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        client?.close()
        super.onDestroy()
    }
}
