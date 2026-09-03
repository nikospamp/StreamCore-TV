# WEB-03D — Implement Web Library

## Dispatch

- **Depends on/base:** accepted `WEB-03A_CONTRACT_COMMIT`; the root supplies its exact 40-character SHA and verifies `HEAD`. No fallback base.
- **Branch:** `codex/web-03d-library`.
- **Concurrency:** feature-local; may run with up to two other WEB-03 feature owners while the root remains active.

## Ownership and Boundary

Owned: `feature/library/ui-web/**` and feature-local showcases/tests. Forbidden: `:webApp`, `:core:ui-web`, root/settings/catalog/convention files,
other features, provider/data implementations, playback, E2E, shared Library contracts, credentials, unreserved screenshots, and build outputs.

Consume `LibraryViewModel`, `LibraryAction`, `LibraryUiState`, shared models/repositories, and WEB-03A components. UI is backend-agnostic and must not
access TMDB, DTOs, SDKs, responses, browser persistence implementations, or client-specific models directly.

## Expected API and Scope

- `WebLibraryRoute(profileId, selectedContentKey, onContentSelected, onError, returnFocusKey, onReturnFocusConsumed, viewModel)` using
  `WebBrowseFocusKey?`, `(ContentModel, WebBrowseFocusKey) -> Unit`, and the mandatory profile-keyed default
  `koinViewModel(key = "web-library:$profileId")`, exactly as frozen in `docs/kmp/WEB-03A-evidence.md`.
- Stateless `WebLibraryScreen(state, onAction, selectedContentKey, returnFocusKey, onReturnFocusConsumed, modifier)` with the frozen types and
  parameter order.
- Continue Watching, Liked, and My List sections; profile-isolated state, empty categories, refresh/retry, failure/offline, content and long-text
  showcases; bounded image requests and fallbacks.
- Stable keys/content types, no per-recomposition list/resource construction, deterministic arrow/Enter/Space/Escape and mouse behavior, scroll to
  focus, and single-consumption return focus when the keyed content still exists.

Mutations remain shared repository/ViewModel actions. Do not add web-specific storage or app route wiring.

## Focused Verification and Evidence

```powershell
.\gradlew.bat :feature:library:ui-web:compileKotlinWasmJs
.\gradlew.bat :feature:library:ui-web:testAndroidHostTest
```

Use the serialized queue. Host evidence covers pure category/empty fixtures, profile-key isolation, keys, and focus resolution. The module convention
intentionally has no feature-local Wasm browser-test task; WEB-03F owns rendering, refresh/error semantics, mutation reflection, and browser focus
assertions in `:webApp:wasmJsBrowserTest`. No Binaryen, complete browser matrix, Android/root gate, live credentials, or screenshot-existence
approval.

Acceptance is a reviewed feature commit limited to reserved paths. WEB-03F owns integration and persisted live-provider claims.
