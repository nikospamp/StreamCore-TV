# WEB-03B — Implement Web Home

## Dispatch

- **Depends on/base:** accepted `WEB-03A_CONTRACT_COMMIT`; the root supplies its exact 40-character SHA and verifies `HEAD`. No fallback base.
- **Branch:** `codex/web-03b-home`.
- **Concurrency:** feature-local; may run with up to two other WEB-03 feature owners while the root remains active.

## Ownership and Boundary

Owned: `feature/home/ui-web/**` except the accepted module-shell build file unless a local dependency correction is proven, plus feature-local
showcases/tests. Forbidden: `:webApp`, `:core:ui-web`, root/settings/catalog/convention files, other features, providers/data, playback, E2E, shared
feature contracts, credentials, screenshots outside the reserved Home evidence path, and build outputs.

Consume `HomeViewModel`, `HomeAction`, `HomeUiState`, shared models/repositories, and WEB-03A's web components. The module stays backend-agnostic: no
TMDB call, DTO, SDK, response, or client-specific model.

## Expected API and Scope

- `WebHomeRoute(profileId, selectedContentKey, onContentSelected, onError, returnFocusKey, onReturnFocusConsumed, viewModel)` using
  `WebBrowseFocusKey?` for selected/return focus and `(ContentModel, WebBrowseFocusKey) -> Unit` for selection, exactly as frozen in
  `docs/kmp/WEB-03A-evidence.md`.
- Stateless `WebHomeScreen(state, onAction, selectedContentKey, returnFocusKey, onReturnFocusConsumed, modifier)` as the first public screen
  composable, with the frozen types and parameter order.
- Hero/backdrop, metadata, primary actions, pager/indicator, horizontal rows, continue-watching progress, bounded Coil/Ktor image requests, fallbacks,
  stable keys/content types, and loading/content/empty/offline/error/long-text showcases.
- Deterministic arrow/Enter/Space/Escape and mouse behavior; hover changes visuals without unpredictably stealing keyboard focus; focused cards scroll
  into view and return-focus is consumed once when its keyed item still exists.

Do not wire app navigation or implement playback; emit the frozen callbacks only.

## Focused Verification and Evidence

```powershell
.\gradlew.bat :feature:home:ui-web:compileKotlinWasmJs
.\gradlew.bat :feature:home:ui-web:testAndroidHostTest
```

Run only through the build queue. The module convention intentionally has no feature-local Wasm browser-test task; host tests cover pure
fixture/focus resolution, while WEB-03F owns Compose browser-node assertions in `:webApp:wasmJsBrowserTest`. Record executed host-test counts, focus
scenario, allocation/key review, and inspected Home screenshots if generated. No file-presence visual claim, Binaryen, full matrix, Android/root
gate, or live credentials.

Acceptance is a reviewed feature commit that changes only reserved paths and satisfies the frozen API. WEB-03F owns integration and the live claim.
