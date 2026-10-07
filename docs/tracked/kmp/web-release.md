# Web release and deployment

The browser application selects TMDB SDK services and the browser playback engine. The target architecture is backend-agnostic; deployment configuration stays outside application artifacts. Use this runbook with [browser testing](web-testing.md) and [SDK verification](../sdk/verification.md).

## Build and validate

Stop development serving before building. From the repository root:

```powershell
.\gradlew.bat :webApp:wasmJsBrowserDistribution
npm --prefix webApp/e2e ci
npm --prefix webApp/e2e run validate:release
```

The output is `webApp/build/dist/wasmJs/productionExecutable`. Set `STREAMCORE_WEB_RELEASE_DIRECTORY` to validate a staged copy instead. Validate the exact directory you will deploy.

The validator requires HTML, JavaScript, referenced content-versioned Wasm binaries, nonempty Compose resources, placeholder configuration, and the local `shaka-playback-adapter.mjs`. It rejects unresolved/escaping script references, remote/global Shaka fallbacks, real `config.json`, and embedded token/session/account configuration. Deploy only validated runtime files; keep source maps private unless a separate symbol policy permits publication, and retain required license notices.

## Runtime configuration

Supply `/config.json` separately at the application's origin:

```json
{
  "tmdbBaseUrl": "https://api.themoviedb.org/",
  "tmdbReadAccessToken": "replace-with-browser-visible-read-access-token",
  "tmdbAccountId": "replace-with-account-id"
}
```

All values are required. Browser-delivered configuration is visible to users. Use HTTPS and a read-access token appropriate for the browser client; never include passwords, signing keys, temporary sessions, or privileged server credentials. TMDB session-authenticated requests use the provider-required `session_id` query parameter; redact it in access logs.

Serve configuration with `Content-Type: application/json; charset=utf-8`, `Cache-Control: no-store`, and `X-Content-Type-Options: nosniff`. Provision and validate it before switching traffic. Do not commit it or copy it into the distribution tree.

Development runs generate their own configuration under `webApp/build/generated/webDevelopmentConfig`, using local TMDB properties. That directory is a development-server input only; production builds do not run the generator. Restart development serving after changing properties.

## Static hosting contract

Serve real files first. Extensionless app routes fall back to `index.html` with status 200 so direct links and reload work: `/login`, `/profiles`, `/profiles/new`, `/profiles/{id}/edit`, `/home`, `/search`, `/library`, `/details/{contentId}`, and `/player/{contentId}`. The app canonicalizes authentication/profile state after startup.

Never rewrite missing configuration, resources, scripts, Wasm, images, media, adapters, or other file-extension paths to HTML. Missing assets return 404; traversal/malformed paths return 400 or 404. Preserve query strings while matching the URL path.

| Extension | Content-Type |
|---|---|
| `.html` | `text/html; charset=utf-8` |
| `.js`, `.mjs` | `text/javascript; charset=utf-8` |
| `.wasm` | `application/wasm` |
| `.json` | `application/json; charset=utf-8` |
| `.css` | `text/css; charset=utf-8` |
| `.svg`, `.png`, `.jpg` / `.jpeg`, `.webp` | `image/svg+xml`, `image/png`, `image/jpeg`, `image/webp` respectively |
| `.woff2` | `font/woff2` |
| `.mpd`, `.m3u8` | `application/dash+xml`, `application/vnd.apple.mpegurl` respectively |
| `.mp4`, `.m4s`, `.webm` | `video/mp4`, `video/iso.segment`, `video/webm` respectively |
| `.vtt` | `text/vtt; charset=utf-8` |

Media origins must preserve range responses: status 206, `Accept-Ranges`, `Content-Range`, and correct `Content-Length`. Honor an origin-required compatible segment MIME type where applicable.

## Origins and security policy

Review the actual API, image, media, redirect, subtitle, and license/key origins used by the configured provider. They must support browser access from the deployed application:

- API CORS permits required headers (`Authorization`, `Accept`, `Content-Type`) and methods (`GET`, `POST`, `DELETE` where used).
- Image fetches and media/manifest requests permit the application origin. Media supports required `GET`, `HEAD`, byte ranges, and exposed range/length headers.
- Redirect targets satisfy the same CORS/CSP rules. Do not disable browser security or reflect unchecked origins with credentials.
- Shaka loads from the pinned local package through `shaka-playback-adapter.mjs`; no CDN script or global fallback is required.

Start with this CSP and replace API/image/media origins with the deployment's reviewed allowlist:

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

Inline styling requires `'unsafe-inline'` in `style-src`. Do not add `'unsafe-eval'`, wildcard sources, or `data:` scripts to fix a violation. Validate under the actual deployed policy; CSP console violations fail acceptance. Also send `Referrer-Policy: strict-origin-when-cross-origin`, `X-Content-Type-Options: nosniff`, and a `Permissions-Policy` disabling unused capabilities. Change embedding policy only for an explicit product requirement.

## Publish, cache, and roll back

Upload a complete validated release to an isolated version, then atomically switch the origin-root mapping. The app expects origin-root paths. Never mix HTML/JS from one version with Wasm/resources from another.

| Resource | Cache policy |
|---|---|
| `/config.json` | `no-store` |
| `index.html` | `no-store`, or `no-cache, must-revalidate` when required |
| Unversioned JS/adapter entries | `no-cache, must-revalidate` unless the origin maps atomically to one immutable release |
| Content-hashed Wasm | `public, max-age=31536000, immutable` |
| `composeResources/**` | Revalidate; filenames are not guaranteed content hashes. Immutable caching requires a versioned release namespace. |
| Media | Follow the origin's policy; manifests revalidate while immutable segments may be long-lived |

Promote only after artifact validation, required browser/live journeys, visual/input review, and deployed CSP/CORS checks pass for that revision. Keep the previous complete artifact and compatible configuration as one rollback unit. On rollback, switch the release pointer atomically, revalidate entry/config responses, exercise authenticated navigation, and confirm temporary-session cleanup. Retain only redacted failure diagnostics.

Browser storage outlives a deployment. Assess persisted schema and rollback behavior together. The development SDK currently has no shipped-data compatibility requirement: it uses namespaced storage and leaves old development keys untouched. A future shipped-data compatibility policy requires its own design and checks.

Manual Safari/macOS acceptance has not been run and was previously waived. Playwright WebKit does not establish Safari behavior. Record the supported browser scope and any explicit waiver for the release being assessed; do not describe unexecuted platform checks as passing.
