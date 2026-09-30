package com.example.streamcore.uiresources

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

/** Deliberately uses no application UI framework or provider data SDK. */
class UiResourceConsumerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val status = TextView(this)
        status.setPadding(24, 96, 24, 24)
        status.text = "Reading published SDK UI resources…"
        setContentView(status)
        val verify: suspend () -> Unit = { verifyPublishedUiResources() }
        verify.startCoroutine(object : Continuation<Unit> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<Unit>) {
                runOnUiThread {
                    status.text = if (result.isSuccess) {
                        "Published SDK UI resources passed"
                    } else {
                        "Published SDK UI resource check failed: ${result.exceptionOrNull()?.message}"
                    }
                }
            }
        })
    }
}
