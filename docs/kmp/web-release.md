# Web release and deployment contract

This contract is host-agnostic. The target architecture remains backend-agnostic; the web composition root selects TMDB and browser playback
implementations behind common repository and playback interfaces.

## Build and artifact validation

Build the production distribution from a frozen candidate, then validate that exact output before upload:

```powershell
.\gradlew.bat :webApp:wasmJsBrowserDistribution
Set-Location webApp/e2e
npm run validate:release
```

The validator inspects `webApp/build/dist/wasmJs/productionExecutable` by default. `STREAMCORE_WEB_RELEASE_DIRECTORY` may point it at a staged copy.
It requires `index.html`, at least one JavaScript bundle, content-versioned Wasm binaries, non-empty `composeResources`,
`config.example.json`, and the module-local `shaka-playback-adapter.mjs`. It verifies the adapter's static
`shaka-player/dist/shaka-player.compiled.js` package import, rejects the obsolete WEB-01 `shaka-adapter.mjs`, remote/CDN imports, and global Shaka
fallbacks, verifies that local HTML script references resolve, and verifies that every packaged Wasm name is content-versioned and referenced by
the JavaScript bundle. The example config must contain placeholders only, and no real `config.json`, raw JWT/read token, bearer value, session
value, or concrete token/account config value may be embedded in HTML, JavaScript, modules, JSON, source maps, or Wasm string content. Every HTML
script source is decoded and resolved inside the validated distribution before its existence is accepted.

Deploy from an allowlist containing the validated runtime files and directories. Source maps are not required at runtime and should not be public
unless a separate controlled symbol policy permits them. Keep license notices according to the dependency and organizational policy. Never commit
or copy a real runtime config into the distribution tree.

## Runtime configuration

The deployment supplies `/config.json` out of band at the same origin as the application:

```json
{
  "tmdbBaseUrl": "https://api.themoviedb.org/",
  "tmdbReadAccessToken": "replace-with-browser-visible-read-access-token",
  "tmdbAccountId": "replace-with-account-id"
}
```

All three values are required. Use HTTPS and a TMDB read-access token intended for a public browser client. Browser-delivered configuration is
observable by every user and must not contain a password, write credential, private signing key, temporary session, or privileged server token.
Protect provider-side capability through least privilege and origin/rate controls where the provider supports them. Authentication passwords and
temporary session IDs remain process/runtime values and must never enter `config.json`, build inputs, logs, URLs controlled by this application, or
committed files. TMDB endpoints that require `session_id` still place it in the provider-defined query parameter at request time; access logs must
redact that parameter.

Serve `/config.json` with `Content-Type: application/json; charset=utf-8`, `Cache-Control: no-store`, and `X-Content-Type-Options: nosniff`.
Provision it before switching traffic to a release and validate the config/application compatibility together.

## MIME types

The static server or object-store metadata must provide these response types without content sniffing:

| Extension | Content-Type |
|---|---|
| `.html` | `text/html; charset=utf-8` |
| `.js`, `.mjs` | `text/javascript; charset=utf-8` |
| `.wasm` | `application/wasm` |
| `.json` | `application/json; charset=utf-8` |
| `.css` | `text/css; charset=utf-8` |
| `.svg` | `image/svg+xml` |
| `.png` | `image/png` |
| `.jpg`, `.jpeg` | `image/jpeg` |
| `.webp` | `image/webp` |
| `.woff2` | `font/woff2` |
| `.mpd` | `application/dash+xml` |
| `.m3u8` | `application/vnd.apple.mpegurl` |
| `.mp4` | `video/mp4` |
| `.m4s` | `video/iso.segment` (or the origin-required compatible video type) |
| `.webm` | `video/webm` |
| `.vtt` | `text/vtt; charset=utf-8` |

Range responses for media must preserve `206 Partial Content`, `Accept-Ranges`, `Content-Range`, and the correct `Content-Length`.

## CORS and external origins

The deployed app makes browser requests to its configured TMDB API origin, TMDB image origin, and the media/manifest origins returned by the
backend-agnostic playback source. Each external origin must allow the deployed application origin. Do not solve CORS by disabling browser security
or by reflecting an unchecked `Origin` while allowing credentials.

- TMDB API responses must permit the application origin and the request headers/methods used by the Ktor Fetch client, including
  `Authorization`, `Accept`, `Content-Type`, `GET`, `POST`, and `DELETE` where applicable.
- Image responses must permit cross-origin image fetches used by the browser UI.
- Manifest, segment, text-track, and image-track origins must allow `GET`, `HEAD`, and byte-range requests and expose `Accept-Ranges`,
  `Content-Length`, and `Content-Range` when emitted.
- Redirect targets must satisfy the same CORS and CSP rules as the original media URL.

The checked-in `shaka-playback-adapter.mjs` is module-local and statically imports the pinned package from the application build. It is served from
the application origin. The obsolete WEB-01 `shaka-adapter.mjs` is not a release artifact, and no Shaka CDN, page-script, or global-object fallback
belongs in the application or `script-src`.

## Content Security Policy

Start from the following policy and replace only the API, image, and media origins with the candidate's reviewed allowlist:

```text
default-src 'none';
base-uri 'self';
object-src 'none';
frame-ancestors 'none';
script-src 'self' 'wasm-unsafe-eval';
style-src 'self' 'unsafe-inline';
connect-src 'self' https://api.themoviedb.org https://image.tmdb.org https://storage.googleapis.com;
img-src 'self' data: blob: https://image.tmdb.org https://storage.googleapis.com;
media-src 'self' blob: https://storage.googleapis.com;
worker-src 'self' blob:;
font-src 'self' data:;
form-action 'self';
upgrade-insecure-requests
```

`'unsafe-inline'` is currently required for the inline shell style. Do not add `'unsafe-eval'`, wildcard sources, remote Shaka script origins, or
`data:` to `script-src`. Confirm the final production matrix under the deployed CSP; CSP console violations fail release acceptance. If a media
provider changes its redirect, license, key, manifest, image-track, or worker origin, review and add the narrow origin rather than broadening the
policy categorically.

Also send `Referrer-Policy: strict-origin-when-cross-origin`, `Permissions-Policy` with unused capabilities disabled, and
`X-Content-Type-Options: nosniff`. A frame policy may be tightened beyond `frame-ancestors 'none'` only through an explicit embedding requirement.

## Cache and atomic publication

Use an immutable release identifier at the storage/CDN publication boundary, even though the application currently expects origin-root URLs.
Upload the complete candidate to an isolated versioned release, validate it, then atomically switch the origin-root mapping or release pointer.

- `index.html` and `/config.json`: `Cache-Control: no-store` (or `no-cache, must-revalidate` for HTML when operationally required).
- `streamcore-web.js` and `shaka-playback-adapter.mjs`: unversioned entry names; `Cache-Control: no-cache, must-revalidate` unless the hosting layer maps the
  entire origin root atomically to an immutable release.
- content-hashed `*.wasm`: `Cache-Control: public, max-age=31536000, immutable`.
- `composeResources/**`: use `no-cache, must-revalidate` because resource filenames are not guaranteed to be content hashes. They may be immutable
  only when the whole release has an immutable URL namespace and HTML/JS never mix release namespaces.
- media: honor the media owner's policy; manifests should revalidate, while immutable segments may use long-lived caching.

Do not purge or overwrite an active release in place. A client must never receive HTML/JS from one release and Wasm/resources from another.

## Static fallback and deep links

Serve real files first. For an extensionless route that does not resolve to a file, return `index.html` with status `200` so direct navigation and
reload work for `/login`, `/profiles`, `/profiles/new`, `/profiles/{id}/edit`, `/home`, `/search`, `/library`, `/details/{contentId}`, and
`/player/{contentId}`. The application performs authentication/profile canonicalization after startup.

Never rewrite `/config.json`, `composeResources/**`, JavaScript, Wasm, adapters, images, manifests, segments, tracks, fonts, source maps, or any other
path with a file extension to HTML. Missing assets return `404`; traversal or malformed paths return `400` or `404`. Preserve query strings while
matching on the URL path, and serve the same fallback behavior on reload and browser Back/Forward navigation.

## Rollback

1. Stop promotion if artifact validation, browser tests, visual inspection, CSP/CORS checks, any non-waived current Safari gate, or the final live
   journey is not green. Safari is explicitly waived as `WAIVED / NOT RUN`; Candidate 7's non-live and live/cleanup gates are green.
2. Keep the previous application artifact and its compatible runtime config available as one rollback unit.
3. Atomically repoint the origin root/release alias to that unit; do not copy individual files over a live release.
4. Purge only `index.html` and `/config.json` where caches require it. Content-hashed assets remain immutable.
5. Run a no-secret readiness check, one authenticated navigation smoke through user-controlled credentials, and confirm session cleanup.
6. Preserve failed-candidate logs after URL/query/header redaction; do not preserve credentials, tokens, session IDs, or response bodies containing
   account data.

Client-side profile/progress storage may outlive a rollback. Therefore a release must remain tolerant of the last accepted browser-storage schema or
ship an explicit forward/backward-compatible migration before promotion.

## Manual current Safari/macOS acceptance

Automated Playwright WebKit is not Safari evidence. The user explicitly waived this gate on 2026-09-04. The checklist therefore remains
**WAIVED / NOT RUN**, not `PASS`; no current Safari or macOS execution evidence is claimed. This explicit waiver removes the Safari gate as the
current WEB-04 blocker without treating Playwright WebKit as a substitute.

| Field | Result |
|---|---|
| Status | **WAIVED / NOT RUN** |
| Operator/date | User waiver / 2026-09-04 |
| macOS version/build | NOT RUN |
| Safari version/build | NOT RUN |
| Release identifier | NOT RUN |
| Public Sintel starts after user activation | NOT RUN |
| Pause/play, seek/scrub and speed | NOT RUN |
| Quality, audio and subtitle controls | NOT RUN |
| Fullscreen enter/exit and focus restoration | NOT RUN |
| Back/Escape closes exactly one layer | NOT RUN |
| Hard reload resumes the selected profile's progress | NOT RUN |
| Recoverable failure shows sanitized copy and Retry recovers | NOT RUN |
| No-filmstrip state remains usable | NOT RUN |
| Repeated enter/play/close leaves no video/session/timer/listener | NOT RUN |
| Logout and temporary-session cleanup | NOT RUN |
| Redacted observations | NOT RUN |

## Candidate 4 live journey, correction, and Candidate 7 status

- Attempt 1 through the boolean-clean wrapper: **FAIL**, 0/1 in 49.7s.
- Reached real login/session, search, Details, and public Sintel; failed while waiting for projected `Play` after an attempted `Pause`.
- Fallback `DELETE` cleanup is confirmed only by live-smoke control flow because no cleanup exception replaced the original failure. Browser
  local/session-storage cleanup was awaited.
- The bounded test-only correction re-resolves stable projected bounds after hover/recomposition and uses native `HTMLVideoElement.paused` state
  as the pause/play oracle. Delta review: **PASS**.
- Attempt 2 on harness revision `eb37969`: **FAIL**, 0/1 in 49.1s. The stable projected `Pause` bounds were clicked, but native
  `video.paused` remained `false`. Fallback `DELETE` and browser local/session-storage cleanup were confirmed only by live-smoke control flow.
- Root cause: the production HtmlElementView immediate parent host intercepted pointer input above the Compose canvas. Production now applies
  immediate video/host `pointer-events:none`, retries unattached hosts for at most six animation frames, reapplies on the post-attachment frame and
  HtmlElementView updates, and cancels pending work on update/release.
- Focused correction evidence: compile initially failed in 10s then passed in 3s (8 actionable, 6 executed/2 up-to-date); engine 21/21 passed in
  54s (148 actionable, 12/136); player UI 14/14 passed up-to-date in 2s (180 actionable, 15/165); WebApp 65/65 passed in 43s (342 actionable,
  66/276); development distribution passed in 35s (338 actionable, 63/275); six-project Play/Pause regression passed 6/6 in 36.8s.
- Candidate 4's non-live pass is historical and not acceptance-eligible. Candidate 5 subsequently failed its complete matrix at 183 passed / 3
  failed / 0 skipped in 26.1m. The bounded classifier correction uses only the established exact navigation-teardown signatures with per-phase or
  per-epoch totals, per-signature uniqueness, and required pair adjacency. A coroutine singleton or one exact adjacent pair shares one signature;
  pair plus singleton or any repeat remains fatal. The affected WebKit focused rerun passed 4/4 in 1.3m, but does not replace the failure.
  Candidate 6 subsequently failed its complete matrix at 185 passed / 1 failed / 0 skipped in 26.0m on unknown WebKit `Context is stopped` during
  cross-document Back. No whitelist was added: the test now uses projected Player navigation, Escape/Back disposal, browser Forward re-entry, and
  final Back disposal with exact route-scoped counters before clean diagnostics.
- Candidate 7 `1cb7ca253182f5f61ed07e7c9905f18e1307c469` passed production/Binaryen distribution and artifact validation, locked install, the complete
  186/186 six-project matrix in 22.2m, current controls/settings visual inspection at both required viewports, and the combined Android/root gate
  in 58s with 2,350 actionable tasks.
- Candidate 7 live attempt 1 **FAIL** at 0/1 in 50.4s after reaching the production Player/public Sintel. The projected `Pause` node appeared
  enabled while the actual control rendered its disabled buffering spinner, so the pre-readiness click dispatched no playback command. A
  credential-free real-media probe proved the unchanged production Player pauses correctly once literal rendered `Pause` is present
  (`pauseCalls=1`, `playCalls=0`, native `paused=true`); all experimental z-order changes were reverted.
- Test-only correction `acc13d034a4d05bb0b3f945d6463e5299b9d5e7f` requires exact rendered Play/Pause content before stable physical activation and retains native
  state assertions.
- Candidate 7 live attempt 2 **FAIL** at 0/1 in 59.9s after corrected playback, seek/fullscreen, Player exit, and hard reload. The restored Details
  `Play` control's centered focus/hover scale could not satisfy the obsolete all-edge bounds oracle. Test-only correction
  `09c346ea40d02da5fbd3e94ee8ba17b765879c31` stabilizes the positive-area center and preserves post-hover remeasurement; the exact credential-free
  Player→Back→reload→Play journey passed 1/1 in 20.5s.
- Candidate 7 live attempt 3 **FAIL** at 0/1 in 58.4s at the same restored-Details checkpoint because consecutive projected centers still did not
  settle. Correction `b70eabf328c000dc27b85374e57a601d6dee0a68` retains unique positive-area bounds and post-hover remeasurement but clicks the latest center
  without cross-frame stability; the exact credential-free transition passed 1/1 in 21.3s.
- Candidate 7 live attempt 4 **FAIL** at 0/1 in 56.2s after repeating all prior checkpoints because restored resumable content correctly exposed
  `Resume`, while the harness queried `Play`. One-word correction `1a7f2c3f57489c737a2c6c5de9db0773cb79a4cb` targets exact `Resume`.
- Fallback temporary-session and browser-storage cleanup for all four failed Candidate 7 attempts were confirmed by harness control flow.
- Candidate 7 live attempt 5 **PASS** at 1/1 in 35.7s. The complete Login → Profiles → Search/Details/My List/Library → production Player →
  Pause/Play → seek → fullscreen → Back → hard reload/Resume → Back → logout journey passed. All 16 required TMDB endpoint classes were observed
  only at 2xx, application-driven temporary-session deletion was confirmed, browser storage was cleared, and no secret value was emitted.
- Candidate 7 is accepted for primary fast-forward. Safari/macOS remains explicitly **WAIVED / NOT RUN**, not `PASS`.
