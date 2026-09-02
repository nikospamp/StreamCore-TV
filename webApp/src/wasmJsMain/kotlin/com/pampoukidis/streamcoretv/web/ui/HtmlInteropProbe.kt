package com.pampoukidis.streamcoretv.web.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.HtmlElementView
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.web.shaka.runShakaCreateDestroyProbe
import kotlinx.browser.document
import org.w3c.dom.HTMLVideoElement

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HtmlInteropProbe(
    modifier: Modifier = Modifier,
) {
    val result = remember { mutableStateOf("Pending") }
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
        modifier = modifier,
    ) {
        Text(
            text = "HtmlElementView + Shaka 5.2.3 ESM probe",
            style = MaterialTheme.typography.titleMedium,
        )
        HtmlElementView(
            factory = {
                (document.createElement("video") as HTMLVideoElement).apply {
                    controls = false
                    muted = true
                    setAttribute("aria-label", "Shaka interop video probe")
                    setAttribute("data-testid", "html-video-probe")
                }
            },
            update = { videoElement ->
                if (result.value == "Pending") {
                    val probeResult = runCatching {
                        check(runShakaCreateDestroyProbe(videoElement))
                        "Create/destroy linked"
                    }.getOrElse { throwable ->
                        "Probe failed: ${throwable.message ?: "unknown interop error"}"
                    }
                    result.value = probeResult
                    videoElement.setAttribute(
                        "data-shaka-probe",
                        if (probeResult == "Create/destroy linked") "linked" else "failed",
                    )
                }
            },
            onRelease = { videoElement ->
                videoElement.pause()
                videoElement.removeAttribute("src")
            },
            modifier = Modifier.size(
                width = VideoProbeWidth,
                height = VideoProbeHeight,
            ),
        )
        Text(
            text = result.value,
            modifier = Modifier,
        )
    }
}

private val VideoProbeWidth = 320.dp
private val VideoProbeHeight = 180.dp
