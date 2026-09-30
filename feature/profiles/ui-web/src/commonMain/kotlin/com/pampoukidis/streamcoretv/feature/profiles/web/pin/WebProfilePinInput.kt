package com.pampoukidis.streamcoretv.feature.profiles.web.pin

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pampoukidis.streamcoretv.feature.profiles.common.pin.ProfilePinUiState

/** Native browser input; covered by the owning screen's backend-free previews. */
@Composable
internal expect fun WebProfilePinInput(
    state: ProfilePinUiState,
    label: String,
    focusRequest: Int,
    onDraftChanged: (String) -> Unit,
    onCancel: () -> Unit,
    onTab: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
)
