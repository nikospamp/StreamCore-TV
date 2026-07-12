package com.pampoukidis.streamcoretv.core.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.pampoukidis.streamcoretv.core.model.general.Platform
import com.pampoukidis.streamcoretv.core.ui.utils.rememberLoginPlatform

/*
 * Material 3 color role guide for StreamCoreTV.
 *
 * App and feature UI should consume MaterialTheme.colorScheme roles, not raw StreamCore* colors.
 * The raw colors in Color.kt are palette values; this file maps them to Material semantics.
 *
 * See also: [com.pampoukidis.streamcoretv.core.ui.extensions.ColorSchemeExtensionsKt]
 *
 * Pairing rule:
 * - Every "on*" role is the foreground color for the matching role.
 *   Example: use onPrimary on primary, onSurface on surface, onError on error.
 *
 * Brand/action roles:
 * - primary: mandatory StreamCore orange. Use for primary actions, focus accents, selected state,
 *   and the strongest brand moments.
 * - primaryContainer: lower-emphasis branded container, such as selected chips or soft action areas.
 * - inversePrimary: primary accent that remains readable on inverseSurface.
 *
 * Support accent roles:
 * - secondary: restrained neutral support role for secondary actions, filters, and controls.
 * - tertiary: green-gray support role for rare categorization or editorial accents.
 * - secondaryContainer / tertiaryContainer: low-emphasis filled support containers.
 *
 * Screen and content roles:
 * - background: app canvas behind scrollable content. Avoid using it for cards.
 * - onBackground: foreground on the app canvas, such as screen-level headings.
 * - surface: default Material surface for cards, sheets, bars, dialogs, and panels.
 * - onSurface: primary text and icons on surfaces.
 * - surfaceVariant: muted filled surfaces, placeholders, and low-emphasis component backgrounds.
 * - onSurfaceVariant: secondary text, metadata, disabled-adjacent labels, and quieter icons.
 *
 * Surface elevation roles:
 * - surfaceDim / surfaceBright: scheme-specific low/high base surfaces used by Material internals.
 * - surfaceContainerLowest -> surfaceContainerHighest: tonal elevation ladder.
 *   Use lower containers for subtle separation and higher containers for more prominent surfaces.
 * - surfaceTint: primary tint used by Material tonal elevation. Do not use as a general accent.
 *
 * Utility roles:
 * - outline: visible component borders and important dividers.
 * - outlineVariant: subtle dividers, separators, and quiet strokes.
 * - inverseSurface / inverseOnSurface: temporary inverse UI, usually snackbars or high-contrast overlays.
 * - scrim: modal/image overlay darkening. There is no Material "onScrim"; artwork text should be
 *   intentionally fixed or handled by a narrow app-specific token.
 *
 * Feedback roles:
 * - error / onError: destructive or invalid states.
 * - errorContainer / onErrorContainer: lower-emphasis error backgrounds and messages.
 *
 * Fixed roles:
 * - primaryFixed, secondaryFixed, tertiaryFixed and their "on*" pairs do not change between light
 *   and dark themes. Prefer normal roles for app chrome; use fixed roles only when a branded accent
 *   must preserve the same color identity across both schemes.
 */
private val LightColorScheme = lightColorScheme(
    // Brand/action roles
    primary = StreamCorePrimary,
    onPrimary = StreamCoreOnPrimary,
    primaryContainer = StreamCorePrimaryContainer,
    onPrimaryContainer = StreamCoreOnPrimaryContainer,
    inversePrimary = StreamCoreInversePrimary,

    // Support accent roles
    secondary = StreamCoreSecondary,
    onSecondary = StreamCoreOnSecondary,
    secondaryContainer = StreamCoreSecondaryContainer,
    onSecondaryContainer = StreamCoreOnSecondaryContainer,
    tertiary = StreamCoreTertiary,
    onTertiary = StreamCoreOnTertiary,
    tertiaryContainer = StreamCoreTertiaryContainer,
    onTertiaryContainer = StreamCoreOnTertiaryContainer,

    // Screen and content roles
    background = StreamCoreBackground,
    onBackground = StreamCoreOnBackground,
    surface = StreamCoreSurface,
    onSurface = StreamCoreOnSurface,
    surfaceVariant = StreamCoreSurfaceVariant,
    onSurfaceVariant = StreamCoreOnSurfaceVariant,
    surfaceTint = StreamCoreSurfaceTint,
    inverseSurface = StreamCoreInverseSurface,
    inverseOnSurface = StreamCoreInverseOnSurface,

    // Feedback roles
    error = StreamCoreError,
    onError = StreamCoreOnError,
    errorContainer = StreamCoreErrorContainer,
    onErrorContainer = StreamCoreOnErrorContainer,

    // Utility and tonal-elevation roles
    outline = StreamCoreOutline,
    outlineVariant = StreamCoreOutlineVariant,
    scrim = StreamCoreScrim,
    surfaceDim = StreamCoreSurfaceDim,
    surfaceBright = StreamCoreSurfaceBright,
    surfaceContainerLowest = StreamCoreSurfaceContainerLowest,
    surfaceContainerLow = StreamCoreSurfaceContainerLow,
    surfaceContainer = StreamCoreSurfaceContainer,
    surfaceContainerHigh = StreamCoreSurfaceContainerHigh,
    surfaceContainerHighest = StreamCoreSurfaceContainerHighest,

    // Fixed accent roles
    primaryFixed = StreamCorePrimaryFixed,
    primaryFixedDim = StreamCorePrimaryFixedDim,
    onPrimaryFixed = StreamCoreOnPrimaryFixed,
    onPrimaryFixedVariant = StreamCoreOnPrimaryFixedVariant,
    secondaryFixed = StreamCoreSecondaryFixed,
    secondaryFixedDim = StreamCoreSecondaryFixedDim,
    onSecondaryFixed = StreamCoreOnSecondaryFixed,
    onSecondaryFixedVariant = StreamCoreOnSecondaryFixedVariant,
    tertiaryFixed = StreamCoreTertiaryFixed,
    tertiaryFixedDim = StreamCoreTertiaryFixedDim,
    onTertiaryFixed = StreamCoreOnTertiaryFixed,
    onTertiaryFixedVariant = StreamCoreOnTertiaryFixedVariant,
)

private val DarkColorScheme = darkColorScheme(
    // Brand/action roles
    primary = StreamCoreDarkPrimary,
    onPrimary = StreamCoreDarkOnPrimary,
    primaryContainer = StreamCoreDarkPrimaryContainer,
    onPrimaryContainer = StreamCoreDarkOnPrimaryContainer,
    inversePrimary = StreamCoreDarkInversePrimary,

    // Support accent roles
    secondary = StreamCoreDarkSecondary,
    onSecondary = StreamCoreDarkOnSecondary,
    secondaryContainer = StreamCoreDarkSecondaryContainer,
    onSecondaryContainer = StreamCoreDarkOnSecondaryContainer,
    tertiary = StreamCoreDarkTertiary,
    onTertiary = StreamCoreDarkOnTertiary,
    tertiaryContainer = StreamCoreDarkTertiaryContainer,
    onTertiaryContainer = StreamCoreDarkOnTertiaryContainer,

    // Screen and content roles
    background = StreamCoreDarkBackground,
    onBackground = StreamCoreDarkOnBackground,
    surface = StreamCoreDarkSurface,
    onSurface = StreamCoreDarkOnSurface,
    surfaceVariant = StreamCoreDarkSurfaceVariant,
    onSurfaceVariant = StreamCoreDarkOnSurfaceVariant,
    surfaceTint = StreamCoreDarkSurfaceTint,
    inverseSurface = StreamCoreDarkInverseSurface,
    inverseOnSurface = StreamCoreDarkInverseOnSurface,

    // Feedback roles
    error = StreamCoreDarkError,
    onError = StreamCoreDarkOnError,
    errorContainer = StreamCoreDarkErrorContainer,
    onErrorContainer = StreamCoreDarkOnErrorContainer,

    // Utility and tonal-elevation roles
    outline = StreamCoreDarkOutline,
    outlineVariant = StreamCoreDarkOutlineVariant,
    scrim = StreamCoreDarkScrim,
    surfaceDim = StreamCoreDarkSurfaceDim,
    surfaceBright = StreamCoreDarkSurfaceBright,
    surfaceContainerLowest = StreamCoreDarkSurfaceContainerLowest,
    surfaceContainerLow = StreamCoreDarkSurfaceContainerLow,
    surfaceContainer = StreamCoreDarkSurfaceContainer,
    surfaceContainerHigh = StreamCoreDarkSurfaceContainerHigh,
    surfaceContainerHighest = StreamCoreDarkSurfaceContainerHighest,

    // Fixed accent roles
    primaryFixed = StreamCoreDarkPrimaryFixed,
    primaryFixedDim = StreamCoreDarkPrimaryFixedDim,
    onPrimaryFixed = StreamCoreDarkOnPrimaryFixed,
    onPrimaryFixedVariant = StreamCoreDarkOnPrimaryFixedVariant,
    secondaryFixed = StreamCoreDarkSecondaryFixed,
    secondaryFixedDim = StreamCoreDarkSecondaryFixedDim,
    onSecondaryFixed = StreamCoreDarkOnSecondaryFixed,
    onSecondaryFixedVariant = StreamCoreDarkOnSecondaryFixedVariant,
    tertiaryFixed = StreamCoreDarkTertiaryFixed,
    tertiaryFixedDim = StreamCoreDarkTertiaryFixedDim,
    onTertiaryFixed = StreamCoreDarkOnTertiaryFixed,
    onTertiaryFixedVariant = StreamCoreDarkOnTertiaryFixedVariant,
)

@Composable
fun StreamCoreTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val platform = rememberLoginPlatform()
    val targetColorScheme = if (platform == Platform.Tv || darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = targetColorScheme.animateColorScheme(),
        typography = Typography,
        shapes = Shapes,
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
        inversePrimary = animatedColor(inversePrimary, "theme-inverse-primary"),
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
        surfaceTint = animatedColor(surfaceTint, "theme-surface-tint"),
        inverseSurface = animatedColor(inverseSurface, "theme-inverse-surface"),
        inverseOnSurface = animatedColor(inverseOnSurface, "theme-inverse-on-surface"),
        error = animatedColor(error, "theme-error"),
        onError = animatedColor(onError, "theme-on-error"),
        errorContainer = animatedColor(errorContainer, "theme-error-container"),
        onErrorContainer = animatedColor(onErrorContainer, "theme-on-error-container"),
        outline = animatedColor(outline, "theme-outline"),
        outlineVariant = animatedColor(outlineVariant, "theme-outline-variant"),
        scrim = animatedColor(scrim, "theme-scrim"),
        surfaceDim = animatedColor(surfaceDim, "theme-surface-dim"),
        surfaceBright = animatedColor(surfaceBright, "theme-surface-bright"),
        surfaceContainerLowest = animatedColor(
            surfaceContainerLowest,
            "theme-surface-container-lowest",
        ),
        surfaceContainerLow = animatedColor(surfaceContainerLow, "theme-surface-container-low"),
        surfaceContainer = animatedColor(surfaceContainer, "theme-surface-container"),
        surfaceContainerHigh = animatedColor(surfaceContainerHigh, "theme-surface-container-high"),
        surfaceContainerHighest = animatedColor(
            surfaceContainerHighest,
            "theme-surface-container-highest",
        ),
        primaryFixed = animatedColor(primaryFixed, "theme-primary-fixed"),
        primaryFixedDim = animatedColor(primaryFixedDim, "theme-primary-fixed-dim"),
        onPrimaryFixed = animatedColor(onPrimaryFixed, "theme-on-primary-fixed"),
        onPrimaryFixedVariant = animatedColor(
            onPrimaryFixedVariant,
            "theme-on-primary-fixed-variant",
        ),
        secondaryFixed = animatedColor(secondaryFixed, "theme-secondary-fixed"),
        secondaryFixedDim = animatedColor(secondaryFixedDim, "theme-secondary-fixed-dim"),
        onSecondaryFixed = animatedColor(onSecondaryFixed, "theme-on-secondary-fixed"),
        onSecondaryFixedVariant = animatedColor(
            onSecondaryFixedVariant,
            "theme-on-secondary-fixed-variant",
        ),
        tertiaryFixed = animatedColor(tertiaryFixed, "theme-tertiary-fixed"),
        tertiaryFixedDim = animatedColor(tertiaryFixedDim, "theme-tertiary-fixed-dim"),
        onTertiaryFixed = animatedColor(onTertiaryFixed, "theme-on-tertiary-fixed"),
        onTertiaryFixedVariant = animatedColor(
            onTertiaryFixedVariant,
            "theme-on-tertiary-fixed-variant",
        ),
    )
}

private const val ThemeColorAnimationDurationMillis = 350
