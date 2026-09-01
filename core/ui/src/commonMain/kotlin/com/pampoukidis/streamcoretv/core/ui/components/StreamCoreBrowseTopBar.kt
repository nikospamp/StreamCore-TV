package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.pampoukidis.streamcoretv.core.model.auth.ProfileAvatarModel
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun StreamCoreBrowseTopBar(
    onProfileSelected: () -> Unit,
    modifier: Modifier = Modifier,
    profileAvatar: ProfileAvatarModel? = null,
    profileArtworkModifier: Modifier = Modifier,
    onSearchSelected: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(StreamCoreDimens.Icon.TouchTarget),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StreamCoreBrandMark()
            Text(
                text = "StreamCore",
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onSearchSelected != null) {
                IconButton(
                    onClick = onSearchSelected,
                    modifier = Modifier
                        .size(StreamCoreDimens.Icon.TouchTarget)
                        .semantics { contentDescription = "Search" },
                ) {
                    StreamCoreSearchIcon(color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Surface(
                onClick = onProfileSelected,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .size(StreamCoreDimens.Icon.Large)
                    .semantics {
                        contentDescription = "Choose profile"
                    },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (profileAvatar != null) {
                        StreamCoreProfileArtwork(
                            avatar = profileAvatar,
                            contentDescription = null,
                            modifier = Modifier
                                .matchParentSize()
                                .then(profileArtworkModifier),
                        )
                    } else {
                        ProfileGlyph(color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
fun StreamCoreBrandMark(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val foreground = MaterialTheme.colorScheme.onSurface

    Canvas(modifier = modifier.size(StreamCoreDimens.Icon.Medium)) {
        drawRoundRect(
            color = foreground,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                StreamCoreDimens.Spacing.Small.toPx(),
            ),
        )
        drawArc(
            color = primary,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = true,
        )
    }
}

@Preview
@Composable
private fun StreamCoreBrandMarkPreview() {
    StreamCoreTheme {
        Surface {
            StreamCoreBrandMark()
        }
    }
}

@Composable
private fun ProfileGlyph(color: Color) {
    Canvas(modifier = Modifier.size(StreamCoreDimens.Icon.Medium)) {
        val strokeWidth = StreamCoreDimens.Stroke.Icon.toPx()
        drawCircle(
            color = color,
            radius = size.minDimension * 0.17f,
            center = Offset(size.width * 0.5f, size.height * 0.34f),
            style = Stroke(width = strokeWidth),
        )
        drawArc(
            color = color,
            startAngle = 205f,
            sweepAngle = 130f,
            useCenter = false,
            topLeft = Offset(size.width * 0.22f, size.height * 0.48f),
            size = androidx.compose.ui.geometry.Size(
                width = size.width * 0.56f,
                height = size.height * 0.42f,
            ),
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
            ),
        )
    }
}

@Preview
@Composable
private fun StreamCoreBrowseTopBarPreview() {
    StreamCoreTheme {
        Surface {
            Row(modifier = Modifier.height(StreamCoreDimens.Icon.TouchTarget)) {
                StreamCoreBrowseTopBar(onProfileSelected = {})
            }
        }
    }
}
