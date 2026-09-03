# WEB-03F — Integrate and Accept the Browse Milestone

## Dispatch and Merge Inputs

- **Depends on:** accepted WEB-03A plus reviewed WEB-03B, WEB-03C, WEB-03D, and WEB-03E commits.
- **Branch:** `codex/web-03f-browse-integration` created at the exact accepted `WEB-03A_CONTRACT_COMMIT` supplied by the root.
- The handoff lists exact 40-character B–E input SHAs. Missing, dirty, unreviewed, rebased, or contract-divergent input blocks integration.
- **Mandatory order:** merge/cherry-pick WEB-03B → WEB-03C → WEB-03D → WEB-03E. Feature owners do not resolve integration conflicts.

## Ownership

The integration owner exclusively owns `webApp/**`, `settings.gradle.kts`, necessary root/catalog/convention build files, `core/ui-web/**` conflict
fixes, cross-feature E2E/visual evidence, and a dedicated WEB-03 evidence document. Feature-local production edits are forbidden except a minimal,
reviewed conflict correction returned to its owning ticket. Provider/data, Android UI, playback implementation, credentials, generated config, and
build outputs remain forbidden.

## Integration Scope and Expected Behavior

- Wire `WebHomeRoute`, `WebSearchRoute`, `WebLibraryRoute`, and `WebDetailsRoute` into the frozen `WebProductShell` and Koin composition root.
- Replace WEB-02's authenticated landing with `/home`; wire `/search`, `/library`, direct `/details/{contentId}`, and the labelled temporary
  `/player/{contentId}`. IDs reconstruct content through repositories after reload; history never requires a serialized `ContentModel`.
- Persist shell destination/focus state only as frozen backend-agnostic values. Back/Forward and Details/recommendation return restore the originating
  section/item when still present. Escape closes one layer. Logout clears protected state and browser Back cannot reveal authenticated content.
- Validate profile switch, mutations reflected across Details/Library, image fallback/size bounds, keyboard/mouse behavior, accessibility semantics,
  and no provider DTO/network access in web UI.

## Serialized Gate

First run focused compile/tests for only conflict/wiring changes. Freeze production code and complete read-only architecture, backend-boundary,
lifecycle/leak, accessibility, and security review. Only then run one Tier 3 candidate, sequentially:

```powershell
.\gradlew.bat :feature:home:ui-web:compileKotlinWasmJs :feature:search:ui-web:compileKotlinWasmJs :feature:library:ui-web:compileKotlinWasmJs :feature:details:ui-web:compileKotlinWasmJs
.\gradlew.bat :webApp:wasmJsBrowserTest
.\gradlew.bat :webApp:wasmJsBrowserDistribution
Set-Location webApp/e2e
npm ci
npx playwright test
Set-Location ../..
.\gradlew.bat :app:compileTmdbDebugKotlin :app:compileClientBDebugKotlin check -PverifyDesignTokensLogFiles=true --continue --max-workers=1
```

The Playwright candidate is the single complete Chromium/Firefox/WebKit matrix at 1280×720 and 1920×1080. Record counts/failures/skips and inspect
the required loading/content/empty/offline/error/long-text screenshots; existence alone is not approval. A production change invalidates the
candidate. A test-only correction gets only affected reruns and must be reported separately from the original matrix.

## Final Live Gate and Acceptance

After all non-live gates pass, run exactly one redacted TMDB journey through the ignored WEB-02 wrapper: Login → Profiles → Home → Search/Library →
Details → temporary Player → Back → hard reload → logout, followed by mandatory session cleanup. Do not read, print, copy, commit, or screenshot
credentials/session/account data. Obtain action-time approval if Codex would transmit; user-manual wrapper execution reports only redacted outcome.

Accept only with evidence for the aggregate WEB-03 criteria, exact commands/counts, reviewed commit, inspected visuals, live cleanup, Android/root
results, changed files/APIs, deferred playback mapping, and final status. Until then report `blocked`, `failed`, or `not-run`; never overclaim.
