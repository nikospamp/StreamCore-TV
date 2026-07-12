package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewMobile

@Composable
fun StreamCoreLoadingChip(
    text: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.labelMedium,
    indicatorSize: Dp = StreamCoreDimens.Chip.IndicatorSize,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = StreamCoreDimens.Chip.ContentHorizontalPadding,
        vertical = StreamCoreDimens.Chip.ContentVerticalPadding,
    ),
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shadowElevation = StreamCoreDimens.Elevation.Medium,
        modifier = modifier.defaultMinSize(minHeight = StreamCoreDimens.Chip.MinHeight),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(contentPadding),
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                strokeWidth = StreamCoreDimens.Stroke.Default,
                modifier = Modifier.size(indicatorSize),
            )
            Text(
                text = text,
                style = textStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@PreviewMobile
@Composable
private fun StreamCoreLoadingChipPreview() {
    StreamCoreTheme {
        Row {
            StreamCoreLoadingChip(
                text = "Updating",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(StreamCoreDimens.Chip.PreviewHeight)
            )
        }
    }
}