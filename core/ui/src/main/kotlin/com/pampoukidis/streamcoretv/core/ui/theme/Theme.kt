package com.pampoukidis.streamcoretv.core.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.pampoukidis.streamcoretv.core.ui.utils.LoginPlatform
import com.pampoukidis.streamcoretv.core.ui.utils.rememberLoginPlatform

private val LightColorScheme = lightColorScheme(
    primary = StreamCorePrimary,
    onPrimary = StreamCoreOnPrimary,
    primaryContainer = StreamCorePrimaryContainer,
    onPrimaryContainer = StreamCoreOnPrimaryContainer,
    secondary = StreamCoreSecondary,
    onSecondary = StreamCoreOnSecondary,
    secondaryContainer = StreamCoreSecondaryContainer,
    onSecondaryContainer = StreamCoreOnSecondaryContainer,
    tertiary = StreamCoreTertiary,
    onTertiary = StreamCoreOnTertiary,
    tertiaryContainer = StreamCoreTertiaryContainer,
    onTertiaryContainer = StreamCoreOnTertiaryContainer,
    background = StreamCoreBackground,
    onBackground = StreamCoreOnBackground,
    surface = StreamCoreSurface,
    onSurface = StreamCoreOnSurface,
    surfaceVariant = StreamCoreSurfaceVariant,
    onSurfaceVariant = StreamCoreOnSurfaceVariant,
    outline = StreamCoreOutline,
    outlineVariant = StreamCoreOutlineVariant,
    error = StreamCoreError,
    onError = StreamCoreOnError,
    errorContainer = StreamCoreErrorContainer,
    onErrorContainer = StreamCoreOnErrorContainer,
)

private val DarkColorScheme = darkColorScheme(
    primary = StreamCoreDarkPrimary,
    onPrimary = StreamCoreDarkOnPrimary,
    primaryContainer = StreamCoreDarkPrimaryContainer,
    onPrimaryContainer = StreamCoreDarkOnPrimaryContainer,
    secondary = StreamCoreDarkSecondary,
    onSecondary = StreamCoreDarkOnSecondary,
    secondaryContainer = StreamCoreDarkSecondaryContainer,
    onSecondaryContainer = StreamCoreDarkOnSecondaryContainer,
    tertiary = StreamCoreDarkTertiary,
    onTertiary = StreamCoreDarkOnTertiary,
    tertiaryContainer = StreamCoreDarkTertiaryContainer,
    onTertiaryContainer = StreamCoreDarkOnTertiaryContainer,
    background = StreamCoreDarkBackground,
    onBackground = StreamCoreDarkOnBackground,
    surface = StreamCoreDarkSurface,
    onSurface = StreamCoreDarkOnSurface,
    surfaceVariant = StreamCoreDarkSurfaceVariant,
    onSurfaceVariant = StreamCoreDarkOnSurfaceVariant,
    outline = StreamCoreDarkOutline,
    outlineVariant = StreamCoreDarkOutlineVariant,
    error = StreamCoreDarkError,
    onError = StreamCoreDarkOnError,
    errorContainer = StreamCoreDarkErrorContainer,
    onErrorContainer = StreamCoreDarkOnErrorContainer,
)

@Composable
fun StreamCoreTVTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val platform = rememberLoginPlatform()

    val targetColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }

        darkTheme && platform != LoginPlatform.Tv -> DarkColorScheme

        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = targetColorScheme.animateColorScheme(),
        typography = Typography,
        content = content,
    )
}

@Composable
private fun ColorScheme.animateColorScheme(): ColorScheme {
    val animationSpec = tween<Color>(
        durationMillis = ThemeColorAnimationDurationMillis,
        easing = FastOutSlowInEasing,
    )

    @Composable
    fun animatedColor(
        targetValue: Color,
        label: String,
    ): Color {
        val color by animateColorAsState(
            targetValue = targetValue,
            animationSpec = animationSpec,
            label = label,
        )
        return color
    }

    return copy(
        primary = animatedColor(primary, "theme-primary"),
        onPrimary = animatedColor(onPrimary, "theme-on-primary"),
        primaryContainer = animatedColor(primaryContainer, "theme-primary-container"),
        onPrimaryContainer = animatedColor(onPrimaryContainer, "theme-on-primary-container"),
        secondary = animatedColor(secondary, "theme-secondary"),
        onSecondary = animatedColor(onSecondary, "theme-on-secondary"),
        secondaryContainer = animatedColor(secondaryContainer, "theme-secondary-container"),
        onSecondaryContainer = animatedColor(onSecondaryContainer, "theme-on-secondary-container"),
        tertiary = animatedColor(tertiary, "theme-tertiary"),
        onTertiary = animatedColor(onTertiary, "theme-on-tertiary"),
        tertiaryContainer = animatedColor(tertiaryContainer, "theme-tertiary-container"),
        onTertiaryContainer = animatedColor(onTertiaryContainer, "theme-on-tertiary-container"),
        background = animatedColor(background, "theme-background"),
        onBackground = animatedColor(onBackground, "theme-on-background"),
        surface = animatedColor(surface, "theme-surface"),
        onSurface = animatedColor(onSurface, "theme-on-surface"),
        surfaceVariant = animatedColor(surfaceVariant, "theme-surface-variant"),
        onSurfaceVariant = animatedColor(onSurfaceVariant, "theme-on-surface-variant"),
        outline = animatedColor(outline, "theme-outline"),
        outlineVariant = animatedColor(outlineVariant, "theme-outline-variant"),
        error = animatedColor(error, "theme-error"),
        onError = animatedColor(onError, "theme-on-error"),
        errorContainer = animatedColor(errorContainer, "theme-error-container"),
        onErrorContainer = animatedColor(onErrorContainer, "theme-on-error-container"),
    )
}

private const val ThemeColorAnimationDurationMillis = 350
