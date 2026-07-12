package com.pampoukidis.streamcoretv.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val StreamCoreFontFamily = FontFamily.Default

val Typography = Typography(
    displayLarge = textStyle(
        size = 45,
        lineHeight = 52,
    ),
    displayMedium = textStyle(
        size = 36,
        lineHeight = 44,
    ),
    displaySmall = textStyle(
        size = 32,
        lineHeight = 40,
    ),
    headlineLarge = textStyle(
        size = 28,
        lineHeight = 36,
    ),
    headlineMedium = textStyle(
        size = 24,
        lineHeight = 32,
    ),
    headlineSmall = textStyle(
        size = 20,
        lineHeight = 28,
    ),
    titleLarge = textStyle(
        size = 22,
        lineHeight = 28,
        weight = FontWeight.SemiBold,
    ),
    titleMedium = textStyle(
        size = 16,
        lineHeight = 24,
        weight = FontWeight.SemiBold,
    ),
    titleSmall = textStyle(
        size = 14,
        lineHeight = 20,
        weight = FontWeight.SemiBold,
    ),
    bodyLarge = textStyle(
        size = 16,
        lineHeight = 24,
        letterSpacing = 0.5f,
    ),
    bodyMedium = textStyle(
        size = 14,
        lineHeight = 20,
        letterSpacing = 0.25f,
    ),
    bodySmall = textStyle(
        size = 12,
        lineHeight = 16,
        letterSpacing = 0.4f,
    ),
    labelLarge = textStyle(
        size = 14,
        lineHeight = 20,
        weight = FontWeight.Medium,
        letterSpacing = 0.1f,
    ),
    labelMedium = textStyle(
        size = 12,
        lineHeight = 16,
        weight = FontWeight.Medium,
        letterSpacing = 0.5f,
    ),
    labelSmall = textStyle(
        size = 11,
        lineHeight = 16,
        weight = FontWeight.Medium,
        letterSpacing = 0.5f,
    ),
)

object StreamCoreTextStyles {
    val MobileTopTenRank = textStyle(
        size = 118,
        lineHeight = 96,
        weight = FontWeight.ExtraBold,
    )

    val TabletTopTenRank = textStyle(
        size = 92,
        lineHeight = 82,
        weight = FontWeight.ExtraBold,
    )
}

private fun textStyle(
    size: Int,
    lineHeight: Int,
    weight: FontWeight = FontWeight.Normal,
    letterSpacing: Float = 0f,
): TextStyle {
    return TextStyle(
        fontFamily = StreamCoreFontFamily,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = letterSpacing.sp,
    )
}