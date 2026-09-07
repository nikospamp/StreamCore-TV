package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme

@Composable
fun StreamCoreTextButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    contentColor: Color? = null,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        colors = StreamCoreTextButtonDefaults.colors(contentColor),
        shape = StreamCoreTextButtonDefaults.shape(),
        modifier = modifier,
    ) {
        Text(text = text, style = StreamCoreTextButtonDefaults.labelStyle())
    }
}

/** The touch renderer remains the visual reference for text actions on every input surface. */
internal object StreamCoreTextButtonDefaults {
    @Composable
    fun colors(contentColor: Color?): ButtonColors {
        return if (contentColor != null) {
            ButtonDefaults.textButtonColors(contentColor = contentColor)
        } else {
            ButtonDefaults.textButtonColors()
        }
    }

    @Composable
    fun shape(): Shape {
        return ButtonDefaults.textShape
    }

    @Composable
    fun labelStyle(): TextStyle {
        return MaterialTheme.typography.labelLarge
    }
}

@Preview
@Composable
private fun StreamCoreTextButtonPreview() {
    StreamCoreTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
            modifier = Modifier
                .fillMaxWidth()
                .padding(StreamCoreDimens.Spacing.Large)
        ) {
            StreamCoreTextButton(
                text = "Forgot password",
                onClick = {},
                enabled = true,
            )
            StreamCoreTextButton(
                text = "Disabled",
                onClick = {},
                enabled = false,
            )
        }
    }
}