# KMP-01 — Replace Hilt with Koin While Remaining Android-Only

## Goal

Remove Android-only Hilt/Dagger coupling before KMP source-set migration, while preserving both Android client graphs and all ViewModel lifecycles.

## Context

Hilt annotations currently cross provider, data, feature, UI-common, application, and ViewModel boundaries. KMP shared code cannot retain that graph.
Migrating DI before moving source files isolates DI regressions from KMP/Gradle regressions.

Koin is locked to version `4.2.2`. Use the classic constructor DSL; do not adopt the Koin compiler plugin or annotation processing.

## Dependencies and Parallelization

- **Depends on:** KMP-00.
- **Blocks:** KMP-02.
- **Parallelization:** None. This ticket touches the application composition root and most module graphs.

## In Scope

- Application, provider, repository, use-case, UI mapper/resolver, playback, and ViewModel definitions.
- TMDB and ClientB flavor-specific Koin roots.
- Koin graph-verification tests.
- Removal of Hilt/Dagger/KSP dependencies that are used only for Hilt.

## Non-Goals

- No KMP plugins or source-set moves.
- No repository, UI-state, navigation, persistence, or playback behavior redesign.
- No Koin annotations, compiler plugin, dynamic module loading, or lazy modules.
- No service-locator access in repositories, use cases, ViewModels, or stateless screens.

## Implementation Tasks

1. Add version-catalog aliases for Koin core, Android, Compose, and Compose ViewModel artifacts at `4.2.2`.
2. Define constructor-based Koin modules beside their owning feature/provider boundary:
    - Repository implementations are `single` unless their current lifecycle is shorter.
    - Use cases are `factory` unless they intentionally retain state.
    - ViewModels use `viewModelOf` or explicit `viewModel { ... }` when parameters are required.
    - Playback sessions remain factory-created; never register a playback session singleton.
3. Replace stringly duplicated qualifiers with shared qualifier constants owned by the module exposing the contract.
4. Start Koin once in `StreamCoreApplication` with Android context and the common Android module list.
5. Add same-signature flavor source-set functions under `app/src/tmdb` and `app/src/clientB` that return their provider modules. Only the selected
   flavor's providers may be on the graph.
6. Remove `@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`, `@Inject`, `@Module`, `@Provides`, `@Binds`, `@InstallIn`, Hilt qualifiers, and
   generated Hilt integrations.
7. Replace route-level `hiltViewModel()` defaults with `koinViewModel()`. Preserve explicit default parameters so previews/tests can pass fake
   ViewModels or state holders.
8. Remove Hilt/Dagger libraries, plugins, compilers, and KSP where KSP has no remaining consumer.
9. Add graph tests that create and verify TMDB and ClientB graphs independently, including every ViewModel factory and qualified DataStore/network
   binding.
10. Ensure graph teardown occurs after each test so Koin global state cannot leak between tests.

## Public API or Type Changes

- Constructors lose DI annotations but retain the same parameters and visibility.
- Route defaults change from `hiltViewModel()` to `koinViewModel()`.
- Each feature/provider exposes an internal Koin `Module` value or module factory to its platform composition root.
- No business/domain API changes are authorized.

## Verification Commands

```powershell
.\gradlew.bat :app:compileTmdbDebugKotlin
.\gradlew.bat :app:compileClientBDebugKotlin
.\gradlew.bat :app:assembleTmdbDebug
.\gradlew.bat :app:assembleClientBDebug
.\gradlew.bat test
rg -n "dagger\.|hiltViewModel|HiltViewModel|HiltAndroidApp|AndroidEntryPoint|javax\.inject" app core client feature playback -g "*.kt" -g "*.kts"
```

The final `rg` command must return no production DI usage. Test fixtures may mention old names only when explicitly testing a migration compatibility
layer, which is not expected.

## Test Scenarios

- TMDB graph starts and resolves app auth, every feature ViewModel, provider repositories, UI resolvers/mappers, playback source, Media3 factory, and
  persistence repositories.
- ClientB graph resolves the equivalent contracts without loading TMDB definitions.
- ViewModels remain scoped to their navigation owners and are recreated after the owner is destroyed.
- Parameterized details/profile/player destinations receive the same runtime inputs as before.
- Playback sessions are distinct per factory request and are closed with their owning ViewModel.

## Acceptance Criteria

- Both flavors build and complete the KMP-00 smoke journeys.
- Both Koin graphs verify without overrides or missing definitions.
- No production Hilt/Dagger/`javax.inject` usage remains.
- Koin lookup is restricted to application/route composition boundaries.
- No unrelated behavior or architecture changed.

## Handoff Checklist

- [ ] Koin modules listed by owner/module.
- [ ] Singleton/factory/ViewModel lifecycle decisions summarized.
- [ ] Both graph-test results included.
- [ ] Both Android flavor build results included.
- [ ] Removal search result included.
- [ ] Known Koin limitations documented.
- [ ] No unrelated files changed.
- [ ] Final working tree is clean after committing this ticket.

