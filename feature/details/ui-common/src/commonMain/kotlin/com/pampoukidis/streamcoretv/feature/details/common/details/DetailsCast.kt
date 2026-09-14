package com.pampoukidis.streamcoretv.feature.details.common.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.details.common.resources.Res
import com.pampoukidis.streamcoretv.feature.details.common.resources.details_cast
import com.pampoukidis.streamcoretv.feature.details.common.testing.DetailsPreviewData
import org.jetbrains.compose.resources.stringResource

/** Shared cast formatting; the platform owns expansion controls and reading interaction. */
@Composable
fun DetailsCast(
    content: ContentModel,
    modifier: Modifier = Modifier,
    headingStyle: TextStyle = MaterialTheme.typography.titleSmall,
    castStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    afterCast: @Composable () -> Unit = {},
) {
    val castText = remember(content.cast) {
        content.cast.joinToString(separator = " · ") { cast ->
            cast.characterName?.let { characterName ->
                "${cast.name} as $characterName"
            } ?: cast.name
        }
    }

    if (castText.isNotBlank()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Tiny),
            modifier = modifier,
        ) {
            Text(
                text = stringResource(Res.string.details_cast),
                style = headingStyle,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = castText,
                style = castStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = maxLines,
                overflow = if (maxLines == Int.MAX_VALUE) TextOverflow.Clip else TextOverflow.Ellipsis,
                onTextLayout = onTextLayout,
            )
            afterCast()
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun DetailsCastPreview() {
    StreamCoreTheme(darkTheme = true) {
        DetailsCast(content = DetailsPreviewData.content)
    }
}
