package com.pampoukidis.streamcore.sdk.providers.clientb

/**
 * Opt-in simulated profile journeys. Protected profiles use the demonstration PIN `1234`.
 * These fixtures do not represent a production authentication or PIN-management backend.
 */
enum class ClientBReferenceProfileScenario {
    /** Existing Primary and Family profiles, neither protected. */
    Standard,
    /** One unprotected Primary profile. */
    Single,
    /** One Primary profile protected by the four-digit demonstration PIN. */
    SingleProtected,
    /** A protected Primary profile and an unprotected Kids profile. */
    HouseholdProtected,
}
