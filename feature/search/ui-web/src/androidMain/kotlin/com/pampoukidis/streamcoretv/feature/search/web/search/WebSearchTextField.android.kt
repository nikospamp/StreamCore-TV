package com.pampoukidis.streamcoretv.feature.search.web.search

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreSearchIcon
import com.pampoukidis.streamcoretv.feature.search.common.testing.SearchTestTags
import org.jetbrains.compose.resources.stringResource
import streamcoretv.feature.search.ui_web.generated.resources.Res
import streamcoretv.feature.search.ui_web.generated.resources.web_search_field_hint

@Composable
internal actual fun WebSearchTextField(
    value: String,
    enabled: Boolean,
    requestFocus: Boolean,
    onValueChange: (String) -> Unit,
    onSubmitCommittedValue: (String) -> Unit,
    onEscape: () -> Unit,
    onFocusRequestConsumed: () -> Unit,
    modifier: Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(requestFocus, focusRequester) {
        if (!requestFocus) {
            return@LaunchedEffect
        }
        repeat(FocusRequestAttempts) {
            withFrameNanos { }
            if (focusRequester.requestFocus()) {
                onFocusRequestConsumed()
                return@LaunchedEffect
            }
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        placeholder = { Text(stringResource(Res.string.web_search_field_hint)) },
        leadingIcon = { StreamCoreSearchIcon() },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Search,
        ),
        keyboardActions = KeyboardActions(
            onSearch = { onSubmitCommittedValue(value) },
        ),
        modifier = modifier
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    false
                } else if (event.key == Key.Escape) {
                    onEscape()
                    true
                } else {
                    false
                }
            }
            .testTag(SearchTestTags.Field),
    )
}

private const val FocusRequestAttempts = 4
