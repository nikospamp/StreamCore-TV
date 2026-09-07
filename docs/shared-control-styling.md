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
- TV caps the chosen radius at 8dp to preserve the existing D-pad focus geometry. A smaller shared override still propagates. Focus borders, inset, scale, elevation, padding and D-pad mechanics remain TV-owned. `StreamCoreTheme` installs an Android-only TV Material mapping for colors, typography and shapes; commonMain has no TV Material dependency.
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
