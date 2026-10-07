# SDK verification

Status maintained 2026-10-07 for the backend-agnostic `0.1.0-alpha02` candidate. The dates below identify distinct verification checkpoints. A passing host or tooling run does not refresh older publication, Wasm application, device or live-backend acceptance. Counts describe executions/reported cases in the stated suites, not unique tests across platforms or flavors.

## Latest repository checkpoint — 2026-10-06

| Check | Recorded result | Scope |
| --- | --- | --- |
| `:check`, aggregate `testAndroidHostTest`, both Android app-flavor unit suites | Passed; 426 reported test cases | Repository build/design/convention checks and host/application tests. A stale login-test fake was updated for `AuthService.restoreSession()`; it rejects unexpected restoration during explicit login. |
| SDK Python tooling tests | 49 passed | Source indexing, graph resolution and explorer integrity. |
| Performance Python tooling tests | 10 passed | Analyzer behavior; no new performance measurement. |
| Node review-tool tests, including browser opt-ins | 30 unique tests passed | Review tooling, not the production application's browser regression suite. |
| PowerShell review-tool assertions | 78 passed | Portable setup/capture helper behavior. |
| Local SDK HTML generation and checks | Both pages generated; explorer snapshot check and all four browser checkers passed | Guide, tree, source tree and explorer checks. These open local documentation, not the streaming application. |

This checkpoint did **not** run new SDK publication/independent-consumer checks, Wasm product suites, device journeys, live TMDB authentication or production-browser application acceptance. The later documentation consolidation did not rerun these tests. Detailed logs and generated evidence are local working artifacts, not files promised by a fresh checkout.

## Most recent SDK platform checkpoint — 2026-10-02

These results belong to the candidate after legacy development-data conversion was removed and shared provider helpers moved into test sources. They have not been refreshed by the repository cleanup.

| Area | Recorded result | Boundary |
| --- | --- | --- |
| Runtime | 68 Android host, 68 Wasm Node and 68 browser cases passed | Current-format restart and auth/context storage failure, cancellation and retry. Browser-only cases return early under Node; their Node XML passes are not browser-storage evidence. |
| Providers | TMDB 112 and ClientB 35 cases passed on each of Android host and Wasm Node | Shared public-contract journeys plus provider fixtures. TMDB transport is mocked; ClientB is simulated. |
| Applications | Both Android debug flavors assembled; 25 unit cases per flavor passed; TMDB runtime configuration check passed; 82 application browser cases passed | No new device, live-backend or application visual journey. |
| Published headless consumers | Fresh local Maven staging, source/dependency/metadata gates and coordinate-only Android/Wasm consumers passed, three cases each | Eight supported artifacts; no SDK project dependencies, composite substitution or checkout-source consumption. Not a remote registry release. |
| Optional UI consumers | Android application/test APK builds and the browser resource task completed | Unchanged outputs were reused where reported up-to-date. No device execution in this checkpoint. |
| Artifact inspection | Eight root artifacts and Android/Wasm variants exclude the shared test-helper package; no `sdk-testing` coordinate. Staged Android artifacts exclude the removed legacy configuration/hooks | The helper inspection covered 48 AAR/JAR/KLIB archives in the shared-test checkpoint; the subsequent fresh no-legacy staging checked all eight Android artifacts. |

The recorded staging directories were `build/sdk-shared-tests/maven` and `build/sdk-no-legacy/maven`. They are ignored local evidence, not required checkout contents or reusable evidence for a later candidate. `:sdk:testing` remains a registered base-plugin support project without Kotlin compilation, publication or its own test execution.

Current storage fixtures write/reopen library, history and progress with Unicode identifiers and overlapping account/profile/content IDs. Malformed current JSON remains unchanged while the operation reports failure. Android host restart fixtures use actual binary DataStore with OkioStorage and the PreferencesSerializer codec; that is not an exhaustive device test of Android FileStorage. Separate browser execution provides real `localStorage` restart coverage. Old development data is not imported; retired migration/quarantine tests do not establish a current compatibility promise.

Generated source/API and compiled ABI snapshot gates were removed on 2026-09-29. Current acceptance uses reviewed public declarations and consumer impact, behavioral tests, source/dependency boundaries, staged metadata/resource inspection and independent consumers. Do not reinstate old snapshot commands from archived logs.

## Device, production-browser and live-authentication limits

| Earlier checkpoint | Accepted evidence at that date | Limits still relevant to a new candidate |
| --- | --- | --- |
| 2026-09-28: production browser | 68 viewport journeys covered by passing executions: 66/68 in the broad run plus two passing focused reruns after correcting the resource-namespace assertion | Not one uninterrupted all-green run. The copied production distribution had fresh source/artifact provenance then; later source changes need a new build and verification. |
| 2026-09-28: Android profile/PIN | TV instrumentation 21/21; focused phone and tablet instrumentation 2/2 each; bounded clean reference PIN journeys passed | ClientB PINs are simulated. User TV layout sign-off remained pending; execution is not approval. Review captures had unknown helper provenance, with APK identity established separately by installation/hash records. No production PIN backend or management/recovery coverage. |
| 2026-09-28: published UI resources | One Android resource instrumentation test and one browser resource test passed, loading all 29 avatar XML payloads and shared/provider strings from Maven artifacts | The final consumer rerun built the Android test APK without repeating the device execution. Later October publication runs likewise do not claim a new device run. |
| 2026-09-27: external Android TMDB | Published-coordinate APK passed live identity discovery without an expected-account restriction, catalogue/details/search, saved-state read/write, demo-source resolution, logout, re-login, second logout and idempotent close | In-memory storage: no disk restart, cross-account, concurrent-instance or fault-injection evidence from this journey. Search success did not assert nonempty matches; source resolution did not prove rendered playback. No fresh alpha02 live external run is recorded. |
| 2026-09-27: representative Android/browser playback | Bounded phone/tablet/TV demo playback and desktop-browser search/history/library/playback journeys were inspected | Not exhaustive responsive, accessibility, account-switching, fullscreen or long-playback coverage. Tablet search/library and corrected title were not rechecked; TV logout/re-authentication, profile editing, completion and cross-account switching were not exercised. |
| 2026-09-27: mobile ANR follow-up | An isolated navigation pass did not reproduce the earlier input-dispatch ANR | System contention was a hypothesis, not a proven cause. The follow-up showed a handled sample-playback failure; unrestricted mobile playback acceptance is not claimed. |

The live TMDB runner's private one-use credential file was removed and its absence verified; both created-session logout operations succeeded. Remote revocation was not independently probed. A failed/interrupted run can still prevent cleanup, and `close()` is not logout. The earlier live run completed after explicit destination authorization; it is not an outstanding approval request.

No remote registry publication, JavaScript/TypeScript package, Swift framework, stable 1.0 compatibility promise or minimum browser-version matrix has been established. Demo playback resolves sample media, not catalogue-title media. Local documentation regeneration does not update a separately hosted guide.

## Checklist for the next candidate

1. Record the revision, relevant local changes, tool versions, exact commands and evidence location. Separate executed, reused/up-to-date, failed, skipped and not-run checks; report counts per target. Preserve the date of earlier evidence.
2. Run the applicable [repository host checks](../../../README.md#verify-a-change) and owning SDK/provider tests. Check direct API behavior, cancellation, failed-logout retry, stale session/activation results, profile/PIN enforcement, account isolation and saved-state rules. Shared provider journeys run inside each provider's tests.
3. Execute Wasm Node and actual browser suites for the affected SDK/application surfaces. Require the browser storage tests to execute in a browser. Recheck Android storage on-device when the change depends on Android FileStorage or platform lifecycle behavior.
4. Use a fresh Maven staging directory and the [publication workflow](integration.md#maintainer-publication-and-verification). Verify all eight artifacts, dependency/metadata/resource boundaries and independent Android/Wasm consumers. Execute Android resource instrumentation when device packaging acceptance is required; APK assembly alone is insufficient.
5. For application acceptance, verify both Android flavors, the current production TMDB browser artifact, representative mobile/tablet/TV input and playback, and applicable live-account journeys. Retain the explicit limits above until their own checks are completed. Simulated-provider tests cannot certify a production backend.
6. For documentation tooling changes, [generate both local pages before checking them](README.md#verify-interactive-documentation). Update this matrix with the new candidate's results rather than appending another chronological transcript. Keep logs, captures and generated HTML local.

Use the [integration reference](integration.md) for current contracts and commands, the [provider template](provider-template.md) for required backend journeys, and the [maintainer guide](maintainer-guide.md) for source ownership.
