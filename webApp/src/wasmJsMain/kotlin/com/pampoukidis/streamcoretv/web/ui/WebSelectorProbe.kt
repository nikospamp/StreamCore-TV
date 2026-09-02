package com.pampoukidis.streamcoretv.web.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreButton
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens

object WebSelectorProbeTags {
    const val Root = "web-selector-probe"
    const val FocusButton = "web-selector-focus-button"
    const val Input = "web-selector-input"
    const val Status = "web-selector-status"
}

@Composable
fun WebSelectorProbe(
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Waiting for selector probe interaction") }
    val inputFocusRequester = remember { FocusRequester() }

    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        modifier = modifier
            .widthIn(max = ProbeMaximumWidth)
            .testTag(WebSelectorProbeTags.Root),
    ) {
        Text(
            text = "Compose UI Test v2 selector probe",
            style = MaterialTheme.typography.titleMedium,
        )
        StreamCoreButton(
            text = "Focus tagged input",
            onClick = {
                inputFocusRequester.requestFocus()
                status = "Tagged input focused"
            },
            enabled = true,
            modifier = Modifier
                .testTag(WebSelectorProbeTags.FocusButton)
                .semantics { contentDescription = "Focus selector probe input" },
        )
        OutlinedTextField(
            value = text,
            onValueChange = { value ->
                text = value
                status = "Input value: $value"
            },
            label = { Text("Selector probe input") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(inputFocusRequester)
                .testTag(WebSelectorProbeTags.Input)
                .semantics { contentDescription = "Selector probe input" },
        )
        Text(
            text = status,
            modifier = Modifier.testTag(WebSelectorProbeTags.Status),
        )
    }
}

private val ProbeMaximumWidth = 520.dp
