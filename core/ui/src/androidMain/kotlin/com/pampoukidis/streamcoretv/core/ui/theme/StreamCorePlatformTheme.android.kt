package com.pampoukidis.streamcoretv.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.MaterialTheme as TvMaterialTheme
import androidx.tv.material3.Shapes as TvShapes
import androidx.tv.material3.Typography as TvTypography

@Composable
internal actual fun StreamCorePlatformTheme(content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val shapes = MaterialTheme.shapes
    TvMaterialTheme(
        colorScheme = darkColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            primaryContainer = colors.primaryContainer,
            onPrimaryContainer = colors.onPrimaryContainer,
            secondary = colors.secondary,
            onSecondary = colors.onSecondary,
            secondaryContainer = colors.secondaryContainer,
            onSecondaryContainer = colors.onSecondaryContainer,
            tertiary = colors.tertiary,
            onTertiary = colors.onTertiary,
            tertiaryContainer = colors.tertiaryContainer,
            onTertiaryContainer = colors.onTertiaryContainer,
            background = colors.background,
            onBackground = colors.onBackground,
            surface = colors.surface,
            onSurface = colors.onSurface,
            surfaceVariant = colors.surfaceVariant,
            onSurfaceVariant = colors.onSurfaceVariant,
            error = colors.error,
            onError = colors.onError,
            errorContainer = colors.errorContainer,
            onErrorContainer = colors.onErrorContainer,
            border = colors.outline,
            borderVariant = colors.outlineVariant,
            scrim = colors.scrim,
            inverseSurface = colors.inverseSurface,
            inverseOnSurface = colors.inverseOnSurface,
            inversePrimary = colors.inversePrimary,
            surfaceTint = colors.surfaceTint,
        ),
        typography = TvTypography(
            displayLarge = type.displayLarge, displayMedium = type.displayMedium, displaySmall = type.displaySmall,
            headlineLarge = type.headlineLarge, headlineMedium = type.headlineMedium, headlineSmall = type.headlineSmall,
            titleLarge = type.titleLarge, titleMedium = type.titleMedium, titleSmall = type.titleSmall,
            bodyLarge = type.bodyLarge, bodyMedium = type.bodyMedium, bodySmall = type.bodySmall,
            labelLarge = type.labelLarge, labelMedium = type.labelMedium, labelSmall = type.labelSmall,
        ),
        shapes = TvShapes(
            extraSmall = shapes.extraSmall, small = shapes.small, medium = shapes.medium,
            large = shapes.large, extraLarge = shapes.extraLarge,
        ),
        content = content,
    )
}
