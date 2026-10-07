# Browser testing

Browser tests exercise the TMDB application root, SDK services, and application playback/UI while preserving backend-agnostic shared contracts. Choose the layer that owns the behavior; the [SDK verification workflow](../sdk/verification.md) covers publication and independent consumers.

## Test layers

| Layer | What it verifies |
|---|---|
| Headless SDK `wasmJsNodeTest` | Portable SDK models, runtime rules, and provider contract journeys |
| `:sdk:runtime:wasmJsBrowserTest` | Actual browser storage behavior |
| `:webApp:wasmJsBrowserTest` | Browser application/runtime and Compose node behavior |
| `:playback:web:wasmJsBrowserTest` | Browser engine, native video, and playback lifecycle |
| `:feature:player:ui-web:wasmJsBrowserTest`, `:feature:profiles:ui-web:wasmJsBrowserTest` | Feature Compose rendering and interactions |
| `webApp/e2e` Playwright | Packaged application, browser navigation, storage/network interception, physical input, and disposal |
| Opt-in live authentication smoke | Authorized real TMDB authentication/navigation and temporary-session cleanup |

Use `androidx.compose.ui.test.v2.runComposeUiTest` for semantics, tags, focus, input, and state assertions inside Compose. A Compose `testTag` is not automatically an HTML `data-testid`. Node tests do not prove browser storage or UI; compilation does not prove execution.

## Run the packaged application matrix

The pinned Playwright package is in `webApp/e2e/package.json`. Its six projects cover Chromium, Firefox, and WebKit at 1280×720 and 1920×1080, with one worker and no retries. The server owns port 4173 and does not reuse an existing server. Stop development serving before building production output.

From the repository root:

```powershell
.\gradlew.bat :webApp:wasmJsBrowserDistribution
Push-Location webApp/e2e
try {
    npm ci
    npx playwright install chromium firefox webkit
    npm run validate:release
    npx playwright test --list
    npx playwright test
} finally {
    Pop-Location
}
```

Use `npm run test:player` inside `webApp/e2e` for the focused player suite. Keep deterministic scenarios mandatory; report the current discovery/execution counts, failures, and skips. A focused rerun does not replace a full-matrix result when the change requires all projects.

On Windows, the config copies the installed official Firefox bundle into ignored `webApp/e2e/.playwright-browsers/` and launches it there to avoid a user-profile SideBySide/`mozglue` loader issue. It does not patch browser binaries. Verify launch on the current machine rather than assuming an earlier installation works.

Manual Safari/macOS acceptance has not been run and was previously waived. Automated WebKit is not Safari evidence. Browser password-manager/autofill integration is also unverified; native input autocomplete hints alone do not prove it.

## Native DOM versus Compose projections

Native boundaries expose explicit DOM contracts and support ordinary Playwright interaction:

| Surface | Contract |
|---|---|
| Login | `login:credentials-form`, `login:identifier`, `login:password`, `login:password-visibility`, `login:submit` |
| Profile name | `profile-display-name`, `profile-display-name-error` (alert linked with `aria-errormessage`) |
| Profile actions | `profile-editor-action-form`, `profile-editor-cancel`, `profile-editor-save`, `profile-editor-delete` |
| Delete confirmation | `profile-editor-delete-dialog`, `profile-editor-delete-cancel`, `profile-editor-delete-confirm` |
| Search | `search:field`, a native searchbox |
| Video | Production `playback-video`; deterministic fixture `player:video` |

Native text fields own normal Tab, caret, selection, Enter, and Escape behavior. Do not reinterpret Left/Right as Compose row navigation while a text input has focus.

Other Compose children render on the canvas. Accessibility-projected role/name nodes are discovery/geometry contracts: locate the intended node, obtain its positive-area viewport bounds, and send `page.mouse`/`page.keyboard` input to the canvas. Do not call DOM `click()` or `fill()` on a projected node. Re-resolve geometry after hover/recomposition and assert the resulting state/route.

Established projected button names include profile selection/management/edit/add actions; `Home`, `Search`, `Library`, `Change profile`, `Sign out`; content actions such as `Play`, `My List`, `In My List`, `Clear search`, `Try again`, and `Open details for <fixture title>`; and login help links. Use a fixture-controlled name and the existing test helpers to disambiguate repeated content/actions. The Home hero uses rendered `More details` copy where a title also appears elsewhere.

Treat these contracts as specific to the pinned Compose/Playwright versions. Revalidate all engine/viewport projects when changing their projection behavior. Do not infer new selectors from an arbitrary Compose tag or from DOM inspection in only one engine.

## Player interaction and lifetime contracts

The native video mounts in `#streamcore-playback-video-layer` below `#streamcore-compose-root`. Both video and layer are pointer-transparent; Compose clears the video area and renders controls above it. Verify visible video composition and physical input recovery separately.

Projected controls include `Back`, `Back 10 seconds`, `Play` / `Pause` / `Replay`, `Forward 10 seconds`, `Enter fullscreen` / `Exit fullscreen`, `Playback settings`, `Retry`, `Back to playback settings`, and state-bearing settings rows. Require the rendered control to be ready before activation; a projected name alone does not prove an enabled player action.

The timeline is an adjustable projected node named `Playback position <position> of <duration>`, not an HTML slider. Locate it by label and drag at its viewport bounds. A capture-phase document Escape handler must close one layer without duplicate Compose dispatch. Verify focus restoration behaviorally after fullscreen and Details return; DOM `activeElement` is not the Compose focus contract.

The deterministic player suite covers readiness, autoplay rejection, retry, hidden-control pointer/keyboard recovery, seek/settings, profile-scoped resume after reload, fullscreen/focus, Back/Escape, invalid IDs, no-filmstrip behavior, and repeated disposal. Preserve the real production auto-hide timing check when changing test timing. On exit, require exact zero remaining sessions, video nodes, relevant listeners, and timers; a leak probe must observe a nonzero mounted count first.

## Host diagnostics and persistence

Explicit shell attributes expose non-sensitive state:

- Runtime/storage/network: `data-runtime-state`, `data-image-probe`, `data-storage-mode`, `data-network-probe`.
- Product readiness: `data-product-route`, `data-product-visual-state`, `data-profile-count`.
- Error copy: `data-product-error-kind`, `data-product-error-title`, `data-product-error-message`, removed when the dialog closes.
- Player fixtures: `data-player-*` with synthetic IDs, sanitized copy, booleans, and counters.

Use these attributes, browser URL/history/storage, intercepted requests, popups, screenshots, and page errors for browser-boundary tests. Never project response bodies, credentials, tokens, or session IDs into diagnostics. Keep unknown runtime errors fatal; any permitted teardown signature must remain exact, bounded, and tied to the expected lifecycle phase.

Profile CRUD/reload tests cover account-scoped browser persistence and isolation; they do not imply remote TMDB profile synchronization. Storage/error tests must assert the actual request boundary and observable state, not infer success from a diagnostic label alone.

## Opt-in live authentication smoke

The default matrix does not contact live TMDB. `npm run test:live-auth` uses `playwright.live.config.ts`, one Chromium worker, and the production distribution. It requires all five process-local variables; missing inputs produce a skip, not a pass:

```text
STREAMCORE_LIVE_TMDB_BASE_URL
STREAMCORE_LIVE_TMDB_READ_ACCESS_TOKEN
STREAMCORE_LIVE_TMDB_ACCOUNT_ID
STREAMCORE_LIVE_TMDB_USERNAME
STREAMCORE_LIVE_TMDB_PASSWORD
```

Use only explicitly authorized account credentials. The live config disables traces, screenshots, and video. It reports redacted endpoint/status information; session IDs exist only in runtime memory, with remote deletion and browser-storage cleanup in `finally`. TMDB-required `session_id` query parameters must never be printed or retained in logs.

For local ignored files, use `webApp/e2e/run-live-auth.ps1`:

- `-CredentialsPath` or `STREAMCORE_TMDB_CREDENTIALS_FILE`: username/identifier/email and password properties; `tmdb*` aliases are accepted.
- `-LocalPropertiesPath` or `STREAMCORE_LOCAL_PROPERTIES`: TMDB read-access token/account ID and optional base URL.
- `-PreflightOnly`: validate parsing and child-environment assignment without Node.
- `-ListOnly`: discover the smoke without launching a browser/server/test.

The launcher accepts literal `key=value` or `key: value` properties, rejects duplicate/blank aliases, prints only configured/missing booleans, and passes values only to the child environment. Use `webApp/e2e/live-auth.credentials.example.properties` as a template; keep real files under ignored `docs/credentials/`.
