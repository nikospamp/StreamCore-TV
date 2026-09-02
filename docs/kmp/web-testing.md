# Web testing strategy

WEB-01 defines the test split for WEB-02 through WEB-04. The target architecture remains backend-agnostic: browser tests exercise the TMDB web
composition root and shared contracts without introducing provider types into core, domain, or feature UI.

## Compose UI Test v2 is the node layer

Use `androidx.compose.ui.test.v2.runComposeUiTest` for Compose semantics and node behavior. Tests may address stable `testTag` values, roles, text,
focus, keyboard input, pointer input, and state changes through Compose test APIs. The WEB-01 probe proves a button click focuses a tagged text field,
accepts text input, and updates a tagged status node through `:webApp:wasmJsBrowserTest`.

Do not duplicate those assertions in Playwright through assumed DOM selectors. A Compose `testTag` is a Compose semantics identifier, not a promised
HTML `data-testid`.

## Proven Playwright selector contract

Playwright `1.62.1` inspected the WEB-01 ready probe in Chromium, Firefox, and WebKit at 1280×720. All three engines produced the same ordinary-DOM
result:

- the Compose viewport is represented by one host `DIV`;
- Compose buttons, text, and text fields expose no ordinary DOM nodes carrying `role`, accessible-name attributes, or `data-testid`;
- the probe's `testTag` values are therefore not valid Playwright selectors;
- the `HtmlElementView` video is an ordinary DOM element and its explicit `data-testid="html-video-probe"` is stable in all three engines.

WEB-02 adds a narrowly scoped accessibility-projection contract for Compose Multiplatform `1.12.0` with Playwright `1.62.1`. The production matrix
proved the following exact role/name queries in Chromium, Firefox, and WebKit at both 1280×720 and 1920×1080:

- `button`, `Select Nikos profile` for the deterministic first-profile fixture;
- `button`, `Manage profiles`;
- `button`, `Edit Nikos profile`;
- `button`, `Add profile`;
- `button`, `Edit Browser profile profile` and `Edit Browser profile edited profile` for the deterministic CRUD fixtures.
- `button`, `Forgot password?`, `Create account`, and `Need help?` for login screenshot readiness.
- `button`, `External link` for the diagnostic popup geometry/no-opener test.

These projected nodes are discovery/geometry contracts, not ordinary hit-testable DOM controls. Playwright may use the exact role/name to wait for a
unique semantic node, obtain and retain its bounding box, or clip a screenshot. User interaction must then be sent to the Compose canvas with
viewport `page.mouse`/`page.keyboard` input at those semantic bounds. Calling DOM `click()`, `fill()`, or similar interaction APIs on a projected
Compose node is not supported; the canvas intercepts pointer input. Dynamic role/name use is allowed only when the test controls a unique profile
display name and asserts the corresponding state/route afterward.

WEB-02 also proves these ordinary DOM contracts hosted explicitly through `HtmlElementView`:

- `login:credentials-form`, `login:identifier`, `login:password`, `login:password-visibility`, and `login:submit`;
- `profile-display-name`;
- `profile-display-name-error` (visible alert text; referenced by `aria-errormessage` through its stable element ID);
- `profile-editor-action-form`, `profile-editor-cancel`, `profile-editor-save`, and `profile-editor-delete`;
- `profile-editor-delete-dialog`, `profile-editor-delete-cancel`, and `profile-editor-delete-confirm`.

Those exact `data-testid` values may use ordinary Playwright DOM interaction. No other Compose `testTag` becomes a Playwright `data-testid` through
this exception. The native login visibility and Continue buttons also expose their standard button roles and exact localized accessible names as
ordinary DOM controls; unlike projected Compose nodes, Playwright may focus, click, or press them directly.

Playwright may use only:

- explicit web-shell DOM contracts such as `body[data-runtime-state]` and `body[data-image-probe]`;
- explicit attributes on content intentionally hosted through `HtmlElementView`;
- the browser URL/history, storage, request/response interception, popups, screenshots, and page errors;
- fixed-viewport keyboard and pointer input against the Compose surface when a node-level selector is unavailable.

The diagnostic shell also exposes host-level `body[data-storage-mode]` and `body[data-network-probe]` state. These are explicit WEB-01 contracts,
not inferred Compose DOM projection. Playwright uses them to prove official DataStore fallback and backend-agnostic TMDB error mapping after its
request interception has independently asserted the real Fetch URL and headers.

WEB-02 adds explicit, non-sensitive error-shell attributes: `body[data-product-error-kind]`, `body[data-product-error-title]`, and
`body[data-product-error-message]`. They expose only mapped UI classification/copy and are removed with the dialog; backend bodies, credentials,
tokens, and session IDs must never be projected into them.

Playwright must not use `getByRole`, `getByLabel`, text locators, or `[data-testid]` for any other canvas-rendered Compose child unless a later,
version-specific all-engine matrix proves the exact selector and updates this document. The WEB-02 role/name list must be revalidated when Compose
Multiplatform or Playwright changes. Browser password-manager or autofill integration is not claimed. The native login inputs expose standard
autocomplete hints, but browser UI and stored-credential behavior remain outside the proven contract.

## Browser matrix and Windows Firefox handling

The pinned projects are Chromium, Firefox, and WebKit. On this Windows host, Playwright's unchanged Firefox bundle could not be loaded from the user
profile because the Windows side-by-side loader rejected its embedded `mozglue` assembly. The Playwright config copies that same installed bundle to
the ignored worktree-local `.playwright-browsers/` directory before launch; no browser binary is patched or committed. Chromium and WebKit use their
normal Playwright-managed paths.

## Commands

```powershell
Set-Location webApp/e2e
npm ci
npx playwright install chromium firefox webkit
npx playwright test
```

Build the production distribution first:

```powershell
.\gradlew.bat :webApp:wasmJsBrowserDistribution
```

## Opt-in live TMDB authentication smoke

The default matrix never contacts live TMDB. `npm run test:live-auth` uses `playwright.live.config.ts`, one Chromium worker, and the production
distribution. It requires all five process-local environment variables below; if any are absent, the single smoke is explicitly reported as
skipped and must not be counted as a pass:

```text
STREAMCORE_LIVE_TMDB_BASE_URL
STREAMCORE_LIVE_TMDB_READ_ACCESS_TOKEN
STREAMCORE_LIVE_TMDB_ACCOUNT_ID
STREAMCORE_LIVE_TMDB_USERNAME
STREAMCORE_LIVE_TMDB_PASSWORD
```

The live config disables trace, screenshots, and video. It never logs request bodies or puts credentials/tokens/session IDs in URLs. A created
session ID remains in test memory only and is deleted in `finally`; browser storage is also cleared. Run it only when the user deliberately supplies
valid credentials through the process environment.

`webApp/e2e/run-live-auth.ps1` is the local file-backed launcher. It accepts `-CredentialsPath` (or
`STREAMCORE_TMDB_CREDENTIALS_FILE`) for an ignored properties file containing case-insensitive `username`/`identifier`/`email` and `password`
keys (the `tmdb*` forms are also accepted), and `-LocalPropertiesPath` (or
`STREAMCORE_LOCAL_PROPERTIES`) for the ignored Android `local.properties` containing `tmdbReadAccessToken`/`tmdbAccountId` and optional
`tmdbBaseUrl`. Both `key=value` and `key: value` forms are parsed at the first separator; duplicate aliases and blank values are rejected. The
launcher parses files literally, prints configured/missing booleans only, removes inherited live variables, invokes the local Playwright CLI with
the resolved Node executable, sets values only on the child process, and clears its in-memory maps/environment entries in `finally`. Use
`live-auth.credentials.example.properties` only as a key-name template and keep the real copy under the already ignored `docs/credentials/`.

`-PreflightOnly` validates parsing and exact child-environment assignment without starting Node. `-ListOnly` invokes Playwright `--list` for offline
configuration/test discovery without launching a browser, server, or test.

An agent-triggered launcher invocation still requires explicit action-time credential authorization. A user running the wrapper manually in their
own terminal needs no additional Codex approval.
