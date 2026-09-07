package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ButtonDefaults as TouchButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreControlDefaults
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme

@Composable
fun StreamCoreTvTextButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    contentColor: Color? = null,
) {
    val colors = StreamCoreTextButtonDefaults.colors(contentColor)
    val shape = StreamCoreTextButtonDefaults.shape()
    val controlStyle = StreamCoreControlDefaults.style()

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .defaultMinSize(minHeight = StreamCoreDimens.Button.MinHeight)
            // TV Surface's inner Box drops minimum constraints; bound the content height.
            .height(IntrinsicSize.Min),
        shape = streamCoreTvButtonShape(shape),
        colors = ButtonDefaults.colors(
            containerColor = colors.containerColor,
            contentColor = colors.contentColor,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            focusedContentColor = colors.contentColor,
            pressedContainerColor = controlStyle.pressed,
            pressedContentColor = controlStyle.onPressed,
            disabledContainerColor = colors.disabledContainerColor,
            disabledContentColor = colors.disabledContentColor,
        ),
        border = streamCoreTvButtonBorder(shape),
        scale = ButtonDefaults.scale(focusedScale = 1f, pressedScale = 1f),
        tonalElevation = 0.dp,
        contentPadding = TouchButtonDefaults.TextButtonContentPadding,
    ) {
        Text(
            text = text,
            style = StreamCoreTextButtonDefaults.labelStyle(),
            modifier = Modifier
                .fillMaxHeight()
                .wrapContentHeight(Alignment.CenterVertically),
        )
    }
}

@Preview
@Composable
private fun StreamCoreTvTextButtonPreview() {
    StreamCoreTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
            modifier = Modifier.padding(StreamCoreDimens.Spacing.Large),
        ) {
            StreamCoreTvTextButton(text = "Forgot password?", onClick = {}, enabled = true)
            StreamCoreTvTextButton(text = "Create account", onClick = {}, enabled = true)
            StreamCoreTvTextButton(text = "Need help?", onClick = {}, enabled = false)
        }
    }
}
