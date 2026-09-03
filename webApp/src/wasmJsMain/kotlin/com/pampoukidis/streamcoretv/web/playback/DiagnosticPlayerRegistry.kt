package com.pampoukidis.streamcoretv.web.playback

internal object DiagnosticPlayerRegistry {
    var activeSessions: Int = 0
        private set
    var activeTimers: Int = 0
        private set
    var closeCount: Int = 0
        private set
    var prepareCount: Int = 0
        private set
    var activationRequired: Boolean = false
        private set

    var pendingScenario: DiagnosticPlayerScenario = DiagnosticPlayerScenario.Success
        private set
    var pendingProfileId: String = DefaultProfileId
        private set

    fun prepareSameDocumentLaunch(
        scenario: DiagnosticPlayerScenario,
        profileId: String,
    ) {
        pendingScenario = scenario
        pendingProfileId = profileId
    }

    fun beginFixture(
        scenario: DiagnosticPlayerScenario,
        profileId: String,
    ) {
        pendingScenario = scenario
        pendingProfileId = profileId
        prepareCount = 0
        activationRequired = scenario == DiagnosticPlayerScenario.AutoplayBlocked
    }

    fun sessionOpened() {
        activeSessions += 1
    }

    fun sessionClosed() {
        activeSessions = (activeSessions - 1).coerceAtLeast(0)
        closeCount += 1
    }

    fun timerStarted() {
        activeTimers += 1
    }

    fun timerStopped() {
        activeTimers = (activeTimers - 1).coerceAtLeast(0)
    }

    fun prepared() {
        prepareCount += 1
    }

    fun activated() {
        activationRequired = false
    }

    const val DefaultProfileId = "diagnostic-profile"
}
