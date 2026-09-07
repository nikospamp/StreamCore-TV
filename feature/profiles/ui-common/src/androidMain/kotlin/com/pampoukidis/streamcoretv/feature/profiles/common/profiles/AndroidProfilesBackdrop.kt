package com.pampoukidis.streamcoretv.feature.profiles.common.profiles

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme

@Composable
fun AndroidProfilesBackdrop(modifier: Modifier = Modifier) {
    ProfilesBackdrop(
        modifier = modifier,
        driftProgress = rememberBackdropDriftProgress(),
    )
}

@Composable
private fun rememberBackdropDriftProgress(): State<Float> {
    val context = LocalContext.current
    val motionEnabled = !LocalInspectionMode.current &&
            remember(context) { context.areSystemAnimationsEnabled() }
    if (!motionEnabled) {
        return remember { mutableFloatStateOf(StaticDriftProgress) }
    }

    val transition = rememberInfiniteTransition(label = "profiles-backdrop-drift")
    return transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = BackdropDriftDurationMillis,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "profiles-backdrop-drift-progress",
    )
}

private fun Context.areSystemAnimationsEnabled(): Boolean {
    return Settings.Global.getFloat(
        contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        DefaultAnimatorDurationScale,
    ) > 0f
}

private const val StaticDriftProgress = 0f
private const val DefaultAnimatorDurationScale = 1f
private const val BackdropDriftDurationMillis = 12_000

@Preview
@Composable
private fun AndroidProfilesBackdropPreview() {
    StreamCoreTheme(darkTheme = true) {
        AndroidProfilesBackdrop(modifier = Modifier.fillMaxSize())
    }
}
