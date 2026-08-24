# Mobile Library Implementation Specification

Status: implementation contract
Target: Android mobile, Jetpack Compose, Material 3
Architecture: backend-agnostic Clean Architecture with UDF
Visual register: premium, cinematic, calm, content-led

## 1. Outcome

Library is the profile-scoped destination for content the subscriber is watching or has intentionally saved. It contains Continue Watching, Liked,
and My List in one vertically scrolling surface. Like and My List are real actions on Details. Trailer and Share reserve their final UI positions but
remain disabled until their product behaviors exist.

The supplied Netflix screens are pattern references, not layouts or branding to clone. StreamCore keeps its own typography, smoky system-aware
palette, ember selection accent, restrained shapes, and backend-neutral naming.

## 2. Product decisions

- Mobile only. Tablet and TV modules and navigation remain unchanged.
- Home, Search, and Library are the three authenticated mobile top-level destinations.
- Liked and My List persist locally per profile for every client flavor. Provider sync is deferred behind the repository contract.
- A title may be both Liked and in My List and therefore appear in both shelves.
- Library cards open Details. Inline play, remove, overflow, and See All actions are out of scope.
- The app follows the existing system light/dark theme. TV remains dark through the existing theme policy.
- Empty shelves remain visible and explain how they are populated.

## 3. Module boundaries

```text
:core:data
  core/model/library/LibraryEntryModel.kt
  core/model/library/LibraryModel.kt
  core/model/library/ContentLibraryStateModel.kt

:core:domain
  LibraryRepository.kt

:feature:library:data
  PreferencesLibraryRepository.kt
  LibraryPreferences.kt
  LibraryDataModule.kt

:feature:library:domain
  ObserveLibraryUseCase.kt
  ObserveContentLibraryStateUseCase.kt
  SetContentLikedUseCase.kt
  SetContentInMyListUseCase.kt

:feature:library:ui-common
  common/library/LibraryAction.kt
  common/library/LibraryEffect.kt
  common/library/LibraryUiState.kt
  common/library/LibraryViewModel.kt
  common/library/LibraryRouteEventEffect.kt
  common/testing/LibraryPreviewData.kt
  common/testing/LibraryTestTags.kt

:feature:library:ui-mobile
  mobile/library/MobileLibraryRoute.kt
  mobile/library/MobileLibraryScreen.kt
```

Feature UI and domain code consume only app models, repository contracts, actions, effects, and immutable state. Provider DTOs, SDKs, and remote
favorite/watchlist concepts do not cross into these modules.

## 4. Persistence and aggregation

`LibraryRepository` exposes a profile-scoped `Flow<AppResult<List<LibraryEntryModel>>>` and idempotent `setLiked` / `setInMyList` operations with an
explicit change timestamp. One entry owns a compact content snapshot plus independent nullable timestamps for Like and My List. Removing one flag
must preserve the other; the entry is deleted only when both timestamps are null.

The Preferences DataStore file is `library.preferences_pb`. Its JSON and DataStore bindings are qualified. Writes happen in one atomic
`DataStore.edit`. Before storage, snapshots clear row identity and playback progress and retain only list-card metadata. Invalid JSON reports
`AppError.Parsing` and blocks mutation instead of overwriting unreadable data. Local I/O maps to `AppError.Unknown`, never a network error.

`ObserveLibraryUseCase` combines saved entries with `PlaybackProgressRepository`:

- Continue Watching retains playback repository order and receives row `library:continue-watching` plus `PlaybackProgressModel`.
- Liked sorts newest first by `likedAtMillis` and receives row `library:liked`.
- My List sorts newest first by `addedToMyListAtMillis` and receives row `library:my-list`.
- Existing playback persistence remains the source of truth for resume eligibility and removal.

Preferences DataStore is the v1 implementation. If saved collections become large or require remote reconciliation, replace the implementation with
Room or a client sync layer without changing feature UI contracts.

## 5. Library UDF

`LibraryUiState` contains loading/error plus immutable Continue Watching, Liked, and My List lists. `LibraryAction` handles `Load(profileId)`,
`Retry`, and `ContentSelected(content)`. `LibraryEffect` emits content navigation and user-visible errors.

Changing profile cancels the previous collection. Duplicate loads for the same profile are ignored. An initial read failure shows retry content; a
later failure retains the last successful rows. Formatting, sorting, progress mapping, and source-row assignment happen before composition.

## 6. Mobile screen

The root is a `LazyColumn` on `MaterialTheme.colorScheme.background` with sufficient bottom clearance for the floating navigation and system inset.

- Header: status-bar-safe, `20dp` horizontal padding, `Library` in `headlineLarge`, trailing profile switch action with a `48dp` target.
- Major-section spacing: `24dp`; heading-to-row spacing: `12dp`; card spacing: `12dp`.
- Continue Watching: `192dp × 108dp`, `10dp` corners, title/metadata, and an inset `3dp` progress line.
- Liked and My List: `120dp × 180dp`, `10dp` corners, content title presented with the shared mobile poster treatment.
- Empty shelf: heading remains visible with a compact tonal guidance row. It is not a decorative card or full-page onboarding illustration.
- Loading: shelf-shaped tonal skeletons shown only when loading persists long enough to avoid a flash.
- Artwork: `StreamCoreContentImage` owns crop, loading, caching, and fallback.

All lazy items use stable `rowId + contentId` keys and explicit content types. Images do not duplicate the parent card's TalkBack announcement.

## 7. Details saved actions

The Details action cluster sits between Play/Resume and Overview. It contains four equal labeled actions: Like, My List, Trailer, and Share.

- Like and My List are independent toggles with outline and filled official vector assets.
- Selected state uses the primary accent and a state description in addition to color.
- Each field has its own pending flag, optimistic target, coroutine job, and rollback value.
- Observer emissions do not replace a field while that field has a pending optimistic mutation.
- Mutation failure restores the prior value and emits the existing `ShowError` effect.
- Trailer and Share remain visible but disabled, announce `Not available yet`, and dispatch nothing.
- Every target is at least `48dp`; labels use product typography and support two lines.

## 8. Floating mobile navigation

The app shell renders one navigation pill outside the `NavHost` for Home, Search, and Library. Route knowledge stays in `:app`; the visual primitive
stays in `:core:ui`.

- Maximum width `360dp`, `20dp` side margins, `68–72dp` height, and `12dp` above the system navigation inset.
- Three equal `Role.Tab` cells with labels always visible and at least `48dp` touch targets.
- Solid `surfaceContainerHigh` outer pill, neutral `surfaceContainerHighest` moving capsule, selected icon in `primary`, tonal elevation only.
- Indicator movement uses `graphicsLayer.translationX`, handles RTL, and runs for `220ms` with `FastOutSlowInEasing`.
- Icon/color transitions run for `150–180ms`; no bounce, glass, border-plus-shadow, or decorative page-load choreography.
- Disabled system animations snap the indicator and avoid scale motion.

Top-level navigation uses `popUpTo<AppRoute.Home> { saveState = true }`, `restoreState = true`, and `launchSingleTop = true`. Reselecting the active
tab is a no-op. Search loses its visible back arrow and the duplicate Home search action. System Back dismisses the Search IME first and then returns
to Home. Details and Player hide the pill and return to the exact originating destination.

Shared Details transitions retain `contentId + sourceRow` identity. The source row is a primitive route argument; large content snapshots remain
ephemeral.

## 9. Accessibility, localization, and performance

- New visible copy and content descriptions live in Android string resources.
- Verify WCAG-equivalent `4.5:1` text contrast and `3:1` large text/icon contrast in both themes.
- Navigation is a selectable group; selected state, toggle state, loading, and disabled state never rely only on color.
- Verify TalkBack order, RTL indicator direction, `320dp` width, 200% font scale, gesture navigation, three-button navigation, and IME overlap.
- Avoid per-card flow collection, list copying during recomposition, unstable lambdas/models, and animated layout measurement.
- Continue using Coil's artwork caching and the existing shared-element identity policy.

## 10. Required tests and previews

Repository tests cover profile isolation, independent flags, atomic concurrent mutations, timestamp ordering, idempotency, restart persistence,
snapshot sanitation, malformed JSON, and I/O errors.

Use-case and ViewModel tests cover three-section aggregation, progress mapping, source rows, profile switching, retry, live updates, optimistic
success,
rollback, and concurrent Like/My List mutations.

Compose/navigation tests cover shelf order, compact empty guidance, click dispatch, toggle semantics, disabled placeholders, selected-tab semantics,
RTL indicator travel, tab state restoration, back behavior, profile changes, and pill visibility.

Previews and rendered verification cover dark/light content, loading, partial and complete empty data, failed artwork, narrow and wide phones, long
localized labels, large font, reduced motion, and navigation insets. The final Impeccable pass checks hierarchy, rhythm, contrast, motion purpose,
touch targets, allocations, and recomposition behavior.

## 11. Acceptance criteria

- Like and My List survive process restart and remain isolated by profile.
- Details mutations update Library live and roll back correctly on failure.
- Continue Watching stays driven by the existing playback progress repository.
- Home, Search, and Library restore their own UI state and the selected capsule animates correctly.
- Details and Player never display the bottom pill and Back returns to the source tab.
- Trailer and Share are visibly final but correctly disabled.
- Mobile works in system light and dark themes; tablet and TV behavior is unchanged.
- Both TMDB and Client B variants compile without provider-specific Library APIs.
- No provider DTO, API response, SDK, or client capability type enters shared models, domain, or feature UI.
