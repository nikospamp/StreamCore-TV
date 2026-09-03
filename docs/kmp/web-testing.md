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
- the former WEB-01 `HtmlElementView` probe exposed `data-testid="html-video-probe"`; WEB-04 retires that probe and its global-fallback adapter in favor
  of the production playback surface described below.

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

WEB-03 adds one ordinary DOM search contract through its typed `HtmlElementView` boundary:

- `search:field`, a native `searchbox` whose value may be filled and whose Enter/Escape/caret behavior may be driven directly.

The WEB-03 production matrix proved these additional Compose-projected `button` names in all six engine/viewport projects: `Home`, `Search`,
`Library`, `Change profile`, `Sign out`, `Play`, `My List`, `In My List`, `Clear search`, `Try again`, and deterministic
`Open details for <fixture title>` values. They remain geometry/discovery contracts only. Tests resolve a unique projected node, wait for its bounds
to settle, then send hover/pointer or keyboard input to the Compose surface; they do not call DOM `click()` on the projection. When Home exposes the
same content title in more than one semantic action, the hero action is disambiguated by its rendered `More details` copy instead of assuming a
globally unique name.

WEB-03 also exposes non-sensitive host readiness through `body[data-product-route]`, `body[data-product-visual-state]`, and
`body[data-profile-count]`. Its temporary `/player/{contentId}` placeholder is replaced by WEB-04's route-scoped production player.

WEB-04 adds these version-specific player contracts:

- the production session owns one native video with `data-testid="playback-video"`; deterministic diagnostic fixtures use
  `data-testid="player:video"` and no media URI;
- the native video and generated `HtmlElementView` host are pointer-transparent so Compose controls and the timeline receive real canvas pointer
  input without nesting the controls inside an interactive parent semantic node;
- projected buttons use exact names `Back`, `Back 10 seconds`, `Play`/`Pause`/`Replay`, `Forward 10 seconds`, `Enter fullscreen`/
  `Exit fullscreen`, `Playback settings`, `Retry`, `Back to playback settings`, plus state-bearing settings rows such as `Speed · 1×` and
  `Quality · 1080p · 5.8 Mbps`;
- Compose projects the timeline as an adjustable generic node, not an HTML `slider` role. Its exact accessible name is dynamic:
  `Playback position <position> of <duration>`, and tests locate it by label before sending pointer drag input to its settled bounds;
- a capture-phase document Escape listener owns real-browser Escape and stops duplicate Compose dispatch. Compose focus remains authoritative:
  fullscreen and Details restoration are proved behaviorally by pressing Space after return and requiring the focused action to reopen its target;
  cross-engine DOM `activeElement` projection is not treated as the focus contract;
- the ten deterministic fixtures are mandatory in the default Playwright suite; there is no environment flag or skip path that can turn a green
  command into 60 unexecuted player registrations. Their `body[data-player-*]` values contain only synthetic IDs, fixed sanitized copy, booleans,
  and counts. The diagnostic listener balance probe records actual relevant document/tagged-video
  registrations by target, type, listener identity, and capture flag; repeated lifecycle tests require a nonzero mounted count and exact zero after
  disposal.

Native login and profile-editor text inputs use the browser's Tab order. Left/Right must retain normal caret and selection semantics while those
inputs own focus; arrow-key focus movement is asserted only for non-text Compose rows and native action/dialog controls.

Playwright may use only:

- explicit web-shell DOM contracts such as `body[data-runtime-state]` and `body[data-image-probe]`;
- explicit attributes on content intentionally hosted through `HtmlElementView`;
- the browser URL/history, storage, request/response interception, popups, screenshots, and page errors;
- fixed-viewport keyboard and pointer input against the Compose surface when a node-level selector is unavailable.

WEB-02 profile create/edit/delete assertions cover account-scoped browser DataStore persistence across reload and isolation between configured
accounts. They do not claim that profile mutations synchronize to TMDB or another remote provider.

The diagnostic shell also exposes host-level `body[data-storage-mode]` and `body[data-network-probe]` state. These are explicit WEB-01 contracts,
not inferred Compose DOM projection. Playwright uses them to prove official DataStore fallback and backend-agnostic TMDB error mapping after its
request interception has independently asserted the real Fetch URL and headers.

WEB-02 adds explicit, non-sensitive error-shell attributes: `body[data-product-error-kind]`, `body[data-product-error-title]`, and
`body[data-product-error-message]`. They expose only mapped UI classification/copy and are removed with the dialog; backend bodies, credentials,
tokens, and session IDs must never be projected into them.

Playwright must not use `getByRole`, `getByLabel`, text locators, or `[data-testid]` for any other canvas-rendered Compose child unless a later,
version-specific all-engine matrix proves the exact selector and updates this document. The WEB-02 role/name list must be revalidated when Compose
Multiplatform or Playwright changes; the same applies to the WEB-03 and WEB-04 additions above. Browser password-manager or autofill integration is not claimed. The native login inputs expose standard
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

The live config disables trace, screenshots, and video. It never logs request bodies, request URLs, or configuration values. Credentials and
request/read-access tokens are never placed in URLs. Production calls that TMDB defines as session-authenticated do use the required `session_id`
query parameter; the runner observes only redacted endpoint labels/status classes and never prints that URL or value. A created session ID remains in
test memory only and is deleted in `finally`; nested cleanup clears browser local/session storage even if the remote deletion request or assertion
fails. Run it only when the user deliberately supplies valid credentials through the process environment.

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
