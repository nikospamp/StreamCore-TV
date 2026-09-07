# Core UI component audit

Reviewed 2026-09-07 against the current working tree, including the user's uncommitted changes.
The target architecture remains backend-agnostic.

## Scope and result

All 44 Kotlin files in these directories were reviewed: 38 common and 6 Android-specific files.
Public item composables, internal rendering helpers, enums, and test-tag support were included.

- Common directory: `core/ui/src/commonMain/kotlin/com/pampoukidis/streamcoretv/core/ui/components`
- Android directory: `core/ui/src/androidMain/kotlin/com/pampoukidis/streamcoretv/core/ui/components`

Implementations, imports, declared dependencies, previews, and Android/shared production call sites were inspected.
Definitions, imports alone, previews, and test fixtures were not counted as production use. Consumer examples below are representative, not counts.
This is not a web UI audit or a repository-wide external API removal guarantee.

No whole component file was found to be unnecessary. Every public component has an Android/shared production caller; internal helpers and enums
also have live consumers. One test-tag helper has only a test-fixture caller in the audited scope. Several naming and rendering cleanups are justified.

`commonMain` expresses compilation portability, not suitability for every input model. Portable mobile/tablet renderers can remain common while
retaining explicit interaction ownership. TV Material implementations require Android dependencies. A pure Kotlin type or portable TV rendering
implementation can technically move, but moving it without a shared consumer does not create useful reuse.

No production code was changed for this audit. No builds, tests, screenshots, or device actions were performed.

## Recommended cleanup

1. **Align the artwork button API with its file and purpose.** `StreamCoreArtworkIconButton.kt` declares `StreamCoreIconButton` at line 22.
   Its circular, translucent artwork surface is more specific than that name implies. Prefer `StreamCoreArtworkIconButton` for the public function,
   updating callers or retaining a compatibility delegate as appropriate. Keep it separate from the TV input icon control and the close action.
2. **Give independently reusable public composables their own files.** `StreamCoreBrandMark` in `StreamCoreBrowseTopBar.kt:98` is called independently
   by tablet and TV navigation. Move it and its preview to `StreamCoreBrandMark.kt`. For consistent application of the repository's file naming rule,
   also separate `StreamCoreFloatingBottomNavigationItem` and `StreamCoreTabletNavigationRailItem`; their current names and APIs are appropriate.
3. **Centralize glyph rendering while retaining semantic wrappers.** Add, Back, Check, Close, Edit, History, Info, Refresh, and Search repeat the
   `Icon`/resource/tint implementation already available in `StreamCoreVectorIcon`. Delegate to the internal helper while preserving 18dp versus
   24dp defaults, colors, and caller modifier sizing. Play uses `Image` plus `ColorFilter` and is another candidate for the same consolidation.
   Named wrappers remain useful app-owned asset APIs; they are not redundant merely because their bodies are small.
4. **Replace the duplicate standard profile glyph implementation.** `StreamCoreBrowseTopBar.kt:129` manually draws `ProfileGlyph` using Canvas,
   despite the existing `StreamCorePersonIcon` and vector resource. Use the app-owned vector path when this surface is next revised. This changes
   the glyph artwork, so it must be included in the user's mobile visual review rather than slipped into a behavior-preserving rename.
5. **Share the artwork scrim.** `StreamCoreMobileArtworkScrim.kt:12` and the TV card's private scrim at `StreamCoreTvContentCard.kt:188` implement
   the same vertical gradient. Generalize the internal common helper to `StreamCoreArtworkScrim`, preserving the callers' ending alpha values
   (mobile 0.76, TV 0.82) and modifiers. Keep card dimensions and interaction outside the helper.
6. **Clarify support ownership and previews.** Keep the rail's production test tag, but move the test-only `destination()` helper out of the public
   production API when its test fixture is next maintained. Add a preview or documented preview exemption for the internal vector/scrim helpers.
   The reusable TV content card currently uses the screen-level `@PreviewTV`; use the reusable-component preview convention for it.
7. **Document TV variant semantics.** `Standard` and `Primary` have identical color roles but different elevation behavior. Document that distinction
   before considering a rename or collapse. They are not behaviorally identical aliases.

Optional later extractions: the button label/icon/loading body and playback-progress rendering. Button renderers must retain their own padding,
height/alignment behavior, and content-color handling. Progress extraction must preserve clamping, distinct track alpha values, mobile semantics,
and caller-owned shared-transition modifiers. These are smaller gains than the scrim and icon consolidation.

## Common controls and infrastructure — 10 files

All filenames in this section are relative to the common directory above.

| File | Purpose / production evidence | Placement and naming verdict |
|---|---|---|
| `ErrorHost.kt` | Application-level conditional error dialog; called by `MainActivity`. | Keep common. `ErrorHost` accurately describes hosting nullable error presentation; a prefix-only rename would be cosmetic. TV action rendering can be reviewed with that dialog surface. |
| `StreamCoreArtworkIconButton.kt` | Circular translucent artwork action with loading semantics; mobile Details, Player, and player settings. | Keep common implementation and its distinct role. Rename public `StreamCoreIconButton` to match the artwork-specific file/purpose. |
| `StreamCoreButton.kt` | Filled action with size/variant/loading behavior; shared LoginForm, mobile/tablet Home, Details, Search, Profiles, Player, and logout. | Keep common and keep name. Share semantic styling/content with TV where useful; retain separate native shells. |
| `StreamCoreButtonSize.kt` | Standard form and Compact artwork action sizes; default Standard plus explicit Compact in mobile/tablet Home. | Keep common enum beside its owning control. Both values are used and named appropriately. |
| `StreamCoreButtonVariant.kt` | Primary/Secondary semantic fills; default Primary and explicit Secondary in tablet Details, Profiles, and logout. | Keep common enum. Both values are used; no justification for importing TV-specific variants into this contract. |
| `StreamCoreCloseButton.kt` | Close action centralizing icon, target, enabled state, and accessible name; mobile/tablet Search and mobile profile editing/avatar picker. | Keep common. Distinct action wrapper, not a duplicate glyph or artwork-overlay button. Name correct. |
| `StreamCoreContentImage.kt` | Coil image request, crossfade, fallback rendering, and overlay slot; cards and mobile/tablet/TV content screens. | Keep common. Uses portable Coil APIs; shared visual foundation with an accurate name. |
| `StreamCoreLoadingChip.kt` | Noninteractive loading indicator and label; tablet/TV Details. | Keep common. Centralizes a status treatment, distinct from a button's inline loading content. Name correct. |
| `StreamCoreSettingsSwitchRow.kt` | Whole-row toggle semantics, label/supporting text, and non-duplicated Switch action; mobile profile editor. | Keep common reusable control. A single present consumer is sufficient for centralized styling/semantics. Name describes the settings-row role. |
| `StreamCoreTextButton.kt` | Text action used across forms, screens, dialogs, and ErrorHost; internal defaults also consumed by TV text button. | Keep common. Public name correct; shared defaults already remove styling duplication. |

## Common glyphs — 18 files

All filenames are relative to the common directory. All glyphs are decorative by default; parent actions supply accessible names.
All 17 public wrappers have private themed previews. `StreamCoreVectorIcon` is internal and needs its preview coverage documented or an own preview.

| File | Representative production consumer / purpose | Verdict |
|---|---|---|
| `StreamCoreAddIcon.kt` | Mobile and TV add-profile tiles; 18dp add glyph. | Keep common/name; delegate repeated rendering to VectorIcon. |
| `StreamCoreBackIcon.kt` | Mobile Details/player settings/Player and TV Player; mirrored back asset. | Keep common/name; delegate repeated rendering, preserving 18dp. |
| `StreamCoreBookmarkIcon.kt` | Mobile/tablet/TV Details and Library; filled/outline state mapping. | Keep common/name; already uses VectorIcon. |
| `StreamCoreCheckIcon.kt` | Mobile player settings and avatar picker; selection check. | Keep common/name; delegate repeated rendering, preserving 18dp. |
| `StreamCoreCloseIcon.kt` | CloseButton, used by Search and profile editing. | Keep common/name; delegate repeated rendering, preserving 18dp. |
| `StreamCoreEditIcon.kt` | Mobile/TV profile tiles and mobile profile editor. | Keep common/name; delegate repeated rendering, preserving 18dp. |
| `StreamCoreHeartIcon.kt` | Mobile/tablet/TV Details and Library; filled/outline state mapping. | Keep common/name; already uses VectorIcon. |
| `StreamCoreHistoryIcon.kt` | Mobile/tablet/TV Library and mobile/tablet Search. | Keep common/name; delegate repeated rendering, preserving 18dp. |
| `StreamCoreHomeIcon.kt` | All three Android navigation surfaces. | Keep common/name; already uses VectorIcon. |
| `StreamCoreInfoIcon.kt` | Mobile/tablet/TV Home details actions. | Keep common/name; delegate repeated rendering, preserving 18dp. |
| `StreamCoreLibraryIcon.kt` | All three Android navigation surfaces. | Keep common/name; already uses VectorIcon. |
| `StreamCorePersonIcon.kt` | Mobile/tablet Library headers and TV navigation. | Keep common/name; already uses VectorIcon. Reuse for the separate manually drawn profile glyph. |
| `StreamCorePlayIcon.kt` | Mobile/tablet/TV Details and mobile/TV Player. | Keep common/name; consider replacing its separate Image/tint path with VectorIcon while preserving intrinsic/caller sizing. |
| `StreamCoreRefreshIcon.kt` | Mobile Details refresh action. | Keep common/name; one caller still justifies the named asset API. Delegate repeated rendering, preserving 18dp. |
| `StreamCoreSearchIcon.kt` | Three navigation surfaces, search fields, and browse top bar. | Keep common/name; delegate repeated rendering, preserving 24dp. |
| `StreamCoreShareIcon.kt` | Mobile Details share action. | Keep common/name; already uses VectorIcon. |
| `StreamCoreTrailerIcon.kt` | Mobile/tablet/TV Details trailer actions. | Keep common/name; already uses VectorIcon. |
| `StreamCoreVectorIcon.kt` | Internal resource/tint/size renderer used by seven glyph wrappers. | Keep common/internal/name. Extend its reuse; preserve existing wrapper defaults. |

## Common content and navigation — 10 files

All filenames are relative to the common directory. Portable touch renderers retain mobile/tablet UX ownership; their KMP source placement does not
claim that they are TV-ready.

| File | Purpose / production evidence | Placement and naming verdict |
|---|---|---|
| `StreamCoreBrowseTopBar.kt` | Touch browsing header used by mobile/tablet Home. Also contains independently used BrandMark. | Keep common top bar/name. Move BrandMark to its own file; reuse the standard vector for the private profile glyph. |
| `StreamCoreFloatingBottomNavigation.kt` | App mobile navigation shell plus `RowScope.StreamCoreFloatingBottomNavigationItem`. | Keep both common APIs and names; do not merge with rail/drawer. Move the public item API to its own file for consistent file ownership. |
| `StreamCoreTabletNavigationRail.kt` | App tablet navigation shell plus `ColumnScope.StreamCoreTabletNavigationRailItem`. | Keep common APIs and explicit tablet ownership; no Android-only dependency. Move the public item API to its own file for consistent file ownership. |
| `StreamCoreTabletNavigationTestTags.kt` | Rail constant applied by core rail; `destination()` only used by its device-test fixture in the audited scope. | Keep the production hook. Not a composable; prefer testing/support ownership and move the fixture-only method out of public production API. |
| `StreamCorePagerCarousel.kt` | HorizontalPager with press/drag pause and lifecycle-aware advance; mobile/tablet Home. | Keep common and separate from TV carousel. Name is accurate; touch-oriented KDoc/package is sufficient. Shared auto-advance constant is also used by TV and must be retained. |
| `StreamCoreCarouselIndicator.kt` | Shared page indicator used by both touch pager and TV carousel. | Keep common/name; already the correct shared rendering boundary. |
| `StreamCoreMobilePosterCard.kt` | Mobile Library poster shelves, with mobile dimensions/click behavior and shared transitions. | Keep common portable mobile renderer and name; retain distinct TV shell. |
| `StreamCoreMobileContinueWatchingCard.kt` | Mobile Library landscape card with progress and shared transitions. | Keep common portable mobile renderer and name. Distinct role from poster card; progress rendering is a possible internal shared extraction. |
| `StreamCoreMobileArtworkScrim.kt` | Internal gradient used by both mobile cards; equivalent gradient exists in TV card. | Generalize within commonMain to internal StreamCoreArtworkScrim with caller-owned end alpha. This rendering has no mobile input assumption. |
| `StreamCoreProfileArtwork.kt` | Profile tiles/editor, library/browse headers, and TV navigation; common avatar model plus artwork resolver. | Keep common/name. Avatar resolution/fallback policy justifies its API alongside generic ContentImage. |

Additional public declarations: BrandMark is used directly by app tablet and TV navigation; the two scoped navigation item composables are used by
their corresponding app navigation adapters. All three are needed. The internal shared carousel interval is needed by both renderers; if files are
reorganized by family, place/document it as shared configuration rather than deleting it with the touch implementation.

## Android components — 6 files

All filenames are relative to the Android directory above.

| File | Purpose / production evidence | Can it be common? / name verdict |
|---|---|---|
| `StreamCoreTvButton.kt` | Native TV action across TV features and app logout; focus/pressed borders, elevation, alignment, loading. | Keep Android: TV Material dependency. Name correct. Common tokens already shared; an internal common content body is optional. |
| `StreamCoreTvButtonVariant.kt` | TV-specific button contract used implicitly and explicitly across TV features. | Technically portable enum, but keep beside its owning TV API; no common consumer needs it. Type/file name correct; document Standard versus Primary elevation. |
| `StreamCoreTvCarousel.kt` | Native TV Carousel, lifecycle/auto-advance and directional focus containment; TV Home. | Keep Android: TV Material dependency. Name correct; retain separate touch/TV controllers and shared indicator. |
| `StreamCoreTvContentCard.kt` | Focus-aware content card supporting several row types; TV Home, Library, Search. | Production code is technically portable; Android PreviewTV is not. Keep TV interaction/layout ownership and extract neutral scrim/progress pieces. Name correct; do not force mobile cards into this multi-layout TV shell. |
| `StreamCoreTvIconButton.kt` | Native TV icon action with inset focus treatment; login visibility slot. | Keep Android: TV Material dependency. Generic content slot makes name appropriate; separate role from artwork and close controls. |
| `StreamCoreTvTextButton.kt` | Native TV text action; login secondary-control slot. | Keep Android: TV Material dependency. Name correct; shared text defaults already cover styling reuse. |

## Follow-through boundaries

- Preserve existing public APIs where deferred consumers require compatibility; migrate all relevant callers before removal.
- Preserve the current mobile visual baseline, icon sizes, TV focus, and caller-owned layout values during cleanup.
- Avoid new platform flags, duplicate provider-facing models, or a module split solely to rearrange directories.
- No runtime performance improvement is claimed. Shared helper extraction is primarily a consistency/maintenance improvement; it should not add
  state holders, interaction collectors, or long-lived scopes.
- Deferred manual/regression checks for any later implementation: glyph tint/size and RTL, focused/disabled/loading controls, card scrim/progress,
  shared transitions, and preview availability. Existing user instructions defer automated tests and screenshots by the agent.
