# Web testing strategy

WEB-01 locks the test split for WEB-02 through WEB-04. The target architecture remains backend-agnostic: browser tests exercise the TMDB web
composition root and shared contracts without introducing provider types into core, domain, or feature UI.

## Compose UI Test v2 is the node layer

Use `androidx.compose.ui.test.v2.runComposeUiTest` for Compose semantics and node behavior. Tests may address stable `testTag` values, roles, text,
focus, keyboard input, pointer input, and state changes through Compose test APIs. The WEB-01 probe proves a button click focuses a tagged text field,
accepts text input, and updates a tagged status node through `:webApp:wasmJsBrowserTest`.

Do not duplicate those assertions in Playwright through assumed DOM selectors. A Compose `testTag` is a Compose semantics identifier, not a promised
HTML `data-testid`.

## Proven Playwright selector contract

Playwright `1.62.1` inspected the ready probe in Chromium, Firefox, and WebKit at 1280×720. All three engines produced the same ordinary-DOM result:

- the Compose viewport is represented by one host `DIV`;
- Compose buttons, text, and text fields expose no ordinary DOM nodes carrying `role`, accessible-name attributes, or `data-testid`;
- the probe's `testTag` values are therefore not valid Playwright selectors;
- the `HtmlElementView` video is an ordinary DOM element and its explicit `data-testid="html-video-probe"` is stable in all three engines.

Playwright may use only:

- explicit web-shell DOM contracts such as `body[data-runtime-state]` and `body[data-image-probe]`;
- explicit attributes on content intentionally hosted through `HtmlElementView`;
- the browser URL/history, storage, request/response interception, popups, screenshots, and page errors;
- fixed-viewport keyboard and pointer input against the Compose surface when a node-level selector is unavailable.

The diagnostic shell also exposes host-level `body[data-storage-mode]` and `body[data-network-probe]` state. These are explicit WEB-01 contracts,
not inferred Compose DOM projection. Playwright uses them to prove official DataStore fallback and backend-agnostic TMDB error mapping after its
request interception has independently asserted the real Fetch URL and headers.

Playwright must not use `getByRole`, `getByLabel`, text locators, or `[data-testid]` for canvas-rendered Compose children unless a later, version-specific
spike proves those selectors in all supported engines and updates this document. Browser password-manager or autofill integration is not claimed.

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
