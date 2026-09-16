# Web / TV UI harmonization

The current Android TV screens are the visual reference. Web adopts their composition, typography, artwork, spacing, and controls while retaining browser input and navigation. The target architecture remains **backend-agnostic**: shared rendering consumes app models and presentation contracts, never provider DTOs or SDKs.

## Source audit

| Surface | Shared rendering and TV alignment | Browser ownership |
| --- | --- | --- |
| Login | `LoginBackground` (Landscape), `LoginHeader`, shared validation strings and state; TV-sized left panel and compact form. | `WebLoginCredentialForm` keeps native HTML autofill, password visibility, validation, and submit behavior. Forgot password, Create account, and Help remain intentionally disabled while their browser flows are unavailable. |
| Profiles / manage | Shared `ProfilesUiState`, actions and ViewModel; `StreamCoreProfileArtwork`, `StreamCoreAddIcon`, `StreamCoreEditIcon`; dimensions follow `StreamCoreDimens.Tv.Profiles`. | Selection/manage layouts and pointer/keyboard activation use `StreamCoreWebActionSurface`; focus restoration and management controls remain web-owned. |
| Profile editor | `ProfileEditorContent`, `ProfileEditorLayout`, `ProfileEditorModifiers`, `ProfilesBackdrop`, and `StreamCoreSettingsSwitchRow`. New display-name/error slots preserve the previous Android defaults. | Native name field and `WebProfileEditorActionButton`; Save commits the current DOM name. Native delete confirmation retains browser modal behavior. |
| Avatar picker | `AvatarPickerContent` / `AvatarPickerLayout`, shared artwork and selection badges. | `WebAvatarPickerOverlay` stays in the existing Compose viewport, contains focus, dismisses on Escape/outside click, and restores avatar focus. Background editor semantics and native controls are hidden while it is open. |
| Home | `HomeHeroArtwork` shares artwork and horizontal/vertical scrims; `HomeHeroContent` shares label, title, metadata, synopsis, and action slot. `StreamCoreCarouselIndicator` and `StreamCoreWebMediaCard` replace duplicate rendering. The header is removed; hero artwork extends behind the rail. | Responsive copy width, compact Details action, arrow/pointer carousel controls, lazy shelf scrolling, and return-focus keys. Narrow layouts reserve space for carousel controls. |
| Search | TV heading, field width, recent-query chips, landscape trending cards, poster results, and inline offline notice. Uses `StreamCoreSearchIcon` and `StreamCoreWebMediaCard`. | Native HTML editing preserves composition, paste, committed submit, and Escape behavior. Transparent input and an inset HTML rectangle preserve the Compose-painted focus border. Per-query removal remains available through a compact close control. |
| Library | Preserves the actual TV hierarchy: Continue Watching, Liked, and My List shelves. Shared media cards, compact empty/error bands, TV card sizes and loading geometry. | Browser scrolling, retry, directional focus, and return-focus restoration. No invented tabs or data placeholders. |
| Details | `DetailsPanoramaHero`, `DetailsActionContent`, `DetailsSynopsis`, `DetailsCast`, `DetailsRecommendationArtwork`; shared bookmark/play/heart/trailer icons. Two-column reading band collapses at narrower widths. | Back control, pointer/keyboard actions, capability-dependent availability, overflow-dependent synopsis/cast expansion, recommendations and focus restoration. |
| Player / settings / error | `PlayerControlIcon`, `PlayerTimelineTrack`, `PlayerSettingsHeaderContent`, `PlayerSettingsIconContainer`, `PlayerSettingsNavigationContent`, `PlayerSettingsSelectionContent`, `PlayerSettingsSpeedOptions`. TV-style artwork controls and side settings panel. | HTML playback surface, timeline gestures/keyboard seeking, document-level control reveal, focus containment, retry/back overlays, browser fullscreen and Escape handling. |
| Shell | `StreamCoreWebNavigationRail` uses shared brand/navigation/profile artwork, TV rail geometry and `navigationContainer` colors. Shared `StreamCoreControlDefaults` styles Compose and native HTML controls. | Rail hover/focus expansion, Tab/arrows, browser history and stored return-focus keys. Non-Home browse surfaces reserve the collapsed rail; Home remains fullbleed. |

The new `StreamCoreContentCardArtwork` leaf is used by both TV and `StreamCoreWebMediaCard`: title overlay, rank, and playback progress now have one implementation. Both use `StreamCoreSharedArtworkImage`. TV retains its original Surface, focus border, dimensions, metadata, and shared-title modifier order. Home's TV wrapper likewise retains the 62% copy width and overlay-before-padding order.

Android render defaults are preserved by additive slots/parameters and unchanged platform wrappers. These source-level invariants still require the visual regressions below. Browser action surfaces own hover, focus, clipping and activation; drawing leaves do not acquire browser or Android input assumptions.

## Browser additions and lifecycle

Details provides an explicit Back control alongside browser history. Player fullscreen uses the browser Fullscreen API; Escape closes a player overlay first, then exits fullscreen, then navigates back. Fullscreen exits when the owning route leaves. HTML input listeners, document listeners, and pending paste timers are removed by their owning disposal/release hooks. No additional ViewModel scopes or long-lived feature flows were introduced by the rendering extraction.

## Verification snapshot

The final production Wasm distribution, design-token gate, and release-artifact validation passed. Both Android application flavors and all seven
`ui-web` Android targets compiled; KMP test-target/compiler-flag/convention/dependency gates and authenticated TMDB runtime-config verification
passed. The release artifact contains 67 assets and two Wasm files, with no real runtime configuration packaged. Logs are in `build/web-tv-review/`.

Authenticated browser review covered Login, Profiles/manage/add/edit/avatar, Home, Search/discovery/results, Details, Library empty sections, and
real playback with controls/settings/quality pages. Browser Back and direct-route reloads were exercised. The avatar's final production overlay
closes on one Escape and restores editor accessibility nodes. Player settings preserve the video, restore player accessibility after dismissal,
and restore the gear's keyboard activation (Space reopens settings). Account/profile edits were left unsaved. A separate Compose dialog viewport
had stranded accessibility nodes; the avatar and player overlays now remain in the main viewport instead.
Home and Search were also inspected at narrower browser widths (685px and 800px); the rail, hero actions, native field outline, and shelves remain usable.

Deferred regression coverage:

- Android mobile/tablet/TV appearance, shared artwork transitions, focus-ring visibility at scroll edges, and Details return focus.
- Browser profile selection/manage/delete/save, avatar open-close-reopen and focus restoration, native autofill, IME/paste, validation, and modal/background isolation after a fresh reload.
- Narrow windows, long localized copy, larger text, keyboard-only traversal, history Back/Forward, and every loading/empty/offline/error state with real and fixture data.
- Player startup, buffering, seeking, auto-hide/reveal, settings navigation/tracks/resize/speed, errors/retry, fullscreen entry/exit, Escape ordering, route disposal, and media capabilities.
- Cross-browser behavior, accessibility-tree/screen-reader output, reduced-motion behavior, and release performance measurements.

No automated regression suite or runtime frame-rate measurement is claimed by this source audit. Successful compilation and the observed browser journeys do not establish 60fps behavior or cross-browser coverage.

## Library review

Keep Compose Multiplatform **1.12.0 stable** and Compose Material3 **1.9.0 stable** for this UI change. Material3 has an independent version line; do not infer its version from Compose Multiplatform. See the [Compose Multiplatform 1.12 release](https://blog.jetbrains.com/kotlin/2026/08/compose-multiplatform-1-12-0/).

Current catalogue entries remain Coil **3.4.0** and Kotlin **2.3.21**. Separate migration candidates:

- **Coil 3.6.2:** includes the JS/Wasm decoder memory-leak fix introduced in 3.6.0 plus subsequent cancellation/linkage fixes. Review changed Kotlin/Skiko/Okio/Compose transitives through the dependency gate. [Official changelog](https://coil-kt.github.io/coil/changelog/), [decoder fix](https://github.com/coil-kt/coil/pull/3503).
- **Kotlin 2.4.20:** review Wasm output improvements and interop/runtime migration requirements, then rerun compatibility and production-build gates. No project-specific size or speed improvement has been measured. [Release and migration notes](https://kotlinlang.org/docs/whatsnew2420.html), [toolchain compatibility](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html).

Neither migration is bundled into visual harmonization.

## Local review

The development server binds to loopback and serves generated runtime assets/configuration only, never the repository root. Direct routes use
SPA fallback. For a fixed production review, build `:webApp:wasmJsBrowserDistribution`, then run `node webApp/e2e/server.mjs` with
`STREAMCORE_WEB_PORT=8080` and `STREAMCORE_WEB_RUNTIME_CONFIG` pointing to the absolute generated development `config.json` outside the distribution.
Do not run a production webpack build while reviewing the development server: both compiler sync tasks stage files in the same Kotlin npm package
directory. Finish the production build first and serve its completed distribution. Screenshots and the local review gallery are in
`build/web-tv-review/`; no credentials are included in the screenshots or gallery.
