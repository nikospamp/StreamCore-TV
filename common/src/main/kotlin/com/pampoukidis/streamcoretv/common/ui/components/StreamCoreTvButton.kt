package com.pampoukidis.streamcoretv.common.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonBorder
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ButtonScale
import androidx.tv.material3.ButtonShape
import androidx.tv.material3.Text
import com.pampoukidis.streamcoretv.common.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.common.ui.theme.StreamCoreTVTheme
import com.pampoukidis.streamcoretv.common.utils.PreviewTV

@Composable
fun StreamCoreTvButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        scale = streamCoreTvButtonScale(),
        shape = streamCoreTvButtonShape(),
        border = streamCoreTvButtonBorder(),
        modifier = modifier,
    ) {
        Text(text = text)
    }
}

@Composable
private fun streamCoreTvButtonBorder(): ButtonBorder = ButtonDefaults.border(
    focusedBorder = focusedButtonBorder(),
    pressedBorder = focusedButtonBorder(),
    focusedDisabledBorder = disabledFocusedButtonBorder(),
)

private fun streamCoreTvButtonScale(): ButtonScale = ButtonDefaults.scale(
    focusedScale = 1f,
    pressedScale = 1f,
)

private fun streamCoreTvButtonShape(): ButtonShape = ButtonDefaults.shape(
    shape = roundedCornerShape(),
    focusedShape = roundedCornerShape(),
    pressedShape = roundedCornerShape(),
    disabledShape = roundedCornerShape(),
    focusedDisabledShape = roundedCornerShape(),
)

private fun roundedCornerShape(): RoundedCornerShape {
    return RoundedCornerShape(StreamCoreDimens.Tv.CornerRadius)
}

@Composable
private fun focusedButtonBorder(): Border {
    return Border(
        border = BorderStroke(
            width = StreamCoreDimens.Tv.FocusBorderWidth,
            color = MaterialTheme.colorScheme.primary,
        ),
        inset = StreamCoreDimens.Tv.FocusBorderPadding,
        shape = roundedCornerShape(),
    )
}

@Composable
private fun disabledFocusedButtonBorder(): Border {
    return Border(
        border = BorderStroke(
            width = StreamCoreDimens.Tv.FocusBorderWidth,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
        ),
        inset = StreamCoreDimens.Tv.FocusBorderPadding,
        shape = roundedCornerShape(),
    )
}

@Preview
@Composable
private fun StreamCoreTvButtonPreview() {
    StreamCoreTVTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            StreamCoreTvButton(
                text = "Continue",
                onClick = {},
                enabled = true,
            )
            StreamCoreTvButton(
                text = "Disabled",
                onClick = {},
                enabled = false,
            )
        }
    }
}
