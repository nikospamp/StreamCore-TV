# WEB-03 Evidence — Browse Surfaces

## Identity and integration inputs

- Branch: `codex/web-03f-browse-integration`.
- Accepted WEB-02 base: `eb5467a661fed227f80f70bd3f8a2e91c52bf102`.
- Frozen WEB-03A contract: `4cb1bb45c2df6b230a9121bf365d96ee39259139`.
- Reviewed feature inputs, merged in the required order:
  - WEB-03B Home: `d2c872717e5ec6517f6a7034bd6a1d6e1dd21200`.
  - WEB-03C Search: `616e099314ea026be0c1f08ad608171a9b979130`.
  - WEB-03D Library: `776f2a809f9bc2eff2b61ba000b767b3167c7d65`.
  - WEB-03E Details: `e610520433b0976d961d2ddd57833b9f53d48d43`.
- Initial integration commit: `0f3095fea18c4c3cfdbe8f39d0f0a16ba98cff5f`.
- Frozen production candidate after the design-token correction: `e4080c1cbc1acd2c176d7aefc900e5d6434abbb6`.
- The target architecture remains backend-agnostic. Core/domain/feature UI contains no TMDB DTO, SDK, response, or client-specific model.

## Integrated behavior

- `/home`, `/search`, `/library`, `/details/{contentId}`, and the explicit non-playing `/player/{contentId}` route are wired through the product
  shell and browser history. IDs, destinations, section keys, and item keys are the only persisted navigation values.
- Home, Search, and Library ViewModels are profile-keyed. Details reconstructs content by ID after direct navigation/reload; no serialized
  `ContentModel` enters history.
- Search uses one typed native search input. Enter commits the current DOM value before dispatch, debounce timers are cancelled on remount, and
  Escape/caret behavior stays native while the input owns focus.
- Details and Library mutations share the backend-agnostic library repository. `My List`/`In My List` changes are reflected across both surfaces.
- Return-focus capture occurs before pushing Details/Player. Back, Forward, Escape, recommendation navigation, and reload consume an exact matching
  focus key once. Explicit top-level/profile/logout transitions clear stale provenance.
- The temporary Player route has no media/video/Shaka element. Space/Escape return one layer to Details and restore the Play action. WEB-04 owns
  playback.
- Logout clears protected profile/navigation state, replaces history with Login, and browser Back cannot reveal authenticated content.
- Artwork requests retain explicit decode bounds and backend-free fallbacks. Web-only dimensions now use `StreamCoreDimens.Web.*`; theme colors use
  Material color roles. These substitutions do not add recomposition state, retained scopes, or per-frame allocations.

## Review

Production code was frozen before the original candidate reviews. Read-only architecture/lifecycle, accessibility/history, and security/test-
integrity reviews found and closed three acceptance issues: Player Escape handling, Details origin provenance across Back/Forward, and weak E2E
terminal assertions. Delta reviews then passed with no reproducible P0/P1, backend-boundary, public-API, security, data-loss, or explicit ticket
acceptance failure.

After the root design-token gate exposed 29 raw values, disjoint Home/Search/Library/Details/core edits replaced them with central tokens and existing
color-scheme extensions. A capped production-delta review and a capped test-delta review both passed. No reviewer was allowed to expand the gate to
stylistic or speculative edge cases.

## Serialized verification ledger

Commands below ran one at a time through the shared build/browser queue unless the concurrency correction is explicitly recorded.

| Scope | Command and observed result |
|---|---|
| Design tokens | `./gradlew verifyDesignTokens -PverifyDesignTokensLogFiles=true --max-workers=1 --console=plain`: PASS; 360 production files, zero violations; 5 actionable tasks (1 executed, 4 up-to-date). |
| Focused Wasm compile | `./gradlew :core:ui-web:compileKotlinWasmJs :feature:home:ui-web:compileKotlinWasmJs :feature:search:ui-web:compileKotlinWasmJs :feature:library:ui-web:compileKotlinWasmJs :feature:details:ui-web:compileKotlinWasmJs --max-workers=1 --console=plain`: PASS in 23s; 89 actionable tasks (37 executed, 52 up-to-date). |
| Browser unit/Compose | `./gradlew :webApp:wasmJsBrowserTest --max-workers=1 --console=plain`: PASS in 1m18s; 61/61, zero failures/errors/skips; 320 actionable tasks (67 executed, 253 up-to-date). |
| Production/Binaryen | `./gradlew :webApp:wasmJsBrowserDistribution --max-workers=1 --console=plain`: PASS in 5m21s; 318 actionable tasks (60 executed, 258 up-to-date). |
| E2E dependencies | `npm ci`: PASS; 3 packages added, 4 audited, zero vulnerabilities. |
| E2E discovery | `npx playwright test --list`: PASS; 126 registrations = 21 scenarios across Chromium, Firefox, and WebKit at 1280x720 and 1920x1080. Live smoke is excluded. |
| Complete production matrix | `npx playwright test`: 123/126 PASS in 6.4m. All 12 new browse registrations passed except the Chromium-1920 interaction described below; two failures were inherited WEB-02 WebKit harness/runtime cases. This row remains an observed 123/126 and is not rewritten as 126/126. |
| Final affected correction reruns | Chromium-1920 browse: 1/1 PASS in 47.3s. WebKit-1280 history/CRUD/visual: 3/3 PASS in 1.5m with one worker. WebKit-1920 history/CRUD/visual: 3/3 PASS in 2.2m with one worker. |
| Final Android/root gate | `./gradlew :app:compileTmdbDebugKotlin :app:compileClientBDebugKotlin check -PverifyDesignTokensLogFiles=true -PstreamcoreLocalPropertiesPath=<primary ignored local.properties> --continue --max-workers=1 --console=plain`: PASS in 3m37s; 2,114 actionable tasks (365 executed, 1,749 up-to-date). Both app graphs, lint/tests, 360-file token verification, and KMP convention/dependency/compiler/test-target checks passed. |
| Final live TMDB journey | The first explicitly authorized run failed 0/1 in 40.0s on an invalid unique-title assumption after reaching Search; its nested `finally` cleanup completed. After the test-only correction and new explicit authorization, the single corrected retry passed 1/1 in 29.0s. All configuration booleans and exact child-environment assignment were true. Login → Profiles → Home → Search → `/details/550` → My List → Library → Details → temporary Player → Back → hard reload → Sign out → protected Back completed. Every required endpoint category was observed with only 2xx responses, application-driven temporary-session deletion returned 2xx JSON `success: true`, and browser storage was cleared. No credential, token, account/session value, request body, or request URL was printed, captured, or committed. |

The 61 browser-test count is: `WebRuntimeConfigTest` 5, `WebGraphTest` 1, `WebRouteTest` 7, `WebProductCoordinatorTest` 23,
`WebStorageFallbackTest` 7, `WebStorageProbeTest` 6, `WebBrowseFeatureScreensTest` 3, `WebBrowseShellTest` 1,
`WebFeatureScreensTest` 7, and `WebSelectorProbeTest` 1.

The production output contains approximately 1.29 MiB `streamcore-web.js`, 5.85 MiB application Wasm, and 8.24 MiB Skiko Wasm. Existing
webpack size/dynamic-dependency warnings and the existing Coil 3.4.0 versus Compose Skiko version warning remain visible; WEB-03 did not add a new
production dependency or claim a bundle-performance improvement.

## Matrix corrections and visual evidence

The complete matrix failures were retained rather than hidden:

- Chromium-1920 clicked 49 ms after the Search result projection appeared. Trace evidence showed stale `300x169` trending-card geometry while the
  final canvas correctly rendered the `199x299` result. The test-only driver now waits for bounded projection/paint turns, resolves fresh semantic
  bounds, hovers, and then sends a real canvas click. The affected browse journey passed 1/1.
- WebKit-1920 profile CRUD created and persisted the third profile and visibly entered Manage mode, but the accessibility projection temporarily
  retained `Select ...` instead of `Edit ...`. The same bounded projection settle fixed the driver without using a forbidden test-tag/DOM fallback;
  the affected CRUD journey passed.
- WebKit-1280 emitted one ordered same-origin blob-worker access/I/O pair during restoration after the avatar had a proven complete 200
  `application/xml` response and passed the visual crop gate. WebKit-1920 later exposed one same-origin `/composeResources/` fetch cancellation
  during hard-reload teardown. The test accepts only the observed ordered blob pair and at most one anchored same-origin Compose-resource abort per
  hard-reload phase after the response/visual proof. A singleton I/O error, reversed/repeated pair, cross-origin/non-resource URL, extra occurrence,
  or any other page error remains fatal.

The production matrix generated 36 browse frames under `webApp/e2e/screenshots/`: loading, content, empty, offline, error, and long-text for all six
engine/viewport projects. Chromium and Firefox passed all 24 frames on the complete matrix. The first WebKit inspection found incomplete transient
text layout in seven frames. The test-only visual driver now applies bounded paint readiness and hovers the empty/error primary action before capture;
the two WebKit visual scenarios were rerun serially. Final inspection passed all 12 WebKit frames: complete chrome/hierarchy, labeled empty/error
actions, visible focus, readable long Greek copy, no material clipping/overlap, backend-free fallback artwork, and normal below-fold continuation.
Therefore the final inspected artifact set is 36/36 PASS. Loading skeletons are explicit and complete; the optional loading caption is not used as a
blocking raster criterion.

## Concurrency correction

An early Android/root command accidentally remained active while later Playwright work started. Its result is retained only as diagnostic evidence:
2,118 tasks ran, both Android app compiles and downstream tests/lint completed under `--continue`, and only `verifyDesignTokens` failed with the 29
values corrected above. Because that timing is not clean serialization evidence, it is not the final Android/root acceptance gate. The final gate is
rerun alone after the test/evidence commit and is the only Android/root result used for acceptance.

## Deferred scope

- Real browser video, Shaka state/commands, autoplay activation, tracks, fullscreen, resume, player cleanup, release packaging/CSP/CORS/cache policy,
  and manual Safari/macOS evidence belong to WEB-04A through WEB-04D.
- Automated Playwright WebKit is not a Safari claim.
- ClientB remains Android-operational and out of the first TMDB web release composition.

## Acceptance status

Accepted. All non-live gates pass, the original complete-matrix failures remain accurately separated from focused correction evidence, final visual
evidence is 36/36 approved, both Android application graphs pass, and the corrected final live journey passed with confirmed application-driven
temporary-session deletion plus browser-storage cleanup. The earlier policy-rejected launch started no process and transmitted nothing. No
credentials, generated production config, build output, request URL, session/account value, or machine-local path is committed.
