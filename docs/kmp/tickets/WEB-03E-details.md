# WEB-03E — Implement Web Details

## Dispatch

- **Depends on/base:** accepted `WEB-03A_CONTRACT_COMMIT`; the root supplies its exact 40-character SHA and verifies `HEAD`. No fallback base.
- **Branch:** `codex/web-03e-details`.
- **Concurrency:** feature-local; may run with up to two other WEB-03 feature owners while the root remains active.

## Ownership and Boundary

Owned: `feature/details/ui-web/**` and feature-local showcases/tests. Forbidden: `:webApp`, `:core:ui-web`, root/settings/catalog/convention files,
other features, provider/data implementations, playback, E2E, shared Details/playback contracts, credentials, unreserved screenshots, and outputs.

Consume `DetailsViewModel`, `DetailsAction`, `DetailsUiState`, `DetailsRequest`, shared models/repositories, `PlaybackRequestModel`, secure URI handling
provided at the route boundary, and WEB-03A components. UI remains backend-agnostic and performs no direct provider/network access.

## Expected API and Scope

- `WebDetailsRoute(profileId, contentId, onRecommendationSelected, onPlaySelected, onBack, onError, initialContent, returnFocusKey,
  onReturnFocusConsumed, viewModel)` using `(ContentModel, WebBrowseFocusKey) -> Unit` and
  `(PlaybackRequestModel, WebBrowseFocusKey) -> Unit`, exactly as frozen in `docs/kmp/WEB-03A-evidence.md`.
- Stateless `WebDetailsScreen(state, onAction, returnFocusKey, onReturnFocusConsumed, modifier)` with the frozen types and parameter order.
- Direct-ID loading that does not require an in-memory `ContentModel`; backdrop, metadata, description, cast/genres, recommendations, bounded images,
  loading/content/offline/error/long-text showcases.
- Like/My List optimistic mutation and rollback through existing shared actions. Trailer uses the injected secure browser URI launcher with protected
  new-tab behavior. Play emits `PlaybackRequestModel`; WEB-03A/F route it to the explicitly temporary placeholder.
- Deterministic focus/mouse handling, stable recommendation keys/content types, no per-recomposition allocation, and focus restoration for nested
  recommendation/back journeys.

Do not implement a player, modify route parsing, or add TMDB-specific ID/payload types.

## Focused Verification and Evidence

```powershell
.\gradlew.bat :feature:details:ui-web:compileKotlinWasmJs
.\gradlew.bat :feature:details:ui-web:testAndroidHostTest
```

Use the serialized queue. Host tests cover pure direct-ID dispatch, fixtures, and recommendation/action focus resolution. The module convention
intentionally has no feature-local Wasm browser-test task; WEB-03F owns rendering, secure URI, mutation, callback, semantics, and browser focus
assertions in `:webApp:wasmJsBrowserTest`. No Binaryen, full matrix, Android/root gate, live credentials, or uninspected visual claim.

Acceptance is a reviewed feature commit limited to reserved paths. WEB-03F owns shell integration and live/cross-browser claims.
