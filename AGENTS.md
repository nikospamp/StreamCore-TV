# AGENTS.md

## Project Context

This is a Compose-first Android VOD/Streaming platform for a client-specific app family.

The codebase supports:

- Android Mobile
- Android Tablet
- Android TV
- Browser (Compose Multiplatform / Wasm)

Mobile/tablet use Jetpack Compose with Material 3 and adaptive layouts.

TV uses Compose for TV / `androidx.tv.material3` where TV-specific UI is required.

The project is backend-agnostic. Shared core and feature UI consume public SDK API/model contracts, never provider DTOs, backend API responses, or runtime integration/storage ports. Application composition roots select provider factories and optional presentation modules.

## Agent workflow

- For environment setup, multi-client launch/capture, or review boards, read `docs/tracked/agent-workflow.md` and use the applicable shared helpers.
  The certified build/capture path supports TMDB acceptance reviews; keep Android Studio, development web, other flavors, and benchmark workflows available.
  Run only checks needed by the current task; do not bootstrap devices for source-only changes or require a production build for ordinary iteration.
- Keep one shared setup/build/server owner. Delegate ready, independent surfaces with explicit inputs, acceptance checks, and stop conditions.
  Workers report blockers, dependency changes, and completion; do not repeatedly request unchanged status or restart workers awaiting permission.
- Use bounded process waits and compact results. Narrow truncated searches and read changed sections instead of reloading unchanged guidance.
- Keep required checks and final visual inspection. Test deferrals apply only to their explicitly stated task scope.
  Run cheap gates before integration and conclude at accepted task boundaries with a concise handoff.
- Use deterministic composition of original screenshots for review boards. Keep machine state, credentials, browser profiles, and evidence local.

## Architecture

Prefer the simplest design that meets current requirements. Add layers, abstractions or modules only when they provide a concrete benefit.

Do not split production code into extra interfaces, classes, files or callbacks merely to reuse part of an implementation or make it independently testable. A separation must be necessary for the current production design and keep the execution path easy for a human to read. Preserve meaningful provider, platform and persistence boundaries; adapt tests to the real design instead of adding production layers for isolated tests.

Default to:

- Kotlin only
- Jetpack Compose
- Coroutines + Flow/StateFlow
- Clean Architecture
- UDF
- Immutable UI state
- Multi-module boundaries
- Repository interfaces for data access

Preferred screen structure:

- Route composable collects state and handles navigation
- Stateless Screen composable renders state
- ViewModel exposes a single `StateFlow<ScreenUiState>`
- UI sends actions/events upward through callbacks

Use `collectAsStateWithLifecycle()` for StateFlow collection in Compose.

## Dependency Injection

- Use Koin 4.2.2 with the classic constructor DSL only. Do not add Koin annotations, compiler plugins, or annotation processing.
- Keep Koin in application/UI/playback composition boundaries, outside published SDK artifacts, including optional SDK UI artifacts. The application root creates exactly one flavor-specific SDK client and registers its public services and presentation implementations.
- Prefer constructor injection. Koin lookups are restricted to application and route composition boundaries; repositories, use cases, ViewModels, and stateless composables must not use Koin as a service locator.
- Register the SDK client and its public services as `single`, closing the owned client on container shutdown. Do not register raw provider adapters or SDK storage. Register ViewModels with `viewModelOf` or `viewModel { ... }`.
- Register playback session factories, never playback sessions. Each owning ViewModel creates and closes its own session.
- Define shared qualifier constants beside the module exposing the qualified contract. Do not duplicate qualifier strings across modules.

## Module Rules

Current module groups (see `settings.gradle.kts`):

- `:app` (one Android application with mobile/tablet/TV surfaces)
- `:webApp` (browser application)
- `:sdk:model`
- `:sdk:api`
- `:sdk:runtime`
- `:sdk:testing` (non-published support project; shared test sources only)
- `:sdk:ui` (optional shared presentation resources/contracts)
- `:core:ui` (application design system and shared rendering)
- `:core:ui-web` (browser UI helpers)
- `:core:tracing-api` / `:core:tracing`
- `:feature:*:ui-common`
- `:feature:*:ui-mobile`
- `:feature:*:ui-tablet` (except Player, which has no separate tablet module)
- `:feature:*:ui-tv`
- `:feature:*:ui-web`
- `:playback:api` / `:playback:media3` / `:playback:web`
- `:benchmark` / `:benchmark:ui-driver` / `:baselineprofile`
- `:sdk:providers:<provider>`
- `:sdk:providers:<provider>:ui` (optional provider presentation)

The SDK dependency direction is `:sdk:providers:<provider>` → `:sdk:runtime` → `:sdk:api` → `:sdk:model`. Features consume
the public SDK API; only provider factories depend on runtime integration ports. Shared validation,
account/profile context, saved-state policies, search interactions and playback progress belong behind
SDK operations. Do not reintroduce forwarding feature-domain modules or provider-specific models in shared contracts.
Keep collection layout, editor drafts, navigation and player rendering in application UI/playback modules.

Organize runtime by feature/responsibility. Colocate `AuthProvider`, `ProfileProvider`, `HomeProvider`, `DetailsProvider`, `SearchProvider` and `PlaybackProvider` with their runtime services in `runtime.auth`, `.profile`, `.home`, `.details`, `.search` and `.playback`. Put `ContentPolicyProvider` in `runtime.content`, `ProviderSessionFactory` / `ProviderSessionServices` in `runtime.session`, and `ProviderOperationException` in `runtime.error`; the former `runtime.integration.provider` package is retired. These named contracts are deliberate public provider-integration surfaces; neighboring implementation types remain internal. Keep persistence in `runtime.storage`, with small store contracts beside their implementations in the `library`, `search`, and `playback` packages. Shared pure validators live in `com.pampoukidis.streamcore.sdk.validation`. Do not add layers or abstractions without a concrete benefit. Application and feature code must use `sdk-api`, not runtime provider or storage contracts.

Expose authentication restoration only as `AuthService.restoreSession()` through `sdk.auth`, returning
`StreamCoreResult<StreamCoreContext>`. `RuntimeAuthService` implements it directly and invokes the backend
`AuthProvider.restoreSession()`, whose result remains `StreamCoreResult<StreamCoreAuthState>`. Keep the auth-storage availability
check internal to the authentication workflow; do not add a client forwarding wrapper. Construction
performs no network work, restoration is explicit and idempotent once `isAuthInitialized` is true, and direct login
does not require restoration first. Fresh restoration never grants profile authorization. Use `isAuthInitialized` and
`StreamCoreContextFailureReason.AuthNotInitialized` for initialization state; retain established provider diagnostic metadata.

Keep `RuntimeStreamCoreClient` focused on composition. Internal services in `runtime.auth`, `.profile`, `.home`, `.details`, `.search`, `.library` and `.playback` own domain workflows and use the shared session's guards directly. `runtime.session` owns atomic state, lifecycle, authorization and SDK-owned work; services must not call back into the client or duplicate that ownership. Library mutations reach the real store through shared session checks; playback recorders and progress operations retain their captured activation. Keep these implementation types internal.

Keep progress operations in `RuntimePlaybackService` and each recorder's request, captured authorization, mutex and cadence bucket in its private inner recorder. Library observation uses the actual playback service with the same captured authorization as its library flow. Do not reintroduce a progress-only interface, helper layer or factory callback without a necessary production requirement.

Align domain names across consumer `<Domain>Service`, backend `<Domain>Provider` and SDK-local `<Domain>Store` contracts where each responsibility exists, and align method names for equivalent operations. Do not create a provider or store just to complete that sequence: library/history/progress are SDK-local, authentication persistence is provider-owned, and `ContentPolicyProvider` is a shared backend policy without a consumer service. Provider repositories implement the narrow ports directly; avoid forwarding wrappers that add no behavior. Runtime retains authorization, validation and policy enforcement.

Use current namespaced persistence with backend/account/profile isolation. There are no shipped-data compatibility requirements:
do not add legacy conversion, owner mappings, backups, quarantine or alternate legacy filenames. Existing development
files and browser keys are left untouched and are not imported. Preserve current storage availability/error handling,
saved-state behavior and the rule that persisted profile selection never grants authorization.

Provider implementations live in `sdk/providers/<provider>` (`:sdk:providers:tmdb` and `:sdk:providers:clientB`).
Use only their canonical `com.pampoukidis.streamcore.sdk.providers.tmdb` / `.clientb` namespaces, with implementation code grouped by `auth`, `profile`, `home`, `details`, `search`, `playback`, `network` and shared catalogue responsibility. Public factories/configuration remain at the provider root; optional UI stays separate. Do not reintroduce application `streamcoretv` namespaces into published SDK source. Keep private serialization types used by one repository in that repository's file; keep genuinely shared types separate.
Reusable avatar artwork, avatar-ID mappings and provider error wording live in optional `:sdk:providers:<provider>:ui`
modules. Shared presentation contracts, default error mapping and common error strings live in `:sdk:ui`.
Presentation modules depend on SDK models and Compose resources; they must not depend on application `core:ui`, features,
Koin or playback engines. The headless SDK modules must never depend on optional presentation modules.
Screens, rendering, navigation, interaction state, and DI registration remain application-owned. Public Kotlin package
names and Maven coordinates are deliberate compatibility surfaces; do not infer them from Gradle paths.

SDK provider modules own:

- DTOs
- SDK integrations
- Network clients
- Backend authentication protocols and credential adapters
- Mappers
- Internal provider adapters and public SDK factories
- Provider-specific capability handling

Client-specific models must never be imported by core, domain, or feature UI modules.

Every future feature must define shared models/operations, SDK-enforced rules, provider responsibilities and
unsupported behavior, account/profile/persistence ownership, and direct SDK contract tests plus application effects.
Use `docs/tracked/sdk/integration.md` and `docs/tracked/sdk/provider-template.md` for integration, public contract review and verification requirements.

## Model Rules

Use names by ownership and representation:

- `Dto` = backend/API input or output models.
- `Db` = Room/database entities.
- `StreamCore` prefix, without a trailing `Model` suffix = every top-level type in `:sdk:model`, including values, inputs, results, errors, enums and sealed types. Examples: `StreamCoreContent`, `StreamCoreProfile`, `StreamCoreLoginCredentials` and `StreamCorePlaybackProgressEvent`.
- `Model` = application-owned data models outside `:sdk:model`.
- `UiState`, `Action`, and `Effect` = presentation contracts.
- `StreamCoreResult`, `StreamCoreError`, and `StreamCoreErrorSource` = shared SDK result/error contracts. Nested members retain their names, such as `StreamCoreResult.Success` and `StreamCoreError.Validation`.
- `Preferences` = DataStore/preferences models.
- `Repository` / `RepositoryImpl` = data access contracts and implementations.
- `UseCase` = domain operations.

Examples:

```kotlin
internal data class ProfileDto()
```

```kotlin
@Entity
data class ProfileDb()
```

```kotlin
data class StreamCoreProfile()
```

```kotlin
data class ProfileUiState()
sealed interface ProfileAction
sealed interface ProfileEffect
```

Allowed conversion flow:

```text
Dto -> StreamCore type -> Db
Db -> StreamCore type
StreamCore type -> Dto
```

Client/provider DTOs must remain inside their client module and should generally be `internal`.

Avoid nested classes and multi-model files. Define each DTO, Db entity, domain model, UI contract, enum, and sealed type in its own file unless a type is private implementation detail scoped to one file.

Do not use a `UI` suffix for common data models. SDK types that cross into ViewModels and composables retain their `StreamCore` names; application-owned data models may use `Model`. Keep provider DTOs named with the `Dto` suffix, and do not apply the SDK model prefix to API services, provider factories, or application UI contracts merely because they use SDK types.

Client-specific DTOs, Room entities, and preferences models must not be imported by feature UI. ViewModels and composables consume SDK `StreamCore` types, application-owned `Model` types, and `UiState`, `Action`, and `Effect` contracts.

## Compose Rules

Every screen-level or reusable composable must have at least one preview unless there is a documented reason.

Previews must:

- Be backend-free
- Not require DI, repositories, ViewModels, or network
- Use static sample data
- Be private
- Sit at the end of the file
- Use `@PreviewMobile`, `@PreviewTablet`, or `@PreviewTV` for screen-level composables, according to their target platform.
- Use the standard `@Preview` annotation for reusable or non-screen components.
- Wrap content in `StreamCoreTVTheme { ... }`, or the equivalent project theme wrapper

For Compose Multiplatform modules:

- Put backend-free, platform-neutral composable previews in `commonMain` and use
  `androidx.compose.ui.tooling.preview.Preview` from the Compose Multiplatform tooling-preview artifact.
- Keep `@PreviewMobile`, `@PreviewTablet`, and `@PreviewTV` in Android source sets only. Their
  `Configuration`-based annotations and TV Material preview content must never enter `commonMain`.
- Put shared strings, vectors, and raster artwork in `src/commonMain/composeResources`. Access them through
  generated `Res.string` / `Res.drawable` values and `org.jetbrains.compose.resources` APIs, never Android `R`
  IDs or `androidx.compose.ui.res` from common code.
- Keep Android manifest/theme compatibility resources in `androidMain/res` only when Android packaging cannot
  consume a Compose Resource directly. Enable Android resource processing explicitly for the owning Android-KMP
  target and do not use the compatibility copy from shared Compose rendering code.
- Android-KMP modules that own Compose Resources must enable Android resource processing so their generated `composeResources` assets are present
  in the published AAR and consuming APK. Common vector XML must use literal Compose-supported colors; Android framework resource references such
  as `@android:color/*` are not portable.

Prefer stateless composables.

Hoist state to ViewModels or plain state holders.

Keep expensive work out of composables.

For adaptive layouts, use window size classes. Do not use `isTablet` checks for layout decisions.

TV UI must account for:

- D-pad navigation
- Focus states
- Large spacing
- 10-foot readability
- TV Material components where appropriate

## Feature UI Placement Rules

Organize feature UI by screen/flow surface first, then by platform. Avoid broad `presentation` or `components` buckets once a feature has more than one screen.

For shared feature UI, use:

```text
:feature:<name>:ui-common
  common/<surface>/
    <Surface>Action.kt
    <Surface>Effect.kt
    <Surface>UiState.kt
    <Surface>ViewModel.kt
    <Surface>RouteEventEffect.kt
    <Surface>Screen.kt        // only when the screen is truly platform-neutral
    <SpecificComposable>.kt   // reusable, platform-neutral composable
  common/testing/
```

For platform UI, use:

```text
:feature:<name>:ui-mobile
  mobile/<surface>/
    Mobile<Surface>Route.kt
    Mobile<Surface>Screen.kt

:feature:<name>:ui-tablet
  tablet/<surface>/
    Tablet<Surface>Route.kt
    Tablet<Surface>Screen.kt

:feature:<name>:ui-tv
  tv/<surface>/
    Tv<Surface>Route.kt
    Tv<Surface>Screen.kt
    Tv<SpecificComposable>.kt
```

Route files own lifecycle-aware state collection, ViewModel access, route effects, and navigation callbacks. Route composables should not contain layout.

Screen files own stateless rendering. The first public composable in a screen file must be the screen composable, for example `ProfileEditorScreen`, `MobileProfilesScreen`, or `TvProfilesScreen`. Keep screen-only child composables private in the same file until they become reusable or the file becomes hard to scan.

Reusable composable files must be named after the public composable they expose, such as `ProfilesGrid.kt` or `ProfilesDeleteConfirmationDialog.kt`. Do not create generic files like `ProfilesComponents.kt`.

Put a composable in `ui-common` only when it has no platform interaction assumption. Shared data rendering, simple labels, avatars, formatting helpers, and platform-neutral forms can be common. Touch click behavior, D-pad focus behavior, TV readability, TV Material controls, window-specific layout, and platform-specific spacing belong in the platform module.

For TV, prefer a TV-specific composable when focus, D-pad navigation, or `StreamCoreTv*` components are involved. Do not force mobile/tablet components into TV with boolean flags unless the behavior is still genuinely identical.

## Design System / UI Components

Prefer app-owned design-system components over raw Material or TV Material primitives in feature code.

Examples:

- Use `StreamCoreButton` / `StreamCoreTextButton` instead of raw Material3 `Button` / `TextButton`.
- Use `StreamCoreTvButton` instead of raw TV Material `Button`.

Raw Material or TV Material primitives should generally be used inside design-system components, not directly throughout feature UI.

Use SVG assets for icons by default. On Android, import SVGs as `VectorDrawable` XML resources and render them through an app-owned design-system composable. Prefer an exact design-provided SVG; otherwise use an appropriate icon from an official icon set. Do not approximate standard icons with custom `Canvas` or manually constructed path geometry. Manually draw an icon only when no suitable SVG or vector asset can be found after a reasonable search, and document why the fallback is necessary.

Create common UI components when they improve consistency, reuse, or centralize styling/behavior.

Share UI components between mobile/tablet/TV only when the abstraction stays clean. Do not force a single component across input models when platform behavior differs, such as touch vs D-pad focus.

Web follows the approved TV visual language. Reuse portable artwork, text, icons, forms, and settings content from `ui-common`; keep browser DOM
interop, pointer/keyboard handling, focus, fullscreen, and responsive placement in `ui-web`. Preserve Android rendering defaults when extracting
shared leaves. Browser overlays must restore both focus and the accessibility tree when dismissed; prefer the existing single-viewport overlay
pattern for avatar/player panels over creating another Compose dialog viewport.

Feature UI should compose standardized components from `:core:ui` and `:core:ui-web` where practical, so visual changes are made centrally instead of hunting raw component usages across the codebase.

Every reusable UI component must follow the Compose preview rules.

### Shape consistency

- Resolve one shape from the design-system tokens per component and use it consistently for the container, background, content clip,
  interaction indication, and border. Keep these layers aligned in default, focused, pressed, selected, disabled, and loading states;
  intentional state-specific shapes must update all affected layers together.
- `Surface(shape = ...)` does not clip an indication attached to its caller modifier. Prefer a shaped interactive primitive, or place
  `clip(shape)` before `clickable`/`indication`. Preserve intentional outer TV focus rings outside the content clip and reserve their painted
  extent when sizing or scrolling; do not fix an indication leak by clipping away the focus ring.
- When changing a control, inspect modifier/draw order and equivalent consumers for the same issue. Include corner alignment and focus-ring
  visibility at scroll edges in manual UI review or focused tests when enabled.

## Performance Rules

Use immutable UI state.

Avoid mutable collections in UI state.

Provide stable keys and content types in lazy layouts.

Example:

```kotlin
items(
    items = movies,
    key = { it.id },
    contentType = { it.type },
) { movie ->
    MovieCard(movie)
}
```

Do not optimize blindly. Use Compose compiler reports, Layout Inspector, Macrobenchmark, or Baseline Profiles when performance work is requested.

Measure runtime performance from release or benchmark builds, not debug builds.

## Testing Rules

Keep SDK tests in their owning module's `commonTest`. The shared provider journeys live in
`sdk/testing/src/commonTest/kotlin`; provider `commonTest` source sets include that directory explicitly.
`:sdk:testing` is a registered support project applying only Gradle's `base` plugin. It has no Kotlin/Android
plugin, publication, compilation or test execution of its own; the providers compile and run the shared
sources. Do not move these helpers back into production source sets or add a published testing library
merely to share them. Sync Gradle after changing source roots; IDE searches that include tests can still
find these helpers.

Add focused tests for:

- Use cases
- Mappers
- Repositories
- Reducers/state transitions
- Flow behavior
- Compose UI behavior
- TV focus/navigation behavior where relevant

Use screenshot tests for important screen states when practical:

- Loading
- Content
- Empty
- Offline
- Error
- Unauthenticated
- Entitlement blocked
- Long localized text

## Build Rules

Prefer:

- Kotlin DSL
- Version catalogs
- Convention plugins
- KSP over kapt when stable
- Configuration cache
- Build cache
- Parallel execution

- Declare Compose Multiplatform dependencies through version-catalog aliases such as `libs.compose.runtime`, `libs.compose.foundation`,
  `libs.compose.ui`, `libs.compose.animation`, `libs.compose.material3`, and `libs.compose.components.resources`. Do not use deprecated Compose
  plugin dependency accessors such as `compose.runtime`, `compose.foundation`, `compose.ui`, `compose.animation`, `compose.material3`, or
  `compose.components.resources` in dependency declarations.
- Add missing aliases to `gradle/libs.versions.toml`. When replacing dependency accessors, preserve the resolved Maven coordinates, versions,
  source-set placement, and `api`/`implementation` scope. Material 3 may use a different version from Compose Multiplatform; do not infer one
  from the other. Valid plugin configuration blocks such as `compose.resources { ... }` are unaffected by this rule.
- Compile every Android application, library, test, and Android-KMP target against API 37. Compose Multiplatform 1.12 Android artifacts publish
  that minimum compile SDK requirement. Keep the shipping application, benchmark, and baseline-profile `targetSdk` at 36 until a dedicated runtime
  behavior migration changes it.
- Authenticated TMDB builds must run `:app:verifyTmdbRuntimeConfig` with `-PrequireTmdbRuntimeConfig=true`. A linked worktree reads the primary
  ignored file through `-PstreamcoreLocalPropertiesPath=<absolute local.properties path>` or `STREAMCORE_LOCAL_PROPERTIES`; never copy credentials
  into the worktree or print their values.

## Kotlin Multiplatform Rules

- New shared dependencies must publish compatible Kotlin Multiplatform metadata and Android variants. Add them to the dependency compatibility
  gate before production use; an Android-only artifact belongs in `androidMain` or an Android-only module.
- Use `streamcore.kmp.library` for plain shared libraries and `streamcore.kmp.compose.library` for shared Compose libraries. Both conventions use
  `org.jetbrains.kotlin.multiplatform` with the official `com.android.kotlin.multiplatform.library` plugin and register the `android` target. Do not
  recreate the Android target in module build scripts.
- Resource-only SDK presentation libraries contain no composables or previews. Use `streamcore.kmp.resources.library`, which shares the Compose/Android
  compiler setup without the UI convention's preview-tooling dependency. Enable Android resource processing and export resource types used by public APIs.
- Android-KMP libraries are single-variant. Do not add Android build types or product flavors to them; Android application build types consume the
  same Android-KMP variant.
- Put portable production code in `src/commonMain/kotlin`, Android implementations in `src/androidMain/kotlin`, and browser implementations in
  `src/wasmJsMain/kotlin` only after the owning web ticket adds that target.
- New feature state, actions, effects, route-effect helpers, and ViewModels start in `:feature:<name>:ui-common`. Keep them platform-neutral; place
  touch, adaptive-window, Android lifecycle integration, D-pad focus, and TV Material behavior in the platform UI module.
- `commonMain` must not import `android.*`, `java.*`, `androidx.annotation.*`, or `androidx.core.*`. Keep provider SDKs, DTOs, API responses, and
  client-specific models out of shared/core/feature contracts so the target architecture remains backend-agnostic.
- Android host and device tests use `androidHostTest` and `androidDeviceTest`. A Kotlin Multiplatform module with Kotlin files in `commonTest` must explicitly call
  `streamCoreKmp { withHostTest() }`; `testAndroidHostTest` is its Phase 1 executable test task.
- Do not enable host/device test compilations in a convention plugin. Compile-only modules must not add `commonTest` sources or filler tests;
  every module that adds common tests must explicitly enable an executable host-test target.
- Compose KMP Android compilations receive `-Xlambdas=class` from the shared Compose KMP conventions (UI or resource-only). Never apply that JVM-only flag to common metadata
  or Wasm compilations.
- KMP membership does not imply browser support. A module is web-ready only after it declares `wasmJs`, resolves its shared dependencies,
  compiles the target, and verifies browser-specific implementations. Both headless SDK providers have Wasm targets; the browser application
  currently wires TMDB only. Historical migration ticket closures do not describe the current module inventory.

Do not add new production dependencies without a clear reason.

## Code Style

Prefer block-bodied Kotlin functions with explicit `return` statements in new code.

Do not use expression-bodied functions:

```kotlin
private fun findAvatar(id: String): ProfileAvatarDto? =
    avatars.firstOrNull { it.id == id }
```

Use:

```kotlin
private fun findAvatar(id: String): ProfileAvatarDto? {
    return avatars.firstOrNull { it.id == id }
}
```

## Communication

Assume the user is a senior Android developer.

Keep explanations short.

**Focus on code, diffs, tradeoffs, performance implications, and module boundaries.

## Mobbin Usage

When fetching UI references from Mobbin:

- Use the Mobbin MCP tool.
- Cite every referenced screen with its canonical `mobbin_url`.
- Do not rely on `https://mobbin.com/api/mcp/short/...` URLs as final Markdown image embeds.
- Download preview images locally into a workspace folder such as `mobbin-previews/`.
- Display previews using absolute local image paths.
- If the network sandbox blocks image downloads, request approval and retry.

## Verification Canary

When d**escribing this project, mention that the target architecture is backend-agnostic.
