# Shared control styling

Issue 023 establishes a small, backend-agnostic component foundation for the shared brand.

## Public contract

`StreamCoreControlDefaults.style()` in `:core:ui` resolves an immutable `StreamCoreControlStyle` from the current Material theme. `LocalStreamCoreControlStyle` permits a composition-boundary override (derive with `copy` inside `StreamCoreTheme`). It is not a provider palette extension point.

| Role | Default |
|---|---|
| `primary` / `onPrimary` | Material primary pair |
| `pressed` / `onPressed` | Material primary-container pair |
| `disabledContainer` / `disabledContent` | onSurface at 10% / onSurfaceVariant at 38% alpha, preserving the resolved Material defaults |
| `buttonLabel` | labelLarge: 14sp, weight 500, 20sp line height, 0.1sp tracking |
| `buttonRadius` | 999dp, clamped by the renderer to its bounds |
| `inputLabel` / `inputRadius` | bodyLarge / 6dp |
| `disabledInputOpacity` | 0.55, applied to native text fields only |

Touch `StreamCoreButton`, Android `StreamCoreTvButton`, and `StreamCoreWebButton` consume the button roles. TV Standard is an app-owned primary action, equivalent in color to Primary. Secondary, tertiary, selected, and destructive fills retain their Material semantic roles; shared label, shape and disabled treatment apply across variants. TV tertiary intentionally retains its transparent disabled container.

All three renderers disable activation while loading, retain the supplied action name in semantics, and render the spinner with their resolved content color. Existing caller-side disabling remains valid. This does not establish a duplicate-submit defect in existing callers. Intrinsic loading width and a localized explicit loading announcement remain outside this change.

## Renderer boundaries

- Touch retains its existing minimum heights, content padding, Material state layers/ripple, and default appearance. Input roles are consumed by native web fields; this is not a touch text-field rewrite. `StreamCoreTextButton` remains a separate contract.
- TV caps the chosen radius at 8dp by default to preserve existing D-pad focus geometry. A smaller shared override still propagates. `StreamCoreTvButton` accepts an optional `shape` with matching focus borders and an optional `contentAlignment`; login uses the shared pill shape, centered content, and the standard 52dp minimum height. Existing callers retain their defaults. Focus borders, inset, scale, elevation, padding and D-pad mechanics remain TV-owned. `StreamCoreTheme` installs an Android-only TV Material mapping for colors, typography and shapes; commonMain has no TV Material dependency.
- Touch `StreamCoreTextButton` and Android `StreamCoreTvTextButton` share native Material text-button colors, shape, and label typography through internal defaults. Touch appearance is preserved. TV adds a 52dp minimum height, neutral focused surface, brand focus border, and fixed scale. Text actions remain a separate styling contract from filled buttons.
- Android `StreamCoreTvIconButton` retains 48dp layout/focus bounds with an inset painted surface and focus border that clear the surrounding text-field outline, plus a TV-to-Compose content-color bridge. Login supplies the platform-neutral `LoginPasswordVisibilityIcon`; TV owns D-pad routing and IME handling.
- Web Compose retains its existing keyboard activation, focus border, hover/scale, scrolling and minimum height. Button label typography and rounding now follow the shared roles.
- `StreamCoreWebControlStyle` in `:core:ui-web` projects roles into CSS. `button()` supplies base or disabled declarations; `buttonStates(scopedSelector)` supplies native focus/hover/pressed/disabled rules; `input()` supplies text-field typography, rounding and disabled opacity. Callers own dimensions, borders, layout, element lifetime and event handlers. Hover/active rules intentionally override inline base declarations. Supply one scoped selector per call.
- CSS preserves alpha with rgba, uses rem for the selected sp typography values, and uses the browser system font stack. The portable contract supports specified sp font size, line height and tracking, and font weight; arbitrary Compose font families, brushes, text transforms and em/unspecified units are not a CSS export contract.

## Ownership for 024–026

- Foundation: `core/ui/src/commonMain/.../theme/StreamCoreControlStyle.kt`, `Shape.kt`, `Theme.kt`, and `components/StreamCoreButton.kt`.
- Android mapping: `core/ui/src/androidMain/.../theme/StreamCorePlatformTheme.android.kt` and `components/StreamCoreTvButton.kt`.
- Web renderer/adapter: `core/ui-web/src/commonMain/.../web/StreamCoreWebComponents.kt` and `StreamCoreWebControlStyle.kt`.
- Native adoption: Wasm login credential form, profile name field/action strip/delete dialog, and search text field style builders.

Platform polish should consume this API and keep screen geometry in its owning platform module. Coordinate a shared correction if multiple platforms need a contract change. No dependency versions, provider behavior, navigation, storage, or playback code belong to this foundation.

## Verification

`StreamCoreButtonBaselineTest` captures backend-free light/dark mobile form and button states. `StreamCoreControlStyleTest` tests a non-shipping override through actual touch/TV/web Compose rendering and the CSS adapter, loading names/activation/spinner color, and D-pad navigation. The core device-test-only dependency on `:core:ui-web` makes cross-renderer coverage possible without changing production dependencies.

The focused Playwright product test checks actual native CSS, focus/hover/pressed/loading states, stable accessible action name, DOM identity, autofill attributes and retry readiness. Existing credential, profile dialog and search/product journeys cover browser-native interaction behavior.

No new scopes, flows, listeners or retained DOM objects are introduced. Style objects/strings are composition-local values; this work makes no measured runtime-performance claim.

## Android login harmonization

`LoginHeader` shares explicit on-surface title/subtitle rendering across mobile, tablet, and TV. Screen modules retain typography overrides, spacing, panel geometry, and input behavior. Explicit title color avoids falling back to an inherited content color on translucent login panels.

`LoginForm` is the single field/action implementation for Android mobile, tablet, and TV. It owns identifier/password rendering, validation errors,
autofill semantics, saveable password visibility, IME submission, localized action labels, and test tags. `LoginFormLayout` supplies spacing;
`LoginFormModifiers` supplies per-control modifiers. Three composable slots select primary, secondary, and password-visibility controls. TV supplies
its native controls and keeps focus requesters, D-pad interception, and IME visibility handling in `TvLoginScreen`. The deprecated `LoginMaterialForm`
symbol only forwards to `LoginForm` for source compatibility with deferred callers; it has no separate implementation.

The touch defaults preserve the current 8dp form gaps, 24dp submit top padding, and 4dp secondary-action spacing/top padding. Existing touch eye
activation during loading is retained; the TV eye remains disabled while loading. Normalizing that behavior is deferred beyond this refactor.

TV login uses 16dp form gaps and 24dp from password to Continue. TV button renderers bound their intrinsic height and center content within the padded height to account for TV Material's internal surface dropping minimum constraints. This adds an intrinsic measurement query; it introduces no scopes, flows, or retained interaction state. Mobile/tablet rendering is unchanged by this TV correction.

Automated regression coverage for this iteration is deferred until manual UI review is finalized:

- Mobile appearance before/after shared heading, icon, and text-action extraction; tablet/TV title contrast.
- TV password Right to reveal, Center to toggle, Left to field, vertical traversal, and caret navigation with IME open.
- Continue and secondary actions in enabled, disabled, loading, and focused states; existing TV consumers retain default geometry.
- TV label/icon/spinner vertical centering at minimum and content-driven heights; eye focus border clearance; login action spacing.
- Long localized labels, large font scale, accessibility labels, and show/hide state.
- Shared-form parity for field errors, autofill, IME submission, action dispatch, and password visibility across native control slots.

## Android profiles harmonization

`ProfileEditorContent` and `AvatarPickerContent` own the shared editor fields, action wiring, avatar artwork, and avatar grid. Mobile keeps its
existing full-screen placement and control defaults. Tablet and TV own centered panel/dialog bounds; TV supplies focusable controls and retains
its D-pad, IME, and dialog focus-restoration behavior. The unused legacy chip-based editor and initials-based profiles grid have been removed.

Mobile/tablet reuse Android touch profile tiles and header rendering. Tablet places the tiles in a centered horizontal lazy row. `ProfilesBackdrop`
owns portable drawing; `AndroidProfilesBackdrop` owns Android animation/accessibility policy. The backdrop still reads its animation in drawing.

Deferred automated regression coverage: mobile appearance parity; tablet row centering/overflow and Manage actions; editor create/edit/save/delete
states; avatar selection and dialog dismissal; TV D-pad traversal, offscreen avatar focus, focus restoration, and IME Done; large text and loading
states. No new repository/ViewModel scopes or flows are introduced by this UI extraction.

## Android Home harmonization

Tablet uses the same app-owned floating bottom navigation as mobile, retaining its width cap, destination state, visibility rules, and keyboard
insets. Home reserves bottom scroll clearance; tablet Search/Library retain destination-local clearance. The tablet browse header receives the
active profile's avatar, and the tablet hero uses a 414dp height, 15% taller than its previous 360dp baseline.

TV Home starts directly with content. The removed header's focus fallback now belongs to the empty-state Retry action. Its Details action uses
the shared pill shape and compact padding while retaining TV Material focus behavior and native minimum sizing. The TV button padding override
defaults to its previous native value, preserving existing consumers.

Deferred regression coverage: mobile rendering; tablet top-level switching, Back/state restoration, IME and last-item clearance; tablet avatar
updates and hero sizing; TV empty Retry focus, hero-to-details return focus, and carousel button states.

Tablet/TV Home hero artwork extends edge to edge behind navigation/header overlays, with gradients protecting copy and blending into the page.
The shared navigation container uses 95% opacity (5% transparent); content, selection indicators, and focus borders keep their own opacity.
TV navigation uses narrower expanded/collapsed geometry and smaller icons while preserving its usable focus targets. Mobile hero layout remains
unchanged. Include bright-artwork contrast and expanded-drawer overlap in the deferred visual regression checks.
