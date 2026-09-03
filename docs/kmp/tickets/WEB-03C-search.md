# WEB-03C — Implement Web Search

## Dispatch

- **Depends on/base:** accepted `WEB-03A_CONTRACT_COMMIT`; the root supplies its exact 40-character SHA and verifies `HEAD`. No fallback base.
- **Branch:** `codex/web-03c-search`.
- **Concurrency:** feature-local; may run with up to two other WEB-03 feature owners while the root remains active.

## Ownership and Boundary

Owned: `feature/search/ui-web/**`, including its native Wasm text-input adapter and feature-local showcases/tests. Forbidden: `:webApp`,
`:core:ui-web`, root/settings/catalog/convention files, other features, providers/data, playback, E2E, shared Search contracts, credentials, global
HTML/resources, unreserved screenshots, and build outputs.

Consume `SearchViewModel`, `SearchAction`, `SearchUiState`, repositories, and the WEB-03A input/component contract. The module remains
backend-agnostic and performs no direct provider/network access.

## Expected API and Scope

- `WebSearchRoute(profileId, selectedContentKey, onContentSelected, onBack, returnFocusKey, onReturnFocusConsumed, viewModel)` using
  `WebBrowseFocusKey?` for selected/return focus and `(ContentModel, WebBrowseFocusKey) -> Unit` for selection, exactly as frozen in
  `docs/kmp/WEB-03A-evidence.md`.
- Stateless `WebSearchScreen(state, onAction, selectedContentKey, returnFocusKey, onReturnFocusConsumed, modifier)` plus the frozen
  `expect/actual WebSearchTextField` value/change/committed-submit/Escape/focus-consumption contract.
- Native browser text entry must synchronously commit input before submit, handle composition/paste, and avoid the zero-delay canvas focus race
  learned in WEB-02. Search ViewModel retains debounce/cancellation behavior.
- Discovery/trending, recent searches, remove/clear, result rows/grid, loading/empty/offline/error/long-text showcases, bounded images, stable keys and
  content types.
- Deterministic keyboard/mouse movement and single-consumption return focus without bare coordinate selectors.

Do not change shared debounce logic, app routes, global HTML, or browser server fixtures; report a missing contract to the root.

## Focused Verification and Evidence

```powershell
.\gradlew.bat :feature:search:ui-web:compileKotlinWasmJs
.\gradlew.bat :feature:search:ui-web:testAndroidHostTest
```

Through the serialized queue, host tests cover pure cancellation/fixture/focus behavior. The module convention intentionally has no feature-local
Wasm browser-test task; WEB-03F owns native text-commit/submit, semantics, IME/paste, and Compose browser assertions in
`:webApp:wasmJsBrowserTest`. Record host-test counts and mark browser-only cases deferred, not passed. No Binaryen, full matrix, Android/root gate,
visual approval by existence, or live credentials.

Acceptance is a reviewed feature commit limited to reserved paths. WEB-03F owns integration, provider, and cross-browser claims.
