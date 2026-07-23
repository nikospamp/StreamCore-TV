package com.pampoukidis.streamcoretv.feature.profiles.mobile.profiles

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import kotlin.math.max

@Composable
internal fun MobileProfilesBackdrop(
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val background = colors.background
    val isDark = background.luminance() < DarkThemeLuminanceThreshold
    val upperGlowAlpha = if (isDark) DarkUpperGlowAlpha else LightUpperGlowAlpha
    val profileGlowAlpha = if (isDark) DarkProfileGlowAlpha else LightProfileGlowAlpha
    val lowerDepthAlpha = if (isDark) DarkLowerDepthAlpha else LightLowerDepthAlpha
    val vignetteAlpha = if (isDark) DarkVignetteAlpha else LightVignetteAlpha
    val baseAnimatedGlowAlpha = if (isDark) {
        DarkBaseAnimatedGlowAlpha
    } else {
        LightBaseAnimatedGlowAlpha
    }
    val animatedGlowAlphaRange = if (isDark) {
        DarkAnimatedGlowAlphaRange
    } else {
        LightAnimatedGlowAlphaRange
    }
    val driftProgress = rememberBackdropDriftProgress()
    val upperGlowCenterColor = if (isDark) {
        colors.surfaceBright
    } else {
        colors.surfaceContainerHighest
    }
    val upperGlowMidColor = if (isDark) {
        colors.surfaceContainerHigh
    } else {
        colors.surfaceContainer
    }
    val leftProfileGlowColor = if (isDark) {
        colors.secondaryFixedDim
    } else {
        colors.secondary
    }
    val rightProfileGlowColor = if (isDark) {
        colors.tertiaryFixedDim
    } else {
        colors.tertiary
    }

    Box(
        modifier = modifier.drawWithCache {
            val transparentUpperGlow = upperGlowCenterColor.copy(alpha = 0f)
            val transparentLeftGlow = leftProfileGlowColor.copy(alpha = 0f)
            val transparentRightGlow = rightProfileGlowColor.copy(alpha = 0f)
            val transparentVignette = colors.scrim.copy(alpha = 0f)
            val upperGlow = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to upperGlowCenterColor.copy(alpha = upperGlowAlpha),
                    0.52f to upperGlowMidColor.copy(alpha = upperGlowAlpha * 0.46f),
                    1f to transparentUpperGlow,
                ),
                center = Offset(
                    x = size.width * UpperGlowCenterX,
                    y = size.height * UpperGlowCenterY,
                ),
                radius = size.width * UpperGlowRadiusMultiplier,
            )
            val leftProfileGlow = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to leftProfileGlowColor.copy(alpha = profileGlowAlpha),
                    ProfileGlowMidStop to leftProfileGlowColor.copy(
                        alpha = profileGlowAlpha * ProfileGlowMidAlphaMultiplier,
                    ),
                    1f to transparentLeftGlow,
                ),
                center = Offset(
                    x = size.width * LeftProfileGlowCenterX,
                    y = size.height * ProfileGlowCenterY,
                ),
                radius = size.width * ProfileGlowRadiusMultiplier,
            )
            val rightProfileGlow = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to rightProfileGlowColor.copy(alpha = profileGlowAlpha),
                    ProfileGlowMidStop to rightProfileGlowColor.copy(
                        alpha = profileGlowAlpha * ProfileGlowMidAlphaMultiplier,
                    ),
                    1f to transparentRightGlow,
                ),
                center = Offset(
                    x = size.width * RightProfileGlowCenterX,
                    y = size.height * ProfileGlowCenterY,
                ),
                radius = size.width * ProfileGlowRadiusMultiplier,
            )
            val lowerDepth = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    colors.surfaceContainerLowest.copy(alpha = lowerDepthAlpha),
                ),
                startY = size.height * LowerDepthStartY,
                endY = size.height,
            )
            val vignette = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to transparentVignette,
                    VignetteClearStop to transparentVignette,
                    1f to colors.scrim.copy(alpha = vignetteAlpha),
                ),
                center = Offset(
                    x = size.width * VignetteCenterX,
                    y = size.height * VignetteCenterY,
                ),
                radius = max(size.width, size.height) * VignetteRadiusMultiplier,
            )
            val upperGlowCenter = Offset(
                x = size.width * UpperGlowCenterX,
                y = size.height * UpperGlowCenterY,
            )
            val upperGlowRadius = size.width * UpperGlowRadiusMultiplier
            val leftProfileGlowCenter = Offset(
                x = size.width * LeftProfileGlowCenterX,
                y = size.height * ProfileGlowCenterY,
            )
            val rightProfileGlowCenter = Offset(
                x = size.width * RightProfileGlowCenterX,
                y = size.height * ProfileGlowCenterY,
            )
            val profileGlowRadius = size.width * ProfileGlowRadiusMultiplier

            onDrawBehind {
                val drift = driftProgress.value
                val horizontalDrift = size.width * ProfileGlowHorizontalDriftMultiplier * drift
                val verticalDrift = size.height * ProfileGlowVerticalDriftMultiplier * drift
                val leftGlowAlpha = baseAnimatedGlowAlpha +
                        animatedGlowAlphaRange * ((drift + 1f) / 2f)
                val rightGlowAlpha = baseAnimatedGlowAlpha +
                        animatedGlowAlphaRange * ((1f - drift) / 2f)
                val leftGlowScale = BaseAnimatedGlowScale +
                        PositiveAnimatedGlowScaleRange * ((drift + 1f) / 2f)
                val rightGlowScale = BaseAnimatedGlowScale +
                        PositiveAnimatedGlowScaleRange * ((1f - drift) / 2f)

                drawRect(color = background)
                withTransform(
                    transformBlock = {
                        translate(
                            left = horizontalDrift * UpperGlowDriftMultiplier,
                            top = verticalDrift * UpperGlowDriftMultiplier,
                        )
                    },
                ) {
                    drawCircle(
                        brush = upperGlow,
                        radius = upperGlowRadius,
                        center = upperGlowCenter,
                    )
                }
                withTransform(
                    transformBlock = {
                        translate(
                            left = horizontalDrift,
                            top = verticalDrift,
                        )
                        scale(
                            scaleX = leftGlowScale,
                            scaleY = leftGlowScale,
                            pivot = leftProfileGlowCenter,
                        )
                    },
                ) {
                    drawCircle(
                        brush = leftProfileGlow,
                        radius = profileGlowRadius,
                        center = leftProfileGlowCenter,
                        alpha = leftGlowAlpha,
                    )
                }
                withTransform(
                    transformBlock = {
                        translate(
                            left = -horizontalDrift,
                            top = -verticalDrift,
                        )
                        scale(
                            scaleX = rightGlowScale,
                            scaleY = rightGlowScale,
                            pivot = rightProfileGlowCenter,
                        )
                    },
                ) {
                    drawCircle(
                        brush = rightProfileGlow,
                        radius = profileGlowRadius,
                        center = rightProfileGlowCenter,
                        alpha = rightGlowAlpha,
                    )
                }
                drawRect(brush = lowerDepth)
                drawRect(brush = vignette)
            }
        },
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

@Preview
@Composable
private fun MobileProfilesBackdropPreview() {
    StreamCoreTheme(darkTheme = true) {
        MobileProfilesBackdrop(modifier = Modifier.fillMaxSize())
    }
}

private const val DarkThemeLuminanceThreshold = 0.5f
private const val DarkUpperGlowAlpha = 0.48f
private const val LightUpperGlowAlpha = 0.64f
private const val DarkProfileGlowAlpha = 0.22f
private const val LightProfileGlowAlpha = 0.18f
private const val DarkLowerDepthAlpha = 0.48f
private const val LightLowerDepthAlpha = 0.24f
private const val DarkVignetteAlpha = 0.22f
private const val LightVignetteAlpha = 0.06f
private const val UpperGlowCenterX = 0.5f
private const val UpperGlowCenterY = 0.20f
private const val UpperGlowRadiusMultiplier = 1.22f
private const val LeftProfileGlowCenterX = 0.24f
private const val RightProfileGlowCenterX = 0.76f
private const val ProfileGlowCenterY = 0.31f
private const val ProfileGlowRadiusMultiplier = 0.58f
private const val ProfileGlowMidStop = 0.44f
private const val ProfileGlowMidAlphaMultiplier = 0.46f
private const val ProfileGlowHorizontalDriftMultiplier = 0.036f
private const val ProfileGlowVerticalDriftMultiplier = 0.014f
private const val UpperGlowDriftMultiplier = 0.48f
private const val DarkBaseAnimatedGlowAlpha = 0.76f
private const val DarkAnimatedGlowAlphaRange = 0.24f
private const val LightBaseAnimatedGlowAlpha = 0.52f
private const val LightAnimatedGlowAlphaRange = 0.48f
private const val BaseAnimatedGlowScale = 0.90f
private const val PositiveAnimatedGlowScaleRange = 0.20f
private const val StaticDriftProgress = 0f
private const val DefaultAnimatorDurationScale = 1f
private const val BackdropDriftDurationMillis = 12_000
private const val LowerDepthStartY = 0.60f
private const val VignetteClearStop = 0.72f
private const val VignetteCenterX = 0.5f
private const val VignetteCenterY = 0.40f
private const val VignetteRadiusMultiplier = 0.78f
