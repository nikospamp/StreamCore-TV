package com.example.streamcore.consumer

import com.pampoukidis.streamcore.sdk.api.StreamCoreClient
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileEntryChooseProfile
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileEntryPinRequired
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileEntryReady

/** Published-API exercise for opt-in ClientB fixtures. `1234` is only the demonstration PIN. */
suspend fun exerciseReferencePinJourney(client: StreamCoreClient, multipleProfiles: Boolean) {
    try {
        client.auth.restoreSession().pinValueOrThrow()
        client.auth.login("pin-viewer", "reference-password").pinValueOrThrow()
        val entry = client.profiles.beginEntry().pinValueOrThrow()
        val challenge = if (multipleProfiles) {
            val chooser = checkNotNull(entry as? StreamCoreProfileEntryChooseProfile)
            check(chooser.profiles.size > 1)
            val protected = chooser.profiles.single { it.pinPolicy != null }
            val selection = client.profiles.selectProfile(protected.id).pinValueOrThrow()
            checkNotNull(selection as? StreamCoreProfileEntryPinRequired).challenge
        } else {
            checkNotNull(entry as? StreamCoreProfileEntryPinRequired).challenge
        }
        check(challenge.digitCount == 4)
        check(client.context.value.profile == null)
        check(client.home.getCollections(challenge.profile.id) is StreamCoreResult.Failure)
        val wrong = client.profiles.confirmPin(challenge.challengeId, "0000")
        check(wrong is StreamCoreResult.Failure && wrong.error is StreamCoreError.PinRejected)
        check(client.context.value.account != null && client.context.value.profile == null)

        val activated = client.profiles.confirmPin(challenge.challengeId, "1234").pinValueOrThrow()
        check(client.context.value.profile?.id == activated.id)
        check(client.home.getCollections(activated.id).pinValueOrThrow().isNotEmpty())
        if (multipleProfiles) {
            val unprotected = client.profiles.getProfiles().pinValueOrThrow().single { it.pinPolicy == null }
            check(client.home.getCollections(unprotected.id) is StreamCoreResult.Failure)
            check(client.profiles.selectProfile(unprotected.id).pinValueOrThrow() is StreamCoreProfileEntryReady)
            check(client.home.getCollections(unprotected.id).pinValueOrThrow().isNotEmpty())
            check(client.home.getCollections(activated.id) is StreamCoreResult.Failure)
        }
        client.auth.logout().pinValueOrThrow()
        check(client.context.value.account == null && client.context.value.profile == null)
        check(client.profiles.confirmPin(challenge.challengeId, "1234") is StreamCoreResult.Failure)
    } finally {
        client.close()
        client.close()
    }
    check(client.context.value.isClosed)
}

private fun <T> StreamCoreResult<T>.pinValueOrThrow(): T {
    return when (this) {
        is StreamCoreResult.Success -> value
        is StreamCoreResult.Failure -> error("Published PIN journey failed: ${error::class.simpleName}")
    }
}
