# PC 2 review-tooling verification

Run this once after pulling the portable review tooling. Keep existing local changes, including the Foojay resolver
fix and `docs/SETUP-VERIFICATION.md`. Fetch `codex/kmp-migration` and use a fast-forward pull only; report a conflict
instead of resetting, overwriting, or discarding work. The app architecture remains backend-agnostic.

The user explicitly authorized using the existing private TMDB credentials for live verification on both PCs.
Read them locally when login is needed; never print, commit, copy into artifacts, or paste their values.

1. Read `AGENTS.md` and the applicable sections of `docs/agent-workflow.md`. One coordinator owns shared setup,
   builds, and servers. Do not change global model/security/tool settings or reinstall tools in response to a
   permission denial.
2. Run `npm --prefix webApp/e2e run test:review-tools` with `STREAMCORE_REVIEW_BROWSER_TEST=1` for the real Chrome
   fixtures, and `pwsh -NoProfile -File tools/review/tests/review-tools.tests.ps1`. Install pinned dependencies only
   if missing. Preserve/restore any pre-existing value of the fixture environment flag.
3. Run scoped Android preflight with `-CheckDevices`, inspect the actual ready serials, and select a client. Also
   invoke preflight once by absolute path from another working directory to verify portable path resolution.
4. Inspect `review-build.mjs status --target all`. Rebuild only missing/stale/unknown certified artifacts using
   `review-build.mjs build --target android|web|all`; this runs the required authenticated configuration gate.
   Keep normal Android Studio, clientB, benchmark, and development-server workflows available. Preserve local
   configuration and stop only a development server owned by this task before a production build.
5. Use `capture-android.ps1` on one available Android device. Install the verified APK only when needed and launch
   with the explicit switches. Do not uninstall or clear app data to resolve a signature conflict. Navigate to
   Profiles, signing in from the existing private login file if needed. Inspect the original capture; the command
   itself does not verify which screen is visible.
6. Identify/reuse a matching review server, or start one on a free loopback port and retain its owning process.
   Run web preflight with Chrome, then `review-web.mjs --screen profiles` with a new output path. Use the existing
   credential file lazily; close the owned browser/server afterward. Do not bypass checkout/freshness checks or
   mark a stale/development capture as current production verification.
7. Compose and visually inspect a deterministic board of the two originals using `review-board.mjs`. Keep all
   outputs under ignored `build/review/`, retain screenshot hashes, and report the original capture paths.

Return a concise report: commit tested; test results; selected device/browser; build provenance; live capture and
visual-inspection results; any blocked step and its exact category. Distinguish missing permission/configuration
from code defects. Do not broaden into UI redesign, dependency upgrades, or unrelated environment repairs.
