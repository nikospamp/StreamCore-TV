package com.pampoukidis.streamcoretv.core.ui.web

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreControlStyle

/** CSS projection of shared semantic roles. Native elements retain their own layout and listeners. */
@Immutable
class StreamCoreWebControlStyle(val roles: StreamCoreControlStyle) {
    fun button(
        background: Color = roles.primary,
        foreground: Color = roles.onPrimary,
        enabled: Boolean = true,
    ): String {
        return "border-radius:${roles.buttonRadius.value}px;${typography(roles.buttonLabel)}" +
            "background:${cssColor(if (enabled) background else roles.disabledContainer)};" +
            "color:${cssColor(if (enabled) foreground else roles.disabledContent)};" +
            "cursor:${if (enabled) "pointer" else "default"};opacity:1;"
    }

    fun input(enabled: Boolean = true): String {
        return "border-radius:${roles.inputRadius.value}px;${typography(roles.inputLabel)}" +
            "opacity:${if (enabled) 1f else roles.disabledInputOpacity};"
    }

    /** Scoped selectors only: never change other forms or the DOM activation model. */
    fun buttonStates(selector: String): String {
        return """
            $selector:disabled{background:${cssColor(roles.disabledContainer)};color:${cssColor(roles.disabledContent)};opacity:1;cursor:default}
            $selector:focus-visible{outline:${StreamCoreWebDimens.FocusBorder.value}px solid ${cssColor(roles.primary)};outline-offset:2px}
            $selector:enabled:hover{background-image:linear-gradient(${cssColor(roles.onPrimary.copy(alpha = 0.08f))},${cssColor(roles.onPrimary.copy(alpha = 0.08f))}) !important;}
            $selector:enabled:active{background:${cssColor(roles.pressed)} !important;color:${cssColor(roles.onPressed)} !important;}
        """.trimIndent()
    }

    private fun typography(style: TextStyle): String {
        return "font-family:system-ui,Segoe UI,Arial,sans-serif;" +
            "font-size:${style.fontSize.value / 16f}rem;" +
            "font-weight:${style.fontWeight?.weight ?: 400};" +
            "line-height:${style.lineHeight.value / 16f}rem;" +
            "letter-spacing:${style.letterSpacing.value / 16f}rem;"
    }

    private fun cssColor(color: Color): String {
        val argb = color.toArgb()
        return "rgba(${(argb shr 16) and 0xff},${(argb shr 8) and 0xff},${argb and 0xff},${color.alpha})"
    }
}
