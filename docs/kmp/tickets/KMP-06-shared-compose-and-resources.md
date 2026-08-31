# KMP-06 — Migrate Shared Presentation, Compose UI, and Resources

## Goal

Move shared ViewModels, UDF contracts, reusable Compose UI, client presentation adapters, and shared resources to Compose Multiplatform while keeping
touch/TV interaction modules Android-specific.

## Context

The current `ui-common` modules are Android libraries even though most state and rendering is portable. `:core:ui` also mixes portable Material3
components with Android previews/resources and TV Material components. Error and avatar contracts expose Android resource IDs indirectly and must
become true multiplatform presentation contracts.

## Dependencies and Parallelization

- **Depends on:** KMP-05.
- **Blocks:** KMP-07.
- **Parallelization:** One integration owner because resource APIs affect core UI, both clients, and every platform UI consumer.

## In-Scope Modules

- `:core:ui`
- All feature `ui-common` modules.
- `:client:tmdb:ui`
- `:client:clientB:ui`
- Android platform modules only where required to consume changed common APIs/resources.
- `HomeViewModel` tracing integration.

## Non-Goals

- Mobile/tablet/TV screen modules do not become KMP.
- TV Material controls are not replaced.
- No web modules or wasm target.
- No visual redesign.
- No navigation-shell migration.

## Implementation Tasks

1. Convert `:core:ui`, all `ui-common` modules, and both client UI modules to Compose KMP Android libraries.
2. Move portable UDF contracts, ViewModels, route-effect helpers, neutral composables, design tokens, and previews to `commonMain`.
3. Use multiplatform Lifecycle/ViewModel artifacts and Koin Compose ViewModel dependencies in common source sets.
4. Move Android-only code to `androidMain`:
    - Android preview annotations using `Configuration`.
    - `androidx.tv.material3` components.
    - Android-only resource compatibility.
    - Platform APIs without common Compose equivalents.
5. Migrate shared `core:ui` strings and vector assets to `src/commonMain/composeResources`.
6. Migrate TMDB/ClientB avatar vectors and provider error strings to each client UI module's Compose Resources.
7. Replace the Android resource-ID error contract:

```kotlin
data class ErrorUiModel(
    val title: StringResource,
    val message: StringResource,
    val confirmAction: StringResource,
    val dismissible: Boolean = true,
)

fun interface ErrorPresentationMapper {
    fun map(error: AppError): ErrorUiModel
}
```

Move these types to the shared UI layer. `AppError`, `AppResult`, and `ErrorSource` remain in core data/infrastructure.

8. Replace the avatar resolver contract:

```kotlin
fun interface ProfileAvatarArtworkResolver {
    fun resolve(avatarId: String): DrawableResource?
}
```

Update `StreamCoreProfileArtwork`, client mappings, tests, previews, and platform consumers.

9. Replace common `java.net.URI`, `Locale.ROOT`, `System.currentTimeMillis()`, and remaining JVM helpers with common equivalents. Preserve HTTPS-only
   trailer validation and invariant search normalization.
10. Inject `PerformanceTracer` into `HomeViewModel`; provide the existing Android tracer in benchmark/profile graphs and no-op tracer in normal/common
    tests.
11. Preserve stateless screen contracts and preview-friendliness. Koin access remains in route/default parameters, not stateless screens.
12. Migrate common ViewModel/reducer/effect tests to common tests. Keep Android UI tests in Android platform modules or explicitly enabled device
    tests.
13. Ensure shared lazy layouts retain stable keys/content types and new resource wrappers do not allocate on every recomposition.

## Public API or Type Changes

- `ErrorModel` becomes UI-owned `ErrorUiModel` using Compose `StringResource` rather than `Int` IDs.
- `ErrorPresentationMapper` moves from core model/data to shared UI.
- `ProfileAvatarArtworkResolver.resolve()` returns `DrawableResource?` rather than `Int?`.
- `HomeViewModel` receives `PerformanceTracer` through its constructor.
- Shared ViewModels lose remaining Android/Hilt dependencies but retain UDF/state/action/effect behavior.

## Verification Commands

```powershell
.\gradlew.bat :core:ui:compileCommonMainKotlinMetadata
.\gradlew.bat :feature:login:ui-common:compileCommonMainKotlinMetadata
.\gradlew.bat :feature:profiles:ui-common:compileCommonMainKotlinMetadata
.\gradlew.bat :feature:home:ui-common:compileCommonMainKotlinMetadata
.\gradlew.bat :feature:search:ui-common:compileCommonMainKotlinMetadata
.\gradlew.bat :feature:details:ui-common:compileCommonMainKotlinMetadata
.\gradlew.bat :feature:library:ui-common:compileCommonMainKotlinMetadata
.\gradlew.bat :feature:player:ui-common:compileCommonMainKotlinMetadata
.\gradlew.bat :client:tmdb:ui:compileCommonMainKotlinMetadata
.\gradlew.bat :client:clientB:ui:compileCommonMainKotlinMetadata
.\gradlew.bat :feature:login:ui-common:allTests
.\gradlew.bat :feature:home:ui-common:allTests
.\gradlew.bat :feature:player:ui-common:allTests
.\gradlew.bat :app:compileTmdbDebugKotlin
.\gradlew.bat :app:compileClientBDebugKotlin
rg -n "^import (android|java)\.|androidx\.tv|androidx\.compose\.ui\.res|@StringRes|@DrawableRes" core/ui/src/commonMain client/*/ui/src/commonMain feature/*/ui-common/src/commonMain -g "*.kt"
```

On Windows, run the final searches per concrete directory if shell glob expansion is unavailable.

## Test Scenarios

- Every `AppError` maps to the same visible copy/action/dismissibility as before.
- Provider-specific authentication errors override the default mapper; all others delegate.
- Every known TMDB/ClientB avatar ID resolves to its expected Compose resource; unknown IDs use the existing color fallback.
- Login, profiles, home, search, details, library, and player ViewModel state/effect tests remain unchanged semantically.
- Trailer validation accepts supported HTTPS URLs and reports an app error for malformed/non-HTTPS URLs.
- Search normalization is locale-invariant.
- Home tracing calls the no-op implementation safely in common tests and the Android implementation in benchmark variants.
- Common previews render without DI/backend/network requirements.

## Acceptance Criteria

- Every converted UI module compiles as common metadata and Android.
- Android mobile/tablet/TV modules compile against the new resource contracts.
- Shared resource lookups use generated Compose Resources, not Android `R` IDs.
- No `android.*`, `java.*`, TV Material, Android resource API, or Hilt dependency exists in common presentation/UI sources.
- Visual and behavioral Android output matches the KMP-00 baseline.

## Handoff Checklist

- [ ] Converted modules/source sets listed.
- [ ] Resource migration inventory included.
- [ ] Error/avatar API call sites summarized.
- [ ] Tracing injection behavior documented.
- [ ] Common tests/previews and both app compilation results included.
- [ ] Any Android-only code left in `androidMain` justified.
- [ ] Final working tree is clean after committing this ticket.

