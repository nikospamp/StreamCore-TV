# Platform feature implementation status

Last source audit: 2026-08-26

This is the living platform-parity register for the backend-agnostic StreamCoreTV app architecture. Mobile is the current behavior baseline. Parity
means an equivalent user outcome; tablet and TV do not need to copy touch-specific interaction or mobile-only behavior such as picture-in-picture.

## Status legend

| Status      | Meaning                                                                                |
|-------------|----------------------------------------------------------------------------------------|
| ✅ Supported | Platform has the screen and the mobile-baseline user outcomes are wired.               |
| 🟡 Partial  | Platform has a screen, but one or more mobile-baseline outcomes are absent or unwired. |
| ❌ Missing   | No platform implementation and/or no reachable platform navigation path exists.        |

`Supported` describes cross-platform parity, not overall product completeness. For example, Forgot password and Help are currently no-op callbacks on
every platform, so they are not recorded as platform gaps.

## Platform matrix

| Surface                      | Mobile | Tablet | TV | Non-mobile gap summary                                                                                                                                                                                                                         |
|------------------------------|:------:|:------:|:--:|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Authenticated app navigation |   ✅    |   ❌    | ❌  | The authenticated Home/Search/Library destination model, saved-state behavior, and visible navigation shell remain mobile-only. Tablet/TV can reach Home after profile selection but have no top-level Search/Library shell.                   |
| Login                        |   ✅    |   ✅    | ✅  | No parity gap found. All variants use the same state/effects and expose submit, password visibility, forgot password, create account, and help.                                                                                                |
| Profile chooser              |   ✅    |   🟡   | 🟡 | Tablet/TV do not render `loadError` or a retry action and use initials instead of the resolved avatar artwork. Selection, add, edit, and delete are available.                                                                                 |
| Create/edit profile          |   ✅    |   🟡   | 🟡 | Tablet/TV can edit the name, avatar, and parental level and can save/cancel, but cannot delete from the editor. Their avatar selector is an ID-label chip list rather than the mobile artwork picker.                                          |
| Home                         |   ✅    |   🟡   | 🟡 | Tablet exposes profile switching but does not receive/render the active profile artwork. TV lacks profile switching, the mobile/tablet featured hero outcome, resumable-only Continue Watching filtering, and playback progress visualization. |
| Search                       |   ✅    |   ❌    | ❌  | Only `:feature:search:ui-mobile` exists, and `AppRoute.Search` always renders `MobileSearchRoute`; tablet/TV have no visible authenticated path to Search.                                                                                     |
| Library / My List            |   ✅    |   ❌    | ❌  | Only `:feature:library:ui-mobile` exists, and `AppRoute.Library` always renders `MobileLibraryRoute`; tablet/TV have no visible authenticated path to Library.                                                                                 |
| Asset details                |   ✅    |   🟡   | 🟡 | Tablet/TV now render Play/Resume plus stateful Like and My List controls with platform-appropriate input behavior. Their routes still discard `DetailsEffect.PlaySelected`, and no platform Player exists.                                     |
| Player                       |   ✅    |   ❌    | ❌  | Only `:feature:player:ui-mobile` exists and `AppRoute.Player` always renders `MobilePlayerRoute`; no tablet/TV playback surface or non-mobile Details-to-Player path exists.                                                                   |

## Missing tablet and TV surfaces

### Search

Tablet and TV Search are missing. Shared backend-agnostic Search state, actions, effects, ViewModel, domain/data layers, and preview data already
exist, but only `:feature:search:ui-mobile` is declared and consumed.

Issue 014 must add dedicated platform routes/screens and an authenticated navigation shell that provides the mobile-baseline outcomes:

- debounced query-as-you-type and explicit submission;
- recent-query selection, individual removal, and clear-all;
- trending discovery and selection;
- loading skeletons, empty results, retryable failure, and cached offline results notice;
- result grid selection into Asset details.

The tablet implementation must remain touch/keyboard adaptive and visually aligned with mobile. TV must add 10-foot composition, deterministic D-pad
traversal, and focus restoration after Details. A platform is not supported until its dedicated route is reachable through the authenticated shell and
its interaction model is verified.

### Library / My List

Tablet and TV Library are missing. Shared backend-agnostic Library state, actions, effects, ViewModel, domain/data layers, and preview data already
exist, but only `:feature:library:ui-mobile` is declared and consumed.

Issue 015 must add dedicated tablet/TV Library routes/screens for Continue Watching, Liked, and My List, including progress, retained-content loading,
retryable failure, per-section empty guidance, profile access, Details dispatch, and selected-content restoration. Reachability depends on the
authenticated shell owned by Issue 014.

### Player

Tablet and TV Player are missing. Shared playback state, actions, effects, settings models, playback sessions, progress persistence, and the
video-surface abstraction exist, but only `:feature:player:ui-mobile` is declared and `AppRoute.Player` always renders `MobilePlayerRoute`.

Issue 013 must add platform-specific routes and controls using the shared player contract:

- play/pause, seek backward/forward, scrub, buffered timeline, and filmstrip preview;
- retry and back handling;
- quality, audio, subtitles, playback speed, and resize mode settings;
- playback progress persistence and lifecycle/foreground state.

Tablet must remain orientation-adaptive and omit PiP for the first implementation. TV must use TV controls with deterministic
initial/focus-restoration behavior, D-pad seek actions, and 10-foot settings UI. Issue 013 also owns forwarding tablet/TV Details play effects; the
complete Details-to-Player journey additionally requires the Play/Resume UI from Issue 016.

## Existing non-mobile parity gaps

### Asset details — tablet and TV

Implemented by Issue 016:

- prominent Play/Resume actions derived from shared resumable progress;
- stateful Like and My List controls with independent pending and unavailable states;
- deterministic TV focus through Play, Like, My List, Back/Refresh, and recommendations, verified on the 1080p TV emulator.

Remaining under Issue 013:

- forwarding `PlaySelected` from tablet/TV Details routes;
- dedicated platform Player routes and surfaces.

Details remains 🟡 until Play/Resume reaches a platform Player.

### Home — TV

Missing compared with mobile/tablet:

- profile chooser access from Home;
- dedicated featured hero/pager with a Details CTA;
- Continue Watching filtering to resumable items;
- playback progress indicator on Continue Watching cards.

TV currently renders every backend row as a generic horizontal row. Featured content remains selectable, so the missing hero is a
presentation/interaction parity gap rather than missing content access.

### Home — tablet

The profile chooser action is wired, but `activeProfile` is not passed to `TabletHomeRoute`, so the top bar cannot show the selected profile artwork
as mobile does.

### Profile chooser — tablet and TV

Missing compared with mobile:

- an explicit load-failure state;
- a retry action that emits `ProfilesAction.Refresh`;
- resolved avatar artwork in profile cards.

The mobile Manage mode is not itself a required parity item: tablet/TV expose Edit and Delete directly on every profile card, which provides the
equivalent outcome.

### Profile editor — tablet and TV

Missing compared with mobile:

- delete-profile request and confirmation from the editor;
- artwork-based avatar browsing/selection.

Name, avatar ID, parental level, save, and cancel remain functional. Delete is still available from the tablet/TV profile chooser.

## Prioritized implementation backlog

| Priority | Work item                                                             | Done when                                                                                                                                                  |
|----------|-----------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------|
| P0       | Implement tablet/TV Player and non-mobile playback wiring (Issue 013) | Dedicated platform players are reachable, and tablet/TV Details effects open the correct Player route.                                                     |
| P0       | Implement tablet/TV Search and authenticated navigation (Issue 014)   | Dedicated polished Search surfaces are reachable through platform shells with saved state, complete states, and verified touch/keyboard or D-pad behavior. |
| P0       | Implement tablet/TV Library (Issue 015)                               | All three polished Library sections and states are reachable, progress is visible, and TV focus/restoration is verified.                                   |
| 🟡       | Add tablet/TV Details Play/Resume, Like, and My List UI (Issue 016)   | The action UI is complete and TV focus is verified; the overall journey remains partial until Issue 013 wires Play into platform Players.                  |
| P1       | Bring TV Home featured and Continue Watching behavior to parity       | Featured interaction is intentional; Continue Watching is filtered and shows progress.                                                                     |
| P2       | Bring tablet/TV profile error and avatar rendering to parity          | Load failure can retry and profile cards render resolved artwork.                                                                                          |
| P2       | Bring tablet/TV profile editor actions to parity                      | Delete confirmation and visual avatar selection are available with touch/D-pad-appropriate controls.                                                       |
| P2       | Pass active-profile identity into tablet Home                         | Tablet top bar displays the selected profile artwork and keeps profile switching wired.                                                                    |

## Audit evidence

The status above was derived from source wiring, not directory names alone:

- module declarations: `settings.gradle.kts`;
- application dependencies: `app/build.gradle.kts`;
- reachable destinations and platform dispatch: `app/src/main/java/com/pampoukidis/streamcoretv/navigation/StreamCoreNavHost.kt`;
- platform routes/screens: `feature/*/ui-{mobile,tablet,tv}/src/main`;
- shared actions, state, effects, and ViewModels: `feature/*/ui-common/src/main`.

When a feature changes, update both the matrix and its backlog row in the same change. A platform becomes ✅ only after the route is reachable,
callbacks/effects are wired, loading/content/empty/error states are handled, and the platform-specific interaction model is verified.
