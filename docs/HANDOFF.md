# StreamCoreTV handoff — 2026-09-16

## Start here

Continue on branch `codex/kmp-migration` in `https://github.com/nikospamp/StreamCore-TV.git`.
Read the repository `AGENTS.md` and this file. For launch/capture work, use the [agent workflow](agent-workflow.md).
Load [shared control styling](shared-control-styling.md) when changing shared controls and the
[web/TV audit](web-tv-ui-harmonization.md) when working on that UI. This is the current handoff; `kmp/ORCHESTRATION-HANDOFF.md` records an older
WEB-01/02 checkpoint and its branch/status/next-step instructions are historical.

The target architecture is backend-agnostic. Shared/core/feature contracts consume app models, never provider DTOs or SDKs.

## Current work

Portable review tooling is documented in [agent workflow](agent-workflow.md). Use its scoped preflight before
client launch/capture, one shared build/server owner, and deterministic composition of original screenshots.
PC 1 verification on 2026-09-24 passed 30 Node tests (including real Chrome fixtures), 78 PowerShell assertions,
`clientB`/TMDB benchmark compilation, development-web webpack, the authenticated Android assembly, and the
production web build/release validator. Authenticated Profiles captures passed on the connected OnePlus AC2003
and Chrome; both originals and the deterministic board were visually inspected. Local evidence and provenance
are under ignored `build/review/pc1-final` and `build/review/provenance`. PC 2 runtime verification remains pending;
follow [the PC 2 verification checklist](verify-review-tooling-pc2.md) after pulling the tooling commit.

- `361dd90`: reviewed TV player harmonization and shared controls/settings.
- `1bbb81d`: ignore local marketing/presentation folders.
- `611acdc`: web/TV harmonization, shared renderers, browser input/focus fixes, and audit documentation.
- The commit containing this handoff completes the transfer checkpoint. Use the branch HEAD on the new PC.

Android mobile remains the visual reference; the user approved the latest tablet/TV iteration. Web now follows the approved TV
language across Login, Profiles/manage/editor/avatar picker, Home, Search, Library, Details, Player/settings/error, and navigation.
The next step is the user's manual web review and specific feedback. No additional redesign or wholesale HTML replacement is requested.

Web uses 38 distinct shared UI composables directly: 24 renderers/content components and 14 standalone icons. This excludes state,
ViewModels, effects, tokens, and web-only wrappers. Newly extracted renderers are `HomeHeroArtwork`, `HomeHeroContent`, and
`StreamCoreContentCardArtwork`; existing shared profile, Details, and player rendering is reused. Preserve Android defaults when editing them.

Six native HTML integration boundaries remain in production: login credentials form; profile-name input; editor Close/Save/Delete
buttons; profile deletion confirmation; Search input; and playback video. These retain browser editing/autofill/validation/focus/media
behavior. Other app rendering uses Compose; browser listeners/fullscreen/history and the page shell are separate integration plumbing.

Important implementation locations:

- `core/ui-web`: browser action surfaces, artwork buttons, media cards, and TV-style navigation rail.
- `core/ui/.../StreamCoreContentCardArtwork.kt` and `feature/home/ui-common`: shared TV/web artwork and hero rendering.
- `feature/*/ui-web`: browser screens, input ownership, and responsive layout.
- `feature/profiles/ui-web`: native editor controls and the single-viewport avatar overlay/Escape effect.
- `feature/player/ui-web`: Compose controls/settings/error overlays around the browser-owned video surface.
- `webApp`: composition root, routing, runtime configuration, and local serving.

## Working preferences and pitfalls

- Coordinate parallel agents on disjoint implementation/review scopes; keep one integration/build owner. The user prefers Astra agents
  with reasoning suited to task complexity, fast iteration, concise updates, and minimal repeated checks.
- The earlier UI-harmonization task deferred its new regression suite until that UI review is finalized. That task-specific
  deferral does not apply to tooling fixes or unrelated development; run tests appropriate to the current change and required gates.
- Preserve the established brand; use impeccable for UI work. Share portable rendering, while platform modules own layout and input.
- Keep container/background/content clip/indication/border shapes aligned. Reserve TV focus-ring clearance at scroll edges.
- A separate Compose browser dialog viewport stranded accessibility nodes after dismissal. Avatar/player overlays now stay in the
  main Compose viewport, isolate background focus/semantics, and restore focus. Do not casually reintroduce `Dialog` for these panels.
- Avatar Escape uses a scoped document listener because removing a focused native input can lose DOM keyboard ownership. Dispose listeners
  when the overlay leaves; avoid duplicate Escape/Back dispatch. Player Escape closes its overlay before fullscreen/navigation.
- Native HTML inputs must not paint over Compose focus borders. Search uses a transparent native input and an inset interop rectangle.
- Login Forgot password/Create account/Help remain disabled while their browser flows are unavailable.
- Serialize Gradle/webpack/Binaryen work. Production and development sync tasks share Kotlin npm staging; a production build can invalidate
  an active development server. Stop development serving before a production build, then serve the completed production distribution.

## New-PC setup

Fetch and switch to `codex/kmp-migration`; for a fresh clone:

```powershell
git clone --branch codex/kmp-migration https://github.com/nikospamp/StreamCore-TV.git
cd StreamCore-TV
```

Use an Android Studio/JDK installation compatible with AGP 9.1.1, Android SDK platform 37, and the checked-in Gradle 9.3.1 wrapper.
The previous PC used Android Studio's bundled JBR. Set `JAVA_HOME` and the local Android SDK path for this PC. Node.js/npm are needed
for the preview server and release validator; Gradle manages the Kotlin/Wasm JS tooling.

These files are intentionally ignored and do **not** transfer through Git:

- `local.properties`: machine-specific SDK path and TMDB configuration. Recreate it locally with `sdk.dir`, `tmdbBaseUrl`,
  `tmdbReadAccessToken`, and `tmdbAccountId` as needed. No values are included in this handoff.
- `docs/credentials/tmdb.txt`: authorized login credentials. Transfer privately if needed; never commit or print its contents.
- Generated `webApp/build/...`, root `build/...`, APKs, build logs, and `build/web-tv-review/` screenshot/gallery files.
- Ignored local marketing/presentation material and other local-only directories.

Browser sessions, ADB connections, emulator state, and the old PC's localhost server do not transfer. Sign in/select a profile on this PC.
The user authorized use of the existing login credentials for project verification; keep credentials out of logs, Git, and artifacts.

## Run web

For iteration, with local configuration restored:

```powershell
.\gradlew.bat :webApp:wasmJsBrowserDevelopmentRun
```

For a fixed production preview, stop the development server first:

```powershell
.\gradlew.bat :webApp:generateWebDevelopmentConfig :webApp:wasmJsBrowserDistribution
npm --prefix webApp/e2e run validate:release
$env:STREAMCORE_WEB_PORT = '8080'
$env:STREAMCORE_WEB_RUNTIME_CONFIG = (Resolve-Path 'webApp/build/generated/webDevelopmentConfig/config.json').Path
node webApp/e2e/server.mjs
```

Open `http://localhost:8080/`. Both servers bind to loopback. Runtime configuration stays outside the production distribution; never
copy credentials/config into the deployable bundle. Use `-PstreamcoreLocalPropertiesPath=<absolute path>` or `STREAMCORE_LOCAL_PROPERTIES`
when working from a linked checkout instead of copying private configuration.

## Verification already completed

These are recorded results from the implementation, not fresh runs on the new PC:

- Production `:webApp:wasmJsBrowserDistribution` and release validator passed: 67 assets, two Wasm files, zero real config files packaged.
- `:app:compileTmdbDebugKotlin`, `:app:compileClientBDebugKotlin`, and all seven `feature:*:ui-web:compileAndroidMain` targets passed.
- `verifyDesignTokens`, `verifyKmpTestTargets`, `verifyKmpAndroidCompilerFlags`, `verifyKmpConventionPlugins`,
  `verifyKmpDependencyCompatibility`, and `:app:verifyTmdbRuntimeConfig -PrequireTmdbRuntimeConfig=true` passed.
- Authenticated browser review covered login, profiles/manage/add/edit/avatar, Home, Search/results, Details, Library empty states,
  real playback/controls/settings/quality, Back/direct-route reloads, modal dismissal, accessibility-node restoration, and gear activation
  after settings close. Home/Search were also checked at narrower widths. Profile edits were not saved.
- No new automated regression suite, full cross-browser matrix, Android visual regression pass, or frame-rate claim accompanies this change.
  The deferred checklist is in the web/TV audit. This commit/push handoff only reruns Git whitespace/scope checks.

## Libraries and next actions

Versions remain Compose Multiplatform 1.12.0, Material3 1.9.0, Kotlin 2.3.21, Coil 3.4.0, and Koin 4.2.2. No dependency upgrades were
included in harmonization. The audit records official references and separate Coil/Kotlin migration candidates; reassess compatibility
before upgrading, and do not align Material3's version number to Compose by assumption.

1. Restore local configuration and start the app on the new PC.
2. Take the user's screen-specific feedback; retain shared rendering and platform ownership while iterating.
3. Keep the deferred regression list current, then add focused tests after UI sign-off.

Suggested prompt for the new Codex task: "Read AGENTS.md and docs/HANDOFF.md, inspect the current branch, and continue the web/TV UI review.
Preserve the accepted Android UI, shared Compose components, and browser-native integration boundaries. Use parallel agents for independent
work and keep verification focused."
