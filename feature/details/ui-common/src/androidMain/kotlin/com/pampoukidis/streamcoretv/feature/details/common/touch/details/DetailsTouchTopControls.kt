package com.pampoukidis.streamcoretv.feature.details.common.touch.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreArtworkIconButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreBackIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreRefreshIcon
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.details.common.resources.Res
import com.pampoukidis.streamcoretv.feature.details.common.resources.details_action_back
import com.pampoukidis.streamcoretv.feature.details.common.resources.details_action_refresh
import com.pampoukidis.streamcoretv.feature.details.common.testing.DetailsTestTags
import org.jetbrains.compose.resources.stringResource

@Composable
fun DetailsTouchTopControls(
    isLoading: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        StreamCoreArtworkIconButton(
            contentDescription = stringResource(Res.string.details_action_back),
            onClick = onBack,
            modifier = Modifier.testTag(DetailsTestTags.BackButton),
        ) {
            StreamCoreBackIcon()
        }
        StreamCoreArtworkIconButton(
            contentDescription = stringResource(Res.string.details_action_refresh),
            onClick = onRefresh,
            isLoading = isLoading,
            modifier = Modifier.testTag(DetailsTestTags.RefreshButton),
        ) {
            StreamCoreRefreshIcon()
        }
    }
}

@Preview
@Composable
private fun DetailsTouchTopControlsPreview() {
    StreamCoreTheme(darkTheme = true) {
        DetailsTouchTopControls(isLoading = false, onBack = {}, onRefresh = {})
    }
}
