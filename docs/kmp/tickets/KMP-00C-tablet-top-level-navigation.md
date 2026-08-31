# KMP-00C — Add Tablet Search, Library, and Top-Level Navigation

## Goal

Make Search and Library reachable and fully functional on Android tablet through an adaptive, app-owned top-level navigation surface.

## Context

KMP-00 device evidence shows tablet Home loads and refreshes, but `MobileNavigationBar` is gated to `Platform.Mobile` and `TvNavigationDrawer` to
`Platform.Tv`. The project also has no `:feature:search:ui-tablet` or `:feature:library:ui-tablet` modules, so tablet Search and Library journeys are
currently unreachable.

## Dependencies and Parallelization

- **Depends on:** KMP-00B.
- **Blocks:** KMP-00G and final KMP-00 acceptance.
- **Parallelization:** Yes, with KMP-00D, KMP-00E, and KMP-00F after KMP-00B. This ticket owns the tablet Search/Library modules and tablet navigation
  component; the integration owner owns `settings.gradle.kts`, app dependencies, and `StreamCoreNavHost.kt`.

## In Scope

- Tablet top-level navigation for Home, Search, and Library.
- New `:feature:search:ui-tablet` and `:feature:library:ui-tablet` modules.
- Tablet-specific routes/screens, previews, focus/selection semantics, and instrumentation tests.
- App/settings dependency wiring and design-system support required by the navigation surface.

## Non-Goals

- No changes to mobile bottom navigation or TV drawer behavior.
- No reuse of mobile screens through `isTablet` flags.
- No search/library domain or provider behavior changes, KMP plugins, DI migration, or visual redesign outside tablet parity.

## Implementation Tasks

1. Add the two tablet UI modules to `settings.gradle.kts` and app dependencies using existing common/domain/data contracts only.
2. Add `TabletSearchRoute`/`TabletSearchScreen` and `TabletLibraryRoute`/`TabletLibraryScreen` using the established route/stateless-screen split.
3. Preserve all existing Search and Library actions, effects, stable lazy keys/content types, profile isolation, selected-content key handling, and error
   presentation.
4. Build an app-owned tablet top-level navigation rail/panel with Home, Search, and Library. Raw Material navigation primitives belong behind a
   reusable `:core:ui` component with a standard preview.
5. Show the tablet navigation surface only for `Platform.Tablet` top-level destinations; hide it on Login, Profiles, editor, Details, and Player.
6. Use `navigateToTopLevel` with saved/restored destination state and clear only intentionally transient selected-content state on destination switch.
7. Keep profile switching reachable from Home and Library and preserve the currently selected profile ID across top-level destinations.
8. Add backend-free `@PreviewTablet` previews for loading/content/empty/error states and long text where relevant.
9. Add Compose tests for destination order, selected state, Search discovery/query/recents/results, Library empty/content/mutation state, and Back/state
   restoration.

## Public API or Type Changes

- New tablet feature modules and public route/screen composables.
- New app-owned/design-system tablet navigation composable.
- No domain/repository/provider API changes.

## Verification Commands

```powershell
.\gradlew.bat :feature:search:ui-tablet:compileDebugKotlin
.\gradlew.bat :feature:search:ui-tablet:compileDebugAndroidTestKotlin
.\gradlew.bat :feature:library:ui-tablet:compileDebugKotlin
.\gradlew.bat :feature:library:ui-tablet:compileDebugAndroidTestKotlin
.\gradlew.bat :feature:search:ui-common:testDebugUnitTest
.\gradlew.bat :feature:library:ui-common:testDebugUnitTest
.\gradlew.bat :app:compileTmdbDebugKotlin
.\gradlew.bat :app:compileClientBDebugKotlin
.\gradlew.bat verifyDesignTokensLogFiles --console=plain
.\gradlew.bat check -PverifyDesignTokensLogFiles=true --continue --console=plain
```

## Required Device Journeys

- `Medium_Tablet`: Home -> Search -> query -> recent search -> first result -> Back -> selected result/state restoration.
- `Medium_Tablet`: Search -> Library -> empty/content sections -> Details -> Like/My List mutation -> Back -> Library reflects changes.
- Switch repeatedly between Home/Search/Library and verify destination state, profile identity, no duplicate destinations, and adaptive landscape layout.
- Run both TMDB and ClientB graphs; use the approved TMDB opaque-session procedure if required.

## Acceptance Criteria

- Home, Search, and Library are visibly reachable on tablet with correct selected semantics.
- All tablet Search and Library reference journeys pass without mobile boolean flags or provider leakage.
- New reusable/screen composables have the required previews and focused Compose tests.
- Both provider app graphs compile, verifier count increases by the expected new production files, and root `check` remains green.

## Handoff Checklist

- [ ] New modules and app dependency edges recorded.
- [ ] Navigation/state-restoration behavior documented.
- [ ] Tablet screenshots and journey outcomes included.
- [ ] Added/executed tests and verifier file-count delta reported.
- [ ] No mobile/TV navigation regression.
- [ ] No unrelated files modified.
