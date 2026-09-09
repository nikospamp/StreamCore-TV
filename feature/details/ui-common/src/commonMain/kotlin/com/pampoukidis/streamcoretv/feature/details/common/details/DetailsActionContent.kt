package com.pampoukidis.streamcoretv.feature.details.common.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreHeartIcon
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme

/** Portable drawing only; callers own touch/D-pad interaction and action semantics. */
@Composable
fun DetailsActionContent(
    label: String,
    contentColor: Color,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    labelStyle: TextStyle = MaterialTheme.typography.labelSmall,
    iconSize: Dp = StreamCoreDimens.Icon.Large,
    loadingSize: Dp = StreamCoreDimens.Icon.Loading,
    spacing: Dp = StreamCoreDimens.Spacing.Tiny,
    icon: @Composable () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing),
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(iconSize),
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = contentColor,
                        strokeWidth = StreamCoreDimens.Stroke.Default,
                        modifier = Modifier.size(loadingSize),
                    )
                } else {
                    icon()
                }
            }
        }
        Text(
            text = label,
            style = labelStyle,
            color = contentColor,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview
@Composable
private fun DetailsActionContentPreview() {
    StreamCoreTheme(darkTheme = true) {
        DetailsActionContent(
            label = "Liked",
            contentColor = MaterialTheme.colorScheme.primary,
            isLoading = false,
        ) {
            StreamCoreHeartIcon(filled = true)
        }
    }
}
