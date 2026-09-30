# SDK candidate verification

## Bounded runtime readability refactor — 2026-09-30

**Passed within the scope below.** The backend-agnostic SDK's runtime operations now expose captured context, checks, side effects and result reconciliation through expanded blocks and clearer internal names. Public API/SPI declarations, `ContextStateFlow`, storage implementations and persisted formats are unchanged.

| Check | Result / evidence |
| --- | --- |
| Runtime Android host / Wasm Node / Chrome browser | 68 / 67 / 67 cases, confirmed from XML reports. [Runtime run](../../build/sdk-readability/runtime-check.log). |
| Provider Android host / Wasm Node | TMDB 112 per target; ClientB 35 per target. [Provider/application run](../../build/sdk-readability/provider-app-check.log). |
| Android applications | `:app:assembleTmdbDebug` and `:app:assembleClientBDebug` passed, with `:app:verifyTmdbRuntimeConfig -PrequireTmdbRuntimeConfig=true`. |
| Wasm application compilation | `:webApp:compileKotlinWasmJs` passed, reported `UP-TO-DATE` in the provider/application run. |
| SDK source boundaries | Existing boundary check passed. [Boundary run](../../build/sdk-readability/boundary-check.log). |

All listed test executions have zero XML failures, errors and skips; counts are per target.
The Node browser-persistence fixture still returns early without browser storage. The separate Chrome runtime suite supplies actual browser-persistence coverage.

Independent scoped review traced login/logout, protected-profile entry, search/history, a library mutation and a playback checkpoint before and after the refactor and found no behavioral change.
The library adapter remains to preserve activation capture, timestamp ownership and the injectable clock. Recorder thresholds, attempted-periodic-bucket cadence, failed-write behavior and unknown-duration handling remain unchanged.
One focused regression test adds simultaneous library/progress failure precedence and recovery-emission coverage; existing behavioral assertions remain intact.

The pre-existing staged index hash was unchanged. Existing `ContextStateFlow` compilation and Coil/Skiko dependency-compatibility warnings remain outside this scope.
Publication, device, live-backend, visual/UI and application-browser suites were not requested or run for this refactor; application compilation does not establish those forms of acceptance.
API snapshots, the Local/Backend saved-data feature and separate behavioral changes remain deferred. Historical entries below retain their original evidence and scope.

## Provider contracts aligned with services — 2026-09-29

**Passed within the scope below.** The backend-agnostic SDK uses domain-aligned provider ports beneath the consumer services, with a separate content-policy contract and SDK-local library/history/progress. Consumer API/model types and storage formats are unchanged; provider integrations follow the [SPI migration](alpha02-migration.md#provider-contracts-aligned-with-services--2026-09-29).

| Check | Result / evidence |
| --- | --- |
| Runtime Android host / Wasm Node / browser | 67 / 66 / 66 cases, confirmed from current XML reports. [Runtime run](../../build/sdk-provider-domains/runtime-check.log). |
| Provider Android host / Wasm Node | TMDB 112 per target; ClientB 35 per target. [Provider/application run](../../build/sdk-provider-domains/provider-app-check.log). |
| Android applications | 25 unit cases per flavor; both TMDB/ClientB debug APKs assembled; TMDB runtime configuration passed in the same run. |
| Browser application compilation | `:webApp:compileKotlinWasmJs` passed; browser application tests and production webpack were not rerun. |
| Source and publication | All nine artifacts staged into `build/sdk-provider-domains/maven`; source/dependency/metadata/resource gates passed. Separate coordinate-only Android and headless Wasm consumers passed 3 cases each. Optional UI browser verification and Android application/test APK builds passed, reusing unchanged outputs where reported up-to-date. [Publication run](../../build/sdk-provider-domains/publication-check.log). |

All listed executed suites have zero failures, errors and skips; counts are per target. The new `separateDomainProvidersApplySharedPolicyToEveryContentPath` regression ran on all three runtime targets. It covers home, recommendations, search, trending, direct details, playback sources, library and progress; it also checks that filtering retains saved data and playback uses the authoritative content snapshot.

Independent review confirmed all nine existing policy call sites, all twenty stale-context checks, cancellation/error handling, and resource ownership remain intact. The public SDK API sources match their pre-change versions. The staged runtime Android AAR contains all seven domain/policy contracts and none of the three retired provider contracts.

No modules or API/ABI snapshots were added, and the pre-existing staged index was preserved. This alignment did not rerun visual/device/live-backend journeys; Android resource test APK builds do not establish device execution. Runtime browser tests above do not establish browser application or production-bundle acceptance. The Node browser-storage fixture still exits early without browser storage; the separate runtime browser suite supplies that coverage. Historical entries retain their original evidence and scope.

## Runtime packages by responsibility — 2026-09-29

**Passed within the scope below.** The backend-agnostic SDK now groups provider contracts in `runtime.integration.provider` and persistence in `runtime.storage`, with small store contracts beside their implementations and one internal legacy migration helper. See the [package migration](alpha02-migration.md#runtime-packages-by-responsibility--2026-09-29).

| Check | Result / evidence |
| --- | --- |
| Runtime Android host / Wasm Node / browser | 66 / 65 / 65 cases, confirmed from current XML reports. [Runtime/application run](../../build/sdk-runtime-packages/runtime-app-check.log). |
| Provider Android host / Wasm Node | TMDB 112 per target; ClientB 35 per target, confirmed from current XML reports and the same run. |
| Application compilation | Both TMDB/ClientB debug APKs assembled; `verifyTmdbRuntimeConfig` and `:webApp:compileKotlinWasmJs` passed. |
| Source review and boundaries | Review compared 36 original source files across 15 moves: changes are package/import updates and exact migration-body extraction. Source boundaries passed, including Android/browser application and flavor sources. |
| Independent artifact consumption | All nine artifacts staged into `build/sdk-runtime-packages/maven`; source/dependency/metadata/resource gates passed. Separate Android and headless Wasm consumers passed 3 cases each. Optional UI browser verification and Android application/test APK builds passed, reusing unchanged outputs where reported up-to-date. The runtime Android AAR contains the reorganized declarations and migration helper, with previous class paths absent. [Publication run](../../build/sdk-runtime-packages/publication-check.log). |

All listed executed suites have zero failures, errors and skips; counts are per target. Existing migration, cancellation and storage coverage ran without adding tests for unchanged behavior. No API/ABI snapshots were introduced, and the pre-existing staged index was preserved.

This package-only change did not rerun browser application tests, production webpack, device/live-backend journeys or visual acceptance. Android resource test APK verification does not establish device execution. Runtime browser tests above are separate from browser application acceptance; the Node browser-storage fixture still exits early without browser storage, with the separate browser suite supplying that coverage. Earlier verification entries retain their original artifacts and scope.

## SDK model prefix — 2026-09-29

**Passed within the scope below.** All 51 top-level types in `:sdk:model` now use `StreamCore`: 48 were renamed and three already had the prefix. The target architecture remains backend-agnostic. See the [migration guide](alpha02-migration.md#sdk-model-prefix--2026-09-29) for the intentional Kotlin source/ABI change.

| Check | Result / evidence |
| --- | --- |
| SDK model Android host / Wasm Node | 9 cases per target, including two new tests using fixed legacy JSON, in the [application run](../../build/sdk-model-prefix/application-check.log). |
| Runtime Android host / Wasm Node / browser | 66 / 65 / 65 cases. Host/Node in the [SDK run](../../build/sdk-model-prefix/sdk-check.log); browser in the application run. |
| Provider Android host / Wasm Node | TMDB 112 per target; ClientB 35 per target, in the SDK run. |
| SDK/application UI and feature host tests | SDK UI 8 in the SDK run; core UI 7, Login 6, Profiles 17, Home 15, Details 14, Library 3, Search 12 and Player 26 in the application run. |
| Android applications | 25 unit cases per flavor; both TMDB/ClientB debug APKs assembled; TMDB runtime configuration gate passed. TMDB AndroidTest APK built only. |
| Browser application | 82 browser cases, in the application run. |
| Source and published model surface | Source boundaries/naming passed. Review compared 353 source files with no unintended behavioral delta. The model Android AAR staged in `build/sdk-model-prefix/maven` contains all 48 new classes and none of the 48 retired classes. |
| Independent artifact consumption | All nine artifacts staged into `build/sdk-model-prefix/maven`; source/dependency/metadata/resource gates passed. Separate coordinate-only Android and headless Wasm consumers passed 3 cases each. The optional UI browser resource test passed, and Android UI application/test APKs built. [Publication run](../../build/sdk-model-prefix/publication-check.log). |

All completed suites above have zero failures, errors and skips; counts are executions per target, not unique cases summed across platforms. Legacy JSON field names/defaults and persisted formats are unchanged. Serializer descriptor names naturally reflect the renamed Kotlin classes; persisted models do not use class discriminators. No API/ABI snapshots were restored, and the pre-existing staged index was preserved.

This source naming change did not rerun production webpack, device or live TMDB journeys, or visual acceptance. The AndroidTest APK builds do not establish device execution. The runtime Node browser-storage fixture still exits early without browser storage; the separate browser suite supplies that coverage. Earlier records below retain their original artifact names, evidence and acceptance scope.

## Playback/search domain services — 2026-09-29

**Passed within the scope below.** The backend-agnostic SDK now exposes `client.playback: PlaybackService` and query/history operations together on `client.search: SearchService`. The old source/progress/history consumer interfaces are removed. `createProgressRecorder(...).reportEvent(...)` handles player events; `updateProgress(entry)` applies an explicit snapshot. See the [migration mapping](alpha02-migration.md#playback-and-search-domain-grouping--2026-09-29) and [event behavior guide](playback-progress.md).

| Check | Result / evidence |
| --- | --- |
| Runtime Android host / Wasm Node / browser | 66 / 65 / 65 cases, zero failures/errors. [Host/Node run](../../build/sdk-domain-facades/runtime-check.log), [browser run](../../build/sdk-domain-facades/web-check.log). |
| TMDB and ClientB contracts on Android host and Node | TMDB 112 per target; ClientB 35 per target; zero failures/errors. [Provider run](../../build/sdk-domain-facades/provider-check.log). |
| Migrated feature ViewModels | Player 26, Search 12, Home 15 and Details 14 host cases passed. [Android run](../../build/sdk-domain-facades/android-check.log). |
| Android applications | 25 unit cases per flavor passed, both TMDB/ClientB debug APKs assembled, TMDB runtime configuration verified in the same Android run. |
| Browser application | All 82 browser cases passed; production Wasm webpack bundle compiled in the browser run. |
| Independent artifact consumption | All nine artifacts staged into `build/sdk-domain-facades/maven`; dependency/source-boundary/metadata/resource gates and separate Android/Wasm consumers passed. [Publication run](../../build/sdk-domain-facades/publication-check.log). |

The three new runtime regressions cover unified facade routing, recorder-versus-direct unknown-duration handling, authorized history management, and periodic failure/checkpoint cadence. Existing authorization, stale activation, cancellation, completion and player-exit assertions were retained. Both coordinate-only sample journeys now exercise direct updates, recorder checkpoints, saved-position readback and completion removal. The published Android API AAR was inspected: the grouped services are present and all three retired service classes are absent. Independent code review found no behavioral regression.

Counts are per execution target, not a sum of unique cases. The Node browser-storage fixture's early exit is still not browser persistence evidence; the separate runtime browser suite provides that coverage. Unchanged publication tasks, including optional UI resource tests, reused outputs where reported up-to-date. The Android resource test APK was built without repeating device execution. This change did not include a new device/visual journey, Playwright acceptance run, live TMDB account run, or copied/certified browser distribution; production webpack compilation is recorded separately from those activities. Persisted model fields, store formats and provider production protocols were not changed. API snapshot checks remain removed, and the pre-existing staged index was preserved.

## Snapshot check removal — 2026-09-29

**Gate change — 2026-09-29:** SDK compiled Android/Kotlin-Wasm ABI snapshot checks and conservative source API baseline checks were removed. The dated results below describe checks executed before that removal; their logs remain historical evidence, not current verification commands or requirements. Behavioral tests, public contract review, module/dependency boundaries and publication/consumer validation remain in the current workflow.

Removal verification passed: [model check](../../build/sdk-snapshot-removal/model-check.log) and the [updated publication workflow](../../build/sdk-snapshot-removal/publication-check.log). The latter staged all nine artifacts into a fresh `build/sdk-snapshot-removal/maven` repository, passed source/dependency/metadata/resource checks, and verified the independent Android and Kotlin/Wasm consumers. Unchanged tasks reused their existing outputs where Gradle reported them up-to-date. The Android UI resource test APK was built; device execution was not repeated. No production SDK behavior changed.

## Alpha02 refinement verification — 2026-09-28

**Status at this checkpoint:** the alpha02 SDK, application, browser and independent-publication checks described below passed. The expanded 21-case TV instrumentation run passed, as did the focused phone and tablet PIN device tests (2/2 each) and the clean tablet manual journey. **The user's TV layout sign-off is pending; no approval is claimed.** Earlier dated records remain below and keep their original scope.

The target architecture is backend-agnostic. This candidate intentionally changes public Kotlin namespaces/services and adds runtime-enforced profile entry/PIN authorization while preserving stored identities/formats. Evidence is local under `build/sdk-refinement/`, module test reports and `build/review/sdk-alpha02/`; counts are executions per target, not unique tests summed across platforms.

### Automated and published-consumer evidence

Counts below were read from the current XML reports; each listed completed suite has zero failures, errors and XML skips.

| Execution target | Cases | Evidence / scope |
| --- | ---: | --- |
| SDK model Android host | 7 | Current `sdk/model/build/test-results/testAndroidHostTest` reports |
| SDK runtime Android host | 63 | [Readiness/runtime/browser run](../../build/sdk-refinement/readiness-web-check.log) |
| SDK runtime Kotlin/Wasm Node / browser | 62 / 62 | Same passing run; Node persistence caveat below |
| TMDB provider Android host / Node | 112 / 112 | Current provider XML reports; corrected contracts followed by final gates |
| ClientB provider Android host / Node | 35 / 35 | Includes public single/protected/household PIN journeys |
| Shared SDK UI Android host | 8 | Current SDK UI XML reports |
| Profiles shared Android host / profile UI browser | 17 / 2 | [Final Android build](../../build/sdk-refinement/final-android-build.log) and current browser reports |
| TV profile instrumentation | 21 | [Final TV device run](../../build/sdk-refinement/tv-final-device-tests.log), including fast PIN input |
| Focused phone PIN instrumentation | 2 | [Phone device run](../../build/sdk-refinement/mobile-pin-device-tests.log); zero XML failures/errors/skips |
| Focused tablet PIN instrumentation | 2 | [Tablet device run](../../build/sdk-refinement/tablet-final-device-tests.log); zero XML failures/errors/skips; both clean application flavors also assembled |
| Android application unit tests, TMDB / ClientB | 25 / 25 | Current per-flavor reports; both flavors assembled again in the final Android build |
| Kotlin/Wasm application browser | 82 | Readiness/browser run, including context/navigation and PIN history coverage |
| Independent published Android unit / Wasm Node consumer | 3 / 3 | [Final consumer run](../../build/sdk-refinement/consumer-final.log), including published PIN flows |
| Independent optional-UI browser / Android resource device test | 1 / 1 | Browser XML and [publication run](../../build/sdk-refinement/publication-check.log); Android resource test explicitly ran on the phone |

`BrowserPreferencesRestartTest` emits an explicit early-exit marker on Node because no browser `localStorage` exists, despite its XML case being reported as passed. That Node case is not persistence evidence; the separate 62-case browser execution supplies the browser context. The final consumer run refreshed the same isolated repository and passed; it built the Android UI test APK without repeating device execution. The one executed phone resource test belongs to the earlier passing publication run, not a claimed final rerun.

[ABI/application checks](../../build/sdk-refinement/abi-app-check.log) passed. All nine artifacts have reviewed Android and Kotlin/Wasm ABI dumps plus the six source API baseline scopes. Review found the expected service/model/PIN/activation changes and no raw provider/transport/storage symbols exposed through consumer/provider/UI surfaces; the distinct runtime SPI remains intentionally public to provider integrations.

The nine artifacts were staged into the isolated `build/sdk-refinement/maven-alpha02` repository. [Final staging](../../build/sdk-refinement/publication-final.log) passed publication and compiled-ABI/dependency gates, then correctly stopped at the conservative source fingerprint for a readiness lambda. The reviewed delta was a lambda-parameter/source-fingerprint change, not an API signature change. Its baseline was updated; the subsequent **consumer-final** run passed source/API/metadata checks and the independent consumers against the refreshed same repository. The stopped staging script is not presented as an end-to-end pass. Coordinate-only consumers use no SDK project dependencies, source imports or composite substitution. Optional-UI consumers retain their separate resource-only dependencies.

Earlier partial logs (`integration-check`, `contracts-check`, and the initial UI/runtime checks) include compilation/test failures subsequently fixed and superseded by the passing runs above. The JVM throwable-identity assertion failure was superseded by passing runtime reruns. The [integration run](../../build/sdk-refinement/integration-check.log) did execute 20 TV profile instrumentation cases successfully even though other tasks in that invocation failed. The later fast-input test compiled in the final Android build, and the subsequent final TV run passed all 21 cases with zero failures/errors/skips in its device XML.

### Production browser acceptance

[Fresh production provenance](../../build/sdk-refinement/verified-web-build.log) records the helper-built/copied, fresh-certified distribution at `webApp/build/dist/wasmJs/productionExecutable`, with source and artifact fingerprints. The readiness run compiled the production webpack bundle; it did not itself establish the copied distribution. Root rechecked that distribution as fresh after temporary diagnostic logging was removed.

On that current bundle, the [full Playwright run](../../build/sdk-refinement/playwright-acceptance-final.log) passed **66 of 68** viewport journeys. The two remaining failures were the same resource-evidence assertion using the pre-migration resource namespace. The test path was corrected to the new namespace, preserving the resource-byte assertion; the [focused rerun](../../build/sdk-refinement/playwright-reload-final.log) then passed both 1280- and 1920-pixel journeys. All **68 journeys are therefore covered by passing executions across these runs**, not by one uninterrupted all-green run. The earlier aborted `playwright-acceptance.log` is excluded from accepted evidence.

### Android review artifact and pending acceptance

The frozen clean reference APK is [streamcore-clientb-pin-review.apk](../../build/review/sdk-alpha02/streamcore-clientb-pin-review.apk), configured with `HouseholdProtected` and demonstration PIN `1234`. Its SHA256 was read directly:

`60C52509456F473353C3E690538458410C688A51AD9C8163AD06AF147E72890D`

Root-owned installation records identify that APK on TV, phone and tablet. Screenshot helper metadata reports `installedArtifactProvenance: unknown` and `semanticScreenVerified: false`; those captures are not helper-certified build/semantic evidence. Artifact identity comes from the explicit install/hash record, and visual/interaction conclusions come from manual inspection. Temporary PIN-input debug logging was removed from the production source; diagnostic-build logs are not the clean artifact's acceptance record.

| Manual / device check | Status at this checkpoint |
| --- | --- |
| TV reference PIN journey | Passed in the bounded manual review; [PIN](../../build/review/sdk-alpha02/tv-final-pin.png), [rejection](../../build/review/sdk-alpha02/tv-final-pin-error.png), [home](../../build/review/sdk-alpha02/tv-final-home-check.png), [logout](../../build/review/sdk-alpha02/tv-final-logout-check.png) |
| Phone reference PIN journey | Passed in the bounded manual review; [masked input](../../build/review/sdk-alpha02/mobile-pin-masked.png), [rejection](../../build/review/sdk-alpha02/mobile-pin-rejected.png), [post-confirmation](../../build/review/sdk-alpha02/mobile-after-correct-pin-settled.png), [re-entry](../../build/review/sdk-alpha02/mobile-pin-reentry.png) |
| Tablet clean retouch journey | Passed on the clean APK using the settled input position: numeric IME, wrong-PIN clearing/error, Back to chooser and correct-PIN entry into Home. Inspected [focused PIN](../../build/review/sdk-alpha02/tablet-clean-final-pin-focused-settled.png), [rejection](../../build/review/sdk-alpha02/tablet-clean-final-pin-error.png) and [Home](../../build/review/sdk-alpha02/tablet-clean-final-home.png). The earlier apparent cancellation was a stale automation coordinate: the IME moved Back under the old field position, and a sanitized stack confirmed its click handler. No product change was needed. |
| Expanded TV instrumentation, including fast PIN input | Passed: 21/21 in the final device run, with zero XML failures/errors/skips. |
| Focused phone PIN instrumentation | Passed: 2/2 in the phone device run, with zero XML failures/errors/skips. |
| Focused tablet PIN instrumentation | Passed: 2/2 in the tablet device run, with zero XML failures/errors/skips. |
| User TV layout approval | Pending asynchronous sign-off; manual execution does not substitute for user approval. |

The [TV review board](../../build/review/sdk-alpha02/tv-review-board.png) composes the original captures and was visually inspected by root. It does not expand their stated journey/provenance scope or imply user layout approval. The pre-task staged-index snapshot remains unchanged.

This evidence covers Android and Kotlin/Wasm only. ClientB PIN verification is simulated reference behavior; it does not establish a production PIN backend or management/recovery flow. No remote registry publication, JavaScript/TypeScript package, Swift framework or stable 1.0 compatibility promise is claimed. Historical live TMDB and older device evidence below retain their recorded date and limitations; this section does not claim a fresh live external TMDB run.

## Earlier alpha01 verification checkpoint

Status recorded 2026-09-27 for `0.1.0-alpha01`. The target architecture is backend-agnostic. **Candidate verification is complete within the scope recorded below:** the previously pending live external Android TMDB journey passed after explicit destination approval. The final 68-case production Playwright run, Kotlin browser suites and bounded visual/interaction reviews also passed. The mobile and persistence-review limitations below remain part of this record.

Evidence paths are relative to the repository root. Build logs, test reports and original screenshots are local ignored artifacts; the documentation and compatibility baselines are tracked.

**Structural-migration provenance (2026-09-28):** the provider modules recorded by the 2026-09-27 checks as `:client:tmdb:data` and `:client:clientB:data` moved to `:sdk:providers:tmdb` and `:sdk:providers:clientB`. At that checkpoint, `client/*/ui` still held application branding. The later optional SDK presentation extraction is recorded separately below. Existing logs, counts and screenshots retain their original provenance. Use the current paths from [integration.md](integration.md#artifacts-and-repository-structure) for subsequent verification.

## Optional SDK presentation verification — 2026-09-28

**Passed.** The optional `:sdk:ui`, `:sdk:providers:tmdb:ui` and `:sdk:providers:clientB:ui` artifacts now own the shared presentation contracts/default error mapping, thirteen common error strings, twenty TMDB avatars, nine ClientB avatars and TMDB's three authentication strings. Artwork, avatar IDs, wording and existing Kotlin type names are preserved. The four extracted common types have one definition each. Provider Koin bindings moved to the Android/Web composition roots; dialogs, navigation, themes, composition locals and player rendering remain application-owned.

[sdk-ui-app-integration-final.log](../../build/sdk-ui-app-integration-final.log) records both Android application assemblies, 25 application unit tests per flavor, seven core-UI host tests, fourteen SDK presentation host tests, all 75 application browser tests, development browser distribution, unchanged new-UI ABI checks and required architecture/KMP gates passing. Four pure error-mapper tests previously executed in each app flavor now live in the shared SDK UI suite, expanded to seven cases; the reduced per-flavor count is deliberate test ownership consolidation.

[sdk-ui-publication-final.log](../../build/sdk-ui-publication-final.log) records all nine aligned artifacts published to the isolated local Maven repository and the independent consumers passing. The six headless artifacts retain their original strict dependency bans. The three optional UI artifacts have separate source/resolved/published-metadata checks; they export model/resource/Compose-runtime dependencies and exclude application code, provider transport/runtime, Koin, Material/TV, Coil and player engines. Source/API and compiled Android/Wasm compatibility baselines include the new presentation surfaces. Archive checks verify actual provider/shared resource payloads in their AAR namespaces.

The separate `uiAndroidApp` and `uiWasm` consumers declare only the two provider-UI coordinates as production dependencies. Both load the actual XML bytes for every one of the 29 avatars and resolve real shared/provider error strings. Android instrumentation executed on `emulator-5554`, with one test and zero failures/errors/skips; its final result is in the publication log. The browser resource test also passed; [sdk-ui-wasm-resource-check.log](../../build/sdk-ui-wasm-resource-check.log) records its explicit execution. Existing headless consumer modules remain separate and received no UI dependencies. No credentials or backend sessions were used by these resource tests.

The checks exposed and corrected three integration details: same-name `ui` projects needed unique internal Gradle identities while retaining explicit public Maven coordinates; Compose runtime needed public compile scope for an independent Compose consumer; and Karma needed to serve the consumer's processed Maven resources at `/composeResources/`. No checkout resources were copied into the consumer. A shared `streamcore.kmp.resources.library` convention supplies the established Compose/Android compiler flags without preview tooling; the compiler-flags gate was preserved, not relaxed. Earlier failed attempts in `sdk-ui-publication-check.log` and `sdk-ui-app-integration.log` are superseded by the final passing runs.

[sdk-ui-browser-journeys.log](../../build/sdk-ui-browser-journeys.log) records eight focused development-distribution journeys passing at 1280×720 and 1920×1080: login/profile selection, provider authentication error text, generic account-verification failure, and profile create/edit/delete with avatar keyboard selection and focus restoration. All four originals were opened and inspected: [1280 error](../../build/review/sdk-ui-error-1280.png), [1920 error](../../build/review/sdk-ui-error-1920.png), [1280 avatars](../../build/review/sdk-ui-avatars-1280.png), [1920 avatars](../../build/review/sdk-ui-avatars-1920.png). Artwork and selection indicators rendered, and the authentication message/OK label were readable. [The review board](../../build/review/sdk-ui-board.png) composes those originals without cropping/redrawing and was inspected as well.

This pass verifies resource publication/loading on Android and browser rendering in the existing application. It does not claim a fresh production-optimized browser run, new mobile/tablet/TV visual journeys, or Swift/TypeScript presentation support. Earlier broad application and live-authentication evidence below keeps its original date. The existing staged Git snapshot was preserved throughout the extraction.

## Provider relocation verification — 2026-09-28

The structural move passed a separate verification run. SHA256 comparison against the pre-move manifest confirmed that all 115 provider source, Android manifest and compiled-ABI reference files retained identical contents. Within the relocated providers, only the two module build files changed; the Wasm ABI reference filenames now match `tmdb` and `clientB`. Public-source API snapshot changes are file paths only. The original Android namespaces and JVM `data`/`data_hostTest` module identities are explicitly preserved, and the existing artifact-based Wasm identities are unchanged.

[sdk-provider-relocation-check.log](../../build/sdk-provider-relocation-check.log) records a successful build with:

* Both providers' Android host tests: 140 cases, no failures.
* Both providers' Wasm Node tests: 140 cases, no failures.
* Both providers' compiled Android and Wasm ABI checks against the unchanged baselines.
* TMDB and ClientB Android application assemblies and 29 unit tests per flavor, all passing.
* Application Kotlin/Wasm compilation, required TMDB configuration validation, and design-token/KMP test-target/compiler-flag/dependency/convention gates.

[sdk-provider-relocation-publication.log](../../build/sdk-provider-relocation-publication.log) records republication of all six artifacts to the isolated local Maven repository, successful compiled-ABI/headless-closure/source-boundary/publication-metadata checks, and a successful separate published-coordinate consumer build. The Wasm consumer test executed; the Android APK and unit-test tasks were up to date after dependency refresh. Neither consumer required changed Maven coordinates or source imports. A separate review found no remaining active references to the old Gradle module paths, and the pre-existing staged Git snapshot was preserved.

This pass changes source/build organization only. Device journeys, live authentication and visual reviews were not repeated; their earlier evidence remains dated below. Path-sensitive review certificates correctly become stale after relocation and require recertification before a new certified device/browser capture.

## Automated checks completed

The XML reports for the active modules at the recorded 2026-09-27 verification checkpoint contain the following results. Removed module directories and earlier attempts were excluded. Counts are executions on each target, not a summed count of unique tests across platforms or application flavors; they are not automatically refreshed by the later directory relocation.

| Execution target | Cases reported | Result |
| --- | ---: | --- |
| Android host tests, active shared/provider/UI modules | 333 | 0 failures, errors or XML skips |
| Android application unit tests, TMDB flavor | 29 | 0 failures, errors or skips |
| Android application unit tests, ClientB flavor | 29 | 0 failures, errors or skips |
| Headless Kotlin/Wasm Node tests: runtime 52, TMDB 111, ClientB 29 | 192 | 0 failures or errors; see browser-only caveat |
| SDK runtime Kotlin/Wasm ChromeHeadless tests | 52 | 0 failures, errors or skips |
| Kotlin/Wasm application ChromeHeadless tests | 75 | 0 failures, errors or skips |
| Kotlin/Wasm profile-picker ChromeHeadless regression | 1 | Passed: directional focus, edge trap, selection and Escape |
| Kotlin/Wasm player UI ChromeHeadless tests | 18 | 0 failures, errors or skips |
| Kotlin/Wasm playback-engine ChromeHeadless tests | 30 | 0 failures, errors or skips |
| Production Playwright, 34 scenarios at each of two viewports | 68 | All passed in one final run |
| Separate published-coordinate Android consumer unit test | 1 | Passed |
| Separate published-coordinate Kotlin/Wasm Node consumer test | 1 | Passed |

The 192 Node cases include `BrowserPreferencesRestartTest`, which deliberately returns early without browser `localStorage` and prints `SKIPPED` although XML records a pass. That execution is **not** browser-persistence evidence. Its ChromeHeadless execution completed without that early exit. The runtime Android host suite has 53 cases, including two real binary DataStore reopen tests; the browser suite contains one browser storage reopen case plus common runtime tests.

The final [consolidated regression run](../../build/sdk-final-regression.log) passed all active Android host tests, both application-flavor unit suites, the SDK browser suite, and design-token/KMP test-target/compiler-flag/dependency/convention checks. The initial [integration attempt](../../build/sdk-integration-check.log) had a stale `SearchViewModelTest` method-name compilation error; its corrected owning suite and the later consolidated run supersede that failed attempt. Application/player Kotlin browser repairs passed in [sdk-browser-repair-check.log](../../build/sdk-browser-repair-check.log); the playback-engine suite passed during [sdk-browser-tests.log](../../build/sdk-browser-tests.log), whose other initial failures were subsequently repaired.

Both `:app:assembleTmdbDebug` and `:app:assembleClientBDebug`, authenticated TMDB configuration verification, and the 26-test player suite passed after the demo-title fix in [sdk-demo-label-check.log](../../build/sdk-demo-label-check.log). Updated provider/runtime Node suites passed after the KLIB identity correction in [sdk-klib-identity-check.log](../../build/sdk-klib-identity-check.log). The final APKs are under `app/build/outputs/apk/{tmdb,clientB}/debug/`.

After startup-quota and native-focus repairs, both Android flavors and the production browser distribution rebuilt successfully in [sdk-final-production.log](../../build/sdk-final-production.log). Both flavors' Android instrumentation sources compiled successfully in the browser-check run; this is compilation evidence, not a claim that instrumentation tests ran on devices.

Browser acceptance then exposed missing avatar-picker arrow routing: Compose/Wasm's default focus handler does not handle arrow keys. The overlay now explicitly moves focus for the four directions while retaining its existing modal trap. A stronger physical-keyboard test also verifies that Space selects a different avatar, avoiding a false pass caused by unrelated screenshot changes. The focused picker regression and all 75 application browser tests passed, and the production distribution rebuilt in [sdk-avatar-production-check.log](../../build/sdk-avatar-production-check.log). [sdk-certified-acceptance.log](../../build/sdk-certified-acceptance.log) records the latest fresh Android/web fingerprints; [sdk-final-gates.log](../../build/sdk-final-gates.log) records the final architecture gates. The final production acceptance run passed all **68 cases in 8.7 minutes**, covering both 1280- and 1920-pixel viewports together in [sdk-playwright-acceptance.log](../../build/sdk-playwright-acceptance.log). It supersedes the earlier 67/68 run and failed timing-only repair.

## Publication, compatibility and independent consumption

[sdk-publication-check.log](../../build/sdk-publication-check.log) records successful staging and a separate consumer build/test run; [sdk-publication-final.log](../../build/sdk-publication-final.log) repeats the gates successfully for the updated candidate. All six aligned artifacts were staged under `build/sdk-repository`: model, API, runtime, testing and both providers. The standalone build under `samples/sdk-consumer` used Maven coordinates, with no SDK project dependencies, included builds, checkout source imports or substitution. Its Android APK assembled, its Android unit test passed, and its headless Kotlin/Wasm consumer executed successfully.

The completed gates cover:

* Consumer/model/provider/integration **source API snapshots and boundaries**, including prohibition of feature imports from provider integration and public raw provider/storage implementations.
* **Compiled Android bytecode ABI and Kotlin/Wasm KLIB ABI** for all six artifacts, checked against reviewed module-local baselines. Source fingerprints supplement these checks; they do not substitute for compiled ABI verification.
* Resolved Android and Wasm compile/runtime dependency closure, rejecting Compose, Skiko, Koin, TV Material and player/rendering engines in published SDK artifacts.
* Staged publication coordinates, internal dependency closure and metadata scopes, plus unique artifact-specific KLIB identities. The discovered publication-reference/identity defect was corrected with explicit unique Maven artifact coordinates and distinct IR module identities for the two provider modules named `data`. Metadata-owner/variant-reference and KLIB-collision gates now reject recurrence; duplicate-name validation remains enabled.

The standalone Android ClientB application also ran its public SDK journey on a device, beyond compilation/unit tests. After the final review corrected stale live-input removal and guaranteed resource closure when logout cleanup fails, the independent build and tests passed again in [sdk-consumer-cleanup-check.log](../../build/sdk-consumer-cleanup-check.log). The rebuilt device journey passed in [sdk-consumer-device-final.log](../../build/sdk-consumer-device-final.log), and root inspected [its final pass screen](../../build/review/sdk-external-clientb-final.png). This proves simulated-provider device consumption, not live TMDB authentication or persistence restart behavior; the sample uses in-memory storage.

The same published-coordinate APK then passed the **live TMDB journey on emulator-5554**, without an expected-account restriction. [sdk-consumer-tmdb-live.log](../../build/sdk-consumer-tmdb-live.log) records the successful runner exit, and root opened and inspected [the live pass screen](../../build/review/sdk-external-tmdb-live.png). This verifies explicit bootstrap, live login and nonblank discovered account identity, reference subprofiles, nonempty home catalogue, successful details/search operations, normalized history, library and progress write/readback, demo-source resolution, successful logout with cleared account context, re-login with the same identity, a second successful logout, and two calls to close. Search success does not assert nonempty matches; source resolution does not assert rendered playback. This live run uses in-memory storage and adds no disk restart/migration, cross-account, concurrent-instance or fault-injection evidence; those are covered separately above and below.

## Behavioral and persistence coverage

Both providers run the same `sdk-testing` `ProviderContract`: public bootstrap/login, stable account identity, provider profiles, catalogue/search/details, local history/library/progress, explicit demo-media capability, unsupported playback without opt-in, logout and close. TMDB uses deterministic mocked transport and verifies discovery without an expected-account restriction. ClientB remains simulated; these tests establish structural substitutability, not production-backend coverage.

Direct SDK and runtime tests cover login/profile validation and normalization; search submission/recent/result-selection history rules; independent library membership, timestamps and ordering; 30-second/95-percent progress thresholds and ten-second checkpoints; storage failure that does not block player exit; cancellation; failed-logout retry; authoritative rejection versus transient/credential failure; deleted-profile/content-policy enforcement; idempotent close; A → logout → B → A with overlapping profile IDs; stale session results/recorders; and account changes during combined saved-collection observation. Provider tests cover the corrected direct-details/source/saved-content policy bypass using TMDB's existing `adult` flag semantics.

Migration tests preserve exact legacy bytes and keys, trusted ownership before new login, unowned quarantine, malformed legacy JSON, non-ASCII account/profile identifiers, selected-profile restoration and retry, per-store interruption/resumption, and account partitioning. Android host tests close/reopen actual binary DataStore files using OkioStorage and the same PreferencesSerializer protobuf codec: Android FileStorage cannot replace an existing file through `File.renameTo` on the Windows host JVM. Production storage was not changed for that test accommodation. This is binary-codec/restart evidence, not an exhaustive emulator Android FileStorage migration test. The browser test closes/reopens actual browser `localStorage` stores for both owned data and quarantined malformed data. These focused fixtures do not imply every possible historical installation or storage failure has been exercised.

## Android visual and interaction review

All review APK installs used `install -r`; no app data was cleared and no profiles were deleted. Existing authenticated TMDB sessions restored without reading or entering credentials. Each referenced original was opened and inspected; capture-command success alone was not treated as visual acceptance. Most mobile/tablet originals precede the final sample-title correction; that correction was rechecked on the rebuilt TV APK.

[The representative home board](../../build/review/sdk-home-board.png) deterministically combines four inspected originals without cropping or redrawing. Its labels distinguish the earlier Android captures from the certified web home capture; the adjacent JSON records source hashes, dimensions and display scaling. The board itself was opened and inspected and does not expand the journey coverage below.

* **Mobile, emulator-5554, 1080×2400:** restored profiles → owner home → loaded details → actual Sintel video → back to details passed. Text, artwork and component corners were readable/aligned. A temporary My List item survived an app-only restart and rendered in Library, then was removed and confirmed unchecked. Evidence: [profiles](../../build/review/sdk-mobile-start2.png), [home](../../build/review/sdk-mobile-home-content.png), [details](../../build/review/sdk-mobile-details-saved.png), [video](../../build/review/sdk-mobile-demo-playing.png), [recovered library](../../build/review/sdk-mobile-library-recheck.png). Search/history was not completed in this bounded device pass.
* **Tablet, emulator-5556, 2560×1600:** restored profiles, owner home, Resident Evil details with cast/recommendations, demo video and explicit player Back to details passed. Readability and rounded corners were good; home retained two provider-artwork placeholders. Evidence: [profiles](../../build/review/sdk-tablet-start.png), [loaded home](../../build/review/sdk-tablet-home-loaded.png), [loaded details after return](../../build/review/sdk-tablet-details-loaded.png), [actual video frame](../../build/review/sdk-tablet-after-player.png). The last filename is misleading: it contains playback at 0:04 with controls/buffering. Search/library were not verified; a follow-up capture timed out. The corrected demo title was not rechecked on tablet.
* **TV, emulator-5558, 1920×1080:** restored profiles, D-pad owner selection, populated home, Resident Evil details, actual demo playback, pause/seek and Back to details passed. Focus rings, focused-button corners, artwork, text and player controls were inspected. Evidence: [profile focus](../../build/review/sdk-tv-profiles-focus.png), [home](../../build/review/sdk-tv-home.png), [details](../../build/review/sdk-tv-details.png), [paused playback](../../build/review/sdk-tv-playback-paused.png), [returned details](../../build/review/sdk-tv-back-details.png). The rebuilt APK then showed **“Sintel (sample playback)”** over actual sample video in [the final label capture](../../build/review/sdk-tv-playback-labelled.png); it contains brief buffering after a seek to 0:28 and is label/media evidence, not a fully settled frame. Logout/live reauthentication, profile editing, long playback/completion and cross-account switching were not exercised on TV.

Initial loading captures, Android full-screen hints, the TV Android CLI helper's Play Protect dialog and timed-out captures are excluded from positive visual evidence. No emulator security settings were changed.

### Mobile ANR observation

After returning from playback to home, mobile reported input-dispatch timeout at 16:16:52 local time, waiting 5,056 ms for a motion event. [The ANR capture](../../build/review/sdk-mobile-anr.png) was inspected. The historical report could not produce Java stacks: debuggerd/tombstoned stack collection timed out for both the app and `system_server`; their main-thread waiting channels were `0`. A later live app-main waiting channel was `do_epoll_wait`.

During 16:16:53–16:17:20, device CPU was 92% total: 4.6% user, 72% kernel and 13% softirq; CPU pressure avg10 was 67.86%. `system_server` used 66%, the sensors HAL 41%, and StreamCore 30% (1.9% user, 28% kernel). Three emulators and builds were active. This supports system-resource contention as a hypothesis, **not a proven Java/app cause or proof that the application is free of an ANR defect**. One Wait attempt did not recover navigation. One authorized app-only force-stop/relaunch preserved data and restored owner → Library navigation; the temporary review item was then removed. No speculative production fix was made from this evidence. The emulator was released for an isolated follow-up.

### Isolated mobile follow-up

At 17:57–18:02 local time, with builds stopped and the browser E2E run finished, the existing installed StreamCore app/session on emulator-5554 completed Home → One Last Shot details → player → Back → details → Home → Library → Search without a new ANR dialog. The device's last-ANR timestamp remained 16:16:52. This improves the navigation evidence without proving the original ANR's cause. Playback itself did **not** succeed on this attempt: the player showed the handled “Playback unavailable / Playback failed” state at 0:00 of 14:48, and Back remained responsive. This was the previously installed APK, still showing the pre-fix catalogue title; no rebuild/reinstall or successful final-player verification is implied. Inspected evidence: [loaded details](../../build/review/sdk-mobile-followup-details.png), [handled playback failure](../../build/review/sdk-mobile-followup-player.png), [returned Library](../../build/review/sdk-mobile-followup-library.png), and [Search discovery/history](../../build/review/sdk-mobile-followup-search.png). The Search capture includes the native keyboard toolbar; no query was submitted. `sdk-mobile-followup-home.png` captured the prior Library surface during transition and must not be used as Home evidence. No credentials were entered, the external consumer app was not touched, no saved data was mutated, and the emulator was released after this bounded pass.

## Final production browser visual and interaction review

The root-owned production preview at `http://127.0.0.1:8081` passed schema-2 checkout/fresh-artifact verification. Chrome ran at 1920×1080 with the existing local `build/review/browser` profile; the owned browser contexts were closed and their profile lock released afterward. The task-owned preview server was stopped after review. No browser state or credentials were exported by this browser review; the separately authorized standalone Android live test is recorded above.

The project capture helper produced [profiles](../../build/review/sdk-web-profiles.png), [owner home](../../build/review/sdk-web-home.png) and [Resident Evil details](../../build/review/sdk-web-details.png). Each was inspected for completed content/artwork, readable typography, layout, and aligned component corners/focus indication. A bounded interaction pass then verified submitted search results, history retained after reload, the restored library empty state, sample video advancing with the explicit **“Sintel (sample playback)”** label, and Escape returning to details with the video element removed. Evidence: [search](../../build/review/sdk-web-search.png), [history](../../build/review/sdk-web-history.png), [library](../../build/review/sdk-web-library.png), [labelled demo at 0:03](../../build/review/sdk-web-demo.png), and [interaction result](../../build/review/sdk-web-interactions.json). Every original was viewed. Some search items use artwork fallbacks; the sample capture is its dark opening frame, with advancing video/time confirmed separately.

The temporary search-history entry was removed and its absence confirmed. Library memberships were not changed. This was a representative desktop-browser pass, not exhaustive responsive, accessibility, account-switching, fullscreen, or long-playback coverage. An initial review locator expected a search-card role that did not match the rendered semantics; the visible results had loaded, and the corrected review assertions passed without changing production code.

After the startup-quota and native-focus repairs, [sdk-web-final-home.png](../../build/review/sdk-web-final-home.png) was captured from certified production source with `artifactStatus: fresh`. It was inspected at 1920×1080: owner home, complete artwork, readable text, navigation and rounded cards were intact. This home capture precedes the later avatar keyboard correction; earlier journey captures remain evidence for their stated builds. The existing private browser profile was retained and its lock released.

The final avatar keyboard correction passed the complete profile create/edit/delete, focus-trap and scrolling journey at both widths. Root inspected the final production captures at [1280×720](../../build/review/sdk-avatar-keyboard-1280.png) and [1920×1080](../../build/review/sdk-avatar-keyboard-1920.png): a different avatar is selected through arrow keys and Space, scrolled into view, and has an aligned, fully visible focus border and selection badge. These captures use deterministic fixture accounts and belong to the latest certified production build.

## Live-test cleanup and remaining review limits

**Live external Android TMDB acceptance: PASSED.** The user explicitly approved the transfer previously blocked by automatic approval review. The runner sent the saved credentials through standard input into the test application's private one-use file without printing them. The application deleted that file immediately after reading it, the runner repeated cleanup on exit, and a separate device check confirmed the file was absent. Both test-created session logout operations returned success before the pass result; remote revocation was not independently probed afterward. The completed test application was stopped after capturing evidence. Network failure or process termination can prevent cleanup in a failed run, as documented in the integration guide. No approval-dependent acceptance check remains.

The isolated mobile recheck did not reproduce the ANR, but its handled sample-playback failure remains a review limitation; unrestricted mobile playback acceptance is not claimed. JS/TS package delivery, Swift delivery and remote registry publication remain outside this milestone. See [integration](integration.md), [ownership baseline](ownership-and-baseline.md) and [provider template](provider-template.md) for the implemented contracts and future-provider requirements.
