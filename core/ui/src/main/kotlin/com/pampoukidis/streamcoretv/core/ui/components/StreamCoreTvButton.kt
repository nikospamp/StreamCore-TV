package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonBorder
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ButtonScale
import androidx.tv.material3.ButtonShape
import androidx.tv.material3.Text
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme

@Composable
fun StreamCoreTvButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        scale = streamCoreTvButtonScale(),
        shape = streamCoreTvButtonShape(),
        border = streamCoreTvButtonBorder(),
        modifier = modifier,
    ) {
        if (loading) {
            CircularProgressIndicator(
                strokeWidth = StreamCoreDimens.Button.LoadingIndicatorStrokeWidth,
                modifier = Modifier.size(StreamCoreDimens.Button.LoadingIndicatorSize),
            )
        } else {
            Text(text = text)
        }
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

@Composable
private fun streamCoreTvButtonShape(): ButtonShape {
    val shape = MaterialTheme.shapes.small
    return ButtonDefaults.shape(
        shape = shape,
        focusedShape = shape,
        pressedShape = shape,
        disabledShape = shape,
        focusedDisabledShape = shape,
    )
}

@Composable
private fun focusedButtonBorder(): Border {
    return Border(
        border = BorderStroke(
            width = StreamCoreDimens.Tv.Focus.BorderWidth,
            color = MaterialTheme.colorScheme.primary,
        ),
        inset = StreamCoreDimens.Tv.Focus.BorderPadding,
        shape = MaterialTheme.shapes.small,
    )
}

@Composable
private fun disabledFocusedButtonBorder(): Border {
    return Border(
        border = BorderStroke(
            width = StreamCoreDimens.Tv.Focus.BorderWidth,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
        ),
        inset = StreamCoreDimens.Tv.Focus.BorderPadding,
        shape = MaterialTheme.shapes.small,
    )
}

@Preview
@Composable
private fun StreamCoreTvButtonPreview() {
    StreamCoreTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
            modifier = Modifier.padding(StreamCoreDimens.Spacing.Large),
        ) {
            StreamCoreTvButton(
                text = "Continue",
                onClick = {},
                enabled = true,
            )
            StreamCoreTvButton(
                text = "Loading",
                onClick = {},
                enabled = false,
                loading = true,
            )
            StreamCoreTvButton(
                text = "Disabled",
                onClick = {},
                enabled = false,
            )
        }
    }
}