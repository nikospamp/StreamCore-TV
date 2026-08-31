package com.pampoukidis.streamcoretv.auth

sealed interface AppAuthAction {
    data object RequestLogout : AppAuthAction
    data object DismissLogoutConfirmation : AppAuthAction
    data object ConfirmLogout : AppAuthAction
}
