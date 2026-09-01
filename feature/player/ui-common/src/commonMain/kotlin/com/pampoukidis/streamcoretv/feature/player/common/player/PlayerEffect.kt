package com.pampoukidis.streamcoretv.feature.player.common.player

sealed interface PlayerEffect {
    data object NavigateBack : PlayerEffect
    data object EnterPictureInPicture : PlayerEffect
}