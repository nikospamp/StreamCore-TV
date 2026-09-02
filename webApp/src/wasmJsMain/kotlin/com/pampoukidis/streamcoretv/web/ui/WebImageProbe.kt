package com.pampoukidis.streamcoretv.web.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import kotlinx.browser.document
import kotlinx.browser.window

@Composable
fun WebImageProbe(
    modifier: Modifier = Modifier,
) {
    val status = remember { mutableStateOf("Loading") }
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
        modifier = modifier,
    ) {
        Text(
            text = "Coil Ktor3 Fetch probe",
            style = MaterialTheme.typography.titleMedium,
        )
        AsyncImage(
            model = "${window.location.origin}/web-probe.png",
            contentDescription = "Web image loading probe",
            onSuccess = {
                status.value = "Loaded"
                document.body?.setAttribute("data-image-probe", "loaded")
            },
            onError = {
                status.value = "Failed"
                document.body?.setAttribute("data-image-probe", "failed")
            },
            modifier = Modifier.size(ImageProbeSize),
        )
        Text("Image status: ${status.value}")
    }
}

private val ImageProbeSize = 64.dp
