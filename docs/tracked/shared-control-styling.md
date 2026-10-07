# Shared controls and platform rendering

This guide defines the shared brand/control contract and the boundaries to preserve when changing screens. The target architecture remains
backend-agnostic: shared rendering consumes public SDK models and app-owned presentation state; platform modules own layout, input and navigation.
Use [DESIGN](../../DESIGN.md) for the accepted visual language and [maintenance](maintenance.md) for outstanding acceptance work.

## Public style contract

`StreamCoreControlDefaults.style()` in `:core:ui` resolves immutable `StreamCoreControlStyle` values from the current Material theme.
`LocalStreamCoreControlStyle` permits an override at a composition boundary, derived with `copy` inside `StreamCoreTheme`; it is not a provider palette.

| Role | Default |
| --- | --- |
| `primary` / `onPrimary` | Material primary pair |
| `pressed` / `onPressed` | Material primary-container pair |
| `disabledContainer` / `disabledContent` | onSurface at 10% / onSurfaceVariant at 38% alpha |
| `buttonLabel` | labelLarge: 14sp, weight 500, 20sp line height, 0.1sp tracking |
| `buttonRadius` | 999dp, clamped to renderer bounds |
| `inputLabel` / `inputRadius` | bodyLarge / 6dp |
| `disabledInputOpacity` | 0.55 for native web text fields |

Touch, TV and web filled buttons consume these roles. Secondary, tertiary, selected and destructive fills retain their Material meanings.
Loading disables activation, preserves the supplied accessible action name and uses the resolved content color for the spinner. `StreamCoreTextButton`
has its own text-action contract. Intrinsic loading width and a separate localized loading announcement are not promised by the shared style.

## Renderer ownership

- **Touch:** retain minimum heights, padding, state layers/ripple and default appearance. Sharing web input roles does not replace touch fields.
- **TV:** retain D-pad mechanics, dimensions, scale, elevation and focus clearance. The default filled-button radius is capped at 8dp; smaller overrides
  propagate. Optional `shape`, alignment and padding let Login use a centered pill without changing other consumers. `Standard` and `Primary` share
  color roles but differ in tonal elevation. TV tertiary retains a transparent disabled container. `StreamCoreTheme` maps TV Material on Android only.
- **TV text/icon actions:** text buttons share internal text defaults with touch while keeping native focus, a 52dp minimum height and fixed scale.
  Icon buttons keep 48dp focus/layout bounds and an inset surface/border so login field outlines remain visible.
- **Web Compose:** retain keyboard activation, hover/focus, scrolling and dimensions. `StreamCoreWebControlStyle` projects roles into scoped CSS through
  `button`, `buttonStates` and `input`. Callers own element lifetime, events, dimensions and borders. State rules override base declarations.
  CSS preserves alpha and supported sp typography in rem; arbitrary Compose font families, brushes and em/unspecified units are outside the export contract.
- **Shapes:** container, fill, content clip, indication and border resolve to the same shape. Preserve intentional outer TV focus rings and reserve their
  painted extent at scrolling edges; do not conceal an indication mismatch by clipping away the focus ring.

Shared contract files live under `core/ui/src/commonMain/.../theme` and `components`; Android TV mapping/renderers live in `core/ui/src/androidMain`;
browser renderers and CSS adapters live in `core/ui-web`. Keep screen geometry in the owning feature platform module.

## Shared screen rendering

| Surface | Shared contract | Platform boundary to preserve |
| --- | --- | --- |
| Login | `LoginHeader`, `LoginForm`, layout/modifier contracts and primary/secondary/visibility slots. | Android form owns field errors, autofill, saveable visibility and submit. TV owns focus/IME/D-pad and native controls. Web retains a native credential form. Touch visibility during loading and disabled TV visibility are distinct existing behavior. |
| Profiles | Shared profile artwork, `ProfileEditorContent`, `AvatarPickerContent` and backdrop drawing. | Mobile full-screen and tablet/TV panel geometry stay local; TV owns focus/IME and restoration. Android backdrop owns its animation/accessibility policy. |
| Home/cards | `HomeHeroArtwork`, `HomeHeroContent`, `StreamCoreContentCardArtwork` and carousel indicator. | Platforms retain hero geometry, lazy shelves, navigation clearances, source-row identity and input. Shared leaves do not acquire browser or Android interaction assumptions. |
| Details | `DetailsPanoramaHero`, `DetailsActionContent`, synopsis/cast and recommendation artwork. | Mobile keeps its vertical reading layout; tablet/TV/web use their reading bands. Platforms own expansion, action availability, focus, card widths and navigation. |
| Player/settings | Transport icons, timeline drawing, and settings header/navigation/selection/speed/resize content. | Touch gestures, TV remote traversal and web media/fullscreen behavior stay local. Settings preserve playback and restore the originating control on close. |

Panorama artwork and gradient move together through `StreamCoreSharedArtworkImage`; text and controls retain their geometry. Keep the exact clicked
artwork URL and stable source-row identity. Image preparation is cancellable and origin-owned, with a bounded fallback to source artwork; keep the
destination URL/painters fixed during the bounds transition and avoid a second post-settle image switch.

TV Details scrolls the summary back into view when focus returns from recommendations. Live hero/action bounds and focus-ring clearance determine
relocation; large text keeps the control visible even when the full hero cannot fit. Reading columns share the outer viewport: Left/Right switch
columns, boundary Up reaches Play, and boundary Down reaches collapse/recommendations. Keep reading jobs composition-owned and cancellable.

## Browser integration boundaries

Native browser integration is deliberate for credentials, profile-name editing, editor actions/delete confirmation, Search editing and playback video.
Preserve autofill, IME/paste, committed values before submit, validation and media behavior. Search's native input remains transparent/inset so it does
not cover the Compose focus border. Shared button roles style native controls without taking ownership of their events or DOM lifetime.

Avatar and Player overlays stay in the existing Compose viewport, isolate background focus/accessibility and restore both on dismissal. A separate
dialog viewport must not strand semantics after closing. Scope document listeners to their owning overlay/route and dispose listeners/timers;
avoid duplicate Escape/Back dispatch. Escape closes a Player overlay before exiting fullscreen or navigating, and fullscreen ends when the route leaves.
Profile save commits the current DOM name. Login auxiliary actions remain disabled while browser flows are unavailable.

## Regression checklist

Run the subset relevant to the changed rendering or interaction; outstanding manual review does not waive tests for unrelated work.

- **Shared controls:** light/dark and existing mobile appearance; enabled/disabled/loading/focused states; long labels, large text, RTL, action names,
  minimum targets, spinner/icon/label centering, correct tint, clipping and focus rings at scrolling edges.
- **Login/profiles:** native autofill, IME submit/paste/caret behavior, password reveal, validation and single dispatch; create/edit/save/delete,
  avatar open-close-reopen, offscreen focus, background isolation, dialog dismissal and restoration after a fresh reload.
- **Browse/navigation:** profile artwork updates; top-level state/Back restoration; keyboard insets and final-item clearance; empty retry focus;
  carousel pause/timing, narrow layouts, large/long copy, keyboard-only traversal and browser history/deep-link reloads.
- **Details/artwork:** bright/dark artwork contrast, corners/focal crop, action fit, pending mutations and errors; warm/cold transitions both directions,
  changing URLs, source identity, long-text expansion/reading, recommendations and exact return focus.
- **Player:** real video remains visible beneath controls; startup/buffering/seek, hidden-control pointer/keyboard reveal, auto-hide, settings/tracks/
  resize/speed, errors/retry, focus containment/restoration, fullscreen/Escape, route disposal and media capability limits. Preserve Android PiP,
  progress-before-navigation, orientation settlement before pop and release-once behavior.
- **Accessibility and browsers:** screen-reader/accessibility-tree output, reduced motion, localized text and loading/empty/offline/error states in
  the relevant Chromium/Firefox/WebKit projects. A WebKit pass is not manual Safari proof. Use [web testing](kmp/web-testing.md) for execution scope.

Existing `StreamCoreButtonBaselineTest`, `StreamCoreControlStyleTest` and Playwright product journeys cover parts of these contracts; inspect their
current assertions before adding coverage. A successful compilation, screenshot capture or source refactor alone is not visual acceptance or a
runtime-performance result. Follow the [review workflow](agent-workflow.md) for capture/provenance and the [performance protocol](performance/mobile-navigation.md)
when claiming measured improvement.
