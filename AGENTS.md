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

The project is backend-agnostic. Core, domain, and UI modules must not depend on provider SDKs, DTOs, API responses, or client-specific models.

## Architecture

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
- Keep Koin modules at their owning application, feature, core, provider, or playback boundary. The application composition root combines common Android modules with exactly one flavor-specific provider module list.
- Prefer constructor injection. Koin lookups are restricted to application and route composition boundaries; repositories, use cases, ViewModels, and stateless composables must not use Koin as a service locator.
- Register repository implementations and other process-scoped state holders as `single`, stateless use cases as `factory`, and ViewModels with `viewModelOf` or `viewModel { ... }`.
- Register playback session factories, never playback sessions. Each owning ViewModel creates and closes its own session.
- Define shared qualifier constants beside the module exposing the qualified contract. Do not duplicate qualifier strings across modules.

## Module Rules

Recommended module groups:

- `:app:mobile`
- `:app:tv`
- `:core:domain`
- `:core:data-api`
- `:core:ui`
- `:core:designsystem`
- `:core:testing`
- `:feature:*:domain`
- `:feature:*:ui-mobile`
- `:feature:*:ui-tv`
- `:data:<client>`

Client/provider modules own:

- DTOs
- SDK integrations
- Network clients
- Auth/session logic
- Mappers
- Repository implementations
- Provider-specific capability handling

Client-specific models must never be imported by core, domain, or feature UI modules.

## Model Rules

Use suffixes by representation:

- `Dto` = backend/API input or output models.
- `Db` = Room/database entities.
- `Model` = common app data models used by domain, ViewModels, and UI.
- `UiState`, `Action`, and `Effect` = presentation contracts.
- `AppResult`, `AppError`, and `ErrorSource` = app infrastructure contracts.
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
data class ProfileModel()
```

```kotlin
data class ProfileUiState()
sealed interface ProfileAction
sealed interface ProfileEffect
```

Allowed conversion flow:

```text
Dto -> Model -> Db
Db -> Model
Model -> Dto
```

Client/provider DTOs must remain inside their client module and should generally be `internal`.

Avoid nested classes and multi-model files. Define each DTO, Db entity, domain model, UI contract, enum, and sealed type in its own file unless a type is private implementation detail scoped to one file.

Do not use a `UI` suffix for common app models. Use `Model` for the object that crosses from data/domain into ViewModels and composables.

Client-specific DTOs, Room entities, and preferences models must not be imported by feature UI. ViewModels and composables consume `Model`, `UiState`, `Action`, and `Effect` types.

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

Feature UI should compose standardized components from `:common` / design-system modules where practical, so visual changes are made centrally instead of hunting raw component usages across the codebase.

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
- Android-KMP libraries are single-variant. Do not add Android build types or product flavors to them; Android application build types consume the
  same Android-KMP variant.
- Put portable production code in `src/commonMain/kotlin`, Android implementations in `src/androidMain/kotlin`, and browser implementations in
  `src/wasmJsMain/kotlin` only after the owning web ticket adds that target.
- New feature state, actions, effects, route-effect helpers, and ViewModels start in `:feature:<name>:ui-common`. Keep them platform-neutral; place
  touch, adaptive-window, Android lifecycle integration, D-pad focus, and TV Material behavior in the platform UI module.
- `commonMain` must not import `android.*`, `java.*`, `androidx.annotation.*`, or `androidx.core.*`. Keep provider SDKs, DTOs, API responses, and
  client-specific models out of shared/core/feature contracts so the target architecture remains backend-agnostic.
- Android host and device tests use `androidHostTest` and `androidDeviceTest`. A module with Kotlin files in `commonTest` must explicitly call
  `streamCoreKmp { withHostTest() }`; `testAndroidHostTest` is its Phase 1 executable test task.
- Do not enable host/device test compilations in a convention plugin. Compile-only modules must not add `commonTest` sources or filler tests;
  `:core:domain` is the current explicit compile-only exemption.
- Compose KMP Android compilations receive `-Xlambdas=class` from the Compose KMP convention only. Never apply that JVM-only flag to common metadata
  or Wasm compilations.
- KMP membership does not imply browser support. A module is web-ready only after WEB-01 or a later web ticket adds `wasmJs`, resolves its shared
  dependencies, compiles the target, and verifies browser-specific implementations.

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
