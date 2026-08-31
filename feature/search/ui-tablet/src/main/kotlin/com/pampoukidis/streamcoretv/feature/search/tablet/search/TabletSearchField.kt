package com.pampoukidis.streamcoretv.feature.search.tablet.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreCloseButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreSearchIcon
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.search.common.testing.SearchTestTags
import com.pampoukidis.streamcoretv.feature.search.tablet.R

@Composable
internal fun TabletSearchField(
    query: String,
    onQueryChanged: (String) -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    val focusModifier = if (focusRequester != null) {
        Modifier.focusRequester(focusRequester)
    } else {
        Modifier
    }
    val fieldLabel = stringResource(R.string.search_field_label)
    val clearDescription = stringResource(R.string.search_clear_query)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(StreamCoreDimens.Button.MinHeight)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.large,
            ),
    ) {
        Spacer(modifier = Modifier.width(StreamCoreDimens.Spacing.Medium))
        StreamCoreSearchIcon(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(StreamCoreDimens.Icon.Medium),
        )
        Spacer(modifier = Modifier.width(StreamCoreDimens.Spacing.Medium))
        BasicTextField(
            value = query,
            onValueChange = onQueryChanged,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search,
            ),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            decorationBox = { innerTextField ->
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier.fillMaxHeight(),
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.search_field_placeholder),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    innerTextField()
                }
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .then(focusModifier)
                .semantics { contentDescription = fieldLabel }
                .testTag(SearchTestTags.Field),
        )
        if (query.isNotEmpty()) {
            StreamCoreCloseButton(
                onClick = onClear,
                enabled = true,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                contentDescription = clearDescription,
                modifier = Modifier.testTag(SearchTestTags.ClearQuery),
            )
        } else {
            Spacer(modifier = Modifier.width(StreamCoreDimens.Spacing.Medium))
        }
    }
}

@Preview(widthDp = 720)
@Composable
private fun TabletSearchFieldPreview() {
    StreamCoreTheme {
        TabletSearchField(
            query = "Orbit",
            onQueryChanged = {},
            onClear = {},
            onSubmit = {},
        )
    }
}
