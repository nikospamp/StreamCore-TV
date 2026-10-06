package com.pampoukidis.streamcore.sdk.runtime.session

import kotlinx.coroutines.flow.flow

/** One profile choice/PIN flow. A newer choice revokes this attempt, even for the same profile ID. */
internal class ProfileSelectionAttempt(val accountSession: AccountSession, val selectionVersion: Long)
