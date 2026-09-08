package com.pampoukidis.streamcoretv.core.ui.extensions

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Foreground color for text/icons placed over poster or backdrop artwork.
 *
 * Artwork is not a Material surface, so roles like onSurface/onBackground are not correct here.
 * This token assumes the artwork is protected by a scrim or gradient.
 */
val ColorScheme.onArtwork: Color
    get() {
        return Color.White
    }

/** Fully transparent container role for controls, gradients, and overlays. */
val ColorScheme.transparentContainer: Color
    get() {
        return Color.Transparent
    }

/** Navigation chrome keeps artwork visible; foreground and focus indicators stay opaque. */
val ColorScheme.navigationContainer: Color
    get() {
        return surfaceContainerHigh.copy(alpha = NavigationContainerOpacity)
    }

/** Subtle expanded-drawer veil using the same opacity as navigation chrome. */
val ColorScheme.navigationScrim: Color
    get() {
        return scrim.copy(alpha = NavigationContainerOpacity)
    }

private const val NavigationContainerOpacity = 0.95f

/** Opaque video-player background and overlay base. */
val ColorScheme.playerSurface: Color
    get() {
        return Color.Black
    }

/** Foreground content rendered over [playerSurface]. */
val ColorScheme.onPlayerSurface: Color
    get() {
        return Color.White
    }

/** Placeholder shown while a player filmstrip frame is unavailable. */
val ColorScheme.playerThumbnailPlaceholder: Color
    get() {
        return Color.DarkGray
    }
