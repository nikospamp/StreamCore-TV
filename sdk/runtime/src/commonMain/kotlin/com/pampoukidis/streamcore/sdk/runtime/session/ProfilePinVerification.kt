package com.pampoukidis.streamcore.sdk.runtime.session

/** Identity of one in-flight PIN verification, used to reject canceled or replaced responses. */
internal class ProfilePinVerification(val pinChallenge: PendingProfilePin)
