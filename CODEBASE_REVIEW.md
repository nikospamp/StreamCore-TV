# StreamCoreTV codebase review

Date: 2026-07-10

Scope: repository-wide static review of 26 Gradle modules, 224 production Kotlin files, 25 unit-test files, 6 instrumented-test files, build
configuration, manifests, and project documentation.

## Executive summary

The codebase has a good Compose/UDF foundation and is already meaningfully backend-agnostic at the provider boundary: provider DTOs are internal,
repositories expose common models, route composables collect lifecycle-aware state, and lazy layouts consistently use keys and content types.

The highest-value work is not a rewrite. It is to remove two production-safety hazards, make the intended Clean Architecture visible in the actual
module graph, narrow the app/platform dependency surface, and turn the current local conventions into automated quality gates.

| Priority | Finding                                                                      | Why it matters                                                                                   |
|----------|------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------|
| P0       | Production login state contains credentials                                  | Ships sensitive-looking defaults, changes UX, and currently breaks the unit suite                |
| P0       | `clientB` is a release-capable fake provider                                 | Any credentials authenticate and production sources return hardcoded demo data                   |
| P1       | Auth session is eligible for backup/transfer                                 | A plaintext session ID is stored while backup rules are still the generated templates            |
| P1       | Module names and dependency directions contradict Clean Architecture         | `domain -> data` and broad `api` edges obscure ownership and increase rebuild scope              |
| P1       | One APK compiles and ships all platform UIs                                  | Mobile, tablet, and TV surfaces are coupled through a runtime width check and a large dispatcher |
| P1       | Full content models are serialized into navigation arguments                 | Risks oversized saved state, stale payloads, and duplicated navigation state                     |
| P1       | There is no CI-quality aggregate and the unit suite is red                   | Local conventions are not protected; complex profiles code has no tests                          |
| P2       | Large UI files and hardcoded copy slow feature work                          | Home screens are about 800 lines each and production copy is not resource-backed                 |
| P2       | Repeated Gradle configuration and broad `api` exposure create build friction | 26 modules repeat the same Android/Compose/Hilt setup; 19 project edges use `api`                |
| P2       | Release performance has no guardrail                                         | Release shrinking is off and there is no baseline-profile or macrobenchmark module               |

## What is already working well

- Provider boundaries are mostly clean. TMDB/client DTOs are `internal`, and repository implementations map them to common `Model` types.
- The error transport is backend-agnostic at the network boundary. [
  `TmdbCallExecutor`](data/tmdb/src/main/kotlin/com/pampoukidis/streamcoretv/data/tmdb/network/TmdbCallExecutor.kt) preserves coroutine cancellation,
  and [`TmdbErrorMapper`](data/tmdb/src/main/kotlin/com/pampoukidis/streamcoretv/data/tmdb/network/TmdbErrorMapper.kt) removes Ktor types before
  errors leave the provider module.
- Route composables consistently use `collectAsStateWithLifecycle()`. One-off effects are also collected inside `repeatOnLifecycle(STARTED)`.
- ViewModels expose a single immutable `StateFlow<UiState>` and accept actions, matching the target UDF structure.
- Lazy lists/grids use stable keys and content types across home, details, and profiles.
- Screen and reusable composable previews are widely present and backend-free.
- Version catalogs, configuration cache, build cache, parallel execution, Kotlin serialization, KSP, and the custom design-token gate are already in
  place.

## Detailed findings

### 1. P0 — remove credentials from production `LoginUiState`

Evidence:

- [`LoginUiState.kt:6`](feature/login/ui-common/src/main/kotlin/com/pampoukidis/streamcoretv/feature/login/common/login/LoginUiState.kt#L6) defaults
  the identifier to `NikosPampoukidis`.
- [`LoginUiState.kt:7`](feature/login/ui-common/src/main/kotlin/com/pampoukidis/streamcoretv/feature/login/common/login/LoginUiState.kt#L7) defaults
  the password to a credential-like value.
- The repository-wide `test` task currently fails at [
  `LoginViewModelTest.kt:49`](feature/login/ui-common/src/test/kotlin/com/pampoukidis/streamcoretv/feature/login/common/login/LoginViewModelTest.kt#L49):
  the test expects empty initial credentials but receives the production defaults.

Action:

1. Restore empty defaults in `LoginUiState`.
2. Keep populated values only inside private preview functions or preview-data objects.
3. Add an explicit `LoginUiState()` invariant test that asserts both credential fields are empty.
4. Add a secret-scanning CI step so credential-like literals cannot accidentally move from previews/tests into `src/main` again.

Performance/memory: this also stops a password from being retained in the initial `StateFlow` for every login ViewModel instance.

### 2. P0 — make the fake `clientB` provider impossible to ship accidentally

Evidence:

- [
  `ClientBAuthenticateRepository.kt:20`](data/clientB/src/main/kotlin/com/pampoukidis/streamcoretv/data/client/auth/ClientBAuthenticateRepository.kt#L20)
  accepts every identifier/password and immediately returns success.
- [`ClientBCatalogSource.kt:14`](data/clientB/src/main/kotlin/com/pampoukidis/streamcoretv/data/client/catalog/ClientBCatalogSource.kt#L14) builds the
  catalog from hardcoded production-source data.
- Its image URLs use the non-routable example host at [
  `ClientBCatalogSource.kt:120`](data/clientB/src/main/kotlin/com/pampoukidis/streamcoretv/data/client/catalog/ClientBCatalogSource.kt#L120).
- `clientBRelease` is currently a valid release variant; there is no build-time guard.

Action:

- If this is a sample provider, rename the module/flavor to `demo` or `fake`, give it a non-production application ID suffix, and disable its release
  variant with `androidComponents`.
- If Client B is intended to ship, treat authentication, catalog networking, persistence, and error mapping as release blockers and remove the
  in-memory implementations from `src/main`.
- Add a release check that rejects known demo hosts and fake repository bindings.

### 3. P1 — define an explicit backup policy for the auth store

Evidence:

- The app enables backup at [`AndroidManifest.xml:16`](app/src/main/AndroidManifest.xml#L16).
- [`data_extraction_rules.xml`](app/src/main/res/xml/data_extraction_rules.xml) and [`backup_rules.xml`](app/src/main/res/xml/backup_rules.xml) are
  still generated templates with no exclusions.
- [`TmdbPreferencesAuthStore.kt:62`](data/tmdb/src/main/kotlin/com/pampoukidis/streamcoretv/data/tmdb/auth/TmdbPreferencesAuthStore.kt#L62) stores the
  TMDB session ID in Preferences DataStore.

Action:

1. Decide whether account/session state may be restored to another device.
2. Prefer excluding `datastore/tmdb_auth.preferences_pb` from both cloud backup and device transfer; otherwise set `allowBackup=false` if the product
   does not require backup.
3. Add a merged-manifest/backup-policy verification test for release variants.
4. Put session storage behind a provider-owned `SecureSessionStore`; use a Keystore-backed implementation if the threat model requires encryption at
   rest.

Memory risk is low; the concern is session disclosure and restoring a stale provider session onto a different install/device.

### 4. P1 — restore clear data/domain/UI dependency directions

The implementation is provider-agnostic, but the module vocabulary says otherwise:

- `:core:data` contains `com...core.model.*` and is depended on by [`:core:domain`](core/domain/build.gradle.kts#L10).
- Feature `data` modules contain domain inputs/results such as [
  `LoginCredentials`](feature/login/data/src/main/kotlin/com/pampoukidis/streamcoretv/feature/login/data/LoginCredentials.kt), [
  `DetailsModel`](feature/details/data/src/main/kotlin/com/pampoukidis/streamcoretv/feature/details/data/DetailsModel.kt), and `ProfileDraftModel`.
- Feature domain modules depend on those feature `data` modules.
- UI-common modules expose domain/data transitively with `api`; platform UI modules then expose UI-common transitively. There are 19 project-level
  `api` edges.
- [`MODULE_DEPENDENCY_GRAPH.md`](MODULE_DEPENDENCY_GRAPH.md) still references a nonexistent `:core:model` and only documents the older login graph.

Recommended target:

```text
:core:model <- :core:domain <- :feature:*:domain
      ^              ^               ^
      |              |               |
:data:<client> ------+        :feature:*:ui-common
                                      ^
                                      |
                         :feature:*:ui-mobile/tablet/tv
```

Action:

1. Rename `:core:data` to `:core:model`, or split it into `:core:model` and `:core:data-api` if common data contracts emerge.
2. Move feature request/draft/validation/result types into their feature domain modules; reserve feature `data` for implementations only.
3. Replace `api(projects...)` with `implementation(projects...)` unless a type is deliberately part of the consuming module's public API.
4. Move `ErrorModel` and `ErrorPresentationMapper` out of the core model layer. Resource-ID-based presentation belongs in `:core:ui` or the app/client
   UI boundary, while provider modules should emit only `AppError`/`ErrorSource`.
5. Rename `ErrorPresentationPresentationMapper` to a single, unambiguous name.
6. Add a dependency-rule test/task and generate the module graph from Gradle instead of maintaining it manually.

Build impact: narrower `implementation` edges reduce ABI invalidation and downstream recompilation.

### 5. P1 — separate platform entry points and use adaptive APIs for mobile/tablet

Evidence:

- [`app/build.gradle.kts:67`](app/build.gradle.kts#L67) through line 78 depend on every mobile, tablet, and TV feature module for every client
  variant.
- [`PlatformUtils.kt:15`](core/ui/src/main/kotlin/com/pampoukidis/streamcoretv/core/ui/utils/PlatformUtils.kt#L15) classifies tablets with a custom
  `screenWidthDp >= 600` check.
- [`StreamCoreNavHost.kt`](app/src/main/java/com/pampoukidis/streamcoretv/navigation/StreamCoreNavHost.kt) repeats the platform switch for every
  destination and has grown to roughly 435 lines.
- The single manifest exposes both launcher and Leanback launcher entry points.

Action:

- Preferred for this app family: create `:app:mobile` and `:app:tv`. Keep tablet as an adaptive mobile surface driven by window size classes; let the
  TV app depend only on TV UI modules and own the Leanback manifest.
- If a universal APK is a hard product requirement, calculate an adaptive window class once at the app shell, inject a platform destination factory,
  and remove width/device detection from `:core:ui`.
- Keep touch and D-pad components separate where behavior differs; share only platform-neutral rendering/data.

Build/runtime impact: platform-specific app modules shrink classpaths, APK/AAB contents, Hilt aggregation, lint scope, and incremental build work.

### 6. P1 — pass navigation identifiers, not `ContentModel`

Evidence:

- [`AppRoute.kt:33`](app/src/main/java/com/pampoukidis/streamcoretv/navigation/AppRoute.kt#L33) includes `ContentModel?` in `AssetDetails`.
- [`ContentModelNavType.kt`](app/src/main/java/com/pampoukidis/streamcoretv/navigation/ContentModelNavType.kt) JSON-serializes the full model into a
  `Bundle`/route value.
- `ContentModel` contains descriptions, cast, genres, artwork, and playback progress; navigation also keeps a second `remember` copy in
  `StreamCoreNavHost`.

Action:

1. Reduce the route to `profileId`, `contentId`, and optionally a small shared-transition key.
2. Load details from the repository/ViewModel using `SavedStateHandle` route IDs.
3. If instant transition content is required, keep it in a navigation-scoped in-memory cache keyed by `contentId`; treat it as an optimization, never
   the source of truth.
4. Add process-death restoration coverage for the details route.

Memory/performance: this removes JSON encode/decode allocations, reduces saved-state size, and avoids Binder transaction-size risk as content models
grow.

### 7. P1 — create one enforceable quality gate and close the test gaps

Current evidence:

- `:app:assembleDebug` succeeds for both `tmdbDebug` and `clientBDebug`.
- Android lint succeeds for both variants with 29 warnings each; non-version warnings are four unused resources, one duplicated XML/bitmap icon, and
  one redundant manifest label.
- `verifyDesignTokens` succeeds and reuses the configuration cache.
- The repository-wide unit test selector fails on the login default-state test described above.
- There is no checked-in GitHub Actions/GitLab/Jenkins pipeline, no Detekt/ktlint setup, no `.editorconfig`, and no contributor README.
- `:feature:profiles:domain` has seven production files and zero tests; `:feature:profiles:ui-common` has sixteen production files and zero tests.
  Home/details TV surfaces also have no focus-navigation tests.

Action:

1. Add a root `qualityGate` task that aggregates unit tests, both client lints, design-token verification, and debug assembly. Do not rely on the
   current root `check`, which only depends on `verifyDesignTokens`.
2. Run that task in CI on every pull request.
3. Add Detekt/formatting with a checked-in `.editorconfig`, including the project's block-body rule and module/package naming rules.
4. Add focused tests for `ProfilesViewModel`, `ProfileEditorViewModel`, all seven profiles use cases, rapid refresh/load replacement, delete failure,
   and selection concurrency.
5. Add TV D-pad focus tests for profiles, home rows, details recommendations, and back navigation.
6. Add screenshot tests for loading/content/empty/offline/error and long localized text; remove the generated `ExampleUnitTest`/
   `ExampleInstrumentedTest`.

Concurrency note: `HomeViewModel` and `DetailsViewModel` cancel replaced loads, but `ProfilesViewModel.refresh()` can launch overlapping refreshes and
`ProfileEditorViewModel` does not cancel an obsolete load request. Align these on one reducer/latest-request policy and test it explicitly.

### 8. P2 — split oversized screens and resource all production copy

Evidence:

- `MobileHomeScreen.kt` is about 798 lines; `TabletHomeScreen.kt` is about 793 lines.
- Details screen files range from about 372 to 448 lines.
- A repository scan found 64 direct `text = "..."` assignments across 17 app/core/feature files. Many are production copy such as errors, actions,
  empty states, and accessibility descriptions.

Action:

- Extract named platform composables such as `MobileHeroPager`, `ContinueWatchingRow`, `HomeShelf`, and `RecommendationsRail` into files named after
  their public composable, each with a private preview.
- Share mobile/tablet rendering primitives in `ui-common` only when interaction and layout assumptions remain platform-neutral. Keep TV focus/D-pad
  surfaces TV-specific.
- Move all production copy and content descriptions to resources. Use format arguments/plurals and allow client flavor resource overlays where
  branding/provider wording differs.
- Add long-text/pseudo-locale screenshot coverage before further visual expansion.

Recomposition impact: extraction alone is not an optimization. Preserve stable parameters, keep hot state reads local, and validate with compiler
reports/Layout Inspector before changing stability annotations.

### 9. P2 — move repeated Gradle logic into convention plugins

Android/Compose/Hilt modules repeat compile SDK, minimum SDK, Java target, Compose enablement, runner, KSP, and nearly identical dependency sets. The
root build script also contains a large custom design-token task implementation.

Action:

- Add an included `build-logic` build with focused plugins such as:
    - `streamcore.android.application`
    - `streamcore.android.library`
    - `streamcore.android.compose`
    - `streamcore.android.hilt`
    - `streamcore.kotlin.jvm`
    - `streamcore.feature.ui`
- Move `VerifyDesignTokensTask` into build logic and cover it with Gradle TestKit tests, including comments/string literals and allowed-token files.
- Add a variant-aware TMDB configuration check. The current build silently substitutes empty strings when `tmdbReadAccessToken`/`tmdbAccountId` are
  missing, moving configuration failures to runtime.
- Add a tracked `README.md` with JDK/SDK requirements, client configuration keys, common tasks, and the `qualityGate` command.

### 10. P2 — add release performance and shrinking guardrails

Evidence:

- [`app/build.gradle.kts:29`](app/build.gradle.kts#L29) disables minification for release.
- There is no baseline-profile generator or macrobenchmark module.

Action:

1. Enable R8 and resource shrinking for release, then add only evidence-based keep rules.
2. Add a baseline-profile module covering cold start, login/profile selection, home first render, and first vertical/horizontal scroll.
3. Add macrobenchmarks against a release-like build with profile installation required.
4. Track startup and frame timing separately for mobile/tablet and TV; do not measure Compose performance from debug builds.

## Documentation and governance drift

- [`AGENTS.md`](AGENTS.md) contains the strongest architecture and coding rules, but [`.gitignore:317`](.gitignore#L317) excludes it, so it is not
  version controlled.
- `MODULE_DEPENDENCY_GRAPH.md` is stale.
- There is no tracked onboarding/contribution document.

Action: either commit `AGENTS.md` or extract its durable rules into tracked `CONTRIBUTING.md` plus architecture decision records. The backend-agnostic
target architecture should be visible and enforceable for every developer and CI environment, not only local tooling.

## Suggested implementation order

### Immediate (same day)

1. Empty the production login defaults; fix the failing test.
2. Disable/rename the `clientB` release variant unless it is ready to ship.
3. Exclude the session DataStore from backup/transfer.
4. Remove the redundant manifest label and unused duplicate icon resources reported by lint.

### Next iteration

1. Add `qualityGate` + CI + formatting/static analysis.
2. Add profiles ViewModel/use-case tests and TV focus tests.
3. Resource production copy and split the two home screen files.
4. Add convention plugins and replace unjustified `api` edges.

### Planned architecture work

1. Rename/split `:core:data` and move feature-domain models out of feature `data` modules.
2. Move error presentation mapping to the UI/app boundary.
3. Split mobile/TV app entry points and adopt window size classes for mobile/tablet.
4. Replace content-model navigation arguments with identifiers.
5. Add release shrinking, baseline profiles, and macrobenchmarks.

## Verification record

Commands run against the reviewed working tree:

```text
./gradlew help --configuration-cache             PASS
./gradlew verifyDesignTokens                     PASS
./gradlew :app:assembleDebug                     PASS (tmdbDebug + clientBDebug)
./gradlew :app:lintTmdbDebug :app:lintClientBDebug PASS (29 warnings per variant)
./gradlew test                                   FAIL (:feature:login:ui-common, 1/5 tests)
```

The working tree already contained extensive uncommitted design-system and screen changes before this review. No production source was modified; this
document is the only review artifact added.
