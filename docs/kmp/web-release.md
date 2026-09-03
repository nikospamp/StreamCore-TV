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
`config.example.json`, and the module-local `shaka-adapter.mjs`. It verifies that local HTML script references resolve, every packaged Wasm name is
content-versioned and referenced by the JavaScript bundle, the example config contains placeholders only, and no real `config.json`, raw JWT/read
token, bearer value, session value, or concrete token/account config value is embedded in HTML, JavaScript, modules, JSON, source maps, or Wasm
string content. Every HTML script source is decoded and resolved inside the validated distribution before its existence is accepted.

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

The checked-in Shaka adapter is module-local and resolves the pinned package from the application build. It is served from the application origin;
no Shaka CDN or global-script fallback belongs in `script-src`.

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
- `streamcore-web.js` and `shaka-adapter.mjs`: unversioned entry names; `Cache-Control: no-cache, must-revalidate` unless the hosting layer maps the
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

1. Stop promotion if artifact validation, browser tests, visual inspection, CSP/CORS checks, current Safari, or the final live journey is not green.
2. Keep the previous application artifact and its compatible runtime config available as one rollback unit.
3. Atomically repoint the origin root/release alias to that unit; do not copy individual files over a live release.
4. Purge only `index.html` and `/config.json` where caches require it. Content-hashed assets remain immutable.
5. Run a no-secret readiness check, one authenticated navigation smoke through user-controlled credentials, and confirm session cleanup.
6. Preserve failed-candidate logs after URL/query/header redaction; do not preserve credentials, tokens, session IDs, or response bodies containing
   account data.

Client-side profile/progress storage may outlive a rollback. Therefore a release must remain tolerant of the last accepted browser-storage schema or
ship an explicit forward/backward-compatible migration before promotion.

## Manual current Safari/macOS acceptance

Automated Playwright WebKit is not Safari evidence. A qualified operator must run this checklist on a current public Safari build and record the
browser and macOS versions. A blank or `NOT RUN` row blocks release acceptance.

| Field | Result |
|---|---|
| Status | **NOT RUN** |
| Operator/date | NOT RUN |
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
